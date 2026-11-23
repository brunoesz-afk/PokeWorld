package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.SpriteManager;
import java.util.Random;

public class EvolutionUI extends BaseUI {
    public String oldSpecies, newSpecies;
    private long startTime;
    private static final long DURATION = 5000; 
    private Random random = new Random();

    public void start(String oldS, String newS) {
        this.oldSpecies = oldS;
        this.newSpecies = newS;
        this.startTime = System.currentTimeMillis();
    }

    public boolean isFinished() { return System.currentTimeMillis() - startTime >= DURATION; }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        long elapsed = System.currentTimeMillis() - startTime;
        float progress = Math.min(1.0f, (float) elapsed / DURATION);

        canvas.drawColor(Color.argb((int) (220 * Math.min(1.0f, progress * 1.5f)), 0, 0, 0));

        float centerX = canvas.getWidth() / 2f;
        float centerY = canvas.getHeight() / 2f;

        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        
        if (progress < 0.8f) {
            canvas.drawText("O QUE ESTÁ ACONTECENDO?!", centerX, 150, paint);
            
            int alpha = (int) (127 + 127 * Math.sin(elapsed / 50.0));
            Bitmap sprite = (elapsed % 200 < 100) ? 
                SpriteManager.getInstance().getUnitSprite(oldSpecies, 0, 1) :
                SpriteManager.getInstance().getUnitSprite(newSpecies, 0, 1);

            if (sprite != null) {
                paint.setAlpha(alpha);
                int r = (int) (120 + 30 * Math.sin(elapsed / 100.0));
                canvas.drawBitmap(sprite, null, new Rect((int)centerX - r, (int)centerY - r, (int)centerX + r, (int)centerY + r), paint);
                paint.setAlpha(255);
            }
            
            for(int i=0; i<15; i++) {
                paint.setColor(Color.WHITE);
                float px = centerX + (random.nextFloat()-0.5f) * 500;
                float py = centerY + (random.nextFloat()-0.5f) * 500;
                canvas.drawCircle(px, py, 3 + random.nextInt(5), paint);
            }
        } else {
            canvas.drawText("PARABÉNS!", centerX, 120, paint);
            paint.setColor(Color.YELLOW);
            canvas.drawText(oldSpecies.toUpperCase() + " EVOLUIU PARA " + newSpecies.toUpperCase() + "!", centerX, 180, paint);
            
            Bitmap sprite = SpriteManager.getInstance().getUnitSprite(newSpecies, 0, 1);
            if (sprite != null) {
                int r = 200;
                canvas.drawBitmap(sprite, null, new Rect((int)centerX - r, (int)centerY - r, (int)centerX + r, (int)centerY + r), null);
            }
            
            if (random.nextDouble() < 0.3) {
                gv.particles.addExplosion(centerX, centerY, Color.YELLOW, 2);
            }

            if (progress >= 1.0f) {
                paint.setColor(Color.WHITE);
                paint.setTextSize(25);
                canvas.drawText("(Toque para continuar)", centerX, canvas.getHeight() - 100, paint);
            }
        }
        paint.setFakeBoldText(false);
    }
}
