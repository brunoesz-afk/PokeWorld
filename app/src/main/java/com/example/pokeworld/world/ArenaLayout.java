package com.example.pokeworld.world;

/**
 * Geometria da sala da Arena (mapa "DUEL_ARENA"), em paisagem: o oponente em cima, a arena larga com a LINHA no meio
 * (lava e pontes nas laterais) e, embaixo, a SUA faixa de treinador, separada da arena por uma grade.
 * Você anda para os lados dentro da faixa e pode dar um passo à frente; o Pokémon é lançado na arena, na sua metade.
 * Sem dependências do Android, para poder ser testada no computador.
 */
public final class ArenaLayout {
    private ArenaLayout() {}

    public static final String MAP = "DUEL_ARENA";
    public static final int ROWS = 13, COLS = 25;
    private static final double T = 64;     // = TileMap.TILE_SIZE (fixo aqui para esta classe não depender do Android)

    // arena (tiles, inclusivo); a linha do meio fica no centro dela
    public static final int AR0 = 3, AR1 = 8, AC0 = 4, AC1 = 20;
    // faixas dos treinadores
    public static final int FOE_R0 = 1, FOE_R1 = 2, ME_R0 = 10, ME_R1 = 11;
    // grade entre a arena e a sua faixa
    public static final int RAIL_ROW = 9;

    public static double mapWidth() { return COLS * T; }
    public static double mapHeight() { return ROWS * T; }
    public static double left() { return AC0 * T; }
    public static double right() { return (AC1 + 1) * T; }
    public static double top() { return AR0 * T; }
    public static double bottom() { return (AR1 + 1) * T; }
    public static double width() { return right() - left(); }
    public static double centerX() { return (left() + right()) / 2.0; }
    public static double arenaCenterY() { return (top() + bottom()) / 2.0; }
    /** Y da linha do meio (centro da arena). */
    public static double lineY() { return arenaCenterY(); }

    // a sua faixa: para os lados e um passo à frente
    public static double meLeft() { return AC0 * T + 28; }
    public static double meRight() { return (AC1 + 1) * T - 28; }
    public static double meMinY() { return ME_R0 * T + 40; }
    public static double meMaxY() { return (ME_R1 + 1) * T - 22; }

    public static double spawnX() { return centerX(); }
    public static double spawnY() { return meMaxY(); }
    public static double foeTrainerX() { return centerX(); }
    public static double foeTrainerY() { return (FOE_R1 + 1) * T - 8; }

    /** O jogador só anda dentro da sua faixa (lados + um passo à frente); a arena e o lado do oponente são proibidos. */
    public static boolean playerBlocked(double x, double y) {
        return x < meLeft() || x > meRight() || y < meMinY() || y > meMaxY();
    }

    /** Quanto o jogador avançou na faixa: 0 = no fundo, 1 = um passo à frente. */
    public static double forward(double y) { return Math.max(0, Math.min(1, (meMaxY() - y) / (meMaxY() - meMinY()))); }
}
