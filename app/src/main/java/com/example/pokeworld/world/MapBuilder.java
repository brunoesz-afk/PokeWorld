package com.example.pokeworld.world;

import java.util.Random;

import static com.example.pokeworld.world.Tiles.*;

/**
 * Monta o layout (matriz de tiles) de cada mapa do jogo.
 *
 * Cada mapa é desenhado com composição fixa (sementes fixas): o jogador sempre
 * encontra o mesmo mapa, com rios, lagos, penhascos, escadas, pontes e muros
 * colocados de propósito. Os tiles se conectam entre si (veja TileRenderer),
 * então um muro de 1 tile vira uma parede com topo e face, um penhasco de 2
 * tiles vira um degrau, e uma escada liga os dois lados.
 *
 * Classe sem dependências do Android.
 */
public final class MapBuilder {
    private final int rows, cols;
    private final int[][] g;
    private final Random rnd;

    private MapBuilder(int rows, int cols, long seed) {
        this.rows = rows;
        this.cols = cols;
        this.g = new int[rows][cols];
        this.rnd = new Random(seed);
    }

    /** Nomes de mapas que usam este construtor (STARTER_FOREST continua no TileMap original). */
    public static int[][] build(String type, int rows, int cols) {
        switch (type) {
            case "CITY": return city(rows, cols);
            case "HOME": return home(rows, cols);
            case "TOWER": return tower(rows, cols);
            case "DUEL_ARENA": return duelArena(rows, cols);
            case "DUNGEON": return dungeonRoom(rows, cols);
            case "SMALL_LAKE": return smallLake(rows, cols);
            case "ROCKY_PATH": return rockyPath(rows, cols);
            case "JUNGLE": return jungle(rows, cols);
            case "MAGMA_CAVE": return magmaCave(rows, cols);
            case "SWAMP": return swamp(rows, cols);
            case "FIRE_CAVE": return fireCave(rows, cols);
            case "SEWERS": return sewers(rows, cols);
            case "VALE_ROCHOSO": return valeRochoso(rows, cols);
            case "MINE": return mine(rows, cols);
            case "RIO": return rio(rows, cols);
            case "OCEAN": return ocean(rows, cols);
            case "DESERT": return desert(rows, cols);
            case "VOLCANO": return volcano(rows, cols);
            case "MOLTEN_SANCTUM_BEGINNER":
            case "MOLTEN_SANCTUM": return moltenSanctum(rows, cols);
            case "SNOW_MOUNTAIN": return snowMountain(rows, cols);
            case "SNOW_PEAK": return snowPeak(rows, cols);
            case "FEAR_ISLAND": return fearIsland(rows, cols);
            case "FOREST":
            default: return forest(rows, cols);
        }
    }

    // ------------------------------------------------------------------
    // Ferramentas de desenho
    // ------------------------------------------------------------------

    private boolean in(int r, int c) { return r >= 0 && r < rows && c >= 0 && c < cols; }

    private void set(int r, int c, int t) { if (in(r, c)) g[r][c] = t; }

    private int get(int r, int c) { return in(r, c) ? g[r][c] : -1; }

    private void fill(int t) { for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) g[r][c] = t; }

    private void rect(int r0, int c0, int r1, int c1, int t) {
        for (int r = r0; r <= r1; r++) for (int c = c0; c <= c1; c++) set(r, c, t);
    }

    /** Retângulo apenas onde o tile atual é um dos permitidos (null = qualquer). */
    private void rectOn(int r0, int c0, int r1, int c1, int t, int... only) {
        for (int r = r0; r <= r1; r++) for (int c = c0; c <= c1; c++) {
            int cur = get(r, c);
            if (cur < 0) continue;
            boolean ok = only.length == 0;
            for (int o : only) if (o == cur) ok = true;
            if (ok) g[r][c] = t;
        }
    }

    /** Borda de um retângulo (espessura th). */
    private void frame(int r0, int c0, int r1, int c1, int th, int t) {
        for (int r = r0; r <= r1; r++) for (int c = c0; c <= c1; c++) {
            if (r < r0 + th || r > r1 - th || c < c0 + th || c > c1 - th) set(r, c, t);
        }
    }

    /** Mancha orgânica (lagoa, clareira, ilha) com borda irregular. */
    private void blob(double cr, double cc, double rr, double rc, int t, double wobble, long seed, int... only) {
        Random n = new Random(seed);
        double p1 = n.nextDouble() * 6.28, p2 = n.nextDouble() * 6.28, p3 = n.nextDouble() * 6.28;
        int r0 = (int) (cr - rr * 1.4) - 1, r1 = (int) (cr + rr * 1.4) + 1;
        int c0 = (int) (cc - rc * 1.4) - 1, c1 = (int) (cc + rc * 1.4) + 1;
        for (int r = r0; r <= r1; r++) for (int c = c0; c <= c1; c++) {
            if (!in(r, c)) continue;
            double dy = (r - cr) / rr, dx = (c - cc) / rc;
            double ang = Math.atan2(dy, dx);
            double k = 1 + wobble * (Math.sin(ang * 2 + p1) * 0.5 + Math.sin(ang * 3 + p2) * 0.3 + Math.sin(ang * 5 + p3) * 0.2);
            if (dx * dx + dy * dy <= k * k) {
                boolean ok = only.length == 0;
                for (int o : only) if (o == g[r][c]) ok = true;
                if (ok) g[r][c] = t;
            }
        }
    }

    /** Caminho sinuoso de (r0,c0) a (r1,c1) com largura w. amp = amplitude da curva. */
    private void path(double r0, double c0, double r1, double c1, int w, int t, double amp, long seed, int... only) {
        Random n = new Random(seed);
        double ph = n.nextDouble() * 6.28, fq = 1.5 + n.nextDouble() * 1.5;
        int steps = (int) (Math.max(Math.abs(r1 - r0), Math.abs(c1 - c0)) * 2) + 1;
        double dr = r1 - r0, dc = c1 - c0, len = Math.hypot(dr, dc);
        double nr = -dc / len, nc = dr / len;
        for (int i = 0; i <= steps; i++) {
            double u = i / (double) steps;
            double off = Math.sin(u * Math.PI * fq + ph) * amp * Math.sin(u * Math.PI);
            double r = r0 + dr * u + nr * off, c = c0 + dc * u + nc * off;
            stamp(r, c, w, t, only);
        }
    }

    /** Caminho passando por uma sequência de pontos {r,c}, suavizado. */
    private void route(double[][] pts, int w, int t, int... only) {
        for (int i = 0; i + 1 < pts.length; i++) {
            double r0 = pts[i][0], c0 = pts[i][1], r1 = pts[i + 1][0], c1 = pts[i + 1][1];
            int steps = (int) (Math.max(Math.abs(r1 - r0), Math.abs(c1 - c0)) * 2) + 1;
            for (int s = 0; s <= steps; s++) {
                double u = s / (double) steps;
                stamp(r0 + (r1 - r0) * u, c0 + (c1 - c0) * u, w, t, only);
            }
        }
    }

    private void stamp(double r, double c, int w, int t, int... only) {
        double h = w / 2.0;
        for (int rr = (int) Math.floor(r - h); rr <= (int) Math.ceil(r + h); rr++) {
            for (int cc = (int) Math.floor(c - h); cc <= (int) Math.ceil(c + h); cc++) {
                if (!in(rr, cc)) continue;
                if (Math.hypot(rr - r, cc - c) > h + 0.3) continue;
                boolean ok = only.length == 0;
                for (int o : only) if (o == g[rr][cc]) ok = true;
                if (ok) g[rr][cc] = t;
            }
        }
    }

    /** Espalha objetos aleatórios sobre certos tipos de chão, evitando raio ao redor de pontos livres. */
    private void scatter(int obj, int count, int[][] keepFree, double freeRadius, int... on) {
        int tries = 0, placed = 0;
        while (placed < count && tries < count * 40) {
            tries++;
            int r = rnd.nextInt(rows), c = rnd.nextInt(cols);
            if (!matches(g[r][c], on)) continue;
            boolean bad = false;
            if (keepFree != null) for (int[] p : keepFree) if (Math.hypot(r - p[0], c - p[1]) < freeRadius) { bad = true; break; }
            if (bad) continue;
            g[r][c] = obj;
            placed++;
        }
    }

    private boolean matches(int v, int[] on) {
        if (on.length == 0) return true;
        for (int o : on) if (o == v) return true;
        return false;
    }


    /** Ruído suave determinístico (0..1) para formar bosques e clareiras em vez de árvores soltas. */
    private double noise(double r, double c, double sc, long seed) {
        Random n = new Random(seed);
        double a = n.nextDouble() * 6.28, b = n.nextDouble() * 6.28, d = n.nextDouble() * 6.28, e = n.nextDouble() * 6.28;
        double v = Math.sin(r * sc + a) + Math.sin(c * sc * 1.3 + b) + Math.sin((r + c) * sc * 0.8 + d) + Math.sin((r * 0.7 - c) * sc * 1.1 + e);
        return (v + 4) / 8.0;
    }

    /** Bosques densos em manchas: onde o ruído passa de 'th' nascem árvores (com 'dens' de densidade). */
    private void groves(int obj, double th, double dens, double sc, long seed, int... on) {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            if (!matches(g[r][c], on)) continue;
            double v = noise(r, c, sc, seed);
            if (v > th && rnd.nextDouble() < dens) g[r][c] = obj;
            else if (v > th - 0.06 && rnd.nextDouble() < dens * 0.3) g[r][c] = obj;
        }
    }

    /** Manchas suaves de um tipo de chão onde o ruído passa de 'th' (sem tiles soltos). */
    private void patches(int t, double th, double sc, long seed, int... on) {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            if (matches(g[r][c], on) && noise(r, c, sc, seed) > th) g[r][c] = t;
        }
    }

    /** Caminho que vira ponte onde cruza água/lava e estrada em terra firme. */
    private void routeWithBridges(double[][] pts, int w, int road, int bridge) {
        boolean[][] mask = new boolean[rows][cols];
        boolean[][] narrow = new boolean[rows][cols];
        for (int i = 0; i + 1 < pts.length; i++) {
            double r0 = pts[i][0], c0 = pts[i][1], r1 = pts[i + 1][0], c1 = pts[i + 1][1];
            int steps = (int) (Math.max(Math.abs(r1 - r0), Math.abs(c1 - c0)) * 2) + 1;
            for (int s = 0; s <= steps; s++) {
                double u = s / (double) steps, r = r0 + (r1 - r0) * u, c = c0 + (c1 - c0) * u, h = w / 2.0;
                for (int rr = (int) Math.floor(r - h); rr <= (int) Math.ceil(r + h); rr++) for (int cc = (int) Math.floor(c - h); cc <= (int) Math.ceil(c + h); cc++) {
                    if (in(rr, cc) && Math.hypot(rr - r, cc - c) <= h + 0.3) mask[rr][cc] = true;
                    if (in(rr, cc) && Math.hypot(rr - r, cc - c) <= 1.6) narrow[rr][cc] = true;
                }
            }
        }
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) if (mask[r][c]) {
            int cur = g[r][c];
            if (Tiles.isLiquid(cur)) { if (narrow[r][c]) g[r][c] = bridge; }
            else if (cur != PORTAL && cur != CLIFF && cur != WALL && cur != BRIDGE && cur != STAIRS) g[r][c] = road;
        }
    }

    /** Força uma área de chão (apaga água, lava, rocha...), preservando portais. */
    private void force(int r0, int c0, int r1, int c1, int t) {
        for (int r = r0; r <= r1; r++) for (int c = c0; c <= c1; c++) if (in(r, c) && g[r][c] != PORTAL) g[r][c] = t;
    }

    /** Ponto de cruzamento livre: reserva pontos de spawn, evitando decoração em cima. */
    private static final int[][] CENTER_FREE_5050 = {{23, 47}, {25, 50}};

    /** Linha de penhasco horizontal (ledge) de c0..c1, com `h` tiles de altura, começando na linha r. */
    private void ledge(int r, int c0, int c1, int h) {
        for (int i = 0; i < h; i++) for (int c = c0; c <= c1; c++) set(r + i, c, CLIFF);
    }

    /** Parede vertical de penhasco entre r0..r1 na coluna c. */
    private void cliffV(int c, int r0, int r1) { for (int r = r0; r <= r1; r++) set(r, c, CLIFF); }

    /** Escada vertical atravessando um penhasco de h tiles em c. */
    private void stairsV(int r, int c, int h, int w) {
        for (int i = 0; i < h; i++) for (int k = 0; k < w; k++) set(r + i, c + k, STAIRS);
    }

    /** Ponte horizontal de c0..c1 em r (largura w). */
    private void bridgeH(int r, int c0, int c1, int w, int type) {
        for (int c = c0; c <= c1; c++) for (int k = 0; k < w; k++) set(r + k, c, type);
    }

    private void bridgeV(int c, int r0, int r1, int w, int type) {
        for (int r = r0; r <= r1; r++) for (int k = 0; k < w; k++) set(r, c + k, type);
    }

    /** Moldura de borda do mapa: preenche as 'th' primeiras linhas/colunas com t. */
    private void border(int th, int t) {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            if (r < th || r >= rows - th || c < th || c >= cols - th) g[r][c] = t;
        }
    }

    /** Portais nas bordas (largura pw, centrados no meio). */
    private void portalsLR(int pw) {
        int mr = rows / 2;
        for (int k = -(pw / 2); k <= pw / 2; k++) { set(mr + k, 0, PORTAL); set(mr + k, cols - 1, PORTAL); }
    }

    private void portalsTB(int pw, boolean top, boolean bottom) {
        int mc = cols / 2;
        for (int k = -(pw / 2); k <= pw / 2; k++) {
            if (top) set(0, mc + k, PORTAL);
            if (bottom) set(rows - 1, mc + k, PORTAL);
        }
    }

    /** Casa com telhado (2 linhas de telhado + 1 de parede com porta). */
    private void house(int r, int c, int w) {
        for (int k = 0; k < w; k++) { set(r, c + k, ROOF); set(r + 1, c + k, ROOF); set(r + 2, c + k, HOUSE); }
        set(r + 2, c + w / 2, DOOR);
    }

    /** Árvores densas em volta de uma área (para criar bordas de floresta). */
    private void denseTrees(double density, int... on) {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            if (matches(g[r][c], on) && rnd.nextDouble() < density) g[r][c] = TREE;
        }
    }

    /** Garante que o miolo do mapa (spawns de inimigos / spawn do jogador) fique livre. */
    private void clearArea(int r0, int c0, int r1, int c1, int ground) {
        for (int r = r0; r <= r1; r++) for (int c = c0; c <= c1; c++) {
            int cur = get(r, c);
            if (cur == TREE || cur == ROCK || cur == BUSH || cur == BERRY_TREE || cur == DEAD_TREE || cur == CACTUS
                    || cur == SNOW_CRYSTAL || cur == SULFUR_CRYSTAL || cur == PILLAR || cur == TOMBSTONE || cur == MAGMA_VENT) {
                g[r][c] = ground;
            }
        }
    }

    // ------------------------------------------------------------------
    // CIDADE (100x50): muralha com 4 portões, praça de pedra, casas e fontes
    // ------------------------------------------------------------------
    private static int[][] city(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 1001);
        b.fill(GRASS);
        int mr = rows / 2, mc = cols / 2;

        // Campo ao redor: bosque nas bordas e estradas de terra até os portais
        b.denseTrees(0.0);
        b.rect(0, 0, 3, cols - 1, GRASS);
        b.scatter(TREE, 220, null, 0, GRASS);
        b.scatter(FLOWERS, 160, null, 0, GRASS);

        // Muralha (retângulo grosso de 2 tiles, vira parede com topo e face)
        int top = 6, bot = 44, left = 12, right = 87;
        b.rect(top - 1, left - 1, bot + 1, right + 1, GRASS);
        b.frame(top, left, bot, right, 2, WALL);
        // Interior: pavimento
        b.rect(top + 2, left + 2, bot - 2, right - 2, STONE_PAVEMENT);
        // Portões 5 tiles de largura em cada lado (abertura no muro, com estrada)
        b.rect(top, mc - 2, top + 1, mc + 2, STONE_PAVEMENT);
        b.rect(bot - 1, mc - 2, bot, mc + 2, STONE_PAVEMENT);
        b.rect(mr - 2, left, mr + 2, left + 1, STONE_PAVEMENT);
        b.rect(mr - 2, right - 1, mr + 2, right, STONE_PAVEMENT);

        // Estradas de terra do portão até a borda do mapa
        b.rect(0, mc - 2, top - 1, mc + 2, ROAD);
        b.rect(bot + 1, mc - 2, rows - 1, mc + 2, ROAD);
        b.rect(mr - 2, 0, mr + 2, left - 1, ROAD);
        b.rect(mr - 2, right + 1, mr + 2, cols - 1, ROAD);

        // Avenidas dentro da cidade (estrada em cruz)
        b.rect(top + 2, mc - 2, bot - 2, mc + 2, ROAD);
        b.rect(mr - 2, left + 2, mr + 2, right - 2, ROAD);

        // Fontes (bacias 3x3 com água) ao norte e ao sul da praça
        b.rect(top + 4, mc - 1, top + 6, mc + 1, FOUNTAIN);
        b.rect(bot - 8, mc - 1, bot - 6, mc + 1, FOUNTAIN);

        // Pilares ladeando as avenidas
        for (int i = 0; i < 3; i++) {
            int rr = mr - 8 + i * 8;
            b.set(rr, mc - 4, PILLAR); b.set(rr, mc + 4, PILLAR);
        }
        // Braseiros nos portões
        b.set(top + 2, mc - 3, BRAZIER); b.set(top + 2, mc + 3, BRAZIER);
        b.set(bot - 2, mc - 3, BRAZIER); b.set(bot - 2, mc + 3, BRAZIER);
        b.set(mr - 3, left + 2, BRAZIER); b.set(mr + 3, left + 2, BRAZIER);
        b.set(mr - 3, right - 2, BRAZIER); b.set(mr + 3, right - 2, BRAZIER);

        // Placa na praça central (a original citava direções)
        b.set(mr + 3, mc + 6, SIGN);

        // Portais (3 tiles) no meio de cada borda do mapa
        b.portalsLR(3);
        b.portalsTB(3, true, true);
        // Miolo livre para NPCs, prédios do jogador e spawn
        b.rectOn(mr - 6, mc - 8, mr + 8, mc + 8, STONE_PAVEMENT, TREE, FLOWERS, FOUNTAIN, PILLAR, GRASS);
        b.rectOn(mr - 2, left + 2, mr + 2, right - 2, ROAD, STONE_PAVEMENT);
        b.rectOn(top + 2, mc - 2, bot - 2, mc + 2, ROAD, STONE_PAVEMENT);
        districts(b);
        b.set(mr + 3, mc + 6, SIGN);
        return b.g;
    }

    /**
     * Distritos da cidade (ver CityDistricts): cada NPC ganha um tapete 5x3 para ficar "no seu espaço",
     * o Coliseu ganha arena com tapete central, e cada distrito recebe pilares/braseiros nos cantos.
     */
    private static void districts(MapBuilder b) {
        // Jardim do Quarteirão da Cura (noroeste)
        b.rect(10, 16, 13, 22, GRASS);
        b.rectOn(10, 16, 13, 22, FLOWERS, GRASS);
        b.set(10, 16, TREE); b.set(10, 22, TREE);

        // Coliseu: pavimento de arena, tapete central, pilares nos cantos e braseiros na entrada sul
        b.rect(9, 70, 22, 85, STONE_PAVEMENT);
        b.rect(12, 74, 18, 81, CARPET);
        b.set(10, 71, PILLAR); b.set(10, 84, PILLAR); b.set(21, 71, PILLAR); b.set(21, 84, PILLAR);
        b.set(10, 76, BRAZIER); b.set(10, 79, BRAZIER);

        // Guilda de Missões: pátio com pilares e braseiros ladeando o Mural
        b.rect(29, 70, 41, 85, STONE_PAVEMENT);
        b.set(29, 71, PILLAR); b.set(29, 84, PILLAR); b.set(40, 71, PILLAR); b.set(40, 84, PILLAR);
        b.set(30, 75, BRAZIER); b.set(30, 81, BRAZIER);

        // Distrito dos Mestres: braseiros marcando o pátio da Forja e dos Clãs
        b.set(30, 15, BRAZIER); b.set(40, 15, BRAZIER); b.set(30, 29, BRAZIER); b.set(40, 29, BRAZIER);

        // Um tapete (5 de largura x 3 de altura) sob cada NPC
        for (CityDistricts.Spot s : CityDistricts.SPOTS) {
            int r = (int) Math.floor(s.ty), c = (int) Math.floor(s.tx);
            b.rect(r - 1, c - 2, r + 1, c + 2, CARPET);
        }
    }

    // ------------------------------------------------------------------
    // BASE DO JOGADOR (160x80): geometria do núcleo preservada (40x36 em 60..100 x 22..58)
    // ------------------------------------------------------------------
    private static int[][] home(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 1002);
        b.fill(GRASS);
        // Floresta ao redor com clareiras e flores (mapa pequeno = leve)
        b.scatter(TREE, 260, null, 0, GRASS);
        b.scatter(FLOWERS, 90, null, 0, GRASS);
        // Pequeno lago no canto noroeste
        b.blob(9, 6, 5, 5, WATER, 0.25, 77, GRASS, TREE, FLOWERS);
        b.blob(9, 6, 3, 3, DEEP_WATER, 0.2, 78, WATER);
        b.scatter(ROCK, 8, null, 0, GRASS);
        // Terreno edificável (igual ao BaseLayout): grama limpa, sem árvores nem pedras.
        // As muralhas, torres e prédios NÃO fazem parte do mapa: são peças que o jogador coloca.
        b.rectOn(BaseLayout.PLOT_Y - 3, BaseLayout.PLOT_X - 3, BaseLayout.PLOT_Y + BaseLayout.PLOT_H + 2,
                BaseLayout.PLOT_X + BaseLayout.PLOT_W + 2, GRASS, TREE, ROCK, FLOWERS, BUSH, WATER, DEEP_WATER);
        // Faixa de flores e arbustos na borda externa do terreno (dá o "recorte" de base)
        for (int i = -3; i < BaseLayout.PLOT_W + 3; i++) {
            if (i % 3 == 0) { b.set(BaseLayout.PLOT_Y - 2, BaseLayout.PLOT_X + i, FLOWERS); b.set(BaseLayout.PLOT_Y + BaseLayout.PLOT_H + 1, BaseLayout.PLOT_X + i, FLOWERS); }
        }
        for (int i = -3; i < BaseLayout.PLOT_H + 3; i++) {
            if (i % 3 == 0) { b.set(BaseLayout.PLOT_Y + i, BaseLayout.PLOT_X - 2, FLOWERS); b.set(BaseLayout.PLOT_Y + i, BaseLayout.PLOT_X + BaseLayout.PLOT_W + 1, FLOWERS); }
        }
        // Estrada do portão (sul) até a borda do mapa, 4 de largura (alinha com o portão do anel)
        for (int r = BaseLayout.RING_B + 1; r < rows; r++)
            for (int c = BaseLayout.GATE_X0 - 1; c <= BaseLayout.GATE_X1 + 1; c++) b.g[r][c] = ROAD;
        // Beira da estrada limpa
        b.rectOn(BaseLayout.RING_B + 1, BaseLayout.GATE_X0 - 4, rows - 1, BaseLayout.GATE_X0 - 2, GRASS, TREE, ROCK);
        b.rectOn(BaseLayout.RING_B + 1, BaseLayout.GATE_X1 + 2, rows - 1, BaseLayout.GATE_X1 + 4, GRASS, TREE, ROCK);
        return b.g;
    }

    private static void g(MapBuilder b, int r, int c, int t) { b.set(r, c, t); }

    private static int[][] dungeonRoom(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 1004);
        b.fill(FLOOR);
        b.frame(0, 0, rows - 1, cols - 1, 1, BRICK);
        b.rect(1, 1, rows - 2, cols - 2, FLOOR);
        for (int c = 4; c < cols - 3; c += 6) { b.set(1, c, BRAZIER); }
        return b.g;
    }

    // ------------------------------------------------------------------
    // TORRE (25x25): salão de pedra com tapete, pilares e muralhas
    // ------------------------------------------------------------------
    private static int[][] tower(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 1003);
        b.fill(FLOOR);
        // Mesma geometria de antes: 1 tile de muro em volta, resto livre
        b.frame(0, 0, rows - 1, cols - 1, 1, BRICK);
        b.rect(1, cols / 2 - 1, rows - 2, cols / 2 + 1, CARPET);
        for (int r = 3; r < rows - 3; r += 4) { b.set(r, 1, PILLAR); b.set(r, cols - 2, PILLAR); }
        b.set(1, 2, BRAZIER); b.set(1, cols - 3, BRAZIER);
        b.set(rows - 1, cols / 2, PORTAL);
        return b.g;
    }

    // ------------------------------------------------------------------
    // ARENA DE DUELOS: arena quadrada com a linha no meio; retângulo do oponente em cima e o seu embaixo
    // ------------------------------------------------------------------
    private static int[][] duelArena(int rows, int cols) {
        final int AC0 = ArenaLayout.AC0, AC1 = ArenaLayout.AC1;
        MapBuilder b = new MapBuilder(rows, cols, 4001);
        b.fill(BASALT_FLOOR);
        b.frame(0, 0, rows - 1, cols - 1, 1, BRICK);
        // rios de lava nas laterais, com pontes (só decoração: ficam fora da área de jogo)
        int[] lavaCols = {2, 3, cols - 4, cols - 3};
        for (int r = 2; r <= 9; r++) for (int c : lavaCols) b.set(r, c, LAVA);
        for (int r = 5; r <= 6; r++) for (int c : lavaCols) b.set(r, c, MAGMA_BRIDGE);
        b.rect(ArenaLayout.FOE_R0, AC0, ArenaLayout.FOE_R1, AC1, STONE_PAVEMENT);      // faixa do oponente
        b.rect(ArenaLayout.ME_R0, AC0, ArenaLayout.ME_R1, AC1, STONE_PAVEMENT);        // a sua faixa
        for (int c = AC0; c <= AC1; c++) b.set(ArenaLayout.RAIL_ROW, c, PILLAR);        // grade entre a arena e a sua faixa
        b.set(ArenaLayout.RAIL_ROW, AC0, BRAZIER); b.set(ArenaLayout.RAIL_ROW, AC1, BRAZIER);
        return b.g;
    }

    // ------------------------------------------------------------------
    // FLOREST (100x50, portais N/S): trilha sinuosa, riacho com ponte, colina com escada
    // ------------------------------------------------------------------
    private static int[][] forest(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2001);
        int mc = cols / 2;
        b.fill(GRASS);
        // Bosques em manchas (com clareiras entre eles), em vez de árvores soltas
        b.groves(TREE, 0.52, 0.8, 0.23, 301, GRASS);
        b.groves(TREE, 0.70, 0.95, 0.31, 302, GRASS, TREE);
        b.scatter(FLOWERS, 160, null, 0, GRASS);
        // Riacho cruzando o mapa de oeste a leste
        double[][] stream = {{15, 0}, {14, 18}, {17, 36}, {15, 52}, {18, 70}, {16, 99}};
        b.route(stream, 6, SAND);
        b.route(stream, 4, WATER);
        b.route(stream, 2, DEEP_WATER);
        // Lagoa de pesca e clareira no sudoeste
        b.blob(37, 18, 6, 11, GRASS, 0.3, 11, TREE, FLOWERS, BUSH);
        b.blob(37, 18, 4, 8, SAND, 0.25, 15);
        b.blob(37, 18, 3, 6, WATER, 0.25, 12);
        b.blob(37, 18, 1.4, 3, DEEP_WATER, 0.2, 13);
        // Colina a leste com penhasco de 2 tiles e escada
        b.force(30, 60, 44, 96, GRASS);
        b.groves(TREE, 0.62, 0.8, 0.3, 303, GRASS);
        b.force(26, 66, 28, 95, GRASS);
        b.ledge(29, 62, 96, 2);
        b.stairsV(29, 78, 2, 3);
        b.cliffV(61, 29, 44);
        b.rectOn(31, 62, 44, 96, GRASS, TREE);
        b.rectOn(32, 70, 40, 90, FLOWERS, GRASS);
        // Trilha principal norte-sul, com ponte sobre o riacho
        b.routeWithBridges(new double[][]{{0, mc}, {8, mc + 2}, {16, mc - 4}, {24, mc - 2}, {32, mc + 4}, {40, mc + 1}, {rows - 1, mc}}, 5, ROAD, BRIDGE);
        // Ramal para a escada da colina
        b.routeWithBridges(new double[][]{{32, mc + 4}, {32, 70}, {31, 79}}, 3, ROAD, BRIDGE);
        b.stairsV(29, 78, 2, 3);
        b.scatter(BUSH, 40, null, 0, GRASS);
        b.scatter(ROCK, 18, null, 0, GRASS);
        b.scatter(BERRY_TREE, 12, null, 0, GRASS);
        b.set(4, mc + 4, SIGN);
        b.clearArea(18, 40, 30, 60, GRASS);
        b.portalsTB(3, true, true);
        return b.g;
    }

    // ------------------------------------------------------------------
    // PEQUENO LAGO (portais L/O): praia ao redor de um lago com ilha e píer
    // ------------------------------------------------------------------
    private static int[][] smallLake(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2002);
        b.fill(SAND);
        b.blob(7, 50, 11, 56, GRASS, 0.2, 21, SAND);
        b.blob(45, 50, 10, 56, GRASS, 0.2, 22, SAND);
        b.groves(TREE, 0.55, 0.75, 0.25, 311, GRASS);
        b.scatter(FLOWERS, 70, null, 0, GRASS);
        // Lago com ilha central
        b.blob(25, 50, 11, 24, WATER, 0.22, 23);
        b.blob(25, 50, 7, 18, DEEP_WATER, 0.2, 24);
        b.blob(25, 50, 3.2, 5.5, SAND, 0.2, 25);
        b.blob(25, 50, 1.6, 3, GRASS, 0.2, 26);
        b.set(24, 49, TREE);
        b.scatter(ROCK, 30, null, 0, SAND);
        // Estrada de oeste a leste contornando o lago pelo norte
        b.routeWithBridges(new double[][]{{25, 0}, {20, 14}, {12, 28}, {10, 50}, {12, 72}, {20, 86}, {25, 99}}, 3, ROAD, BRIDGE);
        // Píer da margem norte até a ilha, mais dois embarcadouros curtos
        b.bridgeV(49, 8, 24, 2, BRIDGE);
        b.bridgeH(31, 38, 46, 1, BRIDGE);
        b.bridgeH(19, 56, 63, 1, BRIDGE);
        b.rect(24, 49, 26, 51, GRASS);
        b.set(25, 50, FLOWERS);
        // Entradas dos portais
        b.force(24, 0, 26, 7, ROAD);
        b.force(24, cols - 8, 26, cols - 1, ROAD);
        b.portalsLR(3);
        return b.g;
    }

    // ------------------------------------------------------------------
    // CAMINHO ROCHOSO (portais L/O): desfiladeiro com terraços, escadas e pedregulhos
    // ------------------------------------------------------------------
    private static int[][] rockyPath(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2003);
        b.fill(FLOOR);
        int mr = rows / 2;
        // Maciços rochosos de cima e de baixo (paredes do desfiladeiro)
        b.rect(0, 0, 9, cols - 1, CLIFF);
        b.rect(rows - 10, 0, rows - 1, cols - 1, CLIFF);
        // Bordas irregulares
        for (int c = 0; c < cols; c++) {
            int up = 10 + (int) Math.round(Math.sin(c * 0.21) * 2 + Math.sin(c * 0.53) * 1.2);
            int dn = rows - 11 - (int) Math.round(Math.cos(c * 0.17) * 2 + Math.sin(c * 0.4) * 1.2);
            for (int r = 8; r <= up; r++) b.set(r, c, CLIFF);
            for (int r = dn; r < rows - 8; r++) b.set(r, c, CLIFF);
        }
        // Terraço elevado no meio com penhasco e escadas
        b.rect(16, 30, 22, 70, GRASS);
        b.ledge(23, 30, 70, 2);
        b.cliffV(29, 16, 24);
        b.cliffV(71, 16, 24);
        b.rect(15, 29, 15, 71, CLIFF);
        b.stairsV(23, 38, 2, 3);
        b.stairsV(23, 60, 2, 3);
        // Trilha principal em S
        b.route(new double[][]{{mr, 0}, {mr, 18}, {mr + 3, 30}, {mr + 3, 50}, {mr, 62}, {mr, 82}, {mr, 99}}, 4, ROAD, FLOOR, GRASS, ROCK, BUSH);
        b.rectOn(17, 31, 22, 69, GRASS, FLOOR);
        b.scatter(BUSH, 30, null, 0, GRASS);
        // Pedregulhos
        b.scatter(ROCK, 90, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 5, FLOOR);
        b.scatter(FLOWERS, 20, null, 0, GRASS);
        b.clearArea(18, 36, 32, 64, FLOOR);
        b.rect(rows / 2 - 1, 0, rows / 2 + 1, 6, ROAD);
        b.rect(rows / 2 - 1, cols - 7, rows / 2 + 1, cols - 1, ROAD);
        b.portalsLR(3);
        return b.g;
    }

    // ------------------------------------------------------------------
    // SELVA (portais L/O): vegetação densa, rio, ruínas com escada e ponte
    // ------------------------------------------------------------------
    private static int[][] jungle(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2004);
        int mr = rows / 2;
        b.fill(GRASS);
        b.groves(TREE, 0.42, 0.9, 0.28, 321, GRASS);
        b.groves(TREE, 0.58, 1.0, 0.35, 322, GRASS, TREE);
        b.groves(BERRY_TREE, 0.60, 0.15, 0.3, 323, TREE);
        // Rio norte-sul
        double[][] river = {{0, 68}, {12, 64}, {24, 72}, {36, 66}, {rows - 1, 70}};
        b.route(river, 8, SAND);
        b.route(river, 5, WATER);
        b.route(river, 2, DEEP_WATER);
        // Ruína antiga: pátio de pedra cercado por muro, com escada e pilares
        b.force(5, 10, 21, 38, GRASS);
        b.force(7, 12, 15, 36, STONE_PAVEMENT);
        b.frame(7, 12, 15, 36, 1, WALL);
        b.force(8, 14, 9, 34, STONE_PAVEMENT);
        b.set(15, 24, STAIRS); b.set(15, 23, STAIRS); b.set(15, 25, STAIRS);
        b.set(9, 14, PILLAR); b.set(9, 34, PILLAR); b.set(13, 14, PILLAR); b.set(13, 34, PILLAR);
        b.rect(10, 20, 12, 28, CARPET);
        // Clareira de flores no sul
        b.blob(38, 24, 6, 11, GRASS, 0.3, 31, TREE, BERRY_TREE);
        b.scatter(FLOWERS, 40, null, 0, GRASS);
        // Trilha leste-oeste com ponte sobre o rio e acesso à ruína
        b.routeWithBridges(new double[][]{{mr, 0}, {mr + 3, 20}, {mr - 2, 40}, {mr, 56}, {mr + 1, 80}, {mr, 99}}, 4, ROAD, BRIDGE);
        b.routeWithBridges(new double[][]{{mr - 2, 40}, {18, 30}, {17, 24}}, 3, ROAD, BRIDGE);
        b.scatter(BUSH, 40, null, 0, GRASS);
        b.scatter(FLOWERS, 80, null, 0, GRASS);
        b.clearArea(18, 38, 32, 58, GRASS);
        b.portalsLR(3);
        return b.g;
    }

    // ------------------------------------------------------------------
    // CAVERNA DE MAGMA (portais L/O)
    // ------------------------------------------------------------------
    private static int[][] magmaCave(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2005);
        int mr = rows / 2;
        b.fill(CLIFF);
        // Salões de obsidiana ligados por corredores
        b.blob(25, 12, 9, 12, OBSIDIAN, 0.2, 41);
        b.blob(25, 36, 11, 13, OBSIDIAN, 0.25, 42);
        b.blob(25, 64, 11, 13, OBSIDIAN, 0.25, 43);
        b.blob(25, 88, 9, 12, OBSIDIAN, 0.2, 44);
        b.rect(mr - 2, 0, mr + 2, cols - 1, OBSIDIAN);
        b.blob(10, 50, 5, 14, OBSIDIAN, 0.3, 45);
        b.blob(40, 50, 5, 14, OBSIDIAN, 0.3, 46);
        b.rect(14, 49, mr - 3, 51, OBSIDIAN);
        b.rect(mr + 3, 49, 36, 51, OBSIDIAN);
        // Poças e rios de lava
        b.blob(22, 36, 4, 6, LAVA, 0.3, 47, OBSIDIAN);
        b.blob(30, 64, 4, 6, LAVA, 0.3, 48, OBSIDIAN);
        b.route(new double[][]{{6, 20}, {14, 26}, {18, 40}, {16, 58}, {20, 72}}, 3, LAVA, OBSIDIAN);
        b.route(new double[][]{{44, 20}, {36, 30}, {32, 44}, {34, 60}, {30, 78}}, 3, LAVA, OBSIDIAN);
        b.blob(10, 50, 2, 8, LAVA, 0.3, 49, OBSIDIAN);
        b.blob(40, 50, 2, 8, LAVA, 0.3, 50, OBSIDIAN);
        // Pontes de basalto sobre a lava
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            if (b.g[r][c] == LAVA && Math.abs(r - mr) <= 2) b.g[r][c] = BRIDGE;
        }
        b.set(mr, 3, OBSIDIAN);
        b.scatter(MAGMA_VENT, 12, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 5, OBSIDIAN);
        b.scatter(ROCK, 40, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 4, OBSIDIAN);
        b.scatter(SULFUR_CRYSTAL, 14, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 5, OBSIDIAN);
        b.rect(mr - 1, 0, mr + 1, 4, OBSIDIAN);
        b.rect(mr - 1, cols - 5, mr + 1, cols - 1, OBSIDIAN);
        b.clearArea(21, 44, 29, 56, OBSIDIAN);
        b.portalsLR(3);
        return b.g;
    }

    // ------------------------------------------------------------------
    // PÂNTANO (portais L/O): ilhas de terra ligadas por pontes de madeira
    // ------------------------------------------------------------------
    private static int[][] swamp(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2006);
        int mr = rows / 2;
        b.fill(WATER);
        // Ilhas principais
        double[][] islands = {{25, 10, 6, 9}, {14, 28, 6, 9}, {35, 30, 7, 10}, {24, 50, 8, 12}, {12, 68, 6, 10}, {36, 70, 7, 11}, {25, 90, 6, 9}};
        for (int i = 0; i < islands.length; i++) b.blob(islands[i][0], islands[i][1], islands[i][2], islands[i][3], GRASS, 0.3, 61 + i);
        // Águas mais profundas nos vãos
        b.blob(26, 30, 3, 5, DEEP_WATER, 0.3, 70, WATER);
        b.blob(22, 70, 3, 5, DEEP_WATER, 0.3, 71, WATER);
        b.blob(8, 48, 3, 6, DEEP_WATER, 0.3, 72, WATER);
        b.blob(42, 50, 3, 6, DEEP_WATER, 0.3, 73, WATER);
        // Pontes ligando as ilhas ao longo do caminho principal
        b.bridgeH(mr, 0, 99, 1, BRIDGE);
        b.bridgeH(mr + 1, 0, 99, 1, BRIDGE);
        b.bridgeV(30, 14, 26, 1, BRIDGE);
        b.bridgeV(31, 14, 26, 1, BRIDGE);
        b.bridgeV(70, 18, 26, 1, BRIDGE);
        b.bridgeV(71, 18, 26, 1, BRIDGE);
        b.bridgeH(14, 33, 62, 1, BRIDGE);
        b.bridgeH(36, 38, 64, 1, BRIDGE);
        // Terra firme: musgo, flores, árvores mortas e raízes
        b.scatter(DEAD_TREE, 32, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 3, GRASS);
        b.scatter(BUSH, 40, null, 0, GRASS);
        b.scatter(FLOWERS, 50, null, 0, GRASS);
        b.scatter(ROCK, 15, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 4, GRASS);
        // Poças, juncos e troncos caídos viram água rasa ao redor
        b.rect(mr - 1, 0, mr + 2, 3, GRASS);
        b.rect(mr - 1, cols - 4, mr + 2, cols - 1, GRASS);
        b.clearArea(21, 42, 29, 58, GRASS);
        b.portalsLR(3);
        return b.g;
    }

    // ------------------------------------------------------------------
    // CAVERNA DE FOGO (portais L/O)
    // ------------------------------------------------------------------
    private static int[][] fireCave(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2007);
        int mr = rows / 2;
        b.fill(CLIFF);
        b.blob(25, 50, 18, 46, FLOOR, 0.15, 81);
        b.blob(10, 20, 6, 14, FLOOR, 0.2, 82);
        b.blob(40, 80, 6, 14, FLOOR, 0.2, 83);
        b.rect(mr - 2, 0, mr + 2, cols - 1, FLOOR);
        // Lagos de lava com margens de pedra e ponte
        b.blob(14, 30, 4, 8, LAVA, 0.3, 84, FLOOR);
        b.blob(36, 68, 4, 9, LAVA, 0.3, 85, FLOOR);
        b.blob(30, 56, 3, 5, LAVA, 0.3, 86, FLOOR);
        b.route(new double[][]{{4, 40}, {14, 46}, {20, 60}, {26, 66}}, 2, LAVA, FLOOR);
        b.bridgeV(46, 12, 22, 2, BRIDGE);
        // Pilares e rochas
        b.scatter(ROCK, 35, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 5, FLOOR);
        b.scatter(PILLAR, 8, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 6, FLOOR);
        b.scatter(BRAZIER, 8, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 5, FLOOR);
        // Estalagmites (penhascos isolados) com escadas
        b.rect(6, 70, 8, 84, CLIFF);
        b.rect(9, 70, 12, 84, FLOOR);
        b.rect(8, 76, 8, 78, STAIRS);
        b.rect(rows - 10, 12, rows - 8, 24, CLIFF);
        b.rect(rows - 7, 12, rows - 5, 24, FLOOR);
        b.rect(rows - 8, 17, rows - 8, 19, STAIRS);
        b.rect(mr - 1, 0, mr + 1, 4, FLOOR);
        b.rect(mr - 1, cols - 5, mr + 1, cols - 1, FLOOR);
        b.force(21, 42, 29, 58, FLOOR);
        b.portalsLR(3);
        return b.g;
    }

    // ------------------------------------------------------------------
    // ESGOTOS (portais L/O): canal central com passarelas, pontes e pilares
    // ------------------------------------------------------------------
    private static int[][] sewers(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2008);
        int mr = rows / 2;
        b.fill(WALL);
        // Sala central grande com canal
        b.rect(mr - 8, 0, mr + 8, cols - 1, FLOOR);
        b.rect(mr - 2, 6, mr + 2, cols - 7, WATER);
        b.rect(mr - 1, 6, mr + 1, cols - 7, DEEP_WATER);
        // Salas laterais ligadas por corredores
        double[][] rooms = {{8, 18, 5, 12}, {8, 52, 5, 14}, {8, 84, 5, 10}, {42, 18, 5, 12}, {42, 52, 5, 14}, {42, 84, 5, 10}};
        for (double[] rm : rooms) {
            int r0 = (int) (rm[0] - rm[2]), r1 = (int) (rm[0] + rm[2]), c0 = (int) (rm[1] - rm[3]), c1 = (int) (rm[1] + rm[3]);
            b.rect(r0, c0, r1, c1, FLOOR);
            // corredor até o canal
            int cc = (int) rm[1];
            if (rm[0] < mr) b.rect(r1, cc - 1, mr - 8, cc + 1, FLOOR);
            else b.rect(mr + 8, cc - 1, r0, cc + 1, FLOOR);
            b.rect(r0 + 1, c0 + 1, r1 - 1, c0 + 1, STONE_PAVEMENT);
        }
        // Poças de esgoto nas salas
        b.rect(7, 12, 9, 15, WATER);
        b.rect(41, 80, 43, 84, WATER);
        // Pontes sobre o canal
        for (int c : new int[]{18, 34, 50, 68, 84}) b.bridgeV(c - 1, mr - 2, mr + 2, 3, BRIDGE);
        b.rect(mr - 1, 0, mr + 1, 0, FLOOR);
        // Pilares e caixotes
        for (int c = 8; c < cols - 8; c += 12) { b.set(mr - 5, c, PILLAR); b.set(mr + 5, c, PILLAR); }
        b.scatter(ROCK, 14, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 5, FLOOR);
        b.rect(mr - 1, 0, mr + 1, 3, FLOOR);
        b.rect(mr - 1, cols - 4, mr + 1, cols - 1, FLOOR);
        // Garantir passagem do portal ao canal: passarela nas bordas do canal
        b.rect(mr - 3, 0, mr - 3, cols - 1, STONE_PAVEMENT);
        b.rect(mr + 3, 0, mr + 3, cols - 1, STONE_PAVEMENT);
        b.rect(mr - 1, 0, mr + 1, 3, FLOOR);
        b.rect(mr - 1, cols - 4, mr + 1, cols - 1, FLOOR);
        b.clearArea(21, 42, 29, 58, FLOOR);
        b.portalsLR(3);
        return b.g;
    }

    // ------------------------------------------------------------------
    // VALE ROCHOSO (portais N/S): terraços de rocha, escadas e estrada de areia
    // ------------------------------------------------------------------
    private static int[][] valeRochoso(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2009);
        int mc = cols / 2;
        b.fill(SAND);
        // Terraço superior (norte) e inferior (sul), com escadas
        b.rect(0, 0, 12, cols - 1, GRASS);
        b.rectOn(0, 0, 12, cols - 1, SAND, GRASS);
        b.ledge(13, 0, cols - 1, 2);
        b.stairsV(13, mc - 2, 2, 5);
        b.stairsV(13, 14, 2, 3);
        b.stairsV(13, 84, 2, 3);
        b.rect(36, 0, 38, cols - 1, SAND);
        b.ledge(36, 0, cols - 1, 2);
        b.stairsV(36, mc - 2, 2, 5);
        b.stairsV(36, 20, 2, 3);
        b.stairsV(36, 80, 2, 3);
        // Estrada central
        b.route(new double[][]{{0, mc}, {10, mc + 3}, {24, mc - 3}, {40, mc + 2}, {rows - 1, mc}}, 5, ROAD, SAND, GRASS, ROCK, BUSH);
        // Formações rochosas (mesas) nos flancos
        b.rect(17, 4, 21, 20, CLIFF);
        b.rect(22, 4, 23, 20, CLIFF);
        b.rect(17, 78, 21, 94, CLIFF);
        b.rect(22, 78, 23, 94, CLIFF);
        b.rect(26, 8, 28, 22, CLIFF);
        b.rect(29, 8, 30, 22, CLIFF);
        b.rect(26, 76, 28, 90, CLIFF);
        b.rect(29, 76, 30, 90, CLIFF);
        b.rect(17, 28, 21, 40, SAND);
        b.scatter(ROCK, 50, new int[][]{{0, mc}, {rows - 1, mc}, {23, 47}, {25, 50}}, 5, SAND);
        b.scatter(BUSH, 25, null, 0, SAND);
        b.scatter(BERRY_TREE, 8, null, 0, GRASS);
        b.scatter(TREE, 16, null, 0, GRASS);
        b.scatter(FLOWERS, 30, null, 0, GRASS, SAND);
        b.route(new double[][]{{0, mc}, {10, mc + 3}, {24, mc - 3}, {40, mc + 2}, {rows - 1, mc}}, 5, ROAD, SAND, GRASS, ROCK, BUSH, TREE, FLOWERS);
        b.clearArea(21, 42, 29, 58, SAND);
        b.rect(0, mc - 1, 3, mc + 1, ROAD);
        b.rect(rows - 4, mc - 1, rows - 1, mc + 1, ROAD);
        b.portalsTB(3, true, true);
        return b.g;
    }

    // ------------------------------------------------------------------
    // MINA (portais N/S): túneis escavados, trilhos de carrinho e depósitos de minério
    // ------------------------------------------------------------------
    private static int[][] mine(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2010);
        int mc = cols / 2;
        b.fill(CLIFF);
        // Galeria principal vertical (com trilho) e galerias laterais
        b.rect(0, mc - 3, rows - 1, mc + 3, FLOOR);
        b.rect(10, 8, 14, cols - 9, FLOOR);
        b.rect(34, 8, 38, cols - 9, FLOOR);
        b.rect(10, 8, 38, 12, FLOOR);
        b.rect(10, cols - 13, 38, cols - 9, FLOOR);
        // Câmaras largas
        b.blob(24, 24, 8, 10, FLOOR, 0.2, 91);
        b.blob(24, 76, 8, 10, FLOOR, 0.2, 92);
        b.blob(24, 50, 6, 10, FLOOR, 0.15, 93);
        // Trilhos (estrada) pelo meio da galeria vertical e nas laterais
        b.rect(0, mc, rows - 1, mc, ROAD);
        b.rect(12, 10, 12, cols - 11, ROAD);
        b.rect(36, 10, 36, cols - 11, ROAD);
        // Poça d'água subterrânea
        b.blob(24, 24, 3, 4, WATER, 0.2, 94);
        b.blob(24, 76, 3, 4, WATER, 0.2, 95);
        // Nível elevado com escada: plataforma de carga
        b.rect(16, 66, 19, 86, CLIFF);
        b.rect(20, 66, 21, 86, CLIFF);
        b.rect(20, 74, 21, 78, STAIRS);
        // Minérios (cristais), pilares de escora e braseiros
        b.scatter(SULFUR_CRYSTAL, 10, new int[][]{{0, mc}, {rows - 1, mc}, {23, 47}, {25, 50}}, 5, FLOOR);
        b.scatter(ROCK, 40, new int[][]{{0, mc}, {rows - 1, mc}, {23, 47}, {25, 50}}, 4, FLOOR);
        for (int r = 4; r < rows - 4; r += 6) { b.set(r, mc - 3, PILLAR); b.set(r, mc + 3, PILLAR); }
        for (int r = 4; r < rows - 4; r += 6) { b.set(r, mc - 3, PILLAR); b.set(r, mc + 3, PILLAR); }
        b.set(8, mc - 2, BRAZIER); b.set(rows - 9, mc + 2, BRAZIER);
        b.rect(0, mc - 1, 4, mc + 1, FLOOR);
        b.rect(rows - 5, mc - 1, rows - 1, mc + 1, FLOOR);
        b.rect(0, mc, 4, mc, ROAD);
        b.rect(rows - 5, mc, rows - 1, mc, ROAD);
        b.clearArea(21, 42, 29, 58, FLOOR);
        b.portalsTB(3, true, true);
        return b.g;
    }

    // ------------------------------------------------------------------
    // RIO (portais L/O): rio largo sinuoso com margens, pontes e escada de pedra
    // ------------------------------------------------------------------
    private static int[][] rio(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2011);
        int mr = rows / 2;
        b.fill(GRASS);
        b.groves(TREE, 0.5, 0.8, 0.22, 331, GRASS);
        b.scatter(FLOWERS, 100, null, 0, GRASS);
        // Rio sinuoso de norte a sul, com margem de areia
        double[][] river = {{0, 40}, {10, 47}, {20, 44}, {mr, 52}, {32, 56}, {40, 48}, {rows - 1, 52}};
        b.route(river, 13, SAND);
        b.route(river, 9, WATER);
        b.route(river, 4, DEEP_WATER);
        // Estrada dos portais, com pontes onde cruza o rio (uma delas no centro do mapa)
        b.routeWithBridges(new double[][]{{mr, 0}, {mr + 2, 14}, {mr, 30}, {mr, 50}, {mr - 2, 70}, {mr, 86}, {mr, 99}}, 3, ROAD, BRIDGE);
        b.routeWithBridges(new double[][]{{8, 34}, {8, 55}}, 3, ROAD, BRIDGE);
        b.routeWithBridges(new double[][]{{42, 40}, {42, 62}}, 3, ROAD, BRIDGE);
        // Penhasco na margem oeste com escada de acesso a um mirante
        b.force(4, 4, 14, 26, GRASS);
        b.ledge(15, 4, 26, 2);
        b.stairsV(15, 14, 2, 3);
        b.cliffV(3, 4, 16);
        b.force(18, 6, 24, 24, GRASS);
        b.set(9, 15, SIGN);
        b.scatter(BUSH, 26, null, 0, GRASS);
        b.scatter(ROCK, 22, null, 0, SAND, GRASS);
        b.clearArea(18, 40, 32, 60, GRASS);
        b.force(mr - 1, 0, mr + 1, 4, ROAD);
        b.force(mr - 1, cols - 5, mr + 1, cols - 1, ROAD);
        b.portalsLR(3);
        return b.g;
    }

    // ------------------------------------------------------------------
    // OCEANO (portais L/O): ilhas ligadas por píeres
    // ------------------------------------------------------------------
    private static int[][] ocean(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2012);
        int mr = rows / 2;
        b.fill(DEEP_WATER);
        // Plataforma rasa ao redor das ilhas
        double[][] isl = {{mr, 7, 6, 8}, {13, 28, 7, 10}, {36, 30, 7, 10}, {mr, 50, 10, 14}, {10, 72, 7, 10}, {38, 72, 7, 10}, {mr, 93, 6, 8}};
        for (int i = 0; i < isl.length; i++) b.blob(isl[i][0], isl[i][1], isl[i][2] + 3, isl[i][3] + 4, WATER, 0.2, 111 + i);
        for (int i = 0; i < isl.length; i++) b.blob(isl[i][0], isl[i][1], isl[i][2], isl[i][3], SAND, 0.25, 121 + i);
        for (int i = 0; i < isl.length; i++) b.blob(isl[i][0], isl[i][1], isl[i][2] * 0.55, isl[i][3] * 0.55, GRASS, 0.25, 131 + i);
        // Píeres de madeira
        b.bridgeH(mr, 10, 40, 1, BRIDGE);
        b.bridgeH(mr + 1, 10, 40, 1, BRIDGE);
        b.bridgeH(mr, 60, 90, 1, BRIDGE);
        b.bridgeH(mr + 1, 60, 90, 1, BRIDGE);
        b.bridgeV(28, 16, 32, 1, BRIDGE);
        b.bridgeV(29, 16, 32, 1, BRIDGE);
        b.bridgeV(72, 14, 34, 1, BRIDGE);
        b.bridgeV(73, 14, 34, 1, BRIDGE);
        // Vegetação, rochas na costa
        b.scatter(BERRY_TREE, 16, null, 0, GRASS);
        b.scatter(TREE, 24, null, 0, GRASS);
        b.scatter(FLOWERS, 40, null, 0, GRASS);
        b.scatter(ROCK, 40, new int[][]{{mr, 0}, {mr, 99}, {23, 47}, {25, 50}}, 4, SAND);
        b.scatter(BUSH, 16, null, 0, GRASS);
        // Ilha central com templo: pequeno altar de pedra com escada
        b.rect(mr - 3, 46, mr - 3, 54, WALL);
        b.rect(mr - 2, 46, mr - 2, 54, WALL);
        b.rect(mr - 1, 46, mr - 1, 54, STONE_PAVEMENT);
        b.rect(mr - 2, 49, mr - 2, 51, STAIRS);
        b.rect(mr - 1, 49, mr - 1, 51, STONE_PAVEMENT);
        b.set(mr - 4, 45, PILLAR); b.set(mr - 4, 55, PILLAR);
        b.clearArea(21, 42, 29, 58, SAND);
        b.rect(mr - 2, 46, mr - 2, 48, WALL);
        b.rect(mr - 2, 52, mr - 2, 54, WALL);
        b.rect(mr - 3, 46, mr - 3, 54, WALL);
        b.rect(mr - 1, 0, mr + 1, 3, SAND);
        b.rect(mr - 1, cols - 4, mr + 1, cols - 1, SAND);
        b.rect(mr, 0, mr + 1, 0, SAND);
        b.rect(mr - 1, 3, mr + 2, 4, BRIDGE);
        b.rect(mr - 1, cols - 5, mr + 2, cols - 4, BRIDGE);
        b.portalsLR(3);
        return b.g;
    }

    // ------------------------------------------------------------------
    // DESERTO (portais N/S): dunas, oásis, mesas de arenito, ruínas com escada
    // ------------------------------------------------------------------
    private static int[][] desert(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2013);
        int mc = cols / 2;
        b.fill(SAND);
        // Dunas escuras (alterna areia/estrada seca em faixas diagonais)
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            double v = Math.sin((c + r * 1.7) * 0.25) + Math.sin((c * 0.7 - r) * 0.18);
            if (v > 1.15) b.g[r][c] = ROAD;
        }
        // Oásis
        b.blob(14, 20, 5, 9, GRASS, 0.25, 141);
        b.blob(14, 20, 3, 6, WATER, 0.25, 142);
        b.blob(14, 20, 1.3, 3, DEEP_WATER, 0.2, 143);
        b.blob(38, 78, 5, 9, GRASS, 0.25, 144);
        b.blob(38, 78, 3, 6, WATER, 0.25, 145);
        // Mesas de arenito com escadas
        b.rect(30, 8, 33, 26, CLIFF);
        b.rect(34, 8, 35, 26, CLIFF);
        b.rect(26, 8, 29, 26, SAND);
        b.rect(34, 15, 35, 18, STAIRS);
        b.rect(10, 70, 13, 92, CLIFF);
        b.rect(14, 70, 15, 92, CLIFF);
        b.rect(14, 78, 15, 81, STAIRS);
        b.rect(6, 70, 9, 92, SAND);
        // Ruína (pirâmide em degraus) no centro-norte
        b.rect(4, 42, 5, 58, WALL);
        b.rect(6, 42, 7, 58, WALL);
        b.rect(8, 42, 8, 58, STONE_PAVEMENT);
        b.rect(6, 49, 7, 51, STAIRS);
        b.rect(8, 44, 11, 56, STONE_PAVEMENT);
        b.set(9, 43, PILLAR); b.set(9, 57, PILLAR);
        // Estrada
        b.route(new double[][]{{0, mc}, {10, mc - 2}, {24, mc + 3}, {38, mc - 2}, {rows - 1, mc}}, 5, ROAD, SAND, GRASS, ROCK, CACTUS, TREE, FLOWERS);
        b.scatter(CACTUS, 40, new int[][]{{0, mc}, {rows - 1, mc}, {23, 47}, {25, 50}}, 5, SAND);
        b.scatter(ROCK, 30, new int[][]{{0, mc}, {rows - 1, mc}, {23, 47}, {25, 50}}, 5, SAND, ROAD);
        b.scatter(DEAD_TREE, 10, new int[][]{{0, mc}, {rows - 1, mc}, {23, 47}, {25, 50}}, 5, SAND);
        b.scatter(TREE, 8, null, 0, GRASS);
        b.clearArea(21, 42, 29, 58, SAND);
        b.rect(0, mc - 1, 3, mc + 1, ROAD);
        b.rect(rows - 4, mc - 1, rows - 1, mc + 1, ROAD);
        b.portalsTB(3, true, true);
        return b.g;
    }

    // ------------------------------------------------------------------
    // VULCÃO (portais N/S): rios de lava com pontes, platôs e escadas de basalto
    // ------------------------------------------------------------------
    private static int[][] volcano(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2014);
        int mc = cols / 2;
        b.fill(BASALT_FLOOR);
        b.patches(ASH_GROUND, 0.58, 0.3, 411, BASALT_FLOOR);
        // Paredes de rocha nas laterais
        b.rect(0, 0, rows - 1, 8, CLIFF);
        b.rect(0, cols - 9, rows - 1, cols - 1, CLIFF);
        for (int r = 0; r < rows; r++) {
            int w = (int) Math.round(Math.sin(r * 0.3) * 2 + Math.sin(r * 0.8) * 1);
            for (int c = 8; c <= 10 + w; c++) b.set(r, c, CLIFF);
            for (int c = cols - 11 - w; c < cols - 8; c++) b.set(r, c, CLIFF);
        }
        // Rios de lava transversais com pontes
        b.route(new double[][]{{8, 11}, {10, 30}, {7, 50}, {10, 70}, {8, 88}}, 5, LAVA, BASALT_FLOOR, ASH_GROUND);
        b.route(new double[][]{{34, 11}, {32, 30}, {36, 50}, {32, 70}, {34, 88}}, 5, LAVA, BASALT_FLOOR, ASH_GROUND);
        b.route(new double[][]{{9, 55}, {20, 62}, {34, 56}}, 4, LAVA, BASALT_FLOOR, ASH_GROUND);
        b.blob(22, 22, 4, 6, LAVA, 0.3, 151, BASALT_FLOOR, ASH_GROUND);
        // Platô elevado com escada (parte sul)
        b.rect(38, 14, 39, 40, CLIFF);
        b.rect(40, 14, 40, 40, CLIFF);
        b.rect(40, 26, 40, 28, STAIRS);
        b.rect(39, 26, 39, 28, STAIRS);
        b.rect(41, 14, 48, 40, BASALT_FLOOR);
        // Estrada principal: vira ponte onde cruza a lava
        b.routeWithBridges(new double[][]{{0, mc}, {12, mc + 1}, {24, mc - 2}, {36, mc + 2}, {rows - 1, mc}}, 5, ROAD, BRIDGE);
        b.scatter(ROCK, 30, new int[][]{{0, mc}, {rows - 1, mc}, {23, 47}, {25, 50}, {43, 47}}, 5, BASALT_FLOOR, ASH_GROUND);
        b.scatter(MAGMA_VENT, 12, new int[][]{{0, mc}, {rows - 1, mc}, {23, 47}, {25, 50}, {43, 47}}, 5, BASALT_FLOOR);
        b.scatter(SULFUR_CRYSTAL, 12, new int[][]{{0, mc}, {rows - 1, mc}, {23, 47}, {25, 50}, {43, 47}}, 5, BASALT_FLOOR);
        // O Charizard chefe nasce em (47,43): precisa ser terreno firme
        b.clearArea(40, 43, 46, 53, BASALT_FLOOR);
        b.rect(40, 42, 47, 54, BASALT_FLOOR);
        b.clearArea(21, 42, 29, 58, BASALT_FLOOR);
        b.rect(0, mc - 1, 3, mc + 1, ROAD);
        b.rect(rows - 4, mc - 1, rows - 1, mc + 1, ROAD);
        b.portalsTB(3, true, true);
        return b.g;
    }

    // ------------------------------------------------------------------
    // SANTUÁRIO DE MAGMA (portais L/O, chefe no centro): arena circular, rios de lava e pontes
    // ------------------------------------------------------------------
    private static int[][] moltenSanctum(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2015);
        int mr = rows / 2, mc = cols / 2;
        b.fill(BASALT_FLOOR);
        b.patches(ASH_GROUND, 0.58, 0.3, 412, BASALT_FLOOR);
        b.border(2, CLIFF);
        // Rios de lava sinuosos
        for (int r = 2; r < rows - 2; r++) {
            int o1 = (int) Math.round(Math.sin(r * 0.35) * 2), o2 = (int) Math.round(Math.cos(r * 0.35) * 2);
            for (int w = -2; w <= 2; w++) {
                b.set(r, cols / 3 + o1 + w, LAVA);
                b.set(r, cols * 2 / 3 + o2 + w, LAVA);
            }
        }
        // Arena circular elevada com muro baixo e escadas nos 4 lados
        int rad = 11;
        for (int r = mr - rad - 2; r <= mr + rad + 2; r++) for (int c = mc - rad - 2; c <= mc + rad + 2; c++) {
            double d = Math.hypot((r - mr) * 1.0, (c - mc) * 0.8 * 1.0);
            if (d <= rad) b.set(r, c, BASALT_FLOOR);
            else if (d <= rad + 1.2) b.set(r, c, CRACKED_OBSIDIAN);
        }
        // Aberturas (portões) da arena em 4 lados + escadas
        b.rect(mr - 1, mc - rad - 3, mr + 1, mc - rad + 1, BASALT_FLOOR);
        b.rect(mr - 1, mc + rad - 1, mr + 1, mc + rad + 3, BASALT_FLOOR);
        b.rect(mr - rad - 2, mc - 1, mr - rad + 1, mc + 1, BASALT_FLOOR);
        b.rect(mr + rad - 1, mc - 1, mr + rad + 2, mc + 1, BASALT_FLOOR);
        // Pontes sobre a lava (3 corredores) e da borda ao portal
        int[] bridgeRows = {rows / 4, mr, rows * 3 / 4};
        for (int br : bridgeRows) for (int c = 2; c < cols - 2; c++) {
            if (b.g[br][c] == LAVA || b.g[br][c] == CRACKED_OBSIDIAN) {
                for (int w = -1; w <= 1; w++) if (b.get(br + w, c) == LAVA || b.get(br + w, c) == CRACKED_OBSIDIAN) b.g[br + w][c] = MAGMA_BRIDGE;
            }
        }
        // Pilares ao redor da arena e cristais de enxofre / vents
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4 + 0.39;
            int pr = mr + (int) Math.round(Math.sin(a) * (rad + 5)), pc = mc + (int) Math.round(Math.cos(a) * (rad + 6) * 1.0);
            if (b.get(pr, pc) == BASALT_FLOOR || b.get(pr, pc) == ASH_GROUND) b.set(pr, pc, PILLAR);
        }
        b.scatter(SULFUR_CRYSTAL, 14, new int[][]{{mr, 0}, {mr, cols - 1}, {mr, mc}}, 14, BASALT_FLOOR, ASH_GROUND);
        b.scatter(MAGMA_VENT, 10, new int[][]{{mr, 0}, {mr, cols - 1}, {mr, mc}}, 14, BASALT_FLOOR, ASH_GROUND);
        b.scatter(BRAZIER, 10, new int[][]{{mr, 0}, {mr, cols - 1}, {mr, mc}}, 12, BASALT_FLOOR, ASH_GROUND);
        // Entradas: borda de cada lado até o portal
        b.rect(mr - 1, 0, mr + 1, 3, BASALT_FLOOR);
        b.rect(mr - 1, cols - 4, mr + 1, cols - 1, BASALT_FLOOR);
        b.rect(mr, 1, mr, 2, MAGMA_BRIDGE);
        b.rect(mr, cols - 3, mr, cols - 2, MAGMA_BRIDGE);
        b.clearArea(mr - 8, mc - 9, mr + 8, mc + 9, BASALT_FLOOR);
        b.portalsLR(1);
        return b.g;
    }

    // ------------------------------------------------------------------
    // MONTANHA NEVADA (portais N/S): penhascos de gelo, lago congelado, trilha com escadas
    // ------------------------------------------------------------------
    private static int[][] snowMountain(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2016);
        int mc = cols / 2;
        b.fill(SNOW);
        // Paredões de gelo nas laterais
        b.rect(0, 0, rows - 1, 12, CLIFF);
        b.rect(0, cols - 13, rows - 1, cols - 1, CLIFF);
        for (int r = 0; r < rows; r++) {
            int w = (int) Math.round(Math.sin(r * 0.37) * 3 + Math.sin(r * 0.11) * 2);
            for (int c = 12; c <= 14 + w; c++) b.set(r, c, CLIFF);
            for (int c = cols - 15 - w; c < cols - 12; c++) b.set(r, c, CLIFF);
        }
        // Lago congelado (água) com bordas
        b.blob(28, 30, 6, 11, WATER, 0.25, 161, SNOW);
        b.blob(28, 30, 3, 6, DEEP_WATER, 0.2, 162, WATER);
        b.blob(20, 72, 5, 9, WATER, 0.25, 163, SNOW);
        // Patamares: dois degraus de gelo com escadas
        b.ledge(14, 16, cols - 17, 2);
        b.stairsV(14, mc - 2, 2, 5);
        b.ledge(36, 16, cols - 17, 2);
        b.stairsV(36, mc - 2, 2, 5);
        // Trilha (ponte de madeira onde cruza o lago)
        b.routeWithBridges(new double[][]{{0, mc}, {10, mc + 3}, {20, mc - 3}, {30, mc + 2}, {40, mc - 1}, {rows - 1, mc}}, 4, ROAD, BRIDGE);
        // Cristais de gelo e rochas; árvores nevadas
        b.scatter(SNOW_CRYSTAL, 34, new int[][]{{0, mc}, {rows - 1, mc}, {25, 50}, {23, 47}}, 5, SNOW);
        b.scatter(ROCK, 28, new int[][]{{0, mc}, {rows - 1, mc}, {25, 50}, {23, 47}}, 4, SNOW);
        b.scatter(TREE, 24, new int[][]{{0, mc}, {rows - 1, mc}, {25, 50}, {23, 47}}, 5, SNOW);
        // O Articuno nasce em (50,25)
        b.clearArea(21, 42, 29, 58, SNOW);
        b.rect(0, mc - 1, 3, mc + 1, ROAD);
        b.rect(rows - 4, mc - 1, rows - 1, mc + 1, ROAD);
        b.portalsTB(3, true, true);
        return b.g;
    }

    // ------------------------------------------------------------------
    // PICO NEVADO (portal só ao sul): pico em escadas no céu, rodeado de nuvens
    // ------------------------------------------------------------------
    private static int[][] snowPeak(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2017);
        int mc = cols / 2;
        b.fill(CLOUD);
        // Silhueta em forma de pico: afunila de baixo para cima
        for (int r = 0; r < rows; r++) {
            int half = (int) (6 + (cols * 0.42 - 6) * (r / (double) (rows - 1)));
            for (int c = mc - half; c <= mc + half; c++) b.set(r, c, SNOW);
        }
        // Borda do pico em penhasco (face de gelo)
        for (int r = 0; r < rows; r++) {
            int half = (int) (6 + (cols * 0.42 - 6) * (r / (double) (rows - 1)));
            b.set(r, mc - half - 1, CLIFF); b.set(r, mc - half - 2, CLIFF);
            b.set(r, mc + half + 1, CLIFF); b.set(r, mc + half + 2, CLIFF);
        }
        for (int c = mc - 9; c <= mc + 9; c++) { b.set(0, c, CLIFF); b.set(1, c, CLIFF); }
        for (int c = mc - 8; c <= mc + 8; c++) b.set(2, c, SNOW);
        // Terraços em escada subindo o pico
        for (int step = 0; step < 3; step++) {
            int rr = 10 + step * 12;
            int half = (int) (6 + (cols * 0.42 - 6) * (rr / (double) (rows - 1))) - 2;
            b.ledge(rr, mc - half, mc + half, 2);
            b.stairsV(rr, mc - 2, 2, 5);
            if (step % 2 == 0) { b.stairsV(rr, mc - half + 2, 2, 3); } else { b.stairsV(rr, mc + half - 4, 2, 3); }
        }
        // Trilha central até o cume
        b.rect(3, mc, rows - 1, mc, ROAD);
        b.rect(3, mc - 1, rows - 1, mc + 1, ROAD);
        for (int step = 0; step < 3; step++) {
            int rr = 10 + step * 12;
            b.stairsV(rr, mc - 2, 2, 5);
        }
        b.scatter(SNOW_CRYSTAL, 22, new int[][]{{rows - 1, mc}, {25, 50}, {23, 47}}, 5, SNOW);
        b.scatter(ROCK, 18, new int[][]{{rows - 1, mc}, {25, 50}, {23, 47}}, 4, SNOW);
        b.set(4, mc - 3, BRAZIER); b.set(4, mc + 3, BRAZIER);
        b.clearArea(21, 42, 29, 58, SNOW);
        b.rect(rows - 4, mc - 1, rows - 1, mc + 1, ROAD);
        b.portalsTB(3, false, true);
        return b.g;
    }

    // ------------------------------------------------------------------
    // ILHA DO MEDO (portal ao norte): ilha sombria, cemitério e capela em ruínas
    // ------------------------------------------------------------------
    private static int[][] fearIsland(int rows, int cols) {
        MapBuilder b = new MapBuilder(rows, cols, 2018);
        int mc = cols / 2;
        b.fill(DEEP_WATER);
        b.blob(25, 50, 22, 44, WATER, 0.12, 171);
        b.blob(25, 50, 19, 40, SAND, 0.15, 172);
        b.blob(25, 50, 16, 35, GRASS, 0.18, 173);
        // Doca ao norte ligando ao portal
        b.bridgeV(mc - 1, 0, 8, 3, BRIDGE);
        b.rect(1, mc - 1, 9, mc + 1, BRIDGE);
        // Estrada tortuosa até a capela
        b.route(new double[][]{{8, mc}, {14, mc - 4}, {20, mc + 4}, {26, mc}}, 3, ROAD, GRASS, SAND, WATER);
        // Capela em ruínas ao norte-centro (paredes quebradas + escada)
        b.rect(12, 62, 13, 82, WALL);
        b.rect(14, 62, 14, 82, STONE_PAVEMENT);
        b.rect(15, 64, 19, 80, STONE_PAVEMENT);
        b.rect(14, 69, 14, 73, STAIRS);
        b.set(16, 63, WALL); b.set(17, 63, WALL); b.set(16, 81, WALL);
        b.set(18, 66, PILLAR); b.set(18, 78, PILLAR);
        b.set(16, 72, BRAZIER);
        // Cemitério a oeste: lápides em fileiras com muro baixo
        b.rect(30, 18, 41, 40, GRASS);
        b.frame(30, 18, 41, 40, 1, WALL);
        b.rect(30, 27, 30, 30, GRASS);
        for (int r = 32; r <= 40; r += 3) for (int c = 21; c <= 38; c += 3) b.set(r, c, TOMBSTONE);
        // Árvores mortas, névoa de arbustos secos e pedras
        b.scatter(DEAD_TREE, 50, new int[][]{{mc, 0}, {0, mc}, {25, 50}, {23, 47}}, 4, GRASS);
        b.scatter(BUSH, 30, null, 0, GRASS);
        b.scatter(ROCK, 24, new int[][]{{0, mc}, {25, 50}, {23, 47}}, 4, GRASS, SAND);
        b.scatter(TOMBSTONE, 10, new int[][]{{0, mc}, {25, 50}, {23, 47}}, 6, GRASS);
        b.clearArea(21, 42, 29, 58, GRASS);
        b.rect(0, mc - 1, 3, mc + 1, BRIDGE);
        b.set(0, mc, PORTAL);
        b.set(0, mc - 1, PORTAL);
        b.set(0, mc + 1, PORTAL);
        b.set(1, mc, BRIDGE);
        b.rect(1, mc - 1, 3, mc + 1, BRIDGE);
        return b.g;
    }
}
