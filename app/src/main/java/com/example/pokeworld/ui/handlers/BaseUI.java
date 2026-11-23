package com.example.pokeworld.ui.handlers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import com.example.pokeworld.core.GameView;

public abstract class BaseUI implements UIHandler {
    protected Paint paint = new Paint();

    protected void drawOverlay(Canvas canvas, String title, int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(Color.argb(100, 255, 255, 255));
        canvas.drawRect(20, 20, canvas.getWidth() - 20, canvas.getHeight() - 20, paint);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE); paint.setTextSize(45); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(title, canvas.getWidth()/2f, 100, paint);
    }

    protected void drawExitButton(Canvas canvas, RectF b) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(150, 0, 0));
        canvas.drawRoundRect(b, 10, 10, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.WHITE);
        paint.setStrokeWidth(3);
        canvas.drawRoundRect(b, 10, 10, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(22);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("SAIR", b.centerX(), b.centerY() + 8, paint);
    }

    protected void drawButton(Canvas canvas, RectF b, String t) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(b, 15, 15, paint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(b, 15, 15, paint);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(26); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(t, b.centerX(), b.centerY() + 10, paint);
    }

    protected void drawButtonWithMultine(Canvas canvas, RectF b, String t) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.BLACK);
        canvas.drawRoundRect(b, 15, 15, paint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(b, 15, 15, paint);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(22); paint.setTextAlign(Paint.Align.CENTER);
        
        String[] lines = t.split("\n");
        float y = b.centerY() - ((lines.length - 1) * 12);
        for (String line : lines) {
            canvas.drawText(line, b.centerX(), y + 8, paint);
            y += 25;
        }
    }
    
    protected void drawIcon(Canvas canvas, RectF b, String icon) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(80, 255, 255, 255));
        canvas.drawRoundRect(b, 20, 20, paint);
        
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.argb(150, 255, 255, 255));
        canvas.drawRoundRect(b, 20, 20, paint);
        
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE); paint.setTextSize(40); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(icon, b.centerX(), b.centerY() + 15, paint);
    }

    protected void drawCooldownIcon(Canvas canvas, RectF b, String icon, float progress) {
        paint.setColor(Color.argb(150, 0, 0, 0));
        canvas.drawRect(b, paint);
        if (progress < 1.0f) {
            paint.setColor(Color.argb(100, 255, 255, 255));
            canvas.drawRect(b.left, b.bottom - (b.height() * (1.0f - progress)), b.right, b.bottom, paint);
        }
        paint.setColor(progress >= 1.0f ? Color.WHITE : Color.GRAY);
        paint.setTextSize(35); paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(icon, b.centerX(), b.centerY() + 12, paint);
    }

    protected void drawStatStars(Canvas canvas, float x, float y, float value) {
        for (int i = 0; i < 5; i++) {
            paint.setColor(i < (int)value ? Color.rgb(60, 180, 120) : Color.rgb(40, 50, 60));
            canvas.drawText("★", x + i * 35, y + 15, paint);
        }
    }

    // ------------------------------------------------------------------
    // Kit de layout compartilhado pelas telas (mochila, time, torre, runas...)
    // ------------------------------------------------------------------
    protected static final int C_BG = Color.rgb(11, 16, 26);
    protected static final int C_PANEL = Color.argb(235, 24, 34, 52);
    protected static final int C_PANEL_HI = Color.argb(240, 40, 58, 88);
    protected static final int C_LINE = Color.argb(120, 150, 175, 215);
    protected static final int C_ACCENT = Color.rgb(255, 196, 64);
    protected static final int C_MUTED = Color.rgb(150, 162, 182);
    protected static final int C_GOOD = Color.rgb(90, 210, 130);
    protected static final int C_BAD = Color.rgb(230, 90, 90);

    /** Fundo + faixa de título à esquerda; o botão SAIR fica à direita (mesmo lugar do botão global). */
    protected void drawHeader(Canvas canvas, String title, String subtitle, RectF exitBtn) {
        float w = canvas.getWidth();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(C_BG);
        canvas.drawRect(0, 0, w, canvas.getHeight(), paint);
        paint.setColor(Color.argb(255, 18, 26, 42));
        canvas.drawRect(0, 0, w, 104, paint);
        paint.setColor(C_ACCENT);
        canvas.drawRect(0, 100, w, 104, paint);
        text(canvas, title, 36, 54, 34, Color.WHITE, Paint.Align.LEFT, true);
        if (subtitle != null) text(canvas, subtitle, 38, 86, 18, C_MUTED, Paint.Align.LEFT, false);
        exitBtn.set(w - 180, 30, w - 40, 90);
        drawExitButton(canvas, exitBtn);
    }

    protected void panel(Canvas canvas, RectF r, int fill, int stroke, float strokeW) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(fill);
        canvas.drawRoundRect(r, 14, 14, paint);
        if (strokeW > 0) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeW);
            paint.setColor(stroke);
            canvas.drawRoundRect(r, 14, 14, paint);
        }
        paint.setStyle(Paint.Style.FILL);
    }

    protected void text(Canvas canvas, String s, float x, float y, float size, int color, Paint.Align align, boolean bold) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTextAlign(align);
        paint.setFakeBoldText(bold);
        canvas.drawText(s, x, y, paint);
        paint.setFakeBoldText(false);
    }

    /** Corta o texto com "…" para caber em maxW usando o tamanho de fonte atual do paint. */
    protected String fit(String s, float maxW) {
        if (s == null) return "";
        if (paint.measureText(s) <= maxW) return s;
        int n = s.length();
        while (n > 1 && paint.measureText(s, 0, n) + paint.measureText("…") > maxW) n--;
        return s.substring(0, n) + "…";
    }

    /** Quebra o texto em linhas de até maxW. Devolve o y logo abaixo da última linha. */
    protected float wrap(Canvas canvas, String s, float x, float y, float maxW, float size, int color, float lineH) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setFakeBoldText(false);
        StringBuilder line = new StringBuilder();
        for (String word : s.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (paint.measureText(test) > maxW && line.length() > 0) {
                canvas.drawText(line.toString(), x, y, paint);
                y += lineH;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(test);
            }
        }
        if (line.length() > 0) { canvas.drawText(line.toString(), x, y, paint); y += lineH; }
        return y;
    }

    protected void chip(Canvas canvas, RectF r, String label, boolean active, int accent) {
        panel(canvas, r, active ? accent : Color.argb(200, 30, 42, 62), active ? Color.WHITE : C_LINE, active ? 3 : 2);
        paint.setTextSize(18);
        text(canvas, fit(label, r.width() - 16), r.centerX(), r.centerY() + 6, 18, active ? Color.BLACK : Color.WHITE, Paint.Align.CENTER, active);
    }

    protected void actionButton(Canvas canvas, RectF r, String label, boolean enabled, int color) {
        panel(canvas, r, enabled ? color : Color.rgb(48, 54, 66), enabled ? Color.WHITE : Color.rgb(80, 88, 104), 3);
        text(canvas, label, r.centerX(), r.centerY() + 9, 26, enabled ? Color.BLACK : Color.rgb(120, 128, 142), Paint.Align.CENTER, true);
    }

    protected void bar(Canvas canvas, RectF r, float frac, int color) {
        frac = Math.max(0f, Math.min(1f, frac));
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(200, 10, 14, 22));
        canvas.drawRoundRect(r, r.height() / 2f, r.height() / 2f, paint);
        if (frac > 0) {
            paint.setColor(color);
            canvas.drawRoundRect(new RectF(r.left, r.top, r.left + r.width() * frac, r.bottom), r.height() / 2f, r.height() / 2f, paint);
        }
    }

    /** Barrinha fina que mostra a posição da rolagem à direita de uma área. */
    protected void scrollbar(Canvas canvas, RectF area, float scroll, float maxScroll) {
        if (maxScroll <= 0) return;
        float trackH = area.height();
        float thumbH = Math.max(40, trackH * trackH / (trackH + maxScroll));
        float top = area.top + (trackH - thumbH) * (scroll / maxScroll);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(140, 255, 255, 255));
        canvas.drawRoundRect(new RectF(area.right - 6, top, area.right - 1, top + thumbH), 3, 3, paint);
    }
}
