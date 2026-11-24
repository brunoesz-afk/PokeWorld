package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.managers.SpriteManager;
import com.example.pokeworld.managers.TrainingManager;

import java.util.ArrayList;
import java.util.List;

/** Academia: vagas de treino no topo e, embaixo, os Pokémon da Caixa/Defesa que podem treinar. */
public class TrainingUI extends BaseUI {
    public final RectF exitBtn = new RectF(), trainBtn = new RectF();
    public final List<RectF> slotBtns = new ArrayList<>();
    public final List<RectF> candRects = new ArrayList<>();
    public final List<CapturedCreature> cands = new ArrayList<>();
    public CapturedCreature selected;

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        int slots = TrainingManager.slots(gv);
        drawHeader(canvas, "ACADEMIA — TREINO", slots == 0 ? "Construa a Academia na base para liberar vagas" : "Vagas: " + slots + "  •  treino de " + TrainingManager.MINUTES + " min rende XP (Pokémon da Caixa/Defesa)", exitBtn);
        slotBtns.clear(); candRects.clear(); cands.clear(); trainBtn.setEmpty();

        // --- vagas ---
        float y = 124, sh = 92, gap = 10;
        float sw = Math.min(260, (w - 60 - gap * 4) / 5f);
        for (int i = 0; i < 5; i++) {
            RectF r = new RectF(30 + i * (sw + gap), y, 30 + i * (sw + gap) + sw, y + sh);
            boolean open = i < slots;
            String sp = open ? TrainingManager.slotSpecies(gv, i) : null;
            panel(canvas, r, open ? C_PANEL : Color.argb(120, 20, 24, 34), sp != null ? C_ACCENT : C_LINE, sp != null ? 3 : 1);
            if (!open) { text(canvas, "🔒 vaga " + (i + 1), r.centerX(), r.centerY() + 6, 16, C_MUTED, Paint.Align.CENTER, false); slotBtns.add(new RectF()); continue; }
            if (sp == null) { text(canvas, "vaga livre", r.centerX(), r.centerY() + 6, 18, C_MUTED, Paint.Align.CENTER, false); slotBtns.add(new RectF()); continue; }
            Bitmap bmp = SpriteManager.getInstance().getUnitSprite(sp, 0, (int) ((System.currentTimeMillis() / 400) % 3), false);
            if (bmp != null) canvas.drawBitmap(bmp, null, new Rect((int) r.left + 6, (int) r.top + 6, (int) r.left + 6 + 56, (int) r.top + 62), null);
            text(canvas, fit(sp, r.width() - 70), r.left + 66, r.top + 30, 17, Color.WHITE, Paint.Align.LEFT, true);
            int left = TrainingManager.secondsLeft(gv, i);
            RectF b = new RectF(r.left + 8, r.bottom - 32, r.right - 8, r.bottom - 6);
            slotBtns.add(b);
            actionButton(canvas, b, left == 0 ? "COLETAR" : String.format("⏳ %d:%02d", left / 60, left % 60), left == 0, C_GOOD);
        }

        // --- candidatos ---
        RectF area = new RectF(30, y + sh + 14, w - 30, h - 24);
        panel(canvas, area, C_PANEL, C_LINE, 2);
        List<CapturedCreature> all = TrainingManager.candidates(gv);
        text(canvas, all.isEmpty() ? "Nenhum Pokémon livre. Mande Pokémon do time para a Caixa ou Defesa." : "Escolha quem vai treinar:", area.left + 20, area.top + 34, 22, Color.WHITE, Paint.Align.LEFT, true);
        float cw = 150, ch = 96, cg = 10;
        int cols = Math.max(1, (int) ((area.width() - 40 + cg) / (cw + cg)));
        int maxRows = Math.max(1, (int) ((area.height() - 60 - 70 + cg) / (ch + cg)));
        int shown = Math.min(all.size(), cols * maxRows);
        for (int i = 0; i < shown; i++) {
            CapturedCreature c = all.get(i);
            float x = area.left + 20 + (i % cols) * (cw + cg), yy = area.top + 52 + (i / cols) * (ch + cg);
            RectF r = new RectF(x, yy, x + cw, yy + ch);
            boolean sel = c == selected;
            panel(canvas, r, sel ? C_PANEL_HI : Color.argb(120, 10, 16, 28), sel ? Color.WHITE : (c.rarity.getColor() | 0xFF000000), sel ? 4 : 1);
            Bitmap bmp = SpriteManager.getInstance().getUnitSprite(c.name, 0, 0, c.isShiny);
            if (bmp != null) canvas.drawBitmap(bmp, null, new Rect((int) r.left + 6, (int) r.top + 6, (int) r.left + 6 + 56, (int) r.top + 62), null);
            text(canvas, fit(c.name, cw - 12), r.left + 8, r.bottom - 26, 15, Color.WHITE, Paint.Align.LEFT, true);
            text(canvas, "🪙 " + TrainingManager.cost(gv, c), r.left + 8, r.bottom - 8, 14, C_ACCENT, Paint.Align.LEFT, false);
            cands.add(c); candRects.add(r);
        }
        if (selected != null && !all.contains(selected)) selected = null;
        trainBtn.set(area.right - 340, area.bottom - 64, area.right - 20, area.bottom - 14);
        boolean can = selected != null && slots > 0;
        actionButton(canvas, trainBtn, selected == null ? "ESCOLHA UM POKÉMON" : "🏋️ TREINAR " + selected.name + " (" + TrainingManager.cost(gv, selected) + " 🪙)", can, C_GOOD);
        if (selected != null) text(canvas, "Rende ~" + TrainingManager.reward(gv, selected.name) + " XP", area.left + 20, area.bottom - 28, 18, C_MUTED, Paint.Align.LEFT, false);
    }
}
