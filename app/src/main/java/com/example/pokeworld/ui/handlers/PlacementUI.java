package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;

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
        canvas.drawText("TOQUE NO CHÃO PARA PAVIMENTAR (2 Pedras)", w/2f, 60, paint);

        // --- BOTÕES ---
        confirmBtn.set(w - 250, h - 120, w - 50, h - 40);
        drawButton(canvas, confirmBtn, "✅ CONFIRMAR");
        
        cancelBtn.set(50, h - 120, 250, h - 40);
        drawButton(canvas, cancelBtn, "❌ CANCELAR");

        // --- GHOST BUILDING ---
        drawGhostBuilding(canvas, gv);
    }

    /** Quadradinho do tile sob o dedo (verde = pode pavimentar). */
    private void drawGhostBuilding(Canvas canvas, GameView gv) {
        float z = gv.world.getCurrentZoom();
        int tx = (int) Math.floor(gv.ui.placementX / 64.0), ty = (int) Math.floor(gv.ui.placementY / 64.0);
        float sx = (float) ((tx * 64.0 - gv.world.getCameraX()) * z), sy = (float) ((ty * 64.0 - gv.world.getCameraY()) * z), sz = 64f * z;
        boolean ok = true;
        if (gv.world.tileMap.getMapName().equals("HOME")) {
            ok = com.example.pokeworld.world.BaseLayout.inPlot(tx, ty, 1, 1) && (gv.world.base == null || gv.world.base.pieceAt(tx, ty) == null);
        }
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ok ? Color.argb(90, 80, 255, 80) : Color.argb(90, 255, 70, 70));
        canvas.drawRect(sx, sy, sx + sz, sy + sz, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(ok ? Color.GREEN : Color.RED);
        canvas.drawRect(sx, sy, sx + sz, sy + sz, paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
