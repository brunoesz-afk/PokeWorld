package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;

public abstract class BaseUI implements UIHandler {
    protected Paint paint = new Paint();

    protected void drawOverlay(Canvas canvas, String title, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawRect(20, 20, canvas.getWidth() - 20, canvas.getHeight() - 20, paint);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE); paint.setTextSize(45); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, canvas.getWidth()/2f, 100, paint);
    }

    protected void drawExitButton(Canvas canvas, RectF b) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(150, 0, 0));
        canvas.drawRoundRect(b, 10, 10, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3);
        canvas.drawRoundRect(b, 10, 10, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SAIR", b.centerX(), b.centerY() + 8, paint);
    }

    protected void drawButton(Canvas canvas, RectF b, String t) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(b, 15, 15, paint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(b, 15, 15, paint);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(26); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(t, b.centerX(), b.centerY() + 10, paint);
    }

    protected void drawButtonWithMultine(Canvas canvas, RectF b, String t) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(b, 15, 15, paint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(b, 15, 15, paint);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(22); paint.setTextAlign(Paint.Align.CENTER);
        
        String[] lines = t.split("\n");
        float y = b.centerY() - ((lines.length - 1) * 12);
        for (String line : lines) {
            canvas.drawText(line, b.centerX(), y + 8, paint);
            y += 25;
        }
    }
    
    protected void drawIcon(Canvas canvas, RectF b, String icon) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRoundRect(b, 20, 20, paint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.argb(150, 255, 255, 255));
        canvas.drawRoundRect(b, 20, 20, paint);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE); paint.setTextSize(40); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(icon, b.centerX(), b.centerY() + 15, paint);
    }

    protected void drawCooldownIcon(Canvas canvas, RectF b, String icon, float progress) {
        paint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRect(b, paint);
        if (progress < 1.0f) {
            paint.setColor(Color.argb(100, 255, 255, 255));
            canvas.drawRect(b.left, b.bottom - (b.height() * (1.0f - progress)), b.right, b.bottom, paint);
        }
        paint.setColor(progress >= 1.0f ? Color.WHITE : Color.GRAY);
        paint.setTextSize(35); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(icon, b.centerX(), b.centerY() + 12, paint);
    }

    protected void drawStatStars(Canvas canvas, float x, float y, float value) {
        for (int i = 0; i < 5; i++) {
            paint.setColor(i < (int)value ? Color.rgb(60, 180, 120) : Color.rgb(40, 50, 60));
            canvas.drawText("★", x + i * 35, y + 15, paint);
        }
    }
}
