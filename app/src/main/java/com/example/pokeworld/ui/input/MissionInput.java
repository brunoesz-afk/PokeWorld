package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.MissionUI;

public class MissionInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        MissionUI ui = (MissionUI) gv.ui.getHandler(UIManager.UIState.MISSIONS);
        if (ui != null) {
            if (ui.tabJourney.contains(x, y)) { ui.showDaily = false; ui.showEvents = false; return true; }
            if (ui.tabDaily.contains(x, y)) { ui.showDaily = true; ui.showEvents = false; return true; }
            if (ui.tabEvents.contains(x, y)) { ui.showEvents = true; ui.showDaily = false; return true; }
            if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }
            return true;
        }
        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
