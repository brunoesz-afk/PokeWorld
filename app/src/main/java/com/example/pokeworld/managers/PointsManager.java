package com.example.pokeworld.managers;

import com.example.pokeworld.core.DatabaseHelper;

/**
 * Carteira de Pontos (1 real = 1 ponto) e assinatura VIP.
 * O saldo verdadeiro mora no SERVIDOR (ver {@link WalletManager}): o valor em kv "points" é só uma cópia para
 * mostrar na tela. Toda compra debita no servidor antes de entregar o item.
 */
public final class PointsManager {
    private PointsManager() {}

    /** true = mostra o botão "+50 pontos (TESTE)" na loja. Desligue antes de publicar o jogo com cobrança real. */
    public static final boolean TEST_MODE = false;
    public static final int TEST_GRANT = 50;

    public static final int PRICE_VIP = 10, PRICE_PASS = 35, VIP_DAYS = 30;
    public static final double VIP_XP = 0.10, VIP_GOLD = 0.10;

    private static RewardManager.Store store;

    public static void init(DatabaseHelper db) { store = RewardManager.of(db); }
    public static void init(RewardManager.Store s) { store = s; }
    public static RewardManager.Store store() { return store; }

    // ------------------------------------------------------------------ saldo
    public static int balance(RewardManager.Store s) { return Math.max(0, s.get("points", 0)); }

    public static void add(RewardManager.Store s, int n) { if (n > 0) s.set("points", balance(s) + n); }

    /** true = o servidor já debitou esta compra; spend() só libera a entrega, sem mexer no saldo local. */
    public static volatile boolean prepaid = false;
    /** true = o saldo é do servidor: ninguém gasta pontos só no aparelho. */
    public static final boolean SERVER_WALLET = true;

    /** Gasta pontos. Retorna false (sem mexer em nada) se o saldo não bastar. */
    public static boolean spend(RewardManager.Store s, int n) {
        if (prepaid) return n > 0;
        if (SERVER_WALLET) return false;
        if (n <= 0 || balance(s) < n) return false;
        s.set("points", balance(s) - n);
        return true;
    }

    // ------------------------------------------------------------------ VIP
    public static int vipSecondsLeft(RewardManager.Store s) { return Math.max(0, s.get("vip_until", 0) - RewardManager.nowSec()); }
    public static boolean isVip(RewardManager.Store s) { return s != null && vipSecondsLeft(s) > 0; }
    public static boolean isVip() { return isVip(store); }

    /** Compra (ou renova: soma 30 dias ao tempo que sobrou). */
    public static boolean buyVip(RewardManager.Store s) {
        if (!spend(s, PRICE_VIP)) return false;
        int base = Math.max(RewardManager.nowSec(), s.get("vip_until", 0));
        s.set("vip_until", base + VIP_DAYS * 86400);
        return true;
    }

    public static double vipXpBonus() { return isVip() ? VIP_XP : 0; }
    public static double vipGoldBonus() { return isVip() ? VIP_GOLD : 0; }

    /** "12d 4h" ou "3h20" — para mostrar o tempo de VIP que resta. */
    public static String vipText(RewardManager.Store s) {
        int sec = vipSecondsLeft(s);
        if (sec <= 0) return "inativo";
        int d = sec / 86400, h = (sec % 86400) / 3600;
        return d > 0 ? d + "d " + h + "h" : h + "h" + String.format("%02d", (sec % 3600) / 60);
    }
}
