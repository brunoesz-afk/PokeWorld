package com.example.pokeworld.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.pokeworld.managers.ParticleManager;
import com.example.pokeworld.managers.SpriteManager;
import com.example.pokeworld.ui.Joystick;
import com.example.pokeworld.world.TileMap;
import com.example.pokeworld.world.WorldManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Player: Representa o treinador, seus status, inventário e coleção de criaturas.
 */
public class Player {
    // --- ATRIBUTOS DE MOVIMENTO ---
    private double positionX, positionY;
    private static final double SPEED = 6.0;
    private Joystick joystick; 
    
    private int direction = 0; 
    private int frameIndex = 1;
    private long lastFrameTime = 0;
    private static final int ANIMATION_SPEED = 300;
    private boolean isMoving = false;

    private long lastDashTime = 0;
    private static final long DASH_COOLDOWN = 2000;
    private long dashStartTime = 0;
    private static final long DASH_DURATION = 200;
    private boolean isDashing = false;

    // --- STATUS E PROGRESSÃO ---
    private int level = 1;
    private int exp = 0;
    private float stamina = 100f;
    private float maxStamina = 100f;
    private int maxHp = 100;
    private int hp = 100;
    
    private int gold;
    private int gems = 0;
    private String clan = "Nenhum";
    private int talentPoints = 0;
    private Map<String, Integer> talents = new HashMap<>();
    
    private int fishingLevel = 1;
    private int fishingExp = 0;

    // --- COLEÇÃO E MISSÕES ---
    private List<CapturedCreature> creatureCollection;
    private List<Mission> activeMissions = new ArrayList<>();
    private Map<String, Integer> inventory = new HashMap<>();
    private Map<String, Integer> storageItems = new HashMap<>(); // Baú da base

    private int skinColor = Color.WHITE;
    private boolean isMounted = false;

    private float hunger = 100f;
    private float maxHunger = 100f;

    private boolean daycareIntroSeen = false;
    private List<TrainerSkill> trainerSkills = new ArrayList<>();

    public Player(double x, double y, double r, int initialGold, List<CapturedCreature> savedCollection, Map<String, Integer> savedInventory) {
        this.positionX = x;
        this.positionY = y;
        this.gold = initialGold;
        this.creatureCollection = savedCollection != null ? savedCollection : new ArrayList<>();
        if (this.creatureCollection.isEmpty()) {
            this.creatureCollection.add(new CapturedCreature("Pikachu", false, Rarity.COMMON, true));
        }
        
        if (savedInventory != null) this.inventory = savedInventory;

        talents.put("Domador", 0);
        talents.put("Guerreiro", 0);
        talents.put("Explorador", 0);

        initTrainerSkills();
    }

    private void initTrainerSkills() {
        trainerSkills.add(new TrainerSkill("Cura Divina", 40, 15000, Color.rgb(0, 255, 127), "HEAL"));
        trainerSkills.add(new TrainerSkill("Grito de Guerra", 60, 25000, Color.rgb(255, 69, 0), "BUFF"));
        trainerSkills.add(new TrainerSkill("Escudo Arcano", 50, 30000, Color.rgb(30, 144, 255), "SHIELD"));
    }

    // --- CICLO DE UPDATE ---

    public void update(Joystick joystick, WorldManager world, boolean canSurf, boolean canFly, boolean isAquatic, ParticleManager particles, String mountSpecies, double passiveSpeedMult) {
        this.joystick = joystick;
        TileMap tileMap = world.tileMap;
        
        updateStamina();
        double speedMult = calculateSpeed(mountSpecies, passiveSpeedMult);
        handleDash(speedMult, particles);

        if (joystick != null && joystick.getIsPressed()) {
            double vx = joystick.getActuatorX() * SPEED * speedMult;
            double vy = joystick.getActuatorY() * SPEED * speedMult;
            
            isMoving = Math.abs(vx) > 0.1 || Math.abs(vy) > 0.1;

            double nextX = positionX + vx;
            double nextY = positionY + vy;
            
            boolean hasFireSaddle = inventory.containsKey("Sela de Fogo");
            boolean isFireResistant = mountSpecies.equalsIgnoreCase("Charizard") || mountSpecies.equalsIgnoreCase("Lobofogo") || hasFireSaddle;
            if (tileMap.isWalkable(nextX, positionY, canSurf, canFly, isAquatic, isFireResistant) && !world.isCollidingWithBuilding(nextX, positionY)) positionX = nextX;
            if (tileMap.isWalkable(positionX, nextY, canSurf, canFly, isAquatic, isFireResistant) && !world.isCollidingWithBuilding(positionX, nextY)) positionY = nextY;
            
            updateDirection(vx, vy);
        } else {
            isMoving = false;
        }
    }

    private void updateStamina() {
        if (stamina < maxStamina) {
            stamina += (0.2f + (level * 0.01f));
            if (stamina > maxStamina) stamina = maxStamina;
        }
        
        // Fome removida conforme pedido
        hunger = maxHunger;
    }

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp < 0) hp = 0;
    }

    public void drainHunger(float amount) {
        // Mecânica de fome desativada
    }

    private float speedDebuff = 1.0f;
    public void applySpeedDebuff(float mult) { this.speedDebuff = mult; }

    public void eat(int amount) {
        hunger = Math.min(maxHunger, hunger + amount);
        hp = Math.min(maxHp, hp + (amount / 2));
    }

    public float getHunger() { return hunger; }
    public float getMaxHunger() { return maxHunger; }

    private double calculateSpeed(String mount, double passive) {
        int mountLv = inventory.getOrDefault("MountSpeed_Lv", 1);
        double mult = (1.0 + (talents.getOrDefault("Explorador", 0) * 0.1) + (mountLv - 1) * 0.1) * passive * speedDebuff;
        if (isMounted) {
            String m = mount.toUpperCase();
            if (m.contains("CHARIZARD") || m.contains("PIDGEOT") || m.contains("RAIQUAZA")) mult *= 3.5;
            else if (m.contains("BLASTOISE") || m.contains("LAPRAS")) mult *= 2.5;
            else mult *= 2.0;
        }
        speedDebuff = 1.0f; // Reset debuff after calculation
        return mult;
    }

    private void handleDash(double speed, ParticleManager p) {
        if (isDashing) {
            if (System.currentTimeMillis() - dashStartTime < DASH_DURATION) {
                if (p != null && System.currentTimeMillis() % 2 == 0) p.addExplosion(positionX, positionY, Color.LTGRAY, 1);
            } else isDashing = false;
        }
    }

    // --- GESTÃO DE COLEÇÃO ---

    public List<CapturedCreature> getParty() {
        List<CapturedCreature> party = new ArrayList<>();
        for (CapturedCreature c : creatureCollection) if (c.isInParty) party.add(c);
        return party;
    }

    public List<CapturedCreature> getStorage() {
        List<CapturedCreature> storage = new ArrayList<>();
        for (CapturedCreature c : creatureCollection) if (!c.isInParty) storage.add(c);
        return storage;
    }

    public void addCreatureToCollection(String species, boolean isShiny, Rarity rarity, int gender) {
        boolean addToParty = getParty().size() < 6;
        creatureCollection.add(new CapturedCreature(species, isShiny, rarity, addToParty, gender));
    }

    // --- RENDERIZAÇÃO ---

    public void draw(Canvas canvas, Companion companion) {
        updateAnimationFrame();
        String spriteName = "personagem"; 
        
        if (isMounted && companion != null) {
            spriteName = companion.getSpecies().toLowerCase();
            if (companion.getFieldAbility() == Companion.FieldAbility.FLY) {
                spriteName += "_voar";
            } else if (companion.getFieldAbility() == Companion.FieldAbility.SURF) {
                spriteName += "_nadar";
            }
        }
        
        Bitmap sprite = SpriteManager.getInstance().getUnitSprite(spriteName, direction, frameIndex, false);
        
        Paint p = new Paint();
        if (skinColor != Color.WHITE) {
            p.setColorFilter(new android.graphics.PorterDuffColorFilter(skinColor, android.graphics.PorterDuff.Mode.MULTIPLY));
        }
        
        canvas.drawOval((float)positionX - 30, (float)positionY + 10, (float)positionX + 30, (float)positionY + 30, new Paint()); // Sombra fixa

        if (sprite != null) {
            int r = isMounted ? 80 : 64; // Montaria é maior
            canvas.drawBitmap(sprite, null, new android.graphics.Rect((int)positionX - r, (int)positionY - r, (int)positionX + r, (int)positionY + r), p);
            
            p.setColor(Color.WHITE); p.setTextSize(24); p.setTextAlign(Paint.Align.CENTER); p.setFakeBoldText(true);
            canvas.drawText("Treinador [Nv." + level + "]", (float)positionX, (float)positionY - r - 40, p);
            
            drawHealthBar(canvas, r);
        }
    }

    public void draw(Canvas canvas) {
        draw(canvas, null);
    }

    private void drawHealthBar(Canvas canvas, int r) {
        Paint p = new Paint();
        float bw = 100f, bh = 10f;
        float bx = (float)positionX - bw/2, by = (float)positionY - r - 30;
        
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
        canvas.drawRect(bx, by + 12, bx + bw, by + 16, p);
        p.setColor(Color.rgb(100, 255, 100));
        float xpPerc = (float)exp / (level * 100);
        canvas.drawRect(bx, by + 12, bx + (bw * xpPerc), by + 16, p);
    }

    private void updateAnimationFrame() {
        if (isMoving) {
            long now = System.currentTimeMillis();
            if (now - lastFrameTime > ANIMATION_SPEED) {
                frameIndex = (frameIndex + 1) % 3;
                lastFrameTime = now;
            }
        } else frameIndex = 1;
    }

    // --- GETTERS E SETTERS ---
    public double getPositionX() { return positionX; }
    public double getPositionY() { return positionY; }
    public void setPosition(double x, double y) { this.positionX = x; this.positionY = y; }
    public int getGold() { return gold; }
    public void addGold(int amount) { this.gold += amount; }
    public void loadProgress(int level, int exp, int talentPoints, int gems, String clan, int daycareIntro) {
        this.level = level;
        this.exp = exp;
        this.talentPoints = talentPoints;
        this.gems = gems;
        this.clan = clan != null ? clan : "Nenhum";
        this.maxHp = 100 + (level * 10);
        this.hp = maxHp;
        this.daycareIntroSeen = daycareIntro == 1;
    }
    public int getGems() { return gems; }
    public void addGems(int a) { gems += a; }
    public int getLevel() { return level; }
    public int getExp() { return exp; }
    public boolean isMoving() { return isMoving; }

    public void addMission(Mission mission) {
        activeMissions.add(mission);
    }

    public boolean useClanSkill() {
        if (clan.equals("Nenhum")) return false;
        long now = System.currentTimeMillis();
        if (now - lastClanSkillTime > CLAN_SKILL_COOLDOWN) {
            lastClanSkillTime = now;
            return true;
        }
        return false;
    }

    public float getClanSkillProgress() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastClanSkillTime;
        return Math.min(1.0f, (float)elapsed / CLAN_SKILL_COOLDOWN);
    }

    public void gainFishingExp(int amount) {
        fishingExp += amount;
        if (fishingExp >= fishingLevel * 50) {
            fishingExp -= fishingLevel * 50;
            fishingLevel++;
        }
    }

    public void useStamina(float amount) {
        this.stamina -= amount;
        if (this.stamina < 0) this.stamina = 0;
    }

    private long lastClanSkillTime = 0;
    private static final long CLAN_SKILL_COOLDOWN = 30000;
    public String getClan() { return clan; }
    public void setClan(String c) { clan = c; }
    public boolean isMounted() { return isMounted; }
    public void setMounted(boolean m) { isMounted = m; }
    public List<CapturedCreature> getCreatureCollection() { return creatureCollection; }
    public Map<String, Integer> getInventory() { return inventory; }
    public Map<String, Integer> getStorageItems() { return storageItems; }
    public void addItem(String n) { inventory.put(n, inventory.getOrDefault(n, 0) + 1); }
    public void addItem(String n, int amount) { inventory.put(n, inventory.getOrDefault(n, 0) + amount); }
    private boolean isTutorialCompleted = false;
    public boolean isTutorialCompleted() { return isTutorialCompleted; }
    public void setTutorialCompleted(boolean completed) { this.isTutorialCompleted = completed; }
    public void useItem(String n) {
        if (inventory.containsKey(n)) {
            int c = inventory.get(n);
            if (c > 1) inventory.put(n, c - 1); else inventory.remove(n);
        }
    }
    public float getStamina() { return stamina; }
    public float getMaxStamina() { return maxStamina; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public int getTalentPoints() { return talentPoints; }
    public Map<String, Integer> getTalents() { return talents; }
    public void useTalentPoint(String t) { if (talentPoints > 0) { talents.put(t, talents.getOrDefault(t, 0) + 1); talentPoints--; } }
    public int getFishingLevel() { return fishingLevel; }
    public int getFishingExp() { return fishingExp; }
    public void setFishingStats(int l, int e) { fishingLevel = l; fishingExp = e; }
    public boolean gainExp(int a) { exp += a; if (exp >= 100 * level) { exp -= 100 * level; level++; talentPoints++; hp = maxHp; return true; } return false; }
    public void dash() { if (System.currentTimeMillis() - lastDashTime > DASH_COOLDOWN && stamina >= 30) { isDashing = true; dashStartTime = System.currentTimeMillis(); lastDashTime = System.currentTimeMillis(); stamina -= 30; } }
    public float getDashCooldownProgress() { return Math.min(1.0f, (float)(System.currentTimeMillis() - lastDashTime) / DASH_COOLDOWN); }
    public List<Mission> getActiveMissions() { return activeMissions; }
    public boolean isMovingState() { return isMoving; }
    private void updateDirection(double vx, double vy) { if (Math.abs(vx) > Math.abs(vy)) direction = (vx > 0) ? 3 : 1; else direction = (vy > 0) ? 0 : 2; }
    public int getNgPlusLevel() { return 0; }
    public int getSkinColor() { return skinColor; }
    public void setSkinColor(int color) { this.skinColor = color; }
    public boolean hasSeenDaycareIntro() { return daycareIntroSeen; }
    public void setDaycareIntroSeen(boolean seen) { this.daycareIntroSeen = seen; }
    public List<TrainerSkill> getTrainerSkills() { return trainerSkills; }
}
