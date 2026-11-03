package com.example.pokeworld.world;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.core.MainActivity;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.ElementalType;
import com.example.pokeworld.entities.Enemy;
import com.example.pokeworld.entities.Loot;
import com.example.pokeworld.entities.NPC;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.entities.BuildingData;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.managers.SpriteManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class WorldManager {
    public TileMap tileMap;
    public List<Enemy> enemies = new ArrayList<>();
    public List<Loot> loots = new ArrayList<>();
    public List<NPC> npcs = new ArrayList<>();
    public List<Building> buildings = new ArrayList<>();
    
    private MainActivity activity;
    private Random random = new Random();
    private float cameraX = 0, cameraY = 0;
    private float currentZoom = 1.0f;
    private float targetZoom = 1.0f;
    private String lastCapturedSpecies = "";
    private int captureChain = 0;
    
    private boolean isMapDark = false;
    private boolean isFlashed = false;

    public void setMapDark(boolean dark) { this.isMapDark = dark; this.isFlashed = false; }
    public void useFlash() { this.isFlashed = true; }
    public boolean isMapDark() { return isMapDark && !isFlashed; }

    public void onCapture(String species) {
        if (species.equals(lastCapturedSpecies)) captureChain++;
        else { captureChain = 1; lastCapturedSpecies = species; }
        activity.dbHelper.incrementCaptureCount(species);
    }
    public int getCaptureChain() { return captureChain; }

    public static int getBiomeBaseLevel(String biome) {
        switch (biome) {
            case "FOREST": return 1;
            case "SEWERS": return 2;
            case "ROCKY_PATH": return 3;
            case "SMALL_LAKE": return 4;
            case "FIRE_CAVE": return 6;
            case "JUNGLE": return 11;
            case "MINE": return 13;
            case "RIO": return 15;
            case "MAGMA_CAVE": return 18;
            case "SWAMP": return 20;
            case "OCEAN": return 26;
            case "DESERT": return 35;
            case "VOLCANO": return 45;
            case "SNOW_MOUNTAIN": return 55;
            case "SNOW_PEAK": return 65;
            case "FEAR_ISLAND": return 75;
            case "SKY_TEMPLE": return 90;
            default: return 1;
        }
    }
    
    private static class RespawnInfo {
        String s; double x, y, r; int c; boolean b, sh; long t;
        RespawnInfo(String s, double x, double y, double r, int c, boolean b, boolean sh, long t) {
            this.s = s; this.x = x; this.y = y; this.r = r; this.c = c; this.b = b; this.sh = sh; this.t = t;
        }
    }
    private List<RespawnInfo> respawnQueue = new ArrayList<>();

    public WorldManager(MainActivity activity) { this.activity = activity; }

    public void loadMap(String name, Player p, Companion c) { loadMap(name, p, c, "CENTER"); }

    public void loadMap(String name, Player p, Companion c, String side) {
        enemies.clear(); npcs.clear(); loots.clear(); buildings.clear();
        int r = 50, col = 100;
        if (name.equals("HOME")) { r = 80; col = 160; }
        tileMap = new TileMap(name, r, col); targetZoom = 1.0f;
        setMapDark(name.contains("CAVE") || name.contains("MINE") || name.equals("SEWERS") || name.equals("DUNGEON"));
        setupPlayerSpawn(p, c, side);
        spawnEntitiesForMap(name, p);
        c.setPosition(p.getPositionX() - 50, p.getPositionY());
    }

    private void setupPlayerSpawn(Player p, Companion c, String side) {
        int rows = tileMap.getRows(), cols = tileMap.getCols();
        double sx = TileMap.TILE_SIZE * (cols/2.0), sy = TileMap.TILE_SIZE * (rows/2.0);
        if (tileMap.getMapName().equals("HOME")) { sx = TileMap.TILE_SIZE * 80; sy = TileMap.TILE_SIZE * 57; }
        else if (side.equals("TOP")) sy = TileMap.TILE_SIZE * 1.5;
        else if (side.equals("BOTTOM")) sy = TileMap.TILE_SIZE * (rows - 1.5);
        else if (side.equals("LEFT")) sx = TileMap.TILE_SIZE * 1.5;
        else if (side.equals("RIGHT")) sx = TileMap.TILE_SIZE * (cols - 1.5);
        p.setPosition(sx, sy);
    }

    private void spawnEntitiesForMap(String name, Player p) {
        switch (name) {
            case "CITY": spawnCityEntities(); break;
            case "FOREST": spawnBasic(new String[]{"Bulbasaur", "Caterpie", "Metapod", "Butterfree", "Weedle", "Kakuna", "Pidgey", "Pidgeotto", "Oddish", "Bellsprout", "Rattata", "Meowth", "Spearow"}, 1, Color.GREEN); break;
            case "JUNGLE": spawnBasic(new String[]{"Ivysaur", "Venusaur", "Gloom", "Vileplume", "Weepinbell", "Victreebel", "Pinsir", "Scyther", "Beedril", "Paras", "Parasect", "Tangela", "Venonat", "Venomoth"}, 11, Color.GREEN); break;
            case "SMALL_LAKE": spawnBasic(new String[]{"Squirtle", "Poliwag", "Poliwhirl", "Psyduck", "Slowpoke", "Magikarp"}, 4, Color.BLUE); break;
            case "RIO": spawnBasic(new String[]{"Wartortle", "Golduck", "Slowpoke", "Slowking", "Poliwhirl", "Tentacool", "Magikarp"}, 15, Color.BLUE); break;
            case "OCEAN": spawnOcean(); break;
            case "ROCKY_PATH": spawnBasic(new String[]{"Sandshrew", "Sandslash", "Diglett", "Dugtrio", "Mankey", "Primeape", "Nidoranf", "Nidoranm", "Nidorina", "Nidorino"}, 3, Color.GRAY); break;
            case "MINE": spawnBasic(new String[]{"Onix", "Machop", "Rhydon", "Diglett", "Dugtrio", "Zubat", "Golbat"}, 13, Color.GRAY); break;
            case "VALE_ROCHOSO": spawnBasic(new String[]{"Abra", "Kadabra", "Alakazam", "Spearow", "Fearow", "Pidgeot", "Clefairy", "Clefable", "Jigglypuff", "Wigglytuff"}, 8, Color.YELLOW); break;
            case "FIRE_CAVE": spawnBasic(new String[]{"Charmander", "Vulpix", "Ponyta", "Growlithe", "Flareon", "Houndour"}, 6, Color.RED); break;
            case "MAGMA_CAVE": spawnBasic(new String[]{"Charmeleon", "Magmar", "Rapidash", "Arcanine", "Ninetales"}, 18, Color.RED); break;
            case "VOLCANO": spawnVolcano(); break;
            case "SEWERS": spawnBasic(new String[]{"Rattata", "Raticate", "Ekans", "Arbok", "Grimer", "Muk", "Koffing", "Weezing", "Zubat"}, 2, Color.DKGRAY); break;
            case "SWAMP": spawnBasic(new String[]{"Arbok", "Weezing", "Muk", "Nidoking", "Nidoqueen", "Gloom", "Victreebel"}, 20, Color.MAGENTA); break;
            case "SNOW_MOUNTAIN": spawnSnow(); break;
            case "DESERT": spawnBasic(new String[]{"Sandshrew", "Sandslash", "Vulpix", "Ninetales", "Ponyta"}, 12, Color.YELLOW); break;
            case "FEAR_ISLAND": spawnBasic(new String[]{"Gastly", "Haunter", "Gengar", "Golbat", "Arbok", "Muk"}, 22, Color.MAGENTA); break;
            case "HOME": spawnHome(p); break;
        }
    }

    private void spawnBasic(String[] s, int base, int color) {
        for (int i = 0; i < 35; i++) {
            double ex = 500 + random.nextInt(5500), ey = 500 + random.nextInt(2500);
            double d = Math.sqrt(Math.pow(ex-3000, 2) + Math.pow(ey-1500, 2));
            int lv = base + (int)(d/400);
            addEnemy(s[random.nextInt(s.length)], ex, ey, 32, color, false, random.nextDouble()<0.05, lv, random.nextInt(2));
        }
    }

    private void spawnCityEntities() {
        double midX = (tileMap.getCols() * TileMap.TILE_SIZE) / 2.0, midY = (tileMap.getRows() * TileMap.TILE_SIZE) / 2.0;
        npcs.add(new NPC("Enfermeira", "Deseja curar seus Pokémons?", midX - 150, midY - 150, NPC.Type.HEALER));
        npcs.add(new NPC("Mercador", "Ouro por itens?", midX + 150, midY - 150, NPC.Type.SHOPKEEPER));
        npcs.add(new NPC("Guarda Real", "Bem-vindo ao Reino!", midX, midY + 300, NPC.Type.GUARD));
        int idx = 0;
        for (Map.Entry<String, BuildingData> entry : activity.dbHelper.getBuildings().entrySet()) {
            String name = entry.getKey();
            if (name.equals("Centro de Cura") || name.equals("Loja") || name.equals("Castelo") || name.equals("Ginásio") || name.equals("Mural de Caça")) {
                double bx = midX + (idx % 2 == 0 ? -800 : 800), by = (midY - 400) + ((idx / 2.0) * 450);
                Building b = new Building(name, bx, by, entry.getValue().level); b.setConstructed(true); buildings.add(b); idx++;
            }
        }
    }

    private void spawnVolcano() {
        int base = getBiomeBaseLevel("VOLCANO");
        spawnBasic(new String[]{"Charizard", "Magmar", "Rapidash", "Arcanine", "Flareon", "Aerodactyl"}, base, Color.RED);
        addEnemy("Charizard_Shiny", 3000, 2800, 60, Color.YELLOW, true, true, base+15, 0);
    }

    private void spawnOcean() { spawnBasic(new String[]{"Blastoise", "Vaporeon", "Lapras", "Gyarados", "Tentacruel", "Poliwrath"}, getBiomeBaseLevel("OCEAN"), Color.BLUE); }
    private void spawnSnow() { spawnBasic(new String[]{"Piloswine", "Cloyster", "Lapras", "Jynx"}, getBiomeBaseLevel("SNOW_MOUNTAIN"), Color.CYAN); addEnemy("Articuno", 3200, 1600, 60, Color.WHITE, true, true, 75, 0); }

    private void spawnHome(Player p) {
        for (CapturedCreature c : p.getStorage()) {
            double ex = TileMap.TILE_SIZE * (22 + random.nextInt(36)), ey = TileMap.TILE_SIZE * (12 + random.nextInt(16));
            PetData d = activity.dbHelper.getPetData(c.name);
            addEnemy(c.name, ex, ey, 32, Color.GREEN, false, c.isShiny, d.level, c.gender, true);
        }
        for (Map.Entry<String, BuildingData> entry : activity.dbHelper.getBuildings().entrySet()) {
            String n = entry.getKey(); BuildingData d = entry.getValue();
            if (n.equals("Oficina") || n.equals("Daycare") || n.equals("Laboratorio") || n.equals("Fabrica de Montaria") || n.equalsIgnoreCase("Torre Choque") || n.equalsIgnoreCase("Tocha") || n.equalsIgnoreCase("Baú") || n.equalsIgnoreCase("Ginásio") || n.equalsIgnoreCase("Mural de Caça") || n.equalsIgnoreCase("Academia")) {
                Building b = new Building(n, d.posX, d.posY, d.level); b.setConstructed(d.isConstructed); b.setConstructionFinishTime(d.constructionFinishTime);
                buildings.add(b);
                if (d.isConstructed) {
                    double npx = b.getCenterX(), npy = b.getCenterY() + (n.equalsIgnoreCase("Tocha") ? 40 : 120);
                    if (n.equals("Oficina")) npcs.add(new NPC("Prof. de Engenharia", "Quer fabricar Pokébolas?", npx, npy, NPC.Type.BLACKSMITH));
                    else if (n.equals("Daycare")) npcs.add(new NPC("Prof. Criador", "Quer cruzar seus Pokémons?", npx, npy, NPC.Type.GUARD));
                    else if (n.equals("Laboratorio")) npcs.add(new NPC("Prof. Invocador", "Deseja invocar novos poderes?", npx, npy, NPC.Type.GUARD));
                    else if (n.equals("Fabrica de Montaria")) npcs.add(new NPC("Prof. de Montaria", "Selas e equipamentos de viagem?", npx, npy, NPC.Type.GUARD));
                    else if (n.equals("Baú")) npcs.add(new NPC("Prof. de Estoque", "Acesse seu baú de itens.", npx, npy, NPC.Type.GUARD));
                    else if (n.equals("Ginásio")) npcs.add(new NPC("Mestre de Batalha", "Pronto para o desafio?", npx, npy, NPC.Type.GUARD));
                    else if (n.equals("Mural de Caça")) npcs.add(new NPC("Prof. Caçador", "Veja as recompensas disponíveis.", npx, npy, NPC.Type.GUARD));
                    else if (n.equals("Academia")) npcs.add(new NPC("Prof. de Treino", "Treine seus Pokémons inativos.", npx, npy, NPC.Type.GUARD));
                }
            }
        }
        npcs.add(new NPC("Mestre da Base", "Gerencie o estoque!", TileMap.TILE_SIZE * 40, TileMap.TILE_SIZE * 31, NPC.Type.GUARD));
    }

    public void update(Player p, Companion c, GameView gv) {
        if (tileMap.isPortal(p.getPositionX(), p.getPositionY())) handlePortal(p, c, gv);
        currentZoom += (targetZoom - currentZoom) * 0.05f;
        updateEnemies(c, gv); handleRespawns(); updateLoots(p, gv);
        if (tileMap.getMapName().equals("HOME")) for (Building b : buildings) b.update(activity);
    }

    private void updateEnemies(Companion c, GameView gv) {
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (!e.isAlive()) {
                int xp = (e.level * 20) + (e.isBoss ? 200 : 0);
                c.gainExp(xp); if (gv.player.gainExp(xp/2)) gv.ui.showMessage("🎊 NÍVEL UP!");
                Loot d = new Loot(e.name, e.positionX, e.positionY, Loot.Type.DEFEATED); d.setOwnerSpecies(e.name); d.setShiny(e.isShiny); d.setRadius(Math.max(65, e.radius * 1.5)); loots.add(d);
                dropLoot(e, gv); respawnQueue.add(new RespawnInfo(e.name, e.startX, e.startY, 32, e.color, e.isBoss, e.isShiny, System.currentTimeMillis() + 30000));
                it.remove();
            } else e.update(c, gv.effects, gv.particles, tileMap, gv.time.isNight());
        }
    }

    private void handleRespawns() {
        long now = System.currentTimeMillis(); Iterator<RespawnInfo> it = respawnQueue.iterator();
        while (it.hasNext()) { RespawnInfo ri = it.next(); if (now >= ri.t) { addEnemy(ri.s, ri.x, ri.y, ri.r, ri.c, ri.b, ri.sh, 1, 0); it.remove(); } }
    }

    private void dropLoot(Enemy e, GameView gv) {
        loots.add(new Loot("Gold", e.positionX, e.positionY, Loot.Type.GOLD));
        if (random.nextDouble() < 0.7) { String item = "Ferro"; if (e.getType() == ElementalType.PLANT) item = "Fruta"; loots.add(new Loot(item, e.positionX + 20, e.positionY, Loot.Type.ITEM)); }
    }

    private void updateLoots(Player p, GameView gv) {
        Iterator<Loot> it = loots.iterator();
        while (it.hasNext()) {
            Loot l = it.next(); 
            if (l.isExpired()) { it.remove(); continue; } 
            l.update(p);
            if (l.getType() == Loot.Type.DEFEATED) continue;
            if (l.isCollectedBy(p)) { 
                if (l.getType() == Loot.Type.GOLD) p.addGold(100); 
                else p.addItem(l.getName()); 
                it.remove(); 
            }
        }
    }

    private void handlePortal(Player p, Companion c, GameView gv) { String m = tileMap.getMapName(); if (m.equals("CITY")) loadMap("FOREST", p, c, "BOTTOM"); else loadMap("CITY", p, c, "CENTER"); }

    public void addEnemy(String s, double x, double y, double r, int c, boolean b, boolean sh) {
        addEnemy(s, x, y, r, c, b, sh, 1, 0, false);
    }

    public void addEnemy(String s, double x, double y, double r, int c, boolean b, boolean sh, int lv) {
        addEnemy(s, x, y, r, c, b, sh, lv, 0, false);
    }

    public void addEnemy(String s, double x, double y, double r, int c, boolean b, boolean sh, int lv, int g) { addEnemy(s,x,y,r,c,b,sh,lv,g,false); }
    public void addEnemy(String s, double x, double y, double r, int c, boolean b, boolean sh, int lv, int g, boolean pet) {
        Enemy e = new Enemy(s, x, y, r, c, b, lv); e.isShiny = sh; e.gender = g; e.isPet = pet;
        if (tileMap.isWalkable(x, y, false, false, false, false)) enemies.add(e);
    }

    public void draw(Canvas canvas, Player p, Companion c, GameView gv) {
        updateCamera(p, canvas.getWidth(), canvas.getHeight());
        canvas.save(); canvas.scale(currentZoom, currentZoom, canvas.getWidth()/2f, canvas.getHeight()/2f);
        canvas.translate(-cameraX, -cameraY);
        if (tileMap != null) tileMap.draw(canvas);
        for (Building b : buildings) b.draw(canvas, p);
        for (NPC n : npcs) n.draw(canvas);
        for (Loot l : loots) l.draw(canvas);
        for (Enemy e : enemies) e.draw(canvas);
        p.draw(canvas, c); if (!p.isMounted()) c.draw(canvas);
        if (gv.effects != null) gv.effects.draw(canvas);
        if (gv.particles != null) gv.particles.draw(canvas);
        canvas.restore();
    }

    private void updateCamera(Player p, int sw, int sh) {
        float vw = sw / currentZoom, vh = sh / currentZoom;
        float tx = (float) p.getPositionX() - vw / 2f, ty = (float) p.getPositionY() - vh / 2f;
        cameraX += (tx - cameraX) * 0.1f; cameraY += (ty - cameraY) * 0.1f;
    }

    public void changeZoom(float d) { targetZoom = Math.max(0.4f, Math.min(1.8f, targetZoom + d)); }
    public boolean isCollidingWithBuilding(double x, double y) { return false; }
    public float getCameraX() { return cameraX; } public float getCameraY() { return cameraY; }
}
