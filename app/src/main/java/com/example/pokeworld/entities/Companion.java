package com.example.pokeworld.entities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.pokeworld.core.MainActivity;
import com.example.pokeworld.managers.EffectManager;
import com.example.pokeworld.managers.ParticleManager;
import com.example.pokeworld.managers.SpriteManager;
import com.example.pokeworld.world.TileMap;
import com.example.pokeworld.world.WorldManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Companion {
    public enum FieldAbility { NONE, SURF, SMASH, CUT, FLASH, FLY }
    public enum Command { AGGRESSIVE, DEFENSIVE, PASSIVE }

    private Context context;
    private Command currentCommand = Command.DEFENSIVE;

    private double positionX, positionY, radius;
    private static final double FOLLOW_SPEED = 0.10, ATTACK_SPEED = 7.0, FOLLOW_DIST = 80.0, ATTACK_RANGE = 70.0;
    private Enemy targetEnemy;
    private String species;
    private String personality = "Normal"; 
    private int hp, maxHp, level = 1, attackPower = 25, exp = 0;
    private int baseMaxHp, baseAttackPower; 
    private int forgeAttackBonus = 0; 
    /** Queimadura, veneno, sono, paralisia e gelo (inimigos de fogo, veneno, etc. também causam). */
    public final StatusSet status = new StatusSet();
    private int paraTick = 0;
    private long lastAttackTime = 0;
    private static final long ATTACK_COOLDOWN = 1000;
    private List<Skill> skills = new ArrayList<>();
    
    private ElementalType type1 = ElementalType.NORMAL;
    private ElementalType type2 = null; 
    private FieldAbility fieldAbility = FieldAbility.NONE;
    
    private boolean isShiny = false;
    private Rarity rarity = Rarity.COMMON;
    private int boost = 0;                       // +N da Gema de Boost
    private TeamBonus team = null;               // apoio dos Pokémon do time que não estão lutando
    private long lastRegenTick = 0;
    private int friendship = 0; 
    private int gender = 0; // 0: Macho, 1: Fêmea
    private boolean isMega = false;
    private long megaEndTime = 0;
    private String originalSpecies;
    private boolean hasShield = false;
    private long shieldEndTime = 0;

    private float rage = 0;
    private static final float MAX_RAGE = 100f;

    private Equipment weapon;
    private Equipment armor;
    private Rune equippedRune;

    private float terrainSpeedMult = 1.0f;
    private float terrainDmgMult = 1.0f;

    private long lastAuraTick = 0;
    private static final long AURA_INTERVAL = 3000;

    private long lastFriendshipTick = 0;
    private long lastHealTick = 0;
    private long lastEmoteTime = 0;
    private String currentEmote = "";

    private long attackBuffEndTime = 0;
    private float attackBuffMult = 1.0f;

    private int direction = 0; 
    private int frameIndex = 1; // Começa no frame central (Idle)
    private long lastFrameTime = 0;
    private static final int ANIMATION_SPEED = 300;
    private boolean isMoving = false;

    public int lastDamageDealt = 0;
    public Enemy lastTargetHit = null;
    public boolean leveledUp = false;
    private int pendingSkillIndex = -1;

    public Companion(Context context, double x, double y, double r, String species) {
        this.context = context;
        this.positionX = x; this.positionY = y; this.radius = r;
        this.species = species;
        generateRandomPersonality();
        setSpeciesStats(species);
    }

    private void generateRandomPersonality() {
        String[] p = {"Brave", "Hardy", "Hasty", "Normal"};
        this.personality = p[new Random().nextInt(p.length)];
    }

    public void loadProgress(int level, int exp, int forgeBonus, boolean isShiny, int friendship, String t2, Rarity rarity, int gender) {
        this.level = level;
        this.exp = exp;
        this.forgeAttackBonus = forgeBonus;
        this.isShiny = isShiny;
        this.friendship = friendship;
        this.rarity = rarity != null ? rarity : Rarity.COMMON;
        this.gender = gender;
        if (t2 != null && !t2.equals("Nenhum")) {
            try {
                this.type2 = ElementalType.valueOf(t2);
            } catch (Exception e) {}
        }
        setSpeciesStats(this.species);
    }

    /** Bônus de talento do treinador (0 se o jogo ainda não está montado). */
    private double talent(String key) {
        if (context instanceof MainActivity) {
            MainActivity a = (MainActivity) context;
            if (a.gameView != null && a.gameView.player != null) return TalentTree.bonus(a.gameView.player, key);
        }
        return 0;
    }

    public void takeDamage(int amount) {
        status.wake();
        if (hasShield && System.currentTimeMillis() < shieldEndTime) return;
        if (friendship >= 50 && Math.random() < 0.1) return;
        float resistance = 0;
        if (equippedRune != null && equippedRune.getEffect() == Rune.Effect.RESISTANCE) resistance = equippedRune.getValue();
        resistance = (float) Math.min(0.8, resistance + talent("Resistencia") + (team != null ? team.resist : 0));
        int finalDamage = com.example.pokeworld.managers.Balance.dmg((int)(amount * (1.0f - resistance)));
        if (this.hp - finalDamage <= 0 && friendship >= 100 && this.hp > 1) this.hp = 1;
        else this.hp -= finalDamage;
        if (this.hp < 0) this.hp = 0;
        addRage(finalDamage / 5f); 
    }

    public void addRage(float amount) {
        rage += amount * (1f + (float) talent("Furia"));
        if (rage > MAX_RAGE) rage = MAX_RAGE;
    }

    private void setSpeciesStats(String species) {
        skills.clear();
        skills.add(new Skill("Investida", 10 + level, 2000, Color.WHITE, ElementalType.NORMAL));
        
        String s = species.toUpperCase();
        if (s.contains("PIKACHU") || s.contains("RAICHU")) {
            this.type1 = ElementalType.ELECTRIC;
            this.fieldAbility = s.contains("RAICHU") ? FieldAbility.FLY : FieldAbility.FLASH;
            this.baseMaxHp = (s.contains("RAICHU") ? 600 : 150) + (level * 25); 
            this.baseAttackPower = (s.contains("RAICHU") ? 140 : 40) + (level * 10);
            if (level >= 5) skills.add(new Skill("Choque do Trovão", 40, 4000, Color.YELLOW, ElementalType.ELECTRIC));
            if (level >= 25) skills.add(new Skill("Trovão", 100, 8000, Color.YELLOW, ElementalType.ELECTRIC, 240));
        } else if (s.contains("CHARMANDER") || s.contains("CHARMELEON") || s.contains("CHARIZARD")) {
            this.type1 = ElementalType.FIRE;
            this.fieldAbility = s.contains("CHARIZARD") ? FieldAbility.FLY : FieldAbility.CUT;
            this.baseMaxHp = (s.contains("CHARIZARD") ? 900 : s.contains("CHARMELEON") ? 450 : 180) + (level * 30);
            this.baseAttackPower = (s.contains("CHARIZARD") ? 180 : s.contains("CHARMELEON") ? 100 : 45) + (level * 12);
            if (level >= 5) skills.add(new Skill("Brasa", 45, 3000, Color.RED, ElementalType.FIRE));
            if (level >= 18) skills.add(new Skill("Fúria do Dragão", 65, 5000, Color.rgb(255, 69, 0), ElementalType.DRAGON, 220));
            if (level >= 36) skills.add(new Skill("Lança-Chamas", 90, 7000, Color.rgb(255, 100, 0), ElementalType.FIRE));
        } else if (s.contains("MEGA CHARIZARD X")) {
            this.type1 = ElementalType.FIRE;
            this.type2 = ElementalType.DRAGON;
            this.baseMaxHp = 1500 + (level * 45);
            this.baseAttackPower = 250 + (level * 20);
            skills.add(new Skill("Garras de Dragão", 85, 3000, Color.BLUE, ElementalType.DRAGON));
        } else if (s.contains("MEGA CHARIZARD Y")) {
            this.type1 = ElementalType.FIRE;
            this.fieldAbility = FieldAbility.FLY;
            this.baseMaxHp = 1300 + (level * 40);
            this.baseAttackPower = 320 + (level * 25);
            skills.add(new Skill("Explosão Solar", 130, 4000, Color.YELLOW, ElementalType.FIRE, 280));
        } else if (s.contains("SQUIRTLE") || s.contains("WARTORTLE") || s.contains("BLASTOISE")) {
            this.type1 = ElementalType.WATER;
            this.fieldAbility = FieldAbility.SURF;
            this.baseMaxHp = (s.contains("BLASTOISE") ? 1100 : s.contains("WARTORTLE") ? 550 : 200) + (level * 35);
            this.baseAttackPower = (s.contains("BLASTOISE") ? 150 : s.contains("WARTORTLE") ? 85 : 35) + (level * 10);
            if (level >= 5) skills.add(new Skill("Jato d'Água", 35, 2500, Color.BLUE, ElementalType.WATER));
            if (level >= 20) skills.add(new Skill("Raio de Gelo", 60, 4500, Color.CYAN, ElementalType.ICE));
            if (level >= 36) skills.add(new Skill("Hidro Canhão", 100, 9000, Color.BLUE, ElementalType.WATER, 250));
        } else if (s.contains("BULBASAUR") || s.contains("IVYSAUR") || s.contains("VENUSAUR")) {
            this.type1 = ElementalType.PLANT;
            this.fieldAbility = FieldAbility.CUT;
            this.baseMaxHp = (s.contains("VENUSAUR") ? 1200 : s.contains("IVYSAUR") ? 600 : 220) + (level * 40); 
            this.baseAttackPower = (s.contains("VENUSAUR") ? 160 : s.contains("IVYSAUR") ? 90 : 40) + (level * 11);
            skills.add(new Skill("Chicote de Vinha", 35, 2500, Color.GREEN, ElementalType.PLANT));
            if (level >= 8) skills.add(new Skill("Folha Navalha", 45, 3500, Color.GREEN, ElementalType.PLANT));
            if (level >= 22) skills.add(new Skill("Pó do Sono", 20, 6000, Color.MAGENTA, ElementalType.NORMAL));
            if (level >= 28) skills.add(new Skill("Pétalas Dançantes", 80, 5000, Color.rgb(255, 105, 180), ElementalType.PLANT));
            if (level >= 32) skills.add(new Skill("Solar Beam", 120, 10000, Color.WHITE, ElementalType.PLANT, 260));
        } else if (s.contains("MEGA VENUSAUR")) {
            this.type1 = ElementalType.PLANT;
            this.type2 = ElementalType.POISON;
            this.baseMaxHp = 1400 + (level * 40);
            this.baseAttackPower = 180 + (level * 15);
            skills.add(new Skill("Petal Blizzard", 100, 4000, Color.MAGENTA, ElementalType.PLANT));
        } else if (s.contains("PIDGEOTTO") || s.contains("PIDGEOT")) {
            this.type1 = ElementalType.NORMAL;
            this.fieldAbility = s.contains("PIDGEOT") ? FieldAbility.FLY : FieldAbility.NONE;
            this.baseMaxHp = (s.contains("PIDGEOT") ? 700 : 400) + (level * 22);
            this.baseAttackPower = (s.contains("PIDGEOT") ? 130 : 80) + (level * 9);
            skills.add(new Skill("Ataque de Asa", 40, 3000, Color.LTGRAY, ElementalType.NORMAL));
        } else if (s.contains("RAIQUAZA")) {
            this.type1 = ElementalType.DRAGON;
            this.fieldAbility = FieldAbility.FLY;
            this.baseMaxHp = 2500 + (level * 100); this.baseAttackPower = 400 + (level * 40);
            skills.add(new Skill("Hyper Beam", 120, 10000, Color.WHITE, ElementalType.DRAGON, 300));
        } else if (s.contains("LAPRAS")) {
            this.type1 = ElementalType.WATER;
            this.type2 = ElementalType.ICE;
            this.fieldAbility = FieldAbility.SURF;
            this.baseMaxHp = 800 + (level * 45);
            this.baseAttackPower = 110 + (level * 12);
            if (level >= 5) skills.add(new Skill("Jato d'Água", 35, 2500, Color.BLUE, ElementalType.WATER));
            if (level >= 15) skills.add(new Skill("Raio de Gelo", 65, 4000, Color.CYAN, ElementalType.ICE));
        } else if (s.contains("LOBOFOGO")) {
            this.type1 = ElementalType.FIRE;
            this.baseMaxHp = 450 + (level * 28);
            this.baseAttackPower = 100 + (level * 14);
        } else if (s.contains("LOBO")) {
            this.type1 = ElementalType.NORMAL;
            this.baseMaxHp = 400 + (level * 24);
            this.baseAttackPower = 85 + (level * 11);
        } else if (s.contains("PICHU")) {
            this.type1 = ElementalType.ELECTRIC;
            this.baseMaxHp = 120 + (level * 12);
            this.baseAttackPower = 35 + (level * 6);
        } else if (s.contains("GROWLITHE") || s.contains("ARCANINE")) {
            this.type1 = ElementalType.FIRE;
            this.baseMaxHp = (s.contains("ARCANINE") ? 950 : 380) + (level * 32);
            this.baseAttackPower = (s.contains("ARCANINE") ? 210 : 100) + (level * 16);
        } else if (s.contains("FLAREON")) {
            this.type1 = ElementalType.FIRE;
            this.baseMaxHp = 850 + (level * 28);
            this.baseAttackPower = 260 + (level * 22);
        } else if (s.contains("VAPOREON")) {
            this.type1 = ElementalType.WATER;
            this.baseMaxHp = 1200 + (level * 40);
            this.baseAttackPower = 170 + (level * 13);
        } else if (s.contains("JOLTEON")) {
            this.type1 = ElementalType.ELECTRIC;
            this.baseMaxHp = 780 + (level * 22);
            this.baseAttackPower = 240 + (level * 24);
        } else if (s.contains("HOUNDOUR")) {
            this.type1 = ElementalType.FIRE;
            this.baseMaxHp = 350 + (level * 24);
            this.baseAttackPower = 95 + (level * 15);
        } else if (s.contains("ZAPDOS")) {
            this.type1 = ElementalType.ELECTRIC;
            this.type2 = ElementalType.NORMAL; 
            this.fieldAbility = FieldAbility.FLY;
            this.baseMaxHp = 2200 + (level * 80);
            this.baseAttackPower = 380 + (level * 35);
        } else {
            this.type1 = ElementalType.NORMAL;
            this.baseMaxHp = 250 + (level * 20); this.baseAttackPower = 50 + (level * 10);
        }

        addTypeMoves();

        if (isShiny) { this.baseMaxHp *= 1.2; this.baseAttackPower *= 1.2; }
        if (personality.equals("Brave")) this.baseAttackPower *= 1.1;
        if (personality.equals("Hardy")) this.maxHp *= 1.1;

        resetToBaseStats();
        this.hp = maxHp;
    }


    // ---------------------------------------------------------------- golpes por tipo (todo Pokémon aprende conforme sobe de nível)
    private static final int MAX_SKILLS = 9;
    /** Por tipo, 8 golpes: {nível, dano, recarga(ms), forma, parâmetro}. Forma: 0 direto, 1 área circular (raio), 2 grade de quadrados (SkillPattern). */
    private static final int[][] MOVE_SLOTS = {
            {4, 28, 2400, 0, 0},
            {8, 38, 4200, 2, 1},      // SQUARE3
            {12, 48, 4500, 1, 190},
            {18, 62, 5600, 2, 3},     // CROSS
            {24, 88, 7000, 0, 0},
            {30, 92, 7500, 2, 6},     // CONE (ou LINE nos tipos de feixe)
            {36, 120, 9500, 1, 280},
            {42, 150, 11000, 2, 2}};  // SQUARE5
    private static final String[][] MOVE_NAMES = {
            /* NORMAL   */ {"Ataque Rápido", "Pisotear", "Corpo a Corpo", "Cruz de Impacto", "Golpe Brutal", "Rugido", "Explosão Normal", "Impacto Final"},
            /* FIRE     */ {"Brasa", "Chão em Chamas", "Roda de Fogo", "Cruz Flamejante", "Lança-Chamas", "Rajada de Fogo", "Inferno", "Mar de Chamas"},
            /* WATER    */ {"Jato d'Água", "Poça Gigante", "Maré Alta", "Cruz de Bolhas", "Hidro Bomba", "Onda Marinha", "Tsunami", "Dilúvio"},
            /* PLANT    */ {"Folha Navalha", "Esporos", "Chuva de Sementes", "Cruz de Espinhos", "Chicote Solar", "Vinha Selvagem", "Tempestade de Folhas", "Floresta Viva"},
            /* ELECTRIC */ {"Choque do Trovão", "Campo Elétrico", "Onda de Choque", "Cruz de Raios", "Trovão Forte", "Raio Linear", "Tempestade Elétrica", "Fúria dos Céus"},
            /* ICE      */ {"Raio de Gelo", "Chão Congelado", "Nevasca", "Cruz de Cristais", "Lança de Gelo", "Vento Glacial", "Era Glacial", "Zero Absoluto"},
            /* ROCK     */ {"Pedrada", "Chuva de Pedras", "Avalanche", "Cruz de Rochas", "Rocha Afiada", "Deslizamento", "Terremoto", "Cataclismo"},
            /* POISON   */ {"Ferrão Tóxico", "Esporos Venenosos", "Nuvem Tóxica", "Cruz Tóxica", "Bomba de Lodo", "Rajada Ácida", "Chuva Ácida", "Pântano Mortal"},
            /* PSYCHIC  */ {"Confusão", "Runas Místicas", "Onda Psíquica", "Cruz Psíquica", "Psicorte", "Raio Mental", "Mente Esmagadora", "Colapso Mental"},
            /* DRAGON   */ {"Garra de Dragão", "Chamas Dracônicas", "Sopro Dracônico", "Cruz Dracônica", "Pulso do Dragão", "Fúria Linear", "Cometa Dracônico", "Ira do Dragão"}};
    private static final int[] MOVE_COLORS = {
            Color.LTGRAY, Color.rgb(255, 110, 30), Color.rgb(60, 140, 255), Color.rgb(80, 200, 80), Color.YELLOW,
            Color.CYAN, Color.rgb(170, 140, 100), Color.rgb(170, 70, 210), Color.rgb(255, 100, 180), Color.rgb(100, 90, 240)};
    private static final SkillPattern[] PATTERNS = {SkillPattern.NONE, SkillPattern.SQUARE3, SkillPattern.SQUARE5, SkillPattern.CROSS, SkillPattern.RING, SkillPattern.LINE, SkillPattern.CONE};

    /** Estado que os golpes do tipo causam (null: nenhum). O 2º golpe (grade 3x3) é o "esporo": chance maior. */
    private static Status statusForType(ElementalType t, int slot) {
        switch (t) {
            case FIRE: return Status.BURN;
            case POISON: return Status.POISON;
            case ELECTRIC: return Status.PARALYZE;
            case ICE: return Status.FREEZE;
            case PSYCHIC: return slot == 1 || slot == 7 ? Status.SLEEP : null;
            case PLANT: return slot == 1 ? Status.SLEEP : slot == 3 ? Status.POISON : null;
            default: return null;
        }
    }

    private boolean hasSkill(String name) {
        for (Skill k : skills) if (k.getName().equalsIgnoreCase(name)) return true;
        return false;
    }

    /** Acrescenta os golpes do(s) tipo(s) do Pokémon que o nível atual já libera (sem repetir os da espécie). */
    private void addTypeMoves() {
        addMovesOf(type1, new int[]{0, 1, 2, 3, 4, 5, 6, 7});
        if (type2 != null && type2 != type1) addMovesOf(type2, new int[]{1, 4});   // tipo secundário: só 1 área e 1 direto
    }

    private void addMovesOf(ElementalType t, int[] slots) {
        if (t == null) return;
        int ti = t.ordinal();
        if (ti >= MOVE_NAMES.length) return;
        for (int si : slots) {
            if (skills.size() >= MAX_SKILLS) return;
            int[] m = MOVE_SLOTS[si];
            String name = MOVE_NAMES[ti][si];
            if (level < m[0] || hasSkill(name)) continue;
            Skill sk;
            if (m[3] == 2) {
                int pi = m[4];
                // o 6º golpe é um feixe (linha) nos tipos de raio/jato e um cone nos demais
                if (si == 5 && (t == ElementalType.ELECTRIC || t == ElementalType.PSYCHIC || t == ElementalType.DRAGON || t == ElementalType.POISON || t == ElementalType.WATER)) pi = 5;
                sk = new Skill(name, m[1], m[2], MOVE_COLORS[ti], t).withPattern(PATTERNS[pi]);
            } else if (m[3] == 1) sk = new Skill(name, m[1], m[2], MOVE_COLORS[ti], t, m[4]);
            else sk = new Skill(name, m[1], m[2], MOVE_COLORS[ti], t);
            Status st = statusForType(t, si);
            if (st != null) sk.withStatus(st, si == 1 ? 0.45f : m[3] == 0 ? 0.2f : 0.3f);
            skills.add(sk);
        }
    }

    public void resetToBaseStats() {
        this.maxHp = baseMaxHp;
        
        // Bônus de Raridade (10% por nível de raridade extra)
        double rarityMult = 1.0 + (rarity.ordinal() * 0.1);
        
        // Bônus de Gênero (Macho: +10% Dano, Fêmea: +10% Vida)
        double boostMult = 1.0 + 0.08 * boost;                       // cada +1 = +8% de ataque e vida
        double atkMult = (gender == 0 ? 1.1 : 1.0) * (1.0 + talent("Guerreiro")) * boostMult * (1.0 + (team != null ? team.atk : 0));
        double hpMult = (gender == 1 ? 1.1 : 1.0) * (1.0 + talent("Vitalidade")) * boostMult * (1.0 + (team != null ? team.hp : 0));

        this.attackPower = (int)((baseAttackPower + forgeAttackBonus) * attackBuffMult * rarityMult * atkMult);
        this.maxHp = (int)(this.maxHp * rarityMult * hpMult * com.example.pokeworld.managers.Balance.HP);

        if (weapon != null) this.attackPower += weapon.getPower();
        if (armor != null) this.maxHp += armor.getPower();
        if (equippedRune != null && equippedRune.getEffect() == Rune.Effect.POWER) this.attackPower *= equippedRune.getValue();
    }

    public void applyClanBonus(String clan) {
        if (clan.equals("Rubi") && (type1 == ElementalType.FIRE || type2 == ElementalType.FIRE)) this.attackPower *= 1.15;
        else if (clan.equals("Safira") && (type1 == ElementalType.WATER || type2 == ElementalType.WATER)) this.maxHp *= 1.15;
        else if (clan.equals("Esmeralda") && (type1 == ElementalType.PLANT || type2 == ElementalType.PLANT)) this.maxHp *= 1.15;
    }

    public void updateTerrainBuffs(String mapName, boolean isNight) {
        terrainSpeedMult = personality.equals("Hasty") ? 1.2f : 1.0f;
        terrainDmgMult = 1.0f;
        if (mapName.equals("MINE") && (type1 == ElementalType.ROCK || type2 == ElementalType.ROCK)) { terrainDmgMult = 1.3f; terrainSpeedMult *= 1.2f; }
        if (mapName.equals("VOLCANO") && (type1 == ElementalType.FIRE || type2 == ElementalType.FIRE)) terrainDmgMult = 1.4f;
    }

    private long skillCastEndTime = 0;
    private long atkAnimStart = -100000, seenSkillEnd = 0;     // animação de ataque (frames girando + avanço no alvo)
    private double atkDirX = 0, atkDirY = 1;
    private static final int ATTACK_ANIM_MS = 600;
    private String currentCastingSpecies = null;

    public void update(Player player, List<Enemy> enemies, EffectManager effects, ParticleManager particles, WorldManager world, String weather) {
        lastDamageDealt = 0; lastTargetHit = null;
        checkMegaTimeout();
        if (status.any() && hp > 0) {
            int dot = status.tick(maxHp, false);
            if (dot > 0) {
                hp = Math.max(0, hp - dot);
                if (effects != null) effects.addText("-" + dot, (float) positionX, (float) positionY - 80, status.has(Status.POISON) ? 0xFFC070F0 : 0xFFFF8030);
            }
            if (status.stunned()) { isMoving = false; return; }                     // dormindo/congelado: não age
            if (status.has(Status.PARALYZE) && (++paraTick & 1) == 0) return;       // paralisado: metade do ritmo
        }
        
        if (pendingSkillIndex != -1) {
            handlePendingSkill(effects, particles, world, weather);
            return; 
        }

        // Se estiver "conjurando" uma magia, fica parado
        if (System.currentTimeMillis() < skillCastEndTime) {
            isMoving = false;
            return; 
        }
        long emoteNow = System.currentTimeMillis();
        if (emoteNow - lastEmoteTime > 5000) {
            if (targetEnemy != null) currentEmote = "💢";
            else if (hp < maxHp * 0.3) currentEmote = "💧";
            else if (friendship > 200 && Math.random() < 0.1) currentEmote = "❤️";
            else currentEmote = "";
            lastEmoteTime = emoteNow;
        }

        // Regeneração lenta: todo Pokémon recupera vida sozinho (bem devagar; fora de combate o dobro). Amizade e Pokémon de água ajudam.
        if (hp > 0 && hp < maxHp) {
            long now = System.currentTimeMillis();
            if (now - lastHealTick > 2000) {
                double rate = 0.003 + 0.003 * Math.min(1.0, friendship / 255.0);            // % da vida máxima a cada 2s
                rate *= (1.0 + (team != null ? team.regen : 0));
                if (targetEnemy != null) rate *= 0.5;
                hp = Math.min(maxHp, hp + Math.max(1, (int) Math.round(maxHp * rate)));
                lastHealTick = now;
            }
        }

        // Ganho de Amizade por caminhar
        if (player.isMoving()) {
            long now = System.currentTimeMillis();
            if (now - lastFriendshipTick > 30000) { // A cada 30s caminhando
                friendship = Math.min(255, friendship + 1);
                lastFriendshipTick = now;
            }
        }
        
        // Verifica se o buff de ataque expirou
        if (attackBuffEndTime > 0 && System.currentTimeMillis() > attackBuffEndTime) {
            attackBuffEndTime = 0;
            attackBuffMult = 1.0f;
            resetToBaseStats();
        }

        if (leveledUp) {
            handleLevelUpEffect(effects, particles);
            leveledUp = false;
        }
        
        if (hp <= 0 || player.isMounted()) { 
            // Se estiver montado ou morto, a posição do companheiro deve acompanhar o player mas não ser desenhada (no WorldManager)
            // ou simplesmente ficar no mesmo lugar/seguir sem animação.
            // Para evitar "duplicação visual", o WorldManager já não o desenha.
            // Aqui garantimos que ele não execute IA de combate ou animação.
            positionX = player.getPositionX();
            positionY = player.getPositionY();
            isMoving = false;
            return;
        }

        // Lógica baseada no comando tático
        if (currentCommand == Command.PASSIVE) {
            followPlayer(player, world);
        } else if (currentCommand == Command.AGGRESSIVE) {
            if (targetEnemy == null || !targetEnemy.isAlive()) {
                // Procura o inimigo mais próximo automaticamente e o SELECIONA (como se você tivesse tocado nele):
                // assim as habilidades já miram nele e ele continua sendo o alvo até cair
                targetEnemy = findNearestEnemy(enemies);
                if (targetEnemy != null) {
                    for (Enemy o : enemies) if (o != targetEnemy && o.isSelected() && !o.isPet) o.setSelected(false);
                    targetEnemy.setSelected(true);
                }
            } else if (!targetEnemy.isSelected()) targetEnemy.setSelected(true);
            if (targetEnemy != null) handleCombat(particles, effects, world);
            else followPlayer(player, world);
        } else { // DEFENSIVE (Comportamento padrão atual)
            targetEnemy = null;
            // Só ataca se não estiver na base ou se for um invasor (não pet)
            for (Enemy e : enemies) {
                if (e.isSelected() && e.isAlive() && !e.isPet) {
                    targetEnemy = e; break; 
                }
            }
            handleAura(enemies, effects, particles);
            if (targetEnemy != null) handleCombat(particles, effects, world);
            else followPlayer(player, world);
        }
    }

    public void applyAttackBuff(float mult, long durationMs) {
        this.attackBuffMult = mult;
        this.attackBuffEndTime = System.currentTimeMillis() + durationMs;
        resetToBaseStats();
    }

    public boolean hasAttackBuff() { return attackBuffEndTime > 0; }

    private Enemy findNearestEnemy(List<Enemy> enemies) {
        Enemy nearest = null;
        double minDist = 500; // Raio de detecção agressiva
        for (Enemy e : enemies) {
            if (e.isAlive() && !e.isPet) {
                double dist = Math.sqrt(Math.pow(e.getPositionX() - positionX, 2) + Math.pow(e.getPositionY() - positionY, 2));
                if (dist < minDist) {
                    minDist = dist;
                    nearest = e;
                }
            }
        }
        return nearest;
    }

    public void setCommand(Command cmd) { this.currentCommand = cmd; }
    public Command getCommand() { return currentCommand; }

    private void handleLevelUpEffect(EffectManager effects, ParticleManager particles) {
        if (effects != null) {
            effects.addText("✨ NÍVEL UP! [" + level + "]", (float)positionX, (float)positionY - 100, Color.YELLOW);
        }
        if (particles != null) {
            particles.addExplosion(positionX, positionY, Color.WHITE, 40);
            particles.addExplosion(positionX, positionY, Color.YELLOW, 30);
        }
    }

    public void draw(Canvas canvas) {
        // Só atualiza o frame da animação se estiver se movendo
        if (isMoving) {
            long now = System.currentTimeMillis();
            if (now - lastFrameTime > ANIMATION_SPEED) {
                frameIndex = (frameIndex + 1) % 3;
                lastFrameTime = now;
            }
        } else {
            frameIndex = 1; // Frame parado (geralmente o do meio na folha 3x3)
        }

        String drawSpecies = species;
        int drawFrame = frameIndex;
        long nowD = System.currentTimeMillis();

        // Habilidade nova lançada: começa a animação de ataque
        if (skillCastEndTime != seenSkillEnd) { seenSkillEnd = skillCastEndTime; if (skillCastEndTime > nowD) atkAnimStart = nowD; }
        boolean attacking = nowD - atkAnimStart < ATTACK_ANIM_MS;
        float lungeX = 0, lungeY = 0;
        if (attacking) {
            Enemy t = targetEnemy;
            if (t != null) {
                double ad = Math.max(1, Math.hypot(t.getPositionX() - positionX, t.getPositionY() - positionY));
                atkDirX = (t.getPositionX() - positionX) / ad; atkDirY = (t.getPositionY() - positionY) / ad;
            }
            updateDirection(atkDirX, atkDirY);                                  // vira para o lado em que ataca
            drawFrame = (int) ((nowD / 110) % 3);                                // os frames giram rápido durante o ataque
            double p01 = (nowD - atkAnimStart) / (double) ATTACK_ANIM_MS;
            float amp = (float) (30 * Math.sin(Math.PI * Math.min(1.0, p01)));   // avança e volta
            lungeX = (float) (atkDirX * amp); lungeY = (float) (atkDirY * amp);
        }

        if (nowD < skillCastEndTime && currentCastingSpecies != null) drawSpecies = currentCastingSpecies;

        Bitmap frame = SpriteManager.getInstance().getUnitSprite(drawSpecies, direction, drawFrame, isShiny);
        
        Paint p = new Paint();
        p.setColor(Color.argb(80, 0, 0, 0));
        float shw = 25f * (float) SpeciesSize.scale(species);
        canvas.drawOval((float)positionX - shw, (float)positionY + 10, (float)positionX + shw, (float)positionY + 25, p);

        if (frame != null) {
            int r = 56; // Proporcional ao player (maior que antes)
            com.example.pokeworld.managers.SkinManager.Skin skin = com.example.pokeworld.ui.handlers.SkinFx.equipped(com.example.pokeworld.managers.SkinManager.Kind.POKEMON);
            Paint sp = null;
            if (skin != null) { sp = new Paint(); com.example.pokeworld.ui.handlers.SkinFx.apply(sp, skin); com.example.pokeworld.ui.handlers.SkinFx.aura(canvas, (float)positionX, (float)positionY + 8, 80, skin); }
            android.graphics.Rect box = SpeciesSize.box(positionX, positionY, r, species);   // tamanho pela altura real da espécie
            box.offset((int) lungeX, (int) lungeY);
            canvas.drawBitmap(frame, null, box, sp);
            
            // Desenha Nome com cor do Gênero
            drawName(canvas);
            
            // Barra de Vida e XP
            drawHealthBar(canvas);
            status.draw(canvas, positionX, positionY, 45);

            // Desenha Emote
            if (!currentEmote.isEmpty()) {
                p.setTextSize(40);
                p.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(currentEmote, (float)positionX, (float)positionY - 120, p);
            }
        }
    }

    /** Altura (negativa) do nome e da barra de vida em relação aos pés: acompanha o tamanho do Pokémon. */
    private double labelOff() { return Math.min(-80, 56 - 112 * SpeciesSize.scale(species) - 24); }

    private void drawName(Canvas canvas) {
        Paint p = new Paint();
        p.setTextSize(22);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        
        int nameColor = gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
        if (isShiny) nameColor = Color.rgb(255, 215, 0);
        p.setColor(nameColor);
        
        String genderIcon = gender == 0 ? "♂" : "♀";
        canvas.drawText(species + " " + genderIcon + " [Nv." + level + "]", (float)positionX, (float)(positionY + labelOff()), p);
    }

    private void drawHealthBar(Canvas canvas) {
        Paint p = new Paint();
        float bw = 80f, bh = 8f;
        float bx = (float)positionX - bw/2, by = (float)(positionY + labelOff() + 5);
        
        // Fundo
        p.setColor(Color.BLACK); 
        canvas.drawRect(bx, by, bx + bw, by + bh, p);
        
        // Cor dinâmica da vida
        float healthPerc = (float) hp / maxHp;
        int healthColor;
        if (healthPerc > 0.6) healthColor = Color.GREEN;
        else if (healthPerc > 0.3) healthColor = Color.YELLOW;
        else healthColor = Color.RED;
        
        p.setColor(healthColor);
        canvas.drawRect(bx, by, bx + (bw * healthPerc), by + bh, p);
        
        // Barra de XP (Mini) abaixo da vida
        p.setColor(Color.DKGRAY);
        canvas.drawRect(bx, by + 10, bx + bw, by + 14, p);
        p.setColor(Color.CYAN);
        float xpPerc = (float)exp / Progression.expToNext(level);
        canvas.drawRect(bx, by + 10, bx + (bw * xpPerc), by + 14, p);
    }

    public void gainExp(int amount) {
        this.exp += (int) Math.round(amount * (1.0 + talent("Estudioso") + (team != null ? team.xp : 0) + com.example.pokeworld.managers.EventCalendar.xpBonus() + com.example.pokeworld.managers.PointsManager.vipXpBonus()));
        int nextLevelExp = Progression.expToNext(level);
        if (this.exp >= nextLevelExp) {
            this.exp -= nextLevelExp;
            this.level++;
            this.leveledUp = true;
            com.example.pokeworld.managers.SoundManager.play("levelup");
            checkEvolution(); 
            setSpeciesStats(this.species);
            
            // Auto-salva ao subir de nível
            if (context instanceof MainActivity) {
                ((MainActivity)context).saveCurrentGame();
            }
        }
    }

    /**
     * Evolução por nível NÃO é automática: ao atingir o nível o jogo só avisa. Quem decide é o jogador,
     * no botão EVOLUIR da ficha do Pokémon (tela do Time) — ou pode seguir com ele na forma atual.
     */
    private void checkEvolution() {
        EvolutionTable.Evo evo = EvolutionTable.byLevel(species, level);
        if (evo != null && context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            if (activity.gameView != null && activity.gameView.ui != null)
                activity.gameView.ui.showMessage("🌟 " + species + " já pode evoluir para " + evo.to + "! Abra o TIME e toque em EVOLUIR (ou siga como está).");
        }
    }

    /** Evolui o companheiro ativo para a nova espécie, toca a cena de evolução e sincroniza Time/banco. */
    public void evolveInto(String nextS) {
        String old = this.species;
        com.example.pokeworld.managers.SoundManager.play("evolve");
        setSpecies(nextS);
        if (context instanceof MainActivity) {
            MainActivity activity = (MainActivity) context;
            if (activity.gameView != null) {
                com.example.pokeworld.managers.PartyManager.onActiveEvolved(activity.gameView, old, nextS);
                com.example.pokeworld.ui.handlers.EvolutionUI evoUI = (com.example.pokeworld.ui.handlers.EvolutionUI) activity.gameView.ui.getHandler(com.example.pokeworld.ui.UIManager.UIState.EVOLVING);
                if (evoUI != null) {
                    evoUI.start(old, nextS);
                    activity.gameView.ui.state = com.example.pokeworld.ui.UIManager.UIState.EVOLVING;
                }
            }
        }
    }

    public void addForgeAttackBonus(int amount) {
        this.forgeAttackBonus += amount;
        resetToBaseStats();
    }

    private void handleAura(List<Enemy> enemies, EffectManager effects, ParticleManager particles) {
        long now = System.currentTimeMillis();
        if (now - lastAuraTick > AURA_INTERVAL) {
            if (type1 == ElementalType.FIRE || type2 == ElementalType.FIRE) {
                for (Enemy e : enemies) {
                    double dist = Math.sqrt(Math.pow(e.getPositionX() - positionX, 2) + Math.pow(e.getPositionY() - positionY, 2));
                    if (dist < 200 && e.isAlive()) { e.takeDamage(attackPower / 4); effects.addText("BURN!", (float)e.getPositionX(), (float)e.getPositionY(), Color.RED); }
                }
            }
            lastAuraTick = now;
        }
    }

    private void handleCombat(ParticleManager particles, EffectManager effects, WorldManager world) {
        double dx = targetEnemy.getPositionX() - positionX, dy = targetEnemy.getPositionY() - positionY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        TileMap tileMap = world.tileMap;
        if (dist > ATTACK_RANGE) {
            double vx = (dx / dist) * ATTACK_SPEED;
            double vy = (dy / dist) * ATTACK_SPEED;
            
            boolean isAquatic = type1 == ElementalType.WATER || type2 == ElementalType.WATER;
            boolean isFireResistant = type1 == ElementalType.FIRE || type2 == ElementalType.FIRE || type1 == ElementalType.DRAGON || type2 == ElementalType.DRAGON;
            if (tileMap.isWalkable(positionX + vx, positionY + vy, true, true, isAquatic, isFireResistant) && !world.isCollidingWithBuilding(positionX + vx, positionY + vy)) {
                positionX += vx; positionY += vy;
                updateDirection(vx, vy);
                isMoving = true;
            } else {
                isMoving = false;
            }
        } else {
            isMoving = false;
            updateDirection(dx, dy);                                  // encara o alvo
            long now = System.currentTimeMillis();
            if (now - lastAttackTime > ATTACK_COOLDOWN) {
                int damage = attackPower;
                double multiplier = ElementalType.getMultiplier(type1, targetEnemy.getType(), type2);
                int finalDamage = (int)(damage * multiplier);
                targetEnemy.takeDamage(finalDamage); addRage(5); applyVampirism(finalDamage);
                com.example.pokeworld.managers.SoundManager.play("hit");
                lastDamageDealt = finalDamage; lastTargetHit = targetEnemy; lastAttackTime = now; atkAnimStart = now;
                if (effects != null) {
                    effects.addText("-" + finalDamage, targetEnemy.getPositionX(), targetEnemy.getPositionY() - 20, Color.WHITE);
                    if (multiplier > 1.0) effects.addText("🔥 SUPER EFETIVO!", (float)targetEnemy.getPositionX(), (float)targetEnemy.getPositionY() - 50, Color.YELLOW);
                    else if (multiplier < 1.0) effects.addText("💤 Pouco efetivo...", (float)targetEnemy.getPositionX(), (float)targetEnemy.getPositionY() - 50, Color.GRAY);
                }
            }
        }
    }

    public void useUltimate(EffectManager effects, ParticleManager particles) {
        if (rage < MAX_RAGE || targetEnemy == null) return;
        
        // Se tiver Mega Pedra, ativa Mega Evolução em vez do golpe comum
        if (!isMega && context instanceof MainActivity) {
            MainActivity act = (MainActivity) context;
            if (act.gameView.player.getInventory().containsKey("Mega Pedra")) {
                activateMega(effects, particles);
                act.gameView.player.useItem("Mega Pedra");
                return;
            }
        }

        rage = 0;
        int dmg = attackPower * 3;
        double multiplier = ElementalType.getMultiplier(type1, targetEnemy.getType(), type2);
        int finalDamage = (int)(dmg * multiplier);
        targetEnemy.takeDamage(finalDamage);
        applyVampirism(finalDamage);
        String msg = "🌟 ULTIMATE: " + finalDamage;
        if (multiplier > 1.0) msg = "🔥 SUPER EFETIVO! " + msg;
        else if (multiplier < 1.0) msg = "💤 Pouco efetivo... " + msg;
        effects.addText(msg, (float)targetEnemy.getPositionX(), (float)targetEnemy.getPositionY(), Color.YELLOW);
        particles.addExplosion(targetEnemy.getPositionX(), targetEnemy.getPositionY(), Color.WHITE, 60);
    }

    public boolean useSkill(int index, EffectManager effects, ParticleManager particles, String weather, WorldManager world) {
        if (index >= 0 && index < skills.size() && targetEnemy != null && targetEnemy.isAlive()) {
            Skill skill = skills.get(index);

            // Verificação de distância para Jato d'Água
            if (skill.getName().equalsIgnoreCase("Jato d'Água")) {
                double dist = Math.sqrt(Math.pow(targetEnemy.getPositionX() - positionX, 2) + Math.pow(targetEnemy.getPositionY() - positionY, 2));
                if (dist > 500) {
                    pendingSkillIndex = index;
                    return false;
                }
            }

            int damage = skill.use();
            if (damage > 0) {
                pendingSkillIndex = -1; // Limpa se estava pendente
                double multiplier = ElementalType.getMultiplier(skill.getType(), targetEnemy.getType(), null);
                
                // Modificadores Climáticos
                if (weather.equals("RAIN")) {
                    if (skill.getType() == ElementalType.WATER || skill.getType() == ElementalType.ELECTRIC) multiplier *= 1.3;
                    if (skill.getType() == ElementalType.FIRE) multiplier *= 0.7;
                } else if (weather.equals("SANDSTORM")) {
                    if (skill.getType() == ElementalType.ROCK) multiplier *= 1.3;
                }

                // Sistema de Crítico
                boolean isCritical = Math.random() < 0.15 + talent("Critico") + (team != null ? team.crit : 0);
                if (isCritical) multiplier *= 2.0;

                int totalDamage = (int)((damage + (attackPower * 0.5f)) * multiplier);
                applyVampirism(totalDamage);

                // Suporte para Projéteis e Animações de Ataque (Tamanho Fixo Estilo Tibia)
                float fixedEffectSize = (float)TileMap.TILE_SIZE * 1.5f;

                if (skill.getName().equalsIgnoreCase("Chicote de Vinha")) {
                    if (effects != null) {
                        float beamHeight = (float)TileMap.TILE_SIZE * 0.8f;
                        effects.addDirectionalAnimation("spells_chicotedevinha", positionX, positionY, targetEnemy.getPositionX(), targetEnemy.getPositionY(), beamHeight, 1200);
                        
                        targetEnemy.takeDamage(totalDamage);
                        effects.addText("-" + totalDamage, targetEnemy.getPositionX(), targetEnemy.getPositionY() - 20, Color.WHITE);
                        skillCastEndTime = System.currentTimeMillis() + 1200;
                    }
                } else if (skill.getName().equalsIgnoreCase("Pétalas Dançantes")) {
                    if (effects != null) {
                        float fixedSize = TileMap.TILE_SIZE * 3.5f; 
                        effects.addAnimation("spells_petaldance", targetEnemy.getPositionX(), targetEnemy.getPositionY(), fixedSize, 1000);
                        double aoeRadius = 250.0;
                        for (Enemy e : world.enemies) {
                            if (e.isAlive()) {
                                double d = Math.sqrt(Math.pow(e.getPositionX() - targetEnemy.getPositionX(), 2) + Math.pow(e.getPositionY() - targetEnemy.getPositionY(), 2));
                                if (d <= aoeRadius) {
                                    e.takeDamage(totalDamage);
                                    effects.addText("-" + totalDamage, e.getPositionX(), e.getPositionY() - 20, Color.WHITE);
                                }
                            }
                        }
                        skillCastEndTime = System.currentTimeMillis() + 1000;
                    }
                } else if (skill.getName().equalsIgnoreCase("Jato d'Água")) {
                    if (effects != null) {
                        float beamWidth = (float)TileMap.TILE_SIZE * 0.8f;
                        effects.addBeam("spells_jatodeagua", positionX, positionY, targetEnemy.getPositionX(), targetEnemy.getPositionY(), beamWidth, 600);
                        targetEnemy.takeDamage(totalDamage);
                        effects.addText("-" + totalDamage, targetEnemy.getPositionX(), targetEnemy.getPositionY() - 20, Color.WHITE);
                        skillCastEndTime = System.currentTimeMillis() + 600;
                        currentCastingSpecies = (species.equalsIgnoreCase("Squirtle")) ? (isShiny ? "squirtle_shiny_jatodeagua" : "squirtle_jatodeagua") : null;
                    }
                } else {
                    // Golpe genérico: grade de quadrados (PokeTibia), explosão em área ou projétil direto
                    double tx = targetEnemy.getPositionX(), ty = targetEnemy.getPositionY();
                    int tile = TileMap.TILE_SIZE;
                    if (skill.getPattern() != SkillPattern.NONE && world != null) {
                        java.util.List<double[]> cells = skill.getPattern().cells(tx, ty, positionX, positionY, tile);
                        if (effects != null) effects.addTilePattern(skill.getType(), cells, 1500);
                        for (Enemy e : world.enemies) {
                            if (!e.isAlive() || e.isPet) continue;   // não acerta os defensores da base
                            for (double[] c : cells) {
                                if (Math.hypot(e.getPositionX() - c[0], e.getPositionY() - c[1]) <= tile * 0.75 + e.getRadius() * 0.5) {
                                    e.takeDamage(totalDamage);
                                    if (effects != null) effects.addText("-" + totalDamage, e.getPositionX(), e.getPositionY() - 20, Color.WHITE);
                                    applySkillStatus(skill, e, effects);
                                    break;
                                }
                            }
                        }
                        skillCastEndTime = System.currentTimeMillis() + 700;
                    } else if (skill.isArea() && world != null) {
                        double r = skill.getRadius();
                        if (effects != null) effects.addAreaAttack(skill.getType(), tx, ty, (float) (r * 0.9), 900);
                        for (Enemy e : world.enemies) {
                            if (!e.isAlive() || e.isPet) continue;
                            if (Math.hypot(e.getPositionX() - tx, e.getPositionY() - ty) <= r) {
                                e.takeDamage(totalDamage);
                                if (effects != null) effects.addText("-" + totalDamage, e.getPositionX(), e.getPositionY() - 20, Color.WHITE);
                                applySkillStatus(skill, e, effects);
                            }
                        }
                        skillCastEndTime = System.currentTimeMillis() + 700;
                    } else if (effects != null) {
                        effects.addDirectAttack(skill.getType(), positionX, positionY, tx, ty, (float) tile * 0.9f, totalDamage, targetEnemy);
                        effects.addText("-" + totalDamage, tx, ty - 20, Color.WHITE);
                        applySkillStatus(skill, targetEnemy, effects);
                        skillCastEndTime = System.currentTimeMillis() + 500;
                        currentCastingSpecies = (skill.getType() == ElementalType.FIRE && species.equalsIgnoreCase("Charmander")) ? species : null;
                    } else {
                        targetEnemy.takeDamage(totalDamage);
                    }
                }
                com.example.pokeworld.managers.SoundManager.play(skill.getType().name().toLowerCase());
                com.example.pokeworld.managers.VibrationManager.tap(isCritical ? 55 : 22);
                if (isCritical) com.example.pokeworld.managers.SoundManager.play("crit");

                addRage(10);
                lastDamageDealt = totalDamage;
                lastTargetHit = targetEnemy;

                if (effects != null) {
                    String msg = skill.getName() + ": " + totalDamage;
                    if (isCritical) msg = "⭐ CRÍTICO! " + msg;
                    else if (multiplier > 1.0) msg = "🔥 SUPER EFETIVO! " + msg;
                    else if (multiplier < 1.0) msg = "💤 Pouco efetivo... " + msg;
                    effects.addText(msg, (float)targetEnemy.getPositionX(), (float)targetEnemy.getPositionY(), isCritical ? Color.YELLOW : skill.getColor());
                }
                if (particles != null) {
                    particles.addExplosion(targetEnemy.getPositionX(), targetEnemy.getPositionY(), skill.getColor(), isCritical ? 50 : 25);
                }
                return true;
            }
        }
        return false;
    }

    /** Sorteia e aplica o estado do golpe (queimar, envenenar...) no inimigo atingido. */
    private void applySkillStatus(Skill skill, Enemy e, EffectManager effects) {
        Status st = skill.getStatus();
        if (st == null || e == null || !e.isAlive()) return;
        if (Math.random() > skill.getStatusChance() + talent("Status") + (team != null ? team.status : 0)) return;
        long dur = st.durationMs;
        if (e.isBoss && st.stuns()) dur = (long) (dur * 0.4);   // chefes resistem a sono e gelo
        if (e.status.apply(st, dur, e.getType()) && effects != null) {
            effects.addText(st.icon + " " + st.label + "!", e.getPositionX(), e.getPositionY() - 55, st.color);
            com.example.pokeworld.managers.SoundManager.play("status");
        }
    }

    private void handlePendingSkill(EffectManager effects, ParticleManager particles, WorldManager world, String weather) {
        if (targetEnemy == null || !targetEnemy.isAlive()) {
            pendingSkillIndex = -1;
            return;
        }

        double dx = targetEnemy.getPositionX() - positionX;
        double dy = targetEnemy.getPositionY() - positionY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        Skill skill = skills.get(pendingSkillIndex);
        double range = skill.getName().equalsIgnoreCase("Jato d'Água") ? 500.0 : ATTACK_RANGE;

        if (dist > range) {
            double vx = (dx / dist) * ATTACK_SPEED;
            double vy = (dy / dist) * ATTACK_SPEED;
            
            boolean isAquatic = type1 == ElementalType.WATER || type2 == ElementalType.WATER;
            boolean isFireResistant = type1 == ElementalType.FIRE || type2 == ElementalType.FIRE || type1 == ElementalType.DRAGON || type2 == ElementalType.DRAGON;
            if (world.tileMap.isWalkable(positionX + vx, positionY + vy, true, true, isAquatic, isFireResistant) && !world.isCollidingWithBuilding(positionX + vx, positionY + vy)) {
                positionX += vx; positionY += vy;
                updateDirection(vx, vy);
                isMoving = true;
            } else {
                isMoving = false;
            }
        } else {
            useSkill(pendingSkillIndex, effects, particles, weather, world);
        }
    }

    public void useSkill(int index, WorldManager world) {
        useSkill(index, null, null, "NONE", world);
    }

    public boolean canUseUltimate() { return rage >= MAX_RAGE && targetEnemy != null; }

    public void healFull() { this.hp = this.maxHp; rage = 0; status.clear(); }

    private void activateMega(EffectManager effects, ParticleManager particles) {
        this.isMega = true;
        this.megaEndTime = System.currentTimeMillis() + 60000; // 1 minuto
        this.originalSpecies = this.species;
        
        String megaName = "Mega " + species;
        
        // Regras especiais de Mega Evolução
        if (species.equalsIgnoreCase("Charizard")) {
            megaName = Math.random() < 0.5 ? "Mega Charizard X" : "Mega Charizard Y";
        } else if (species.equalsIgnoreCase("Venusaur") || species.equalsIgnoreCase("Venosaur")) {
            megaName = "Mega Venusaur";
        }
        
        setSpecies(megaName);
        this.attackBuffMult = 2.0f; // Dobro de dano
        resetToBaseStats();
        
        if (effects != null) effects.addText("🧬 " + megaName.toUpperCase() + "!", (float)positionX, (float)positionY - 100, Color.MAGENTA);
        if (particles != null) particles.addExplosion(positionX, positionY, Color.MAGENTA, 80);
    }

    private void checkMegaTimeout() {
        if (isMega && System.currentTimeMillis() > megaEndTime) {
            isMega = false;
            setSpecies(originalSpecies);
            this.attackBuffMult = 1.0f;
            resetToBaseStats();
        }
    }

    private void followPlayer(Player player, WorldManager world) {
        double dx = player.getPositionX() - positionX;
        double dy = player.getPositionY() - positionY;
        double dist = Math.sqrt(dx * dx + dy * dy);
        TileMap tileMap = world.tileMap;
        if (dist > FOLLOW_DIST) {
            double vx = dx * FOLLOW_SPEED;
            double vy = dy * FOLLOW_SPEED;
            
            boolean isAquatic = type1 == ElementalType.WATER || type2 == ElementalType.WATER;
            boolean isFireResistant = type1 == ElementalType.FIRE || type2 == ElementalType.FIRE || type1 == ElementalType.DRAGON || type2 == ElementalType.DRAGON;
            if (tileMap.isWalkable(positionX + vx, positionY + vy, true, true, isAquatic, isFireResistant) && !world.isCollidingWithBuilding(positionX + vx, positionY + vy)) {
                positionX += vx; positionY += vy;
                updateDirection(vx, vy);
                isMoving = true;
            } else {
                isMoving = false;
            }
        } else {
            isMoving = false;
        }
    }

    private void updateDirection(double vx, double vy) {
        if (Math.abs(vx) > Math.abs(vy)) direction = (vx > 0) ? 3 : 1;
        else direction = (vy > 0) ? 0 : 2;
    }

    public String getPassiveDescription() {
        String s = species.toUpperCase();
        if (s.contains("PIKACHU") || s.contains("RAICHU")) return "⚡ Estática: +15% Velocidade Jogador";
        if (s.contains("SQUIRTLE") || s.contains("BLASTOISE")) return "🛡️ Casco: -10% Dano Recebido";
        if (s.contains("CHARMANDER") || s.contains("CHARIZARD")) return "🔥 Brasa: +10% Dano de Fogo";
        if (s.contains("BULBASAUR") || s.contains("VENUSAUR")) return "🌿 Síntese: Regenera Vida do Jogador";
        return "✨ Companheirismo: Ganha Ouro ao andar";
    }

    public double getPlayerSpeedMultiplier() {
        String s = species.toUpperCase();
        double m = (s.contains("PIKACHU") || s.contains("RAICHU")) ? 1.15 : 1.0;
        if (equippedRune != null && equippedRune.getEffect() == Rune.Effect.SPEED) m *= equippedRune.getValue();
        return m;
    }

    /** Runa de Sangue: cura o companheiro com uma parte do dano causado. */
    private void applyVampirism(int dealt) {
        if (equippedRune == null || equippedRune.getEffect() != Rune.Effect.VAMPIRISM || dealt <= 0) return;
        heal(Math.max(1, (int)(dealt * equippedRune.getValue())));
    }

    /** Runa da Sorte: multiplicador para ouro e chances de drop (1.0 sem a runa). */
    public float getLuckMultiplier() {
        float base = (equippedRune != null && equippedRune.getEffect() == Rune.Effect.LUCK) ? equippedRune.getValue() : 1.0f;
        return base * (1f + (team != null ? team.luck : 0f));
    }

    public void setPosition(double x, double y) { this.positionX = x; this.positionY = y; }
    public double getPositionX() { return positionX; }
    public double getPositionY() { return positionY; }
    public void setRarity(Rarity r) { this.rarity = r; resetToBaseStats(); }
    public Rarity getRarity() { return rarity; }
    public int getBoost() { return boost; }
    public TeamBonus getTeamBonus() { return team; }

    /** Aplica boost (+N) e o apoio do time. Só recalcula quando algo mudou, mantendo a proporção de vida. */
    public void setSupport(int newBoost, TeamBonus tb) {
        String sigNew = tb == null ? "" : tb.signature();
        String sigOld = team == null ? "" : team.signature();
        if (newBoost == boost && sigNew.equals(sigOld)) return;
        float ratio = maxHp > 0 ? hp / (float) maxHp : 1f;
        this.boost = newBoost;
        this.team = tb;
        resetToBaseStats();
        if (hp > 0) hp = Math.max(1, Math.min(maxHp, Math.round(ratio * maxHp)));
    }

    public boolean isAquatic() {
        return type1 == ElementalType.WATER || (type2 != null && type2 == ElementalType.WATER);
    }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
    public void setHp(int hp) { this.hp = Math.max(0, Math.min(maxHp, hp)); }
    public float getRage() { return rage; }
    public float getMaxRage() { return MAX_RAGE; }
    public String getSpecies() { return species; }
    public void setSpecies(String s) { this.species = s; setSpeciesStats(s); }
    public int getLevel() { return level; }
    public int getExp() { return exp; }
    public int getForgeAttackBonus() { return forgeAttackBonus; }
    public List<Skill> getSkills() { return skills; }
    public Equipment getWeapon() { return weapon; }
    public void setWeapon(Equipment w) { this.weapon = w; resetToBaseStats(); }
    public Equipment getArmor() { return armor; }
    public void setArmor(Equipment a) { this.armor = a; resetToBaseStats(); }
    public Rune getRune() { return equippedRune; }
    public void setRune(Rune r) { this.equippedRune = r; resetToBaseStats(); }
    public int getFriendship() { return friendship; }
    public String getPersonality() { return personality; }
    public int getGender() { return gender; }
    public void setGender(int g) { this.gender = g; resetToBaseStats(); }
    public ElementalType getType1() { return type1; }
    public ElementalType getType2() { return type2; }
    public FieldAbility getFieldAbility() { return fieldAbility; }
    public int getDirection() { return direction; }
    public int getFrameIndex() { return frameIndex; }
    public boolean isShiny() { return isShiny; }
    public double getRadius() { return radius; }
    public int getAttackPower() { return attackPower; }
    public void setTargetEnemy(Enemy e) { this.targetEnemy = e; }
    public void setAttackPower(int attackPower) { this.attackPower = attackPower; }
    public void heal(int amount) { this.hp = Math.min(maxHp, this.hp + amount); }
    public void setShield(long durationMs) { this.hasShield = true; this.shieldEndTime = System.currentTimeMillis() + durationMs; }
}
