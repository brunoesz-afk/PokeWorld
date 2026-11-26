package com.example.pokeworld.entities;

import java.util.ArrayList;
import java.util.List;

/** Todas as evoluções do jogo (por nível e por pedra). Uma única fonte para o Companion, a Dex, o Time e a tela de pedras. */
public final class EvolutionTable {
    private EvolutionTable() {}

    public static final class Evo {
        public final String from, to;
        public final int level;      // > 0: evolui ao atingir esse nível
        public final String stone;   // != null: evolui com essa pedra
        Evo(String from, String to, int level, String stone) { this.from = from; this.to = to; this.level = level; this.stone = stone; }
        public String describe() { return stone != null ? stone : "Nv. " + level; }
    }

    public static final String FIRE = "Pedra Fogo", WATER = "Pedra Água", THUNDER = "Pedra Trovão", LEAF = "Pedra Folha";
    public static final String[] STONES = {FIRE, WATER, THUNDER, LEAF};

    private static final List<Evo> ALL = new ArrayList<>();

    private static void lvl(String from, String to, int level) { ALL.add(new Evo(from, to, level, null)); }
    private static void stone(String from, String to, String stone) { ALL.add(new Evo(from, to, 0, stone)); }

    static {
        // Só entram evoluções cujo desenho existe no jogo (sem desenho, a espécie aparecia como Charmander)
        lvl("Charmander", "Charmeleon", 16); lvl("Charmeleon", "Charizard", 36);
        lvl("Squirtle", "Wartortle", 16);    lvl("Wartortle", "Blastoise", 36);
        lvl("Bulbasaur", "Ivysaur", 16);     lvl("Ivysaur", "Venusaur", 32);
        lvl("Caterpie", "Metapod", 7);       lvl("Metapod", "Butterfree", 10);
        lvl("Weedle", "Kakuna", 7);          lvl("Kakuna", "Beedril", 10);
        lvl("Pidgey", "Pidgeotto", 18);      lvl("Pidgeotto", "Pidgeot", 36);
        lvl("Rattata", "Raticate", 20);
        lvl("Spearow", "Fearow", 20);
        lvl("Ekans", "Arbok", 22);
        lvl("Sandshrew", "Sandslash", 22);
        lvl("Nidoranf", "Nidorina", 16);     lvl("Nidorina", "Nidoqueen", 36);
        lvl("Nidoranm", "Nidorino", 16);     lvl("Nidorino", "Nidoking", 36);
        lvl("Clefairy", "Clefable", 32);     lvl("Clefary", "Clefable", 32);
        lvl("Jigglypuff", "Wigglytuff", 32);
        lvl("Zubat", "Golbat", 22);
        lvl("Oddish", "Gloom", 21);
        lvl("Paras", "Parasect", 24);
        lvl("Venonat", "Venomoth", 31);
        lvl("Diglett", "Dugtrio", 26);
        lvl("Meowth", "Persian", 28);
        lvl("Psyduck", "Golduck", 33);
        lvl("Mankey", "Primeape", 28);
        lvl("Poliwag", "Poliwhirl", 25);
        lvl("Bellsprout", "Weepinbell", 21);
        lvl("Abra", "Kadabra", 16);          lvl("Kadabra", "Alakazam", 36);
        lvl("Machop", "Machoke", 16);        lvl("Machoke", "Machamp", 36);
        lvl("Tentacool", "Tentacruel", 30);
        lvl("Ponyta", "Rapidash", 40);
        lvl("Slowpoke", "Slowking", 37);
        lvl("Grimer", "Muk", 38);
        lvl("Koffing", "Weezing", 35);
        lvl("Gastly", "Haunter", 25);        lvl("Haunter", "Gengar", 38);
        lvl("Magikarp", "Gyarados", 20);
        lvl("Pichu", "Pikachu", 12);
        lvl("Pikachu", "Raichu", 30);

        stone("Pikachu", "Raichu", THUNDER);
        stone("Eevee", "Flareon", FIRE);   stone("Eevee", "Vaporeon", WATER);   stone("Eevee", "Jolteon", THUNDER);
        stone("Growlithe", "Arcanine", FIRE); stone("Vulpix", "Ninetales", FIRE);
        stone("Gloom", "Vileplume", LEAF);   stone("Weepinbell", "Victreebel", LEAF);
        stone("Slowpoke", "Slowking", WATER);
    }

    private static boolean same(String a, String b) { return a != null && b != null && a.equalsIgnoreCase(b); }

    /** Evolução por nível disponível para essa espécie nesse nível, ou null. */
    public static Evo byLevel(String species, int level) {
        for (Evo e : ALL) if (e.level > 0 && same(e.from, species) && level >= e.level) return e;
        return null;
    }

    /** Evolução que essa pedra provoca nessa espécie, ou null. */
    public static Evo byStone(String species, String stone) {
        for (Evo e : ALL) if (e.stone != null && e.stone.equals(stone) && same(e.from, species)) return e;
        return null;
    }

    /** Todas as evoluções possíveis a partir da espécie (nível e pedras). */
    public static List<Evo> from(String species) {
        List<Evo> out = new ArrayList<>();
        for (Evo e : ALL) if (same(e.from, species)) out.add(e);
        return out;
    }

    /** Espécie de onde essa evolui, ou null (para mostrar a cadeia na Dex). */
    public static String preEvolution(String species) {
        for (Evo e : ALL) if (same(e.to, species)) return e.from;
        return null;
    }

    /** Estágio na linha evolutiva: 0 = básico, 1 = primeira evolução, 2 = segunda evolução (Pichu não conta como estágio). */
    public static int stage(String species) {
        int n = 0; String cur = species;
        while (n < 3) {
            String pre = preEvolution(cur);
            if (pre == null) break;
            if (!pre.equalsIgnoreCase("Pichu")) n++;
            cur = pre;
        }
        return n;
    }

    public static boolean canUseStone(String species, String stone) { return byStone(species, stone) != null; }
}
