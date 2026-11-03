package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;

public class GymUI extends BaseUI {
    public RectF startBattleBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        drawOverlay(canvas, "GINÁSIO DA CIDADE", Color.argb(245, 60, 10, 60));
        float w = canvas.getWidth();
        float mid = w / 2;

        paint.setColor(Color.WHITE); paint.setTextSize(26); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Desafie o Líder para ganhar a Insígnia!", mid, 220, paint);

        startBattleBtn.set(mid - 200, 300, mid + 200, 420);
        drawButton(canvas, startBattleBtn, "⚔️ DESAFIAR LÍDER");
        
        if (gv.player.getInventory().containsKey("Insígnia da Rocha")) {
            canvas.drawText("🏆 VOCÊ JÁ POSSUI ESTA INSÍGNIA!", mid, 500, paint);
        }
    }
}
