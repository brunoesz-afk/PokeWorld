package com.example.pokeworld.managers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Grupo de caça (até 4 jogadores): quem derrota um Pokémon divide o XP com os colegas que estão no mesmo mapa,
 * perto dele. O servidor guarda o grupo (ver server/index.js, eventos party*); aqui fica a cópia local para a tela.
 */
public class GroupManager {
    public static class Member { public String code = "", name = "Treinador"; }
    public static class Invite { public String fromCode = "", fromName = "Treinador"; public long at; }

    private static final GroupManager INSTANCE = new GroupManager();
    public static GroupManager getInstance() { return INSTANCE; }

    public static final int MAX_MEMBERS = 4;
    public static final long INVITE_TTL_MS = 60_000;
    /** Cada colega recebe esta fração do XP de quem derrotou o Pokémon. */
    public static final double SHARE = 0.6;
    /** Mais longe que isto (ou em outro mapa) o colega aparece com o botão TELEPORTAR. */
    public static final double FAR_PX = 1100;

    private final List<Member> members = new ArrayList<>();
    private String leader = "";
    private Invite pending;
    private int xpQueue = 0;
    private String xpFrom = "";

    // ------------------------------------------------------------------ leitura (tela)
    public synchronized boolean inGroup() { return members.size() > 1; }
    public synchronized List<Member> getMembers() { return new ArrayList<>(members); }
    public synchronized boolean isMember(String code) { return find(code) != null; }
    public synchronized boolean isLeader() { return leader.equals(FriendManager.getInstance().getMyCode()); }
    public synchronized boolean isFull() { return members.size() >= MAX_MEMBERS; }
    public synchronized String leaderCode() { return leader; }

    /** Convite que está esperando resposta (some depois de 60 s). */
    public synchronized Invite pendingInvite() {
        if (pending != null && System.currentTimeMillis() - pending.at > INVITE_TTL_MS) pending = null;
        return pending;
    }

    private Member find(String code) {
        for (Member m : members) if (m.code.equals(code)) return m;
        return null;
    }

    // ------------------------------------------------------------------ ações
    public void invite(final String code, final String name) {
        if (code == null || code.isEmpty()) { FriendManager.getInstance().notice("Esse jogador não tem código (está anônimo)."); return; }
        try {
            boolean sent = MultiplayerManager.getInstance().emitAck("partyInvite", new JSONObject().put("code", code), args -> {
                JSONObject res = args.length > 0 && args[0] instanceof JSONObject ? (JSONObject) args[0] : null;
                if (res != null && res.optBoolean("ok")) FriendManager.getInstance().notice("👥 Convite enviado para " + res.optString("name", name) + "!");
                else FriendManager.getInstance().notice(res != null ? res.optString("error", "Não foi possível convidar.") : "Sem resposta do servidor.");
            });
            if (!sent) FriendManager.getInstance().notice("Sem conexão com o servidor. Tente de novo em instantes.");
        } catch (Exception e) { FriendManager.getInstance().notice("Erro ao enviar o convite."); }
    }

    public void respond(final boolean accept) {
        final Invite inv;
        synchronized (this) { inv = pendingInvite(); pending = null; }
        if (inv == null) return;
        try {
            boolean sent = MultiplayerManager.getInstance().emitAck("partyRespond", new JSONObject().put("code", inv.fromCode).put("accept", accept), args -> {
                JSONObject res = args.length > 0 && args[0] instanceof JSONObject ? (JSONObject) args[0] : null;
                if (res == null || !res.optBoolean("ok")) FriendManager.getInstance().notice(res != null ? res.optString("error", "Convite expirado.") : "Sem resposta do servidor.");
                else if (!accept) FriendManager.getInstance().notice("Convite de " + inv.fromName + " recusado.");
            });
            if (!sent) FriendManager.getInstance().notice("Sem conexão com o servidor.");
        } catch (Exception e) { FriendManager.getInstance().notice("Erro ao responder o convite."); }
    }

    public void leave() {
        MultiplayerManager.getInstance().emitAck("partyLeave", new JSONObject(), args -> FriendManager.getInstance().notice("Você saiu do grupo."));
        synchronized (this) { members.clear(); leader = ""; }
    }

    /** Chamado quando você derrota um Pokémon: avisa o servidor para repassar aos colegas. */
    public void shareKillXp(int xp) {
        if (xp <= 0 || !inGroup()) return;
        try {
            MultiplayerManager.getInstance().emit("partyXp", new JSONObject().put("amount", xp));
        } catch (Exception ignored) {}
    }

    /** XP que os colegas ganharam e que agora é seu (já com a fração do grupo); zera ao ler. */
    public synchronized int pollSharedXp() { int x = xpQueue; xpQueue = 0; return x; }
    public synchronized String lastXpFrom() { return xpFrom; }

    // ------------------------------------------------------------------ eventos do servidor
    public synchronized void onUpdate(JSONObject d) {
        JSONObject p = d == null ? null : d.optJSONObject("party");
        List<Member> before = new ArrayList<>(members);
        members.clear();
        if (p == null) {
            leader = "";
            if (before.size() > 1) FriendManager.getInstance().notice("👥 O grupo foi desfeito.");
            return;
        }
        leader = p.optString("leader", "");
        JSONArray arr = p.optJSONArray("members");
        if (arr != null) for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o == null) continue;
            Member m = new Member();
            m.code = o.optString("code", ""); m.name = o.optString("name", "Treinador");
            members.add(m);
        }
        String me = FriendManager.getInstance().getMyCode();
        for (Member m : members) {
            boolean had = false;
            for (Member b : before) if (b.code.equals(m.code)) { had = true; break; }
            if (!had && !m.code.equals(me) && !before.isEmpty()) FriendManager.getInstance().notice("👥 " + m.name + " entrou no grupo!");
        }
        for (Member b : before) if (find(b.code) == null && !b.code.equals(me)) FriendManager.getInstance().notice("👥 " + b.name + " saiu do grupo.");
        if (before.size() < 2 && members.size() > 1) FriendManager.getInstance().notice("👥 Grupo formado! Vocês dividem o XP das caçadas.");
    }

    public synchronized void onInvite(JSONObject d) {
        if (d == null || inGroupLocked()) return;
        Invite i = new Invite();
        i.fromCode = d.optString("fromCode", ""); i.fromName = d.optString("fromName", "Treinador"); i.at = System.currentTimeMillis();
        if (i.fromCode.isEmpty()) return;
        pending = i;
        FriendManager.getInstance().notice("👥 " + i.fromName + " convidou você para um grupo!");
    }

    private boolean inGroupLocked() { return members.size() > 1; }

    public synchronized void onXp(JSONObject d) {
        if (d == null) return;
        int amount = Math.max(0, Math.min(5000, d.optInt("amount", 0)));
        xpQueue += (int) Math.round(amount * SHARE);
        xpFrom = d.optString("fromName", "colega");
    }

    public synchronized void clear() { members.clear(); leader = ""; pending = null; xpQueue = 0; }
}
