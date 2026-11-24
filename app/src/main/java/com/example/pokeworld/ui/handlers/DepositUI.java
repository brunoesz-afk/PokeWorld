package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.RewardManager;
import com.example.pokeworld.managers.WalletManager;

/** Depósito de pontos por Pix: escolha o valor, gere o Pix, pague e os pontos entram sozinhos. */
public class DepositUI extends BaseUI {
    public final RectF exitBtn = new RectF(), createBtn = new RectF(), copyBtn = new RectF(), newBtn = new RectF();
    public final RectF[] packs = new RectF[WalletManager.PACKS.length];
    public int selected = 3;      // índice em PACKS (35 = o Passe)

    private static final int BLUE = Color.rgb(120, 190, 255), GOLD = Color.rgb(255, 214, 90);

    public DepositUI() { for (int i = 0; i < packs.length; i++) packs[i] = new RectF(); }

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        RewardManager.Store s = PointsManager.store();
        WalletManager.tick();
        WalletManager.refresh(false);
        drawHeader(canvas, "💠 DEPOSITAR PONTOS", "1 real = 1 ponto  •  pagamento por Pix, confirmado automaticamente", exitBtn);
        text(canvas, "Saldo: 💠 " + PointsManager.balance(s), 30, 150, 32, BLUE, Paint.Align.LEFT, true);

        // ---- esquerda: valores
        RectF left = new RectF(30, 170, w * 0.42f, h - 24);
        panel(canvas, left, C_PANEL, C_LINE, 2);
        text(canvas, "Escolha o valor", left.left + 24, left.top + 40, 24, Color.WHITE, Paint.Align.LEFT, true);
        float gx = 16, gy = 14, cw = (left.width() - 48 - gx) / 2f, chh = 84;
        for (int i = 0; i < packs.length; i++) {
            int col = i % 2, row = i / 2;
            RectF r = packs[i];
            r.set(left.left + 24 + col * (cw + gx), left.top + 62 + row * (chh + gy), left.left + 24 + col * (cw + gx) + cw, left.top + 62 + row * (chh + gy) + chh);
            boolean on = i == selected;
            panel(canvas, r, on ? Color.argb(240, 70, 60, 24) : C_PANEL_HI, on ? C_ACCENT : C_LINE, on ? 4 : 2);
            text(canvas, "💠 " + WalletManager.PACKS[i], r.centerX(), r.top + 40, 32, on ? C_ACCENT : Color.WHITE, Paint.Align.CENTER, true);
            text(canvas, "R$ " + WalletManager.PACKS[i] + ",00" + tag(WalletManager.PACKS[i]), r.centerX(), r.top + 68, 17, C_MUTED, Paint.Align.CENTER, false);
        }
        boolean can = !WalletManager.busy && !WalletManager.hasPix();
        createBtn.set(left.left + 24, left.bottom - 84, left.right - 24, left.bottom - 20);
        actionButton(canvas, createBtn, WalletManager.busy ? "GERANDO…" : "GERAR PIX — R$ " + WalletManager.PACKS[selected] + ",00", can, C_GOOD);

        // ---- direita: o Pix
        RectF right = new RectF(left.right + 16, 170, w - 30, h - 24);
        panel(canvas, right, C_PANEL, C_LINE, 2);
        copyBtn.setEmpty(); newBtn.setEmpty();
        float cx = right.centerX();
        if (WalletManager.hasPix()) {
            text(canvas, "Pix de R$ " + WalletManager.pixPoints + ",00  →  " + WalletManager.pixPoints + " pontos", cx, right.top + 40, 26, Color.WHITE, Paint.Align.CENTER, true);
            float qs = Math.min(240, right.height() - 270);
            RectF qr = new RectF(right.left + 28, right.top + 66, right.left + 28 + qs, right.top + 66 + qs);
            paint.setStyle(Paint.Style.FILL); paint.setColor(Color.WHITE);
            canvas.drawRoundRect(new RectF(qr.left - 8, qr.top - 8, qr.right + 8, qr.bottom + 8), 10, 10, paint);
            Bitmap b = WalletManager.qrBitmap();
            if (b != null) canvas.drawBitmap(b, null, new Rect((int) qr.left, (int) qr.top, (int) qr.right, (int) qr.bottom), null);
            float tx = qr.right + 34, tw = right.right - 24 - tx;
            text(canvas, "Como pagar", tx, qr.top + 22, 22, GOLD, Paint.Align.LEFT, true);
            float y = wrap(canvas, "1) Toque em COPIAR CÓDIGO.", tx, qr.top + 54, tw, 17, Color.WHITE, 24);
            y = wrap(canvas, "2) No app do banco: Pix → Pix Copia e Cola → cole o código.", tx, y, tw, 17, Color.WHITE, 24);
            wrap(canvas, "3) Pague. Os pontos entram sozinhos em poucos segundos.", tx, y, tw, 17, Color.WHITE, 24);
            copyBtn.set(tx, qr.bottom - 52, tx + Math.min(300, tw), qr.bottom);
            actionButton(canvas, copyBtn, "📋 COPIAR CÓDIGO", true, Color.rgb(190, 140, 255));
            paint.setTextSize(14);
            text(canvas, fit(WalletManager.pixCode, right.width() - 56), right.left + 28, qr.bottom + 36, 14, C_MUTED, Paint.Align.LEFT, false);
            text(canvas, "ou escaneie o QR Code com outro celular", right.left + 28, qr.bottom + 58, 14, C_MUTED, Paint.Align.LEFT, false);
            int dots = (int) ((System.currentTimeMillis() / 400) % 4);
            text(canvas, "⏳ Aguardando o pagamento" + "...".substring(0, dots), cx, right.bottom - 96, 22, GOLD, Paint.Align.CENTER, true);
            newBtn.set(cx - 150, right.bottom - 82, cx + 150, right.bottom - 42);
            pillButton(canvas, newBtn, "Cancelar / trocar valor", Color.rgb(90, 98, 116), 18);
        } else if (WalletManager.creditedPoints > 0) {
            text(canvas, "✅", cx, right.top + 130, 90, Color.WHITE, Paint.Align.CENTER, false);
            text(canvas, "+" + WalletManager.creditedPoints + " pontos!", cx, right.top + 200, 44, C_GOOD, Paint.Align.CENTER, true);
            text(canvas, "Pagamento confirmado. Bom jogo!", cx, right.top + 240, 22, Color.WHITE, Paint.Align.CENTER, false);
            newBtn.set(cx - 170, right.top + 280, cx + 170, right.top + 340);
            actionButton(canvas, newBtn, "FAZER OUTRO", true, C_ACCENT);
        } else {
            text(canvas, "💠", cx, right.top + 120, 80, Color.WHITE, Paint.Align.CENTER, false);
            float y = wrap(canvas, "Escolha um valor e toque em GERAR PIX. Você paga pelo app do seu banco e os pontos são creditados automaticamente.", right.left + 40, right.top + 190, right.width() - 80, 22, Color.WHITE, 32);
            wrap(canvas, "Sugestões: 10 pontos = VIP por 30 dias  •  35 pontos = Passe Premium da temporada.", right.left + 40, y + 10, right.width() - 80, 18, C_MUTED, 26);
        }
        String st = WalletManager.status;
        if (st != null && !st.isEmpty() && !WalletManager.hasPix() && WalletManager.creditedPoints == 0)
            text(canvas, fit(st, right.width() - 40), cx, right.bottom - 70, 20, st.startsWith("⚠") || st.startsWith("📶") ? C_BAD : GOLD, Paint.Align.CENTER, true);
        paint.setTextSize(13);
        text(canvas, "Pagamento processado pelo Mercado Pago. Pontos não são reembolsáveis em dinheiro.", cx, right.bottom - 18, 13, C_MUTED, Paint.Align.CENTER, false);
    }

    private static String tag(int p) { return p == 10 ? "  •  VIP" : p == 35 ? "  •  Passe" : ""; }
}
