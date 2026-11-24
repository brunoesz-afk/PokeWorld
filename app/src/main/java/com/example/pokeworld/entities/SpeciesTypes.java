package com.example.pokeworld.entities;

/** Tipo de cada espécie (para os bônus do time e para inimigos). */
public final class SpeciesTypes {
    private SpeciesTypes() {}

    private static boolean has(String n, String... keys) { for (String k : keys) if (n.contains(k)) return true; return false; }

    public static ElementalType of(String species) {
        String n = species == null ? "" : species.toUpperCase();
        if (has(n, "DRATINI", "DRAGONAIR", "DRAGONITE", "RAIQUAZA", "DRAGAO", "DRAGÃO")) return ElementalType.DRAGON;
        if (has(n, "SQUIRTLE", "WARTORTLE", "BLASTOISE", "VAPOREON", "GYARADOS", "LAPRAS", "SHELLDER", "CLOYSTER", "KRAKEN", "POLIWAG", "POLIWHIRL", "STARYU", "MAGIKARP", "PSYDUCK")) return ElementalType.WATER;
        if (has(n, "CHARMANDER", "CHARMELEON", "CHARIZARD", "FLAREON", "GROWLITHE", "ARCANINE", "HOUNDOUR", "MAGMAR", "PONYTA", "VULPIX", "NINETALES", "LOBOFOGO")) return ElementalType.FIRE;
        if (has(n, "BULBASAUR", "IVYSAUR", "VENUSAUR", "VENOSAUR", "ODDISH", "GLOOM", "BELLSPROUT", "LEAFEON", "EXEGG")) return ElementalType.PLANT;
        if (has(n, "PIKACHU", "RAICHU", "PICHU", "ZAPDOS", "JOLTEON", "MAGNEMITE", "MAGNETON")) return ElementalType.ELECTRIC;
        if (has(n, "GEODUDE", "GRAVELER", "GOLEM", "ONIX", "RHYHORN", "RHYDON", "LARVITAR", "ARON")) return ElementalType.ROCK;
        if (has(n, "SWINUB", "PILOSWINE", "SNEASEL", "ARTICUNO")) return ElementalType.ICE;
        if (has(n, "EKANS", "ARBOK", "ZUBAT", "GOLBAT", "WEEDLE", "KAKUNA", "BEEDRILL", "GRIMER", "MUK", "KOFFING", "WEEZING", "NIDO")) return ElementalType.POISON;
        if (has(n, "ABRA", "KADABRA", "ALAKAZAM", "DROWZEE", "HYPNO", "GASTLY", "HAUNTER", "GENGAR", "MEWTWO")) return ElementalType.PSYCHIC;
        return ElementalType.NORMAL;
    }
}
