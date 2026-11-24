package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.ArenaLadder;
import com.example.pokeworld.managers.ArenaNet;
import com.example.pokeworld.managers.FriendManager;
import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.RewardManager;

import java.util.List;

/** Ranking da Arena: placar global (servidor) ou a liga local de treinadores quando está sem conexão. */
public class ArenaRankUI extends BaseUI {
    public final RectF exitBtn = new RectF();
    public final RectF[] tabs = {new RectF(), new RectF()};
    public final RectF listArea = new RectF();
    public int page = -1;                 // 0 global, 1 local; -1 = escolhe sozinho ao abrir
    public float scroll = 0, maxScroll = 0;
    private static final float ROW = 46;

    public void open() { page = -1; scroll = 0; }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        RewardManager.Store s = PointsManager.store();
        drawHeader(canvas, "🏆 RANKING DA ARENA", "Suba de liga vencendo lutadores. Pontos de ranking (PR) decidem sua posição", exitBtn);
        ArenaUI au = (ArenaUI) gv.ui.getHandler(com.example.pokeworld.ui.UIManager.UIState.ARENA);
        ArenaNet.refresh(s, au == null ? 0 : au.myPower(), false);
        if (page < 0) page = ArenaNet.loaded() ? 0 : 1;
        boolean global = page == 0 && ArenaNet.loaded();

        // ---- cartão da esquerda: sua liga e a tabela de ligas
        RectF c = new RectF(30, 118, 360, h - 24);
        panel(canvas, c, C_PANEL, C_LINE, 2);
        int rp = ArenaLadder.rp(s), tier = ArenaLadder.tierOf(rp), tc = ArenaLadder.TIER_COLOR[tier];
        text(canvas, ArenaLadder.TIER_ICON[tier] + " " + ArenaLadder.TIER_NAME[tier], c.centerX(), c.top + 52, 34, tc, Paint.Align.CENTER, true);
        text(canvas, rp + " PR", c.centerX(), c.top + 90, 28, Color.WHITE, Paint.Align.CENTER, true);
        List<ArenaLadder.Row> rows = global ? ArenaNet.top() : ArenaLadder.localBoard(s, FriendManager.getInstance().getMyName(), ArenaLadder.weekIndex());
        int myPos = 0;
        if (global) myPos = ArenaNet.myRank();
        else for (ArenaLadder.Row r : rows) if (r.me) myPos = r.rank;
        String posTxt = myPos > 0 ? "Sua posição: #" + myPos + (global ? " de " + ArenaNet.total() : " de " + rows.size()) : "Sem posição ainda — vença uma luta!";
        text(canvas, posTxt, c.centerX(), c.top + 122, 17, C_ACCENT, Paint.Align.CENTER, true);
        float ty = c.top + 160;
        for (int i = ArenaLadder.TIER_NAME.length - 1; i >= 0; i--) {
            boolean cur = i == tier;
            RectF r = new RectF(c.left + 16, ty, c.right - 16, ty + 36);
            panel(canvas, r, cur ? Color.argb(235, 54, 70, 104) : Color.argb(120, 20, 28, 44), cur ? ArenaLadder.TIER_COLOR[i] : C_LINE, cur ? 3 : 1);
            text(canvas, ArenaLadder.TIER_ICON[i] + " " + ArenaLadder.TIER_NAME[i], r.left + 12, r.centerY() + 7, 19, ArenaLadder.TIER_COLOR[i], Paint.Align.LEFT, true);
            text(canvas, ArenaLadder.TIER_MIN[i] + "+ PR", r.right - 12, r.centerY() + 6, 16, cur ? Color.WHITE : C_MUTED, Paint.Align.RIGHT, false);
            ty += 42;
        }

        // ---- abas e lista
        float lx = c.right + 16;
        tabs[0].set(lx, 118, lx + 230, 118 + 44);
        tabs[1].set(lx + 240, 118, lx + 470, 118 + 44);
        chip(canvas, tabs[0], "🌐 Global" + (ArenaNet.loaded() ? "" : " (offline)"), page == 0, C_ACCENT);
        chip(canvas, tabs[1], "🏠 Liga local", page == 1, C_ACCENT);

        RectF head = new RectF(lx, 172, w - 30, 172 + 36);
        panel(canvas, head, Color.argb(235, 30, 42, 66), C_LINE, 1);
        text(canvas, "#", head.left + 16, head.centerY() + 7, 17, C_MUTED, Paint.Align.LEFT, true);
        text(canvas, "TREINADOR", head.left + 80, head.centerY() + 7, 17, C_MUTED, Paint.Align.LEFT, true);
        text(canvas, "V / D", head.right - 200, head.centerY() + 7, 17, C_MUTED, Paint.Align.RIGHT, true);
        text(canvas, "PR", head.right - 16, head.centerY() + 7, 17, C_MUTED, Paint.Align.RIGHT, true);

        listArea.set(lx, 214, w - 30, h - 24);
        if (page == 0 && !ArenaNet.loaded()) {
            panel(canvas, listArea, C_PANEL, C_LINE, 1);
            wrap(canvas, "Sem conexão com o servidor. O ranking global aparece quando você estiver online (suas vitórias são enviadas automaticamente). Veja a Liga local ao lado.", listArea.left + 30, listArea.top + 60, listArea.width() - 60, 22, C_MUTED, 30);
            maxScroll = 0;
            return;
        }
        float total = rows.size() * ROW;
        maxScroll = Math.max(0, total - listArea.height());
        scroll = Math.max(0, Math.min(maxScroll, scroll));
        canvas.save();
        canvas.clipRect(listArea);
        for (int i = 0; i < rows.size(); i++) {
            float y = listArea.top + i * ROW - scroll;
            if (y + ROW < listArea.top || y > listArea.bottom) continue;
            ArenaLadder.Row r = rows.get(i);
            RectF rr = new RectF(listArea.left, y + 2, listArea.right - 10, y + ROW - 2);
            int tcol = ArenaLadder.TIER_COLOR[ArenaLadder.tierOf(r.rp)];
            panel(canvas, rr, r.me ? Color.argb(240, 70, 60, 24) : C_PANEL, r.me ? C_ACCENT : Color.argb(60, 150, 175, 215), r.me ? 3 : 1);
            String medal = r.rank == 1 ? "🥇" : r.rank == 2 ? "🥈" : r.rank == 3 ? "🥉" : "#" + r.rank;
            text(canvas, medal, rr.left + 16, rr.centerY() + 8, 22, Color.WHITE, Paint.Align.LEFT, true);
            paint.setTextSize(20);
            text(canvas, fit(ArenaLadder.TIER_ICON[ArenaLadder.tierOf(r.rp)] + " " + r.name + (r.me ? "  (você)" : ""), rr.width() - 330), rr.left + 80, rr.centerY() + 7, 20, r.me ? C_ACCENT : Color.WHITE, Paint.Align.LEFT, true);
            text(canvas, r.wins + " / " + r.losses, rr.right - 200, rr.centerY() + 7, 17, C_MUTED, Paint.Align.RIGHT, false);
            text(canvas, String.valueOf(r.rp), rr.right - 16, rr.centerY() + 8, 22, tcol, Paint.Align.RIGHT, true);
        }
        canvas.restore();
        scrollbar(canvas, listArea, scroll, maxScroll);
    }
}
