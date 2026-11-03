package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.ui.UIManager;

import java.util.List;

public class StorageInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        float mid = gv.getWidth() / 2f;
        
        if (y > 200 && y < gv.getHeight() - 100) {
            int index = (int)((y - 230) / 45);
            
            if (x < mid) {
                List<CapturedCreature> storage = gv.player.getStorage();
                if (index >= 0 && index < storage.size()) {
                    if (gv.player.getParty().size() < 6) {
                        storage.get(index).isInParty = true;
                        gv.ui.showMessage(storage.get(index).name + " entrou no TIME!");
                        gv.applyKingdomBonuses();
                    } else {
                        gv.ui.showMessage("Time cheio! Remova alguém primeiro.");
                    }
                }
            } else {
                List<CapturedCreature> party = gv.player.getParty();
                if (index >= 0 && index < party.size()) {
                    if (party.size() > 1) {
                        party.get(index).isInParty = false;
                        gv.ui.showMessage(party.get(index).name + " enviado para a BASE!");
                        gv.applyKingdomBonuses();
                    } else {
                        gv.ui.showMessage("Você precisa de pelo menos 1 no time!");
                    }
                }
            }
        } else if (x < 50 || x > gv.getWidth() - 50 || y < 50 || y > gv.getHeight() - 50) {
            gv.ui.state = UIManager.UIState.EXPLORING;
        }
        return true;
    }
}
