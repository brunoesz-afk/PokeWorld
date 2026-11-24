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
                    int goldReward = rewardGold(towerFloor);
                    gv.player.addGold(goldReward);
                    String item = rewardItem(towerFloor);
                    int amount = rewardItemAmount(towerFloor);
                    gv.player.addItem(item, amount);
                    int gemsB = rewardBoostGems(towerFloor);
                    if (gemsB > 0) gv.player.addItem("Gema de Boost", gemsB);
                    markCleared(towerFloor);
                    gv.ui.showMessage("🎉 ANDAR " + towerFloor + " CONCLUÍDO! +" + goldReward + "🪙  +" + amount + " " + item + (gemsB > 0 ? "  +" + gemsB + " 💎 Gema de Boost" : ""));
                    waitingForNpcAdvance = true;
                    
                    double midX = gv.player.getPositionX();
                    double midY = Math.max(160, gv.player.getPositionY() - 260);
                    
                    // O Mestre fica ao lado e o portal um pouco acima: o jogador precisa andar até ele
                    // (antes o portal nascia dentro do raio de coleta e era pego na hora).
                    gv.world.npcs.add(new NPC("Mestre da Torre", "Parabéns! Entre no portal ou fale comigo para o Andar " + (towerFloor + 1), midX + 110, midY + 40, NPC.Type.GUARD));
                    
                    Loot portalLoot = new Loot("aincrad_portal", midX - 60, midY, Loot.Type.ITEM);
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
        if (!gv.world.tileMap.getMapName().equals("TOWER")) return;   // jogador saiu durante o intervalo entre ondas
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

    public static final int MAX_FLOOR = 20;
    private static final String[] RUNES = {"Runa de Ferro", "Runa de Sangue", "Runa de Mercúrio", "Runa da Sorte", "Runa de Zeus"};

    /** Maior andar já concluído (salvo em preferências). */
    public int getHighestCleared() {
        return Math.max(gv.activity.dbHelper.getKv("tower_cleared", 0), gv.activity.getSharedPreferences("tower", 0).getInt("cleared", 0));
    }

    public int getMaxUnlockedFloor() {
        return Math.min(MAX_FLOOR, getHighestCleared() + 1);
    }

    private void markCleared(int floor) {
        if (floor > getHighestCleared()) gv.activity.dbHelper.setKv("tower_cleared", floor);
    }

    /** Item de prêmio do andar: runa a cada 5 andares, pedra nos pares, Pokébolas nos ímpares. */
    public static String rewardItem(int floor) {
        if (floor % 5 == 0) return RUNES[(floor / 5 - 1) % RUNES.length];
        if (floor % 2 == 0) return com.example.pokeworld.entities.EvolutionTable.STONES[(floor / 2 - 1) % com.example.pokeworld.entities.EvolutionTable.STONES.length];
        return "Pokébola";
    }

    public static int rewardItemAmount(int floor) {
        return (floor % 5 != 0 && floor % 2 != 0) ? 2 + floor / 4 : 1;
    }

    /** Gemas de Boost nos andares 5, 10, 15 e 20 (o último dá 2). */
    public static int rewardBoostGems(int floor) { return floor % 5 == 0 ? 1 + floor / 20 : 0; }

    public static int rewardGold(int floor) { return floor * 150; }

    /** Nível médio dos inimigos na última onda do andar. */
    public static int enemyLevel(int floor) { return floor * 2 + TOTAL_WAVES; }

    public boolean isWaitingForNpcAdvance() {
        return waitingForNpcAdvance;
    }
}
