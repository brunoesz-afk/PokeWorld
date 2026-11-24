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
    public final RectF exitBtn = new RectF(), towerBtn = new RectF(), endlessBtn = new RectF(), gymBtn = new RectF(), rivalBtn = new RectF(), arenaBtn = new RectF(), rankBtn = new RectF();

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
        float cw = (w - 60 - 2 * gap) / 3f, ch = Math.min(210, (h - top - 24 - gap) / 2f);
        RectF[] g = new RectF[6];
        for (int i = 0; i < 6; i++) {
            float x = 30 + (i % 3) * (cw + gap), y = top + (i / 3) * (ch + gap);
            g[i] = new RectF(x, y, x + cw, y + ch);
        }
        com.example.pokeworld.managers.RewardManager.Store ps = com.example.pokeworld.managers.PointsManager.store();
        int rp = ps == null ? 0 : com.example.pokeworld.managers.ArenaLadder.rp(ps);
        int tier = com.example.pokeworld.managers.ArenaLadder.tierOf(rp);
        int tries = ps == null ? 0 : com.example.pokeworld.managers.ArenaLadder.triesLeft(ps);

        card(canvas, g[0], arenaBtn, "⚔️ ARENA", "Duelos 3x3 contra lutadores. Troque de Pokémon a cada 5 s!",
                "Tentativas hoje: " + tries + " / " + com.example.pokeworld.managers.ArenaLadder.TRIES_PER_DAY, "LUTAR", true, Color.rgb(255, 120, 80));
        card(canvas, g[1], rankBtn, "🏆 RANKING", "Suba de liga (Bronze → Lenda) e veja os melhores treinadores.",
                com.example.pokeworld.managers.ArenaLadder.TIER_ICON[tier] + " Liga " + com.example.pokeworld.managers.ArenaLadder.TIER_NAME[tier] + "  •  " + rp + " PR", "VER", true, com.example.pokeworld.managers.ArenaLadder.TIER_COLOR[tier]);
        card(canvas, g[2], towerBtn, "🗼 TORRE DE PROVAS", "Andares com hordas e chefes (5, 10, 15, 20).",
                "Maior andar concluído: " + cleared + " / " + TowerManager.MAX_FLOOR + (ev.towerGold > 0 ? "  •  🪙 x2 hoje!" : ""), "ENTRAR", true, C_ACCENT);
        card(canvas, g[3], endlessBtn, "♾️ TORRE INFINITA", "Andares sem fim, cada vez mais difíceis.",
                endlessOk ? "Seu recorde: andar " + gv.tower.getEndlessBest() : "🔒 Conclua o andar " + TowerManager.MAX_FLOOR + " da Torre", endlessOk ? "ENTRAR" : "BLOQUEADO", endlessOk, Color.rgb(190, 140, 255));
        card(canvas, g[4], gymBtn, "🥋 GINÁSIO", "Enfrente o Líder Brock e o seu Onix.",
                "Nível do líder: " + (gv.player.getLevel() + 3), "DESAFIAR", true, Color.rgb(235, 120, 90));
        card(canvas, g[5], rivalBtn, "🏰 BASES RIVAIS", "Ataque bases rivais com seu esquadrão (3 novas por dia).",
                "Poder do time: " + ExpeditionManager.partyPower(gv) + (ev.baseReward > 0 ? "  •  +60% hoje!" : ""), "ATACAR", true, C_GOOD);
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
