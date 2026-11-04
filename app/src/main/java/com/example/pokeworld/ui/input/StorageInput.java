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
        if (ui == null) return false;

        if (ui.exitBtn.contains(x, y)) {
            gv.ui.state = UIManager.UIState.EXPLORING;
            return true;
        }

        if (ui.prevPageBtn.contains(x, y)) {
            if (ui.storagePage > 0) ui.storagePage--;
            return true;
        }

        if (ui.nextPageBtn.contains(x, y)) {
            ui.storagePage++;
            return true;
        }

        float mid = gv.getWidth() / 2f;
        
        if (y >= 170 && y < gv.getHeight() - 80) {
            int lineIndex = (int)((y - 170) / 58);
            
            if (x < mid) {
                // Toque na lista da BASE -> Move para o TIME (PARTY)
                List<CapturedCreature> storage = gv.player.getStorage();
                int actualIndex = (ui.storagePage * StorageUI.ITEMS_PER_PAGE) + lineIndex;

                if (actualIndex >= 0 && actualIndex < storage.size()) {
                    if (gv.player.getParty().size() < 6) {
                        CapturedCreature c = storage.get(actualIndex);
                        c.isInParty = true;
                        gv.ui.showMessage("🎒 " + c.name + " adicionado ao seu TIME!");
                        gv.applyKingdomBonuses();
                        gv.activity.saveCurrentGame();
                    } else {
                        gv.ui.showMessage("❌ Seu time está cheio (Máximo 6)!");
                    }
                }
            } else {
                // Toque na lista do TIME -> Move para a BASE (GUARDA)
                List<CapturedCreature> party = gv.player.getParty();
                if (lineIndex >= 0 && lineIndex < party.size()) {
                    if (party.size() > 1) {
                        int currentGuards = gv.player.getStorage().size();
                        int maxGuards = gv.player.getMaxGuardCapacity();
                        if (currentGuards >= maxGuards) {
                            gv.ui.showMessage("❌ Limite de Guardas na Base atingido (Máx: " + maxGuards + " para o Nível " + gv.player.getLevel() + ")!");
                            return true;
                        }

                        CapturedCreature c = party.get(lineIndex);
                        c.isInParty = false;
                        c.isGuard = true; // Ativa modo de guarda e proteção na base
                        gv.ui.showMessage("🛡️ " + c.name + " guardado na BASE em modo GUARDA!");
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
