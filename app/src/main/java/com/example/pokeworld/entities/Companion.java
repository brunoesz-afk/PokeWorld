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
    private long lastAttackTime = 0;
    private static final long ATTACK_COOLDOWN = 1000;
    private List<Skill> skills = new ArrayList<>();
    
    private ElementalType type1 = ElementalType.NORMAL;
    private ElementalType type2 = null; 
    private FieldAbility fieldAbility = FieldAbility.NONE;
    
    private boolean isShiny = false;
    private Rarity rarity = Rarity.COMMON;
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
        if (hasShield && System.currentTimeMillis() < shieldEndTime) return;
        if (friendship >= 50 && Math.random() < 0.1) return;
        float resistance = 0;
        if (equippedRune != null && equippedRune.getEffect() == Rune.Effect.RESISTANCE) resistance = equippedRune.getValue();
        resistance = (float) Math.min(0.8, resistance + talent("Resistencia"));
        int finalDamage = (int)(amount * (1.0f - resistance));
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
            if (level >= 25) skills.add(new Skill("Trovão", 100, 8000, Color.YELLOW, ElementalType.ELECTRIC));
        } else if (s.contains("CHARMANDER") || s.contains("CHARMELEON") || s.contains("CHARIZARD")) {
            this.type1 = ElementalType.FIRE;
            this.fieldAbility = s.contains("CHARIZARD") ? FieldAbility.FLY : FieldAbility.CUT;
            this.baseMaxHp = (s.contains("CHARIZARD") ? 900 : s.contains("CHARMELEON") ? 450 : 180) + (level * 30);
            this.baseAttackPower = (s.contains("CHARIZARD") ? 180 : s.contains("CHARMELEON") ? 100 : 45) + (level * 12);
            if (level >= 5) skills.add(new Skill("Brasa", 45, 3000, Color.RED, ElementalType.FIRE));
            if (level >= 18) skills.add(new Skill("Fúria do Dragão", 65, 5000, Color.rgb(255, 69, 0), ElementalType.DRAGON));
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
            skills.add(new Skill("Explosão Solar", 130, 4000, Color.YELLOW, ElementalType.FIRE));
        } else if (s.contains("SQUIRTLE") || s.contains("WARTORTLE") || s.contains("BLASTOISE")) {
            this.type1 = ElementalType.WATER;
            this.fieldAbility = FieldAbility.SURF;
            this.baseMaxHp = (s.contains("BLASTOISE") ? 1100 : s.contains("WARTORTLE") ? 550 : 200) + (level * 35);
            this.baseAttackPower = (s.contains("BLASTOISE") ? 150 : s.contains("WARTORTLE") ? 85 : 35) + (level * 10);
            if (level >= 5) skills.add(new Skill("Jato d'Água", 35, 2500, Color.BLUE, ElementalType.WATER));
            if (level >= 20) skills.add(new Skill("Raio de Gelo", 60, 4500, Color.CYAN, ElementalType.ICE));
            if (level >= 36) skills.add(new Skill("Hidro Canhão", 100, 9000, Color.BLUE, ElementalType.WATER));
        } else if (s.contains("BULBASAUR") || s.contains("IVYSAUR") || s.contains("VENUSAUR")) {
            this.type1 = ElementalType.PLANT;
            this.fieldAbility = FieldAbility.CUT;
            this.baseMaxHp = (s.contains("VENUSAUR") ? 1200 : s.contains("IVYSAUR") ? 600 : 220) + (level * 40); 
            this.baseAttackPower = (s.contains("VENUSAUR") ? 160 : s.contains("IVYSAUR") ? 90 : 40) + (level * 11);
            skills.add(new Skill("Chicote de Vinha", 35, 2500, Color.GREEN, ElementalType.PLANT));
            if (level >= 8) skills.add(new Skill("Folha Navalha", 45, 3500, Color.GREEN, ElementalType.PLANT));
            if (level >= 22) skills.add(new Skill("Pó do Sono", 20, 6000, Color.MAGENTA, ElementalType.NORMAL));
            if (level >= 28) skills.add(new Skill("Pétalas Dançantes", 80, 5000, Color.rgb(255, 105, 180), ElementalType.PLANT));
            if (level >= 32) skills.add(new Skill("Solar Beam", 120, 10000, Color.WHITE, ElementalType.PLANT));
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
            skills.add(new Skill("Hyper Beam", 120, 10000, Color.WHITE, ElementalType.DRAGON));
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

        if (isShiny) { this.baseMaxHp *= 1.2; this.baseAttackPower *= 1.2; }
        if (personality.equals("Brave")) this.baseAttackPower *= 1.1;
        if (personality.equals("Hardy")) this.maxHp *= 1.1;

        resetToBaseStats();
        this.hp = maxHp;
    }

    public void resetToBaseStats() {
        this.maxHp = baseMaxHp;
        
        // Bônus de Raridade (10% por nível de raridade extra)
        double rarityMult = 1.0 + (rarity.ordinal() * 0.1);
        
        // Bônus de Gênero (Macho: +10% Dano, Fêmea: +10% Vida)
        double atkMult = (gender == 0 ? 1.1 : 1.0) * (1.0 + talent("Guerreiro"));
        double hpMult = (gender == 1 ? 1.1 : 1.0) * (1.0 + talent("Vitalidade"));

        this.attackPower = (int)((baseAttackPower + forgeAttackBonus) * attackBuffMult * rarityMult * atkMult);
        this.maxHp = (int)(this.maxHp * rarityMult * hpMult);

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
    private String currentCastingSpecies = null;

    public void update(Player player, List<Enemy> enemies, EffectManager effects, ParticleManager particles, WorldManager world, String weather) {
        lastDamageDealt = 0; lastTargetHit = null;
        checkMegaTimeout();
        
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

        // Regeneração por Amizade (Fora de combate)
        if (targetEnemy == null && hp > 0 && hp < maxHp) {
            long now = System.currentTimeMillis();
            if (now - lastHealTick > 5000) { // A cada 5s
                int heal = 1 + (friendship / 50);
                hp = Math.min(maxHp, hp + heal);
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
                // Procura o inimigo mais próximo automaticamente
                targetEnemy = findNearestEnemy(enemies);
            }
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

        if (System.currentTimeMillis() < skillCastEndTime && currentCastingSpecies != null) {
            drawSpecies = currentCastingSpecies;
            // Durante o ataque, usamos o frame 2 (geralmente uma pose de ação/ataque em folhas 3x3)
            drawFrame = 2; 
        }

        Bitmap frame = SpriteManager.getInstance().getUnitSprite(drawSpecies, direction, drawFrame, isShiny);
        
        Paint p = new Paint();
        p.setColor(Color.argb(80, 0, 0, 0));
        canvas.drawOval((float)positionX - 25, (float)positionY + 10, (float)positionX + 25, (float)positionY + 25, p);

        if (frame != null) {
            int r = 56; // Proporcional ao player (maior que antes)
            canvas.drawBitmap(frame, null, new Rect((int)positionX - r, (int)positionY - r, (int)positionX + r, (int)positionY + r), null);
            
            // Desenha Nome com cor do Gênero
            drawName(canvas);
            
            // Barra de Vida e XP
            drawHealthBar(canvas);

            // Desenha Emote
            if (!currentEmote.isEmpty()) {
                p.setTextSize(40);
                p.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(currentEmote, (float)positionX, (float)positionY - 120, p);
            }
        }
    }

    private void drawName(Canvas canvas) {
        Paint p = new Paint();
        p.setTextSize(22);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        
        int nameColor = gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
        if (isShiny) nameColor = Color.rgb(255, 215, 0);
        p.setColor(nameColor);
        
        String genderIcon = gender == 0 ? "♂" : "♀";
        canvas.drawText(species + " " + genderIcon + " [Nv." + level + "]", (float)positionX, (float)positionY - 80, p);
    }

    private void drawHealthBar(Canvas canvas) {
        Paint p = new Paint();
        float bw = 80f, bh = 8f;
        float bx = (float)positionX - bw/2, by = (float)positionY - 75f;
        
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
        float xpPerc = (float)exp / (level * 100);
        canvas.drawRect(bx, by + 10, bx + (bw * xpPerc), by + 14, p);
    }

    public void gainExp(int amount) {
        this.exp += (int) Math.round(amount * (1.0 + talent("Estudioso")));
        int nextLevelExp = level * 100;
        if (this.exp >= nextLevelExp) {
            this.exp -= nextLevelExp;
            this.level++;
            this.leveledUp = true;
            checkEvolution(); 
            setSpeciesStats(this.species);
            
            // Auto-salva ao subir de nível
            if (context instanceof MainActivity) {
                ((MainActivity)context).saveCurrentGame();
            }
        }
    }

    private void checkEvolution() {
        EvolutionTable.Evo evo = EvolutionTable.byLevel(species, level);
        if (evo != null) evolveInto(evo.to);
    }

    /** Evolui o companheiro ativo para a nova espécie, toca a cena de evolução e sincroniza Time/banco. */
    public void evolveInto(String nextS) {
        String old = this.species;
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
            long now = System.currentTimeMillis();
            if (now - lastAttackTime > ATTACK_COOLDOWN) {
                int damage = attackPower;
                double multiplier = ElementalType.getMultiplier(type1, targetEnemy.getType(), type2);
                int finalDamage = (int)(damage * multiplier);
                targetEnemy.takeDamage(finalDamage); addRage(5); applyVampirism(finalDamage);
                lastDamageDealt = finalDamage; lastTargetHit = targetEnemy; lastAttackTime = now;
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
                boolean isCritical = Math.random() < 0.15 + talent("Critico");
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
                } else if (skill.getType() == ElementalType.FIRE) {
                    if (effects != null) {
                        effects.addProjectile("bola_de_fogo", positionX, positionY, targetEnemy.getPositionX(), targetEnemy.getPositionY(), fixedEffectSize, totalDamage, targetEnemy);
                        skillCastEndTime = System.currentTimeMillis() + 600;
                        currentCastingSpecies = (species.equalsIgnoreCase("Charmander")) ? species : null;
                        if (Math.random() < 0.2) effects.addText("🔥 QUEIMADURA!", (float)targetEnemy.getPositionX(), (float)targetEnemy.getPositionY(), Color.RED);
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
                } else if (skill.getType() == ElementalType.PLANT) {
                    if (effects != null) {
                        float fixedSize = TileMap.TILE_SIZE * 2.5f; 
                        effects.addAnimation("spells_sementes", positionX, positionY, fixedSize * 0.6f, 800);
                        effects.addAnimation("spells_sementes2", targetEnemy.getPositionX(), targetEnemy.getPositionY(), fixedSize, 800);
                        double aoeRadius = 200.0;
                        for (Enemy e : world.enemies) {
                            if (e.isAlive()) {
                                double d = Math.sqrt(Math.pow(e.getPositionX() - targetEnemy.getPositionX(), 2) + Math.pow(e.getPositionY() - targetEnemy.getPositionY(), 2));
                                if (d <= aoeRadius) {
                                    e.takeDamage(totalDamage);
                                    effects.addText("-" + totalDamage, e.getPositionX(), e.getPositionY() - 20, Color.WHITE);
                                }
                            }
                        }
                        skillCastEndTime = System.currentTimeMillis() + 800;
                    }
                } else {
                    targetEnemy.takeDamage(totalDamage);
                    if (effects != null) effects.addText("-" + totalDamage, targetEnemy.getPositionX(), targetEnemy.getPositionY() - 20, Color.WHITE);
                }

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

    public void healFull() { this.hp = this.maxHp; rage = 0; }

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
        return (equippedRune != null && equippedRune.getEffect() == Rune.Effect.LUCK) ? equippedRune.getValue() : 1.0f;
    }

    public void setPosition(double x, double y) { this.positionX = x; this.positionY = y; }
    public double getPositionX() { return positionX; }
    public double getPositionY() { return positionY; }
    public void setRarity(Rarity r) { this.rarity = r; resetToBaseStats(); }
    public Rarity getRarity() { return rarity; }

    public boolean isAquatic() {
        return type1 == ElementalType.WATER || (type2 != null && type2 == ElementalType.WATER);
    }

    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }
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
