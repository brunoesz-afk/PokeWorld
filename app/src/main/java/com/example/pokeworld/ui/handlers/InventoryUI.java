package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.ItemCatalog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Mochila: abas por categoria, grade rolável à esquerda e painel de detalhes (com botão de uso) à direita. */
public class InventoryUI extends BaseUI {
    public RectF exitBtn = new RectF();
    public RectF useBtn = new RectF();
    public RectF gridArea = new RectF();
    public List<RectF> tabRects = new ArrayList<>();
    public List<Cell> cells = new ArrayList<>();

    /** -1 = Tudo, senão o ordinal da categoria. */
    public int tab = -1;
    public String selected = null;
    public float scroll = 0, maxScroll = 0;

    public static class Cell {
        public final String name;
        public final RectF rect = new RectF();
        Cell(String n) { name = n; }
    }

    /** Itens da aba atual, ordenados por categoria e nome. */
    public List<String> visibleItems(Map<String, Integer> inv) {
        List<String> out = new ArrayList<>();
        for (String n : inv.keySet()) {
            if (inv.get(n) <= 0) continue;
            if (tab == -1 || ItemCatalog.get(n).category.ordinal() == tab) out.add(n);
        }
        Collections.sort(out, (a, b) -> {
            int ca = ItemCatalog.get(a).category.ordinal(), cb = ItemCatalog.get(b).category.ordinal();
            return ca != cb ? Integer.compare(ca, cb) : a.compareTo(b);
        });
        return out;
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        Map<String, Integer> inv = gv.player.getInventory();

        int total = 0;
        for (int q : inv.values()) total += q;
        drawHeader(canvas, "MOCHILA", total + " itens  •  🪙 " + gv.player.getGold() + "  •  💎 " + gv.player.getGems(), exitBtn);

        // --- Abas ---
        ItemCatalog.Category[] cats = ItemCatalog.Category.values();
        int[] counts = new int[cats.length];
        for (String n : inv.keySet()) counts[ItemCatalog.get(n).category.ordinal()]++;
        tabRects.clear();
        float tx = 30, ty = 118, th = 44, tw = Math.min(190, (w * 0.62f - 30 - 6 * 8) / 6f);
        for (int i = -1; i < cats.length; i++) {
            RectF r = new RectF(tx, ty, tx + tw, ty + th);
            tabRects.add(r);
            String label = i == -1 ? "Tudo" : cats[i].label + (counts[i] > 0 ? " " + counts[i] : "");
            chip(canvas, r, label, tab == i, i == -1 ? C_ACCENT : cats[i].color | 0xFF000000);
            tx += tw + 8;
        }

        // --- Grade ---
        gridArea.set(30, 176, w * 0.62f, h - 24);
        panel(canvas, gridArea, Color.argb(120, 10, 16, 28), C_LINE, 2);

        List<String> items = visibleItems(inv);
        if (selected != null && !items.contains(selected)) selected = null;
        if (selected == null && !items.isEmpty()) selected = items.get(0);

        cells.clear();
        if (items.isEmpty()) {
            text(canvas, "Nada por aqui ainda", gridArea.centerX(), gridArea.centerY(), 26, C_MUTED, Paint.Align.CENTER, false);
        } else {
            int cols = w > 1500 ? 5 : 4;
            float pad = 12, gap = 10;
            float cw = (gridArea.width() - pad * 2 - gap * (cols - 1)) / cols;
            float ch = cw * 0.9f;
            int rows = (items.size() + cols - 1) / cols;
            float content = pad * 2 + rows * ch + (rows - 1) * gap;
            maxScroll = Math.max(0, content - gridArea.height());
            scroll = Math.max(0, Math.min(maxScroll, scroll));

            canvas.save();
            canvas.clipRect(gridArea);
            for (int i = 0; i < items.size(); i++) {
                String name = items.get(i);
                ItemCatalog.Info info = ItemCatalog.get(name);
                float x = gridArea.left + pad + (i % cols) * (cw + gap);
                float y = gridArea.top + pad + (i / cols) * (ch + gap) - scroll;
                Cell cell = new Cell(name);
                cell.rect.set(x, y, x + cw, y + ch);
                cells.add(cell);
                if (cell.rect.bottom < gridArea.top || cell.rect.top > gridArea.bottom) continue;

                boolean sel = name.equals(selected);
                panel(canvas, cell.rect, sel ? C_PANEL_HI : C_PANEL, sel ? C_ACCENT : (info.category.color | 0xFF000000), sel ? 4 : 2);
                text(canvas, info.icon, cell.rect.centerX(), cell.rect.top + ch * 0.55f, Math.min(52, ch * 0.42f), Color.WHITE, Paint.Align.CENTER, false);
                paint.setTextSize(16);
                text(canvas, fit(name, cw - 14), cell.rect.centerX(), cell.rect.bottom - 14, 16, Color.WHITE, Paint.Align.CENTER, false);
                // selo de quantidade
                String q = "x" + inv.get(name);
                paint.setTextSize(17);
                float qw = paint.measureText(q) + 16;
                RectF badge = new RectF(cell.rect.right - qw - 6, cell.rect.top + 6, cell.rect.right - 6, cell.rect.top + 30);
                panel(canvas, badge, Color.argb(220, 0, 0, 0), C_ACCENT, 2);
                text(canvas, q, badge.centerX(), badge.centerY() + 6, 17, C_ACCENT, Paint.Align.CENTER, true);
            }
            canvas.restore();
            scrollbar(canvas, gridArea, scroll, maxScroll);
        }

        // --- Painel de detalhes ---
        RectF det = new RectF(gridArea.right + 18, 176, w - 30, h - 24);
        panel(canvas, det, C_PANEL, C_LINE, 2);
        useBtn.setEmpty();
        if (selected == null) {
            text(canvas, "Mochila vazia", det.centerX(), det.centerY(), 26, C_MUTED, Paint.Align.CENTER, false);
            return;
        }
        ItemCatalog.Info info = ItemCatalog.get(selected);
        float cx = det.centerX();
        text(canvas, info.icon, cx, det.top + 100, 80, Color.WHITE, Paint.Align.CENTER, false);
        paint.setTextSize(30);
        text(canvas, fit(selected, det.width() - 30), cx, det.top + 150, 30, Color.WHITE, Paint.Align.CENTER, true);
        RectF cat = new RectF(cx - 80, det.top + 164, cx + 80, det.top + 194);
        panel(canvas, cat, info.category.color | 0xFF000000, Color.WHITE, 2);
        text(canvas, info.category.label, cat.centerX(), cat.centerY() + 6, 17, Color.BLACK, Paint.Align.CENTER, true);
        text(canvas, "Você tem: " + inv.get(selected), cx, det.top + 224, 20, C_ACCENT, Paint.Align.CENTER, true);
        wrap(canvas, info.desc, det.left + 20, det.top + 262, det.width() - 40, 19, Color.rgb(215, 224, 240), 26);

        useBtn.set(det.left + 24, det.bottom - 84, det.right - 24, det.bottom - 24);
        actionButton(canvas, useBtn, info.useLabel != null ? info.useLabel : "SEM USO DIRETO", info.useLabel != null, C_GOOD);
    }
}
