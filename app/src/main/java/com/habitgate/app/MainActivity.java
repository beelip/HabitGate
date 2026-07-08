package com.habitgate.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends ThemedActivity {
    private HabitDb db;
    private LinearLayout doList;
    private LinearLayout reduceList;
    private TextView cycleDateText;
    private TextView cycleStartText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        ReminderScheduler.createNotificationChannel(this);
        requestNotificationPermissionIfNeeded();
        ReminderScheduler.scheduleNext(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        CycleAutoCloser.closeIfDueAndReschedule(this);
        refreshLists();
    }

    private void buildUi() {
        LinearLayout root = Ui.screen(this);

        root.addView(Ui.title(this, "HabitGate"));

        // 現在のサイクル + 主要アクション
        LinearLayout cycleCard = Ui.card(this, root);
        cycleDateText = new TextView(this);
        cycleDateText.setTextSize(19);
        cycleDateText.setTypeface(Typeface.DEFAULT_BOLD);
        cycleDateText.setTextColor(Ui.TEXT);
        cycleCard.addView(cycleDateText);
        cycleStartText = Ui.note(this, "");
        cycleCard.addView(cycleStartText);

        Button editTasks = Ui.primaryButton(this, "タスク編集");
        editTasks.setOnClickListener(v -> startActivity(new Intent(this, TaskEditActivity.class)));
        cycleCard.addView(editTasks);
        Ui.space(this, cycleCard, 8);

        Button closeDay = Ui.tonalButton(this, "一日を終えて次の日へ");
        closeDay.setOnClickListener(v -> confirmCloseCurrentCycle());
        cycleCard.addView(closeDay);

        LinearLayout navRow = Ui.horizontal(this);
        Button stats = Ui.button(this, "📊 集計");
        stats.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        Button settings = Ui.button(this, "⚙ 設定");
        settings.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        LinearLayout.LayoutParams half = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        half.rightMargin = Ui.dp(this, 8);
        navRow.addView(stats, half);
        navRow.addView(settings, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(navRow);

        // やること
        root.addView(Ui.section(this, "やること"));
        LinearLayout doCard = Ui.card(this, root);
        doList = Ui.vertical(this);
        doCard.addView(doList);

        // 減らすこと
        root.addView(Ui.section(this, "減らすこと"));
        LinearLayout reduceCard = Ui.card(this, root);
        reduceList = Ui.vertical(this);
        reduceCard.addView(reduceList);

        refreshLists();
    }

    private void confirmCloseCurrentCycle() {
        Models.Cycle cycle = db.getCurrentCycle();
        Ui.dialog(this)
                .setTitle("一日を終えますか？")
                .setMessage(DateTools.formatDisplayDate(cycle.cycleDate) + " を終了して次の日へ移ります。未完了のやることは繰り越されます。")
                .setPositiveButton("終了する", (dialog, which) -> closeCurrentCycle())
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void closeCurrentCycle() {
        Models.Cycle current = db.getCurrentCycle();
        int auto = AppUsage.autoRecordLinkedApps(this, db, current.cycleDate);
        Models.Cycle next = db.endCurrentCycleAndStartNext();
        ReminderScheduler.scheduleNext(this);
        AutoSync.run(this);
        refreshLists();
        String autoMessage = auto > 0 ? " / 自動計測" + auto + "件" : "";
        Toast.makeText(this, "一日を終了しました。次の対象日: " + DateTools.formatDisplayDate(next.cycleDate) + autoMessage, Toast.LENGTH_LONG).show();
    }

    private void refreshLists() {
        if (doList == null || reduceList == null) return;
        Models.Cycle cycle = db.getCurrentCycle();
        cycleDateText.setText("対象日: " + DateTools.formatDisplayDate(cycle.cycleDate));
        cycleStartText.setText("開始: " + DateTools.formatDateTime(cycle.startAt));

        doList.removeAllViews();
        List<Models.Task> tasks = db.getActiveDoTasks();
        if (tasks.isEmpty()) {
            doList.addView(Ui.note(this, "やることはありません。「タスク編集」から追加できます。"));
        } else {
            for (Models.Task t : tasks) {
                Ui.addDivider(this, doList);
                LinearLayout row = Ui.horizontal(this);
                Ui.tappable(row);
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(this, TaskEntryActivity.class);
                    intent.putExtra("task_id", t.id);
                    startActivity(intent);
                });
                String text = DateTools.formatShortDateWithWeekday(t.plannedDate) + "  " + t.title;
                if (!t.note.isEmpty()) text += "\nメモ: " + t.note;
                row.addView(Ui.body(this, text), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                Button carryOver = Ui.iconButton(this, "→");
                carryOver.setContentDescription("翌日に繰り越す");
                carryOver.setOnClickListener(v -> confirmCarryOver(t, cycle));
                row.addView(carryOver, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
                doList.addView(row);
            }
        }

        reduceList.removeAllViews();
        List<Models.ReduceItem> items = db.getActiveReduceItems();
        if (items.isEmpty()) {
            reduceList.addView(Ui.note(this, "減らすことはありません。「タスク編集」から追加できます。"));
        } else {
            for (Models.ReduceItem item : items) {
                Ui.addDivider(this, reduceList);
                LinearLayout row = Ui.vertical(this);
                Ui.tappable(row);
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ReduceEntryActivity.class);
                    intent.putExtra("item_id", item.id);
                    startActivity(intent);
                });

                TextView titleView = new TextView(this);
                titleView.setText(item.title);
                titleView.setTextSize(15);
                titleView.setTypeface(Typeface.DEFAULT_BOLD);
                titleView.setTextColor(Ui.TEXT);
                row.addView(titleView);

                int minutes;
                String status;
                if (item.hasLinkedApp()) {
                    String appLabel = AppUsage.appLabel(this, item.appPackage);
                    if (AppUsage.hasPermission(this)) {
                        minutes = AppUsage.foregroundMinutesOn(this, item.appPackage, cycle.cycleDate);
                        status = "📱 " + appLabel + " ・ 今日 " + DateTools.formatMinutes(minutes);
                    } else {
                        minutes = 0;
                        status = "📱 " + appLabel + " ・ 計測には権限が必要";
                    }
                } else {
                    minutes = db.getReduceMinutesOn(item.title, cycle.cycleDate);
                    status = "今日 " + DateTools.formatMinutes(minutes);
                }
                TextView statusView = new TextView(this);
                statusView.setText(status);
                statusView.setTextSize(13);
                statusView.setTextColor(Ui.MUTED);
                row.addView(statusView);

                UsageBarView bar = new UsageBarView(this);
                bar.setValues(minutes, item.gaugeMaxMinutes);
                LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 10));
                barLp.topMargin = Ui.dp(this, 4);
                barLp.bottomMargin = Ui.dp(this, 4);
                row.addView(bar, barLp);

                reduceList.addView(row);
            }
        }
    }

    private void confirmCarryOver(Models.Task t, Models.Cycle cycle) {
        String nextDate = DateTools.nextDay(DateTools.maxDate(t.plannedDate, cycle.cycleDate));
        Ui.dialog(this)
                .setTitle("翌日に繰り越しますか？")
                .setMessage("「" + t.title + "」を " + DateTools.formatShortDateWithWeekday(nextDate) + " に移動します。")
                .setPositiveButton("繰り越す", (dialog, which) -> {
                    db.carryOverDoTask(t.id, nextDate);
                    AutoSync.run(this);
                    refreshLists();
                    Toast.makeText(this, "繰り越しました", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }
}
