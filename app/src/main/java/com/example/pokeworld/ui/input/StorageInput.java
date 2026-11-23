package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.managers.PartyManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.StorageUI;

import java.util.List;

public class StorageInput extends TapScrollInput {
    @Override
    protected void onScroll(float x, float y, float dy, GameView gv) {
        StorageUI ui = (StorageUI) gv.ui.getHandler(UIManager.UIState.STORAGE);
        if (ui == null) return;
        for (int i = 0; i < 3; i++) {
            if (ui.colRects[i].contains(x, y)) ui.scroll[i] = Math.max(0, Math.min(ui.maxScroll[i], ui.scroll[i] - dy));
        }
    }

    @Override
    protected boolean onTap(float x, float y, GameView gv) {
        StorageUI ui = (StorageUI) gv.ui.getHandler(UIManager.UIState.STORAGE);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }

        for (StorageUI.Row r : ui.rows) {
            if (ui.colRects[r.col].contains(x, y) && r.rect.contains(x, y)) { ui.selected = r.creature; return true; }
        }
        if (ui.selected == null) return true;
        if (ui.toBoxBtn.contains(x, y)) move(gv, ui, StorageUI.COL_BOX);
        else if (ui.toDefBtn.contains(x, y)) move(gv, ui, StorageUI.COL_DEF);
        else if (ui.toTeamBtn.contains(x, y)) move(gv, ui, StorageUI.COL_TEAM);
        return true;
    }

    private void move(GameView gv, StorageUI ui, int target) {
        CapturedCreature c = ui.selected;
        int from = ui.columnOf(c);
        if (from == target) return;
        List<CapturedCreature> party = gv.player.getParty();

        if (target == StorageUI.COL_TEAM && party.size() >= 6) { gv.ui.showMessage("❌ Seu time está cheio (máximo 6)!"); return; }
        if (target == StorageUI.COL_DEF && gv.player.getDefenders().size() >= ui.capacity) {
            gv.ui.showMessage("❌ Defesa cheia (" + ui.capacity + ")! Construa mais na base ou suba o Quartel.");
            return;
        }
        if (from == StorageUI.COL_TEAM) {
            if (party.size() <= 1) { gv.ui.showMessage("❌ Mantenha pelo menos 1 Pokémon no time!"); return; }
            // Se for o companheiro ativo, passa o posto para outro do time antes de sair
            if (PartyManager.isActive(gv, c)) {
                for (CapturedCreature other : party) if (other != c) { PartyManager.switchActive(gv, other); break; }
            }
        }

        c.isInParty = target == StorageUI.COL_TEAM;
        c.isGuard = target == StorageUI.COL_DEF;
        String msg = target == StorageUI.COL_TEAM ? "🎒 " + c.name + " vai com você!"
                : target == StorageUI.COL_DEF ? "🛡️ " + c.name + " agora defende a base!" : "📦 " + c.name + " guardado na caixa.";
        gv.ui.showMessage(msg);
        gv.applyKingdomBonuses();
        gv.world.refreshGuards(gv.player);
        gv.activity.saveCurrentGame();
    }
}
