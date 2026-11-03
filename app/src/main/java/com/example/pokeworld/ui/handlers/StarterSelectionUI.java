package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.SpriteManager;

public class StarterSelectionUI extends BaseUI {
    public RectF charmanderBtn = new RectF();
    public RectF bulbasaurBtn = new RectF();
    public RectF squirtleBtn = new RectF();
    public RectF pikachuBtn = new RectF();

    private int animFrame = 0;
    private long lastAnimTime = 0;

    @Override
    public void draw(Canvas canvas, GameView gv) {
        drawOverlay(canvas, "ESCOLHA SEU POKÉMON INICIAL", Color.argb(240, 15, 30, 20));

        float w = canvas.getWidth();

        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Selecione seu primeiro companheiro de jornada:", w / 2f, 110, paint);

        long now = System.currentTimeMillis();
        if (now - lastAnimTime > 250) {
            animFrame = (animFrame + 1) % 3;
            lastAnimTime = now;
        }

        float cardW = 200;
        float cardH = 260;
        float spacing = 20;
        float totalW = (cardW * 4) + (spacing * 3);
        float startX = (w - totalW) / 2f;
        float startY = 160;

        charmanderBtn.set(startX, startY, startX + cardW, startY + cardH);
        bulbasaurBtn.set(startX + cardW + spacing, startY, startX + (cardW * 2) + spacing, startY + cardH);
        squirtleBtn.set(startX + (cardW + spacing) * 2, startY, startX + (cardW * 3) + (spacing * 2), startY + cardH);
        pikachuBtn.set(startX + (cardW + spacing) * 3, startY, startX + (cardW * 4) + (spacing * 3), startY + cardH);

        drawStarterCard(canvas, charmanderBtn, "Charmander", "🔥 Fogo", Color.rgb(255, 100, 50));
        drawStarterCard(canvas, bulbasaurBtn, "Bulbasaur", "🌿 Planta", Color.rgb(100, 200, 100));
        drawStarterCard(canvas, squirtleBtn, "Squirtle", "💧 Água", Color.rgb(100, 180, 255));
        drawStarterCard(canvas, pikachuBtn, "Pikachu", "⚡ Elétrico", Color.rgb(255, 215, 0));
    }

    private void drawStarterCard(Canvas canvas, RectF card, String species, String typeText, int color) {
        // Background do Card
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(200, 30, 40, 50));
        canvas.drawRoundRect(card, 15, 15, paint);

        // Borda
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(color);
        canvas.drawRoundRect(card, 15, 15, paint);

        // Sprite
        Bitmap sprite = SpriteManager.getInstance().getUnitSprite(species, 0, animFrame, false);
        if (sprite != null) {
            float r = 50;
            canvas.drawBitmap(sprite, null, new RectF(card.centerX() - r, card.top + 30, card.centerX() + r, card.top + 130), null);
        }

        // Nome da Espécie
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        canvas.drawText(species, card.centerX(), card.top + 165, paint);

        // Tipo
        paint.setColor(color);
        paint.setTextSize(18);
        paint.setFakeBoldText(false);
        canvas.drawText(typeText, card.centerX(), card.top + 195, paint);

        // Botão Escolher
        RectF btn = new RectF(card.left + 15, card.bottom - 45, card.right - 15, card.bottom - 10);
        paint.setColor(color);
        canvas.drawRoundRect(btn, 10, 10, paint);
        paint.setColor(Color.BLACK);
        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("ESCOLHER", btn.centerX(), btn.centerY() + 6, paint);
    }
}
