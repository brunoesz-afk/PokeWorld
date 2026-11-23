package com.example.pokeworld.entities;

public class Skill {
    private String name;
    private int damage;
    private long cooldown;
    private long lastUsedTime;
    private int color;
    private ElementalType type;
    private boolean area = false;     // true: explode no alvo e acerta todos num raio
    private double radius = 0;        // raio da área (px do mundo)

    /** Golpe em área: a animação 3x1 de explosão do elemento toca no alvo e todos num raio levam dano. */
    public Skill(String name, int damage, long cooldown, int color, ElementalType type, double radius) {
        this(name, damage, cooldown, color, type);
        this.area = radius > 0;
        this.radius = radius;
    }

    public Skill(String name, int damage, long cooldown, int color, ElementalType type) {
        this.name = name;
        this.damage = damage;
        this.cooldown = cooldown;
        this.color = color;
        this.type = type;
        this.lastUsedTime = 0;
    }

    public boolean isReady() {
        return System.currentTimeMillis() - lastUsedTime > cooldown;
    }

    public int use() {
        if (isReady()) {
            lastUsedTime = System.currentTimeMillis();
            return damage;
        }
        return 0;
    }

    public String getName() { return name; }
    public int getColor() { return color; }
    public ElementalType getType() { return type; }
    public boolean isArea() { return area; }
    public double getRadius() { return radius; }

    public float getCooldownProgress() {
        long elapsed = System.currentTimeMillis() - lastUsedTime;
        return Math.min(1.0f, (float) elapsed / cooldown);
    }
}
