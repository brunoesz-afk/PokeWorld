package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.ui.UIManager;

public class InventoryInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        if (y > 150 && y < 220 && gv.player.getInventory().containsKey("Poção")) {
            gv.player.useItem("Poção");
            gv.companion.healFull();
            gv.ui.showMessage("❤️ Pet Curado!");
        } else if (y > 240 && y < 310 && gv.player.getInventory().containsKey("Bomba Fumaça")) {
            gv.player.useItem("Bomba Fumaça");
            gv.world.loadMap("CITY", gv.player, gv.companion);
            gv.ui.showMessage("🌫️ Fugiu para a cidade!");
        } else if (y > 330 && y < 400 && gv.player.getInventory().containsKey("Poção Fúria")) {
            gv.player.useItem("Poção Fúria");
            gv.companion.addRage(50);
            gv.ui.showMessage("🔥 Fúria Aumentada!");
        } else if (y > 420 && y < 490 && (gv.player.getInventory().containsKey("Pedra do Trovão") || 
                gv.player.getInventory().containsKey("Pedra de Fogo") || 
                gv.player.getInventory().containsKey("Pedra de Água") || 
                gv.player.getInventory().containsKey("Pedra de Folha") ||
                gv.player.getInventory().containsKey("Pedra Lunar"))) {
            
            String stoneUsed = "";
            if (gv.player.getInventory().containsKey("Pedra do Trovão")) stoneUsed = "Pedra do Trovão";
            else if (gv.player.getInventory().containsKey("Pedra de Fogo")) stoneUsed = "Pedra de Fogo";
            else if (gv.player.getInventory().containsKey("Pedra de Água")) stoneUsed = "Pedra de Água";
            else if (gv.player.getInventory().containsKey("Pedra de Folha")) stoneUsed = "Pedra de Folha";
            else if (gv.player.getInventory().containsKey("Pedra Lunar")) stoneUsed = "Pedra Lunar";

            gv.player.useItem(stoneUsed);
            gv.companion.evolve(gv.player);
            gv.ui.showMessage("✨ " + stoneUsed.toUpperCase() + " USADA!");
            gv.particles.addExplosion(gv.player.getPositionX(), gv.player.getPositionY(), android.graphics.Color.YELLOW, 60);
            
        } else if (gv.player.getInventory().containsKey("Peixe")) {
            gv.player.useItem("Peixe");
            gv.companion.healFull();
            gv.ui.showMessage("🐟 Pet alimentado e curado!");
            gv.particles.addExplosion(gv.player.getPositionX(), gv.player.getPositionY(), android.graphics.Color.CYAN, 20);
        } else if (gv.player.getInventory().containsKey("Fruta")) {
            gv.player.useItem("Fruta");
            gv.companion.applyAttackBuff(1.25f, 30000);
            gv.ui.showMessage("🍎 Buff de Ataque Ativado (30s)!");
            gv.particles.addExplosion(gv.player.getPositionX(), gv.player.getPositionY(), android.graphics.Color.RED, 25);
        }
        gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }
}
