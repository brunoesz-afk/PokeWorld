package com.example.pokeworld.managers;

import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.entities.Rarity;
import com.example.pokeworld.ui.UIManager;

import java.util.ArrayList;
import java.util.List;

public class FusionManager {
    private Player player;
    private UIManager ui;

    public FusionManager(Player player, UIManager ui) {
        this.player = player;
        this.ui = ui;
    }

    /**
     * Lógica de Melhoria de Raridade (4 para 1)
     * Transforma 4 Pokémons iguais de mesma raridade em 1 de raridade superior.
     * Diferencia entre Shiny e Normal.
     */
    public void attemptUpgrade(String species, Rarity currentRarity, boolean isShiny) {
        if (currentRarity == Rarity.LEGENDARY) {
            ui.showMessage("Já está no nível máximo!");
            return;
        }

        List<CapturedCreature> collection = player.getCreatureCollection();
        List<CapturedCreature> matches = new ArrayList<>();
        
        for (CapturedCreature c : collection) {
            if (c.name.equalsIgnoreCase(species) && c.rarity == currentRarity && c.isShiny == isShiny) {
                matches.add(c);
            }
        }

        if (matches.size() < 4) {
            String label = isShiny ? " SH " : " ";
            ui.showMessage("Precisa de 4 " + species + label + currentRarity.getLabel() + "!");
            return;
        }

        // Remove os 4 usados
        int removedCount = 0;
        for (int i = collection.size() - 1; i >= 0 && removedCount < 4; i--) {
            CapturedCreature c = collection.get(i);
            if (c.name.equalsIgnoreCase(species) && c.rarity == currentRarity && c.isShiny == isShiny) {
                collection.remove(i);
                removedCount++;
            }
        }

        // Adiciona o novo com raridade superior mantendo o estado Shiny
        Rarity nextRarity = currentRarity.next();
        int gender = (int)(Math.random() * 2);
        player.addCreatureToCollection(species, isShiny, nextRarity, gender);
        
        String shLabel = isShiny ? " [SH] " : " ";
        ui.showMessage("✨ UPGRADE: " + species + shLabel + "agora é " + nextRarity.getLabel() + "!");
    }
}
