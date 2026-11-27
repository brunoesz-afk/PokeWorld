package com.example.pokeworld.managers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.util.Log;

import com.example.pokeworld.world.TileMap;

import java.util.HashMap;
import java.util.Map;

public class SpriteManager {
    private static SpriteManager instance;
    private boolean isLoaded = false;
    private final Map<String, Bitmap[][]> unitSprites = new HashMap<>();
    private final Map<String, Bitmap> buildingSprites = new HashMap<>();
    private final Map<Integer, Bitmap> tileSprites = new HashMap<>();
    private final Map<String, Bitmap> spellSprites = new HashMap<>();

    private Context appContext;
    private final java.util.Set<String> missing = new java.util.HashSet<>();

    private SpriteManager() {}

    public static SpriteManager getInstance() {
        if (instance == null) instance = new SpriteManager();
        return instance;
    }

    public void loadSprites(Context context) {
        this.appContext = context.getApplicationContext() != null ? context.getApplicationContext() : context;
        if (isLoaded) return;
        try {
            // Carrega folhas de sprites de Pokémon (Padrão 3x3)
            String[] species = {
                "personagem", "pikachu", "charmander", "charmeleon", "squirtle", "bulbasaur", "raichu", "charizard", "blastoise",
                "oddish", "caterpie", "pidgey", "sandshrew", "onix", "ekans", "arbok",
                "magmar", "ponyta", "lapras", "snorlax", "psyduck", "magikarp","spearow","fearow",
                "gastly", "haunter", "articuno", "venusaur", "ivysaur", "wartortle","sandslash","nidoranm","nidorina","nidoranf","nidorino","nidoking","nidoqueen",
                "abra", "aerodactyl", "alakazam", "bellsprout", "clefable", "clefairy", "diglett", "dugtrio", "gengar", "gloom", "golbat", "grimer", "jigglypuff", "kadabra",
                    "koffing", "machop", "mankey", "meowth", "meowth_shiny", "paras", "parasect", "persian", "poliwag", "poliwhirl", "primeape", "rhydon", "tangela", "tentacool", "tentacruel",
                    "venomoth", "venonat", "victreebel", "vileplume", "weedle", "weepinbell", "wigglytuff", "zubat",
                "charizard_shiny", "venosaur", "charmander_shiny", "charmeleon_shiny", "venosaur_shiny",
                "kakuna", "pidgeot", "pidgeotto", "blastoise_shiny", "bulbasaur_shiny", "ivysaur_shiny",
                "squirtle_shiny", "wartortle_shiny", "butterfree", "squirtle_jatodeagua", "squirtle_shiny_jatodeagua", "pokebola",
                "pidgeot_voar", "blastoise_nadar", "lapras_nadar", "charizard_voar", "raiquaza_voar",
                "arcanine", "flareon", "vaporeon", "jolteon", "growlithe","growlithe_shiny", "houndour", "rattata", "rattata_shiny","raticate", "pichu", "zapdos",
                "mega_charizard_x", "mega_charizard_y", "enfermeira", "mega_venusaur", "tauros", "pinsir", "slowpoke","slowking", "golduck", "vulpix", "ninetales", "rapidash", "gyardos", "gyardos_shiny", "clefary", "psiduck", "metapod", "muk", "weezing", "scyther",
                "prof_oficina", "prof_daycare", "prof_laboratorio", "prof_montaria", "prof_bau", "prof_ginasio", "prof_mural", "prof_academia","beedril",
                "treinador_laboratorio", "treinador_montaria", "treinador_daycare", "treinador_pokebola", "treinador_academia"
            };
            int count = 0;
            for (String s : species) {
                loadUnitSheet(context, s, 3, 3);
                count++;
                if (count % 12 == 0) {
                    try { Thread.sleep(4); } catch (Exception ignored) {}
                }
            }

            // Carrega Sheets de Magias (3x3)
            loadUnitSheet(context, "spells_sementes2", 3, 3);
            loadUnitSheet(context, "spells_sementes", 3, 3);
            loadUnitSheet(context, "spells_petaldance", 3, 3);
            loadUnitSheet(context, "spells_chicotedevinha", 3, 3);

            // Carrega Construções
            String[] buildings = {"castelo_sprite", "casa", "centro_de_cura", "construcao_loja", "ferreiro", "laboratorio", "oficina", "oficina_construcao", "oficina_pokebola", "fonte_termal", "fabrica_montaria","construcao_daycare", "construcao_hospitaljoy", "bounty_board", "ginasio_building"};
            for (String b : buildings) {
                loadBuildingSprite(context, b);
            }

            // Carrega Tiles Individuais
            loadTileSprite(context, "arvore", TileMap.TREE);
            loadTileSprite(context, "arvore", TileMap.BERRY_TREE);
            loadTileSprite(context, "chao_pedra", TileMap.ROAD);
            loadTileSprite(context, "arbusto", TileMap.BUSH);
            loadTileSprite(context, "grama", TileMap.GRASS);
            loadTileSprite(context, "snow_tiled", TileMap.SNOW);
            loadTileSprite(context, "snow_tiled2", TileMap.SNOW_CRYSTAL);
            loadTileSprite(context, "obsidiana_tile", TileMap.OBSIDIAN);
            loadTileSprite(context, "vulcan_tiled", TileMap.LAVA);
            loadTileSprite(context, "water_tiled", TileMap.WATER);
            loadTileSprite(context, "gramas", 100); 

            // Carrega Spells
            loadSpellSprite(context, "bola_de_fogo");
            loadSpellSprite(context, "spells_jatodeagua");
            loadSpellSprite(context, "spells_sementes");
            loadSpellSprite(context, "spells_sementes2");
            loadSpellSprite(context, "spells_petaldance");
            loadSpellSprite(context, "spells_chicotedevinha");

            // Carrega Sprites de Morte/Derrota
            String[] deaths = {
                "arcanine_death", "bellsprout_death", "blastoise_death", "blastoise_shiny_death",
                "bulbasaur_death", "bulbasaur_shiny_death", "butterfree_death", "caterpie_death",
                "charizard_death", "charizard_shiny_death", "charmander_death", "charmander_shiny_death",
                "charmeleon_death", "charmeleon_shiny_death", "chikorita_death", "croconaw_death",
                "gloom_death", "growlithe_death", "growlithe_shiny_death", "houndour_death",
                "ivysaur_death", "ivysaur_shiny_death", "kakuna_death", "meowth_death",
                "meowth_shiny_death", "metapod_death", "oddish_death", "pidgeot_death",
                "pidgeotto_death", "pidgey_death", "pikachu_death", "pikachu_shiny_death",
                "poliwag_death", "raichu_death", "raichu_shiny_death", "rattata_death",
                "rattata_shiny_death", "squirtle_death", "squirtle_shiny_death", "tangela_death",
                "venosaur_death", "venosaur_shiny_death", "victreebel_death", "vileplume_death",
                "wartortle_death", "wartortle_shiny_death", "weedle_death", "weedle_shiny_death",
                "weepinbell_death", "ovo"
            };
            for (String d : deaths) {
                loadMiscSprite(context, d);
            }
            loadMiscSprite(context, "pokedex");
            loadMiscSprite(context, "pokebola_item");
            loadMiscSprite(context, "pokebola");
            
            // Fatia os Tilesets Vulcânicos e Magma (1024x1024 Lava e 4x4 Volcano)
            loadLavaTileset(context);
            loadVolcanoTileset(context);
            loadTileSprite(context, "tile_magma_vent_3x1", TileMap.MAGMA_VENT);
            
            isLoaded = true;
        } catch (OutOfMemoryError oom) {
            System.gc();
            Log.e("SpriteManager", "Erro de memória OOM recuperado: " + oom.getMessage());
            isLoaded = true;
        } catch (Exception e) {
            Log.e("SpriteManager", "Erro ao carregar sprites: " + e.getMessage());
            isLoaded = true;
        }
    }

    private Bitmap decodeSpriteBitmap(Context context, String name) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.RGB_565; // Usa 50% menos memória RAM (evita fechamentos em celulares como Galaxy A31)

            String filename = name.toLowerCase().replace(" ", "_") + ".png";
            java.io.File downloadedFile = new java.io.File(context.getFilesDir(), filename);
            if (downloadedFile.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(downloadedFile.getAbsolutePath(), options);
                if (bmp != null) return bmp;
            }
            int resId = context.getResources().getIdentifier(name.toLowerCase(), "drawable", context.getPackageName());
            if (resId != 0) {
                return BitmapFactory.decodeResource(context.getResources(), resId, options);
            }
        } catch (OutOfMemoryError oom) {
            System.gc();
            Log.e("SpriteManager", "OOM ao decodificar sprite " + name + ": " + oom.getMessage());
        } catch (Exception ignored) {}
        return null;
    }

    private void loadTileSprite(Context context, String name, int tileType) {
        Bitmap bmp = decodeSpriteBitmap(context, name);
        if (bmp != null) tileSprites.put(tileType, bmp);
    }

    private Bitmap[][] lavaTileset = null;

    public void loadLavaTileset(Context context) {
        try {
            Bitmap sheet = decodeSpriteBitmap(context, "tiled_lava");
            if (sheet != null) {
                int rows = 32;
                int cols = 32;
                int tileSize = sheet.getWidth() / cols; // 32px por tile na imagem 1024x1024
                lavaTileset = new Bitmap[rows][cols];
                for (int r = 0; r < rows; r++) {
                    for (int c = 0; c < cols; c++) {
                        lavaTileset[r][c] = Bitmap.createBitmap(sheet, c * tileSize, r * tileSize, tileSize, tileSize);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("SpriteManager", "Erro ao fatiar tileset de lava: " + e.getMessage());
        }
    }

    public void loadVolcanoTileset(Context context) {
        try {
            Bitmap sheet = decodeSpriteBitmap(context, "tileset_volcano_4x4");
            if (sheet == null) {
                int resId = context.getResources().getIdentifier("tileset_volcano_4x4", "drawable", context.getPackageName());
                if (resId != 0) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.RGB_565;
                    sheet = BitmapFactory.decodeResource(context.getResources(), resId, options);
                }
            }
            if (sheet != null) {
                int cols = 4;
                int rows = 4;
                int tileW = sheet.getWidth() / cols;
                int tileH = sheet.getHeight() / rows;

                Bitmap basalt = Bitmap.createBitmap(sheet, 0 * tileW, 0 * tileH, tileW, tileH);
                Bitmap ash = Bitmap.createBitmap(sheet, 1 * tileW, 0 * tileH, tileW, tileH);
                Bitmap cracked = Bitmap.createBitmap(sheet, 2 * tileW, 0 * tileH, tileW, tileH);
                Bitmap bridge = Bitmap.createBitmap(sheet, 3 * tileW, 0 * tileH, tileW, tileH);
                Bitmap crystal = Bitmap.createBitmap(sheet, 0 * tileW, 1 * tileH, tileW, tileH);

                tileSprites.put(TileMap.BASALT_FLOOR, basalt);
                tileSprites.put(TileMap.ASH_GROUND, ash);
                tileSprites.put(TileMap.CRACKED_OBSIDIAN, cracked);
                tileSprites.put(TileMap.MAGMA_BRIDGE, bridge);
                tileSprites.put(TileMap.SULFUR_CRYSTAL, crystal);
                Log.d("SpriteManager", "Tileset volcano 4x4 fatiado com sucesso!");
            }
        } catch (Exception e) {
            Log.e("SpriteManager", "Erro ao fatiar tileset_volcano_4x4: " + e.getMessage());
        }
    }

    public Bitmap getLavaTile(int row, int col) {
        if (lavaTileset != null && row >= 0 && row < 32 && col >= 0 && col < 32) {
            return lavaTileset[row][col];
        }
        return null;
    }

    public Bitmap getLavaTile(int tileCoord) {
        int r = tileCoord / 32;
        int c = tileCoord % 32;
        return getLavaTile(r, c);
    }

    public Bitmap getTileSprite(int tileType) {
        return tileSprites.get(tileType);
    }

    private void loadSpellSprite(Context context, String name) {
        Bitmap bmp = decodeSpriteBitmap(context, name);
        if (bmp != null) spellSprites.put(name.toLowerCase(), bmp);
    }

    public Bitmap getSpellSprite(String name) {
        return spellSprites.get(name.toLowerCase());
    }

    private final Map<String, Bitmap> miscSprites = new HashMap<>();
    private void loadMiscSprite(Context context, String name) {
        Bitmap bmp = decodeSpriteBitmap(context, name);
        if (bmp != null) miscSprites.put(name.toLowerCase(), bmp);
    }

    public Bitmap getMiscSprite(String name) {
        return miscSprites.get(normalizeName(name));
    }

    private String normalizeName(String name) {
        if (name == null) return "";
        String normalized = name.toLowerCase()
                .replace("ã", "a").replace("õ", "o")
                .replace("ê", "e").replace("í", "i")
                .replace("ç", "c")
                .replace(" ", "_");
        if (normalized.equals("mega_venusaur")) return "mega_venusaur";
        if (normalized.contains("venusaur")) normalized = normalized.replace("venusaur", "venosaur");
        // os arquivos de imagem têm o nome escrito diferente do da espécie
        normalized = normalized.replace("gyarados", "gyardos").replace("clefairy", "clefary").replace("psyduck", "psiduck");
        return normalized;
    }

    private void loadUnitSheet(Context context, String name, int rows, int cols) {
        Bitmap sheet = decodeSpriteBitmap(context, name);
        if (sheet != null) {
            String key = normalizeName(name);
            if (key.contains("charizard_shiny")) {
                int[] vShift = new int[]{0, -30, 0};
                int[] vExtra = new int[]{-40, 0, 0};
                int[][] hShift = new int[][]{{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
                int[][] hExtra = new int[][]{{-40, 0, 0}, {-30, 0, 0}, {10, 0, 0}};
                unitSprites.put(key, sliceCustomSheet(sheet, rows, cols, vShift, vExtra, hShift, hExtra));
            } else if (key.contains("charizard") || key.contains("dragao")) {
                int[] vShift = new int[]{0, 55, 0};
                int[] vExtra = new int[]{55, 0, 0};
                int[][] hShift = new int[][]{{0, -18, 0}, {0, -30, 0}, {10, 10, 0}};
                int[][] hExtra = new int[][]{{-40, 0, 0}, {-30, 0, 0}, {10, 0, 0}};
                unitSprites.put(key, sliceCustomSheet(sheet, rows, cols, vShift, vExtra, hShift, hExtra));
            } else {
                unitSprites.put(key, sliceUnitSheet(sheet, rows, cols));
            }
        }
    }

    private Bitmap[][] sliceCustomSheet(Bitmap sheet, int rows, int cols, int[] vShift, int[] vExtra, int[][] hShift, int[][] hExtra) {
        Bitmap[][] frames = new Bitmap[rows][cols];
        int fw = sheet.getWidth() / cols, fh = sheet.getHeight() / rows;
        for (int r = 0; r < rows; r++) {
            for (int f = 0; f < cols; f++) {
                int startY = Math.max(0, r * fh + (vShift.length > r ? vShift[r] : 0));
                int height = fh + (vExtra.length > r ? vExtra[r] : 0) - (vShift.length > r ? vShift[r] : 0);
                if (startY + height > sheet.getHeight()) height = sheet.getHeight() - startY;
                int shiftX = (hShift.length > r && hShift[r].length > f) ? hShift[r][f] : 0;
                int extraW = (hExtra.length > r && hExtra[r].length > f) ? hExtra[r][f] : 0;
                int startX = Math.max(0, f * fw + shiftX), width = fw + extraW;
                if (startX + width > sheet.getWidth()) width = sheet.getWidth() - startX;
                frames[r][f] = Bitmap.createBitmap(sheet, startX, startY, width, height);
            }
        }
        return frames;
    }

    private void loadBuildingSprite(Context context, String name) {
        int resId = context.getResources().getIdentifier(name.toLowerCase(), "drawable", context.getPackageName());
        if (resId != 0) buildingSprites.put(name.toLowerCase(), BitmapFactory.decodeResource(context.getResources(), resId));
    }

    private Bitmap[][] sliceUnitSheet(Bitmap sheet, int rows, int cols) {
        Bitmap[][] frames = new Bitmap[rows][cols];
        int fw = sheet.getWidth() / cols, fh = sheet.getHeight() / rows;
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) frames[r][c] = Bitmap.createBitmap(sheet, c * fw, r * fh, fw, fh);
        return frames;
    }

    /**
     * Animação de ataque 3x1 (3 quadros em 1 linha) do elemento. Carregada só quando o golpe é usado pela
     * primeira vez (poupa RAM). {@code elem}: fire, water, plant, electric, ice, rock, poison, psychic, dragon, normal.
     */
    public Bitmap[][] getFxSheet(String elem, boolean area) {
        return getSheet("fx_" + elem.toLowerCase() + (area ? "_area" : "_direct") + "_3x1");
    }

    /** Efeito de 1 quadrado do chão (3 quadros) do elemento: usado nos golpes em grade. */
    public Bitmap[][] getTileFxSheet(String elem) { return getSheet("fxt_" + elem.toLowerCase() + "_3x1"); }

    /** Folha 3x1 qualquer, carregada só na primeira vez que alguém pede. */
    public Bitmap[][] getSheet(String name) {
        String key = normalizeName(name);
        Bitmap[][] sheet = unitSprites.get(key);
        if (sheet == null && appContext != null && !missing.contains(key)) {
            loadUnitSheet(appContext, name, 1, 3);
            sheet = unitSprites.get(key);
            if (sheet == null) missing.add(key);   // não tenta de novo a cada quadro
        }
        return sheet;
    }

    public Bitmap[][] getUnitSheet(String species) { 
        String key = normalizeName(species);
        Bitmap[][] sheet = unitSprites.get(key);
        if (sheet == null && key.equals("pokebola")) {
            Bitmap misc = miscSprites.get("pokebola");
            if (misc == null) misc = miscSprites.get("pokebola_item");
            if (misc != null) {
                sheet = new Bitmap[1][1];
                sheet[0][0] = misc;
                unitSprites.put(key, sheet);
            }
        }
        return sheet;
    }
    public Bitmap getUnitSprite(String s, int d, int f) { return getUnitSprite(s, d, f, false); }
    public Bitmap getUnitSprite(String s, int d, int f, boolean sh) {
        String key = normalizeName(s); if (sh) { String sk = key + "_shiny"; if (unitSprites.containsKey(sk)) key = sk; }
        Bitmap[][] frames = unitSprites.get(key);
        if (frames == null) {
            if (key.contains("raichu")) frames = unitSprites.get("pikachu");
            else if (key.contains("charmeleon") || key.contains("charizard")) frames = unitSprites.get("charmander");
            else if (key.contains("wartortle") || key.contains("blastoise")) frames = unitSprites.get("squirtle");
            else if (key.contains("ivysaur") || key.contains("venosaur")) frames = unitSprites.get("bulbasaur");
            else if (key.contains("gengar") || key.contains("haunter")) frames = unitSprites.get("gastly");
            else if (key.contains("raiquaza")) frames = unitSprites.get("dragao");
            else if (key.contains("pidgeotto") || key.contains("pidgeot")) frames = unitSprites.get("pidgey");
            else if (key.contains("metapod") || key.contains("butterfree")) frames = unitSprites.get("caterpie");
        }
        if (frames == null) for (String k : unitSprites.keySet()) if (key.contains(k)) { frames = unitSprites.get(k); break; }
        if (frames == null) frames = unitSprites.get("charmander");
        if (frames == null) return null;
        int row; boolean flip = false;
        switch (d) { case 0: row = 0; break; case 2: row = 2; break; case 3: row = 1; break; case 1: row = 1; flip = true; break; default: row = 0; }
        row = Math.min(row, frames.length - 1);
        if (frames[row] == null || frames[row].length == 0) return null;
        int cc = frames[row].length, col = ((f % cc) + cc) % cc;
        Bitmap sprite = frames[row][col];
        if (sprite == null) return null;
        return flip ? flipBitmap(sprite) : sprite;
    }

    public Bitmap getBuildingSprite(String n) {
        String k = n.toLowerCase().replace(" ", "_").replace("ã", "a").replace("ç", "c").replace("ê", "e");
        Bitmap b = buildingSprites.get(k); if (b == null) b = buildingSprites.get("casa"); return b;
    }

    private Bitmap flipBitmap(Bitmap src) { Matrix m = new Matrix(); m.preScale(-1, 1); return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true); }
}
