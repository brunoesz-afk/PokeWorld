package com.example.pokeworld.entities;

import android.graphics.Color;

public enum Rarity {
    COMMON("Comum", Color.WHITE),
    UNCOMMON("Incomum", Color.GREEN),
    RARE("Raro", Color.BLUE),
    EPIC("Épico", Color.rgb(128, 0, 128)), // Roxo
    LEGENDARY("Lendário", Color.RED);

    private final String label;
    private final int color;

    Rarity(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }

    public Rarity next() {
        int nextOrdinal = this.ordinal() + 1;
        if (nextOrdinal < Rarity.values().length) {
            return Rarity.values()[nextOrdinal];
        }
        return this;
    }
}
