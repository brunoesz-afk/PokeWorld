package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.managers.BreedingManager;
import java.util.List;

public class BreedingUI extends BaseUI {
    public RectF selectParentBtn = new RectF(300, 200, 900, 300);
    public RectF collectEggBtn = new RectF(300, 350, 900, 450);
    public boolean selectingParent = false;
    public int parentSlot = 0;

    @Override
    public void draw(Canvas canvas, GameView gv) {
        BreedingManager manager = gv.breeding;
        Player p = gv.player;
        drawOverlay(canvas, "DAYCARE - CRUZAMENTO", Color.argb(245, 0, 40, 20));
        paint.setColor(Color.WHITE); paint.setTextSize(25); paint.setTextAlign(Paint.Align.CENTER);
        
        if (selectingParent) {
            canvas.drawText("Selecione o Genitor " + (parentSlot + 1) + ":", canvas.getWidth()/2f, 130, paint);
            int y = 200;
            List<CapturedCreature> collection = p.getCreatureCollection();
            for (int i = 0; i < collection.size(); i++) {
                CapturedCreature creature = collection.get(i);
                paint.setColor(creature.isShiny ? Color.rgb(255, 215, 0) : Color.WHITE);
                canvas.drawText(creature.name + (creature.isShiny ? " ✨" : ""), canvas.getWidth()/2f, y, paint);
                y += 50;
            }
            return;
        }

        if (!manager.isDaycareFull()) {
            canvas.drawText("Deixe dois pets para gerarem um ovo!", canvas.getWidth()/2f, 150, paint);
            drawButton(canvas, selectParentBtn, "SELECIONAR PAIS");
        } else if (manager.hasEgg()) {
            canvas.drawText("🥚 O ovo está pronto para ser coletado!", canvas.getWidth()/2f, 150, paint);
            drawButton(canvas, collectEggBtn, "COLETAR OVO");
        } else {
            canvas.drawText("Cruzamento em progresso... Explore o mundo!", canvas.getWidth()/2f, 150, paint);
        }
    }
}
