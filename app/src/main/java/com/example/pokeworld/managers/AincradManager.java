package com.example.pokeworld.managers;

import android.graphics.Color;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Enemy;

public class AincradManager {
    private GameView gv;
    private int currentFloor = 1;
    private int maxFloorReached = 1;
    private boolean isActive = false;

    public AincradManager(GameView gv) {
        this.gv = gv;
    }

    public void start(int floor) {
        this.currentFloor = floor;
        this.isActive = true;
        gv.ui.showMessage("🏰 ENTRANDO EM AINCRAD - ANDAR " + floor);
        gv.world.loadMap("AINCRAD", gv.player, gv.companion, "CENTER");
        spawnFloorChallenges();
    }

    public void spawnFloorChallenges() {
        gv.world.enemies.clear();
        int baseLv = 10 + (currentFloor * 2);
        int enemyCount = 3 + (currentFloor / 5);
        
        // Inimigos do andar
        for (int i = 0; i < enemyCount; i++) {
            double rx = 500 + Math.random() * 2000;
            double ry = 500 + Math.random() * 2000;
            gv.world.addEnemy("Guardião " + currentFloor, rx, ry, 32, Color.LTGRAY, false, Math.random() < 0.1, baseLv, 0);
        }

        // Chefe a cada 5 andares (Checkpoints)
        if (currentFloor % 5 == 0) {
            gv.world.addEnemy("Boss Aincrad F" + currentFloor, 1280, 1280, 60, Color.RED, true, true, baseLv + 5, 0);
            gv.ui.showMessage("⚠️ ANDAR DE CHEFE! ⚠️");
        }
    }

    public void update() {
        if (!isActive || !gv.world.tileMap.getMapName().equals("AINCRAD")) return;

        if (gv.world.enemies.isEmpty()) {
            gv.ui.showMessage("✨ ANDAR " + currentFloor + " LIMPO!");
            currentFloor++;
            if (currentFloor > maxFloorReached) {
                maxFloorReached = currentFloor;
                if ((currentFloor - 1) % 5 == 0) {
                    gv.ui.showMessage("💾 CHECKPOINT ALCANÇADO!");
                    gv.activity.saveCurrentGame();
                }
            }
            spawnFloorChallenges();
        }
    }

    public int getCurrentFloor() { return currentFloor; }
    public int getMaxFloorReached() { return maxFloorReached; }
    public void setMaxFloorReached(int f) { this.maxFloorReached = f; }
    public boolean isActive() { return isActive; }
    public void stop() { isActive = false; }
}
