package com.example.pokeworld.managers;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.PetData;

import java.util.List;
import java.util.Random;

/**
 * Expedições (Pokémon da Caixa/Defesa viajam e voltam com recursos, mesmo com o jogo fechado) e ataques às bases rivais
 * (geradas todo dia; resolvidos pelo poder do seu time). Tudo guardado na tabela kv.
 */
public final class ExpeditionManager {
    private ExpeditionManager() {}

    public static final class Dest {
        public final String name, icon; public final int minutes, gold; public final String[] items;
        Dest(String n, String i, int min, int gold, String... items) { name = n; icon = i; minutes = min; this.gold = gold; this.items = items; }
    }

    public static final Dest[] DESTS = {
        new Dest("Floresta Antiga", "🌲", 10, 250, "Madeira", "Fruta", "Pelo"),
        new Dest("Caverna Profunda", "⛏️", 30, 800, "Ferro", "Pedra", "Carvão", "Núcleo de Rocha"),
        new Dest("Ruínas Perdidas", "🏛️", 60, 2000, "Pedra Fogo", "Pedra Água", "Pedra Trovão", "Pedra Folha", "Poeira Mística"),
    };

    /** Os melhores Pokémon fora do time (Caixa + Defesa), até 3: formam a equipe da expedição. */
    public static int squadPower(GameView gv) {
        double[] best = {0, 0, 0};
        for (CapturedCreature c : gv.player.getCreatureCollection()) {
            if (c.isInParty) continue;
            double v = strength(gv, c);
            for (int i = 0; i < 3; i++) if (v > best[i]) { double t = best[i]; best[i] = v; v = t; }
        }
        return (int) Math.round(best[0] + best[1] + best[2]);
    }

    public static int squadSize(GameView gv) {
        int n = 0;
        for (CapturedCreature c : gv.player.getCreatureCollection()) if (!c.isInParty) n++;
        return Math.min(3, n);
    }

    /** Força de um Pokémon: nível x raridade x boost. */
    public static double strength(GameView gv, CapturedCreature c) {
        int lv = com.example.pokeworld.managers.PartyManager.isActive(gv, c) ? gv.companion.getLevel() : levelOf(gv, c.name);
        return lv * (1.0 + 0.25 * c.rarity.ordinal()) * (1.0 + 0.08 * c.boost);
    }

    private static int levelOf(GameView gv, String species) {
        PetData d = gv.activity.dbHelper.getPetData(species);
        return d != null ? Math.max(1, d.level) : 1;
    }

    /** Poder do time de 6 (para atacar bases rivais). */
    public static int partyPower(GameView gv) {
        double sum = 0;
        for (CapturedCreature c : gv.player.getParty()) sum += strength(gv, c);
        return (int) Math.round(sum);
    }

    private static int partyAvgLevel(GameView gv) {
        int n = 0, sum = 0;
        for (CapturedCreature c : gv.player.getParty()) { n++; sum += com.example.pokeworld.managers.PartyManager.isActive(gv, c) ? gv.companion.getLevel() : levelOf(gv, c.name); }
        return n == 0 ? 1 : Math.max(1, sum / n);
    }

    private static int nowSec() { return (int) (System.currentTimeMillis() / 1000L); }

    // ----------------------------------------------------------------- expedição

    public static int activeDest(GameView gv) { return gv.activity.dbHelper.getKv("exp_dest", 0) - 1; }   // -1 = nenhuma
    public static int secondsLeft(GameView gv) { return Math.max(0, gv.activity.dbHelper.getKv("exp_end", 0) - nowSec()); }
    public static boolean ready(GameView gv) { return activeDest(gv) >= 0 && secondsLeft(gv) == 0; }

    public static String start(GameView gv, int dest) {
        if (activeDest(gv) >= 0) return "Já existe uma expedição em andamento.";
        if (squadSize(gv) == 0) return "Precisa de Pokémon fora do time (na Caixa ou na Defesa) para enviar.";
        int power = squadPower(gv);
        gv.activity.dbHelper.setKv("exp_dest", dest + 1);
        gv.activity.dbHelper.setKv("exp_end", nowSec() + DESTS[dest].minutes * 60);
        gv.activity.dbHelper.setKv("exp_power", power);
        SoundManager.play("click");
        return DESTS[dest].icon + " Expedição enviada para " + DESTS[dest].name + " (poder " + power + "). Volta em " + DESTS[dest].minutes + " min — pode fechar o jogo!";
    }

    public static String collect(GameView gv) {
        int d = activeDest(gv);
        if (d < 0) return "Nenhuma expedição.";
        if (secondsLeft(gv) > 0) return "A equipe ainda não voltou.";
        Dest dest = DESTS[d];
        int power = gv.activity.dbHelper.getKv("exp_power", 10);
        Random r = new Random();
        double mult = 0.6 + Math.min(2.4, power / 60.0);          // equipe forte traz mais
        int gold = (int) Math.round(dest.gold * mult * (0.9 + r.nextDouble() * 0.2));
        gv.player.addGold(gold);
        StringBuilder sb = new StringBuilder("🎒 EXPEDIÇÃO CONCLUÍDA! +" + gold + "🪙");
        int rolls = 2 + (int) Math.round(mult);
        for (int i = 0; i < rolls; i++) {
            String it = dest.items[r.nextInt(dest.items.length)];
            int q = 1 + r.nextInt(2 + d);
            gv.player.addItem(it, q);
            sb.append(" +").append(q).append(" ").append(it);
        }
        if (r.nextDouble() < 0.10 + 0.12 * d) { gv.player.addItem("Gema de Boost", 1); sb.append("  💎 +1 Gema de Boost!"); }
        gv.activity.dbHelper.setKv("exp_dest", 0);
        gv.activity.dbHelper.setKv("exp_end", 0);
        gv.missions.count(MissionManager.Metric.EXPEDITIONS);
        SoundManager.play("capture");
        gv.activity.saveCurrentGame();
        return sb.toString();
    }

    // ----------------------------------------------------------------- bases rivais (3 por dia)

    public static final String[] RIVAL_NAMES = {"Treinador Ash", "Equipe Rocket", "Líder Brock", "Capitã Misty", "Mestre Oak", "Cavaleiro Red", "Dama Erika", "Doutor Fuji", "Campeão Blue", "Ninja Koga", "Rainha Sabrina", "Capitão Surge"};
    public static final String[] TIER = {"Fácil", "Média", "Difícil"};
    private static final double[] TIER_MULT = {0.65, 1.0, 1.5};

    private static int day() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return c.get(java.util.Calendar.YEAR) * 1000 + c.get(java.util.Calendar.DAY_OF_YEAR);
    }

    public static String rivalName(int i) {
        int d = day();
        return RIVAL_NAMES[(d * 5 + i * 4 + (d / 7)) % RIVAL_NAMES.length];
    }

    public static int rivalPower(GameView gv, int i) {
        return (int) Math.round((20 + 5.5 * partyAvgLevel(gv)) * TIER_MULT[i]);
    }

    public static int winChance(GameView gv, int i) {
        double ratio = partyPower(gv) / (double) Math.max(1, rivalPower(gv, i));
        return (int) Math.round(Math.max(0.05, Math.min(0.95, 0.5 + (ratio - 1.0) * 0.9)) * 100);
    }

    /** 0 = não atacada hoje, 1 = vencida, 2 = derrota (bloqueia o resto do dia). */
    public static int rivalState(GameView gv, int i) {
        if (gv.activity.dbHelper.getKv("rival_day", 0) != day()) return 0;
        return gv.activity.dbHelper.getKv("rival_" + i, 0);
    }

    public static String attack(GameView gv, int i) {
        int d = day();
        if (gv.activity.dbHelper.getKv("rival_day", 0) != d) {
            gv.activity.dbHelper.setKv("rival_day", d);
            for (int k = 0; k < 3; k++) gv.activity.dbHelper.setKv("rival_" + k, 0);
        }
        if (rivalState(gv, i) != 0) return "Você já atacou essa base hoje.";
        if (gv.player.getParty().isEmpty()) return "Leve Pokémon no time para atacar!";
        int chance = winChance(gv, i);
        boolean win = new Random().nextInt(100) < chance;
        gv.activity.dbHelper.setKv("rival_" + i, win ? 1 : 2);
        if (!win) {
            SoundManager.play("faint");
            VibrationManager.tap(200);
            return "💥 Seu time foi derrotado na base de " + rivalName(i) + "! Fortaleça o time (boost, fusão, evolução) e tente amanhã.";
        }
        int power = rivalPower(gv, i);
        int gold = power * (8 + 4 * i);
        gv.player.addGold(gold);
        StringBuilder sb = new StringBuilder("🏴 BASE DE " + rivalName(i).toUpperCase() + " SAQUEADA! +" + gold + "🪙");
        Random r = new Random();
        String[] loot = {"Ferro", "Madeira", "Pokébola", "Great Ball", "Pedra"};
        int q = 2 + r.nextInt(3) + i * 2;
        String it = loot[r.nextInt(loot.length)];
        gv.player.addItem(it, q);
        sb.append(" +").append(q).append(" ").append(it);
        if (r.nextDouble() < 0.20 + 0.20 * i) { gv.player.addItem("Gema de Boost", 1); sb.append("  💎 +1 Gema de Boost"); }
        if (r.nextDouble() < 0.5) { gv.player.addGems(1 + i); sb.append("  +").append(1 + i).append(" gemas"); }
        gv.missions.count(MissionManager.Metric.RIVAL_WINS);
        SoundManager.play("levelup");
        VibrationManager.tap(80);
        gv.activity.saveCurrentGame();
        return sb.toString();
    }
}
