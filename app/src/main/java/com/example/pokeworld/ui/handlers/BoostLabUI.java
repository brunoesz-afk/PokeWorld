package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.managers.SpriteManager;

import java.util.ArrayList;
import java.util.List;

/** Laboratório de Boost: transfere o boost de um Pokémon para outro, ou recicla em Gemas de Boost. */
public class BoostLabUI extends BaseUI {
    public final RectF exitBtn = new RectF(), prevBtn = new RectF(), nextBtn = new RectF(), transferBtn = new RectF(), resetBtn = new RectF();
    public final List<RectF> cardRects = new ArrayList<>();
    public final List<CapturedCreature> cards = new ArrayList<>();
    public CapturedCreature source, target;
    public int page = 0;

    public static int transferCost(CapturedCreature src) { return 300 * Math.max(1, src.boost); }
    public static int resetCost(CapturedCreature src) { return 100 * Math.max(1, src.boost); }
    /** Reciclar devolve gemas: boost +1 = nada, +2 = 1, +3 = 2... (a primeira gema é a taxa do laboratório). */
    public static int refund(CapturedCreature src) { return Math.max(0, src.boost - 1); }
    public static int transferResult(CapturedCreature src) { return Math.max(0, src.boost - 1); }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        drawHeader(canvas, "💎 LABORATÓRIO DE BOOST",
                source == null ? "1) toque em um Pokémon com boost (💎)" : target == null ? "2) toque em quem vai RECEBER o boost (ou recicle)" : "3) confirme a transferência", exitBtn);
        cardRects.clear(); cards.clear();
        prevBtn.setEmpty(); nextBtn.setEmpty(); transferBtn.setEmpty(); resetBtn.setEmpty();

        List<CapturedCreature> all = new ArrayList<>(gv.player.getCreatureCollection());
        // com boost primeiro
        java.util.Collections.sort(all, (a, b) -> b.boost - a.boost);
        RectF area = new RectF(30, 124, w - 30, h - 24);
        panel(canvas, area, C_PANEL, C_LINE, 2);

        float cw = 160, ch = 100, gap = 10;
        int cols = Math.max(1, (int) ((area.width() - 28 + gap) / (cw + gap)));
        int rows = Math.max(1, (int) ((area.height() - 14 - 120 + gap) / (ch + gap)));
        int per = cols * rows, pages = Math.max(1, (all.size() + per - 1) / per);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < per; i++) {
            int idx = page * per + i;
            if (idx >= all.size()) break;
            CapturedCreature c = all.get(idx);
            RectF r = new RectF(area.left + 14 + (i % cols) * (cw + gap), area.top + 14 + (i / cols) * (ch + gap), 0, 0);
            r.right = r.left + cw; r.bottom = r.top + ch;
            boolean isSrc = c == source, isDst = c == target;
            panel(canvas, r, isSrc ? C_PANEL_HI : isDst ? Color.argb(230, 20, 48, 34) : Color.argb(120, 10, 16, 28), isSrc ? Color.rgb(120, 200, 255) : isDst ? C_GOOD : (c.rarity.getColor() | 0xFF000000), (isSrc || isDst) ? 4 : 1);
            Bitmap bmp = SpriteManager.getInstance().getUnitSprite(c.name, 0, 0, c.isShiny);
            if (bmp != null) canvas.drawBitmap(bmp, null, new Rect((int) r.left + 6, (int) r.top + 6, (int) r.left + 6 + 60, (int) r.top + 66), null);
            text(canvas, fit(c.name, cw - 12), r.left + 8, r.bottom - 26, 15, Color.WHITE, Paint.Align.LEFT, true);
            text(canvas, c.rarity.getLabel() + (c.isInParty ? " • time" : ""), r.left + 8, r.bottom - 8, 13, C_MUTED, Paint.Align.LEFT, false);
            if (c.boost > 0) text(canvas, "💎+" + c.boost, r.right - 8, r.top + 28, 20, Color.rgb(120, 200, 255), Paint.Align.RIGHT, true);
            cards.add(c); cardRects.add(r);
        }

        // rodapé: ações
        float by = area.bottom - 58;
        if (pages > 1) {
            prevBtn.set(area.left + 14, by, area.left + 104, by + 44);
            nextBtn.set(area.left + 114, by, area.left + 204, by + 44);
            actionButton(canvas, prevBtn, "◀", page > 0, C_ACCENT);
            actionButton(canvas, nextBtn, "▶", page < pages - 1, C_ACCENT);
        }
        if (source != null) {
            resetBtn.set(area.right - 14 - 330 - 10 - 330, by, area.right - 14 - 330 - 10, by + 44);
            transferBtn.set(area.right - 14 - 330, by, area.right - 14, by + 44);
            actionButton(canvas, resetBtn, "♻ RECICLAR (" + resetCost(source) + "🪙 ➜ " + refund(source) + " 💎)", true, Color.rgb(230, 150, 90));
            boolean ok = target != null && transferResult(source) > target.boost;
            actionButton(canvas, transferBtn, target == null ? "ESCOLHA O DESTINO" : "➜ TRANSFERIR +" + transferResult(source) + " (" + transferCost(source) + "🪙)", ok, Color.rgb(120, 200, 255));
            text(canvas, "Transferir perde 1 nível de boost. Reciclar devolve gemas (nível -1).", area.left + 226, by + 28, 15, C_MUTED, Paint.Align.LEFT, false);
        }
    }
}
