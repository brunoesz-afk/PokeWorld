package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;

public class ForgeUI extends BaseUI {
    public RectF upgradeAttackBtn = new RectF(200, 150, 600, 220);
    public RectF craftVitalBtn = new RectF(200, 240, 600, 310);
    public RectF craftRockBtn = new RectF(700, 240, 1100, 310);
    public RectF craftSolarBtn = new RectF(200, 330, 600, 400);
    public RectF craftDragonBtn = new RectF(700, 330, 1100, 400);

    @Override
    public void draw(Canvas canvas, GameView gv) {
        drawOverlay(canvas, "FORJA DO REINO", Color.argb(245, 40, 40, 40));
        paint.setColor(Color.WHITE); paint.setTextSize(25);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Use materiais de Boss para forjar itens lendários!", canvas.getWidth()/2f, 130, paint);
        drawButton(canvas, upgradeAttackBtn, "UPGRADE ATK (5 Materiais)");
        drawButton(canvas, craftVitalBtn, "LÂMINA VITAL (1 Essência)");
        drawButton(canvas, craftRockBtn, "ARMADURA ROCHA (1 Núcleo)");
        drawButton(canvas, craftSolarBtn, "CAJADO SOLAR (1 Relíquia)");
        drawButton(canvas, craftDragonBtn, "COURAÇA DRAGÃO (1 Escama)");
    }
}
