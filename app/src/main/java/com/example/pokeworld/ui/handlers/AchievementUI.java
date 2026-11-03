package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.AchievementManager;
import java.util.Set;

public class AchievementUI extends BaseUI {
    @Override
    public void draw(Canvas canvas, GameView gv) {
        AchievementManager manager = gv.achievements;
        drawOverlay(canvas, "CONQUISTAS", Color.argb(245, 60, 40, 0));
        int y = 160; paint.setTextSize(25);
        paint.setTextAlign(Paint.Align.CENTER);
        Set<String> unlocked = manager.getUnlockedAchievements();
        String[] all = {"Colecionador Iniciante", "Magnata", "Mestre Treinador", "Explorador"};
        for (String s : all) {
            boolean isOk = unlocked.contains(s);
            paint.setColor(isOk ? Color.YELLOW : Color.GRAY);
            canvas.drawText((isOk ? "✅ " : "🔒 ") + s, canvas.getWidth()/2f, y, paint);
            y += 60;
        }
    }
}
