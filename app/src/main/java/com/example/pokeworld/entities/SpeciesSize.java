package com.example.pokeworld.entities;

import android.graphics.Rect;

import java.util.HashMap;
import java.util.Map;

/**
 * Tamanho de cada espécie no mundo, pela altura real da Pokédex (em metros; fonte: pokemondb.net / Bulbapedia).
 * Os desenhos têm todos o mesmo quadro, então o que muda é o tamanho em que o quadro é desenhado: quem tem até a altura
 * do Bulbasaur (0,7 m) fica do tamanho dele; os maiores crescem, e os gigantes (Onix, Gyarados) ganham bastante destaque.
 * Espécie desconhecida usa 0,7 m (escala 1).
 */
public final class SpeciesSize {
    private SpeciesSize() {}

    /** Altura do Bulbasaur (0,7 m): tamanho mínimo. Quem é menor ou igual fica do tamanho dele. */
    public static final double REF_M = 0.7;
    private static final double MID_M = 2.0, MAX = 4.5;
    private static final Map<String, Double> H = new HashMap<>();

    private static void put(double m, String... names) { for (String n : names) H.put(key(n), m); }

    static {
        put(0.2, "Diglett");
        put(0.3, "Caterpie", "Weedle", "Pidgey", "Rattata", "Spearow", "Paras", "Magnemite", "Shellder", "Ditto", "Eevee", "Pichu");
        put(0.4, "Pikachu", "Nidoranf", "Meowth", "Geodude", "Krabby", "Exeggcute", "Cubone", "Horsea", "Omanyte", "Mew");
        put(0.5, "Squirtle", "Nidoranm", "Jigglypuff", "Oddish", "Mankey", "Voltorb", "Kabuto");
        put(0.6, "Charmander", "Kakuna", "Sandshrew", "Clefairy", "Clefary", "Vulpix", "Poliwag", "Koffing", "Goldeen", "Houndour", "Larvitar");
        put(0.7, "Bulbasaur", "Metapod", "Raticate", "Dugtrio", "Growlithe", "Bellsprout");
        put(0.8, "Raichu", "Nidorina", "Zubat", "Gloom", "Psyduck", "Psiduck", "Machop", "Farfetchd", "Staryu", "Jolteon", "Porygon", "Glaceon");
        put(0.9, "Nidorino", "Abra", "Tentacool", "Grimer", "Magikarp", "Flareon", "Espeon");
        put(1.0, "Ivysaur", "Wartortle", "Beedrill", "Beedril", "Sandslash", "Wigglytuff", "Parasect", "Venonat", "Persian", "Primeape", "Poliwhirl", "Weepinbell", "Graveler", "Ponyta", "Magneton", "Drowzee", "Marowak", "Rhyhorn", "Tangela", "Vaporeon", "Omastar", "Leafeon", "Umbreon", "Sylveon");
        put(1.1, "Charmeleon", "Butterfree", "Pidgeotto", "Ninetales", "Seel", "Chansey", "Starmie", "Electabuzz");
        put(1.2, "Fearow", "Vileplume", "Slowpoke", "Muk", "Electrode", "Lickitung", "Weezing", "Seadra");
        put(1.3, "Nidoqueen", "Clefable", "Poliwrath", "Kadabra", "Gastly", "Kingler", "Seaking", "Mr. Mime", "Magmar", "Kabutops");
        put(1.4, "Nidoking", "Golem", "Doduo", "Hitmonchan", "Jynx", "Tauros", "Houndoom", "Ampharos");
        put(1.5, "Pidgeot", "Venomoth", "Alakazam", "Machoke", "Cloyster", "Gengar", "Hitmonlee", "Scyther", "Pinsir", "Heracross");
        put(1.6, "Blastoise", "Mega Blastoise", "Golbat", "Machamp", "Tentacruel", "Slowbro", "Haunter", "Hypno", "Zapdos");
        put(1.7, "Charizard", "Mega Charizard X", "Mega Charizard Y", "Golduck", "Victreebel", "Rapidash", "Dewgong", "Articuno");
        put(1.8, "Dodrio", "Aerodactyl", "Dratini", "Scizor");
        put(1.9, "Arcanine", "Rhydon", "Raikou");
        put(2.0, "Venusaur", "Venosaur", "Ekans", "Exeggutor", "Moltres", "Mewtwo", "Slowking");
        put(2.1, "Snorlax");
        put(2.2, "Kangaskhan", "Dragonite");
        put(2.4, "Mega Venusaur", "Mega Venosaur");
        put(2.5, "Lapras");
        put(3.5, "Arbok");
        put(4.0, "Dragonair");
        put(6.5, "Gyarados", "Gyardos");
        put(7.0, "Rayquaza", "Raiquaza", "Dragao");
        put(8.8, "Onix");
        put(9.2, "Steelix");
        put(10.8, "Mega Rayquaza");
    }

    static String key(String s) {
        if (s == null) return "";
        String k = s.toLowerCase().replace("_shiny", "").replaceAll("[^a-z0-9♀♂]", "");
        return k;
    }

    /** Altura em metros da espécie (0,7 se não conhecida). */
    public static double meters(String species) {
        Double h = H.get(key(species));
        return h != null ? h : REF_M;
    }

    /**
     * Multiplicador do tamanho de desenho (1,0 = Bulbasaur). Até 2 m cresce devagar (raiz quadrada da razão; Charizard ~1,6x),
     * depois cresce mais (Gyarados ~3,6x, Onix ~4,5x) para os gigantes terem destaque de verdade.
     */
    public static double scale(String species) {
        double h = meters(species);
        if (h <= REF_M) return 1.0;
        double s = Math.sqrt(Math.min(h, MID_M) / REF_M);
        if (h > MID_M) s *= Math.pow(h / MID_M, 0.65);
        return Math.min(MAX, s);
    }

    /** Versão mais suave para a Arena (o quadro lá é fixo): 1,0 a 1,8. */
    public static double softScale(String species) {
        return Math.min(1.8, scale(species));
    }

    /** Quadro de desenho do sprite: largura/altura 2*r*escala, ancorado nos pés (a base do quadro padrão) para o pequeno não flutuar. */
    public static Rect box(double x, double y, int baseR, String species) {
        int r = Math.max(8, (int) Math.round(baseR * scale(species)));
        int bottom = (int) Math.round(y + baseR);
        return new Rect((int) Math.round(x) - r, bottom - 2 * r, (int) Math.round(x) + r, bottom);
    }
}
