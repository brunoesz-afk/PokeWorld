package com.example.pokeworld.managers;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.example.pokeworld.entities.CapturedCreature;

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

    public void saveSession(Context context) {
        if (context == null) return;
        context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
            .edit()
            .putString("access_token", accessToken)
            .putString("user_id", userId)
            .apply();
    }

    public void loadSession(Context context) {
        if (context == null) return;
        accessToken = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE).getString("access_token", "");
        userId = context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE).getString("user_id", "");
    }

    public void signOut(Context context) {
        if (context != null) {
            context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE).edit().clear().apply();
        }
        accessToken = "";
        userId = "";
    }

    public void signIn(Context context, String email, String password, AuthCallback callback) {
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
                    saveSession(context);

                    if (callback != null) callback.onResult(true, "Conta vinculada com sucesso ao site!");
                } else {
                    if (callback != null) callback.onResult(false, "E-mail ou senha incorretos.");
                }
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro no login: " + e.getMessage());
                if (callback != null) callback.onResult(false, "Erro de conexão: " + e.getMessage());
            }
        }).start();
    }

    public void syncPlayerProfile(int playerLevel, int experience, String progressSummary, List<String> badges) {
        if (!isLogged()) return;
        new Thread(() -> {
            try {
                URL url = new URL(supabaseUrl + "/rest/v1/player_profiles?user_id=eq." + userId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("PATCH");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Authorization", "Bearer " + (accessToken.isEmpty() ? supabaseAnonKey : accessToken));
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=minimal");
                conn.setDoOutput(true);

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                String utcTime = sdf.format(new Date());

                JSONObject json = new JSONObject();
                json.put("player_level", playerLevel);
                json.put("experience", experience);
                json.put("progress_summary", progressSummary != null ? progressSummary : "Aventurando no PokeWorld");
                
                JSONArray badgesArray = new JSONArray();
                if (badges != null) {
                    for (String b : badges) badgesArray.put(b);
                }
                json.put("badges", badgesArray);
                json.put("last_game_sync_at", utcTime);

                OutputStream os = conn.getOutputStream();
                os.write(json.toString().getBytes("UTF-8"));
                os.close();

                Log.d("SupabaseManager", "Sync Profile Response: " + conn.getResponseCode());
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro ao sincronizar player_profiles: " + e.getMessage());
            }
        }).start();
    }

    public void syncParty(List<CapturedCreature> party) {
        if (!isLogged()) return;
        new Thread(() -> {
            try {
                URL deleteUrl = new URL(supabaseUrl + "/rest/v1/pokemon_party?user_id=eq." + userId);
                HttpURLConnection delConn = (HttpURLConnection) deleteUrl.openConnection();
                delConn.setRequestMethod("DELETE");
                delConn.setRequestProperty("apikey", supabaseAnonKey);
                delConn.setRequestProperty("Authorization", "Bearer " + (accessToken.isEmpty() ? supabaseAnonKey : accessToken));
                delConn.getResponseCode();

                for (int i = 0; i < Math.min(party.size(), 6); i++) {
                    CapturedCreature c = party.get(i);
                    int slot = i + 1;

                    URL url = new URL(supabaseUrl + "/rest/v1/pokemon_party");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("apikey", supabaseAnonKey);
                    conn.setRequestProperty("Authorization", "Bearer " + (accessToken.isEmpty() ? supabaseAnonKey : accessToken));
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject json = new JSONObject();
                    json.put("user_id", userId);
                    json.put("slot", slot);
                    json.put("pokemon_name", c.name);
                    json.put("pokemon_level", 10);
                    json.put("species_id", c.name.toLowerCase());
                    json.put("nickname", c.isShiny ? c.name + " (Shiny)" : c.name);

                    OutputStream os = conn.getOutputStream();
                    os.write(json.toString().getBytes("UTF-8"));
                    os.close();
                    conn.getResponseCode();
                }
                Log.d("SupabaseManager", "pokemon_party sincronizada com sucesso!");
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro ao sincronizar pokemon_party: " + e.getMessage());
            }
        }).start();
    }

    public void fetchCloudSave(com.example.pokeworld.core.DatabaseHelper dbHelper, Runnable onComplete) {
        if (!isLogged()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        new Thread(() -> {
            try {
                URL profileUrl = new URL(supabaseUrl + "/rest/v1/player_profiles?user_id=eq." + userId + "&select=*");
                HttpURLConnection conn = (HttpURLConnection) profileUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Authorization", "Bearer " + (accessToken.isEmpty() ? supabaseAnonKey : accessToken));

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONArray arr = new JSONArray(sb.toString());
                    if (arr.length() > 0) {
                        JSONObject profile = arr.getJSONObject(0);
                        int level = profile.optInt("player_level", 1);
                        int exp = profile.optInt("experience", 0);
                        dbHelper.updatePlayerStatsFromCloud(level, exp);
                    }
                }

                URL partyUrl = new URL(supabaseUrl + "/rest/v1/pokemon_party?user_id=eq." + userId + "&select=*");
                HttpURLConnection pConn = (HttpURLConnection) partyUrl.openConnection();
                pConn.setRequestMethod("GET");
                pConn.setRequestProperty("apikey", supabaseAnonKey);
                pConn.setRequestProperty("Authorization", "Bearer " + (accessToken.isEmpty() ? supabaseAnonKey : accessToken));

                if (pConn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(pConn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONArray partyArr = new JSONArray(sb.toString());
                    if (partyArr.length() > 0) {
                        dbHelper.clearParty();
                        for (int i = 0; i < partyArr.length(); i++) {
                            JSONObject poke = partyArr.getJSONObject(i);
                            String pName = poke.optString("pokemon_name", "Pikachu");
                            dbHelper.addCreatureToCollectionDb(pName, false, com.example.pokeworld.entities.Rarity.COMMON, true, 0);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro ao carregar save da nuvem: " + e.getMessage());
            } finally {
                if (onComplete != null) onComplete.run();
            }
        }).start();
    }
}
