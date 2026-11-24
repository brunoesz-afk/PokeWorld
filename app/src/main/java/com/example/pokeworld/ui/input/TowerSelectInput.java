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

        if (!ui.endlessBtn.isEmpty() && ui.endlessBtn.contains(x, y)) {
            if (gv.tower.isEndlessUnlocked()) {
                gv.tower.startEndless();
                gv.ui.state = UIManager.UIState.EXPLORING;
            } else gv.ui.showMessage("🔒 Conclua o andar 20 para liberar o modo infinito.");
            return true;
        }
        for (TowerSelectUI.FloorCard card : ui.floorCards) {
            if (card.rect.contains(x, y)) {
                ui.selectedFloor = card.floor;
                if (!card.unlocked) gv.ui.showMessage("🔒 Conclua o andar " + (card.floor - 1) + " para liberar este.");
                return true;
            }
        }
        if (!ui.enterBtn.isEmpty() && ui.enterBtn.contains(x, y)) {
            if (ui.selectedFloor <= gv.tower.getMaxUnlockedFloor()) {
                gv.tower.startTowerAtFloor(ui.selectedFloor);
                gv.ui.state = UIManager.UIState.EXPLORING;
            } else {
                gv.ui.showMessage("🔒 Andar bloqueado! Complete os anteriores.");
            }
        }
        return true;
    }
}
