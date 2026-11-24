package com.example.pokeworld.world;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.pokeworld.entities.Enemy;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Invasões à base: Pokémons selvagens vêm das bordas e tentam chegar ao Quartel. Eles batem nas
 * muralhas/prédios que bloqueiam o caminho; as torres e os guardiões (Pokémons que ficam na base)
 * atiram neles. Vencer rende recursos; muralhas derrubadas são reconstruídas sozinhas no fim.
 */
public class BaseDefense {
    private static final int T = 64; // = TileMap.TILE_SIZE

    /** Ponte com o resto do jogo (mundo, jogador, efeitos). */
    public interface Host {
        List<Enemy> enemies();
        Enemy spawnRaider(String species, double x, double y, int level);
        void removeEnemy(Enemy e);
        boolean hasElectricPokemon();
        void say(String msg);
        void text(String t, double x, double y, int color);
        void burst(double x, double y, int color, int n);
        void giveItem(String item, int n);
        void addGold(int g);
        int gold();
        int playerLevel();
        void raidWon();
    }

    private static final long FIRST_RAID_DELAY = 6 * 60 * 1000L, RAID_INTERVAL = 9 * 60 * 1000L, RAID_MAX = 120 * 1000L;
    private static final String[] EARLY = {"Rattata", "Pidgey", "Spearow", "Meowth", "Weedle", "Oddish", "Bellsprout", "Caterpie"};
    private static final String[] MID = {"Raticate", "Pidgeotto", "Mankey", "Sandshrew", "Ekans", "Nidorino", "Growlithe", "Machop"};
    private static final String[] LATE = {"Primeape", "Arbok", "Pinsir", "Scyther", "Rhydon", "Ponyta", "Sandslash", "Fearow"};

    /** Tipos de invasão (sorteados a cada vez): mudam quantidade, nível, espécies e prêmio. */
    private enum Kind {
        NORMAL("Invasão", 1.0, 0, 1.0, null),
        ENXAME("ENXAME", 1.7, -2, 1.2, null),
        ELITE("ELITE", 0.5, 6, 1.7, null),
        FOGO("INVASÃO DE FOGO 🔥", 1.0, 1, 1.3, new String[]{"Growlithe", "Ponyta", "Charmander", "Vulpix"}),
        AGUA("INVASÃO DE ÁGUA 💧", 1.0, 1, 1.3, new String[]{"Poliwag", "Squirtle"}),
        PLANTA("INVASÃO DE PLANTAS 🌿", 1.0, 1, 1.3, new String[]{"Oddish", "Bellsprout", "Bulbasaur"}),
        VENENO("INVASÃO VENENOSA ☠️", 1.0, 1, 1.3, new String[]{"Ekans", "Arbok", "Koffing", "Grimer", "Zubat", "Golbat"}),
        FANTASMA("INVASÃO NOTURNA 👻", 0.9, 2, 1.4, new String[]{"Gastly", "Haunter", "Zubat"});
        final String label; final double count; final int lvl; final double reward; final String[] pool;
        Kind(String l, double c, int lv, double r, String[] p) { label = l; count = c; lvl = lv; reward = r; pool = p; }
    }
    private Kind kind = Kind.NORMAL;

    private final BaseManager base;
    private final Host host;
    private final Random rnd = new Random();

    private boolean active = false;
    private long startTime, endTime, nextRaidAt;
    private final List<Enemy> raiders = new ArrayList<>();
    private final Map<BasePiece, Integer> hp = new IdentityHashMap<>();
    private final Map<Enemy, Long> raiderHit = new IdentityHashMap<>();
    private final Map<Enemy, Long> guardShot = new IdentityHashMap<>();
    private final Map<BasePiece, Long> towerShot = new IdentityHashMap<>();
    private final List<BasePiece> fallen = new ArrayList<>();
    private final List<Enemy> guards = new ArrayList<>();
    private final List<int[]> tracers = new ArrayList<>(); // x0,y0,x1,y1,cor
    private final List<Long> tracerExpire = new ArrayList<>();
    private int spawned, downed;
    private int wallsFallen;
    private boolean quartelFell;

    public BaseDefense(BaseManager base, Host host, long now) {
        this.base = base; this.host = host;
        this.nextRaidAt = now + FIRST_RAID_DELAY;
    }

    public boolean isActive() { return active; }
    public int raidersLeft() { return countAlive(); }

    // ================================================================= alcance das torres

    public static float towerRange(BasePiece p) { return towerRange(p.kind, p.level); }
    public static float towerRange(BaseKind k, int level) {
        return k == BaseKind.TORRE_CHOQUE ? (4.5f + 0.4f * level) * T : (6f + 0.5f * level) * T;
    }

    // ================================================================= vida das peças

    public static int maxHp(BasePiece p) {
        if (p.kind.isWall()) return BaseKind.WALL_HP[Math.max(0, Math.min(p.level, BaseKind.WALL_HP.length) - 1)];
        if (p.kind == BaseKind.QUARTEL) return 2500 * p.level;
        if (p.kind.isDefense()) return 500 * p.level;
        return 300 + 150 * p.level;
    }

    /** Fração de vida (0..1) de uma peça durante a invasão; 1 se intacta. */
    public float hpFraction(BasePiece p) {
        Integer v = hp.get(p);
        return v == null ? 1f : Math.max(0f, v / (float) maxHp(p));
    }

    // ================================================================= ciclo

    /** Chamado todo frame enquanto o jogador está na base (e não em modo construção). */
    public void update(long now) {
        if (!active) {
            if (now >= nextRaidAt && hasDefenders()) start(now);
            else if (now >= nextRaidAt) nextRaidAt = now + 60 * 1000L; // sem defesa nenhuma: espera
            return;
        }
        pruneTracers(now);

        // --- guardiões da base (Pokémons guardados que estão soltos na base) ---
        guards.clear();
        for (Enemy e : host.enemies()) if (e.isPet && e.isAlive) guards.add(e);
        for (Enemy g : guards) g.controlled = true;

        // --- invasores ---
        Iterator<Enemy> it = raiders.iterator();
        while (it.hasNext()) {
            Enemy r = it.next();
            if (!r.isAlive) { downed++; it.remove(); continue; }
            moveRaider(r, now);
        }

        if (quartelFell) { fail(now); return; }
        shootTowers(now);
        runGuards(now);

        if (raiders.isEmpty()) { victory(now); return; }
        if (now >= endTime) fail(now);
    }

    private boolean hasDefenders() {
        for (BasePiece p : base.layout.getPieces()) if (p.kind.isDefense() && p.kind != BaseKind.MURALHA && p.isUsable()) return true;
        for (Enemy e : host.enemies()) if (e.isPet) return true;
        return false;
    }

    private int countAlive() {
        int n = 0;
        for (Enemy e : raiders) if (e.isAlive) n++;
        return n;
    }

    // ================================================================= início

    public void start(long now) {
        if (active) return;
        int hq = base.hq();
        Kind[] ks = Kind.values();
        kind = hq >= 2 || rnd.nextBoolean() ? ks[rnd.nextInt(ks.length)] : Kind.NORMAL;   // no começo, quase sempre a invasão normal
        int count = Math.max(2, (int) Math.round((3 + hq * 2 + Math.min(4, host.playerLevel() / 15)) * kind.count));
        int lvl = Math.max(2, hq * 4 + host.playerLevel() / 6 + kind.lvl);
        String[] pool = kind.pool != null ? kind.pool : kind.lvl >= 6 ? LATE : hq >= 4 ? LATE : hq >= 2 ? MID : EARLY;
        raiders.clear(); hp.clear(); fallen.clear(); raiderHit.clear(); guardShot.clear(); towerShot.clear();
        tracers.clear(); tracerExpire.clear();
        spawned = 0; downed = 0; wallsFallen = 0; quartelFell = false;
        for (int i = 0; i < count; i++) {
            double[] pos = spawnPoint(i, count);
            Enemy e = host.spawnRaider(pool[rnd.nextInt(pool.length)], pos[0], pos[1], lvl + rnd.nextInt(3));
            if (e != null) { raiders.add(e); spawned++; }
        }
        if (raiders.isEmpty()) { nextRaidAt = now + 60 * 1000L; return; }
        active = true; startTime = now; endTime = now + RAID_MAX;
        host.say("⚔️ " + kind.label + "! " + raiders.size() + " Pokémons selvagens atacam a base!");
    }

    /** Pontos nas bordas norte/leste/oeste do terreno (o sul tem a estrada). */
    private double[] spawnPoint(int i, int n) {
        int side = i % 3;
        double x, y;
        if (side == 0) { x = (BaseLayout.PLOT_X + 4 + rnd.nextInt(BaseLayout.PLOT_W - 8)) * T; y = (BaseLayout.PLOT_Y - 1) * T; }
        else if (side == 1) { x = (BaseLayout.PLOT_X + BaseLayout.PLOT_W + 1) * T; y = (BaseLayout.PLOT_Y + 4 + rnd.nextInt(BaseLayout.PLOT_H - 14)) * T; }
        else { x = (BaseLayout.PLOT_X - 2) * T; y = (BaseLayout.PLOT_Y + 4 + rnd.nextInt(BaseLayout.PLOT_H - 14)) * T; }
        return new double[]{x, y};
    }

    // ================================================================= invasores

    private BasePiece hq() { return base.layout.find(BaseKind.QUARTEL); }

    private void moveRaider(Enemy r, long now) {
        BasePiece q = hq();
        double tx = q != null ? q.centerX() : 80 * T, ty = q != null ? q.centerY() : 40 * T;
        double dx = tx - r.positionX, dy = ty - r.positionY;
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < 1) return;
        double sp = 1.9 + Math.min(1.0, r.level / 40.0);
        double nx = r.positionX + dx / d * (sp + 14), ny = r.positionY + dy / d * (sp + 14); // olha um pouco à frente
        BasePiece blocker = pieceBlocking(nx, ny);
        if (blocker == null && d > 130) {
            r.stepToward(tx, ty, sp);
            return;
        }
        r.setMoving(false);
        r.faceTowards(tx, ty);
        BasePiece target = blocker != null ? blocker : q;
        if (target == null) return;
        Long last = raiderHit.get(r);
        if (last != null && now - last < 1000) return;
        raiderHit.put(r, now);
        int dmg = 12 + r.level * 2;
        hitPiece(target, dmg, r.positionX, r.positionY, now);
    }

    private BasePiece pieceBlocking(double wx, double wy) {
        int gx = (int) Math.floor(wx / T), gy = (int) Math.floor(wy / T);
        BasePiece p = base.layout.at(gx, gy);
        if (p == null || !base.layout.blocks(wx, wy)) return null;
        return p;
    }

    private void hitPiece(BasePiece p, int dmg, double fromX, double fromY, long now) {
        Integer cur = hp.get(p);
        int v = (cur == null ? maxHp(p) : cur) - dmg;
        hp.put(p, v);
        host.burst(p.centerX(), p.centerY(), Color.rgb(200, 160, 90), 3);
        if (v <= 0 && p.kind != BaseKind.QUARTEL) {
            hp.remove(p);
            base.layout.remove(p);
            fallen.add(p);
            if (p.kind.isWall()) wallsFallen++;
            host.burst(p.centerX(), p.centerY(), Color.GRAY, 14);
        } else if (v <= 0) {
            quartelFell = true; // tratado depois do laço (não mexe na lista durante a iteração)
        }
    }

    // ================================================================= torres e guardiões

    private void shootTowers(long now) {
        boolean electric = host.hasElectricPokemon();
        for (BasePiece p : base.layout.getPieces()) {
            if (!p.isUsable() || !(p.kind == BaseKind.TORRE_VIGIA || p.kind == BaseKind.TORRE_CHOQUE)) continue;
            if (p.kind == BaseKind.TORRE_CHOQUE && !electric) continue;
            long cd = p.kind == BaseKind.TORRE_CHOQUE ? 2000 : 1400;
            Long last = towerShot.get(p);
            if (last != null && now - last < cd) continue;
            Enemy t = nearest(p.centerX(), p.centerY(), towerRange(p));
            if (t == null) continue;
            towerShot.put(p, now);
            double ox = p.centerX(), oy = p.gy * T + 8;
            if (p.kind == BaseKind.TORRE_CHOQUE) {
                int hit = 0;
                for (Enemy e : raiders) {
                    if (!e.isAlive || dist(e.positionX, e.positionY, p.centerX(), p.centerY()) > towerRange(p)) continue;
                    zap(e, 30 + 20 * p.level, ox, oy, Color.rgb(255, 235, 60), now);
                    if (++hit >= 3) break;
                }
            } else {
                zap(t, 20 + 14 * p.level, ox, oy, Color.rgb(255, 170, 80), now);
            }
        }
    }

    private void runGuards(long now) {
        for (Enemy g : guards) {
            Enemy t = nearest(g.positionX, g.positionY, 900);
            if (t == null) { g.setMoving(false); continue; }
            double d = dist(g.positionX, g.positionY, t.positionX, t.positionY);
            if (d > 300) {
                // anda até ficar ao alcance, sem atravessar muralhas
                double vx = t.positionX - g.positionX, vy = t.positionY - g.positionY;
                double k = 3.0 / d;
                double sx = vx * k, sy = vy * k;
                double nx = g.positionX + sx, ny = g.positionY + sy;
                if (!base.layout.blocks(nx, ny)) { g.positionX = nx; g.positionY = ny; g.setMoving(true); }
                else if (!base.layout.blocks(g.positionX + sx, g.positionY)) { g.positionX += sx; g.setMoving(true); }
                else if (!base.layout.blocks(g.positionX, g.positionY + sy)) { g.positionY += sy; g.setMoving(true); }
                else g.setMoving(false);
                g.faceTowards(t.positionX, t.positionY);
            } else {
                g.setMoving(false);
                g.faceTowards(t.positionX, t.positionY);
                Long last = guardShot.get(g);
                if (last == null || now - last >= 900) {
                    guardShot.put(g, now);
                    zap(t, 14 + g.level * 3, g.positionX, g.positionY - 10, Color.rgb(120, 220, 255), now);
                }
            }
        }
    }

    private void zap(Enemy target, int dmg, double fx, double fy, int color, long now) {
        target.takeDamage(dmg);
        host.text("-" + dmg, target.positionX, target.positionY - 30, color);
        tracers.add(new int[]{(int) fx, (int) fy, (int) target.positionX, (int) target.positionY, color});
        tracerExpire.add(now + 160);
        if (!target.isAlive) host.burst(target.positionX, target.positionY, Color.WHITE, 12);
    }

    private Enemy nearest(double x, double y, double range) {
        Enemy best = null; double bd = range;
        for (Enemy e : raiders) {
            if (!e.isAlive) continue;
            double d = dist(e.positionX, e.positionY, x, y);
            if (d <= bd) { bd = d; best = e; }
        }
        return best;
    }

    private static double dist(double ax, double ay, double bx, double by) {
        double dx = ax - bx, dy = ay - by; return Math.sqrt(dx * dx + dy * dy);
    }

    // ================================================================= fim

    private void victory(long now) {
        int hq = base.hq();
        int gold = 0, wood = 0, iron = 0, balls = 0, fruit = 0;
        for (int i = 0; i < spawned; i++) {
            gold += (int) ((30 + 15 * hq + rnd.nextInt(20)) * kind.reward * (1.0 + com.example.pokeworld.managers.EventCalendar.baseRewardBonus()));
            wood += 1 + rnd.nextInt(3);
            if (rnd.nextDouble() < 0.7) iron += 1 + rnd.nextInt(2);
            if (rnd.nextDouble() < 0.40) balls += 1;
            if (rnd.nextDouble() < 0.35) fruit += 1;
        }
        host.addGold(gold);
        if (wood > 0) host.giveItem("Madeira", wood);
        if (iron > 0) host.giveItem("Ferro", iron);
        if (balls > 0) host.giveItem("Pokébola", balls);
        if (fruit > 0) host.giveItem("Fruta", fruit);
        if (kind != Kind.NORMAL && rnd.nextDouble() < 0.25 * kind.reward) {   // invasões especiais podem soltar uma Gema de Boost
            host.giveItem("Gema de Boost", 1);
            host.say("💎 Os invasores deixaram uma Gema de Boost!");
        }
        StringBuilder sb = new StringBuilder("🏆 " + kind.label + " REPELIDA! +" + gold + "🪙");
        if (wood > 0) sb.append(" +").append(wood).append("🪵");
        if (iron > 0) sb.append(" +").append(iron).append("⛓️");
        if (balls > 0) sb.append(" +").append(balls).append("🔴");
        if (fruit > 0) sb.append(" +").append(fruit).append("🍎");
        host.say(sb.toString());
        host.raidWon();
        finish(now);
    }

    private void fail(long now) {
        int lost = Math.min(300, host.gold() / 20);
        if (lost > 0) host.addGold(-lost);
        for (Enemy e : raiders) host.removeEnemy(e);
        host.say("💥 Os invasores saquearam a base" + (lost > 0 ? " (-" + lost + "🪙)" : "") + ". Reforce as defesas!");
        finish(now);
    }

    private void finish(long now) {
        // reconstrói de graça o que caiu
        for (BasePiece p : fallen) if (base.layout.isFree(p.kind, p.gx, p.gy, null)) base.layout.add(p);
        if (wallsFallen > 0) host.say("🧱 " + wallsFallen + " muralhas reconstruídas.");
        fallen.clear(); hp.clear(); raiders.clear();
        for (Enemy g : guards) g.controlled = false;
        guards.clear();
        active = false;
        nextRaidAt = now + RAID_INTERVAL;
    }

    /** Cancela tudo sem prêmio nem punição (troca de mapa). */
    public void abort() {
        if (!active) return;
        for (BasePiece p : fallen) if (base.layout.isFree(p.kind, p.gx, p.gy, null)) base.layout.add(p);
        fallen.clear(); hp.clear(); raiders.clear();
        for (Enemy g : guards) g.controlled = false;
        guards.clear();
        active = false;
    }

    // ================================================================= desenho

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** No mundo: rastros dos tiros e barras de vida das peças atacadas. */
    public void draw(Canvas c, long now) {
        if (!active) return;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        for (int i = 0; i < tracers.size(); i++) {
            int[] t = tracers.get(i);
            paint.setColor(t[4]);
            c.drawLine(t[0], t[1], t[2], t[3], paint);
        }
        paint.setStyle(Paint.Style.FILL);
        for (Map.Entry<BasePiece, Integer> en : hp.entrySet()) {
            BasePiece p = en.getKey();
            float f = Math.max(0f, en.getValue() / (float) maxHp(p));
            float w = Math.max(T, p.pw() * 0.8f), x = (float) p.centerX() - w / 2, y = p.py() - 10;
            paint.setColor(Color.BLACK); c.drawRect(x - 1, y - 1, x + w + 1, y + 7, paint);
            paint.setColor(f > 0.5f ? Color.rgb(80, 200, 80) : f > 0.25f ? Color.rgb(230, 200, 60) : Color.rgb(220, 60, 50));
            c.drawRect(x, y, x + w * f, y + 6, paint);
        }
    }

    private void pruneTracers(long now) {
        for (int i = tracers.size() - 1; i >= 0; i--) {
            if (now >= tracerExpire.get(i)) { tracers.remove(i); tracerExpire.remove(i); }
        }
    }

    /** Texto do aviso fixo na tela durante a invasão (null = nada). */
    public String hudText(long now) {
        if (!active) return null;
        return "⚔️ " + kind.label + "  " + countAlive() + "/" + spawned + " invasores  ⏱ " + Math.max(0, (endTime - now) / 1000) + "s";
    }

    /** Tempo até a próxima invasão automática, em segundos (-1 = em andamento). */
    public long secondsToNext(long now) { return active ? -1 : Math.max(0, (nextRaidAt - now) / 1000); }
}
