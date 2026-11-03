package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Rune;
import com.example.pokeworld.ui.UIManager;

import java.util.Map;

public class RuneInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        int index = (int)((y - 200) / 50);
        Map<String, Integer> inv = gv.player.getInventory();
        int count = 0;
        for (String item : inv.keySet()) {
            if (item.contains("Runa")) {
                if (count == index) {
                    gv.companion.setRune(Rune.create(item));
                    gv.ui.showMessage("🔷 Runa Equipada!");
                    gv.ui.state = UIManager.UIState.EXPLORING;
                    return true;
                }
                count++;
            }
        }
        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
