package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.ArenaDuel;
import com.example.pokeworld.managers.ArenaLadder;
import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.RewardManager;
import com.example.pokeworld.ui.UIManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Duelo da Arena. A arena é um quadrado com uma LINHA no meio: o oponente (escondido) joga na metade de cima e você na de baixo.
 * Atrás da arena, separado dela, fica o seu retângulo de treinador: você anda livremente dentro dele (arraste o dedo) e
 * LANÇA o seu Pokémon tocando em qualquer ponto da sua metade da arena. Durante o preparo (com tempo) dá para relançar
 * quantas vezes quiser e escolher até 3 Pokémon; aperte PRONTO. Quando os dois estiverem prontos, o oponente é revelado
 * e os Pokémon lutam sozinhos; durante a luta você pode trocar de Pokémon a cada 5 segundos (o treinador lança outra bola).
 */
public class ArenaBattleUI extends BaseUI {
    public final RectF exitBtn = new RectF(), readyBtn = new RectF(), continueBtn = new RectF();
    public final RectF[] cards = new RectF[6];

    public ArenaDuel duel;
    public ArenaLadder.Fighter fighter;
    public List<ArenaDuel.Unit> roster = new ArrayList<>();
    public final List<Integer> picks = new ArrayList<>();     // índices do roster, na ordem (1º = inicial)
    public ArenaLadder.Result result;
    private boolean spent = false, resultApplied = false;
    private long lastT = 0;

    private static final class Pop { String text; int color; boolean onFoe; long t0; int slot; }
    private final List<Pop> pops = new ArrayList<>();
    private long meLunge = 0, foeLunge = 0;

    // mapa: a arena (quadrado com a linha no meio) e os retângulos dos treinadores, atrás dela
    public final RectF field = new RectF();           // arena
    public final RectF meBox = new RectF();           // onde o seu treinador anda
    public final RectF foeBox = new RectF();          // onde o treinador do oponente fica
    public float midY;                                // y da linha do meio
    public float placeX = 0.5f, placeY = 0.6f;        // onde você lançou o inicial (0..1 dentro da sua metade)
    public boolean thrown = false;                    // já lançou o Pokémon na arena?
    private float foePlaceX = 0.5f;
    private float meX, meY, foeX, foeY;               // posições desenhadas (feet) dos Pokémon
    private float trX = -1, trY = -1, trTX = -1, trTY = -1;   // treinador: posição e destino
    private boolean trMoving = false;
    private long fightT0 = 0;
    private ArenaDuel.Phase lastPhase = null;
    private float pkSize = 80, trSize = 80;

    // bola em voo (lançamento do jogador ou do oponente)
    private static final class Ball { float x0, y0, x1, y1; long t0; boolean foe; }
    private final List<Ball> balls = new ArrayList<>();
    private static final long BALL_MS = 520;

    public ArenaBattleUI() { for (int i = 0; i < cards.length; i++) cards[i] = new RectF(); }

    // ------------------------------------------------------------------ montagem
    public void begin(GameView gv, ArenaLadder.Fighter f, List<ArenaDuel.Unit> foeTeam) {
        fighter = f;
        duel = new ArenaDuel(new Random());
        duel.foe.name = f.name;
        duel.foe.team.addAll(foeTeam);
        duel.foe.active = 0;
        duel.foeReadyIn = 4000 + new Random().nextInt(9000);     // o oponente demora um pouco para ficar pronto
        duel.prepLeft = ArenaDuel.PREP_MS;
        placeX = 0.5f; placeY = 0.6f; thrown = false; foePlaceX = 0.3f + new Random().nextFloat() * 0.4f; fightT0 = 0;
        trX = -1; trTX = -1; trMoving = false; balls.clear(); lastPhase = null;
        roster = ArenaLadder.playerRoster(gv);
        duel.me.name = com.example.pokeworld.managers.FriendManager.getInstance().getMyName();
        picks.clear();
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < roster.size(); i++) order.add(i);
        java.util.Collections.sort(order, (a, b) -> Integer.compare(roster.get(b).power(), roster.get(a).power()));
        for (int i = 0; i < Math.min(ArenaDuel.MAX_TEAM, order.size()); i++) picks.add(order.get(i));
        rebuildMe();
        pops.clear(); result = null; spent = false; resultApplied = false; lastT = System.currentTimeMillis();
        gv.ui.state = UIManager.UIState.ARENA_BATTLE;
    }

    public void rebuildMe() {
        duel.me.team.clear();
        for (int p : picks) duel.me.team.add(roster.get(p));
        duel.me.active = duel.me.team.isEmpty() ? -1 : 0;
    }

    /** Toque na arena durante o preparo: o treinador LANÇA o inicial ali, mas só na sua metade (abaixo da linha). */
    public boolean tapField(float x, float y, GameView gv) {
        if (duel == null || duel.phase != ArenaDuel.Phase.PREP || !field.contains(x, y)) return false;
        if (duel.meReady) return true;
        if (y < midY + 10) { gv.ui.showMessage("Lance na SUA metade da arena (abaixo da linha)."); return true; }
        throwAt(x, y, true);
        return true;
    }

    /** Lança o Pokémon: guarda o ponto (limitado à sua metade) e solta uma bola do treinador até lá. */
    public void throwAt(float x, float y, boolean animate) {
        float minY = midY + pkSize * 0.55f, maxY = field.bottom - 8;
        float cy = Math.max(minY, Math.min(maxY, y));
        float cx = Math.max(field.left + pkSize * 0.5f, Math.min(field.right - pkSize * 0.5f, x));
        placeX = (cx - field.left) / Math.max(1f, field.width());
        placeY = (cy - midY) / Math.max(1f, field.bottom - midY);
        thrown = true;
        if (animate) launchBall(cx, cy - pkSize * 0.3f, false);
    }

    private void launchBall(float toX, float toY, boolean foe) {
        Ball b = new Ball();
        if (foe) { b.x0 = (foeBox.left + foeBox.right) / 2f; b.y0 = foeBox.centerY(); }
        else { b.x0 = trX < 0 ? meBox.centerX() : trX; b.y0 = (trY < 0 ? meBox.centerY() : trY) - 20; }
        b.x1 = toX; b.y1 = toY; b.t0 = System.currentTimeMillis(); b.foe = foe;
        balls.add(b);
    }

    /** Anda com o treinador: arraste o dedo dentro do seu retângulo. */
    public boolean walkTo(float x, float y) {
        if (duel == null || meBox.isEmpty()) return false;
        if (!meBox.contains(x, y)) return false;
        trTX = x; trTY = y;
        return true;
    }

    /** Pokémon trocado na luta: o treinador lança outra bola no lugar do Pokémon. */
    public void onSwap() { launchBall(meX, meY - pkSize * 0.3f, false); }

    /** Toque numa carta da preparação: adiciona, vira inicial ou remove. */
    public void togglePick(int rosterIdx) {
        if (duel != null && duel.meReady) return;       // PRONTO trava o time
        int at = picks.indexOf(rosterIdx);
        if (at < 0) { if (picks.size() < ArenaDuel.MAX_TEAM) picks.add(rosterIdx); }
        else if (at > 0) { picks.remove(at); picks.add(0, rosterIdx); }
        else if (picks.size() > 1) picks.remove(0);
        rebuildMe();
    }

    /** PRONTO: gasta a tentativa e trava o time; a luta começa quando o oponente também estiver pronto. Retorna false se não houver tentativas. */
    public boolean ready(GameView gv) {
        if (duel == null || duel.phase != ArenaDuel.Phase.PREP || duel.meReady || picks.isEmpty()) return false;
        RewardManager.Store s = PointsManager.store();
        if (!ArenaLadder.useTry(s)) return false;
        spent = true;
        if (!thrown) throwAt(field.left + field.width() * placeX, midY + (field.bottom - midY) * placeY, false);   // tempo acabou: lança no ponto padrão
        duel.ready();
        lastT = System.currentTimeMillis();
        return true;
    }

    /** Sair da tela: em luta conta como derrota. */
    public void leave(GameView gv) {
        if (duel != null && (duel.phase == ArenaDuel.Phase.FIGHT || duel.phase == ArenaDuel.Phase.COUNTDOWN)) { duel.forfeit(); applyResult(gv); }
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

    // ------------------------------------------------------------------ desenho
    @Override
    public void draw(Canvas canvas, GameView gv) {
        if (duel == null) { gv.ui.state = UIManager.UIState.ARENA; return; }
        float w = canvas.getWidth(), h = canvas.getHeight();
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
        drainPops(now);

        layout(w, h);
        if (duel.phase != lastPhase) {
            if (duel.phase == ArenaDuel.Phase.COUNTDOWN) launchBall(foeX, foeY - pkSize * 0.3f, true);   // o oponente lança o dele
            lastPhase = duel.phase;
        }
        stepTrainer(dt);
        drawField(canvas, w, now);
        drawTopBar(canvas, w, now);

        float size = pkSize;
        int frame = (int) ((now / 320) % 3);
        computePositions(now);
        drawFoe(canvas, w, h, size, frame, now);
        drawMe(canvas, w, h, size, frame, now);
        drawTrainers(canvas, now, frame);
        drawBalls(canvas, now);

        // ---- textos flutuantes
        for (int i = pops.size() - 1; i >= 0; i--) {
            Pop p = pops.get(i);
            float a = (now - p.t0) / 1000f;
            if (a > 1f) { pops.remove(i); continue; }
            float px = p.onFoe ? foeX : meX, py = (p.onFoe ? foeY : meY) - size * 0.7f - a * 46 - p.slot * 22;
            paint.setAlpha(255);
            text(canvas, p.text, px + (p.slot % 2 == 0 ? -30 : 30), py, 30, p.color, Paint.Align.CENTER, true);
        }

        drawBottom(canvas, w, h, now);
        drawOverlay(canvas, w, h, now);
    }

    private void drainPops(long now) {
        int slot = 0;
        for (ArenaDuel.Pop p : duel.pops) {
            Pop q = new Pop();
            q.text = p.text; q.color = p.color; q.onFoe = p.onFoe; q.t0 = now; q.slot = (slot++) % 3;
            pops.add(q);
            if (p.text.startsWith("-") || p.text.startsWith("⭐") || p.text.startsWith("💥")) { if (p.onFoe) meLunge = now; else foeLunge = now; }
        }
        duel.pops.clear();
    }

    /** Calcula a arena e os dois retângulos de treinador, empilhados: oponente, arena (quadrado) e você. */
    private void layout(float w, float h) {
        float top = 72, bot = h * 0.745f - 30, rh = bot - top, gap = 8;
        float foeH = rh * 0.13f, meH = rh * 0.23f;
        float ah = rh - foeH - meH - 2 * gap;
        float aw = Math.min(w * 0.62f, ah * 1.7f);
        float ax = (w - aw) / 2f;
        foeBox.set(ax, top, ax + aw, top + foeH);
        field.set(ax, foeBox.bottom + gap, ax + aw, foeBox.bottom + gap + ah);
        meBox.set(ax, field.bottom + gap, ax + aw, field.bottom + gap + meH);
        midY = (field.top + field.bottom) / 2f;
        pkSize = Math.max(60, Math.min(ah * 0.36f, aw * 0.20f));
        trSize = Math.max(56, Math.min(meBox.height() * 0.72f, 90));
        if (trX < 0) { trX = meBox.centerX(); trY = meBox.bottom - 12; trTX = trX; trTY = trY; }
    }

    private void clampTrainer() {
        float half = trSize * 0.32f;
        trTX = Math.max(meBox.left + half, Math.min(meBox.right - half, trTX));
        trTY = Math.max(meBox.top + trSize * 0.92f, Math.min(meBox.bottom - 6, trTY));
    }

    private void stepTrainer(long dt) {
        clampTrainer();
        float dx = trTX - trX, dy = trTY - trY, dist = (float) Math.hypot(dx, dy);
        trMoving = dist > 3;
        if (trMoving) {
            float step = Math.min(dist, 340f * dt / 1000f);
            trX += dx / dist * step; trY += dy / dist * step;
        }
        float half = trSize * 0.32f;
        trX = Math.max(meBox.left + half, Math.min(meBox.right - half, trX));
        trY = Math.max(meBox.top + trSize * 0.92f, Math.min(meBox.bottom - 6, trY));
    }

    /** Onde cada Pokémon fica: no preparo, onde foi lançado; na luta, avançam até se encontrar perto da linha. */
    private void computePositions(long now) {
        float foeStartX = field.left + field.width() * foePlaceX, foeStartY = field.top + (midY - field.top) * 0.80f;
        float meStartX = field.left + field.width() * placeX, meStartY = midY + (field.bottom - midY) * placeY;
        float cx = field.centerX();
        float fx = cx + field.width() * 0.06f, fy = midY - pkSize * 0.05f;
        float mx = cx - field.width() * 0.06f, my = midY + pkSize * 0.62f;
        float p = 0;
        if (duel.phase == ArenaDuel.Phase.FIGHT || duel.phase == ArenaDuel.Phase.RESULT)
            p = fightT0 == 0 ? 0 : Math.min(1f, (now - fightT0) / 1200f);
        p = p * p * (3 - 2 * p);
        foeX = foeStartX + (fx - foeStartX) * p; foeY = foeStartY + (fy - foeStartY) * p;
        meX = meStartX + (mx - meStartX) * p; meY = meStartY + (my - meStartY) * p;
    }

    private void drawField(Canvas canvas, float w, long now) {
        float h = canvas.getHeight();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(14, 20, 34));
        canvas.drawRect(0, 0, w, h, paint);
        // fundo de pedra ao redor
        paint.setColor(Color.rgb(24, 32, 48));
        canvas.drawRect(0, 70, w, h * 0.745f - 26, paint);

        // retângulos dos treinadores (chão de pedra, separados da arena)
        for (int k = 0; k < 2; k++) {
            RectF b = k == 0 ? foeBox : meBox;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(k == 0 ? Color.rgb(78, 52, 56) : Color.rgb(52, 66, 92));
            canvas.drawRect(b, paint);
            paint.setColor(k == 0 ? Color.argb(70, 0, 0, 0) : Color.argb(60, 255, 255, 255));
            for (float x = b.left; x < b.right; x += 48) canvas.drawRect(x, b.top, x + 2, b.bottom, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4);
            paint.setColor(k == 0 ? Color.rgb(230, 90, 90) : Color.rgb(90, 210, 130));
            canvas.drawRect(b, paint);
        }
        paint.setStyle(Paint.Style.FILL);
        text(canvas, fit(duel.foe.name, foeBox.width() * 0.3f), foeBox.left + 12, foeBox.top + 22, 15, Color.argb(220, 255, 200, 200), Paint.Align.LEFT, true);
        text(canvas, "SEU TREINADOR (arraste)", meBox.left + 12, meBox.bottom - 10, 15, Color.argb(230, 190, 255, 210), Paint.Align.LEFT, true);

        // arena: grama em xadrez
        float tile = Math.max(32, field.width() / 14f);
        for (float y = field.top, r = 0; y < field.bottom; y += tile, r++)
            for (float x = field.left, c = 0; x < field.right; x += tile, c++) {
                paint.setColor(((int) (r + c)) % 2 == 0 ? Color.rgb(54, 112, 70) : Color.rgb(48, 102, 64));
                canvas.drawRect(x, y, Math.min(x + tile, field.right), Math.min(y + tile, field.bottom), paint);
            }
        paint.setColor(Color.argb(60, 200, 50, 50));
        canvas.drawRect(field.left, field.top, field.right, midY, paint);
        paint.setColor(Color.argb(50, 50, 110, 220));
        canvas.drawRect(field.left, midY, field.right, field.bottom, paint);
        // zona de lançamento (pulsa no preparo, enquanto você ainda pode lançar)
        if (duel.phase == ArenaDuel.Phase.PREP && !duel.meReady) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(now / 280.0);
            paint.setColor(Color.argb((int) (35 + 40 * pulse), 255, 220, 90));
            canvas.drawRect(field.left, midY, field.right, field.bottom, paint);
        }
        // círculo central e a LINHA
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4); paint.setColor(Color.argb(150, 255, 255, 255));
        canvas.drawCircle(field.centerX(), midY, Math.min(60, field.height() * 0.2f), paint);
        paint.setStrokeWidth(6); paint.setColor(Color.WHITE);
        for (float x = field.left; x < field.right; x += 44) canvas.drawLine(x, midY, Math.min(x + 28, field.right), midY, paint);
        paint.setStrokeWidth(5); paint.setColor(Color.argb(220, 255, 196, 64));
        canvas.drawRect(field.left, field.top, field.right, field.bottom, paint);
        paint.setStyle(Paint.Style.FILL);
        text(canvas, "SUA METADE", field.left + 10, midY + 22, 14, Color.argb(220, 255, 255, 255), Paint.Align.LEFT, true);
        text(canvas, "METADE DO OPONENTE", field.left + 10, midY - 10, 14, Color.argb(200, 255, 190, 190), Paint.Align.LEFT, true);
        if (duel.phase == ArenaDuel.Phase.PREP && !duel.meReady && !thrown)
            text(canvas, "👆 Toque na sua metade para LANÇAR o Pokémon", field.centerX(), field.bottom - 14, 18, Color.WHITE, Paint.Align.CENTER, true);
    }

    private void drawTrainers(Canvas canvas, long now, int frame) {
        // oponente: parado no retângulo de cima, de frente para a arena
        float fx = foeBox.centerX(), fy = foeBox.bottom - 3, fsz = Math.min(foeBox.height() * 1.05f, 64);
        ArenaUI.sprite(canvas, paint, "personagem", false, 0, 1, fx, fy, fsz);
        // você: de costas para o jogador, olhando a arena
        float msz = trSize;
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawOval(new RectF(trX - msz * 0.3f, trY - msz * 0.06f, trX + msz * 0.3f, trY + msz * 0.06f), paint);
        ArenaUI.sprite(canvas, paint, "personagem", false, 2, trMoving ? frame : 1, trX, trY, msz);
    }

    private void drawBalls(Canvas canvas, long now) {
        for (int i = balls.size() - 1; i >= 0; i--) {
            Ball b = balls.get(i);
            float t = (now - b.t0) / (float) BALL_MS;
            if (t >= 1f) {
                // pequeno clarão onde a bola abriu
                float a = Math.min(1f, (t - 1f) * 4f);
                if (a >= 1f) { balls.remove(i); continue; }
                paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb((int) (200 * (1 - a)), 255, 255, 255));
                canvas.drawCircle(b.x1, b.y1, 20 + 50 * a, paint);
                continue;
            }
            float x = b.x0 + (b.x1 - b.x0) * t, y = b.y0 + (b.y1 - b.y0) * t - (float) Math.sin(Math.PI * t) * 60;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(230, 50, 50)); canvas.drawCircle(x, y, 11, paint);
            paint.setColor(Color.WHITE); canvas.drawRect(x - 11, y - 1.5f, x + 11, y + 1.5f, paint);
            paint.setColor(Color.rgb(250, 250, 250)); canvas.drawCircle(x, y, 4, paint);
        }
    }

    private void drawTopBar(Canvas canvas, float w, long now) {
        paint.setColor(Color.argb(255, 18, 26, 42));
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
        text(canvas, duel.phase == ArenaDuel.Phase.PREP || duel.phase == ArenaDuel.Phase.RESULT ? "◀ VOLTAR" : "🏳 DESISTIR", exitBtn.centerX(), exitBtn.centerY() + 8, 20, Color.WHITE, Paint.Align.CENTER, true);
    }

    private void infoBox(Canvas canvas, RectF r, ArenaDuel.Unit u, String owner, int accent, ArenaDuel.Side side) {
        panel(canvas, r, Color.argb(225, 16, 24, 40), accent, 3);
        if (u == null) { text(canvas, "—", r.centerX(), r.centerY() + 8, 24, C_MUTED, Paint.Align.CENTER, false); return; }
        paint.setTextSize(22);
        text(canvas, fit(u.species + (u.shiny ? " ✨" : ""), r.width() - 100), r.left + 14, r.top + 30, 22, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, "Nv. " + u.level, r.right - 14, r.top + 30, 20, Color.rgb(255, 214, 90), Paint.Align.RIGHT, true);
        float frac = u.hpFrac();
        bar(canvas, new RectF(r.left + 14, r.top + 42, r.right - 14, r.top + 60), frac, frac > 0.5f ? C_GOOD : frac > 0.25f ? Color.rgb(255, 200, 60) : C_BAD);
        text(canvas, Math.max(0, u.hp) + " / " + u.maxHp, r.left + 14, r.top + 80, 15, C_MUTED, Paint.Align.LEFT, false);
        text(canvas, owner, r.right - 14, r.top + 80, 15, C_MUTED, Paint.Align.RIGHT, false);
    }

    private boolean ballInFlight(boolean foe, long now) {
        for (Ball b : balls) if (b.foe == foe && now - b.t0 < BALL_MS) return true;
        return false;
    }

    private void drawFoe(Canvas canvas, float w, float h, float size, int frame, long now) {
        boolean hidden = duel.phase == ArenaDuel.Phase.PREP;      // a escolha do oponente fica escondida até todos ficarem prontos
        if (hidden) {
            int n = Math.max(1, Math.min(ArenaDuel.MAX_TEAM, duel.foe.team.size()));
            for (int i = 0; i < n; i++) {
                float cx = field.left + field.width() * (0.30f + 0.2f * i), cy = field.top + (midY - field.top) * (i == 1 ? 0.62f : 0.78f);
                paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(150, 0, 0, 0));
                canvas.drawOval(new RectF(cx - size * 0.35f, cy - size * 0.05f, cx + size * 0.35f, cy + size * 0.05f), paint);
                paint.setColor(Color.argb(215, 38, 38, 56));
                canvas.drawCircle(cx, cy - size * 0.40f, size * 0.34f, paint);
                text(canvas, "?", cx, cy - size * 0.40f + size * 0.16f, size * 0.46f, Color.argb(230, 255, 255, 255), Paint.Align.CENTER, true);
            }
            text(canvas, duel.foeReady ? "✅ " + duel.foe.name + " está PRONTO" : "⏳ " + duel.foe.name + " está escolhendo…",
                    foeBox.right - 12, foeBox.bottom - 12, 16, duel.foeReady ? C_GOOD : Color.WHITE, Paint.Align.RIGHT, true);
            return;
        }
        ArenaDuel.Unit u = duel.foe.cur();
        float iw = Math.min(w * 0.30f, field.left - 30);
        if (iw > 150) infoBox(canvas, new RectF(field.left - 16 - iw, field.top, field.left - 16, field.top + 92), u, duel.foe.name, Color.rgb(230, 90, 90), duel.foe);
        if (u != null && u.alive() && !ballInFlight(true, now)) {
            float lunge = Math.max(0, 1 - (now - foeLunge) / 220f) * 30;
            float shake = duel.foe.hurtFlash > 0 ? (float) Math.sin(now / 22.0) * 7 : 0;
            shadow(canvas, foeX + shake, foeY + lunge, size);
            ArenaUI.sprite(canvas, paint, u.species, u.shiny, 0, frame, foeX + shake, foeY + lunge, size);
        }
        // time do oponente (quadrinhos com vida)
        float x = w - 30;
        for (int i = duel.foe.team.size() - 1; i >= 0; i--) {
            ArenaDuel.Unit t = duel.foe.team.get(i);
            RectF r = new RectF(x - 62, 76, x, 128);
            panel(canvas, r, i == duel.foe.active ? C_PANEL_HI : C_PANEL, t.alive() ? (i == duel.foe.active ? Color.RED : C_LINE) : Color.DKGRAY, 2);
            if (t.alive()) ArenaUI.sprite(canvas, paint, t.species, t.shiny, 0, 0, r.centerX(), r.bottom - 8, 40);
            else text(canvas, "💀", r.centerX(), r.centerY() + 10, 26, Color.WHITE, Paint.Align.CENTER, false);
            bar(canvas, new RectF(r.left + 6, r.bottom - 8, r.right - 6, r.bottom - 3), t.hpFrac(), C_GOOD);
            x -= 68;
        }
    }

    private void shadow(Canvas canvas, float cx, float feetY, float size) {
        paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(90, 0, 0, 0));
        canvas.drawOval(new RectF(cx - size * 0.38f, feetY - size * 0.07f, cx + size * 0.38f, feetY + size * 0.07f), paint);
    }

    private void drawMe(Canvas canvas, float w, float h, float size, int frame, long now) {
        ArenaDuel.Unit u = duel.me.cur();
        if (duel.phase != ArenaDuel.Phase.PREP) {
            float iw = Math.min(w * 0.30f, w - field.right - 30);
            if (iw > 150) infoBox(canvas, new RectF(field.right + 16, field.bottom - 92, field.right + 16 + iw, field.bottom), u, duel.me.name, C_GOOD, duel.me);
        }
        boolean visible = duel.phase != ArenaDuel.Phase.PREP || thrown;
        if (u != null && u.alive() && visible && !ballInFlight(false, now)) {
            float lunge = Math.max(0, 1 - (now - meLunge) / 220f) * 30;
            float shake = duel.me.hurtFlash > 0 ? (float) Math.sin(now / 22.0) * 7 : 0;
            shadow(canvas, meX + shake, meY - lunge, size);
            ArenaUI.sprite(canvas, paint, u.species, u.shiny, 2, frame, meX + shake, meY - lunge, size);
        }
    }

    private void drawBottom(Canvas canvas, float w, float h, long now) {
        float top = h * 0.745f, bottom = h - 14;
        ArenaDuel.Phase ph = duel.phase;
        if (ph == ArenaDuel.Phase.PREP) {
            text(canvas, duel.meReady ? "Time travado! Esperando o oponente ficar pronto…" : thrown ? "Pronto para a luta? Pode lançar de novo em outro ponto, ou aperte PRONTO. O oponente não vê." : "1) Escolha até 3 Pokémon  2) Toque na sua metade da arena para LANÇAR o inicial  3) PRONTO", 30, top - 6, 17, duel.meReady ? C_GOOD : Color.WHITE, Paint.Align.LEFT, true);
            readyBtn.set(w - 250, top + 6, w - 30, bottom);
            boolean can = !picks.isEmpty() && !duel.meReady && thrown;
            actionButton(canvas, readyBtn, duel.meReady ? "⏳ AGUARDANDO" : "✅ PRONTO!", can, C_GOOD);
            int n = Math.min(6, roster.size());
            float areaW = readyBtn.left - 30 - 14, g = 8;
            float cw = Math.min(190, (areaW - (n - 1) * g) / Math.max(1, n));
            for (int i = 0; i < 6; i++) cards[i].setEmpty();
            for (int i = 0; i < n; i++) {
                RectF r = cards[i];
                r.set(30 + i * (cw + g), top + 6, 30 + i * (cw + g) + cw, bottom);
                ArenaDuel.Unit u = roster.get(i);
                int at = picks.indexOf(i);
                panel(canvas, r, at >= 0 ? C_PANEL_HI : C_PANEL, at == 0 ? C_ACCENT : at > 0 ? C_GOOD : C_LINE, at >= 0 ? 4 : 2);
                float sz = Math.min(r.height() - 62, cw - 20);
                ArenaUI.sprite(canvas, paint, u.species, u.shiny, 0, (int) ((now / 320) % 3), r.centerX(), r.top + 8 + sz, sz);
                paint.setTextSize(15);
                text(canvas, fit(u.species, cw - 10), r.centerX(), r.bottom - 38, 15, Color.WHITE, Paint.Align.CENTER, true);
                text(canvas, "Nv." + u.level + "  ⚡" + u.power(), r.centerX(), r.bottom - 20, 14, Color.rgb(255, 214, 90), Paint.Align.CENTER, false);
                text(canvas, typeLabel(u), r.centerX(), r.bottom - 5, 12, C_MUTED, Paint.Align.CENTER, false);
                if (at >= 0) {
                    paint.setColor(at == 0 ? C_ACCENT : C_GOOD); paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(r.left + 18, r.top + 18, 14, paint);
                    text(canvas, at == 0 ? "★" : String.valueOf(at + 1), r.left + 18, r.top + 25, 20, Color.BLACK, Paint.Align.CENTER, true);
                }
            }
        } else {
            readyBtn.setEmpty();
            int n = duel.me.team.size();
            for (int i = 0; i < 6; i++) cards[i].setEmpty();
            float g = 10, cw = Math.min(260, (w - 60 - (n - 1) * g) / Math.max(1, n));
            if (ph == ArenaDuel.Phase.FIGHT) {
                String hint = duel.me.active == -1 ? "⚠ ESCOLHA O PRÓXIMO POKÉMON!" : duel.me.swapCd > 0 ? "Troca disponível em " + String.format("%.1f", duel.me.swapCd / 1000f) + "s" : "🔄 Toque em um Pokémon para TROCAR";
                text(canvas, hint, 30, top - 6, 20, duel.me.active == -1 ? C_BAD : duel.me.swapCd > 0 ? C_MUTED : C_GOOD, Paint.Align.LEFT, true);
            }
            for (int i = 0; i < n; i++) {
                RectF r = cards[i];
                r.set(30 + i * (cw + g), top + 6, 30 + i * (cw + g) + cw, bottom);
                ArenaDuel.Unit u = duel.me.team.get(i);
                boolean act = i == duel.me.active;
                panel(canvas, r, act ? C_PANEL_HI : C_PANEL, !u.alive() ? Color.DKGRAY : act ? C_ACCENT : C_LINE, act ? 4 : 2);
                float sz = r.height() - 20;
                if (u.alive()) ArenaUI.sprite(canvas, paint, u.species, u.shiny, 0, (int) ((now / 320) % 3), r.left + sz * 0.5f + 8, r.bottom - 8, sz * 0.8f);
                else text(canvas, "💀", r.left + 50, r.centerY() + 14, 44, Color.WHITE, Paint.Align.CENTER, false);
                float tx = r.left + sz * 0.8f + 18;
                paint.setTextSize(18);
                text(canvas, fit(u.species, r.right - tx - 8), tx, r.top + 30, 18, Color.WHITE, Paint.Align.LEFT, true);
                text(canvas, "Nv." + u.level, tx, r.top + 52, 15, Color.rgb(255, 214, 90), Paint.Align.LEFT, false);
                bar(canvas, new RectF(tx, r.top + 62, r.right - 12, r.top + 76), u.hpFrac(), u.hpFrac() > 0.5f ? C_GOOD : u.hpFrac() > 0.25f ? Color.rgb(255, 200, 60) : C_BAD);
                text(canvas, Math.max(0, u.hp) + "/" + u.maxHp, tx, r.top + 96, 13, C_MUTED, Paint.Align.LEFT, false);
                if (ph == ArenaDuel.Phase.FIGHT && u.alive() && !act && duel.me.active != -1 && duel.me.swapCd > 0) {
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
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.argb(110, 0, 0, 0));
            canvas.drawRect(0, 0, w, h, paint);
            text(canvas, n > 0 ? String.valueOf(n) : "LUTAR!", w / 2f, h * 0.5f + 40, 150, C_ACCENT, Paint.Align.CENTER, true);
            text(canvas, "Prepare-se! Você troca de Pokémon a cada 5 s.", w / 2f, h * 0.5f + 90, 24, Color.WHITE, Paint.Align.CENTER, false);
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
