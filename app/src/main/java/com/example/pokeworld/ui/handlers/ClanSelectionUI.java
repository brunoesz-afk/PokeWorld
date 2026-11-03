package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;

public class ClanSelectionUI extends BaseUI {
    public RectF rubiBtn = new RectF(300, 200, 900, 270);
    public RectF safiraBtn = new RectF(300, 290, 900, 360);
    public RectF esmeraldaBtn = new RectF(300, 380, 900, 450);

    @Override
    public void draw(Canvas canvas, GameView gv) {
        drawOverlay(canvas, "ESCOLHA SEU CLÃ", Color.argb(245, 0, 0, 0));
        paint.setColor(Color.WHITE); paint.setTextSize(25);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Bônus de +15% para pets do elemento do clã.", canvas.getWidth()/2f, 130, paint);
        drawButton(canvas, rubiBtn, "CLÃ RUBI (FOGO)");
        drawButton(canvas, safiraBtn, "CLÃ SAFIRA (ÁGUA)");
        drawButton(canvas, esmeraldaBtn, "CLÃ ESMERALDA (PLANTA)");
    }
}
