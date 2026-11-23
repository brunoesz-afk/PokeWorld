package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.AchievementUI;

public class AchievementInput extends TapScrollInput {
    @Override
    protected void onScroll(float x, float y, float dy, GameView gv) {
        AchievementUI ui = (AchievementUI) gv.ui.getHandler(UIManager.UIState.ACHIEVEMENTS);
        if (ui != null && ui.listArea.contains(x, y)) ui.scroll = Math.max(0, Math.min(ui.maxScroll, ui.scroll - dy));
    }

    @Override
    protected boolean onTap(float x, float y, GameView gv) {
        AchievementUI ui = (AchievementUI) gv.ui.getHandler(UIManager.UIState.ACHIEVEMENTS);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }
        for (int i = 0; i < ui.filterRects.size(); i++) {
            if (ui.filterRects.get(i).contains(x, y)) { ui.filter = i; ui.scroll = 0; return true; }
        }
        return true;
    }
}
