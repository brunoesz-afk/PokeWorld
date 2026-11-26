package com.example.pokeworld.managers;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

public class TimeManager {
    private long startTime;
    private static final long CYCLE_DURATION = 120000; // 2 minutos por ciclo (Dia + Noite)
    private boolean isNight = false;
    private boolean cycleEnabled = false;      // ciclo dia/noite (escurecer + inimigos mais fortes): desligado, ativa nas Configurações
    private Paint filterPaint = new Paint();
    
    private int totalCycles = 0;
    private boolean cycleCounted = false;

    public TimeManager() {
        this.startTime = System.currentTimeMillis();
    }

    public void update() {
        long elapsedTotal = System.currentTimeMillis() - startTime;
        long elapsedInCycle = elapsedTotal % CYCLE_DURATION;
        
        isNight = cycleEnabled && elapsedInCycle > (CYCLE_DURATION / 2);
        
        // Conta ciclos globais
        int currentTotalCycles = (int)(elapsedTotal / CYCLE_DURATION);
        if (currentTotalCycles > totalCycles) {
            totalCycles = currentTotalCycles;
        }
    }

    public void drawFilter(Canvas canvas) {
        if (isNight) {
            filterPaint.setColor(Color.argb(80, 0, 0, 100));
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), filterPaint);
        }
    }

    public void setCycleEnabled(boolean on) { cycleEnabled = on; if (!on) isNight = false; }
    public boolean isCycleEnabled() { return cycleEnabled; }

    public boolean isNight() {
        return isNight;
    }

    public int getTotalCycles() {
        return totalCycles;
    }

    public boolean isTournamentDay() {
        // Torneio acontece a cada 5 ciclos
        return totalCycles > 0 && totalCycles % 5 == 0;
    }

    public String getTimeString() {
        String base = isNight ? "🌙 Noite" : "☀️ Dia";
        if (isTournamentDay()) return base + " [🏆 TORNEIO!]";
        return base;
    }
}
