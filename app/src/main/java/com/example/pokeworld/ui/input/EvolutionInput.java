package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;

public class EvolutionInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        com.example.pokeworld.ui.handlers.EvolutionUI ui = (com.example.pokeworld.ui.handlers.EvolutionUI) gv.ui.getHandler(UIManager.UIState.EVOLVING);
        if (ui == null || ui.isFinished()) gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
