package com.example.pokeworld.world;

import android.graphics.Canvas;
import android.graphics.Color;

import com.example.pokeworld.entities.RemotePlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Treinadores simulados na cidade: passeiam pelos distritos com o Pokémon ao lado, param um pouco e seguem de novo,
 * para a cidade parecer cheia de jogadores. São só visuais (não interagem), desenhados como os jogadores remotos.
 * Sem dependência do mapa: quem chama diz onde se pode pisar (Ground) e onde há NPCs a evitar.
 */
public final class CityCrowd {
    public interface Ground { boolean walkable(double x, double y); }

    private static final class Bot {
        RemotePlayer rp;
        double homeX, homeY, x0, y0, x1, y1;     // casa e limites do passeio (pixels)
        double tx, ty, speed;
        long pauseUntil;
        boolean moving;
        double faceX = 0, faceY = 1;
    }

    private static final int COUNT_PER_DISTRICT = 2;
    private static final double WANDER = 6 * 64;
    private static final String[] NAMES = {"Kaio", "Luna", "AshBR", "Mari", "Lipe", "Duda", "Gui_Pk", "Nath", "Rafa", "Bia", "Theo", "Yuri",
            "Lara", "Caio", "Isa", "Dante", "Nina", "Vitor", "Alice", "Pedrin", "Sofia", "Leo", "Joy", "Bruna", "Ravi", "Maya", "Davi", "Tati"};
    private static final String[] PETS = {"Pikachu", "Charmander", "Squirtle", "Bulbasaur", "Growlithe", "Meowth", "Vulpix", "Machop",
            "Oddish", "Pidgey", "Rattata", "Jigglypuff", "Abra", "Houndour", "Pichu", "Poliwag", "Slowpoke", "Magikarp", "Ponyta", "Mankey", "Raichu", "Ninetales", "Wartortle", "Ivysaur", "Pidgeotto", "Persian", "Golduck"};
    private static final String[] MOUNTS = {"Arcanine", "Rapidash", "Tauros", "Charizard"};
    private static final int[] SKINS = {Color.WHITE, Color.WHITE, Color.WHITE, Color.rgb(255, 222, 200), Color.rgb(228, 205, 255), Color.rgb(205, 232, 255), Color.rgb(255, 235, 185), Color.rgb(210, 255, 220)};

    private final List<Bot> bots = new ArrayList<>();
    private long last = 0;

    public void clear() { bots.clear(); last = 0; }
    public int size() { return bots.size(); }
    public List<RemotePlayer> players() { List<RemotePlayer> l = new ArrayList<>(); for (Bot b : bots) l.add(b.rp); return l; }

    /** Cria os treinadores: alguns por distrito (ou nos retângulos dados, em tiles r0,c0,r1,c1), longe dos NPCs e só em chão livre. */
    public void spawn(int[][] areas, Ground g, double[][] avoid, Random rnd) {
        bots.clear(); last = 0;
        List<String> names = new ArrayList<>(java.util.Arrays.asList(NAMES));
        java.util.Collections.shuffle(names, rnd);
        int n = 0;
        for (int[] a : areas) {
            for (int k = 0; k < COUNT_PER_DISTRICT; k++) {
                for (int tries = 0; tries < 60; tries++) {
                    double x = (a[1] + rnd.nextDouble() * (a[3] - a[1] + 1)) * 64, y = (a[0] + rnd.nextDouble() * (a[2] - a[0] + 1)) * 64;
                    if (!free(g, avoid, x, y)) continue;
                    Bot b = new Bot();
                    String name = names.get(n++ % names.size());
                    boolean mounted = rnd.nextInt(7) == 0;
                    RemotePlayer rp = new RemotePlayer("bot_" + n, name, x, y, "CITY");
                    rp.skinColor = SKINS[rnd.nextInt(SKINS.length)];
                    rp.isMounted = mounted;
                    rp.petSpecies = mounted ? MOUNTS[rnd.nextInt(MOUNTS.length)] : PETS[rnd.nextInt(PETS.length)];
                    rp.isShiny = rnd.nextInt(12) == 0;
                    rp.direction = rnd.nextInt(4);
                    rp.petX = rp.petTargetX = x - 50; rp.petY = rp.petTargetY = y + 6;
                    b.rp = rp; b.homeX = x; b.homeY = y;
                    b.x0 = Math.max(a[1] * 64, x - WANDER); b.x1 = Math.min((a[3] + 1) * 64, x + WANDER);
                    b.y0 = Math.max(a[0] * 64, y - WANDER); b.y1 = Math.min((a[2] + 1) * 64, y + WANDER);
                    b.tx = x; b.ty = y; b.speed = mounted ? 170 : 85 + rnd.nextInt(40);
                    b.pauseUntil = System.currentTimeMillis() + rnd.nextInt(4000);
                    bots.add(b);
                    break;
                }
            }
        }
    }

    private boolean free(Ground g, double[][] avoid, double x, double y) {
        if (!g.walkable(x, y) || !g.walkable(x - 24, y) || !g.walkable(x + 24, y) || !g.walkable(x, y + 24)) return false;
        if (avoid != null) for (double[] a : avoid) if (Math.hypot(a[0] - x, a[1] - y) < 110) return false;
        return true;
    }

    public void update(Ground g, double[][] avoid, Random rnd, long now) {
        if (bots.isEmpty()) return;
        long dt = last == 0 ? 16 : Math.min(100, Math.max(0, now - last));
        last = now;
        for (Bot b : bots) {
            RemotePlayer rp = b.rp;
            if (!b.moving) {
                if (now >= b.pauseUntil) {
                    for (int tries = 0; tries < 12; tries++) {
                        double x = b.x0 + rnd.nextDouble() * (b.x1 - b.x0), y = b.y0 + rnd.nextDouble() * (b.y1 - b.y0);
                        if (Math.hypot(x - rp.posX, y - rp.posY) < 120 || !free(g, avoid, x, y)) continue;
                        b.tx = x; b.ty = y; b.moving = true; break;
                    }
                    if (!b.moving) b.pauseUntil = now + 1500;
                }
            } else {
                double dx = b.tx - rp.posX, dy = b.ty - rp.posY, dist = Math.hypot(dx, dy);
                double step = b.speed * dt / 1000.0;
                if (dist <= step) { b.moving = false; b.pauseUntil = now + 1500 + rnd.nextInt(5000); }
                else {
                    double nx = rp.posX + dx / dist * step, ny = rp.posY + dy / dist * step;
                    if (g.walkable(nx, ny + 12)) {
                        rp.posX = nx; rp.posY = ny; b.faceX = dx / dist; b.faceY = dy / dist;
                        rp.direction = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 3 : 1) : (dy > 0 ? 0 : 2);
                    } else { b.moving = false; b.pauseUntil = now + 800 + rnd.nextInt(2500); }    // bateu em algo: espera e escolhe outro ponto
                }
            }
            rp.targetX = rp.posX; rp.targetY = rp.posY;
            rp.frameIndex = b.moving ? (int) ((now / 170) % 3) : 1;
            // o Pokémon fica atrás do treinador e o segue suavemente
            double px = rp.posX - b.faceX * 54, py = rp.posY - b.faceY * 30 + 8;
            double f = Math.min(1.0, dt / 220.0);
            double ox = rp.petX, oy = rp.petY;
            rp.petX += (px - rp.petX) * f; rp.petY += (py - rp.petY) * f;
            rp.petTargetX = rp.petX; rp.petTargetY = rp.petY;
            double mx = rp.petX - ox, my = rp.petY - oy;
            boolean petMoves = Math.hypot(mx, my) > 0.15;
            if (petMoves) rp.petDirection = Math.abs(mx) > Math.abs(my) ? (mx > 0 ? 3 : 1) : (my > 0 ? 0 : 2);
            rp.petFrameIndex = petMoves ? (int) ((now / 170) % 3) : 1;
        }
    }

    /** Desenha os que estão perto da tela (limites do mundo visível, com folga). */
    public void draw(Canvas canvas, float vl, float vt, float vr, float vb) {
        for (Bot b : bots) {
            RemotePlayer rp = b.rp;
            if (rp.posX < vl - 200 || rp.posX > vr + 200 || rp.posY < vt - 200 || rp.posY > vb + 200) continue;
            rp.draw(canvas);
        }
    }
}
