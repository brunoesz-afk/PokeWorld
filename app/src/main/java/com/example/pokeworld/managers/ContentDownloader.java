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

    private ContentDownloader() {}

    public static synchronized ContentDownloader getInstance() {
        if (instance == null) {
            instance = new ContentDownloader();
        }
        return instance;
    }

    public void syncContentSilently(Context context, ProgressCallback callback, Runnable onComplete) {
        new Thread(() -> {
            try {
                if (callback != null) callback.onProgress("Verificando atualizações de conteúdo...", 40);

                URL url = new URL(MANIFEST_URL + "?t=" + System.currentTimeMillis());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000); // Timeout rápido de 2s para não travar sem internet
                conn.setReadTimeout(2000);
                conn.setUseCaches(false);

                if (conn.getResponseCode() == 200) {
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
                            
                            if (callback != null) {
                                callback.onProgress("Baixando arquivo (" + (i + 1) + "/" + files.length() + ")...", 50 + (i * 20 / files.length()));
                            }
                            downloadFileIfNeeded(context, fileName, fileUrl);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("ContentDownloader", "Erro ao sincronizar conteúdo: " + e.getMessage());
            } finally {
                if (callback != null) callback.onProgress("Carregamento concluído (100%)", 100);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }).start();
    }

    private void downloadFileIfNeeded(Context context, String fileName, String fileUrl) {
        try {
            File targetFile = new File(context.getFilesDir(), fileName);
            URL url = new URL(fileUrl + "?t=" + System.currentTimeMillis());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
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
