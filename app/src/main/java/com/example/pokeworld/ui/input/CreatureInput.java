package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.PartyManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.CreatureUI;

public class CreatureInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        CreatureUI ui = (CreatureUI) gv.ui.getHandler(UIManager.UIState.CREATURES);
        if (ui == null) return false;

        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }

        for (CreatureUI.Slot s : ui.slots) {
            if (s.rect.contains(x, y)) { ui.selected = s.creature; return true; }
        }
        if (!ui.activateBtn.isEmpty() && ui.activateBtn.contains(x, y) && ui.selected != null) {
            if (PartyManager.switchActive(gv, ui.selected)) gv.ui.showMessage("🐾 Companheiro ativo: " + ui.selected.name);
            return true;
        }
        if (ui.manageBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.STORAGE; return true; }
        return true;
    }
}
