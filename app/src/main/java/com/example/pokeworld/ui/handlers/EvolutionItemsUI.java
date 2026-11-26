package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.EvolutionTable;
import com.example.pokeworld.entities.ItemCatalog;
import com.example.pokeworld.managers.SpriteManager;

import java.util.ArrayList;
import java.util.List;

/** Tela de pedras: lista os Pokémon (time e base) que evoluem com a pedra escolhida e mostra a prévia da evolução. */
public class EvolutionItemsUI extends BaseUI {
    public String stone = EvolutionTable.FIRE;
    public CapturedCreature selected;
    public float scroll = 0, maxScroll = 0;

    public RectF exitBtn = new RectF();
    public RectF evolveBtn = new RectF();
    public RectF listArea = new RectF();
    public List<Row> rows = new ArrayList<>();

    public static class Row {
        public final CapturedCreature creature;
        public final RectF rect = new RectF();
        Row(CapturedCreature c) { creature = c; }
    }

    public List<CapturedCreature> candidates(GameView gv) {
        List<CapturedCreature> out = new ArrayList<>();
        for (CapturedCreature c : gv.player.getCreatureCollection()) {
            if (EvolutionTable.canUseStone(c.name, stone)) out.add(c);
        }
        return out;
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        ItemCatalog.Info info = ItemCatalog.get(stone);
        int have = gv.player.getInventory().getOrDefault(stone, 0);
        drawHeader(canvas, info.icon + " " + stone.toUpperCase(), "Você tem " + have + "  •  Escolha quem vai evoluir", exitBtn);

        List<CapturedCreature> list = candidates(gv);
        if (selected != null && !list.contains(selected)) selected = null;
        if (selected == null && !list.isEmpty()) selected = list.get(0);

        listArea.set(30, 124, w * 0.55f, h - 24);
        panel(canvas, listArea, Color.argb(120, 10, 16, 28), C_LINE, 2);
        rows.clear();

        if (list.isEmpty()) {
            text(canvas, "Nenhum Pokémon seu evolui com esta pedra.", listArea.centerX(), listArea.centerY() - 10, 24, C_MUTED, Paint.Align.CENTER, false);
            StringBuilder sb = new StringBuilder("Funciona em: ");
            List<String> seen = new ArrayList<>();
            for (String f : fromStone()) if (!seen.contains(f)) seen.add(f);
            for (int i = 0; i < seen.size(); i++) sb.append(i > 0 ? ", " : "").append(seen.get(i));
            wrap(canvas, sb.toString(), listArea.left + 30, listArea.centerY() + 30, listArea.width() - 60, 18, C_MUTED, 24);
        } else {
            float rowH = 92, gap = 10, pad = 12;
            float content = pad * 2 + list.size() * (rowH + gap) - gap;
            maxScroll = Math.max(0, content - listArea.height());
            scroll = Math.max(0, Math.min(maxScroll, scroll));
            int anim = (int) ((System.currentTimeMillis() / 600) % 3);
            canvas.save();
            canvas.clipRect(listArea);
            for (int i = 0; i < list.size(); i++) {
                CapturedCreature c = list.get(i);
                Row r = new Row(c);
                float y = listArea.top + pad + i * (rowH + gap) - scroll;
                r.rect.set(listArea.left + pad, y, listArea.right - pad, y + rowH);
                rows.add(r);
                if (r.rect.bottom < listArea.top || r.rect.top > listArea.bottom) continue;
                boolean sel = c == selected;
                panel(canvas, r.rect, sel ? C_PANEL_HI : C_PANEL, sel ? C_ACCENT : C_LINE, sel ? 4 : 2);
                Bitmap bmp = SpriteManager.getInstance().getUnitSprite(c.name, 0, anim, c.isShiny);
                if (bmp != null) canvas.drawBitmap(bmp, null, new Rect((int) r.rect.left + 12, (int) r.rect.top + 8, (int) r.rect.left + 88, (int) r.rect.bottom - 8), null);
                paint.setTextSize(24);
                text(canvas, fit(c.name + (c.isShiny ? " ✨" : ""), r.rect.width() - 300), r.rect.left + 104, r.rect.top + 40, 24, Color.WHITE, Paint.Align.LEFT, true);
                text(canvas, c.isInParty ? "No time" : "Na base", r.rect.left + 104, r.rect.bottom - 18, 16, c.isInParty ? C_GOOD : C_MUTED, Paint.Align.LEFT, false);
                EvolutionTable.Evo evo = EvolutionTable.byStone(c.name, stone);
                text(canvas, "→ " + evo.to, r.rect.right - 20, r.rect.centerY() + 8, 22, C_ACCENT, Paint.Align.RIGHT, true);
            }
            canvas.restore();
            scrollbar(canvas, listArea, scroll, maxScroll);
        }

        // Prévia
        RectF pv = new RectF(listArea.right + 18, 124, w - 30, h - 24);
        panel(canvas, pv, C_PANEL, C_LINE, 2);
        evolveBtn.setEmpty();
        if (selected == null) {
            text(canvas, "Escolha um Pokémon", pv.centerX(), pv.centerY(), 24, C_MUTED, Paint.Align.CENTER, false);
            return;
        }
        EvolutionTable.Evo evo = EvolutionTable.byStone(selected.name, stone);
        float cx = pv.centerX();
        float sz = Math.min(pv.width() * 0.34f, 150);
        Bitmap a = SpriteManager.getInstance().getUnitSprite(selected.name, 0, 0, selected.isShiny);
        Bitmap b = SpriteManager.getInstance().getUnitSprite(evo.to, 0, 0, selected.isShiny);
        float top = pv.top + 40;
        if (a != null) canvas.drawBitmap(a, null, new Rect((int) (cx - sz - 24), (int) top, (int) (cx - 24), (int) (top + sz)), null);
        text(canvas, "➜", cx, top + sz / 2 + 14, 44, C_ACCENT, Paint.Align.CENTER, true);
        if (b != null) canvas.drawBitmap(b, null, new Rect((int) (cx + 24), (int) top, (int) (cx + sz + 24), (int) (top + sz)), null);
        text(canvas, selected.name + "  ➜  " + evo.to, cx, top + sz + 50, 24, Color.WHITE, Paint.Align.CENTER, true);
        wrap(canvas, "A pedra será consumida e o nível, o equipamento e a runa do Pokémon são mantidos.", pv.left + 24, top + sz + 90, pv.width() - 48, 18, C_MUTED, 24);

        evolveBtn.set(pv.left + 24, pv.bottom - 84, pv.right - 24, pv.bottom - 24);
        actionButton(canvas, evolveBtn, have > 0 ? "EVOLUIR" : "SEM PEDRA", have > 0, C_ACCENT);
    }

    private List<String> fromStone() {
        List<String> out = new ArrayList<>();
        for (String sp : new String[]{"Eevee", "Pikachu", "Growlithe", "Vulpix", "Gloom", "Weepinbell", "Slowpoke"}) {
            if (EvolutionTable.canUseStone(sp, stone)) out.add(sp);
        }
        return out;
    }
}
