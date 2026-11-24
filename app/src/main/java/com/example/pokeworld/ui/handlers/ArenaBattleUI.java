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
 * Duelo da Arena. Oponente em cima, você embaixo. Fase de preparação: escolha até 3 Pokémon (o 1º é o inicial) e aperte PRONTO.
 * Na luta, toque em outro Pokémon do seu time para trocar (recarga de 5 s).
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

    public ArenaBattleUI() { for (int i = 0; i < cards.length; i++) cards[i] = new RectF(); }

    // ------------------------------------------------------------------ montagem
    public void begin(GameView gv, ArenaLadder.Fighter f, List<ArenaDuel.Unit> foeTeam) {
        fighter = f;
        duel = new ArenaDuel(new Random());
        duel.foe.name = f.name;
        duel.foe.team.addAll(foeTeam);
        duel.foe.active = 0;
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

    /** Toque numa carta da preparação: adiciona, vira inicial ou remove. */
    public void togglePick(int rosterIdx) {
        int at = picks.indexOf(rosterIdx);
        if (at < 0) { if (picks.size() < ArenaDuel.MAX_TEAM) picks.add(rosterIdx); }
        else if (at > 0) { picks.remove(at); picks.add(0, rosterIdx); }
        else if (picks.size() > 1) picks.remove(0);
        rebuildMe();
    }

    /** PRONTO: gasta a tentativa e começa a contagem. Retorna false se não houver tentativas. */
    public boolean ready(GameView gv) {
        if (duel == null || duel.phase != ArenaDuel.Phase.PREP || picks.isEmpty()) return false;
        RewardManager.Store s = PointsManager.store();
        if (!ArenaLadder.useTry(s)) return false;
        spent = true;
        while (duel.foe.team.size() > picks.size()) duel.foe.team.remove(duel.foe.team.size() - 1);   // mesmo número de Pokémon dos dois lados
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
        if (duel.phase == ArenaDuel.Phase.RESULT) applyResult(gv);
        drainPops(now);

        drawBackdrop(canvas, w, h);
        drawTopBar(canvas, w, now);

        // ---- oponente (em cima, à direita) e você (embaixo, à esquerda)
        float size = Math.min(h * 0.24f, w * 0.2f);
        int frame = (int) ((now / 320) % 3);
        drawFoe(canvas, w, h, size, frame, now);
        drawMe(canvas, w, h, size, frame, now);

        // ---- textos flutuantes
        for (int i = pops.size() - 1; i >= 0; i--) {
            Pop p = pops.get(i);
            float a = (now - p.t0) / 1000f;
            if (a > 1f) { pops.remove(i); continue; }
            float px = p.onFoe ? w * 0.62f : w * 0.33f, py = (p.onFoe ? h * 0.24f : h * 0.50f) - a * 46 - p.slot * 22;
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

    private void drawBackdrop(Canvas canvas, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(14, 20, 34));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setColor(Color.rgb(22, 32, 52));
        canvas.drawRect(0, h * 0.10f, w, h * 0.72f, paint);
        // plataformas
        paint.setColor(Color.argb(150, 70, 90, 130));
        canvas.drawOval(new RectF(w * 0.62f - 150, h * 0.30f, w * 0.62f + 150, h * 0.30f + 54), paint);
        canvas.drawOval(new RectF(w * 0.33f - 170, h * 0.58f, w * 0.33f + 170, h * 0.58f + 62), paint);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3); paint.setColor(Color.argb(180, 255, 196, 64));
        canvas.drawOval(new RectF(w * 0.62f - 150, h * 0.30f, w * 0.62f + 150, h * 0.30f + 54), paint);
        canvas.drawOval(new RectF(w * 0.33f - 170, h * 0.58f, w * 0.33f + 170, h * 0.58f + 62), paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawTopBar(Canvas canvas, float w, long now) {
        paint.setColor(Color.argb(255, 18, 26, 42));
        canvas.drawRect(0, 0, w, 64, paint);
        paint.setColor(C_ACCENT);
        canvas.drawRect(0, 62, w, 66, paint);
        text(canvas, "🏟️ ARENA  •  " + (fighter != null ? fighter.icon + " " + fighter.name : ""), 24, 42, 26, Color.WHITE, Paint.Align.LEFT, true);
        if (duel.phase == ArenaDuel.Phase.FIGHT || duel.phase == ArenaDuel.Phase.RESULT) {
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

    private void drawFoe(Canvas canvas, float w, float h, float size, int frame, long now) {
        ArenaDuel.Unit u = duel.foe.cur();
        infoBox(canvas, new RectF(w * 0.04f, h * 0.13f, w * 0.40f, h * 0.13f + 92), u, duel.foe.name, Color.rgb(230, 90, 90), duel.foe);
        if (u != null && u.alive()) {
            float lunge = Math.max(0, 1 - (now - foeLunge) / 220f) * 26;
            float shake = duel.foe.hurtFlash > 0 ? (float) Math.sin(now / 22.0) * 7 : 0;
            ArenaUI.sprite(canvas, paint, u.species, u.shiny, 0, frame, w * 0.62f + shake, h * 0.30f + 34 + lunge, size);
        }
        // time do oponente (bolinhas com vida)
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

    private void drawMe(Canvas canvas, float w, float h, float size, int frame, long now) {
        ArenaDuel.Unit u = duel.me.cur();
        infoBox(canvas, new RectF(w * 0.60f, h * 0.47f, w * 0.96f, h * 0.47f + 92), u, duel.me.name, C_GOOD, duel.me);
        if (u != null && u.alive()) {
            float lunge = Math.max(0, 1 - (now - meLunge) / 220f) * 26;
            float shake = duel.me.hurtFlash > 0 ? (float) Math.sin(now / 22.0) * 7 : 0;
            ArenaUI.sprite(canvas, paint, u.species, u.shiny, 2, frame, w * 0.33f + shake, h * 0.58f + 40 - lunge, size * 1.15f);
        }
    }

    private void drawBottom(Canvas canvas, float w, float h, long now) {
        float top = h * 0.745f, bottom = h - 14;
        ArenaDuel.Phase ph = duel.phase;
        if (ph == ArenaDuel.Phase.PREP) {
            text(canvas, "Escolha até 3 Pokémon. Toque de novo para definir o INICIAL (1º) ou remover.", 30, top - 6, 18, Color.WHITE, Paint.Align.LEFT, true);
            readyBtn.set(w - 250, top + 6, w - 30, bottom);
            boolean can = !picks.isEmpty();
            actionButton(canvas, readyBtn, "✅ PRONTO!", can, C_GOOD);
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
