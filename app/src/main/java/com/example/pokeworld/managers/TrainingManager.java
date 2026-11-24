package com.example.pokeworld.managers;

import android.content.SharedPreferences;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.entities.Progression;
import com.example.pokeworld.world.BaseKind;
import com.example.pokeworld.world.BasePiece;

import java.util.ArrayList;
import java.util.List;

/** Treino na Academia: Pokémon da Caixa/Defesa treinam em tempo real (até com o jogo fechado) e voltam com XP. */
public final class TrainingManager {
    private TrainingManager() {}

    public static final int MINUTES = 5;

    private static SharedPreferences prefs(GameView gv) { return gv.activity.getSharedPreferences("training", 0); }

    /** Nível da Academia pronta (0 = não construída). Cada nível libera 1 vaga. */
    public static int academyLevel(GameView gv) {
        int lv = 0;
        for (BasePiece p : gv.activity.dbHelper.getBasePieces()) if (p.kind == BaseKind.ACADEMIA && p.isUsable()) lv = Math.max(lv, p.level);
        return lv;
    }

    public static int slots(GameView gv) { return Math.min(5, academyLevel(gv)); }

    public static String slotSpecies(GameView gv, int i) { String v = prefs(gv).getString("s" + i, ""); return v.isEmpty() ? null : v; }
    public static long slotEnd(GameView gv, int i) { return prefs(gv).getLong("e" + i, 0); }
    public static int secondsLeft(GameView gv, int i) { return (int) Math.max(0, (slotEnd(gv, i) - System.currentTimeMillis()) / 1000); }

    public static int cost(int level) { return 40 * Math.max(1, level); }

    /** Pokémon que podem treinar: fora do time e que não estão em outra vaga (uma entrada por espécie). */
    public static List<CapturedCreature> candidates(GameView gv) {
        List<CapturedCreature> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (CapturedCreature c : gv.player.getCreatureCollection()) {
            if (c.isInParty || !seen.add(c.name.toLowerCase())) continue;
            boolean busy = false;
            for (int i = 0; i < 5; i++) if (c.name.equalsIgnoreCase(slotSpecies(gv, i))) busy = true;
            if (!busy) out.add(c);
        }
        return out;
    }

    private static int levelOf(GameView gv, String species) {
        PetData d = gv.activity.dbHelper.getPetData(species);
        return d != null ? Math.max(1, d.level) : 1;
    }

    public static int cost(GameView gv, CapturedCreature c) { return cost(levelOf(gv, c.name)); }

    /** XP que o treino rende: mais para quem tem Academia melhor. */
    public static int reward(GameView gv, String species) {
        int lv = levelOf(gv, species);
        return (int) (Progression.expToNext(lv) * (0.5 + 0.1 * academyLevel(gv))) + 15 * academyLevel(gv);
    }

    public static String start(GameView gv, CapturedCreature c) {
        int n = slots(gv);
        if (n == 0) return "Construa a Academia na base para treinar.";
        int free = -1;
        for (int i = 0; i < n; i++) if (slotSpecies(gv, i) == null) { free = i; break; }
        if (free < 0) return "Todas as vagas de treino estão ocupadas.";
        int cost = cost(gv, c);
        if (gv.player.getGold() < cost) return "Ouro insuficiente (" + cost + " 🪙).";
        gv.player.addGold(-cost);
        prefs(gv).edit().putString("s" + free, c.name).putLong("e" + free, System.currentTimeMillis() + MINUTES * 60_000L).apply();
        SoundManager.play("click");
        return "🏋️ " + c.name + " começou o treino! Volta em " + MINUTES + " min (pode fechar o jogo).";
    }

    public static String collect(GameView gv, int i) {
        String sp = slotSpecies(gv, i);
        if (sp == null) return "Vaga vazia.";
        if (secondsLeft(gv, i) > 0) return "O treino ainda não acabou.";
        PetData d = gv.activity.dbHelper.getPetData(sp);
        int xp = reward(gv, sp);
        int level = d.level, exp = d.exp + xp, ups = 0;
        while (exp >= Progression.expToNext(level) && level < 100) { exp -= Progression.expToNext(level); level++; ups++; }
        gv.activity.dbHelper.savePet(sp, level, exp, d.forgeBonus, d.weapon, d.armor, d.isShiny, d.personality, d.rune, Math.min(255, d.friendship + 5), d.type2, d.rarity, d.gender);
        prefs(gv).edit().remove("s" + i).remove("e" + i).apply();
        gv.missions.count(MissionManager.Metric.TRAININGS);
        SoundManager.play(ups > 0 ? "levelup" : "capture");
        gv.activity.saveCurrentGame();
        return "💪 " + sp + " treinou: +" + xp + " XP" + (ups > 0 ? "  ⬆ Nível " + level + "!" : "") + "  +5 💗";
    }
}
