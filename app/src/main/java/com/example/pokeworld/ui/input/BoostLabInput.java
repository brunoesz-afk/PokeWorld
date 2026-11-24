package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.managers.SoundManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.BoostLabUI;

public class BoostLabInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        BoostLabUI ui = (BoostLabUI) gv.ui.getHandler(UIManager.UIState.BOOSTLAB);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { ui.source = null; ui.target = null; gv.ui.state = UIManager.UIState.EXPLORING; return true; }
        if (!ui.prevBtn.isEmpty() && ui.prevBtn.contains(x, y)) { ui.page = Math.max(0, ui.page - 1); return true; }
        if (!ui.nextBtn.isEmpty() && ui.nextBtn.contains(x, y)) { ui.page++; return true; }

        if (!ui.resetBtn.isEmpty() && ui.resetBtn.contains(x, y) && ui.source != null) {
            CapturedCreature s = ui.source;
            int cost = BoostLabUI.resetCost(s);
            if (gv.player.getGold() < cost) { gv.ui.showMessage("Ouro insuficiente (" + cost + " 🪙)."); return true; }
            gv.player.addGold(-cost);
            int gems = BoostLabUI.refund(s);
            if (gems > 0) gv.player.addItem("Gema de Boost", gems);
            gv.ui.showMessage("♻ Boost de " + s.name + " reciclado: +" + gems + " 💎 Gema de Boost");
            s.boost = 0; ui.source = null; ui.target = null;
            SoundManager.play("boost");
            gv.activity.saveCurrentGame();
            return true;
        }
        if (!ui.transferBtn.isEmpty() && ui.transferBtn.contains(x, y) && ui.source != null) {
            CapturedCreature s = ui.source, t = ui.target;
            if (t == null) { gv.ui.showMessage("Escolha quem vai receber o boost."); return true; }
            int res = BoostLabUI.transferResult(s);
            if (res <= t.boost) { gv.ui.showMessage("O destino já tem boost igual ou maior que o resultado (+" + res + ")."); return true; }
            int cost = BoostLabUI.transferCost(s);
            if (gv.player.getGold() < cost) { gv.ui.showMessage("Ouro insuficiente (" + cost + " 🪙)."); return true; }
            gv.player.addGold(-cost);
            t.boost = res; s.boost = 0;
            gv.ui.showMessage("💎 Boost movido: " + s.name + " ➜ " + t.name + " (+" + res + ")");
            ui.source = null; ui.target = null;
            SoundManager.play("boost");
            gv.activity.saveCurrentGame();
            return true;
        }
        for (int i = 0; i < ui.cardRects.size(); i++) {
            if (!ui.cardRects.get(i).contains(x, y)) continue;
            CapturedCreature c = ui.cards.get(i);
            if (ui.source == null) {
                if (c.boost <= 0) { gv.ui.showMessage("Escolha primeiro um Pokémon que tenha boost (💎)."); return true; }
                ui.source = c;
            } else if (c == ui.source) { ui.source = null; ui.target = null; }
            else ui.target = c;
            return true;
        }
        return true;
    }
}
