package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.MountFactoryUI;

public class MountFactoryInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        MountFactoryUI ui = (MountFactoryUI) gv.ui.getHandler(UIManager.UIState.MOUNT_FACTORY);
        
        if (ui.upgradeSpeed.contains(x, y)) {
            int cost = 50;
            if (gv.player.getInventory().getOrDefault("Madeira", 0) >= cost) {
                for(int i=0; i<cost; i++) gv.player.useItem("Madeira");
                int lv = gv.player.getInventory().getOrDefault("MountSpeed_Lv", 1);
                gv.player.getInventory().put("MountSpeed_Lv", lv + 1);
                gv.ui.showMessage("🏇 VELOCIDADE AUMENTADA!");
                gv.activity.saveCurrentGame();
            } else gv.ui.showMessage("Falta Madeira!");
            return true;
        } else if (ui.saddleFire.contains(x, y)) {
            craft(gv, "Sela de Fogo", "Essência de Fogo", 30);
            return true;
        } else if (ui.saddleWater.contains(x, y)) {
            craft(gv, "Sela Aquática", "Cristal de Água", 30);
            return true;
        }

        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }

    private void craft(GameView gv, String item, String res, int cost) {
        if (gv.player.getInventory().getOrDefault(res, 0) >= cost) {
            for(int i=0; i<cost; i++) gv.player.useItem(res);
            gv.player.addItem(item);
            gv.ui.showMessage("🔨 FABRICADO: " + item);
            gv.activity.saveCurrentGame();
        } else gv.ui.showMessage("Faltam recursos!");
    }
}
