package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.world.BaseDefense;
import com.example.pokeworld.world.BaseKind;
import com.example.pokeworld.world.BaseLayout;
import com.example.pokeworld.world.BaseManager;
import com.example.pokeworld.world.BasePiece;
import com.example.pokeworld.world.PlayerWallet;

import java.util.ArrayList;
import java.util.List;

/**
 * Menu do modo construção (estilo Clash/Big Farm): o mapa fica visível por trás, a base mostra a
 * grade, e aqui ficam só os painéis: recursos no topo e, embaixo, o menu de peças OU as ações da
 * peça selecionada OU os botões de confirmar/cancelar do posicionamento.
 */
public class BuildUI extends BaseUI {
    public static final int CARD_W = 196, CARD_H = 150, GAP = 10;
    public static final float PANEL_H = 190f;

    // zonas (preenchidas a cada frame; a BuildInput só lê)
    public final RectF panel = new RectF();
    public final RectF raidBtn = new RectF();
    public final RectF confirmBtn = new RectF(), cancelBtn = new RectF(), paintToggleBtn = new RectF();
    public final RectF moveBtn = new RectF(), upgradeBtn = new RectF(), sellBtn = new RectF(), closeBtn = new RectF(), wallAllBtn = new RectF();
    public final List<RectF> cardRects = new ArrayList<>();
    public final List<BaseKind> cardKinds = new ArrayList<>();
    public float menuScroll = 0;
    public float menuMaxScroll = 0;

    /** Peças que aparecem no menu (o Quartel já existe desde o começo). */
    public static List<BaseKind> menuKinds() {
        List<BaseKind> l = new ArrayList<>();
        for (BaseKind k : BaseKind.values()) if (k != BaseKind.QUARTEL) l.add(k);
        return l;
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        BaseManager base = gv.world.base;
        if (base == null) { gv.ui.state = com.example.pokeworld.ui.UIManager.UIState.EXPLORING; return; }
        gv.world.tickBase(gv); // o relógio das obras também anda aqui dentro

        float w = canvas.getWidth(), h = canvas.getHeight();
        long now = System.currentTimeMillis();
        PlayerWallet wallet = new PlayerWallet(gv.player);

        clearZones();
        drawTopBar(canvas, gv, base, wallet, w, now);

        panel.set(0, h - PANEL_H, w, h);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(225, 24, 26, 34));
        canvas.drawRect(panel, paint);
        paint.setColor(Color.argb(255, 90, 100, 130));
        canvas.drawRect(0, h - PANEL_H, w, h - PANEL_H + 4, paint);

        if (base.movingSelected && base.selected != null) drawMovePanel(canvas, base, w, h);
        else if (base.placing != null) drawPlacePanel(canvas, base, wallet, w, h);
        else if (base.selected != null) drawSelectedPanel(canvas, base, wallet, w, h, now);
        else drawMenu(canvas, base, wallet, w, h);

        drawExitButton(canvas, new RectF(w - 180, 20, w - 20, 90));
    }

    private void clearZones() {
        cardRects.clear(); cardKinds.clear();
        for (RectF r : new RectF[]{raidBtn, confirmBtn, cancelBtn, paintToggleBtn, moveBtn, upgradeBtn, sellBtn, closeBtn, wallAllBtn}) r.setEmpty();
    }

    // ------------------------------------------------------------------ topo
    private void drawTopBar(Canvas canvas, GameView gv, BaseManager base, PlayerWallet wallet, float w, long now) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(200, 0, 0, 0));
        canvas.drawRoundRect(new RectF(16, 12, w - 200, 74), 16, 16, paint);
        paint.setColor(Color.WHITE); paint.setTextSize(26); paint.setTextAlign(Paint.Align.LEFT);
        int guards = 0;
        for (com.example.pokeworld.entities.Enemy e : gv.world.enemies) if (e.isPet) guards++;
        String line = "🪵 " + wallet.count(BaseManager.WOOD) + "   ⛓️ " + wallet.count(BaseManager.IRON) + "   🪙 " + wallet.gold()
                + "   │  🏰 Quartel Lv." + base.hq() + "   🐾 " + guards + "/" + base.guardCapacity()
                + "   🧱 " + base.layout.count(BaseKind.MURALHA) + "/" + BaseKind.MURALHA.maxCount(base.hq());
        canvas.drawText(line, 32, 52, paint);

        if (gv.world.defense != null) {
            BaseDefense d = gv.world.defense;
            raidBtn.set(w - 360, 90, w - 20, 150);
            long next = d.secondsToNext(now);
            String t = d.isActive() ? "⚔️ Invasão em andamento" : "⚔️ Treinar defesa (próxima em " + (next / 60) + ":" + String.format("%02d", next % 60) + ")";
            paint.setTextSize(20);
            drawButton(canvas, raidBtn, t);
        }

        if (base.editing && base.selected == null && base.placing == null) {
            paint.setColor(Color.argb(210, 255, 255, 255)); paint.setTextSize(22); paint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText("Arraste o mapa para andar • toque numa peça para editar • escolha uma peça abaixo para construir", 24, 108, paint);
        }
    }

    // ------------------------------------------------------------------ menu de peças
    private void drawMenu(Canvas canvas, BaseManager base, PlayerWallet wallet, float w, float h) {
        List<BaseKind> kinds = menuKinds();
        float total = kinds.size() * (CARD_W + GAP) + GAP;
        menuMaxScroll = Math.max(0, total - w);
        menuScroll = Math.max(0, Math.min(menuMaxScroll, menuScroll));
        float top = h - PANEL_H + 20;
        canvas.save();
        canvas.clipRect(0, h - PANEL_H + 6, w, h);
        for (int i = 0; i < kinds.size(); i++) {
            BaseKind k = kinds.get(i);
            float x = GAP + i * (CARD_W + GAP) - menuScroll;
            RectF r = new RectF(x, top, x + CARD_W, top + CARD_H);
            cardRects.add(r); cardKinds.add(k);
            String blocked = base.blockedReason(k);
            boolean afford = base.canAfford(wallet, k.cost(1));

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(blocked != null ? Color.argb(255, 40, 40, 46) : Color.argb(255, 50, 58, 78));
            canvas.drawRoundRect(r, 14, 14, paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3);
            paint.setColor(blocked != null ? Color.argb(255, 80, 80, 90) : afford ? Color.argb(255, 120, 200, 130) : Color.argb(255, 200, 150, 90));
            canvas.drawRoundRect(r, 14, 14, paint);

            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(blocked != null ? Color.GRAY : Color.WHITE);
            paint.setTextSize(40);
            canvas.drawText(k.icon, r.centerX(), r.top + 48, paint);
            paint.setTextSize(21);
            canvas.drawText(k.label, r.centerX(), r.top + 78, paint);
            paint.setTextSize(19);
            paint.setColor(blocked != null ? Color.GRAY : afford ? Color.rgb(170, 235, 170) : Color.rgb(240, 190, 130));
            canvas.drawText(base.costText(k.cost(1)), r.centerX(), r.top + 104, paint);
            paint.setColor(Color.argb(200, 200, 205, 220));
            paint.setTextSize(17);
            String sub = k.w + "x" + k.h + "  •  " + base.layout.count(k) + "/" + k.maxCount(base.hq());
            canvas.drawText(sub, r.centerX(), r.top + 128, paint);
        }
        canvas.restore();
    }

    // ------------------------------------------------------------------ posicionar peça nova
    private void drawPlacePanel(Canvas canvas, BaseManager base, PlayerWallet wallet, float w, float h) {
        BaseKind k = base.placing;
        float top = h - PANEL_H + 24;
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE); paint.setTextSize(30);
        canvas.drawText(k.icon + " " + k.label, 30, top + 36, paint);
        paint.setTextSize(21); paint.setColor(Color.argb(220, 210, 215, 230));
        canvas.drawText(k.description(), 30, top + 70, paint);
        int[] cost = k.cost(1);
        boolean afford = base.canAfford(wallet, cost);
        paint.setColor(afford ? Color.rgb(170, 235, 170) : Color.rgb(240, 150, 130));
        canvas.drawText("Custo: " + base.costText(cost) + (k.buildSeconds > 0 ? "   ⏱ " + k.seconds(1) + "s" : ""), 30, top + 102, paint);

        if (k.isWall()) {
            paint.setColor(Color.argb(220, 210, 215, 230));
            canvas.drawText("Muralhas: " + base.layout.count(k) + "/" + k.maxCount(base.hq()) + " • arraste para pintar uma linha", 30, top + 134, paint);
            paintToggleBtn.set(w - 640, top, w - 340, top + 90);
            drawButton(canvas, paintToggleBtn, base.paintWalls ? "🖌 Pintando\n(toque p/ mover mapa)" : "✋ Movendo mapa\n(toque p/ pintar)");
            confirmBtn.set(w - 320, top, w - 170, top + 90);
            drawButton(canvas, confirmBtn, "✔ Pronto");
            return;
        }
        boolean ok = base.layout.isFree(k, base.ghostX, base.ghostY, null);
        paint.setColor(ok ? Color.rgb(170, 235, 170) : Color.rgb(240, 150, 130));
        canvas.drawText(ok ? "Arraste a peça e toque em CONSTRUIR" : "Posição inválida (ocupada ou fora do terreno)", 30, top + 134, paint);
        confirmBtn.set(w - 470, top, w - 250, top + 90);
        cancelBtn.set(w - 230, top, w - 30, top + 90);
        drawButton(canvas, confirmBtn, ok && afford ? "✔ CONSTRUIR" : "✔ CONSTRUIR\n(indisponível)");
        drawButton(canvas, cancelBtn, "✖ Cancelar");
    }

    private void drawMovePanel(Canvas canvas, BaseManager base, float w, float h) {
        BasePiece p = base.selected;
        float top = h - PANEL_H + 24;
        paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE); paint.setTextSize(30);
        canvas.drawText("🔄 Mover " + p.kind.label, 30, top + 36, paint);
        boolean ok = base.layout.isFree(p.kind, base.ghostX, base.ghostY, p);
        paint.setTextSize(21); paint.setColor(ok ? Color.rgb(170, 235, 170) : Color.rgb(240, 150, 130));
        canvas.drawText(ok ? "Arraste até o novo lugar e confirme (mover é de graça)" : "Posição inválida", 30, top + 74, paint);
        confirmBtn.set(w - 470, top, w - 250, top + 90);
        cancelBtn.set(w - 230, top, w - 30, top + 90);
        drawButton(canvas, confirmBtn, "✔ MOVER AQUI");
        drawButton(canvas, cancelBtn, "✖ Cancelar");
    }

    // ------------------------------------------------------------------ peça selecionada
    private void drawSelectedPanel(Canvas canvas, BaseManager base, PlayerWallet wallet, float w, float h, long now) {
        BasePiece p = base.selected;
        BaseKind k = p.kind;
        float top = h - PANEL_H + 20;

        // 1) botões (da direita para a esquerda); depois o texto usa o espaço que sobrou
        boolean wall = k.isWall();
        float bw = wall ? 160 : 180, bh = 100, gap = 10, x = w - 24 - bw, by = top + 10;
        if (!wall) { closeBtn.set(x, by, x + bw, by + bh); drawButton(canvas, closeBtn, "✖ Fechar"); x -= bw + gap; }

        int[] refund = base.refund(p);
        sellBtn.set(x, by, x + bw, by + bh);
        drawButtonWithMultine(canvas, sellBtn, refund == null ? "💰 Vender\n(não vende)" : "💰 Vender\n+" + base.costText(refund));
        x -= bw + gap;

        upgradeBtn.set(x, by, x + bw, by + bh);
        if (p.level >= k.maxLevel) drawButtonWithMultine(canvas, upgradeBtn, "⬆ Melhorar\nnível máximo");
        else drawButtonWithMultine(canvas, upgradeBtn, "⬆ Melhorar\n" + base.costText(base.nextCost(p)));
        x -= bw + gap;

        moveBtn.set(x, by, x + bw, by + bh);
        drawButtonWithMultine(canvas, moveBtn, "🔄 Mover");
        x -= bw + gap;

        if (wall && p.level < k.maxLevel) {
            int n = base.countWalls(p.level);
            int[] c = k.cost(p.level + 1);
            wallAllBtn.set(x - 60, by, x + bw, by + bh);
            drawButtonWithMultine(canvas, wallAllBtn, "⬆ Todas Lv." + p.level + " (" + n + ")\n" + base.costText(new int[]{c[0] * n, c[1] * n, c[2] * n}));
            x = wallAllBtn.left - gap;
        }
        float maxW = Math.max(200, x - 50);

        // 2) texto
        paint.setStyle(Paint.Style.FILL); paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Color.WHITE); paint.setTextSize(30);
        String lv = wall ? BaseKind.WALL_NAMES[Math.max(0, Math.min(5, p.level - 1))] + " Lv." + p.level : "Lv." + p.level + "/" + k.maxLevel;
        canvas.drawText(fit(k.icon + " " + k.label + "  " + lv, maxW), 30, top + 34, paint);
        paint.setTextSize(20); paint.setColor(Color.argb(225, 210, 215, 230));
        canvas.drawText(fit(k.description(), maxW), 30, top + 66, paint);
        String status;
        if (p.isBusy(now)) status = (p.upgrading ? "⬆ Melhorando" : "🏗️ Em obra") + " — " + fmt(p.remainingSeconds(now));
        else if (wall) status = "❤ Resistência: " + BaseKind.WALL_HP[Math.max(0, Math.min(5, p.level - 1))];
        else if (k.isDefense()) status = "🎯 Alcance " + (int) (BaseDefense.towerRange(p) / 64f) + " tiles   ❤ " + BaseDefense.maxHp(p);
        else status = "Pronto para uso";
        canvas.drawText(fit(status, maxW), 30, top + 98, paint);
        if (p.level < k.maxLevel && p.level >= k.levelCap(base.hq())) {
            paint.setColor(Color.rgb(240, 190, 130));
            canvas.drawText(fit("Suba o Quartel para liberar o nível " + (p.level + 1), maxW), 30, top + 128, paint);
        }
    }

    /** Corta o texto com "…" para caber na largura. */
    private String fit(String t, float maxW) {
        if (paint.measureText(t) <= maxW) return t;
        int n = t.length();
        while (n > 1 && paint.measureText(t.substring(0, n) + "…") > maxW) n--;
        return t.substring(0, n) + "…";
    }

    private static String fmt(int s) { return String.format("%d:%02d", s / 60, s % 60); }
}
