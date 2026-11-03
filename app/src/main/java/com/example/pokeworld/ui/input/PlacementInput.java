package com.example.pokeworld.ui.input;

import android.view.MotionEvent;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.PlacementUI;
import com.example.pokeworld.world.TileMap;

public class PlacementInput implements InputExtension {
    @Override
    public boolean handleEvent(MotionEvent event, GameView gv) {
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            float x = event.getX();
            float y = event.getY();
            
            gv.ui.placementX = x + gv.world.getCameraX();
            gv.ui.placementY = y + gv.world.getCameraY();
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
        // Verifica se está dentro do core da base (25 a 75 tiles)
        int tileX = (int)(gv.ui.placementX / TileMap.TILE_SIZE);
        int tileY = (int)(gv.ui.placementY / TileMap.TILE_SIZE);
        
        if (gv.world.tileMap.getMapName().equals("HOME")) {
            String bName = gv.ui.placementBuildingName;
            int margin = 5; // Margem para prédios 10x10
            if (bName.equalsIgnoreCase("Fabrica de Montaria")) margin = 7; // Prédio 14x14
            else if (bName.equalsIgnoreCase("Tocha") || bName.equalsIgnoreCase("Baú")) margin = 1;

            if (tileX < 20 + margin || tileX > 60 - margin || tileY < 10 + margin || tileY > 30 - margin) {
                gv.ui.showMessage("❌ Muito próximo aos limites da base!");
                return;
            }
        }

        if (gv.world.isCollidingWithBuilding(gv.ui.placementX, gv.ui.placementY)) {
            gv.ui.showMessage("❌ Local obstruído!");
            return;
        }

        String name = gv.ui.placementBuildingName;
        
        // Caso especial: Pavimentação (não é um prédio)
        if (name.equals("PISO_PEDRA")) {
            if (gv.player.getInventory().getOrDefault("Pedra", 0) >= 2) {
                gv.player.useItem("Pedra");
                gv.world.tileMap.setTileType(gv.ui.placementX, gv.ui.placementY, TileMap.STONE_PAVEMENT);
                gv.ui.showMessage("🧱 CHÃO PAVIMENTADO!");
                gv.activity.saveCurrentGame();
            } else {
                gv.ui.showMessage("Faltam Pedras!");
                gv.ui.state = UIManager.UIState.EXPLORING;
            }
            return;
        }

        // Cobra os recursos na confirmação
        int madeiraCost = 0, ferroCost = 0;
        if (name.equalsIgnoreCase("Fabrica de Montaria")) madeiraCost = 50;
        else if (name.equalsIgnoreCase("Torre Choque")) ferroCost = 40;
        else if (name.equalsIgnoreCase("Tocha")) madeiraCost = 5;
        else if (name.equalsIgnoreCase("Baú")) madeiraCost = 10;
        else if (name.equalsIgnoreCase("Oficina")) { madeiraCost = 5; ferroCost = 2; }
        else if (name.equalsIgnoreCase("Daycare")) { madeiraCost = 15; ferroCost = 5; }
        else if (name.equalsIgnoreCase("Laboratorio")) { madeiraCost = 30; ferroCost = 15; }

        for (int i = 0; i < madeiraCost; i++) gv.player.useItem("Madeira");
        for (int i = 0; i < ferroCost; i++) gv.player.useItem("Ferro");

        // Calcula posição TOP-LEFT baseada no clique CENTRAL
        float w = 640, h = 640;
        if (name.toLowerCase().contains("montaria")) { w = 896; h = 896; }
        else if (name.toLowerCase().contains("tocha")) { w = 64; h = 64; }
        else if (name.toLowerCase().contains("baú")) { w = 128; h = 128; }

        double bx = gv.ui.placementX - (w / 2);
        double by = gv.ui.placementY - (h / 2);

        // Tempo de construção baseado no tipo
        long duration = 30000; // 30 segundos padrão
        if (name.equalsIgnoreCase("Laboratorio") || name.equalsIgnoreCase("Fabrica de Montaria")) duration = 60000; // 1 minuto

        gv.activity.dbHelper.updateBuildingPosition(name, bx, by, System.currentTimeMillis() + duration);
        gv.ui.state = UIManager.UIState.EXPLORING;
        gv.ui.showMessage("🏗️ INICIADA CONSTRUÇÃO DE " + name.toUpperCase() + "!");
        
        // Recarrega o mapa para aplicar a mudança
        gv.world.loadMap(gv.world.tileMap.getMapName(), gv.player, gv.companion, "CENTER");
        gv.activity.saveCurrentGame();
    }
}
