package com.example.pokeworld.world;

import android.graphics.Color;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Enemy;
import com.example.pokeworld.entities.Loot;

import java.util.Random;

public class DungeonManager {
    public enum ChaosModifier { NONE, EXPLOSIVE, REGEN, SPEEDY, DARKNESS }
    
    private int currentFloor = 0;
    private boolean inDungeon = false;
    private Random random = new Random();
    private GameView gv;
    private boolean bossSpawned = false;
    private ChaosModifier currentModifier = ChaosModifier.NONE;

    public DungeonManager(GameView gv) {
        this.gv = gv;
    }

    public void startDungeon() {
        inDungeon = true;
        currentFloor = 1;
        loadFloor();
    }

    public void loadFloor() {
        // Sorteia modificador
        currentModifier = ChaosModifier.values()[random.nextInt(ChaosModifier.values().length)];
        
        gv.ui.showMessage("🏰 ANDAR " + currentFloor + " - " + currentModifier.name());
        gv.world.tileMap = new TileMap("DUNGEON", 12, 25);
        gv.world.setMapDark(currentModifier == ChaosModifier.DARKNESS);
        gv.world.enemies.clear();
        gv.world.loots.clear();
        bossSpawned = false;
        
        int enemyCount = 2 + (currentFloor / 3);
        int ngPlus = gv.player.getNgPlusLevel();
        
        for (int i = 0; i < enemyCount; i++) {
            String name = getRandomEnemyName();
            int color = getRandomColor();
            Enemy e = new Enemy(name, 400 + random.nextInt(800), 200 + random.nextInt(400), 25, color, false, currentFloor);
            
            // Apply Modifier Buffs
            if (currentModifier == ChaosModifier.SPEEDY) e.maxHp /= 1.2; // Menos vida mas mais rápido
            if (currentModifier == ChaosModifier.REGEN) e.maxHp *= 1.2;
            
            e.maxHp = (int)(e.maxHp * (1 + currentFloor * 0.1) * (1 + ngPlus * 0.5));
            e.hp = e.maxHp;
            gv.world.enemies.add(e);
        }

        if (currentFloor % 5 == 0) {
            String bossName = ngPlus > 0 ? "GUARDIÃO CORROMPIDO" : "GUARDIÃO DO ANDAR";
            Enemy boss = new Enemy(bossName, 800, 300, 35, Color.BLACK, true, currentFloor + 2);
            boss.maxHp = (int)(boss.maxHp * (1 + currentFloor * 0.2) * (1 + ngPlus * 0.5));
            boss.hp = boss.maxHp;
            gv.world.enemies.add(boss);
            bossSpawned = true;
        }
    }

    public void update() {
        if (!inDungeon) return;
        
        // Aplica efeitos dos modificadores
        if (currentModifier == ChaosModifier.REGEN) {
            for (Enemy e : gv.world.enemies) {
                if (e.isAlive() && e.hp < e.maxHp && Math.random() < 0.01) e.hp += 1;
            }
        }

        if (bossSpawned && gv.world.enemies.isEmpty()) {
            bossSpawned = false;
            gv.world.loots.add(new Loot("Baú Lendário", 800, 300, Loot.Type.CHEST));
            gv.ui.showMessage("🎁 O Guardião deixou um tesouro épico!");
        }
    }

    public void nextFloor() {
        currentFloor++;
        loadFloor();
        gv.player.addGold(100 * currentFloor);
        gv.player.gainExp(50 * currentFloor);
    }

    private String getRandomEnemyName() {
        String[] names = {"Espectro", "Zumbi", "Morcego", "Aranha", "Caveira"};
        return names[random.nextInt(names.length)];
    }

    private int getRandomColor() {
        int[] colors = {Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA, Color.CYAN};
        return colors[random.nextInt(colors.length)];
    }

    public int getCurrentFloor() { return currentFloor; }
    public boolean isInDungeon() { return inDungeon; }
    public ChaosModifier getCurrentModifier() { return currentModifier; }
    public void exitDungeon() { inDungeon = false; currentFloor = 0; bossSpawned = false; currentModifier = ChaosModifier.NONE; }
}
