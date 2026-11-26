package com.example.pokeworld.world;

/**
 * Geometria da sala da Arena (mapa "DUEL_ARENA"): o oponente em cima, a arena quadrada (13x13 tiles) com a LINHA no meio,
 * e o seu retângulo de treinador embaixo, separado da arena por uma grade com um portão no meio.
 * Você anda livremente atrás da linha (no seu retângulo e na sua metade da arena) e lança o Pokémon de onde estiver.
 * Sem dependências do Android, para poder ser testada no computador.
 */
public final class ArenaLayout {
    private ArenaLayout() {}

    public static final String MAP = "DUEL_ARENA";
    public static final int ROWS = 32, COLS = 21;
    private static final double T = 64;     // = TileMap.TILE_SIZE (fixo aqui para esta classe não depender do Android)

    // arena (tiles, inclusivo) e a linha do meio
    public static final int AR0 = 9, AR1 = 21, AC0 = 4, AC1 = 16, LINE_ROW = 15;
    // retângulos dos treinadores
    public static final int FOE_R0 = 2, FOE_R1 = 6, ME_R0 = 25, ME_R1 = 30;
    // grades: a do oponente é fechada; a sua tem um portão no meio
    public static final int RAIL_FOE_ROW = 7, RAIL_ME_ROW = 23, GATE_C0 = 8, GATE_C1 = 12;

    public static double left() { return AC0 * T; }
    public static double right() { return (AC1 + 1) * T; }
    public static double top() { return AR0 * T; }
    public static double bottom() { return (AR1 + 1) * T; }
    public static double width() { return right() - left(); }
    public static double centerX() { return (left() + right()) / 2.0; }
    public static double arenaCenterY() { return (top() + bottom()) / 2.0; }
    /** Y da linha do meio (centro do tile da linha). */
    public static double lineY() { return (LINE_ROW + 0.5) * T; }

    public static double spawnX() { return centerX(); }
    public static double spawnY() { return (ME_R0 + ME_R1 + 1) / 2.0 * T; }
    public static double foeTrainerX() { return centerX(); }
    public static double foeTrainerY() { return (FOE_R0 + FOE_R1 + 1) / 2.0 * T + 36; }

    /** O jogador nunca passa da linha: tudo acima dela (metade do oponente e o retângulo dele) é proibido. */
    public static boolean playerBlocked(double x, double y) { return y < lineY() + 28; }
}
