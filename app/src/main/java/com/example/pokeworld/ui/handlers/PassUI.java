package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.PassManager;
import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.RewardManager;

import java.util.ArrayList;
import java.util.List;

/** Passe da Temporada: níveis (trilha grátis e premium) e missões do passe. */
public class PassUI extends BaseUI {
    public static final class Cell { public final RectF r = new RectF(); public int a, b; public boolean flag; }

    public final RectF exitBtn = new RectF(), buyBtn = new RectF(), claimAllBtn = new RectF(), shopBtn = new RectF();
    public final RectF[] tabs = {new RectF(), new RectF(), new RectF(), new RectF()};
    public int page = 0;                                   // 0..2 níveis, 3 missões
    public final List<Cell> levelCells = new ArrayList<>();    // a = nível, flag = trilha premium
    public final List<Cell> missionCells = new ArrayList<>();  // a = grupo (0 diária, 1 temporada), b = índice

    private static final int GOLD = Color.rgb(255, 214, 90);

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        RewardManager.Store s = PointsManager.store();
        PassManager.ensure(s);
        int level = PassManager.level(s), xp = PassManager.xp(s);
        boolean prem = PassManager.premium(s);
        int ready = PassManager.readyCount(s);
        drawHeader(canvas, "🎟️ PASSE DA TEMPORADA", "Faltam " + PassManager.daysLeft() + " dias  •  complete missões, ganhe XP e reivindique cada nível", exitBtn);

        // ---- linha de progresso e botões
        float y = 116, bh = 56, gap = 10;
        shopBtn.set(w - 30 - 190, y, w - 30, y + bh);
        claimAllBtn.set(shopBtn.left - gap - 250, y, shopBtn.left - gap, y + bh);
        buyBtn.set(claimAllBtn.left - gap - 300, y, claimAllBtn.left - gap, y + bh);
        actionButton(canvas, shopBtn, "💠 LOJA", true, Color.rgb(120, 190, 255));
        pillButton(canvas, claimAllBtn, ready > 0 ? "🎁 COLETAR TUDO (" + ready + ")" : "Nada a coletar", ready > 0 ? C_GOOD : Color.rgb(70, 78, 92), 22);
        if (prem) pillButton(canvas, buyBtn, "⭐ PASSE PREMIUM ATIVO", Color.rgb(120, 100, 40), 21);
        else pillButton(canvas, buyBtn, "⭐ COMPRAR PASSE — " + PointsManager.PRICE_PASS + " pts", C_ACCENT, 22);

        RectF lb = new RectF(30, y + 6, buyBtn.left - 24, y + 6 + 22);
        text(canvas, "Nível " + level + " / " + PassManager.LEVELS + (level < PassManager.LEVELS ? "   •   " + (xp % PassManager.XP_PER_LEVEL) + "/" + PassManager.XP_PER_LEVEL + " XP" : "   •   MÁXIMO"), lb.left, lb.top + 17, 22, Color.WHITE, Paint.Align.LEFT, true);
        bar(canvas, new RectF(lb.left, y + 34, lb.right, y + 50), level >= PassManager.LEVELS ? 1f : (xp % PassManager.XP_PER_LEVEL) / (float) PassManager.XP_PER_LEVEL, C_ACCENT);

        // ---- abas
        float ty = 186, tw = Math.min(260, (w - 60 - 3 * gap) / 4f);
        String[] names = {"Níveis 1–10", "Níveis 11–20", "Níveis 21–30", "📋 Missões" + (PassManager.missionsReady(s) > 0 ? " (" + PassManager.missionsReady(s) + ")" : "")};
        for (int i = 0; i < 4; i++) {
            tabs[i].set(30 + i * (tw + gap), ty, 30 + i * (tw + gap) + tw, ty + 44);
            chip(canvas, tabs[i], names[i], page == i, C_ACCENT);
        }

        levelCells.clear(); missionCells.clear();
        float top = ty + 44 + 14;
        if (page < 3) drawLevels(canvas, gv, s, w, h, top, level, prem);
        else drawMissions(canvas, s, w, h, top);
    }

    private void drawLevels(Canvas canvas, GameView gv, RewardManager.Store s, float w, float h, float top, int level, boolean prem) {
        float gap = 8, cw = (w - 60 - 9 * gap) / 10f;
        float ch = Math.max(100, Math.min(170, (h - top - 70) / 2f));
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 260.0);
        float rowFree = top + 22, rowPrem = rowFree + ch + 30;
        text(canvas, "GRÁTIS", 30, top + 14, 16, C_MUTED, Paint.Align.LEFT, true);
        text(canvas, "⭐ PREMIUM" + (prem ? "" : "  (compre o passe para liberar)"), 30, rowFree + ch + 20, 16, GOLD, Paint.Align.LEFT, true);
        for (int i = 0; i < 10; i++) {
            int lv = page * 10 + i + 1;
            float x = 30 + i * (cw + gap);
            drawCell(canvas, s, x, rowFree, cw, ch, lv, false, level, prem, pulse);
            drawCell(canvas, s, x, rowPrem, cw, ch, lv, true, level, prem, pulse);
        }
    }

    private void drawCell(Canvas canvas, RewardManager.Store s, float x, float y, float cw, float ch, int lv, boolean premium, int level, boolean hasPass, float pulse) {
        PassManager.Reward rw = premium ? PassManager.premiumReward(lv) : PassManager.freeReward(lv);
        boolean reached = level >= lv;
        boolean claimed = premium ? PassManager.premClaimed(s, lv) : PassManager.freeClaimed(s, lv);
        boolean locked = premium && !hasPass;
        boolean ready = reached && !claimed && !locked;
        Cell c = new Cell(); c.r.set(x, y, x + cw, y + ch); c.a = lv; c.flag = premium; levelCells.add(c);

        int stroke = ready ? Color.argb((int) (170 + 85 * pulse), 255, 205, 80) : claimed ? C_GOOD : premium ? Color.argb(140, 255, 214, 90) : C_LINE;
        int fill = ready ? Color.argb(240, 70, 54, 16) : premium ? Color.argb(235, 40, 34, 22) : C_PANEL;
        panel(canvas, c.r, fill, stroke, ready ? 4 : 2);
        boolean skin = rw.skinId != null;
        text(canvas, "Nv " + lv, x + 8, y + 18, 13, reached ? Color.WHITE : C_MUTED, Paint.Align.LEFT, true);
        text(canvas, rw.icon(), x + cw / 2f, y + ch * 0.38f, Math.min(40, cw * 0.4f), Color.WHITE, Paint.Align.CENTER, false);
        int tc = (reached && !locked) ? (skin ? Color.rgb(150, 230, 255) : Color.rgb(255, 224, 140)) : C_MUTED;
        wrap(canvas, rw.text(), x + 6, y + ch * 0.38f + 26, cw - 12, 13, tc, 16);
        String st = claimed ? "✔ PEGO" : locked ? "🔒 bloqueado" : "🔒 Nv " + lv;
        if (ready) pillButton(canvas, new RectF(x + 6, y + ch - 36, x + cw - 6, y + ch - 8), "PEGAR", C_ACCENT, 15);
        else text(canvas, st, x + cw / 2f, y + ch - 14, 15, claimed ? C_GOOD : C_MUTED, Paint.Align.CENTER, claimed);
    }

    private void drawMissions(Canvas canvas, RewardManager.Store s, float w, float h, float top) {
        float gap = 10;
        text(canvas, "MISSÕES DE HOJE  (renovam todo dia)", 30, top + 14, 17, GOLD, Paint.Align.LEFT, true);
        float dw = (w - 60 - 2 * gap) / 3f, dh = 84, dy = top + 24;
        for (int i = 0; i < PassManager.DAILY_SLOTS; i++) {
            PassManager.Mission q = PassManager.daily(i);
            card(canvas, new RectF(30 + i * (dw + gap), dy, 30 + i * (dw + gap) + dw, dy + dh), q, PassManager.dailyProgress(s, i), PassManager.dailyClaimed(s, i), PassManager.dailyReady(s, i), 0, i);
        }
        float sy = dy + dh + 16;
        text(canvas, "MISSÕES DA TEMPORADA  (valem até o fim do mês)", 30, sy + 14, 17, GOLD, Paint.Align.LEFT, true);
        float cw = (w - 60 - gap) / 2f;
        float rh = Math.max(56, Math.min(78, (h - sy - 30 - 16 - 4 * gap) / 5f));
        for (int i = 0; i < PassManager.SEASON.length; i++) {
            int col = i % 2, row = i / 2;
            float x = 30 + col * (cw + gap), y = sy + 26 + row * (rh + gap);
            card(canvas, new RectF(x, y, x + cw, y + rh), PassManager.SEASON[i], PassManager.seasonProgress(s, i), PassManager.seasonClaimed(s, i), PassManager.seasonReady(s, i), 1, i);
        }
    }

    private void card(Canvas canvas, RectF r, PassManager.Mission q, int prog, boolean claimed, boolean ready, int group, int idx) {
        Cell c = new Cell(); c.r.set(r); c.a = group; c.b = idx; c.flag = ready; missionCells.add(c);
        panel(canvas, r, ready ? Color.argb(240, 70, 54, 16) : C_PANEL, ready ? C_ACCENT : claimed ? C_GOOD : C_LINE, ready ? 3 : 2);
        float btnW = 130;
        paint.setTextSize(18);
        text(canvas, fit(q.title, r.width() - btnW - 32), r.left + 14, r.top + 26, 18, claimed ? C_MUTED : Color.WHITE, Paint.Align.LEFT, true);
        float bx = r.left + 14, bw = r.width() - btnW - 40;
        bar(canvas, new RectF(bx, r.bottom - 24, bx + bw, r.bottom - 12), Math.min(1f, prog / (float) q.target), claimed ? C_GOOD : C_ACCENT);
        RectF b = new RectF(r.right - btnW - 10, r.centerY() - 20, r.right - 10, r.centerY() + 20);
        if (claimed) text(canvas, "✔ Recebida", b.centerX(), b.centerY() + 6, 16, C_GOOD, Paint.Align.CENTER, true);
        else if (ready) pillButton(canvas, b, "PEGAR +" + q.xp, C_ACCENT, 16);
        else {
            text(canvas, Math.min(prog, q.target) + " / " + q.target, b.centerX(), b.centerY() - 2, 18, Color.WHITE, Paint.Align.CENTER, true);
            text(canvas, "+" + q.xp + " XP", b.centerX(), b.centerY() + 18, 14, C_MUTED, Paint.Align.CENTER, false);
        }
    }
}
