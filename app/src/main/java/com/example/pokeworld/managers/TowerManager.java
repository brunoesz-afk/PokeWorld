package com.example.pokeworld.managers;

import android.graphics.Color;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.NPC;

public class TowerManager {
    private final GameView gv;
    private int towerFloor = 1;
    private boolean waveActive = false;
    private boolean waitingForNpcAdvance = false;

    public TowerManager(GameView gv) {
        this.gv = gv;
    }

    public void startTower() {
        towerFloor = 1;
        waveActive = false;
        waitingForNpcAdvance = false;
        gv.world.loadMap("TOWER", gv.player, gv.companion, "CENTER");
        gv.ui.showMessage("🏰 TORRE DE DESAFIO - ANDAR 1");
        spawnWave();
    }

    public void update() {
        if (gv.world.tileMap.getMapName().equals("TOWER")) {
            if (waveActive && gv.world.enemies.isEmpty()) {
                waveActive = false;
                int goldReward = towerFloor * 150;
                gv.player.addGold(goldReward);

                if (towerFloor == 1) {
                    gv.ui.showMessage("🎉 ANDAR 1 VENCIDO! Fale com o NPC Mestre da Torre para ir ao Nível 2.");
                    waitingForNpcAdvance = true;
                    // Spawna NPC no centro da torre para avançar
                    double midX = gv.player.getPositionX();
                    double midY = gv.player.getPositionY() - 80;
                    gv.world.npcs.add(new NPC("Mestre da Torre", "Parabéns! Fale comigo para subir ao Nível 2!", midX, midY, NPC.Type.GUARD));
                } else if (towerFloor == 2) {
                    towerFloor = 3; // Concluído
                    gv.activity.saveCurrentGame();
                    if (gv.tutorial != null) {
                        gv.tutorial.onTowerChallengeCompleted();
                    } else {
                        gv.ui.isDialogueActive = true;
                        gv.ui.currentSpeaker = "Mestre da Torre";
                        gv.ui.currentDialogue = "Parabéns por vencer o desafio da Torre!";
                    }
                }
            }
        }
    }

    public void advanceFloor() {
        if (waitingForNpcAdvance && towerFloor == 1) {
            towerFloor = 2;
            waitingForNpcAdvance = false;
            gv.ui.showMessage("🏰 TORRE DE DESAFIO - ANDAR 2 (4 Pokémons)");
            gv.ui.showMessage("🧪 DICA: Use o botão de Poção na esquerda caso precise curar seu Pokémon!");
            spawnWave();
        }
    }

    private void spawnWave() {
        double px = gv.player.getPositionX();
        double py = gv.player.getPositionY();

        String[] pool = {"Rattata", "Pidgey", "Zubat", "Caterpie"};
        int count = (towerFloor == 1) ? 3 : 4; // 3 no andar 1, 4 no andar 2
        int baseLevel = towerFloor * 2;
        int color = Color.rgb(150, 50, 200);

        for (int i = 0; i < count; i++) {
            double angle = i * (2 * Math.PI / count);
            double ex = px + Math.cos(angle) * 280;
            double ey = py + Math.sin(angle) * 280;
            String species = pool[i % pool.length];

            gv.world.addEnemy(species, ex, ey, 32, color, false, false, baseLevel, 0);
        }

        waveActive = true;
    }

    public int getTowerFloor() {
        return towerFloor;
    }

    public boolean isWaitingForNpcAdvance() {
        return waitingForNpcAdvance;
    }
}
