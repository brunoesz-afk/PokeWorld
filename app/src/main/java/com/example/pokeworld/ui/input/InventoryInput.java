package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.InventoryUI;

import java.util.Map;

public class InventoryInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        InventoryUI ui = (InventoryUI) gv.ui.getHandler(UIManager.UIState.INVENTORY);
        if (ui != null && ui.exitBtn.contains(x, y)) {
            gv.ui.state = UIManager.UIState.EXPLORING;
            return true;
        }

        float w = gv.getWidth();
        int cols = 3;
        float startX = 80;
        float startY = 150;
        float cardW = (w - 160 - (cols - 1) * 20) / cols;
        float cardH = 95;
        float spacing = 20;

        int index = 0;
        for (Map.Entry<String, Integer> entry : gv.player.getInventory().entrySet()) {
            String itemName = entry.getKey();
            int r = index / cols;
            int c = index % cols;

            float cx = startX + c * (cardW + spacing);
            float cy = startY + r * (cardH + spacing);

            android.graphics.RectF card = new android.graphics.RectF(cx, cy, cx + cardW, cy + cardH);
            if (card.contains(x, y)) {
                if (itemName.contains("Poção") || itemName.equals("Peixe")) {
                    gv.player.useItem(itemName);
                    gv.companion.healFull();
                    gv.ui.showMessage("❤️ Item usado: " + itemName + " (Pet Curado!)");
                } else if (itemName.contains("Bomba Fumaça")) {
                    gv.player.useItem("Bomba Fumaça");
                    gv.world.loadMap("CITY", gv.player, gv.companion);
                    gv.ui.showMessage("🌫️ Fugiu para a cidade!");
                } else if (itemName.contains("Pedra")) {
                    gv.player.useItem(itemName);
                    gv.companion.evolve(gv.player);
                    gv.ui.showMessage("✨ " + itemName.toUpperCase() + " USADA!");
                    gv.particles.addExplosion(gv.player.getPositionX(), gv.player.getPositionY(), android.graphics.Color.YELLOW, 60);
                } else if (itemName.contains("Fruta")) {
                    gv.player.useItem(itemName);
                    gv.companion.applyAttackBuff(1.25f, 30000);
                    gv.ui.showMessage("🍎 Buff de Ataque Ativado (30s)!");
                } else {
                    gv.ui.showMessage("📦 Item selecionado: " + itemName);
                }
                gv.activity.saveCurrentGame();
                return true;
            }
            index++;
        }

        return true;
    }
}
