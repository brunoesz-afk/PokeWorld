package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;

public class InventoryUI extends BaseUI {
    @Override
    public void draw(Canvas canvas, GameView gv) {
        Player p = gv.player;
        drawOverlay(canvas, "INVENTÁRIO", Color.argb(245, 10, 10, 10));
        
        android.graphics.RectF exitBtn = new android.graphics.RectF(canvas.getWidth() - 200, 40, canvas.getWidth() - 40, 110);
        drawExitButton(canvas, exitBtn);

        int y = 160; paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        for (String item : p.getInventory().keySet()) {
            paint.setColor(item.equals("Poção") ? Color.GREEN : Color.WHITE);
            canvas.drawText(item + " x" + p.getInventory().get(item), canvas.getWidth()/2f, y, paint);
            y += 60;
        }
    }
}
