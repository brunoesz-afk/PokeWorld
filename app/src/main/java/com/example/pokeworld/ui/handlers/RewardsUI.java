package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.EventCalendar;
import com.example.pokeworld.managers.RewardManager;

/** Central de Recompensas: Presente Diário de 7 dias, Baú do Evento, Recarga e "Coletar tudo". */
public class RewardsUI extends BaseUI {
    public final RectF exitBtn = new RectF(), giftBtn = new RectF(), chestBtn = new RectF(), rechargeBtn = new RectF(), allBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        RewardManager.Store s = RewardManager.of(gv.activity.dbHelper);
        int pending = RewardManager.pendingCount(gv, s);
        drawHeader(canvas, "🎁 CENTRAL DE RECOMPENSAS", "Pegue o que está pronto. Os presentes diários contam dias seguidos.", exitBtn);

        // ---- trilha de 7 dias
        float top = 120, pad = 30, gap = 10;
        float cw = (w - 2 * pad - 6 * gap) / 7f, ch = Math.min(190, h * 0.30f);
        int claimed = RewardManager.giftClaimedInCycle(s);
        boolean ready = RewardManager.giftReady(s);
        int todayIdx = ready ? claimed : -1;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 260.0);
        for (int i = 0; i < 7; i++) {
            RectF r = new RectF(pad + i * (cw + gap), top, pad + i * (cw + gap) + cw, top + ch);
            boolean done = i < claimed, isToday = i == todayIdx;
            int stroke = isToday ? Color.argb((int) (170 + 85 * pulse), 255, 205, 80) : done ? C_GOOD : C_LINE;
            panel(canvas, r, isToday ? Color.argb(240, 70, 54, 16) : C_PANEL, stroke, isToday ? 4 : 2);
            text(canvas, "DIA " + (i + 1), r.centerX(), r.top + 28, 18, done ? C_GOOD : Color.WHITE, Paint.Align.CENTER, true);
            text(canvas, done ? "✔" : RewardManager.GIFTS[i].icon, r.centerX(), r.top + 78, 40, Color.WHITE, Paint.Align.CENTER, false);
            wrap(canvas, RewardManager.GIFTS[i].text(), r.left + 8, r.top + 104, r.width() - 16, 14, done ? C_MUTED : Color.rgb(255, 224, 140), 18);
        }
        giftBtn.set(pad, top + ch + 12, w - pad, top + ch + 12 + 56);
        int streak = RewardManager.giftStreak(s);
        if (ready) actionButton(canvas, giftBtn, "🎁 PEGAR PRESENTE DO DIA " + (claimed + 1), true, C_ACCENT);
        else actionButton(canvas, giftBtn, "✔ Presente de hoje pego — volte amanhã  (sequência: " + streak + " dias)", false, C_GOOD);

        // ---- cartões: baú do evento e recarga
        float cy = giftBtn.bottom + 16, cardH = Math.min(170, h - cy - 96 - 16);
        float half = (w - 2 * pad - gap) / 2f;
        EventCalendar.Event ev = EventCalendar.today();
        RectF a = new RectF(pad, cy, pad + half, cy + cardH), b = new RectF(a.right + gap, cy, w - pad, cy + cardH);

        boolean chest = RewardManager.chestReady(s);
        panel(canvas, a, C_PANEL, chest ? Color.rgb(255, 150, 90) : C_LINE, chest ? 3 : 1);
        text(canvas, fit(ev.icon + " BAÚ DO EVENTO — " + ev.name, a.width() - 32), a.left + 16, a.top + 34, 22, Color.WHITE, Paint.Align.LEFT, true);
        RewardManager.Prize cp = RewardManager.eventChest();
        text(canvas, fit("Hoje: " + cp.text(), a.width() - 32), a.left + 16, a.top + 70, 18, Color.rgb(255, 224, 140), Paint.Align.LEFT, false);
        chestBtn.set(a.left + 16, a.bottom - 58, a.right - 16, a.bottom - 12);
        actionButton(canvas, chestBtn, chest ? "ABRIR BAÚ" : "✔ Aberto hoje", chest, Color.rgb(255, 150, 90));

        boolean rech = RewardManager.rechargeReady(s);
        panel(canvas, b, C_PANEL, rech ? C_GOOD : C_LINE, rech ? 3 : 1);
        text(canvas, "🔋 RECARGA DE SUPRIMENTOS", b.left + 16, b.top + 34, 22, Color.WHITE, Paint.Align.LEFT, true);
        wrap(canvas, "Cura o time e completa até " + RewardManager.POTION_CAP + " Poções e " + RewardManager.BALL_CAP + " Pokébolas. Disponível a cada 6 horas.",
                b.left + 16, b.top + 62, b.width() - 32, 16, C_MUTED, 22);
        rechargeBtn.set(b.left + 16, b.bottom - 58, b.right - 16, b.bottom - 12);
        actionButton(canvas, rechargeBtn, rech ? "RECARREGAR" : "Próxima em " + RewardManager.countdown(RewardManager.rechargeSecondsLeft(s)), rech, C_GOOD);

        // ---- coletar tudo
        allBtn.set(pad, h - 84, w - pad, h - 24);
        actionButton(canvas, allBtn, pending > 0 ? "✨ COLETAR TUDO (" + pending + ")" : "Nada para reivindicar agora", pending > 0, C_ACCENT);
    }
}
