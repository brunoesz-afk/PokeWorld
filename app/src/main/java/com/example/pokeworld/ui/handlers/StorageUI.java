package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.managers.PartyManager;
import com.example.pokeworld.managers.SpriteManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gerenciador de Pokémon em 3 colunas: CAIXA (guardados), DEFESA (andam pela base e defendem) e TIME (vão com você).
 * Toque num Pokémon e use os botões de baixo para movê-lo.
 */
public class StorageUI extends BaseUI {
    public static final int COL_BOX = 0, COL_DEF = 1, COL_TEAM = 2;

    public RectF exitBtn = new RectF();
    public RectF toBoxBtn = new RectF(), toDefBtn = new RectF(), toTeamBtn = new RectF();
    public RectF[] colRects = {new RectF(), new RectF(), new RectF()};
    public float[] scroll = new float[3];
    public float[] maxScroll = new float[3];
    public List<Row> rows = new ArrayList<>();
    public CapturedCreature selected;
    public int capacity = 4;

    private final Map<String, PetData> cache = new HashMap<>();
    private long cacheTime = 0, capTime = 0;

    public static class Row {
        public final CapturedCreature creature;
        public final int col;
        public final RectF rect = new RectF();
        Row(CapturedCreature c, int col) { creature = c; this.col = col; }
    }

    private PetData data(GameView gv, String species) {
        long now = System.currentTimeMillis();
        if (now - cacheTime > 1500) { cache.clear(); cacheTime = now; }
        PetData d = cache.get(species);
        if (d == null) { d = gv.activity.dbHelper.getPetData(species); cache.put(species, d); }
        return d;
    }

    public int columnOf(CapturedCreature c) { return c.isInParty ? COL_TEAM : c.isGuard ? COL_DEF : COL_BOX; }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        long now = System.currentTimeMillis();
        if (now - capTime > 1000) { capTime = now; capacity = gv.world.defenderCapacity(gv.player); }

        List<CapturedCreature> box = gv.player.getBox(), def = gv.player.getDefenders(), team = gv.player.getParty();
        drawHeader(canvas, "GERENCIADOR DE POKÉMON",
                "Caixa " + box.size() + "   •   Defesa " + def.size() + "/" + capacity + "   •   Time " + team.size() + "/6   (a defesa cresce com a base)", exitBtn);

        if (selected != null && !gv.player.getCreatureCollection().contains(selected)) selected = null;

        float pad = 30, gap = 14;
        float cw = (w - pad * 2 - gap * 2) / 3f;
        float top = 118, bottom = h - 110;
        String[] titles = {"📦 CAIXA", "🛡️ DEFESA  " + def.size() + "/" + capacity, "🎒 TIME  " + team.size() + "/6"};
        int[] accents = {Color.rgb(120, 170, 230), C_GOOD, C_ACCENT};
        List<List<CapturedCreature>> lists = new ArrayList<>();
        lists.add(box); lists.add(def); lists.add(team);

        rows.clear();
        int anim = (int) ((now / 800) % 3);
        for (int col = 0; col < 3; col++) {
            RectF r = colRects[col];
            r.set(pad + col * (cw + gap), top, pad + col * (cw + gap) + cw, bottom);
            panel(canvas, r, Color.argb(120, 10, 16, 28), accents[col], 2);
            text(canvas, titles[col], r.centerX(), r.top + 30, 20, accents[col], Paint.Align.CENTER, true);

            RectF inner = new RectF(r.left + 8, r.top + 46, r.right - 8, r.bottom - 8);
            List<CapturedCreature> list = lists.get(col);
            float rowH = 64, rg = 8;
            float content = list.size() * (rowH + rg) - rg;
            maxScroll[col] = Math.max(0, content - inner.height());
            scroll[col] = Math.max(0, Math.min(maxScroll[col], scroll[col]));
            if (list.isEmpty()) {
                String empty = col == COL_BOX ? "Caixa vazia" : col == COL_DEF ? "Nenhum defensor.\nEscale alguém da caixa." : "Time vazio";
                float yy = inner.centerY() - 10;
                for (String line : empty.split("\n")) { text(canvas, line, r.centerX(), yy, 16, C_MUTED, Paint.Align.CENTER, false); yy += 22; }
                continue;
            }
            canvas.save();
            canvas.clipRect(inner);
            for (int i = 0; i < list.size(); i++) {
                CapturedCreature c = list.get(i);
                Row row = new Row(c, col);
                float y = inner.top + i * (rowH + rg) - scroll[col];
                row.rect.set(inner.left, y, inner.right, y + rowH);
                rows.add(row);
                if (row.rect.bottom < inner.top || row.rect.top > inner.bottom) continue;
                boolean sel = c == selected;
                panel(canvas, row.rect, sel ? C_PANEL_HI : C_PANEL, sel ? Color.WHITE : (c.rarity.getColor() | 0xFF000000), sel ? 4 : 2);
                Bitmap bmp = SpriteManager.getInstance().getUnitSprite(c.name, 0, anim, c.isShiny);
                if (bmp != null) canvas.drawBitmap(bmp, null, new Rect((int) row.rect.left + 6, (int) row.rect.top + 6, (int) row.rect.left + 58, (int) row.rect.bottom - 6), null);
                int nameColor = c.isShiny ? Color.rgb(255, 215, 0) : c.gender == 0 ? Color.rgb(120, 170, 255) : Color.rgb(255, 160, 210);
                paint.setTextSize(18);
                text(canvas, fit(c.name + (c.gender == 0 ? " ♂" : " ♀") + (c.isShiny ? " ✨" : ""), row.rect.width() - 70), row.rect.left + 66, row.rect.top + 28, 18, nameColor, Paint.Align.LEFT, true);
                int lv = PartyManager.isActive(gv, c) ? gv.companion.getLevel() : data(gv, c.name).level;
                text(canvas, "Nv. " + lv + (PartyManager.isActive(gv, c) ? "  ⭐" : ""), row.rect.left + 66, row.rect.top + 52, 14, Color.WHITE, Paint.Align.LEFT, false);
                text(canvas, c.rarity.getLabel(), row.rect.right - 10, row.rect.top + 52, 13, c.rarity.getColor() | 0xFF000000, Paint.Align.RIGHT, false);
            }
            canvas.restore();
            scrollbar(canvas, inner, scroll[col], maxScroll[col]);
        }

        // Barra de ações
        float by = h - 96, bh = 70;
        float bw = (w - pad * 2 - gap * 2) / 3f;
        toBoxBtn.set(pad, by, pad + bw, by + bh);
        toDefBtn.set(pad + bw + gap, by, pad + bw * 2 + gap, by + bh);
        toTeamBtn.set(pad + (bw + gap) * 2, by, pad + (bw + gap) * 2 + bw, by + bh);
        int cur = selected == null ? -1 : columnOf(selected);
        boolean has = selected != null;
        actionButton(canvas, toBoxBtn, "📦 GUARDAR NA CAIXA", has && cur != COL_BOX, Color.rgb(120, 170, 230));
        actionButton(canvas, toDefBtn, "🛡️ ESCALAR NA DEFESA", has && cur != COL_DEF, C_GOOD);
        actionButton(canvas, toTeamBtn, "🎒 LEVAR NO TIME", has && cur != COL_TEAM, C_ACCENT);
    }
}
