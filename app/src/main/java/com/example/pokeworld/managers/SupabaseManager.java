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

    private String refreshToken = "";
    private long expiresAtMs = 0;
    private Context appContext;

    public void saveSession(Context context) {
        if (context != null) appContext = context.getApplicationContext();
        if (appContext == null) return;
        appContext.getSharedPreferences("supabase_session", Context.MODE_PRIVATE)
            .edit()
            .putString("access_token", accessToken)
            .putString("user_id", userId)
            .putString("refresh_token", refreshToken)
            .putLong("expires_at_ms", expiresAtMs)
            .apply();
    }

    public void loadSession(Context context) {
        if (context == null) return;
        appContext = context.getApplicationContext();
        android.content.SharedPreferences sp = appContext.getSharedPreferences("supabase_session", Context.MODE_PRIVATE);
        accessToken = sp.getString("access_token", "");
        userId = sp.getString("user_id", "");
        refreshToken = sp.getString("refresh_token", "");
        expiresAtMs = sp.getLong("expires_at_ms", 0);
        if (expiresAtMs == 0 && !accessToken.isEmpty()) expiresAtMs = jwtExpiryMs(accessToken);
        // sessão antiga sem refresh_token e já vencida: não dá para renovar, então volta a pedir o login (em vez de fingir que está online)
        if (!userId.isEmpty() && refreshToken.isEmpty() && expiresAtMs > 0 && System.currentTimeMillis() > expiresAtMs) {
            Log.w("SupabaseManager", "Sessão vencida e sem refresh_token: faça login de novo.");
            accessToken = ""; userId = ""; expiresAtMs = 0;
            sp.edit().clear().apply();
        }
    }

    /** Lê o "exp" do JWT (segundos) -> ms; 0 se não der. */
    private static long jwtExpiryMs(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length < 2) return 0;
            byte[] raw = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE | android.util.Base64.NO_WRAP | android.util.Base64.NO_PADDING);
            return new JSONObject(new String(raw, "UTF-8")).optLong("exp", 0) * 1000L;
        } catch (Exception e) { return 0; }
    }

    private void readTokens(JSONObject res) {
        accessToken = res.optString("access_token", accessToken);
        String rt = res.optString("refresh_token", "");
        if (!rt.isEmpty()) refreshToken = rt;
        long inSec = res.optLong("expires_in", 0);
        expiresAtMs = inSec > 0 ? System.currentTimeMillis() + inSec * 1000L : jwtExpiryMs(accessToken);
    }

    /**
     * Cabeçalho Authorization sempre com token válido. O token do Supabase vale ~1 h: sem renovar, depois disso
     * toda sincronização (perfil, time, coleção) falhava em silêncio com 401 e o site parava de atualizar.
     * Só chamar fora da thread principal (faz rede quando precisa renovar).
     */
    private synchronized String bearer() {
        if (!refreshToken.isEmpty() && System.currentTimeMillis() > expiresAtMs - 90_000L) {
            try {
                URL url = new URL(supabaseUrl + "/auth/v1/token?grant_type=refresh_token");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("refresh_token", refreshToken);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();
                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    readTokens(new JSONObject(sb.toString()));
                    saveSession(null);
                    Log.d("SupabaseManager", "Token renovado.");
                } else {
                    Log.e("SupabaseManager", "Falha ao renovar o token: HTTP " + code + " (faça login de novo)");
                }
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro ao renovar o token: " + e.getMessage());
            }
        }
        return "Bearer " + (accessToken.isEmpty() ? supabaseAnonKey : accessToken);
    }

    public void signOut(Context context) {
        if (context != null) {
            context.getSharedPreferences("supabase_session", Context.MODE_PRIVATE).edit().clear().apply();
        }
        accessToken = "";
        userId = "";
        refreshToken = "";
        expiresAtMs = 0;
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
                    readTokens(res);
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
                conn.setRequestProperty("Authorization", bearer());
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

    public void syncParty(List<CapturedCreature> party) { syncParty(party, null); }

    /** levelOf: nível real de cada espécie (guardado na nuvem para repor depois de reinstalar). */
    public void syncParty(List<CapturedCreature> party, java.util.function.Function<String, Integer> levelOf) {
        if (!isLogged()) return;
        new Thread(() -> {
            try {
                URL deleteUrl = new URL(supabaseUrl + "/rest/v1/pokemon_party?user_id=eq." + userId);
                HttpURLConnection delConn = (HttpURLConnection) deleteUrl.openConnection();
                delConn.setRequestMethod("DELETE");
                delConn.setRequestProperty("apikey", supabaseAnonKey);
                delConn.setRequestProperty("Authorization", bearer());
                delConn.getResponseCode();

                for (int i = 0; i < Math.min(party.size(), 6); i++) {
                    CapturedCreature c = party.get(i);
                    int slot = i + 1;

                    URL url = new URL(supabaseUrl + "/rest/v1/pokemon_party");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("apikey", supabaseAnonKey);
                    conn.setRequestProperty("Authorization", bearer());
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject json = new JSONObject();
                    json.put("user_id", userId);
                    json.put("slot", slot);
                    json.put("pokemon_name", c.name);
                    json.put("pokemon_level", levelOf != null ? Math.max(1, levelOf.apply(c.name)) : 10);
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

    // ---- backup da coleção inteira (time + defesa da base + caixa) na tabela pokemon_collection ----
    private volatile boolean collectionSyncing = false;
    private String lastCollectionSig = "";

    /** Envia a coleção toda quando mudou. A pokemon_party só aceita 6 lugares, por isso a caixa/defesa vai para outra tabela. */
    public void syncCollection(final List<CapturedCreature> collection, final java.util.function.Function<String, Integer> levelOf) {
        if (!isLogged() || collection == null || collection.isEmpty() || collectionSyncing) return;
        final JSONArray rows = new JSONArray();
        StringBuilder sig = new StringBuilder();
        try {
            int slot = 1;
            for (CapturedCreature c : new java.util.ArrayList<>(collection)) {
                String loc = c.isInParty ? "party" : c.isGuard ? "guard" : "box";
                int lvl = levelOf != null ? Math.max(1, levelOf.apply(c.name)) : 1;
                JSONObject o = new JSONObject();
                o.put("user_id", userId);
                o.put("slot", slot++);
                o.put("pokemon_name", c.name);
                o.put("pokemon_level", lvl);
                o.put("shiny", c.isShiny);
                o.put("rarity", c.rarity != null ? c.rarity.name() : "COMMON");
                o.put("gender", c.gender);
                o.put("boost", c.boost);
                o.put("location", loc);
                rows.put(o);
                sig.append(c.name).append('|').append(loc).append('|').append(c.isShiny).append('|').append(c.rarity).append('|').append(c.gender).append('|').append(c.boost).append('|').append(lvl).append(';');
            }
        } catch (Exception e) { return; }
        final String signature = sig.toString();
        if (signature.equals(lastCollectionSig)) return;
        collectionSyncing = true;
        new Thread(() -> {
            try {
                String auth = bearer();
                URL url = new URL(supabaseUrl + "/rest/v1/pokemon_collection?on_conflict=user_id,slot");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Authorization", auth);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates,return=minimal");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                os.write(rows.toString().getBytes("UTF-8"));
                os.close();
                int code = conn.getResponseCode();
                if (code < 200 || code >= 300) { Log.e("SupabaseManager", "pokemon_collection: HTTP " + code + " (tabela criada no Supabase?)"); return; }
                // tira o que sobrou de uma coleção maior (Pokémon vendido/fundido)
                URL del = new URL(supabaseUrl + "/rest/v1/pokemon_collection?user_id=eq." + userId + "&slot=gt." + rows.length());
                HttpURLConnection dc = (HttpURLConnection) del.openConnection();
                dc.setRequestMethod("DELETE");
                dc.setRequestProperty("apikey", supabaseAnonKey);
                dc.setRequestProperty("Authorization", auth);
                dc.getResponseCode();
                lastCollectionSig = signature;
                Log.d("SupabaseManager", "pokemon_collection sincronizada: " + rows.length());
            } catch (Exception e) {
                Log.e("SupabaseManager", "Erro ao sincronizar pokemon_collection: " + e.getMessage());
            } finally {
                collectionSyncing = false;
            }
        }).start();
    }

    /** Baixa a coleção inteira da nuvem. Retorna null se a tabela não existe/está vazia/falhou. */
    private JSONArray fetchCollection() {
        try {
            URL url = new URL(supabaseUrl + "/rest/v1/pokemon_collection?user_id=eq." + userId + "&select=*&order=slot.asc");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("GET");
            c.setRequestProperty("apikey", supabaseAnonKey);
            c.setRequestProperty("Authorization", bearer());
            if (c.getResponseCode() != 200) return null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();
            JSONArray arr = new JSONArray(sb.toString());
            return arr.length() > 0 ? arr : null;
        } catch (Exception e) {
            Log.e("SupabaseManager", "Erro ao baixar pokemon_collection: " + e.getMessage());
            return null;
        }
    }

    public void fetchCloudSave(com.example.pokeworld.core.DatabaseHelper dbHelper, Runnable onComplete) {
        if (!isLogged()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        new Thread(() -> {
            try {
                final boolean fresh = dbHelper.isFreshInstall();   // só repõe o time/pets da nuvem numa instalação nova
                URL profileUrl = new URL(supabaseUrl + "/rest/v1/player_profiles?user_id=eq." + userId + "&select=*");
                HttpURLConnection conn = (HttpURLConnection) profileUrl.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Authorization", bearer());

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
                pConn.setRequestProperty("Authorization", bearer());

                if (pConn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(pConn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONArray partyArr = new JSONArray(sb.toString());
                    JSONArray full = fresh ? fetchCollection() : null;       // coleção completa (time + defesa + caixa)
                    if (full != null) {
                        java.util.List<CapturedCreature> restored = new java.util.ArrayList<>();
                        java.util.Map<String, Integer> levels = new java.util.HashMap<>();
                        for (int i = 0; i < full.length(); i++) {
                            JSONObject o = full.getJSONObject(i);
                            com.example.pokeworld.entities.Rarity r = com.example.pokeworld.entities.Rarity.COMMON;
                            try { r = com.example.pokeworld.entities.Rarity.valueOf(o.optString("rarity", "COMMON")); } catch (Exception ignored) {}
                            String loc = o.optString("location", "box");
                            CapturedCreature c = new CapturedCreature(o.optString("pokemon_name", "Pikachu"), o.optBoolean("shiny", false), r, "party".equals(loc), o.optInt("gender", 0));
                            c.isGuard = "guard".equals(loc);
                            c.boost = o.optInt("boost", 0);
                            restored.add(c);
                            int lv = o.optInt("pokemon_level", 1);
                            Integer prev = levels.get(c.name);
                            if (prev == null || lv > prev) levels.put(c.name, lv);
                        }
                        dbHelper.replaceCollection(restored);
                        for (java.util.Map.Entry<String, Integer> e : levels.entrySet()) dbHelper.restorePetLevel(e.getKey(), e.getValue());
                    } else if (partyArr.length() > 0 && fresh) {
                        dbHelper.clearParty();
                        for (int i = 0; i < partyArr.length(); i++) {
                            JSONObject poke = partyArr.getJSONObject(i);
                            String pName = poke.optString("pokemon_name", "Pikachu");
                            boolean shiny = poke.optString("nickname", "").contains("(Shiny)");
                            dbHelper.addCreatureToCollectionDb(pName, shiny, com.example.pokeworld.entities.Rarity.COMMON, true, 0);
                            dbHelper.restorePetLevel(pName, poke.optInt("pokemon_level", 1));
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
