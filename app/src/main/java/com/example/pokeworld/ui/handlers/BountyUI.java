package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.BountyManager;

public class BountyUI extends BaseUI {
    @Override
    public void draw(Canvas canvas, GameView gv) {
        BountyManager bm = gv.bounties;
        drawOverlay(canvas, "MURAL DE CAÇADAS", Color.argb(245, 40, 20, 10));
        int y = 160;
        paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        for (BountyManager.Bounty b : bm.getActiveBounties()) {
            paint.setColor(b.completed ? Color.GREEN : Color.WHITE);
            canvas.drawText("🎯 " + b.targetName + ": " + b.current + "/" + b.required + " (+" + b.rewardGems + " 💎)", canvas.getWidth()/2f, y, paint);
            y += 60;
        }
    }
}
