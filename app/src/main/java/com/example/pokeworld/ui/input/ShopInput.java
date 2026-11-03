package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.BuildingData;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ShopUI;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class ShopInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        Map<String, BuildingData> buildings = gv.activity.dbHelper.getBuildings();
        int shopLv = buildings.containsKey("Loja") ? buildings.get("Loja").level : 1;
        ShopUI ui = (ShopUI) gv.ui.getHandler(UIManager.UIState.SHOP);

        if (ui.buyPotionBtn.contains(x, y)) buy(gv, "Poção", gv.kingdom.calculateDiscountedPrice(50, shopLv));
        else if (ui.buyCrystalBtn.contains(x, y)) buy(gv, "Cristal", gv.kingdom.calculateDiscountedPrice(200, shopLv));
        else if (ui.buyBallBtn.contains(x, y)) buy(gv, "Pokébola", 100);
        else if (ui.buyGreatBallBtn.contains(x, y)) buy(gv, "Great Ball", 300);
        else if (ui.buyUltraBallBtn.contains(x, y)) buy(gv, "Ultra Ball", 800);
        else if (ui.buySmokeBtn.contains(x, y)) buy(gv, "Bomba Fumaça", 100);
        else if (ui.sellBtn.contains(x, y)) sellAllMaterials(gv);
        else gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }

    private void sellAllMaterials(GameView gv) {
        int totalGold = 0;
        Map<String, Integer> inv = gv.player.getInventory();
        
        // Lista expandida com os novos recursos dos biomas
        String[] materials = {
            "Madeira", "Ferro", "Pedra", "Fruta", 
            "Carvão", "Essência de Fogo", "Obsidiana",
            "Pérola", "Coral", "Cristal de Água",
            "Poeira Mística", "Fragmento Shiny",
            "Presa", "Gosma", "Ferrão", "Pelo", "Núcleo de Rocha", "Essência Vital"
        };

        List<String> toRemove = new ArrayList<>();
        for (String m : materials) {
            if (inv.containsKey(m)) {
                int count = inv.get(m);
                // Preço base 15, itens raros valem mais
                int price = 15;
                if (m.equals("Pérola") || m.equals("Obsidiana")) price = 50;
                if (m.equals("Fragmento Shiny")) price = 100;
                
                totalGold += count * price;
                toRemove.add(m);
            }
        }
        
        for (String m : toRemove) inv.remove(m);

        if (totalGold > 0) {
            gv.player.addGold(totalGold);
            gv.ui.showMessage("Vendeu tudo por " + totalGold + " 🪙");
            gv.activity.saveCurrentGame();
        } else {
            gv.ui.showMessage("Nada para vender!");
        }
    }

    private void buy(GameView gv, String item, int cost) {
        if (gv.player.getGold() >= cost) {
            gv.player.addGold(-cost);
            gv.player.addItem(item);
            gv.ui.showMessage("Comprou " + item);
            gv.activity.saveCurrentGame();
        } else gv.ui.showMessage("Ouro insuficiente!");
    }
}
