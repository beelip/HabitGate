package com.habitgate.app;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** 内閣府の祝日CSVを取得して holidays テーブルを更新する。 */
public final class HolidaySync {
    public interface Callback { void onResult(boolean ok, int count, String message); }

    public static final String SOURCE_URL = "https://www8.cao.go.jp/chosei/shukujitsu/syukujitsu.csv";
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}/\\d{1,2}/\\d{1,2}$");
    private static final int MIN_ROWS = 500;

    private HolidaySync() {}

    /** ワーカースレッドで取得・保存する。callback もワーカースレッドから呼ばれる。 */
    public static void sync(Context context, Callback callback) {
        final Context appContext = context.getApplicationContext();
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(SOURCE_URL).openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "HabitGate");
                int status = conn.getResponseCode();
                if (status != HttpURLConnection.HTTP_OK) {
                    callback.onResult(false, 0, "HTTP " + status);
                    return;
                }
                List<String[]> rows = new ArrayList<>();
                try (InputStream is = conn.getInputStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, "Shift_JIS"))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(",", 2);
                        String first = parts[0].trim();
                        if (!DATE_PATTERN.matcher(first).matches()) continue;
                        String[] ymd = first.split("/");
                        LocalDate date = LocalDate.of(
                                Integer.parseInt(ymd[0]), Integer.parseInt(ymd[1]), Integer.parseInt(ymd[2]));
                        String name = parts.length > 1 ? parts[1].trim() : "";
                        rows.add(new String[]{date.format(DateTools.DATE), name});
                    }
                }
                if (rows.size() < MIN_ROWS) {
                    callback.onResult(false, 0, "取得データが不完全です");
                    return;
                }
                new HabitDb(appContext).replaceHolidays(rows);
                ReminderScheduler.prefs(appContext).edit()
                        .putLong("holiday_last_sync", System.currentTimeMillis())
                        .putInt("holiday_count", rows.size())
                        .apply();
                JapaneseHolidays.invalidateCache();
                callback.onResult(true, rows.size(), "");
            } catch (Exception e) {
                String msg = e.getMessage();
                callback.onResult(false, 0, msg == null ? e.getClass().getSimpleName() : msg);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }
}
