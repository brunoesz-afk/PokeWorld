package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.PassManager;
import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.RewardManager;
import com.example.pokeworld.managers.WalletManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.PassUI;

public class PassInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        PassUI ui = (PassUI) gv.ui.getHandler(UIManager.UIState.PASS);
        RewardManager.Store s = PointsManager.store();
        if (ui == null || s == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }
        if (ui.shopBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.POINTS_SHOP; return true; }
        for (int i = 0; i < ui.tabs.length; i++) if (ui.tabs[i].contains(x, y)) { ui.page = i; return true; }

        if (ui.buyBtn.contains(x, y)) {
            if (PassManager.premium(s)) { gv.ui.showMessage("⭐ Você já tem o Passe Premium desta temporada."); return true; }
            int missing = PointsManager.PRICE_PASS - PointsManager.balance(s);
            if (missing > 0) { gv.ui.showMessage("💠 Faltam " + missing + " pontos para o Passe. Abra a LOJA e toque em DEPOSITAR."); return true; }
            if (BuyConfirm.confirm(gv, "pass", "Passe Premium", PointsManager.PRICE_PASS)) {
                WalletManager.purchase(gv, "pass:" + PassManager.season(), PointsManager.PRICE_PASS, () -> PassManager.buyPass(s), "⭐ PASSE PREMIUM ATIVO! Reivindique as recompensas dos níveis.");
            }
            return true;
        }
        if (ui.claimAllBtn.contains(x, y)) {
            int n = PassManager.claimAll(gv, s);
            gv.ui.showMessage(n == 0 ? "Nada para reivindicar agora." : "🎟️ " + n + " itens do passe reivindicados!");
            if (n > 0) gv.activity.saveCurrentGame();
            return true;
        }
        for (PassUI.Cell c : ui.levelCells) {
            if (!c.r.contains(x, y)) continue;
            if (c.flag && !PassManager.premium(s)) { gv.ui.showMessage("⭐ Trilha premium: compre o Passe para liberar."); return true; }
            boolean ok = c.flag ? PassManager.claimPrem(gv, s, c.a) : PassManager.claimFree(gv, s, c.a);
            if (ok) gv.activity.saveCurrentGame();
            else if (PassManager.level(s) < c.a) gv.ui.showMessage("🔒 Alcance o nível " + c.a + " (faltam " + (c.a * PassManager.XP_PER_LEVEL - PassManager.xp(s)) + " XP).");
            return true;
        }
        for (PassUI.Cell c : ui.missionCells) {
            if (!c.r.contains(x, y)) continue;
            boolean ok = c.a == 0 ? PassManager.claimDaily(gv, s, c.b) : PassManager.claimSeason(gv, s, c.b);
            if (ok) gv.activity.saveCurrentGame();
            return true;
        }
        return true;
    }
}
