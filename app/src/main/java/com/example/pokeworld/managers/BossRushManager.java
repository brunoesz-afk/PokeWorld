package com.example.pokeworld.managers;

import android.graphics.Color;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;

import java.util.ArrayList;
import java.util.List;

public class BossRushManager {
    private GameView gv;
    private int bossIndex = 0;
    private boolean isActive = false;
    private List<String> bossList = new ArrayList<>();

    public BossRushManager(GameView gv) {
        this.gv = gv;
        bossList.add("ENT ANCIÃO");
        bossList.add("GOLEM REI");
        bossList.add("FARAÓ ESCORPIÃO");
        bossList.add("DRAGÃO DE GELO");
        bossList.add("FÊNIX DE FOGO");
        bossList.add("KRAKEN");
        bossList.add("RAIQUAZA");
    }

    public void start() {
        isActive = true;
        bossIndex = 0;
        gv.ui.showMessage("🔥 BOSS RUSH INICIADO! 🔥");
        spawnNextBoss();
    }

    private void spawnNextBoss() {
        if (bossIndex < bossList.size()) {
            String name = bossList.get(bossIndex);
            gv.world.enemies.clear();
            
            int hp = 1000 + (bossIndex * 500);
            int color = getBossColor(name);
            int lv = 10 + (bossIndex * 5);
            
            Enemy boss = new Enemy(name, 640, 360, 45, color, true, lv);
            boss.hp = hp;
            boss.maxHp = hp;
            gv.world.enemies.add(boss);
            gv.ui.showMessage("DESAFIO: " + name);
        } else {
            complete();
        }
    }

    private int getBossColor(String name) {
        if (name.contains("ENT")) return Color.rgb(34, 139, 34);
        if (name.contains("GOLEM")) return Color.GRAY;
        if (name.contains("FARAÓ")) return Color.YELLOW;
        if (name.contains("DRAGÃO")) return Color.WHITE;
        if (name.contains("FÊNIX")) return Color.RED;
        if (name.contains("KRAKEN")) return Color.BLUE;
        return Color.GREEN;
    }

    public void update() {
        if (!isActive) return;
        if (gv.world.enemies.isEmpty()) {
            bossIndex++;
            if (bossIndex < bossList.size()) {
                gv.ui.showMessage("Boss derrotado! Próximo em breve...");
                spawnNextBoss();
            } else {
                complete();
            }
        }
    }

    private void complete() {
        isActive = false;
        gv.player.addGems(20);
        gv.ui.showMessage("🏆 BOSS RUSH COMPLETO! +20 💎");
        gv.world.loadMap("CITY", gv.player, gv.companion);
    }

    public boolean isActive() { return isActive; }
}
