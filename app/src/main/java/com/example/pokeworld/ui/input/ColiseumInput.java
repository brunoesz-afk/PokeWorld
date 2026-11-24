package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ColiseumUI;

public class ColiseumInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        ColiseumUI ui = (ColiseumUI) gv.ui.getHandler(UIManager.UIState.COLISEUM);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }
        if (ui.arenaBtn.contains(x, y)) {
            com.example.pokeworld.ui.handlers.ArenaUI au = (com.example.pokeworld.ui.handlers.ArenaUI) gv.ui.getHandler(UIManager.UIState.ARENA);
            if (au != null) au.invalidate();
            gv.ui.state = UIManager.UIState.ARENA; return true;
        }
        if (ui.rankBtn.contains(x, y)) { ArenaInput.openRank(gv); return true; }
        if (ui.towerBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.TOWER_SELECT; return true; }
        if (ui.endlessBtn.contains(x, y)) {
            if (gv.tower.isEndlessUnlocked()) gv.ui.state = UIManager.UIState.TOWER_SELECT;
            else gv.ui.showMessage("🔒 Conclua o andar " + com.example.pokeworld.managers.TowerManager.MAX_FLOOR + " da Torre primeiro.");
            return true;
        }
        if (ui.gymBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.GYM; return true; }
        if (ui.rivalBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPEDITION; return true; }
        return true;
    }
}
