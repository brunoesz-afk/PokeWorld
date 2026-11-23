package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.EvolutionTable;
import com.example.pokeworld.managers.MissionManager;
import com.example.pokeworld.managers.PartyManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.CreatureUI;

public class CreatureInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        CreatureUI ui = (CreatureUI) gv.ui.getHandler(UIManager.UIState.CREATURES);
        if (ui == null) return false;

        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }

        for (CreatureUI.EvoBtn eb : ui.evoButtons) {
            if (!eb.rect.contains(x, y) || ui.selected == null) continue;
            CapturedCreature c = ui.selected;
            EvolutionTable.Evo evo = eb.evo;
            if (!ui.canEvolve(gv, c, evo)) return true;
            if (evo.stone != null) {
                gv.player.useItem(evo.stone);   // a pedra só é gasta quando o jogador escolhe evoluir
                gv.missions.count(MissionManager.Metric.STONES_USED);
            }
            String old = c.name;
            PartyManager.evolveCreature(gv, c, evo.to);   // o ativo passa pela cena de evolução
            ui.refreshCache();
            if (gv.ui.state == UIManager.UIState.CREATURES) gv.ui.showMessage("✨ " + old + " evoluiu para " + evo.to + "!");
            return true;
        }

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
