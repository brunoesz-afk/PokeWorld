package com.example.pokeworld.managers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

public class SoundManager {
    private static SoundManager instance;
    private SoundPool soundPool;
    private int sndDash, sndCapture, sndLevelUp, sndUltimate, sndHit;

    private SoundManager(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attrs)
                .build();

        // Carregamento (Simulado com logs, requer arquivos em res/raw)
        // sndDash = soundPool.load(context, R.raw.dash, 1);
        Log.d("SoundManager", "Sistemas de áudio inicializados");
    }

    public static SoundManager getInstance(Context context) {
        if (instance == null) instance = new SoundManager(context);
        return instance;
    }

    public void playSound(String type) {
        // soundPool.play(id, 1, 1, 1, 0, 1);
        Log.d("SoundManager", "Tocando som: " + type);
    }
}
