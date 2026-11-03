package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.BreedingUI;

import java.util.List;

public class BreedingInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        List<CapturedCreature> collection = gv.player.getCreatureCollection();
        BreedingUI ui = (BreedingUI) gv.ui.getHandler(UIManager.UIState.BREEDING);
        if (ui.selectingParent) {
            int index = (int)((y - 200) / 50);
            if (index >= 0 && index < collection.size()) {
                if (ui.parentSlot == 0) gv.breeding.setParent1(collection.get(index));
                else gv.breeding.setParent2(collection.get(index));
                ui.selectingParent = false;
                gv.ui.showMessage("Selecionado!");
            }
            return true;
        }

        if (ui.selectParentBtn.contains(x, y)) {
            ui.selectingParent = true;
            ui.parentSlot = gv.breeding.getParent1() == null ? 0 : 1;
            return true;
        } else if (ui.collectEggBtn.contains(x, y)) {
            gv.breeding.collectEgg();
        }
        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
