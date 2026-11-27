package com.example.pokeworld.core;

import android.content.Context;
import android.graphics.Canvas;

import com.example.pokeworld.R;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.Equipment;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.entities.BuildingData;
import com.example.pokeworld.managers.AchievementManager;
import com.example.pokeworld.managers.AincradManager;
import com.example.pokeworld.managers.ArenaManager;
import com.example.pokeworld.managers.BestiaryManager;
import com.example.pokeworld.managers.BossRushManager;
import com.example.pokeworld.managers.BountyManager;
import com.example.pokeworld.managers.BreedingManager;
import com.example.pokeworld.managers.CombatManager;
import com.example.pokeworld.managers.EffectManager;
import com.example.pokeworld.managers.FarmingManager;
import com.example.pokeworld.managers.FishingManager;
import com.example.pokeworld.managers.FusionManager;
import com.example.pokeworld.managers.MissionManager;
import com.example.pokeworld.managers.MultiplayerManager;
import com.example.pokeworld.managers.ParticleManager;
import com.example.pokeworld.managers.RandomEventManager;
import com.example.pokeworld.managers.SoundManager;
import com.example.pokeworld.managers.SpriteManager;
import com.example.pokeworld.managers.SummonManager;
import com.example.pokeworld.managers.TimeManager;
import com.example.pokeworld.managers.TowerManager;
import com.example.pokeworld.managers.TutorialManager;
import com.example.pokeworld.managers.VibrationManager;
import com.example.pokeworld.managers.WeatherManager;
import com.example.pokeworld.ui.GameInterfaceOverlay;
import com.example.pokeworld.ui.InputHandler;
import com.example.pokeworld.ui.UIManager;
import com.example.pokeworld.world.DungeonManager;
import com.example.pokeworld.world.KingdomManager;
import com.example.pokeworld.world.WorldManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class GameView {
    public WorldManager world;
    public UIManager ui;
    public InputHandler input;
    public MissionManager missions;
    public FishingManager fishing;
    public CombatManager combat;
    public FusionManager fusion;
    public EffectManager effects;
    public KingdomManager kingdom;
    public ParticleManager particles;
    public WeatherManager weather;
    public TimeManager time;
    public DungeonManager dungeon;
    public ArenaManager arena;
    public BestiaryManager bestiary;
    public AchievementManager achievements;
    public SummonManager summon;
    public RandomEventManager events;
    public BossRushManager bossRush;
    public AincradManager aincrad;
    public VibrationManager vibration;
    public BreedingManager breeding;
    public BountyManager bounties;
    public FarmingManager farming;
    public TutorialManager tutorial;
    public com.example.pokeworld.managers.TipManager tips;
    public TowerManager tower;
    
    public Player player;
    public Companion companion;
    public MainActivity activity;

    public GameView(Context context, int gold, List<CapturedCreature> collection, Map<String, Integer> inventory, String species, PetData petData) {
        this.activity = (MainActivity) context;
        
        SpriteManager.getInstance().loadSprites(context);
        
        this.player = new Player(0, 0, 30, gold, collection, inventory);
        this.companion = new Companion(context, 0, 0, 20, species);
        this.companion.loadProgress(petData.level, petData.exp, petData.forgeBonus, petData.isShiny, petData.friendship, petData.type2, petData.rarity, petData.gender);
        this.companion.setWeapon(Equipment.create(petData.weapon));
        this.companion.setArmor(Equipment.create(petData.armor));
        
        this.ui = new UIManager();
        this.world = new WorldManager(activity);
        this.missions = new MissionManager(player, ui);
        this.missions.bind(activity.dbHelper);
        this.fishing = new FishingManager(this);
        this.combat = new CombatManager(this);
        this.fusion = new FusionManager(player, ui);
        this.input = new InputHandler(this);
        this.effects = new EffectManager();
        this.kingdom = new KingdomManager();
        this.particles = new ParticleManager();
        this.weather = new WeatherManager();
        this.time = new TimeManager();
        this.dungeon = new DungeonManager(this);
        this.arena = new ArenaManager(this);
        this.bestiary = new BestiaryManager(activity.dbHelper);
        this.achievements = new AchievementManager(ui, activity.dbHelper);
        this.summon = new SummonManager(this);
        this.events = new RandomEventManager(this);
        this.bossRush = new BossRushManager(this);
        this.aincrad = new AincradManager(this);
        this.vibration = new VibrationManager(context);
        SoundManager.getInstance(context);
        this.breeding = new BreedingManager(this);
        this.bounties = new BountyManager(this);
        this.farming = new FarmingManager(this);
        this.tutorial = new TutorialManager(this);
        this.tips = new com.example.pokeworld.managers.TipManager(this);
        this.tower = new TowerManager(this);

        missions.giveInitialMission();

        GameInterfaceOverlay uiOverlay = activity.findViewById(R.id.uiOverlay);
        if (uiOverlay != null) uiOverlay.setGameView(this);

        // Inicia carregando o mapa
        if (!activity.dbHelper.isTutorialCompleted()) {
            world.loadMap("STARTER_FOREST", player, companion, "CENTER");
            tutorial.beginOrResume();
        } else {
            world.loadMap("CITY", player, companion, "CENTER");
        }
        com.example.pokeworld.managers.PartyManager.restoreActiveHp(this);
        time.setCycleEnabled(activity.dbHelper.getKv("set_night", 0) == 1);                                 // dia/noite: desligado por padrão
        vibration.setEnabled(activity.dbHelper.getKv("set_vib", 1) == 1);                                   // preferências de som/vibração salvas
        com.example.pokeworld.managers.SoundManager.getInstance(activity).setEnabled(activity.dbHelper.getKv("set_sound", 1) == 1);
    }

    public void applyKingdomBonuses() {
        Map<String, BuildingData> buildings = activity.dbHelper.getBuildings();
        // Converte Map<String, BuildingData> para Map<String, Integer> de níveis para o KingdomManager
        Map<String, Integer> levels = new HashMap<>();
        for (Map.Entry<String, BuildingData> e : buildings.entrySet()) {
            levels.put(e.getKey(), e.getValue().level);
        }
        // Prédios da base (sistema de construção livre): valem os que já estão prontos
        for (com.example.pokeworld.world.BasePiece bp : activity.dbHelper.getBasePieces()) {
            if (bp.finishTime == 0 || bp.upgrading) {
                String key = bp.kind.legacyName();
                if (bp.level > levels.getOrDefault(key, 0)) levels.put(key, bp.level);
            }
        }
        kingdom.applyBonuses(companion, levels);
        companion.applyClanBonus(player.getClan());
    }

    private int shakeDuration = 0;
    private float shakeMagnitude = 0;
    private Random random = new Random();
    
    private long lastAutoSaveTime = 0;
    private static final long AUTO_SAVE_INTERVAL = 10000; // 10 segundos

    private double lastPlayerX, lastPlayerY;

    public void update() {
        ui.updateFade();

        // XP compartilhado: colegas do grupo derrotaram um Pokémon perto de você
        int sharedXp = com.example.pokeworld.managers.GroupManager.getInstance().pollSharedXp();
        if (sharedXp > 0 && companion != null && player != null) {
            companion.gainExp(sharedXp);
            boolean up = player.gainExp(sharedXp / 2);
            ui.showMessage("👥 +" + sharedXp + " XP (grupo)" + (up ? "  🎊 NÍVEL UP!" : ""));
        }
        
        if (shakeDuration > 0) shakeDuration--;
        
        // Auto-salvamento periódico
        if (System.currentTimeMillis() - lastAutoSaveTime > AUTO_SAVE_INTERVAL) {
            activity.saveCurrentGame();
            lastAutoSaveTime = System.currentTimeMillis();
        }

        if (ui.state == UIManager.UIState.ARENA_BATTLE) {
            // Sala da Arena: você anda com o joystick (atrás da linha) enquanto o duelo roda sozinho
            input.joystick.update();
            player.update(input.joystick, world, false, false, false, particles, companion.getSpecies(), companion.getPlayerSpeedMultiplier());
            com.example.pokeworld.ui.handlers.ArenaBattleUI arenaRoom = (com.example.pokeworld.ui.handlers.ArenaBattleUI) ui.getHandler(UIManager.UIState.ARENA_BATTLE);
            if (arenaRoom != null) arenaRoom.update(this);
            effects.update();
            particles.update();
        }

        if (ui.state == UIManager.UIState.EXPLORING && !ui.isDialogueActive) {
            input.joystick.update();
            
            boolean hasFireSaddle = player.getInventory().containsKey("Sela de Fogo");
            boolean hasWaterSaddle = player.getInventory().containsKey("Sela Aquática");

            boolean canSurf = player.isMounted() && (companion.getFieldAbility() == Companion.FieldAbility.SURF || hasWaterSaddle);
            boolean canFly = player.isMounted() && (companion.getFieldAbility() == Companion.FieldAbility.FLY);
            boolean isAquatic = companion.isAquatic() || (player.isMounted() && hasWaterSaddle);
            
            player.update(input.joystick, world, canSurf, canFly, isAquatic, particles, companion.getSpecies(), companion.getPlayerSpeedMultiplier());
            
            // --- Efeitos Climáticos Extremos ---
            String weatherType = weather.getCurrentType();
            if (weatherType.equals("HEATWAVE")) {
                // Efeito de calor (ex: cansa mais rápido)
                player.useStamina(0.01f);
            } else if (weatherType.equals("RAIN")) {
                // Diminui velocidade na chuva
                player.applySpeedDebuff(0.8f); 
            } else if (weatherType.equals("SNOW") && com.example.pokeworld.world.MapVariants.base(world.tileMap.getMapName()).equals("SNOW_MOUNTAIN")) {
                if (!player.getInventory().containsKey("Capa Térmica")) {
                    if (System.currentTimeMillis() % 1000 < 50) {
                        player.takeDamage(5);
                        ui.showMessage("❄️ ESTÁ CONGELANDO! USE UMA CAPA!");
                    }
                    player.applySpeedDebuff(0.6f);
                }
            }

            // Calcula distância movida para o Daycare (Ovos)
            double dist = Math.sqrt(Math.pow(player.getPositionX() - lastPlayerX, 2) + Math.pow(player.getPositionY() - lastPlayerY, 2));
            if (dist > 1 && dist < 100) { 
                breeding.updateProgress(dist);
            }
            lastPlayerX = player.getPositionX();
            lastPlayerY = player.getPositionY();

            companion.update(player, world.enemies, effects, particles, world, weather.getCurrentType());
            world.update(player, companion, this);
            aincrad.update();
            farming.update();
            tower.update();
            achievements.check(this);
            missions.check(this);
            tips.update();
            com.example.pokeworld.managers.PartyManager.updateFaint(this);
            com.example.pokeworld.managers.PartyManager.tickTeam(this);
            
            effects.update();
            particles.update();
            
            // Efeitos de Bioma
            String map = com.example.pokeworld.world.MapVariants.base(world.tileMap.getMapName());
            if (map.equals("FOREST")) particles.addBiomeEffect("LEAVES", getWidth(), getHeight());
            else if (map.equals("VOLCANO")) particles.addBiomeEffect("EMBER", getWidth(), getHeight());

            weather.update(this);
            events.update();
            time.update();
            fishing.update();
            if (tutorial != null) tutorial.update();

            // Envia estado do jogador local e do acompanhante via Multiplayer
            MultiplayerManager.getInstance().sendPlayerState(
                world.tileMap.getMapName(),
                player.getPositionX(),
                player.getPositionY(),
                player.getDirection(),
                player.getFrameIndex(),
                player.getSkinColor(),
                player.isMounted(),
                companion != null ? companion.getSpecies() : "",
                companion != null ? companion.getPositionX() : player.getPositionX() - 45,
                companion != null ? companion.getPositionY() : player.getPositionY(),
                companion != null ? companion.getDirection() : 0,
                companion != null ? companion.getFrameIndex() : 1,
                companion != null && companion.isShiny()
            );
        }
    }

    public void draw(Canvas canvas) {
        if (world != null) {
            world.draw(canvas, player, companion, this);
        }
        if (tutorial != null) {
            tutorial.drawOverlay(canvas);
        }
    }

    public void startShake(int duration, float magnitude) {
        this.shakeDuration = duration;
        this.shakeMagnitude = magnitude;
    }

    public float getShakeX() {
        return shakeDuration > 0 ? (random.nextFloat() - 0.5f) * 2 * shakeMagnitude : 0;
    }

    public float getShakeY() {
        return shakeDuration > 0 ? (random.nextFloat() - 0.5f) * 2 * shakeMagnitude : 0;
    }
    public void resume() {}
    public void pause() {}
    public int getWidth() { return activity.getResources().getDisplayMetrics().widthPixels; }
    public int getHeight() { return activity.getResources().getDisplayMetrics().heightPixels; }
}
