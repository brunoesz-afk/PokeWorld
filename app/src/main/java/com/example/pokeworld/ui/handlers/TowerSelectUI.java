package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.ItemCatalog;
import com.example.pokeworld.managers.TowerManager;

import java.util.ArrayList;
import java.util.List;

/** Seleção de andares da Torre: mapa de 20 andares com progresso à esquerda e detalhes/prêmios do andar à direita. */
public class TowerSelectUI extends BaseUI {
    public RectF endlessBtn = new RectF();
    public RectF exitBtn = new RectF();
    public RectF enterBtn = new RectF();
    public List<FloorCard> floorCards = new ArrayList<>();
    public int selectedFloor = 0;

    public static class FloorCard {
        public int floor;
        public RectF rect = new RectF();
        public boolean unlocked;

        public FloorCard(int floor, boolean unlocked) {
            this.floor = floor;
            this.unlocked = unlocked;
        }
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        int cleared = gv.tower.getHighestCleared();
        int unlockedMax = gv.tower.getMaxUnlockedFloor();
        drawHeader(canvas, "TORRE DE DESAFIOS", "Andares concluídos: " + cleared + " / " + TowerManager.MAX_FLOOR + "   •   cada andar tem 3 hordas", exitBtn);
        if (selectedFloor < 1 || selectedFloor > TowerManager.MAX_FLOOR) selectedFloor = unlockedMax;

        // --- Mapa dos andares (1 em baixo à esquerda, 20 no topo à direita) ---
        RectF map = new RectF(30, 124, w * 0.64f, h - 24);
        panel(canvas, map, Color.argb(120, 10, 16, 28), C_LINE, 2);
        int cols = 5, rows = 4;
        float pad = 16, gap = 12;
        float cw = (map.width() - pad * 2 - gap * (cols - 1)) / cols;
        float ch = (map.height() - pad * 2 - gap * (rows - 1)) / rows;

        floorCards.clear();
        for (int f = 1; f <= TowerManager.MAX_FLOOR; f++) {
            int idx = f - 1;
            int r = rows - 1 - idx / cols;   // andares mais altos ficam no topo
            int c = idx % cols;
            FloorCard card = new FloorCard(f, f <= unlockedMax);
            float x = map.left + pad + c * (cw + gap);
            float y = map.top + pad + r * (ch + gap);
            card.rect.set(x, y, x + cw, y + ch);
            floorCards.add(card);

            boolean done = f <= cleared;
            boolean next = f == unlockedMax && !done;
            boolean boss = f % 5 == 0;
            boolean sel = f == selectedFloor;

            int fill = done ? Color.rgb(34, 70, 52) : card.unlocked ? Color.rgb(44, 66, 104) : Color.rgb(26, 28, 36);
            int stroke = sel ? Color.WHITE : boss ? Color.rgb(255, 150, 60) : done ? C_GOOD : next ? C_ACCENT : Color.rgb(70, 78, 96);
            panel(canvas, card.rect, fill, stroke, sel ? 5 : (next || boss ? 3 : 2));

            text(canvas, String.valueOf(f), card.rect.centerX(), card.rect.top + ch * 0.50f, Math.min(44, ch * 0.42f), card.unlocked ? Color.WHITE : Color.GRAY, Paint.Align.CENTER, true);
            String badge = done ? "✔" : next ? "▶" : card.unlocked ? "" : "🔒";
            if (!badge.isEmpty()) text(canvas, badge, card.rect.right - 14, card.rect.top + 26, 22, done ? C_GOOD : C_ACCENT, Paint.Align.RIGHT, true);
            if (boss) text(canvas, "🏆", card.rect.left + 12, card.rect.top + 28, 22, Color.WHITE, Paint.Align.LEFT, false);
            String icon = ItemCatalog.get(TowerManager.rewardItem(f)).icon;
            text(canvas, icon, card.rect.centerX(), card.rect.bottom - 12, Math.min(26, ch * 0.24f), Color.WHITE, Paint.Align.CENTER, false);
        }

        // --- Detalhes do andar selecionado ---
        RectF det = new RectF(map.right + 18, 124, w - 30, h - 24);
        panel(canvas, det, C_PANEL, C_LINE, 2);
        int f = selectedFloor;
        boolean unlocked = f <= unlockedMax;
        boolean done = f <= cleared;
        float cx = det.centerX();
        text(canvas, "ANDAR " + f, cx, det.top + 56, 40, Color.WHITE, Paint.Align.CENTER, true);
        String status = done ? "✔ Concluído (pode repetir)" : unlocked ? "▶ Disponível" : "🔒 Conclua o andar " + (f - 1);
        text(canvas, status, cx, det.top + 90, 19, done ? C_GOOD : unlocked ? C_ACCENT : C_BAD, Paint.Align.CENTER, true);
        if (f % 5 == 0) text(canvas, "🏆 Andar especial: prêmio de runa", cx, det.top + 118, 17, Color.rgb(255, 170, 90), Paint.Align.CENTER, false);

        float y = det.top + 160;
        text(canvas, "Inimigos", det.left + 24, y, 18, C_MUTED, Paint.Align.LEFT, true);
        text(canvas, "3 hordas • até Nv. " + TowerManager.enemyLevel(f), det.left + 24, y + 28, 22, Color.WHITE, Paint.Align.LEFT, false);
        y += 74;
        text(canvas, "Prêmios", det.left + 24, y, 18, C_MUTED, Paint.Align.LEFT, true);
        text(canvas, "🪙 " + TowerManager.rewardGold(f) + " de ouro", det.left + 24, y + 30, 22, C_ACCENT, Paint.Align.LEFT, false);
        String item = TowerManager.rewardItem(f);
        paint.setTextSize(22);
        text(canvas, fit(ItemCatalog.get(item).icon + " " + TowerManager.rewardItemAmount(f) + "x " + item, det.width() - 48), det.left + 24, y + 62, 22, Color.WHITE, Paint.Align.LEFT, false);

        if (TowerManager.rewardBoostGems(f) > 0)
            text(canvas, "💎 " + TowerManager.rewardBoostGems(f) + "x Gema de Boost", det.left + 24, y + 94, 22, Color.rgb(120, 200, 255), Paint.Align.LEFT, false);

        endlessBtn.set(det.left + 24, det.bottom - 154, det.right - 24, det.bottom - 98);
        boolean eu = gv.tower.isEndlessUnlocked();
        actionButton(canvas, endlessBtn, eu ? "♾️ MODO INFINITO (recorde: andar " + gv.tower.getEndlessBest() + ")" : "♾️ INFINITO: conclua o andar 20", eu, Color.rgb(190, 120, 255));
        enterBtn.set(det.left + 24, det.bottom - 84, det.right - 24, det.bottom - 24);
        actionButton(canvas, enterBtn, unlocked ? "ENTRAR" : "BLOQUEADO", unlocked, C_GOOD);
    }
}
