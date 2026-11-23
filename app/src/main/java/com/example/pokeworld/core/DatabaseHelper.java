package com.example.pokeworld.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.entities.Rarity;
import com.example.pokeworld.entities.BuildingData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DatabaseHelper: Gerencia a persistência de dados do jogador, coleção e progresso.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "pokeworld.db";
    private static final int DATABASE_VERSION = 38; 

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // --- CICLO DE VIDA DO BANCO ---

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabelas de Estado do Jogador
        db.execSQL("CREATE TABLE player_stats (id INTEGER PRIMARY KEY, gold INTEGER, level INTEGER, exp INTEGER, active_species TEXT, talent_points INTEGER DEFAULT 0, gems INTEGER DEFAULT 0, clan TEXT DEFAULT 'Nenhum', ng_plus INTEGER DEFAULT 0, skin_color INTEGER DEFAULT -1, fishing_level INTEGER DEFAULT 1, fishing_exp INTEGER DEFAULT 0, aincrad_floor INTEGER DEFAULT 1, daycare_intro_seen INTEGER DEFAULT 0, tutorial_completed INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE inventory (id INTEGER PRIMARY KEY AUTOINCREMENT, item_name TEXT, quantity INTEGER)");
        
        // Tabelas de Coleção e Progresso
        db.execSQL("CREATE TABLE collection (id INTEGER PRIMARY KEY AUTOINCREMENT, creature_name TEXT, is_shiny INTEGER DEFAULT 0, rarity TEXT DEFAULT 'COMMON', is_in_party INTEGER DEFAULT 0, gender INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE pets (species TEXT PRIMARY KEY, level INTEGER, exp INTEGER, forge_bonus INTEGER, weapon TEXT, armor TEXT, is_shiny INTEGER DEFAULT 0, personality TEXT DEFAULT 'Normal', rune TEXT DEFAULT 'Nenhum', friendship INTEGER DEFAULT 0, type2 TEXT DEFAULT 'Nenhum', rarity TEXT DEFAULT 'COMMON', gender INTEGER DEFAULT 0)");
        
        // Tabelas de Mundo e Conquistas (Adicionado colunas de posição e tempo de construção)
        db.execSQL("CREATE TABLE buildings (id INTEGER PRIMARY KEY AUTOINCREMENT, building_name TEXT, level INTEGER, is_constructed INTEGER DEFAULT 1, pos_x REAL DEFAULT 0, pos_y REAL DEFAULT 0, construction_finish_time INTEGER DEFAULT 0)");
        createBasePiecesTable(db);
        db.execSQL("CREATE TABLE bestiary (id INTEGER PRIMARY KEY AUTOINCREMENT, species_name TEXT UNIQUE)");
        db.execSQL("CREATE TABLE talents (id INTEGER PRIMARY KEY AUTOINCREMENT, talent_name TEXT UNIQUE, level INTEGER)");
        db.execSQL("CREATE TABLE achievements (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT UNIQUE)");
        db.execSQL("CREATE TABLE capture_counts (species TEXT PRIMARY KEY, count INTEGER DEFAULT 0)");

        // Dados Iniciais
        initDefaultStats(db);
    }

    private void initDefaultStats(SQLiteDatabase db) {
        ContentValues stats = new ContentValues();
        stats.put("id", 1);
        stats.put("gold", 1000); // Aumentado inicial para facilitar testes
        stats.put("level", 1);
        stats.put("exp", 0);
        stats.put("active_species", "Pikachu");
        stats.put("gems", 10);
        db.insert("player_stats", null, stats);

        savePetInitial(db, "Pikachu");
        
        ContentValues col = new ContentValues();
        col.put("creature_name", "Pikachu");
        col.put("is_in_party", 1);
        col.put("gender", 0); // Macho inicial
        db.insert("collection", null, col);

        // Recursos iniciais para facilitar a primeira missão de construção
        db.insert("inventory", null, createItem("Madeira", 20));
        db.insert("inventory", null, createItem("Ferro", 10));
        db.insert("inventory", null, createItem("Pedra", 10));

        // Posições alinhadas com o novo Core centralizado (60-100, 22-58)
        saveBuildingLevel(db, "Centro de Cura", 1, 1, 2560, 1920);
        saveBuildingLevel(db, "Loja", 1, 1, 3200, 1920);
        saveBuildingLevel(db, "Oficina", 1, 0, 4200, 1600); // Top Left do Core
        saveBuildingLevel(db, "Daycare", 1, 0, 5600, 1600); // Top Right do Core
        saveBuildingLevel(db, "Laboratorio", 1, 0, 4200, 2800); // Bottom Left do Core
        saveBuildingLevel(db, "Fabrica de Montaria", 1, 0, 5600, 2800); // Bottom Right do Core
        saveBuildingLevel(db, "Castelo", 1, 1, 2560, 2560);
        saveBuildingLevel(db, "Ginásio", 1, 1, 1280, 1920); // CITY
        saveBuildingLevel(db, "Mural de Caça", 1, 1, 1920, 1920); // CITY
        saveBuildingLevel(db, "Academia", 1, 0, 4900, 2200); 
    }

    private ContentValues createItem(String name, int qty) {
        ContentValues v = new ContentValues();
        v.put("item_name", name);
        v.put("quantity", qty);
        return v;
    }

    private void createBasePiecesTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS base_pieces (id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT, gx INTEGER, gy INTEGER, level INTEGER DEFAULT 1, finish_time INTEGER DEFAULT 0, upgrading INTEGER DEFAULT 0, crop_ready INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // 36 -> 37: só acrescenta a tabela de peças da base (NÃO apaga o progresso do jogador)
        if (oldVersion == 36) {
            createBasePiecesTable(db);
            migrateLegacyHomeBuildings(db);
            return;
        }
        // 37 -> 38: plantação nas hortas da base
        if (oldVersion == 37) {
            try { db.execSQL("ALTER TABLE base_pieces ADD COLUMN crop_ready INTEGER DEFAULT 0"); }
            catch (Exception e) { Log.e("DatabaseHelper", "crop_ready já existe?", e); }
            return;
        }
        db.execSQL("DROP TABLE IF EXISTS base_pieces");
        db.execSQL("DROP TABLE IF EXISTS player_stats");
        db.execSQL("DROP TABLE IF EXISTS inventory");
        db.execSQL("DROP TABLE IF EXISTS collection");
        db.execSQL("DROP TABLE IF EXISTS buildings");
        db.execSQL("DROP TABLE IF EXISTS bestiary");
        db.execSQL("DROP TABLE IF EXISTS talents");
        db.execSQL("DROP TABLE IF EXISTS pets");
        db.execSQL("DROP TABLE IF EXISTS achievements");
        db.execSQL("DROP TABLE IF EXISTS capture_counts");
        onCreate(db);
    }

    /** Prédios da base do sistema antigo viram peças "sem posição" (gx = -1); a base acha um lugar livre. */
    private void migrateLegacyHomeBuildings(SQLiteDatabase db) {
        try {
            Cursor c = db.rawQuery("SELECT building_name, level, is_constructed, construction_finish_time FROM buildings", null);
            long now = System.currentTimeMillis();
            while (c.moveToNext()) {
                com.example.pokeworld.world.BaseKind k = com.example.pokeworld.world.BaseKind.fromLegacyName(c.getString(0));
                if (k == null) continue;
                boolean built = c.getInt(2) == 1;
                long finish = c.getLong(3);
                if (!built && finish <= 0) continue; // era só um "terreno" reservado, nunca foi construído
                ContentValues v = new ContentValues();
                v.put("kind", k.name());
                v.put("gx", -1);
                v.put("gy", -1);
                v.put("level", Math.max(1, c.getInt(1)));
                v.put("finish_time", built ? 0 : Math.max(finish, now));
                v.put("upgrading", 0);
                db.insert("base_pieces", null, v);
            }
            c.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Migração das construções da base falhou", e);
        }
    }

    // --- PEÇAS DA BASE ---

    public List<com.example.pokeworld.world.BasePiece> getBasePieces() {
        List<com.example.pokeworld.world.BasePiece> out = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT id, kind, gx, gy, level, finish_time, upgrading, crop_ready FROM base_pieces ORDER BY id", null);
        while (c.moveToNext()) {
            com.example.pokeworld.world.BaseKind k = com.example.pokeworld.world.BaseKind.fromId(c.getString(1));
            if (k == null) continue;
            com.example.pokeworld.world.BasePiece p = new com.example.pokeworld.world.BasePiece(k, c.getInt(2), c.getInt(3), c.getInt(4));
            p.id = c.getInt(0);
            p.finishTime = c.getLong(5);
            p.upgrading = c.getInt(6) == 1;
            p.cropReady = c.getLong(7);
            out.add(p);
        }
        c.close();
        return out;
    }

    public void insertBasePiece(com.example.pokeworld.world.BasePiece p) {
        long id = getWritableDatabase().insert("base_pieces", null, basePieceValues(p));
        p.id = (int) id;
    }

    public void updateBasePiece(com.example.pokeworld.world.BasePiece p) {
        if (p.id < 0) { insertBasePiece(p); return; }
        getWritableDatabase().update("base_pieces", basePieceValues(p), "id=?", new String[]{String.valueOf(p.id)});
    }

    public void deleteBasePiece(com.example.pokeworld.world.BasePiece p) {
        if (p.id >= 0) getWritableDatabase().delete("base_pieces", "id=?", new String[]{String.valueOf(p.id)});
    }

    /** Insere várias peças de uma vez (base inicial, paredes em lote). */
    public void insertBasePieces(List<com.example.pokeworld.world.BasePiece> list) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (com.example.pokeworld.world.BasePiece p : list) p.id = (int) db.insert("base_pieces", null, basePieceValues(p));
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public void updateBasePieces(List<com.example.pokeworld.world.BasePiece> list) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (com.example.pokeworld.world.BasePiece p : list)
                db.update("base_pieces", basePieceValues(p), "id=?", new String[]{String.valueOf(p.id)});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private ContentValues basePieceValues(com.example.pokeworld.world.BasePiece p) {
        ContentValues v = new ContentValues();
        v.put("kind", p.kind.name());
        v.put("gx", p.gx);
        v.put("gy", p.gy);
        v.put("level", p.level);
        v.put("finish_time", p.finishTime);
        v.put("upgrading", p.upgrading ? 1 : 0);
        v.put("crop_ready", p.cropReady);
        return v;
    }

    // --- MÉTODOS DE SALVAMENTO (WRITE) ---

    public void savePlayerState(int gold, int level, int exp, int talentPoints, int gems, String clan, int ngPlus, int skinColor, Map<String, Integer> talents, List<CapturedCreature> collection, Map<String, Integer> inventory, int fishingLevel, int fishingExp, int aincradFloor, int daycareIntro) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues stats = new ContentValues();
        stats.put("gold", gold);
        stats.put("level", level);
        stats.put("exp", exp);
        stats.put("talent_points", talentPoints);
        stats.put("gems", gems);
        stats.put("clan", clan);
        stats.put("ng_plus", ngPlus);
        stats.put("skin_color", skinColor);
        stats.put("fishing_level", fishingLevel);
        stats.put("fishing_exp", fishingExp);
        stats.put("aincrad_floor", aincradFloor);
        stats.put("daycare_intro_seen", daycareIntro);
        db.update("player_stats", stats, "id=1", null);

        if (talents != null) {
            for (Map.Entry<String, Integer> entry : talents.entrySet()) {
                ContentValues tv = new ContentValues();
                tv.put("level", entry.getValue());
                db.update("talents", tv, "talent_name=?", new String[]{entry.getKey()});
            }
        }

        if (collection != null) {
            db.delete("collection", null, null);
            for (CapturedCreature c : collection) {
                ContentValues cv = new ContentValues();
                cv.put("creature_name", c.name);
                cv.put("is_shiny", c.isShiny ? 1 : 0);
                cv.put("rarity", c.rarity.name());
                cv.put("is_in_party", c.isInParty ? 1 : 0);
                cv.put("gender", c.gender);
                db.insert("collection", null, cv);
            }
        }

        if (inventory != null) {
            db.delete("inventory", null, null);
            for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                ContentValues iv = new ContentValues();
                iv.put("item_name", entry.getKey());
                iv.put("quantity", entry.getValue());
                db.insert("inventory", null, iv);
            }
        }
    }

    public void savePet(String species, int level, int exp, int forgeBonus, String weapon, String armor, boolean isShiny, String personality, String rune, int friendship, String type2, Rarity rarity, int gender) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("species", species);
        v.put("level", level);
        v.put("exp", exp);
        v.put("forge_bonus", forgeBonus);
        v.put("weapon", weapon);
        v.put("armor", armor);
        v.put("is_shiny", isShiny ? 1 : 0);
        v.put("personality", personality);
        v.put("rune", rune);
        v.put("friendship", friendship);
        v.put("type2", type2);
        v.put("rarity", rarity != null ? rarity.name() : "COMMON");
        v.put("gender", gender);
        db.insertWithOnConflict("pets", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public void updateBuilding(String name, int level, int constructed, double x, double y, long finishTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("level", level);
        v.put("is_constructed", constructed);
        v.put("pos_x", x);
        v.put("pos_y", y);
        v.put("construction_finish_time", finishTime);
        db.update("buildings", v, "building_name=?", new String[]{name});
    }

    public void updateBuildingPosition(String name, double x, double y, long finishTime) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("pos_x", x);
        v.put("pos_y", y);
        v.put("is_constructed", 0); // Fica como não construído inicialmente
        v.put("construction_finish_time", finishTime);
        db.update("buildings", v, "building_name=?", new String[]{name});
    }

    // --- MÉTODOS DE CONSULTA (READ) ---

    public PetData getPetData(String species) {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT * FROM pets WHERE species=?", new String[]{species});
            if (c.moveToFirst()) {
                String rarityStr = c.getColumnCount() > 11 ? c.getString(11) : "COMMON";
                Rarity rarity = Rarity.COMMON;
                try { if (rarityStr != null) rarity = Rarity.valueOf(rarityStr); } catch (Exception e) {}

                int gender = c.getColumnCount() > 12 ? c.getInt(12) : 0;

                PetData data = new PetData(
                    c.getString(0), c.getInt(1), c.getInt(2), c.getInt(3),
                    c.getString(4), c.getString(5),
                    c.getInt(6) == 1,
                    c.getString(7),
                    c.getString(8),
                    c.getInt(9),
                    c.getString(10),
                    rarity,
                    gender
                );
                c.close();
                return data;
            }
            c.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Erro em getPetData: " + e.getMessage());
        }
        return new PetData(species, 1, 0, 0, "Nenhum", "Nenhum", false, "Normal", "Nenhum", 0, "Nenhum", Rarity.COMMON, 0);
    }

    public List<CapturedCreature> getCollection() {
        List<CapturedCreature> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("SELECT creature_name, is_shiny, rarity, is_in_party, gender FROM collection", null);
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                if (name == null) continue;
                String rarityStr = cursor.getString(2);
                Rarity r = Rarity.COMMON;
                try { if (rarityStr != null) r = Rarity.valueOf(rarityStr); } catch (Exception e) {}
                int gender = cursor.getColumnCount() > 4 ? cursor.getInt(4) : 0;
                list.add(new CapturedCreature(name, cursor.getInt(1) == 1, r, cursor.getInt(3) == 1, gender));
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Erro em getCollection: " + e.getMessage());
        }
        return list;
    }

    public Map<String, BuildingData> getBuildings() {
        Map<String, BuildingData> map = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT building_name, level, is_constructed, pos_x, pos_y, construction_finish_time FROM buildings", null);
        while (cursor.moveToNext()) {
            BuildingData data = new BuildingData(cursor.getInt(1), cursor.getInt(2) == 1);
            data.posX = cursor.getDouble(3);
            data.posY = cursor.getDouble(4);
            data.constructionFinishTime = cursor.getLong(5);
            map.put(cursor.getString(0), data);
        }
        cursor.close();
        return map;
    }

    // --- AUXILIARES INTERNOS ---

    private void savePetInitial(SQLiteDatabase db, String species) {
        ContentValues v = new ContentValues();
        v.put("species", species);
        v.put("level", 1);
        v.put("exp", 0);
        v.put("is_shiny", 0);
        v.put("rarity", "COMMON");
        v.put("gender", 0);
        db.insertWithOnConflict("pets", null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private void saveBuildingLevel(SQLiteDatabase db, String name, int level, int constructed, double x, double y) {
        ContentValues v = new ContentValues();
        v.put("building_name", name);
        v.put("level", level);
        v.put("is_constructed", constructed);
        v.put("pos_x", x);
        v.put("pos_y", y);
        v.put("construction_finish_time", 0);
        db.insert("buildings", null, v);
    }

    public int[] getPlayerStats() {
        int[] s = {500, 1, 0, 0, 0, 0, -1, 1, 0, 1, 0};
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor cursor = db.rawQuery("SELECT gold, level, exp, talent_points, gems, ng_plus, skin_color, fishing_level, fishing_exp, aincrad_floor, daycare_intro_seen FROM player_stats WHERE id=1", null);
            if (cursor.moveToFirst()) {
                int count = Math.min(11, cursor.getColumnCount());
                for (int i = 0; i < count; i++) s[i] = cursor.getInt(i);
            }
            cursor.close();
        } catch (Exception e) {
            Log.e("DatabaseHelper", "Erro em getPlayerStats: " + e.getMessage());
        }
        return s;
    }

    public void setDaycareIntroSeen() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("daycare_intro_seen", 1);
        db.update("player_stats", v, "id=1", null);
    }

    public void updatePlayerStatsFromCloud(int level, int exp) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("level", level);
        v.put("exp", exp);
        db.update("player_stats", v, "id=1", null);
    }

    public void clearParty() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("is_in_party", 0);
        db.update("collection", v, null, null);
    }

    public void addCreatureToCollectionDb(String name, boolean isShiny, Rarity rarity, boolean isInParty, int gender) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("creature_name", name);
        cv.put("is_shiny", isShiny ? 1 : 0);
        cv.put("rarity", rarity.name());
        cv.put("is_in_party", isInParty ? 1 : 0);
        cv.put("gender", gender);
        db.insert("collection", null, cv);
    }

    public boolean isTutorialCompleted() {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT tutorial_completed FROM player_stats WHERE id=1", null);
            boolean completed = false;
            if (c.moveToFirst()) {
                completed = c.getInt(0) == 1;
            }
            c.close();
            return completed;
        } catch (Exception e) {
            // Tenta adicionar a coluna caso a tabela já existisse
            try { db.execSQL("ALTER TABLE player_stats ADD COLUMN tutorial_completed INTEGER DEFAULT 0"); } catch (Exception ex) {}
            return false;
        }
    }

    public void setTutorialCompleted(boolean completed) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("tutorial_completed", completed ? 1 : 0);
        db.update("player_stats", v, "id=1", null);
    }

    public boolean hasFaintTutorialShown() {
        SQLiteDatabase db = this.getReadableDatabase();
        try {
            Cursor c = db.rawQuery("SELECT faint_tutorial_shown FROM player_stats WHERE id=1", null);
            boolean shown = false;
            if (c.moveToFirst()) {
                shown = c.getInt(0) == 1;
            }
            c.close();
            return shown;
        } catch (Exception e) {
            try { db.execSQL("ALTER TABLE player_stats ADD COLUMN faint_tutorial_shown INTEGER DEFAULT 0"); } catch (Exception ex) {}
            return false;
        }
    }

    public void setFaintTutorialShown() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("faint_tutorial_shown", 1);
        db.update("player_stats", v, "id=1", null);
    }

    public Map<String, Integer> getInventory() {
        Map<String, Integer> map = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT item_name, quantity FROM inventory", null);
        while (cursor.moveToNext()) map.put(cursor.getString(0), cursor.getInt(1));
        cursor.close();
        return map;
    }

    public void updateActiveSpecies(String species) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("active_species", species);
        db.update("player_stats", v, "id=1", null);
    }

    public void addToBestiary(String species) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("species_name", species);
        db.insertWithOnConflict("bestiary", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public Set<String> getBestiary() {
        Set<String> set = new HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT species_name FROM bestiary", null);
        while (cursor.moveToNext()) set.add(cursor.getString(0));
        cursor.close();
        return set;
    }

    public String getActiveSpecies() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT active_species FROM player_stats WHERE id=1", null);
        String s = "Pikachu";
        if (cursor.moveToFirst()) s = cursor.getString(0);
        cursor.close();
        return s;
    }

    public Map<String, Integer> getTalents() {
        Map<String, Integer> map = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT talent_name, level FROM talents", null);
        while (cursor.moveToNext()) map.put(cursor.getString(0), cursor.getInt(1));
        cursor.close();
        return map;
    }

    public String getClan() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT clan FROM player_stats WHERE id=1", null);
        String clan = "Nenhum";
        if (cursor.moveToFirst()) clan = cursor.getString(0);
        cursor.close();
        return clan;
    }

    public void saveAchievement(String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("title", title);
        db.insertWithOnConflict("achievements", null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public Set<String> getUnlockedAchievements() {
        Set<String> set = new HashSet<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT title FROM achievements", null);
        while (c.moveToNext()) set.add(c.getString(0));
        c.close();
        return set;
    }

    public void incrementCaptureCount(String species) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("INSERT OR REPLACE INTO capture_counts (species, count) VALUES (?, COALESCE((SELECT count FROM capture_counts WHERE species = ?), 0) + 1)", new String[]{species, species});
    }

    public int getCaptureCount(String species) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT count FROM capture_counts WHERE species = ?", new String[]{species});
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count;
    }
}
