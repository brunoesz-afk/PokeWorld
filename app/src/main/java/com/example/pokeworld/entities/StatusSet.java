package com.example.pokeworld.entities;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.example.pokeworld.managers.SpriteManager;

/** Estados ativos de um Pokémon (inimigo ou companheiro): tempo restante, dano por segundo e desenho animado. */
public class StatusSet {
    private final long[] until = new long[Status.values().length];
    private long lastTick = 0;
    private final Paint paint = new Paint();

    /** Aplica o estado; devolve true se entrou agora (false se imune ou se já estava ativo e só renovou). */
    public boolean apply(Status s, long durationMs, ElementalType ownerType) {
        if (s == null || s.immune(ownerType)) return false;
        long now = System.currentTimeMillis();
        boolean fresh = until[s.ordinal()] <= now;
        until[s.ordinal()] = Math.max(until[s.ordinal()], now + durationMs);
        if (s.stuns()) { // sono e gelo não acumulam com o outro
            Status other = s == Status.SLEEP ? Status.FREEZE : Status.SLEEP;
            until[other.ordinal()] = 0;
        }
        return fresh;
    }

    public boolean has(Status s) { return until[s.ordinal()] > System.currentTimeMillis(); }

    public boolean any() {
        long now = System.currentTimeMillis();
        for (long u : until) if (u > now) return true;
        return false;
    }

    /** Dormindo ou congelado: não anda nem ataca. */
    public boolean stunned() { return has(Status.SLEEP) || has(Status.FREEZE); }

    public void clear() { for (int i = 0; i < until.length; i++) until[i] = 0; }

    /** Tomar dano acorda quem está dormindo. */
    public void wake() { until[Status.SLEEP.ordinal()] = 0; }

    /** Dano de queimadura/veneno: no máximo 1x por segundo. {@code strong}: chefe (sofre metade). */
    public int tick(int maxHp, boolean strong) {
        long now = System.currentTimeMillis();
        if (now - lastTick < 1000) return 0;
        lastTick = now;
        int dmg = 0;
        if (has(Status.BURN)) dmg += Math.max(2, (int) (maxHp * 0.025));
        if (has(Status.POISON)) dmg += Math.max(2, (int) (maxHp * 0.035));
        return strong ? dmg / 2 : dmg;
    }

    /** Ícones animados (folhas 3x1) acima do Pokémon e um anel colorido pulsando em volta. */
    public void draw(Canvas canvas, double x, double y, double radius) {
        long now = System.currentTimeMillis();
        int n = 0;
        for (Status s : Status.values()) if (until[s.ordinal()] > now) n++;
        if (n == 0) return;
        int i = 0, size = 48;
        for (Status s : Status.values()) {
            if (until[s.ordinal()] <= now) continue;
            float pulse = (float) (Math.sin(now / 160.0 + s.ordinal()) * 4);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3);
            paint.setColor((s.color & 0x00FFFFFF) | 0x80000000);
            canvas.drawCircle((float) x, (float) y, (float) radius * 1.35f + pulse, paint);
            paint.setStyle(Paint.Style.FILL);
            Bitmap[][] sheet = SpriteManager.getInstance().getSheet("st_" + s.key + "_3x1");
            float cx = (float) (x + (i - (n - 1) / 2f) * (size + 2)), cy = (float) (y - radius * 1.6 - 6);
            if (sheet != null && sheet.length > 0) {
                Bitmap b = sheet[0][(int) ((now / 140) % sheet[0].length)];
                canvas.drawBitmap(b, null, new Rect((int) cx - size / 2, (int) cy - size / 2, (int) cx + size / 2, (int) cy + size / 2), null);
            } else {
                paint.setTextSize(30); paint.setTextAlign(Paint.Align.CENTER); paint.setColor(s.color);
                canvas.drawText(s.icon, cx, cy, paint);
            }
            i++;
        }
    }
}
