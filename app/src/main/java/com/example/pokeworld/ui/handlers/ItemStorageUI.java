package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import java.util.Map;

public class ItemStorageUI extends BaseUI {
    public RectF depositAllBtn = new RectF();
    public RectF withdrawAllBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        drawOverlay(canvas, "BAÚ DE ARMAZENAMENTO", Color.argb(245, 40, 30, 20));
        float mid = canvas.getWidth() / 2f;
        
        paint.setColor(Color.WHITE); paint.setTextSize(24); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("🎒 SEU INVENTÁRIO:", 80, 180, paint);
        
        int y = 230;
        for (Map.Entry<String, Integer> entry : gv.player.getInventory().entrySet()) {
            if (y > canvas.getHeight() - 150) break;
            canvas.drawText("• " + entry.getKey() + " x" + entry.getValue(), 100, y, paint);
            y += 35;
        }

        canvas.drawText("📦 NO BAÚ:", mid + 50, 180, paint);
        y = 230;
        for (Map.Entry<String, Integer> entry : gv.player.getStorageItems().entrySet()) {
            if (y > canvas.getHeight() - 150) break;
            canvas.drawText("• " + entry.getKey() + " x" + entry.getValue(), mid + 70, y, paint);
            y += 35;
        }

        depositAllBtn.set(100, canvas.getHeight() - 120, mid - 50, canvas.getHeight() - 50);
        drawButton(canvas, depositAllBtn, "📥 GUARDAR TUDO");

        withdrawAllBtn.set(mid + 50, canvas.getHeight() - 120, canvas.getWidth() - 100, canvas.getHeight() - 50);
        drawButton(canvas, withdrawAllBtn, "📤 PEGAR TUDO");
    }
}
