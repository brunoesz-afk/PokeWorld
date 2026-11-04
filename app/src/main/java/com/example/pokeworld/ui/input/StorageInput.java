package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.StorageUI;

import java.util.List;

public class StorageInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        StorageUI ui = (StorageUI) gv.ui.getHandler(UIManager.UIState.STORAGE);
        if (ui != null && ui.exitBtn.contains(x, y)) {
            gv.ui.state = UIManager.UIState.EXPLORING;
            return true;
        }

        float mid = gv.getWidth() / 2f;
        
        if (y >= 180 && y < gv.getHeight() - 60) {
            int index = (int)((y - 180) / 60);
            
            if (x < mid) {
                // Toque na lista da BASE -> Move para o TIME (PARTY)
                List<CapturedCreature> storage = gv.player.getStorage();
                if (index >= 0 && index < storage.size()) {
                    if (gv.player.getParty().size() < 6) {
                        CapturedCreature c = storage.get(index);
                        c.isInParty = true;
                        gv.ui.showMessage("🎒 " + c.name + " adicionado ao seu TIME!");
                        gv.applyKingdomBonuses();
                        gv.activity.saveCurrentGame();
                    } else {
                        gv.ui.showMessage("❌ Seu time está cheio (Máximo 6)!");
                    }
                }
            } else {
                // Toque na lista do TIME -> Move para a BASE
                List<CapturedCreature> party = gv.player.getParty();
                if (index >= 0 && index < party.size()) {
                    if (party.size() > 1) {
                        CapturedCreature c = party.get(index);
                        c.isInParty = false;
                        gv.ui.showMessage("🏠 " + c.name + " guardado na BASE!");
                        gv.applyKingdomBonuses();
                        gv.activity.saveCurrentGame();
                    } else {
                        gv.ui.showMessage("❌ Mantenha pelo menos 1 Pokémon no time!");
                    }
                }
            }
        }
        return true;
    }
}
