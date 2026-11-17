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
    public RectF exitBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        Companion comp = gv.companion;
        Player p = gv.player;
        float w = canvas.getWidth();
        float h = canvas.getHeight();

        drawOverlay(canvas, "MEU TIME DE POKÉMONS (MÁX 6)", Color.argb(245, 15, 25, 40));

        exitBtn.set(w - 180, 30, w - 40, 90);
        drawExitButton(canvas, exitBtn);

        paint.setColor(Color.WHITE); paint.setTextSize(20); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Companheiro Atual: " + comp.getSpecies().toUpperCase() + " (Nv." + comp.getLevel() + ")", w / 2f, 115, paint);

        List<CapturedCreature> party = p.getParty();
        float startY = 150;
        float cardH = 80;
        float spacing = 12;

        // Animação muito devagar (ritmo lento a cada 800ms)
        int slowAnimFrame = (int)((System.currentTimeMillis() / 800) % 3);

        for (int i = 0; i < party.size(); i++) {
            CapturedCreature creature = party.get(i);
            boolean isCurrent = creature.name.equalsIgnoreCase(comp.getSpecies());

            RectF card = new RectF(80, startY + (i * (cardH + spacing)), w - 80, startY + (i * (cardH + spacing)) + cardH);

            // Fundo do card
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(isCurrent ? Color.argb(180, 50, 75, 50) : Color.argb(150, 30, 45, 65));
            canvas.drawRoundRect(card, 16, 16, paint);

            // Borda
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(isCurrent ? 4 : 2);
            paint.setColor(isCurrent ? Color.GREEN : Color.YELLOW);
            canvas.drawRoundRect(card, 16, 16, paint);

            // Sprite em animação lenta (frente, slowAnimFrame)
            Bitmap icon = SpriteManager.getInstance().getUnitSprite(creature.name, 0, slowAnimFrame, creature.isShiny);
            if (icon != null) {
                canvas.drawBitmap(icon, null, new Rect((int)card.left + 25, (int)card.top + 8, (int)card.left + 105, (int)card.bottom - 8), null);
            }

            // Nome e Gênero
            int nameColor = creature.gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
            if (creature.isShiny) nameColor = Color.rgb(255, 215, 0);
            else if (isCurrent) nameColor = Color.CYAN;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(nameColor);
            paint.setTextSize(24);
            paint.setTextAlign(Paint.Align.LEFT);
            String genderIcon = creature.gender == 0 ? "♂" : "♀";
            canvas.drawText(creature.name + " " + genderIcon + (creature.isShiny ? " ✨" : ""), card.left + 130, card.top + 35, paint);

            // Raridade
            paint.setColor(creature.rarity.getColor());
            paint.setTextSize(14);
            canvas.drawText("[" + creature.rarity.getLabel() + "]", card.left + 130, card.bottom - 18, paint);

            if (isCurrent) {
                paint.setColor(Color.GREEN);
                paint.setTextSize(16);
                paint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText("⭐ ATIVO", card.right - 30, card.centerY() + 6, paint);
            }
        }
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
