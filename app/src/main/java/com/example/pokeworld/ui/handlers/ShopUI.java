package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.PassManager;
import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.RewardManager;
import com.example.pokeworld.managers.SkinManager;
import com.example.pokeworld.managers.SpriteManager;

import java.util.ArrayList;
import java.util.List;

/** Loja de Pontos: VIP, Passe e skins (1 real = 1 ponto). */
public class ShopUI extends BaseUI {
    public static final class SkinCard { public final RectF btn = new RectF(); public SkinManager.Skin skin; }

    public final RectF exitBtn = new RectF(), depositBtn = new RectF(), vipBtn = new RectF(), passBtn = new RectF();
    public final RectF[] tabs = {new RectF(), new RectF(), new RectF()};
    public final List<SkinCard> skinCards = new ArrayList<>();
    public int tab = 0;   // 0 acesso, 1 skins de Pokémon, 2 skins do personagem

    private static final int GOLD = Color.rgb(255, 214, 90), BLUE = Color.rgb(120, 190, 255);

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        RewardManager.Store s = PointsManager.store();
        drawHeader(canvas, "💠 LOJA DE PONTOS", "1 real = 1 ponto  •  VIP, Passe e skins (só visual, nenhuma dá vantagem em combate)", exitBtn);

        float y = 116, gap = 10;
        depositBtn.set(w - 30 - 330, y, w - 30, y + 56);
        text(canvas, "💠 " + PointsManager.balance(s) + " pontos", 30, y + 40, 34, BLUE, Paint.Align.LEFT, true);
        if (PointsManager.TEST_MODE) actionButton(canvas, depositBtn, "+" + PointsManager.TEST_GRANT + " pts (TESTE)", true, Color.rgb(255, 150, 90));
        else actionButton(canvas, depositBtn, "DEPOSITAR (em breve)", false, C_ACCENT);

        float ty = 186, tw = Math.min(280, (w - 60 - 2 * gap) / 3f);
        String[] names = {"⭐ VIP e Passe", "🐾 Skins de Pokémon", "🧑 Skins do Personagem"};
        for (int i = 0; i < 3; i++) {
            tabs[i].set(30 + i * (tw + gap), ty, 30 + i * (tw + gap) + tw, ty + 44);
            chip(canvas, tabs[i], names[i], tab == i, C_ACCENT);
        }

        skinCards.clear();
        float top = ty + 44 + 16;
        if (tab == 0) drawAccess(canvas, s, w, h, top);
        else drawSkins(canvas, gv, s, w, h, top, tab == 1 ? SkinManager.Kind.POKEMON : SkinManager.Kind.CHAR);
    }

    private void drawAccess(Canvas canvas, RewardManager.Store s, float w, float h, float top) {
        float gap = 14, cw = (w - 60 - gap) / 2f, ch = Math.min(400, h - top - 24);
        RectF a = new RectF(30, top, 30 + cw, top + ch), b = new RectF(a.right + gap, top, w - 30, top + ch);

        boolean vip = PointsManager.isVip(s);
        panel(canvas, a, C_PANEL, vip ? C_GOOD : GOLD, 3);
        text(canvas, "👑 VIP — 30 dias", a.left + 20, a.top + 44, 28, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, PointsManager.PRICE_VIP + " pontos", a.left + 20, a.top + 80, 22, GOLD, Paint.Align.LEFT, true);
        String[] vb = {"• +10% de XP para o time", "• +10% de ouro nos abates", "• Recarga de suprimentos a cada 3h (em vez de 6h)", "• Coroa 👑 no nome do treinador", "• Renovar soma +30 dias ao que sobrou"};
        for (int i = 0; i < vb.length; i++) text(canvas, fit2(vb[i], a.width() - 40), a.left + 20, a.top + 118 + i * 30, 18, C_MUTED, Paint.Align.LEFT, false);
        text(canvas, vip ? "✔ VIP ativo: restam " + PointsManager.vipText(s) : "VIP inativo", a.left + 20, a.bottom - 84, 19, vip ? C_GOOD : C_MUTED, Paint.Align.LEFT, true);
        vipBtn.set(a.left + 20, a.bottom - 66, a.right - 20, a.bottom - 14);
        actionButton(canvas, vipBtn, vip ? "RENOVAR +30 DIAS" : "COMPRAR VIP", true, GOLD);

        boolean prem = PassManager.premium(s);
        panel(canvas, b, C_PANEL, prem ? C_GOOD : GOLD, 3);
        text(canvas, "🎟️ Passe Premium — temporada", b.left + 20, b.top + 44, 28, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, PointsManager.PRICE_PASS + " pontos", b.left + 20, b.top + 80, 22, GOLD, Paint.Align.LEFT, true);
        String[] pb = {"• 30 níveis com recompensas em dobro", "• Pokébolas melhores, gemas e Gemas de Boost", "• 3 skins exclusivas (níveis 10, 20 e 30)", "• Vale até o fim do mês (faltam " + PassManager.daysLeft() + " dias)", "• Missões do passe são grátis para todos"};
        for (int i = 0; i < pb.length; i++) text(canvas, fit2(pb[i], b.width() - 40), b.left + 20, b.top + 118 + i * 30, 18, C_MUTED, Paint.Align.LEFT, false);
        text(canvas, prem ? "✔ Passe premium ativo (nível " + PassManager.level(s) + ")" : "Passe premium não comprado", b.left + 20, b.bottom - 84, 19, prem ? C_GOOD : C_MUTED, Paint.Align.LEFT, true);
        passBtn.set(b.left + 20, b.bottom - 66, b.right - 20, b.bottom - 14);
        actionButton(canvas, passBtn, prem ? "ABRIR PASSE" : "COMPRAR PASSE", true, GOLD);
    }

    private String fit2(String t, float maxW) { paint.setTextSize(18); return fit(t, maxW); }

    private void drawSkins(Canvas canvas, GameView gv, RewardManager.Store s, float w, float h, float top, SkinManager.Kind kind) {
        List<SkinManager.Skin> list = SkinManager.of(kind);
        int cols = 4, rows = 2;
        float gap = 12, cw = (w - 60 - (cols - 1) * gap) / cols, ch = Math.min(260, (h - top - 20 - gap) / rows);
        SkinManager.Skin eq = SkinManager.equipped(s, kind);
        String species = kind == SkinManager.Kind.POKEMON && gv.companion != null ? gv.companion.getSpecies() : "personagem";
        Bitmap sprite = SpriteManager.getInstance().getUnitSprite(species, 0, 1, false);
        for (int i = 0; i < list.size() && i < cols * rows; i++) {
            SkinManager.Skin k = list.get(i);
            float x = 30 + (i % cols) * (cw + gap), y = top + (i / cols) * (ch + gap);
            RectF r = new RectF(x, y, x + cw, y + ch);
            boolean own = SkinManager.owned(s, k), on = eq == k;
            panel(canvas, r, C_PANEL, on ? C_GOOD : own ? C_ACCENT : C_LINE, on ? 4 : 2);

            float pr = Math.min(52, ch * 0.2f), cx = r.centerX(), cy = r.top + 14 + pr;
            SkinFx.aura(canvas, cx, cy + pr * 0.4f, pr * 1.4f, k);
            if (sprite != null) {
                Paint sp = new Paint(); SkinFx.apply(sp, k);
                canvas.drawBitmap(sprite, null, new android.graphics.Rect((int) (cx - pr), (int) (cy - pr), (int) (cx + pr), (int) (cy + pr)), sp);
            }
            paint.setTextSize(20);
            text(canvas, fit(k.icon + " " + k.name, cw - 20), cx, r.top + 14 + pr * 2 + 26, 20, Color.WHITE, Paint.Align.CENTER, true);
            String info = !k.forSale() ? k.source : own ? "Você tem" : k.price + " pontos";
            text(canvas, info, cx, r.top + 14 + pr * 2 + 50, 16, own ? C_GOOD : k.forSale() ? GOLD : Color.rgb(150, 230, 255), Paint.Align.CENTER, false);

            SkinCard sc = new SkinCard(); sc.skin = k; sc.btn.set(r.left + 12, r.bottom - 50, r.right - 12, r.bottom - 12); skinCards.add(sc);
            if (on) pillButton(canvas, sc.btn, "EQUIPADA (tirar)", C_GOOD, 17);
            else if (own) pillButton(canvas, sc.btn, "EQUIPAR", C_ACCENT, 18);
            else if (k.forSale()) pillButton(canvas, sc.btn, "COMPRAR", BLUE, 18);
            else pillButton(canvas, sc.btn, "🔒 NO PASSE", Color.rgb(90, 98, 114), 17);
        }
    }
}
