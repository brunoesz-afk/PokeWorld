package com.example.pokeworld.managers;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import com.example.pokeworld.entities.*;
import com.example.pokeworld.core.*;

public class VibrationManager {
    private Vibrator vibrator;
    private boolean enabled = true;

    /** Instância do jogo em andamento (para quem não tem o GameView à mão). */
    public static VibrationManager shared;

    public VibrationManager(Context context) {
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        shared = this;
    }

    /** Toque curto seguro de chamar de qualquer lugar. */
    public static void tap(long ms) { if (shared != null) shared.vibrate(ms); }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public void vibrate(long milliseconds) {
        if (vibrator == null || !enabled) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(milliseconds);
        }
    }

    public void dashVibrate() {
        vibrate(50);
    }

    public void hitVibrate() {
        vibrate(100);
    }

    public void ultimateVibrate() {
        if (vibrator == null || !enabled) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0, 100, 50, 200}, -1));
        } else {
            vibrator.vibrate(new long[]{0, 100, 50, 200}, -1);
        }
    }
}
