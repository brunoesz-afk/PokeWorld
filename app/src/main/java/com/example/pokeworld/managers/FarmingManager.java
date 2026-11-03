package com.example.pokeworld.managers;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;
import com.example.pokeworld.world.TileMap;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FarmingManager {
    private GameView gv;
    private List<Crop> crops = new ArrayList<>();

    public static class Crop {
        int r, c;
        long plantTime;
        long growDuration = 60000; // 60 segundos para crescer totalmente
        boolean isGrown = false;

        public Crop(int r, int c) {
            this.r = r;
            this.c = c;
            this.plantTime = System.currentTimeMillis();
        }
    }

    public FarmingManager(GameView gv) {
        this.gv = gv;
    }

    public void plant(int r, int c) {
        // Verifica se já existe algo plantado nessa coordenada
        for (Crop crop : crops) {
            if (crop.r == r && crop.c == c) return;
        }
        crops.add(new Crop(r, c));
    }

    public void update() {
        long now = System.currentTimeMillis();
        Iterator<Crop> it = crops.iterator();
        while (it.hasNext()) {
            Crop crop = it.next();
            if (!crop.isGrown && now - crop.plantTime > crop.growDuration) {
                crop.isGrown = true;
                // Transforma o solo em uma árvore de frutos
                gv.world.tileMap.setTileType(crop.c * TileMap.TILE_SIZE, crop.r * TileMap.TILE_SIZE, TileMap.BERRY_TREE);
                gv.ui.showMessage("🌱 Uma planta cresceu na base!");
                it.remove(); // Remove da lista de monitoramento pois já cresceu
            }
        }
    }
}
