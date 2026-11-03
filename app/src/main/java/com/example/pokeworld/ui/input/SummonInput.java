package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.SummonUI;

public class SummonInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        SummonUI ui = (SummonUI) gv.ui.getHandler(UIManager.UIState.SUMMON);
        if (ui.summonCommonBtn.contains(x, y)) gv.summon.commonSummon();
        else if (ui.summonPremiumBtn.contains(x, y)) gv.summon.premiumSummon();
        else gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
