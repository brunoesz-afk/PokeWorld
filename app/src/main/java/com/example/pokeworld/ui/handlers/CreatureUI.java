package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.CapturedCreature;
import com.example.pokeworld.entities.Companion;
import com.example.pokeworld.entities.EvolutionTable;
import com.example.pokeworld.entities.PetData;
import com.example.pokeworld.entities.Progression;
import com.example.pokeworld.managers.PartyManager;
import com.example.pokeworld.managers.SpriteManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Time: 6 espaços à esquerda e ficha completa do Pokémon selecionado à direita. */
public class CreatureUI extends BaseUI {
    public RectF exitBtn = new RectF();
    public RectF activateBtn = new RectF();
    public RectF manageBtn = new RectF();
    public List<Slot> slots = new ArrayList<>();
    public List<EvoBtn> evoButtons = new ArrayList<>();

    /** Botão EVOLUIR de uma linha da ficha (por nível ou por pedra). */
    public static class EvoBtn {
        public final RectF rect = new RectF();
        public final EvolutionTable.Evo evo;
        EvoBtn(EvolutionTable.Evo e) { evo = e; }
    }
    public CapturedCreature selected;

    private final Map<String, PetData> cache = new HashMap<>();
    private long cacheTime = 0;

    public static class Slot {
        public final CapturedCreature creature;
        public final RectF rect = new RectF();
        Slot(CapturedCreature c) { creature = c; }
    }

    /** Nível atual de um Pokémon do time (o ativo vem do Companion, os outros do banco). */
    public int levelOf(GameView gv, CapturedCreature c) {
        return PartyManager.isActive(gv, c) ? gv.companion.getLevel() : data(gv, c.name).level;
    }

    /** Pode evoluir agora? Por nível (nível atingido) ou por pedra (tem a pedra na mochila). */
    public boolean canEvolve(GameView gv, CapturedCreature c, EvolutionTable.Evo e) {
        if (e.stone != null) return gv.player.getInventory().getOrDefault(e.stone, 0) > 0;
        return levelOf(gv, c) >= e.level;
    }

    public void refreshCache() { cache.clear(); cacheTime = System.currentTimeMillis(); }

    private PetData data(GameView gv, String species) {
        long now = System.currentTimeMillis();
        if (now - cacheTime > 1500) { cache.clear(); cacheTime = now; }   // evita consultar o banco a cada quadro
        PetData d = cache.get(species);
        if (d == null) { d = gv.activity.dbHelper.getPetData(species); cache.put(species, d); }
        return d;
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        Companion comp = gv.companion;
        List<CapturedCreature> party = gv.player.getParty();
        drawHeader(canvas, "MEU TIME", "Pokémon no time: " + party.size() + " / 6   •   toque em um para ver a ficha", exitBtn);

        if (selected == null || !party.contains(selected)) {
            selected = null;
            for (CapturedCreature c : party) if (PartyManager.isActive(gv, c)) selected = c;
            if (selected == null && !party.isEmpty()) selected = party.get(0);
        }

        // --- 6 espaços em 2 colunas x 3 linhas ---
        RectF left = new RectF(30, 124, w * 0.50f, h - 24);
        panel(canvas, left, Color.argb(120, 10, 16, 28), C_LINE, 2);
        float pad = 14, gap = 12;
        float sw = (left.width() - pad * 2 - gap) / 2f;
        float sh = (left.height() - pad * 2 - gap * 2) / 3f;
        slots.clear();
        int anim = (int) ((System.currentTimeMillis() / 800) % 3);
        for (int i = 0; i < 6; i++) {
            float x = left.left + pad + (i % 2) * (sw + gap);
            float y = left.top + pad + (i / 2) * (sh + gap);
            RectF r = new RectF(x, y, x + sw, y + sh);
            if (i >= party.size()) {
                panel(canvas, r, Color.argb(90, 20, 28, 42), Color.rgb(60, 70, 90), 2);
                text(canvas, "vazio", r.centerX(), r.centerY() + 6, 18, Color.rgb(90, 100, 120), Paint.Align.CENTER, false);
                continue;
            }
            CapturedCreature c = party.get(i);
            Slot s = new Slot(c);
            s.rect.set(r);
            slots.add(s);
            boolean active = PartyManager.isActive(gv, c);
            boolean sel = c == selected;
            panel(canvas, r, active ? Color.rgb(32, 64, 46) : sel ? C_PANEL_HI : C_PANEL, sel ? Color.WHITE : active ? C_GOOD : (c.rarity.getColor() | 0xFF000000), sel ? 5 : 2);
            Bitmap bmp = SpriteManager.getInstance().getUnitSprite(c.name, 0, anim, c.isShiny);
            float is = Math.min(sh - 16, sw * 0.36f);
            if (bmp != null) canvas.drawBitmap(bmp, null, new Rect((int) r.left + 10, (int) (r.centerY() - is / 2), (int) (r.left + 10 + is), (int) (r.centerY() + is / 2)), null);
            float tx = r.left + is + 22;
            int nameColor = c.isShiny ? Color.rgb(255, 215, 0) : c.gender == 0 ? Color.rgb(120, 170, 255) : Color.rgb(255, 160, 210);
            paint.setTextSize(20);
            text(canvas, fit(c.name + (c.gender == 0 ? " ♂" : " ♀") + (c.isShiny ? " ✨" : ""), r.right - tx - 8), tx, r.top + sh * 0.38f, 20, nameColor, Paint.Align.LEFT, true);
            int lv = active ? comp.getLevel() : data(gv, c.name).level;
            text(canvas, "Nv. " + lv, tx, r.top + sh * 0.62f, 17, Color.WHITE, Paint.Align.LEFT, false);
            text(canvas, c.rarity.getLabel(), tx, r.top + sh * 0.84f, 14, c.rarity.getColor() | 0xFF000000, Paint.Align.LEFT, false);
            if (active) text(canvas, "⭐", r.right - 12, r.top + 28, 22, Color.WHITE, Paint.Align.RIGHT, false);
            for (EvolutionTable.Evo e : EvolutionTable.from(c.name)) {
                if (canEvolve(gv, c, e)) { text(canvas, "⬆ EVOLUI", r.right - 10, r.bottom - 10, 15, C_GOOD, Paint.Align.RIGHT, true); break; }
            }
        }

        // --- Ficha ---
        RectF det = new RectF(left.right + 18, 124, w - 30, h - 24);
        panel(canvas, det, C_PANEL, C_LINE, 2);
        activateBtn.setEmpty();
        manageBtn.set(det.left + 24, det.bottom - 74, det.right - 24, det.bottom - 20);
        if (selected == null) {
            text(canvas, "Seu time está vazio", det.centerX(), det.centerY(), 24, C_MUTED, Paint.Align.CENTER, false);
            return;
        }
        CapturedCreature c = selected;
        boolean active = PartyManager.isActive(gv, c);
        PetData d = active ? null : data(gv, c.name);
        int level = active ? comp.getLevel() : d.level;
        int exp = active ? comp.getExp() : d.exp;
        int friendship = active ? comp.getFriendship() : d.friendship;
        String weapon = active ? (comp.getWeapon() != null ? comp.getWeapon().getName() : "Nenhum") : d.weapon;
        String armor = active ? (comp.getArmor() != null ? comp.getArmor().getName() : "Nenhum") : d.armor;
        String rune = active ? (comp.getRune() != null ? comp.getRune().getName() : "Nenhum") : d.rune;

        float cx = det.centerX();
        Bitmap big = SpriteManager.getInstance().getUnitSprite(c.name, 0, (int) ((System.currentTimeMillis() / 600) % 3), c.isShiny);
        float bs = Math.min(det.width() * 0.34f, 130);
        if (big != null) canvas.drawBitmap(big, null, new Rect((int) (det.left + 20), (int) det.top + 16, (int) (det.left + 20 + bs), (int) (det.top + 16 + bs)), null);
        float tx = det.left + bs + 36;
        paint.setTextSize(28);
        text(canvas, fit(c.name + (c.gender == 0 ? " ♂" : " ♀") + (c.isShiny ? " ✨" : ""), det.right - tx - 12), tx, det.top + 50, 28, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, "[" + c.rarity.getLabel() + "]", tx, det.top + 80, 18, c.rarity.getColor() | 0xFF000000, Paint.Align.LEFT, true);
        text(canvas, "Nível " + level, tx, det.top + 112, 22, C_ACCENT, Paint.Align.LEFT, true);
        bar(canvas, new RectF(tx, det.top + 124, det.right - 24, det.top + 138), exp / (float) Progression.expToNext(level), Color.rgb(90, 170, 255));
        text(canvas, "EXP " + exp + " / " + Progression.expToNext(level), tx, det.top + 160, 14, C_MUTED, Paint.Align.LEFT, false);

        float y = det.top + bs + 50;
        if (active) {
            text(canvas, "❤ Vida " + comp.getHp() + "/" + comp.getMaxHp() + "     ⚔ Ataque " + comp.getAttackPower(), det.left + 24, y, 19, Color.WHITE, Paint.Align.LEFT, false);
            y += 34;
        }
        text(canvas, "💗 Amizade", det.left + 24, y, 17, C_MUTED, Paint.Align.LEFT, false);
        bar(canvas, new RectF(det.left + 150, y - 14, det.right - 24, y), friendship / 100f, Color.rgb(255, 110, 160));
        y += 32;
        paint.setTextSize(17);
        text(canvas, fit("⚔ Arma: " + weapon + "    🛡 Armadura: " + armor, det.width() - 48), det.left + 24, y, 17, Color.WHITE, Paint.Align.LEFT, false);
        y += 28;
        text(canvas, "🔷 Runa: " + rune, det.left + 24, y, 17, Color.WHITE, Paint.Align.LEFT, false);
        y += 38;

        text(canvas, "EVOLUÇÃO", det.left + 24, y, 16, C_MUTED, Paint.Align.LEFT, true);
        y += 12;
        evoButtons.clear();
        List<EvolutionTable.Evo> evos = EvolutionTable.from(c.name);
        if (evos.isEmpty()) {
            y += 18;
            text(canvas, EvolutionTable.preEvolution(c.name) != null ? "Forma final" : "Não evolui", det.left + 24, y, 17, Color.WHITE, Paint.Align.LEFT, false);
        } else {
            float limit = det.bottom - 144 - 12;                    // acima do botão TORNAR ATIVO
            float rowH = 44;
            for (EvolutionTable.Evo e : evos) {
                if (y + rowH > limit) break;
                boolean can = canEvolve(gv, c, e);
                float btnW = 150;
                RectF row = new RectF(det.left + 24, y, det.right - 24, y + rowH - 6);
                panel(canvas, row, Color.argb(110, 10, 16, 28), can ? C_GOOD : C_LINE, can ? 2 : 1);
                String need;
                if (e.stone != null) need = can ? e.stone + " ✔" : "precisa: " + e.stone;
                else need = can ? "Nv. " + e.level + " ✔" : "Nv. " + e.level + " (faltam " + (e.level - level) + ")";
                paint.setTextSize(17);
                text(canvas, fit(c.name + "  ➜  " + e.to, row.width() - btnW - 24), row.left + 12, row.centerY() - 2, 17, Color.WHITE, Paint.Align.LEFT, true);
                text(canvas, fit(need, row.width() - btnW - 24), row.left + 12, row.centerY() + 15, 14, can ? C_GOOD : C_MUTED, Paint.Align.LEFT, false);
                EvoBtn eb = new EvoBtn(e);
                eb.rect.set(row.right - btnW - 6, row.top + 4, row.right - 6, row.bottom - 4);
                if (can) {
                    panel(canvas, eb.rect, C_GOOD, Color.WHITE, 2);
                    text(canvas, "EVOLUIR", eb.rect.centerX(), eb.rect.centerY() + 7, 19, Color.BLACK, Paint.Align.CENTER, true);
                    evoButtons.add(eb);
                } else {
                    panel(canvas, eb.rect, Color.rgb(40, 46, 58), Color.rgb(70, 78, 94), 1);
                    text(canvas, "bloqueado", eb.rect.centerX(), eb.rect.centerY() + 6, 15, Color.rgb(110, 118, 134), Paint.Align.CENTER, false);
                }
                y += rowH;
            }
        }

        activateBtn.set(det.left + 24, det.bottom - 144, det.right - 24, det.bottom - 84);
        actionButton(canvas, activateBtn, active ? "⭐ COMPANHEIRO ATIVO" : "TORNAR ATIVO", !active, C_GOOD);
        actionButton(canvas, manageBtn, "GERENCIAR BASE", true, Color.rgb(120, 170, 230));
    }
}
