package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Rune;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.RuneUI;

public class RuneInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        RuneUI ui = (RuneUI) gv.ui.getHandler(UIManager.UIState.RUNES);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }

        for (int i = 0; i < ui.rowRects.size(); i++) {
            if (ui.rowRects.get(i).contains(x, y)) { ui.selected = Rune.ALL[i]; return true; }
        }

        Rune eq = gv.companion.getRune();
        if (ui.removeBtn.contains(x, y) && eq != null) {
            gv.player.addItem(eq.getName());
            gv.companion.setRune(null);
            gv.ui.showMessage("🔷 " + eq.getName() + " voltou para a mochila.");
            gv.activity.saveCurrentGame();
            return true;
        }
        if (ui.equipBtn.contains(x, y)) {
            if (eq != null && eq.getName().equals(ui.selected)) return true;
            if (gv.player.getInventory().getOrDefault(ui.selected, 0) <= 0) { gv.ui.showMessage("❌ Você não tem " + ui.selected + "."); return true; }
            gv.player.useItem(ui.selected);
            if (eq != null) gv.player.addItem(eq.getName());   // a runa antiga volta para a mochila
            gv.companion.setRune(Rune.create(ui.selected));
            gv.ui.showMessage("🔷 " + ui.selected + " equipada!");
            gv.activity.saveCurrentGame();
        }
        return true;
    }
}
