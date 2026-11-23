package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.TalentTree;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.TalentUI;

public class TalentInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        TalentUI ui = (TalentUI) gv.ui.getHandler(UIManager.UIState.TALENTS);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }

        for (TalentUI.NodeBox b : ui.boxes) {
            if (b.rect.contains(x, y)) { ui.selected = b.node.key; return true; }
        }

        if (ui.upgradeBtn.contains(x, y)) {
            TalentTree.Node n = TalentTree.get(ui.selected);
            if (n != null && TalentTree.canUpgrade(gv.player, n)) {
                gv.player.useTalentPoint(n.key);
                gv.applyKingdomBonuses();   // recalcula os status do companheiro com o novo talento
                gv.ui.showMessage("🌟 " + n.name + " nível " + TalentTree.level(gv.player, n.key));
                gv.activity.saveCurrentGame();
            } else if (n != null && !TalentTree.isUnlocked(gv.player, n)) {
                gv.ui.showMessage("🔒 " + TalentTree.requirementText(n));
            }
            return true;
        }
        if (ui.resetBtn.contains(x, y)) {
            if (TalentTree.spentPoints(gv.player) == 0) { gv.ui.showMessage("Você ainda não gastou pontos."); }
            else if (gv.player.getGold() < TalentUI.RESET_COST) { gv.ui.showMessage("❌ Precisa de " + TalentUI.RESET_COST + " de ouro para resetar."); }
            else {
                gv.player.addGold(-TalentUI.RESET_COST);
                int back = gv.player.refundTalents();
                gv.applyKingdomBonuses();
                gv.ui.showMessage("↺ " + back + " pontos devolvidos!");
                gv.activity.saveCurrentGame();
            }
        }
        return true;
    }
}
