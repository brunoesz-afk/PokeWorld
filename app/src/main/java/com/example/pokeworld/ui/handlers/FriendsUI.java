package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.FriendManager;
import com.example.pokeworld.managers.MultiplayerManager;

import java.util.ArrayList;
import java.util.List;

/** Amigos: seu código, pedidos recebidos e lista com status online. */
public class FriendsUI extends BaseUI {
    /** Área tocável gerada no último desenho: ação + código do amigo/pedido. */
    public static final class Hit {
        public final RectF r; public final String action, code;
        Hit(RectF r, String action, String code) { this.r = r; this.action = action; this.code = code; }
    }

    public final RectF exitBtn = new RectF(), addBtn = new RectF(), nameBtn = new RectF(),
            copyBtn = new RectF(), shareBtn = new RectF(), prevBtn = new RectF(), nextBtn = new RectF();
    public final List<Hit> hits = new ArrayList<>();
    public int page = 0;
    /** Código do amigo que está esperando o segundo toque para ser removido. */
    public String confirmRemove = "";

    @Override
    public void draw(Canvas canvas, GameView gv) {
        FriendManager fm = FriendManager.getInstance();
        String n;
        while ((n = fm.pollNotice()) != null) gv.ui.showMessage(n);
        fm.syncIfStale(15000);

        float w = canvas.getWidth(), h = canvas.getHeight();
        boolean online = MultiplayerManager.getInstance().isConnected();
        drawHeader(canvas, "👥 AMIGOS", online ? "🟢 Servidor online" : "🟡 Conectando ao servidor… (pedidos precisam de conexão)", exitBtn);
        hits.clear();
        prevBtn.setEmpty(); nextBtn.setEmpty();

        float lw = Math.min(w * 0.40f, 470);
        RectF left = new RectF(30, 124, 30 + lw, h - 24), right = new RectF(left.right + 18, 124, w - 30, h - 24);
        panel(canvas, left, C_PANEL, C_LINE, 2);
        panel(canvas, right, C_PANEL, C_LINE, 2);

        // ---------------- esquerda: seu código
        float cx = left.centerX();
        text(canvas, "SEU CÓDIGO DE AMIGO", cx, left.top + 38, 20, C_MUTED, Paint.Align.CENTER, true);
        RectF codeBox = new RectF(left.left + 20, left.top + 54, left.right - 20, left.top + 130);
        panel(canvas, codeBox, C_PANEL_HI, C_ACCENT, 3);
        text(canvas, fm.displayCode(), codeBox.centerX(), codeBox.centerY() + 16, 46, Color.WHITE, Paint.Align.CENTER, true);
        paint.setTextSize(20);
        text(canvas, fit("Treinador: " + fm.getMyName(), left.width() - 40), cx, left.top + 162, 20, Color.WHITE, Paint.Align.CENTER, false);

        float bw = (left.width() - 40 - 16) / 3f, by = left.top + 182;
        nameBtn.set(left.left + 20, by, left.left + 20 + bw, by + 56);
        copyBtn.set(nameBtn.right + 8, by, nameBtn.right + 8 + bw, by + 56);
        shareBtn.set(copyBtn.right + 8, by, copyBtn.right + 8 + bw, by + 56);
        smallButton(canvas, nameBtn, "✏ NOME", C_ACCENT);
        smallButton(canvas, copyBtn, "📋 COPIAR", C_ACCENT);
        smallButton(canvas, shareBtn, "📤 ENVIAR", C_ACCENT);

        addBtn.set(left.left + 20, by + 76, left.right - 20, by + 76 + 80);
        actionButton(canvas, addBtn, "➕ ADICIONAR AMIGO", true, C_GOOD);

        float ty = addBtn.bottom + 34;
        ty = wrap(canvas, "Passe seu código para um amigo (botão ENVIAR) ou digite o código dele em ADICIONAR AMIGO. Quando ele aceitar, vocês aparecem na lista um do outro.", left.left + 22, ty, left.width() - 44, 16, C_MUTED, 22);
        wrap(canvas, "Amigos online aparecem com ★ verde no mapa.", left.left + 22, ty + 8, left.width() - 44, 16, C_MUTED, 22);

        // ---------------- direita: pedidos + amigos
        List<FriendManager.Request> reqs = fm.getRequests();
        List<FriendManager.Friend> fl = fm.getFriends();
        text(canvas, "AMIGOS (" + fl.size() + ")  •  🟢 " + fm.onlineCount() + " online" + (reqs.isEmpty() ? "" : "  •  📨 " + reqs.size() + " pedido(s)"),
                right.left + 20, right.top + 36, 22, Color.WHITE, Paint.Align.LEFT, true);

        int total = reqs.size() + fl.size();
        if (total == 0) {
            text(canvas, "Você ainda não tem amigos.", right.centerX(), right.centerY() - 10, 24, Color.WHITE, Paint.Align.CENTER, true);
            text(canvas, "Toque em ADICIONAR AMIGO e digite o código de quem você quer convidar.", right.centerX(), right.centerY() + 26, 17, C_MUTED, Paint.Align.CENTER, false);
            return;
        }

        float rowH = 84, gap = 8, top = right.top + 54;
        int perPage = Math.max(1, (int) ((right.bottom - top - 56 + gap) / (rowH + gap)));
        int pages = (total + perPage - 1) / perPage;
        page = Math.max(0, Math.min(page, pages - 1));

        for (int i = 0; i < perPage; i++) {
            int idx = page * perPage + i;
            if (idx >= total) break;
            RectF r = new RectF(right.left + 14, top + i * (rowH + gap), right.right - 14, top + i * (rowH + gap) + rowH);
            if (idx < reqs.size()) drawRequest(canvas, r, reqs.get(idx));
            else drawFriend(canvas, r, fl.get(idx - reqs.size()));
        }

        if (pages > 1) {
            prevBtn.set(right.left + 14, right.bottom - 50, right.left + 154, right.bottom - 10);
            nextBtn.set(right.right - 154, right.bottom - 50, right.right - 14, right.bottom - 10);
            actionButton(canvas, prevBtn, "◀", page > 0, C_ACCENT);
            actionButton(canvas, nextBtn, "▶", page < pages - 1, C_ACCENT);
            text(canvas, (page + 1) + " / " + pages, right.centerX(), right.bottom - 20, 20, C_MUTED, Paint.Align.CENTER, false);
        }
    }

    private void drawRequest(Canvas canvas, RectF r, FriendManager.Request q) {
        panel(canvas, r, Color.argb(150, 60, 48, 14), C_ACCENT, 2);
        paint.setTextSize(22);
        text(canvas, fit("📨 " + q.name, r.width() - 360), r.left + 16, r.top + 36, 22, Color.WHITE, Paint.Align.LEFT, true);
        text(canvas, "quer ser seu amigo", r.left + 16, r.top + 64, 16, C_MUTED, Paint.Align.LEFT, false);
        RectF no = new RectF(r.right - 140, r.top + 14, r.right - 12, r.bottom - 14);
        RectF yes = new RectF(no.left - 160, r.top + 14, no.left - 8, r.bottom - 14);
        smallButton(canvas, yes, "ACEITAR", C_GOOD);
        smallButton(canvas, no, "RECUSAR", C_BAD);
        hits.add(new Hit(yes, "accept", q.code));
        hits.add(new Hit(no, "decline", q.code));
    }

    private void drawFriend(Canvas canvas, RectF r, FriendManager.Friend f) {
        panel(canvas, r, Color.argb(120, 10, 16, 28), f.online ? C_GOOD : C_LINE, f.online ? 2 : 1);
        paint.setTextSize(22);
        text(canvas, fit((f.online ? "★ " : "") + f.name, r.width() - 220), r.left + 16, r.top + 36, 22, f.online ? Color.rgb(150, 245, 175) : Color.WHITE, Paint.Align.LEFT, true);
        String sub = f.online ? "🟢 Online" + (f.map.isEmpty() ? "" : " em " + mapLabel(f.map)) : "⚫ Offline" + lastSeen(f.lastSeen);
        paint.setTextSize(16);
        text(canvas, fit(sub, r.width() - 220), r.left + 16, r.top + 64, 16, C_MUTED, Paint.Align.LEFT, false);
        boolean armed = f.code.equals(confirmRemove);
        RectF rm = new RectF(r.right - 190, r.top + 16, r.right - 12, r.bottom - 16);
        smallButton(canvas, rm, armed ? "CONFIRMAR?" : "REMOVER", armed ? C_BAD : Color.rgb(120, 128, 148));
        hits.add(new Hit(rm, "remove", f.code));
    }

    private void smallButton(Canvas canvas, RectF r, String label, int color) {
        panel(canvas, r, color, Color.WHITE, 2);
        paint.setTextSize(18);
        text(canvas, fit(label, r.width() - 10), r.centerX(), r.centerY() + 6, 18, Color.BLACK, Paint.Align.CENTER, true);
    }

    private static String mapLabel(String map) {
        switch (map) {
            case "CITY": return "Cidade";
            case "HOME": return "Base";
            default: return map.replace('_', ' ').toLowerCase();
        }
    }

    private static String lastSeen(long t) {
        if (t <= 0) return "";
        long min = Math.max(0, (System.currentTimeMillis() - t) / 60000);
        if (min < 60) return " • visto há " + Math.max(1, min) + " min";
        if (min < 60 * 24) return " • visto há " + (min / 60) + " h";
        return " • visto há " + (min / 1440) + " d";
    }
}
