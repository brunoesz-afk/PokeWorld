package com.example.pokeworld.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Grade da base: guarda as peças, valida posições e responde colisão.
 * Lógica pura (sem Android) para poder ser testada fora do aparelho.
 */
public class BaseLayout implements MovementBlocker {
    private static final int T = 64; // = TileMap.TILE_SIZE
    /** Tamanho do mapa HOME (pequeno de propósito: menos tiles desenhados = mais leve). */
    public static final int MAP_ROWS = 56, MAP_COLS = 64;
    /** Terreno edificável (em tiles). */
    public static final int PLOT_X = 14, PLOT_Y = 8, PLOT_W = 36, PLOT_H = 36;
    /** Anel de muralha inicial (células do anel) e portão. */
    public static final int RING_L = 19, RING_R = 44, RING_T = 13, RING_B = 38, GATE_X0 = 30, GATE_X1 = 33;
    /** Onde nasce o Quartel (4x4, no centro do anel). */
    public static final int QUARTEL_X = 30, QUARTEL_Y = 24;

    private final int rows, cols;
    private final BasePiece[][] grid;
    private final List<BasePiece> pieces = new ArrayList<>();

    public BaseLayout(int rows, int cols) {
        this.rows = rows; this.cols = cols;
        this.grid = new BasePiece[rows][cols];
    }

    public List<BasePiece> getPieces() { return pieces; }
    public BasePiece at(int gx, int gy) {
        return (gy < 0 || gy >= rows || gx < 0 || gx >= cols) ? null : grid[gy][gx];
    }

    public void add(BasePiece p) { pieces.add(p); mark(p, p); sort(); }
    public void remove(BasePiece p) { mark(p, null); pieces.remove(p); }
    public void clear() { for (BasePiece[] r : grid) java.util.Arrays.fill(r, null); pieces.clear(); }

    public void move(BasePiece p, int gx, int gy) {
        mark(p, null); p.gx = gx; p.gy = gy; mark(p, p); sort();
    }

    private void mark(BasePiece p, BasePiece v) {
        for (int y = p.gy; y < p.gy + p.kind.h; y++)
            for (int x = p.gx; x < p.gx + p.kind.w; x++)
                if (y >= 0 && y < rows && x >= 0 && x < cols) grid[y][x] = v;
    }

    /** Ordem de desenho: de cima para baixo (pela borda inferior). */
    private void sort() {
        Collections.sort(pieces, new Comparator<BasePiece>() {
            @Override public int compare(BasePiece a, BasePiece b) {
                int d = (a.gy + a.kind.h) - (b.gy + b.kind.h);
                return d != 0 ? d : a.gx - b.gx;
            }
        });
    }

    public static boolean inPlot(int gx, int gy, int w, int h) {
        return gx >= PLOT_X && gy >= PLOT_Y && gx + w <= PLOT_X + PLOT_W && gy + h <= PLOT_Y + PLOT_H;
    }

    /** A área está livre (ignorando a própria peça, ao movê-la)? */
    public boolean isFree(BaseKind k, int gx, int gy, BasePiece ignore) {
        if (!inPlot(gx, gy, k.w, k.h)) return false;
        for (int y = gy; y < gy + k.h; y++)
            for (int x = gx; x < gx + k.w; x++) {
                BasePiece o = grid[y][x];
                if (o != null && o != ignore) return false;
            }
        return true;
    }

    public int count(BaseKind k) {
        int n = 0;
        for (BasePiece p : pieces) if (p.kind == k) n++;
        return n;
    }

    public BasePiece find(BaseKind k) {
        for (BasePiece p : pieces) if (p.kind == k) return p;
        return null;
    }

    public int hqLevel() {
        BasePiece q = find(BaseKind.QUARTEL);
        return q == null ? 1 : q.level;
    }

    /** Procura em espiral a célula livre mais próxima para o tipo. null se não houver. */
    public int[] nearestFree(BaseKind k, int gx, int gy) {
        for (int r = 0; r < 30; r++)
            for (int dy = -r; dy <= r; dy++)
                for (int dx = -r; dx <= r; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != r) continue;
                    if (isFree(k, gx + dx, gy + dy, null)) return new int[]{gx + dx, gy + dy};
                }
        return null;
    }

    // ----- Muralhas: ligação automática -----
    public static final int N = 1, E = 2, S = 4, W = 8;
    public int wallMask(int gx, int gy) {
        int m = 0;
        if (joins(at(gx, gy - 1))) m |= N;
        if (joins(at(gx + 1, gy))) m |= E;
        if (joins(at(gx, gy + 1))) m |= S;
        if (joins(at(gx - 1, gy))) m |= W;
        return m;
    }

    /** Muralha liga em outra muralha e nas torres. */
    private static boolean joins(BasePiece p) {
        return p != null && (p.kind.isWall() || p.kind == BaseKind.TORRE_VIGIA || p.kind == BaseKind.TORRE_CHOQUE);
    }

    // ----- Colisão -----
    /** Prédios grandes deixam a última linha livre (frente), para o jogador chegar nas portas/NPCs. */
    @Override
    public boolean blocks(double wx, double wy) {
        int gx = (int) Math.floor(wx / T), gy = (int) Math.floor(wy / T);
        BasePiece p = at(gx, gy);
        if (p == null) return false;
        if (p.kind.isWall() || p.kind.isDefense() || p.kind == BaseKind.TOCHA) return true;
        if (p.kind.h >= 3 && gy == p.gy + p.kind.h - 1) return false;
        return true;
    }

    public boolean isBlockedCell(int gx, int gy) {
        return blocks(gx * T + T / 2.0, gy * T + T / 2.0);
    }

    // ----- Base inicial -----
    /** Quartel + anel de muralha (nível 1) com portão ao sul + 4 torres de vigia nos cantos. */
    public static List<BasePiece> defaultPieces() {
        List<BasePiece> out = new ArrayList<>();
        out.add(new BasePiece(BaseKind.QUARTEL, QUARTEL_X, QUARTEL_Y, 1));
        out.addAll(defaultRing());
        return out;
    }

    /** Só o anel: 4 torres nos cantos + muralhas com portão ao sul. */
    public static List<BasePiece> defaultRing() {
        List<BasePiece> out = new ArrayList<>();
        out.add(new BasePiece(BaseKind.TORRE_VIGIA, RING_L, RING_T, 1));
        out.add(new BasePiece(BaseKind.TORRE_VIGIA, RING_R - 1, RING_T, 1));
        out.add(new BasePiece(BaseKind.TORRE_VIGIA, RING_L, RING_B - 1, 1));
        out.add(new BasePiece(BaseKind.TORRE_VIGIA, RING_R - 1, RING_B - 1, 1));
        for (int x = RING_L; x <= RING_R; x++)
            for (int y = RING_T; y <= RING_B; y++) {
                boolean edge = x == RING_L || x == RING_R || y == RING_T || y == RING_B;
                if (!edge) continue;
                boolean corner = (x <= RING_L + 1 || x >= RING_R - 1) && (y <= RING_T + 1 || y >= RING_B - 1);
                if (corner) continue;                                   // fica por conta das torres
                if (y == RING_B && x >= GATE_X0 && x <= GATE_X1) continue; // portão
                out.add(new BasePiece(BaseKind.MURALHA, x, y, 1));
            }
        return out;
    }
}
