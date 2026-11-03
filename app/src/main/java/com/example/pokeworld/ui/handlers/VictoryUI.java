package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.pokeworld.core.GameView;

public class VictoryUI extends BaseUI {
    @Override
    public void draw(Canvas canvas, GameView gv) {
        paint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        paint.setColor(Color.YELLOW);
        paint.setTextSize(60);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("🏆 VITÓRIA SUPREMA! 🏆", canvas.getWidth()/2f, 200, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(30);
        canvas.drawText("Você derrotou o Raiquaza e salvou o mundo!", canvas.getWidth()/2f, 300, paint);
        canvas.drawText("Pressione qualquer lugar para continuar sua jornada...", canvas.getWidth()/2f, 500, paint);
    }
}
