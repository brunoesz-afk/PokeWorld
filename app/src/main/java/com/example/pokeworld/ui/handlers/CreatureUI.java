package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.entities.Rarity;
import com.example.pokeworld.managers.SpriteManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreatureUI extends BaseUI {
    public RectF evolveBtn = new RectF(400, 560, 800, 630);
    public RectF upgradeBtn = new RectF(820, 470, 1150, 540);
    public RectF exitBtn = new RectF(1080, 40, 1240, 110);

    @Override
    public void draw(Canvas canvas, GameView gv) {
        Companion comp = gv.companion;
        Player p = gv.player;
        drawOverlay(canvas, "MEU TIME (MÁX 6)", Color.argb(245, 0, 15, 45));
        paint.setColor(Color.YELLOW); paint.setTextSize(30);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("ATIVO: " + comp.getSpecies().toUpperCase() + " (Nv." + comp.getLevel() + ")", canvas.getWidth()/2f, 130, paint);
        int y = 200;
        List<CapturedCreature> party = p.getParty();
        for (int i = 0; i < party.size(); i++) {
            CapturedCreature creature = party.get(i);
            boolean isCurrent = creature.name.equalsIgnoreCase(comp.getSpecies());
            Bitmap icon = SpriteManager.getInstance().getUnitSprite(creature.name, 0, 0, creature.isShiny);
            
            if (icon != null) {
                Rect dest = new Rect((int)(canvas.getWidth() * 0.30f), y - 40, (int)(canvas.getWidth() * 0.37f), y + 10);
                canvas.drawBitmap(icon, null, dest, null);
            }

            paint.setColor(creature.rarity.getColor());
            paint.setTextSize(24);
            paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("[" + creature.rarity.getLabel() + "]", canvas.getWidth() * 0.38f, y, paint);

            int nameColor = creature.gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
            if (creature.isShiny) nameColor = Color.rgb(255, 215, 0);
            else if (isCurrent) nameColor = Color.CYAN;
            
            paint.setColor(nameColor);
            paint.setTextSize(32);
            String genderIcon = creature.gender == 0 ? "♂" : "♀";
            canvas.drawText(creature.name + " " + genderIcon + (creature.isShiny ? " ✨" : ""), canvas.getWidth() * 0.52f, y, paint);
            
            y += 60;
        }

        if (!comp.isShiny() && p.getInventory().containsKey("Cristal") && comp.getSpecies().equalsIgnoreCase("Lobo")) {
            drawButton(canvas, evolveBtn, "EVOLUIR LOBO (1 CRISTAL)");
        }

        if (hasPossibleUpgrade(party)) {
            drawButton(canvas, upgradeBtn, "UPGRADE (4 IGUAIS)");
        }
        
        drawExitButton(canvas, exitBtn);
    }

    private boolean hasPossibleUpgrade(List<CapturedCreature> collection) {
        Map<String, Integer> counts = new HashMap<>();
        for (CapturedCreature c : collection) {
            if (c.rarity != Rarity.LEGENDARY) {
                String key = c.name + "_" + c.rarity.name() + "_" + (c.isShiny ? "SH" : "NORM");
                counts.put(key, counts.getOrDefault(key, 0) + 1);
                if (counts.get(key) >= 4) return true;
            }
        }
        return false;
    }
}
