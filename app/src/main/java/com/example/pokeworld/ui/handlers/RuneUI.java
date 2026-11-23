package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.Rune;
import com.example.pokeworld.managers.SpriteManager;

import java.util.ArrayList;
import java.util.List;

/** Runas: à esquerda o companheiro com o espaço da runa; à direita a lista das 5 runas com efeito e quantidade. */
public class RuneUI extends BaseUI {
    public RectF exitBtn = new RectF();
    public RectF equipBtn = new RectF();
    public RectF removeBtn = new RectF();
    public List<RectF> rowRects = new ArrayList<>();
    public String selected = Rune.ALL[0];

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        Companion comp = gv.companion;
        drawHeader(canvas, "RUNAS", "A runa equipada vale para o companheiro ativo: " + comp.getSpecies(), exitBtn);

        // --- Painel do companheiro ---
        RectF left = new RectF(30, 124, w * 0.36f, h - 24);
        panel(canvas, left, C_PANEL, C_LINE, 2);
        Bitmap sp = SpriteManager.getInstance().getUnitSprite(comp.getSpecies(), 0, (int) ((System.currentTimeMillis() / 600) % 3), comp.isShiny());
        float sz = Math.min(left.width() * 0.5f, 170);
        if (sp != null) canvas.drawBitmap(sp, null, new Rect((int) (left.centerX() - sz / 2), (int) left.top + 20, (int) (left.centerX() + sz / 2), (int) (left.top + 20 + sz)), null);
        text(canvas, comp.getSpecies() + "  Nv." + comp.getLevel(), left.centerX(), left.top + sz + 56, 24, Color.WHITE, Paint.Align.CENTER, true);

        Rune eq = comp.getRune();
        RectF slot = new RectF(left.left + 24, left.top + sz + 80, left.right - 24, left.top + sz + 200);
        panel(canvas, slot, Color.argb(200, 12, 18, 30), eq != null ? (eq.getRarityColor() | 0xFF000000) : C_LINE, 3);
        if (eq == null) {
            text(canvas, "Sem runa equipada", slot.centerX(), slot.centerY() + 8, 20, C_MUTED, Paint.Align.CENTER, false);
        } else {
            text(canvas, Rune.icon(eq.getName()) + " " + eq.getName(), slot.centerX(), slot.top + 36, 22, Color.WHITE, Paint.Align.CENTER, true);
            wrap(canvas, eq.getDescription(), slot.left + 14, slot.top + 66, slot.width() - 28, 16, Color.rgb(200, 210, 228), 22);
        }
        removeBtn.set(left.left + 24, left.bottom - 76, left.right - 24, left.bottom - 20);
        actionButton(canvas, removeBtn, "DESEQUIPAR", eq != null, C_BAD);

        // --- Lista de runas ---
        RectF right = new RectF(left.right + 18, 124, w - 30, h - 24);
        panel(canvas, right, Color.argb(120, 10, 16, 28), C_LINE, 2);
        rowRects.clear();
        float pad = 14, gap = 10, rowH = Math.min(92, (right.height() - 110 - pad * 2 - gap * 4) / 5f);
        for (int i = 0; i < Rune.ALL.length; i++) {
            String name = Rune.ALL[i];
            Rune r = Rune.create(name);
            int qty = gv.player.getInventory().getOrDefault(name, 0);
            RectF row = new RectF(right.left + pad, right.top + pad + i * (rowH + gap), right.right - pad, right.top + pad + i * (rowH + gap) + rowH);
            rowRects.add(row);
            boolean sel = name.equals(selected);
            boolean worn = eq != null && eq.getName().equals(name);
            panel(canvas, row, sel ? C_PANEL_HI : C_PANEL, sel ? C_ACCENT : (qty > 0 || worn ? (r.getRarityColor() | 0xFF000000) : Color.rgb(60, 68, 86)), sel ? 4 : 2);
            text(canvas, Rune.icon(name), row.left + 44, row.centerY() + 14, 40, qty > 0 || worn ? Color.WHITE : Color.argb(110, 255, 255, 255), Paint.Align.CENTER, false);
            paint.setTextSize(22);
            text(canvas, fit(name, row.width() - 230), row.left + 86, row.top + rowH * 0.42f, 22, qty > 0 || worn ? Color.WHITE : C_MUTED, Paint.Align.LEFT, true);
            paint.setTextSize(15);
            text(canvas, fit(r.getDescription(), row.width() - 200), row.left + 86, row.top + rowH * 0.74f, 15, C_MUTED, Paint.Align.LEFT, false);
            String tag = worn ? "EQUIPADA" : qty > 0 ? "x" + qty : "não possui";
            text(canvas, tag, row.right - 16, row.centerY() + 6, 18, worn ? C_GOOD : qty > 0 ? C_ACCENT : Color.rgb(110, 118, 136), Paint.Align.RIGHT, true);
        }

        // --- Ação ---
        float by = right.top + pad + 5 * (rowH + gap);
        int have = gv.player.getInventory().getOrDefault(selected, 0);
        boolean already = eq != null && eq.getName().equals(selected);
        text(canvas, "Como obter: chefes, drops raros e prêmios da Torre (a cada 5 andares).", right.left + pad + 4, by + 22, 16, C_MUTED, Paint.Align.LEFT, false);
        equipBtn.set(right.left + pad, right.bottom - 70, right.right - pad, right.bottom - 14);
        actionButton(canvas, equipBtn, already ? "JÁ EQUIPADA" : have > 0 ? "EQUIPAR " + selected.toUpperCase() : "VOCÊ NÃO TEM ESTA RUNA", have > 0 && !already, C_GOOD);
    }
}
