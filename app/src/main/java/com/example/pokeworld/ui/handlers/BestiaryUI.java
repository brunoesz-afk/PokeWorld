package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.EvolutionTable;
import com.example.pokeworld.managers.BestiaryManager;
import com.example.pokeworld.managers.SpriteManager;

import java.util.ArrayList;
import java.util.List;

/** Pokédex regional: grade rolável com silhuetas dos não vistos, filtros e ficha da espécie selecionada. */
public class BestiaryUI extends BaseUI {
    public static final String[] FILTERS = {"Todos", "Vistos", "Capturados", "Faltam"};

    public RectF exitBtn = new RectF();
    public RectF gridArea = new RectF();
    public List<RectF> filterRects = new ArrayList<>();
    public List<Cell> cells = new ArrayList<>();
    public int filter = 0;
    public int selected = 0;        // índice em BestiaryManager.POKEDEX
    public float scroll = 0, maxScroll = 0;

    private final Paint silhouette = new Paint();
    private int cachedCountFor = -1, cachedCount = 0;

    public static class Cell {
        public final int index;
        public final RectF rect = new RectF();
        Cell(int i) { index = i; }
    }

    public boolean matches(BestiaryManager b, int i) {
        String s = BestiaryManager.POKEDEX[i];
        switch (filter) {
            case 1: return b.isSeen(s);
            case 2: return b.isCaptured(s);
            case 3: return !b.isCaptured(s);
            default: return true;
        }
    }

    private void drawSprite(Canvas canvas, String species, Rect dst, boolean seen, int frame) {
        Bitmap bmp = SpriteManager.getInstance().getUnitSprite(species, 0, frame, false);
        if (bmp == null) return;
        if (seen) {
            canvas.drawBitmap(bmp, null, dst, null);
        } else {
            silhouette.setColorFilter(new PorterDuffColorFilter(Color.rgb(8, 10, 16), PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bmp, null, dst, silhouette);
        }
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        BestiaryManager b = gv.bestiary;
        int total = BestiaryManager.POKEDEX.length;
        drawHeader(canvas, "POKÉDEX REGIONAL", "Vistos " + b.seenCount() + "/" + total + "   •   Capturados " + b.capturedCount() + "/" + total, exitBtn);

        // Filtros
        filterRects.clear();
        float fx = 30, fw = Math.min(150, (w * 0.62f - 30 - 3 * 8) / 4f);
        for (int i = 0; i < FILTERS.length; i++) {
            RectF r = new RectF(fx, 118, fx + fw, 160);
            filterRects.add(r);
            chip(canvas, r, FILTERS[i], filter == i, C_ACCENT);
            fx += fw + 8;
        }
        // Barra de progresso de captura
        RectF pb = new RectF(fx + 12, 130, w * 0.62f, 148);
        if (pb.width() > 60) {
            bar(canvas, pb, b.capturedCount() / (float) total, C_GOOD);
        }

        // Grade
        gridArea.set(30, 172, w * 0.62f, h - 24);
        panel(canvas, gridArea, Color.argb(120, 10, 16, 28), C_LINE, 2);
        List<Integer> shown = new ArrayList<>();
        for (int i = 0; i < total; i++) if (matches(b, i)) shown.add(i);

        cells.clear();
        if (shown.isEmpty()) {
            text(canvas, "Nenhuma espécie neste filtro", gridArea.centerX(), gridArea.centerY(), 22, C_MUTED, Paint.Align.CENTER, false);
        } else {
            float pad = 12, gap = 10;
            float cw = 116, ch = 128;
            int cols = Math.max(1, (int) ((gridArea.width() - pad * 2 + gap) / (cw + gap)));
            cw = (gridArea.width() - pad * 2 - gap * (cols - 1)) / cols;
            int rows = (shown.size() + cols - 1) / cols;
            maxScroll = Math.max(0, pad * 2 + rows * ch + (rows - 1) * gap - gridArea.height());
            scroll = Math.max(0, Math.min(maxScroll, scroll));
            int frame = (int) ((System.currentTimeMillis() / 700) % 3);

            canvas.save();
            canvas.clipRect(gridArea);
            for (int k = 0; k < shown.size(); k++) {
                int idx = shown.get(k);
                String s = BestiaryManager.POKEDEX[idx];
                float x = gridArea.left + pad + (k % cols) * (cw + gap);
                float y = gridArea.top + pad + (k / cols) * (ch + gap) - scroll;
                Cell c = new Cell(idx);
                c.rect.set(x, y, x + cw, y + ch);
                cells.add(c);
                if (c.rect.bottom < gridArea.top || c.rect.top > gridArea.bottom) continue;

                boolean seen = b.isSeen(s), cap = b.isCaptured(s), sel = idx == selected;
                panel(canvas, c.rect, sel ? C_PANEL_HI : C_PANEL, sel ? C_ACCENT : cap ? C_GOOD : seen ? C_LINE : Color.rgb(54, 60, 78), sel ? 4 : 2);
                int sz = (int) (cw - 28);
                drawSprite(canvas, s, new Rect((int) (c.rect.centerX() - sz / 2f), (int) c.rect.top + 8, (int) (c.rect.centerX() + sz / 2f), (int) c.rect.top + 8 + sz), seen, frame);
                text(canvas, String.format("Nº %03d", idx + 1), c.rect.left + 8, c.rect.top + 18, 12, C_MUTED, Paint.Align.LEFT, false);
                paint.setTextSize(15);
                text(canvas, fit(seen ? s : "???", cw - 10), c.rect.centerX(), c.rect.bottom - 10, 15, seen ? Color.WHITE : Color.rgb(100, 108, 126), Paint.Align.CENTER, seen);
                if (cap) text(canvas, "🔴", c.rect.right - 8, c.rect.top + 20, 16, Color.WHITE, Paint.Align.RIGHT, false);
            }
            canvas.restore();
            scrollbar(canvas, gridArea, scroll, maxScroll);
        }

        // Ficha
        RectF det = new RectF(gridArea.right + 18, 118, w - 30, h - 24);
        panel(canvas, det, C_PANEL, C_LINE, 2);
        String s = BestiaryManager.POKEDEX[Math.max(0, Math.min(total - 1, selected))];
        boolean seen = b.isSeen(s), cap = b.isCaptured(s);
        float cx = det.centerX();
        float sz = Math.min(det.width() * 0.5f, 170);
        drawSprite(canvas, s, new Rect((int) (cx - sz / 2), (int) det.top + 14, (int) (cx + sz / 2), (int) (det.top + 14 + sz)), seen, (int) ((System.currentTimeMillis() / 600) % 3));
        float y = det.top + sz + 48;
        text(canvas, seen ? s : "???", cx, y, 30, seen ? Color.WHITE : C_MUTED, Paint.Align.CENTER, true);
        text(canvas, String.format("Nº %03d", selected + 1), cx, y + 26, 16, C_MUTED, Paint.Align.CENTER, false);
        text(canvas, cap ? "🔴 Capturado" : seen ? "👁 Visto, ainda não capturado" : "Não encontrado", cx, y + 56, 19, cap ? C_GOOD : seen ? C_ACCENT : C_MUTED, Paint.Align.CENTER, true);

        if (seen) {
            y += 92;
            if (cachedCountFor != selected) { cachedCountFor = selected; cachedCount = gv.activity.dbHelper.getCaptureCount(s); }
            if (cap) { text(canvas, "Capturas registradas: " + cachedCount, det.left + 24, y, 17, Color.WHITE, Paint.Align.LEFT, false); y += 30; }
            List<String> hab = BestiaryManager.habitats(s);
            text(canvas, "ONDE APARECE", det.left + 24, y, 14, C_MUTED, Paint.Align.LEFT, true);
            y = wrap(canvas, hab.isEmpty() ? "Evento, chefe ou evolução." : join(hab), det.left + 24, y + 22, det.width() - 48, 17, Color.WHITE, 23);
            y += 10;
            text(canvas, "EVOLUÇÃO", det.left + 24, y, 14, C_MUTED, Paint.Align.LEFT, true);
            y += 24;
            String pre = EvolutionTable.preEvolution(s);
            if (pre != null) { text(canvas, "Vem de: " + pre, det.left + 24, y, 17, Color.WHITE, Paint.Align.LEFT, false); y += 24; }
            for (EvolutionTable.Evo e : EvolutionTable.from(s)) {
                if (y > det.bottom - 12) break;
                paint.setTextSize(17);
                text(canvas, fit(e.describe() + "  ➜  " + e.to, det.width() - 48), det.left + 24, y, 17, C_ACCENT, Paint.Align.LEFT, false);
                y += 24;
            }
            if (pre == null && EvolutionTable.from(s).isEmpty()) text(canvas, "Não evolui", det.left + 24, y, 17, Color.WHITE, Paint.Align.LEFT, false);
        } else {
            wrap(canvas, "Encontre esta espécie na natureza para registrar os dados dela.", det.left + 24, y + 96, det.width() - 48, 17, C_MUTED, 24);
        }
    }

    private static String join(List<String> l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < l.size(); i++) sb.append(i > 0 ? ", " : "").append(l.get(i));
        return sb.toString();
    }
}
