package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.EventCalendar;
import com.example.pokeworld.managers.ExpeditionManager;
import com.example.pokeworld.managers.TowerManager;

/** Coliseu: central dos desafios de combate. Cada cartão leva à tela do desafio e mostra o seu progresso. */
public class ColiseumUI extends BaseUI {
    public final RectF exitBtn = new RectF(), towerBtn = new RectF(), endlessBtn = new RectF(), gymBtn = new RectF(), rivalBtn = new RectF();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        drawHeader(canvas, "🏟️ COLISEU DE POKEWORLD", "Escolha seu desafio. Os prêmios e bônus de hoje aparecem aqui.", exitBtn);

        // bônus do dia
        EventCalendar.Event ev = EventCalendar.today();
        RectF bonus = new RectF(30, 120, w - 30, 190);
        panel(canvas, bonus, Color.argb(235, 60, 46, 14), C_ACCENT, 3);
        paint.setTextSize(24);
        text(canvas, fit("HOJE: " + ev.icon + " " + ev.name + " — " + ev.desc, bonus.width() - 40), bonus.centerX(), bonus.centerY() + 9, 24, Color.WHITE, Paint.Align.CENTER, true);

        int cleared = gv.tower.getHighestCleared();
        boolean endlessOk = gv.tower.isEndlessUnlocked();
        float top = 212, gap = 14;
        float cw = (w - 60 - gap) / 2f, ch = Math.min(210, (h - top - 24 - gap) / 2f);
        RectF a = new RectF(30, top, 30 + cw, top + ch), b = new RectF(a.right + gap, top, w - 30, top + ch);
        RectF c = new RectF(30, top + ch + gap, 30 + cw, top + 2 * ch + gap), d = new RectF(c.right + gap, top + ch + gap, w - 30, top + 2 * ch + gap);

        card(canvas, a, towerBtn, "🗼 TORRE DE PROVAS", "Andares com hordas e chefes (5, 10, 15, 20).",
                "Maior andar concluído: " + cleared + " / " + TowerManager.MAX_FLOOR + (ev.towerGold > 0 ? "   •   🪙 em dobro hoje!" : ""), "ENTRAR", true, C_ACCENT);
        card(canvas, b, endlessBtn, "♾️ TORRE INFINITA", "Andares sem fim, cada vez mais difíceis.",
                endlessOk ? "Seu recorde: andar " + gv.tower.getEndlessBest() : "🔒 Conclua o andar " + TowerManager.MAX_FLOOR + " da Torre para liberar", endlessOk ? "ENTRAR" : "BLOQUEADO", endlessOk, Color.rgb(190, 140, 255));
        card(canvas, c, gymBtn, "🥋 GINÁSIO", "Enfrente o Líder Brock e o seu Onix.",
                "Nível do líder: " + (gv.player.getLevel() + 3), "DESAFIAR", true, Color.rgb(235, 120, 90));
        card(canvas, d, rivalBtn, "⚔️ BASES RIVAIS", "Ataque bases rivais com seu esquadrão (3 novas por dia).",
                "Poder do seu time: " + ExpeditionManager.partyPower(gv) + (ev.baseReward > 0 ? "   •   prêmios +60% hoje!" : ""), "ATACAR", true, C_GOOD);
    }

    private void card(Canvas canvas, RectF r, RectF btn, String title, String desc, String status, String label, boolean enabled, int color) {
        panel(canvas, r, C_PANEL, enabled ? color : C_LINE, enabled ? 3 : 1);
        paint.setTextSize(26);
        text(canvas, fit(title, r.width() - 40), r.left + 20, r.top + 44, 26, Color.WHITE, Paint.Align.LEFT, true);
        paint.setTextSize(17);
        wrap(canvas, desc, r.left + 20, r.top + 78, r.width() - 40, 17, C_MUTED, 24);
        paint.setTextSize(18);
        text(canvas, fit(status, r.width() - 40), r.left + 20, r.top + 128, 18, enabled ? Color.rgb(255, 214, 90) : C_MUTED, Paint.Align.LEFT, false);
        btn.set(r.left + 20, r.bottom - 66, r.right - 20, r.bottom - 14);
        actionButton(canvas, btn, label, enabled, color);
    }
}
