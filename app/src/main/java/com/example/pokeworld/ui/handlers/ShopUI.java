package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.entities.BuildingData;
import com.example.pokeworld.entities.ItemCatalog;
import com.example.pokeworld.entities.ShopCatalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Mercado: abas COMPRAR / VENDER, lista rolável à esquerda e detalhe com quantidade à direita. */
public class ShopUI extends BaseUI {
    public static final int[] BUY_QTY = {1, 5, 10};
    public static final int[] SELL_QTY = {1, 10, 0};   // 0 = tudo

    public boolean selling = false;
    public int buyIndex = 0;
    public String sellItem = null;
    public int qtyIndex = 0;
    public float scroll = 0, maxScroll = 0;

    public RectF exitBtn = new RectF(), listArea = new RectF(), actionBtn = new RectF(), scrapBtn = new RectF();
    public List<RectF> tabRects = new ArrayList<>();
    public List<RectF> qtyRects = new ArrayList<>();
    public List<RectF> rowRects = new ArrayList<>();
    public List<String> rowKeys = new ArrayList<>();   // buy: índice da oferta; sell: nome do item

    public int shopLevel(GameView gv) {
        Map<String, BuildingData> b = gv.activity.dbHelper.getBuildings();
        return b.containsKey("Loja") ? b.get("Loja").level : 1;
    }

    public int qty(int owned) {
        int[] set = selling ? SELL_QTY : BUY_QTY;
        int q = set[Math.min(qtyIndex, set.length - 1)];
        return q == 0 ? owned : q;
    }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        int shopLv = shopLevel(gv);
        int disc = gv.kingdom.getShopDiscount(shopLv);
        drawHeader(canvas, "🛒 MERCADO", "Seu ouro: " + gv.player.getGold() + " 🪙   •   Loja nível " + shopLv + (disc > 0 ? "  (desconto de " + disc + " 🪙 nas poções)" : ""), exitBtn);

        // dica de dinheiro que muda a cada 7s, no rodapé
        final String[] tips = {
            "💡 Ouro é raro: faça as missões diárias 📅 e as expedições 🎒 todo dia.",
            "💡 Madeira, Ferro, Pedra, Fruta e Pelo não são vendidos de propósito: a base precisa deles.",
            "💡 Prefira Frutas e Peixes para curar quando puder: Poções custam 50 🪙.",
            "💡 A Loja do reino (nível maior) dá desconto nas Poções.",
            "💡 Great/Ultra Ball valem a pena só para chefes e Pokémon raros.",
            "💡 Sábado: invasões pagam mais e expedições levam metade do tempo.",
            "💡 A Gema de Boost (1500 🪙) rende mais que várias Poções: use em quem você mais usa."};
        paint.setStyle(Paint.Style.FILL);
        text(canvas, tips[(int) ((System.currentTimeMillis() / 7000) % tips.length)], w / 2f, h - 6, 15, C_MUTED, Paint.Align.CENTER, false);

        // abas
        tabRects.clear();
        String[] tabs = {"COMPRAR", "VENDER"};
        for (int i = 0; i < 2; i++) {
            RectF r = new RectF(30 + i * 190, 118, 30 + i * 190 + 180, 160);
            tabRects.add(r);
            chip(canvas, r, tabs[i], (i == 1) == selling, C_ACCENT);
        }

        listArea.set(30, 174, w * 0.56f, h - 24);
        panel(canvas, listArea, Color.argb(120, 10, 16, 28), C_LINE, 2);
        RectF det = new RectF(listArea.right + 18, 174, w - 30, h - 24);
        panel(canvas, det, C_PANEL, C_LINE, 2);
        rowRects.clear(); rowKeys.clear(); qtyRects.clear(); scrapBtn.setEmpty(); actionBtn.setEmpty();

        List<String> keys = new ArrayList<>();
        if (selling) keys.addAll(ShopCatalog.sellable(gv.player.getInventory()));
        else for (int i = 0; i < ShopCatalog.BUY.size(); i++) keys.add(String.valueOf(i));

        if (selling) {
            if (sellItem == null || !keys.contains(sellItem)) sellItem = keys.isEmpty() ? null : keys.get(0);
        } else {
            buyIndex = Math.max(0, Math.min(ShopCatalog.BUY.size() - 1, buyIndex));
        }

        // --- lista ---
        if (keys.isEmpty()) {
            text(canvas, "Nada para vender por enquanto.", listArea.centerX(), listArea.centerY() - 8, 24, C_MUTED, Paint.Align.CENTER, false);
            text(canvas, "Materiais dropam dos Pokémon que você derrota.", listArea.centerX(), listArea.centerY() + 26, 18, C_MUTED, Paint.Align.CENTER, false);
        } else {
            float rowH = 84, gap = 8, pad = 12;
            float content = pad * 2 + keys.size() * (rowH + gap) - gap;
            maxScroll = Math.max(0, content - listArea.height());
            scroll = Math.max(0, Math.min(maxScroll, scroll));
            canvas.save();
            canvas.clipRect(listArea);
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                String name = selling ? key : ShopCatalog.BUY.get(Integer.parseInt(key)).name;
                int price = selling ? ShopCatalog.sellPrice(name) : ShopCatalog.buyPrice(ShopCatalog.BUY.get(Integer.parseInt(key)), gv.kingdom, shopLv);
                int owned = gv.player.getInventory().getOrDefault(name, 0);
                float y = listArea.top + pad + i * (rowH + gap) - scroll;
                RectF r = new RectF(listArea.left + pad, y, listArea.right - pad - 10, y + rowH);
                rowRects.add(r); rowKeys.add(key);
                if (r.bottom < listArea.top || r.top > listArea.bottom) continue;
                boolean sel = selling ? key.equals(sellItem) : i == buyIndex;
                boolean afford = selling || gv.player.getGold() >= price;
                panel(canvas, r, sel ? C_PANEL_HI : C_PANEL, sel ? Color.WHITE : C_LINE, sel ? 4 : 2);
                text(canvas, ItemCatalog.get(name).icon, r.left + 38, r.centerY() + 14, 40, Color.WHITE, Paint.Align.CENTER, false);
                paint.setTextSize(24);
                text(canvas, fit(name, r.width() - 250), r.left + 80, r.top + 36, 24, Color.WHITE, Paint.Align.LEFT, true);
                text(canvas, "Você tem: " + owned, r.left + 80, r.top + 64, 17, C_MUTED, Paint.Align.LEFT, false);
                text(canvas, price + " 🪙", r.right - 16, r.centerY() + 9, 26, afford ? Color.rgb(255, 214, 90) : C_BAD, Paint.Align.RIGHT, true);
            }
            canvas.restore();
            scrollbar(canvas, listArea, scroll, maxScroll);
        }

        // --- detalhe ---
        String name = null; int price = 0; String desc = "";
        if (selling && sellItem != null) { name = sellItem; price = ShopCatalog.sellPrice(name); desc = ItemCatalog.get(name).desc; }
        else if (!selling) { ShopCatalog.Offer o = ShopCatalog.BUY.get(buyIndex); name = o.name; price = ShopCatalog.buyPrice(o, gv.kingdom, shopLv); desc = o.desc; }
        if (name == null) return;

        int owned = gv.player.getInventory().getOrDefault(name, 0);
        text(canvas, ItemCatalog.get(name).icon, det.left + 54, det.top + 78, 64, Color.WHITE, Paint.Align.CENTER, false);
        paint.setTextSize(28);
        text(canvas, fit(name, det.width() - 130), det.left + 104, det.top + 56, 28, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, selling ? "Mercador paga " + price + " 🪙 cada" : price + " 🪙 cada", det.left + 104, det.top + 86, 19, Color.rgb(255, 214, 90), Paint.Align.LEFT, false);
        float y = wrap(canvas, desc, det.left + 24, det.top + 132, det.width() - 48, 18, Color.rgb(210, 216, 230), 25);
        text(canvas, "Na mochila: " + owned, det.left + 24, y + 12, 19, C_ACCENT, Paint.Align.LEFT, true);

        // quantidade
        int[] set = selling ? SELL_QTY : BUY_QTY;
        float qy = det.bottom - 250, qw = (det.width() - 48 - 16) / 3f;
        text(canvas, "Quantidade", det.left + 24, qy - 10, 16, C_MUTED, Paint.Align.LEFT, true);
        for (int i = 0; i < set.length; i++) {
            RectF r = new RectF(det.left + 24 + i * (qw + 8), qy, det.left + 24 + i * (qw + 8) + qw, qy + 44);
            qtyRects.add(r);
            chip(canvas, r, set[i] == 0 ? "TUDO" : "x" + set[i], i == Math.min(qtyIndex, set.length - 1), C_ACCENT);
        }

        int q = qty(owned);
        int total = price * q;
        boolean can = selling ? owned > 0 && q > 0 : gv.player.getGold() >= price;
        if (!selling) q = Math.max(1, Math.min(q, price > 0 ? Math.max(1, gv.player.getGold() / price) : q));   // compra o que dá, no máximo o pedido
        total = price * q;
        text(canvas, selling ? "Total: +" + total + " 🪙" : "Total: " + total + " 🪙", det.centerX(), qy + 84, 24, selling ? C_GOOD : (can ? Color.rgb(255, 214, 90) : C_BAD), Paint.Align.CENTER, true);
        actionBtn.set(det.left + 24, det.bottom - 150, det.right - 24, det.bottom - 90);
        String label = selling ? (can ? "VENDER " + q : "NADA PARA VENDER") : (can ? "COMPRAR " + q : "OURO INSUFICIENTE");
        actionButton(canvas, actionBtn, label, can, selling ? Color.rgb(120, 200, 140) : C_ACCENT);
        if (selling) {
            scrapBtn.set(det.left + 24, det.bottom - 76, det.right - 24, det.bottom - 24);
            int scrap = scrapValue(gv);
            actionButton(canvas, scrapBtn, scrap > 0 ? "VENDER SUCATA (+" + scrap + " 🪙)" : "SEM SUCATA", scrap > 0, Color.rgb(240, 190, 90));
        }
    }

    /** Quanto rende vender tudo o que não é de construção/oficina. */
    public int scrapValue(GameView gv) {
        int t = 0;
        for (Map.Entry<String, Integer> e : gv.player.getInventory().entrySet()) {
            if (ShopCatalog.isKept(e.getKey())) continue;
            t += ShopCatalog.sellPrice(e.getKey()) * Math.max(0, e.getValue());
        }
        return t;
    }
}
