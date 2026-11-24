package com.example.pokeworld.managers;

import com.example.pokeworld.core.DatabaseHelper;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.EvolutionTable;
import com.example.pokeworld.entities.TalentTree;
import com.example.pokeworld.ui.UIManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Conquistas com progresso (atual/meta) e recompensa. As conquistas desbloqueadas ficam salvas no banco. */
public class AchievementManager {
    public enum Metric { COLLECTION, GOLD, COMPANION_LEVEL, SPECIES_SEEN, SPECIES_CAPTURED, TOWER_FLOOR, BASE_PIECES, EVOLVED, SHINY, TALENT_POINTS, RUNE, PLAYER_LEVEL }

    public static final class Def {
        public final String title, desc, icon, group;
        public final Metric metric;
        public final int target, rewardGold, rewardGems;
        Def(String group, String title, String desc, String icon, Metric m, int target, int gold, int gems) {
            this.group = group; this.title = title; this.desc = desc; this.icon = icon; this.metric = m; this.target = target; this.rewardGold = Economy.scale(gold, Economy.REWARD); this.rewardGems = gems;
        }
        public String rewardText() {
            return (rewardGold > 0 ? "🪙 " + rewardGold : "") + (rewardGems > 0 ? (rewardGold > 0 ? "  " : "") + "💎 " + rewardGems : "");
        }
    }

    public static final List<Def> ALL = new ArrayList<>();
    private static void d(String g, String t, String desc, String icon, Metric m, int target, int gold, int gems) { ALL.add(new Def(g, t, desc, icon, m, target, gold, gems)); }

    static {
        // Os 4 títulos antigos foram mantidos para não perder o que já estava registrado.
        d("Coleção", "Colecionador Iniciante", "Tenha 3 Pokémon na coleção", "🎒", Metric.COLLECTION, 3, 200, 0);
        d("Coleção", "Colecionador", "Tenha 10 Pokémon na coleção", "📦", Metric.COLLECTION, 10, 600, 1);
        d("Coleção", "Mestre Colecionador", "Tenha 25 Pokémon na coleção", "🏛️", Metric.COLLECTION, 25, 2000, 3);
        d("Coleção", "Brilho Raro", "Tenha um Pokémon shiny", "✨", Metric.SHINY, 1, 1000, 2);
        d("Treino", "Mestre Treinador", "Leve um Pokémon ao nível 10", "💪", Metric.COMPANION_LEVEL, 10, 300, 0);
        d("Treino", "Campeão", "Leve um Pokémon ao nível 30", "🥇", Metric.COMPANION_LEVEL, 30, 1500, 2);
        d("Treino", "Lenda Viva", "Leve um Pokémon ao nível 50", "👑", Metric.COMPANION_LEVEL, 50, 5000, 5);
        d("Treino", "Primeira Evolução", "Evolua um Pokémon", "🧬", Metric.EVOLVED, 1, 500, 1);
        d("Treino", "Treinador Veterano", "Chegue ao nível 15 de treinador", "🎖️", Metric.PLAYER_LEVEL, 15, 1000, 2);
        d("Pokédex", "Explorador", "Descubra 10 espécies diferentes", "🧭", Metric.SPECIES_SEEN, 10, 300, 0);
        d("Pokédex", "Naturalista", "Descubra 40 espécies diferentes", "🔭", Metric.SPECIES_SEEN, 40, 1500, 2);
        d("Pokédex", "Pesquisador", "Descubra 80 espécies diferentes", "📚", Metric.SPECIES_SEEN, 80, 4000, 4);
        d("Pokédex", "Domador", "Capture 5 espécies diferentes", "🔴", Metric.SPECIES_CAPTURED, 5, 400, 0);
        d("Pokédex", "Domador de Elite", "Capture 25 espécies diferentes", "🏅", Metric.SPECIES_CAPTURED, 25, 2500, 3);
        d("Riqueza", "Magnata", "Acumule 1.000 de ouro", "💰", Metric.GOLD, 1000, 300, 0);
        d("Riqueza", "Milionário", "Acumule 10.000 de ouro", "💎", Metric.GOLD, 10000, 0, 5);
        d("Torre", "Escalador", "Conclua o andar 5 da Torre", "🏰", Metric.TOWER_FLOOR, 5, 800, 1);
        d("Torre", "Conquistador da Torre", "Conclua o andar 10 da Torre", "⚔️", Metric.TOWER_FLOOR, 10, 2500, 3);
        d("Torre", "Mestre da Torre", "Conclua o andar 20 da Torre", "🏆", Metric.TOWER_FLOOR, 20, 8000, 8);
        d("Base", "Construtor", "Tenha 10 peças na sua base", "🔨", Metric.BASE_PIECES, 10, 400, 0);
        d("Base", "Fortaleza", "Tenha 40 peças na sua base", "🏯", Metric.BASE_PIECES, 40, 2000, 2);
        d("Talentos", "Aprendiz", "Gaste 5 pontos de talento", "🌱", Metric.TALENT_POINTS, 5, 300, 0);
        d("Talentos", "Especialista", "Gaste 20 pontos de talento", "🌳", Metric.TALENT_POINTS, 20, 2000, 3);
        d("Talentos", "Portador de Runa", "Equipe uma runa", "🔷", Metric.RUNE, 1, 500, 1);
    }

    private final UIManager ui;
    private final DatabaseHelper dbHelper;
    private final Set<String> unlockedAchievements = new HashSet<>();
    private long lastCheck = 0;
    private long lastBaseQuery = 0;
    private int cachedBasePieces = 0;

    public AchievementManager(UIManager ui, DatabaseHelper dbHelper) {
        this.ui = ui;
        this.dbHelper = dbHelper;
        unlockedAchievements.addAll(dbHelper.getUnlockedAchievements());
    }

    public int progress(Def d, GameView gv) {
        switch (d.metric) {
            case COLLECTION: return gv.player.getCreatureCollection().size();
            case GOLD: return gv.player.getGold();
            case COMPANION_LEVEL: return gv.companion.getLevel();
            case PLAYER_LEVEL: return gv.player.getLevel();
            case SPECIES_SEEN: return gv.bestiary.seenCount();
            case SPECIES_CAPTURED: return gv.bestiary.capturedCount();
            case TOWER_FLOOR: return gv.tower.getHighestCleared();
            case TALENT_POINTS: return TalentTree.spentPoints(gv.player);
            case RUNE: return gv.companion.getRune() != null ? 1 : 0;
            case SHINY: {
                int n = 0;
                for (CapturedCreature c : gv.player.getCreatureCollection()) if (c.isShiny) n++;
                return n;
            }
            case EVOLVED: {
                int n = 0;
                for (CapturedCreature c : gv.player.getCreatureCollection()) if (EvolutionTable.preEvolution(c.name) != null) n++;
                return n;
            }
            case BASE_PIECES: {
                long now = System.currentTimeMillis();
                if (now - lastBaseQuery > 3000) { lastBaseQuery = now; cachedBasePieces = dbHelper.getBasePieces().size(); }
                return cachedBasePieces;
            }
            default: return 0;
        }
    }

    /** Verifica as conquistas (no máximo a cada 2s) e entrega as recompensas das novas. */
    public void check(GameView gv) {
        long now = System.currentTimeMillis();
        if (now - lastCheck < 2000) return;
        lastCheck = now;
        for (Def d : ALL) {
            if (unlockedAchievements.contains(d.title)) continue;
            if (progress(d, gv) >= d.target) unlock(d, gv);
        }
    }

    private void unlock(Def d, GameView gv) {
        unlockedAchievements.add(d.title);
        dbHelper.saveAchievement(d.title);
        if (d.rewardGold > 0) gv.player.addGold(d.rewardGold);
        if (d.rewardGems > 0) gv.player.addGems(d.rewardGems);
        ui.showMessage("🏆 CONQUISTA: " + d.title + "  " + d.rewardText());
        gv.activity.saveCurrentGame();
    }

    public boolean isUnlocked(Def d) { return unlockedAchievements.contains(d.title); }
    public int unlockedCount() {
        int n = 0;
        for (Def d : ALL) if (unlockedAchievements.contains(d.title)) n++;
        return n;
    }
    public Set<String> getUnlockedAchievements() { return unlockedAchievements; }
}
