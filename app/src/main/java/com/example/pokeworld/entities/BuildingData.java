package com.example.pokeworld.entities;

/**
 * Modelo de dados para informações de construções salvas.
 */
public class BuildingData {
    public int level;
    public boolean isConstructed;
    public double posX, posY;
    public long constructionFinishTime;

    public BuildingData(int level, boolean isConstructed) {
        this.level = level;
        this.isConstructed = isConstructed;
    }
}
