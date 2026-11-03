package com.example.pokeworld.managers;

import com.example.pokeworld.core.GameView;
import java.util.Random;

public class EventManager {
    private GameView gv;
    private Random random = new Random();
    private long lastEventTime;
    private static final long EVENT_INTERVAL = 180000; // 3 minutos

    public EventManager(GameView gv) {
        this.gv = gv;
        this.lastEventTime = System.currentTimeMillis();
    }

    public void update() {
        if (System.currentTimeMillis() - lastEventTime > EVENT_INTERVAL) {
            triggerRandomEvent();
            lastEventTime = System.currentTimeMillis();
        }
    }

    private void triggerRandomEvent() {
        double r = random.nextDouble();
        if (r < 0.3) {
            triggerHorda();
        } else if (r < 0.6 && gv.world.tileMap.getMapName().equals("HOME")) {
            triggerInvasion();
        }
    }

    private void triggerHorda() {
        String[] species = {"Eevee", "Pikachu", "Dratini", "Magikarp"};
        String s = species[random.nextInt(species.length)];
        gv.ui.showMessage("⚠️ UMA HORDA DE " + s.toUpperCase() + " APARECEU!");
        
        for (int i = 0; i < 8; i++) {
            double x = gv.player.getPositionX() + (random.nextDouble() - 0.5) * 800;
            double y = gv.player.getPositionY() + (random.nextDouble() - 0.5) * 800;
            gv.world.addEnemy(s, x, y, 32, 0, false, random.nextDouble() < 0.1); 
        }
    }

    private void triggerInvasion() {
        gv.ui.showMessage("🚨 SUA BASE ESTÁ SENDO INVADIDA!");
        for (int i = 0; i < 12; i++) {
            double x = random.nextDouble() < 0.5 ? 100 : (gv.world.tileMap.getCols() * 64 - 100);
            double y = random.nextDouble() * (gv.world.tileMap.getRows() * 64);
            gv.world.addEnemy("Rattata", x, y, 32, 0, false, false);
        }
    }
}
