package com.example.pokeworld.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.pokeworld.managers.EffectManager;
import com.example.pokeworld.managers.ParticleManager;
import com.example.pokeworld.managers.SpriteManager;
import com.example.pokeworld.world.TileMap;

/**
 * Enemy: Representa as criaturas selvagens, bosses e pets soltos na base.
 */
public class Enemy {
    // --- ATRIBUTOS BÁSICOS ---
    public String name;
    public double positionX, positionY, radius;
    public int hp, maxHp, color, level;
    public boolean isAlive = true;
    public boolean isBoss = false;
    public boolean isShiny = false;
    public boolean isPet = false;
    /** Invasor da base (movido pela BaseDefense, não pela IA normal). */
    public boolean isRaider = false;
    /** Enquanto true, a IA normal fica parada (a BaseDefense comanda o guardião durante uma invasão). */
    public boolean controlled = false;
    public int gender = 0; // 0: Macho, 1: Fêmea
    private ElementalType type = ElementalType.NORMAL;
    
    // --- MOVIMENTAÇÃO E IA ---
    public double startX, startY;
    private double targetX, targetY;
    private long lastWanderTime = 0;
    private static final long WANDER_INTERVAL = 3000;
    private boolean isWandering = false;
    private boolean isMoving = false;

    private long lastAttackTime = 0;
    private static final long ATTACK_COOLDOWN = 1500;
    private long skillCastEndTime = 0;

    // --- ANIMAÇÃO ---
    private int direction = 0; 
    private int frameIndex = 1; 
    private long lastFrameTime = 0;
    private static final int ANIMATION_SPEED = 300;

    private boolean isSelected = false;

    /** Queimadura, veneno, sono, paralisia e gelo que os golpes do jogador causam. */
    public final StatusSet status = new StatusSet();
    private int paraTick = 0;

    public Enemy(String name, double x, double y, double r, int color, boolean isBoss) {
        this(name, x, y, r, color, isBoss, 1);
    }

    public Enemy(String name, double x, double y, double r, int color, boolean isBoss, int level) {
        this.name = name; this.positionX = x; this.positionY = y;
        this.startX = x; this.startY = y;
        this.targetX = x; this.targetY = y;
        this.level = level;
        
        calculateScale(name, r, isBoss);
        this.color = color;
        this.isBoss = isBoss; 
        this.maxHp = (int)(((isBoss ? 5000 : 250) + (level * 55)) * com.example.pokeworld.managers.Balance.ENEMY_HP);
        this.hp = maxHp;
        this.gender = Math.random() < 0.5 ? 0 : 1;
        setEnemyType();
    }

    private void calculateScale(String name, double baseR, boolean boss) {
        String n = name.toUpperCase();
        double scale;
        if (n.contains("CHARIZARD") || n.contains("BLASTOISE") || n.contains("VENUSAUR") || n.contains("RAICHU") || n.contains("RAIQUAZA")) scale = 1.6;
        else if (n.contains("CHARMELEON") || n.contains("WARTORTLE") || n.contains("IVYSAUR")) scale = 1.25;
        else scale = 0.9;
        this.radius = boss ? baseR * 2.5 : baseR * scale;
    }

    private void setEnemyType() {
        String n = name.toUpperCase();
        if (n.contains("SQUIRTLE") || n.contains("BLASTOISE") || n.contains("VAPOREON") || n.contains("GYARADOS") || n.contains("LAPRAS") || n.contains("SHELLDER") || n.contains("CLOYSTER") || n.contains("KRAKEN")) type = ElementalType.WATER;
        else if (n.contains("CHARMANDER") || n.contains("CHARIZARD") || n.contains("FLAREON") || n.contains("GROWLITHE") || n.contains("ARCANINE") || n.contains("HOUNDOUR") || n.contains("MAGMAR") || n.contains("PONYTA")) type = ElementalType.FIRE;
        else if (n.contains("BULBASAUR") || n.contains("VENUSAUR") || n.contains("ODDISH")) type = ElementalType.PLANT;
        else if (n.contains("PIKACHU") || n.contains("RAICHU") || n.contains("PICHU") || n.contains("ZAPDOS")) type = ElementalType.ELECTRIC;
        else if (n.contains("GEODUDE") || n.contains("GRAVELER") || n.contains("GOLEM") || n.contains("ONIX") || n.contains("RHYHORN") || n.contains("RHYDON") || n.contains("LARVITAR") || n.contains("ARON")) type = ElementalType.ROCK;
        else if (n.contains("EKANS") || n.contains("ARBOK") || n.contains("ZUBAT") || n.contains("GOLBAT") || n.contains("WEEDLE") || n.contains("KAKUNA") || n.contains("BEEDRILL") || n.contains("GRIMER") || n.contains("KOFFING") || n.contains("NIDO")) type = ElementalType.POISON;
        else if (n.contains("ABRA") || n.contains("KADABRA") || n.contains("ALAKAZAM") || n.contains("DROWZEE") || n.contains("GASTLY") || n.contains("HAUNTER") || n.contains("GENGAR")) type = ElementalType.PSYCHIC;
        else if (n.contains("DRATINI") || n.contains("DRAGONAIR") || n.contains("DRAGONITE") || n.contains("RAIQUAZA") || n.contains("DRAGAO")) type = ElementalType.DRAGON;
        else if (n.contains("SWINUB") || n.contains("PILOSWINE") || n.contains("SNEASEL") || n.contains("ARTICUNO")) type = ElementalType.ICE;
        else type = ElementalType.NORMAL;
    }

    // --- CICLO DE UPDATE ---

    public void update(Companion companion, EffectManager effects, ParticleManager particles, TileMap tileMap, boolean isNight) {
        if (!isAlive) return;
        if (status.any()) {
            int dot = status.tick(maxHp, isBoss);
            if (dot > 0 && !isPet) {
                hp -= dot;
                if (effects != null) effects.addText("-" + dot, positionX, positionY - radius, status.has(Status.POISON) ? 0xFFC070F0 : 0xFFFF8030);
                if (hp <= 0) { isAlive = false; return; }
            }
            if (status.stunned()) { isMoving = false; frameIndex = 1; return; }                       // dormindo/congelado: não faz nada
            if (status.has(Status.PARALYZE) && (++paraTick & 1) == 0) return;                          // paralisado: age na metade do ritmo
        }
        if (isRaider || controlled) { updateAnimation(); return; } // quem move o invasor é a BaseDefense
        isMoving = false;

        // Comportamento Pacífico (Pets na base ou em qualquer lugar)
        if (isPet || (tileMap != null && tileMap.getMapName().equals("HOME"))) {
            handleWandering(tileMap); // na base os guardiões ficam dentro das muralhas (sem sair para cortar árvores)
            updateAnimation();
            return;
        }

        if (System.currentTimeMillis() < skillCastEndTime) { frameIndex = 1; return; }

        double dx = companion.getPositionX() - positionX;
        double dy = companion.getPositionY() - positionY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist < radius + 60) {
            handleAttack(companion, effects, isNight);
        } else if (isSelected) {
            handleChaseOrFlee(companion);
        } else {
            handleWandering(tileMap);
        }
        
        handleShinyParticles(particles);
        updateAnimation();
        if (isMoving) updateDirection(dx, dy);
    }

    private void handleAttack(Companion comp, EffectManager effects, boolean isNight) {
        long now = System.currentTimeMillis();
        if (now - lastAttackTime > ATTACK_COOLDOWN) {
            int dmg = isBoss ? 150 : 25;
            dmg += (level * 5); // Dano escala com o nível
            dmg = (int) (dmg * 1.2);   // inimigos batem 20% mais forte
            if (isNight) dmg *= 1.5;
            if (isShiny) dmg *= 1.3;

            double multiplier = ElementalType.getMultiplier(type, comp.getType1(), comp.getType2());
            int finalDmg = (int)(dmg * multiplier);
            if (level <= 1 && name.equalsIgnoreCase("Rattata")) {
                finalDmg = 2; // Rattata do tutorial causa dano inofensivo
            }

            if (effects != null) {
                float fixedEffectSize = (float)TileMap.TILE_SIZE * 1.5f;

                if (type == ElementalType.FIRE) {
                    effects.addProjectile("bola_de_fogo", positionX, positionY, comp.getPositionX(), comp.getPositionY(), fixedEffectSize, finalDmg, comp);
                    skillCastEndTime = System.currentTimeMillis() + 500;
                } else if (type == ElementalType.WATER) {
                    double dist = Math.sqrt(Math.pow(comp.getPositionX() - positionX, 2) + Math.pow(comp.getPositionY() - positionY, 2));
                    if (dist <= 600) {
                        float beamWidth = (float)TileMap.TILE_SIZE * 0.8f;
                        effects.addBeam("spells_jatodeagua", positionX, positionY, comp.getPositionX(), comp.getPositionY(), beamWidth, 600);
                        comp.takeDamage(finalDmg);
                        skillCastEndTime = System.currentTimeMillis() + 600;
                    }
                } else if (name.equalsIgnoreCase("Chicote de Vinha")) {
                    float beamHeight = (float)TileMap.TILE_SIZE * 0.8f;
                    effects.addDirectionalAnimation("spells_chicotedevinha", positionX, positionY, comp.getPositionX(), comp.getPositionY(), beamHeight, 1200);
                    comp.takeDamage(finalDmg);
                    effects.addText("-" + finalDmg, comp.getPositionX(), comp.getPositionY() - 20, Color.RED);
                    skillCastEndTime = System.currentTimeMillis() + 1200;
                } else if (type == ElementalType.PLANT) {
                    if (level >= 30) {
                        float fixedSize = (float)TileMap.TILE_SIZE * 3.5f;
                        effects.addAnimation("spells_petaldance", comp.getPositionX(), comp.getPositionY(), fixedSize, 1000);
                        comp.takeDamage(finalDmg);
                        effects.addText("-" + finalDmg, comp.getPositionX(), comp.getPositionY() - 20, Color.RED);
                        skillCastEndTime = System.currentTimeMillis() + 1000;
                    } else if (level >= 8) {
                        float dx = (float)(comp.getPositionX() - positionX);
                        float dy = (float)(comp.getPositionY() - positionY);
                        float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
                        float fixedSize = (float)TileMap.TILE_SIZE * 3.0f;
                        effects.addAnimation("spells_chicotedevinha", comp.getPositionX(), comp.getPositionY(), fixedSize, 1200, angle);
                        comp.takeDamage(finalDmg);
                        effects.addText("-" + finalDmg, comp.getPositionX(), comp.getPositionY() - 20, Color.RED);
                        skillCastEndTime = System.currentTimeMillis() + 1200;
                    } else {
                        float fixedAoESize = (float)TileMap.TILE_SIZE * 2.5f; 
                        effects.addAnimation("spells_sementes", positionX, positionY, fixedEffectSize, 800);
                        effects.addAnimation("spells_sementes2", comp.getPositionX(), comp.getPositionY(), fixedAoESize, 800);
                        comp.takeDamage(finalDmg);
                        effects.addText("-" + finalDmg, comp.getPositionX(), comp.getPositionY() - 20, Color.RED);
                        skillCastEndTime = System.currentTimeMillis() + 800;
                    }
                } else if (type == ElementalType.POISON || type == ElementalType.ELECTRIC || type == ElementalType.ICE
                        || type == ElementalType.ROCK || type == ElementalType.PSYCHIC || type == ElementalType.DRAGON) {
                    // golpe em quadrados do chão sobre o alvo (ex.: esporos venenosos)
                    effects.addTilePattern(type, SkillPattern.SQUARE3.cells(comp.getPositionX(), comp.getPositionY(), positionX, positionY, TileMap.TILE_SIZE), 900);
                    comp.takeDamage(finalDmg);
                    effects.addText("-" + finalDmg, comp.getPositionX(), comp.getPositionY() - 20, Color.RED);
                    skillCastEndTime = System.currentTimeMillis() + 500;
                } else {
                    comp.takeDamage(finalDmg);
                    effects.addText("-" + finalDmg, comp.getPositionX(), comp.getPositionY() - 20, Color.RED);
                }
                Status st = statusOfType(type);   // alguns inimigos também causam estados
                if (st != null && Math.random() < (isBoss ? 0.30 : 0.15) && comp.status.apply(st, st.durationMs, comp.getType1())) {
                    effects.addText(st.icon + " " + st.label, comp.getPositionX(), comp.getPositionY() - 70, st.color);
                    com.example.pokeworld.managers.SoundManager.play("status");
                }
                if (multiplier > 1.0) effects.addText("🔥 SUPER EFETIVO!", (float)comp.getPositionX(), (float)comp.getPositionY() - 50, Color.YELLOW);
                else if (multiplier < 1.0) effects.addText("💤 Pouco efetivo...", (float)comp.getPositionX(), (float)comp.getPositionY() - 50, Color.GRAY);
            } else {
                comp.takeDamage(finalDmg);
            }
            
            lastAttackTime = now;
        }
    }

    private static Status statusOfType(ElementalType t) {
        switch (t) {
            case FIRE: return Status.BURN;
            case POISON: case PLANT: return Status.POISON;
            case ELECTRIC: return Status.PARALYZE;
            case ICE: return Status.FREEZE;
            case PSYCHIC: return Status.SLEEP;
            default: return null;
        }
    }

    private void handleWorkAI(TileMap tm) {
        if (tm == null) return;
        long now = System.currentTimeMillis();
        
        if (!isWandering && now - lastWanderTime > 5000) {
            int targetType = (getType() == ElementalType.PLANT) ? TileMap.TREE : 
                             (getType() == ElementalType.ROCK) ? TileMap.ROCK : -1;
            
            if (targetType != -1) {
                for (int dr = -10; dr <= 10; dr++) {
                    for (int dc = -10; dc <= 10; dc++) {
                        int r = (int)(positionY / TileMap.TILE_SIZE) + dr;
                        int c = (int)(positionX / TileMap.TILE_SIZE) + dc;
                        if (tm.getTileType(c * TileMap.TILE_SIZE, r * TileMap.TILE_SIZE) == targetType) {
                            targetX = c * TileMap.TILE_SIZE;
                            targetY = r * TileMap.TILE_SIZE;
                            isWandering = true;
                            break;
                        }
                    }
                    if (isWandering) break;
                }
            }
            if (!isWandering) handleWandering(tm);
        }

        if (isWandering) {
            double vx = targetX - positionX, vy = targetY - positionY;
            double d = Math.sqrt(vx * vx + vy * vy);
            if (d > 32) {
                positionX += (vx / d) * 2.0;
                positionY += (vy / d) * 2.0;
                isMoving = true;
                updateDirection(vx, vy);
            } else {
                isMoving = false;
                if (now % 2000 < 100) isWandering = false; 
            }
        }
    }

    private void handleChaseOrFlee(Companion comp) {
        double vx = comp.getPositionX() - positionX;
        double vy = comp.getPositionY() - positionY;
        double d = Math.sqrt(vx * vx + vy * vy);
        
        double factor = (hp < maxHp * 0.2 && !isBoss) ? -4.5 : 3.5; 
        positionX += (vx / d) * factor;
        positionY += (vy / d) * factor;
        isMoving = true;
    }

    private void handleWandering(TileMap tileMap) {
        long now = System.currentTimeMillis();
        if (now - lastWanderTime > WANDER_INTERVAL) {
            if (Math.random() < 0.6) {
                isWandering = true;
                targetX = startX + (Math.random() - 0.5) * 1000;
                targetY = startY + (Math.random() - 0.5) * 1000;
            } else isWandering = false;
            lastWanderTime = now;
        }

        if (isWandering) {
            double vx = targetX - positionX, vy = targetY - positionY;
            double d = Math.sqrt(vx * vx + vy * vy);
            if (d > 10) {
                double nx = positionX + (vx / d) * 2.5, ny = positionY + (vy / d) * 2.5;
                boolean isFireResistant = type == ElementalType.FIRE || type == ElementalType.DRAGON;
                if (tileMap != null && tileMap.isWalkable(nx, ny, false, false, type == ElementalType.WATER, isFireResistant)) {
                    positionX = nx; positionY = ny; isMoving = true;
                } else isWandering = false;
            } else isWandering = false;
        }
    }

    private void handleShinyParticles(ParticleManager p) {
        if (isShiny && p != null && System.currentTimeMillis() % 10 == 0) {
            p.addExplosion(positionX, positionY, Color.rgb(255, 215, 0), 1);
        }
    }

    private void updateAnimation() {
        if (isMoving) {
            if (System.currentTimeMillis() - lastFrameTime > ANIMATION_SPEED) {
                frameIndex = (frameIndex + 1) % 3;
                lastFrameTime = System.currentTimeMillis();
            }
        } else frameIndex = 1;
    }

    // --- RENDERIZAÇÃO ---

    public void draw(Canvas canvas) {
        if (!isAlive) return;
        Paint p = new Paint();
        
        p.setColor(Color.argb(80, 0, 0, 0));
        canvas.drawOval((float)positionX - (float)radius, (float)positionY + (float)radius/2, (float)positionX + (float)radius, (float)positionY + (float)radius, p);

        if (isShiny || isBoss) {
            Paint auraPaint = new Paint();
            auraPaint.setStyle(Paint.Style.STROKE);
            auraPaint.setStrokeWidth(4);
            int auraColor = isBoss ? Color.RED : Color.YELLOW;
            auraPaint.setColor(auraColor);
            float pulse = (float) Math.sin(System.currentTimeMillis() / 200.0) * 10f;
            canvas.drawCircle((float)positionX, (float)positionY, (float)radius * 1.5f + pulse, auraPaint);
        }

        String drawName = name;
        int drawFrame = frameIndex;

        if (System.currentTimeMillis() < skillCastEndTime) {
            drawFrame = 2; 
            if (type == ElementalType.WATER && name.equalsIgnoreCase("Squirtle")) {
                drawName = isShiny ? "squirtle_shiny_jatodeagua" : "squirtle_jatodeagua";
            }
        }

        Bitmap sprite = SpriteManager.getInstance().getUnitSprite(drawName, direction, drawFrame, isShiny);
        if (sprite != null) {
            int r = (int)(radius * 1.5);
            if (isSelected) {
                p.setColor(Color.YELLOW); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(5);
                canvas.drawCircle((float)positionX, (float)positionY, (float)radius * 1.5f + 10, p);
            }
            Rect box = isBoss ? new Rect((int)positionX - r, (int)positionY - r, (int)positionX + r, (int)positionY + r) : SpeciesSize.box(positionX, positionY, r, name);
            canvas.drawBitmap(sprite, null, box, null);
            drawName(canvas);
            drawHealthBar(canvas);
        }
        status.draw(canvas, positionX, positionY, radius);
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
        canvas.drawText(name + " " + genderIcon + " [Nv." + level + "]", (float)positionX, (float)positionY - (float)radius - 30, p);
    }

    private void drawHealthBar(Canvas canvas) {
        Paint p = new Paint();
        float bw = (float)radius * 2f, bh = 8f;
        float bx = (float)positionX - (float)radius, by = (float)positionY - (float)radius - 15f;
        p.setColor(Color.BLACK); 
        canvas.drawRect(bx, by, bx + bw, by + bh, p);
        float healthPerc = (float) hp / maxHp;
        int healthColor;
        if (healthPerc > 0.6) healthColor = Color.GREEN;
        else if (healthPerc > 0.3) healthColor = Color.YELLOW;
        else healthColor = Color.RED;
        p.setColor(healthColor);
        canvas.drawRect(bx, by, bx + (bw * healthPerc), by + bh, p);
    }

    public void takeDamage(int dmg) {
        if (isPet) return; 
        status.wake();
        hp -= com.example.pokeworld.managers.Balance.dmg(dmg);
        if (hp <= 0) isAlive = false;
    }
    /** Anda em direção a um ponto (usado pela defesa da base). Retorna a distância restante. */
    public double stepToward(double tx, double ty, double speed) {
        double vx = tx - positionX, vy = ty - positionY;
        double d = Math.sqrt(vx * vx + vy * vy);
        if (status.stunned()) { isMoving = false; return d; }
        if (status.has(Status.PARALYZE)) speed *= 0.5;
        if (d > 1) {
            double k = Math.min(speed, d) / d;
            positionX += vx * k; positionY += vy * k;
            isMoving = true; updateDirection(vx, vy);
        }
        return d;
    }
    public void setMoving(boolean m) { isMoving = m; }
    public void faceTowards(double tx, double ty) { updateDirection(tx - positionX, ty - positionY); }

    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean s) { isSelected = s; }
    public boolean isAlive() { return isAlive; }
    public double getRadius() { return radius; }
    public double getPositionX() { return positionX; }
    public double getPositionY() { return positionY; }
    public String getName() { return name; }
    public ElementalType getType() { return type; }
    public int getMaxHp() { return maxHp; }
    public boolean isTouched(float tx, float ty) { return Math.sqrt(Math.pow(positionX - tx, 2) + Math.pow(positionY - ty, 2)) < radius * 3; }
    private void updateDirection(double dx, double dy) { if (Math.abs(dx) > Math.abs(dy)) direction = (dx > 0) ? 3 : 1; else direction = (dy > 0) ? 0 : 2; }
}
