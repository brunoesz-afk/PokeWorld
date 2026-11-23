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
import com.example.pokeworld.entities.EvolutionTable;
import com.example.pokeworld.entities.Rune;
import com.example.pokeworld.entities.TalentTree;
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
    
    // ---- Pontos de spawn pré-definidos (só existem Pokémons perto do jogador) ----
    private SpawnZones zones;
    private long lastZoneCheck = 0;
    private final SpawnZones.Sink zoneSink = new SpawnZones.Sink() {
        @Override public Enemy spawn(SpawnZones.Point pt) {
            if (!tileMap.isWalkable(pt.x, pt.y, false, false, false, false)) return null;
            int lv = pt.boss ? pt.level : pt.level + random.nextInt(2);
            Enemy e = new Enemy(pt.species, pt.x, pt.y, pt.radius, pt.color, pt.boss, lv);
            e.isShiny = pt.boss ? pt.shinyBoss : random.nextDouble() < 0.05;
            e.gender = random.nextInt(2);
            enemies.add(e);
            return e;
        }
        @Override public void despawn(Enemy e) { enemies.remove(e); }
    };

    public WorldManager(MainActivity activity) { this.activity = activity; }

    public void loadMap(String name, Player p, Companion c) { loadMap(name, p, c, "CENTER"); }

    public void loadMap(String name, Player p, Companion c, String side) {
        enemies.clear(); npcs.clear(); loots.clear(); buildings.clear(); zones = null;
        int r = 50, col = 100;
        if (name.equals("HOME")) { r = BaseLayout.MAP_ROWS; col = BaseLayout.MAP_COLS; }
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
        if (tileMap.getMapName().equals("HOME")) { sx = TileMap.TILE_SIZE * (BaseLayout.GATE_X0 + BaseLayout.GATE_X1 + 1) / 2.0; sy = TileMap.TILE_SIZE * (BaseLayout.RING_B + 0.5); }
        else if (tileMap.getMapName().equals("TOWER")) { sx = TileMap.TILE_SIZE * (cols / 2.0); sy = TileMap.TILE_SIZE * (rows - 4.0); }
        else if (side.equals("TOP")) sy = TileMap.TILE_SIZE * 1.5;
        else if (side.equals("BOTTOM")) sy = TileMap.TILE_SIZE * (rows - 1.5);
        else if (side.equals("LEFT")) sx = TileMap.TILE_SIZE * 1.5;
        else if (side.equals("RIGHT")) sx = TileMap.TILE_SIZE * (cols - 1.5);
        p.setPosition(sx, sy);
    }

    private void spawnEntitiesForMap(String name, Player p) {
        zones = null;
        switch (name) {
            case "CITY": spawnCityEntities(); break;
            case "HOME": spawnHome(p); break;
            case "FOREST": case "JUNGLE": case "SMALL_LAKE": case "RIO": case "OCEAN": case "ROCKY_PATH": case "MINE":
            case "VALE_ROCHOSO": case "FIRE_CAVE": case "MAGMA_CAVE": case "VOLCANO": case "MOLTEN_SANCTUM_BEGINNER":
            case "MOLTEN_SANCTUM": case "SEWERS": case "SWAMP": case "SNOW_MOUNTAIN": case "DESERT": case "FEAR_ISLAND":
                setupZones(name, p); break;
        }
    }

    private void setupZones(String name, Player p) {
        zones = new SpawnZones(name, tileMap.getRows(), tileMap.getCols(), TileMap.TILE_SIZE, getSpeciesForMap(name),
                getBiomeBaseLevel(name), getBiomeColor(name),
                new SpawnZones.Ground() { @Override public boolean walkable(double x, double y) { return tileMap.isWalkable(x, y, false, false, false, false); } },
                p.getPositionX(), p.getPositionY());
        double midX = tileMap.getCols() * TileMap.TILE_SIZE / 2.0, midY = tileMap.getRows() * TileMap.TILE_SIZE / 2.0;
        switch (name) {
            case "VOLCANO": zones.addBoss(3000, 2800, "Charizard_Shiny", getBiomeBaseLevel("VOLCANO") + 15, Color.YELLOW, 60, true); break;
            case "SNOW_MOUNTAIN": zones.addBoss(3200, 1600, "Articuno", 75, Color.WHITE, 60, true); break;
            case "MOLTEN_SANCTUM_BEGINNER": zones.addBoss(midX, midY, "Charmeleon", 15, Color.RED, 50, false); break;
            case "MOLTEN_SANCTUM": zones.addBoss(midX, midY, "Mega_Charizard_X", 85, Color.RED, 60, true); break;
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

    private void spawnHome(Player p) {
        spawnGuards(p);
        refreshBaseNpcs();
    }

    /** Quantos defensores a base comporta agora: só depende do tamanho da base (Quartel + construções). */
    public int defenderCapacity(Player p) {
        return base != null ? base.guardCapacity() : BaseManager.capacityFor(activity.dbHelper.getBasePieces());
    }

    /** Só os defensores escalados aparecem e andam pela base; o resto fica guardado na caixa (sem custo de tela/RAM). */
    private void spawnGuards(Player p) {
        int max = defenderCapacity(p);
        int spawned = 0;
        for (CapturedCreature c : p.getDefenders()) {
            if (spawned >= max) break;
            PetData d = activity.dbHelper.getPetData(c.name);
            // Procura um ponto livre dentro das muralhas (o pátio do anel inicial)
            for (int tries = 0; tries < 12; tries++) {
                double ex = TileMap.TILE_SIZE * (BaseLayout.RING_L + 3 + random.nextInt(BaseLayout.RING_R - BaseLayout.RING_L - 5));
                double ey = TileMap.TILE_SIZE * (BaseLayout.RING_T + 3 + random.nextInt(BaseLayout.RING_B - BaseLayout.RING_T - 5));
                int before = enemies.size();
                addEnemy(c.name, ex, ey, 32, Color.GREEN, false, c.isShiny, d.level, c.gender, true);
                if (enemies.size() > before) { spawned++; break; }
            }
        }
    }

    /** Depois de mudar quem defende: recria os defensores na base (não mexe durante uma invasão). */
    public void refreshGuards(Player p) {
        if (base == null || !tileMap.getMapName().equals("HOME")) return;
        if (defense != null && defense.isActive()) return;
        for (Iterator<Enemy> it = enemies.iterator(); it.hasNext();) if (it.next().isPet) it.remove();
        spawnGuards(p);
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
    private long lastSeenCheck = 0;

    public void update(Player p, Companion c, GameView gv) {
        if (tileMap.isPortal(p.getPositionX(), p.getPositionY())) handlePortal(p, c, gv);
        currentZoom += (targetZoom - currentZoom) * 0.05f;

        // Pontos de spawn: nasce/some conforme a distância do jogador (2x por segundo)
        long nowZ = System.currentTimeMillis();
        if (zones != null && nowZ - lastZoneCheck > 500) {
            zones.update(p.getPositionX(), p.getPositionY(), nowZ, zoneSink);
            lastZoneCheck = nowZ;
        }

        // Registra na Pokédex o que está perto do jogador (2x por segundo, junto com os pontos de spawn)
        if (nowZ - lastSeenCheck > 500) {
            lastSeenCheck = nowZ;
            for (Enemy e : enemies) {
                if (!e.isAlive || e.isRaider) continue;
                double dx = e.positionX - p.getPositionX(), dy = e.positionY - p.getPositionY();
                if (dx * dx + dy * dy < 700.0 * 700.0) gv.bestiary.markAsSeen(e.name);
            }
        }

        updateEnemies(c, gv); updateLoots(p, gv);
        if (tileMap.getMapName().equals("HOME")) for (Building b : buildings) b.update(activity);
        if (base != null) {
            ensureDefense(gv);
            tickBase(gv);
            defense.update(System.currentTimeMillis());
        }
    }

    /** Mapas selvagens (para a Pokédex mostrar onde cada espécie aparece). */
    public static final String[] WILD_MAPS = {"FOREST", "JUNGLE", "SMALL_LAKE", "RIO", "OCEAN", "ROCKY_PATH", "MINE", "VALE_ROCHOSO", "FIRE_CAVE", "MAGMA_CAVE", "VOLCANO", "SEWERS", "SWAMP", "SNOW_MOUNTAIN", "DESERT", "FEAR_ISLAND", "MOLTEN_SANCTUM_BEGINNER", "MOLTEN_SANCTUM"};

    public static String mapLabel(String map) {
        switch (map) {
            case "FOREST": return "Floresta"; case "JUNGLE": return "Selva"; case "SMALL_LAKE": return "Lago";
            case "RIO": return "Rio"; case "OCEAN": return "Oceano"; case "ROCKY_PATH": return "Trilha Rochosa";
            case "MINE": return "Mina"; case "VALE_ROCHOSO": return "Vale Rochoso"; case "FIRE_CAVE": return "Caverna de Fogo";
            case "MAGMA_CAVE": return "Caverna de Magma"; case "VOLCANO": return "Vulcão"; case "SEWERS": return "Esgoto";
            case "SWAMP": return "Pântano"; case "SNOW_MOUNTAIN": return "Montanha Nevada"; case "DESERT": return "Deserto";
            case "FEAR_ISLAND": return "Ilha do Medo"; case "MOLTEN_SANCTUM_BEGINNER": return "Santuário (Iniciante)";
            case "MOLTEN_SANCTUM": return "Santuário de Magma"; default: return map;
        }
    }

    public static String[] getSpeciesForMap(String name) {
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
            case "MOLTEN_SANCTUM_BEGINNER": return new String[]{"Charmander", "Vulpix", "Ponyta", "Growlithe", "Houndour"};
            case "MOLTEN_SANCTUM": return new String[]{"Magmar", "Rapidash", "Arcanine", "Flareon", "Houndoom", "Charizard"};
            default: return new String[]{"Pikachu", "Rattata", "Pidgey"};
        }
    }

    private int getBiomeColor(String biome) {
        switch (biome) {
            case "FOREST": case "JUNGLE": return Color.GREEN;
            case "SMALL_LAKE": case "RIO": case "OCEAN": return Color.BLUE;
            case "ROCKY_PATH": case "MINE": return Color.GRAY;
            case "FIRE_CAVE": case "MAGMA_CAVE": case "VOLCANO": case "MOLTEN_SANCTUM": case "MOLTEN_SANCTUM_BEGINNER": return Color.RED;
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
                dropLoot(e, gv);
                
                // Atualiza progresso das missões ao derrotar qualquer Pokémon selvagem
                gv.missions.updateProgress("Pokemon");
                gv.missions.updateProgress(e.name);

                it.remove();
            } else e.update(c, gv.effects, gv.particles, tileMap, gv.time.isNight());
        }
    }

    /**
     * Tabela de drops: ouro cresce com o nível; o material depende do tipo do Pokémon
     * (planta → Fruta, pedra → Pedra, fogo/elétrico → Ferro...); Pokébola e Poção são raras; chefe solta muito mais.
     */
    private void dropLoot(Enemy e, GameView gv) {
        int lvl = Math.max(1, e.level);
        double bossMult = e.isBoss ? 5 : 1;
        loots.add(new Loot("Gold", e.positionX, e.positionY, Loot.Type.GOLD).withAmount((int) ((6 + lvl * 2 + random.nextInt(lvl + 1)) * bossMult * gv.companion.getLuckMultiplier() * (1.0 + TalentTree.bonus(gv.player, "Garimpeiro")))));

        String mat; double chance; int max = 1;
        ElementalType t = e.getType();
        if (t == ElementalType.PLANT) { mat = "Fruta"; chance = 0.65; max = 2; }
        else if (t == ElementalType.ROCK) { mat = random.nextDouble() < 0.7 ? "Pedra" : "Ferro"; chance = 0.60; }
        else if (t == ElementalType.FIRE || t == ElementalType.ELECTRIC) { mat = "Ferro"; chance = 0.55; }
        else if (t == ElementalType.POISON) { mat = random.nextDouble() < 0.5 ? "Fruta" : "Ferro"; chance = 0.45; }
        else if (t == ElementalType.WATER || t == ElementalType.ICE) { mat = random.nextDouble() < 0.6 ? "Fruta" : "Madeira"; chance = 0.40; }
        else { String[] pool = {"Madeira", "Fruta", "Ferro"}; mat = pool[random.nextInt(pool.length)]; chance = 0.45; }
        if (e.isBoss) { chance = 1.0; max = 3; }
        if (random.nextDouble() < chance) {
            loots.add(new Loot(mat, e.positionX + 20, e.positionY + 6, Loot.Type.ITEM).withAmount(1 + random.nextInt(max)));
        }
        if (random.nextDouble() < (e.isBoss ? 1.0 : 0.07)) loots.add(new Loot("Pokébola", e.positionX - 22, e.positionY + 8, Loot.Type.ITEM).withAmount(e.isBoss ? 3 : 1));
        if (random.nextDouble() < 0.05) loots.add(new Loot("Poção", e.positionX, e.positionY + 22, Loot.Type.ITEM));
        // Pedras de evolução (da mesma "família" do Pokémon; chefes soltam qualquer uma) e runas raras
        float luck = gv.companion.getLuckMultiplier() * (1f + (float) TalentTree.bonus(gv.player, "Achado"));
        String stone = stoneFor(t);
        if (e.isBoss) stone = EvolutionTable.STONES[random.nextInt(EvolutionTable.STONES.length)];
        if (stone != null && random.nextDouble() < (e.isBoss ? 0.6 : 0.03) * luck)
            loots.add(new Loot(stone, e.positionX + 30, e.positionY - 14, Loot.Type.ITEM));
        if (random.nextDouble() < (e.isBoss ? 0.35 : 0.012) * luck)
            loots.add(new Loot(Rune.ALL[random.nextInt(Rune.ALL.length)], e.positionX - 30, e.positionY - 14, Loot.Type.ITEM));
        if (e.isBoss) gv.player.addGems(1 + random.nextInt(2));
    }

    private static String stoneFor(ElementalType t) {
        if (t == ElementalType.FIRE) return EvolutionTable.FIRE;
        if (t == ElementalType.WATER || t == ElementalType.ICE) return EvolutionTable.WATER;
        if (t == ElementalType.ELECTRIC) return EvolutionTable.THUNDER;
        if (t == ElementalType.PLANT) return EvolutionTable.LEAF;
        return null;
    }

    private void updateLoots(Player p, GameView gv) {
        Iterator<Loot> it = loots.iterator();
        while (it.hasNext()) {
            Loot l = it.next(); 
            if (l.isExpired()) { it.remove(); continue; } 
            l.update(p);
            if (l.getType() == Loot.Type.DEFEATED) continue;
            if (l.isCollectedBy(p)) { 
                if (l.getType() == Loot.Type.GOLD) { p.addGold(l.getAmount()); gv.effects.addText("+" + l.getAmount() + "🪙", l.getPositionX(), l.getPositionY() - 20, Color.YELLOW); it.remove(); }
                else if (l.getName().equalsIgnoreCase("aincrad_portal")) {
                    gv.tower.advanceFloor();
                    it.remove();
                } else {
                    p.addItem(l.getName(), l.getAmount());
                    gv.effects.addText("+" + l.getAmount() + " " + l.getName(), l.getPositionX(), l.getPositionY() - 20, Color.CYAN);
                    it.remove(); 
                }
            }
        }
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
