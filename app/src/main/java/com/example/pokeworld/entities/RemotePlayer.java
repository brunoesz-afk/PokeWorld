package com.example.pokeworld.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.managers.SpriteManager;

public class RemotePlayer {
    public String id;
    public String name = "Treinador";
    public String mapName = "CITY";
    public double posX;
    public double posY;
    public double targetX;
    public double targetY;
    public int direction = 0;
    public int animFrame = 0;
    public String spriteName = "personagem";
    public boolean isShiny = false;
    public long lastUpdateTime;

    private static final Paint namePaint = new Paint();
    private static final Paint nameBgPaint = new Paint();

    static {
        namePaint.setColor(Color.WHITE);
        namePaint.setTextSize(20f);
        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setAntiAlias(true);
        namePaint.setFakeBoldText(true);

        nameBgPaint.setColor(Color.argb(160, 0, 0, 0));
        nameBgPaint.setStyle(Paint.Style.FILL);
    }

    public RemotePlayer(String id, String name, double x, double y, String mapName) {
        this.id = id;
        this.name = name != null ? name : "Treinador";
        this.posX = x;
        this.posY = y;
        this.targetX = x;
        this.targetY = y;
        this.mapName = mapName;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void setTargetPosition(double newX, double newY) {
        this.targetX = newX;
        this.targetY = newY;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void update() {
        // Interpolação suave de posição
        double dx = targetX - posX;
        double dy = targetY - posY;
        posX += dx * 0.25;
        posY += dy * 0.25;
    }

    public void draw(Canvas canvas) {
        Bitmap sprite = SpriteManager.getInstance().getUnitSprite(spriteName, direction, animFrame, isShiny);
        if (sprite != null) {
            float drawX = (float) posX - (sprite.getWidth() / 2f);
            float drawY = (float) posY - (sprite.getHeight() / 2f);
            canvas.drawBitmap(sprite, drawX, drawY, null);

            // Nome acima do personagem
            float textX = (float) posX;
            float textY = drawY - 10f;
            float textWidth = namePaint.measureText(name);

            RectF bgRect = new RectF(textX - textWidth / 2f - 8f, textY - 22f, textX + textWidth / 2f + 8f, textY + 6f);
            canvas.drawRoundRect(bgRect, 6f, 6f, nameBgPaint);
            canvas.drawText(name, textX, textY, namePaint);
        }
    }
}
