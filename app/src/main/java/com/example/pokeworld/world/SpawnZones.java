package com.example.pokeworld.world;

import com.example.pokeworld.entities.Enemy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pontos de spawn PRÉ-DEFINIDOS de um mapa (sempre os mesmos, sorteados uma vez a partir do nome do mapa).
 * Um Pokémon só existe enquanto o jogador está perto do ponto; ao se afastar ele é removido (economiza
 * memória e processamento) e, ao voltar, reaparece. Depois de derrotado, o ponto espera um tempo.
 */
public class SpawnZones {

    /** Terreno do mapa (onde um Pokémon pode ficar em pé). */
    public interface Ground { boolean walkable(double x, double y); }

    /** Ponte com o mundo: cria e remove o Pokémon de verdade. */
    public interface Sink {
        Enemy spawn(Point p);
        void despawn(Enemy e);
    }

    public static class Point {
        public final double x, y;
        public final String species;
        public final int level, color;
        public final double radius;
        public final boolean boss, shinyBoss;
        public final long cooldownMs;
        long readyAt = 0;
        Enemy live;

        Point(double x, double y, String species, int level, int color, double radius, boolean boss, boolean shinyBoss, long cooldownMs) {
            this.radius = radius; this.x = x; this.y = y; this.species = species; this.level = level; this.color = color;
            this.boss = boss; this.shinyBoss = shinyBoss; this.cooldownMs = cooldownMs;
        }
        public Enemy getLive() { return live; }
    }

    /**
     * Raios medidos a partir do "raio de visão" R (metade da diagonal da tela, em px do mundo):
     * nasce logo depois da borda da tela, nunca dentro dela, e some assim que fica um pouco além da borda.
     * Assim só existe o que está na tela e ao redor imediato (menos RAM e menos processamento).
     */
    public static final double POP_MARGIN = 40, ACTIVATE_MARGIN = 260, DEACTIVATE_MARGIN = 360;
    /** Raio de visão usado quando o chamador não informa (tela cheia, zoom padrão). */
    public static final double DEFAULT_VIEW = 800;
    public static final long WILD_COOLDOWN = 60_000L, BOSS_COOLDOWN = 5 * 60_000L;

    private final List<Point> points = new ArrayList<>();
    private final int tile;

    /**
     * @param avoidX/avoidY onde o jogador nasce no mapa (nenhum ponto perto dele)
     */
    public SpawnZones(String mapName, int rows, int cols, int tile, String[] species, int baseLevel, int color,
                      Ground ground, double avoidX, double avoidY) {
        this.tile = tile;
        if (species == null || species.length == 0) return;
        Random rnd = new Random(mapName.hashCode() * 31L + 7);
        int target = Math.max(10, Math.min(40, rows * cols / 170));
        double minGap = 7.0 * tile, avoid = 9.0 * tile;
        double cx = cols * tile / 2.0, cy = rows * tile / 2.0;
        for (int tries = 0; tries < target * 60 && points.size() < target; tries++) {
            double x = (3 + rnd.nextInt(Math.max(1, cols - 6)) + 0.5) * tile;
            double y = (3 + rnd.nextInt(Math.max(1, rows - 6)) + 0.5) * tile;
            if (dist(x, y, avoidX, avoidY) < avoid) continue;
            if (!ground.walkable(x, y)) continue;
            // 4 vizinhos livres: evita ponto encostado em parede/árvore
            if (!ground.walkable(x + tile, y) || !ground.walkable(x - tile, y) || !ground.walkable(x, y + tile) || !ground.walkable(x, y - tile)) continue;
            boolean close = false;
            for (Point q : points) if (dist(x, y, q.x, q.y) < minGap) { close = true; break; }
            if (close) continue;
            int lv = Math.max(1, baseLevel + (int) (dist(x, y, cx, cy) / 400));
            points.add(new Point(x, y, species[rnd.nextInt(species.length)], lv, color, 32, false, false, WILD_COOLDOWN));
        }
    }

    /** Chefe fixo (volta depois de 5 min). */
    public void addBoss(double x, double y, String species, int level, int color, double radius, boolean shiny) {
        points.add(new Point(x, y, species, level, color, radius, true, shiny, BOSS_COOLDOWN));
    }

    public List<Point> getPoints() { return points; }

    public int liveCount() {
        int n = 0;
        for (Point p : points) if (p.live != null) n++;
        return n;
    }

    /** Chamado ~2x por segundo. */
    public void update(double px, double py, long now, Sink sink) { update(px, py, now, sink, DEFAULT_VIEW); }

    /** @param viewR metade da diagonal da área visível (px do mundo) */
    public void update(double px, double py, long now, Sink sink, double viewR) {
        final double noPop = viewR + POP_MARGIN, activate = viewR + ACTIVATE_MARGIN, deactivate = viewR + DEACTIVATE_MARGIN;
        for (Point p : points) {
            if (p.live != null) {
                if (!p.live.isAlive) { p.live = null; p.readyAt = now + p.cooldownMs; continue; } // derrotado
                if (dist(p.live.positionX, p.live.positionY, px, py) > deactivate && !p.live.isRaider) {
                    sink.despawn(p.live);   // jogador se afastou: some, sem prêmio nem cooldown
                    p.live = null;
                }
                continue;
            }
            if (now < p.readyAt) continue;
            double d = dist(p.x, p.y, px, py);
            if (d <= activate && (p.boss || d >= noPop)) {
                p.live = sink.spawn(p);
                if (p.live == null) p.readyAt = now + 5000; // terreno bloqueado: tenta de novo depois
            }
        }
    }

    /** Remove tudo que está vivo (troca de mapa). */
    public void clear(Sink sink) {
        for (Point p : points) { if (p.live != null) { sink.despawn(p.live); p.live = null; } }
    }

    private static double dist(double ax, double ay, double bx, double by) {
        double dx = ax - bx, dy = ay - by; return Math.sqrt(dx * dx + dy * dy);
    }
}
