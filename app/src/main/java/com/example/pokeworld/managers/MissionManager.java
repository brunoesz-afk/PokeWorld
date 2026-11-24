package com.example.pokeworld.managers;

import com.example.pokeworld.core.DatabaseHelper;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.EvolutionTable;
import com.example.pokeworld.entities.NPC;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.entities.TalentTree;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.world.BaseKind;
import com.example.pokeworld.world.BasePiece;

import java.util.ArrayList;
import java.util.List;

/**
 * Jornada do Treinador: missões em capítulos (atos) que guiam o jogador pelo jogo.
 * O progresso vem de contadores salvos e do estado do jogo; as recompensas são entregues sozinhas ao cumprir.
 * Os atos seguintes só aparecem depois que o ato atual termina.
 */
public class MissionManager {
    public enum Metric {
        KILLS, BOSS_KILLS, CAPTURES, FISH, HARVESTS, RAIDS_WON, STONES_USED,                 // contadores salvos
        SPECIES_CAPTURED, SPECIES_SEEN, COMPANION_LEVEL, PLAYER_LEVEL, TOWER, TALENTS, RUNE, // estado do jogo
        EVOLVED, SHINY, DEFENDERS, HQ_LEVEL, BUILT, BASE_PIECES,
        BOOSTS, FUSIONS, TOWER_RUNS, EXPEDITIONS, RIVAL_WINS, ENDLESS_BEST                                                           // contadores novos
    }

    public static final class Quest {
        public final String id, title, desc, hint, rewardItem;
        public final int act, target, gold, gems, itemQty;
        public final Metric metric;
        public final String arg;
        Quest(int act, String id, String title, String desc, String hint, Metric m, String arg, int target, int gold, int gems, String item, int qty) {
            this.act = act; this.id = id; this.title = title; this.desc = desc; this.hint = hint; this.metric = m; this.arg = arg;
            this.target = target; this.gold = gold; this.gems = gems; this.rewardItem = item; this.itemQty = qty;
        }
        public String rewardText() {
            StringBuilder sb = new StringBuilder();
            if (gold > 0) sb.append("🪙 ").append(gold);
            if (gems > 0) sb.append(sb.length() > 0 ? "  " : "").append("💎 ").append(gems);
            if (rewardItem != null) sb.append(sb.length() > 0 ? "  " : "").append(itemQty).append("x ").append(rewardItem);
            return sb.toString();
        }
    }

    public static final String[] ACT_NAMES = {"", "Ato I — Primeiros Passos", "Ato II — A Expansão do Domínio", "Ato III — Torre, Talentos e Runas", "Ato IV — Mestres e Feras", "Ato V — Poder Máximo", "Ato VI — Lenda Viva"};
    public static final List<Quest> QUESTS = new ArrayList<>();

    private static void q(int act, String id, String title, String desc, String hint, Metric m, String arg, int target, int gold, int gems, String item, int qty) {
        QUESTS.add(new Quest(act, id, title, desc, hint, m, arg, target, gold, gems, item, qty));
    }

    static {
        // Ato I
        q(1, "despertar", "O Despertar", "Derrote 5 Pokémon selvagens", "Toque na Bússola 🧭 e vá para a Floresta. Toque no inimigo para atacar.", Metric.KILLS, null, 5, 150, 0, "Poção", 2);
        q(1, "captura", "Treinador de Verdade", "Capture 3 Pokémon", "Derrotou? Arraste a Pokébola até o corpo do Pokémon. Quanto mais fraco ele for, maior a chance.", Metric.CAPTURES, null, 3, 200, 0, "Pokébola", 3);
        q(1, "oficina", "O Chamado da Base", "Construa a Oficina na sua Base", "Na base, toque em 🔨, escolha a Oficina e posicione.", Metric.BUILT, "OFICINA", 1, 300, 0, "Madeira", 10);
        q(1, "pesca", "Mestre dos Mares", "Pesque 1 peixe", "Vá até um lago (Bússola 🧭) e use a vara de pescar perto da água.", Metric.FISH, null, 1, 120, 0, "Fruta", 3);
        // Ato II
        q(2, "evolucao", "Primeira Evolução", "Evolua um Pokémon", "Suba de nível (Charmander chega ao 16) e toque em EVOLUIR na ficha do Time, ou use uma Pedra de Evolução.", Metric.EVOLVED, null, 1, 500, 1, "Pedra Água", 1);
        q(2, "horta", "Mãos na Terra", "Colha uma Horta da base", "Construa uma Horta 🌱 na base, toque nela para plantar e depois para colher.", Metric.HARVESTS, null, 1, 250, 0, "Pokébola", 5);
        q(2, "defesa", "Guardiões do Reino", "Escale 2 Pokémon na defesa da base", "No Prof. de Time (Quartel), mande Pokémon da Caixa para a Defesa.", Metric.DEFENDERS, null, 2, 400, 0, "Ferro", 5);
        q(2, "colecao", "Colecionador de Espécies", "Capture 8 espécies diferentes", "Explore mapas diferentes: cada mapa tem espécies próprias.", Metric.SPECIES_CAPTURED, null, 8, 600, 0, "Pokébola", 5);
        q(2, "invasao", "Base Intocável", "Repele uma invasão na base", "Fique na base quando o aviso de invasão aparecer. Torres e defensores ajudam.", Metric.RAIDS_WON, null, 1, 700, 1, "Ferro", 8);
        // Ato III
        q(3, "torre3", "Primeiros Andares", "Conclua o andar 3 da Torre", "Fale com o Mestre da Torre no Quartel. Cada andar tem 3 hordas.", Metric.TOWER, null, 3, 800, 0, "Pedra Trovão", 1);
        q(3, "talentos", "Árvore de Talentos", "Gaste 3 pontos de talento", "Toque no ícone de talentos. Nós novos liberam quando o pai sobe de nível.", Metric.TALENTS, null, 3, 500, 0, "Poção", 3);
        q(3, "runa", "Poder Antigo", "Equipe uma runa", "Runas caem de chefes e da Torre (andar 5). Equipe no menu de Runas.", Metric.RUNE, null, 1, 600, 1, "Pedra Folha", 1);
        q(3, "pedra", "Pedra de Evolução", "Use uma Pedra de Evolução", "Abra a Mochila, escolha a pedra e depois quem vai evoluir.", Metric.STONES_USED, null, 1, 600, 0, "Pedra Fogo", 1);
        q(3, "nivel10", "Pokémon Forte", "Leve um Pokémon ao nível 10", "Lute em mapas do seu nível: inimigos muito fracos dão pouco XP.", Metric.COMPANION_LEVEL, null, 10, 900, 1, "Poção", 5);
        // Ato IV
        q(4, "chefe", "Caçador de Chefes", "Derrote um chefe", "Chefes ficam em pontos fixos: Vulcão, Montanha Nevada e Santuário.", Metric.BOSS_KILLS, null, 1, 1200, 2, "Pokébola", 8);
        q(4, "torre10", "Subindo a Torre", "Conclua o andar 10 da Torre", "Evolua seu time e equipe runas antes de subir.", Metric.TOWER, null, 10, 2000, 2, "Pedra Água", 1);
        q(4, "quartel3", "Reino Crescendo", "Suba o Quartel para o nível 3", "Quartel maior = mais defensores e mais construções permitidas.", Metric.HQ_LEVEL, null, 3, 1500, 1, "Ferro", 15);
        q(4, "pokedex", "Pesquisador", "Descubra 40 espécies na Pokédex", "Basta chegar perto de um Pokémon para registrá-lo.", Metric.SPECIES_SEEN, null, 40, 1800, 2, "Pokébola", 10);
        q(4, "brilho", "Brilho Raro", "Tenha um Pokémon shiny", "Shinies aparecem raramente entre os selvagens (e nos chefes especiais).", Metric.SHINY, null, 1, 2500, 3, null, 0);
        q(4, "nivel30", "Campeão", "Leve um Pokémon ao nível 30", "Evolua o time e use Fruta/Runas para aguentar as zonas finais.", Metric.COMPANION_LEVEL, null, 30, 4000, 3, "Runa da Sorte", 1);
        q(4, "torre20", "Mestre da Torre", "Conclua o andar 20 da Torre", "O desafio final: 20 andares.", Metric.TOWER, null, 20, 8000, 8, "Runa de Zeus", 1);
    }

    static {
        // Ato V — boost, fusão e time forte
        q(5, "boost1", "Gema Brilhante", "Suba o boost de um Pokémon (Gema de Boost)", "Ganhe gemas nos andares 5/10/15/20 da Torre e em chefes. Use na ficha do Pokémon.", Metric.BOOSTS, null, 1, 3000, 2, "Gema de Boost", 1);
        q(5, "fusao", "4 em 1", "Funda 4 Pokémon iguais em uma raridade maior", "Time ➜ ficha do Pokémon ➜ FUNDIR. Precisa de 4 da mesma espécie e raridade (sem boost).", Metric.FUSIONS, null, 1, 3500, 2, "Pokébola", 10);
        q(5, "exped", "Aventureiros", "Complete 1 expedição", "Fale com o Capitão de Expedição no Quartel. Pokémon da Caixa/Defesa viajam e voltam com recursos.", Metric.EXPEDITIONS, null, 1, 3500, 2, "Pedra Folha", 1);
        q(5, "evo5", "Linhagem Forte", "Tenha 5 Pokémon evoluídos", "Evolua por nível ou pedra pela ficha do Pokémon.", Metric.EVOLVED, null, 5, 4000, 2, "Pedra Trovão", 1);
        q(5, "defensores4", "Muralha Viva", "Escale 4 defensores na base", "Quanto maior o Quartel, mais defensores você pode escalar.", Metric.DEFENDERS, null, 4, 4500, 2, "Runa de Ferro", 1);
        q(5, "invasao3", "Rei da Defesa", "Repele 3 invasões na base", "Deixe bons defensores na base e esteja lá quando o alarme tocar.", Metric.RAIDS_WON, null, 3, 5000, 3, "Gema de Boost", 1);
        q(5, "quartel5", "Capital do Reino", "Suba o Quartel para o nível 5", "É o nível máximo do Quartel.", Metric.HQ_LEVEL, null, 5, 6000, 3, "Ferro", 30);
        // Ato VI — a lenda
        q(6, "nivel45", "Além do Limite", "Leve um Pokémon ao nível 45", "Combine boost, runa e time completo para encarar as zonas finais.", Metric.COMPANION_LEVEL, null, 45, 9000, 5, "Gema de Boost", 2);
        q(6, "rival5", "Saqueador de Bases", "Vença 5 bases rivais", "Capitão de Expedição ➜ Bases Rivais. Três bases novas todo dia.", Metric.RIVAL_WINS, null, 5, 9000, 4, "Gema de Boost", 1);
        q(6, "infinito25", "Sem Limites", "Chegue ao andar 25 da Torre (modo infinito)", "Conclua o andar 20 e use o botão ♾️ na tela da Torre. Boss a cada 5 andares.", Metric.ENDLESS_BEST, null, 25, 12000, 6, "Gema de Boost", 2);
        q(6, "boost3", "Poder Condensado", "Faça 3 aumentos de boost", "Cada nível de boost dá +8% de ataque e vida.", Metric.BOOSTS, null, 3, 8000, 4, "Gema de Boost", 1);
        q(6, "especies80", "Enciclopédia Viva", "Descubra 80 espécies na Pokédex", "Explore todos os mapas, de dia e de noite.", Metric.SPECIES_SEEN, null, 80, 10000, 5, "Ultra Ball", 5);
        q(6, "chefes10", "Terror dos Chefes", "Derrote 10 chefes", "Chefes têm pontos fixos de respawn e soltam gemas e pedras raras.", Metric.BOSS_KILLS, null, 10, 12000, 6, "Gema de Boost", 2);
        q(6, "shiny3", "Colecionador de Brilhos", "Tenha 3 Pokémon shiny", "Shinies aparecem raramente entre os selvagens.", Metric.SHINY, null, 3, 15000, 8, null, 0);
    }

    private final Player player;
    private final UIManager ui;
    private DatabaseHelper db;
    private long lastCheck = 0, lastBase = 0;
    private int hqCache = 1, piecesCache = 0;
    private final List<String> builtCache = new ArrayList<>();

    public MissionManager(Player player, UIManager ui) {
        this.player = player;
        this.ui = ui;
    }

    public void giveInitialMission() { /* a Jornada começa sozinha: nada a entregar */ }
    public void updateProgress(String type) { /* mantido por compatibilidade; os contadores agora vêm de count() */ }
    public void checkRewards(NPC npc) { /* recompensas automáticas */ }

    // ---------------------------------------------------------------- contadores

    public void bind(DatabaseHelper dbHelper) { this.db = dbHelper; }

    /** Soma 1 ao contador salvo (KILLS, CAPTURES...). */
    public void count(Metric m) {
        if (db == null) return;
        db.setKv("stat_" + m.name(), db.getKv("stat_" + m.name(), 0) + 1);
    }

    private boolean isDone(Quest q) { return db != null && db.getKv("quest_" + q.id, 0) == 1; }

    // ---------------------------------------------------------------- progresso

    public int progress(Quest q, GameView gv) {
        switch (q.metric) {
            case KILLS: case BOSS_KILLS: case CAPTURES: case FISH: case HARVESTS: case RAIDS_WON: case STONES_USED: case BOOSTS: case FUSIONS: case TOWER_RUNS: case EXPEDITIONS: case RIVAL_WINS:
                return db.getKv("stat_" + q.metric.name(), 0);
            case SPECIES_CAPTURED: return gv.bestiary.capturedCount();
            case SPECIES_SEEN: return gv.bestiary.seenCount();
            case COMPANION_LEVEL: return gv.companion.getLevel();
            case PLAYER_LEVEL: return gv.player.getLevel();
            case ENDLESS_BEST: return gv.tower.getEndlessBest();
            case TOWER: return gv.tower.getHighestCleared();
            case TALENTS: return TalentTree.spentPoints(gv.player);
            case RUNE: return gv.companion.getRune() != null ? 1 : 0;
            case DEFENDERS: return gv.player.getDefenders().size();
            case EVOLVED: {
                int n = 0;
                for (CapturedCreature c : gv.player.getCreatureCollection()) if (EvolutionTable.preEvolution(c.name) != null) n++;
                return n;
            }
            case SHINY: {
                int n = 0;
                for (CapturedCreature c : gv.player.getCreatureCollection()) if (c.isShiny) n++;
                return n;
            }
            case HQ_LEVEL: refreshBase(); return hqCache;
            case BASE_PIECES: refreshBase(); return piecesCache;
            case BUILT: refreshBase(); return builtCache.contains(q.arg) ? 1 : 0;
            default: return 0;
        }
    }

    private void refreshBase() {
        long now = System.currentTimeMillis();
        if (now - lastBase < 3000) return;
        lastBase = now;
        hqCache = 1; piecesCache = 0; builtCache.clear();
        for (BasePiece p : db.getBasePieces()) {
            if (!p.isUsable()) continue;
            piecesCache++;
            if (p.kind == BaseKind.QUARTEL) hqCache = Math.max(hqCache, p.level);
            builtCache.add(p.kind.name());
        }
    }

    /** Ato em andamento (o primeiro com missão pendente); 0 quando a Jornada terminou. */
    public int currentAct() {
        for (Quest q : QUESTS) if (!isDone(q)) return q.act;
        return 0;
    }

    /** Missões pendentes do ato atual. */
    public List<Quest> activeQuests() {
        List<Quest> out = new ArrayList<>();
        int act = currentAct();
        for (Quest q : QUESTS) if (q.act == act && !isDone(q)) out.add(q);
        return out;
    }

    public List<Quest> questsOfAct(int act) {
        List<Quest> out = new ArrayList<>();
        for (Quest q : QUESTS) if (q.act == act) out.add(q);
        return out;
    }

    public static int totalQuests() { return QUESTS.size(); }

    public boolean isQuestDone(Quest q) { return isDone(q); }

    public int doneCount() {
        int n = 0;
        for (Quest q : QUESTS) if (isDone(q)) n++;
        return n;
    }

    /** Confere as missões do ato atual (a cada 1,5s) e entrega as recompensas das cumpridas. */
    public void check(GameView gv) {
        if (db == null) db = gv.activity.dbHelper;
        long now = System.currentTimeMillis();
        if (now - lastCheck < 1500) return;
        lastCheck = now;
        checkDailies(gv);
        for (Quest q : activeQuests()) {
            if (progress(q, gv) >= q.target) complete(q, gv);
        }
    }

    private void complete(Quest q, GameView gv) {
        db.setKv("quest_" + q.id, 1);
        if (q.gold > 0) gv.player.addGold(q.gold);
        if (q.gems > 0) gv.player.addGems(q.gems);
        if (q.rewardItem != null) gv.player.addItem(q.rewardItem, q.itemQty);
        ui.showMessage("📜 MISSÃO CUMPRIDA: " + q.title + "  " + q.rewardText());
        if (currentAct() != q.act && currentAct() != 0) {
            ui.showMessage("📖 NOVO CAPÍTULO: " + ACT_NAMES[currentAct()]);
        } else if (currentAct() == 0) {
            ui.showMessage("🏆 JORNADA COMPLETA! Você é um Mestre Pokémon!");
        }
        gv.activity.saveCurrentGame();
    }

    // ---------------------------------------------------------------- missões diárias (3 por dia, renovam à meia-noite)

    public static final class Daily {
        public final String title; public final Metric metric; public final int target, gold; public final String item; public final int qty;
        Daily(String t, Metric m, int target, int gold, String item, int qty) { title = t; metric = m; this.target = target; this.gold = gold; this.item = item; this.qty = qty; }
        public String rewardText() { return "🪙 " + gold + (item != null ? "  " + qty + "x " + item : ""); }
    }

    private static final Daily[] DAILY_POOL = {
        new Daily("Caçada: derrote 25 Pokémon", Metric.KILLS, 25, 500, "Poção", 2),
        new Daily("Caçada pesada: derrote 60 Pokémon", Metric.KILLS, 60, 1100, "Pokébola", 5),
        new Daily("Colecionador: capture 3 Pokémon", Metric.CAPTURES, 3, 600, "Great Ball", 2),
        new Daily("Pescador: pesque 3 peixes", Metric.FISH, 3, 450, "Fruta", 3),
        new Daily("Fazendeiro: colha 3 hortas", Metric.HARVESTS, 3, 450, "Madeira", 8),
        new Daily("Caça-chefes: derrote 1 chefe", Metric.BOSS_KILLS, 1, 1200, "Pedra Fogo", 1),
        new Daily("Defensor: repele 1 invasão", Metric.RAIDS_WON, 1, 900, "Ferro", 6),
        new Daily("Escalador: conclua 1 andar da Torre", Metric.TOWER_RUNS, 1, 900, "Runa de Ferro", 1),
    };

    private String dayKey() {
        java.util.Calendar c = java.util.Calendar.getInstance();
        return c.get(java.util.Calendar.YEAR) + "" + (c.get(java.util.Calendar.DAY_OF_YEAR) + 1000);
    }

    private int dayNum() { return Integer.parseInt(dayKey()); }

    private List<Daily> todayList;
    private int todayNum = -1;

    /** As 3 missões de hoje (mesmas o dia todo; sorteadas pela data). Na virada do dia guarda os contadores-base. */
    public List<Daily> dailies() {
        int d = dayNum();
        if (todayList != null && todayNum == d && db != null) return todayList;
        java.util.Random r = new java.util.Random(d * 7919L);
        List<Daily> pool = new ArrayList<>(java.util.Arrays.asList(DAILY_POOL));
        java.util.Collections.shuffle(pool, r);
        todayList = new ArrayList<>(pool.subList(0, 3));
        todayNum = d;
        if (db == null) return todayList;     // ainda sem banco: tenta de novo no próximo quadro
        if (db != null && db.getKv("daily_day", 0) != d) {
            db.setKv("daily_day", d);
            for (Metric m : Metric.values()) db.setKv("dbase_" + m.name(), db.getKv("stat_" + m.name(), 0));
            for (int i = 0; i < 3; i++) db.setKv("daily_done_" + i, 0);
            db.setKv("daily_bonus", 0);
        }
        return todayList;
    }

    public int dailyProgress(Daily q) {
        if (db == null) return 0;
        return Math.min(q.target, Math.max(0, db.getKv("stat_" + q.metric.name(), 0) - db.getKv("dbase_" + q.metric.name(), 0)));
    }

    public boolean dailyDone(int i) { return db != null && db.getKv("daily_done_" + i, 0) == 1; }
    public boolean dailyBonusTaken() { return db != null && db.getKv("daily_bonus", 0) == 1; }
    public int dailiesDone() { int n = 0; for (int i = 0; i < 3; i++) if (dailyDone(i)) n++; return n; }

    private long lastDaily = 0;
    private void checkDailies(GameView gv) {
        long now = System.currentTimeMillis();
        if (now - lastDaily < 2000) return;
        lastDaily = now;
        List<Daily> ds = dailies();
        for (int i = 0; i < ds.size(); i++) {
            Daily q = ds.get(i);
            if (dailyDone(i) || dailyProgress(q) < q.target) continue;
            db.setKv("daily_done_" + i, 1);
            gv.player.addGold(q.gold);
            if (q.item != null) gv.player.addItem(q.item, q.qty);
            ui.showMessage("📅 DIÁRIA CUMPRIDA: " + q.title + "  " + q.rewardText());
            com.example.pokeworld.managers.SoundManager.play("levelup");
            gv.activity.saveCurrentGame();
        }
        if (dailiesDone() == 3 && !dailyBonusTaken()) {
            db.setKv("daily_bonus", 1);
            gv.player.addGems(2);
            gv.player.addItem("Gema de Boost", 1);
            ui.showMessage("🎁 TODAS AS DIÁRIAS! Bônus: 💎 2 gemas + 1x Gema de Boost");
            gv.activity.saveCurrentGame();
        }
    }

    public void syncNPCs(List<NPC> npcs) {
        for (NPC n : npcs) {
            if (n.getType() == NPC.Type.GUARD) n.setQuestIcon(NPC.QuestIcon.NONE);
        }
    }
}
