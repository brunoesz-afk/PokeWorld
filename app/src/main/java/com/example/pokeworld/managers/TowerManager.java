package com.example.pokeworld.managers;

import android.graphics.Color;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.world.TileMap;

public class TowerManager {
    private GameView gv;
    private int towerFloor = 1;
    private boolean waveActive = false;

    public TowerManager(GameView gv) {
        this.gv = gv;
    }

    public void startTower() {
        towerFloor = 1;
        waveActive = false;
        gv.world.loadMap("TOWER", gv.player, gv.companion, "CENTER");
        gv.ui.showMessage("🏰 TORRE DE DESAFIO - ANDAR 1");
        spawnWave();
    }

    public void update() {
        if (gv.world.tileMap.getMapName().equals("TOWER")) {
            if (waveActive && gv.world.enemies.isEmpty()) {
                // Onda derrotada! Recompensa e sobe de andar
                waveActive = false;
                int goldReward = towerFloor * 100;
                gv.player.addGold(goldReward);
                gv.ui.showMessage("🎉 ANDAR " + towerFloor + " CONCLUÍDO! Recompensa: +" + goldReward + " Ouro");
                
                towerFloor++;
                gv.activity.saveCurrentGame();

                // Pequeno atraso ou spawn automático da próxima onda
                spawnWave();
            }
        }
    }

    private void spawnWave() {
        double px = gv.player.getPositionX();
        double py = gv.player.getPositionY();

        String[] pool = {"Rattata", "Pidgey", "Ekans", "Zubat", "Machop", "Onix", "Gastly", "Charizard"};
        int count = Math.min(2 + (towerFloor / 2), 8);
        int baseLevel = towerFloor * 3;
        int color = Color.rgb(150, 50, 200);

        for (int i = 0; i < count; i++) {
            double angle = i * (2 * Math.PI / count);
            double ex = px + Math.cos(angle) * 350;
            double ey = py + Math.sin(angle) * 350;
            String species = pool[(int)(Math.random() * pool.length)];
            boolean isBoss = (towerFloor % 5 == 0 && i == 0);

            gv.world.addEnemy(species, ex, ey, isBoss ? 60 : 32, color, isBoss, Math.random() < 0.1, baseLevel, (int)(Math.random() * 2));
        }

        waveActive = true;
        gv.ui.showMessage("⚔️ ONDA DE POKÉMONS INICIADA (Andar " + towerFloor + ")");
    }

    public int getTowerFloor() {
        return towerFloor;
    }
}
