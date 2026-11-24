package com.example.pokeworld.world;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Desenha as peças da base. As muralhas são montadas por 16 combinações de vizinhos
 * (N/L/S/O) para cada nível, então se ligam sozinhas formando linhas, cantos e cruzamentos.
 * Tudo é "assado" em Bitmaps pequenos e reaproveitado (nada é desenhado do zero a cada frame).
 */
public class BaseRenderer {
    private static final int T = 64; // = TileMap.TILE_SIZE

    /** Fornece sprites prontos (os prédios que já têm arte). */
    public interface SpriteSource { Bitmap get(String key); }

    private SpriteSource sprites;
    private final Map<Long, Bitmap> cache = new HashMap<>();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    public void setSprites(SpriteSource s) { this.sprites = s; }

    // ================================================================= desenho no mundo

    /** Desenha todas as peças visíveis (a lista já vem ordenada de cima para baixo). */
    public void draw(Canvas c, BaseLayout layout, long now, float vl, float vt, float vr, float vb) {
        List<BasePiece> list = layout.getPieces();
        for (int i = 0; i < list.size(); i++) {
            BasePiece pc = list.get(i);
            float x = pc.px(), y = pc.py();
            if (x + pc.pw() + 64 < vl || x - 64 > vr || y + pc.ph() + 96 < vt || y - 128 > vb) continue;
            boolean building = pc.finishTime > 0 && !pc.upgrading;
            if (building) {
                drawScaffold(c, pc.kind.w, pc.kind.h, pc.gx, pc.gy, 255);
            } else {
                int mask = pc.kind.isWall() ? layout.wallMask(pc.gx, pc.gy) : 0;
                drawPiece(c, pc.kind, pc.level, pc.gx, pc.gy, mask, 255, now);
            }
        }
    }

    /** Barras de progresso e selos de nível (por cima de tudo). */
    public void drawStatus(Canvas c, BaseLayout layout, long now, float vl, float vt, float vr, float vb, boolean showLevels) {
        List<BasePiece> list = layout.getPieces();
        for (int i = 0; i < list.size(); i++) {
            BasePiece pc = list.get(i);
            float x = pc.px(), y = pc.py();
            if (x + pc.pw() < vl || x > vr || y + pc.ph() < vt || y - 64 > vb) continue;
            if (pc.kind == BaseKind.HORTA && pc.isUsable()) drawCrop(c, pc, now);
            if (pc.kind == BaseKind.ESTUFA && pc.isUsable()) drawGreenhouseBadge(c, pc, now);
            if (pc.finishTime > now) drawProgress(c, pc, now);
            else if (showLevels && !pc.kind.isWall() && pc.kind != BaseKind.TOCHA) drawLevelBadge(c, pc);
        }
    }

    /** Plantação da horta: brotos crescendo e, quando pronta, arbusto com frutas balançando. */
    private void drawCrop(Canvas c, BasePiece pc, long now) {
        int stage = BaseManager.cropStage(pc, now);
        if (stage == 0) return;
        float total = BaseKind.cropSeconds(pc.level) * 1000f;
        float grow = stage == 2 ? 1f : Math.max(0f, Math.min(1f, 1f - (pc.cropReady - now) / total));
        float ox = pc.px(), oy = pc.py();
        for (int i = 0; i < 6; i++) {
            float cx = ox + 28 + (i % 3) * 36, cy = oy + 54 + (i / 3) * 40;
            if (stage == 1) {
                float h = 4 + grow * 16;
                line(c, cx, cy, cx, cy - h, 3f, rgb(70, 150, 60));
                disc(c, cx - 4 - grow * 3, cy - h, 3 + grow * 3, rgb(96, 190, 80));
                disc(c, cx + 4 + grow * 3, cy - h + 2, 3 + grow * 3, rgb(80, 170, 70));
            } else {
                float bob = (float) Math.sin(now / 260.0 + i) * 1.6f;
                disc(c, cx, cy - 12 + bob, 13, rgb(60, 140, 56));
                disc(c, cx - 5, cy - 16 + bob, 7, rgb(86, 178, 72));
                disc(c, cx + 4, cy - 10 + bob, 5.2f, rgb(222, 56, 52));
                disc(c, cx - 6, cy - 8 + bob, 4.6f, rgb(236, 74, 66));
                disc(c, cx + 5, cy - 18 + bob, 4.2f, rgb(222, 56, 52));
                disc(c, cx + 3, cy - 12 + bob, 1.4f, 0xAAFFFFFF);
            }
        }
        if (stage == 2) { // balãozinho "pronta!"
            float cx = (float) pc.centerX(), cy = oy - 6 + (float) Math.sin(now / 200.0) * 3f;
            disc(c, cx, cy - 14, 15, 0xE6FFFFFF);
            p.setColor(0xFFE53935); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(24); p.setFakeBoldText(true);
            c.drawText("!", cx, cy - 6, p); p.setFakeBoldText(false);
        }
    }

    /** Estufa: balão com a quantidade de unidades prontas (some quando vazia). */
    private void drawGreenhouseBadge(Canvas c, BasePiece pc, long now) {
        int n = BaseManager.greenhouseStock(pc, now);
        if (n <= 0) return;
        float cx = (float) pc.centerX(), cy = pc.py() - 6 + (float) Math.sin(now / 220.0) * 3f;
        disc(c, cx, cy - 14, 17, 0xE6FFFFFF);
        p.setColor(0xFF2E7D32); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(22); p.setFakeBoldText(true);
        c.drawText(String.valueOf(n), cx, cy - 6, p); p.setFakeBoldText(false);
    }

    private void drawProgress(Canvas c, BasePiece pc, long now) {
        long total = Math.max(1000L, pc.kind.seconds(pc.upgrading ? pc.level + 1 : 1) * 1000L);
        float frac = 1f - Math.max(0f, Math.min(1f, (pc.finishTime - now) / (float) total));
        float w = Math.max(96f, Math.min(pc.pw(), 160f));
        float cx = (float) pc.centerX();
        float top = pc.py() - 6;
        r.set(cx - w / 2, top - 22, cx + w / 2, top);
        p.setStyle(Paint.Style.FILL); p.setColor(0xCC101418);
        c.drawRoundRect(r, 8, 8, p);
        r.set(cx - w / 2 + 3, top - 19, cx - w / 2 + 3 + (w - 6) * frac, top - 3);
        p.setColor(pc.upgrading ? 0xFF4FC3F7 : 0xFFFFC107);
        c.drawRoundRect(r, 6, 6, p);
        int s = pc.remainingSeconds(now);
        String t = (pc.upgrading ? "⬆ " : "🏗 ") + (s >= 60 ? (s / 60) + "m " + (s % 60) + "s" : s + "s");
        p.setTextAlign(Paint.Align.CENTER); p.setTextSize(20); p.setFakeBoldText(true);
        p.setColor(0xFF000000); c.drawText(t, cx + 1, top - 26 + 1, p);
        p.setColor(0xFFFFFFFF); c.drawText(t, cx, top - 26, p);
        p.setFakeBoldText(false);
    }

    private void drawLevelBadge(Canvas c, BasePiece pc) {
        float x = pc.px() + 4, y = pc.py() + pc.ph() - 26;
        r.set(x, y, x + 62, y + 22);
        p.setStyle(Paint.Style.FILL); p.setColor(0xB0000000);
        c.drawRoundRect(r, 8, 8, p);
        p.setColor(0xFFFFD54F); p.setTextSize(17); p.setFakeBoldText(true); p.setTextAlign(Paint.Align.CENTER);
        c.drawText("Nv." + pc.level, x + 31, y + 17, p);
        p.setFakeBoldText(false);
    }

    // ================================================================= peças

    /** Desenha uma peça (ou um "fantasma" com alpha < 255) na posição da grade. */
    public void drawPiece(Canvas c, BaseKind k, int level, int gx, int gy, int mask, int alpha, long now) {
        Bitmap b;
        float dx, dy, dw, dh;
        if (k.sprite != null) {
            Bitmap sp = sprites == null ? null : sprites.get(k.sprite);
            if (sp == null) { drawScaffold(c, k.w, k.h, gx, gy, alpha); return; }
            float bw = k.w * T, bh = k.h * T;
            android.graphics.Rect src = contentBounds(k.sprite, sp);
            int sw = src.right - src.left, sh = src.bottom - src.top;
            float sc = Math.min(bw / sw, bh / sh);
            dw = sw * sc; dh = sh * sc;
            dx = gx * T + (bw - dw) / 2; dy = gy * T + bh - dh;
            shadow(c, gx * T + bw / 2, gy * T + bh - 6, dw * 0.46f, 12, alpha);
            p.setStyle(Paint.Style.FILL); p.setColor((alpha << 24) | 0xFFFFFF);
            r.set(dx, dy, dx + dw, dy + dh);
            c.drawBitmap(sp, src, r, p);
            return;
        }
        int frame = k == BaseKind.TOCHA ? (int) ((now / 140) % 3) : 0;
        b = bitmap(k, level, mask, frame);
        dw = b.getWidth(); dh = b.getHeight();
        dx = gx * T + (k.w * T - dw) / 2f;
        dy = (gy + k.h) * T - dh;
        p.setStyle(Paint.Style.FILL); p.setColor((alpha << 24) | 0xFFFFFF);
        c.drawBitmap(b, dx, dy, p);
    }

    private final Map<String, android.graphics.Rect> bounds = new HashMap<>();

    /** Área realmente desenhada do sprite (ignora a margem transparente). Calculada uma vez por sprite. */
    private android.graphics.Rect contentBounds(String key, Bitmap sp) {
        android.graphics.Rect rc = bounds.get(key);
        if (rc != null) return rc;
        int w = sp.getWidth(), h = sp.getHeight();
        int[] px = new int[w * h];
        sp.getPixels(px, 0, w, 0, 0, w, h);
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if ((px[y * w + x] >>> 24) > 24) {
                    if (x < minX) minX = x; if (x > maxX) maxX = x;
                    if (y < minY) minY = y; if (y > maxY) maxY = y;
                }
        rc = maxX < 0 ? new android.graphics.Rect(0, 0, w, h) : new android.graphics.Rect(minX, minY, maxX + 1, maxY + 1);
        bounds.put(key, rc);
        return rc;
    }

    private void shadow(Canvas c, float cx, float cy, float rx, float ry, int alpha) {
        p.setStyle(Paint.Style.FILL); p.setColor(((alpha * 70 / 255) << 24));
        r.set(cx - rx, cy - ry, cx + rx, cy + ry);
        c.drawOval(r, p);
    }

    private Bitmap bitmap(BaseKind k, int level, int mask, int frame) {
        long key = ((long) k.ordinal() << 40) | ((long) level << 32) | ((long) mask << 8) | frame;
        Bitmap b = cache.get(key);
        if (b == null) {
            switch (k) {
                case MURALHA: b = bakeWall(level, mask); break;
                case TORRE_VIGIA: b = bakeTower(level); break;
                case QUARTEL: b = bakeHall(level); break;
                case TOCHA: b = bakeTorch(frame); break;
                case BAU: b = bakeChest(level); break;
                case LABORATORIO: b = bakeLab(level); break;
                case HORTA: b = bakeGarden(level); break;
                case ESTUFA: b = bakeGreenhouse(level); break;
                case FONTE: b = bakeFountain(level); break;
                case ESTATUA: b = bakeStatue(level); break;
                case ARMADILHA: b = bakeTrap(level); break;
                case MERCADO_TROCAS: b = bakeTradeStall(level); break;
                default: b = Bitmap.createBitmap(T, T, Bitmap.Config.ARGB_8888);
            }
            cache.put(key, b);
        }
        return b;
    }

    public void drawScaffold(Canvas c, int tw, int th, int gx, int gy, int alpha) {
        long key = (1L << 60) | ((long) tw << 8) | th;
        Bitmap b = cache.get(key);
        if (b == null) { b = bakeScaffold(tw, th); cache.put(key, b); }
        p.setStyle(Paint.Style.FILL); p.setColor((alpha << 24) | 0xFFFFFF);
        c.drawBitmap(b, gx * T, gy * T, p);
    }

    // ================================================================= grade (modo construir)

    public void drawGrid(Canvas c, BaseLayout layout, float vl, float vt, float vr, float vb) {
        int x0 = Math.max(BaseLayout.PLOT_X, (int) Math.floor(vl / T)), x1 = Math.min(BaseLayout.PLOT_X + BaseLayout.PLOT_W, (int) Math.ceil(vr / T));
        int y0 = Math.max(BaseLayout.PLOT_Y, (int) Math.floor(vt / T)), y1 = Math.min(BaseLayout.PLOT_Y + BaseLayout.PLOT_H, (int) Math.ceil(vb / T));
        p.setStyle(Paint.Style.FILL);
        for (int y = y0; y < y1; y++)
            for (int x = x0; x < x1; x++) {
                if (((x + y) & 1) == 0) continue;
                p.setColor(0x14000000);
                c.drawRect(x * T, y * T, x * T + T, y * T + T, p);
            }
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.5f); p.setColor(0x38FFFFFF);
        for (int x = x0; x <= x1; x++) c.drawLine(x * T, y0 * T, x * T, y1 * T, p);
        for (int y = y0; y <= y1; y++) c.drawLine(x0 * T, y * T, x1 * T, y * T, p);
        p.setStrokeWidth(5f); p.setColor(0xCCFFD54F);
        c.drawRect(BaseLayout.PLOT_X * T, BaseLayout.PLOT_Y * T, (BaseLayout.PLOT_X + BaseLayout.PLOT_W) * T, (BaseLayout.PLOT_Y + BaseLayout.PLOT_H) * T, p);
        p.setStyle(Paint.Style.FILL);
    }

    /** Pegada verde/vermelha da peça (fantasma de posicionamento ou seleção). */
    public void drawFootprint(Canvas c, int gx, int gy, int w, int h, boolean ok, boolean outlineOnly) {
        r.set(gx * T, gy * T, (gx + w) * T, (gy + h) * T);
        if (!outlineOnly) {
            p.setStyle(Paint.Style.FILL); p.setColor(ok ? 0x6643D17A : 0x66E53935);
            c.drawRect(r, p);
        }
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4f); p.setColor(ok ? 0xFF69F0AE : 0xFFFF5252);
        c.drawRect(r, p);
        p.setStyle(Paint.Style.FILL);
    }

    /** Círculo de alcance (torres). */
    public void drawRange(Canvas c, float cx, float cy, float radius) {
        p.setStyle(Paint.Style.FILL); p.setColor(0x224FC3F7);
        c.drawCircle(cx, cy, radius, p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3f); p.setColor(0xAA4FC3F7);
        c.drawCircle(cx, cy, radius, p);
        p.setStyle(Paint.Style.FILL);
    }

    // ================================================================= arte procedural

    private static int rgb(int r, int g, int b) { return 0xFF000000 | (r << 16) | (g << 8) | b; }
    private static int shade(int col, float f) {
        int r = Math.min(255, Math.round(((col >> 16) & 255) * f)), g = Math.min(255, Math.round(((col >> 8) & 255) * f)), b = Math.min(255, Math.round((col & 255) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    private static int alpha(int col, int a) { return (a << 24) | (col & 0xFFFFFF); }

    private void rect(Canvas c, float x0, float y0, float x1, float y1, int col) {
        p.setStyle(Paint.Style.FILL); p.setColor(col);
        c.drawRect(x0, y0, x1, y1, p);
    }
    private void line(Canvas c, float x0, float y0, float x1, float y1, float w, int col) {
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(w); p.setColor(col);
        c.drawLine(x0, y0, x1, y1, p);
        p.setStyle(Paint.Style.FILL);
    }
    private void disc(Canvas c, float x, float y, float rad, int col) {
        p.setStyle(Paint.Style.FILL); p.setColor(col);
        c.drawCircle(x, y, rad, p);
    }

    // ---------------------------------------------------------------- muralha
    // top, face, outline, detail
    private static final int[][] WALL_PAL = {
            {rgb(172, 122, 66), rgb(114, 75, 38), rgb(56, 34, 14), rgb(214, 168, 104)},    // 1 madeira
            {rgb(178, 178, 174), rgb(120, 120, 126), rgb(56, 56, 64), rgb(220, 220, 214)},  // 2 pedra
            {rgb(112, 122, 136), rgb(66, 74, 88), rgb(26, 30, 40), rgb(196, 206, 220)},     // 3 ferro
            {rgb(98, 158, 208), rgb(52, 100, 152), rgb(18, 44, 82), rgb(196, 230, 252)},    // 4 aço
            {rgb(246, 206, 80), rgb(192, 142, 32), rgb(100, 62, 6), rgb(255, 242, 170)},    // 5 ouro
            {rgb(82, 58, 110), rgb(42, 28, 60), rgb(14, 8, 24), rgb(255, 132, 40)}          // 6 obsidiana
    };
    private static final int FACE = 14; // altura da face frontal

    private Bitmap bakeWall(int level, int mask) {
        level = Math.max(1, Math.min(6, level));
        int[] pal = WALL_PAL[level - 1];
        Bitmap b = Bitmap.createBitmap(T, T, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        // retângulos do topo: poste + braços
        float[][] tops = new float[5][];
        tops[0] = new float[]{16, 10, 48, 42};
        int n = 1;
        if ((mask & BaseLayout.N) != 0) tops[n++] = new float[]{22, 0, 42, 26};
        if ((mask & BaseLayout.S) != 0) tops[n++] = new float[]{22, 26, 42, 64};
        if ((mask & BaseLayout.W) != 0) tops[n++] = new float[]{0, 16, 32, 36};
        if ((mask & BaseLayout.E) != 0) tops[n++] = new float[]{32, 16, 64, 36};
        // sombra no chão
        p.setStyle(Paint.Style.FILL); p.setColor(0x30000000);
        for (int i = 0; i < n; i++) c.drawRect(tops[i][0], tops[i][1] + FACE, tops[i][2], tops[i][3] + FACE + 4, p);
        // camada da frente (face): contorno + preenchimento
        for (int i = 0; i < n; i++) rect(c, tops[i][0] - 1.5f, tops[i][1] + FACE - 1.5f, tops[i][2] + 1.5f, tops[i][3] + FACE + 1.5f, pal[2]);
        for (int i = 0; i < n; i++) rect(c, tops[i][0], tops[i][1] + FACE, tops[i][2], tops[i][3] + FACE, pal[1]);
        // detalhes da face
        for (int i = 0; i < n; i++) faceDetail(c, level, pal, tops[i]);
        // camada de cima: contorno + preenchimento
        for (int i = 0; i < n; i++) rect(c, tops[i][0] - 1.5f, tops[i][1] - 1.5f, tops[i][2] + 1.5f, tops[i][3] + 1.5f, pal[2]);
        for (int i = 0; i < n; i++) rect(c, tops[i][0], tops[i][1], tops[i][2], tops[i][3], pal[0]);
        for (int i = 1; i < n; i++) topDetail(c, level, pal, tops[i]);
        // poste: tampa mais clara + enfeite do nível
        float[] po = tops[0];
        rect(c, po[0] + 3, po[1] + 3, po[2] - 3, po[3] - 3, shade(pal[0], 1.12f));
        rect(c, po[0] + 3, po[1] + 3, po[2] - 3, po[1] + 6, alpha(pal[3], 150));
        postDetail(c, level, pal, po);
        return b;
    }

    private void faceDetail(Canvas c, int level, int[] pal, float[] t) {
        float x0 = t[0], x1 = t[2];
        float fy1 = t[3] + FACE;
        float fy0 = t[1] + FACE > t[3] ? t[1] + FACE : t[3]; // só a parte da face que aparece embaixo do topo
        if (fy1 <= fy0 + 1) return;
        switch (level) {
            case 1: // paliçada: troncos verticais
                for (float x = x0 + 4; x < x1 - 1; x += 6) line(c, x, fy0, x, fy1, 1.4f, shade(pal[1], 0.7f));
                break;
            case 2: // tijolos
                line(c, x0, fy0 + (fy1 - fy0) / 2, x1, fy0 + (fy1 - fy0) / 2, 1.4f, shade(pal[1], 0.72f));
                for (float x = x0 + 5; x < x1; x += 10) line(c, x, fy0, x, fy0 + (fy1 - fy0) / 2, 1.2f, shade(pal[1], 0.72f));
                for (float x = x0 + 10; x < x1; x += 10) line(c, x, fy0 + (fy1 - fy0) / 2, x, fy1, 1.2f, shade(pal[1], 0.72f));
                break;
            case 3: case 4: // chapas
                line(c, x0, fy0 + (fy1 - fy0) / 2, x1, fy0 + (fy1 - fy0) / 2, 1.3f, shade(pal[1], 0.65f));
                break;
            case 5:
                line(c, x0, fy0 + 2, x1, fy0 + 2, 1.5f, alpha(pal[3], 160));
                break;
            default: // rachaduras de magma
                line(c, x0 + 2, fy0 + 3, x0 + (x1 - x0) * 0.5f, fy1 - 2, 1.8f, pal[3]);
                line(c, x0 + (x1 - x0) * 0.5f, fy1 - 2, x1 - 2, fy0 + 2, 1.8f, pal[3]);
        }
    }

    private void topDetail(Canvas c, int level, int[] pal, float[] t) {
        boolean vertical = (t[2] - t[0]) < (t[3] - t[1]);
        switch (level) {
            case 1: // tábuas no sentido do braço
                if (vertical) { line(c, (t[0] + t[2]) / 2, t[1], (t[0] + t[2]) / 2, t[3], 1.3f, shade(pal[0], 0.78f)); }
                else { line(c, t[0], (t[1] + t[3]) / 2, t[2], (t[1] + t[3]) / 2, 1.3f, shade(pal[0], 0.78f)); }
                break;
            case 2:
                if (vertical) for (float y = t[1] + 6; y < t[3]; y += 10) line(c, t[0], y, t[2], y, 1.1f, shade(pal[0], 0.8f));
                else for (float x = t[0] + 6; x < t[2]; x += 10) line(c, x, t[1], x, t[3], 1.1f, shade(pal[0], 0.8f));
                break;
            case 5: // brilho
                if (vertical) line(c, t[0] + 3, t[1], t[0] + 3, t[3], 1.6f, alpha(pal[3], 190));
                else line(c, t[0], t[1] + 3, t[2], t[1] + 3, 1.6f, alpha(pal[3], 190));
                break;
            case 6:
                if (vertical) line(c, (t[0] + t[2]) / 2, t[1], (t[0] + t[2]) / 2, t[3], 1.6f, alpha(pal[3], 210));
                else line(c, t[0], (t[1] + t[3]) / 2, t[2], (t[1] + t[3]) / 2, 1.6f, alpha(pal[3], 210));
                break;
            default:
                if (vertical) line(c, t[0] + 3, t[1], t[0] + 3, t[3], 1.4f, alpha(pal[3], 120));
                else line(c, t[0], t[1] + 3, t[2], t[1] + 3, 1.4f, alpha(pal[3], 120));
        }
    }

    private void postDetail(Canvas c, int level, int[] pal, float[] po) {
        float cx = (po[0] + po[2]) / 2, cy = (po[1] + po[3]) / 2;
        switch (level) {
            case 1:
                for (float x = po[0] + 8; x < po[2] - 4; x += 6) line(c, x, po[1] + 4, x, po[3] - 3, 1.3f, shade(pal[0], 0.72f));
                break;
            case 2:
                line(c, po[0] + 4, cy, po[2] - 4, cy, 1.2f, shade(pal[0], 0.72f));
                line(c, cx, po[1] + 4, cx, cy, 1.2f, shade(pal[0], 0.72f));
                break;
            case 3: case 4:
                disc(c, po[0] + 7, po[1] + 8, 1.9f, pal[3]); disc(c, po[2] - 7, po[1] + 8, 1.9f, pal[3]);
                disc(c, po[0] + 7, po[3] - 7, 1.9f, pal[3]); disc(c, po[2] - 7, po[3] - 7, 1.9f, pal[3]);
                if (level == 4) disc(c, cx, cy, 4.5f, alpha(pal[3], 230));
                break;
            case 5:
                disc(c, cx, cy, 5.5f, rgb(214, 40, 60)); disc(c, cx - 1.5f, cy - 1.5f, 1.8f, rgb(255, 170, 180));
                break;
            default:
                disc(c, cx, cy, 6f, alpha(pal[3], 90)); disc(c, cx, cy, 3.6f, pal[3]);
        }
    }

    // ---------------------------------------------------------------- torre de vigia
    private static final int[] ROOF = {rgb(150, 100, 58), rgb(120, 126, 136), rgb(60, 120, 190), rgb(196, 56, 56), rgb(236, 190, 60)};

    private Bitmap bakeTower(int level) {
        level = Math.max(1, Math.min(5, level));
        Bitmap b = Bitmap.createBitmap(128, 176, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        shadow(c, 64, 164, 58, 11, 255);
        int stone = level >= 3 ? rgb(150, 156, 168) : rgb(176, 172, 164);
        // fuste de pedra
        rect(c, 28, 98, 100, 164, shade(stone, 0.82f));
        rect(c, 28, 98, 60, 164, stone);
        rect(c, 92, 98, 100, 164, shade(stone, 0.66f));
        for (int y = 108; y < 164; y += 11) line(c, 28, y, 100, y, 1.3f, shade(stone, 0.6f));
        for (int y = 108, k = 0; y < 164; y += 11, k++) for (int x = 28 + (k % 2) * 9; x < 100; x += 18) line(c, x, y, x, y + 11, 1.2f, shade(stone, 0.6f));
        // porta
        rect(c, 54, 134, 74, 164, rgb(70, 44, 22));
        p.setStyle(Paint.Style.FILL); p.setColor(rgb(70, 44, 22)); r.set(54, 124, 74, 144); c.drawOval(r, p);
        line(c, 64, 126, 64, 164, 1.4f, rgb(40, 24, 10));
        // plataforma de madeira
        rect(c, 20, 90, 108, 102, rgb(120, 82, 44));
        rect(c, 20, 90, 108, 94, rgb(160, 112, 62));
        // cabine
        rect(c, 36, 58, 92, 92, rgb(150, 104, 58));
        for (int x = 42; x < 92; x += 8) line(c, x, 58, x, 92, 1.2f, rgb(110, 74, 38));
        rect(c, 54, 68, 74, 84, rgb(30, 22, 18));
        line(c, 64, 68, 64, 84, 1.4f, rgb(150, 104, 58));
        // telhado cônico
        int roof = ROOF[level - 1];
        Path tri = new Path(); tri.moveTo(28, 62); tri.lineTo(100, 62); tri.lineTo(64, 14); tri.close();
        p.setStyle(Paint.Style.FILL); p.setColor(shade(roof, 0.8f)); c.drawPath(tri, p);
        Path half = new Path(); half.moveTo(28, 62); half.lineTo(64, 62); half.lineTo(64, 14); half.close();
        p.setColor(roof); c.drawPath(half, p);
        line(c, 28, 62, 100, 62, 2f, shade(roof, 0.55f));
        // bandeira
        line(c, 64, 14, 64, 2, 2f, rgb(70, 50, 30));
        Path fl = new Path(); fl.moveTo(64, 2); fl.lineTo(86, 7); fl.lineTo(64, 12); fl.close();
        p.setColor(level >= 3 ? rgb(230, 70, 70) : rgb(240, 200, 60)); c.drawPath(fl, p);
        // pontos de nível
        for (int i = 0; i < level; i++) disc(c, 64 + (i - (level - 1) / 2f) * 9, 172, 2.6f, rgb(255, 213, 79));
        return b;
    }

    // ---------------------------------------------------------------- quartel
    private static final int[] HALL_ROOF = {rgb(190, 70, 56), rgb(70, 120, 190), rgb(120, 70, 170), rgb(40, 150, 120), rgb(230, 186, 60)};

    private Bitmap bakeHall(int level) {
        level = Math.max(1, Math.min(5, level));
        Bitmap b = Bitmap.createBitmap(256, 320, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        shadow(c, 128, 306, 118, 16, 255);
        int stone = rgb(190, 184, 172);
        // corpo
        rect(c, 24, 160, 232, 300, shade(stone, 0.84f));
        rect(c, 24, 160, 90, 300, stone);
        rect(c, 200, 160, 232, 300, shade(stone, 0.68f));
        for (int y = 172; y < 300; y += 14) line(c, 24, y, 232, y, 1.3f, shade(stone, 0.62f));
        // base
        rect(c, 18, 286, 238, 302, shade(stone, 0.55f));
        // porta
        rect(c, 104, 218, 152, 300, rgb(92, 58, 30));
        p.setStyle(Paint.Style.FILL); p.setColor(rgb(92, 58, 30)); r.set(104, 196, 152, 240); c.drawOval(r, p);
        line(c, 128, 200, 128, 300, 2f, rgb(52, 32, 14));
        disc(c, 119, 262, 2.8f, rgb(236, 190, 60)); disc(c, 137, 262, 2.8f, rgb(236, 190, 60));
        // janelas
        for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 2; k++) {
            float wx = 128 + s * (58 + k * 40) - 9;
            rect(c, wx, 210, wx + 18, 244, rgb(40, 52, 74));
            rect(c, wx + 2, 212, wx + 16, 226, rgb(120, 170, 220));
            line(c, wx + 9, 210, wx + 9, 244, 1.3f, rgb(70, 50, 30));
        }
        // telhado
        int roof = HALL_ROOF[level - 1];
        Path big = new Path(); big.moveTo(8, 168); big.lineTo(248, 168); big.lineTo(204, 84); big.lineTo(52, 84); big.close();
        p.setStyle(Paint.Style.FILL); p.setColor(shade(roof, 0.82f)); c.drawPath(big, p);
        Path hi = new Path(); hi.moveTo(8, 168); hi.lineTo(128, 168); hi.lineTo(128, 84); hi.lineTo(52, 84); hi.close();
        p.setColor(roof); c.drawPath(hi, p);
        for (int y = 98; y < 168; y += 14) line(c, 16 + (y - 168) * -0.3f, y, 240 - (y - 168) * -0.3f, y, 1.5f, shade(roof, 0.58f));
        line(c, 8, 168, 248, 168, 3f, shade(roof, 0.5f));
        // chaminé
        rect(c, 170, 52, 192, 108, rgb(140, 120, 110)); rect(c, 166, 48, 196, 58, rgb(100, 86, 80));
        // torre central com bandeira
        rect(c, 108, 34, 148, 90, shade(stone, 0.9f)); rect(c, 108, 34, 124, 90, stone);
        Path cone = new Path(); cone.moveTo(100, 40); cone.lineTo(156, 40); cone.lineTo(128, 6); cone.close();
        p.setColor(roof); c.drawPath(cone, p);
        line(c, 128, 8, 128, 1, 2.4f, rgb(70, 50, 30));
        Path fl = new Path(); fl.moveTo(128, 1); fl.lineTo(158, 8); fl.lineTo(128, 15); fl.close();
        p.setColor(rgb(240, 200, 60)); c.drawPath(fl, p);
        for (int i = 0; i < level; i++) disc(c, 128 + (i - (level - 1) / 2f) * 12, 314, 3.4f, rgb(255, 213, 79));
        return b;
    }

    // ---------------------------------------------------------------- tocha
    private Bitmap bakeTorch(int frame) {
        Bitmap b = Bitmap.createBitmap(T, 96, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        shadow(c, 32, 90, 14, 4, 255);
        // brilho
        disc(c, 32, 38, 30, 0x16FFB74D); disc(c, 32, 38, 22, 0x1EFFB74D); disc(c, 32, 38, 14, 0x26FFD54F);
        rect(c, 29, 44, 35, 90, rgb(110, 74, 38));
        rect(c, 29, 44, 31, 90, rgb(150, 104, 58));
        rect(c, 24, 40, 40, 50, rgb(70, 70, 76));
        rect(c, 22, 38, 42, 42, rgb(110, 110, 118));
        float[] hh = {26, 30, 23};
        float h = hh[frame % 3], sway = (frame - 1) * 1.8f;
        Path f1 = new Path(); f1.moveTo(24, 40); f1.quadTo(22, 40 - h * 0.5f, 32 + sway, 40 - h - 8); f1.quadTo(42, 40 - h * 0.5f, 40, 40); f1.close();
        p.setStyle(Paint.Style.FILL); p.setColor(rgb(255, 120, 30)); c.drawPath(f1, p);
        Path f2 = new Path(); f2.moveTo(27, 40); f2.quadTo(26, 40 - h * 0.4f, 32 + sway * 0.6f, 40 - h * 0.8f - 2); f2.quadTo(38, 40 - h * 0.4f, 37, 40); f2.close();
        p.setColor(rgb(255, 200, 50)); c.drawPath(f2, p);
        Path f3 = new Path(); f3.moveTo(29.5f, 40); f3.quadTo(29, 40 - h * 0.25f, 32, 40 - h * 0.5f); f3.quadTo(35, 40 - h * 0.25f, 34.5f, 40); f3.close();
        p.setColor(rgb(255, 245, 190)); c.drawPath(f3, p);
        return b;
    }

    // ---------------------------------------------------------------- baú
    private Bitmap bakeChest(int level) {
        level = Math.max(1, Math.min(3, level));
        Bitmap b = Bitmap.createBitmap(128, 112, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        shadow(c, 64, 102, 52, 8, 255);
        int band = level == 1 ? rgb(120, 126, 136) : level == 2 ? rgb(96, 156, 206) : rgb(246, 206, 80);
        int wood = rgb(140, 92, 48);
        rect(c, 14, 54, 114, 100, shade(wood, 0.78f));
        for (int x = 14; x < 114; x += 12) line(c, x, 54, x, 100, 1.2f, shade(wood, 0.55f));
        rect(c, 14, 54, 114, 60, shade(wood, 0.6f));
        // tampa arredondada
        Path lid = new Path(); lid.moveTo(14, 56); lid.lineTo(14, 44); lid.quadTo(64, 8, 114, 44); lid.lineTo(114, 56); lid.close();
        p.setStyle(Paint.Style.FILL); p.setColor(wood); c.drawPath(lid, p);
        Path hl = new Path(); hl.moveTo(14, 56); hl.lineTo(14, 44); hl.quadTo(40, 24, 64, 22); hl.lineTo(64, 56); hl.close();
        p.setColor(shade(wood, 1.15f)); c.drawPath(hl, p);
        line(c, 14, 56, 114, 56, 3f, shade(wood, 0.45f));
        // faixas e fechadura
        for (int s = 0; s < 2; s++) {
            float x = s == 0 ? 28 : 92;
            rect(c, x - 5, 30 + (s == 0 ? 2 : 2), x + 5, 100, band);
            rect(c, x - 5, 30 + 2, x - 3, 100, shade(band, 1.25f));
        }
        rect(c, 56, 50, 72, 72, shade(band, 0.8f)); rect(c, 58, 52, 70, 70, band);
        disc(c, 64, 60, 3f, rgb(40, 30, 20));
        rect(c, 63, 60, 65, 67, rgb(40, 30, 20));
        return b;
    }

    // ---------------------------------------------------------------- horta
    /** Estufa 3x3: base, paredes de vidro, telhado em degraus e plantas dentro. */
    private Bitmap bakeGreenhouse(int level) {
        level = Math.max(1, Math.min(5, level));
        Bitmap b = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        int frame = level <= 2 ? rgb(236, 236, 230) : level == 3 ? rgb(120, 190, 120) : level == 4 ? rgb(96, 156, 206) : rgb(246, 206, 80);
        rect(c, 8, 180, 184, 190, 0x55000000);
        rect(c, 10, 70, 182, 178, shade(frame, 0.6f));
        rect(c, 14, 74, 178, 170, alpha(rgb(170, 225, 235), 235));
        rect(c, 14, 74, 178, 84, alpha(rgb(220, 245, 250), 235));
        for (int i = 1; i < 4; i++) rect(c, 14 + i * 41, 74, 16 + i * 41, 170, frame);
        rect(c, 14, 120, 178, 122, frame);
        rect(c, 4, 56, 188, 74, frame);
        rect(c, 24, 38, 168, 58, shade(frame, 1.08f));
        rect(c, 56, 22, 136, 40, shade(frame, 1.15f));
        rect(c, 56, 22, 136, 26, shade(frame, 1.3f));
        for (int i = 0; i < 4; i++) {
            float cx = 32 + i * 42;
            rect(c, cx - 11, 148, cx + 11, 166, rgb(150, 92, 54));
            disc(c, cx, 140, 13, rgb(62, 150, 62));
            disc(c, cx - 5, 136, 7, rgb(96, 190, 86));
            disc(c, cx + 5, 142, 4.5f, i % 2 == 0 ? rgb(226, 60, 56) : rgb(250, 200, 60));
        }
        rect(c, 82, 128, 110, 178, rgb(120, 80, 48));
        rect(c, 84, 130, 108, 176, rgb(150, 104, 66));
        disc(c, 102, 154, 2.4f, rgb(246, 206, 80));
        return b;
    }

    private Bitmap bakeFountain(int level) {
        Bitmap b = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        int stone = level == 1 ? rgb(190, 190, 196) : level == 2 ? rgb(214, 196, 150) : rgb(240, 214, 110);
        rect(c, 10, 112, 118, 122, 0x55000000);
        disc(c, 64, 84, 52, shade(stone, 0.7f));
        disc(c, 64, 80, 48, stone);
        disc(c, 64, 80, 40, rgb(96, 170, 230));
        disc(c, 56, 74, 14, alpha(rgb(190, 230, 255), 190));
        rect(c, 58, 44, 70, 82, shade(stone, 0.85f));
        disc(c, 64, 40, 12, stone);
        for (int i = -1; i <= 1; i++) line(c, 64, 36, 64 + i * 22, 62, 3f, alpha(rgb(200, 235, 255), 220));
        return b;
    }

    private Bitmap bakeStatue(int level) {
        Bitmap b = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        int stone = level == 1 ? rgb(170, 172, 180) : level == 2 ? rgb(196, 160, 90) : rgb(250, 210, 70);
        rect(c, 14, 112, 114, 122, 0x55000000);
        rect(c, 28, 96, 100, 116, rgb(110, 112, 120));
        rect(c, 34, 90, 94, 100, rgb(140, 142, 150));
        rect(c, 50, 40, 78, 92, stone);                  // corpo
        disc(c, 64, 30, 16, stone);                      // cabeça
        rect(c, 36, 50, 52, 66, stone); rect(c, 76, 50, 92, 66, stone);   // braços
        disc(c, 58, 28, 2.5f, rgb(40, 40, 48)); disc(c, 70, 28, 2.5f, rgb(40, 40, 48));
        rect(c, 56, 10, 72, 18, shade(stone, 0.8f));
        return b;
    }

    private Bitmap bakeTrap(int level) {
        Bitmap b = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        int metal = level == 1 ? rgb(150, 150, 158) : level == 2 ? rgb(210, 160, 70) : rgb(120, 200, 240);
        disc(c, 32, 36, 24, 0x44000000);
        disc(c, 32, 34, 22, shade(metal, 0.6f));
        disc(c, 32, 34, 18, metal);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            float x = 32 + (float) Math.cos(a) * 14, y = 34 + (float) Math.sin(a) * 14;
            line(c, 32 + (float) Math.cos(a) * 6, 34 + (float) Math.sin(a) * 6, x, y, 4f, rgb(80, 80, 90));
        }
        disc(c, 32, 34, 5, rgb(220, 60, 50));
        return b;
    }

    /** Barraca do Mercado de Trocas 4x3: toldo listrado, balcão e caixotes. */
    private Bitmap bakeTradeStall(int level) {
        Bitmap b = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        int a1 = level == 1 ? rgb(220, 70, 60) : level == 2 ? rgb(70, 130, 220) : rgb(240, 190, 60);
        rect(c, 8, 176, 248, 188, 0x55000000);
        rect(c, 20, 70, 236, 176, rgb(150, 104, 66));
        rect(c, 28, 78, 228, 170, rgb(110, 76, 48));
        rect(c, 20, 130, 236, 146, rgb(190, 140, 90));                    // balcão
        for (int i = 0; i < 8; i++) rect(c, 12 + i * 29, 30, 12 + i * 29 + 29, 74, i % 2 == 0 ? a1 : rgb(246, 244, 236));   // toldo
        rect(c, 12, 70, 244, 78, shade(a1, 0.7f));
        rect(c, 12, 22, 244, 32, shade(a1, 0.8f));
        for (int i = 0; i < 3; i++) { rect(c, 44 + i * 62, 108, 84 + i * 62, 130, rgb(190, 150, 80)); disc(c, 64 + i * 62, 104, 9, i == 1 ? rgb(250, 200, 60) : rgb(96, 190, 86)); }
        disc(c, 128, 54, 12, rgb(250, 240, 200));
        p.setColor(rgb(60, 40, 30)); p.setTextAlign(Paint.Align.CENTER); p.setTextSize(18); p.setFakeBoldText(true);
        c.drawText("⇄", 128, 60, p); p.setFakeBoldText(false);
        return b;
    }

    private Bitmap bakeGarden(int level) {
        level = Math.max(1, Math.min(5, level));
        Bitmap b = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        int frame = level == 1 ? rgb(150, 100, 56) : level == 2 ? rgb(138, 142, 150) : level == 3 ? rgb(96, 156, 206) : level == 4 ? rgb(120, 190, 120) : rgb(246, 206, 80);
        int soil = rgb(88, 58, 36);
        rect(c, 4, 22, 124, 124, shade(frame, 0.55f));            // sombra da moldura
        rect(c, 4, 18, 124, 120, frame);
        rect(c, 4, 18, 124, 24, shade(frame, 1.2f));
        rect(c, 12, 30, 116, 112, shade(soil, 0.8f));
        rect(c, 12, 30, 116, 36, shade(soil, 0.6f));
        for (int row = 0; row < 2; row++) {
            float y0 = 44 + row * 40;
            for (int k = 0; k < 3; k++) {
                float x0 = 18 + k * 34;
                rect(c, x0, y0 + 14, x0 + 28, y0 + 24, soil);
                rect(c, x0, y0 + 14, x0 + 28, y0 + 17, shade(soil, 1.3f));
                rect(c, x0, y0 + 24, x0 + 28, y0 + 27, shade(soil, 0.6f));
            }
        }
        for (int i = 0; i < level; i++) disc(c, 24 + i * 11, 12, 3.2f, rgb(255, 213, 79));
        return b;
    }

    // ---------------------------------------------------------------- laboratório
    private Bitmap bakeLab(int level) {
        level = Math.max(1, Math.min(5, level));
        Bitmap b = Bitmap.createBitmap(256, 280, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        shadow(c, 128, 268, 116, 14, 255);
        int wall = rgb(226, 232, 236), teal = rgb(54, 160, 160);
        rect(c, 24, 130, 232, 262, shade(wall, 0.86f));
        rect(c, 24, 130, 80, 262, wall);
        rect(c, 24, 124, 232, 140, teal); rect(c, 24, 124, 232, 128, shade(teal, 1.25f));
        rect(c, 18, 250, 238, 264, rgb(120, 126, 134));
        // portas de vidro
        rect(c, 96, 188, 160, 262, rgb(60, 78, 96)); rect(c, 100, 192, 126, 262, rgb(150, 205, 230)); rect(c, 130, 192, 156, 262, rgb(150, 205, 230));
        // janelas
        for (int s = -1; s <= 1; s += 2) for (int k = 0; k < 2; k++) {
            float wx = 128 + s * (70 + k * 0) - 20 + (k == 1 ? s * 0 : 0);
            if (k == 1) continue;
            rect(c, wx, 160, wx + 40, 214, rgb(50, 66, 84)); rect(c, wx + 3, 163, wx + 37, 211, rgb(150, 205, 230));
            line(c, wx + 20, 160, wx + 20, 214, 2f, rgb(50, 66, 84));
        }
        // cúpula e antena
        disc(c, 128, 126, 46, shade(teal, 0.8f));
        rect(c, 60, 126, 196, 140, teal);
        disc(c, 128, 124, 38, rgb(120, 200, 230));
        disc(c, 114, 108, 10, 0x66FFFFFF);
        rect(c, 126, 52, 130, 90, rgb(90, 96, 104)); disc(c, 128, 50, 6, rgb(230, 70, 70));
        // frasco no letreiro
        rect(c, 110, 146, 146, 160, rgb(40, 52, 66));
        disc(c, 128, 153, 4.5f, rgb(110, 230, 150));
        for (int i = 0; i < level; i++) disc(c, 128 + (i - (level - 1) / 2f) * 12, 275, 3.4f, rgb(255, 213, 79));
        return b;
    }

    // ---------------------------------------------------------------- obra
    private Bitmap bakeScaffold(int tw, int th) {
        int w = tw * T, h = th * T;
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        shadow(c, w / 2f, h - 8, w * 0.44f, 10, 255);
        // chão de terra
        p.setStyle(Paint.Style.FILL); p.setColor(rgb(150, 118, 78));
        r.set(10, h * 0.42f, w - 10, h - 6); c.drawRoundRect(r, 16, 16, p);
        p.setColor(rgb(172, 140, 96)); r.set(18, h * 0.46f, w - 18, h - 14); c.drawRoundRect(r, 12, 12, p);
        // postes e tábuas
        float lx = 20, rx = w - 24, ty = h * 0.18f, by = h - 14;
        for (float x : new float[]{lx, rx}) {
            rect(c, x, ty, x + 5, by, rgb(120, 80, 42)); rect(c, x, ty, x + 2, by, rgb(160, 112, 62));
        }
        rect(c, lx, ty, rx + 5, ty + 8, rgb(140, 96, 50));
        rect(c, lx, (ty + by) / 2, rx + 5, (ty + by) / 2 + 7, rgb(140, 96, 50));
        line(c, lx + 5, by, rx, (ty + by) / 2, 3f, rgb(110, 74, 38));
        line(c, rx, by, lx + 5, (ty + by) / 2, 3f, rgb(110, 74, 38));
        // pilha de tábuas e um bloco de pedra
        rect(c, w * 0.18f, by - 14, w * 0.18f + 34, by - 4, rgb(176, 128, 72));
        rect(c, w * 0.18f, by - 24, w * 0.18f + 34, by - 14, rgb(150, 106, 58));
        rect(c, w * 0.7f, by - 20, w * 0.7f + 26, by - 4, rgb(150, 150, 150));
        rect(c, w * 0.7f, by - 20, w * 0.7f + 26, by - 14, rgb(190, 190, 190));
        return b;
    }
}
