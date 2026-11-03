package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;

public class EvolutionInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
