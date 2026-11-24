package com.example.pokeworld.managers;

import android.graphics.Color;
import android.util.Log;

import com.example.pokeworld.entities.RemotePlayer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;

public class MultiplayerManager {
    private static MultiplayerManager instance;
    private Socket socket;
    private boolean isConnected = false;
    private String serverUrl = "https://pokeworld-1-nmva.onrender.com"; // Nova URL do servidor Render
    private final Map<String, RemotePlayer> remotePlayers = new ConcurrentHashMap<>();
    private long lastSendTime = 0;
    private static final long SEND_INTERVAL = 50; // Envia a cada 50ms (20 pacotes/seg)

    private MultiplayerManager() {}

    public static synchronized MultiplayerManager getInstance() {
        if (instance == null) {
            instance = new MultiplayerManager();
        }
        return instance;
    }

    public void setServerUrl(String url) {
        this.serverUrl = url;
    }

    public void connect(String playerName) {
        if (isConnected && socket != null) return;

        new Thread(() -> {
            try {
                IO.Options options = new IO.Options();
                options.forceNew = true;
                options.reconnection = true;
                options.timeout = 3000; // Timeout de 3s para evitar qualquer travamento

                socket = IO.socket(serverUrl, options);

                socket.on(Socket.EVENT_CONNECT, args -> {
                    isConnected = true;
                    Log.d("MultiplayerManager", "Conectado ao servidor multiplayer!");

                    // entra com nome + código de amigo + segredo (FriendManager)
                    socket.emit("join", FriendManager.getInstance().joinPayload(playerName));
                    FriendManager.getInstance().sync();
                });

                // pedidos / aceites / remoções de amigos; confirma o recebimento para o servidor apagar da caixa
                socket.on("friendInbox", args -> {
                    try {
                        if (args.length > 0 && args[0] instanceof JSONArray) {
                            FriendManager.getInstance().onInbox((JSONArray) args[0]);
                            if (args[args.length - 1] instanceof Ack) ((Ack) args[args.length - 1]).call();
                        }
                    } catch (Exception e) {
                        Log.e("MultiplayerManager", "Erro na caixa de amigos: " + e.getMessage());
                    }
                });

                socket.on("identityRejected", args ->
                        FriendManager.getInstance().notice("⚠ O servidor recusou o seu código de amigo (em uso por outro aparelho)."));

                socket.on(Socket.EVENT_DISCONNECT, args -> {
                    isConnected = false;
                    remotePlayers.clear();
                    Log.d("MultiplayerManager", "Desconectado do servidor.");
                });

                socket.on("playerJoined", args -> {
                    if (args.length > 0 && args[0] instanceof JSONObject) {
                        JSONObject data = (JSONObject) args[0];
                        handlePlayerUpdate(data);
                    }
                });

                socket.on("playerMoved", args -> {
                    if (args.length > 0 && args[0] instanceof JSONObject) {
                        JSONObject data = (JSONObject) args[0];
                        handlePlayerUpdate(data);
                    }
                });

                socket.on("playerLeft", args -> {
                    if (args.length > 0 && args[0] instanceof String) {
                        String id = (String) args[0];
                        remotePlayers.remove(id);
                    } else if (args.length > 0 && args[0] instanceof JSONObject) {
                        try {
                            String id = ((JSONObject) args[0]).getString("id");
                            remotePlayers.remove(id);
                        } catch (JSONException ignored) {}
                    }
                });

                socket.on("towerInvitation", args -> {
                    if (args.length > 0 && args[0] instanceof JSONObject) {
                        JSONObject data = (JSONObject) args[0];
                        String fromName = data.optString("fromName", "Amigo");
                        Log.d("Multiplayer", "🏰 Convite Co-op para a Torre recebido de " + fromName);
                    }
                });

                socket.on("towerAccepted", args -> {
                    if (args.length > 0 && args[0] instanceof JSONObject) {
                        JSONObject data = (JSONObject) args[0];
                        String friendName = data.optString("friendName", "Amigo");
                        Log.d("Multiplayer", "🏰 " + friendName + " aceitou entrar na Torre com você!");
                    }
                });

                socket.connect();
            } catch (Exception e) {
                Log.e("MultiplayerManager", "Erro ao conectar ao servidor (modo offline): " + e.getMessage());
            }
        }).start();
    }

    public void inviteToTower(String targetId, int floor) {
        if (!isConnected || socket == null) return;
        try {
            JSONObject data = new JSONObject();
            data.put("targetId", targetId);
            data.put("floor", floor);
            socket.emit("inviteTower", data);
        } catch (JSONException e) {
            Log.e("MultiplayerManager", "Erro ao convidar para torre: " + e.getMessage());
        }
    }

    public void acceptTowerInvite(String hostId) {
        if (!isConnected || socket == null) return;
        try {
            JSONObject data = new JSONObject();
            data.put("hostId", hostId);
            socket.emit("acceptTower", data);
        } catch (JSONException e) {
            Log.e("MultiplayerManager", "Erro ao aceitar torre: " + e.getMessage());
        }
    }

    /** Envia um evento e entrega a resposta do servidor ao Ack. Retorna false se estiver offline. */
    public boolean emitAck(String event, JSONObject data, Ack ack) {
        Socket s = socket;
        if (!isConnected || s == null) return false;
        s.emit(event, data, ack);
        return true;
    }

    public void rename(String name) {
        Socket s = socket;
        if (!isConnected || s == null) return;
        try { s.emit("rename", new JSONObject().put("name", name)); } catch (JSONException ignored) {}
    }

    private void handlePlayerUpdate(JSONObject data) {
        try {
            String id = data.getString("id");
            if (socket != null && id.equals(socket.id())) return; // Ignora o próprio jogador

            String name = data.optString("name", "Treinador");
            double x = data.optDouble("x", 0);
            double y = data.optDouble("y", 0);
            String map = data.optString("mapName", "CITY");

            double petX = data.optDouble("petX", x - 45);
            double petY = data.optDouble("petY", y);

            RemotePlayer rp = remotePlayers.get(id);
            if (rp == null) {
                rp = new RemotePlayer(id, name, x, y, map);
                remotePlayers.put(id, rp);
            }

            rp.setTargetPosition(x, y, petX, petY);
            rp.mapName = map;
            rp.name = data.optString("name", rp.name);
            rp.code = data.optString("code", "");
            rp.direction = data.optInt("direction", 0);
            rp.frameIndex = data.optInt("frameIndex", 1);
            rp.skinColor = data.optInt("skinColor", Color.WHITE);
            rp.isMounted = data.optBoolean("isMounted", false);

            rp.petSpecies = data.optString("petSpecies", "");
            rp.petDirection = data.optInt("petDirection", 0);
            rp.petFrameIndex = data.optInt("petFrameIndex", 1);
            rp.isShiny = data.optBoolean("isShiny", false);
        } catch (JSONException e) {
            Log.e("MultiplayerManager", "Erro ao processar dados do jogador: " + e.getMessage());
        }
    }

    public void sendPlayerState(String mapName, double x, double y, int direction, int frameIndex, int skinColor, boolean isMounted,
                                String petSpecies, double petX, double petY, int petDirection, int petFrameIndex, boolean isShiny) {
        if (!isConnected || socket == null) return;

        long now = System.currentTimeMillis();
        if (now - lastSendTime < SEND_INTERVAL) return;
        lastSendTime = now;

        try {
            JSONObject data = new JSONObject();
            data.put("mapName", mapName);
            data.put("x", x);
            data.put("y", y);
            data.put("direction", direction);
            data.put("frameIndex", frameIndex);
            data.put("skinColor", skinColor);
            data.put("isMounted", isMounted);

            data.put("petSpecies", petSpecies != null ? petSpecies : "");
            data.put("petX", petX);
            data.put("petY", petY);
            data.put("petDirection", petDirection);
            data.put("petFrameIndex", petFrameIndex);
            data.put("isShiny", isShiny);

            socket.emit("move", data);
        } catch (JSONException e) {
            Log.e("MultiplayerManager", "Erro ao enviar estado do jogador: " + e.getMessage());
        }
    }

    public Collection<RemotePlayer> getRemotePlayers() {
        return remotePlayers.values();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void disconnect() {
        if (socket != null) {
            socket.disconnect();
            socket.close();
            socket = null;
        }
        isConnected = false;
        remotePlayers.clear();
    }

    public void checkMapForMultiplayer(String mapName) {
        if (mapName.equals("CITY") || mapName.equals("HOME")) {
            if (!isConnected) {
                connect("Treinador");
            }
        }
    }
}
