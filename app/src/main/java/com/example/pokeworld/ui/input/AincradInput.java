package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.AincradUI;

public class AincradInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        AincradUI ui = (AincradUI) gv.ui.getHandler(UIManager.UIState.AINCRAD);
        
        int max = gv.aincrad.getMaxFloorReached();
        int checkpoint = ((max - 1) / 5) * 5 + 1;

        if (ui.startBtn.contains(x, y)) {
            gv.aincrad.start(1);
            return true;
        } else if (checkpoint > 1 && ui.checkpointBtn.contains(x, y)) {
            gv.aincrad.start(checkpoint);
            return true;
        }

        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
