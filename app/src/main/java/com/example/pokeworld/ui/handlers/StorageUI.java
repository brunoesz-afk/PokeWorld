package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.managers.SpriteManager;

import java.util.List;

public class StorageUI extends BaseUI {
    public RectF exitBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        Player p = gv.player;
        float w = canvas.getWidth();
        float mid = w / 2f;

        drawOverlay(canvas, "GERENCIADOR DE POKÉMONS (PROFESSOR DE TIME)", Color.argb(245, 15, 25, 40));
        
        exitBtn.set(w - 180, 30, w - 40, 90);
        drawExitButton(canvas, exitBtn);

        paint.setColor(Color.WHITE); paint.setTextSize(20); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Toque num Pokémon da Base para LEVAR CONSIGO, ou num do Time para DEIXAR NA BASE", mid, 120, paint);

        // --- LADO ESQUERDO (POKÉMONS NA BASE) ---
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.CYAN); paint.setTextSize(24);
        canvas.drawText("🏠 POKÉMONS GUARDADOS NA BASE:", 60, 165, paint);

        List<CapturedCreature> storage = p.getStorage();
        float y = 210;
        float itemH = 50;

        for (int i = 0; i < Math.min(storage.size(), 8); i++) {
            CapturedCreature c = storage.get(i);
            
            // Fundo da linha
            RectF row = new RectF(50, y - 30, mid - 20, y + 20);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(120, 30, 45, 60));
            canvas.drawRoundRect(row, 10, 10, paint);

            // Ícone do Sprite
            Bitmap icon = SpriteManager.getInstance().getUnitSprite(c.name, 0, 0, c.isShiny);
            if (icon != null) {
                canvas.drawBitmap(icon, null, new Rect(60, (int)y - 28, 110, (int)y + 18), null);
            }

            int nameColor = c.gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
            if (c.isShiny) nameColor = Color.rgb(255, 215, 0);
            paint.setColor(nameColor); paint.setTextSize(20); paint.setFakeBoldText(true);
            String genderIcon = c.gender == 0 ? "♂" : "♀";
            canvas.drawText(c.name + " " + genderIcon + (c.isShiny ? " ✨" : ""), 125, y, paint);

            paint.setColor(c.rarity.getColor()); paint.setTextSize(16); paint.setFakeBoldText(false);
            canvas.drawText("[" + c.rarity.getLabel() + "]", mid - 140, y, paint);

            y += itemH + 10;
        }

        // --- LADO DIREITO (POKÉMONS CARREGADOS / PARTY) ---
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.YELLOW); paint.setTextSize(24);
        canvas.drawText("🎒 POKÉMONS CARREGADOS COMIGO (" + p.getParty().size() + "/6):", mid + 30, 165, paint);

        List<CapturedCreature> party = p.getParty();
        y = 210;

        for (int i = 0; i < party.size(); i++) {
            CapturedCreature c = party.get(i);

            // Fundo da linha
            RectF row = new RectF(mid + 20, y - 30, w - 50, y + 20);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(120, 60, 55, 30));
            canvas.drawRoundRect(row, 10, 10, paint);

            // Ícone do Sprite
            Bitmap icon = SpriteManager.getInstance().getUnitSprite(c.name, 0, 0, c.isShiny);
            if (icon != null) {
                canvas.drawBitmap(icon, null, new Rect((int)mid + 30, (int)y - 28, (int)mid + 80, (int)y + 18), null);
            }

            int nameColor = c.gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
            if (c.isShiny) nameColor = Color.rgb(255, 215, 0);
            paint.setColor(nameColor); paint.setTextSize(20); paint.setFakeBoldText(true);
            String genderIcon = c.gender == 0 ? "♂" : "♀";
            canvas.drawText(c.name + " " + genderIcon + (c.isShiny ? " ✨" : ""), mid + 95, y, paint);

            paint.setColor(c.rarity.getColor()); paint.setTextSize(16); paint.setFakeBoldText(false);
            canvas.drawText("[" + c.rarity.getLabel() + "]", w - 170, y, paint);

            y += itemH + 10;
        }
    }
}
