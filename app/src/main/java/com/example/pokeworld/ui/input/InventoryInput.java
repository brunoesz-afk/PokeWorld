package com.example.pokeworld.ui.input;

import android.graphics.Color;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.ItemCatalog;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.InventoryUI;

public class InventoryInput extends TapScrollInput {
    @Override
    protected void onScroll(float x, float y, float dy, GameView gv) {
        InventoryUI ui = (InventoryUI) gv.ui.getHandler(UIManager.UIState.INVENTORY);
        if (ui != null && ui.gridArea.contains(x, y)) ui.scroll = Math.max(0, Math.min(ui.maxScroll, ui.scroll - dy));
    }

    @Override
    protected boolean onTap(float x, float y, GameView gv) {
        InventoryUI ui = (InventoryUI) gv.ui.getHandler(UIManager.UIState.INVENTORY);
        if (ui == null) return false;

        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }

        for (int i = 0; i < ui.tabRects.size(); i++) {
            if (ui.tabRects.get(i).contains(x, y)) {
                int t = i - 1;
                if (ui.tab != t) { ui.tab = t; ui.scroll = 0; ui.selected = null; }
                return true;
            }
        }
        for (InventoryUI.Cell c : ui.cells) {
            if (ui.gridArea.contains(x, y) && c.rect.contains(x, y)) { ui.selected = c.name; return true; }
        }
        if (!ui.useBtn.isEmpty() && ui.useBtn.contains(x, y) && ui.selected != null) {
            useItem(gv, ui.selected);
        }
        return true;
    }

    private void useItem(GameView gv, String item) {
        ItemCatalog.Info info = ItemCatalog.get(item);
        if (info.useLabel == null) {
            gv.ui.showMessage("ℹ️ " + item + ": sem uso direto. Veja a descrição.");
            return;
        }
        if (item.equals("Poção") || item.equals("Peixe")) {
            if (gv.companion.getHp() >= gv.companion.getMaxHp()) { gv.ui.showMessage("❤️ O companheiro já está com a vida cheia."); return; }
            gv.player.useItem(item);
            gv.companion.healFull();
            gv.ui.showMessage("❤️ " + item + " usada: companheiro curado!");
        } else if (item.equals("Bomba Fumaça")) {
            gv.player.useItem(item);
            gv.world.loadMap("CITY", gv.player, gv.companion);
            gv.ui.state = UIManager.UIState.EXPLORING;
            gv.ui.showMessage("🌫️ Fugiu para a cidade!");
        } else if (ItemCatalog.isStone(item)) {
            // A tela de evolução escolhe o Pokémon e aplica a pedra.
            gv.ui.openEvolutionStone(item);
            return;
        } else if (item.equals("Fruta")) {
            gv.player.useItem(item);
            gv.companion.applyAttackBuff(1.25f, 30000);
            gv.ui.showMessage("🍎 Buff de ataque ativado (30s)!");
        } else if (item.equals("Doce de Fruta")) {
            gv.player.useItem(item);
            gv.player.eat(40);
            gv.ui.showMessage("🍬 Fome recuperada!");
        } else if (item.startsWith("Runa")) {
            gv.ui.state = UIManager.UIState.RUNES;
            return;
        }
        gv.activity.saveCurrentGame();
    }
}
