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
        points.add(new MapPoint("Santuário (Iniciante)", "MOLTEN_SANCTUM_BEGINNER", "🌋", 10, Color.rgb(255, 120, 0), 0, 10));
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

        // --- Cópias dos mapas de cada faixa (mesmo visual, layout espelhado, outros Pokémon e um pouco mais fortes) ---
        points.add(new MapPoint("Bosque Encantado", "FOREST#2", "🍃", 2, Color.rgb(90, 190, 90), 0, 1));
        points.add(new MapPoint("Esgoto Profundo", "SEWERS#2", "🐀", 3, Color.rgb(140, 140, 90), 0, 1));
        points.add(new MapPoint("Trilha dos Mineiros", "ROCKY_PATH#2", "⛏️", 4, Color.rgb(170, 170, 170), 0, 1));
        points.add(new MapPoint("Riacho Cristalino", "SMALL_LAKE#2", "💧", 5, Color.rgb(120, 200, 255), 0, 1));
        points.add(new MapPoint("Gruta Ardente", "FIRE_CAVE#2", "🔥", 7, Color.rgb(255, 130, 30), 0, 1));
        points.add(new MapPoint("Selva Profunda", "JUNGLE#2", "🌴", 12, Color.rgb(40, 130, 40), 1, 10));
        points.add(new MapPoint("Minas Abandonadas", "MINE#2", "⛏️", 14, Color.rgb(200, 150, 100), 1, 12));
        points.add(new MapPoint("Delta do Rio", "RIO#2", "🌊", 16, Color.rgb(50, 130, 220), 1, 14));
        points.add(new MapPoint("Câmara de Magma", "MAGMA_CAVE#2", "🌋", 19, Color.rgb(220, 70, 20), 1, 16));
        points.add(new MapPoint("Brejo Maldito", "SWAMP#2", "🍄", 21, Color.rgb(100, 120, 80), 1, 18));
        points.add(new MapPoint("Mar Aberto", "OCEAN#2", "🌊", 27, Color.rgb(40, 120, 230), 2, 25));
        points.add(new MapPoint("Dunas Ardentes", "DESERT#2", "🏜️", 36, Color.rgb(250, 210, 120), 2, 30));
        points.add(new MapPoint("Cratera Vulcânica", "VOLCANO#2", "🌋", 46, Color.rgb(255, 80, 20), 2, 45));
        points.add(new MapPoint("Geleira", "SNOW_MOUNTAIN#2", "🧊", 56, Color.rgb(210, 245, 255), 2, 50));
    }

    public static final int PAGE_COUNT = 3;
    private static final int[] PAGE_MIN_LV = {1, 11, 26};
    private static final String[] PAGE_NAME = {"VILA RAIZ (Lv. 1-10)", "REINO DE BRONZE (Lv. 11-25)", "TERRAS ELEMENTAIS (Lv. 26+)"};

    /** Página do mapa-múndi que corresponde ao nível do jogador (é a que abre ao apertar a bússola). */
    public static int pageForLevel(int level) {
        int page = 0;
        for (int i = 0; i < PAGE_MIN_LV.length; i++) if (level >= PAGE_MIN_LV[i]) page = i;
        return page;
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        int w = canvas.getWidth();
        int h = canvas.getHeight();
        float k = ExploringUI.uiScale(w, h);
        int playerLv = gv.player.getLevel();
        int myPage = pageForLevel(playerLv);

        canvas.drawColor(Color.argb(240, 10, 15, 30));

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(40 * k);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        canvas.drawText("🗺️ MAPA MÚNDI — " + PAGE_NAME[currentPage], w / 2f, 62 * k, paint);
        paint.setFakeBoldText(false);
        paint.setTextSize(18 * k);
        paint.setColor(currentPage == myPage ? Color.rgb(125, 255, 154) : Color.rgb(170, 180, 200));
        canvas.drawText(currentPage == myPage ? "★ Mapas do seu nível (Lv. " + playerLv + ")" : "Você está no Lv. " + playerLv + " — seus mapas ficam na página " + (myPage + 1), w / 2f, 90 * k, paint);

        float navY = h - 72 * k, navW = 220 * k, navH = 52 * k;
        backBtn.set(40 * k, navY, 40 * k + 170 * k, navY + navH);
        prevBtn.set(w / 2f - navW - 20 * k, navY, w / 2f - 20 * k, navY + navH);
        nextBtn.set(w / 2f + 20 * k, navY, w / 2f + navW + 20 * k, navY + navH);

        List<MapPoint> page = new ArrayList<>();
        for (MapPoint p : points) if (p.page == currentPage) page.add(p);
        java.util.Collections.sort(page, (x, y) -> x.recLv != y.recLv ? Integer.compare(x.recLv, y.recLv) : x.name.compareTo(y.name));   // do mais fraco ao mais forte
        int cols = 4, rows = Math.max(1, (page.size() + cols - 1) / cols);
        float gap = 14 * k, areaL = 40 * k, areaR = w - 40 * k, areaT = 112 * k, areaB = navY - 18 * k;
        float btnW = (areaR - areaL - gap * (cols - 1)) / cols;
        float btnH = Math.min(150 * k, (areaB - areaT - gap * (rows - 1)) / rows);

        for (int i = 0; i < page.size(); i++) {
            MapPoint p = page.get(i);
            int r = i / cols, c = i % cols;
            float x = areaL + c * (btnW + gap), y = areaT + r * (btnH + gap);
            p.bounds.set(x, y, x + btnW, y + btnH);
            boolean locked = playerLv < p.unlockLv;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(locked ? Color.argb(100, 50, 50, 50) : Color.argb(180, 30, 40, 70));
            canvas.drawRoundRect(p.bounds, 15, 15, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(locked ? Color.GRAY : p.color);
            canvas.drawRoundRect(p.bounds, 15, 15, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);

            if (locked) {
                paint.setColor(Color.WHITE); paint.setTextSize(26 * k);
                canvas.drawText("🔒 Lv." + p.unlockLv, p.bounds.centerX(), p.bounds.centerY() + 9 * k, paint);
            } else {
                paint.setColor(Color.WHITE); paint.setTextSize(Math.min(38 * k, btnH * 0.28f));
                canvas.drawText(p.icon, p.bounds.centerX(), p.bounds.top + btnH * 0.38f, paint);
                paint.setTextSize(Math.min(21 * k, btnH * 0.16f));
                canvas.drawText(p.name, p.bounds.centerX(), p.bounds.top + btnH * 0.68f, paint);
                paint.setTextSize(Math.min(17 * k, btnH * 0.13f));
                paint.setColor(playerLv >= p.recLv ? Color.rgb(200, 220, 255) : Color.rgb(255, 200, 120));
                canvas.drawText("Lv. " + p.recLv, p.bounds.centerX(), p.bounds.top + btnH * 0.90f, paint);
            }
        }

        if (currentPage < PAGE_COUNT - 1) drawNavBtn(canvas, nextBtn, "PRÓXIMO ➡️", k);
        if (currentPage > 0) drawNavBtn(canvas, prevBtn, "⬅️ ANTERIOR", k);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.RED);
        canvas.drawRoundRect(backBtn, 10, 10, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(22 * k);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("⬅️ VOLTAR", backBtn.centerX(), backBtn.centerY() + 8 * k, paint);
    }

    private void drawNavBtn(Canvas canvas, RectF r, String label, float k) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(200, 50, 50, 80));
        canvas.drawRoundRect(r, 10, 10, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(21 * k);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, r.centerX(), r.centerY() + 8 * k, paint);
    }
}
