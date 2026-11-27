package com.example.pokeworld.ui.handlers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;
import com.example.pokeworld.managers.SpriteManager;

import java.util.Map;

/** Fábrica de Montaria do reino: selas especiais e velocidade da montaria. */
public class MountFactoryUI extends BaseUI {
    public final RectF exitBtn = new RectF();
    public final RectF saddleFire = new RectF();
    public final RectF saddleWater = new RectF();
    public final RectF upgradeSpeed = new RectF();

    public static final int SADDLE_COST = 30, SPEED_COST = 50;

    @Override
    public void draw(Canvas canvas, GameView gv) {
        float w = canvas.getWidth(), h = canvas.getHeight();
        drawHeader(canvas, "🐎 FÁBRICA DE MONTARIA", "Fabrique selas e deixe a sua montaria mais veloz", exitBtn);
        Map<String, Integer> inv = gv.player.getInventory();
        int speedLv = inv.getOrDefault("MountSpeed_Lv", 1);

        float top = 124, bottom = h - 24, pad = 30;
        float leftW = Math.min(w * 0.32f, 420);
        RectF mount = new RectF(pad, top, pad + leftW, bottom);
        RectF list = new RectF(mount.right + 20, top, w - pad, bottom);

        // ---- esquerda: a montaria
        panel(canvas, mount, C_PANEL, C_LINE, 2);
        text(canvas, "SUA MONTARIA", mount.centerX(), mount.top + 40, 24, C_ACCENT, Paint.Align.CENTER, true);
        String sp = gv.companion != null ? gv.companion.getSpecies() : null;
        Bitmap bmp = sp == null ? null : SpriteManager.getInstance().getUnitSprite(sp, 0, 0, gv.companion.isShiny());
        float sz = Math.min(mount.width() - 80, (mount.height() - 250) * 0.9f);
        if (bmp != null && sz > 40) canvas.drawBitmap(bmp, null, new Rect((int) (mount.centerX() - sz / 2), (int) (mount.top + 60), (int) (mount.centerX() + sz / 2), (int) (mount.top + 60 + sz)), null);
        float y = mount.top + 60 + Math.max(60, sz) + 28;
        text(canvas, sp == null ? "—" : sp, mount.centerX(), y, 22, Color.WHITE, Paint.Align.CENTER, true);
        text(canvas, "Velocidade Nv. " + speedLv + "  (+" + (speedLv - 1) * 10 + "%)", mount.centerX(), y + 34, 20, C_GOOD, Paint.Align.CENTER, true);
        bar(canvas, new RectF(mount.left + 30, y + 50, mount.right - 30, y + 64), Math.min(1f, (speedLv - 1) / 20f), C_GOOD);
        text(canvas, "Cada nível: +10% de velocidade", mount.centerX(), y + 92, 16, C_MUTED, Paint.Align.CENTER, false);
        boolean fire = inv.getOrDefault("Sela de Fogo", 0) > 0, water = inv.getOrDefault("Sela Aquática", 0) > 0;
        text(canvas, (fire ? "🔥 Sela de Fogo ✓" : "🔥 Sela de Fogo —") + "    " + (water ? "🌊 Sela Aquática ✓" : "🌊 Sela Aquática —"), mount.centerX(), mount.bottom - 22, 15, C_MUTED, Paint.Align.CENTER, false);

        // ---- direita: três cartões
        float gap = 16, ch = (list.height() - gap * 2) / 3f;
        card(canvas, new RectF(list.left, list.top, list.right, list.top + ch), saddleFire, "🔥", "Sela de Fogo",
                "Cavalgando com ela você atravessa lava sem se queimar.", "Essência de Fogo", inv.getOrDefault("Essência de Fogo", 0), SADDLE_COST,
                fire ? "JÁ POSSUI" : "FABRICAR", !fire, Color.rgb(255, 140, 70));
        card(canvas, new RectF(list.left, list.top + ch + gap, list.right, list.top + 2 * ch + gap), saddleWater, "🌊", "Sela Aquática",
                "Cavalgando com ela você anda sobre a água.", "Cristal de Água", inv.getOrDefault("Cristal de Água", 0), SADDLE_COST,
                water ? "JÁ POSSUI" : "FABRICAR", !water, Color.rgb(100, 180, 255));
        card(canvas, new RectF(list.left, list.top + 2 * (ch + gap), list.right, list.bottom), upgradeSpeed, "⚡", "Velocidade da Montaria  Nv." + speedLv + " ➜ " + (speedLv + 1),
                "+10% de velocidade ao andar (e mais ainda montado).", "Madeira", inv.getOrDefault("Madeira", 0), SPEED_COST,
                "MELHORAR", true, Color.rgb(255, 220, 90));
    }

    private void card(Canvas canvas, RectF r, RectF btn, String icon, String title, String desc, String res, int have, int need, String action, boolean available, int accent) {
        panel(canvas, r, C_PANEL, C_LINE, 2);
        text(canvas, icon, r.left + 52, r.centerY() + 18, 52, Color.WHITE, Paint.Align.CENTER, false);
        float tx = r.left + 110, bw = Math.min(250, r.width() * 0.30f);
        paint.setTextSize(24);
        text(canvas, fit(title, r.width() - 110 - bw - 40), tx, r.top + 40, 24, Color.WHITE, Paint.Align.LEFT, true);
        paint.setTextSize(16);
        float yy = wrap(canvas, desc, tx, r.top + 66, r.width() - 110 - bw - 40, 16, C_MUTED, 20);
        boolean enough = have >= need;
        text(canvas, res + ": " + have + " / " + need, tx, Math.max(yy + 8, r.bottom - 26), 18, enough ? C_GOOD : C_BAD, Paint.Align.LEFT, true);
        btn.set(r.right - bw - 20, r.centerY() - 32, r.right - 20, r.centerY() + 32);
        actionButton(canvas, btn, action, available && enough, accent);
        if (available && !enough) text(canvas, "faltam " + (need - have), btn.centerX(), btn.bottom + 22, 15, C_BAD, Paint.Align.CENTER, false);
    }
}
