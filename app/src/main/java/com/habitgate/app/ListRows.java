package com.habitgate.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** トップ画面と通知からの入力画面で共用する、やること／減らすことの行UI。 */
public final class ListRows {
    private ListRows() {}

    /** やること1件の行を作って返す（親への追加・ドラッグ配線は呼び出し側が行う）。 */
    public static LinearLayout taskRow(Activity activity, HabitDb db, Models.Task t, Models.Cycle cycle, Runnable refresh) {
        LinearLayout row = Ui.horizontal(activity);

        LinearLayout col = Ui.vertical(activity);
        col.addView(Ui.body(activity, DateTools.formatShortDateWithWeekday(t.plannedDate) + "  " + t.title));
        if (t.dueAt > 0) {
            TextView dueLine = new TextView(activity);
            dueLine.setText("⏰ 期限: " + DateTools.formatDateTime(t.dueAt));
            dueLine.setTextSize(13);
            dueLine.setTextColor(t.dueAt < System.currentTimeMillis() ? Ui.DANGER : Ui.HINT);
            col.addView(dueLine);
        }
        if (!t.note.isEmpty()) {
            TextView noteLine = new TextView(activity);
            noteLine.setText("メモ: " + t.note);
            noteLine.setTextSize(13);
            noteLine.setTextColor(Ui.HINT);
            col.addView(noteLine);
        }
        row.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button carryOver = Ui.iconButton(activity, "→");
        carryOver.setContentDescription("翌日に繰り越す");
        carryOver.setOnClickListener(v -> confirmCarryOver(activity, db, t, cycle, refresh));
        row.addView(carryOver, new LinearLayout.LayoutParams(Ui.dp(activity, 48), LinearLayout.LayoutParams.WRAP_CONTENT));

        int tint = Ui.priorityTint(t.priority);
        Ui.tappableRow(row, tint);
        if (tint != 0) {
            row.setPadding(Ui.dp(activity, 10), Ui.dp(activity, 8), Ui.dp(activity, 6), Ui.dp(activity, 8));
        }
        row.setOnClickListener(v -> {
            Intent intent = new Intent(activity, TaskEntryActivity.class);
            intent.putExtra("task_id", t.id);
            activity.startActivity(intent);
        });
        return row;
    }

    /** 減らすこと1件の行を作って返す。 */
    public static LinearLayout reduceRow(Activity activity, HabitDb db, Models.ReduceItem item, String date) {
        LinearLayout row = Ui.vertical(activity);
        Ui.tappable(row);
        row.setOnClickListener(v -> {
            Intent intent = new Intent(activity, ReduceEntryActivity.class);
            intent.putExtra("item_id", item.id);
            activity.startActivity(intent);
        });

        TextView titleView = new TextView(activity);
        titleView.setText(item.title);
        titleView.setTextSize(15);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        titleView.setTextColor(Ui.TEXT);
        row.addView(titleView);

        int minutes;
        String status;
        if (item.hasLinkedApp()) {
            String appLabel = AppUsage.appLabel(activity, item.appPackage);
            if (AppUsage.hasPermission(activity)) {
                int manual = db.getReduceMinutesOn(item.title, date) - db.getReduceAutoMinutesOn(item.title, date);
                int measured = AppUsage.foregroundMinutesOn(activity, item.appPackage, date);
                minutes = measured + Math.max(0, manual);
                status = "📱 " + appLabel + " ・ 今日 " + DateTools.formatMinutes(minutes);
            } else {
                minutes = 0;
                status = "📱 " + appLabel + " ・ 計測には権限が必要";
            }
        } else {
            minutes = db.getReduceMinutesOn(item.title, date);
            status = "今日 " + DateTools.formatMinutes(minutes);
        }
        TextView statusView = new TextView(activity);
        statusView.setText(status);
        statusView.setTextSize(13);
        statusView.setTextColor(Ui.MUTED);
        row.addView(statusView);

        UsageBarView bar = new UsageBarView(activity);
        bar.setValues(minutes, item.gaugeMaxMinutes);
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 10));
        barLp.topMargin = Ui.dp(activity, 4);
        barLp.bottomMargin = Ui.dp(activity, 4);
        row.addView(bar, barLp);
        return row;
    }

    private static void confirmCarryOver(Activity activity, HabitDb db, Models.Task t, Models.Cycle cycle, Runnable refresh) {
        String nextDate = DateTools.nextDay(DateTools.maxDate(t.plannedDate, cycle.cycleDate));
        Ui.dialog(activity)
                .setTitle("翌日に繰り越しますか？")
                .setMessage("「" + t.title + "」を " + DateTools.formatShortDateWithWeekday(nextDate) + " に移動します。")
                .setPositiveButton("繰り越す", (dialog, which) -> {
                    db.carryOverDoTask(t.id, nextDate);
                    AutoSync.run(activity);
                    refresh.run();
                    Toast.makeText(activity, "繰り越しました", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }
}
