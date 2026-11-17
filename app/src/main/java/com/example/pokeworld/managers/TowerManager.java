package com.example.pokeworld.managers;

import android.graphics.Color;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.NPC;
import com.example.pokeworld.entities.Loot;

public class TowerManager {
    private final GameView gv;
    private int towerFloor = 1;
    private final int maxUnlockedFloor = 20; // Até 20 andares
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
        waveActive = false;
        waitingForNpcAdvance = false;
        
        // Limpa todos os rastros de andares anteriores (corpos, itens, npcs)
        if (gv.world != null) {
            gv.world.enemies.clear();
            gv.world.loots.clear();
            gv.world.npcs.clear();
        }

        gv.world.loadMap("TOWER", gv.player, gv.companion, "CENTER");
        gv.ui.showMessage("🏰 TORRE DE DESAFIO - ANDAR " + towerFloor);
        spawnWave();
    }

    public void update() {
        if (gv.world.tileMap.getMapName().equals("TOWER")) {
            if (waveActive && gv.world.enemies.isEmpty()) {
                waveActive = false;
                int goldReward = towerFloor * 150;
                gv.player.addGold(goldReward);

                if (towerFloor < 20) {
                    gv.ui.showMessage("🎉 ANDAR " + towerFloor + " VENCIDO! Fale com o NPC ou entre no portal para subir.");
                    waitingForNpcAdvance = true;
                    
                    double midX = gv.player.getPositionX();
                    double midY = gv.player.getPositionY() - 100;
                    
                    // Spawna NPC Guardião
                    gv.world.npcs.add(new NPC("Mestre da Torre", "Parabéns! Fale comigo ou entre no portal para subir ao próximo andar!", midX, midY, NPC.Type.GUARD));
                    
                    // Spawna o Portal com a sprite aincrad_portal
                    Loot portalLoot = new Loot("aincrad_portal", midX, midY + 50, Loot.Type.ITEM);
                    portalLoot.setRadius(50);
                    gv.world.loots.add(portalLoot);
                } else {
                    gv.ui.showMessage("🏆 PARABÉNS POR ZERAR A TORRE DE 20 ANDARES!");
                    gv.ui.isDialogueActive = true;
                    gv.ui.currentSpeaker = "Mestre da Torre";
                    gv.ui.currentDialogue = "Inacreditável! Você conquistou todos os 20 andares da Torre de Desafios!\nSua bravura será lembrada em todo o PokeWorld!";
                    gv.activity.saveCurrentGame();
                }
            }
        }
    }

    public void advanceFloor() {
        if (waitingForNpcAdvance && towerFloor < 20) {
            towerFloor++;
            waitingForNpcAdvance = false;
            
            // Limpa o rastro de inimigos e loot do andar anterior ao subir
            if (gv.world != null) {
                gv.world.enemies.clear();
                gv.world.loots.clear();
                gv.world.npcs.clear();
            }

            gv.world.loadMap("TOWER", gv.player, gv.companion, "CENTER");
            gv.ui.showMessage("🏰 TORRE DE DESAFIO - ANDAR " + towerFloor);
            if (towerFloor == 2) {
                gv.ui.showMessage("🧪 DICA: Use o botão de Poção na esquerda caso precise curar seu Pokémon!");
            }
            spawnWave();
        }
    }

    private void spawnWave() {
        double px = gv.player.getPositionX();
        double py = gv.player.getPositionY() - 250; // Nasce na parte superior da sala da torre

        String[] pool = {"Rattata", "Pidgey", "Zubat", "Caterpie", "Ekans", "Oddish", "Pikachu", "Charmander"};
        int count = (towerFloor == 1) ? 3 : (towerFloor == 2) ? 4 : Math.min(3 + (towerFloor / 2), 8);
        int baseLevel = towerFloor * 2;
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
        return maxUnlockedFloor;
    }

    public boolean isWaitingForNpcAdvance() {
        return waitingForNpcAdvance;
    }
}
