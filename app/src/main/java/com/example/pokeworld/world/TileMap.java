package com.example.pokeworld.world;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.pokeworld.managers.SpriteManager;

import java.util.Random;

public class TileMap {
    public static final int TILE_SIZE = 64;
    private int rows, cols;
    private int[][] mapData;
    private Paint paint;
    
    public static final int GRASS = 0, TREE = 1, PORTAL = 2, WATER = 3, FLOOR = 4, ROCK = 5, SAND = 6, SNOW = 7, BRICK = 8, LAVA = 9, DEEP_WATER = 10, CLOUD = 11, OBSIDIAN = 12, BUSH = 13, ROAD = 14, SIGN = 15, SOIL = 16, BERRY_TREE = 17, STONE_PAVEMENT = 18, CARPET = 19, SNOW_CRYSTAL = 20;

    private String mapName;

    public TileMap(String type, int rows, int cols) {
        this.mapName = type;
        this.rows = rows;
        this.cols = cols;
        this.mapData = new int[rows][cols];
        this.paint = new Paint();
        
        switch (type) {
            case "CITY": generateCityMap(); break;
            case "FOREST": generateForestMap(); break;
            case "SMALL_LAKE": generateSmallLakeMap(); break;
            case "ROCKY_PATH": generateRockyPathMap(); break;
            case "JUNGLE": generateJungleMap(); break;
            case "MAGMA_CAVE": generateMagmaCaveMap(); break;
            case "SWAMP": generateSwampMap(); break;
            case "FIRE_CAVE": generateFireCaveMap(); break;
            case "SEWERS": generateSewersMap(); break;
            case "VALE_ROCHOSO": generateValeRochosoMap(); break;
            case "MINE": generateMineMap(); break;
            case "RIO": generateRioMap(); break;
            case "OCEAN": generateOceanMap(); break;
            case "DESERT": generateDesertMap(); break;
            case "VOLCANO": generateVolcanoMap(); break;
            case "SNOW_MOUNTAIN": generateSnowMountainMap(); break;
            case "SNOW_PEAK": generateSnowPeakMap(); break;
            case "FEAR_ISLAND": generateFearIslandMap(); break;
            case "HOME": generateHomeMap(); break;
            case "STARTER_FOREST": generateStarterForestMap(); break;
            case "TOWER": generateTowerMap(); break;
            default: generateForestMap(); break;
        }
    }

    private void generateStarterForestMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1 || r == 1 || r == rows - 2 || c == 1 || c == cols - 2) {
                    mapData[r][c] = TREE;
                } else if (rand.nextDouble() < 0.12) {
                    mapData[r][c] = TREE;
                } else {
                    mapData[r][c] = GRASS;
                }
            }
        }
        for (int r = rows/2 - 4; r <= rows/2 + 4; r++) {
            for (int c = cols/2 - 4; c <= cols/2 + 4; c++) {
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    mapData[r][c] = GRASS;
                }
            }
        }
    }

    private void generateTowerMap() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == 0 || r == rows - 1 || c == 0 || c == cols - 1) {
                    mapData[r][c] = BRICK;
                } else {
                    mapData[r][c] = FLOOR;
                }
            }
        }
        mapData[rows - 1][cols / 2] = PORTAL;
    }

    private void generateHomeMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Core de 40x36 centralizado no mapa de 80x160
                boolean isCoreX = (c >= 60 && c <= 100);
                boolean isCoreY = (r >= 22 && r <= 58);
                
                boolean isWallX = (c == 60 || c == 100) && isCoreY;
                boolean isWallY = (r == 22 || r == 58) && isCoreX;

                if (isWallX || isWallY) {
                    // Portão no centro da parede sul
                    if (r == 58 && c >= 78 && c <= 82) mapData[r][c] = ROAD;
                    else mapData[r][c] = BRICK; 
                } else if (isCoreX && isCoreY) {
                    boolean isMainVertical = (c >= 78 && c <= 82);
                    boolean isMainHorizontal = (r >= 38 && r <= 42);
                    
                    if (isMainVertical || isMainHorizontal) mapData[r][c] = ROAD;
                    else {
                        double rval = rand.nextDouble();
                        if (rval < 0.03) mapData[r][c] = TREE;
                        else if (rval < 0.05) mapData[r][c] = ROCK;
                        else mapData[r][c] = GRASS;
                    }
                } else {
                    mapData[r][c] = rand.nextDouble() < 0.15 ? TREE : GRASS;
                }
            }
        }
        // Estrada principal atravessando os arredores até o fim do mapa
        for (int r = 59; r < rows; r++) {
            for (int w = -2; w <= 2; w++) mapData[r][80 + w] = ROAD;
        }
    }

    private void generateCityMap() {
        Random rand = new Random();
        // Core da cidade (um quadrado limpo e centralizado no mapa 50x100, estilo Base)
        int coreTop = 10, coreBottom = 40;
        int coreLeft = 20, coreRight = 80;

        int midR = rows / 2;
        int midC = cols / 2;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                boolean isCoreX = (c >= coreLeft && c <= coreRight);
                boolean isCoreY = (r >= coreTop && r <= coreBottom);

                if (isCoreX && isCoreY) {
                    // Estradas cruzando o centro do quadrado limpo
                    boolean isMainVertical = (c >= midC - 2 && c <= midC + 2);
                    boolean isMainHorizontal = (r >= midR - 2 && r <= midR + 2);

                    if (isMainVertical || isMainHorizontal) {
                        mapData[r][c] = ROAD;
                    } else {
                        // Quadrado limpo com chão de pedra
                        mapData[r][c] = STONE_PAVEMENT;
                    }
                } else {
                    // Em volta: grama e árvores
                    mapData[r][c] = GRASS;
                    if (rand.nextDouble() < 0.20) {
                        mapData[r][c] = TREE;
                    }
                }
            }
        }

        // Portais de Saída nas bordas do mapa total
        for (int c = 0; c < cols; c++) { mapData[0][c] = PORTAL; mapData[rows - 1][c] = PORTAL; }
        for (int r = 0; r < rows; r++) { mapData[r][0] = PORTAL; mapData[r][cols - 1] = PORTAL; }
    }

    private void generateForestMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = rand.nextDouble() < 0.3 ? TREE : GRASS;
        int midC = cols / 2;
        for (int r = 0; r < rows; r++) mapData[r][midC] = ROAD;
        mapData[0][midC] = PORTAL; mapData[rows-1][midC] = PORTAL;
    }

    private void generateSmallLakeMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = SAND;
        int midR = rows / 2, midC = cols / 2;
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            double d = Math.sqrt(Math.pow(r - midR, 2) + Math.pow(c - midC, 2) * 0.5);
            if (d < 12) mapData[r][c] = WATER;
            if (d < 8) mapData[r][c] = DEEP_WATER;
        }
        for (int i = 0; i < 40; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            if (mapData[rr][rc] == SAND) mapData[rr][rc] = rand.nextDouble() < 0.7 ? TREE : ROCK;
        }
        mapData[midR][0] = PORTAL; mapData[midR][cols - 1] = PORTAL;
    }

    private void generateRockyPathMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = FLOOR;
        for (int i = 0; i < 150; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            mapData[rr][rc] = rand.nextDouble() < 0.8 ? ROCK : BUSH;
        }
        mapData[rows / 2][0] = PORTAL; mapData[rows / 2][cols - 1] = PORTAL;
    }

    private void generateJungleMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = GRASS;
        for (int i = 0; i < 200; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            mapData[rr][rc] = rand.nextDouble() < 0.9 ? TREE : BERRY_TREE;
        }
        mapData[rows / 2][0] = PORTAL; mapData[rows / 2][cols - 1] = PORTAL;
    }

    private void generateMagmaCaveMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = OBSIDIAN;
        for (int i = 0; i < 30; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            for (int dr = -3; dr <= 3; dr++) for (int dc = -3; dc <= 3; dc++) {
                int nr = rr + dr, nc = rc + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && rand.nextDouble() < 0.7) mapData[nr][nc] = LAVA;
            }
        }
        mapData[rows / 2][0] = PORTAL; mapData[rows / 2][cols - 1] = PORTAL;
    }

    private void generateSwampMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = GRASS;
        for (int i = 0; i < 40; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            int size = 2 + rand.nextInt(3);
            for (int dr = -size; dr <= size; dr++) for (int dc = -size; dc <= size; dc++) {
                int nr = rr + dr, nc = rc + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && Math.sqrt(dr*dr + dc*dc) < size) mapData[nr][nc] = WATER;
            }
        }
        mapData[rows / 2][0] = PORTAL; mapData[rows / 2][cols - 1] = PORTAL;
    }

    private void generateFireCaveMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = FLOOR;
        for (int i = 0; i < 20; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            for (int dr = -2; dr <= 2; dr++) for (int dc = -2; dc <= 2; dc++) {
                int nr = rr + dr, nc = rc + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && rand.nextDouble() < 0.6) mapData[nr][nc] = LAVA;
            }
        }
        mapData[rows / 2][0] = PORTAL; mapData[rows / 2][cols - 1] = PORTAL;
    }

    private void generateSewersMap() {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = BRICK;
        int midR = rows / 2;
        for (int c = 0; c < cols; c++) {
            mapData[midR][c] = WATER; // Água de esgoto no meio
            if (midR > 0) mapData[midR - 1][c] = FLOOR;
            if (midR < rows - 1) mapData[midR + 1][c] = FLOOR;
        }
        mapData[midR][0] = PORTAL; mapData[midR][cols - 1] = PORTAL;
    }

    private void generateValeRochosoMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = SAND;
        int midC = cols / 2;
        for (int r = 0; r < rows; r++) mapData[r][midC] = ROAD;
        for (int i = 0; i < 150; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            if (mapData[rr][rc] == SAND) {
                double rv = rand.nextDouble();
                if (rv < 0.7) mapData[rr][rc] = ROCK;
                else if (rv < 0.9) mapData[rr][rc] = BUSH;
                else mapData[rr][rc] = BERRY_TREE;
            }
        }
        mapData[0][midC] = PORTAL; mapData[rows-1][midC] = PORTAL;
    }

    private void generateMineMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = FLOOR;
        int midC = cols / 2;
        for (int r = 0; r < rows; r++) mapData[r][midC] = ROAD;
        for (int i = 0; i < 50; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            if (mapData[rr][rc] == FLOOR) mapData[rr][rc] = ROCK;
        }
        mapData[0][midC] = PORTAL; mapData[rows-1][midC] = PORTAL;
    }

    private void generateSnowMountainMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = SNOW;
        int midC = cols / 2;
        for (int r = 0; r < rows; r++) for (int w = -1; w <= 1; w++) mapData[r][midC + w] = ROAD;
        for (int i = 0; i < 30; i++) {
            int rootR = rand.nextInt(rows), rootC = rand.nextInt(cols);
            int size = 2 + rand.nextInt(4);
            for (int dr = -size; dr <= size; dr++) for (int dc = -size; dc <= size; dc++) {
                int nr = rootR + dr, nc = rootC + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols && dr*dr + dc*dc < size*size) mapData[nr][nc] = (rand.nextDouble() < 0.7) ? WATER : ROCK;
            }
        }
        for (int i = 0; i < 60; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            if (mapData[rr][rc] == SNOW) mapData[rr][rc] = SNOW_CRYSTAL;
        }
        mapData[0][midC] = PORTAL; mapData[rows-1][midC] = PORTAL;
    }

    private void generateSnowPeakMap() {
        Random rand = new Random();
        int midC = cols / 2;
        for (int r = 0; r < rows; r++) {
            int peakWidth = (int)(10 + (cols - 20) * (r / (float)rows)); 
            for (int c = 0; c < cols; c++) {
                if (c >= midC - peakWidth/2 && c <= midC + peakWidth/2) mapData[r][c] = SNOW;
                else mapData[r][c] = CLOUD; 
            }
        }
        for (int r = 0; r < rows; r++) if (mapData[r][midC] == SNOW) mapData[r][midC] = ROAD;
        for (int i = 0; i < 60; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            if (mapData[rr][rc] == SNOW) mapData[rr][rc] = rand.nextDouble() < 0.7 ? ROCK : WATER;
        }
        for (int i = 0; i < 40; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            if (mapData[rr][rc] == SNOW) mapData[rr][rc] = SNOW_CRYSTAL;
        }
        mapData[rows-1][midC] = PORTAL; 
    }

    private void generateDesertMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = SAND;
        int midC = cols / 2;
        for (int r = 0; r < rows; r++) mapData[r][midC] = ROAD;
        for (int i = 0; i < 15; i++) {
            int rootR = rand.nextInt(rows), rootC = rand.nextInt(cols);
            int size = 2 + rand.nextInt(3);
            for (int dr = -size; dr <= size; dr++) for (int dc = -size; dc <= size; dc++) {
                int nr = rootR + dr, nc = rootC + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    double d = Math.sqrt(dr*dr + dc*dc);
                    if (d < size) mapData[nr][nc] = rand.nextBoolean() ? WATER : ROCK;
                    if (d < size - 1 && mapData[nr][nc] == WATER) mapData[nr][nc] = DEEP_WATER;
                }
            }
        }
        for (int i = 0; i < 100; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            if (mapData[rr][rc] == SAND && rand.nextDouble() < 0.2) mapData[rr][rc] = ROAD;
        }
        mapData[0][midC] = PORTAL; mapData[rows-1][midC] = PORTAL;
    }

    private void generateVolcanoMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = FLOOR;
        int midC = cols / 2;
        for (int r = 0; r < rows; r++) mapData[r][midC] = ROAD;
        for (int i = 0; i < 40; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            for (int dr = -4; dr <= 4; dr++) for (int dc = -2; dc <= 2; dc++) {
                int nr = rr+dr, nc = rc+dc;
                if (nr>=0 && nr<rows && nc>=0 && nc<cols && rand.nextDouble()<0.85) mapData[nr][nc] = LAVA;
            }
        }
        for (int i = 0; i < 100; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            if (mapData[rr][rc] == FLOOR) mapData[rr][rc] = OBSIDIAN;
        }
        mapData[0][midC] = PORTAL; mapData[rows-1][midC] = PORTAL;
    }

    private void generateRioMap() {
        Random rand = new Random();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = SAND;
        int midR = rows / 2;
        for (int c = 0; c < cols; c++) {
            int offset = (int)(Math.sin(c * 0.1) * 4);
            for (int dr = -5; dr <= 5; dr++) {
                int nr = midR + offset + dr;
                if (nr >= 0 && nr < rows) {
                    mapData[nr][c] = WATER;
                    if (Math.abs(dr) < 3) mapData[nr][c] = DEEP_WATER;
                }
            }
        }
        for (int i = 0; i < 80; i++) {
            int rr = rand.nextInt(rows), rc = rand.nextInt(cols);
            if (mapData[rr][rc] == SAND) {
                double rv = rand.nextDouble();
                if (rv < 0.3) mapData[rr][rc] = GRASS;
                else if (rv < 0.6) mapData[rr][rc] = TREE;
                else if (rv < 0.8) mapData[rr][rc] = ROCK;
            }
        }
        mapData[midR][0] = PORTAL;
        mapData[midR][cols-1] = PORTAL;
    }

    private void generateOceanMap() {
        Random rand = new Random();
        // Todo o fundo é água profunda
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = DEEP_WATER;
        
        // Grandes Arquipélagos de Areia
        for (int i = 0; i < 40; i++) {
            int rootR = rand.nextInt(rows), rootC = rand.nextInt(cols);
            int size = 5 + rand.nextInt(8);
            for (int dr = -size; dr <= size; dr++) for (int dc = -size; dc <= size; dc++) {
                int nr = rootR + dr, nc = rootC + dc;
                if (nr >= 0 && nr < rows && nc >= 0 && nc < cols) {
                    double d = Math.sqrt(dr*dr + dc*dc);
                    if (d < size) mapData[nr][nc] = WATER;
                    if (d < size - 2) mapData[nr][nc] = SAND;
                    if (d < size - 5) mapData[nr][nc] = GRASS;
                    if (d < 1 && rand.nextDouble() < 0.5) mapData[nr][nc] = BERRY_TREE;
                    if (d > size - 1 && d < size && rand.nextDouble() < 0.3) mapData[nr][nc] = ROCK;
                }
            }
        }
        int midR = rows / 2;
        mapData[midR][0] = PORTAL;
        mapData[midR][cols - 1] = PORTAL;
    }

    private void generateFearIslandMap() {
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) mapData[r][c] = GRASS;
        mapData[0][cols/2] = PORTAL;
    }

    private final Rect reusableDest = new Rect();
    private final Rect reusableSrc = new Rect();

    public void draw(Canvas canvas, com.example.pokeworld.entities.Player p, float screenWidth, float screenHeight, float zoom) {
        SpriteManager sm = SpriteManager.getInstance();
        float visibleW = screenWidth / zoom;
        float visibleH = screenHeight / zoom;

        int minCol = Math.max(0, (int) ((p.getPositionX() - visibleW / 2f) / TILE_SIZE) - 2);
        int maxCol = Math.min(cols, (int) ((p.getPositionX() + visibleW / 2f) / TILE_SIZE) + 2);
        int minRow = Math.max(0, (int) ((p.getPositionY() - visibleH / 2f) / TILE_SIZE) - 2);
        int maxRow = Math.min(rows, (int) ((p.getPositionY() + visibleH / 2f) / TILE_SIZE) + 2);

        for (int r = minRow; r < maxRow; r++) {
            for (int c = minCol; c < maxCol; c++) {
                drawTileAt(canvas, mapData[r][c], r, c, sm);
            }
        }
    }

    public void draw(Canvas canvas) {
        SpriteManager sm = SpriteManager.getInstance();
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) drawTileAt(canvas, mapData[r][c], r, c, sm);
    }

    private void drawTileAt(Canvas canvas, int type, int r, int c, SpriteManager sm) {
        int x = c * TILE_SIZE, y = r * TILE_SIZE;
        reusableDest.set(x, y, x + TILE_SIZE, y + TILE_SIZE);
        Rect dest = reusableDest;
        if (type == TREE || type == BERRY_TREE) {
            Bitmap grass = sm.getTileSprite(GRASS);
            if (grass != null) canvas.drawBitmap(grass, null, dest, null);
            int treeSize = (int)(TILE_SIZE * 2.5f);
            int off = (treeSize - TILE_SIZE) / 2;
            Rect tDest = new Rect(x - off, y - (treeSize - TILE_SIZE), x + TILE_SIZE + off, y + TILE_SIZE);
            Bitmap s = sm.getTileSprite(type);
            if (s != null) canvas.drawBitmap(s, null, tDest, null);
            else drawFallbackColor(canvas, type, x, y);
        } else if (type == SNOW_CRYSTAL) {
            Bitmap snow = sm.getTileSprite(SNOW);
            if (snow != null) {
                int fw = snow.getWidth() / 3, fh = snow.getHeight() / 3;
                reusableSrc.set(fw, fh, fw * 2, fh * 2);
                canvas.drawBitmap(snow, reusableSrc, dest, null);
            }
            int crySize = (int)(TILE_SIZE * 2.0f);
            int off = (crySize - TILE_SIZE) / 2;
            Rect cDest = new Rect(x - off, y - off, x + TILE_SIZE + off, y + TILE_SIZE + off);
            Bitmap s = sm.getTileSprite(type);
            if (s != null) canvas.drawBitmap(s, null, cDest, null);
            else drawFallbackColor(canvas, type, x, y);
        } else if (type == LAVA || type == WATER) {
            Bitmap sheet = sm.getTileSprite(type);
            if (sheet != null && sheet.getWidth() >= TILE_SIZE * 3 && sheet.getHeight() == TILE_SIZE) {
                // Animação 3x1 para Lava e Água (Estilo vulcan)
                int frameCount = 3;
                int frameIndex = (int)((System.currentTimeMillis() / 400) % frameCount);
                int fw = sheet.getWidth() / frameCount;
                reusableSrc.set(frameIndex * fw, 0, (frameIndex + 1) * fw, sheet.getHeight());
                
                // Overlap para evitar frestas
                Rect destTile = new Rect(x - 2, y - 2, x + TILE_SIZE + 2, y + TILE_SIZE + 2);
                canvas.drawBitmap(sheet, reusableSrc, destTile, null);
            } else if (sheet != null && sheet.getWidth() >= TILE_SIZE * 3 && sheet.getHeight() >= TILE_SIZE * 3) {
                // Fallback para Autotiling 3x3 se a imagem for quadrada
                int sr = 1, sc = 1; 
                boolean t = (r > 0 && mapData[r-1][c] == type), b = (r < rows - 1 && mapData[r+1][c] == type);
                boolean l = (c > 0 && mapData[r][c-1] == type), rr = (c < cols - 1 && mapData[r][c+1] == type);
                if (!t && !l) { sc = 0; sr = 0; } else if (!t && !rr) { sc = 2; sr = 0; } else if (!b && !l) { sc = 0; sr = 2; } else if (!b && !rr) { sc = 2; sr = 2; }
                else if (!t) { sc = 1; sr = 0; } else if (!b) { sc = 1; sr = 2; } else if (!l) { sc = 0; sr = 1; } else if (!rr) { sc = 2; sr = 1; }
                int fw = sheet.getWidth() / 3, fh = sheet.getHeight() / 3;
                reusableSrc.set(sc * fw, sr * fh, (sc + 1) * fw, (sr + 1) * fh);
                canvas.drawBitmap(sheet, reusableSrc, dest, null);
            } else {
                Bitmap s = sm.getTileSprite(type);
                if (s != null) canvas.drawBitmap(s, null, dest, null);
                else drawFallbackColor(canvas, type, x, y);
            }
        } else if (type == SNOW || type == SAND) {
            Bitmap sheet = sm.getTileSprite(type);
            if (sheet != null && sheet.getWidth() >= TILE_SIZE * 3 && sheet.getHeight() >= TILE_SIZE * 3) {
                int sr = 1, sc = 1; 
                boolean t = (r > 0 && mapData[r-1][c] == type), b = (r < rows - 1 && mapData[r+1][c] == type);
                boolean l = (c > 0 && mapData[r][c-1] == type), rr = (c < cols - 1 && mapData[r][c+1] == type);
                if (!t && !l) { sc = 0; sr = 0; } else if (!t && !rr) { sc = 2; sr = 0; } else if (!b && !l) { sc = 0; sr = 2; } else if (!b && !rr) { sc = 2; sr = 2; }
                else if (!t) { sc = 1; sr = 0; } else if (!b) { sc = 1; sr = 2; } else if (!l) { sc = 0; sr = 1; } else if (!rr) { sc = 2; sr = 1; }
                int fw = sheet.getWidth() / 3, fh = sheet.getHeight() / 3;
                reusableSrc.set(sc * fw, sr * fh, (sc + 1) * fw, (sr + 1) * fh);
                canvas.drawBitmap(sheet, reusableSrc, dest, null);
            } else {
                Bitmap s = sm.getTileSprite(type);
                if (s != null) canvas.drawBitmap(s, null, dest, null);
                else drawFallbackColor(canvas, type, x, y);
            }
        } else {
            Bitmap s = sm.getTileSprite(type);
            if (s != null) canvas.drawBitmap(s, null, dest, null);
            else drawFallbackColor(canvas, type, x, y);
        }
    }

    private void drawFallbackColor(Canvas canvas, int type, int x, int y) { paint.setColor(getTileColor(type)); canvas.drawRect(x, y, x + TILE_SIZE, y + TILE_SIZE, paint); }
    private int getTileColor(int type) {
        switch(type) {
            case GRASS: return Color.GREEN; case TREE: return Color.rgb(0, 100, 0); case PORTAL: return Color.YELLOW; case WATER: return Color.BLUE;
            case FLOOR: return Color.LTGRAY; case ROCK: return Color.GRAY; case SAND: return Color.rgb(240, 230, 140); case SNOW: return Color.WHITE;
            case BRICK: return Color.rgb(139, 69, 19); case LAVA: return Color.RED; case DEEP_WATER: return Color.rgb(0, 0, 139); case CLOUD: return Color.rgb(200, 230, 255);
            case OBSIDIAN: return Color.rgb(40, 20, 50); case BUSH: return Color.rgb(34, 139, 34); case ROAD: return Color.rgb(180, 160, 140);
            case SIGN: return Color.rgb(160, 82, 45); case SOIL: return Color.rgb(100, 70, 30); case BERRY_TREE: return Color.rgb(50, 150, 50);
            case STONE_PAVEMENT: return Color.rgb(120, 120, 130); case CARPET: return Color.rgb(180, 20, 20); case SNOW_CRYSTAL: return Color.rgb(200, 255, 255); default: return Color.MAGENTA;
        }
    }

    public boolean isWalkable(double x, double y, boolean surf, boolean fly, boolean aquatic, boolean fireRes) {
        int r = (int)(y / TILE_SIZE); int c = (int)(x / TILE_SIZE);
        if (r < 0 || r >= rows || c < 0 || c >= cols) return false;
        int t = mapData[r][c];
        if (fly) return true;
        if (t == WATER || t == DEEP_WATER) return surf || aquatic;
        if (t == LAVA) return fireRes;
        return t != TREE && t != ROCK && t != SIGN && t != BERRY_TREE && t != BRICK && t != SNOW_CRYSTAL;
    }

    public boolean isPortal(double x, double y) { int r = (int)(y / TILE_SIZE), c = (int)(x / TILE_SIZE); return r >= 0 && r < rows && c >= 0 && c < cols && mapData[r][c] == PORTAL; }
    public int getTileType(double x, double y) { int r = (int)(y / TILE_SIZE), c = (int)(x / TILE_SIZE); if (r >= 0 && r < rows && c >= 0 && c < cols) return mapData[r][c]; return -1; }
    public void setTileType(double x, double y, int type) { int r = (int)(y / TILE_SIZE), c = (int)(x / TILE_SIZE); if (r >= 0 && r < rows && c >= 0 && c < cols) mapData[r][c] = type; }
    public String getMapName() { return mapName; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
}
