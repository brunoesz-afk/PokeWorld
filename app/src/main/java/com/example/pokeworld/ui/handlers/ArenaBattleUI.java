package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.managers.ArenaDuel;
import com.example.pokeworld.managers.ArenaLadder;
import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.RewardManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.world.ArenaLayout;
import com.example.pokeworld.world.RoomOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Duelo da Arena, numa SALA própria do jogo (mapa "DUEL_ARENA"): arena quadrada com a LINHA no meio, o oponente (escondido)
 * em cima e o seu retângulo de treinador embaixo. Você anda com o joystick atrás da linha (no seu retângulo e na sua metade
 * da arena) e LANÇA o Pokémon tocando em qualquer ponto da sua metade. Durante o preparo (com tempo) dá para relançar e
 * escolher até 3 Pokémon; aperte PRONTO. Quando os dois estiverem prontos, o oponente é revelado e os Pokémon lutam sozinhos;
 * na luta você troca de Pokémon a cada 5 segundos (o treinador lança outra bola).
 * Esta classe é o HUD (telas) e também a camada do mundo (RoomOverlay) que desenha a arena e manda na câmera.
 */
public class ArenaBattleUI extends BaseUI implements RoomOverlay {
    public final RectF exitBtn = new RectF(), readyBtn = new RectF(), continueBtn = new RectF();
    public final RectF[] cards = new RectF[6];

    public ArenaDuel duel;
    public ArenaLadder.Fighter fighter;
    public List<ArenaDuel.Unit> roster = new ArrayList<>();
    public final List<Integer> picks = new ArrayList<>();     // índices do roster, na ordem (1º = inicial)
    public ArenaLadder.Result result;
    private boolean spent = false, resultApplied = false;
    private long lastT = 0;
    private GameView gv;

    // sala
    public boolean thrown = false;                  // já lançou o Pokémon na arena?
    public float placeX, placeY;                    // onde lançou (mundo; pés do Pokémon)
    private float foePlaceX = 0.5f;                 // onde o oponente fica (fração da largura)
    private float meX, meY, foeX, foeY;             // pés dos Pokémon (mundo)
    private long fightT0 = 0;
    private ArenaDuel.Phase lastPhase = null;
    private String returnMap = "CITY";
    private double returnX, returnY;
    private double camX, camY; private boolean camInit = false;
    public static final float PK = 96;              // tamanho dos Pokémon no mundo
    private static final long BALL_MS = 520;

    private static final class Pop { String text; int color; boolean onFoe; long t0; int slot; float x, y; }
    private final List<Pop> pops = new ArrayList<>();
    private static final class Ball { float x0, y0, x1, y1; long t0; boolean foe; }
    private final List<Ball> balls = new ArrayList<>();
    private long meLunge = 0, foeLunge = 0;

    public ArenaBattleUI() { for (int i = 0; i < cards.length; i++) cards[i] = new RectF(); }

    // ------------------------------------------------------------------ montagem
    /** Entra na sala da Arena para enfrentar o lutador f. */
    public void begin(GameView gv, ArenaLadder.Fighter f, List<ArenaDuel.Unit> foeTeam) {
        this.gv = gv;
        fighter = f;
        if (gv.world != null) {
            String m = gv.world.getMapName();
            if (!ArenaLayout.MAP.equals(m)) { returnMap = m == null || m.isEmpty() ? "CITY" : m; returnX = gv.player.getPositionX(); returnY = gv.player.getPositionY(); }
        }
        gv.player.setMounted(false);
        if (gv.world != null) { gv.world.loadMap(ArenaLayout.MAP, gv.player, gv.companion, "CENTER"); gv.world.roomOverlay = this; }
        duel = new ArenaDuel(new Random());
        duel.foe.name = f.name;
        duel.foe.team.addAll(foeTeam);
        duel.foe.active = 0;
        duel.foeReadyIn = 4000 + new Random().nextInt(9000);     // o oponente demora um pouco para ficar pronto
        duel.prepLeft = ArenaDuel.PREP_MS;
        roster = ArenaLadder.playerRoster(gv);
        duel.me.name = com.example.pokeworld.managers.FriendManager.getInstance().getMyName();
        picks.clear();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < roster.size(); i++) order.add(i);
        java.util.Collections.sort(order, (a, b) -> Integer.compare(roster.get(b).power(), roster.get(a).power()));
        for (int i = 0; i < Math.min(ArenaDuel.MAX_TEAM, order.size()); i++) picks.add(order.get(i));
        rebuildMe();
        pops.clear(); balls.clear(); result = null; spent = false; resultApplied = false; lastT = System.currentTimeMillis();
        thrown = false; foePlaceX = 0.3f + new Random().nextFloat() * 0.4f; fightT0 = 0; lastPhase = null; camInit = false;
        placeX = (float) ArenaLayout.centerX(); placeY = (float) (ArenaLayout.lineY() + (ArenaLayout.bottom() - ArenaLayout.lineY()) * 0.6);
        gv.ui.state = UIManager.UIState.ARENA_BATTLE;
    }

    public void rebuildMe() {
        duel.me.team.clear();
        for (int p : picks) duel.me.team.add(roster.get(p));
        duel.me.active = duel.me.team.isEmpty() ? -1 : 0;
    }

    /** Toque numa carta da preparação: adiciona, vira inicial ou remove. */
    public void togglePick(int rosterIdx) {
        if (duel != null && duel.meReady) return;       // PRONTO trava o time
        int at = picks.indexOf(rosterIdx);
        if (at < 0) { if (picks.size() < ArenaDuel.MAX_TEAM) picks.add(rosterIdx); }
        else if (at > 0) { picks.remove(at); picks.add(0, rosterIdx); }
        else if (picks.size() > 1) picks.remove(0);
        rebuildMe();
    }

    /** Toque no mundo durante o preparo: lança o inicial ali, só na sua metade (atrás da linha). */
    public boolean tapWorld(float wx, float wy) {
        if (duel == null || duel.phase != ArenaDuel.Phase.PREP || duel.meReady) return false;
        if (wx < ArenaLayout.left() || wx > ArenaLayout.right() || wy < ArenaLayout.top() || wy > ArenaLayout.bottom()) return false;
        if (wy < ArenaLayout.lineY() + 8) { if (gv != null) gv.ui.showMessage("Lance na SUA metade da arena (abaixo da linha)."); return true; }
        throwAt(wx, wy, true);
        return true;
    }

    /** Guarda o ponto do lançamento (limitado à sua metade) e solta uma bola do treinador até lá. */
    public void throwAt(float x, float y, boolean animate) {
        float minY = (float) ArenaLayout.lineY() + PK * 0.62f + 8, maxY = (float) ArenaLayout.bottom() - 6;
        placeY = Math.max(minY, Math.min(maxY, y));
        placeX = Math.max((float) ArenaLayout.left() + PK * 0.5f, Math.min((float) ArenaLayout.right() - PK * 0.5f, x));
        thrown = true;
        if (animate) launchBall(placeX, placeY - PK * 0.3f, false);
    }

    private void launchBall(float toX, float toY, boolean foe) {
        Ball b = new Ball();
        if (foe) { b.x0 = (float) ArenaLayout.foeTrainerX(); b.y0 = (float) ArenaLayout.foeTrainerY() - 40; }
        else { b.x0 = gv != null ? (float) gv.player.getPositionX() : toX; b.y0 = gv != null ? (float) gv.player.getPositionY() - 20 : toY; }
        b.x1 = toX; b.y1 = toY; b.t0 = System.currentTimeMillis(); b.foe = foe;
        balls.add(b);
    }

    /** Pokémon trocado na luta: o treinador lança outra bola no lugar do Pokémon. */
    public void onSwap() { launchBall(meX, meY - PK * 0.3f, false); }

    /** PRONTO: gasta a tentativa e trava o time; a luta começa quando o oponente também estiver pronto. Retorna false se não houver tentativas. */
    public boolean ready(GameView gv) {
        if (duel == null || duel.phase != ArenaDuel.Phase.PREP || duel.meReady || picks.isEmpty()) return false;
        RewardManager.Store s = PointsManager.store();
        if (!ArenaLadder.useTry(s)) return false;
        spent = true;
        if (!thrown) throwAt(placeX, placeY, false);          // tempo acabou sem lançar: lança no ponto padrão
        duel.ready();
        lastT = System.currentTimeMillis();
        return true;
    }

    /** Sair da sala: depois do PRONTO conta como derrota. Volta para a cidade e para a lista de lutadores. */
    public void leave(GameView gv) {
        if (duel != null && (duel.phase == ArenaDuel.Phase.FIGHT || duel.phase == ArenaDuel.Phase.COUNTDOWN || (duel.phase == ArenaDuel.Phase.PREP && duel.meReady))) {
            duel.forfeit(); applyResult(gv);
        }
        if (gv.world != null) {
            gv.world.roomOverlay = null;
            if (ArenaLayout.MAP.equals(gv.world.getMapName())) {
                gv.world.loadMap(returnMap, gv.player, gv.companion, "CENTER");
                gv.player.setPosition(returnX, returnY);
                gv.companion.setPosition(returnX - 50, returnY);
            }
        }
        duel = null;
        ArenaUI ui = (ArenaUI) gv.ui.getHandler(UIManager.UIState.ARENA);
        if (ui != null) ui.invalidate();
        gv.ui.state = UIManager.UIState.ARENA;
    }

    private void applyResult(GameView gv) {
        if (resultApplied || duel == null || duel.phase != ArenaDuel.Phase.RESULT) return;
        resultApplied = true;
        result = ArenaLadder.report(PointsManager.store(), fighter, duel.won);
        if (result.gold > 0) gv.player.addGold(result.gold);
        gv.activity.saveCurrentGame();
        com.example.pokeworld.managers.ArenaNet.refresh(PointsManager.store(), ArenaLadder.bestPower(roster), true);
    }

    // ------------------------------------------------------------------ atualização (a cada quadro, vinda do GameView)
    public void update(GameView gv) {
        this.gv = gv;
        if (duel == null) return;
        long now = System.currentTimeMillis();
        long dt = Math.min(100, Math.max(0, now - lastT));
        lastT = now;
        duel.update(dt);
        // tempo de preparo acabou: fica PRONTO sozinho com o time que estiver montado
        if (duel.phase == ArenaDuel.Phase.PREP && !duel.meReady && duel.prepLeft <= 0) {
            if (!ready(gv)) { gv.ui.showMessage("⛔ Acabaram as tentativas de hoje."); leave(gv); return; }
        }
        if (duel.phase == ArenaDuel.Phase.FIGHT && fightT0 == 0) fightT0 = now;
        if (duel.phase == ArenaDuel.Phase.RESULT) applyResult(gv);
        computePositions(now);
        if (duel.phase != lastPhase) {
            if (duel.phase == ArenaDuel.Phase.COUNTDOWN) launchBall(foeX, foeY - PK * 0.3f, true);   // o oponente lança o dele
            lastPhase = duel.phase;
        }
        drainPops(now);
    }

    private void drainPops(long now) {
        int slot = 0;
        for (ArenaDuel.Pop p : duel.pops) {
            Pop q = new Pop();
            q.text = p.text; q.color = p.color; q.onFoe = p.onFoe; q.t0 = now; q.slot = (slot++) % 3;
            q.x = p.onFoe ? foeX : meX; q.y = (p.onFoe ? foeY : meY) - PK * 0.9f;
            pops.add(q);
            if (p.text.startsWith("-") || p.text.startsWith("⭐") || p.text.startsWith("💥")) { if (p.onFoe) meLunge = now; else foeLunge = now; }
        }
        duel.pops.clear();
    }

    /** Onde cada Pokémon fica: no preparo, onde foi lançado; na luta, avançam até se encontrar perto da linha. */
    private void computePositions(long now) {
        float L = (float) ArenaLayout.left(), W = (float) ArenaLayout.width(), T = (float) ArenaLayout.top(), line = (float) ArenaLayout.lineY();
        float cx = (float) ArenaLayout.centerX();
        float foeStartX = L + W * foePlaceX, foeStartY = T + (line - T) * 0.80f;
        float fx = cx + W * 0.06f, fy = line - PK * 0.05f;
        float mx = cx - W * 0.06f, my = line + PK * 0.62f;
        float p = 0;
        if (duel.phase == ArenaDuel.Phase.FIGHT || duel.phase == ArenaDuel.Phase.RESULT)
            p = fightT0 == 0 ? 0 : Math.min(1f, (now - fightT0) / 1200f);
        p = p * p * (3 - 2 * p);
        foeX = foeStartX + (fx - foeStartX) * p; foeY = foeStartY + (fy - foeStartY) * p;
        meX = placeX + (mx - placeX) * p; meY = placeY + (my - placeY) * p;
    }

    // ------------------------------------------------------------------ câmera e mundo (RoomOverlay)
    @Override
    public double[] focus(Player p) {
        if (duel == null) return null;
        double cy = ArenaLayout.arenaCenterY(), tx = ArenaLayout.centerX(), ty = cy;
        if (duel.phase == ArenaDuel.Phase.PREP) ty = Math.max(cy, Math.min(cy + 260, p.getPositionY()));   // no preparo acompanha você
        if (!camInit) { camX = tx; camY = ty; camInit = true; }
        else { camX += (tx - camX) * 0.12; camY += (ty - camY) * 0.12; }
        return new double[]{camX, camY, 1.0};
    }

    private boolean ballInFlight(boolean foe, long now) {
        for (Ball b : balls) if (b.foe == foe && now - b.t0 < BALL_MS) return true;
        return false;
    }

    private void shadow(Canvas canvas, float cx, float feetY) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawOval(new RectF(cx - PK * 0.38f, feetY - PK * 0.07f, cx + PK * 0.38f, feetY + PK * 0.07f), paint);
    }

    @Override
    public void drawWorld(Canvas canvas, long now) {
        if (duel == null) return;
        float L = (float) ArenaLayout.left(), R = (float) ArenaLayout.right(), B = (float) ArenaLayout.bottom(), line = (float) ArenaLayout.lineY();
        int frame = (int) ((now / 320) % 3);
        paint.setStyle(Paint.Style.FILL);
        // zona de lançamento: a sua metade pulsa enquanto você ainda pode lançar
        if (duel.phase == ArenaDuel.Phase.PREP && !duel.meReady) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(now / 280.0);
            paint.setColor(Color.argb((int) (30 + 40 * pulse), 255, 220, 90));
            canvas.drawRect(L, line + 32, R, B, paint);
            if (!thrown) text(canvas, "👆 Toque na sua metade para LANÇAR o Pokémon", (L + R) / 2f, B - 20, 22, Color.WHITE, Paint.Align.CENTER, true);
        }
        text(canvas, "SUA METADE", L + 12, line + 58, 18, Color.argb(220, 255, 255, 255), Paint.Align.LEFT, true);
        text(canvas, "METADE DO OPONENTE", L + 12, line - 40, 18, Color.argb(210, 255, 200, 200), Paint.Align.LEFT, true);

        // treinador do oponente (parado no retângulo de cima, de frente para a arena)
        ArenaUI.sprite(canvas, paint, "personagem", false, 0, 1, (float) ArenaLayout.foeTrainerX(), (float) ArenaLayout.foeTrainerY(), 100);
        text(canvas, fit(duel.foe.name, 380), (float) ArenaLayout.foeTrainerX(), (float) ArenaLayout.foeTrainerY() - 104, 20, Color.argb(235, 255, 200, 200), Paint.Align.CENTER, true);

        // oponente: escondido ("?") até todos ficarem prontos
        if (duel.phase == ArenaDuel.Phase.PREP) {
            int n = Math.max(1, Math.min(ArenaDuel.MAX_TEAM, duel.foe.team.size()));
            float T = (float) ArenaLayout.top();
            for (int i = 0; i < n; i++) {
                float cx = L + (R - L) * (0.30f + 0.2f * i), cy = T + (line - T) * (i == 1 ? 0.62f : 0.80f);
                paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(150, 0, 0, 0));
                canvas.drawOval(new RectF(cx - PK * 0.35f, cy - PK * 0.05f, cx + PK * 0.35f, cy + PK * 0.05f), paint);
                paint.setColor(Color.argb(220, 38, 38, 56));
                canvas.drawCircle(cx, cy - PK * 0.40f, PK * 0.34f, paint);
                text(canvas, "?", cx, cy - PK * 0.40f + PK * 0.16f, PK * 0.46f, Color.argb(235, 255, 255, 255), Paint.Align.CENTER, true);
            }
        } else {
            ArenaDuel.Unit u = duel.foe.cur();
            if (u != null && u.alive() && !ballInFlight(true, now)) {
                float lunge = Math.max(0, 1 - (now - foeLunge) / 220f) * 30;
                float shake = duel.foe.hurtFlash > 0 ? (float) Math.sin(now / 22.0) * 7 : 0;
                shadow(canvas, foeX + shake, foeY + lunge);
                ArenaUI.sprite(canvas, paint, u.species, u.shiny, 0, frame, foeX + shake, foeY + lunge, PK);
            }
        }
        // o seu Pokémon (aparece quando a bola chega)
        ArenaDuel.Unit mu = duel.me.cur();
        boolean visible = duel.phase != ArenaDuel.Phase.PREP || thrown;
        if (mu != null && mu.alive() && visible && !ballInFlight(false, now)) {
            float lunge = Math.max(0, 1 - (now - meLunge) / 220f) * 30;
            float shake = duel.me.hurtFlash > 0 ? (float) Math.sin(now / 22.0) * 7 : 0;
            shadow(canvas, meX + shake, meY - lunge);
            ArenaUI.sprite(canvas, paint, mu.species, mu.shiny, 2, frame, meX + shake, meY - lunge, PK);
        }
        drawBalls(canvas, now);
        // textos flutuantes (dano, crítico, efetividade)
        for (int i = pops.size() - 1; i >= 0; i--) {
            Pop p = pops.get(i);
            float a = (now - p.t0) / 1000f;
            if (a > 1f) { pops.remove(i); continue; }
            text(canvas, p.text, p.x + (p.slot % 2 == 0 ? -30 : 30), p.y - a * 46 - p.slot * 22, 30, p.color, Paint.Align.CENTER, true);
        }
    }

    private void drawBalls(Canvas canvas, long now) {
        for (int i = balls.size() - 1; i >= 0; i--) {
            Ball b = balls.get(i);
            float t = (now - b.t0) / (float) BALL_MS;
            if (t >= 1f) {
                float a = Math.min(1f, (t - 1f) * 4f);          // clarão onde a bola abriu
                if (a >= 1f) { balls.remove(i); continue; }
                paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb((int) (200 * (1 - a)), 255, 255, 255));
                canvas.drawCircle(b.x1, b.y1, 20 + 50 * a, paint);
                continue;
            }
            float x = b.x0 + (b.x1 - b.x0) * t, y = b.y0 + (b.y1 - b.y0) * t - (float) Math.sin(Math.PI * t) * 70;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(230, 50, 50)); canvas.drawCircle(x, y, 12, paint);
            paint.setColor(Color.WHITE); canvas.drawRect(x - 12, y - 1.5f, x + 12, y + 1.5f, paint);
            paint.setColor(Color.rgb(250, 250, 250)); canvas.drawCircle(x, y, 4, paint);
        }
    }

    // ------------------------------------------------------------------ HUD (tela)
    @Override
    public void draw(Canvas canvas, GameView gv) {
        this.gv = gv;
        if (duel == null) { gv.ui.state = UIManager.UIState.ARENA; return; }
        float w = canvas.getWidth(), h = canvas.getHeight();
        long now = System.currentTimeMillis();
        drawTopBar(canvas, w, now);
        drawFoeHud(canvas, w, h);
        drawBottom(canvas, w, h, now);
        drawOverlay(canvas, w, h, now);
    }

    private void drawTopBar(Canvas canvas, float w, long now) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(235, 18, 26, 42));
        canvas.drawRect(0, 0, w, 64, paint);
        paint.setColor(C_ACCENT);
        canvas.drawRect(0, 62, w, 66, paint);
        text(canvas, "🏟️ ARENA  •  " + (fighter != null ? fighter.icon + " " + fighter.name : ""), 24, 42, 26, Color.WHITE, Paint.Align.LEFT, true);
        if (duel.phase == ArenaDuel.Phase.PREP) {
            long left = (duel.prepLeft + 999) / 1000;
            text(canvas, (duel.meReady ? "✅ PRONTO — " : "⏱ PREPARO: ") + left + "s", w / 2f, 42, 28, duel.meReady ? C_GOOD : left <= 5 ? C_BAD : Color.WHITE, Paint.Align.CENTER, true);
        } else if (duel.phase == ArenaDuel.Phase.FIGHT || duel.phase == ArenaDuel.Phase.RESULT) {
            long left = duel.timeLeft() / 1000;
            text(canvas, "⏱ " + (left / 60) + ":" + (left % 60 < 10 ? "0" : "") + (left % 60), w / 2f, 42, 28, left < 15 ? C_BAD : Color.WHITE, Paint.Align.CENTER, true);
        }
        exitBtn.set(w - 180, 10, w - 24, 56);
        panel(canvas, exitBtn, Color.rgb(120, 40, 44), Color.WHITE, 2);
        boolean free = duel.phase == ArenaDuel.Phase.PREP && !duel.meReady || duel.phase == ArenaDuel.Phase.RESULT;
        text(canvas, free ? "◀ SAIR" : "🏳 DESISTIR", exitBtn.centerX(), exitBtn.centerY() + 8, 20, Color.WHITE, Paint.Align.CENTER, true);
    }

    private void infoBox(Canvas canvas, RectF r, ArenaDuel.Unit u, String owner, int accent) {
        panel(canvas, r, Color.argb(225, 16, 24, 40), accent, 3);
        if (u == null) { text(canvas, "—", r.centerX(), r.centerY() + 8, 24, C_MUTED, Paint.Align.CENTER, false); return; }
        text(canvas, fit(u.species + (u.shiny ? " ✨" : ""), r.width() - 100), r.left + 14, r.top + 30, 22, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, "Nv. " + u.level, r.right - 14, r.top + 30, 20, Color.rgb(255, 214, 90), Paint.Align.RIGHT, true);
        float frac = u.hpFrac();
        bar(canvas, new RectF(r.left + 14, r.top + 42, r.right - 14, r.top + 60), frac, frac > 0.5f ? C_GOOD : frac > 0.25f ? Color.rgb(255, 200, 60) : C_BAD);
        text(canvas, Math.max(0, u.hp) + " / " + u.maxHp, r.left + 14, r.top + 80, 15, C_MUTED, Paint.Align.LEFT, false);
        text(canvas, fit(owner, r.width() * 0.5f), r.right - 14, r.top + 80, 15, C_MUTED, Paint.Align.RIGHT, false);
    }

    /** Vida do oponente e do seu Pokémon (à esquerda, em cima) e o time do oponente (à direita, em cima). */
    private void drawFoeHud(Canvas canvas, float w, float h) {
        if (duel.phase == ArenaDuel.Phase.PREP) {
            text(canvas, duel.foeReady ? "✅ " + duel.foe.name + " está PRONTO" : "⏳ " + duel.foe.name + " está escolhendo…",
                    w / 2f, 100, 20, duel.foeReady ? C_GOOD : Color.WHITE, Paint.Align.CENTER, true);
            return;
        }
        float margin = w / 2f - (float) ArenaLayout.width() / 2f;      // espaço ao lado da arena (zoom 1)
        float iw = Math.max(170, Math.min(360, margin - 30));
        infoBox(canvas, new RectF(20, 78, 20 + iw, 78 + 92), duel.foe.cur(), duel.foe.name, Color.rgb(230, 90, 90));
        infoBox(canvas, new RectF(20, 178, 20 + iw, 178 + 92), duel.me.cur(), duel.me.name, C_GOOD);
        float x = w - 30;
        for (int i = duel.foe.team.size() - 1; i >= 0; i--) {
            ArenaDuel.Unit t = duel.foe.team.get(i);
            RectF r = new RectF(x - 62, 78, x, 130);
            panel(canvas, r, i == duel.foe.active ? C_PANEL_HI : C_PANEL, t.alive() ? (i == duel.foe.active ? Color.RED : C_LINE) : Color.DKGRAY, 2);
            if (t.alive()) ArenaUI.sprite(canvas, paint, t.species, t.shiny, 0, 0, r.centerX(), r.bottom - 8, 40);
            else text(canvas, "💀", r.centerX(), r.centerY() + 10, 26, Color.WHITE, Paint.Align.CENTER, false);
            bar(canvas, new RectF(r.left + 6, r.bottom - 8, r.right - 6, r.bottom - 3), t.hpFrac(), C_GOOD);
            x -= 68;
        }
    }

    private void drawBottom(Canvas canvas, float w, float h, long now) {
        float top = h - 176, bottom = h - 14, x0 = w * 0.32f;      // o joystick fica à esquerda: os cartões ocupam a direita
        ArenaDuel.Phase ph = duel.phase;
        for (int i = 0; i < 6; i++) cards[i].setEmpty();
        readyBtn.setEmpty();
        if (ph == ArenaDuel.Phase.RESULT || ph == ArenaDuel.Phase.COUNTDOWN) return;
        if (ph == ArenaDuel.Phase.PREP) {
            String hint = duel.meReady ? "Time travado! Esperando o oponente ficar pronto…"
                    : thrown ? "Pode lançar de novo em outro ponto, ou aperte PRONTO. O oponente não vê." : "1) Escolha até 3 Pokémon  2) Ande e toque na sua metade para LANÇAR o inicial  3) PRONTO";
            text(canvas, hint, x0, top - 8, 17, duel.meReady ? C_GOOD : Color.WHITE, Paint.Align.LEFT, true);
            readyBtn.set(w - 210, top + 6, w - 20, bottom);
            actionButton(canvas, readyBtn, duel.meReady ? "⏳ AGUARDANDO" : "✅ PRONTO!", !picks.isEmpty() && !duel.meReady && thrown, C_GOOD);
            int n = Math.min(6, roster.size());
            float areaW = readyBtn.left - 14 - x0, g = 8;
            float cw = Math.min(170, (areaW - (n - 1) * g) / Math.max(1, n));
            for (int i = 0; i < n; i++) {
                RectF r = cards[i];
                r.set(x0 + i * (cw + g), top + 6, x0 + i * (cw + g) + cw, bottom);
                ArenaDuel.Unit u = roster.get(i);
                int at = picks.indexOf(i);
                panel(canvas, r, at >= 0 ? C_PANEL_HI : C_PANEL, at == 0 ? C_ACCENT : at > 0 ? C_GOOD : C_LINE, at >= 0 ? 4 : 2);
                float sz = Math.min(r.height() - 62, cw - 20);
                ArenaUI.sprite(canvas, paint, u.species, u.shiny, 0, (int) ((now / 320) % 3), r.centerX(), r.top + 8 + sz, sz);
                text(canvas, fit(u.species, cw - 10), r.centerX(), r.bottom - 38, 15, Color.WHITE, Paint.Align.CENTER, true);
                text(canvas, "Nv." + u.level + "  ⚡" + u.power(), r.centerX(), r.bottom - 20, 14, Color.rgb(255, 214, 90), Paint.Align.CENTER, false);
                text(canvas, typeLabel(u), r.centerX(), r.bottom - 5, 12, C_MUTED, Paint.Align.CENTER, false);
                if (at >= 0) {
                    paint.setColor(at == 0 ? C_ACCENT : C_GOOD); paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(r.left + 18, r.top + 18, 14, paint);
                    text(canvas, at == 0 ? "★" : String.valueOf(at + 1), r.left + 18, r.top + 25, 20, Color.BLACK, Paint.Align.CENTER, true);
                }
            }
        } else {   // FIGHT: seu time para trocar
            int n = duel.me.team.size();
            float g = 10, cw = Math.min(260, (w - x0 - 20 - (n - 1) * g) / Math.max(1, n));
            boolean pick = duel.me.active == -1, cd = duel.me.swapCd > 0;
            String hint = pick ? "⚠ ESCOLHA O PRÓXIMO POKÉMON!" : cd ? "Troca disponível em " + String.format("%.1f", duel.me.swapCd / 1000f) + "s" : "🔄 Toque em um Pokémon para TROCAR";
            text(canvas, hint, x0, top - 8, 20, pick ? C_BAD : cd ? C_MUTED : C_GOOD, Paint.Align.LEFT, true);
            if (cd && !pick) bar(canvas, new RectF(x0, top - 2, x0 + 320, top + 4), 1f - duel.me.swapCd / (float) ArenaDuel.SWAP_CD_MS, C_ACCENT);
            for (int i = 0; i < n; i++) {
                RectF r = cards[i];
                r.set(x0 + i * (cw + g), top + 8, x0 + i * (cw + g) + cw, bottom);
                ArenaDuel.Unit u = duel.me.team.get(i);
                boolean act = i == duel.me.active;
                panel(canvas, r, act ? C_PANEL_HI : C_PANEL, !u.alive() ? Color.DKGRAY : act ? C_ACCENT : C_LINE, act ? 4 : 2);
                float sz = r.height() - 20;
                if (u.alive()) ArenaUI.sprite(canvas, paint, u.species, u.shiny, 0, (int) ((now / 320) % 3), r.left + sz * 0.5f + 8, r.bottom - 8, sz * 0.8f);
                else text(canvas, "💀", r.left + 50, r.centerY() + 14, 44, Color.WHITE, Paint.Align.CENTER, false);
                float tx = r.left + sz * 0.8f + 18;
                text(canvas, fit(u.species, r.right - tx - 8), tx, r.top + 30, 18, Color.WHITE, Paint.Align.LEFT, true);
                text(canvas, "Nv." + u.level, tx, r.top + 52, 15, Color.rgb(255, 214, 90), Paint.Align.LEFT, false);
                bar(canvas, new RectF(tx, r.top + 62, r.right - 12, r.top + 76), u.hpFrac(), u.hpFrac() > 0.5f ? C_GOOD : u.hpFrac() > 0.25f ? Color.rgb(255, 200, 60) : C_BAD);
                text(canvas, Math.max(0, u.hp) + "/" + u.maxHp, tx, r.top + 96, 13, C_MUTED, Paint.Align.LEFT, false);
                if (u.alive() && !act && !pick && cd) {
                    float f = duel.me.swapCd / (float) ArenaDuel.SWAP_CD_MS;
                    paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(150, 0, 0, 0));
                    canvas.drawRoundRect(new RectF(r.left, r.top, r.right, r.top + r.height() * f), 14, 14, paint);
                    text(canvas, String.format("%.1f", duel.me.swapCd / 1000f) + "s", r.centerX(), r.centerY() + 8, 24, Color.WHITE, Paint.Align.CENTER, true);
                }
                if (act) text(canvas, "EM CAMPO", r.right - 10, r.bottom - 8, 13, C_ACCENT, Paint.Align.RIGHT, true);
            }
        }
    }

    private String typeLabel(ArenaDuel.Unit u) {
        String a = u.t1 == null ? "" : u.t1.name();
        return u.t2 != null ? a + "/" + u.t2.name() : a;
    }

    private void drawOverlay(Canvas canvas, float w, float h, long now) {
        if (duel.phase == ArenaDuel.Phase.COUNTDOWN) {
            int n = (int) Math.ceil(duel.countdown / 1000.0);
            text(canvas, n > 0 ? String.valueOf(n) : "LUTAR!", w / 2f, h * 0.42f, 150, C_ACCENT, Paint.Align.CENTER, true);
            text(canvas, "Os Pokémon lutam sozinhos. Você troca de Pokémon a cada 5 s.", w / 2f, h * 0.42f + 50, 24, Color.WHITE, Paint.Align.CENTER, true);
            continueBtn.setEmpty();
        } else if (duel.phase == ArenaDuel.Phase.RESULT && result != null) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(170, 0, 0, 0));
            canvas.drawRect(0, 0, w, h, paint);
            RectF r = new RectF(w / 2f - 300, h / 2f - 190, w / 2f + 300, h / 2f + 190);
            panel(canvas, r, Color.argb(250, 20, 28, 46), result.won ? C_GOOD : C_BAD, 4);
            text(canvas, result.won ? "🏆 VITÓRIA!" : "💀 DERROTA", r.centerX(), r.top + 66, 52, result.won ? C_GOOD : C_BAD, Paint.Align.CENTER, true);
            if (duel.timeUp) text(canvas, "Tempo esgotado — decidido pela vida restante", r.centerX(), r.top + 96, 17, C_MUTED, Paint.Align.CENTER, false);
            text(canvas, (result.rpDelta >= 0 ? "+" : "") + result.rpDelta + " PR   →   " + result.newRp + " PR", r.centerX(), r.top + 140, 30, Color.WHITE, Paint.Align.CENTER, true);
            text(canvas, ArenaLadder.TIER_ICON[result.newTier] + " Liga " + ArenaLadder.TIER_NAME[result.newTier], r.centerX(), r.top + 176, 24, ArenaLadder.TIER_COLOR[result.newTier], Paint.Align.CENTER, true);
            if (result.newTier > result.oldTier) text(canvas, "⬆ VOCÊ SUBIU DE LIGA!", r.centerX(), r.top + 208, 24, C_ACCENT, Paint.Align.CENTER, true);
            if (result.gold > 0) text(canvas, "🪙 +" + result.gold + " de ouro", r.centerX(), r.top + 244, 26, Color.rgb(255, 214, 90), Paint.Align.CENTER, true);
            continueBtn.set(r.centerX() - 150, r.bottom - 84, r.centerX() + 150, r.bottom - 20);
            actionButton(canvas, continueBtn, "CONTINUAR", true, C_ACCENT);
        } else continueBtn.setEmpty();
    }
}
