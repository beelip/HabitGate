package com.habitgate.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.time.ZoneId;

/** 設定時刻を過ぎたサイクルを自動で終了し、次回のアラームを再設定する。 */
public final class CycleAutoCloser {
    private static final int MAX_ITERATIONS = 400;

    private CycleAutoCloser() {}

    public static int closeIfDueAndReschedule(Context context) {
        HabitDb db = new HabitDb(context);
        int closed = 0;
        int[] time = DateTools.parseTime(ReminderScheduler.autoCloseTime(context), 5, 0);
        for (int i = 0; i < MAX_ITERATIONS; i++) {
            Models.Cycle cycle = db.getCurrentCycle();
            long deadline = deadlineMillis(cycle.cycleDate, time[0], time[1]);
            if (System.currentTimeMillis() < deadline) break;
            AppUsage.autoRecordLinkedApps(context, db, cycle.cycleDate);
            db.endCurrentCycleAndStartNext();
            closed++;
        }
        if (closed > 0) {
            AutoSync.run(context);
            ReminderScheduler.scheduleNext(context);
        }
        scheduleAlarm(context, db, time[0], time[1]);
        return closed;
    }

    private static long deadlineMillis(String cycleDate, int hour, int minute) {
        return DateTools.parseOrToday(cycleDate).plusDays(1).atTime(hour, minute)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static void scheduleAlarm(Context context, HabitDb db, int hour, int minute) {
        Models.Cycle cycle = db.getCurrentCycle();
        long deadline = deadlineMillis(cycle.cycleDate, hour, minute);
        Intent intent = new Intent(context, AutoCloseReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                ReminderScheduler.REQUEST_AUTO_CLOSE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pi);
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline, pi);
        }
    }
}
