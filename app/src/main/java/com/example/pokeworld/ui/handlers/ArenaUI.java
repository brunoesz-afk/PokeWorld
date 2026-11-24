package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.ArenaDuel;
import com.example.pokeworld.managers.ArenaLadder;
import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.RewardManager;
import com.example.pokeworld.managers.SpriteManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Arena: lista dos lutadores do dia (nível e poder), sua liga e as tentativas. Tocar em LUTAR abre o duelo. */
public class ArenaUI extends BaseUI {
    public final RectF exitBtn = new RectF(), rankBtn = new RectF();
    public final RectF[] fightBtns = new RectF[ArenaLadder.FIGHTERS];

    // cache: criar os times (Companion) todo quadro seria desperdício
    private List<ArenaLadder.Fighter> fighters;
    private final Map<Integer, List<ArenaDuel.Unit>> teams = new HashMap<>();
    private int myPower, myAvgLevel, cacheKey = -1;
    private long cacheAt = 0;

    public ArenaUI() { for (int i = 0; i < fightBtns.length; i++) fightBtns[i] = new RectF(); }

    public void invalidate() { cacheAt = 0; }
    public List<ArenaLadder.Fighter> fighters() { return fighters; }
    public int myPower() { return myPower; }

    private void refresh(GameView gv) {
        long now = System.currentTimeMillis();
        if (fighters != null && now - cacheAt < 4000) return;
        cacheAt = now;
        List<ArenaDuel.Unit> roster = ArenaLadder.playerRoster(gv);
        myPower = ArenaLadder.bestPower(roster);
        myAvgLevel = ArenaLadder.avgLevel(roster);
        int key = RewardManager.today() * 1000 + myAvgLevel;
        if (fighters == null || key != cacheKey) {
            cacheKey = key;
            fighters = ArenaLadder.fighters(RewardManager.today(), myAvgLevel);
            teams.clear();
            for (ArenaLadder.Fighter f : fighters) {
                List<ArenaDuel.Unit> t = ArenaLadder.teamOf(gv.activity, f);
                f.power = ArenaLadder.powerOf(t);
                teams.put(f.index, t);
            }
        }
    }

    /** Desenha o sprite de uma espécie com o pé em (cx, bottom). */
    public static void sprite(Canvas canvas, Paint p, String species, boolean shiny, int dir, int frame, float cx, float bottom, float size) {
        Bitmap bmp = SpriteManager.getInstance().getUnitSprite(species, dir, frame, shiny);
        if (bmp == null) return;
        canvas.drawBitmap(bmp, null, new Rect((int) (cx - size / 2), (int) (bottom - size), (int) (cx + size / 2), (int) bottom), p);
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        RewardManager.Store s = PointsManager.store();
        refresh(gv);
        drawHeader(canvas, "🏟️ ARENA DE LUTADORES", "Escolha um lutador, monte seu time e vença para subir de liga", exitBtn);
        int frame = (int) ((System.currentTimeMillis() / 350) % 3);

        float leftR = w * 0.68f;
        float gap = 8, top = 118;
        float rowH = (h - top - 24 - (ArenaLadder.FIGHTERS - 1) * gap) / ArenaLadder.FIGHTERS;
        int tries = ArenaLadder.triesLeft(s);
        for (int i = 0; i < fighters.size(); i++) {
            ArenaLadder.Fighter f = fighters.get(i);
            RectF r = new RectF(30, top + i * (rowH + gap), leftR, top + i * (rowH + gap) + rowH);
            boolean done = ArenaLadder.beaten(s, i);
            double ratio = myPower <= 0 ? 1 : f.power / (double) myPower;
            int dc = ratio <= 0.85 ? C_GOOD : ratio <= 1.2 ? Color.rgb(255, 214, 90) : C_BAD;
            String dn = ratio <= 0.85 ? "Fácil" : ratio <= 1.2 ? "Equilibrada" : "Difícil";
            panel(canvas, r, done ? Color.argb(235, 22, 40, 34) : C_PANEL, done ? C_GOOD : dc, 2);
            float cy = r.centerY();
            text(canvas, f.icon, r.left + 16, cy + 14, 40, Color.WHITE, Paint.Align.LEFT, false);
            paint.setTextSize(21);
            text(canvas, fit(f.name, 215), r.left + 78, cy - 6, 21, Color.WHITE, Paint.Align.LEFT, true);
            text(canvas, "Nv. " + f.level + "   ⚡ " + f.power, r.left + 78, cy + 20, 17, Color.rgb(255, 214, 90), Paint.Align.LEFT, false);
            text(canvas, dn, r.left + 78, r.bottom - 6, 13, dc, Paint.Align.LEFT, true);
            List<ArenaDuel.Unit> t = teams.get(f.index);
            float sz = Math.min(rowH - 6, 58);
            if (t != null) for (int k = 0; k < t.size(); k++) sprite(canvas, paint, t.get(k).species, false, 0, frame, r.left + 340 + k * (sz + 4) + sz / 2, r.bottom - 4, sz);
            float rx = r.left + 340 + 3 * (sz + 4) + 14;
            text(canvas, "+" + f.rpWin + " PR", rx, cy - 4, 18, Color.WHITE, Paint.Align.LEFT, true);
            text(canvas, "🪙 " + f.gold, rx, cy + 20, 16, Color.rgb(255, 214, 90), Paint.Align.LEFT, false);
            fightBtns[i].set(r.right - 128, r.top + 10, r.right - 12, r.bottom - 10);
            boolean can = !done && tries > 0;
            panel(canvas, fightBtns[i], done ? Color.rgb(34, 70, 52) : can ? C_ACCENT : Color.rgb(48, 54, 66), done ? C_GOOD : Color.WHITE, 2);
            paint.setTextSize(20);
            text(canvas, done ? "✔ VENCIDO" : "LUTAR", fightBtns[i].centerX(), fightBtns[i].centerY() + 7, 20, done ? C_GOOD : can ? Color.BLACK : C_MUTED, Paint.Align.CENTER, true);
        }

        // ---- cartão da liga
        RectF c = new RectF(leftR + 14, top, w - 30, h - 24);
        panel(canvas, c, C_PANEL, C_LINE, 2);
        int rp = ArenaLadder.rp(s), tier = ArenaLadder.tierOf(rp);
        int tc = ArenaLadder.TIER_COLOR[tier];
        float cx = c.centerX();
        text(canvas, ArenaLadder.TIER_ICON[tier], cx, c.top + 80, 64, Color.WHITE, Paint.Align.CENTER, false);
        text(canvas, "LIGA " + ArenaLadder.TIER_NAME[tier].toUpperCase(), cx, c.top + 118, 26, tc, Paint.Align.CENTER, true);
        text(canvas, rp + " PR", cx, c.top + 152, 24, Color.WHITE, Paint.Align.CENTER, true);
        int next = ArenaLadder.toNextTier(rp);
        float bx = c.left + 24, bw = c.width() - 48;
        float frac = next == 0 ? 1f : (rp - ArenaLadder.TIER_MIN[tier]) / (float) (ArenaLadder.TIER_MIN[tier + 1] - ArenaLadder.TIER_MIN[tier]);
        bar(canvas, new RectF(bx, c.top + 168, bx + bw, c.top + 184), frac, tc);
        paint.setTextSize(15);
        text(canvas, next == 0 ? "Liga máxima!" : "Faltam " + next + " PR para " + ArenaLadder.TIER_ICON[tier + 1] + " " + ArenaLadder.TIER_NAME[tier + 1], cx, c.top + 206, 15, C_MUTED, Paint.Align.CENTER, false);

        float y = c.top + 242;
        text(canvas, "Vitórias / Derrotas:  " + ArenaLadder.wins(s) + " / " + ArenaLadder.losses(s), c.left + 24, y, 17, Color.WHITE, Paint.Align.LEFT, false);
        text(canvas, "Tentativas hoje:  " + tries + " / " + ArenaLadder.TRIES_PER_DAY, c.left + 24, y + 28, 17, tries > 0 ? Color.WHITE : C_BAD, Paint.Align.LEFT, false);
        text(canvas, "Seu poder (3 melhores):  ⚡ " + myPower, c.left + 24, y + 56, 17, Color.rgb(255, 214, 90), Paint.Align.LEFT, false);
        float ny = wrap(canvas, "Na luta você pode trocar de Pokémon a cada 5 segundos. Vantagem de tipo faz diferença!", c.left + 24, y + 92, c.width() - 48, 15, C_MUTED, 20);

        rankBtn.set(c.left + 24, c.bottom - 74, c.right - 24, c.bottom - 18);
        actionButton(canvas, rankBtn, "🏆 RANKING", true, Color.rgb(190, 140, 255));
    }
}
