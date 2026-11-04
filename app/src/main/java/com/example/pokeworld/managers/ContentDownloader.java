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

    private ContentDownloader() {}

    public static synchronized ContentDownloader getInstance() {
        if (instance == null) {
            instance = new ContentDownloader();
        }
        return instance;
    }

    public void syncContentSilently(Context context, Runnable onComplete) {
        new Thread(() -> {
            try {
                Log.d("ContentDownloader", "Buscando atualizações de conteúdo silenciosas...");
                URL url = new URL(MANIFEST_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(4000);
                conn.setReadTimeout(4000);

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

                    if (files != null) {
                        for (int i = 0; i < files.length(); i++) {
                            JSONObject item = files.getJSONObject(i);
                            String fileName = item.getString("name");
                            String fileUrl = item.getString("url");
                            downloadFileIfNeeded(context, fileName, fileUrl);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("ContentDownloader", "Erro ao sincronizar conteúdo: " + e.getMessage());
            } finally {
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        }).start();
    }

    private void downloadFileIfNeeded(Context context, String fileName, String fileUrl) {
        try {
            File targetFile = new File(context.getFilesDir(), fileName);
            URL url = new URL(fileUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.connect();

            if (conn.getResponseCode() == 200) {
                InputStream is = conn.getInputStream();
                FileOutputStream fos = new FileOutputStream(targetFile);
                byte[] buffer = new byte[2048];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                is.close();
                Log.d("ContentDownloader", "Arquivo baixado com sucesso: " + fileName);
            }
        } catch (Exception e) {
            Log.e("ContentDownloader", "Erro ao baixar " + fileName + ": " + e.getMessage());
        }
    }
}
