package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.MissionManager;
import com.example.pokeworld.managers.MissionManager.Quest;

import java.util.List;

/** Jornada do Treinador: cartões do ato atual com progresso, dica de como cumprir e recompensa. */
public class MissionUI extends BaseUI {
    public RectF exitBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        MissionManager mm = gv.missions;
        int total = MissionManager.totalQuests();
        int act = mm.currentAct();
        boolean finished = act == 0;
        drawHeader(canvas, "JORNADA DO TREINADOR",
                "Missões cumpridas: " + mm.doneCount() + " / " + total + "   •   recompensas entregues automaticamente", exitBtn);

        // barra geral da jornada
        bar(canvas, new RectF(36, 118, w - 36, 134), mm.doneCount() / (float) total, C_ACCENT);

        if (finished) {
            panel(canvas, new RectF(36, 160, w - 36, 300), C_PANEL, C_GOOD, 3);
            text(canvas, "🏆 Jornada completa!", w / 2f, 220, 34, C_GOOD, Paint.Align.CENTER, true);
            text(canvas, "Você cumpriu todas as missões. Continue evoluindo, fortaleça a base e suba a Torre.", w / 2f, 262, 22, Color.WHITE, Paint.Align.CENTER, false);
            return;
        }

        text(canvas, MissionManager.ACT_NAMES[act], 40, 172, 28, Color.WHITE, Paint.Align.LEFT, true);
        List<Quest> quests = mm.questsOfAct(act);
        float top = 190, gap = 10;
        float cardH = Math.min(130, (h - top - 20 - gap * (quests.size() - 1)) / Math.max(1, quests.size()));
        float y = top;
        for (Quest q : quests) {
            boolean done = mm.isQuestDone(q);
            int prog = done ? q.target : Math.min(q.target, mm.progress(q, gv));
            RectF card = new RectF(36, y, w - 36, y + cardH);
            panel(canvas, card, done ? Color.argb(230, 20, 48, 34) : C_PANEL, done ? C_GOOD : C_LINE, 2);
            text(canvas, (done ? "✅ " : "🎯 ") + q.title, card.left + 18, card.top + 34, 26, done ? C_GOOD : Color.WHITE, Paint.Align.LEFT, true);
            paint.setTextSize(19);
            text(canvas, fit(q.desc, card.width() * 0.62f), card.left + 18, card.top + 62, 19, Color.rgb(210, 216, 230), Paint.Align.LEFT, false);
            if (cardH >= 100) {
                paint.setTextSize(17);
                text(canvas, fit("Dica: " + q.hint, card.width() - 36), card.left + 18, card.top + 88, 17, C_MUTED, Paint.Align.LEFT, false);
            }
            text(canvas, q.rewardText(), card.right - 18, card.top + 34, 20, Color.rgb(255, 214, 90), Paint.Align.RIGHT, true);
            RectF pb = new RectF(card.right - 260, card.top + 48, card.right - 18, card.top + 64);
            bar(canvas, pb, prog / (float) q.target, done ? C_GOOD : C_ACCENT);
            text(canvas, prog + " / " + q.target, pb.centerX(), pb.bottom + 20, 16, Color.WHITE, Paint.Align.CENTER, false);
            y += cardH + gap;
        }
    }
}
