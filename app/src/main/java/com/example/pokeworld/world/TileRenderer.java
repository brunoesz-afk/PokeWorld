package com.example.pokeworld.world;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;

import java.util.HashMap;
import java.util.Random;

import static com.example.pokeworld.world.Tiles.*;

/**
 * Desenha os mapas novos.
 *
 * Em vez de repetir uma mesma imagem em cada quadrado, cada tile é desenhado
 * olhando os vizinhos (autotile):
 *  - penhascos e muros têm topo, face, base e extremidades e formam degraus de altura;
 *  - escadas se encaixam no penhasco/muro e ligam os dois níveis;
 *  - água tem espuma na margem e cantos arredondados; lava tem crosta nas bordas;
 *  - chãos diferentes se misturam com bordas orgânicas (grama invadindo a trilha etc.);
 *  - pontes mudam de direção e ganham corrimão só onde precisa.
 *
 * Cada combinação (tipo + vizinhança) é desenhada uma vez num Bitmap 64x64 e reaproveitada.
 */
public final class TileRenderer {

    /** Fonte das imagens que já existem no jogo (grama, árvore, etc.). Pode devolver null. */
    public interface Sprites { Bitmap get(int key); }

    private static final int T = 64;

    private static final int[] DR = {-1, 0, 1, 0, -1, 1, 1, -1};
    private static final int[] DC = {0, 1, 0, -1, 1, 1, -1, -1};
    private static final int N = 0, E = 1, S = 2, W = 3, NE = 4, SE = 5, SW = 6, NW = 7;

    private final int[][] g;
    private final int rows, cols;
    private final Theme th;
    private final Sprites sprites;
    private final HashMap<Long, Bitmap> cache = new HashMap<>();
    private final HashMap<Integer, Bitmap> tallCache = new HashMap<>();
    private final byte[][] bridgeDir;
    private int frame = 0;
    private final int grassAvg, snowAvg;

    private final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Rect srcR = new Rect(), dstR = new Rect();
    private Canvas cv;

    public TileRenderer(String mapName, int[][] grid, Sprites sprites) {
        this.g = grid;
        this.rows = grid.length;
        this.cols = grid[0].length;
        this.th = Theme.forMap(mapName);
        this.sprites = sprites;
        this.bridgeDir = new byte[rows][cols];
        computeBridgeDirs();
        Bitmap gs = sprite(GRASS);
        this.grassAvg = gs != null ? average(gs) : th.grass;
        Bitmap ss = sprite(SNOW);
        this.snowAvg = ss != null ? average(ss) : th.snow;
    }

    /** Chamar uma vez por quadro: define o frame de animação (água, lava, fogo). */
    public void beginFrame() {
        frame = (int) ((System.currentTimeMillis() / 380) % 3);
        if (cache.size() > 2500) cache.clear();
    }

    private Bitmap sprite(int k) { return sprites == null ? null : sprites.get(k); }

    // ------------------------------------------------------------------
    // API de desenho por tile
    // ------------------------------------------------------------------

    /** Primeira passada: chão, água, muros, objetos baixos. */
    public void drawTile(Canvas canvas, int r, int c) {
        int t = g[r][c];
        Bitmap b = tileBitmap(r, c, t);
        int x = c * T, y = r * T;
        dstR.set(x, y, x + T, y + T);
        canvas.drawBitmap(b, null, dstR, null);
    }

    /** Segunda passada: objetos altos (árvores, pilares...) que passam por cima dos tiles vizinhos. */
    public void drawTall(Canvas canvas, int r, int c) {
        int t = g[r][c];
        if (!isTall(t)) return;
        int x = c * T, y = r * T;
        if (t == TREE || t == BERRY_TREE) {
            Bitmap s = sprite(TREE);
            int size = (int) (T * 2.5f), off = (size - T) / 2;
            if (s != null) {
                dstR.set(x - off, y - (size - T), x + T + off, y + T);
                canvas.drawBitmap(s, null, dstR, null);
                if (t == BERRY_TREE) berries(canvas, x, y, r, c);
            } else {
                P.setStyle(Paint.Style.FILL);
                P.setColor(0xFF2e6b22);
                canvas.drawCircle(x + 32, y - 20, 50, P);
                P.setColor(0xFF4a8a30);
                canvas.drawCircle(x + 22, y - 30, 30, P);
            }
            return;
        }
        int h = tallHeight(t);
        Bitmap b = tallBitmap(t, hash(r, c) & 3);
        dstR.set(x, y + T - h, x + T, y + T);
        canvas.drawBitmap(b, null, dstR, null);
    }

    private void berries(Canvas canvas, int x, int y, int r, int c) {
        Random rr = new Random(hash(r, c) * 31L + 7);
        P.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 7; i++) {
            float bx = x + 8 + rr.nextInt(48), by = y - 62 + rr.nextInt(58);
            P.setColor(0xFFd8213a);
            canvas.drawCircle(bx, by, 3.2f, P);
            P.setColor(0xFFff8a96);
            canvas.drawCircle(bx - 1, by - 1, 1.1f, P);
        }
    }

    private int tallHeight(int t) {
        switch (t) {
            case DEAD_TREE: return 112;
            case PILLAR: return 100;
            case CACTUS: return 84;
            case SNOW_CRYSTAL: return 92;
            default: return 64;
        }
    }

    // ------------------------------------------------------------------
    // Cache
    // ------------------------------------------------------------------

    private static int hash(int r, int c) {
        int h = r * 73856093 ^ c * 19349663;
        h ^= (h >>> 13);
        h *= 0x5bd1e995;
        h ^= (h >>> 15);
        return h & 0x7fffffff;
    }

    private int at(int r, int c, int self) {
        if (r < 0 || r >= rows || c < 0 || c >= cols) return self;
        return g[r][c];
    }

    private int[] neighbors(int r, int c, int self) {
        int[] nb = new int[8];
        for (int i = 0; i < 8; i++) nb[i] = at(r + DR[i], c + DC[i], self);
        return nb;
    }

    /** Código usado na chave de cache: objetos viram um código único; flores viram grama. */
    private static int norm(int t, int self) {
        if (t == FLOWERS) return GRASS;
        if (t == PORTAL) return self;
        if (isObject(t) || t == FOUNTAIN) return t == FOUNTAIN ? t : 63;
        if (t == MAGMA_BRIDGE) return BRIDGE;
        if (t == BRICK) return WALL;
        return t;
    }

    private static boolean animated(int t) {
        return t == WATER || t == DEEP_WATER || t == LAVA || t == MAGMA_VENT || t == BRAZIER || t == FOUNTAIN
                || t == PORTAL || t == BRIDGE || t == MAGMA_BRIDGE;
    }

    private int depthBelow(int r, int c, int t) {
        int d = 0;
        int rr = r + 1;
        while (d < 3 && rr < rows && isMassSame(g[rr][c], t)) { d++; rr++; }
        return d;
    }

    /** Massa "sólida" para efeito de ligação (penhasco/muro/casa se ligam entre si por família). */
    private static boolean isMassSame(int a, int b) {
        return isMass(a);
    }

    private Bitmap tileBitmap(int r, int c, int t) {
        int[] nb = neighbors(r, c, t);
        int variant = hash(r, c) & 3;
        int fr = animated(t) ? frame : 0;
        int under = 0, extra = 0;
        if (isObject(t) || t == BRIDGE || t == MAGMA_BRIDGE || t == FOUNTAIN || t == STAIRS) under = underlay(r, c, nb, t);
        if (isMass(t)) extra = depthBelow(r, c, t);
        if (t == BRIDGE || t == MAGMA_BRIDGE) extra = bridgeDir[r][c] + 1;
        if (t == FOUNTAIN || t == STAIRS) extra = 0;
        long k = 1469598103934665603L;
        k = (k ^ t) * 1099511628211L;
        k = (k ^ under) * 1099511628211L;
        k = (k ^ variant) * 1099511628211L;
        k = (k ^ fr) * 1099511628211L;
        k = (k ^ extra) * 1099511628211L;
        for (int i = 0; i < 8; i++) k = (k ^ norm(nb[i], t)) * 1099511628211L;
        Bitmap b = cache.get(k);
        if (b == null) {
            b = Bitmap.createBitmap(T, T, Bitmap.Config.ARGB_8888);
            cv = new Canvas(b);
            paintTile(t, nb, under, variant, fr, extra, r, c);
            cache.put(k, b);
        }
        return b;
    }

    private Bitmap tallBitmap(int t, int variant) {
        int key = t * 8 + variant;
        Bitmap b = tallCache.get(key);
        if (b == null) {
            b = Bitmap.createBitmap(T, tallHeight(t), Bitmap.Config.ARGB_8888);
            cv = new Canvas(b);
            paintTall(t, variant, tallHeight(t));
            tallCache.put(key, b);
        }
        return b;
    }

    /** Chão que fica por baixo de objetos, pontes etc.: o mais comum entre os vizinhos. */
    private int underlay(int r, int c, int[] nb, int self) {
        int[] cnt = new int[COUNT];
        int best = th.defaultGround, bestN = 0;
        for (int i = 0; i < 8; i++) {
            int v = nb[i] == FLOWERS ? GRASS : nb[i];
            if (isGround(v) && v != CLOUD) {
                cnt[v] += i < 4 ? 2 : 1;
                if (cnt[v] > bestN) { bestN = cnt[v]; best = v; }
            }
        }
        if (self == BRIDGE || self == MAGMA_BRIDGE) {
            int lava = 0, water = 0;
            for (int i = 0; i < 4; i++) { if (nb[i] == LAVA) lava++; if (isWater(nb[i])) water++; }
            if (lava > 0 && lava >= water) return LAVA;
            if (water > 0) return WATER;
            if (self == MAGMA_BRIDGE) return LAVA;
        }
        return best;
    }

    private void computeBridgeDirs() {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            if (g[r][c] != BRIDGE && g[r][c] != MAGMA_BRIDGE) continue;
            int h = 1, v = 1;
            int x = c - 1;
            while (x >= 0 && isBr(g[r][x])) { h++; x--; }
            x = c + 1;
            while (x < cols && isBr(g[r][x])) { h++; x++; }
            int y = r - 1;
            while (y >= 0 && isBr(g[y][c])) { v++; y--; }
            y = r + 1;
            while (y < rows && isBr(g[y][c])) { v++; y++; }
            bridgeDir[r][c] = (byte) (h > v ? 0 : 1); // 0 = atravessa na horizontal, 1 = na vertical
        }
    }

    private static boolean isBr(int t) { return t == BRIDGE || t == MAGMA_BRIDGE; }

    // ------------------------------------------------------------------
    // Utilidades de cor e forma
    // ------------------------------------------------------------------

    private static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private static int shade(int c, float f) {
        int r = Math.min(255, Math.max(0, (int) (((c >> 16) & 255) * f)));
        int gg = Math.min(255, Math.max(0, (int) (((c >> 8) & 255) * f)));
        int b = Math.min(255, Math.max(0, (int) ((c & 255) * f)));
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static int withAlpha(int c, int a) { return (a << 24) | (c & 0xFFFFFF); }

    private static int average(Bitmap b) {
        long rs = 0, gs = 0, bs = 0;
        int n = 0;
        for (int y = 4; y < b.getHeight(); y += Math.max(1, b.getHeight() / 8)) {
            for (int x = 4; x < b.getWidth(); x += Math.max(1, b.getWidth() / 8)) {
                int p = b.getPixel(x, y);
                if ((p >>> 24) < 128) continue;
                rs += (p >> 16) & 255; gs += (p >> 8) & 255; bs += p & 255; n++;
            }
        }
        if (n == 0) return 0xFF4f9a2f;
        return 0xFF000000 | ((int) (rs / n) << 16) | ((int) (gs / n) << 8) | (int) (bs / n);
    }

    private void fill(float l, float t, float r, float b, int color) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(color);
        cv.drawRect(l, t, r, b, P);
    }

    private void circle(float x, float y, float r, int color) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(color);
        cv.drawCircle(x, y, r, P);
    }

    private void line(float x0, float y0, float x1, float y1, float w, int color) {
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(w);
        P.setColor(color);
        cv.drawLine(x0, y0, x1, y1, P);
        P.setStyle(Paint.Style.FILL);
    }

    /** Degradê vertical em faixas de 2px. */
    private void vgrad(float l, float t, float r, float b, int c1, int c2) {
        int n = Math.max(1, (int) ((b - t) / 2f));
        for (int i = 0; i < n; i++) {
            float y0 = t + (b - t) * i / n, y1 = t + (b - t) * (i + 1) / n;
            fill(l, y0, r, y1 + 0.6f, mix(c1, c2, i / (float) Math.max(1, n - 1)));
        }
    }

    private void hgrad(float l, float t, float r, float b, int c1, int c2) {
        int n = Math.max(1, (int) ((r - l) / 2f));
        for (int i = 0; i < n; i++) {
            float x0 = l + (r - l) * i / n, x1 = l + (r - l) * (i + 1) / n;
            fill(x0, t, x1 + 0.6f, b, mix(c1, c2, i / (float) Math.max(1, n - 1)));
        }
    }

    private void speckle(Random rr, int n, int color, int alpha, float minR, float maxR) {
        for (int i = 0; i < n; i++) {
            circle(3 + rr.nextFloat() * 58, 3 + rr.nextFloat() * 58, minR + rr.nextFloat() * (maxR - minR), withAlpha(color, alpha));
        }
    }

    private void wave(float x0, float y, float w, float amp, float sw, int color) {
        path.reset();
        path.moveTo(x0, y);
        path.quadTo(x0 + w / 2, y - amp, x0 + w, y);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(sw);
        P.setColor(color);
        cv.drawPath(path, P);
        P.setStyle(Paint.Style.FILL);
    }

    private static boolean isLand(int t) { return !isLiquid(t) && !isBr(t) && t != STAIRS; }

    private int groundColor(int t) {
        switch (t) {
            case GRASS: case FLOWERS: return grassAvg;
            case SAND: return th.sand;
            case SNOW: return snowAvg;
            case ROAD: return th.road;
            case STONE_PAVEMENT: return th.pave;
            case FLOOR: return th.floor;
            case SOIL: return th.soil;
            case CARPET: return 0xFFa01c24;
            case ASH_GROUND: return th.ash;
            case BASALT_FLOOR: return th.basalt;
            case OBSIDIAN: return th.obsidian;
            case CLOUD: return 0xFFeaf4ff;
            default: return th.defaultGround == t ? grassAvg : groundColor(th.defaultGround);
        }
    }

    /** Quanto maior, mais o chão "invade" os vizinhos de prioridade menor. */
    private static int priority(int t) {
        switch (t) {
            case GRASS: case FLOWERS: return 7;
            case SNOW: return 6;
            case SAND: return 5;
            case ASH_GROUND: return 4;
            case SOIL: return 4;
            case ROAD: return 3;
            case OBSIDIAN: return 3;
            case BASALT_FLOOR: return 2;
            case FLOOR: case STONE_PAVEMENT: return 1;
            default: return 0;
        }
    }

    // ------------------------------------------------------------------
    // Pintura principal
    // ------------------------------------------------------------------

    /** Vizinhança uniforme para o chão que fica sob objetos: sem bordas nem contornos. */
    private int[] underNb(int[] nb, int under) {
        int[] u = new int[8];
        for (int i = 0; i < 8; i++) u[i] = isMass(nb[i]) ? nb[i] : under;
        return u;
    }

    private void paintTile(int t, int[] nb, int under, int v, int fr, int extra, int r, int c) {
        if (isObject(t) && t != PORTAL) nb = underNb(nb, under);
        switch (t) {
            case WATER: case DEEP_WATER: paintWater(t, nb, v, fr); break;
            case LAVA: paintLava(nb, v, fr); break;
            case MAGMA_VENT: paintGround(BASALT_FLOOR, nb, v, t); paintVent(fr, v); break;
            case CLIFF: case CRACKED_OBSIDIAN: paintRock(t, nb, extra, v); break;
            case WALL: case BRICK: paintBrick(nb, extra, v); break;
            case ROOF: paintRoof(nb, v); break;
            case HOUSE: case DOOR: paintHouse(t, nb, v); break;
            case STAIRS: paintStairs(nb, under, v); break;
            case BRIDGE: case MAGMA_BRIDGE: paintBridge(t, nb, under, extra - 1, v, fr); break;
            case FOUNTAIN: paintFountain(nb, v, fr); break;
            case PORTAL: paintGround(th.defaultGround == GRASS ? ROAD : th.defaultGround, nb, v, t); paintPortal(fr); break;
            case TREE: case BERRY_TREE:
                paintGround(under, nb, v, t);
                P.setStyle(Paint.Style.FILL);
                P.setColor(0x46000000);
                cv.drawOval(new android.graphics.RectF(10, 46, 54, 62), P);
                break;
            case BUSH: paintGround(under, nb, v, t); paintBush(v); break;
            case ROCK: paintGround(under, nb, v, t); paintBoulder(v); break;
            case SIGN: paintGround(under, nb, v, t); paintSign(v); break;
            case BRAZIER: paintGround(under, nb, v, t); paintBrazier(fr); break;
            case TOMBSTONE: paintGround(under, nb, v, t); paintTomb(v); break;
            case SULFUR_CRYSTAL: paintGround(under, nb, v, t); paintCrystal(0xFFffd23a, 0xFFfff2a0, 0xFFb07a10, v); break;
            case SNOW_CRYSTAL: case DEAD_TREE: case PILLAR: case CACTUS:
                paintGround(under, nb, v, t);
                if (t != SNOW_CRYSTAL) { P.setColor(0x44000000); cv.drawOval(new android.graphics.RectF(14, 50, 50, 62), P); }
                break;
            default: paintGround(t, nb, v, t); break;
        }
    }

    // ------------------------------------------------------------------
    // Chão com transições suaves
    // ------------------------------------------------------------------

    private void paintGround(int t, int[] nb, int v, int self) {
        paintGroundBase(t, v, nb);
        // Bordas orgânicas: chão de maior prioridade invade o de menor prioridade
        int pt = priority(t);
        boolean constructed = t == FLOOR || t == STONE_PAVEMENT || t == CARPET || t == CLOUD;
        for (int s = 0; s < 4; s++) {
            int n = nb[s] == FLOWERS ? GRASS : nb[s];
            if (n == t || !isGround(n)) continue;
            if (t != CLOUD && priority(n) > pt && !constructed) fringe(s, groundColor(n), n, v);
        }
        // Cantos diagonais
        for (int d = 4; d < 8; d++) {
            int n = nb[d] == FLOWERS ? GRASS : nb[d];
            int a = nb[d == 4 ? N : d == 5 ? E : d == 6 ? S : W], b2 = nb[d == 4 ? E : d == 5 ? S : d == 6 ? W : N];
            if (n != t && isGround(n) && priority(n) > pt && !constructed && a != n && b2 != n && a == t && b2 == t) cornerBlob(d, groundColor(n));
        }
        if (!constructed) {
            for (int s = 0; s < 4; s++) {
                int n = nb[s];
                if (n == STONE_PAVEMENT || n == FLOOR || n == CARPET) {
                    edgeLine(s, shade(groundColor(n), 0.6f), 3);
                    int w = 3;
                    switch (s) {
                        case N: fill(0, w, T, w + 1, withAlpha(0xFFffffff, 50)); break;
                        case S: fill(0, T - w - 1, T, T - w, withAlpha(0xFFffffff, 50)); break;
                        case W: fill(w, 0, w + 1, T, withAlpha(0xFFffffff, 50)); break;
                        default: fill(T - w - 1, 0, T - w, T, withAlpha(0xFFffffff, 50)); break;
                    }
                }
            }
        }
        if (constructed && t != CLOUD) {
            for (int s = 0; s < 4; s++) {
                int n = nb[s];
                if (n == t || isMass(n) || n == STAIRS || n == PORTAL || isBr(n)) continue;
                if (t == CARPET && n == FLOOR) { edgeLine(s, 0xFF5a0e12, 3); continue; }
                edgeLine(s, shade(groundColor(t), 0.62f), 2);
            }
        }
        // Sombra projetada por paredes ao norte
        if (isMass(nb[N]) && t != CLOUD) {
            for (int i = 0; i < 6; i++) fill(0, i * 2.4f, T, (i + 1) * 2.4f, withAlpha(0xFF000000, 80 - i * 13));
        }
    }

    private void edgeLine(int s, int color, int w) {
        switch (s) {
            case N: fill(0, 0, T, w, color); break;
            case S: fill(0, T - w, T, T, color); break;
            case W: fill(0, 0, w, T, color); break;
            default: fill(T - w, 0, T, T, color); break;
        }
    }

    private void cornerBlob(int d, int color) {
        float cx = (d == 4 || d == 5) ? T : 0, cy = (d == 5 || d == 6) ? T : 0;
        circle(cx, cy, 11, color);
        circle(cx, cy, 11, withAlpha(0xFF000000, 0));
    }

    /** Borda ondulada de uma cor invadindo o tile pelo lado s. */
    private void fringe(int s, int color, int n, int v) {
        Random rr = new Random(s * 1013L + n * 31L + v);
        float ph = rr.nextFloat() * 6.28f, ph2 = rr.nextFloat() * 6.28f;
        float base = 7 + rr.nextFloat() * 3;
        int dark = shade(color, 0.86f);
        for (int pass = 0; pass < 2; pass++) {
            path.reset();
            float extra = pass == 0 ? 2.2f : 0;
            int steps = 16;
            for (int i = 0; i <= steps; i++) {
                float u = i * T / (float) steps;
                float d = base + extra + 4.2f * (float) Math.sin(u * 0.19f + ph) + 2.4f * (float) Math.sin(u * 0.47f + ph2);
                float x, y;
                switch (s) {
                    case N: x = u; y = d; break;
                    case S: x = u; y = T - d; break;
                    case W: x = d; y = u; break;
                    default: x = T - d; y = u; break;
                }
                if (i == 0) {
                    switch (s) {
                        case N: path.moveTo(0, 0); path.lineTo(x, y); break;
                        case S: path.moveTo(0, T); path.lineTo(x, y); break;
                        case W: path.moveTo(0, 0); path.lineTo(x, y); break;
                        default: path.moveTo(T, 0); path.lineTo(x, y); break;
                    }
                } else path.lineTo(x, y);
            }
            switch (s) {
                case N: path.lineTo(T, 0); break;
                case S: path.lineTo(T, T); break;
                case W: path.lineTo(0, T); break;
                default: path.lineTo(T, T); break;
            }
            path.close();
            P.setStyle(Paint.Style.FILL);
            P.setColor(pass == 0 ? withAlpha(dark, 255) : color);
            cv.drawPath(path, P);
        }
        // Folhinhas / grãos para dar textura à borda
        if (n == GRASS) {
            Random r2 = new Random(s * 77L + v);
            for (int i = 0; i < 5; i++) {
                float u = 4 + r2.nextFloat() * 56, d = base + 3 + r2.nextFloat() * 5;
                float x = s == W ? d : s == E ? T - d : u, y = s == N ? d : s == S ? T - d : u;
                line(x, y, x + (r2.nextFloat() - 0.5f) * 5, y - 4, 1.4f, shade(color, 1.18f));
            }
        }
    }

    private void paintGroundBase(int t, int v, int[] nb) {
        Random rr = new Random(t * 131L + v * 977L + 5);
        switch (t) {
            case GRASS: case FLOWERS: {
                Bitmap s = sprite(GRASS);
                if (s != null) {
                    cv.save();
                    if ((v & 1) != 0) { cv.translate(T, 0); cv.scale(-1, 1); }
                    cv.drawBitmap(s, null, rect(0, 0, T, T), null);
                    cv.restore();
                } else {
                    fill(0, 0, T, T, th.grass);
                    speckle(rr, 10, th.grassDark, 150, 2, 5);
                    for (int i = 0; i < 14; i++) {
                        float x = 3 + rr.nextFloat() * 58, y = 6 + rr.nextFloat() * 54;
                        line(x, y, x + (rr.nextFloat() - 0.5f) * 3, y - 5, 1.4f, i % 2 == 0 ? th.grassLight : th.grassDark);
                    }
                }
                speckle(rr, 3, 0xFF000000, 12, 6, 12);
                if (t == FLOWERS) flowers(rr);
                break;
            }
            case SAND: {
                fill(0, 0, T, T, th.sand);
                speckle(rr, 5, th.sandDark, 40, 6, 12);
                for (int i = 0; i < 3; i++) wave(6 + rr.nextInt(10), 14 + i * 19 + v * 2, 30 + rr.nextInt(14), 3, 1.6f, withAlpha(th.sandDark, 120));
                speckle(rr, 12, th.sandDark, 150, 0.8f, 1.6f);
                speckle(rr, 6, 0xFFfff3cf, 130, 0.8f, 1.5f);
                break;
            }
            case SNOW: {
                Bitmap s = sprite(SNOW);
                if (s != null) {
                    int fw = s.getWidth() / 3, fh = s.getHeight() / 3;
                    srcR.set(fw, fh, fw * 2, fh * 2);
                    cv.save();
                    if ((v & 1) != 0) { cv.translate(T, 0); cv.scale(-1, 1); }
                    cv.drawBitmap(s, srcR, rect(0, 0, T, T), null);
                    cv.restore();
                } else {
                    fill(0, 0, T, T, th.snow);
                    speckle(rr, 6, th.snowShade, 90, 7, 14);
                }
                speckle(rr, 4, 0xFFffffff, 110, 3, 7);
                speckle(rr, 5, th.snowShade, 110, 1, 2.2f);
                break;
            }
            case ROAD: {
                Bitmap s = th.cobbleRoad ? sprite(ROAD) : null;
                if (s != null) {
                    cv.drawBitmap(s, null, rect(0, 0, T, T), null);
                } else {
                    fill(0, 0, T, T, th.road);
                    speckle(rr, 6, th.roadDark, 70, 6, 12);
                    speckle(rr, 14, th.roadDark, 200, 0.8f, 2);
                    speckle(rr, 7, mix(th.road, 0xFFffffff, 0.4f), 170, 0.8f, 2);
                    // marcas de roda
                    line(0, 21 + v, T, 21 + v, 2.2f, withAlpha(th.roadDark, 70));
                    line(0, 43 - v, T, 43 - v, 2.2f, withAlpha(th.roadDark, 70));
                }
                break;
            }
            case STONE_PAVEMENT: {
                fill(0, 0, T, T, th.pave);
                for (int row = 0; row < 2; row++) {
                    for (int col = 0; col < 2; col++) {
                        float x = col * 32 + (row == 1 ? 16 : 0);
                        // lajes (a linha 2 é deslocada para quebrar as juntas)
                        int tone = rr.nextInt(5);
                        for (int dx = -1; dx <= 1; dx++) {
                            float xx = x + dx * 64;
                            if (xx + 32 > 0 && xx < T) fill(xx + 1, row * 32 + 1, xx + 31, row * 32 + 31, shade(th.pave, 0.94f + tone * 0.025f));
                        }
                    }
                }
                for (int row = 0; row < 2; row++) {
                    line(0, row * 32, T, row * 32, 2, th.paveDark);
                    float off = row == 1 ? 16 : 0;
                    for (int col = -1; col < 3; col++) {
                        float x = col * 32 + off;
                        if (x >= 0 && x <= T) line(x, row * 32, x, row * 32 + 32, 2, th.paveDark);
                    }
                }
                line(0, 1, T, 1, 1, withAlpha(0xFFffffff, 60));
                speckle(rr, 8, th.paveDark, 90, 0.8f, 1.8f);
                break;
            }
            case FLOOR: {
                fill(0, 0, T, T, th.floor);
                int tone = rr.nextInt(3);
                fill(1, 1, 31, 31, shade(th.floor, 0.96f + tone * 0.03f));
                fill(33, 33, 63, 63, shade(th.floor, 0.97f + (2 - tone) * 0.03f));
                fill(33, 1, 63, 31, shade(th.floor, 1.02f));
                fill(1, 33, 31, 63, shade(th.floor, 0.99f));
                line(0, 32, T, 32, 2, th.floorDark);
                line(32, 0, 32, T, 2, th.floorDark);
                line(0, 0, T, 0, 1, withAlpha(0xFFffffff, 40));
                speckle(rr, 6, th.floorDark, 110, 0.8f, 1.8f);
                if (v == 1) { line(8, 12, 15, 20, 1.2f, th.floorDark); line(15, 20, 14, 27, 1.2f, th.floorDark); }
                if (v == 2) { line(44, 40, 52, 46, 1.2f, th.floorDark); line(52, 46, 50, 55, 1.2f, th.floorDark); }
                break;
            }
            case SOIL: {
                fill(0, 0, T, T, th.soil);
                for (int i = 0; i < 4; i++) {
                    fill(0, 4 + i * 16, T, 9 + i * 16, th.soilDark);
                    fill(0, 3 + i * 16, T, 4 + i * 16, shade(th.soil, 1.18f));
                }
                speckle(rr, 10, th.soilDark, 160, 0.8f, 1.8f);
                break;
            }
            case CARPET: {
                fill(0, 0, T, T, 0xFFa01c24);
                fill(0, 0, T, T, 0xFFa01c24);
                speckle(rr, 10, 0xFF7c1018, 90, 2, 5);
                path.reset();
                path.moveTo(32, 18); path.lineTo(46, 32); path.lineTo(32, 46); path.lineTo(18, 32); path.close();
                P.setStyle(Paint.Style.STROKE); P.setStrokeWidth(2); P.setColor(0xFFe6c45a);
                cv.drawPath(path, P);
                P.setStyle(Paint.Style.FILL);
                for (int s = 0; s < 4; s++) if (nb[s] != CARPET) {
                    edgeLine(s, 0xFFe6c45a, 4);
                    int inner = 4;
                    switch (s) {
                        case N: fill(0, inner, T, inner + 2, 0xFF6e0c14); break;
                        case S: fill(0, T - inner - 2, T, T - inner, 0xFF6e0c14); break;
                        case W: fill(inner, 0, inner + 2, T, 0xFF6e0c14); break;
                        default: fill(T - inner - 2, 0, T - inner, T, 0xFF6e0c14); break;
                    }
                }
                break;
            }
            case ASH_GROUND: {
                fill(0, 0, T, T, th.ash);
                speckle(rr, 8, shade(th.ash, 0.7f), 120, 4, 9);
                speckle(rr, 8, shade(th.ash, 1.3f), 120, 1, 3);
                break;
            }
            case BASALT_FLOOR: {
                Bitmap s = sprite(BASALT_FLOOR);
                if (s != null) {
                    cv.save();
                    if ((v & 1) != 0) { cv.translate(T, 0); cv.scale(-1, 1); }
                    cv.drawBitmap(s, null, rect(0, 0, T, T), null);
                    cv.restore();
                } else {
                    fill(0, 0, T, T, th.basalt);
                    for (int i = 0; i < 5; i++) {
                        float x = rr.nextInt(48), y = rr.nextInt(48);
                        fill(x, y, x + 12 + rr.nextInt(8), y + 10 + rr.nextInt(8), shade(th.basalt, 1.15f + rr.nextFloat() * 0.2f));
                    }
                    line(0, 32, T, 32, 1.6f, shade(th.basalt, 0.6f));
                    line(32, 0, 32, T, 1.6f, shade(th.basalt, 0.6f));
                }
                break;
            }
            case OBSIDIAN: {
                fill(0, 0, T, T, th.obsidian);
                speckle(rr, 7, shade(th.obsidian, 1.4f), 90, 3, 8);
                int glow = th.obsidianGlow;
                for (int i = 0; i < 2; i++) {
                    float x = 8 + rr.nextInt(40), y = 6 + rr.nextInt(40);
                    path.reset();
                    path.moveTo(x, y);
                    path.lineTo(x + 8 + rr.nextInt(6), y + 5 + rr.nextInt(6));
                    path.lineTo(x + 6 + rr.nextInt(10), y + 14 + rr.nextInt(8));
                    P.setStyle(Paint.Style.STROKE); P.setStrokeWidth(2.2f); P.setColor(withAlpha(glow, 70));
                    cv.drawPath(path, P);
                    P.setStrokeWidth(1); P.setColor(withAlpha(glow, 200));
                    cv.drawPath(path, P);
                    P.setStyle(Paint.Style.FILL);
                }
                break;
            }
            case CLOUD: {
                vgrad(0, 0, T, T, 0xFFd8ecff, 0xFFf5faff);
                for (int i = 0; i < 4; i++) circle(6 + rr.nextInt(52), 8 + rr.nextInt(50), 8 + rr.nextInt(8), withAlpha(0xFFffffff, 150));
                break;
            }
            default:
                fill(0, 0, T, T, groundColor(t));
                speckle(rr, 10, shade(groundColor(t), 0.8f), 120, 1, 3);
        }
    }

    private void flowers(Random rr) {
        int[] cols = {0xFFf6e24a, 0xFFffffff, 0xFFf08ab8, 0xFFc9a0ff, 0xFFff9a4a};
        int pick = rr.nextInt(cols.length);
        for (int i = 0; i < 6; i++) {
            float x = 6 + rr.nextFloat() * 52, y = 8 + rr.nextFloat() * 50;
            int col = cols[(pick + i % 2 + (i > 3 ? 2 : 0)) % cols.length];
            for (int k = 0; k < 5; k++) {
                double a = k * 1.2566;
                circle(x + (float) Math.cos(a) * 2.4f, y + (float) Math.sin(a) * 2.4f, 1.9f, col);
            }
            circle(x, y, 1.5f, 0xFFd8a31a);
            line(x, y + 2, x, y + 6, 1.2f, 0xFF2e7a22);
        }
    }

    private Rect rect(int l, int t, int r, int b) { dstR.set(l, t, r, b); return dstR; }

    // ------------------------------------------------------------------
    // Água e lava
    // ------------------------------------------------------------------

    private boolean liquidLike(int t) { return isLiquid(t) || isBr(t); }

    private void paintWater(int t, int[] nb, int v, int fr) {
        boolean deep = t == DEEP_WATER;
        fill(0, 0, T, T, deep ? th.deep : th.water);
        // brilho suave do centro
        if (deep) {
            for (int s = 0; s < 4; s++) if (nb[s] == WATER) {
                for (int i = 0; i < 4; i++) {
                    int a = 70 - i * 17;
                    int col = withAlpha(th.water, a);
                    switch (s) {
                        case N: fill(0, i * 4, T, i * 4 + 4, col); break;
                        case S: fill(0, T - i * 4 - 4, T, T - i * 4, col); break;
                        case W: fill(i * 4, 0, i * 4 + 4, T, col); break;
                        default: fill(T - i * 4 - 4, 0, T - i * 4, T, col); break;
                    }
                }
            }
        }
        // ondas animadas (ficam dentro do tile para não gerar emenda)
        Random rr = new Random(v * 911L + (deep ? 3 : 7));
        for (int k = 0; k < 5; k++) {
            float y = 8 + k * 12.5f + rr.nextInt(4);
            float x0 = 6 + rr.nextInt(18) + ((fr * 3 + k * 2) % 9);
            wave(x0, y, 14 + rr.nextInt(8), 3, 1.6f, withAlpha(deep ? th.water : th.waterLight, deep ? 110 : 150));
        }
        for (int i = 0; i < 3; i++) {
            float x = 6 + rr.nextInt(52), y = 6 + rr.nextInt(52);
            if ((i + fr) % 3 == 0) circle(x, y, 1.6f, 0xCCffffff);
        }
        shore(nb, fr);
    }

    private void shore(int[] nb, int fr) {
        boolean[] land = new boolean[4];
        for (int s = 0; s < 4; s++) land[s] = !liquidLike(nb[s]) && nb[s] != STAIRS;
        // Cantos arredondados quando dois lados vizinhos são terra
        for (int d = 0; d < 4; d++) {
            int a = d, b = (d + 1) % 4;
            if (land[a] && land[b] && !isMass(nb[a]) && !isMass(nb[b])) {
                int col = landColorForRound(nb[a]);
                roundCorner(a, b, col);
            }
        }
        for (int s = 0; s < 4; s++) {
            if (!land[s]) continue;
            if (isMass(nb[s])) {
                for (int i = 0; i < 5; i++) edgeShadow(s, i * 2, 2, 90 - i * 16);
                continue;
            }
            // espuma ondulada
            float amp = 1.6f + fr * 0.6f;
            path.reset();
            int steps = 12;
            for (int i = 0; i <= steps; i++) {
                float u = i * T / (float) steps;
                float d = 5 + amp * (float) Math.sin(u * 0.33f + fr * 0.9f);
                float x = s == W ? d : s == E ? T - d : u;
                float y = s == N ? d : s == S ? T - d : u;
                if (i == 0) {
                    if (s == N) { path.moveTo(0, 0); path.lineTo(x, y); }
                    else if (s == S) { path.moveTo(0, T); path.lineTo(x, y); }
                    else if (s == W) { path.moveTo(0, 0); path.lineTo(x, y); }
                    else { path.moveTo(T, 0); path.lineTo(x, y); }
                } else path.lineTo(x, y);
            }
            if (s == N) path.lineTo(T, 0); else if (s == S) path.lineTo(T, T); else if (s == W) path.lineTo(0, T); else path.lineTo(T, T);
            path.close();
            P.setStyle(Paint.Style.FILL);
            P.setColor(withAlpha(th.foam, 205));
            cv.drawPath(path, P);
            // faixa de água clara ao lado da espuma
            switch (s) {
                case N: fill(0, 8, T, 13, withAlpha(th.waterLight, 70)); break;
                case S: fill(0, T - 13, T, T - 8, withAlpha(th.waterLight, 70)); break;
                case W: fill(8, 0, 13, T, withAlpha(th.waterLight, 70)); break;
                default: fill(T - 13, 0, T - 8, T, withAlpha(th.waterLight, 70)); break;
            }
        }
        // Cantos internos: quando os lados são água mas a diagonal é terra
        for (int d = 4; d < 8; d++) {
            int a = d == 4 ? N : d == 5 ? E : d == 6 ? S : W, b = d == 4 ? E : d == 5 ? S : d == 6 ? W : N;
            if (liquidLike(nb[a]) && liquidLike(nb[b]) && !liquidLike(nb[d]) && !isMass(nb[d]) && nb[d] != STAIRS) {
                float cx = (d == 4 || d == 5) ? T : 0, cy = (d == 5 || d == 6) ? T : 0;
                float sx = cx == 0 ? 1 : -1, sy = cy == 0 ? 1 : -1;
                path.reset();
                path.moveTo(cx, cy);
                path.lineTo(cx + sx * 9, cy);
                path.quadTo(cx + sx * 9, cy + sy * 9, cx, cy + sy * 9);
                path.close();
                P.setStyle(Paint.Style.FILL);
                P.setColor(withAlpha(th.foam, 205));
                cv.drawPath(path, P);
                path.reset();
                path.moveTo(cx, cy);
                path.lineTo(cx + sx * 6, cy);
                path.quadTo(cx + sx * 6, cy + sy * 6, cx, cy + sy * 6);
                path.close();
                P.setColor(landColorForRound(nb[d]));
                cv.drawPath(path, P);
            }
        }
    }

    private int landColorForRound(int n) {
        return groundColor(isGround(n) ? n : th.defaultGround);
    }

    private void edgeShadow(int s, float off, float w, int alpha) {
        int col = withAlpha(0xFF000000, Math.max(0, alpha));
        switch (s) {
            case N: fill(0, off, T, off + w, col); break;
            case S: fill(0, T - off - w, T, T - off, col); break;
            case W: fill(off, 0, off + w, T, col); break;
            default: fill(T - off - w, 0, T - off, T, col); break;
        }
    }

    /** Região côncava no canto (lados a e b são terra): deixa a margem da água redonda. */
    private void roundCorner(int a, int b, int landColor) {
        boolean top = a == N || b == N, left = a == W || b == W;
        float cx = left ? 0 : T, cy = top ? 0 : T;
        float sx = left ? 1 : -1, sy = top ? 1 : -1;
        float R = 20;
        path.reset();
        path.moveTo(cx, cy);
        path.lineTo(cx + sx * R, cy);
        path.quadTo(cx, cy, cx, cy + sy * R);
        path.close();
        P.setStyle(Paint.Style.FILL);
        P.setColor(landColor);
        cv.drawPath(path, P);
        path.reset();
        path.moveTo(cx + sx * R, cy);
        path.quadTo(cx, cy, cx, cy + sy * R);
        P.setStyle(Paint.Style.STROKE);
        P.setStrokeWidth(3);
        P.setColor(withAlpha(th.foam, 205));
        cv.drawPath(path, P);
        P.setStyle(Paint.Style.FILL);
    }

    private void paintLava(int[] nb, int v, int fr) {
        fill(0, 0, T, T, th.lava);
        Random rr = new Random(v * 313L + 1);
        P.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 4; i++) {
            float x = 8 + rr.nextInt(48), y = 8 + rr.nextInt(48), w = 12 + rr.nextInt(8), h = 9 + rr.nextInt(7);
            P.setColor(withAlpha(th.lavaDark, 120));
            cv.drawOval(new android.graphics.RectF(x - w, y - h, x + w, y + h), P);
        }
        for (int i = 0; i < 4; i++) {
            float x = 10 + rr.nextInt(44) + ((fr + i) % 3 - 1) * 2, y = 10 + rr.nextInt(44), w = 6 + rr.nextInt(6), h = 4 + rr.nextInt(4);
            P.setColor(withAlpha(th.lavaLight, (i + fr) % 3 == 0 ? 210 : 120));
            cv.drawOval(new android.graphics.RectF(x - w, y - h, x + w, y + h), P);
        }
        for (int i = 0; i < 3; i++) {
            float x = 6 + rr.nextInt(48), y = 6 + rr.nextInt(52);
            wave(x, y, 10 + rr.nextInt(8), 2, 1.5f, withAlpha(th.lavaDark, 190));
        }
        for (int i = 0; i < 3; i++) if ((i + fr) % 3 == 0) circle(8 + rr.nextInt(48), 8 + rr.nextInt(48), 1.6f, 0xDDfff0a0);
        for (int s = 0; s < 4; s++) {
            int n = nb[s];
            if (isLiquid(n) || isBr(n)) continue;
            // crosta escura com brilho na beirada
            int crust = 0xFF2a1818;
            switch (s) {
                case N: fill(0, 0, T, 6, crust); fill(0, 6, T, 8, withAlpha(th.lavaLight, 180)); break;
                case S: fill(0, T - 6, T, T, crust); fill(0, T - 8, T, T - 6, withAlpha(th.lavaLight, 180)); break;
                case W: fill(0, 0, 6, T, crust); fill(6, 0, 8, T, withAlpha(th.lavaLight, 180)); break;
                default: fill(T - 6, 0, T, T, crust); fill(T - 8, 0, T - 6, T, withAlpha(th.lavaLight, 180)); break;
            }
        }
    }

    private void paintVent(int fr, int v) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(0xFF1a1012);
        cv.drawOval(new android.graphics.RectF(8, 14, 56, 56), P);
        P.setColor(th.lava);
        cv.drawOval(new android.graphics.RectF(14, 20, 50, 50), P);
        P.setColor(fr == 0 ? th.lavaLight : th.lava);
        cv.drawOval(new android.graphics.RectF(20, 26, 44, 44), P);
        circle(32, 35, 5, 0xFFfff0a0);
        for (int i = 0; i < 3; i++) circle(24 + i * 8, 14 - ((fr + i) % 3) * 4, 3 - (fr % 2), withAlpha(0xFFffb040, 150 - ((fr + i) % 3) * 40));
    }

    // ------------------------------------------------------------------
    // Rocha (penhasco) e muro de tijolos
    // ------------------------------------------------------------------

    private void paintRock(int type, int[] nb, int d, int v) {
        boolean up = isMass(nb[N]), left = isMass(nb[W]), right = isMass(nb[E]);
        boolean face = d <= 1;
        Random rr = new Random(v * 1301L + d * 17 + 3);
        if (!face) {
            // topo plano da massa de rocha
            fill(0, 0, T, T, th.cliffTop);
            for (int i = 0; i < 7; i++) {
                float x = rr.nextInt(54), y = rr.nextInt(54);
                P.setStyle(Paint.Style.FILL);
                P.setColor(shade(th.cliffTop, 0.82f + rr.nextFloat() * 0.45f));
                cv.drawOval(new android.graphics.RectF(x, y, x + 10 + rr.nextInt(12), y + 8 + rr.nextInt(10)), P);
            }
            speckle(rr, 8, shade(th.cliffTop, 0.6f), 120, 0.8f, 2);
            if (!up) { fill(0, 0, T, 5, th.cliffFace); fill(0, 5, T, 7, withAlpha(0xFF000000, 70)); }
            if (!left) { fill(0, 0, 4, T, withAlpha(th.cliffFace, 200)); }
            if (!right) { fill(T - 4, 0, T, T, withAlpha(th.cliffFace, 200)); }
            return;
        }
        float top = 0;
        float f0, f1;
        if (d == 0) { f0 = up ? 0.35f : 0f; f1 = 1f; }
        else { f0 = 0f; f1 = 0.7f; }
        vgrad(0, 0, T, T, mix(th.cliffFace, th.cliffFaceDark, f0 * 0.9f), mix(th.cliffFace, th.cliffFaceDark, f1 * 1.15f));
        // colunas de rocha (faixas verticais claras e escuras)
        for (float x = 0; x < T; ) {
            float w = 6 + rr.nextInt(9);
            fill(x, 0, Math.min(T, x + w), T, withAlpha(rr.nextBoolean() ? 0xFF000000 : 0xFFffffff, 16 + rr.nextInt(22)));
            fill(x, 0, x + 1.6f, T, withAlpha(0xFF000000, 70));
            x += w;
        }
        // estratos horizontais
        for (int i = 0; i < 3; i++) {
            float y = 14 + i * 17 + rr.nextInt(5);
            wave(0, y, T, 2.5f, 1.6f, withAlpha(th.cliffEdge, 70));
        }
        // rachaduras verticais e blocos claros
        for (int i = 0; i < 4; i++) {
            float x = 6 + rr.nextInt(52), y = rr.nextInt(24), len = 12 + rr.nextInt(18);
            line(x, y, x + (rr.nextFloat() - 0.5f) * 5, y + len, 1.6f, withAlpha(th.cliffEdge, 110));
            line(x + 2, y + 1, x + 2, y + len * 0.7f, 1.2f, withAlpha(0xFFffffff, 35));
        }
        speckle(rr, 6, shade(th.cliffFace, 1.2f), 110, 1.5f, 4);
        if (d == 0) {
            // base: sombra de contato e pedrinhas
            fill(0, T - 5, T, T, withAlpha(0xFF000000, 95));
            fill(0, T - 9, T, T - 5, withAlpha(0xFF000000, 35));
            for (int i = 0; i < 3; i++) circle(8 + rr.nextInt(48), T - 3, 2.2f, shade(th.cliffFaceDark, 0.8f));
        }
        if (!up) {
            // borda do platô: lábio de grama/neve/areia por cima da face
            path.reset();
            path.moveTo(0, 0);
            path.lineTo(T, 0);
            float h0 = 12 + rr.nextInt(3);
            for (int i = 8; i >= 0; i--) {
                float x = i * 8;
                path.lineTo(x, h0 + 3.2f * (float) Math.sin(x * 0.27f + v) + (i % 2) * 2);
            }
            path.close();
            P.setStyle(Paint.Style.FILL);
            P.setColor(th.cliffLip);
            cv.drawPath(path, P);
            fill(0, 0, T, 2, shade(th.cliffLip, 1.2f));
            // sombra logo abaixo do lábio
            fill(0, 15, T, 21, withAlpha(0xFF000000, 55));
        } else if (d == 1) {
            // topo da face encontra o topo plano: realce de aresta
            fill(0, 0, T, 3, withAlpha(0xFFffffff, 45));
        }
        // extremidades laterais
        if (!left) {
            for (int i = 0; i < 5; i++) fill(i * 2, 0, i * 2 + 2, T, withAlpha(0xFF000000, 120 - i * 22));
        }
        if (!right) {
            for (int i = 0; i < 5; i++) fill(T - i * 2 - 2, 0, T - i * 2, T, withAlpha(0xFF000000, 120 - i * 22));
        }
    }

    private void paintBrick(int[] nb, int d, int v) {
        boolean up = isMass(nb[N]), left = isMass(nb[W]), right = isMass(nb[E]);
        Random rr = new Random(v * 211L + d);
        if (d >= 1) {
            // topo do muro (passadiço de pedra)
            fill(0, 0, T, T, th.wallTop);
            for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) fill(j * 32 + 1, i * 32 + 1, j * 32 + 31, i * 32 + 31, shade(th.wallTop, 0.95f + rr.nextFloat() * 0.07f));
            line(0, 32, T, 32, 1.6f, shade(th.wallTop, 0.7f));
            line(32, 0, 32, T, 1.6f, shade(th.wallTop, 0.7f));
            speckle(rr, 6, shade(th.wallTop, 0.65f), 120, 0.8f, 1.6f);
            if (!up) { fill(0, 0, T, 4, shade(th.wallTop, 1.15f)); fill(0, 4, T, 6, withAlpha(0xFF000000, 60)); }
            if (!left) { fill(0, 0, 4, T, shade(th.wallTop, 1.15f)); fill(4, 0, 6, T, withAlpha(0xFF000000, 50)); }
            if (!right) { fill(T - 4, 0, T, T, shade(th.wallTop, 0.8f)); fill(T - 6, 0, T - 4, T, withAlpha(0xFF000000, 40)); }
            return;
        }
        float faceTop = up ? 0 : 15;
        vgrad(0, faceTop, T, T, shade(th.wallFace, 1.05f), th.wallFaceDark);
        // tijolos alinhados à grade do mapa (as juntas continuam entre tiles vizinhos)
        for (float y = faceTop; y < T; y += 8) {
            int row = (int) ((y - faceTop) / 8);
            line(0, y, T, y, 1.4f, withAlpha(th.mortar, 170));
            float off = row % 2 == 0 ? 0 : 8;
            for (float x = off; x < T; x += 16) line(x, y, x, Math.min(T, y + 8), 1.4f, withAlpha(th.mortar, 170));
        }
        for (int i = 0; i < 9; i++) {
            float x = rr.nextInt(56), y = faceTop + rr.nextInt((int) (T - faceTop - 6));
            fill(x, y, x + 12, y + 6, withAlpha(rr.nextBoolean() ? 0xFF000000 : 0xFFffffff, 20));
        }
        fill(0, T - 5, T, T, withAlpha(0xFF000000, 80));
        if (!up) {
            // coroamento do muro
            fill(0, 0, T, 15, th.wallTop);
            fill(0, 0, T, 3, shade(th.wallTop, 1.15f));
            fill(0, 12, T, 15, shade(th.wallTop, 0.72f));
            for (int i = 0; i < 4; i++) line(i * 16 + 8, 3, i * 16 + 8, 12, 1.2f, shade(th.wallTop, 0.8f));
        }
        if (!left) fill(0, 0, 4, T, withAlpha(0xFF000000, 110));
        if (!right) fill(T - 4, 0, T, T, withAlpha(0xFF000000, 110));
    }

    private void paintRoof(int[] nb, int v) {
        boolean up = nb[N] == ROOF, down = nb[S] == ROOF;
        boolean left = nb[W] == ROOF, right = nb[E] == ROOF;
        fill(0, 0, T, T, th.roof);
        for (int row = 0; row < 4; row++) {
            float y = row * 16;
            float off = (row % 2) * 8;
            for (float x = -8 + off; x < T; x += 16) {
                path.reset();
                path.moveTo(x, y);
                path.lineTo(x + 16, y);
                path.lineTo(x + 16, y + 11);
                path.quadTo(x + 8, y + 18, x, y + 11);
                path.close();
                P.setStyle(Paint.Style.FILL);
                P.setColor(shade(th.roof, 0.92f + ((int) (x + row * 3) & 3) * 0.04f));
                cv.drawPath(path, P);
                P.setStyle(Paint.Style.STROKE); P.setStrokeWidth(1.2f); P.setColor(th.roofDark);
                cv.drawPath(path, P);
            }
        }
        P.setStyle(Paint.Style.FILL);
        if (!up) { fill(0, 0, T, 8, shade(th.roof, 1.2f)); fill(0, 8, T, 10, th.roofDark); }
        if (!down) { fill(0, T - 8, T, T, th.roofDark); fill(0, T - 3, T, T, withAlpha(0xFF000000, 120)); }
        if (!left) fill(0, 0, 4, T, th.roofDark);
        if (!right) fill(T - 4, 0, T, T, th.roofDark);
    }

    private void paintHouse(int t, int[] nb, int v) {
        Random rr = new Random(v * 41L + t);
        vgrad(0, 0, T, T, shade(th.plaster, 0.9f), th.plaster);
        // beiral projeta sombra
        for (int i = 0; i < 4; i++) fill(0, i * 3, T, i * 3 + 3, withAlpha(0xFF000000, 80 - i * 18));
        fill(0, T - 8, T, T, 0xFF7a7468);
        fill(0, T - 8, T, T - 6, 0xFF9a9488);
        for (int i = 0; i < 4; i++) line(i * 16, T - 8, i * 16, T, 1.2f, 0xFF5e5a50);
        if (nb[W] != HOUSE && nb[W] != DOOR) fill(0, 0, 4, T, th.timber);
        if (nb[E] != HOUSE && nb[E] != DOOR) fill(T - 4, 0, T, T, th.timber);
        fill(0, 0, T, 4, th.timber);
        if (t == DOOR) {
            fill(16, 12, 48, T, 0xFF3e2a18);
            path.reset();
            path.moveTo(18, T); path.lineTo(18, 24); path.quadTo(32, 6, 46, 24); path.lineTo(46, T); path.close();
            P.setStyle(Paint.Style.FILL); P.setColor(0xFF7a4e2a);
            cv.drawPath(path, P);
            for (int i = 0; i < 4; i++) line(18 + i * 7 + 2, 14, 18 + i * 7 + 2, T, 1.2f, 0xFF4e3016);
            circle(40, 42, 2, 0xFFe6c45a);
            fill(14, T - 3, 50, T, 0xFFa09a8c);
        } else if ((v & 1) == 0) {
            fill(18, 16, 46, 42, th.timber);
            fill(21, 19, 43, 39, 0xFF8fc4e8);
            fill(21, 19, 43, 25, 0xFFb4dcf4);
            line(32, 19, 32, 39, 2, th.timber);
            line(21, 29, 43, 29, 2, th.timber);
            fill(16, 42, 48, 46, shade(th.timber, 0.85f));
            circle(24, 44, 2, 0xFFd45a7a);
            circle(32, 44, 2, 0xFFf0c040);
            circle(40, 44, 2, 0xFFd45a7a);
        } else {
            for (int i = 0; i < 3; i++) line(8, 20 + i * 10, 56, 20 + i * 10, 1.2f, withAlpha(0xFF000000, 25));
        }
    }

    // ------------------------------------------------------------------
    // Escada, ponte e fonte
    // ------------------------------------------------------------------

    private void paintStairs(int[] nb, int under, int v) {
        boolean upMass = isMass(nb[N]), downMass = isMass(nb[S]);
        boolean upSt = nb[N] == STAIRS, downSt = nb[S] == STAIRS;
        boolean wallL = isMass(nb[W]), wallR = isMass(nb[E]);
        paintGroundBase(under, v, nb);
        vgrad(0, 0, T, T, mix(th.stairs, 0xFFffffff, upSt ? 0.18f : 0.28f), mix(th.stairs, 0xFF000000, downSt ? 0.25f : 0.1f));
        int steps = 4;
        for (int i = 0; i < steps; i++) {
            float y0 = i * 16f;
            float lighten = 1.22f - i * 0.09f;
            fill(0, y0, T, y0 + 5, shade(th.stairs, lighten + 0.1f));
            fill(0, y0 + 5, T, y0 + 16, shade(th.stairs, lighten - 0.22f));
            fill(0, y0 + 15, T, y0 + 16, withAlpha(0xFF000000, 120));
            fill(0, y0, T, y0 + 1, withAlpha(0xFFffffff, 70));
        }
        if (!upSt) fill(0, 0, T, 6, shade(th.stairs, 1.25f));
        // paredes laterais da escada
        for (int s = 0; s < 2; s++) {
            boolean w = s == 0 ? wallL : wallR;
            boolean sameSt = (s == 0 ? nb[W] : nb[E]) == STAIRS;
            float x0 = s == 0 ? 0 : T - 6;
            if (w) fill(x0, 0, x0 + 6, T, withAlpha(0xFF000000, 90));
            else if (!sameSt) {
                fill(s == 0 ? 0 : T - 5, 0, s == 0 ? 5 : T, T, th.stairsRail);
                fill(s == 0 ? 5 : T - 6, 0, s == 0 ? 6 : T - 5, T, withAlpha(0xFF000000, 80));
            }
        }
        if (!downSt) {
            fill(0, T - 4, T, T, withAlpha(0xFF000000, 80));
        }
    }

    private void paintBridge(int t, int[] nb, int under, int dir, int v, int fr) {
        // por baixo: água ou lava
        if (under == LAVA) paintLava(new int[]{LAVA, LAVA, LAVA, LAVA, LAVA, LAVA, LAVA, LAVA}, v, fr);
        else if (under == WATER) paintWaterPlain(v, fr);
        else paintGroundBase(under, v, nb);
        boolean lava = under == LAVA;
        int wood = lava ? th.bridgeLava : th.wood, woodD = shade(wood, 0.62f);
        boolean horiz = dir == 0;
        if (horiz) {
            // viaja na horizontal: tábuas verticais
            boolean railN = !isBr(nb[N]), railS = !isBr(nb[S]);
            float y0 = railN ? 7 : 0, y1 = railS ? T - 7 : T;
            fill(0, y0, T, y1, wood);
            for (int x = 0; x < T; x += 8) {
                fill(x, y0, x + 7, y1, shade(wood, 0.96f + ((x / 8 + v) % 3) * 0.04f));
                line(x + 7.5f, y0, x + 7.5f, y1, 1, woodD);
            }
            fill(0, y0, T, y0 + 2, withAlpha(0xFFffffff, 40));
            if (railN) { fill(0, 3, T, 9, woodD); fill(0, 3, T, 5, shade(wood, 1.2f)); for (int x = 4; x < T; x += 20) fill(x, 1, x + 4, 12, shade(wood, 0.5f)); }
            if (railS) { fill(0, T - 9, T, T - 3, woodD); fill(0, T - 9, T, T - 7, shade(wood, 1.1f)); for (int x = 4; x < T; x += 20) fill(x, T - 12, x + 4, T - 1, shade(wood, 0.5f)); }
            if (!isBr(nb[W]) && !isLiquid(nb[W])) fill(0, y0, 4, y1, woodD);
        } else {
            boolean railW = !isBr(nb[W]), railE = !isBr(nb[E]);
            float x0 = railW ? 7 : 0, x1 = railE ? T - 7 : T;
            fill(x0, 0, x1, T, wood);
            for (int y = 0; y < T; y += 8) {
                fill(x0, y, x1, y + 7, shade(wood, 0.96f + ((y / 8 + v) % 3) * 0.04f));
                line(x0, y + 7.5f, x1, y + 7.5f, 1, woodD);
            }
            if (railW) { fill(3, 0, 9, T, woodD); fill(3, 0, 5, T, shade(wood, 1.2f)); for (int y = 4; y < T; y += 20) fill(1, y, 12, y + 4, shade(wood, 0.5f)); }
            if (railE) { fill(T - 9, 0, T - 3, T, woodD); fill(T - 9, 0, T - 7, T, shade(wood, 1.1f)); for (int y = 4; y < T; y += 20) fill(T - 12, y, T - 1, y + 4, shade(wood, 0.5f)); }
        }
    }

    private void paintWaterPlain(int v, int fr) {
        paintWater(WATER, new int[]{WATER, WATER, WATER, WATER, WATER, WATER, WATER, WATER}, v, fr);
    }

    private void paintFountain(int[] nb, int v, int fr) {
        paintGroundBase(STONE_PAVEMENT, v, nb);
        boolean n = nb[N] == FOUNTAIN, s = nb[S] == FOUNTAIN, w = nb[W] == FOUNTAIN, e = nb[E] == FOUNTAIN;
        float l = w ? 0 : 6, r = e ? T : T - 6, tp = n ? 0 : 6, bt = s ? T : T - 6;
        // borda de pedra
        fill(l, tp, r, bt, 0xFF8f8c84);
        float il = w ? 0 : 12, ir = e ? T : T - 12, it = n ? 0 : 12, ib = s ? T : T - 12;
        fill(il, it, ir, ib, 0xFF6a6862);
        fill(il, it, ir, ib, th.water);
        circle(32, 32, 28, withAlpha(th.waterLight, 40));
        Random rr = new Random(v + 99);
        for (int k = 0; k < 3; k++) wave(il + 4 + rr.nextInt(10) + fr * 2, it + 10 + k * 14, 14, 2.5f, 1.4f, withAlpha(th.waterLight, 160));
        if (!n) fill(il, it, ir, it + 3, withAlpha(0xFF000000, 70));
        if (!w) fill(il, it, il + 3, ib, withAlpha(0xFF000000, 50));
        if (!n) fill(6, 6, T - 6, 9, 0xFFb4b0a6);
        if (n && s && w && e) {
            circle(32, 34, 9, 0xFF8f8c84);
            circle(32, 32, 7, 0xFFb4b0a6);
            for (int i = 0; i < 3; i++) line(32, 30, 32 + (i - 1) * 5 + (fr - 1) * 2, 18 - i, 1.8f, withAlpha(0xFFd8f0ff, 200));
            circle(32, 32, 3, th.waterLight);
        }
        if (!s) fill(6, T - 9, T - 6, T - 6, 0xFF6e6c66);
    }

    // ------------------------------------------------------------------
    // Objetos
    // ------------------------------------------------------------------

    private void paintPortal(int fr) {
        P.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 5; i++) {
            float rad = 30 - i * 5;
            circle(32, 32, rad, withAlpha(i % 2 == 0 ? 0xFF7a4cff : 0xFF54d0ff, 60 + i * 22));
        }
        for (int i = 0; i < 3; i++) {
            double a = (fr * 2.1 + i * 2.09);
            circle(32 + (float) Math.cos(a) * 15, 32 + (float) Math.sin(a) * 15, 3, 0xEEffffff);
        }
        circle(32, 32, 5, 0xFFffffff);
    }

    private void paintBush(int v) {
        Bitmap s = sprite(BUSH);
        if (s != null) {
            cv.drawBitmap(s, null, rect(-4, -6, T + 4, T + 2), null);
            return;
        }
        P.setStyle(Paint.Style.FILL);
        P.setColor(0x40000000);
        cv.drawOval(new android.graphics.RectF(8, 46, 56, 62), P);
        circle(22, 40, 14, 0xFF2f7a2a);
        circle(42, 40, 14, 0xFF2f7a2a);
        circle(32, 32, 16, 0xFF3f9a36);
        circle(28, 28, 6, 0xFF5cbc4a);
    }

    private void paintBoulder(int v) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(0x55000000);
        cv.drawOval(new android.graphics.RectF(6, 44, 58, 62), P);
        int base = th.boulder;
        float[][] rocks = v % 2 == 0 ? new float[][]{{30, 36, 20, 16}, {48, 46, 10, 8}} : new float[][]{{34, 38, 18, 15}, {16, 48, 10, 8}};
        for (float[] rk : rocks) {
            P.setColor(shade(base, 0.7f));
            cv.drawOval(new android.graphics.RectF(rk[0] - rk[2], rk[1] - rk[3] + 2, rk[0] + rk[2], rk[1] + rk[3] + 2), P);
            P.setColor(base);
            cv.drawOval(new android.graphics.RectF(rk[0] - rk[2], rk[1] - rk[3], rk[0] + rk[2], rk[1] + rk[3]), P);
            P.setColor(shade(base, 1.25f));
            cv.drawOval(new android.graphics.RectF(rk[0] - rk[2] * 0.6f, rk[1] - rk[3] * 0.85f, rk[0] + rk[2] * 0.1f, rk[1] - rk[3] * 0.1f), P);
            line(rk[0] + 2, rk[1] - 2, rk[0] + 6, rk[1] + rk[3] * 0.6f, 1.2f, shade(base, 0.55f));
        }
    }

    private void paintSign(int v) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(0x44000000);
        cv.drawOval(new android.graphics.RectF(16, 50, 48, 62), P);
        fill(29, 28, 35, 58, 0xFF6e4524);
        fill(12, 12, 52, 36, 0xFF9a6a3a);
        fill(12, 12, 52, 15, 0xFFc08a52);
        fill(12, 33, 52, 36, 0xFF6e4524);
        for (int i = 0; i < 3; i++) line(18, 20 + i * 5, 46 - (i % 2) * 8, 20 + i * 5, 1.6f, 0xFF4e3016);
        circle(14, 14, 1.4f, 0xFF3a2410);
        circle(50, 14, 1.4f, 0xFF3a2410);
    }

    private void paintBrazier(int fr) {
        P.setStyle(Paint.Style.FILL);
        circle(32, 30, 26, withAlpha(0xFFff8a20, 40));
        P.setColor(0x55000000);
        cv.drawOval(new android.graphics.RectF(12, 50, 52, 62), P);
        fill(28, 40, 36, 56, 0xFF55524a);
        fill(20, 54, 44, 60, 0xFF6a675e);
        path.reset();
        path.moveTo(14, 30); path.lineTo(50, 30); path.lineTo(42, 44); path.lineTo(22, 44); path.close();
        P.setColor(0xFF7a766c);
        cv.drawPath(path, P);
        fill(14, 28, 50, 32, 0xFF9a968a);
        float h = 14 + fr * 3;
        path.reset();
        path.moveTo(20, 30); path.quadTo(22, 30 - h, 32, 10 - fr); path.quadTo(42, 30 - h, 44, 30); path.close();
        P.setColor(0xFFff6a14);
        cv.drawPath(path, P);
        path.reset();
        path.moveTo(25, 30); path.quadTo(27, 30 - h * 0.7f, 32, 17); path.quadTo(37, 30 - h * 0.7f, 39, 30); path.close();
        P.setColor(0xFFffc030);
        cv.drawPath(path, P);
    }

    private void paintTomb(int v) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(0x55000000);
        cv.drawOval(new android.graphics.RectF(12, 50, 54, 62), P);
        path.reset();
        path.moveTo(16, 56); path.lineTo(16, 24); path.quadTo(32, 4, 48, 24); path.lineTo(48, 56); path.close();
        P.setColor(0xFF7e7e88);
        cv.drawPath(path, P);
        path.reset();
        path.moveTo(20, 56); path.lineTo(20, 26); path.quadTo(32, 10, 44, 26); path.lineTo(44, 56); path.close();
        P.setColor(0xFF9a9aa4);
        cv.drawPath(path, P);
        line(32, 22, 32, 40, 3, 0xFF5e5e68);
        line(26, 28, 38, 28, 3, 0xFF5e5e68);
        if (v % 2 == 0) { circle(22, 50, 4, 0xFF4a7a36); circle(42, 53, 3, 0xFF4a7a36); }
    }

    private void paintCrystal(int main, int light, int dark, int v) {
        P.setStyle(Paint.Style.FILL);
        P.setColor(0x55000000);
        cv.drawOval(new android.graphics.RectF(8, 48, 56, 62), P);
        float[][] cr = {{22, 52, 9, 28}, {38, 54, 11, 36}, {50, 52, 7, 20}};
        for (float[] c : cr) {
            path.reset();
            path.moveTo(c[0] - c[2], c[1]); path.lineTo(c[0] - c[2] * 0.6f, c[1] - c[3] * 0.7f); path.lineTo(c[0], c[1] - c[3]);
            path.lineTo(c[0] + c[2] * 0.6f, c[1] - c[3] * 0.7f); path.lineTo(c[0] + c[2], c[1]); path.close();
            P.setStyle(Paint.Style.FILL); P.setColor(main);
            cv.drawPath(path, P);
            path.reset();
            path.moveTo(c[0], c[1]); path.lineTo(c[0], c[1] - c[3]); path.lineTo(c[0] + c[2] * 0.6f, c[1] - c[3] * 0.7f); path.lineTo(c[0] + c[2], c[1]); path.close();
            P.setColor(dark);
            cv.drawPath(path, P);
            line(c[0] - c[2] * 0.4f, c[1] - c[3] * 0.3f, c[0] - c[2] * 0.2f, c[1] - c[3] * 0.8f, 1.6f, light);
        }
    }

    // ------------------------------------------------------------------
    // Objetos altos (bitmap próprio, desenhado na segunda passada)
    // ------------------------------------------------------------------

    private void paintTall(int t, int v, int h) {
        P.setStyle(Paint.Style.FILL);
        float b = h; // base no pé do bitmap
        switch (t) {
            case DEAD_TREE: {
                int bark = 0xFF4a3a30;
                fill(27, b - 52, 37, b - 8, bark);
                fill(27, b - 52, 31, b - 8, shade(bark, 1.3f));
                // galhos retorcidos
                line(32, b - 40, 14, b - 66, 4, bark);
                line(14, b - 66, 8, b - 90, 2.6f, bark);
                line(14, b - 66, 22, b - 84, 2.2f, bark);
                line(32, b - 46, 52, b - 70, 3.6f, bark);
                line(52, b - 70, 58, b - 96, 2.4f, bark);
                line(52, b - 70, 44, b - 88, 2, bark);
                line(32, b - 52, 30, b - 90, 3.4f, bark);
                line(30, b - 90, 24, b - 108, 2, bark);
                line(30, b - 80, 40, b - 100, 2, bark);
                path.reset();
                path.moveTo(24, b - 8); path.quadTo(32, b - 20, 40, b - 8); path.lineTo(46, b); path.lineTo(18, b); path.close();
                P.setStyle(Paint.Style.FILL); P.setColor(bark);
                cv.drawPath(path, P);
                break;
            }
            case PILLAR: {
                int st = th.pillar;
                P.setColor(0x55000000);
                cv.drawOval(new android.graphics.RectF(10, b - 12, 54, b), P);
                fill(14, b - 14, 50, b - 4, shade(st, 0.8f));
                fill(14, b - 14, 50, b - 11, shade(st, 1.1f));
                hgrad(19, 22, 45, b - 14, shade(st, 1.2f), shade(st, 0.7f));
                for (int i = 0; i < 4; i++) line(24 + i * 5, 24, 24 + i * 5, b - 16, 1, withAlpha(0xFF000000, 40));
                fill(14, 10, 50, 22, shade(st, 0.85f));
                fill(14, 10, 50, 14, shade(st, 1.2f));
                fill(18, 22, 46, 25, shade(st, 0.6f));
                fill(18, 4, 46, 10, shade(st, 1.0f));
                break;
            }
            case CACTUS: {
                int gcol = 0xFF3a8a3e;
                P.setColor(0x44000000);
                cv.drawOval(new android.graphics.RectF(16, b - 12, 50, b), P);
                android.graphics.RectF body = new android.graphics.RectF(25, 14, 41, b - 6);
                P.setColor(gcol); cv.drawRoundRect(body, 8, 8, P);
                fill(35, 20, 40, b - 12, shade(gcol, 0.8f));
                fill(26, 20, 30, b - 12, shade(gcol, 1.2f));
                android.graphics.RectF arm1 = new android.graphics.RectF(10, 34, 24, 70);
                P.setColor(gcol); cv.drawRoundRect(arm1, 6, 6, P);
                fill(24, 56, 28, 66, gcol);
                android.graphics.RectF arm2 = new android.graphics.RectF(42, 28, 56, 56);
                cv.drawRoundRect(arm2, 6, 6, P);
                fill(38, 48, 44, 58, gcol);
                for (int i = 0; i < 8; i++) line(28 + (i % 2) * 8, 20 + i * 7, 30 + (i % 2) * 8, 18 + i * 7, 1, 0xFFeaf2c0);
                if (v % 2 == 0) { circle(33, 12, 3.2f, 0xFFf06a9a); circle(33, 12, 1.2f, 0xFFffe08a); }
                break;
            }
            case SNOW_CRYSTAL: {
                P.setColor(0x33000000);
                cv.drawOval(new android.graphics.RectF(8, b - 14, 58, b - 2), P);
                float[][] cr = {{24, b - 6, 11, 50}, {40, b - 4, 13, 70}, {52, b - 6, 8, 36}};
                for (float[] c : cr) {
                    path.reset();
                    path.moveTo(c[0] - c[2], c[1]); path.lineTo(c[0] - c[2] * 0.7f, c[1] - c[3] * 0.65f); path.lineTo(c[0], c[1] - c[3]);
                    path.lineTo(c[0] + c[2] * 0.7f, c[1] - c[3] * 0.65f); path.lineTo(c[0] + c[2], c[1]); path.close();
                    P.setStyle(Paint.Style.FILL); P.setColor(0xFF9ad8f4);
                    cv.drawPath(path, P);
                    path.reset();
                    path.moveTo(c[0], c[1]); path.lineTo(c[0], c[1] - c[3]); path.lineTo(c[0] + c[2] * 0.7f, c[1] - c[3] * 0.65f); path.lineTo(c[0] + c[2], c[1]); path.close();
                    P.setColor(0xFF5eaed8);
                    cv.drawPath(path, P);
                    line(c[0] - c[2] * 0.45f, c[1] - c[3] * 0.25f, c[0] - c[2] * 0.2f, c[1] - c[3] * 0.8f, 2, 0xE6ffffff);
                }
                break;
            }
            default: break;
        }
    }

    // ------------------------------------------------------------------
    // Temas (paletas) por mapa
    // ------------------------------------------------------------------

    static final class Theme {
        int defaultGround = GRASS;
        boolean cobbleRoad = false;
        int grass = 0xFF4f9a2f, grassDark = 0xFF3b7d24, grassLight = 0xFF73b848;
        int sand = 0xFFdcc58a, sandDark = 0xFFb89c5c;
        int snow = 0xFFf2f6fa, snowShade = 0xFFc4d6e6;
        int road = 0xFFa88658, roadDark = 0xFF7e6038;
        int pave = 0xFFaaa69c, paveDark = 0xFF7a766c;
        int floor = 0xFF8e8e98, floorDark = 0xFF5e5e6a;
        int soil = 0xFF6b4a2a, soilDark = 0xFF45301a;
        int ash = 0xFF6b6565, basalt = 0xFF2f2f37, obsidian = 0xFF2a1d3a, obsidianGlow = 0xFFb06cff;
        int cliffTop = 0xFF6b5a45, cliffFace = 0xFF9a8062, cliffFaceDark = 0xFF5e4a36, cliffLip = 0xFF5aa83a, cliffEdge = 0xFF3a2c1e;
        int wallTop = 0xFFb9b4a6, wallFace = 0xFF8f8a7e, wallFaceDark = 0xFF65615a, mortar = 0xFF4a463e;
        int roof = 0xFFb04a30, roofDark = 0xFF6e2a1c, plaster = 0xFFe8dcc0, timber = 0xFF6a4a2e;
        int water = 0xFF2f8fdc, waterDark = 0xFF1f6fb8, waterLight = 0xFF86d0f4, deep = 0xFF1d62ad, foam = 0xFFeaf8ff;
        int lava = 0xFFd8400e, lavaDark = 0xFF8a2008, lavaLight = 0xFFffb024;
        int wood = 0xFF9a6a3a, bridgeLava = 0xFF7a5a48;
        int stairs = 0, stairsRail = 0;
        int boulder = 0xFF8a8a90, pillar = 0xFFc8c4b8;

        static Theme forMap(String m) {
            Theme t = new Theme();
            switch (m == null ? "" : m) {
                case "CITY":
                    t.cobbleRoad = true;
                    t.wallTop = 0xFFd6d1c2; t.wallFace = 0xFFa9a495; t.wallFaceDark = 0xFF7a766a;
                    t.pave = 0xFFb8b4aa; t.paveDark = 0xFF858176;
                    t.cliffLip = 0xFF5aa83a;
                    break;
                case "HOME":
                    t.cobbleRoad = true;
                    t.wallTop = 0xFFc2b8a2; t.wallFace = 0xFF968a76; t.wallFaceDark = 0xFF6a604e;
                    t.cliffLip = 0xFF5aa83a;
                    break;
                case "TOWER":
                case "DUEL_ARENA":
                    t.defaultGround = FLOOR; t.cobbleRoad = true;
                    t.floor = 0xFF86838e; t.floorDark = 0xFF54525e;
                    t.wallTop = 0xFF9c98a6; t.wallFace = 0xFF6e6a7a; t.wallFaceDark = 0xFF46424f; t.mortar = 0xFF2e2b36;
                    t.pillar = 0xFFa8a4b6; t.stairs = 0xFF9a96a8;
                    break;
                case "SMALL_LAKE":
                    t.water = 0xFF3aa0e0; t.deep = 0xFF1c64a8; t.sand = 0xFFe6d29a;
                    t.cliffLip = 0xFF5aa83a;
                    break;
                case "ROCKY_PATH":
                    t.defaultGround = FLOOR;
                    t.floor = 0xFF9a9588; t.floorDark = 0xFF6e695c;
                    t.cliffTop = 0xFF5e5648; t.cliffFace = 0xFF8e8472; t.cliffFaceDark = 0xFF584f40; t.cliffLip = 0xFF74a04a;
                    t.road = 0xFFb59c70;
                    break;
                case "JUNGLE":
                    t.cliffTop = 0xFF4a5a3a; t.cliffFace = 0xFF76805e; t.cliffFaceDark = 0xFF424b36; t.cliffLip = 0xFF3f8a28;
                    t.water = 0xFF2a8a9a; t.deep = 0xFF14607a; t.waterLight = 0xFF7ad0d0;
                    t.wallTop = 0xFFa8a690; t.wallFace = 0xFF7a8068; t.wallFaceDark = 0xFF4e5642;
                    t.road = 0xFF9a7a4a;
                    break;
                case "MAGMA_CAVE":
                    t.defaultGround = OBSIDIAN;
                    t.cliffTop = 0xFF1c1620; t.cliffFace = 0xFF4a3a52; t.cliffFaceDark = 0xFF241a2c; t.cliffLip = 0xFF5e4a6a; t.cliffEdge = 0xFF100a14;
                    t.obsidian = 0xFF2c2038; t.floor = 0xFF4a4252; t.floorDark = 0xFF2a2430;
                    t.bridgeLava = 0xFF7a5a48; t.boulder = 0xFF4a4252;
                    break;
                case "SWAMP":
                    t.grass = 0xFF47822a;
                    t.water = 0xFF4b6e3c; t.deep = 0xFF2a4528; t.waterLight = 0xFF9ab87a; t.foam = 0xFFc8d8a8; t.waterDark = 0xFF33502a;
                    t.wood = 0xFF6e5a3a;
                    t.cliffLip = 0xFF47822a;
                    break;
                case "FIRE_CAVE":
                    t.defaultGround = FLOOR;
                    t.floor = 0xFF6a5a58; t.floorDark = 0xFF3e3230;
                    t.cliffTop = 0xFF241818; t.cliffFace = 0xFF5a3a36; t.cliffFaceDark = 0xFF2c1c1a; t.cliffLip = 0xFF7a5046; t.cliffEdge = 0xFF140c0a;
                    t.bridgeLava = 0xFF7a5a48; t.boulder = 0xFF6a5a58; t.pillar = 0xFF8a7a76; t.stairs = 0xFF8a7a76;
                    break;
                case "SEWERS":
                    t.defaultGround = FLOOR;
                    t.floor = 0xFF6e7266; t.floorDark = 0xFF3c4038; t.pave = 0xFF7a7e70; t.paveDark = 0xFF464a40;
                    t.wallTop = 0xFF7c8a70; t.wallFace = 0xFF566350; t.wallFaceDark = 0xFF34402f; t.mortar = 0xFF1e271c;
                    t.water = 0xFF4a7a3c; t.deep = 0xFF2a4a26; t.waterLight = 0xFF9cc47a; t.foam = 0xFFb4d490; t.waterDark = 0xFF33582c;
                    t.pillar = 0xFF8a9080; t.stairs = 0xFF8a9080; t.wood = 0xFF6e5a3a;
                    break;
                case "VALE_ROCHOSO":
                    t.cliffTop = 0xFF8a6a4a; t.cliffFace = 0xFFc29a68; t.cliffFaceDark = 0xFF8a6842; t.cliffLip = 0xFFd8c080; t.cliffEdge = 0xFF4a341e;
                    t.road = 0xFFb89a68; t.sand = 0xFFe2cb92;
                    t.boulder = 0xFF9a9084;
                    break;
                case "MINE":
                    t.defaultGround = FLOOR;
                    t.floor = 0xFF776f66; t.floorDark = 0xFF48423b; t.road = 0xFF8a7250; t.roadDark = 0xFF5a4630;
                    t.cliffTop = 0xFF26221e; t.cliffFace = 0xFF5a5046; t.cliffFaceDark = 0xFF2c2620; t.cliffLip = 0xFF6e6458; t.cliffEdge = 0xFF14100c;
                    t.pillar = 0xFF8a6a40; t.boulder = 0xFF6e665c; t.stairs = 0xFF8a8070;
                    break;
                case "RIO":
                    t.water = 0xFF3598e0; t.deep = 0xFF1a5aa0;
                    t.cliffLip = 0xFF5aa83a;
                    break;
                case "OCEAN":
                    t.water = 0xFF38b0e8; t.deep = 0xFF145aa0; t.waterLight = 0xFF9ae0f8;
                    t.wallTop = 0xFFd6d0bc; t.wallFace = 0xFFa8a290; t.wallFaceDark = 0xFF77725f;
                    t.sand = 0xFFeedca0;
                    break;
                case "DESERT":
                    t.defaultGround = SAND;
                    t.sand = 0xFFe3c887; t.sandDark = 0xFFbf9e5a; t.road = 0xFFd0aa68; t.roadDark = 0xFFa88540;
                    t.cliffTop = 0xFFb8904e; t.cliffFace = 0xFFd8aa62; t.cliffFaceDark = 0xFF9a7240; t.cliffLip = 0xFFecd08e; t.cliffEdge = 0xFF5e4020;
                    t.wallTop = 0xFFe0c892; t.wallFace = 0xFFc2a56e; t.wallFaceDark = 0xFF8c7448; t.mortar = 0xFF6e5a34;
                    t.pave = 0xFFcdb888; t.paveDark = 0xFF9a8658;
                    t.boulder = 0xFFa89870; t.pillar = 0xFFe0c892; t.stairs = 0xFFd0b680;
                    break;
                case "VOLCANO":
                    t.defaultGround = BASALT_FLOOR;
                    t.cliffTop = 0xFF1c1519; t.cliffFace = 0xFF4a3238; t.cliffFaceDark = 0xFF22171b; t.cliffLip = 0xFF63404a; t.cliffEdge = 0xFF0e0709;
                    t.road = 0xFF5a4a4a; t.roadDark = 0xFF362a2a;
                    t.bridgeLava = 0xFF7a5a48; t.boulder = 0xFF4a4048;
                    break;
                case "MOLTEN_SANCTUM":
                case "MOLTEN_SANCTUM_BEGINNER":
                    t.defaultGround = BASALT_FLOOR;
                    t.cliffTop = 0xFF1a1317; t.cliffFace = 0xFF4a3040; t.cliffFaceDark = 0xFF201419; t.cliffLip = 0xFF6a3c4a; t.cliffEdge = 0xFF0c0608;
                    t.bridgeLava = 0xFF7a5a48; t.pillar = 0xFF5a4650; t.boulder = 0xFF4a4048;
                    break;
                case "SNOW_MOUNTAIN":
                    t.defaultGround = SNOW;
                    t.cliffTop = 0xFFe6eff7; t.cliffFace = 0xFFb0cce4; t.cliffFaceDark = 0xFF7a9ab8; t.cliffLip = 0xFFffffff; t.cliffEdge = 0xFF4a6682;
                    t.water = 0xFF7fc6e8; t.deep = 0xFF3a82b8; t.waterLight = 0xFFd0f0ff; t.foam = 0xFFffffff;
                    t.road = 0xFFb8a888; t.roadDark = 0xFF8a7c60;
                    t.boulder = 0xFF8a8e98;
                    break;
                case "SNOW_PEAK":
                    t.defaultGround = SNOW;
                    t.cliffTop = 0xFFe6eff7; t.cliffFace = 0xFFb6d2ea; t.cliffFaceDark = 0xFF7e9ebc; t.cliffLip = 0xFFffffff; t.cliffEdge = 0xFF4a6682;
                    t.road = 0xFFb8a888; t.roadDark = 0xFF8a7c60; t.boulder = 0xFF8a8e98; t.pillar = 0xFFdce8f2;
                    break;
                case "FEAR_ISLAND":
                    t.grass = 0xFF4a6034; t.grassDark = 0xFF33482a;
                    t.water = 0xFF2c4478; t.deep = 0xFF121c40; t.waterLight = 0xFF6a82c0; t.foam = 0xFFb8c4e8; t.waterDark = 0xFF1e2e58;
                    t.sand = 0xFFb8a888; t.sandDark = 0xFF8a7a5a;
                    t.wallTop = 0xFF8a8296; t.wallFace = 0xFF605a70; t.wallFaceDark = 0xFF3c3648; t.mortar = 0xFF241f2e;
                    t.pave = 0xFF76707e; t.paveDark = 0xFF4a4654; t.wood = 0xFF5e4a34;
                    t.pillar = 0xFF8a8296; t.stairs = 0xFF7e788a; t.road = 0xFF7a6a50; t.roadDark = 0xFF54482e;
                    break;
                default: break;
            }
            if (t.stairs == 0) t.stairs = mix(t.cliffFace, 0xFFffffff, 0.38f);
            if (t.stairsRail == 0) t.stairsRail = shade(t.cliffFaceDark, 0.8f);
            return t;
        }
    }
}
