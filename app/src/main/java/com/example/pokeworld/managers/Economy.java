package com.example.pokeworld.managers;

/**
 * Balanceamento do dinheiro em um lugar só. Ouro é mais difícil de ganhar: inimigos soltam menos,
 * recompensas de missão/Torre/expedição pagam menos e a sucata vale menos no Mercador.
 * Para facilitar ou dificultar tudo de uma vez, mude os números abaixo.
 */
public final class Economy {
    private Economy() {}

    public static final double ENEMY_DROP = 0.55;   // ouro que cai dos inimigos
    public static final double REWARD = 0.70;       // missões, conquistas, diárias, Torre, expedições, bases rivais, invasões
    public static final double SELL = 0.60;         // preço de venda no Mercador
    public static final double FISH = 0.70;         // pesca

    public static int scale(int amount, double mult) {
        return amount <= 0 ? 0 : Math.max(1, (int) Math.round(amount * mult));
    }
}
