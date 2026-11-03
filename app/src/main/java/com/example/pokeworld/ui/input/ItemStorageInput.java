package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ItemStorageUI;
import java.util.Map;

public class ItemStorageInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        ItemStorageUI ui = (ItemStorageUI) gv.ui.getHandler(UIManager.UIState.ITEM_STORAGE);
        
        if (ui.depositAllBtn.contains(x, y)) {
            for (Map.Entry<String, Integer> e : gv.player.getInventory().entrySet()) {
                String item = e.getKey();
                int count = e.getValue();
                gv.player.getStorageItems().put(item, gv.player.getStorageItems().getOrDefault(item, 0) + count);
            }
            gv.player.getInventory().clear();
            gv.ui.showMessage("📥 ITENS GUARDADOS!");
            gv.activity.saveCurrentGame();
            return true;
        } else if (ui.withdrawAllBtn.contains(x, y)) {
            for (Map.Entry<String, Integer> e : gv.player.getStorageItems().entrySet()) {
                String item = e.getKey();
                int count = e.getValue();
                gv.player.getInventory().put(item, gv.player.getInventory().getOrDefault(item, 0) + count);
            }
            gv.player.getStorageItems().clear();
            gv.ui.showMessage("📤 ITENS RECOLHIDOS!");
            gv.activity.saveCurrentGame();
            return true;
        }

        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
