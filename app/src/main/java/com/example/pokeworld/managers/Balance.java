package com.example.pokeworld.managers;

/**
 * Ajuste central do ritmo dos combates: todo dano recebido é multiplicado por DAMAGE (menor que 1)
 * e a vida máxima dos Pokémon por HP. Ficou em um lugar só para ser fácil de calibrar.
 * Efeito prático: um combate comum dura cerca de 3x mais que antes.
 */
public final class Balance {
    private Balance() {}

    /** Multiplicador do dano de ataques (recebido por Pokémon, inimigos e treinador). */
    public static final float DAMAGE = 0.6f;
    /** Multiplicador da vida máxima dos seus Pokémon. */
    public static final float HP = 1.8f;
    /** Vida dos inimigos selvagens (menor que HP para as caçadas não ficarem arrastadas demais). */
    public static final float ENEMY_HP = 1.25f;

    /** Aplica o fator de dano garantindo ao menos 1 de dano em golpes que acertam. */
    public static int dmg(int raw) {
        if (raw <= 0) return 0;
        return Math.max(1, Math.round(raw * DAMAGE));
    }
}
