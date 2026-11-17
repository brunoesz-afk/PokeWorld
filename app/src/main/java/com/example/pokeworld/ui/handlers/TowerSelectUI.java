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
    public RectF prevPageBtn = new RectF();
    public RectF nextPageBtn = new RectF();
    public int towerPage = 0; // 0: 1-10, 1: 11-20
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

        drawOverlay(canvas, "TORRE DE DESAFIOS (SELECIONE O ANDAR)", Color.argb(245, 15, 25, 40));

        exitBtn.set(w - 180, 30, w - 40, 90);
        drawExitButton(canvas, exitBtn);

        paint.setColor(Color.WHITE); paint.setTextSize(18); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Página " + (towerPage + 1) + " / 2 (Andares 1 a 20)", w / 2f, 115, paint);

        floorCards.clear();
        int cols = 5;
        float startX = 100;
        float startY = 150;
        float cardW = (w - 200 - (cols - 1) * 25) / cols;
        float cardH = 120; // Cartões maiores (zoom)
        float spacing = 20;

        int startFloor = towerPage == 0 ? 1 : 11;
        int endFloor = towerPage == 0 ? 10 : 20;
        int maxUnlockedFloor = gv.tower.getMaxUnlockedFloor();

        for (int f = startFloor; f <= endFloor; f++) {
            int localIdx = f - startFloor;
            int r = localIdx / cols;
            int c = localIdx % cols;

            float cx = startX + c * (cardW + spacing);
            float cy = startY + r * (cardH + spacing);

            FloorCard card = new FloorCard(f, f <= maxUnlockedFloor);
            card.rect.set(cx, cy, cx + cardW, cy + cardH);
            floorCards.add(card);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(card.unlocked ? Color.rgb(45, 70, 110) : Color.rgb(30, 30, 35));
            canvas.drawRoundRect(card.rect, 16, 16, paint);

            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(card.unlocked ? Color.YELLOW : Color.GRAY);
            canvas.drawRoundRect(card.rect, 16, 16, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(card.unlocked ? Color.WHITE : Color.GRAY);
            paint.setTextSize(22);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("Andar " + f, card.rect.centerX(), card.rect.top + 38, paint);

            paint.setColor(Color.GREEN);
            paint.setTextSize(15);
            canvas.drawText("3 Hordas", card.rect.centerX(), card.rect.top + 70, paint);
            canvas.drawText("Prêmio: " + (f * 150) + " 🪙", card.rect.centerX(), card.rect.bottom - 22, paint);
        }

        // Botões de Paginação
        prevPageBtn.set(w / 2f - 220, h - 75, w / 2f - 20, h - 25);
        nextPageBtn.set(w / 2f + 20, h - 75, w / 2f + 220, h - 25);

        if (towerPage > 0) {
            drawButton(canvas, prevPageBtn, "◄ Anterior (1-10)");
        }
        if (towerPage < 1) {
            drawButton(canvas, nextPageBtn, "Próximo (11-20) ►");
        }
    }
}
