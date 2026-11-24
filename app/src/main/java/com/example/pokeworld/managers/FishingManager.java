package com.example.pokeworld.managers;

import com.example.pokeworld.core.DatabaseHelper;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Enemy;
import com.example.pokeworld.entities.BuildingData;

import java.util.Map;
import java.util.Random;

public class FishingManager {
    public boolean isFishing = false;
    public boolean fishBiting = false;
    public boolean isSelectingFishingSpot = false;
    private long fishingStartTime = 0;
    private long biteMessageTime = 0;
    private Random random = new Random();
    private GameView gv;

    public FishingManager(GameView gv) {
        this.gv = gv;
    }

    private int getCastleLevel() {
        Map<String, BuildingData> bData = gv.activity.dbHelper.getBuildings();
        return bData.containsKey("Castelo") ? bData.get("Castelo").level : 1;
    }

    public void update() {
        if (!isFishing) return;

        long elapsed = System.currentTimeMillis() - fishingStartTime;
        
        if (elapsed > 3000 && !fishBiting && random.nextInt(100) < 5) {
            fishBiting = true;
            gv.ui.showMessage("FISGOU! PUXE!");
            biteMessageTime = System.currentTimeMillis();
        }
        
        if (fishBiting && System.currentTimeMillis() - biteMessageTime > 1500) {
            isFishing = false;
            fishBiting = false;
            gv.ui.showMessage("O peixe fugiu...");
        }
    }

    public void handleAction(MissionManager missions) {
        if (isSelectingFishingSpot) {
            gv.ui.showMessage("Selecione a água no mapa!");
            return;
        }

        if (!isFishing) {
            isSelectingFishingSpot = true;
            gv.ui.showMessage("Toque na água para lançar a isca!");
        } else if (fishBiting) {
            isFishing = false;
            fishBiting = false;
            
            double encounterChance = Math.random();
            if (encounterChance < 0.10) { // 10% de chance de pescar um BOSS Gyarados
                spawnGyaradosBoss();
            } else if (encounterChance < 0.30) { // 20% de chance de pescar um monstro comum
                spawnFishingEnemy();
            } else {
                catchFish(missions);
            }
            gv.player.gainFishingExp(10); // Ganha 10 de EXP por tentativa bem-sucedida
        } else {
            isFishing = false;
            gv.ui.showMessage("Puxou cedo demais...");
        }
    }

    private void catchFish(MissionManager missions) {
        int baseGold = 15;
        int castleLv = getCastleLevel();
        
        String item = "Peixe";
        double rareChance = Math.random();
        if (rareChance < 0.15) {
            item = "Peixe Raro";
            baseGold = 100;
        }

        int totalGold = com.example.pokeworld.managers.Economy.scale(gv.kingdom.calculateGoldWithBonus(baseGold, castleLv), com.example.pokeworld.managers.Economy.FISH);
        
        gv.player.addItem(item);
        gv.player.addGold(totalGold);
        missions.count(MissionManager.Metric.FISH);
        gv.ui.showMessage("✨ " + item.toUpperCase() + " PESCADO (+" + totalGold + " 🪙)!");
        gv.particles.addExplosion(gv.player.getPositionX(), gv.player.getPositionY(), android.graphics.Color.BLUE, 15);
    }

    private void spawnGyaradosBoss() {
        gv.ui.showMessage("🚨 UM GYARADOS ENORME FOI ATRAÍDO!");
        gv.startShake(100, 30);
        Enemy boss = new Enemy("Gyarados_Boss", gv.player.getPositionX() + 150, gv.player.getPositionY(), 60, android.graphics.Color.BLUE, true, gv.player.getLevel() + 5);
        gv.world.enemies.add(boss);
        boss.setSelected(true);
        gv.particles.addExplosion(boss.getPositionX(), boss.getPositionY(), android.graphics.Color.WHITE, 50);
    }

    private void spawnFishingEnemy() {
        gv.ui.showMessage("⚠️ ALGO PERIGOSO FOI FISGADO!");
        gv.startShake(20, 15);
        
        // Spawna um inimigo aquático perto do jogador
        String[] waterEnemies = {"Slime", "Kraken Mini", "Peixe Espada"};
        String species = waterEnemies[new Random().nextInt(waterEnemies.length)];
        
        Enemy fishingEnemy = new Enemy(species, gv.player.getPositionX() + 100, gv.player.getPositionY(), 30, android.graphics.Color.BLUE, false);
        gv.world.enemies.add(fishingEnemy);
        fishingEnemy.setSelected(true); // Seleciona automaticamente para o pet atacar
        
        gv.particles.addExplosion(fishingEnemy.getPositionX(), fishingEnemy.getPositionY(), android.graphics.Color.CYAN, 30);
    }

    public void startFishing() {
        isSelectingFishingSpot = false;
        isFishing = true;
        fishingStartTime = System.currentTimeMillis();
        fishBiting = false;
        gv.ui.showMessage("Lançando isca...");
    }

    public void cancelSelection() {
        isSelectingFishingSpot = false;
        gv.ui.showMessage("Pesca cancelada.");
    }
}
