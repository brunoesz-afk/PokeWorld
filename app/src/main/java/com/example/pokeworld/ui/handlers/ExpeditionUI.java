package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.ExpeditionManager;

import java.util.ArrayList;
import java.util.List;

/** Expedições (esquerda) e Bases Rivais (direita). Chamada pelo Capitão de Expedição no Quartel. */
public class ExpeditionUI extends BaseUI {
    public final RectF exitBtn = new RectF();
    public final List<RectF> sendBtns = new ArrayList<>();
    public final RectF collectBtn = new RectF();
    public final List<RectF> attackBtns = new ArrayList<>();

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        drawHeader(canvas, "EXPEDIÇÕES E BASES RIVAIS",
                "Equipe da expedição (melhores fora do time): poder " + ExpeditionManager.squadPower(gv) + "   •   seu time: poder " + ExpeditionManager.partyPower(gv), exitBtn);
        sendBtns.clear(); attackBtns.clear(); collectBtn.setEmpty();

        float mid = w * 0.5f;
        RectF left = new RectF(30, 124, mid - 9, h - 24), right = new RectF(mid + 9, 124, w - 30, h - 24);
        panel(canvas, left, C_PANEL, C_LINE, 2);
        panel(canvas, right, C_PANEL, C_LINE, 2);

        // ---- expedições
        text(canvas, "🎒 EXPEDIÇÕES", left.left + 20, left.top + 36, 24, Color.WHITE, Paint.Align.LEFT, true);
        int active = ExpeditionManager.activeDest(gv);
        float y = left.top + 56;
        float ch = Math.min(120, (left.bottom - y - 12 - 20) / 3f);
        for (int i = 0; i < ExpeditionManager.DESTS.length; i++) {
            ExpeditionManager.Dest d = ExpeditionManager.DESTS[i];
            RectF c = new RectF(left.left + 14, y, left.right - 14, y + ch);
            boolean mine = active == i;
            panel(canvas, c, mine ? C_PANEL_HI : Color.argb(120, 10, 16, 28), mine ? C_ACCENT : C_LINE, mine ? 3 : 1);
            text(canvas, fit(d.icon + " " + d.name, c.width() - 190), c.left + 14, c.top + 34, 21, Color.WHITE, Paint.Align.LEFT, true);
            text(canvas, d.minutes + " min  •  🪙 ~" + d.gold + "  •  " + d.items[0] + ", " + d.items[1] + "…", c.left + 14, c.top + 62, 15, C_MUTED, Paint.Align.LEFT, false);
            RectF b = new RectF(c.right - 170, c.top + 12, c.right - 10, c.bottom - 12);
            if (mine) {
                int s = ExpeditionManager.secondsLeft(gv);
                if (s == 0) { collectBtn.set(b); actionButton(canvas, b, "COLETAR", true, C_GOOD); }
                else actionButton(canvas, b, String.format("⏳ %d:%02d", s / 60, s % 60), false, C_ACCENT);
                sendBtns.add(new RectF());
            } else {
                b = new RectF(b);
                sendBtns.add(b);
                actionButton(canvas, b, "ENVIAR", active < 0 && ExpeditionManager.squadSize(gv) > 0, C_ACCENT);
            }
            y += ch + 8;
        }
        text(canvas, "Pokémon da Caixa e da Defesa viajam por você, mesmo com o jogo fechado.", left.left + 20, left.bottom - 16, 14, C_MUTED, Paint.Align.LEFT, false);

        // ---- bases rivais
        text(canvas, "🏴 BASES RIVAIS (renovam todo dia)", right.left + 20, right.top + 36, 24, Color.WHITE, Paint.Align.LEFT, true);
        y = right.top + 56;
        for (int i = 0; i < 3; i++) {
            RectF c = new RectF(right.left + 14, y, right.right - 14, y + ch);
            int st = ExpeditionManager.rivalState(gv, i);
            panel(canvas, c, st == 1 ? Color.argb(230, 20, 48, 34) : st == 2 ? Color.argb(230, 56, 24, 24) : Color.argb(120, 10, 16, 28), st == 1 ? C_GOOD : st == 2 ? C_BAD : C_LINE, 2);
            text(canvas, fit(ExpeditionManager.rivalName(i) + " — " + ExpeditionManager.TIER[i], c.width() - 190), c.left + 14, c.top + 34, 21, Color.WHITE, Paint.Align.LEFT, true);
            text(canvas, "Poder " + ExpeditionManager.rivalPower(gv, i) + "  •  chance de vitória " + ExpeditionManager.winChance(gv, i) + "%", c.left + 14, c.top + 62, 15, C_MUTED, Paint.Align.LEFT, false);
            RectF b = new RectF(c.right - 170, c.top + 12, c.right - 10, c.bottom - 12);
            attackBtns.add(b);
            actionButton(canvas, b, st == 1 ? "✅ SAQUEADA" : st == 2 ? "❌ DERROTA" : "⚔ ATACAR", st == 0, Color.rgb(230, 110, 90));
            y += ch + 8;
        }
        text(canvas, "Quanto mais forte o time (nível, raridade, boost), maior a chance.", right.left + 20, right.bottom - 16, 14, C_MUTED, Paint.Align.LEFT, false);
    }
}
