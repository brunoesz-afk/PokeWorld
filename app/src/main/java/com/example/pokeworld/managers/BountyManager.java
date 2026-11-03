package com.example.pokeworld.managers;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BountyManager {
    private GameView gv;
    private List<Bounty> activeBounties = new ArrayList<>();
    private Random random = new Random();

    public static class Bounty {
        public String targetName;
        public int required;
        public int current;
        public int rewardGems;
        public boolean completed = false;

        public Bounty(String targetName, int required, int rewardGems) {
            this.targetName = targetName;
            this.required = required;
            this.rewardGems = rewardGems;
            this.current = 0;
        }
    }

    public BountyManager(GameView gv) {
        this.gv = gv;
        generateDailyBounties();
    }

    public void generateDailyBounties() {
        activeBounties.clear();
        String[] targets = {"Charmander", "Squirtle", "Bulbasaur", "Geodude", "Gastly", "Ekans", "Oddish"};
        activeBounties.add(new Bounty(targets[random.nextInt(targets.length)], 3 + random.nextInt(5), 2));
        activeBounties.add(new Bounty(targets[random.nextInt(targets.length)], 8 + random.nextInt(10), 5));
    }

    public void onEnemyKilled(String name) {
        for (Bounty b : activeBounties) {
            if (!b.completed && b.targetName.equalsIgnoreCase(name)) {
                b.current++;
                if (b.current >= b.required) {
                    b.completed = true;
                    gv.player.addGems(b.rewardGems);
                    gv.ui.showMessage("🎯 RECOMPENSA: +" + b.rewardGems + " 💎 (Caçada)");
                }
            }
        }
    }

    public List<Bounty> getActiveBounties() {
        return activeBounties;
    }
}
