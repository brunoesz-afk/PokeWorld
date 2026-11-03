package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.CraftingUI;
import java.util.Map;

public class CraftingInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        CraftingUI ui = (CraftingUI) gv.ui.getHandler(UIManager.UIState.CRAFTING);
        if (ui.ballItem.contains(x, y)) {
            tryCraftPokeballs(gv);
            return true;
        }
        if (ui.mountFactoryBtn != null && ui.mountFactoryBtn.contains(x, y)) {
            tryStartBuildingPlacement("Fabrica de Montaria", 50, gv);
            return true;
        }
        if (ui.axeUpgradeBtn.contains(x, y)) {
            tryUpgradeTool("Machado", "Madeira", gv);
            return true;
        }
        if (ui.pickaxeUpgradeBtn.contains(x, y)) {
            tryUpgradeTool("Picareta", "Ferro", gv);
            return true;
        }
        if (ui.stoveBtn.contains(x, y)) {
            tryBuildStove(gv);
            return true;
        }
        if (ui.cookBtn.contains(x, y)) {
            tryCook(gv);
            return true;
        }
        if (ui.thermalCapeBtn.contains(x, y)) {
            tryCraftCape(gv);
            return true;
        }
        if (ui.shockTowerBtn.contains(x, y)) {
            boolean hasElectric = false;
            for (com.example.pokeworld.entities.CapturedCreature c : gv.player.getCreatureCollection()) {
                if (c.name.equalsIgnoreCase("Pikachu") || c.name.equalsIgnoreCase("Raichu")) {
                    hasElectric = true; break;
                }
            }
            if (hasElectric) {
                tryStartBuildingPlacement("Torre Choque", 40, gv);
            } else {
                gv.ui.showMessage("❌ Você precisa de um Pokémon Elétrico (Pikachu)!");
            }
            return true;
        }
        if (ui.torchBtn.contains(x, y)) {
            tryStartBuildingPlacement("Tocha", 5, gv);
            return true;
        }
        if (ui.chestBtn.contains(x, y)) {
            tryStartBuildingPlacement("Baú", 10, gv);
            return true;
        }
        if (ui.pavementBtn.contains(x, y)) {
            gv.ui.placementBuildingName = "PISO_PEDRA";
            gv.ui.state = UIManager.UIState.PLACEMENT;
            gv.ui.showMessage("Clique no chão para pavimentar (2 Pedras)");
            return true;
        }

        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }

    private void tryStartBuildingPlacement(String name, int woodReq, GameView gv) {
        Map<String, Integer> inventory = gv.player.getInventory();
        Integer woodObj = inventory.get("Madeira");
        int wood = (woodObj != null) ? woodObj : 0;
        
        if (wood >= woodReq) {
            gv.ui.placementBuildingName = name;
            gv.ui.placementX = gv.player.getPositionX();
            gv.ui.placementY = gv.player.getPositionY();
            gv.ui.state = UIManager.UIState.PLACEMENT;
            gv.ui.showMessage("Escolha o local para a " + name);
        } else {
            gv.ui.showMessage("Faltam recursos! (" + woodReq + " Madeira)");
        }
    }

    private void tryCraftPokeballs(GameView gv) {
        Integer ferroObj = gv.player.getInventory().getOrDefault("Ferro", 0);
        Integer frutasObj = gv.player.getInventory().getOrDefault("Fruta", 0);
        int ferro = ferroObj != null ? ferroObj : 0;
        int frutas = frutasObj != null ? frutasObj : 0;
        
        if (ferro >= 5 && frutas >= 2) {
            for (int i = 0; i < 5; i++) gv.player.useItem("Ferro");
            for (int i = 0; i < 2; i++) gv.player.useItem("Fruta");
            for (int i = 0; i < 5; i++) gv.player.addItem("Pokébola");
            gv.ui.showMessage("🔴 5 POKÉBOLAS FABRICADAS!");
            gv.activity.saveCurrentGame();
        } else {
            gv.ui.showMessage("Recursos insuficientes (5 Ferro + 2 Fruta)");
        }
    }

    private void tryUpgradeTool(String toolName, String resourceName, GameView gv) {
        String key = toolName + "_Lv";
        int currentLv = gv.player.getInventory().getOrDefault(key, 1);
        int cost = 20; 

        Integer resObj = gv.player.getInventory().get(resourceName);
        int resAmount = resObj != null ? resObj : 0;

        if (resAmount >= cost) {
            for (int i = 0; i < cost; i++) gv.player.useItem(resourceName);
            gv.player.getInventory().put(key, currentLv + 1);
            gv.ui.showMessage("🛠️ " + toolName.toUpperCase() + " MELHORADO PARA LV." + (currentLv + 1) + "!");
            gv.activity.saveCurrentGame();
        } else {
            gv.ui.showMessage("Faltam " + resourceName + "! (Necessário: " + cost + ")");
        }
    }

    private void tryBuildStove(GameView gv) {
        if (gv.player.getInventory().getOrDefault("Ferro", 0) >= 15) {
            for (int i = 0; i < 15; i++) gv.player.useItem("Ferro");
            gv.player.addItem("Fogão");
            gv.ui.showMessage("🍳 FOGÃO CONSTRUÍDO!");
            gv.activity.saveCurrentGame();
        } else gv.ui.showMessage("Falta Ferro! (15)");
    }

    private void tryCook(GameView gv) {
        if (gv.player.getInventory().getOrDefault("Fruta", 0) >= 5) {
            for (int i = 0; i < 5; i++) gv.player.useItem("Fruta");
            gv.player.addItem("Doce de Fruta");
            gv.ui.showMessage("🍲 DOCE DE FRUTA COZINHADO!");
            gv.activity.saveCurrentGame();
        } else gv.ui.showMessage("Faltam Frutas! (5)");
    }

    private void tryCraftCape(GameView gv) {
        if (gv.player.getInventory().getOrDefault("Pelo", 0) >= 10) {
            for (int i = 0; i < 10; i++) gv.player.useItem("Pelo");
            gv.player.addItem("Capa Térmica");
            gv.ui.showMessage("🧣 CAPA TÉRMICA FABRICADA!");
            gv.activity.saveCurrentGame();
        } else gv.ui.showMessage("Falta Pelo! (10)");
    }
}
