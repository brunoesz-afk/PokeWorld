package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.PassManager;
import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.RewardManager;
import com.example.pokeworld.managers.SkinManager;
import com.example.pokeworld.managers.WalletManager;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ShopUI;

public class ShopInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        ShopUI ui = (ShopUI) gv.ui.getHandler(UIManager.UIState.POINTS_SHOP);
        RewardManager.Store s = PointsManager.store();
        if (ui == null || s == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }
        for (int i = 0; i < ui.tabs.length; i++) if (ui.tabs[i].contains(x, y)) { ui.tab = i; return true; }

        if (ui.depositBtn.contains(x, y)) {
            if (PointsManager.TEST_MODE) { PointsManager.add(s, PointsManager.TEST_GRANT); gv.ui.showMessage("💠 +" + PointsManager.TEST_GRANT + " pontos (modo de teste)"); gv.activity.saveCurrentGame(); }
            else { com.example.pokeworld.managers.WalletManager.refresh(true); gv.ui.state = UIManager.UIState.DEPOSIT; }
            return true;
        }
        if (ui.tab == 0) {
            if (ui.vipBtn.contains(x, y)) { buy(gv, s, "vip", "VIP (30 dias)", PointsManager.PRICE_VIP); return true; }
            if (ui.passBtn.contains(x, y)) {
                if (PassManager.premium(s)) { gv.ui.state = UIManager.UIState.PASS; return true; }
                buy(gv, s, "pass", "Passe Premium", PointsManager.PRICE_PASS);
                return true;
            }
            return true;
        }
        for (ShopUI.SkinCard c : ui.skinCards) {
            if (!c.btn.contains(x, y)) continue;
            SkinManager.Skin k = c.skin;
            if (SkinManager.owned(s, k)) {
                boolean on = SkinManager.toggle(s, k);
                gv.ui.showMessage(on ? "✨ Skin " + k.name + " equipada!" : "Skin removida.");
                gv.activity.saveCurrentGame();
            } else if (!k.forSale()) {
                gv.ui.showMessage("🔒 Skin exclusiva: " + k.source + ".");
            } else if (PointsManager.balance(s) < k.price) {
                gv.ui.showMessage("💠 Faltam " + (k.price - PointsManager.balance(s)) + " pontos para a skin " + k.name + ".");
            } else if (BuyConfirm.confirm(gv, "skin_" + k.id, "Skin " + k.name, k.price)) {
                final SkinManager.Skin sk = k;
                WalletManager.purchase(gv, "skin:" + k.id, k.price, () -> { if (SkinManager.buy(s, sk) == 0) SkinManager.toggle(s, sk); }, "✨ Skin " + k.name + " comprada e equipada!");
            }
            return true;
        }
        return true;
    }

    private void buy(GameView gv, RewardManager.Store s, String what, String label, int price) {
        int missing = price - PointsManager.balance(s);
        if (missing > 0) { gv.ui.showMessage("💠 Faltam " + missing + " pontos para " + label + "."); return; }
        if (!BuyConfirm.confirm(gv, what, label, price)) return;
        boolean vip = what.equals("vip");
        WalletManager.purchase(gv, vip ? "vip" : "pass:" + PassManager.season(), price,
                () -> { if (vip) PointsManager.buyVip(s); else PassManager.buyPass(s); },
                vip ? "👑 VIP ativo por 30 dias!" : "⭐ PASSE PREMIUM ATIVO!");
    }
}
