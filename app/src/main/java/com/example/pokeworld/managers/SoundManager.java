package com.example.pokeworld.managers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Efeitos sonoros curtos (res/raw/sfx_*.wav) tocados por SoundPool. Cada som é carregado na primeira vez que toca.
 * Nomes: fire, water, electric, plant, ice, rock, poison, psychic, dragon, normal, hit, crit, faint, levelup,
 * capture, buy, evolve, click, status, heal, boost.
 */
public class SoundManager {
    private static SoundManager instance;
    private final SoundPool soundPool;
    private final Context context;
    private final Map<String, Integer> ids = new HashMap<>();
    private final Map<String, Long> lastPlayed = new HashMap<>();
    private boolean enabled = true;
    private final java.util.Set<Integer> ready = new java.util.HashSet<>();
    private final java.util.Map<Integer, String> pendingPlay = new HashMap<>();

    private SoundManager(Context ctx) {
        this.context = ctx.getApplicationContext() != null ? ctx.getApplicationContext() : ctx;
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build();
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) {
                ready.add(sampleId);
                if (pendingPlay.remove(sampleId) != null) pool.play(sampleId, 0.8f, 0.8f, 1, 0, 1f);   // tocava o primeiro uso
            }
        });
    }

    public static SoundManager getInstance(Context context) {
        if (instance == null) instance = new SoundManager(context);
        return instance;
    }

    /** Só existe depois que o jogo criou o gerenciador (GameView). */
    public static void play(String name) {
        if (instance != null) instance.playSound(name);
    }

    public void setEnabled(boolean on) { enabled = on; }
    public boolean isEnabled() { return enabled; }

    public void playSound(String name) {
        if (!enabled || name == null) return;
        long now = System.currentTimeMillis();
        Long last = lastPlayed.get(name);
        if (last != null && now - last < 60) return;   // evita rajada do mesmo som no mesmo instante
        lastPlayed.put(name, now);
        Integer id = ids.get(name);
        if (id == null) {
            int res = context.getResources().getIdentifier("sfx_" + name, "raw", context.getPackageName());
            if (res == 0) { ids.put(name, -1); Log.d("SoundManager", "Sem arquivo para o som: " + name); return; }
            id = soundPool.load(context, res, 1);
            ids.put(name, id);
            pendingPlay.put(id, name);
            return;
        }
        if (id < 0) return;
        if (ready.contains(id)) soundPool.play(id, 0.8f, 0.8f, 1, 0, 1f);
    }
}
