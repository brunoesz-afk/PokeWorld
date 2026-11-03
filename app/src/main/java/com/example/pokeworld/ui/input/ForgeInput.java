package com.example.pokeworld.ui.input;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Equipment;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.ui.handlers.ForgeUI;

import java.util.Map;

public class ForgeInput implements InputExtension {
    @Override
    public boolean handle(float x, float y, GameView gv) {
        ForgeUI ui = (ForgeUI) gv.ui.getHandler(UIManager.UIState.FORGE);
        if (ui.upgradeAttackBtn.contains(x, y)) {
            if (tryConsumeMaterials(gv, 5)) {
                gv.companion.addForgeAttackBonus(10);
                gv.ui.showMessage("⚔️ Ataque Aumentado!");
                gv.particles.addExplosion(gv.companion.getPositionX(), gv.companion.getPositionY(), android.graphics.Color.GRAY, 20);
            } else gv.ui.showMessage("❌ Materiais insuficientes!");
        } else if (ui.craftVitalBtn.contains(x, y)) {
            if (gv.player.getInventory().getOrDefault("Essência Vital", 0) > 0) {
                gv.player.useItem("Essência Vital");
                gv.companion.setWeapon(Equipment.create("Lâmina Vital"));
                gv.ui.showMessage("⚔️ Lâmina Vital Forjada!");
            }
        } else if (ui.craftRockBtn.contains(x, y)) {
            if (gv.player.getInventory().getOrDefault("Núcleo de Rocha", 0) > 0) {
                gv.player.useItem("Núcleo de Rocha");
                gv.companion.setArmor(Equipment.create("Armadura de Rocha"));
                gv.ui.showMessage("🛡️ Armadura de Rocha Forjada!");
            }
        } else if (ui.craftSolarBtn.contains(x, y)) {
            if (gv.player.getInventory().getOrDefault("Relíquia Dourada", 0) > 0) {
                gv.player.useItem("Relíquia Dourada");
                gv.companion.setWeapon(Equipment.create("Cajado Solar"));
                gv.ui.showMessage("⚔️ Cajado Solar Forjado!");
            }
        } else if (ui.craftDragonBtn.contains(x, y)) {
            if (gv.player.getInventory().getOrDefault("Escama Celestial", 0) > 0) {
                gv.player.useItem("Escama Celestial");
                gv.companion.setArmor(Equipment.create("Couraça de Dragão"));
                gv.ui.showMessage("🛡️ Couraça de Dragão Forjada!");
            }
        } else gv.ui.state = UIManager.UIState.EXPLORING;
        return true;
    }

    private boolean tryConsumeMaterials(GameView gv, int amount) {
        Map<String, Integer> inv = gv.player.getInventory();
        int total = 0;
        String[] types = {"Material", "Presa", "Gosma", "Ferrão", "Pelo"};
        for (String type : types) total += inv.getOrDefault(type, 0);
        if (total < amount) return false;
        int remaining = amount;
        for (String type : types) {
            if (inv.containsKey(type)) {
                int count = inv.get(type);
                int toTake = Math.min(count, remaining);
                if (count == toTake) inv.remove(type);
                else inv.put(type, count - toTake);
                remaining -= toTake;
                if (remaining <= 0) break;
            }
        }
        return true;
    }
}
