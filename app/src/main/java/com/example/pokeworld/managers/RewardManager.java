package com.example.pokeworld.managers;

import com.example.pokeworld.core.DatabaseHelper;
import com.example.pokeworld.core.GameView;

import java.util.Calendar;
import java.util.function.Supplier;

/**
 * Central de recompensas do reino: Presente Diário (sequência de 7 dias), Baú do Evento do dia
 * e Recarga grátis de suprimentos (a cada 6 horas). Tudo é reivindicado pelo jogador.
 */
public final class RewardManager {
    private RewardManager() {}

    /** Onde os contadores ficam salvos (o jogo usa a tabela kv; os testes usam um mapa). */
    public interface Store { int get(String key, int def); void set(String key, int value); }

    public static Store of(final DatabaseHelper db) {
        return new Store() {
            @Override public int get(String key, int def) { return db.getKv(key, def); }
            @Override public void set(String key, int value) { db.setKv(key, value); }
        };
    }

    /** Relógio (trocável nos testes). */
    static Supplier<Calendar> clock = Calendar::getInstance;

    public static final class Prize {
        public final String icon; public final int gold, gems, qty; public final String item;
        Prize(String icon, int gold, int gems, String item, int qty) {
            this.icon = icon; this.gold = gold > 0 ? Economy.scale(gold, Economy.REWARD) : 0; this.gems = gems; this.item = item; this.qty = qty;
        }
        public String text() {
            StringBuilder sb = new StringBuilder();
            if (gold > 0) sb.append("🪙 ").append(gold);
            if (gems > 0) sb.append(sb.length() > 0 ? "  " : "").append("💎 ").append(gems);
            if (item != null) sb.append(sb.length() > 0 ? "  " : "").append(qty).append("x ").append(item);
            return sb.toString();
        }
        void give(GameView gv) {
            if (gold > 0) gv.player.addGold(gold);
            if (gems > 0) gv.player.addGems(gems);
            if (item != null) gv.player.addItem(item, qty);
        }
    }

    // ------------------------------------------------------------------ dias
    public static int dayKey(Calendar c) { return c.get(Calendar.YEAR) * 400 + c.get(Calendar.DAY_OF_YEAR); }
    public static int today() { return dayKey(clock.get()); }
    public static int yesterday() { Calendar c = clock.get(); c.add(Calendar.DAY_OF_YEAR, -1); return dayKey(c); }
    public static int weekday() { return clock.get().get(Calendar.DAY_OF_WEEK) - 1; }   // 0 = domingo

    // ------------------------------------------------------------------ presente diário (7 dias)
    public static final Prize[] GIFTS = {
        new Prize("🎁", 300, 0, "Poção", 3),
        new Prize("🎁", 0, 0, "Pokébola", 5),
        new Prize("🎁", 500, 0, "Great Ball", 3),
        new Prize("🎁", 0, 2, "Ferro", 5),
        new Prize("🎁", 800, 0, "Fruta", 5),
        new Prize("🎁", 0, 0, "Ultra Ball", 3),
        new Prize("🎉", 1000, 3, "Gema de Boost", 1),
    };

    /** Quantos presentes do ciclo atual já foram pegos (0..7). */
    public static int giftClaimedInCycle(Store s) {
        int last = s.get("gift_last", 0), streak = s.get("gift_streak", 0);
        if (last == today()) return ((streak - 1) % 7) + 1;
        if (last == yesterday()) return streak % 7;
        return 0;
    }

    public static boolean giftReady(Store s) { return s.get("gift_last", 0) != today(); }
    public static int giftStreak(Store s) {
        int last = s.get("gift_last", 0);
        return (last == today() || last == yesterday()) ? s.get("gift_streak", 0) : 0;
    }

    /** Entrega o presente de hoje. Retorna null se já foi pego. */
    public static Prize claimGift(GameView gv, Store s) {
        if (!giftReady(s)) return null;
        int idx = giftClaimedInCycle(s);
        int streak = (s.get("gift_last", 0) == yesterday() ? s.get("gift_streak", 0) : 0) + 1;
        s.set("gift_last", today());
        s.set("gift_streak", streak);
        Prize p = GIFTS[idx % 7];
        p.give(gv);
        return p;
    }

    // ------------------------------------------------------------------ baú do evento (1 por dia)
    private static final Prize[] EVENT_CHEST = {
        new Prize("✨", 300, 0, "Fragmento Shiny", 1),   // domingo
        new Prize("🧠", 300, 0, "Fruta", 5),             // segunda
        new Prize("🪙", 900, 0, null, 0),                // terça
        new Prize("🔴", 0, 0, "Great Ball", 6),          // quarta
        new Prize("👑", 0, 2, "Pedra Fogo", 1),          // quinta
        new Prize("🗼", 0, 1, "Ferro", 8),               // sexta
        new Prize("🏰", 0, 0, "Madeira", 15),            // sábado
    };

    public static Prize eventChest() { return EVENT_CHEST[weekday()]; }
    public static boolean chestReady(Store s) { return s.get("event_chest_day", 0) != today(); }

    public static Prize claimChest(GameView gv, Store s) {
        if (!chestReady(s)) return null;
        s.set("event_chest_day", today());
        Prize p = eventChest();
        p.give(gv);
        return p;
    }

    // ------------------------------------------------------------------ recarga de suprimentos
    public static final int RECHARGE_SECONDS = 6 * 3600, POTION_CAP = 5, BALL_CAP = 10;

    private static int nowSec() { return (int) (clock.get().getTimeInMillis() / 1000L); }

    public static int rechargeSecondsLeft(Store s) {
        int last = s.get("recharge_last", 0);
        if (last == 0) return 0;
        return Math.max(0, last + RECHARGE_SECONDS - nowSec());
    }

    public static boolean rechargeReady(Store s) { return rechargeSecondsLeft(s) == 0; }

    /** Cura o time e completa Poções (até 5) e Pokébolas (até 10). Nunca tira nada. Retorna o texto do resultado ou null. */
    public static String claimRecharge(GameView gv, Store s) {
        if (!rechargeReady(s)) return null;
        s.set("recharge_last", nowSec());
        PartyManager.healAll(gv);
        int potion = Math.max(0, POTION_CAP - gv.player.getInventory().getOrDefault("Poção", 0));
        int ball = Math.max(0, BALL_CAP - gv.player.getInventory().getOrDefault("Pokébola", 0));
        if (potion > 0) gv.player.addItem("Poção", potion);
        if (ball > 0) gv.player.addItem("Pokébola", ball);
        StringBuilder sb = new StringBuilder("🔋 Recarga! Time curado");
        if (potion > 0) sb.append("  +").append(potion).append(" Poção");
        if (ball > 0) sb.append("  +").append(ball).append(" Pokébola");
        return sb.toString();
    }

    public static String countdown(int seconds) {
        if (seconds >= 3600) return (seconds / 3600) + "h" + String.format("%02d", (seconds % 3600) / 60);
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }

    // ------------------------------------------------------------------ resumo para a barra
    /** Quantas coisas há para reivindicar agora (usa só contadores em cache: seguro para chamar todo quadro). */
    public static int pendingCount(GameView gv, Store s) {
        int n = 0;
        if (giftReady(s)) n++;
        if (chestReady(s)) n++;
        if (rechargeReady(s)) n++;
        n += gv.missions.readyJourneyCount() + gv.missions.readyDailyCount() + gv.achievements.readyCount();
        return n;
    }

    /** Pega tudo o que estiver pronto. Retorna quantos itens foram reivindicados. */
    public static int claimAll(GameView gv, Store s) {
        int n = 0;
        Prize g = claimGift(gv, s);
        if (g != null) { n++; gv.ui.showMessage("🎁 Presente diário: " + g.text()); }
        Prize c = claimChest(gv, s);
        if (c != null) { n++; gv.ui.showMessage(c.icon + " Baú do evento: " + c.text()); }
        String r = claimRecharge(gv, s);
        if (r != null) { n++; gv.ui.showMessage(r); }
        n += gv.missions.claimAllQuests(gv);
        n += gv.missions.claimAllDailies(gv);
        n += gv.achievements.claimAll(gv);
        if (n > 0) gv.activity.saveCurrentGame();
        return n;
    }
}
