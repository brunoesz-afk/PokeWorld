package com.example.pokeworld.managers;

import android.graphics.Color;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.NPC;
import com.example.pokeworld.entities.Loot;

public class TowerManager {
    private final GameView gv;
    private int towerFloor = 1;
    private int currentWave = 1;
    private static final int TOTAL_WAVES = 3; // 3 hordas por andar
    private boolean waveActive = false;
    private boolean waitingForNpcAdvance = false;

    public TowerManager(GameView gv) {
        this.gv = gv;
    }

    public void startTower() {
        startTowerAtFloor(1);
    }

    public void startTowerAtFloor(int floor) {
        towerFloor = floor;
        currentWave = 1;
        waveActive = false;
        waitingForNpcAdvance = false;
        
        if (gv.world != null) {
            gv.world.enemies.clear();
            gv.world.loots.clear();
            gv.world.npcs.clear();
        }

        gv.world.loadMap("TOWER", gv.player, gv.companion, "CENTER");
        gv.ui.showMessage("🏰 TORRE - ANDAR " + towerFloor + " (Onda 1/" + TOTAL_WAVES + ")");
        spawnWave();
    }

    public void update() {
        if (gv.world.tileMap.getMapName().equals("TOWER")) {
            if (waveActive && gv.world.enemies.isEmpty()) {
                waveActive = false;

                if (currentWave < TOTAL_WAVES) {
                    currentWave++;
                    gv.ui.showMessage("⚡ ONDA " + currentWave + " / " + TOTAL_WAVES + " INICIANDO!");
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this::spawnWave, 1500);
                } else {
                    int goldReward = towerFloor * 150;
                    gv.player.addGold(goldReward);
                    gv.ui.showMessage("🎉 ANDAR " + towerFloor + " CONCLUÍDO! Entre no portal ou fale com o NPC.");
                    waitingForNpcAdvance = true;
                    
                    double midX = gv.player.getPositionX();
                    double midY = gv.player.getPositionY() - 100;
                    
                    gv.world.npcs.add(new NPC("Mestre da Torre", "Parabéns! Entre no portal ou fale comigo para o Andar " + (towerFloor + 1), midX, midY, NPC.Type.GUARD));
                    
                    // Portal aincrad_portal no chão para avançar de fase
                    Loot portalLoot = new Loot("aincrad_portal", midX, midY + 60, Loot.Type.ITEM);
                    portalLoot.setRadius(55);
                    gv.world.loots.add(portalLoot);
                    gv.activity.saveCurrentGame();
                }
            }
        }
    }

    public void advanceFloor() {
        if (waitingForNpcAdvance && towerFloor < 20) {
            towerFloor++;
            currentWave = 1;
            waitingForNpcAdvance = false;
            
            if (gv.world != null) {
                gv.world.enemies.clear();
                gv.world.loots.clear();
                gv.world.npcs.clear();
            }

            gv.world.loadMap("TOWER", gv.player, gv.companion, "CENTER");
            gv.ui.showMessage("🏰 TORRE DE DESAFIO - ANDAR " + towerFloor + " (Onda 1/" + TOTAL_WAVES + ")");
            if (towerFloor == 2) {
                gv.ui.showMessage("🧪 DICA: Use o botão de Poção na esquerda caso precise curar seu Pokémon!");
            }
            spawnWave();
        }
    }

    private void spawnWave() {
        double px = gv.player.getPositionX();
        double py = gv.player.getPositionY() - 250;

        String[] pool = {"Rattata", "Pidgey", "Zubat", "Caterpie", "Ekans", "Oddish", "Pikachu", "Charmander"};
        int count = (towerFloor == 1 && currentWave == 1) ? 3 : 2 + currentWave;
        int baseLevel = towerFloor * 2 + currentWave;
        int color = Color.rgb(150, 50, 200);

        for (int i = 0; i < count; i++) {
            double angle = i * (2 * Math.PI / count);
            double ex = px + Math.cos(angle) * 180;
            double ey = py + Math.sin(angle) * 180;
            String species = pool[i % pool.length];

            gv.world.addEnemy(species, ex, ey, 32, color, false, false, baseLevel, 0);
        }

        waveActive = true;
    }

    public int getTowerFloor() {
        return towerFloor;
    }

    public int getMaxUnlockedFloor() {
        return 20;
    }

    public boolean isWaitingForNpcAdvance() {
        return waitingForNpcAdvance;
    }
}
