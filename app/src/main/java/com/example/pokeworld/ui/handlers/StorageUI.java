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
    public RectF prevPageBtn = new RectF();
    public RectF nextPageBtn = new RectF();
    public int storagePage = 0;
    public static final int ITEMS_PER_PAGE = 7;

    @Override
    public void draw(Canvas canvas, GameView gv) {
        Player p = gv.player;
        float w = canvas.getWidth();
        float h = canvas.getHeight();
        float mid = w / 2f;

        drawOverlay(canvas, "GERENCIADOR DE POKÉMONS (PROFESSOR DE TIME)", Color.argb(245, 15, 25, 40));
        
        exitBtn.set(w - 180, 30, w - 40, 90);
        drawExitButton(canvas, exitBtn);

        paint.setColor(Color.WHITE); paint.setTextSize(20); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("Toque num Pokémon da Base para LEVAR CONSIGO, ou num do Time para DEIXAR NA BASE", mid, 120, paint);

        List<CapturedCreature> storage = p.getStorage();
        int maxPages = Math.max(1, (int) Math.ceil((double) storage.size() / ITEMS_PER_PAGE));
        if (storagePage >= maxPages) storagePage = maxPages - 1;
        if (storagePage < 0) storagePage = 0;

        // --- LADO ESQUERDO (POKÉMONS NA BASE - MODO GUARDA) ---
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.CYAN); paint.setTextSize(22);
        canvas.drawText("🏠 POKÉMONS NA BASE / GUARDA (" + storage.size() + " Total - Pág " + (storagePage + 1) + "/" + maxPages + "):", 50, 160, paint);

        float y = 200;
        float itemH = 50;

        int startIndex = storagePage * ITEMS_PER_PAGE;
        int endIndex = Math.min(storage.size(), startIndex + ITEMS_PER_PAGE);

        for (int i = startIndex; i < endIndex; i++) {
            CapturedCreature c = storage.get(i);
            
            RectF row = new RectF(50, y - 28, mid - 20, y + 22);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(120, 30, 45, 60));
            canvas.drawRoundRect(row, 10, 10, paint);

            Bitmap icon = SpriteManager.getInstance().getUnitSprite(c.name, 0, 0, c.isShiny);
            if (icon != null) {
                canvas.drawBitmap(icon, null, new Rect(60, (int)y - 26, 110, (int)y + 20), null);
            }

            int nameColor = c.gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
            if (c.isShiny) nameColor = Color.rgb(255, 215, 0);
            paint.setColor(nameColor); paint.setTextSize(19); paint.setFakeBoldText(true);
            String genderIcon = c.gender == 0 ? "♂" : "♀";
            canvas.drawText(c.name + " " + genderIcon + (c.isShiny ? " ✨" : ""), 125, y, paint);

            paint.setColor(Color.GREEN); paint.setTextSize(14); paint.setFakeBoldText(false);
            canvas.drawText("🛡️ Guarda", mid - 180, y, paint);

            paint.setColor(c.rarity.getColor()); paint.setTextSize(14);
            canvas.drawText("[" + c.rarity.getLabel() + "]", mid - 100, y, paint);

            y += itemH + 8;
        }

        // Botões de Paginação do Depósito
        prevPageBtn.set(50, h - 70, 180, h - 25);
        nextPageBtn.set(mid - 150, h - 70, mid - 20, h - 25);

        if (storagePage > 0) {
            drawButton(canvas, prevPageBtn, "◄ Anter.");
        }
        if (storagePage < maxPages - 1) {
            drawButton(canvas, nextPageBtn, "Próx. ►");
        }

        // --- LADO DIREITO (POKÉMONS CARREGADOS / PARTY) ---
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.YELLOW); paint.setTextSize(22);
        canvas.drawText("🎒 POKÉMONS CARREGADOS COMIGO (" + p.getParty().size() + "/6):", mid + 30, 160, paint);

        List<CapturedCreature> party = p.getParty();
        y = 200;

        for (int i = 0; i < party.size(); i++) {
            CapturedCreature c = party.get(i);

            RectF row = new RectF(mid + 20, y - 28, w - 50, y + 22);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(120, 60, 55, 30));
            canvas.drawRoundRect(row, 10, 10, paint);

            Bitmap icon = SpriteManager.getInstance().getUnitSprite(c.name, 0, 0, c.isShiny);
            if (icon != null) {
                canvas.drawBitmap(icon, null, new Rect((int)mid + 30, (int)y - 26, (int)mid + 80, (int)y + 20), null);
            }

            int nameColor = c.gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
            if (c.isShiny) nameColor = Color.rgb(255, 215, 0);
            paint.setColor(nameColor); paint.setTextSize(19); paint.setFakeBoldText(true);
            String genderIcon = c.gender == 0 ? "♂" : "♀";
            canvas.drawText(c.name + " " + genderIcon + (c.isShiny ? " ✨" : ""), mid + 95, y, paint);

            paint.setColor(c.rarity.getColor()); paint.setTextSize(14); paint.setFakeBoldText(false);
            canvas.drawText("[" + c.rarity.getLabel() + "]", w - 150, y, paint);

            y += itemH + 8;
        }
    }
}
