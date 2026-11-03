package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;

public class AincradUI extends BaseUI {
    public RectF startBtn = new RectF();
    public RectF checkpointBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        drawOverlay(canvas, "CASTELO FLUTUANTE AINCRAD", Color.argb(245, 10, 10, 40));
        float mid = canvas.getWidth() / 2f;
        
        int max = gv.aincrad.getMaxFloorReached();
        int checkpoint = ((max - 1) / 5) * 5 + 1;
        if (checkpoint < 1) checkpoint = 1;

        paint.setColor(Color.WHITE); paint.setTextSize(26); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Suba os andares e enfrente desafios crescentes!", mid, 200, paint);
        canvas.drawText("Recorde: Andar " + max, mid, 250, paint);

        startBtn.set(mid - 250, 300, mid + 250, 380);
        drawButton(canvas, startBtn, "🏰 COMEÇAR DO ANDAR 1");

        if (checkpoint > 1) {
            checkpointBtn.set(mid - 250, 400, mid + 250, 480);
            drawButton(canvas, checkpointBtn, "💾 VOLTAR AO CHECKPOINT (F" + checkpoint + ")");
        }
    }
}
