package com.example.pokeworld.managers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.pokeworld.core.GameView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeatherManager {
    private List<Particle> particles = new ArrayList<>();
    private Random random = new Random();
    private Paint paint = new Paint();
    private String currentType = "NONE";

    private static class Particle {
        float x, y, speed, size;
        Particle(float x, float y, float speed, float size) {
            this.x = x; this.y = y; this.speed = speed; this.size = size;
        }
    }

    public void setWeather(String mapName) {
        particles.clear();
        if (mapName.equals("FOREST")) currentType = "RAIN";
        else if (mapName.equals("MOUNTAIN") || mapName.equals("SNOW_MOUNTAIN")) currentType = "SNOW";
        else if (mapName.equals("DESERT")) currentType = "SANDSTORM";
        else if (mapName.equals("VOLCANO")) currentType = "HEATWAVE";
        else currentType = "NONE";
    }

    public void update(GameView gv) {
        if (currentType.equals("NONE")) return;

        int width = gv.getWidth();
        int height = gv.getHeight();

        if (particles.size() < 100) {
            float x = random.nextFloat() * width;
            float y = -10;
            if (currentType.equals("SANDSTORM")) {
                x = width + 10;
                y = random.nextFloat() * height;
            }
            particles.add(new Particle(x, y, 5 + random.nextFloat() * 10, 2 + random.nextFloat() * 5));
        }

        // Lightning Logic
        if (currentType.equals("RAIN") && random.nextDouble() < 0.005) {
            triggerLightning(gv);
        }

        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            if (currentType.equals("RAIN")) {
                p.y += p.speed;
                p.x += 1;
            } else if (currentType.equals("SNOW")) {
                p.y += p.speed * 0.3f;
                p.x += Math.sin(p.y * 0.05f) * 2;
            } else if (currentType.equals("SANDSTORM")) {
                p.x -= p.speed;
                p.y += (random.nextFloat() - 0.5f) * 2;
            }

            if (p.y > height || p.x < -10 || p.x > width + 10) {
                particles.remove(i);
            }
        }
    }

    private void triggerLightning(GameView gv) {
        gv.startShake(15, 20);
        float lx = (float) (gv.player.getPositionX() + (random.nextFloat() - 0.5f) * 400);
        float ly = (float) (gv.player.getPositionY() + (random.nextFloat() - 0.5f) * 400);
        
        gv.particles.addExplosion(lx, ly, Color.WHITE, 30);
        gv.effects.addText("⚡ CABUM!", lx, ly, Color.YELLOW);
        
        // Dano se estiver muito perto
        double dist = Math.sqrt(Math.pow(gv.player.getPositionX() - lx, 2) + Math.pow(gv.player.getPositionY() - ly, 2));
        if (dist < 100) {
            gv.companion.takeDamage(20);
            gv.ui.showMessage("⚠️ Seu pet foi atingido por um raio!");
        }
    }

    public void draw(Canvas canvas) {
        if (currentType.equals("NONE")) return;

        if (currentType.equals("RAIN")) paint.setColor(Color.argb(150, 100, 150, 255));
        else if (currentType.equals("SNOW")) paint.setColor(Color.WHITE);
        else if (currentType.equals("SANDSTORM")) paint.setColor(Color.argb(100, 210, 180, 140));
        else if (currentType.equals("HEATWAVE")) paint.setColor(Color.argb(50, 255, 100, 0));

        for (Particle p : particles) {
            if (currentType.equals("RAIN")) {
                canvas.drawLine(p.x, p.y, p.x + 2, p.y + p.size * 2, paint);
            } else if (currentType.equals("HEATWAVE")) {
                // Efeito de calor (Ondas)
                float wave = (float) Math.sin(System.currentTimeMillis() / 200.0 + p.y * 0.1) * 5;
                canvas.drawCircle(p.x + wave, p.y, p.size, paint);
            } else {
                canvas.drawCircle(p.x, p.y, p.size, paint);
            }
        }
    }

    public String getCurrentType() {
        return currentType;
    }
}
