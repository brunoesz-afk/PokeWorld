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
import com.example.pokeworld.entities.RemotePlayer;
import com.example.pokeworld.managers.MultiplayerManager;
import com.example.pokeworld.managers.SpriteManager;
import com.example.pokeworld.ui.UIManager;

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

    // ---- Base do jogador (mapa HOME) ----
    public BaseManager base;            // null fora da HOME
    public BaseDefense defense;
    private final List<NPC> baseNpcs = new ArrayList<>();
    private BaseDefense.Host defenseHost;
    private boolean wasBuilding = false;
    private float savedZoom = 1.3f;
    private double buildCamX, buildCamY;
    
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
            case "MOLTEN_SANCTUM_BEGINNER": return 10;
            case "MOLTEN_SANCTUM": return 50;
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
        else if (name.equals("TOWER")) { r = 25; col = 25; }
        if (defense != null) defense.abort();
        tileMap = new TileMap(name, r, col);
        base = null; defense = null; defenseHost = null; baseNpcs.clear(); wasBuilding = false;
        if (name.equals("HOME")) setupBase(r, col);
        targetZoom = 1.3f; 
        currentZoom = 1.3f;
        setMapDark(name.contains("CAVE") || name.contains("MINE") || name.equals("SEWERS") || name.equals("DUNGEON"));
        setupPlayerSpawn(p, c, side);
        spawnEntitiesForMap(name, p);
        c.setPosition(p.getPositionX() - 50, p.getPositionY());
        MultiplayerManager.getInstance().checkMapForMultiplayer(name);
    }

    private void setupPlayerSpawn(Player p, Companion c, String side) {
        int rows = tileMap.getRows(), cols = tileMap.getCols();
        double sx = TileMap.TILE_SIZE * (cols/2.0), sy = TileMap.TILE_SIZE * (rows/2.0);
        if (tileMap.getMapName().equals("HOME")) { sx = TileMap.TILE_SIZE * 80; sy = TileMap.TILE_SIZE * 57; }
        else if (tileMap.getMapName().equals("TOWER")) { sx = TileMap.TILE_SIZE * (cols / 2.0); sy = TileMap.TILE_SIZE * (rows - 4.0); }
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
            case "MOLTEN_SANCTUM_BEGINNER": spawnMoltenSanctumBeginner(); break;
            case "MOLTEN_SANCTUM": spawnMoltenSanctum(); break;
            case "SEWERS": spawnBasic(new String[]{"Rattata", "Raticate", "Ekans", "Arbok", "Grimer", "Muk", "Koffing", "Weezing", "Zubat"}, 2, Color.DKGRAY); break;
            case "SWAMP": spawnBasic(new String[]{"Arbok", "Weezing", "Muk", "Nidoking", "Nidoqueen", "Gloom", "Victreebel"}, 20, Color.MAGENTA); break;
            case "SNOW_MOUNTAIN": spawnSnow(); break;
            case "DESERT": spawnBasic(new String[]{"Sandshrew", "Sandslash", "Vulpix", "Ninetales", "Ponyta"}, 12, Color.YELLOW); break;
            case "FEAR_ISLAND": spawnBasic(new String[]{"Gastly", "Haunter", "Gengar", "Golbat", "Arbok", "Muk"}, 22, Color.MAGENTA); break;
            case "HOME": spawnHome(p); break;
        }
    }

    private void spawnBasic(String[] s, int base, int color) {
        // Spawn inicial otimizado (10 pokémons ao redor do jogador em vez de 35)
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 150 + random.nextInt(600);
            double ex = 3000 + Math.cos(angle) * distance;
            double ey = 1500 + Math.sin(angle) * distance;
            double d = Math.sqrt(Math.pow(ex-3000, 2) + Math.pow(ey-1500, 2));
            int lv = base + (int)(d/400);
            addEnemy(s[random.nextInt(s.length)], ex, ey, 32, color, false, random.nextDouble()<0.05, Math.max(1, lv), random.nextInt(2));
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
        int maxGuards = Math.min(p.getMaxGuardCapacity(), base != null ? base.guardCapacity() : 6);
        int spawnedGuards = 0;
        for (CapturedCreature c : p.getStorage()) {
            if (spawnedGuards >= maxGuards) break;
            PetData d = activity.dbHelper.getPetData(c.name);
            // Procura um ponto livre dentro das muralhas (o pátio do anel inicial)
            for (int tries = 0; tries < 12; tries++) {
                double ex = TileMap.TILE_SIZE * (BaseLayout.RING_L + 3 + random.nextInt(BaseLayout.RING_R - BaseLayout.RING_L - 5));
                double ey = TileMap.TILE_SIZE * (BaseLayout.RING_T + 3 + random.nextInt(BaseLayout.RING_B - BaseLayout.RING_T - 5));
                int before = enemies.size();
                addEnemy(c.name, ex, ey, 32, Color.GREEN, false, c.isShiny, d.level, c.gender, true);
                if (enemies.size() > before) { spawnedGuards++; break; }
            }
        }
        refreshBaseNpcs();
    }

    /** Cria a base (grade + peças salvas) e liga a colisão dela ao mapa. */
    private void setupBase(int rows, int cols) {
        base = new BaseManager(rows, cols);
        base.renderer.setSprites(new BaseRenderer.SpriteSource() {
            @Override public Bitmap get(String key) { return SpriteManager.getInstance().getBuildingSprite(key); }
        });
        final com.example.pokeworld.core.DatabaseHelper db = activity.dbHelper;
        base.load(db.getBasePieces(), new BaseManager.Store() {
            @Override public void insert(BasePiece p) { db.insertBasePiece(p); }
            @Override public void update(BasePiece p) { db.updateBasePiece(p); }
            @Override public void delete(BasePiece p) { db.deleteBasePiece(p); }
            @Override public void insertAll(List<BasePiece> l) { db.insertBasePieces(l); }
            @Override public void updateAll(List<BasePiece> l) { db.updateBasePieces(l); }
        }, System.currentTimeMillis());
        tileMap.setBlocker(base);
    }

    /** Recria os NPCs que ficam na porta de cada prédio da base (some o NPC de prédio em obra). */
    public void refreshBaseNpcs() {
        npcs.removeAll(baseNpcs);
        baseNpcs.clear();
        if (base == null) return;
        for (BasePiece pc : base.layout.getPieces()) {
            if (!pc.isUsable()) continue;
            double fx = pc.centerX(), fy = (pc.gy + pc.kind.h) * (double) TileMap.TILE_SIZE + 40;
            switch (pc.kind) {
                case QUARTEL:
                    baseNpcs.add(new NPC("Prof. de Time", "Gerencie os Pokémons do seu time e da sua base!", fx, fy, NPC.Type.GUARD));
                    baseNpcs.add(new NPC("Mestre da Base", "Gerencie o estoque!", fx - 100, fy, NPC.Type.GUARD));
                    baseNpcs.add(new NPC("Mestre da Torre", "Quer desafiar as ondas de Pokémons na Torre?", fx + 100, fy, NPC.Type.GUARD));
                    break;
                case OFICINA: baseNpcs.add(new NPC("Prof. de Engenharia", "Quer fabricar Pokébolas?", fx, fy, NPC.Type.BLACKSMITH)); break;
                case DAYCARE: baseNpcs.add(new NPC("Prof. Criador", "Quer cruzar seus Pokémons?", fx, fy, NPC.Type.GUARD)); break;
                case LABORATORIO: baseNpcs.add(new NPC("Prof. Invocador", "Deseja invocar novos poderes?", fx, fy, NPC.Type.GUARD)); break;
                case FABRICA: baseNpcs.add(new NPC("Prof. de Montaria", "Selas e equipamentos de viagem?", fx, fy, NPC.Type.GUARD)); break;
                case BAU: baseNpcs.add(new NPC("Prof. de Estoque", "Acesse seu baú de itens.", fx, fy, NPC.Type.GUARD)); break;
                case ACADEMIA: baseNpcs.add(new NPC("Prof. de Treino", "Treine seus Pokémons inativos.", fx, fy, NPC.Type.GUARD)); break;
                default: break;
            }
        }
        npcs.addAll(baseNpcs);
    }

    private void ensureDefense(final GameView gv) {
        if (base == null || defense != null) return;
        defenseHost = new BaseDefense.Host() {
            @Override public List<Enemy> enemies() { return enemies; }
            @Override public Enemy spawnRaider(String species, double x, double y, int level) {
                for (int i = 0; i < 16; i++) {
                    double ex = x + (random.nextDouble() - 0.5) * 6 * TileMap.TILE_SIZE, ey = y + (random.nextDouble() - 0.5) * 6 * TileMap.TILE_SIZE;
                    if (!tileMap.isWalkable(ex, ey, false, false, false, false)) continue;
                    Enemy e = new Enemy(species, ex, ey, 32, Color.RED, false, level);
                    e.isRaider = true;
                    enemies.add(e);
                    return e;
                }
                return null;
            }
            @Override public void removeEnemy(Enemy e) { enemies.remove(e); }
            @Override public boolean hasElectricPokemon() {
                for (CapturedCreature c : gv.player.getCreatureCollection())
                    if (c.name.equalsIgnoreCase("Pikachu") || c.name.equalsIgnoreCase("Raichu")) return true;
                return false;
            }
            @Override public void say(String msg) { gv.ui.showMessage(msg); }
            @Override public void text(String t, double x, double y, int color) { if (gv.effects != null) gv.effects.addText(t, x, y, color); }
            @Override public void burst(double x, double y, int color, int n) { if (gv.particles != null) gv.particles.addExplosion(x, y, color, n); }
            @Override public void giveItem(String item, int n) { gv.player.addItem(item, n); }
            @Override public void addGold(int g) { gv.player.addGold(g); }
            @Override public int gold() { return gv.player.getGold(); }
            @Override public int playerLevel() { return gv.player.getLevel(); }
        };
        defense = new BaseDefense(base, defenseHost, System.currentTimeMillis());
    }

    /** Conclui obras e melhorias que terminaram (chamado no jogo e também dentro do menu de construção). */
    public void tickBase(GameView gv) {
        if (base == null) return;
        List<BasePiece> done = base.tick(System.currentTimeMillis());
        if (done.isEmpty()) return;
        for (BasePiece pc : done) {
            gv.ui.showMessage("✅ " + pc.kind.label.toUpperCase() + (pc.level > 1 ? " NÍVEL " + pc.level : "") + " CONCLUÍDO!");
            gv.particles.addExplosion(pc.centerX(), pc.centerY(), Color.YELLOW, 40);
        }
        refreshBaseNpcs();
        gv.applyKingdomBonuses();
        gv.activity.saveCurrentGame();
    }

    // ---- Câmera do modo construção ----
    public void setBuildCamera(double x, double y) {
        double minX = (BaseLayout.PLOT_X - 4) * (double) TileMap.TILE_SIZE, maxX = (BaseLayout.PLOT_X + BaseLayout.PLOT_W + 4) * (double) TileMap.TILE_SIZE;
        double minY = (BaseLayout.PLOT_Y - 4) * (double) TileMap.TILE_SIZE, maxY = (BaseLayout.PLOT_Y + BaseLayout.PLOT_H + 6) * (double) TileMap.TILE_SIZE;
        buildCamX = Math.max(minX, Math.min(maxX, x));
        buildCamY = Math.max(minY, Math.min(maxY, y));
    }
    public double getBuildCamX() { return buildCamX; }
    public double getBuildCamY() { return buildCamY; }
    public double screenToWorldX(float sx) { return cameraX + sx / currentZoom; }
    public double screenToWorldY(float sy) { return cameraY + sy / currentZoom; }

    private long lastDynamicSpawnCheck = 0;

    public void update(Player p, Companion c, GameView gv) {
        if (tileMap.isPortal(p.getPositionX(), p.getPositionY())) handlePortal(p, c, gv);
        currentZoom += (targetZoom - currentZoom) * 0.05f;

        // Atualização de spawn dinâmico por proximidade do jogador (a cada 1 segundo)
        if (System.currentTimeMillis() - lastDynamicSpawnCheck > 1000) {
            updateDynamicSpawning(p, tileMap.getMapName(), gv);
            lastDynamicSpawnCheck = System.currentTimeMillis();
        }

        updateEnemies(c, gv); handleRespawns(); updateLoots(p, gv);
        if (tileMap.getMapName().equals("HOME")) for (Building b : buildings) b.update(activity);
        if (base != null) {
            ensureDefense(gv);
            tickBase(gv);
            defense.update(System.currentTimeMillis());
        }
    }

    private void updateDynamicSpawning(Player p, String mapName, GameView gv) {
        if (mapName.equals("CITY") || mapName.equals("HOME") || mapName.equals("TOWER")) return;

        double px = p.getPositionX();
        double py = p.getPositionY();

        // 1. Remove inimigos que saíram da área de visão/proximidade (> 1200 pixels) com efeito de partícula
        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            double dist = Math.sqrt(Math.pow(e.positionX - px, 2) + Math.pow(e.positionY - py, 2));
            if (dist > 1200 && !e.isBoss && !e.isPet) {
                if (gv != null && gv.particles != null) {
                    gv.particles.addExplosion(e.positionX, e.positionY, Color.WHITE, 10);
                }
                it.remove();
            }
        }

        // 2. Conta quantos inimigos estão visíveis na tela (num raio de 800 pixels)
        int nearbyCount = 0;
        for (Enemy e : enemies) {
            double dist = Math.sqrt(Math.pow(e.positionX - px, 2) + Math.pow(e.positionY - py, 2));
            if (dist < 800) {
                nearbyCount++;
            }
        }

        // 3. Se houver menos de 10 inimigos na tela, spawna novos na borda da visão do jogador (limite máximo de 10)
        if (nearbyCount < 5) {
            String[] speciesList = getSpeciesForMap(mapName);
            int baseLevel = getBiomeBaseLevel(mapName);
            int color = getBiomeColor(mapName);

            for (int i = 0; i < 2; i++) {
                double angle = random.nextDouble() * 2 * Math.PI;
                double distance = 400 + random.nextInt(400); // Na borda da tela (entre 400 e 800 pixels)
                double ex = px + Math.cos(angle) * distance;
                double ey = py + Math.sin(angle) * distance;

                double mapMaxX = tileMap.getCols() * TileMap.TILE_SIZE;
                double mapMaxY = tileMap.getRows() * TileMap.TILE_SIZE;

                if (ex > 100 && ex < mapMaxX - 100 && ey > 100 && ey < mapMaxY - 100) {
                    double dCenter = Math.sqrt(Math.pow(ex - mapMaxX/2, 2) + Math.pow(ey - mapMaxY/2, 2));
                    int lv = baseLevel + (int)(dCenter / 400);
                    String sName = speciesList[random.nextInt(speciesList.length)];
                    addEnemy(sName, ex, ey, 32, color, false, random.nextDouble() < 0.05, Math.max(1, lv), random.nextInt(2));
                    if (gv != null && gv.particles != null) {
                        gv.particles.addExplosion(ex, ey, color, 12);
                    }
                }
            }
        }
    }

    private String[] getSpeciesForMap(String name) {
        switch (name) {
            case "FOREST": return new String[]{"Bulbasaur", "Caterpie", "Metapod", "Butterfree", "Weedle", "Kakuna", "Pidgey", "Pidgeotto", "Oddish", "Bellsprout", "Rattata", "Meowth", "Spearow"};
            case "JUNGLE": return new String[]{"Ivysaur", "Venusaur", "Gloom", "Vileplume", "Weepinbell", "Victreebel", "Pinsir", "Scyther", "Beedril", "Paras", "Parasect", "Tangela", "Venonat", "Venomoth"};
            case "SMALL_LAKE": return new String[]{"Squirtle", "Poliwag", "Poliwhirl", "Psyduck", "Slowpoke", "Magikarp"};
            case "RIO": return new String[]{"Wartortle", "Golduck", "Slowpoke", "Slowking", "Poliwhirl", "Tentacool", "Magikarp"};
            case "OCEAN": return new String[]{"Blastoise", "Vaporeon", "Lapras", "Gyarados", "Tentacruel", "Poliwrath"};
            case "ROCKY_PATH": return new String[]{"Sandshrew", "Sandslash", "Diglett", "Dugtrio", "Mankey", "Primeape", "Nidoranf", "Nidoranm", "Nidorina", "Nidorino"};
            case "MINE": return new String[]{"Onix", "Machop", "Rhydon", "Diglett", "Dugtrio", "Zubat", "Golbat"};
            case "VALE_ROCHOSO": return new String[]{"Abra", "Kadabra", "Alakazam", "Spearow", "Fearow", "Pidgeot", "Clefairy", "Clefable", "Jigglypuff", "Wigglytuff"};
            case "FIRE_CAVE": return new String[]{"Charmander", "Vulpix", "Ponyta", "Growlithe", "Flareon", "Houndour"};
            case "MAGMA_CAVE": return new String[]{"Charmeleon", "Magmar", "Rapidash", "Arcanine", "Ninetales"};
            case "VOLCANO": return new String[]{"Charizard", "Magmar", "Rapidash", "Arcanine", "Flareon", "Aerodactyl"};
            case "SEWERS": return new String[]{"Rattata", "Raticate", "Ekans", "Arbok", "Grimer", "Muk", "Koffing", "Weezing", "Zubat"};
            case "SWAMP": return new String[]{"Arbok", "Weezing", "Muk", "Nidoking", "Nidoqueen", "Gloom", "Victreebel"};
            case "SNOW_MOUNTAIN": return new String[]{"Piloswine", "Cloyster", "Lapras", "Jynx"};
            case "DESERT": return new String[]{"Sandshrew", "Sandslash", "Vulpix", "Ninetales", "Ponyta"};
            case "FEAR_ISLAND": return new String[]{"Gastly", "Haunter", "Gengar", "Golbat", "Arbok", "Muk"};
            default: return new String[]{"Pikachu", "Rattata", "Pidgey"};
        }
    }

    private int getBiomeColor(String biome) {
        switch (biome) {
            case "FOREST": case "JUNGLE": return Color.GREEN;
            case "SMALL_LAKE": case "RIO": case "OCEAN": return Color.BLUE;
            case "ROCKY_PATH": case "MINE": return Color.GRAY;
            case "FIRE_CAVE": case "MAGMA_CAVE": case "VOLCANO": return Color.RED;
            case "SNOW_MOUNTAIN": return Color.CYAN;
            case "DESERT": case "VALE_ROCHOSO": return Color.YELLOW;
            default: return Color.GREEN;
        }
    }

    private void updateEnemies(Companion c, GameView gv) {
        // Separação entre entidades/pokémons para evitar sobreposição (ocupando cada um seu espaço)
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e1 = enemies.get(i);
            if (!e1.isAlive) continue;
            for (int j = i + 1; j < enemies.size(); j++) {
                Enemy e2 = enemies.get(j);
                if (!e2.isAlive) continue;

                double dx = e2.positionX - e1.positionX;
                double dy = e2.positionY - e1.positionY;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double minDst = TileMap.TILE_SIZE * 0.9;

                if (dist < minDst && dist > 0) {
                    double overlap = (minDst - dist) / 2.0;
                    double nx = dx / dist;
                    double ny = dy / dist;

                    e1.positionX -= nx * overlap;
                    e1.positionY -= ny * overlap;
                    e2.positionX += nx * overlap;
                    e2.positionY += ny * overlap;
                }
            }
        }

        Iterator<Enemy> it = enemies.iterator();
        while (it.hasNext()) {
            Enemy e = it.next();
            if (!e.isAlive() && e.isRaider) {
                // Invasor derrotado: dá experiência e fica "desmaiado" (dá para capturar); o prêmio vem ao fim da invasão
                c.gainExp(e.level * 8); gv.player.gainExp(e.level * 4);
                Loot d = new Loot(e.name, e.positionX, e.positionY, Loot.Type.DEFEATED); d.setOwnerSpecies(e.name); d.setShiny(e.isShiny); d.setRadius(Math.max(65, e.radius * 1.5)); loots.add(d);
                it.remove();
                continue;
            }
            if (!e.isAlive()) {
                int xp = (e.level * 20) + (e.isBoss ? 200 : 0);
                c.gainExp(xp); if (gv.player.gainExp(xp/2)) gv.ui.showMessage("🎊 NÍVEL UP!");
                Loot d = new Loot(e.name, e.positionX, e.positionY, Loot.Type.DEFEATED); d.setOwnerSpecies(e.name); d.setShiny(e.isShiny); d.setRadius(Math.max(65, e.radius * 1.5)); loots.add(d);
                dropLoot(e, gv); respawnQueue.add(new RespawnInfo(e.name, e.startX, e.startY, 32, e.color, e.isBoss, e.isShiny, System.currentTimeMillis() + 30000));
                
                // Atualiza progresso das missões ao derrotar qualquer Pokémon selvagem
                gv.missions.updateProgress("Pokemon");
                gv.missions.updateProgress(e.name);

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
                else if (l.getName().equalsIgnoreCase("aincrad_portal")) {
                    gv.tower.advanceFloor();
                    it.remove();
                } else {
                    p.addItem(l.getName()); 
                    it.remove(); 
                }
            }
        }
    }

    private void spawnMoltenSanctumBeginner() {
        int base = getBiomeBaseLevel("MOLTEN_SANCTUM_BEGINNER");
        spawnBasic(new String[]{"Charmander", "Vulpix", "Ponyta", "Growlithe", "Houndour"}, base, Color.RED);
        double midX = (tileMap.getCols() * TileMap.TILE_SIZE) / 2.0;
        double midY = (tileMap.getRows() * TileMap.TILE_SIZE) / 2.0;
        addEnemy("Charmeleon", midX, midY, 50, Color.RED, true, false, 15, 0);
    }

    private void spawnMoltenSanctum() {
        int base = getBiomeBaseLevel("MOLTEN_SANCTUM");
        spawnBasic(new String[]{"Magmar", "Rapidash", "Arcanine", "Flareon", "Houndoom", "Charizard"}, base, Color.RED);
        double midX = (tileMap.getCols() * TileMap.TILE_SIZE) / 2.0;
        double midY = (tileMap.getRows() * TileMap.TILE_SIZE) / 2.0;
        addEnemy("Mega_Charizard_X", midX, midY, 60, Color.RED, true, true, 85, 0);
    }

    private void handlePortal(Player p, Companion c, GameView gv) {
        String m = tileMap.getMapName();
        if (m.equals("CITY")) loadMap("FOREST", p, c, "BOTTOM");
        else if (m.equals("FIRE_CAVE")) loadMap("MOLTEN_SANCTUM_BEGINNER", p, c, "LEFT");
        else if (m.equals("MOLTEN_SANCTUM_BEGINNER")) loadMap("FIRE_CAVE", p, c, "CENTER");
        else if (m.equals("VOLCANO")) loadMap("MOLTEN_SANCTUM", p, c, "LEFT");
        else if (m.equals("MOLTEN_SANCTUM")) loadMap("VOLCANO", p, c, "CENTER");
        else loadMap("CITY", p, c, "CENTER");
    }

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
        float sw = canvas.getWidth();
        float sh = canvas.getHeight();

        boolean building = base != null && gv.ui.state == UIManager.UIState.BUILD;
        if (building != wasBuilding) {
            wasBuilding = building;
            if (building) { savedZoom = targetZoom; targetZoom = 0.8f; setBuildCamera(p.getPositionX(), p.getPositionY()); }
            else { targetZoom = savedZoom; }
            if (base != null) base.setEditing(building);
        }
        double focusX = building ? buildCamX : p.getPositionX(), focusY = building ? buildCamY : p.getPositionY();

        cameraX = (float) focusX - (sw / 2f) / currentZoom;
        cameraY = (float) focusY - (sh / 2f) / currentZoom;

        canvas.save();
        canvas.translate(sw / 2f, sh / 2f);
        canvas.scale(currentZoom, currentZoom);
        canvas.translate((float) -focusX, (float) -focusY);

        long nowMs = System.currentTimeMillis();
        float vl = cameraX, vt = cameraY, vr = cameraX + sw / currentZoom, vb = cameraY + sh / currentZoom;
        if (tileMap != null) tileMap.draw(canvas, p, sw, sh, currentZoom);
        if (base != null) base.drawWorld(canvas, nowMs, vl, vt, vr, vb);
        for (Building b : buildings) b.draw(canvas, p);
        for (NPC n : npcs) n.draw(canvas);
        for (Loot l : loots) l.draw(canvas);
        for (Enemy e : enemies) e.draw(canvas);

        // Desenha outros jogadores conectados no mesmo mapa
        for (RemotePlayer rp : MultiplayerManager.getInstance().getRemotePlayers()) {
            if (rp.mapName != null && rp.mapName.equals(tileMap.getMapName())) {
                rp.update();
                rp.draw(canvas);
            }
        }

        p.draw(canvas, c); if (!p.isMounted()) c.draw(canvas);
        if (base != null) {
            base.drawOverlay(canvas, nowMs, vl, vt, vr, vb);
            if (defense != null) defense.draw(canvas, nowMs);
        }
        if (gv.effects != null) gv.effects.draw(canvas);
        if (gv.particles != null) gv.particles.draw(canvas);
        canvas.restore();
    }

    private void updateCamera(Player p, int sw, int sh) {
        // Câmera perfeitamente centralizada via canvas.translate no draw()
    }

    public void changeZoom(float d) { targetZoom = Math.max(0.4f, Math.min(1.8f, targetZoom + d)); }
    public float getCurrentZoom() { return currentZoom; }
    public boolean isCollidingWithBuilding(double x, double y) { return false; }
    public boolean isCollidingWithEnemy(double x, double y, double padding) {
        for (Enemy e : enemies) {
            if (e.isAlive && !e.isPet && !e.isRaider) {
                double dist = Math.sqrt(Math.pow(e.positionX - x, 2) + Math.pow(e.positionY - y, 2));
                if (dist < e.radius + padding) {
                    return true;
                }
            }
        }
        return false;
    }
    public float getCameraX() { return cameraX; } public float getCameraY() { return cameraY; }
}
