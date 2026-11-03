package com.example.pokeworld.entities;

import android.graphics.Color;

public class Equipment {
    public enum Type { WEAPON, ARMOR }
    
    private String name;
    private Type type;
    private int power;
    private int rarityColor;

    public Equipment(String name, Type type, int power, int rarityColor) {
        this.name = name;
        this.type = type;
        this.power = power;
        this.rarityColor = rarityColor;
    }

    public static Equipment create(String name) {
        if (name == null || name.equals("Nenhum")) return null;
        
        switch (name) {
            // Armas
            case "Espada de Ferro": return new Equipment("Espada de Ferro", Type.WEAPON, 15, Color.LTGRAY);
            case "Lâmina Vital": return new Equipment("Lâmina Vital", Type.WEAPON, 40, Color.GREEN);
            case "Cajado Solar": return new Equipment("Cajado Solar", Type.WEAPON, 60, Color.YELLOW);
            case "Foice Sombria": return new Equipment("Foice Sombria", Type.WEAPON, 100, Color.rgb(75, 0, 130));
            
            // Armaduras
            case "Escudo de Madeira": return new Equipment("Escudo de Madeira", Type.ARMOR, 30, Color.rgb(139, 69, 19));
            case "Armadura de Rocha": return new Equipment("Armadura de Rocha", Type.ARMOR, 80, Color.GRAY);
            case "Manto de Gelo": return new Equipment("Manto de Gelo", Type.ARMOR, 150, Color.CYAN);
            case "Couraça de Dragão": return new Equipment("Couraça de Dragão", Type.ARMOR, 300, Color.RED);
            
            default: return null;
        }
    }

    public String getName() { return name; }
    public Type getType() { return type; }
    public int getPower() { return power; }
    public int getRarityColor() { return rarityColor; }
}
