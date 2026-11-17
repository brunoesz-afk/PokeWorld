package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.TowerSelectUI;

public class TowerSelectInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        TowerSelectUI ui = (TowerSelectUI) gv.ui.getHandler(UIManager.UIState.TOWER_SELECT);
        if (ui == null) return false;

        if (ui.exitBtn.contains(x, y)) {
            gv.ui.state = UIManager.UIState.EXPLORING;
            return true;
        }

        for (TowerSelectUI.FloorCard card : ui.floorCards) {
            if (card.rect.contains(x, y)) {
                if (card.unlocked) {
                    gv.tower.startTowerAtFloor(card.floor);
                    gv.ui.state = UIManager.UIState.EXPLORING;
                } else {
                    gv.ui.showMessage("❌ Andar bloqueado! Complete os andares anteriores.");
                }
                return true;
            }
        }
        return true;
    }
}
