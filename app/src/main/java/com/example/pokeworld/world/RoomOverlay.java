package com.example.pokeworld.world;

import android.graphics.Canvas;
import com.example.pokeworld.entities.Player;

/** Camada extra de uma sala especial (ex.: a Arena): desenha no mundo e pode mandar na câmera. */
public interface RoomOverlay {
    /** Desenha em coordenadas do mundo (depois do chão, antes do jogador). */
    void drawWorld(Canvas canvas, long nowMs);
    /** Foco da câmera {x, y, zoom}; null = seguir o jogador como sempre. */
    double[] focus(Player p);
}
