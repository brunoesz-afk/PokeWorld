package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.AchievementManager;

import java.util.ArrayList;
import java.util.List;

/** Conquistas: filtros, lista rolável em 2 colunas com barra de progresso e recompensa. */
public class AchievementUI extends BaseUI {
    public static final String[] FILTERS = {"Todas", "Em andamento", "Concluídas"};

    public RectF exitBtn = new RectF();
    public RectF listArea = new RectF();
    public List<RectF> filterRects = new ArrayList<>();
    public int filter = 0;
    public float scroll = 0, maxScroll = 0;

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        AchievementManager m = gv.achievements;
        int total = AchievementManager.ALL.size();
        drawHeader(canvas, "CONQUISTAS", "Concluídas: " + m.unlockedCount() + " / " + total + "   •   cada conquista dá uma recompensa", exitBtn);

        filterRects.clear();
        float fx = 30, fw = Math.min(180, (w * 0.6f - 30 - 2 * 8) / 3f);
        for (int i = 0; i < FILTERS.length; i++) {
            RectF r = new RectF(fx, 118, fx + fw, 160);
            filterRects.add(r);
            chip(canvas, r, FILTERS[i], filter == i, C_ACCENT);
            fx += fw + 8;
        }
        RectF pb = new RectF(fx + 16, 130, w - 30, 148);
        bar(canvas, pb, m.unlockedCount() / (float) total, C_ACCENT);

        listArea.set(30, 172, w - 30, h - 24);
        panel(canvas, listArea, Color.argb(120, 10, 16, 28), C_LINE, 2);

        List<AchievementManager.Def> shown = new ArrayList<>();
        for (AchievementManager.Def d : AchievementManager.ALL) {
            boolean done = m.isUnlocked(d);
            if (filter == 1 && done) continue;
            if (filter == 2 && !done) continue;
            shown.add(d);
        }
        // as concluídas por último na visão "Todas"
        if (filter == 0) {
            List<AchievementManager.Def> sorted = new ArrayList<>();
            for (AchievementManager.Def d : shown) if (!m.isUnlocked(d)) sorted.add(d);
            for (AchievementManager.Def d : shown) if (m.isUnlocked(d)) sorted.add(d);
            shown = sorted;
        }
        if (shown.isEmpty()) {
            text(canvas, "Nada por aqui ainda", listArea.centerX(), listArea.centerY(), 22, C_MUTED, Paint.Align.CENTER, false);
            return;
        }

        float pad = 14, gap = 12;
        int cols = w > 1000 ? 2 : 1;
        float cw = (listArea.width() - pad * 2 - gap * (cols - 1)) / cols;
        float ch = 112;
        int rows = (shown.size() + cols - 1) / cols;
        maxScroll = Math.max(0, pad * 2 + rows * ch + (rows - 1) * gap - listArea.height());
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        canvas.save();
        canvas.clipRect(listArea);
        for (int i = 0; i < shown.size(); i++) {
            AchievementManager.Def d = shown.get(i);
            boolean done = m.isUnlocked(d);
            int cur = Math.min(d.target, m.progress(d, gv));
            float x = listArea.left + pad + (i % cols) * (cw + gap);
            float y = listArea.top + pad + (i / cols) * (ch + gap) - scroll;
            RectF r = new RectF(x, y, x + cw, y + ch);
            if (r.bottom < listArea.top || r.top > listArea.bottom) continue;

            panel(canvas, r, done ? Color.rgb(64, 56, 24) : C_PANEL, done ? Color.rgb(255, 215, 0) : C_LINE, done ? 3 : 2);
            text(canvas, done ? d.icon : "🔒", r.left + 44, r.centerY() + 16, 46, Color.WHITE, Paint.Align.CENTER, false);
            float tx = r.left + 88;
            paint.setTextSize(21);
            text(canvas, fit(d.title, cw - 88 - 120), tx, r.top + 32, 21, done ? Color.rgb(255, 225, 120) : Color.WHITE, Paint.Align.LEFT, true);
            paint.setTextSize(15);
            text(canvas, fit(d.desc, cw - 100), tx, r.top + 56, 15, C_MUTED, Paint.Align.LEFT, false);
            text(canvas, done ? "✔ Concluída" : d.rewardText(), r.right - 14, r.top + 32, 16, done ? C_GOOD : C_ACCENT, Paint.Align.RIGHT, true);
            RectF pbar = new RectF(tx, r.bottom - 34, r.right - 80, r.bottom - 20);
            bar(canvas, pbar, cur / (float) d.target, done ? Color.rgb(255, 215, 0) : C_GOOD);
            text(canvas, cur + " / " + d.target, r.right - 14, r.bottom - 19, 15, Color.WHITE, Paint.Align.RIGHT, false);
            text(canvas, d.group, tx, r.bottom - 8, 11, Color.rgb(110, 120, 140), Paint.Align.LEFT, false);
        }
        canvas.restore();
        scrollbar(canvas, listArea, scroll, maxScroll);
    }
}
