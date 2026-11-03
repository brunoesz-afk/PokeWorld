package com.example.pokeworld.world;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;

import com.example.pokeworld.managers.SpriteManager;

import java.util.Random;

/**
 * LavaMapBuilder: Construtor de mapas vulcânicos e cavernas de lava
 * utilizando a sprite sheet tiled_lava.png (1024x1024 dividida em 32x32 tiles de 32x32px).
 */
public class LavaMapBuilder {
    private int rows, cols;
    // Armazena as coordenadas do tileset (row * 32 + col) para cada célula do mapa
    private int[][] tileCoordGrid;
    private Random random = new Random();

    public LavaMapBuilder(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.tileCoordGrid = new int[rows][cols];
    }

    /**
     * Define um tile específico da sprite sheet tiled_lava em uma posição do mapa.
     * @param mapRow Linha na grade do mapa.
     * @param mapCol Coluna na grade do mapa.
     * @param tilesetRow Linha na sprite sheet (0 a 31).
     * @param tilesetCol Coluna na sprite sheet (0 a 31).
     */
    public LavaMapBuilder setTile(int mapRow, int mapCol, int tilesetRow, int tilesetCol) {
        if (mapRow >= 0 && mapRow < rows && mapCol >= 0 && mapCol < cols) {
            tileCoordGrid[mapRow][mapCol] = (tilesetRow * 32) + tilesetCol;
        }
        return this;
    }

    /**
     * Preenche uma região retangular do mapa com um tile do tileset.
     */
    public LavaMapBuilder fillArea(int startRow, int startCol, int width, int height, int tilesetRow, int tilesetCol) {
        int tileVal = (tilesetRow * 32) + tilesetCol;
        for (int r = startRow; r < startRow + height && r < rows; r++) {
            for (int c = startCol; c < startCol + width && c < cols; c++) {
                if (r >= 0 && c >= 0) {
                    tileCoordGrid[r][c] = tileVal;
                }
            }
        }
        return this;
    }

    /**
     * Adiciona bordas ao redor do mapa.
     */
    public LavaMapBuilder addBorder(int tilesetRow, int tilesetCol) {
        int tileVal = (tilesetRow * 32) + tilesetCol;
        for (int c = 0; c < cols; c++) {
            tileCoordGrid[0][c] = tileVal;
            tileCoordGrid[rows - 1][c] = tileVal;
        }
        for (int r = 0; r < rows; r++) {
            tileCoordGrid[r][0] = tileVal;
            tileCoordGrid[r][cols - 1] = tileVal;
        }
        return this;
    }

    /**
     * Gera um lago de lava com formato orgânico e bordas.
     */
    public LavaMapBuilder addLavaLake(int centerRow, int centerCol, int radius, int lavaRow, int lavaCol, int borderRow, int borderCol) {
        int lavaVal = (lavaRow * 32) + lavaCol;
        int borderVal = (borderRow * 32) + borderCol;

        for (int dr = -radius; dr <= radius; dr++) {
            for (int dc = -radius; dc <= radius; dc++) {
                int r = centerRow + dr;
                int c = centerCol + dc;
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    double dist = Math.sqrt(dr * dr + dc * dc) + (random.nextDouble() * 0.8 - 0.4);
                    if (dist < radius - 1) {
                        tileCoordGrid[r][c] = lavaVal;
                    } else if (dist < radius) {
                        tileCoordGrid[r][c] = borderVal;
                    }
                }
            }
        }
        return this;
    }

    /**
     * Adiciona uma ponte de pedra/rocha sobre a lava.
     */
    public LavaMapBuilder addBridge(int startRow, int startCol, int length, boolean horizontal, int bridgeRow, int bridgeCol) {
        int bridgeVal = (bridgeRow * 32) + bridgeCol;
        for (int i = 0; i < length; i++) {
            int r = horizontal ? startRow : startRow + i;
            int c = horizontal ? startCol + i : startCol;
            if (r >= 0 && r < rows && c >= 0 && c < cols) {
                tileCoordGrid[r][c] = bridgeVal;
            }
        }
        return this;
    }

    /**
     * Espalha cristais e detalhes vulcânicos aleatoriamente.
     */
    public LavaMapBuilder addVolcanicDetails(int count, int detailRow, int detailCol) {
        int detailVal = (detailRow * 32) + detailCol;
        for (int i = 0; i < count; i++) {
            int r = random.nextInt(rows);
            int c = random.nextInt(cols);
            tileCoordGrid[r][c] = detailVal;
        }
        return this;
    }

    /**
     * Retorna a matriz do mapa construído.
     */
    public int[][] build() {
        return tileCoordGrid;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
}
