package com.example.pokeworld.ui.input;

import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.TrainingManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.TrainingUI;

public class TrainingInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        TrainingUI ui = (TrainingUI) gv.ui.getHandler(UIManager.UIState.TRAINING);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }
        for (int i = 0; i < ui.slotBtns.size(); i++) {
            RectF r = ui.slotBtns.get(i);
            if (!r.isEmpty() && r.contains(x, y)) { gv.ui.showMessage(TrainingManager.collect(gv, i)); return true; }
        }
        if (!ui.trainBtn.isEmpty() && ui.trainBtn.contains(x, y) && ui.selected != null) {
            gv.ui.showMessage(TrainingManager.start(gv, ui.selected));
            ui.selected = null;
            return true;
        }
        for (int i = 0; i < ui.candRects.size(); i++) {
            if (ui.candRects.get(i).contains(x, y)) { ui.selected = ui.cands.get(i); return true; }
        }
        return true;
    }
}
