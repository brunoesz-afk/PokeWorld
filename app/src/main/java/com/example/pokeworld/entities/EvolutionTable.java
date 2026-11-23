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
        lvl("Charmander", "Charmeleon", 16); lvl("Charmeleon", "Charizard", 36);
        lvl("Squirtle", "Wartortle", 16);    lvl("Wartortle", "Blastoise", 36);
        lvl("Bulbasaur", "Ivysaur", 16);     lvl("Ivysaur", "Venusaur", 32);
        lvl("Caterpie", "Metapod", 7);       lvl("Metapod", "Butterfree", 10);
        lvl("Weedle", "Kakuna", 7);          lvl("Kakuna", "Beedrill", 10);
        lvl("Pidgey", "Pidgeotto", 18);      lvl("Pidgeotto", "Pidgeot", 36);
        lvl("Abra", "Kadabra", 16);          lvl("Kadabra", "Alakazam", 36);
        lvl("Machop", "Machoke", 16);        lvl("Machoke", "Machamp", 36);
        lvl("Gastly", "Haunter", 25);        lvl("Haunter", "Gengar", 38);
        lvl("Pikachu", "Raichu", 30);

        stone("Pikachu", "Raichu", THUNDER);
        stone("Eevee", "Flareon", FIRE);   stone("Eevee", "Vaporeon", WATER);
        stone("Eevee", "Jolteon", THUNDER); stone("Eevee", "Leafeon", LEAF);
        stone("Growlithe", "Arcanine", FIRE); stone("Vulpix", "Ninetales", FIRE);
        stone("Poliwhirl", "Poliwrath", WATER); stone("Staryu", "Starmie", WATER);
        stone("Magneton", "Magnezone", THUNDER);
        stone("Gloom", "Vileplume", LEAF);   stone("Exeggcute", "Exeggutor", LEAF);
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

    public static boolean canUseStone(String species, String stone) { return byStone(species, stone) != null; }
}
