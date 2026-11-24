package com.example.pokeworld.managers;

import android.content.Context;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.PetData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Arena: lutadores do dia, pontos de ranking (PR), ligas e o placar. Tudo guardado em kv no banco local.
 * O duelo em si fica em {@link ArenaDuel}.
 */
public final class ArenaLadder {
    private ArenaLadder() {}

    public static final int TRIES_PER_DAY = 8;
    public static final int FIGHTERS = 6;

    // ------------------------------------------------------------------ ligas
    public static final int[] TIER_MIN = {0, 100, 250, 500, 900, 1400, 2000};
    public static final String[] TIER_NAME = {"Bronze", "Prata", "Ouro", "Platina", "Diamante", "Mestre", "Lenda"};
    public static final String[] TIER_ICON = {"🥉", "🥈", "🥇", "💠", "💎", "👑", "🔥"};
    public static final int[] TIER_COLOR = {0xFFCD8B4E, 0xFFC8D0DC, 0xFFFFD24A, 0xFF5CE0D0, 0xFF6AB4FF, 0xFFD08CFF, 0xFFFF7A3C};

    public static int tierOf(int rp) {
        int t = 0;
        for (int i = 0; i < TIER_MIN.length; i++) if (rp >= TIER_MIN[i]) t = i;
        return t;
    }
    /** PR que faltam para a próxima liga (0 se já está na última). */
    public static int toNextTier(int rp) {
        int t = tierOf(rp);
        return t >= TIER_MIN.length - 1 ? 0 : TIER_MIN[t + 1] - rp;
    }

    // ------------------------------------------------------------------ pontos e tentativas
    public static int rp(RewardManager.Store s) { return Math.max(0, s.get("arena_rp", 0)); }
    public static int wins(RewardManager.Store s) { return s.get("arena_w", 0); }
    public static int losses(RewardManager.Store s) { return s.get("arena_l", 0); }
    public static int bestRp(RewardManager.Store s) { return s.get("arena_best", 0); }

    private static void ensureDay(RewardManager.Store s) {
        int d = RewardManager.today();
        if (s.get("arena_day", 0) != d) { s.set("arena_day", d); s.set("arena_used", 0); s.set("arena_bmask", 0); }
    }

    public static int triesLeft(RewardManager.Store s) { ensureDay(s); return Math.max(0, TRIES_PER_DAY - s.get("arena_used", 0)); }

    public static boolean beaten(RewardManager.Store s, int i) { ensureDay(s); return (s.get("arena_bmask", 0) & (1 << i)) != 0; }

    /** Gasta uma tentativa ao apertar PRONTO. Retorna false se acabaram. */
    public static boolean useTry(RewardManager.Store s) {
        ensureDay(s);
        int used = s.get("arena_used", 0);
        if (used >= TRIES_PER_DAY) return false;
        s.set("arena_used", used + 1);
        return true;
    }

    // ------------------------------------------------------------------ lutadores do dia
    public static final class Fighter {
        public int index, level, rpWin, gold;
        public String name, icon;
        public final String[] species = new String[3];
        public final int[] levels = new int[3];
        public int power;                       // calculado com os mesmos números do duelo
    }

    /** Linhas evolutivas: {estágio 1, estágio 2, estágio 3}, trocadas por nível (16 e 36). */
    private static final String[][] LINES = {
            {"Charmander", "Charmeleon", "Charizard"}, {"Squirtle", "Wartortle", "Blastoise"},
            {"Bulbasaur", "Ivysaur", "Venusaur"}, {"Pichu", "Pikachu", "Raichu"},
            {"Growlithe", "Growlithe", "Arcanine"}, {"Houndour", "Houndour", "Houndour"},
            {"Pidgeotto", "Pidgeotto", "Pidgeot"}, {"Vaporeon", "Vaporeon", "Vaporeon"},
            {"Jolteon", "Jolteon", "Jolteon"}, {"Flareon", "Flareon", "Flareon"}, {"Lapras", "Lapras", "Lapras"}};
    private static final String[] NAMES = {"Treinador Ash", "Equipe Rocket", "Líder Brock", "Capitã Misty", "Mestre Oak", "Cavaleiro Red",
            "Dama Erika", "Doutor Fuji", "Campeão Blue", "Ninja Koga", "Rainha Sabrina", "Capitão Surge", "Domadora Lorelei", "Bruno de Pedra",
            "Agatha", "Lance, o Dragão", "Cinthia", "Jovem Joey", "Pescador Ben", "Alpinista Sam"};
    private static final String[] ICONS = {"🧢", "🥷", "🧙", "🦸", "🧑‍🚀", "🤺", "👸", "🧑‍🔬", "🥋", "🕵️", "🦹", "👨‍✈️"};
    private static final double[] LEVEL_MULT = {0.8, 0.95, 1.1, 1.25, 1.4, 1.6};

    private static String stage(String[] line, int level) { return line[level >= 36 ? 2 : level >= 16 ? 1 : 0]; }

    /** Os 6 lutadores do dia, do mais fraco ao mais forte, escalados pelo nível médio do seu time. */
    public static List<Fighter> fighters(int day, int playerLevel) {
        List<Fighter> out = new ArrayList<>();
        int base = Math.max(3, playerLevel);
        for (int i = 0; i < FIGHTERS; i++) {
            Random r = new Random(day * 131L + i * 17L);
            Fighter f = new Fighter();
            f.index = i;
            f.level = Math.max(3, (int) Math.round(base * LEVEL_MULT[i]));
            f.name = NAMES[Math.abs(r.nextInt()) % NAMES.length];
            f.icon = ICONS[Math.abs(r.nextInt()) % ICONS.length];
            f.rpWin = 14 + i * 4;
            f.gold = 60 * f.level + i * 120;
            List<Integer> pool = new ArrayList<>();
            for (int k = 0; k < LINES.length; k++) pool.add(k);
            Collections.shuffle(pool, r);
            for (int k = 0; k < 3; k++) {
                f.levels[k] = Math.max(1, f.level + r.nextInt(3) - 1);
                f.species[k] = stage(LINES[pool.get(k)], f.levels[k]);
            }
            out.add(f);
        }
        return out;
    }

    // ------------------------------------------------------------------ times (usam Companion de verdade = mesmos stats do jogo)
    public static ArenaDuel.Unit unit(Context ctx, String species, int level, boolean shiny, com.example.pokeworld.entities.Rarity rarity, int gender, int boost, int forge) {
        Companion c = new Companion(ctx, 0, 0, 0, species);
        c.loadProgress(Math.max(1, level), 0, forge, shiny, 0, null, rarity, gender);
        if (boost > 0) c.setSupport(boost, null);
        c.healFull();
        return ArenaDuel.Unit.from(c);
    }

    /** Time do lutador: contexto nulo de propósito, para os talentos do jogador não valerem para o oponente. */
    public static List<ArenaDuel.Unit> teamOf(Context ctx, Fighter f) {
        List<ArenaDuel.Unit> l = new ArrayList<>();
        for (int k = 0; k < 3; k++) l.add(unit(null, f.species[k], f.levels[k], false, com.example.pokeworld.entities.Rarity.COMMON, k % 2, 0, 0));
        return l;
    }

    public static int powerOf(List<ArenaDuel.Unit> team) { int s = 0; for (ArenaDuel.Unit u : team) s += u.power(); return s; }

    /** Os Pokémon do jogador disponíveis para a Arena (o time de 6), já com nível/raridade/boost reais. */
    public static List<ArenaDuel.Unit> playerRoster(GameView gv) {
        List<ArenaDuel.Unit> l = new ArrayList<>();
        for (CapturedCreature c : gv.player.getParty()) {
            int lv; int forge = 0;
            if (PartyManager.isActive(gv, c)) { lv = gv.companion.getLevel(); }
            else {
                PetData d = gv.activity.dbHelper.getPetData(c.name);
                lv = d != null ? Math.max(1, d.level) : 1;
                forge = d != null ? d.forgeBonus : 0;
            }
            l.add(unit(gv.activity, c.name, lv, c.isShiny, c.rarity, c.gender, c.boost, forge));
        }
        return l;
    }

    public static int avgLevel(List<ArenaDuel.Unit> roster) {
        if (roster.isEmpty()) return 1;
        int s = 0; for (ArenaDuel.Unit u : roster) s += u.level;
        return Math.max(1, s / roster.size());
    }

    /** Poder dos 3 melhores do jogador (é o que ele levaria se escolhesse bem). */
    public static int bestPower(List<ArenaDuel.Unit> roster) {
        List<Integer> p = new ArrayList<>();
        for (ArenaDuel.Unit u : roster) p.add(u.power());
        Collections.sort(p, Collections.reverseOrder());
        int s = 0; for (int i = 0; i < Math.min(3, p.size()); i++) s += p.get(i);
        return s;
    }

    // ------------------------------------------------------------------ resultado
    public static final class Result {
        public boolean won; public int rpDelta, gold, oldTier, newTier, newRp;
    }

    public static Result report(RewardManager.Store s, Fighter f, boolean won) {
        ensureDay(s);
        Result r = new Result();
        r.won = won;
        int old = rp(s);
        r.oldTier = tierOf(old);
        if (won) {
            r.rpDelta = f.rpWin;
            r.gold = f.gold;
            s.set("arena_w", wins(s) + 1);
            s.set("arena_bmask", s.get("arena_bmask", 0) | (1 << f.index));
        } else {
            r.rpDelta = -Math.min(old, 8 + f.index * 2);
            s.set("arena_l", losses(s) + 1);
        }
        int now = Math.max(0, old + r.rpDelta);
        s.set("arena_rp", now);
        if (now > bestRp(s)) s.set("arena_best", now);
        r.newRp = now;
        r.newTier = tierOf(now);
        if (r.newTier > r.oldTier && r.newTier > s.get("arena_tier_paid", 0)) {   // prêmio de subir de liga (uma vez por liga)
            r.gold += 1500 * r.newTier;
            s.set("arena_tier_paid", r.newTier);
        }
        return r;
    }

    // ------------------------------------------------------------------ placar local (liga de treinadores NPC)
    public static final class Row {
        public int rank, rp, wins, losses; public String name; public boolean me;
        public Row(int rank, String name, int rp, int wins, int losses, boolean me) { this.rank = rank; this.name = name; this.rp = rp; this.wins = wins; this.losses = losses; this.me = me; }
    }

    /** Liga local: 40 treinadores fixos que sobem devagar a cada semana + você. Funciona sem internet. */
    public static List<Row> localBoard(RewardManager.Store s, String myName, int week) {
        List<Row> rows = new ArrayList<>();
        Random r = new Random(7777);
        for (int i = 0; i < 40; i++) {
            int base = (int) Math.round(2300 * Math.pow(0.90, i)) + r.nextInt(30);
            int growth = Math.min(300, week * (6 + r.nextInt(6)));
            int rp = Math.max(0, Math.min(2600, base + growth - 250));
            int w = rp / 14 + r.nextInt(10);
            rows.add(new Row(0, NAMES[i % NAMES.length] + (i >= NAMES.length ? " " + (i / NAMES.length + 1) : ""), rp, w, w / 3 + r.nextInt(8), false));
        }
        rows.add(new Row(0, myName, rp(s), wins(s), losses(s), true));
        Collections.sort(rows, (a, b) -> b.rp != a.rp ? Integer.compare(b.rp, a.rp) : Boolean.compare(b.me, a.me));
        for (int i = 0; i < rows.size(); i++) rows.get(i).rank = i + 1;
        return rows;
    }

    public static int weekIndex() { return RewardManager.today() / 7 % 1000; }
}
