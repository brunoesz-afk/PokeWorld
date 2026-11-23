package com.example.pokeworld.entities;

import android.graphics.Color;

public class Rune {
    public enum Effect { VAMPIRISM, RESISTANCE, POWER, SPEED, LUCK }
    
    private String name;
    private Effect effect;
    private float value; // Multiplicador ou valor fixo
    private int rarityColor;

    public Rune(String name, Effect effect, float value, int rarityColor) {
        this.name = name;
        this.effect = effect;
        this.value = value;
        this.rarityColor = rarityColor;
    }

    public static Rune create(String name) {
        if (name == null || name.equals("Nenhum")) return null;
        switch (name) {
            case "Runa de Sangue": return new Rune("Runa de Sangue", Effect.VAMPIRISM, 0.05f, Color.RED);
            case "Runa de Ferro": return new Rune("Runa de Ferro", Effect.RESISTANCE, 0.10f, Color.GRAY);
            case "Runa de Zeus": return new Rune("Runa de Zeus", Effect.POWER, 1.20f, Color.YELLOW);
            case "Runa de Mercúrio": return new Rune("Runa de Mercúrio", Effect.SPEED, 1.15f, Color.CYAN);
            case "Runa da Sorte": return new Rune("Runa da Sorte", Effect.LUCK, 1.50f, Color.MAGENTA);
            default: return null;
        }
    }

    public String getDescription() {
        switch (effect) {
            case VAMPIRISM: return "Cada golpe do companheiro cura " + Math.round(value * 100) + "% do dano causado.";
            case RESISTANCE: return "Reduz em " + Math.round(value * 100) + "% o dano recebido pelo companheiro.";
            case POWER: return "Aumenta o ataque do companheiro em " + Math.round((value - 1f) * 100) + "%.";
            case SPEED: return "Aumenta a velocidade de movimento em " + Math.round((value - 1f) * 100) + "%.";
            case LUCK: return "Aumenta em " + Math.round((value - 1f) * 100) + "% as chances de drops raros e a quantidade de ouro.";
            default: return "";
        }
    }

    /** Todas as runas existentes, na ordem de exibição. */
    public static final String[] ALL = {"Runa de Sangue", "Runa de Ferro", "Runa de Zeus", "Runa de Mercúrio", "Runa da Sorte"};

    public static String icon(String runeName) {
        Rune r = create(runeName);
        if (r == null) return "🔷";
        switch (r.effect) {
            case VAMPIRISM: return "🩸";
            case RESISTANCE: return "🛡️";
            case POWER: return "⚡";
            case SPEED: return "💨";
            default: return "🍀";
        }
    }

    public String getName() { return name; }
    public Effect getEffect() { return effect; }
    public float getValue() { return value; }
    public int getRarityColor() { return rarityColor; }
}
