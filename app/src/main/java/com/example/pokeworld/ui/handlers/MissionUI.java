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
    public RectF tabJourney = new RectF(), tabDaily = new RectF();
    public RectF tabEvents = new RectF();
    public boolean showDaily = false, showEvents = false;

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        MissionManager mm = gv.missions;
        int total = MissionManager.totalQuests();
        int act = mm.currentAct();
        boolean finished = act == 0;
        drawHeader(canvas, "JORNADA DO TREINADOR",
                "Missões cumpridas: " + mm.doneCount() + " / " + total + "   •   recompensas entregues automaticamente", exitBtn);

        // abas: Jornada | Diárias
        tabJourney.set(36, 112, 36 + 220, 154);
        tabDaily.set(36 + 232, 112, 36 + 232 + 260, 154);
        tabEvents.set(36 + 232 + 272, 112, 36 + 232 + 272 + 250, 154);
        actionButton(canvas, tabJourney, "📖 JORNADA", showDaily || showEvents, C_ACCENT);
        actionButton(canvas, tabDaily, "📅 DIÁRIAS " + mm.dailiesDone() + "/3", !showDaily || showEvents, C_GOOD);
        actionButton(canvas, tabEvents, "🎉 EVENTOS", !showEvents, Color.rgb(255, 150, 90));
        if (showEvents) { drawEvents(canvas, w, h); return; }
        if (showDaily) { drawDaily(canvas, gv, w, h); return; }
        // barra geral da jornada
        bar(canvas, new RectF(36, 162, w - 36, 176), mm.doneCount() / (float) total, C_ACCENT);

        if (finished) {
            panel(canvas, new RectF(36, 200, w - 36, 340), C_PANEL, C_GOOD, 3);
            text(canvas, "🏆 Jornada completa!", w / 2f, 260, 34, C_GOOD, Paint.Align.CENTER, true);
            text(canvas, "Você cumpriu todas as missões. Continue evoluindo, fortaleça a base e suba a Torre.", w / 2f, 302, 22, Color.WHITE, Paint.Align.CENTER, false);
            return;
        }

        text(canvas, MissionManager.ACT_NAMES[act], 40, 212, 28, Color.WHITE, Paint.Align.LEFT, true);
        List<Quest> quests = mm.questsOfAct(act);
        float top = 230, gap = 10;
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

    private void drawDaily(Canvas canvas, GameView gv, float w, float h) {
        MissionManager mm = gv.missions;
        java.util.List<MissionManager.Daily> ds = mm.dailies();
        text(canvas, "Missões de hoje — renovam à meia-noite", 40, 196, 24, Color.WHITE, Paint.Align.LEFT, true);
        float y = 214, gap = 10, cardH = Math.min(110, (h - y - 120) / 3f);
        for (int i = 0; i < ds.size(); i++) {
            MissionManager.Daily q = ds.get(i);
            boolean done = mm.dailyDone(i);
            int prog = done ? q.target : mm.dailyProgress(q);
            RectF card = new RectF(36, y, w - 36, y + cardH);
            panel(canvas, card, done ? Color.argb(230, 20, 48, 34) : C_PANEL, done ? C_GOOD : C_LINE, 2);
            text(canvas, fit((done ? "✅ " : "🎯 ") + q.title, card.width() * 0.6f), card.left + 18, card.top + 36, 24, done ? C_GOOD : Color.WHITE, Paint.Align.LEFT, true);
            text(canvas, q.rewardText(), card.right - 18, card.top + 36, 20, Color.rgb(255, 214, 90), Paint.Align.RIGHT, true);
            RectF pb = new RectF(card.left + 18, card.top + 54, card.right - 18, card.top + 70);
            bar(canvas, pb, prog / (float) q.target, done ? C_GOOD : C_ACCENT);
            text(canvas, prog + " / " + q.target, pb.centerX(), pb.bottom + 20, 16, Color.WHITE, Paint.Align.CENTER, false);
            y += cardH + gap;
        }
        RectF bonus = new RectF(36, y, w - 36, y + 70);
        boolean got = mm.dailyBonusTaken();
        panel(canvas, bonus, got ? Color.argb(230, 20, 48, 34) : C_PANEL, got ? C_GOOD : Color.rgb(255, 200, 90), 2);
        text(canvas, (got ? "✅ " : "🎁 ") + "Complete as 3 diárias: 💎 2 gemas + 1x Gema de Boost", bonus.centerX(), bonus.centerY() + 8, 22, got ? C_GOOD : Color.rgb(255, 214, 90), Paint.Align.CENTER, true);
    }

    private void drawEvents(Canvas canvas, float w, float h) {
        text(canvas, "Calendário semanal — um bônus diferente a cada dia", 40, 196, 24, Color.WHITE, Paint.Align.LEFT, true);
        int today = com.example.pokeworld.managers.EventCalendar.dayIndex();
        float y = 214, gap = 8, ch = Math.min(78, (h - y - 20 - gap * 6) / 7f);
        // começa em segunda e termina em domingo
        for (int n = 0; n < 7; n++) {
            int d = (n + 1) % 7;
            com.example.pokeworld.managers.EventCalendar.Event e = com.example.pokeworld.managers.EventCalendar.WEEK[d];
            boolean isToday = d == today;
            RectF card = new RectF(36, y, w - 36, y + ch);
            panel(canvas, card, isToday ? Color.argb(235, 70, 52, 20) : C_PANEL, isToday ? C_ACCENT : C_LINE, isToday ? 4 : 1);
            text(canvas, com.example.pokeworld.managers.EventCalendar.DAY_NAMES[d] + (isToday ? "  • HOJE" : ""), card.left + 18, card.centerY() + 8, 20, isToday ? C_ACCENT : C_MUTED, Paint.Align.LEFT, true);
            text(canvas, fit(e.icon + " " + e.name, 360), card.left + 210, card.centerY() - 4, 22, Color.WHITE, Paint.Align.LEFT, true);
            text(canvas, fit(e.desc, card.width() - 240), card.left + 210, card.centerY() + 20, 16, isToday ? Color.rgb(255, 230, 150) : Color.rgb(190, 198, 214), Paint.Align.LEFT, false);
            y += ch + gap;
        }
    }
}
