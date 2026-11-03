package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.managers.SpriteManager;
import com.example.pokeworld.ui.UIManager;
import java.util.List;

public class DexUI extends BaseUI {
    public CapturedCreature selectedCreatureForDex;
    public int dexScrollOffset = 0;
    public RectF closeBtn = new RectF();
    public RectF searchBar = new RectF();

    public RectF nextBtn = new RectF();
    public RectF prevBtn = new RectF();
    public String searchQuery = "";

    @Override
    public void draw(Canvas canvas, GameView gv) {
        if (selectedCreatureForDex == null) {
            gv.ui.state = UIManager.UIState.CREATURES;
            return;
        }

        int w = canvas.getWidth();
        int h = canvas.getHeight();
        
        Bitmap bg = SpriteManager.getInstance().getMiscSprite("pokedex");
        if (bg != null) {
            canvas.drawBitmap(bg, null, new Rect(0, 0, w, h), null);
        } else {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(30, 35, 45));
            canvas.drawRoundRect(new RectF(50, 50, w - 50, h - 50), 25, 25, paint);
        }

        // Botão Fechar (SAIR)
        drawExitButton(canvas, closeBtn);

        // Contador Discovered/Total
        paint.setColor(Color.WHITE); paint.setTextSize(22); paint.setTextAlign(Paint.Align.RIGHT);
        int discovered = gv.bestiary.getSeenCreatures().size();
        canvas.drawText(discovered + " / 151", w * 0.88f, h * 0.09f, paint);
        
        // --- PAINEL ESQUERDO (LISTA) ---
        drawDexLeftPanel(canvas, gv.player.getCreatureCollection(), w, h);

        // --- PAINEL CENTRAL (VISUALIZAÇÃO) ---
        drawDexCenterPanel(canvas, selectedCreatureForDex, w, h);

        // --- PAINEL DIREITO (INFO) ---
        drawDexRightPanel(canvas, selectedCreatureForDex, w, h, gv);
    }

    private void drawDexLeftPanel(Canvas canvas, List<CapturedCreature> collection, int w, int h) {
        // Barra de Pesquisa
        searchBar.set(w * 0.04f, h * 0.15f, w * 0.31f, h * 0.22f);
        paint.setColor(Color.WHITE); paint.setTextSize(20); paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(searchQuery.isEmpty() ? "Pesquisar..." : searchQuery, w * 0.06f, h * 0.195f, paint);
        
        // Lista de Pokémons
        float startY = h * 0.25f;
        float itemH = h * 0.12f;
        float spacing = h * 0.02f;

        int drawn = 0;
        for (int i = 0; i < collection.size() && drawn < 5; i++) {
            CapturedCreature c = collection.get(i);
            if (!searchQuery.isEmpty() && !c.name.toLowerCase().contains(searchQuery.toLowerCase())) continue;

            boolean isSelected = c == selectedCreatureForDex;
            float y = startY + drawn * (itemH + spacing);
            
            if (isSelected) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(4);
                paint.setColor(Color.rgb(180, 40, 40));
                canvas.drawRoundRect(new RectF(w * 0.04f, y, w * 0.31f, y + itemH), 15, 15, paint);
            }

            // Ícone pequeno
            Bitmap icon = SpriteManager.getInstance().getUnitSprite(c.name, 0, 0, c.isShiny);
            if (icon != null) {
                canvas.drawBitmap(icon, null, new Rect((int)(w * 0.05f), (int)(y + 10), (int)(w * 0.11f), (int)(y + itemH - 10)), null);
            }

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.LTGRAY); paint.setTextSize(16);
            canvas.drawText("No. [00" + (i + 1) + "]", w * 0.12f, y + 35, paint);
            
            int nameColor = c.gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
            if (c.isShiny) nameColor = Color.rgb(255, 215, 0);
            paint.setColor(nameColor);
            paint.setTextSize(20);
            String genderIcon = c.gender == 0 ? "♂" : "♀";
            canvas.drawText(c.name + " " + genderIcon, w * 0.12f, y + 65, paint);
            drawn++;
        }
    }

    private void drawDexCenterPanel(Canvas canvas, CapturedCreature c, int w, int h) {
        // Setas de Navegação (Ajustadas para as setas na imagem)
        prevBtn.set(w * 0.35f, h * 0.45f, w * 0.38f, h * 0.55f);
        nextBtn.set(w * 0.62f, h * 0.45f, w * 0.65f, h * 0.55f);

        // Sprite Central (Tamanho aumentado para melhor visualização)
        Bitmap sprite = SpriteManager.getInstance().getUnitSprite(c.name, 0, 0, c.isShiny);
        if (sprite != null) {
            float size = w * 0.22f;
            canvas.drawBitmap(sprite, null, new Rect((int)(w * 0.40f), (int)(h * 0.32f), (int)(w * 0.40f + size), (int)(h * 0.32f + size)), null);
        }

        // Círculos de Tipos (Topo central)
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setColor(Color.WHITE);
        float circleX = w * 0.54f;
        for (int i = 0; i < 3; i++) {
            canvas.drawCircle(circleX + i * 35, h * 0.11f, 12, paint);
        }
    }

    private void drawDexRightPanel(Canvas canvas, CapturedCreature c, int w, int h, GameView gv) {
        float startX = w * 0.68f;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setStyle(Paint.Style.FILL);
        
        // Nome e Gênero (No topo da área de detalhes)
        int nameColor = c.gender == 0 ? Color.rgb(100, 150, 255) : Color.rgb(255, 150, 200);
        if (c.isShiny) nameColor = Color.rgb(255, 215, 0);
        paint.setColor(nameColor);
        paint.setTextSize(32);
        String genderIcon = c.gender == 0 ? "♂" : "♀";
        canvas.drawText(c.name.toUpperCase() + " " + genderIcon, startX, h * 0.11f, paint);

        // Tipos (Badge formatada)
        paint.setColor(Color.WHITE); paint.setTextSize(18);
        canvas.drawText("TIPOS: [NORMAL]", startX, h * 0.17f, paint);
        
        int count = gv.activity.dbHelper.getCaptureCount(c.name);
        canvas.drawText("CAPTURADOS: " + count, startX, h * 0.22f, paint);

        // Valores de exemplo baseados na espécie
        int[] values = getStatsForSpecies(c.name);
        float yStart = h * 0.355f;
        float rowSpacing = h * 0.082f;

        for (int i = 0; i < 6; i++) {
            float rowY = yStart + i * rowSpacing;
            // Estrelas (Alinhadas com o grid da imagem)
            drawStatStars(canvas, w * 0.78f, rowY - 10, values[i]);
            
            // Valor numérico
            paint.setColor(Color.WHITE);
            paint.setTextSize(18);
            canvas.drawText("[" + (values[i] * 20) + "]", w * 0.92f, rowY, paint);
        }

        // Nível Necessário
        paint.setColor(Color.WHITE); paint.setTextSize(26);
        canvas.drawText("Nv. 80", startX + 10, h * 0.93f, paint);
    }

    private int[] getStatsForSpecies(String name) {
        String n = name.toUpperCase();
        if (n.contains("PIKACHU")) return new int[]{2, 3, 3, 2, 2, 4};
        if (n.contains("CHARIZARD")) return new int[]{4, 4, 5, 3, 3, 5};
        if (n.contains("BLASTOISE")) return new int[]{4, 4, 4, 5, 5, 3};
        if (n.contains("VENUSAUR")) return new int[]{4, 4, 4, 4, 4, 3};
        return new int[]{3, 3, 3, 3, 3, 3};
    }
}
