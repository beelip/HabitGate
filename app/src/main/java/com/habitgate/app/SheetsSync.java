package com.habitgate.app;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SheetsSync {
    private SheetsSync() {}

    public static void syncUnsynced(Context context, boolean showToast) {
        String url = ReminderScheduler.webhookUrl(context);
        if (url == null || url.trim().isEmpty()) return;
        Context app = context.getApplicationContext();
        new Thread(() -> {
            HabitDb db = new HabitDb(app);
            List<Models.Record> records = db.getUnsyncedRecords();
            if (records.isEmpty()) {
                postToast(app, showToast ? "未同期データはありません" : null);
                return;
            }
            HttpURLConnection conn = null;
            try {
                JSONObject body = new JSONObject();
                body.put("source", "HabitGate");
                body.put("records", db.recordsToJson(records));
                byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);

                conn = (HttpURLConnection) new URL(url.trim()).openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }
                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    ArrayList<Long> ids = new ArrayList<>();
                    for (Models.Record r : records) ids.add(r.id);
                    db.markSynced(ids);
                    postToast(app, showToast ? "Sheets 同期完了: " + records.size() + "件" : null);
                } else {
                    postToast(app, showToast ? "Sheets 同期失敗: HTTP " + code : null);
                }
            } catch (Exception e) {
                postToast(app, showToast ? "Sheets 同期失敗: " + e.getMessage() : null);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private static void postToast(Context context, String message) {
        if (message == null || message.isEmpty()) return;
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }
}
