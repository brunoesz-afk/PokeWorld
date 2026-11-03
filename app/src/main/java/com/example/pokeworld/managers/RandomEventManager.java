package com.example.pokeworld.managers;

import android.graphics.Color;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;
import com.example.pokeworld.world.TileMap;

import java.util.Random;

public class RandomEventManager {
    private GameView gv;
    private Random random = new Random();
    private long lastEventCheck = 0;
    private static final long CHECK_INTERVAL = 60000; // 1 minuto
    private NPC travelingMerchant;
    private boolean isSlimeInvasion = false;

    public RandomEventManager(GameView gv) {
        this.gv = gv;
    }

    public void update() {
        long now = System.currentTimeMillis();
        if (now - lastEventCheck > CHECK_INTERVAL) {
            lastEventCheck = now;
            String map = gv.world.tileMap.getMapName();
            
            if (map.equals("CITY")) {
                trySpawnMerchant();
                trySlimeInvasion();
            } else if (map.equals("HOME")) {
                tryBaseInvasion();
            }
            
            if (map.equals("FOREST") || map.equals("RIO") || map.equals("OCEAN")) {
                tryHordaEvent();
            }
            
            // Evento de Meteoros pode acontecer em qualquer mapa exceto Dungeon/Arena
            if (!map.equals("DUNGEON") && !map.equals("ARENA")) {
                tryMeteorShower();
            }
        }
        
        if (isSlimeInvasion && gv.world.enemies.isEmpty()) {
            isSlimeInvasion = false;
            gv.ui.showMessage("📢 A invasão de Slimes foi repelida!");
            gv.player.addGems(2);
        }
    }

    private void tryMeteorShower() {
        if (random.nextDouble() < 0.2) { // 20% de chance a cada minuto
            gv.ui.showMessage("☄️ CHUVA DE METEOROS! Procure por Minério Estelar!");
            for (int i = 0; i < 3; i++) {
                float rx = 100 + random.nextInt(1000);
                float ry = 100 + random.nextInt(500);
                gv.world.loots.add(new Loot("Minério Estelar", rx, ry, Loot.Type.ITEM));
                gv.particles.addExplosion(rx, ry, Color.rgb(255, 165, 0), 20);
            }
            gv.startShake(40, 10);
        }
    }

    private void trySpawnMerchant() {
        if (random.nextDouble() < 0.3) {
            if (travelingMerchant == null) {
                travelingMerchant = new NPC("Mercador Viajante", "Tenho itens raros hoje!", TileMap.TILE_SIZE * 12, TileMap.TILE_SIZE * 8, NPC.Type.SHOPKEEPER);
                gv.world.npcs.add(travelingMerchant);
                gv.ui.showMessage("📢 Um Mercador Viajante chegou à cidade!");
            }
        } else if (travelingMerchant != null) {
            gv.world.npcs.remove(travelingMerchant);
            travelingMerchant = null;
            gv.ui.showMessage("📢 O Mercador Viajante foi embora.");
        }
    }

    private void trySlimeInvasion() {
        if (!isSlimeInvasion && random.nextDouble() < 0.2) {
            isSlimeInvasion = true;
            gv.ui.showMessage("⚠️ ALERTA: Invasão de Slimes na Cidade!");
            int lv = gv.player.getLevel();
            for (int i = 0; i < 8; i++) {
                gv.world.addEnemy("Slime", 200 + random.nextInt(800), 200 + random.nextInt(400), 20, Color.GREEN, false, false, lv, 0);
            }
        }
    }

    private void tryBaseInvasion() {
        if (random.nextDouble() < 0.15) {
            gv.ui.showMessage("🚨 SUA BASE ESTÁ SENDO INVADIDA POR RATTATAS!");
            for (int i = 0; i < 12; i++) {
                double x = random.nextDouble() < 0.5 ? 100 : (gv.world.tileMap.getCols() * 64 - 100);
                double y = random.nextDouble() * (gv.world.tileMap.getRows() * 64);
                gv.world.addEnemy("Rattata", x, y, 32, Color.GRAY, false, false);
            }
        }
    }

    private void tryHordaEvent() {
        if (random.nextDouble() < 0.1) {
            String[] species = {"Eevee", "Pikachu", "Dratini", "Magikarp"};
            String s = species[random.nextInt(species.length)];
            gv.ui.showMessage("⚠️ UMA HORDA DE " + s.toUpperCase() + " APARECEU!");
            for (int i = 0; i < 8; i++) {
                double x = gv.player.getPositionX() + (random.nextDouble() - 0.5) * 800;
                double y = gv.player.getPositionY() + (random.nextDouble() - 0.5) * 800;
                gv.world.addEnemy(s, x, y, 32, Color.YELLOW, false, random.nextDouble() < 0.1); 
            }
        }
    }
}
