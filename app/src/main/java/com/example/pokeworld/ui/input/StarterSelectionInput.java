package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.StarterSelectionUI;

public class StarterSelectionInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        StarterSelectionUI ui = (StarterSelectionUI) gv.ui.getHandler(UIManager.UIState.STARTER_SELECTION);
        if (ui == null) return false;

        String chosenStarter = null;
        if (ui.charmanderBtn.contains(x, y)) chosenStarter = "Charmander";
        else if (ui.bulbasaurBtn.contains(x, y)) chosenStarter = "Bulbasaur";
        else if (ui.squirtleBtn.contains(x, y)) chosenStarter = "Squirtle";
        else if (ui.pikachuBtn.contains(x, y)) chosenStarter = "Pikachu";

        if (chosenStarter != null && gv.tutorial != null) {
            gv.tutorial.onStarterSelected(chosenStarter);
        }
        return true;
    }
}
