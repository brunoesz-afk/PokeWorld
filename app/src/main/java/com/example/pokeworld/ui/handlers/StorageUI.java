package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Player;
import java.util.List;

public class StorageUI extends BaseUI {
    @Override
    public void draw(Canvas canvas, GameView gv) {
        Player p = gv.player;
        drawOverlay(canvas, "GERENCIAMENTO DE TIME", Color.argb(245, 20, 25, 30));
        paint.setColor(Color.WHITE); paint.setTextSize(22); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("LADO ESQUERDO: DEPÓSITO (BASE) | LADO DIREITO: SEU TIME (PARTY)", canvas.getWidth()/2f, 130, paint);
        
        float w = canvas.getWidth();
        float mid = w / 2;

        // --- LADO ESQUERDO (STORAGE) ---
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.LTGRAY); paint.setTextSize(25);
        canvas.drawText("🏠 NO DEPÓSITO:", 80, 180, paint);
        
        int y = 230;
        List<CapturedCreature> storage = p.getStorage();
        for (int i = 0; i < Math.min(storage.size(), 10); i++) {
            CapturedCreature c = storage.get(i);
            int nameColor = c.gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
            if (c.isShiny) nameColor = Color.rgb(255, 215, 0);
            
            paint.setColor(nameColor);
            String genderIcon = c.gender == 0 ? "♂" : "♀";
            String shLabel = c.isShiny ? " [SH]" : "";
            canvas.drawText("• " + c.name + " " + genderIcon + shLabel + " (" + c.rarity.getLabel() + ")", 100, y, paint);
            y += 45;
        }

        // --- LADO DIREITO (PARTY) ---
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.YELLOW);
        canvas.drawText("🎒 NO TIME (MÁX 6):", mid + 50, 180, paint);
        
        y = 230;
        List<CapturedCreature> party = p.getParty();
        for (int i = 0; i < party.size(); i++) {
            CapturedCreature c = party.get(i);
            int nameColor = c.gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
            if (c.isShiny) nameColor = Color.rgb(255, 215, 0);
            
            paint.setColor(nameColor);
            String genderIcon = c.gender == 0 ? "♂" : "♀";
            String shLabel = c.isShiny ? " [SH]" : "";
            canvas.drawText("• " + c.name + " " + genderIcon + shLabel + " [" + c.rarity.getLabel() + "]", mid + 70, y, paint);
            y += 45;
        }
        
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE); paint.setTextSize(18);
        canvas.drawText("Toque em um Pokémon para trocá-lo de lugar", canvas.getWidth()/2f, canvas.getHeight() - 80, paint);
    }
}
