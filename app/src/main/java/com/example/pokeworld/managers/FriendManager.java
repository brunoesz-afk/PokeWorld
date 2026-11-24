package com.example.pokeworld.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Amigos: cada jogador tem um código (8 letras/números) e um segredo gerados no aparelho.
 * A lista de amigos e os pedidos recebidos ficam salvos aqui; o servidor só roteia pedidos,
 * mostra quem está online e guarda avisos de quem estava offline.
 */
public class FriendManager {
    public static class Friend {
        public String code = "", name = "Treinador", map = "";
        public boolean online;
        public long lastSeen;
    }

    public static class Request {
        public String code = "", name = "Treinador";
    }

    private static final FriendManager INSTANCE = new FriendManager();
    public static FriendManager getInstance() { return INSTANCE; }

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // sem O/0/I/1
    private static final String SECRET_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private SharedPreferences prefs;
    private String myCode = "", mySecret = "", myName = "Treinador";
    private final List<Friend> friends = new ArrayList<>();
    private final List<Request> requests = new ArrayList<>();
    private final ConcurrentLinkedQueue<String> notices = new ConcurrentLinkedQueue<>();
    private volatile long lastSync = 0;

    private FriendManager() {}

    // ------------------------------------------------------------------ identidade
    public synchronized void init(Context ctx) {
        if (prefs != null || ctx == null) return;
        prefs = ctx.getApplicationContext().getSharedPreferences("friends", Context.MODE_PRIVATE);
        SecureRandom rnd = new SecureRandom();
        myCode = prefs.getString("my_code", "");
        mySecret = prefs.getString("my_secret", "");
        if (myCode.length() != 8 || mySecret.length() < 16) {
            StringBuilder c = new StringBuilder(), s = new StringBuilder();
            for (int i = 0; i < 8; i++) c.append(ALPHABET.charAt(rnd.nextInt(ALPHABET.length())));
            for (int i = 0; i < 32; i++) s.append(SECRET_CHARS.charAt(rnd.nextInt(SECRET_CHARS.length())));
            myCode = c.toString();
            mySecret = s.toString();
            prefs.edit().putString("my_code", myCode).putString("my_secret", mySecret).apply();
        }
        myName = clean(prefs.getString("my_name", "Treinador"));
        try {
            JSONArray fa = new JSONArray(prefs.getString("friends", "[]"));
            for (int i = 0; i < fa.length(); i++) {
                JSONObject o = fa.getJSONObject(i);
                Friend f = new Friend();
                f.code = o.optString("code");
                f.name = o.optString("name", "Treinador");
                f.lastSeen = o.optLong("lastSeen", 0);
                if (f.code.length() == 8) friends.add(f);
            }
            JSONArray ra = new JSONArray(prefs.getString("requests", "[]"));
            for (int i = 0; i < ra.length(); i++) {
                JSONObject o = ra.getJSONObject(i);
                Request r = new Request();
                r.code = o.optString("code");
                r.name = o.optString("name", "Treinador");
                if (r.code.length() == 8) requests.add(r);
            }
        } catch (Exception e) { Log.e("FriendManager", "load: " + e.getMessage()); }
    }

    /** Dados enviados ao servidor ao entrar. Sem init() o jogador entra anônimo (sem código). */
    public synchronized JSONObject joinPayload(String fallbackName) {
        JSONObject o = new JSONObject();
        try {
            o.put("name", prefs != null ? myName : (fallbackName != null ? fallbackName : "Treinador"));
            if (prefs != null) { o.put("code", myCode); o.put("secret", mySecret); }
        } catch (Exception ignored) {}
        return o;
    }

    public synchronized String getMyCode() { return myCode; }
    public synchronized String getMyName() { return myName; }

    /** "ABCD-EFGH" */
    public synchronized String displayCode() {
        return myCode.length() == 8 ? myCode.substring(0, 4) + "-" + myCode.substring(4) : "--------";
    }

    public synchronized String shareText() {
        return "Me adicione no PokeWorld! Meu código de amigo: " + displayCode() + " (Amigos → Adicionar amigo)";
    }

    public synchronized void setName(String raw) {
        myName = clean(raw);
        if (prefs != null) prefs.edit().putString("my_name", myName).apply();
        MultiplayerManager.getInstance().rename(myName);
    }

    // ------------------------------------------------------------------ consulta (cópias seguras para a tela)
    public synchronized List<Friend> getFriends() {
        List<Friend> out = new ArrayList<>(friends);
        Collections.sort(out, (a, b) -> {
            if (a.online != b.online) return a.online ? -1 : 1;
            return a.name.compareToIgnoreCase(b.name);
        });
        return out;
    }

    public synchronized List<Request> getRequests() { return new ArrayList<>(requests); }
    public synchronized int requestCount() { return requests.size(); }
    public synchronized int onlineCount() { int n = 0; for (Friend f : friends) if (f.online) n++; return n; }
    public synchronized boolean isFriend(String code) { return find(code) != null; }

    // ------------------------------------------------------------------ ações
    public void sendRequest(String raw) {
        final String code = norm(raw);
        if (code.length() != 8) { notice("Código inválido: são 8 letras/números."); return; }
        boolean mine, already, theyAsked;
        synchronized (this) {
            mine = code.equals(myCode);
            already = find(code) != null;
            theyAsked = findRequest(code) != null;
        }
        if (mine) { notice("Esse é o seu próprio código!"); return; }
        if (already) { notice("Vocês já são amigos."); return; }
        if (theyAsked) { respond(code, true); return; } // ele já tinha te pedido: aceita direto
        try {
            JSONObject d = new JSONObject().put("code", code);
            boolean sent = MultiplayerManager.getInstance().emitAck("friendRequest", d, args -> {
                JSONObject res = args.length > 0 && args[0] instanceof JSONObject ? (JSONObject) args[0] : null;
                if (res != null && res.optBoolean("ok")) notice("Pedido enviado para " + res.optString("name", "o jogador") + "!");
                else notice(res != null ? res.optString("error", "Não foi possível enviar.") : "Sem resposta do servidor.");
            });
            if (!sent) notice("Sem conexão com o servidor. Tente de novo em instantes.");
        } catch (Exception e) { notice("Erro ao enviar o pedido."); }
    }

    public void respond(final String code, final boolean accept) {
        final Request req;
        synchronized (this) { req = findRequest(code); }
        if (req == null) return;
        try {
            JSONObject d = new JSONObject().put("code", code).put("accept", accept);
            boolean sent = MultiplayerManager.getInstance().emitAck("friendRespond", d, args -> {
                JSONObject res = args.length > 0 && args[0] instanceof JSONObject ? (JSONObject) args[0] : null;
                boolean ok = res != null && res.optBoolean("ok");
                synchronized (FriendManager.this) {
                    requests.remove(findRequest(code));
                    if (ok && accept && find(code) == null) {
                        Friend f = new Friend();
                        f.code = code; f.name = req.name; f.online = true;
                        friends.add(f);
                    }
                    persist();
                }
                if (ok) notice(accept ? "🤝 " + req.name + " agora é seu amigo!" : "Pedido de " + req.name + " recusado.");
                else notice(res != null ? res.optString("error", "Pedido expirado.") : "Sem resposta do servidor.");
                if (ok && accept) sync();
            });
            if (!sent) notice("Sem conexão com o servidor. Tente de novo em instantes.");
        } catch (Exception e) { notice("Erro ao responder o pedido."); }
    }

    public void remove(final String code) {
        Friend f;
        synchronized (this) { f = find(code); }
        if (f == null) return;
        try {
            final String nm = f.name;
            boolean sent = MultiplayerManager.getInstance().emitAck("friendRemove", new JSONObject().put("code", code), args -> {
                synchronized (FriendManager.this) { friends.remove(find(code)); persist(); }
                notice(nm + " foi removido dos amigos.");
            });
            if (!sent) notice("Conecte-se ao servidor para remover um amigo.");
        } catch (Exception e) { notice("Erro ao remover."); }
    }

    /** Atualiza online/offline, mapa e nomes dos amigos. */
    public void sync() {
        List<String> codes = new ArrayList<>();
        synchronized (this) { for (Friend f : friends) codes.add(f.code); }
        lastSync = System.currentTimeMillis();
        if (codes.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray();
            for (String c : codes) arr.put(c);
            MultiplayerManager.getInstance().emitAck("friendsSync", new JSONObject().put("codes", arr), args -> {
                if (args.length == 0 || !(args[0] instanceof JSONObject)) return;
                JSONArray list = ((JSONObject) args[0]).optJSONArray("friends");
                if (list == null) return;
                synchronized (FriendManager.this) {
                    for (Friend f : friends) f.online = false;
                    for (int i = 0; i < list.length(); i++) {
                        JSONObject o = list.optJSONObject(i);
                        if (o == null) continue;
                        Friend f = find(o.optString("code"));
                        if (f == null) continue;
                        f.name = o.optString("name", f.name);
                        f.online = o.optBoolean("online");
                        f.map = o.optString("mapName", "");
                        f.lastSeen = o.optLong("lastSeen", f.lastSeen);
                    }
                    persist();
                }
            });
        } catch (Exception e) { Log.e("FriendManager", "sync: " + e.getMessage()); }
    }

    public void syncIfStale(long ms) {
        if (System.currentTimeMillis() - lastSync > ms && MultiplayerManager.getInstance().isConnected()) sync();
    }

    /** Avisos vindos do servidor (pedido, aceite, remoção). Idempotente: o servidor pode reenviar. */
    public void onInbox(JSONArray items) {
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.optJSONObject(i);
            if (o == null) continue;
            String type = o.optString("type"), code = norm(o.optString("fromCode")), name = clean(o.optString("fromName"));
            if (code.length() != 8) continue;
            synchronized (this) {
                if (code.equals(myCode)) continue;
                switch (type) {
                    case "request":
                        if (find(code) == null && findRequest(code) == null) {
                            Request r = new Request(); r.code = code; r.name = name;
                            requests.add(r);
                            notice("👥 " + name + " quer ser seu amigo! Abra Amigos.");
                        }
                        break;
                    case "accepted":
                        requests.remove(findRequest(code));
                        if (find(code) == null) {
                            Friend f = new Friend(); f.code = code; f.name = name; f.online = true;
                            friends.add(f);
                            notice("🎉 " + name + " aceitou seu pedido de amizade!");
                        }
                        break;
                    case "removed":
                        Friend gone = find(code);
                        if (gone != null) { friends.remove(gone); notice(gone.name + " removeu você dos amigos."); }
                        break;
                    default: break;
                }
                persist();
            }
        }
        sync();
    }

    // ------------------------------------------------------------------ avisos para a HUD
    public void notice(String msg) { if (msg != null) notices.add(msg); }
    public String pollNotice() { return notices.poll(); }

    // ------------------------------------------------------------------ internos
    private Friend find(String code) {
        for (Friend f : friends) if (f.code.equals(code)) return f;
        return null;
    }

    private Request findRequest(String code) {
        for (Request r : requests) if (r.code.equals(code)) return r;
        return null;
    }

    private void persist() {
        if (prefs == null) return;
        try {
            JSONArray fa = new JSONArray();
            for (Friend f : friends) fa.put(new JSONObject().put("code", f.code).put("name", f.name).put("lastSeen", f.lastSeen));
            JSONArray ra = new JSONArray();
            for (Request r : requests) ra.put(new JSONObject().put("code", r.code).put("name", r.name));
            prefs.edit().putString("friends", fa.toString()).putString("requests", ra.toString()).apply();
        } catch (Exception e) { Log.e("FriendManager", "persist: " + e.getMessage()); }
    }

    public static String norm(String s) {
        return s == null ? "" : s.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private static String clean(String s) {
        if (s == null) return "Treinador";
        String t = s.replaceAll("[\\p{Cntrl}<>]", "").trim();
        if (t.length() > 16) t = t.substring(0, 16);
        return t.isEmpty() ? "Treinador" : t;
    }
}
