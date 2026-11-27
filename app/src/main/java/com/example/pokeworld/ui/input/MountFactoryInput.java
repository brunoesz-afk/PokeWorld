package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.MountFactoryUI;

public class MountFactoryInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        MountFactoryUI ui = (MountFactoryUI) gv.ui.getHandler(UIManager.UIState.MOUNT_FACTORY);
        
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }

        if (ui.upgradeSpeed.contains(x, y)) {
            int cost = MountFactoryUI.SPEED_COST;
            if (gv.player.getInventory().getOrDefault("Madeira", 0) >= cost) {
                for(int i=0; i<cost; i++) gv.player.useItem("Madeira");
                int lv = gv.player.getInventory().getOrDefault("MountSpeed_Lv", 1);
                gv.player.getInventory().put("MountSpeed_Lv", lv + 1);
                gv.ui.showMessage("🏇 VELOCIDADE AUMENTADA!");
                gv.activity.saveCurrentGame();
            } else gv.ui.showMessage("Falta Madeira!");
            return true;
        } else if (ui.saddleFire.contains(x, y)) {
            if (gv.player.getInventory().getOrDefault("Sela de Fogo", 0) > 0) gv.ui.showMessage("Você já tem a Sela de Fogo."); else craft(gv, "Sela de Fogo", "Essência de Fogo", MountFactoryUI.SADDLE_COST);
            return true;
        } else if (ui.saddleWater.contains(x, y)) {
            if (gv.player.getInventory().getOrDefault("Sela Aquática", 0) > 0) gv.ui.showMessage("Você já tem a Sela Aquática."); else craft(gv, "Sela Aquática", "Cristal de Água", MountFactoryUI.SADDLE_COST);
            return true;
        }

        return true;   // toque fora dos botões não fecha mais (a saída é o botão SAIR)
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
