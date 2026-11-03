package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.SpriteManager;

public class PlacementUI extends BaseUI {
    public RectF confirmBtn = new RectF();
    public RectF cancelBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth();
        float h = canvas.getHeight();
        
        // --- INSTRUÇÕES ---
        paint.setColor(Color.argb(180, 0, 0, 0));
        canvas.drawRect(0, 0, w, 100, paint);
        paint.setColor(Color.WHITE); paint.setTextSize(30); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ARRASTE PARA POSICIONAR: " + gv.ui.placementBuildingName.toUpperCase(), w/2f, 60, paint);

        // --- BOTÕES ---
        confirmBtn.set(w - 250, h - 120, w - 50, h - 40);
        drawButton(canvas, confirmBtn, "✅ CONFIRMAR");
        
        cancelBtn.set(50, h - 120, 250, h - 40);
        drawButton(canvas, cancelBtn, "❌ CANCELAR");

        // --- GHOST BUILDING ---
        drawGhostBuilding(canvas, gv);
    }

    private void drawGhostBuilding(Canvas canvas, GameView gv) {
        String key = gv.ui.placementBuildingName.toLowerCase();
        if (key.contains("oficina")) key = "oficina_pokebola";
        else if (key.contains("daycare")) key = "fabrica_montaria";
        else if (key.contains("laboratorio")) key = "laboratorio";
        else if (key.contains("montaria")) key = "fabrica_montaria";

        Bitmap sprite = SpriteManager.getInstance().getBuildingSprite(key);
        if (sprite != null) {
            float size = 320; // Tamanho visual do ghost
            float screenX = (float) (gv.ui.placementX - gv.world.getCameraX());
            float screenY = (float) (gv.ui.placementY - gv.world.getCameraY());
            
            paint.setAlpha(150);
            canvas.drawBitmap(sprite, null, new RectF(screenX - size/2, screenY - size/2, screenX + size/2, screenY + size/2), paint);
            paint.setAlpha(255);
            
            // Desenha borda indicando colisão
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3);
            boolean canPlace = !gv.world.isCollidingWithBuilding(gv.ui.placementX, gv.ui.placementY);
            paint.setColor(canPlace ? Color.GREEN : Color.RED);
            canvas.drawRect(screenX - size/2, screenY - size/2, screenX + size/2, screenY + size/2, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }
}
