package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Equipment;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.entities.Rarity;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.CreatureUI;
import com.example.pokeworld.ui.handlers.EvolutionItemsUI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatureInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        CreatureUI ui = (CreatureUI) gv.ui.getHandler(UIManager.UIState.CREATURES);
        if (ui.evolveBtn.contains(x, y)) {
            if (gv.player.getInventory().containsKey("Cristal") && gv.companion.getSpecies().equalsIgnoreCase("Lobo")) {
                gv.player.useItem("Cristal");
                gv.companion.evolve(gv.player);
                gv.applyKingdomBonuses(); 
                gv.ui.showMessage("✨ Evoluiu!");
                return true; 
            }
        } 
        
        if (ui.upgradeBtn.contains(x, y)) {
            List<CapturedCreature> col = gv.player.getCreatureCollection();
            Map<String, Integer> counts = new HashMap<>();
            for (CapturedCreature c : col) {
                if (c.rarity != Rarity.LEGENDARY) {
                    String key = c.name + "_" + c.rarity.name() + "_" + (c.isShiny ? "SH" : "NORM");
                    counts.put(key, counts.getOrDefault(key, 0) + 1);
                    if (counts.get(key) >= 4) {
                        gv.fusion.attemptUpgrade(c.name, c.rarity, c.isShiny);
                        gv.applyKingdomBonuses(); 
                        return true;
                    }
                }
            }
            return true;
        }
        
        if (x > gv.getWidth() * 0.3f && x < gv.getWidth() * 0.7f) {
            int index = (int)((y - 170) / 50);
            List<CapturedCreature> party = gv.player.getParty();
            if (index >= 0 && index < party.size()) {
                CapturedCreature selected = party.get(index);
                
                if (x < gv.getWidth() * 0.45f) {
                    EvolutionItemsUI evUI = (EvolutionItemsUI) gv.ui.getHandler(UIManager.UIState.EVOLUTION);
                    evUI.selectedCreature = selected;
                    gv.ui.state = UIManager.UIState.EVOLUTION;
                    return true;
                }

                if (gv.world.tileMap.getMapName().equals("HOME") && party.size() > 1) {
                    selected.isInParty = false;
                    gv.ui.showMessage(selected.name + " enviado para a BASE!");
                    gv.ui.state = UIManager.UIState.EXPLORING;
                    return true;
                }

                gv.activity.dbHelper.savePet(
                    gv.companion.getSpecies(),
                    gv.companion.getLevel(),
                    gv.companion.getExp(),
                    gv.companion.getForgeAttackBonus(),
                    gv.companion.getWeapon() != null ? gv.companion.getWeapon().getName() : "Nenhum",
                    gv.companion.getArmor() != null ? gv.companion.getArmor().getName() : "Nenhum",
                    gv.companion.isShiny(),
                    gv.companion.getPersonality(),
                    gv.companion.getRune() != null ? gv.companion.getRune().getName() : "Nenhum",
                    gv.companion.getFriendship(),
                    gv.companion.getType2() != null ? gv.companion.getType2().name() : "Nenhum",
                    gv.companion.getRarity(),
                    gv.companion.getGender()
                );

                String species = selected.name;
                gv.companion.setSpecies(species);
                PetData data = gv.activity.dbHelper.getPetData(species);
                // Força o estado Shiny do CapturedCreature selecionado
                gv.companion.loadProgress(data.level, data.exp, data.forgeBonus, selected.isShiny, data.friendship, data.type2, data.rarity, selected.gender);
                gv.companion.setWeapon(Equipment.create(data.weapon));
                gv.companion.setArmor(Equipment.create(data.armor));
                gv.applyKingdomBonuses();
                gv.player.setMounted(false);
                gv.ui.showMessage("🐾 Companheiro: " + species);
                return true; 
            }
        }
        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
