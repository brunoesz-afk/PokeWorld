package com.example.pokeworld.managers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Ranking global da Arena pelo servidor multiplayer. Sem conexão, a tela cai para a liga local. */
public final class ArenaNet {
    private ArenaNet() {}

    private static volatile List<ArenaLadder.Row> top = Collections.emptyList();
    private static volatile int myRank = 0, total = 0;
    private static volatile boolean loaded = false;
    private static long lastFetch = 0;

    public static boolean loaded() { return loaded; }
    public static List<ArenaLadder.Row> top() { return top; }
    public static int myRank() { return myRank; }
    public static int total() { return total; }

    /** Envia seus pontos e baixa o top 50 (no máximo uma vez a cada 20 s). */
    public static void refresh(RewardManager.Store s, int power, boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastFetch < 20000) return;
        lastFetch = now;
        try {
            JSONObject d = new JSONObject().put("rp", ArenaLadder.rp(s)).put("wins", ArenaLadder.wins(s))
                    .put("losses", ArenaLadder.losses(s)).put("power", power);
            MultiplayerManager mm = MultiplayerManager.getInstance();
            boolean sent = mm.emitAck("arenaScore", d, a -> fetchTop());
            if (!sent) loaded = false;
        } catch (Exception ignored) { }
    }

    private static void fetchTop() {
        try {
            MultiplayerManager.getInstance().emitAck("arenaTop", new JSONObject(), args -> {
                if (args.length == 0 || !(args[0] instanceof JSONObject)) return;
                JSONObject res = (JSONObject) args[0];
                if (!res.optBoolean("ok")) return;
                JSONArray arr = res.optJSONArray("top");
                List<ArenaLadder.Row> rows = new ArrayList<>();
                if (arr != null) for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o == null) continue;
                    rows.add(new ArenaLadder.Row(o.optInt("rank", i + 1), o.optString("name", "Treinador"), o.optInt("rp"),
                            o.optInt("wins"), o.optInt("losses"), o.optBoolean("me")));
                }
                top = rows; myRank = res.optInt("myRank"); total = res.optInt("total"); loaded = true;
            });
        } catch (Exception ignored) { }
    }
}
