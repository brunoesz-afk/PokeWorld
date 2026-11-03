package com.example.pokeworld.entities;

/**
 * Representa uma criatura capturada na coleção do jogador.
 */
public class CapturedCreature {
    public String name;
    public boolean isShiny;
    public Rarity rarity;
    public boolean isInParty;
    public int gender; // 0: Macho, 1: Fêmea

    public CapturedCreature(String name, boolean isShiny, Rarity rarity, boolean isInParty, int gender) {
        this.name = name;
        this.isShiny = isShiny;
        this.rarity = rarity != null ? rarity : Rarity.COMMON;
        this.isInParty = isInParty;
        this.gender = gender;
    }

    // Sobrecarga para compatibilidade se necessário
    public CapturedCreature(String name, boolean isShiny, Rarity rarity, boolean isInParty) {
        this(name, isShiny, rarity, isInParty, 0);
    }
}
