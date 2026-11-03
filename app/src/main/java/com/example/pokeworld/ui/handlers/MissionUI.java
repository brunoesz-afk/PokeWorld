package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Mission;
import com.example.pokeworld.entities.Player;

public class MissionUI extends BaseUI {
    @Override
    public void draw(Canvas canvas, GameView gv) {
        Player p = gv.player;
        drawOverlay(canvas, "MISSÕES ATIVAS", Color.argb(245, 0, 40, 10));
        int y = 160; paint.setTextSize(28);
        for (Mission m : p.getActiveMissions()) {
            paint.setColor(m.isCompleted() ? Color.GREEN : Color.WHITE);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("• " + m.getTitle(), 150, y, paint);
            paint.setColor(Color.LTGRAY); paint.setTextSize(22);
            canvas.drawText(m.getDescription(), 170, y + 30, paint);
            paint.setColor(Color.YELLOW); paint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(m.getStatusText(), canvas.getWidth() - 150, y + 15, paint);
            y += 100;
        }
    }
}
