package com.example.pokeworld.world;

/**
 * Variantes de mapa: "FOREST#2" é a "cópia" da FOREST, com o mesmo visual mas o layout espelhado, outros pontos de
 * spawn e outros Pokémon. Os mapas de bioma continuam sendo escritos uma vez só (MapBuilder); a variante só espelha.
 * Classe sem dependências do Android.
 */
public final class MapVariants {
    private MapVariants() {}

    public static final char SEP = '#';

    public static boolean isVariant(String name) { return name != null && name.indexOf(SEP) >= 0; }

    /** "FOREST#2" -> "FOREST" (para escolher visual, tema, escuridão, clima...). */
    public static String base(String name) {
        if (name == null) return "";
        int i = name.indexOf(SEP);
        return i < 0 ? name : name.substring(0, i);
    }

    /** "FOREST#2" -> 2; mapa normal -> 1. */
    public static int index(String name) {
        if (!isVariant(name)) return 1;
        try { return Integer.parseInt(name.substring(name.indexOf(SEP) + 1)); } catch (Exception e) { return 2; }
    }

    /**
     * Espelha o mapa da esquerda para a direita (só dados). Com largura par, a coluna do meio (onde o jogador
     * nasce) fica no mesmo lugar, então o ponto de entrada continua sendo o mesmo tile de antes.
     */
    public static void mirror(int[][] g) {
        for (int[] row : g) {
            int n = row.length;
            int[] old = row.clone();
            if (n % 2 == 0) for (int c = 0; c < n; c++) row[c] = old[(n - c) % n];
            else for (int c = 0; c < n; c++) row[c] = old[n - 1 - c];
        }
    }
}
