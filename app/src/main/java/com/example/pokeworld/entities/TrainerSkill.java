package com.example.pokeworld.entities;

public class TrainerSkill {
    private String name;
    private int staminaCost;
    private long cooldown;
    private long lastUsedTime = 0;
    private int color;
    private String effectType; // HEAL, BUFF, SHIELD

    public TrainerSkill(String name, int staminaCost, long cooldown, int color, String effectType) {
        this.name = name;
        this.staminaCost = staminaCost;
        this.cooldown = cooldown;
        this.color = color;
        this.effectType = effectType;
    }

    public boolean canUse(float currentStamina) {
        return currentStamina >= staminaCost && System.currentTimeMillis() - lastUsedTime > cooldown;
    }

    public void use() {
        lastUsedTime = System.currentTimeMillis();
    }

    public float getCooldownProgress() {
        long elapsed = System.currentTimeMillis() - lastUsedTime;
        return Math.min(1.0f, (float) elapsed / cooldown);
    }

    public String getName() { return name; }
    public int getStaminaCost() { return staminaCost; }
    public int getColor() { return color; }
    public String getEffectType() { return effectType; }
}
