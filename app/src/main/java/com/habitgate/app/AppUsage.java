package com.habitgate.app;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Process;
import android.provider.Settings;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/**
 * 減らすこと項目に連携したアプリの使用時間を UsageStatsManager で計測する。
 * 計測には「使用状況へのアクセス」権限（設定画面からの手動許可）が必要。
 */
public final class AppUsage {
    public static final String AUTO_NOTE = "自動計測";

    private AppUsage() {}

    public static boolean hasPermission(Context context) {
        try {
            android.app.AppOpsManager appOps = (android.app.AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
            if (appOps == null) return false;
            int mode = appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(), context.getPackageName());
            if (mode == android.app.AppOpsManager.MODE_DEFAULT) {
                return context.checkSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS)
                        == PackageManager.PERMISSION_GRANTED;
            }
            return mode == android.app.AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    public static Intent usageAccessSettingsIntent() {
        return new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
    }

    /**
     * 指定日のそのアプリの前面使用時間（分）を返す。
     * 未来分・権限なしは 0 分。当日は現在時刻までを集計する。
     */
    public static int foregroundMinutesOn(Context context, String packageName, String isoDate) {
        if (packageName == null || packageName.isEmpty()) return 0;
        if (!hasPermission(context)) return 0;
        long from = DateTools.dayStartMillis(isoDate);
        long to = Math.min(DateTools.dayEndMillis(isoDate), System.currentTimeMillis());
        if (to <= from) return 0;
        return (int) (foregroundMillis(context, packageName, from, to) / 60000L);
    }

    private static long foregroundMillis(Context context, String packageName, long from, long to) {
        try {
            UsageStatsManager usm = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) return 0;
            UsageEvents events = usm.queryEvents(from, to);
            UsageEvents.Event event = new UsageEvents.Event();
            long total = 0;
            long resumedAt = -1;
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                if (!packageName.equals(event.getPackageName())) continue;
                int type = event.getEventType();
                boolean resumed = type == UsageEvents.Event.ACTIVITY_RESUMED;
                boolean paused = type == UsageEvents.Event.ACTIVITY_PAUSED
                        || type == UsageEvents.Event.ACTIVITY_STOPPED;
                if (resumed) {
                    if (resumedAt < 0) resumedAt = event.getTimeStamp();
                } else if (paused && resumedAt >= 0) {
                    total += Math.max(0, event.getTimeStamp() - resumedAt);
                    resumedAt = -1;
                }
            }
            // 期間終了時点でまだ前面にいる場合（当日はアプリ使用中に開いた場合など）
            if (resumedAt >= 0) total += Math.max(0, to - resumedAt);
            return total;
        } catch (Exception e) {
            return 0;
        }
    }

    /** ランチャーに表示されるアプリの一覧（自分自身を除き、名前順）。 */
    public static List<AppEntry> launchableApps(Context context) {
        ArrayList<AppEntry> list = new ArrayList<>();
        try {
            PackageManager pm = context.getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            HashSet<String> seen = new HashSet<>();
            for (ResolveInfo info : pm.queryIntentActivities(main, 0)) {
                if (info.activityInfo == null) continue;
                String pkg = info.activityInfo.packageName;
                if (pkg == null || pkg.equals(context.getPackageName()) || !seen.add(pkg)) continue;
                String label = String.valueOf(info.loadLabel(pm));
                list.add(new AppEntry(pkg, label.isEmpty() ? pkg : label));
            }
            Collator collator = Collator.getInstance(Locale.JAPAN);
            list.sort((a, b) -> collator.compare(a.label, b.label));
        } catch (Exception ignored) {
        }
        return list;
    }

    /** パッケージ名から表示名を引く。取得できなければパッケージ名を返す。 */
    public static String appLabel(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return "";
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            return String.valueOf(pm.getApplicationLabel(info));
        } catch (Exception e) {
            return packageName;
        }
    }

    /**
     * アプリ連携済みの減らすこと項目について、指定日の記録がまだ無ければ
     * 計測時間で自動記録する。追加した件数を返す。
     */
    public static int autoRecordLinkedApps(Context context, HabitDb db, String isoDate) {
        if (!hasPermission(context)) return 0;
        int added = 0;
        for (Models.ReduceItem item : db.getActiveReduceItems()) {
            if (!item.hasLinkedApp()) continue;
            if (db.hasRecordOn(HabitDb.CATEGORY_REDUCE, item.title, isoDate)) continue;
            int minutes = foregroundMinutesOn(context, item.appPackage, isoDate);
            if (minutes <= 0) continue;
            db.addRecord(HabitDb.CATEGORY_REDUCE, item.title,
                    AUTO_NOTE + ": " + appLabel(context, item.appPackage), minutes, isoDate);
            added++;
        }
        return added;
    }

    public static class AppEntry {
        public final String packageName;
        public final String label;

        public AppEntry(String packageName, String label) {
            this.packageName = packageName;
            this.label = label;
        }
    }
}
