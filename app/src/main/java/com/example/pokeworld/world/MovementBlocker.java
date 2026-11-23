package com.example.pokeworld.world;

/** Bloqueio extra de movimento sobre o mapa (ex.: muralhas e prédios da base). */
public interface MovementBlocker {
    boolean blocks(double worldX, double worldY);
}
