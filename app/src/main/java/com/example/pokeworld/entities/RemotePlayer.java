package com.example.pokeworld.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.managers.SpriteManager;

public class RemotePlayer {
    public String id;
    public String name = "Treinador";
    public String mapName = "CITY";

    // Posição e animação do Treinador
    public double posX;
    public double posY;
    public double targetX;
    public double targetY;
    public int direction = 0;
    public int frameIndex = 1;
    public int skinColor = Color.WHITE;
    public boolean isMounted = false;

    // Posição e animação do Pokémon acompanhante
    public String petSpecies = "";
    public double petX;
    public double petY;
    public double petTargetX;
    public double petTargetY;
    public int petDirection = 0;
    public int petFrameIndex = 1;
    public boolean isShiny = false;

    public long lastUpdateTime;

    private static final Paint namePaint = new Paint();
    private static final Paint nameBgPaint = new Paint();
    private static final Paint shadowPaint = new Paint();

    static {
        namePaint.setColor(Color.WHITE);
        namePaint.setTextSize(20f);
        namePaint.setTextAlign(Paint.Align.CENTER);
        namePaint.setAntiAlias(true);
        namePaint.setFakeBoldText(true);

        nameBgPaint.setColor(Color.argb(160, 0, 0, 0));
        nameBgPaint.setStyle(Paint.Style.FILL);

        shadowPaint.setColor(Color.argb(80, 0, 0, 0));
        shadowPaint.setStyle(Paint.Style.FILL);
    }

    public RemotePlayer(String id, String name, double x, double y, String mapName) {
        this.id = id;
        this.name = (name != null && !name.isEmpty()) ? name : "Treinador";
        this.posX = x;
        this.posY = y;
        this.targetX = x;
        this.targetY = y;
        this.petX = x - 45;
        this.petY = y;
        this.petTargetX = x - 45;
        this.petTargetY = y;
        this.mapName = mapName;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void setTargetPosition(double newX, double newY, double newPetX, double newPetY) {
        this.targetX = newX;
        this.targetY = newY;
        this.petTargetX = newPetX;
        this.petTargetY = newPetY;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public void update() {
        // Interpolação suave das posições do Treinador e do Pet
        posX += (targetX - posX) * 0.25;
        posY += (targetY - posY) * 0.25;

        petX += (petTargetX - petX) * 0.25;
        petY += (petTargetY - petY) * 0.25;
    }

    public void draw(Canvas canvas) {
        if (isMounted && petSpecies != null && !petSpecies.isEmpty()) {
            // Desenha o jogador montado no Pokémon
            Bitmap mountSprite = SpriteManager.getInstance().getUnitSprite(petSpecies, direction, frameIndex, isShiny);
            if (mountSprite != null) {
                int r = 80;
                canvas.drawOval((float) posX - 30, (float) posY + 10, (float) posX + 30, (float) posY + 30, shadowPaint);
                canvas.drawBitmap(mountSprite, null, new Rect((int) posX - r, (int) posY - r, (int) posX + r, (int) posY + r), null);
                drawNameTag(canvas, name, (float) posX, (float) posY - r - 15);
            }
            return;
        }

        // 1. Desenha o Treinador (Com raio idêntico ao Player.java -> r = 64)
        Bitmap trainerSprite = SpriteManager.getInstance().getUnitSprite("personagem", direction, frameIndex, false);
        if (trainerSprite != null) {
            int r = 64;
            canvas.drawOval((float) posX - 30, (float) posY + 10, (float) posX + 30, (float) posY + 30, shadowPaint);

            Paint p = null;
            if (skinColor != Color.WHITE) {
                p = new Paint();
                p.setColorFilter(new PorterDuffColorFilter(skinColor, PorterDuff.Mode.MULTIPLY));
            }

            canvas.drawBitmap(trainerSprite, null, new Rect((int) posX - r, (int) posY - r, (int) posX + r, (int) posY + r), p);
            drawNameTag(canvas, name, (float) posX, (float) posY - r - 15);
        }

        // 2. Desenha o Pokémon Acompanhante (Com raio idêntico ao Companion.java -> r = 56)
        if (petSpecies != null && !petSpecies.isEmpty()) {
            Bitmap petSprite = SpriteManager.getInstance().getUnitSprite(petSpecies, petDirection, petFrameIndex, isShiny);
            if (petSprite != null) {
                int r = 56;
                canvas.drawOval((float) petX - 25, (float) petY + 10, (float) petX + 25, (float) petY + 25, shadowPaint);
                canvas.drawBitmap(petSprite, null, new Rect((int) petX - r, (int) petY - r, (int) petX + r, (int) petY + r), null);
                drawNameTag(canvas, petSpecies, (float) petX, (float) petY - r - 15);
            }
        }
    }

    private void drawNameTag(Canvas canvas, String text, float textX, float textY) {
        float textWidth = namePaint.measureText(text);
        RectF bgRect = new RectF(textX - textWidth / 2f - 8f, textY - 22f, textX + textWidth / 2f + 8f, textY + 6f);
        canvas.drawRoundRect(bgRect, 6f, 6f, nameBgPaint);
        canvas.drawText(text, textX, textY, namePaint);
    }
}
