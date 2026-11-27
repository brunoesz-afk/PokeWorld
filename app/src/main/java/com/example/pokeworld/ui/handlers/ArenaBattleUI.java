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
 * Duelo da Arena, numa SALA própria do jogo (mapa "DUEL_ARENA", em paisagem, com lava e pontes nas laterais):
 * o oponente (escondido) em cima, a arena larga com a LINHA no meio e, embaixo, a sua faixa de treinador, separada por
 * uma grade. Você anda para os lados e dá um passo à frente (joystick) e o seu Pokémon é lançado na sua metade da arena.
 * Preparo (com tempo): painel "SELECIONE SEU POKÉMON INICIAL" com os cartões do time (ESCOLHER), dá para montar o time
 * (até 3) e relançar tocando na sua metade; aperte PRONTO. Quando os dois estiverem prontos, o oponente é revelado e os
 * Pokémon lutam sozinhos; na luta você troca de Pokémon a cada 5 segundos pelo botão Pokémon.
 * Esta classe é o HUD (telas) e também a camada do mundo (RoomOverlay) que desenha a arena e manda na câmera.
 */
public class ArenaBattleUI extends BaseUI implements RoomOverlay {
    public final RectF exitBtn = new RectF(), readyBtn = new RectF(), continueBtn = new RectF();
    public final RectF pokeBtn = new RectF(), fleeBtn = new RectF(), logBox = new RectF();
    public final RectF modalRect = new RectF(), modalClose = new RectF(), modalTeamBtn = new RectF(), modalOk = new RectF();
    public final RectF[] cards = new RectF[6];          // cartões do painel central (time, roster ou troca)
    public final RectF[] okBtns = new RectF[6];         // botão ENTRAR/TIRAR (ou ESCOLHER) de cada cartão
    public final RectF[] leftBtns = new RectF[6], rightBtns = new RectF[6];   // ◀ ▶: muda a ordem de combate
    public enum Modal { NONE, STARTER, TEAM, SWAP }
    public Modal modal = Modal.NONE;
    private int thrownIdx = -1;                       // índice do roster do Pokémon lançado por último
    private final List<String> log = new ArrayList<>();
    private float screenW = 1280, screenH = 720;

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
    private boolean foeReadyLogged = false;
    private String returnMap = "CITY";
    private double returnX, returnY;
    private double camX, camY; private boolean camInit = false;
    public static final float PK = 128;             // tamanho dos Pokémon no mundo
    private static final long BALL_MS = 520;

    private static final class Pop { String text; int color; boolean onFoe; long t0; int slot; float x, y; }
    private final List<Pop> pops = new ArrayList<>();
    private static final class Ball { float x0, y0, x1, y1; long t0; boolean foe; }
    private final List<Ball> balls = new ArrayList<>();
    private long meLunge = 0, foeLunge = 0;

    public ArenaBattleUI() { for (int i = 0; i < cards.length; i++) { cards[i] = new RectF(); okBtns[i] = new RectF(); leftBtns[i] = new RectF(); rightBtns[i] = new RectF(); } }

    private void say(String m) { log.add(m); while (log.size() > 30) log.remove(0); }

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
        foeReadyLogged = false; thrown = false; thrownIdx = -1; foePlaceX = 0.3f + new Random().nextFloat() * 0.4f; fightT0 = 0; lastPhase = null; camInit = false;
        placeX = (float) ArenaLayout.centerX(); placeY = (float) (ArenaLayout.lineY() + (ArenaLayout.bottom() - ArenaLayout.lineY()) * 0.6);
        log.clear(); modal = Modal.STARTER;
        say(f.name + " desafia você!");
        say("Selecione seu Pokémon para iniciar!");
        gv.ui.state = UIManager.UIState.ARENA_BATTLE;
    }

    public void rebuildMe() {
        duel.me.team.clear();
        for (int p : picks) duel.me.team.add(roster.get(p));
        duel.me.active = duel.me.team.isEmpty() ? -1 : 0;
    }

    /** Painel do time: entra/sai do time (até 3, sempre ao menos 1). */
    public void toggleMember(int rosterIdx) {
        if (duel == null || duel.meReady) return;       // PRONTO trava o time
        int at = picks.indexOf(rosterIdx);
        if (at >= 0) { if (picks.size() > 1) picks.remove(at); }
        else if (picks.size() < ArenaDuel.MAX_TEAM) picks.add(rosterIdx);
        rebuildMe();
    }

    /** ◀ ▶ no cartão: troca a posição do Pokémon na ordem de combate (o 1º é o inicial). */
    public void movePick(int rosterIdx, int delta) {
        if (duel == null || duel.meReady) return;
        int at = picks.indexOf(rosterIdx), to = at + delta;
        if (at < 0 || to < 0 || to >= picks.size()) return;
        java.util.Collections.swap(picks, at, to);
        rebuildMe();
    }

    /** OK do painel do time: o 1º da ordem é o inicial; só relança se ele mudou (ou se ainda não foi lançado). */
    public void confirmTeam() {
        if (duel == null) return;
        if (picks.isEmpty()) return;
        if (thrown && thrownIdx == picks.get(0)) { modal = Modal.NONE; return; }
        chooseStarter(0);
    }

    /** ESCOLHER no painel inicial: o cartão vira o Pokémon inicial e é lançado (na frente do treinador, se ainda não foi). */
    public void chooseStarter(int pickIdx) {
        if (duel == null || duel.meReady || pickIdx < 0 || pickIdx >= picks.size()) return;
        picks.add(0, picks.remove(pickIdx));
        rebuildMe();
        thrownIdx = picks.get(0);
        modal = Modal.NONE;
        say("Você escolheu " + duel.me.cur().species + "!");
        if (!thrown) throwInFront(true); else launchBall(placeX, placeY - PK * 0.3f, false);
    }

    /** Lança na frente do treinador: mesmo x dele; quanto mais ele avançou na faixa, mais perto da linha. */
    public void throwInFront(boolean animate) {
        double px = gv != null ? gv.player.getPositionX() : ArenaLayout.centerX();
        double py = gv != null ? gv.player.getPositionY() : ArenaLayout.spawnY();
        float minY = (float) ArenaLayout.lineY() + PK * 0.86f + 8, maxY = (float) ArenaLayout.bottom() - 6;
        float y = maxY - (maxY - minY) * (float) ArenaLayout.forward(py);
        throwAt((float) px, y, animate);
    }

    /** Painel de troca (luta): ESCOLHER troca de Pokémon (recarga de 5 s) e o treinador lança outra bola. */
    public void swapTo(int teamIdx) {
        if (duel == null || duel.phase != ArenaDuel.Phase.FIGHT) return;
        if (teamIdx < 0 || teamIdx >= duel.me.team.size()) return;
        if (teamIdx == duel.me.active) { say("Esse Pokémon já está em campo."); return; }
        if (!duel.me.team.get(teamIdx).alive()) { say("Esse Pokémon desmaiou."); return; }
        if (!duel.swap(teamIdx)) { say("Troca disponível em " + String.format("%.1f", duel.me.swapCd / 1000f) + "s."); return; }
        onSwap();
        modal = Modal.NONE;
        say("Vai, " + duel.me.cur().species + "!");
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
        float minY = (float) ArenaLayout.lineY() + PK * 0.86f + 8, maxY = (float) ArenaLayout.bottom() - 6;
        placeY = Math.max(minY, Math.min(maxY, y));
        placeX = Math.max((float) ArenaLayout.left() + PK * 0.5f, Math.min((float) ArenaLayout.right() - PK * 0.5f, x));
        thrown = true;
        thrownIdx = picks.isEmpty() ? -1 : picks.get(0);
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
        modal = Modal.NONE;
        say("Você está PRONTO! Esperando " + duel.foe.name + "…");
        if (!thrown) throwInFront(false);                     // tempo acabou sem lançar: lança na frente do treinador
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
        if (duel.phase == ArenaDuel.Phase.FIGHT && fightT0 == 0) { fightT0 = now; say("A luta começou! Pokémon, ataquem!"); }
        if (duel.phase == ArenaDuel.Phase.FIGHT && duel.me.active == -1 && modal != Modal.SWAP) { modal = Modal.SWAP; say("Seu Pokémon caiu! Escolha o próximo."); }
        if (duel.phase == ArenaDuel.Phase.COUNTDOWN || duel.phase == ArenaDuel.Phase.RESULT) modal = Modal.NONE;
        if (duel.phase == ArenaDuel.Phase.PREP && duel.foeReady && !foeReadyLogged) { foeReadyLogged = true; say(duel.foe.name + " está PRONTO!"); }
        if (duel.phase == ArenaDuel.Phase.RESULT) applyResult(gv);
        computePositions(now);
        if (duel.phase != lastPhase) {
            if (duel.phase == ArenaDuel.Phase.COUNTDOWN) { launchBall(foeX, foeY - PK * 0.3f, true); say(duel.foe.name + " lançou " + (duel.foe.cur() != null ? duel.foe.cur().species : "um Pokémon") + "!"); }   // o oponente lança o dele
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
            if (p.text.startsWith("💀")) say((p.onFoe ? duel.foe.name : duel.me.name) + ": " + p.text.substring(2).trim() + " desmaiou!");
            if (p.text.startsWith("🔄") && p.onFoe) say(duel.foe.name + " trocou para " + p.text.substring(2).trim() + ".");
            if (p.text.startsWith("-") || p.text.startsWith("⭐") || p.text.startsWith("💥")) { if (p.onFoe) meLunge = now; else foeLunge = now; }
        }
        duel.pops.clear();
    }

    /** Onde cada Pokémon fica: no preparo, onde foi lançado; na luta, avançam até se encontrar perto da linha. */
    private void computePositions(long now) {
        float L = (float) ArenaLayout.left(), W = (float) ArenaLayout.width(), T = (float) ArenaLayout.top(), line = (float) ArenaLayout.lineY();
        float cx = (float) ArenaLayout.centerX();
        float foeStartX = L + W * foePlaceX, foeStartY = T + (line - T) * 0.80f;
        // um espaço entre os dois: o oponente fica mais para cima/lado e o seu mais para baixo, sem um pisar no outro
        float fx = cx + W * 0.11f, fy = line - PK * 0.40f;
        float mx = cx - W * 0.11f, my = line + PK * 0.86f;
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
        // a sala inteira cabe na tela entre a barra de cima e a de baixo
        double zoom = Math.min(screenW / ArenaLayout.mapWidth(), (screenH - 170) / ArenaLayout.mapHeight());
        zoom = Math.max(0.35, Math.min(1.8, zoom));
        return new double[]{ArenaLayout.mapWidth() / 2.0, ArenaLayout.mapHeight() / 2.0 + 14 / zoom, zoom};
    }

    private boolean ballInFlight(boolean foe, long now) {
        for (Ball b : balls) if (b.foe == foe && now - b.t0 < BALL_MS) return true;
        return false;
    }

    private void ring(Canvas canvas, float cx, float cy, float rx, float ry, float a, int r, int g, int b) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb((int) (60 * a), r, g, b));
        canvas.drawOval(new RectF(cx - rx * 1.25f, cy - ry * 1.25f, cx + rx * 1.25f, cy + ry * 1.25f), paint);
        paint.setColor(Color.argb((int) (90 * a), r, g, b));
        canvas.drawOval(new RectF(cx - rx, cy - ry, cx + rx, cy + ry), paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4);
        paint.setColor(Color.argb((int) (230 * a), 255, 230, 150));
        canvas.drawOval(new RectF(cx - rx, cy - ry, cx + rx, cy + ry), paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void shadow(Canvas canvas, float cx, float feetY, float size) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawOval(new RectF(cx - size * 0.38f, feetY - size * 0.07f, cx + size * 0.38f, feetY + size * 0.07f), paint);
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
            if (!thrown && modal == Modal.NONE) text(canvas, "👆 Toque na sua metade para LANÇAR o Pokémon", (L + R) / 2f, B - 20, 22, Color.WHITE, Paint.Align.CENTER, true);
        }
        text(canvas, "SUA METADE", L + 12, line + 58, 18, Color.argb(220, 255, 255, 255), Paint.Align.LEFT, true);
        text(canvas, "METADE DO OPONENTE", L + 12, line - 40, 18, Color.argb(210, 255, 200, 200), Paint.Align.LEFT, true);

        // anel brilhante sob o treinador (você) e sob o do oponente
        float glow = 0.6f + 0.4f * (float) Math.sin(now / 350.0);
        if (gv != null) ring(canvas, (float) gv.player.getPositionX(), (float) gv.player.getPositionY() + 4, 74, 26, glow, 255, 150, 50);
        ring(canvas, (float) ArenaLayout.foeTrainerX(), (float) ArenaLayout.foeTrainerY() + 4, 74, 26, glow, 255, 70, 60);
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
                float fs = PK * (float) com.example.pokeworld.entities.SpeciesSize.softScale(u.species);   // tamanho pela altura da espécie
                shadow(canvas, foeX + shake, foeY + lunge, fs);
                ArenaUI.sprite(canvas, paint, u.species, u.shiny, 0, frame, foeX + shake, foeY + lunge, fs);
            }
        }
        // o seu Pokémon (aparece quando a bola chega)
        ArenaDuel.Unit mu = duel.me.cur();
        boolean visible = duel.phase != ArenaDuel.Phase.PREP || thrown;
        if (mu != null && mu.alive() && visible && !ballInFlight(false, now)) {
            float lunge = Math.max(0, 1 - (now - meLunge) / 220f) * 30;
            float shake = duel.me.hurtFlash > 0 ? (float) Math.sin(now / 22.0) * 7 : 0;
            float ms = PK * (float) com.example.pokeworld.entities.SpeciesSize.softScale(mu.species);
            shadow(canvas, meX + shake, meY - lunge, ms);
            ArenaUI.sprite(canvas, paint, mu.species, mu.shiny, 2, frame, meX + shake, meY - lunge, ms);
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
        screenW = w; screenH = h;
        long now = System.currentTimeMillis();
        drawTopBar(canvas, w, now);
        drawBottomBar(canvas, w, h, now);
        drawModal(canvas, w, h, now);
        drawOverlay(canvas, w, h, now);
    }

    /** Uma plaquinha de lutador: nome, nível e barra de vida (verde = você, vermelha = oponente). */
    private void namePlate(Canvas canvas, RectF r, String name, int level, float frac, String hpText, boolean foe) {
        panel(canvas, r, Color.argb(235, 30, 22, 26), foe ? Color.rgb(230, 90, 70) : Color.rgb(110, 210, 120), 3);
        text(canvas, fit(name, r.width() - 110), r.left + 14, r.top + 28, 21, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, "Nv. " + level, r.right - 14, r.top + 28, 20, Color.rgb(255, 214, 90), Paint.Align.RIGHT, true);
        int col = foe ? Color.rgb(224, 70, 60) : (frac > 0.5f ? Color.rgb(90, 200, 90) : frac > 0.25f ? Color.rgb(255, 200, 60) : C_BAD);
        bar(canvas, new RectF(r.left + 14, r.top + 42, r.right - 14, r.top + 60), frac, col);
        text(canvas, hpText, r.right - 16, r.bottom - 6, 13, Color.argb(230, 255, 255, 255), Paint.Align.RIGHT, false);
    }

    private void drawTopBar(Canvas canvas, float w, long now) {
        float right = w - 196, top = 8, bot = 84, g = 10, left = 12;
        float total = right - left - 2 * g;
        float sw = total * 0.36f, cw = total - 2 * sw;
        RectF mine = new RectF(left, top, left + sw, bot);
        RectF mid = new RectF(mine.right + g, top, mine.right + g + cw, bot);
        RectF foe = new RectF(mid.right + g, top, right, bot);
        // você
        ArenaDuel.Unit mu = duel.me.cur();
        int lvl = gv != null ? gv.player.getLevel() : 1;
        float mf = mu != null ? mu.hpFrac() : teamFrac(duel.me.team);
        namePlate(canvas, mine, duel.me.name, lvl, mf, mu != null ? Math.max(0, mu.hp) + " / " + mu.maxHp : "", false);
        // título e relógio
        panel(canvas, mid, Color.argb(235, 40, 24, 22), Color.rgb(255, 150, 60), 3);
        text(canvas, "⚔ ARENA", mid.centerX(), mid.top + 30, 24, Color.rgb(255, 214, 90), Paint.Align.CENTER, true);
        if (duel.phase == ArenaDuel.Phase.PREP) {
            long left2 = (duel.prepLeft + 999) / 1000;
            text(canvas, (duel.meReady ? "✅ PRONTO  " : "⏱ ") + left2 + "s", mid.centerX(), mid.top + 60, 22, duel.meReady ? C_GOOD : left2 <= 5 ? C_BAD : Color.WHITE, Paint.Align.CENTER, true);
        } else if (duel.phase == ArenaDuel.Phase.COUNTDOWN) {
            text(canvas, "Prepare-se!", mid.centerX(), mid.top + 60, 20, Color.WHITE, Paint.Align.CENTER, true);
        } else {
            long l = duel.timeLeft() / 1000;
            text(canvas, "⏱ " + (l / 60) + ":" + (l % 60 < 10 ? "0" : "") + (l % 60), mid.centerX(), mid.top + 60, 22, l < 15 ? C_BAD : Color.WHITE, Paint.Align.CENTER, true);
        }
        // oponente (escondido até a luta)
        ArenaDuel.Unit fu = duel.foe.cur();
        boolean reveal = duel.phase != ArenaDuel.Phase.PREP && fu != null;
        namePlate(canvas, foe, duel.foe.name, fighter != null ? fighter.level : 1, reveal ? fu.hpFrac() : 1f, reveal ? Math.max(0, fu.hp) + " / " + fu.maxHp : "???", true);
        // sair (a região do botão voltar do jogo toca aqui também)
        exitBtn.set(w - 180, 20, w - 20, 90);
        boolean free = duel.phase == ArenaDuel.Phase.PREP && !duel.meReady || duel.phase == ArenaDuel.Phase.RESULT;
        panel(canvas, exitBtn, Color.rgb(120, 40, 44), Color.WHITE, 2);
        text(canvas, free ? "◀ SAIR" : "🏳 DESISTIR", exitBtn.centerX(), exitBtn.centerY() + 8, 20, Color.WHITE, Paint.Align.CENTER, true);
    }

    private float teamFrac(List<ArenaDuel.Unit> t) {
        float hp = 0, mx = 0;
        for (ArenaDuel.Unit u : t) { hp += Math.max(0, u.hp); mx += u.maxHp; }
        return mx <= 0 ? 1f : hp / mx;
    }

    private void drawBottomBar(Canvas canvas, float w, float h, long now) {
        float top = h - 96, bot = h - 12;
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(150, 12, 8, 10));
        canvas.drawRect(0, top - 8, w, h, paint);
        ArenaDuel.Phase ph = duel.phase;
        readyBtn.setEmpty(); pokeBtn.setEmpty(); fleeBtn.setEmpty();
        boolean busy = ph == ArenaDuel.Phase.RESULT;
        float bw = 136, g = 10, x = w - 14;
        fleeBtn.set(x - bw, top, x, bot); x -= bw + g;
        if (ph == ArenaDuel.Phase.PREP) { readyBtn.set(x - bw - 20, top, x, bot); x -= bw + 20 + g; }
        pokeBtn.set(x - bw, top, x, bot); x -= bw + g;
        float lx = w * 0.30f;
        logBox.set(lx, top, Math.max(lx + 120, x), bot);
        // log
        panel(canvas, logBox, Color.argb(235, 24, 18, 20), Color.rgb(120, 90, 70), 2);
        int n = Math.min(3, log.size());
        for (int i = 0; i < n; i++) {
            String line = log.get(log.size() - n + i);
            boolean last = i == n - 1;
            text(canvas, fit("[Sistema] " + line, logBox.width() - 24), logBox.left + 14, logBox.top + 28 + i * 25, 17,
                    last ? Color.WHITE : Color.argb(170, 255, 255, 255), Paint.Align.LEFT, last);
        }
        // botões
        boolean fight = ph == ArenaDuel.Phase.FIGHT;
        boolean canTeam = (ph == ArenaDuel.Phase.PREP && !duel.meReady) || fight;
        boolean cd = fight && duel.me.swapCd > 0 && duel.me.active != -1;
        String pl = fight ? (cd ? String.format("%.1fs", duel.me.swapCd / 1000f) : "Pokémon") : "Pokémon";
        actionButton(canvas, pokeBtn, pl, canTeam && !cd, Color.rgb(70, 120, 200));
        if (ph == ArenaDuel.Phase.PREP) actionButton(canvas, readyBtn, duel.meReady ? "AGUARDE…" : "PRONTO!", !picks.isEmpty() && !duel.meReady && thrown, C_GOOD);
        actionButton(canvas, fleeBtn, "Fugir", !busy, Color.rgb(170, 60, 60));
        if (fight && cd) bar(canvas, new RectF(pokeBtn.left + 8, pokeBtn.bottom - 12, pokeBtn.right - 8, pokeBtn.bottom - 6), 1f - duel.me.swapCd / (float) ArenaDuel.SWAP_CD_MS, C_ACCENT);
    }

    private String typeLabel(ArenaDuel.Unit u) {
        String a = u.t1 == null ? "" : u.t1.name();
        return u.t2 != null ? a + "/" + u.t2.name() : a;
    }

    /** Pode fechar o painel? (a escolha forçada depois de um desmaio não fecha) */
    public boolean modalClosable() { return !(modal == Modal.SWAP && duel != null && duel.me.active == -1); }

    private void drawModal(Canvas canvas, float w, float h, long now) {
        for (int i = 0; i < cards.length; i++) { cards[i].setEmpty(); okBtns[i].setEmpty(); leftBtns[i].setEmpty(); rightBtns[i].setEmpty(); }
        modalClose.setEmpty(); modalTeamBtn.setEmpty(); modalOk.setEmpty(); modalRect.setEmpty();
        if (modal == Modal.NONE || duel == null) return;
        boolean picker = modal == Modal.STARTER || modal == Modal.TEAM;      // escolha do time + ordem de combate
        List<ArenaDuel.Unit> list = new ArrayList<>();
        String title;
        if (picker) { title = modal == Modal.STARTER ? "MONTE SEU TIME E A ORDEM DE COMBATE" : "TIME E ORDEM DE COMBATE"; for (int i = 0; i < Math.min(6, roster.size()); i++) list.add(roster.get(i)); }
        else { title = duel.me.active == -1 ? "ESCOLHA O PRÓXIMO POKÉMON!" : "TROCAR POKÉMON"; for (int i = 0; i < duel.me.team.size(); i++) list.add(duel.me.team.get(i)); }
        int n = list.size();
        int slots = picker ? 6 : Math.max(1, n);                      // no picker os 6 lugares aparecem sempre (vazios = tracejado)
        int cols = picker ? 3 : Math.min(3, Math.max(1, n)), rows = (slots + cols - 1) / cols;
        float mw = Math.min(w - 24, picker ? 900 : 820), mh = Math.min(h - 24, picker ? 660 : (rows == 1 ? 420 : 470));
        float mx = w / 2f - mw / 2f, my = (h - mh) / 2f;
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawRect(0, 0, w, h, paint);
        modalRect.set(mx, my, mx + mw, my + mh);
        panel(canvas, modalRect, Color.argb(248, 28, 20, 22), Color.rgb(255, 150, 60), 4);
        text(canvas, title, modalRect.centerX(), my + 44, picker ? 24 : 28, Color.rgb(255, 214, 90), Paint.Align.CENTER, true);
        if (modalClosable()) {
            modalClose.set(modalRect.right - 58, my + 10, modalRect.right - 12, my + 56);
            panel(canvas, modalClose, Color.rgb(120, 40, 44), Color.WHITE, 2);
            text(canvas, "✕", modalClose.centerX(), modalClose.centerY() + 9, 24, Color.WHITE, Paint.Align.CENTER, true);
        }
        if (picker) text(canvas, "Escolha até " + ArenaDuel.MAX_TEAM + ". Use ◀ ▶ para definir a ordem: o 1º entra primeiro.", modalRect.centerX(), my + 68, 15, C_MUTED, Paint.Align.CENTER, false);
        float areaTop = my + (picker ? 82 : 66), areaBot = my + mh - (modal == Modal.SWAP ? 18 : 70);
        float gap = picker ? 12 : 14, cw = Math.min(picker ? 270 : 236, (mw - 40 - (cols - 1) * gap) / cols), ch = Math.min(picker ? 250 : 300, (areaBot - areaTop - (rows - 1) * gap) / rows);
        float gx = modalRect.centerX() - (cols * cw + (cols - 1) * gap) / 2f;
        boolean fightPick = modal == Modal.SWAP;
        boolean cd = fightPick && duel.me.swapCd > 0 && duel.me.active != -1;
        for (int i = 0; i < slots; i++) {
            int c = i % cols, r = i / cols;
            float l = gx + c * (cw + gap), t = areaTop + r * (ch + gap);
            if (i >= n) {                                                   // lugar vazio (menos de 6 na mão)
                RectF e = new RectF(l, t, l + cw, t + ch);
                panel(canvas, e, Color.argb(120, 40, 30, 30), Color.rgb(90, 70, 60), 2);
                text(canvas, "vazio", e.centerX(), e.centerY() + 6, 18, Color.argb(140, 255, 255, 255), Paint.Align.CENTER, false);
                continue;
            }
            ArenaDuel.Unit u = list.get(i);
            RectF rc = cards[i];
            rc.set(l, t, l + cw, t + ch);
            int order = picker ? picks.indexOf(i) : -1;
            boolean inTeam = picker && order >= 0;
            boolean act = fightPick && i == duel.me.active;
            boolean dead = fightPick && !u.alive();
            int edge = dead ? Color.DKGRAY : act ? C_ACCENT : inTeam ? C_GOOD : Color.rgb(150, 110, 80);
            panel(canvas, rc, inTeam ? Color.argb(245, 36, 54, 40) : Color.argb(240, 46, 32, 30), edge, inTeam || act ? 4 : 2);
            float btnH = ch < 200 ? 34 : 40;
            text(canvas, fit(u.species + (u.shiny ? " ✨" : ""), cw - (inTeam ? 64 : 16)), rc.centerX() + (inTeam ? 16 : 0), rc.top + 26, 19, Color.WHITE, Paint.Align.CENTER, true);
            if (inTeam) {                                                   // selo da ordem de combate
                paint.setStyle(Paint.Style.FILL); paint.setColor(order == 0 ? C_ACCENT : C_GOOD);
                canvas.drawCircle(rc.left + 24, rc.top + 22, 16, paint);
                text(canvas, String.valueOf(order + 1), rc.left + 24, rc.top + 29, 20, Color.BLACK, Paint.Align.CENTER, true);
            }
            float sz = Math.max(48, Math.min(cw - 40, ch - btnH - 84));
            float spriteBox = (float) com.example.pokeworld.entities.SpeciesSize.softScale(u.species);
            if (dead) text(canvas, "💀", rc.centerX(), rc.top + 26 + sz * 0.7f, 44, Color.WHITE, Paint.Align.CENTER, false);
            else ArenaUI.sprite(canvas, paint, u.species, u.shiny, 0, (int) ((now / 320) % 3), rc.centerX(), rc.top + 32 + sz, sz * Math.min(1f, 0.62f + 0.38f * spriteBox / 1.8f));
            float ty = rc.top + 32 + sz + 20;
            text(canvas, "Nv. " + u.level + (fightPick ? "  " + Math.max(0, u.hp) + "/" + u.maxHp : "  ⚡" + u.power()), rc.centerX(), ty, 16, Color.rgb(255, 214, 90), Paint.Align.CENTER, true);
            if (ch >= 190) text(canvas, typeLabel(u), rc.centerX(), ty + 20, 14, C_MUTED, Paint.Align.CENTER, false);
            RectF ok = okBtns[i];
            if (picker) {
                float by = rc.bottom - btnH - 8, aw = 44;
                if (inTeam) {                                               // ◀ [TIRAR] ▶
                    leftBtns[i].set(rc.left + 8, by, rc.left + 8 + aw, by + btnH);
                    rightBtns[i].set(rc.right - 8 - aw, by, rc.right - 8, by + btnH);
                    ok.set(leftBtns[i].right + 6, by, rightBtns[i].left - 6, by + btnH);
                    actionButton(canvas, leftBtns[i], "◀", !duel.meReady && order > 0, Color.rgb(90, 90, 120));
                    actionButton(canvas, rightBtns[i], "▶", !duel.meReady && order < picks.size() - 1, Color.rgb(90, 90, 120));
                    actionButton(canvas, ok, "TIRAR", !duel.meReady && picks.size() > 1, Color.rgb(170, 70, 70));
                } else {
                    ok.set(rc.left + 8, by, rc.right - 8, by + btnH);
                    boolean full = picks.size() >= ArenaDuel.MAX_TEAM;
                    actionButton(canvas, ok, full ? "TIME CHEIO" : "ENTRAR", !duel.meReady && !full, Color.rgb(70, 120, 200));
                }
                continue;
            }
            ok.set(rc.left + 12, rc.bottom - btnH - 10, rc.right - 12, rc.bottom - 10);
            String lab = act ? "EM CAMPO" : dead ? "DESMAIOU" : cd ? String.format("%.1fs", duel.me.swapCd / 1000f) : "ESCOLHER";
            actionButton(canvas, ok, lab, !act && !dead && !cd, Color.rgb(70, 170, 90));
        }
        if (picker) {
            modalOk.set(modalRect.centerX() - 130, my + mh - 56, modalRect.centerX() + 130, my + mh - 14);
            actionButton(canvas, modalOk, modal == Modal.STARTER || !thrown ? "COMEÇAR COM O 1º" : "OK", !picks.isEmpty(), C_ACCENT);
        }
    }

    private void drawOverlay(Canvas canvas, float w, float h, long now) {
        if (duel.phase == ArenaDuel.Phase.COUNTDOWN) {
            int n = (int) Math.ceil(duel.countdown / 1000.0);
            text(canvas, n > 0 ? String.valueOf(n) : "LUTAR!", w / 2f, h * 0.45f, 150, C_ACCENT, Paint.Align.CENTER, true);
            text(canvas, "Os Pokémon lutam sozinhos. Você troca de Pokémon a cada 5 s.", w / 2f, h * 0.45f + 50, 24, Color.WHITE, Paint.Align.CENTER, true);
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
