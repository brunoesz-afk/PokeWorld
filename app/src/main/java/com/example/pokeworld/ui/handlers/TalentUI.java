package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.Player;
import com.example.pokeworld.entities.TalentTree;

import java.util.ArrayList;
import java.util.List;

/** Árvore de talentos em 3 ramos: nós filhos só liberam depois que o nó pai atinge o nível exigido. */
public class TalentUI extends BaseUI {
    public static final int RESET_COST = 1000;

    public RectF exitBtn = new RectF();
    public RectF upgradeBtn = new RectF();
    public RectF resetBtn = new RectF();
    public List<NodeBox> boxes = new ArrayList<>();
    public String selected = "Domador";

    public static class NodeBox {
        public final TalentTree.Node node;
        public final RectF rect = new RectF();
        NodeBox(TalentTree.Node n) { node = n; }
    }

    private NodeBox box(String key) {
        for (NodeBox b : boxes) if (b.node.key.equals(key)) return b;
        return null;
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        Player p = gv.player;
        drawHeader(canvas, "ÁRVORE DE TALENTOS", "Pontos disponíveis: " + p.getTalentPoints() + "   •   ganhe 1 ponto a cada nível do treinador", exitBtn);

        float areaTop = 118, areaBottom = h - 156;
        float branchW = (w - 60) / 3f;
        float rowH = (areaBottom - areaTop - 34) / 3f;
        float cardW = Math.min(186, branchW / 2f - 12);
        float cardH = Math.min(rowH - 22, 120);

        // posições
        boxes.clear();
        for (TalentTree.Node n : TalentTree.NODES) {
            NodeBox b = new NodeBox(n);
            float cx = 30 + branchW * (n.branch + 0.5f) + n.side * (cardW / 2f + 8);
            float top = areaTop + 34 + n.row * rowH + (rowH - cardH) / 2f;
            b.rect.set(cx - cardW / 2f, top, cx + cardW / 2f, top + cardH);
            boxes.add(b);
        }

        // fundo e título de cada ramo
        for (int br = 0; br < 3; br++) {
            RectF col = new RectF(30 + br * branchW + 4, areaTop, 30 + (br + 1) * branchW - 4, areaBottom);
            panel(canvas, col, Color.argb(90, 12, 18, 30), C_LINE, 2);
            text(canvas, TalentTree.BRANCH_ICONS[br] + " " + TalentTree.BRANCH_NAMES[br], col.centerX(), areaTop + 28, 20, C_ACCENT, Paint.Align.CENTER, true);
        }

        // ligações pai -> filho
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        for (NodeBox b : boxes) {
            if (b.node.parent == null) continue;
            NodeBox par = box(b.node.parent);
            if (par == null) continue;
            boolean lit = TalentTree.isUnlocked(p, b.node);
            paint.setColor(lit ? C_ACCENT : Color.rgb(60, 68, 86));
            float x1 = par.rect.centerX(), y1 = par.rect.bottom, x2 = b.rect.centerX(), y2 = b.rect.top;
            float my = (y1 + y2) / 2f;
            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(x1, y1); path.lineTo(x1, my); path.lineTo(x2, my); path.lineTo(x2, y2);
            canvas.drawPath(path, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        // nós
        for (NodeBox b : boxes) {
            TalentTree.Node n = b.node;
            int lv = TalentTree.level(p, n.key);
            boolean unlocked = TalentTree.isUnlocked(p, n);
            boolean maxed = lv >= n.max;
            boolean can = TalentTree.canUpgrade(p, n);
            boolean sel = n.key.equals(selected);
            int fill = maxed ? Color.rgb(84, 70, 22) : unlocked ? Color.rgb(36, 58, 92) : Color.rgb(24, 26, 34);
            int stroke = sel ? Color.WHITE : maxed ? Color.rgb(255, 215, 0) : can ? C_GOOD : unlocked ? C_LINE : Color.rgb(60, 66, 82);
            panel(canvas, b.rect, fill, stroke, sel ? 5 : can ? 4 : 2);
            text(canvas, unlocked ? n.icon : "🔒", b.rect.centerX(), b.rect.top + cardH * 0.40f, Math.min(38, cardH * 0.34f), Color.WHITE, Paint.Align.CENTER, false);
            paint.setTextSize(15);
            text(canvas, fit(n.name, b.rect.width() - 10), b.rect.centerX(), b.rect.top + cardH * 0.66f, 15, unlocked ? Color.WHITE : Color.GRAY, Paint.Align.CENTER, true);
            // pips de nível
            float pw = Math.min(14, (b.rect.width() - 24) / n.max - 3);
            float px = b.rect.centerX() - (n.max * (pw + 3) - 3) / 2f;
            for (int i = 0; i < n.max; i++) {
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(i < lv ? (maxed ? Color.rgb(255, 215, 0) : C_GOOD) : Color.rgb(50, 58, 76));
                canvas.drawRoundRect(new RectF(px + i * (pw + 3), b.rect.bottom - 22, px + i * (pw + 3) + pw, b.rect.bottom - 12), 3, 3, paint);
            }
        }

        // faixa de detalhes
        RectF det = new RectF(30, h - 144, w - 30, h - 20);
        panel(canvas, det, C_PANEL, C_LINE, 2);
        TalentTree.Node n = TalentTree.get(selected);
        if (n == null) n = TalentTree.NODES.get(0);
        int lv = TalentTree.level(p, n.key);
        boolean unlocked = TalentTree.isUnlocked(p, n);
        text(canvas, n.icon + " " + n.name + "   Nv. " + lv + " / " + n.max, det.left + 24, det.top + 38, 26, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, n.effectText(), det.left + 24, det.top + 70, 18, Color.rgb(215, 224, 240), Paint.Align.LEFT, false);
        String now = lv > 0 ? "Agora: " + (n.percent() * lv) + "% total" : "Ainda sem bônus";
        text(canvas, unlocked ? now : "🔒 " + TalentTree.requirementText(n), det.left + 24, det.top + 100, 17, unlocked ? C_ACCENT : C_BAD, Paint.Align.LEFT, false);

        float bw = Math.min(260, (det.width() - 60) / 3f);
        upgradeBtn.set(det.right - bw - 20, det.top + 14, det.right - 20, det.top + 66);
        boolean can = TalentTree.canUpgrade(p, n);
        String lab = lv >= n.max ? "NO MÁXIMO" : !unlocked ? "BLOQUEADO" : p.getTalentPoints() <= 0 ? "SEM PONTOS" : "MELHORAR (1 ponto)";
        actionButton(canvas, upgradeBtn, lab, can, C_GOOD);
        resetBtn.set(det.right - bw - 20, det.top + 74, det.right - 20, det.bottom - 12);
        boolean canReset = TalentTree.spentPoints(p) > 0 && p.getGold() >= RESET_COST;
        panel(canvas, resetBtn, canReset ? Color.rgb(90, 50, 50) : Color.rgb(40, 40, 46), canReset ? Color.WHITE : Color.rgb(70, 72, 84), 2);
        text(canvas, "↺ Resetar ( " + RESET_COST + " 🪙 )", resetBtn.centerX(), resetBtn.centerY() + 6, 17, canReset ? Color.WHITE : Color.GRAY, Paint.Align.CENTER, false);
    }
}
