package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import java.util.ArrayList;
import java.util.List;

public class TeleportUI implements UIHandler {
    private Paint paint = new Paint();
    public int currentPage = 0;
    
    public static class MapPoint {
        public String name;
        public String biomeKey;
        public String icon;
        public int recLv;
        public RectF bounds;
        public int color;
        public int page;
        public int unlockLv;

        public MapPoint(String name, String key, String icon, int recLv, int color, int page, int unlockLv) {
            this.name = name; this.biomeKey = key; this.icon = icon; this.recLv = recLv; 
            this.color = color; this.page = page; this.unlockLv = unlockLv;
            this.bounds = new RectF();
        }
    }

    public List<MapPoint> points = new ArrayList<>();
    public RectF backBtn = new RectF(50, 50, 220, 120);
    public RectF nextBtn = new RectF();
    public RectF prevBtn = new RectF();

    public TeleportUI() {
        // --- PÁGINA 1: Vila Raiz (Lv. 1-10) ---
        points.add(new MapPoint("Cidade PokeWorld", "CITY", "🏙️", 1, Color.rgb(100, 200, 255), 0, 1));
        points.add(new MapPoint("Mata Viridiana", "FOREST", "🌲", 1, Color.rgb(50, 150, 50), 0, 1));
        points.add(new MapPoint("Caminho de Cascalho", "ROCKY_PATH", "⛏️", 3, Color.rgb(150, 150, 150), 0, 1));
        points.add(new MapPoint("Lagoa da Paz", "SMALL_LAKE", "💧", 4, Color.rgb(100, 180, 255), 0, 1));
        points.add(new MapPoint("Caverna de Brasa", "FIRE_CAVE", "🔥", 6, Color.rgb(255, 100, 0), 0, 1));
        points.add(new MapPoint("Esgoto da Vila", "SEWERS", "🐀", 2, Color.rgb(120, 120, 80), 0, 1));

        // --- PÁGINA 2: Reino de Bronze (Lv. 11-25) ---
        points.add(new MapPoint("Selva Tropical", "JUNGLE", "🌳", 11, Color.rgb(30, 100, 30), 1, 10));
        points.add(new MapPoint("Minas de Bronze", "MINE", "⛏️", 13, Color.rgb(180, 130, 80), 1, 12));
        points.add(new MapPoint("Foz do Rio", "RIO", "🌊", 15, Color.rgb(30, 100, 200), 1, 14));
        points.add(new MapPoint("Túneis de Lava", "MAGMA_CAVE", "🌋", 18, Color.rgb(200, 50, 0), 1, 16));
        points.add(new MapPoint("Pântano Sombrio", "SWAMP", "🍄", 20, Color.rgb(80, 100, 60), 1, 18));
        
        // --- PÁGINA 3: Terras Elementais (Lv. 26-60) ---
        points.add(new MapPoint("Costa Azul", "OCEAN", "🌊", 26, Color.rgb(30, 100, 200), 2, 25));
        points.add(new MapPoint("Deserto Árido", "DESERT", "🏜️", 35, Color.rgb(240, 200, 100), 2, 30));
        points.add(new MapPoint("Vulcão Supremo", "VOLCANO", "🌋", 45, Color.rgb(255, 50, 0), 2, 45));
        points.add(new MapPoint("Santuário de Magma", "MOLTEN_SANCTUM", "🌋", 50, Color.rgb(255, 30, 0), 2, 50));
        points.add(new MapPoint("Montanha Nevada", "SNOW_MOUNTAIN", "❄️", 55, Color.rgb(200, 240, 255), 2, 50));
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        nextBtn.set(w - 250, h/2f - 40, w - 50, h/2f + 40);
        prevBtn.set(50, h/2f - 40, 250, h/2f + 40);

        canvas.drawColor(Color.argb(240, 10, 15, 30));
        
        paint.setColor(Color.WHITE);
        paint.setTextSize(50);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        String title = "🗺️ MAPA MÚNDI - PÁGINA " + (currentPage + 1);
        canvas.drawText(title, w / 2f, 80, paint);

        float startX = 280, startY = 130;
        float btnW = 320, btnH = 140;
        float spacing = 20;
        int playerLv = gv.player.getLevel();

        int itemsOnPage = 0;
        for (MapPoint p : points) {
            if (p.page != currentPage) continue;
            
            int r = itemsOnPage / 2;
            int c = itemsOnPage % 2;
            itemsOnPage++;

            float x = startX + (c * (btnW + spacing));
            float y = startY + (r * (btnH + spacing));
            p.bounds.set(x, y, x + btnW, y + btnH);

            boolean locked = playerLv < p.unlockLv;

            paint.setColor(locked ? Color.argb(100, 50, 50, 50) : Color.argb(180, 30, 40, 70));
            canvas.drawRoundRect(p.bounds, 15, 15, paint);
            
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(locked ? Color.GRAY : p.color);
            canvas.drawRoundRect(p.bounds, 15, 15, paint);
            paint.setStyle(Paint.Style.FILL);

            if (locked) {
                paint.setColor(Color.WHITE); paint.setTextSize(30);
                canvas.drawText("🔒 Lv." + p.unlockLv, p.bounds.centerX(), p.bounds.centerY() + 10, paint);
            } else {
                paint.setColor(Color.WHITE); paint.setTextSize(40);
                canvas.drawText(p.icon, p.bounds.centerX(), p.bounds.top + 55, paint);
                paint.setTextSize(24);
                canvas.drawText(p.name, p.bounds.centerX(), p.bounds.top + 90, paint);
                paint.setTextSize(18);
                canvas.drawText("Lv. " + p.recLv, p.bounds.centerX(), p.bounds.top + 120, paint);
            }
        }

        if (currentPage < 2) drawNavBtn(canvas, nextBtn, "PRÓXIMO ➡️");
        if (currentPage > 0) drawNavBtn(canvas, prevBtn, "⬅️ ANTERIOR");

        paint.setColor(Color.RED);
        canvas.drawRoundRect(backBtn, 10, 10, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(25);
        canvas.drawText("⬅️ VOLTAR", backBtn.centerX(), backBtn.centerY() + 10, paint);
    }

    private void drawNavBtn(Canvas canvas, RectF r, String label) {
        paint.setColor(Color.argb(200, 50, 50, 80));
        canvas.drawRoundRect(r, 10, 10, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, r.centerX(), r.centerY() + 8, paint);
    }
}
