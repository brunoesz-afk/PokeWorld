package com.example.pokeworld.managers;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;

import java.util.Random;

public class BreedingManager {
    private GameView gv;
    private CapturedCreature parent1;
    private CapturedCreature parent2;
    private int stepsRequired = 1000; 
    private int currentProgress = 0;
    private boolean hasEgg = false;

    public BreedingManager(GameView gv) {
        this.gv = gv;
    }

    public void setParent1(CapturedCreature p1) {
        this.parent1 = p1;
        this.currentProgress = 0;
        this.hasEgg = false;
    }

    public void setParent2(CapturedCreature p2) {
        this.parent2 = p2;
        this.currentProgress = 0;
        this.hasEgg = false;
    }

    public void updateProgress(double distanceMoved) {
        if (parent1 != null && parent2 != null && !hasEgg) {
            currentProgress += (int) distanceMoved;
            if (currentProgress >= stepsRequired) {
                hasEgg = true;
                gv.ui.showMessage("🥚 Um ovo foi gerado no Daycare!");
            }
        }
    }

    public void collectEgg() {
        if (hasEgg) {
            Random r = new Random();
            String species = r.nextBoolean() ? parent1.name : parent2.name;
            
            // Lógica de Herança Genética (Item 20)
            double shinyChance = (parent1.isShiny || parent2.isShiny) ? 0.15 : 0.02;
            boolean isShiny = r.nextDouble() < shinyChance;
            
            // Herança de Runas (Chance de 25% de herdar a runa de um dos pais)
            String inheritedRune = "Nenhum";
            if (r.nextDouble() < 0.25) {
                PetData d1 = gv.activity.dbHelper.getPetData(parent1.name);
                PetData d2 = gv.activity.dbHelper.getPetData(parent2.name);
                inheritedRune = r.nextBoolean() ? d1.rune : d2.rune;
            }

            // Herança de Raridade
            Rarity rarity = parent1.rarity.ordinal() > parent2.rarity.ordinal() ? parent2.rarity : parent1.rarity;

            int gender = r.nextDouble() < 0.5 ? 0 : 1;
            gv.player.addCreatureToCollection(species, isShiny, rarity, gender);
            gv.bestiary.markAsCaptured(species);
            
            // Salva os dados iniciais do filhote com a runa herdada (V19: com friendship e type2)
            gv.activity.dbHelper.savePet(species, 1, 0, 0, "Nenhum", "Nenhum", isShiny, "Normal", inheritedRune, 0, "Nenhum", rarity, gender);
            
            parent1 = null;
            parent2 = null;
            hasEgg = false;
            currentProgress = 0;
            
            String msg = "✨ O ovo chocou em um " + species;
            if (isShiny) msg += " SHINY!";
            if (!inheritedRune.equals("Nenhum")) msg += " com Runa!";
            gv.ui.showMessage(msg);
        }
    }

    public boolean isDaycareFull() { return parent1 != null && parent2 != null; }
    public boolean hasEgg() { return hasEgg; }
    public CapturedCreature getParent1() { return parent1; }
    public CapturedCreature getParent2() { return parent2; }
}
