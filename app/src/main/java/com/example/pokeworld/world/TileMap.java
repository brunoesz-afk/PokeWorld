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
    public static final int BASALT_FLOOR = 21, ASH_GROUND = 22, CRACKED_OBSIDIAN = 23, MAGMA_BRIDGE = 24, SULFUR_CRYSTAL = 25, MAGMA_VENT = 26;

    public static final int WALL = Tiles.WALL, CLIFF = Tiles.CLIFF, STAIRS = Tiles.STAIRS, BRIDGE = Tiles.BRIDGE, FLOWERS = Tiles.FLOWERS,
            ROOF = Tiles.ROOF, HOUSE = Tiles.HOUSE, DOOR = Tiles.DOOR, PILLAR = Tiles.PILLAR, BRAZIER = Tiles.BRAZIER,
            DEAD_TREE = Tiles.DEAD_TREE, CACTUS = Tiles.CACTUS, FOUNTAIN = Tiles.FOUNTAIN, TOMBSTONE = Tiles.TOMBSTONE;

    private String mapName;
    /** Renderizador com tiles conectáveis; null só na floresta inicial (que usa o desenho antigo). */
    private TileRenderer renderer;

    public TileMap(String type, int rows, int cols) {
        this.mapName = type;
        this.rows = rows;
        this.cols = cols;
        this.mapData = new int[rows][cols];
        this.paint = new Paint();
        
        if ("STARTER_FOREST".equals(type)) {
            // A floresta inicial continua exatamente como era (grama + árvores).
            generateStarterForestMap();
        } else {
            // Todos os outros mapas são desenhados com composição fixa (MapBuilder)
            // e renderizados com tiles que se conectam (TileRenderer).
            this.mapData = MapBuilder.build(type, rows, cols);
            this.renderer = new TileRenderer(type, mapData, new TileRenderer.Sprites() {
                @Override public Bitmap get(int key) { return SpriteManager.getInstance().getTileSprite(key); }
            });
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

        if (renderer != null) {
            drawWithRenderer(canvas, minRow, maxRow, minCol, maxCol);
            return;
        }
        for (int r = minRow; r < maxRow; r++) {
            for (int c = minCol; c < maxCol; c++) {
                drawTileAt(canvas, mapData[r][c], r, c, sm);
            }
        }
    }

    /** Duas passadas: primeiro chão/água/muros, depois objetos altos (árvores, pilares) por cima dos vizinhos. */
    private void drawWithRenderer(Canvas canvas, int minRow, int maxRow, int minCol, int maxCol) {
        renderer.beginFrame();
        for (int r = minRow; r < maxRow; r++) for (int c = minCol; c < maxCol; c++) renderer.drawTile(canvas, r, c);
        int tallRows = Math.min(rows, maxRow + 2);
        for (int r = minRow; r < tallRows; r++) for (int c = minCol; c < maxCol; c++) renderer.drawTall(canvas, r, c);
    }

    public void draw(Canvas canvas) {
        SpriteManager sm = SpriteManager.getInstance();
        if (renderer != null) { drawWithRenderer(canvas, 0, rows, 0, cols); return; }
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
        } else if (type == LAVA || type == WATER || type == MAGMA_VENT) {
            Bitmap sheet = sm.getTileSprite(type);
            if (sheet != null && sheet.getWidth() >= TILE_SIZE * 3 && sheet.getHeight() == TILE_SIZE) {
                int frameCount = 3;
                int frameIndex = (int)((System.currentTimeMillis() / 400) % frameCount);
                int fw = sheet.getWidth() / frameCount;
                reusableSrc.set(frameIndex * fw, 0, (frameIndex + 1) * fw, sheet.getHeight());
                
                Rect destTile = new Rect(x - 2, y - 2, x + TILE_SIZE + 2, y + TILE_SIZE + 2);
                canvas.drawBitmap(sheet, reusableSrc, destTile, null);
            } else if (sheet != null && sheet.getWidth() >= TILE_SIZE * 3 && sheet.getHeight() >= TILE_SIZE * 3) {
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
            case STONE_PAVEMENT: return Color.rgb(120, 120, 130); case CARPET: return Color.rgb(180, 20, 20); case SNOW_CRYSTAL: return Color.rgb(200, 255, 255);
            case BASALT_FLOOR: return Color.rgb(30, 30, 35); case ASH_GROUND: return Color.rgb(80, 75, 75); case CRACKED_OBSIDIAN: return Color.rgb(50, 20, 40);
            case MAGMA_BRIDGE: return Color.rgb(120, 80, 50); case SULFUR_CRYSTAL: return Color.rgb(255, 215, 0); case MAGMA_VENT: return Color.rgb(255, 60, 0);
            default: return Color.MAGENTA;
        }
    }

    /** Bloqueio extra sobre o mapa (muralhas e prédios da base). */
    private MovementBlocker blocker;
    public void setBlocker(MovementBlocker b) { this.blocker = b; }

    public boolean isWalkable(double x, double y, boolean surf, boolean fly, boolean aquatic, boolean fireRes) {
        int r = (int)(y / TILE_SIZE); int c = (int)(x / TILE_SIZE);
        if (r < 0 || r >= rows || c < 0 || c >= cols) return false;
        if (blocker != null && blocker.blocks(x, y)) return false;
        int t = mapData[r][c];
        if (fly) return true;
        if (t == CLOUD) return false;
        if (t == WATER || t == DEEP_WATER) return surf || aquatic;
        if (t == LAVA || t == MAGMA_VENT) return fireRes;
        return t != TREE && t != ROCK && t != SIGN && t != BERRY_TREE && t != BRICK && t != SNOW_CRYSTAL && t != CRACKED_OBSIDIAN && t != SULFUR_CRYSTAL
                && !Tiles.blocksMovement(t);
    }

    public boolean isPortal(double x, double y) { int r = (int)(y / TILE_SIZE), c = (int)(x / TILE_SIZE); return r >= 0 && r < rows && c >= 0 && c < cols && mapData[r][c] == PORTAL; }
    public int getTileType(double x, double y) { int r = (int)(y / TILE_SIZE), c = (int)(x / TILE_SIZE); if (r >= 0 && r < rows && c >= 0 && c < cols) return mapData[r][c]; return -1; }
    public void setTileType(double x, double y, int type) { int r = (int)(y / TILE_SIZE), c = (int)(x / TILE_SIZE); if (r >= 0 && r < rows && c >= 0 && c < cols) mapData[r][c] = type; }
    public String getMapName() { return mapName; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
}
