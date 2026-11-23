package com.example.pokeworld.managers;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;
import com.example.pokeworld.world.WorldManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BestiaryManager {
    /** Pokédex regional, na ordem de exibição (o número da entrada é a posição + 1). */
    public static final String[] POKEDEX = {
        "Bulbasaur", "Ivysaur", "Venusaur", "Charmander", "Charmeleon", "Charizard", "Squirtle", "Wartortle", "Blastoise",
        "Caterpie", "Metapod", "Butterfree", "Weedle", "Kakuna", "Beedril", "Pidgey", "Pidgeotto", "Pidgeot",
        "Rattata", "Raticate", "Spearow", "Fearow", "Ekans", "Arbok", "Pichu", "Pikachu", "Raichu",
        "Sandshrew", "Sandslash", "Nidoranf", "Nidorina", "Nidoqueen", "Nidoranm", "Nidorino", "Nidoking",
        "Clefairy", "Clefable", "Vulpix", "Ninetales", "Jigglypuff", "Wigglytuff", "Zubat", "Golbat",
        "Oddish", "Gloom", "Vileplume", "Paras", "Parasect", "Venonat", "Venomoth", "Diglett", "Dugtrio",
        "Meowth", "Persian", "Psyduck", "Golduck", "Mankey", "Primeape", "Growlithe", "Arcanine",
        "Poliwag", "Poliwhirl", "Poliwrath", "Abra", "Kadabra", "Alakazam", "Machop", "Machoke", "Machamp",
        "Bellsprout", "Weepinbell", "Victreebel", "Tentacool", "Tentacruel", "Geodude", "Graveler", "Golem",
        "Ponyta", "Rapidash", "Slowpoke", "Slowking", "Grimer", "Muk", "Cloyster", "Gastly", "Haunter", "Gengar",
        "Onix", "Koffing", "Weezing", "Rhydon", "Tangela", "Scyther", "Jynx", "Magmar", "Pinsir", "Tauros",
        "Magikarp", "Gyarados", "Lapras", "Eevee", "Vaporeon", "Jolteon", "Flareon", "Leafeon",
        "Aerodactyl", "Snorlax", "Houndour", "Houndoom", "Piloswine",
        "Zapdos", "Articuno", "Raiquaza", "Kraken", "Fênix de Fogo"
    };

    private Set<String> seenCreatures = new HashSet<>();
    private Set<String> capturedCreatures = new HashSet<>();
    private DatabaseHelper dbHelper;

    public BestiaryManager(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
        for (String s : dbHelper.getBestiary()) seenCreatures.add(normalize(s));
        // Inicialmente, sincroniza capturados da coleção (quem está na coleção, foi visto)
        for (CapturedCreature c : dbHelper.getCollection()) {
            String n = normalize(c.name);
            capturedCreatures.add(n);
            seenCreatures.add(n);
        }
    }

    /** Minúsculas e sem sufixos de variação (shiny etc.), para comparar nomes de fontes diferentes. */
    public static String normalize(String name) {
        if (name == null) return "";
        String n = name.toLowerCase().trim();
        int u = n.indexOf('_');
        if (u > 0) n = n.substring(0, u);
        if (n.equals("venosaur")) n = "venusaur";
        if (n.equals("beedrill")) n = "beedril";
        return n;
    }

    public static int indexOf(String species) {
        String n = normalize(species);
        for (int i = 0; i < POKEDEX.length; i++) if (normalize(POKEDEX[i]).equals(n)) return i;
        return -1;
    }

    public void markAsSeen(String species) {
        if (indexOf(species) < 0) return;           // só espécies da Pokédex (ignora slimes, espectros etc.)
        String n = normalize(species);
        if (seenCreatures.add(n)) dbHelper.addToBestiary(POKEDEX[indexOf(species)]);
    }

    public void markAsCaptured(String species) {
        markAsSeen(species); // Se capturou, certamente viu
        capturedCreatures.add(normalize(species));
    }

    public Set<String> getSeenCreatures() { return seenCreatures; }
    public Set<String> getCapturedCreatures() { return capturedCreatures; }
    public boolean isSeen(String species) { return seenCreatures.contains(normalize(species)); }
    public boolean isCaptured(String species) { return capturedCreatures.contains(normalize(species)); }

    public int seenCount() {
        int c = 0;
        for (String s : POKEDEX) if (isSeen(s)) c++;
        return c;
    }

    public int capturedCount() {
        int c = 0;
        for (String s : POKEDEX) if (isCaptured(s)) c++;
        return c;
    }

    /** Nomes dos mapas onde a espécie aparece como selvagem. */
    public static List<String> habitats(String species) {
        List<String> out = new ArrayList<>();
        String n = normalize(species);
        for (String map : WorldManager.WILD_MAPS) {
            for (String s : WorldManager.getSpeciesForMap(map)) {
                if (normalize(s).equals(n)) { out.add(WorldManager.mapLabel(map)); break; }
            }
        }
        return out;
    }
}
