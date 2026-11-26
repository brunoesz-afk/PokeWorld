package com.example.pokeworld.entities;

/** Curva de dificuldade do jogo num lugar só: EXP para o próximo nível, XP por abate e chance de captura. */
public final class Progression {
    private Progression() {}

    /** EXP necessária para sair do nível dado (treinador e Pokémon usam a mesma curva). 135, 330, 585, ... 2700 no nv.10, 8400 no nv.20. */
    public static int expToNext(int level) {
        level = Math.max(1, level);
        return 120 * level + 15 * level * level;
    }

    /** Fator do XP por abate: lutar contra inimigos muito mais fracos rende cada vez menos (mínimo 20%). */
    public static double expFactor(int myLevel, int enemyLevel) {
        double f = 1.0 - 0.08 * (myLevel - enemyLevel - 3);
        return Math.max(0.2, Math.min(1.0, f));
    }

    /**
     * Chance de capturar um Pokémon derrotado. Nível alto do alvo, chefes e shiny dificultam;
     * o nível do seu companheiro e o talento Domador ajudam.
     */
    /** Nível com que um Pokémon capturado entra na conta: metade do nível do selvagem (mín. 1). 42 -> 21, 41 -> 20. */
    public static int capturedLevel(int wildLevel) { return Math.max(1, wildLevel / 2); }

    public static double captureChance(int targetLevel, boolean boss, boolean shiny, int myLevel, double domadorBonus) {
        double c = 0.60 - 0.01 * targetLevel + 0.004 * myLevel + domadorBonus + com.example.pokeworld.managers.EventCalendar.captureBonus();
        if (boss) c -= 0.30;
        if (shiny) c -= 0.15;
        if (targetLevel <= 1) c = Math.max(c, 0.90);   // Pokémon de nível 1 (tutorial) é fácil
        return Math.max(0.05, Math.min(0.90, c));
    }
}
