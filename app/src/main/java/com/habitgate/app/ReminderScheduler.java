package com.habitgate.app;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class ReminderScheduler {
    public static final String PREFS = "friction_habit_settings";
    public static final String KEY_REMINDER_TIME = "reminder_time";
    public static final String KEY_WEBHOOK_URL = "webhook_url";
    public static final String CHANNEL_ID = "daily_check_in";
    public static final int REQUEST_ALARM = 7710;

    private ReminderScheduler() {}

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String reminderTime(Context context) {
        return prefs(context).getString(KEY_REMINDER_TIME, "22:30");
    }

    public static void saveReminderTime(Context context, int hour, int minute) {
        prefs(context).edit().putString(KEY_REMINDER_TIME, DateTools.formatTime(hour, minute)).apply();
        scheduleNext(context);
    }

    public static String webhookUrl(Context context) {
        return prefs(context).getString(KEY_WEBHOOK_URL, "");
    }

    public static void saveWebhookUrl(Context context, String url) {
        prefs(context).edit().putString(KEY_WEBHOOK_URL, url == null ? "" : url.trim()).apply();
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "毎日の振り返り",
                    NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("指定時刻に『今日は何をした？』を出します");
            nm.createNotificationChannel(ch);
        }
    }

    public static void scheduleNext(Context context) {
        createNotificationChannel(context);
        String hhmm = reminderTime(context);
        int[] parts = DateTools.parseTime(hhmm, 22, 30);
        LocalDateTime target = LocalDate.now().atTime(parts[0], parts[1]);
        LocalDateTime now = LocalDateTime.now();
        if (!target.isAfter(now.plusMinutes(1))) {
            target = target.plusDays(1);
        }
        long triggerAt = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                REQUEST_ALARM,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static Intent exactAlarmSettingsIntent(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            return intent;
        }
        return new Intent(Settings.ACTION_SETTINGS);
    }
}
