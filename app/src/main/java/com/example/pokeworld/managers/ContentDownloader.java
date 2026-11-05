package com.example.pokeworld.managers;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ContentDownloader {
    private static ContentDownloader instance;
    private static final String MANIFEST_URL = "https://raw.githubusercontent.com/brunoesz-afk/PokeWorld/master/assets_manifest.json";

    public interface ProgressCallback {
        void onProgress(String status, int percent);
    }

    public interface ConnectionCallback {
        void onResult(boolean isOnline);
    }

    private ContentDownloader() {}

    public static synchronized ContentDownloader getInstance() {
        if (instance == null) {
            instance = new ContentDownloader();
        }
        return instance;
    }

    public void checkConnectionAndSync(Context context, ProgressCallback callback, ConnectionCallback listener) {
        new Thread(() -> {
            boolean isOnline = false;
            try {
                if (callback != null) callback.onProgress("Verificando conexão com a internet...", 30);

                URL url = new URL(MANIFEST_URL + "?t=" + System.currentTimeMillis());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000); // Aumentado para 6s para conexões móveis (4G/5G)
                conn.setReadTimeout(6000);
                conn.setUseCaches(false);

                if (conn.getResponseCode() == 200) {
                    isOnline = true;
                    if (callback != null) callback.onProgress("Conectado Online! Baixando dados...", 50);

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();

                    JSONObject manifest = new JSONObject(sb.toString());
                    JSONArray files = manifest.optJSONArray("files");

                    if (files != null && files.length() > 0) {
                        for (int i = 0; i < files.length(); i++) {
                            JSONObject item = files.getJSONObject(i);
                            String fileName = item.getString("name");
                            String fileUrl = item.getString("url");
                            downloadFileIfNeeded(context, fileName, fileUrl);
                        }
                    }
                }
            } catch (Exception e) {
                isOnline = false;
            } finally {
                if (listener != null) {
                    listener.onResult(isOnline);
                }
            }
        }).start();
    }

    private void downloadFileIfNeeded(Context context, String fileName, String fileUrl) {
        try {
            File targetFile = new File(context.getFilesDir(), fileName);
            URL url = new URL(fileUrl + "?t=" + System.currentTimeMillis());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setUseCaches(false);
            conn.connect();

            if (conn.getResponseCode() == 200) {
                int fileSize = conn.getContentLength();
                InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(targetFile);
                byte[] buffer = new byte[2048];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();
                Log.d("ContentDownloader", "Arquivo baixado: " + fileName + " (" + fileSize + " bytes)");
            }
        } catch (Exception e) {
            Log.e("ContentDownloader", "Erro ao baixar " + fileName + ": " + e.getMessage());
        }
    }
}
