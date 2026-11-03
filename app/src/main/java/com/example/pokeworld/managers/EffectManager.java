package com.example.pokeworld.managers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EffectManager {
    private List<FloatingText> effects = new ArrayList<>();
    private List<Projectile> projectiles = new ArrayList<>();
    private List<Beam> beams = new ArrayList<>();
    private List<CaptureEffect> captureEffects = new ArrayList<>();
    private final List<StaticAnimation> animations = new ArrayList<>();
    private final List<DirectionalAnimation> dirAnimations = new ArrayList<>();
    private Paint paint = new Paint();

    private static class CaptureEffect {
        Bitmap[][] sheet;
        int frameIndex = 0;
        long lastFrameTime = 0;
        double x, y, tx, ty;
        float size = 40;
        enum State { THROWING, WOBBLING, DONE }
        State state = State.THROWING;
        long stateStartTime;
        Loot target;
        Runnable onComplete;

        CaptureEffect(Bitmap[][] sheet, double x, double y, Loot target, Runnable onComplete) {
            this.sheet = sheet; this.x = x; this.y = y; this.target = target;
            this.tx = target.getPositionX(); this.ty = target.getPositionY();
            this.onComplete = onComplete;
            this.stateStartTime = System.currentTimeMillis();
        }

        void update() {
            long now = System.currentTimeMillis();
            int rows = sheet != null ? sheet.length : 1;
            int cols = sheet != null ? sheet[0].length : 1;
            int totalFrames = rows * cols;

            if (state == State.THROWING) {
                double dx = tx - x, dy = ty - y;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist < 20) {
                    x = tx; y = ty;
                    state = State.WOBBLING;
                    stateStartTime = now;
                    target.setVisible(false);
                } else {
                    x += (dx / dist) * 20;
                    y += (dy / dist) * 20;
                }
                if (now - lastFrameTime > 80) {
                    frameIndex = (frameIndex + 1) % totalFrames;
                    lastFrameTime = now;
                }
            } else if (state == State.WOBBLING) {
                if (now - lastFrameTime > 200) {
                    frameIndex = (frameIndex + 1) % totalFrames;
                    lastFrameTime = now;
                }
                if (now - stateStartTime > 2000) {
                    state = State.DONE;
                    if (onComplete != null) onComplete.run();
                }
            }
        }

        void draw(Canvas canvas) {
            if (sheet != null && state != State.DONE) {
                int rows = sheet.length;
                int cols = sheet[0].length;
                int rIdx = frameIndex / cols;
                int cIdx = frameIndex % cols;
                Bitmap b = sheet[Math.min(rIdx, rows - 1)][Math.min(cIdx, cols - 1)];
                int r = (int)size;
                canvas.drawBitmap(b, null, new Rect((int)x - r, (int)y - r, (int)x + r, (int)y + r), null);
            }
        }
    }

    private static class FloatingText {
        String text;
        float x, y, alpha = 255;
        float vx; 
        int color;
        long startTime;

        FloatingText(String text, float x, float y, int color) {
            this.text = text; this.x = x; this.y = y; this.color = color;
            this.startTime = System.currentTimeMillis();
            this.vx = (float)(Math.random() - 0.5) * 4f;
        }
    }

    private static class Beam {
        Bitmap sprite;
        double x, y, tx, ty;
        long endTime;
        float width;

        Beam(Bitmap sprite, double x, double y, double tx, double ty, float width, int durationMs) {
            this.sprite = sprite; this.x = x; this.y = y; this.tx = tx; this.ty = ty;
            this.width = width;
            this.endTime = System.currentTimeMillis() + durationMs;
        }
    }

    private static class StaticAnimation {
        Bitmap sprite;
        Bitmap[][] sheet;
        int frameIndex = 0;
        long lastFrameTime = 0;
        double x, y;
        float size;
        long endTime;
        float rotation = 0;

        StaticAnimation(Bitmap sprite, double x, double y, float size, int durationMs) {
            this.sprite = sprite; this.x = x; this.y = y; this.size = size;
            this.endTime = System.currentTimeMillis() + durationMs;
        }

        StaticAnimation(Bitmap[][] sheet, double x, double y, float size, int durationMs) {
            this.sheet = sheet; this.x = x; this.y = y; this.size = size;
            this.endTime = System.currentTimeMillis() + durationMs;
        }

        void draw(Canvas canvas) {
            Bitmap toDraw;
            if (sheet != null) {
                long now = System.currentTimeMillis();
                int rows = sheet.length;
                int cols = sheet[0].length;
                int total = rows * cols;
                if (now - lastFrameTime > 80) {
                    frameIndex = (frameIndex + 1) % total;
                    lastFrameTime = now;
                }
                int rIdx = frameIndex / cols;
                int cIdx = frameIndex % cols;
                toDraw = sheet[rIdx][cIdx];
            } else {
                toDraw = sprite;
            }
            if (toDraw != null) {
                int r = (int)size;
                canvas.save();
                canvas.translate((float)x, (float)y);
                if (rotation != 0) canvas.rotate(rotation);
                canvas.drawBitmap(toDraw, null, new Rect(-r, -r, r, r), null);
                canvas.restore();
            }
        }
    }

    private static class DirectionalAnimation {
        Bitmap[][] sheet;
        double x, y, tx, ty;
        long startTime, endTime;
        float height;

        DirectionalAnimation(Bitmap[][] sheet, double x, double y, double tx, double ty, float height, int durationMs) {
            this.sheet = sheet; this.x = x; this.y = y; this.tx = tx; this.ty = ty;
            this.height = height;
            this.startTime = System.currentTimeMillis();
            this.endTime = startTime + durationMs;
        }

        void draw(Canvas canvas) {
            if (sheet == null || sheet.length == 0 || sheet[0].length == 0) return;
            long now = System.currentTimeMillis();
            float progress = (float)(now - startTime) / (endTime - startTime);
            if (progress > 1.0f) progress = 1.0f;

            int cols = sheet[0].length;
            int rows = sheet.length;
            int totalFrames = rows * cols;
            int fIdx = (int)(progress * (totalFrames - 1));

            int rIdx = fIdx / cols;
            int cIdx = fIdx % cols;
            Bitmap b = sheet[Math.min(rIdx, rows - 1)][Math.min(cIdx, cols - 1)];
            if (b == null) return;

            double dx = tx - x, dy = ty - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

            canvas.save();
            canvas.translate((float)x, (float)y);
            canvas.rotate(angle);
            // Estica o sprite do atacante até o alvo
            Rect dst = new Rect(0, (int)(-height / 2), (int)dist, (int)(height / 2));
            canvas.drawBitmap(b, null, dst, null);
            canvas.restore();
        }
    }

    private static class Projectile {
        Bitmap sprite;
        Bitmap[][] sheet;
        int frameIndex = 0;
        long lastFrameTime = 0;
        double x, y, tx, ty;
        double speed = 15.0;
        float size;
        boolean reached = false;
        int damage;
        Object target;

        Projectile(Bitmap sprite, double x, double y, double tx, double ty, float size, int damage, Object target) {
            this.sprite = sprite; this.x = x; this.y = y; this.tx = tx; this.ty = ty; this.size = size;
            this.damage = damage; this.target = target;
        }

        Projectile(Bitmap[][] sheet, double x, double y, double tx, double ty, float size, int damage, Object target) {
            this.sheet = sheet; this.x = x; this.y = y; this.tx = tx; this.ty = ty; this.size = size;
            this.damage = damage; this.target = target;
        }

        void update() {
            double dx = tx - x, dy = ty - y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist < speed) {
                x = tx; y = ty; reached = true;
                applyDamage();
            } else {
                x += (dx / dist) * speed;
                y += (dy / dist) * speed;
            }
        }

        private void applyDamage() {
            if (damage <= 0 || target == null) return;
            if (target instanceof Enemy) {
                ((Enemy) target).takeDamage(damage);
            } else if (target instanceof Companion) {
                ((Companion) target).takeDamage(damage);
            }
        }

        void draw(Canvas canvas) {
            Bitmap toDraw = sprite;
            if (sheet != null) {
                long now = System.currentTimeMillis();
                int rows = sheet.length;
                int cols = sheet[0].length;
                int totalFrames = rows * cols;
                if (now - lastFrameTime > 100) {
                    frameIndex = (frameIndex + 1) % totalFrames;
                    lastFrameTime = now;
                }
                int rIdx = frameIndex / cols;
                int cIdx = frameIndex % cols;
                toDraw = sheet[Math.min(rIdx, rows - 1)][Math.min(cIdx, cols - 1)];
            }
            if (toDraw != null) {
                int r = (int)size;
                canvas.drawBitmap(toDraw, null, new Rect((int)x - r, (int)y - r, (int)x + r, (int)y + r), null);
            }
        }
    }

    public void addCaptureEffect(double x, double y, Loot target, Runnable onComplete) {
        Bitmap[][] sheet = SpriteManager.getInstance().getUnitSheet("pokebola");
        if (sheet != null) {
            captureEffects.add(new CaptureEffect(sheet, x, y, target, onComplete));
        }
    }

    public void addText(String text, double x, double y, int color) {
        effects.add(new FloatingText(text, (float)x, (float)y, color));
    }

    public void addProjectile(String spellName, double x, double y, double tx, double ty, float size, int damage, Object target) {
        Bitmap[][] sheet = SpriteManager.getInstance().getUnitSheet(spellName);
        if (sheet != null) {
            projectiles.add(new Projectile(sheet, x, y, tx, ty, size, damage, target));
        } else {
            Bitmap s = SpriteManager.getInstance().getSpellSprite(spellName);
            if (s != null) {
                projectiles.add(new Projectile(s, x, y, tx, ty, size, damage, target));
            }
        }
    }

    public void addBeam(String spellName, double x, double y, double tx, double ty, float width, int durationMs) {
        Bitmap s = SpriteManager.getInstance().getSpellSprite(spellName);
        if (s != null) {
            beams.add(new Beam(s, x, y, tx, ty, width, durationMs));
        }
    }

    public void addAnimation(String spellName, double x, double y, float size, int durationMs) {
        addAnimation(spellName, x, y, size, durationMs, 0);
    }

    public void addAnimation(String spellName, double x, double y, float size, int durationMs, float rotation) {
        Bitmap[][] sheet = SpriteManager.getInstance().getUnitSheet(spellName);
        if (sheet != null) {
            StaticAnimation anim = new StaticAnimation(sheet, x, y, size, durationMs);
            anim.rotation = rotation;
            animations.add(anim);
        } else {
            Bitmap s = SpriteManager.getInstance().getSpellSprite(spellName);
            if (s != null) {
                StaticAnimation anim = new StaticAnimation(s, x, y, size, durationMs);
                anim.rotation = rotation;
                animations.add(anim);
            }
        }
    }

    public void addDirectionalAnimation(String spellName, double x, double y, double tx, double ty, float height, int durationMs) {
        Bitmap[][] sheet = SpriteManager.getInstance().getUnitSheet(spellName);
        if (sheet != null) {
            dirAnimations.add(new DirectionalAnimation(sheet, x, y, tx, ty, height, durationMs));
        }
    }

    public void update() {
        Iterator<FloatingText> it = effects.iterator();
        while (it.hasNext()) {
            FloatingText ft = it.next();
            ft.y -= 3; 
            ft.x += ft.vx; 
            ft.alpha -= 5; 
            if (ft.alpha <= 0 || System.currentTimeMillis() - ft.startTime > 1000) {
                it.remove();
            }
        }

        Iterator<Projectile> pit = projectiles.iterator();
        while (pit.hasNext()) {
            Projectile p = pit.next();
            p.update();
            if (p.reached) pit.remove();
        }

        Iterator<Beam> bit = beams.iterator();
        while (bit.hasNext()) {
            if (System.currentTimeMillis() > bit.next().endTime) bit.remove();
        }

        Iterator<CaptureEffect> ceit = captureEffects.iterator();
        while (ceit.hasNext()) {
            CaptureEffect ce = ceit.next();
            ce.update();
            if (ce.state == CaptureEffect.State.DONE) ceit.remove();
        }

        Iterator<StaticAnimation> ait = animations.iterator();
        while (ait.hasNext()) {
            if (System.currentTimeMillis() > ait.next().endTime) ait.remove();
        }

        Iterator<DirectionalAnimation> dit = dirAnimations.iterator();
        while (dit.hasNext()) {
            if (System.currentTimeMillis() > dit.next().endTime) dit.remove();
        }
    }

    public void draw(Canvas canvas) {
        for (Beam b : beams) {
            if (b.sprite == null) continue;
            double dx = b.tx - b.x, dy = b.ty - b.y;
            double dist = Math.sqrt(dx * dx + dy * dy);
            float angle = (float) Math.toDegrees(Math.atan2(dy, dx));

            canvas.save();
            canvas.translate((float)b.x, (float)b.y);
            canvas.rotate(angle);
            Rect dst = new Rect(0, (int)(-b.width / 2), (int)dist, (int)(b.width / 2));
            canvas.drawBitmap(b.sprite, null, dst, null);
            canvas.restore();
        }

        for (Projectile p : projectiles) {
            p.draw(canvas);
        }

        for (CaptureEffect ce : captureEffects) {
            ce.draw(canvas);
        }

        for (StaticAnimation a : animations) {
            a.draw(canvas);
        }

        for (DirectionalAnimation d : dirAnimations) {
            d.draw(canvas);
        }

        paint.setTextSize(35);
        paint.setFakeBoldText(true);
        paint.setTextAlign(Paint.Align.CENTER);
        for (FloatingText ft : effects) {
            paint.setColor(ft.color);
            paint.setAlpha((int)ft.alpha);
            canvas.drawText(ft.text, ft.x, ft.y, paint);
        }
        paint.setAlpha(255);
    }
}
