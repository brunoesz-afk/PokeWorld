package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.EvolutionTable;
import com.example.pokeworld.managers.MissionManager;
import com.example.pokeworld.managers.PartyManager;
import com.example.pokeworld.managers.SoundManager;
import com.example.pokeworld.managers.VibrationManager;
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

        if (!ui.boostBtn.isEmpty() && ui.boostBtn.contains(x, y) && ui.selected != null) {
            CapturedCreature c = ui.selected;
            if (c.boost >= CreatureUI.MAX_BOOST) { gv.ui.showMessage("Boost já está no máximo!"); return true; }
            if (gv.player.getInventory().getOrDefault("Gema de Boost", 0) <= 0) {
                gv.ui.showMessage("💎 Sem Gema de Boost. Ganhe na Torre, em chefes ou compre no Mercado."); return true;
            }
            gv.player.useItem("Gema de Boost");
            if (Math.random() * 100 < CreatureUI.boostChance(c.boost)) {
                c.boost++;
                gv.missions.count(MissionManager.Metric.BOOSTS);
                SoundManager.play("boost");
                gv.ui.showMessage("💎 " + c.name + " subiu para +" + c.boost + "! (+8% ataque e vida por nível)");
                VibrationManager.tap(40);
            } else {
                SoundManager.play("hit");
                gv.ui.showMessage("💨 A gema se desfez… " + c.name + " continua em +" + c.boost + ". Tente de novo!");
            }
            gv.activity.saveCurrentGame();
            return true;
        }
        if (!ui.fuseBtn.isEmpty() && ui.fuseBtn.contains(x, y) && ui.selected != null) {
            CapturedCreature c = ui.selected;
            if (gv.fusion.attemptUpgrade(c.name, c.rarity, c.isShiny, gv.companion.getSpecies())) {
                gv.missions.count(MissionManager.Metric.FUSIONS);
                ui.selected = null;
                gv.activity.saveCurrentGame();
            }
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
