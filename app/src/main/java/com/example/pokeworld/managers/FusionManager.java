package com.example.pokeworld.managers;

import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.entities.Rarity;
import com.example.pokeworld.ui.UIManager;

import java.util.ArrayList;
import java.util.List;

/** Fusão de raridade: 4 Pokémon iguais (mesma espécie, raridade e brilho) viram 1 de raridade superior. */
public class FusionManager {
    private final Player player;
    private final UIManager ui;

    public FusionManager(Player player, UIManager ui) {
        this.player = player;
        this.ui = ui;
    }

    /** Os que podem ser gastos: nunca o companheiro ativo; a caixa vai primeiro, depois defensores, depois o time. */
    public List<CapturedCreature> candidates(String species, Rarity rarity, boolean shiny, String activeSpecies) {
        List<CapturedCreature> box = new ArrayList<>(), guards = new ArrayList<>(), party = new ArrayList<>();
        boolean skippedActive = false;
        for (CapturedCreature c : player.getCreatureCollection()) {
            if (!c.name.equalsIgnoreCase(species) || c.rarity != rarity || c.isShiny != shiny || c.boost > 0) continue;   // quem tem boost é protegido
            if (c.isInParty && c.name.equalsIgnoreCase(activeSpecies) && !skippedActive) { skippedActive = true; continue; }   // preserva o ativo
            if (c.isInParty) party.add(c); else if (c.isGuard) guards.add(c); else box.add(c);
        }
        List<CapturedCreature> out = new ArrayList<>(box);
        out.addAll(guards); out.addAll(party);
        return out;
    }

    public int available(String species, Rarity rarity, boolean shiny, String activeSpecies) {
        return candidates(species, rarity, shiny, activeSpecies).size();
    }

    /** Funde 4 em 1 de raridade superior. Devolve true se funcionou. */
    public boolean attemptUpgrade(String species, Rarity currentRarity, boolean isShiny, String activeSpecies) {
        if (currentRarity == Rarity.LEGENDARY) { ui.showMessage("Já está no nível máximo!"); return false; }
        List<CapturedCreature> use = candidates(species, currentRarity, isShiny, activeSpecies);
        if (use.size() < 4) {
            ui.showMessage("Precisa de 4 " + species + (isShiny ? " SH " : " ") + currentRarity.getLabel() + " (fora o ativo)! Você tem " + use.size() + ".");
            return false;
        }
        for (int i = 0; i < 4; i++) player.getCreatureCollection().remove(use.get(i));
        Rarity next = currentRarity.next();
        player.addCreatureToCollection(species, isShiny, next, (int) (Math.random() * 2));
        ui.showMessage("✨ FUSÃO: " + species + (isShiny ? " [SH] " : " ") + "agora é " + next.getLabel() + "!");
        SoundManager.play("boost");
        return true;
    }
}
