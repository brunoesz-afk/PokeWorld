package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.Player;

public class RuneUI extends BaseUI {
    @Override
    public void draw(Canvas canvas, GameView gv) {
        Player p = gv.player;
        Companion comp = gv.companion;
        drawOverlay(canvas, "SISTEMA DE RUNAS", Color.argb(245, 0, 20, 60));
        paint.setColor(Color.WHITE); paint.setTextSize(25); paint.setTextAlign(Paint.Align.CENTER);
        String runeName = comp.getRune() != null ? comp.getRune().getName() : "Nenhuma";
        canvas.drawText("Runa Equipada: " + runeName, canvas.getWidth()/2f, 130, paint);
        int y = 200;
        for (String item : p.getInventory().keySet()) {
            if (item.contains("Runa")) {
                paint.setColor(Color.CYAN);
                canvas.drawText(item + " (Toque para equipar)", canvas.getWidth()/2f, y, paint);
                y += 50;
            }
        }
    }
}
