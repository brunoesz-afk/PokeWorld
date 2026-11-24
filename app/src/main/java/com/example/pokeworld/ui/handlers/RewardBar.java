package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.EventCalendar;
import com.example.pokeworld.managers.RewardManager;

/**
 * Barra de ícones no topo da cidade e da base: Presente, Evento, Diárias, Jornada, Conquistas, Recarga e COLETAR TUDO.
 * Cada ícone mostra um selo vermelho com a quantidade pronta para reivindicar.
 */
public class RewardBar {
    public static final int GIFT = 0, EVENT = 1, DAILY = 2, JOURNEY = 3, ACH = 4, RECHARGE = 5, PASS = 6, SHOP = 7, ALL = 8, COUNT = 9;

    private final RectF[] rects = new RectF[COUNT];
    private final Paint paint = new Paint();

    // valores em cache (atualizados 2x por segundo: a barra é desenhada todo quadro)
    private long lastRefresh = 0;
    private final int[] badge = new int[COUNT];
    private int rechargeLeft = 0, dailiesDone = 0, pending = 0;
    private String eventIcon = "🎉";

    public RewardBar() {
        for (int i = 0; i < COUNT; i++) rects[i] = new RectF();
        paint.setAntiAlias(true);
    }

    public boolean visible(GameView gv) {
        if (gv.world == null || gv.world.tileMap == null) return false;
        String m = gv.world.tileMap.getMapName();
        return "CITY".equals(m) || "HOME".equals(m);
    }

    /** Altura ocupada pela barra (para o resto da HUD não ficar por cima). */
    public float bottom(float w, float h) {
        float k = ExploringUI.uiScale(w, h);
        return 100 * k + size(w, h) + 22 * k;
    }

    private float size(float w, float h) {
        float k = ExploringUI.uiScale(w, h);
        float left = 436 * k, right = w - 370 * k, gap = 6 * k;
        return Math.max(40 * k, Math.min(76 * k, (right - left - gap * (COUNT - 1)) / COUNT));
    }

    private void layout(float w, float h) {
        float k = ExploringUI.uiScale(w, h);
        float left = 436 * k, right = w - 370 * k, gap = 6 * k, s = size(w, h);
        float total = COUNT * s + (COUNT - 1) * gap;
        float x = (left + right) / 2f - total / 2f, y = 100 * k;
        for (int i = 0; i < COUNT; i++) { rects[i].set(x, y, x + s, y + s); x += s + gap; }
    }

    private void refresh(GameView gv) {
        long now = System.currentTimeMillis();
        if (now - lastRefresh < 500) return;
        lastRefresh = now;
        RewardManager.Store st = RewardManager.of(gv.activity.dbHelper);
        badge[GIFT] = RewardManager.giftReady(st) ? 1 : 0;
        badge[EVENT] = RewardManager.chestReady(st) ? 1 : 0;
        badge[DAILY] = gv.missions.readyDailyCount();
        badge[JOURNEY] = gv.missions.readyJourneyCount();
        badge[ACH] = gv.achievements.readyCount();
        rechargeLeft = RewardManager.rechargeSecondsLeft(st);
        badge[RECHARGE] = rechargeLeft == 0 ? 1 : 0;
        dailiesDone = gv.missions.dailiesDone();
        badge[PASS] = com.example.pokeworld.managers.PassManager.readyCount(st);
        pending = badge[GIFT] + badge[EVENT] + badge[DAILY] + badge[JOURNEY] + badge[ACH] + badge[RECHARGE] + badge[PASS];
        badge[ALL] = pending;
        eventIcon = EventCalendar.today().icon;
    }

    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        float k = ExploringUI.uiScale(w, h);
        layout(w, h);
        refresh(gv);
        String[] icons = {"🎁", eventIcon, "📅", "📖", "🏆", "🔋", "🎟️", "💠", "✨"};
        String[] labels = {"Presente", "Evento", "Diárias", "Jornada", "Conquistas", "Recarga", "Passe", "Loja", "Coletar tudo"};
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 260.0);

        for (int i = 0; i < COUNT; i++) {
            RectF r = rects[i];
            boolean has = badge[i] > 0;
            boolean all = i == ALL;
            paint.setStyle(Paint.Style.FILL);
            if (all) paint.setColor(has ? Color.argb(235, 40, 150, 80) : Color.argb(150, 30, 34, 44));
            else paint.setColor(Color.argb(has ? 215 : 165, has ? 60 : 14, has ? 46 : 20, has ? 14 : 34));
            canvas.drawRoundRect(r, 14 * k, 14 * k, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth((has ? 3.5f : 2f) * k);
            paint.setColor(has ? Color.argb((int) (170 + 85 * pulse), 255, 205, 80) : Color.argb(110, 170, 190, 220));
            canvas.drawRoundRect(r, 14 * k, 14 * k, paint);
            paint.setStyle(Paint.Style.FILL);

            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(Color.WHITE);
            paint.setTextSize(r.width() * 0.5f);
            canvas.drawText(icons[i], r.centerX(), r.centerY() + r.width() * 0.14f, paint);

            // nota pequena dentro do ícone
            String note = null;
            if (i == DAILY && !has) note = dailiesDone + "/3";
            if (i == RECHARGE && !has) note = RewardManager.countdown(rechargeLeft);
            if (note != null) {
                paint.setTextSize(r.width() * 0.2f);
                paint.setColor(Color.rgb(200, 210, 230));
                canvas.drawText(note, r.centerX(), r.bottom - r.width() * 0.07f, paint);
            }

            // selo vermelho
            if (has && !all) {
                float br = Math.max(9f * k, r.width() * 0.19f);
                float bx = r.right - br * 0.45f, by = r.top + br * 0.45f;
                paint.setColor(Color.rgb(225, 45, 45));
                canvas.drawCircle(bx, by, br, paint);
                paint.setColor(Color.WHITE);
                paint.setTextSize(br * 1.25f);
                paint.setFakeBoldText(true);
                canvas.drawText(badge[i] > 9 ? "9+" : String.valueOf(badge[i]), bx, by + br * 0.43f, paint);
                paint.setFakeBoldText(false);
            }

            // rótulo embaixo, com contorno para ler sobre o mapa
            paint.setTextSize(Math.max(12f * k, r.width() * 0.2f));
            paint.setFakeBoldText(true);
            paint.setColor(Color.argb(220, 0, 0, 0));
            canvas.drawText(labels[i], r.centerX() + 1.5f, r.bottom + 17 * k + 1.5f, paint);
            paint.setColor(all && has ? Color.rgb(150, 255, 180) : Color.WHITE);
            canvas.drawText(labels[i], r.centerX(), r.bottom + 17 * k, paint);
            paint.setFakeBoldText(false);
        }
    }

    /** Qual ícone foi tocado (-1 = nenhum). */
    public int hit(float x, float y) {
        for (int i = 0; i < COUNT; i++) if (rects[i].contains(x, y)) return i;
        return -1;
    }

    /** Força a releitura dos contadores (depois de reivindicar algo). */
    public void invalidate() { lastRefresh = 0; }
}
