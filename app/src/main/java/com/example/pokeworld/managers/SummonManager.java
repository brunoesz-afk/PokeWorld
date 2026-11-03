package com.example.pokeworld.managers;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Rarity;

import java.util.Random;

public class SummonManager {
    private Random random = new Random();
    private GameView gv;

    public SummonManager(GameView gv) {
        this.gv = gv;
    }

    public void commonSummon() {
        if (gv.player.getGems() >= 5) {
            gv.player.addGems(-5);
            performSummon(false);
        } else {
            gv.ui.showMessage("❌ Precisa de 5 Gemas!");
        }
    }

    public void premiumSummon() {
        if (gv.player.getGems() >= 15) {
            gv.player.addGems(-15);
            performSummon(true);
        } else {
            gv.ui.showMessage("❌ Precisa de 15 Gemas!");
        }
    }

    private void performSummon(boolean isPremium) {
        String[] pool = isPremium ? 
            new String[]{"Charizard", "Blastoise", "Venusaur", "Gengar", "Magmar"} :
            new String[]{"Pikachu", "Charmander", "Squirtle", "Bulbasaur", "Geodude", "Gastly"};
        
        String species = pool[random.nextInt(pool.length)];
        
        // Chance de Shiny: 1% normal, 10% premium
        boolean isShiny = random.nextDouble() < (isPremium ? 0.10 : 0.01);
        
        // Lógica de Raridade no Summon
        Rarity rarity = Rarity.COMMON;
        double r = random.nextDouble();
        if (isPremium) {
            if (r < 0.05) rarity = Rarity.LEGENDARY;
            else if (r < 0.15) rarity = Rarity.EPIC;
            else if (r < 0.40) rarity = Rarity.RARE;
            else rarity = Rarity.UNCOMMON;
        } else {
            if (r < 0.01) rarity = Rarity.LEGENDARY;
            else if (r < 0.05) rarity = Rarity.EPIC;
            else if (r < 0.15) rarity = Rarity.RARE;
            else if (r < 0.40) rarity = Rarity.UNCOMMON;
        }

        int gender = random.nextDouble() < 0.5 ? 0 : 1;
        gv.player.addCreatureToCollection(species, isShiny, rarity, gender);
        gv.bestiary.markAsCaptured(species);
        
        String msg = (isShiny ? "✨ SHINY " : "✨ ") + rarity.getLabel() + " CONVOCADO: ";
        gv.ui.showMessage(msg + species.toUpperCase() + "!");
        
        int particleColor = isShiny ? android.graphics.Color.YELLOW : android.graphics.Color.CYAN;
        gv.particles.addExplosion(gv.player.getPositionX(), gv.player.getPositionY(), particleColor, 50);
    }
}
