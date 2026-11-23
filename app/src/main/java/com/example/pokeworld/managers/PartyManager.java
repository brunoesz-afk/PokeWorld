package com.example.pokeworld.managers;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.Equipment;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.entities.Rune;

/** Troca do Pokémon ativo e sincronização com o Time/banco quando ele evolui. */
public final class PartyManager {
    private PartyManager() {}

    /** Grava o estado do companheiro ativo na tabela de pets. */
    public static void saveActive(GameView gv) {
        Companion c = gv.companion;
        gv.activity.dbHelper.savePet(
            c.getSpecies(), c.getLevel(), c.getExp(), c.getForgeAttackBonus(),
            c.getWeapon() != null ? c.getWeapon().getName() : "Nenhum",
            c.getArmor() != null ? c.getArmor().getName() : "Nenhum",
            c.isShiny(), c.getPersonality(),
            c.getRune() != null ? c.getRune().getName() : "Nenhum",
            c.getFriendship(),
            c.getType2() != null ? c.getType2().name() : "Nenhum",
            c.getRarity(), c.getGender());
    }

    public static boolean isActive(GameView gv, CapturedCreature c) {
        return c.name.equalsIgnoreCase(gv.companion.getSpecies());
    }

    /** Torna a criatura o companheiro ativo (salva o anterior e carrega nível, equipamento e runa do novo). */
    public static boolean switchActive(GameView gv, CapturedCreature sel) {
        if (isActive(gv, sel)) return false;
        saveActive(gv);
        Companion comp = gv.companion;
        comp.setSpecies(sel.name);
        PetData d = gv.activity.dbHelper.getPetData(sel.name);
        comp.loadProgress(d.level, d.exp, d.forgeBonus, sel.isShiny, d.friendship, d.type2, sel.rarity, sel.gender);
        comp.setWeapon(Equipment.create(d.weapon));
        comp.setArmor(Equipment.create(d.armor));
        comp.setRune(Rune.create(d.rune));
        gv.applyKingdomBonuses();
        gv.player.setMounted(false);
        gv.activity.dbHelper.updateActiveSpecies(sel.name);
        gv.activity.saveCurrentGame();
        return true;
    }

    /** Chamado depois que o companheiro ativo virou {@code newSpecies}: renomeia a criatura do time e migra o registro do pet. */
    public static void onActiveEvolved(GameView gv, String oldSpecies, String newSpecies) {
        for (CapturedCreature c : gv.player.getCreatureCollection()) {
            if (c.name.equalsIgnoreCase(oldSpecies) && c.isInParty) { c.name = newSpecies; break; }
        }
        gv.activity.dbHelper.deletePet(oldSpecies);
        saveActive(gv);
        gv.activity.dbHelper.updateActiveSpecies(newSpecies);
        gv.activity.dbHelper.addToBestiary(newSpecies);
        gv.activity.saveCurrentGame();
    }

    /**
     * Evolui um Pokémon qualquer do time (não só o ativo): se for o ativo, passa pelo Companion;
     * senão só renomeia a criatura e migra os dados salvos.
     */
    public static void evolveCreature(GameView gv, CapturedCreature c, String newSpecies) {
        String old = c.name;
        if (isActive(gv, c)) {
            gv.companion.evolveInto(newSpecies);
            return;
        }
        PetData d = gv.activity.dbHelper.getPetData(old);
        c.name = newSpecies;
        gv.activity.dbHelper.deletePet(old);
        gv.activity.dbHelper.savePet(newSpecies, d.level, d.exp, d.forgeBonus, d.weapon, d.armor, c.isShiny, d.personality, d.rune, d.friendship, d.type2, c.rarity, c.gender);
        gv.activity.dbHelper.addToBestiary(newSpecies);
        gv.activity.saveCurrentGame();
    }
}
