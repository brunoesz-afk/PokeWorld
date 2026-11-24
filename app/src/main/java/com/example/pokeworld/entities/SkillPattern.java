package com.example.pokeworld.entities;

import java.util.ArrayList;
import java.util.List;

/** Formas de golpe em quadrados do chão (estilo PokeTibia): cada célula recebe a animação do elemento e causa dano. */
public enum SkillPattern {
    NONE, SQUARE3, SQUARE5, CROSS, RING, LINE, CONE;

    /**
     * Centros (x,y em px do mundo) das células. Quadrado/cruz/anel ficam centrados no alvo (encaixados no grid);
     * linha e cone saem de quem lançou, na direção do alvo.
     */
    public List<double[]> cells(double tx, double ty, double fromX, double fromY, int tile) {
        List<double[]> out = new ArrayList<>();
        double cx = (Math.floor(tx / tile) + 0.5) * tile, cy = (Math.floor(ty / tile) + 0.5) * tile;
        switch (this) {
            case SQUARE3: for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) out.add(new double[]{cx + dx * tile, cy + dy * tile}); break;
            case SQUARE5: for (int dx = -2; dx <= 2; dx++) for (int dy = -2; dy <= 2; dy++) out.add(new double[]{cx + dx * tile, cy + dy * tile}); break;
            case CROSS:
                out.add(new double[]{cx, cy});
                for (int i = 1; i <= 2; i++) {
                    out.add(new double[]{cx + i * tile, cy}); out.add(new double[]{cx - i * tile, cy});
                    out.add(new double[]{cx, cy + i * tile}); out.add(new double[]{cx, cy - i * tile});
                }
                break;
            case RING:
                for (int dx = -2; dx <= 2; dx++) for (int dy = -2; dy <= 2; dy++)
                    if (Math.abs(dx) == 2 || Math.abs(dy) == 2) out.add(new double[]{cx + dx * tile, cy + dy * tile});
                break;
            case LINE: case CONE: {
                double vx = tx - fromX, vy = ty - fromY, d = Math.hypot(vx, vy);
                if (d < 1) { vx = 1; vy = 0; d = 1; }
                vx /= d; vy /= d;
                double px = -vy, py = vx;
                if (this == LINE) {
                    for (int i = 1; i <= 6; i++) out.add(new double[]{fromX + vx * i * tile, fromY + vy * i * tile});
                } else {
                    for (int i = 1; i <= 3; i++) for (int j = -(i - 1); j <= i - 1; j++)
                        out.add(new double[]{fromX + vx * i * tile + px * j * tile, fromY + vy * i * tile + py * j * tile});
                }
                break;
            }
            default: break;
        }
        return out;
    }
}
