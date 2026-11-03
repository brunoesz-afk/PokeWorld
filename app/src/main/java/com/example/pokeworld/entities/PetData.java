package com.example.pokeworld.entities;

/**
 * Modelo de dados para persistência e transferência de informações de um Pokémon.
 */
public class PetData {
    public String species, weapon, armor, personality, rune, type2;
    public int level, exp, forgeBonus, friendship;
    public boolean isShiny;
    public Rarity rarity;
    public int gender; // 0: Macho, 1: Fêmea

    public PetData(String s, int l, int e, int f, String w, String a, boolean shiny, String p, String r, int fr, String t2, Rarity rarity, int gender) {
        this.species = s; 
        this.level = l; 
        this.exp = e; 
        this.forgeBonus = f; 
        this.weapon = w; 
        this.armor = a; 
        this.isShiny = shiny; 
        this.personality = p; 
        this.rune = r; 
        this.friendship = fr; 
        this.type2 = t2;
        this.rarity = rarity != null ? rarity : Rarity.COMMON;
        this.gender = gender;
    }
}
