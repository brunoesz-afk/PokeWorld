package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.EvolutionTable;
import com.example.pokeworld.managers.PartyManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.EvolutionItemsUI;

public class EvolutionItemsInput extends TapScrollInput {
    @Override
    protected void onScroll(float x, float y, float dy, GameView gv) {
        EvolutionItemsUI ui = (EvolutionItemsUI) gv.ui.getHandler(UIManager.UIState.EVOLUTION);
        if (ui != null && ui.listArea.contains(x, y)) ui.scroll = Math.max(0, Math.min(ui.maxScroll, ui.scroll - dy));
    }

    @Override
    protected boolean onTap(float x, float y, GameView gv) {
        EvolutionItemsUI ui = (EvolutionItemsUI) gv.ui.getHandler(UIManager.UIState.EVOLUTION);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.INVENTORY; return true; }

        for (EvolutionItemsUI.Row r : ui.rows) {
            if (ui.listArea.contains(x, y) && r.rect.contains(x, y)) { ui.selected = r.creature; return true; }
        }
        if (!ui.evolveBtn.isEmpty() && ui.evolveBtn.contains(x, y) && ui.selected != null) {
            EvolutionTable.Evo evo = EvolutionTable.byStone(ui.selected.name, ui.stone);
            if (evo == null) return true;
            if (gv.player.getInventory().getOrDefault(ui.stone, 0) <= 0) { gv.ui.showMessage("Você não tem " + ui.stone + "!"); return true; }
            gv.player.useItem(ui.stone);
            com.example.pokeworld.entities.CapturedCreature c = ui.selected;
            String old = c.name;
            ui.selected = null;
            PartyManager.evolveCreature(gv, c, evo.to);
            if (gv.ui.state == UIManager.UIState.EVOLUTION) gv.ui.showMessage("✨ " + old + " evoluiu para " + evo.to + "!");
        }
        return true;
    }
}
