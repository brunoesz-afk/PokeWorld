package com.example.pokeworld.managers;

import android.util.Log;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SupabaseManager {
    private static SupabaseManager instance;
    
    // Configurações do Supabase (O usuário define sua URL e Chave Anon pública do site)
    private String supabaseUrl = ""; // Ex: https://seu-projeto.supabase.co
    private String supabaseAnonKey = ""; // Chave anon pública do Supabase
    private String playerEmail = "";

    private SupabaseManager() {}

    public static synchronized SupabaseManager getInstance() {
        if (instance == null) {
            instance = new SupabaseManager();
        }
        return instance;
    }

    public void configure(String url, String anonKey, String email) {
        this.supabaseUrl = url;
        this.supabaseAnonKey = anonKey;
        this.playerEmail = email;
    }

    public boolean isConfigured() {
        return !supabaseUrl.isEmpty() && !supabaseAnonKey.isEmpty() && !playerEmail.isEmpty();
    }

    public void syncPlayerProfile(int gold, int level, int exp, int gems, String activeSpecies) {
        if (!isConfigured()) return;
        new Thread(() -> {
            try {
                URL url = new URL(supabaseUrl + "/rest/v1/players");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Authorization", "Bearer " + supabaseAnonKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("email", playerEmail);
                json.put("gold", gold);
                json.put("level", level);
                json.put("exp", exp);
                json.put("active_species", activeSpecies);
                json.put("gems", gems);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("SupabaseManager", "Sync Profile Response: " + responseCode);
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro ao sincronizar perfil com Supabase: " + e.getMessage());
            }
        }).start();
    }

    public void syncCollection(String creatureName, boolean isShiny, String rarity, boolean isInParty, int gender) {
        if (!isConfigured()) return;
        new Thread(() -> {
            try {
                URL url = new URL(supabaseUrl + "/rest/v1/collection");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Authorization", "Bearer " + supabaseAnonKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("email", playerEmail);
                json.put("creature_name", creatureName);
                json.put("is_shiny", isShiny ? 1 : 0);
                json.put("rarity", rarity);
                json.put("is_in_party", isInParty ? 1 : 0);
                json.put("gender", gender);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                int responseCode = conn.getResponseCode();
                Log.d("SupabaseManager", "Sync Collection Response: " + responseCode);
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro ao sincronizar coleção com Supabase: " + e.getMessage());
            }
        }).start();
    }
}
