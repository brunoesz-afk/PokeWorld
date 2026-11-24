package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.ShopCatalog;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ShopUI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ShopInput extends TapScrollInput {
    @Override
    protected void onScroll(float x, float y, float dy, GameView gv) {
        ShopUI ui = (ShopUI) gv.ui.getHandler(UIManager.UIState.SHOP);
        if (ui != null && ui.listArea.contains(x, y)) ui.scroll = Math.max(0, Math.min(ui.maxScroll, ui.scroll - dy));
    }

    @Override
    protected boolean onTap(float x, float y, GameView gv) {
        ShopUI ui = (ShopUI) gv.ui.getHandler(UIManager.UIState.SHOP);
        if (ui == null) return false;
        if (ui.exitBtn.contains(x, y)) { gv.ui.state = UIManager.UIState.EXPLORING; return true; }

        for (int i = 0; i < ui.tabRects.size(); i++) {
            if (ui.tabRects.get(i).contains(x, y)) {
                boolean sell = i == 1;
                if (sell != ui.selling) { ui.selling = sell; ui.qtyIndex = 0; ui.scroll = 0; }
                return true;
            }
        }
        if (ui.listArea.contains(x, y)) {
            for (int i = 0; i < ui.rowRects.size(); i++) {
                if (!ui.rowRects.get(i).contains(x, y)) continue;
                if (ui.selling) ui.sellItem = ui.rowKeys.get(i); else ui.buyIndex = Integer.parseInt(ui.rowKeys.get(i));
                return true;
            }
            return true;
        }
        for (int i = 0; i < ui.qtyRects.size(); i++) {
            if (ui.qtyRects.get(i).contains(x, y)) { ui.qtyIndex = i; return true; }
        }
        if (!ui.scrapBtn.isEmpty() && ui.scrapBtn.contains(x, y)) { sellScrap(gv, ui); return true; }
        if (!ui.actionBtn.isEmpty() && ui.actionBtn.contains(x, y)) {
            if (ui.selling) sell(gv, ui); else buy(gv, ui);
            return true;
        }
        return true;
    }

    private void buy(GameView gv, ShopUI ui) {
        ShopCatalog.Offer o = ShopCatalog.BUY.get(ui.buyIndex);
        int price = ShopCatalog.buyPrice(o, gv.kingdom, ui.shopLevel(gv));
        int want = ui.qty(0);
        int can = price > 0 ? gv.player.getGold() / price : 0;
        if (can <= 0) { gv.ui.showMessage("Ouro insuficiente para " + o.name + "!"); return; }
        int n = Math.min(want, can);
        gv.player.addGold(-price * n);
        gv.player.addItem(o.name, n);
        com.example.pokeworld.managers.SoundManager.play("buy");
        gv.ui.showMessage("🛒 Comprou " + n + "x " + o.name + " por " + (price * n) + " 🪙" + (n < want ? " (era o que dava com seu ouro)" : ""));
        gv.activity.saveCurrentGame();
    }

    private void sell(GameView gv, ShopUI ui) {
        if (ui.sellItem == null) return;
        Map<String, Integer> inv = gv.player.getInventory();
        int owned = inv.getOrDefault(ui.sellItem, 0);
        int n = Math.min(owned, ui.qty(owned));
        if (n <= 0) return;
        int gold = ShopCatalog.sellPrice(ui.sellItem) * n;
        if (n >= owned) inv.remove(ui.sellItem); else inv.put(ui.sellItem, owned - n);
        gv.player.addGold(gold);
        gv.ui.showMessage("💰 Vendeu " + n + "x " + ui.sellItem + " por " + gold + " 🪙");
        gv.activity.saveCurrentGame();
    }

    /** Vende tudo o que o Mercador compra, menos o que a base/oficina usa (madeira, ferro, pedra, fruta, pelo). */
    private void sellScrap(GameView gv, ShopUI ui) {
        Map<String, Integer> inv = gv.player.getInventory();
        int total = 0;
        List<String> remove = new ArrayList<>();
        for (Map.Entry<String, Integer> e : inv.entrySet()) {
            if (ShopCatalog.isKept(e.getKey())) continue;
            int p = ShopCatalog.sellPrice(e.getKey());
            if (p > 0 && e.getValue() > 0) { total += p * e.getValue(); remove.add(e.getKey()); }
        }
        if (total <= 0) { gv.ui.showMessage("Nenhuma sucata para vender."); return; }
        for (String k : remove) inv.remove(k);
        gv.player.addGold(total);
        gv.ui.showMessage("💰 Sucata vendida por " + total + " 🪙 (madeira, ferro, pedra, fruta e pelo foram mantidos)");
        gv.activity.saveCurrentGame();
    }
}
