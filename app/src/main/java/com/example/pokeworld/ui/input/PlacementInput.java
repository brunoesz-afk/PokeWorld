package com.example.pokeworld.ui.input;

import android.view.MotionEvent;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.PlacementUI;
import com.example.pokeworld.world.BaseLayout;
import com.example.pokeworld.world.TileMap;

/**
 * Colocação de piso de pedra. (Os prédios, torres, baús e tochas da base agora são construídos
 * pelo modo construção — ver BuildInput.)
 */
public class PlacementInput implements InputExtension {
    @Override
    public boolean handleEvent(MotionEvent event, GameView gv) {
        if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
            gv.ui.placementX = gv.world.screenToWorldX(event.getX());
            gv.ui.placementY = gv.world.screenToWorldY(event.getY());
        }
        return false;
    }

    @Override
    public boolean handle(float x, float y, GameView gv) {
        PlacementUI ui = (PlacementUI) gv.ui.getHandler(UIManager.UIState.PLACEMENT);

        if (ui.confirmBtn.contains(x, y)) {
            confirmPlacement(gv);
            return true;
        } else if (ui.cancelBtn.contains(x, y)) {
            gv.ui.state = UIManager.UIState.EXPLORING;
            return true;
        }
        return true;
    }

    private void confirmPlacement(GameView gv) {
        int tileX = (int) (gv.ui.placementX / TileMap.TILE_SIZE);
        int tileY = (int) (gv.ui.placementY / TileMap.TILE_SIZE);

        // Na base só dá para pavimentar dentro do terreno e fora das peças construídas
        if (gv.world.tileMap.getMapName().equals("HOME")) {
            if (!BaseLayout.inPlot(tileX, tileY, 1, 1)) {
                gv.ui.showMessage("❌ Fora do terreno da base!");
                return;
            }
            if (gv.world.base != null && gv.world.base.pieceAt(tileX, tileY) != null) {
                gv.ui.showMessage("❌ Local obstruído!");
                return;
            }
        }

        if (!gv.ui.placementBuildingName.equals("PISO_PEDRA")) { gv.ui.state = UIManager.UIState.EXPLORING; return; }

        if (gv.player.getInventory().getOrDefault("Pedra", 0) >= 2) {
            gv.player.useItem("Pedra");
            gv.player.useItem("Pedra");
            gv.world.tileMap.setTileType(gv.ui.placementX, gv.ui.placementY, TileMap.STONE_PAVEMENT);
            gv.ui.showMessage("🧱 CHÃO PAVIMENTADO!");
            gv.activity.saveCurrentGame();
        } else {
            gv.ui.showMessage("Faltam Pedras!");
            gv.ui.state = UIManager.UIState.EXPLORING;
        }
    }
}
