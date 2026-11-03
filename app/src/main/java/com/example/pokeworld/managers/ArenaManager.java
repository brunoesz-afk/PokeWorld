package com.example.pokeworld.managers;

import android.graphics.Color;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Enemy;

import java.util.Random;

public class ArenaManager {
    private int currentWave = 0;
    private boolean inArena = false;
    private boolean isTournament = false;
    private Random random = new Random();
    private GameView gv;
    private long waveCooldown = 0;
    private static final long COOLDOWN_TIME = 3000;

    public ArenaManager(GameView gv) {
        this.gv = gv;
    }

    public void startArena(boolean tournament) {
        inArena = true;
        isTournament = tournament;
        currentWave = 1;
        loadWave();
    }

    public void loadWave() {
        String prefix = isTournament ? "🏆 TORNEIO - ONDA " : "⚔️ ONDA ";
        gv.ui.showMessage(prefix + currentWave);
        gv.world.enemies.clear();
        
        int enemyCount = 3 + (currentWave / 2);
        if (isTournament) enemyCount += 2;

        for (int i = 0; i < enemyCount; i++) {
            String name = getRandomArenaEnemy();
            int hp = 150 + (currentWave * 30);
            if (isTournament) hp *= 1.5;
            int color = getRandomColor();
            int lv = currentWave + (gv.player.getLevel() / 2);
            gv.world.enemies.add(new Enemy(name, 300 + random.nextInt(600), 200 + random.nextInt(300), 25, color, false, lv));
        }

        if (currentWave % 5 == 0) {
            String bossName = isTournament ? "MESTRE DO TORNEIO" : "CAMPEÃO DA ARENA";
            int lv = currentWave + 5;
            gv.world.enemies.add(new Enemy(bossName, 800, 300, 40, Color.YELLOW, true, lv));
        }
    }

    public void update() {
        if (!inArena) return;

        if (gv.world.enemies.isEmpty()) {
            if (waveCooldown == 0) {
                waveCooldown = System.currentTimeMillis();
                String msg = isTournament ? "Vitória na rodada! Próximo desafio em 3s..." : "Onda concluída! Próxima em 3s...";
                gv.ui.showMessage(msg);
                
                // Recompensa imediata por onda
                if (isTournament) {
                    if (currentWave % 5 == 0) gv.player.addGems(2);
                } else {
                    if (currentWave % 5 == 0) gv.player.addGems(1);
                }
            } else if (System.currentTimeMillis() - waveCooldown > COOLDOWN_TIME) {
                currentWave++;
                waveCooldown = 0;
                loadWave();
                gv.player.addGold(100 * currentWave * (isTournament ? 2 : 1));
            }
        }
    }

    private String getRandomArenaEnemy() {
        String[] names = {"Gladiador", "Guerreiro", "Berserker", "Lutador"};
        return names[random.nextInt(names.length)];
    }

    private int getRandomColor() {
        int[] colors = {Color.RED, Color.GRAY, Color.rgb(139, 0, 0), Color.BLACK};
        return colors[random.nextInt(colors.length)];
    }

    public int getCurrentWave() { return currentWave; }
    public boolean isInArena() { return inArena; }
    public boolean isTournament() { return isTournament; }
    public void exitArena() { inArena = false; isTournament = false; currentWave = 0; }
}
