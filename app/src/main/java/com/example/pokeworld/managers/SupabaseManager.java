package com.example.pokeworld.managers;

import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SupabaseManager {
    private static SupabaseManager instance;
    
    private final String supabaseUrl = "https://bfgfpsymzycdjzgkhaqx.supabase.co";
    private final String supabaseAnonKey = "sb_publishable_pW7f4bvF_EVMAMg0-eRjgw_4fHigyUt";
    private String accessToken = "";
    private String userId = "";

    public interface AuthCallback {
        void onResult(boolean success, String message);
    }

    private SupabaseManager() {}

    public static synchronized SupabaseManager getInstance() {
        if (instance == null) {
            instance = new SupabaseManager();
        }
        return instance;
    }

    public boolean isLogged() {
        return !userId.isEmpty();
    }

    public void signIn(String email, String password, AuthCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(supabaseUrl + "/auth/v1/token?grant_type=password");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("email", email);
                json.put("password", password);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject res = new JSONObject(sb.toString());
                    this.accessToken = res.optString("access_token", "");
                    JSONObject userObj = res.optJSONObject("user");
                    if (userObj != null) {
                        this.userId = userObj.optString("id", "");
                    }

                    if (callback != null) callback.onResult(true, "Login realizado com sucesso!");
                } else {
                    if (callback != null) callback.onResult(false, "E-mail ou senha incorretos.");
                }
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro no login: " + e.getMessage());
                if (callback != null) callback.onResult(false, "Erro de conexão: " + e.getMessage());
            }
        }).start();
    }

    public void syncPlayerProfile(String playerName, int playerLevel, String region, String progressNote) {
        if (!isLogged()) return;
        new Thread(() -> {
            try {
                URL url = new URL(supabaseUrl + "/rest/v1/player_profiles");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Authorization", "Bearer " + (accessToken.isEmpty() ? supabaseAnonKey : accessToken));
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("user_id", userId);
                json.put("player_name", playerName);
                json.put("player_level", playerLevel);
                json.put("region", region != null ? region : "Base Central");
                json.put("progress_note", progressNote != null ? progressNote : "Jogando PokeWorld");

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                Log.d("SupabaseManager", "Sync Profile Response: " + conn.getResponseCode());
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro ao sincronizar player_profiles: " + e.getMessage());
            }
        }).start();
    }

    public void syncParty(java.util.List<com.example.pokeworld.entities.CapturedCreature> party) {
        if (!isLogged()) return;
        new Thread(() -> {
            try {
                for (int i = 0; i < Math.min(party.size(), 6); i++) {
                    com.example.pokeworld.entities.CapturedCreature c = party.get(i);
                    int slot = i + 1;

                    URL url = new URL(supabaseUrl + "/rest/v1/pokemon_party");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("apikey", supabaseAnonKey);
                    conn.setRequestProperty("Authorization", "Bearer " + (accessToken.isEmpty() ? supabaseAnonKey : accessToken));
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setRequestProperty("Prefer", "resolution=merge-duplicates");
                    conn.setDoOutput(true);

                    JSONObject json = new JSONObject();
                    json.put("user_id", userId);
                    json.put("slot", slot);
                    json.put("pokemon_name", c.name);
                    json.put("pokemon_level", 10);

                    OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes("UTF-8"));
                    os.close();
                    conn.getResponseCode();
                }
                Log.d("SupabaseManager", "Party sincronizada com sucesso no Supabase!");
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro ao sincronizar pokemon_party: " + e.getMessage());
            }
        }).start();
    }
}
