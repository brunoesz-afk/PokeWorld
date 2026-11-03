package com.example.pokeworld.managers;

import android.util.Log;

import com.example.pokeworld.entities.RemotePlayer;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.socket.client.IO;
import io.socket.client.Socket;

public class MultiplayerManager {
    private static MultiplayerManager instance;
    private Socket socket;
    private boolean isConnected = false;
    private String serverUrl = "https://pokeworld-wejp.onrender.com"; // URL do servidor Render
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

        try {
            IO.Options options = new IO.Options();
            options.forceNew = true;
            options.reconnection = true;

            socket = IO.socket(serverUrl, options);

            socket.on(Socket.EVENT_CONNECT, args -> {
                isConnected = true;
                Log.d("MultiplayerManager", "Conectado ao servidor multiplayer!");

                // Envia pacote de entrada no servidor
                try {
                    JSONObject playerInfo = new JSONObject();
                    playerInfo.put("name", playerName);
                    socket.emit("join", playerInfo);
                } catch (JSONException e) {
                    Log.e("MultiplayerManager", "Erro ao enviar join: " + e.getMessage());
                }
            });

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

            socket.connect();
        } catch (URISyntaxException e) {
            Log.e("MultiplayerManager", "URL de servidor invalida: " + e.getMessage());
        }
    }

    private void handlePlayerUpdate(JSONObject data) {
        try {
            String id = data.getString("id");
            if (socket != null && id.equals(socket.id())) return; // Ignora o próprio jogador

            String name = data.optString("name", "Treinador");
            double x = data.optDouble("x", 0);
            double y = data.optDouble("y", 0);
            String map = data.optString("mapName", "CITY");

            RemotePlayer rp = remotePlayers.get(id);
            if (rp == null) {
                rp = new RemotePlayer(id, name, x, y, map);
                remotePlayers.put(id, rp);
            }

            rp.setTargetPosition(x, y);
            rp.mapName = map;
            rp.direction = data.optInt("direction", 0);
            rp.animFrame = data.optInt("animFrame", 0);
            rp.spriteName = data.optString("spriteName", "personagem");
            rp.isShiny = data.optBoolean("isShiny", false);
        } catch (JSONException e) {
            Log.e("MultiplayerManager", "Erro ao processar dados do jogador: " + e.getMessage());
        }
    }

    public void sendPlayerState(String mapName, double x, double y, int direction, int animFrame, String spriteName, boolean isShiny) {
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
            data.put("animFrame", animFrame);
            data.put("spriteName", spriteName);
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
}
