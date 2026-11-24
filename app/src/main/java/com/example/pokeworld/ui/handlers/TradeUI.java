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
import com.example.pokeworld.managers.TradeManager;

import java.util.ArrayList;
import java.util.List;

/** Mercado de Trocas: Pokémon duplicados (fora do time) viram itens. */
public class TradeUI extends BaseUI {
    public final RectF exitBtn = new RectF(), prevBtn = new RectF(), nextBtn = new RectF();
    public final List<RectF> tradeBtns = new ArrayList<>();
    public final List<TradeManager.Offer> offers = new ArrayList<>();
    public int page = 0;

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        drawHeader(canvas, "🔁 MERCADO DE TROCAS", "Troque repetidos (fora do time, sem boost). Sempre fica 1 de cada.", exitBtn);
        tradeBtns.clear(); offers.clear(); prevBtn.setEmpty(); nextBtn.setEmpty();

        List<TradeManager.Offer> all = TradeManager.offers(gv);
        RectF area = new RectF(30, 124, w - 30, h - 24);
        panel(canvas, area, C_PANEL, C_LINE, 2);
        if (all.isEmpty()) {
            text(canvas, "Você não tem Pokémon repetidos para trocar.", area.centerX(), area.centerY() - 10, 24, Color.WHITE, Paint.Align.CENTER, true);
            text(canvas, "Capture o mesmo Pokémon duas vezes (mesma raridade) e mande o extra para a Caixa.", area.centerX(), area.centerY() + 26, 18, C_MUTED, Paint.Align.CENTER, false);
            return;
        }
        float ch = 92, gap = 8;
        int perPage = Math.max(1, (int) ((area.height() - 70 + gap) / (ch + gap)));
        int pages = (all.size() + perPage - 1) / perPage;
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= all.size()) break;
            TradeManager.Offer o = all.get(idx);
            RectF r = new RectF(area.left + 14, area.top + 14 + i * (ch + gap), area.right - 14, area.top + 14 + i * (ch + gap) + ch);
            panel(canvas, r, Color.argb(120, 10, 16, 28), o.rarityColor | 0xFF000000, 2);
            Bitmap bmp = SpriteManager.getInstance().getUnitSprite(o.species, 0, 0, o.shiny);
            if (bmp != null) canvas.drawBitmap(bmp, null, new Rect((int) r.left + 8, (int) r.top + 8, (int) r.left + 8 + 72, (int) r.top + 8 + 72), null);
            text(canvas, fit(o.species + (o.shiny ? " ✨" : "") + "  [" + o.rarityLabel + "]  x" + o.spare + " sobrando", r.width() - 380), r.left + 92, r.top + 36, 22, Color.WHITE, Paint.Align.LEFT, true);
            text(canvas, fit("Recebe: " + o.rewardText, r.width() - 380), r.left + 92, r.top + 66, 17, Color.rgb(255, 214, 90), Paint.Align.LEFT, false);
            RectF b = new RectF(r.right - 230, r.top + 18, r.right - 14, r.bottom - 18);
            tradeBtns.add(b); offers.add(o);
            actionButton(canvas, b, "TROCAR 1", true, C_GOOD);
        }
        if (pages > 1) {
            prevBtn.set(area.left + 14, area.bottom - 50, area.left + 154, area.bottom - 10);
            nextBtn.set(area.right - 154, area.bottom - 50, area.right - 14, area.bottom - 10);
            actionButton(canvas, prevBtn, "◀", page > 0, C_ACCENT);
            actionButton(canvas, nextBtn, "▶", page < pages - 1, C_ACCENT);
            text(canvas, (page + 1) + " / " + pages, area.centerX(), area.bottom - 20, 20, C_MUTED, Paint.Align.CENTER, false);
        }
    }
}
