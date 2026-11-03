package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.EvolutionItemsUI;

public class EvolutionItemsInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        EvolutionItemsUI ui = (EvolutionItemsUI) gv.ui.getHandler(UIManager.UIState.EVOLUTION); // Usando EVOLUTION para simplificar
        if (ui.selectedCreature == null) {
            gv.ui.state = UIManager.UIState.EXPLORING;
            return true;
        }

        if (ui.fireStoneBtn.contains(x, y)) evolve(gv, ui, "Pedra Fogo", "Flareon", "Arcanine", "Magmar");
        else if (ui.waterStoneBtn.contains(x, y)) evolve(gv, ui, "Pedra Água", "Vaporeon", "Blastoise", "Poliwrath");
        else if (ui.thunderStoneBtn.contains(x, y)) evolve(gv, ui, "Pedra Trovão", "Jolteon", "Raichu", "Electabuzz");
        else gv.ui.state = UIManager.UIState.CREATURES;

        return true;
    }

    private void evolve(GameView gv, EvolutionItemsUI ui, String stone, String targetEevee, String targetOther1, String targetOther2) {
        if (gv.player.getInventory().getOrDefault(stone, 0) > 0) {
            gv.player.useItem(stone);
            String oldName = ui.selectedCreature.name.toUpperCase();
            String newName = targetOther1;
            if (oldName.contains("EEVEE")) newName = targetEevee;
            else if (oldName.contains("PIKACHU")) newName = "RAICHU";
            else if (oldName.contains("GROWLITHE")) newName = "ARCANINE";
            
            ui.selectedCreature.name = newName;
            gv.ui.showMessage("✨ " + oldName + " EVOLUIU PARA " + newName + "!");
            gv.ui.state = UIManager.UIState.CREATURES;
            gv.activity.saveCurrentGame();
        } else gv.ui.showMessage("Você não tem " + stone + "!");
    }
}
