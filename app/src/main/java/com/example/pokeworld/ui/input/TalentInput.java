package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.TalentUI;

public class TalentInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        TalentUI ui = (TalentUI) gv.ui.getHandler(UIManager.UIState.TALENTS);
        if (ui.domadorTalentBtn.contains(x, y)) gv.player.useTalentPoint("Domador");
        else if (ui.guerreiroTalentBtn.contains(x, y)) gv.player.useTalentPoint("Guerreiro");
        else if (ui.exploradorTalentBtn.contains(x, y)) gv.player.useTalentPoint("Explorador");
        else gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
