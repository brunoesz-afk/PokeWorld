package com.example.pokeworld.managers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ParticleManager {
    private List<Particle> particles = new ArrayList<>();
    private Random random = new Random();
    private Paint paint = new Paint();

    private static class Particle {
        float x, y, vx, vy, size, alpha;
        int color;
        long lifeTime;
        long startTime;
        int frame = 0;
        long lastFrameTime = 0;

        Particle(float x, float y, int color, float size) {
            this.x = x; this.y = y; this.color = color; this.size = size;
            this.alpha = 255;
            this.startTime = System.currentTimeMillis();
            this.lifeTime = 500 + new Random().nextInt(500);
            
            Random r = new Random();
            this.vx = (r.nextFloat() - 0.5f) * 10f;
            this.vy = (r.nextFloat() - 0.5f) * 10f;
            this.frame = r.nextInt(9); // Começa em um frame aleatório do sheet 3x3
        }
    }

    public void addExplosion(double x, double y, int color, int count) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle((float)x, (float)y, color, 5 + random.nextFloat() * 10));
        }
    }

    public void addBiomeEffect(String type, int width, int height) {
        if (particles.size() > 200) return;
        
        if (type.equals("LEAVES")) {
            if (random.nextDouble() < 0.1) {
                Particle p = new Particle(random.nextFloat() * width, -10, Color.rgb(34, 139, 34), 5 + random.nextFloat() * 5);
                p.vx = (random.nextFloat() - 0.2f) * 2f;
                p.vy = 2 + random.nextFloat() * 3f;
                p.lifeTime = 5000;
                particles.add(p);
            }
        } else if (type.equals("EMBER")) {
            if (random.nextDouble() < 0.1) {
                Particle p = new Particle(random.nextFloat() * width, height + 10, Color.rgb(255, 69, 0), 3 + random.nextFloat() * 4);
                p.vx = (random.nextFloat() - 0.5f) * 2f;
                p.vy = -3 - random.nextFloat() * 2f;
                p.lifeTime = 3000;
                particles.add(p);
            }
        }
    }

    public void update() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.alpha -= 5;
            if (p.alpha <= 0 || System.currentTimeMillis() - p.startTime > p.lifeTime) {
                it.remove();
            }
        }
    }

    public void draw(Canvas canvas) {
        SpriteManager sm = SpriteManager.getInstance();
        android.graphics.Bitmap leavesSheet = sm.getMiscSprite("leaf_particle");
        android.graphics.Bitmap embersSheet = sm.getMiscSprite("ember_particle");

        for (Particle p : particles) {
            android.graphics.Bitmap targetSheet = null;
            if (p.color == Color.rgb(34, 139, 34)) targetSheet = leavesSheet;
            else if (p.color == Color.rgb(255, 69, 0)) targetSheet = embersSheet;

            if (targetSheet != null) {
                // Desenha frame do sheet 3x3
                int frameW = targetSheet.getWidth() / 3;
                int frameH = targetSheet.getHeight() / 3;
                int fx = (p.frame % 3) * frameW;
                int fy = (p.frame / 3) * frameH;
                
                android.graphics.Rect src = new android.graphics.Rect(fx, fy, fx + frameW, fy + frameH);
                android.graphics.RectF dst = new android.graphics.RectF(p.x - p.size, p.y - p.size, p.x + p.size, p.y + p.size);
                
                paint.setAlpha((int)p.alpha);
                canvas.drawBitmap(targetSheet, src, dst, paint);
                
                // Anima o frame (mais lento)
                if (System.currentTimeMillis() - p.lastFrameTime > 200) {
                    p.frame = (p.frame + 1) % 9;
                    p.lastFrameTime = System.currentTimeMillis();
                }
            } else {
                paint.setColor(p.color);
                paint.setAlpha((int)p.alpha);
                canvas.drawCircle(p.x, p.y, p.size, paint);
            }
        }
        paint.setAlpha(255);
    }
}
