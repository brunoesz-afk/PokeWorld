package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;

public class SummonUI extends BaseUI {
    public RectF summonCommonBtn = new RectF(300, 250, 600, 450);
    public RectF summonPremiumBtn = new RectF(700, 250, 1000, 450);

    @Override
    public void draw(Canvas canvas, GameView gv) {
        Player p = gv.player;
        drawOverlay(canvas, "PORTAL DE INVOCAÇÃO", Color.argb(245, 20, 0, 40));
        drawSummonBox(canvas, summonCommonBtn, "Comum", "5 💎", Color.CYAN);
        drawSummonBox(canvas, summonPremiumBtn, "Premium", "15 💎", Color.rgb(255, 215, 0));
        paint.setColor(Color.WHITE); paint.setTextSize(30); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Suas Gemas: " + p.getGems() + " 💎", canvas.getWidth()/2f, 550, paint);
    }

    private void drawSummonBox(Canvas canvas, RectF b, String title, String cost, int color) {
        paint.setColor(Color.argb(100, 0, 0, 0)); canvas.drawRoundRect(b, 20, 20, paint);
        paint.setColor(color); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(5);
        canvas.drawRoundRect(b, 20, 20, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(35); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, b.centerX(), b.top + 60, paint);
        paint.setColor(Color.WHITE); paint.setTextSize(30);
        canvas.drawText(cost, b.centerX(), b.bottom - 40, paint);
        paint.setTextSize(60);
        canvas.drawText("✨", b.centerX(), b.centerY() + 20, paint);
    }
}
