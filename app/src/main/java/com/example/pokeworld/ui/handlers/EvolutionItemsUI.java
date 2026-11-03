package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;

public class EvolutionItemsUI extends BaseUI {
    public CapturedCreature selectedCreature;
    public RectF fireStoneBtn = new RectF();
    public RectF waterStoneBtn = new RectF();
    public RectF thunderStoneBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        if (selectedCreature == null) return;
        drawOverlay(canvas, "ITENS DE EVOLUÇÃO: " + selectedCreature.name.toUpperCase(), Color.argb(245, 10, 30, 20));
        float mid = canvas.getWidth() / 2f;

        paint.setColor(Color.WHITE); paint.setTextSize(26); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Selecione uma pedra para evoluir instantaneamente!", mid, 200, paint);

        fireStoneBtn.set(mid - 350, 300, mid - 150, 420);
        drawButton(canvas, fireStoneBtn, "🔥 Pedra Fogo");

        waterStoneBtn.set(mid - 100, 300, mid + 100, 420);
        drawButton(canvas, waterStoneBtn, "🌊 Pedra Água");

        thunderStoneBtn.set(mid + 150, 300, mid + 350, 420);
        drawButton(canvas, thunderStoneBtn, "⚡ Pedra Trovão");
    }
}
