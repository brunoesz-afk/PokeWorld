package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.TradeManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.TradeUI;

public class TradeInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        TradeUI ui = (TradeUI) gv.ui.getHandler(UIManager.UIState.TRADE);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }
        if (!ui.prevBtn.isEmpty() && ui.prevBtn.contains(x, y)) { ui.page = Math.max(0, ui.page - 1); return true; }
        if (!ui.nextBtn.isEmpty() && ui.nextBtn.contains(x, y)) { ui.page++; return true; }
        for (int i = 0; i < ui.tradeBtns.size(); i++) {
            if (ui.tradeBtns.get(i).contains(x, y)) { gv.ui.showMessage(TradeManager.trade(gv, ui.offers.get(i))); return true; }
        }
        return true;
    }
}
