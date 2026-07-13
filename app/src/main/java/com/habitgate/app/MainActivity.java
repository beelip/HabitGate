package com.habitgate.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ThemedActivity {
    private HabitDb db;
    private LinearLayout doList;
    private LinearLayout reduceList;
    private TextView cycleDateText;
    private TextView cycleStartText;
    private Button sortChip;
    private String sortMode;
    private final List<Long> doOrder = new ArrayList<>();
    private final List<Long> reduceOrder = new ArrayList<>();
    private boolean todayCollapsed = false;
    private boolean laterCollapsed = false;

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
        LinearLayout doHeaderRow = Ui.horizontal(this);
        doHeaderRow.addView(Ui.section(this, "やること"), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        sortChip = Ui.chip(this, "⇅ 優先順位");
        sortChip.setOnClickListener(v -> openSortDialog());
        doHeaderRow.addView(sortChip, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(doHeaderRow);
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

    private void openSortDialog() {
        Ui.dialog(this)
                .setTitle("並び順")
                .setItems(new String[]{"優先順位（手動並べ替え）", "優先度", "期限", "作成日"}, (dialog, which) -> {
                    String[] values = {"manual", "priority", "due", "created"};
                    ReminderScheduler.prefs(this).edit().putString("do_sort_mode", values[which]).apply();
                    refreshLists();
                })
                .show();
    }

    private String sortChipLabel(String mode) {
        if ("priority".equals(mode)) return "⇅ 優先度";
        if ("due".equals(mode)) return "⇅ 期限";
        if ("created".equals(mode)) return "⇅ 作成日";
        return "⇅ 優先順位";
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
        sortMode = ReminderScheduler.prefs(this).getString("do_sort_mode", "manual");
        if (sortChip != null) sortChip.setText(sortChipLabel(sortMode));

        Models.Cycle cycle = db.getCurrentCycle();
        cycleDateText.setText("対象日: " + DateTools.formatDisplayDate(cycle.cycleDate));
        cycleStartText.setText("開始: " + DateTools.formatDateTime(cycle.startAt));

        doList.removeAllViews();
        doOrder.clear();
        List<Models.Task> tasks = db.getActiveDoTasks(sortMode);
        for (Models.Task t : tasks) doOrder.add(t.id);

        if (tasks.isEmpty()) {
            doList.addView(Ui.note(this, "やることはありません。「タスク編集」から追加できます。"));
        } else if ("due".equals(sortMode)) {
            String todayRef = cycle.cycleDate;
            List<Models.Task> todayGroup = new ArrayList<>();
            List<Models.Task> laterGroup = new ArrayList<>();
            for (Models.Task t : tasks) {
                String dueDate = DateTools.dateOfMillis(t.dueAt);
                if (t.dueAt > 0 && !dueDate.isEmpty() && dueDate.compareTo(todayRef) <= 0) {
                    todayGroup.add(t);
                } else {
                    laterGroup.add(t);
                }
            }

            TextView todayHeader = buildGroupHeader("今日やること", todayGroup.size(), todayCollapsed);
            todayHeader.setOnClickListener(v -> {
                todayCollapsed = !todayCollapsed;
                refreshLists();
            });
            doList.addView(todayHeader);
            if (!todayCollapsed) {
                for (Models.Task t : todayGroup) addDoRow(doList, t, cycle);
            }

            TextView laterHeader = buildGroupHeader("明日以降やること", laterGroup.size(), laterCollapsed);
            laterHeader.setOnClickListener(v -> {
                laterCollapsed = !laterCollapsed;
                refreshLists();
            });
            doList.addView(laterHeader);
            if (!laterCollapsed) {
                for (Models.Task t : laterGroup) addDoRow(doList, t, cycle);
            }
        } else {
            for (Models.Task t : tasks) addDoRow(doList, t, cycle);
        }

        reduceList.removeAllViews();
        reduceOrder.clear();
        List<Models.ReduceItem> items = db.getActiveReduceItems();
        for (Models.ReduceItem item : items) reduceOrder.add(item.id);
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
                row.setOnLongClickListener(v -> {
                    v.startDragAndDrop(null, new View.DragShadowBuilder(v), "reduce:" + item.id, 0);
                    return true;
                });
                row.setOnDragListener(this::onRowDrag);
                row.setTag("reduce:" + item.id);

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
                        int manual = db.getReduceMinutesOn(item.title, cycle.cycleDate) - db.getReduceAutoMinutesOn(item.title, cycle.cycleDate);
                        int measured = AppUsage.foregroundMinutesOn(this, item.appPackage, cycle.cycleDate);
                        minutes = measured + Math.max(0, manual);
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

    private TextView buildGroupHeader(String label, int count, boolean collapsed) {
        TextView header = new TextView(this);
        header.setText(label + "（" + count + "）" + (collapsed ? "▸" : "▾"));
        header.setTextSize(15);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(Ui.TEXT);
        int padV = Ui.dp(this, 8);
        header.setPadding(0, padV, 0, padV);
        Ui.tappable(header);
        return header;
    }

    private void addDoRow(LinearLayout container, Models.Task t, Models.Cycle cycle) {
        Ui.addDivider(this, container);
        LinearLayout row = Ui.horizontal(this);

        LinearLayout col = Ui.vertical(this);
        col.addView(Ui.body(this, DateTools.formatShortDateWithWeekday(t.plannedDate) + "  " + t.title));
        if (t.dueAt > 0) {
            TextView dueLine = new TextView(this);
            dueLine.setText("⏰ 期限: " + DateTools.formatDateTime(t.dueAt));
            dueLine.setTextSize(13);
            dueLine.setTextColor(t.dueAt < System.currentTimeMillis() ? Ui.DANGER : Ui.HINT);
            col.addView(dueLine);
        }
        if (!t.note.isEmpty()) {
            TextView noteLine = new TextView(this);
            noteLine.setText("メモ: " + t.note);
            noteLine.setTextSize(13);
            noteLine.setTextColor(Ui.HINT);
            col.addView(noteLine);
        }
        row.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Button carryOver = Ui.iconButton(this, "→");
        carryOver.setContentDescription("翌日に繰り越す");
        carryOver.setOnClickListener(v -> confirmCarryOver(t, cycle));
        row.addView(carryOver, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));

        int tint = Ui.priorityTint(t.priority);
        Ui.tappableRow(row, tint);
        if (tint != 0) {
            row.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8));
        }
        row.setOnClickListener(v -> {
            Intent intent = new Intent(this, TaskEntryActivity.class);
            intent.putExtra("task_id", t.id);
            startActivity(intent);
        });
        row.setOnLongClickListener(v -> {
            if (!"manual".equals(sortMode)) {
                Toast.makeText(this, "並べ替えは「優先順位」ソート中のみ使えます", Toast.LENGTH_SHORT).show();
                return true;
            }
            v.startDragAndDrop(null, new View.DragShadowBuilder(v), "do:" + t.id, 0);
            return true;
        });
        row.setOnDragListener(this::onRowDrag);
        row.setTag("do:" + t.id);

        container.addView(row);
    }

    private boolean onRowDrag(View v, DragEvent event) {
        Object local = event.getLocalState();
        String dragged = local instanceof String ? (String) local : "";
        String target = v.getTag() instanceof String ? (String) v.getTag() : "";
        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
                return !dragged.isEmpty() && !target.isEmpty()
                        && dragged.substring(0, dragged.indexOf(':')).equals(target.substring(0, target.indexOf(':')));
            case DragEvent.ACTION_DRAG_ENTERED:
                v.setAlpha(0.4f);
                return true;
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DRAG_ENDED:
                v.setAlpha(1f);
                return true;
            case DragEvent.ACTION_DROP:
                v.setAlpha(1f);
                handleDrop(dragged, target);
                return true;
            default:
                return true;
        }
    }

    private void handleDrop(String dragged, String target) {
        if (dragged.isEmpty() || target.isEmpty() || dragged.equals(target)) return;
        boolean isDo = dragged.startsWith("do:");
        List<Long> order = isDo ? doOrder : reduceOrder;
        long draggedId = Long.parseLong(dragged.substring(dragged.indexOf(':') + 1));
        long targetId = Long.parseLong(target.substring(target.indexOf(':') + 1));
        int from = order.indexOf(draggedId);
        int to = order.indexOf(targetId);
        if (from < 0 || to < 0) return;
        List<Long> next = new ArrayList<>(order);
        next.remove(Long.valueOf(draggedId));
        int insertAt = next.indexOf(targetId);
        if (from < to) insertAt += 1;
        if (insertAt < 0) insertAt = next.size();
        if (insertAt > next.size()) insertAt = next.size();
        next.add(insertAt, draggedId);
        if (isDo) db.saveDoTaskOrder(next); else db.saveReduceItemOrder(next);
        AutoSync.run(this);
        refreshLists();
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
