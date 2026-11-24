package com.example.pokeworld.ui.input;

import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.ExpeditionManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ExpeditionUI;

public class ExpeditionInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        ExpeditionUI ui = (ExpeditionUI) gv.ui.getHandler(UIManager.UIState.EXPEDITION);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }
        if (!ui.collectBtn.isEmpty() && ui.collectBtn.contains(x, y)) { gv.ui.showMessage(ExpeditionManager.collect(gv)); return true; }
        for (int i = 0; i < ui.sendBtns.size(); i++) {
            RectF r = ui.sendBtns.get(i);
            if (!r.isEmpty() && r.contains(x, y)) { gv.ui.showMessage(ExpeditionManager.start(gv, i)); return true; }
        }
        for (int i = 0; i < ui.attackBtns.size(); i++) {
            if (ui.attackBtns.get(i).contains(x, y)) { gv.ui.showMessage(ExpeditionManager.attack(gv, i)); return true; }
        }
        return true;
    }
}
