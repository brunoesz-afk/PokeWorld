package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;

public class MountFactoryUI extends BaseUI {
    public RectF saddleFire = new RectF();
    public RectF saddleWater = new RectF();
    public RectF upgradeSpeed = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        drawOverlay(canvas, "FÁBRICA DE MONTARIA", Color.argb(245, 20, 40, 20));
        float w = canvas.getWidth();
        float mid = w / 2;

        paint.setColor(Color.WHITE); paint.setTextSize(26); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Melhore sua velocidade e fabrique equipamentos!", mid, 180, paint);

        saddleFire.set(mid - 300, 250, mid - 50, 350);
        drawButton(canvas, saddleFire, "🔥 Sela de Fogo\n(30 Essência Fogo)");

        saddleWater.set(mid + 50, 250, mid + 300, 350);
        drawButton(canvas, saddleWater, "🌊 Sela Aquática\n(30 Cristal Água)");

        upgradeSpeed.set(mid - 150, 400, mid + 150, 500);
        int currentSpeedLv = gv.player.getInventory().getOrDefault("MountSpeed_Lv", 1);
        drawButton(canvas, upgradeSpeed, "⚡ Vel. Montaria Lv." + currentSpeedLv + "\n(50 Madeiras)");
    }
}
