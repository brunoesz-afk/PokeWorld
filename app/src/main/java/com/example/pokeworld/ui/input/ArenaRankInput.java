package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ArenaRankUI;

public class ArenaRankInput extends TapScrollInput {
    @Override
    protected boolean onTap(float x, float y, GameView gv) {
        ArenaRankUI ui = (ArenaRankUI) gv.ui.getHandler(UIManager.UIState.ARENA_RANK);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.ARENA; return true; }
        for (int i = 0; i < ui.tabs.length; i++) if (ui.tabs[i].contains(x, y)) { ui.page = i; ui.scroll = 0; return true; }
        return true;
    }

    @Override
    protected void onScroll(float x, float y, float dy, GameView gv) {
        ArenaRankUI ui = (ArenaRankUI) gv.ui.getHandler(UIManager.UIState.ARENA_RANK);
        if (ui != null) ui.scroll = Math.max(0, Math.min(ui.maxScroll, ui.scroll - dy));
    }
}
