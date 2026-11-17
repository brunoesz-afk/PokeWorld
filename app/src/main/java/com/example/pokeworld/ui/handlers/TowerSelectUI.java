package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;

import java.util.ArrayList;
import java.util.List;

public class TowerSelectUI extends BaseUI {
    public RectF exitBtn = new RectF();
    public List<FloorCard> floorCards = new ArrayList<>();

    public static class FloorCard {
        public int floor;
        public RectF rect = new RectF();
        public boolean unlocked;

        public FloorCard(int floor, boolean unlocked) {
            this.floor = floor;
            this.unlocked = unlocked;
        }
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth();
        float h = canvas.getHeight();

        drawOverlay(canvas, "TORRE DE DESAFIOS (SELECIONE O ANDAR - ATÉ 20)", Color.argb(245, 15, 25, 40));

        exitBtn.set(w - 180, 30, w - 40, 90);
        drawExitButton(canvas, exitBtn);

        floorCards.clear();
        int cols = 5;
        float startX = 80;
        float startY = 150;
        float cardW = (w - 160 - (cols - 1) * 20) / cols;
        float cardH = 80;
        float spacing = 15;

        int maxUnlockedFloor = gv.tower.getMaxUnlockedFloor();

        for (int f = 1; f <= 20; f++) {
            int r = (f - 1) / cols;
            int c = (f - 1) % cols;

            float cx = startX + c * (cardW + spacing);
            float cy = startY + r * (cardH + spacing);

            FloorCard card = new FloorCard(f, f <= maxUnlockedFloor);
            card.rect.set(cx, cy, cx + cardW, cy + cardH);
            floorCards.add(card);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(card.unlocked ? Color.rgb(40, 60, 90) : Color.rgb(30, 30, 35));
            canvas.drawRoundRect(card.rect, 12, 12, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            paint.setColor(card.unlocked ? Color.YELLOW : Color.GRAY);
            canvas.drawRoundRect(card.rect, 12, 12, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(card.unlocked ? Color.WHITE : Color.GRAY);
            paint.setTextSize(18);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Andar " + f, card.rect.centerX(), card.rect.top + 28, paint);

            paint.setColor(Color.GREEN);
            paint.setTextSize(13);
            canvas.drawText("Prêmio: " + (f * 150) + " 🪙", card.rect.centerX(), card.rect.bottom - 15, paint);
        }
    }
}
