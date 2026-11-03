package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.BestiaryManager;
import com.example.pokeworld.managers.SpriteManager;

public class BestiaryUI extends BaseUI {
    @Override
    public void draw(Canvas canvas, GameView gv) {
        BestiaryManager bestiary = gv.bestiary;
        drawOverlay(canvas, "POKÉDEX REGIONAL", Color.argb(245, 10, 30, 10));
        int x = 80, y = 180;
        
        String[] allPossible = {
            "Pikachu", "Raichu", "Charmander", "Charmeleon", "Charizard", 
            "Squirtle", "Wartortle", "Blastoise", "Bulbasaur", "Ivysaur", "Venusaur",
            "Oddish", "Caterpie", "Pidgey", "Geodude", "Graveler", "Golem",
            "Onix", "Sandshrew", "Sandslash", "Ekans", "Arbok", "Magmar", "Ponyta",
            "Lapras", "Snorlax", "Psyduck", "Magikarp", "Gengar", "Haunter", "Gastly",
            "Articuno", "RAIQUAZA", "KRAKEN", "FÊNIX DE FOGO"
        };
        
        paint.setTextSize(22);
        paint.setTextAlign(Paint.Align.LEFT);
        for (String s : allPossible) {
            boolean seen = bestiary.isSeen(s);
            boolean captured = bestiary.isCaptured(s);
            
            if (captured) {
                paint.setColor(Color.WHITE);
                Bitmap icon = SpriteManager.getInstance().getUnitSprite(s, 0, 0, true);
                if (icon != null) {
                    canvas.drawBitmap(icon, null, new Rect(x, y - 45, x + 70, y + 15), null);
                }
                canvas.drawText(s, x + 80, y, paint);
            } else if (seen) {
                paint.setColor(Color.argb(150, 200, 200, 200));
                canvas.drawText(s + " (Visto)", x + 80, y, paint);
            } else {
                paint.setColor(Color.argb(100, 150, 150, 150));
                canvas.drawText("???", x + 80, y, paint);
            }
            
            y += 75;
            if (y > canvas.getHeight() - 100) {
                y = 180;
                x += 280; 
            }
        }
    }
}
