package com.example.pokeworld.managers;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import com.example.pokeworld.core.GameView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Carteira de pontos no SERVIDOR: saldo, depósito por Pix e compras. O saldo que aparece no jogo (kv "points")
 * é só uma cópia do que o servidor informou; nada se compra sem o servidor confirmar o débito.
 */
public final class WalletManager {
    private WalletManager() {}

    public static final int[] PACKS = {5, 10, 20, 35, 50, 100};

    // ---- estado do depósito (lido pela tela)
    public static volatile String pixId = null, pixCode = "", pixBase64 = "", status = "";
    public static volatile int pixPoints = 0, creditedPoints = 0;
    public static volatile boolean busy = false, waiting = false;
    private static Bitmap qr;
    private static String qrFor = "";

    private static volatile long lastRefresh = 0, lastCheck = 0;
    private static volatile boolean checking = false, buying = false;

    private static boolean send(String ev, JSONObject d, io.socket.client.Ack ack) {
        return MultiplayerManager.getInstance().emitAck(ev, d, ack);
    }

    private static JSONObject res(Object[] args) { return args.length > 0 && args[0] instanceof JSONObject ? (JSONObject) args[0] : null; }

    // ------------------------------------------------------------------ saldo
    /** Pede o saldo e os itens comprados ao servidor (no máximo a cada 15 s, a menos que force). */
    public static void refresh(boolean force) {
        RewardManager.Store s = PointsManager.store();
        long now = System.currentTimeMillis();
        if (s == null || (!force && now - lastRefresh < 15000)) return;
        lastRefresh = now;
        try {
            send("walletGet", new JSONObject(), args -> {
                JSONObject r = res(args);
                if (r == null || !r.optBoolean("ok")) return;
                s.set("points", Math.max(0, r.optInt("balance", 0)));
                reconcile(s, r.optJSONArray("items"));
            });
        } catch (Exception ignored) { }
    }

    /**
     * Garante que o que o servidor registrou como comprado está valendo no aparelho
     * (cobre o caso do app fechar entre o débito e a entrega do item).
     */
    static void reconcile(RewardManager.Store s, JSONArray items) {
        if (items == null) return;
        long vipUntil = 0;
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.optJSONObject(i);
            if (o == null) continue;
            String item = o.optString("item", "");
            long atSec = o.optLong("at", 0) / 1000L;
            if (item.equals("vip")) {
                vipUntil = Math.max(atSec, vipUntil) + PointsManager.VIP_DAYS * 86400L;
            } else if (item.startsWith("pass:")) {
                try {
                    int season = Integer.parseInt(item.substring(5));
                    if (season == PassManager.season() && !PassManager.premium(s)) s.set("pass_premium", season);
                } catch (NumberFormatException ignored) { }
            } else if (item.startsWith("skin:")) {
                String id = item.substring(5);
                if (s.get("skin_own_" + id, 0) != 1) s.set("skin_own_" + id, 1);
            }
        }
        if (vipUntil > s.get("vip_until", 0) && vipUntil < Integer.MAX_VALUE) s.set("vip_until", (int) vipUntil);
    }

    // ------------------------------------------------------------------ compra (débito no servidor, depois entrega local)
    /** Debita no servidor e só então roda {@code apply} (que entrega o item). */
    public static void purchase(final GameView gv, String item, int price, final Runnable apply, final String okMessage) {
        if (buying) return;
        buying = true;
        try {
            boolean sent = send("walletSpend", new JSONObject().put("points", price).put("item", item), args -> {
                JSONObject r = res(args);
                buying = false;
                if (r == null || !r.optBoolean("ok")) {
                    String err = r != null ? r.optString("error", "Compra recusada.") : "Sem resposta do servidor.";
                    if (r != null && r.has("balance")) { RewardManager.Store s = PointsManager.store(); if (s != null) s.set("points", Math.max(0, r.optInt("balance", 0))); }
                    gv.ui.showMessage("💠 " + err);
                    return;
                }
                RewardManager.Store s = PointsManager.store();
                if (s != null) s.set("points", Math.max(0, r.optInt("balance", 0)));
                gv.activity.runOnUiThread(() -> {
                    PointsManager.prepaid = true;
                    try { apply.run(); } finally { PointsManager.prepaid = false; }
                    gv.ui.showMessage(okMessage);
                    gv.activity.saveCurrentGame();
                });
            });
            if (!sent) { buying = false; gv.ui.showMessage("📶 Sem conexão com o servidor. Conecte-se para comprar."); }
        } catch (Exception e) { buying = false; gv.ui.showMessage("Erro ao comprar."); }
    }

    // ------------------------------------------------------------------ depósito por Pix
    public static boolean hasPix() { return pixId != null; }

    public static void createPix(int points) {
        if (busy) return;
        busy = true; creditedPoints = 0; status = "Gerando o Pix…";
        try {
            boolean sent = send("pixCreate", new JSONObject().put("points", points), args -> {
                busy = false;
                JSONObject r = res(args);
                if (r == null || !r.optBoolean("ok")) { status = "⚠ " + (r != null ? r.optString("error", "Não foi possível gerar o Pix.") : "Sem resposta do servidor."); return; }
                pixId = r.optString("id"); pixPoints = r.optInt("points", points);
                pixCode = r.optString("qr_code"); pixBase64 = r.optString("qr_base64");
                qr = null; qrFor = "";
                waiting = true; lastCheck = System.currentTimeMillis();
                status = "⏳ Aguardando o pagamento… confirma sozinho.";
            });
            if (!sent) { busy = false; status = "📶 Sem conexão com o servidor."; }
        } catch (Exception e) { busy = false; status = "⚠ Erro ao gerar o Pix."; }
    }

    /** Chamado todo quadro pela tela de depósito: confere o Pix aberto a cada 4 s. */
    public static void tick() {
        long now = System.currentTimeMillis();
        if (pixId == null || checking || now - lastCheck < 4000) return;
        lastCheck = now; checking = true;
        final String id = pixId;
        try {
            boolean sent = send("pixCheck", new JSONObject().put("id", id), args -> {
                checking = false;
                JSONObject r = res(args);
                if (r == null || !r.optBoolean("ok")) return;
                String st = r.optString("status");
                if ("approved".equals(st)) {
                    RewardManager.Store s = PointsManager.store();
                    if (s != null) s.set("points", Math.max(0, r.optInt("balance", 0)));
                    creditedPoints = pixPoints; waiting = false; pixId = null; qr = null;
                    status = "✅ Pagamento confirmado! +" + creditedPoints + " pontos.";
                } else if ("rejected".equals(st) || "cancelled".equals(st) || "expired".equals(st)) {
                    waiting = false; pixId = null; qr = null;
                    status = "⚠ Esse Pix não foi concluído (" + st + "). Gere outro.";
                }
            });
            if (!sent) checking = false;
        } catch (Exception e) { checking = false; }
    }

    /** Descarta o Pix da tela (o servidor continua confirmando se você pagar depois, e o saldo entra). */
    public static void clearPix() { pixId = null; waiting = false; qr = null; status = ""; }

    public static Bitmap qrBitmap() {
        if (pixBase64 == null || pixBase64.isEmpty()) return null;
        if (qr == null || !pixBase64.equals(qrFor)) {
            try {
                byte[] b = Base64.decode(pixBase64, Base64.DEFAULT);
                qr = BitmapFactory.decodeByteArray(b, 0, b.length);
                qrFor = pixBase64;
            } catch (Exception e) { qr = null; }
        }
        return qr;
    }

    /** Lista simples para testes de reconcile. */
    static List<String> names(JSONArray a) { List<String> l = new ArrayList<>(); if (a != null) for (int i = 0; i < a.length(); i++) l.add(a.optJSONObject(i).optString("item")); return l; }
}
