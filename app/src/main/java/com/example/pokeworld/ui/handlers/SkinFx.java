package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

import com.example.pokeworld.managers.PointsManager;
import com.example.pokeworld.managers.SkinManager;

import java.util.HashMap;
import java.util.Map;

/** Desenho das skins: filtro de cor (matiz/saturação/brilho) e aura. Não altera nenhum atributo do jogo. */
public final class SkinFx {
    private SkinFx() {}

    private static final Map<String, ColorMatrixColorFilter> CACHE = new HashMap<>();

    private static ColorMatrix hue(float deg) {
        double rad = Math.toRadians(deg);
        float c = (float) Math.cos(rad), s = (float) Math.sin(rad);
        return new ColorMatrix(new float[]{
                0.213f + c * 0.787f - s * 0.213f, 0.715f - c * 0.715f - s * 0.715f, 0.072f - c * 0.072f + s * 0.928f, 0, 0,
                0.213f - c * 0.213f + s * 0.143f, 0.715f + c * 0.285f + s * 0.140f, 0.072f - c * 0.072f - s * 0.283f, 0, 0,
                0.213f - c * 0.213f - s * 0.787f, 0.715f - c * 0.715f + s * 0.715f, 0.072f + c * 0.928f + s * 0.072f, 0, 0,
                0, 0, 0, 1, 0});
    }

    private static ColorMatrixColorFilter build(SkinManager.Skin k, float hueDeg) {
        ColorMatrix m = hue(hueDeg);
        ColorMatrix sat = new ColorMatrix(); sat.setSaturation(k.sat);
        m.postConcat(sat);
        ColorMatrix sc = new ColorMatrix(); sc.setScale(k.r, k.g, k.b, 1f);
        m.postConcat(sc);
        return new ColorMatrixColorFilter(m);
    }

    /** Põe a cor da skin no Paint (null = visual normal). */
    public static void apply(Paint p, SkinManager.Skin k) {
        if (k == null) return;
        if (k.rainbow) {
            int step = (int) ((System.currentTimeMillis() / 60) % 36) * 10;   // gira o matiz, em passos de 10° (reaproveita o cache)
            String key = k.id + "#" + step;
            ColorMatrixColorFilter f = CACHE.get(key);
            if (f == null) { f = build(k, step); CACHE.put(key, f); }
            p.setColorFilter(f);
        } else {
            ColorMatrixColorFilter f = CACHE.get(k.id);
            if (f == null) { f = build(k, k.hue); CACHE.put(k.id, f); }
            p.setColorFilter(f);
        }
    }

    /** Brilho suave embaixo/atrás do sprite (só para skins com aura). */
    public static void aura(Canvas canvas, float x, float y, float radius, SkinManager.Skin k) {
        if (k == null || k.aura == 0) return;
        float pulse = 0.85f + 0.15f * (float) Math.sin(System.currentTimeMillis() / 300.0);
        Paint p = new Paint();
        p.setAntiAlias(true);
        int c = k.aura;
        int a = (c >>> 24);
        p.setShader(new RadialGradient(x, y, radius * pulse, (a << 24) | (c & 0xFFFFFF), c & 0x00FFFFFF, Shader.TileMode.CLAMP));
        canvas.drawCircle(x, y, radius * pulse, p);
    }

    /** Skin equipada do tipo pedido, ou null. */
    public static SkinManager.Skin equipped(SkinManager.Kind kind) {
        com.example.pokeworld.managers.RewardManager.Store s = PointsManager.store();
        return s == null ? null : SkinManager.equipped(s, kind);
    }
}
