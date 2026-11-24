package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;

/** Compra com pontos pede um segundo toque (em até 4 s) para confirmar, para evitar gastar sem querer. */
public final class BuyConfirm {
    private BuyConfirm() {}
    private static String lastKey = null;
    private static long lastAt = 0;

    /** Retorna true quando é o segundo toque na mesma compra. No primeiro toque, avisa o preço e retorna false. */
    public static boolean confirm(GameView gv, String key, String label, int price) {
        long now = System.currentTimeMillis();
        if (key.equals(lastKey) && now - lastAt < 4000) { lastKey = null; return true; }
        lastKey = key; lastAt = now;
        gv.ui.showMessage("💠 " + label + " por " + price + " pontos. Toque de novo para confirmar.");
        return false;
    }
}
