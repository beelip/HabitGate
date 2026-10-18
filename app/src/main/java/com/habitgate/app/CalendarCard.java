package com.habitgate.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** トップ画面の月間カレンダー。 */
public final class CalendarCard {
    private static final int SATURDAY_COLOR = 0xFF2563EB;
    private static final String[] WEEK_HEADERS = {"日", "月", "火", "水", "木", "金", "土"};

    public interface OnMonthChanged { void onChanged(String newMonthFirstDayIso); }

    private CalendarCard() {}

    /** カレンダーカード全体（ヘッダ+曜日行+6週グリッド）を構築して返す。 */
    public static LinearLayout build(Activity activity, HabitDb db, String monthFirstDayIso,
                                     OnMonthChanged onMonthChanged) {
        LinearLayout root = Ui.vertical(activity);
        LocalDate first = DateTools.parseOrToday(monthFirstDayIso).withDayOfMonth(1);
        String cycleDate = db.getCurrentCycle().cycleDate;

        // ヘッダ行
        LinearLayout header = Ui.horizontal(activity);
        Button prev = Ui.iconButton(activity, "‹");
        prev.setContentDescription("前の月");
        prev.setOnClickListener(v -> onMonthChanged.onChanged(first.minusMonths(1).format(DateTools.DATE)));
        header.addView(prev, new LinearLayout.LayoutParams(Ui.dp(activity, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView monthLabel = new TextView(activity);
        monthLabel.setText(first.getYear() + "年" + first.getMonthValue() + "月");
        monthLabel.setTextSize(16);
        monthLabel.setTypeface(Typeface.DEFAULT_BOLD);
        monthLabel.setTextColor(Ui.TEXT);
        monthLabel.setGravity(Gravity.CENTER);
        header.addView(monthLabel, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button next = Ui.iconButton(activity, "›");
        next.setContentDescription("次の月");
        next.setOnClickListener(v -> onMonthChanged.onChanged(first.plusMonths(1).format(DateTools.DATE)));
        header.addView(next, new LinearLayout.LayoutParams(Ui.dp(activity, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(header);

        // 曜日行
        LinearLayout weekRow = Ui.horizontal(activity);
        weekRow.setPadding(0, Ui.dp(activity, 8), 0, Ui.dp(activity, 4));
        for (int i = 0; i < 7; i++) {
            TextView tv = new TextView(activity);
            tv.setText(WEEK_HEADERS[i]);
            tv.setTextSize(12);
            tv.setGravity(Gravity.CENTER);
            tv.setTextColor(i == 0 ? Ui.DANGER : (i == 6 ? SATURDAY_COLOR : Ui.MUTED));
            weekRow.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        }
        root.addView(weekRow);

        // 予定日ごとにタスクを振り分ける
        Map<String, List<Models.Task>> byDate = new HashMap<>();
        for (Models.Task t : db.getActiveDoTasks("planned")) {
            if (t.plannedDate == null) continue;
            List<Models.Task> bucket = byDate.get(t.plannedDate);
            if (bucket == null) {
                bucket = new ArrayList<>();
                byDate.put(t.plannedDate, bucket);
            }
            bucket.add(t);
        }

        // グリッド（日曜始まり・6週）
        int offset = first.getDayOfWeek().getValue() % 7; // 日曜=0
        LocalDate cursor = first.minusDays(offset);
        for (int w = 0; w < 6; w++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 7; c++) {
                row.addView(buildCell(activity, cursor, first, cycleDate, byDate),
                        new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1));
                cursor = cursor.plusDays(1);
            }
            root.addView(row, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }
        return root;
    }

    private static LinearLayout buildCell(Activity activity, LocalDate date, LocalDate monthFirst,
                                          String cycleDate, Map<String, List<Models.Task>> byDate) {
        boolean inMonth = date.getYear() == monthFirst.getYear() && date.getMonthValue() == monthFirst.getMonthValue();
        String iso = date.format(DateTools.DATE);

        LinearLayout cell = Ui.vertical(activity);
        cell.setMinimumHeight(Ui.dp(activity, 56));
        int pad = Ui.dp(activity, 2);
        cell.setPadding(pad, pad, pad, pad);

        TextView num = new TextView(activity);
        num.setText(String.valueOf(date.getDayOfMonth()));
        num.setTextSize(11);
        num.setGravity(Gravity.CENTER);
        List<Models.Task> tasks = byDate.get(iso);

        if (!inMonth) {
            num.setTextColor(Ui.HINT);
        } else {
            int color;
            if (date.getDayOfWeek() == DayOfWeek.SUNDAY || JapaneseHolidays.isHoliday(date)) {
                color = Ui.DANGER;
            } else if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
                color = SATURDAY_COLOR;
            } else {
                color = Ui.TEXT;
            }
            num.setTextColor(color);
            if (iso.equals(cycleDate)) {
                num.setBackground(Ui.rounded(Ui.PRIMARY_SOFT, 0, Ui.dp(activity, 9)));
                num.setTextColor(Ui.PRIMARY);
                num.setTypeface(Typeface.DEFAULT_BOLD);
            }
        }
        int numPadH = Ui.dp(activity, 5);
        num.setPadding(numPadH, Ui.dp(activity, 1), numPadH, Ui.dp(activity, 1));
        LinearLayout.LayoutParams numLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        numLp.gravity = Gravity.CENTER_HORIZONTAL;
        cell.addView(num, numLp);

        if (inMonth && tasks != null) {
            int shown = Math.min(3, tasks.size());
            for (int i = 0; i < shown; i++) {
                cell.addView(taskLine(activity, tasks.get(i).title, Ui.TEXT));
            }
            if (tasks.size() > 3) {
                cell.addView(taskLine(activity, "+" + (tasks.size() - 3), Ui.HINT));
            }
        }

        Ui.tappable(cell);
        final List<Models.Task> dayTasks = tasks == null ? new ArrayList<>() : tasks;
        cell.setOnClickListener(v -> showDayDialog(activity, iso, dayTasks));
        return cell;
    }

    private static TextView taskLine(Activity activity, String text, int color) {
        TextView tv = new TextView(activity);
        tv.setText(text);
        tv.setTextSize(8);
        tv.setTextColor(color);
        tv.setSingleLine(true);
        tv.setEllipsize(TextUtils.TruncateAt.END);
        return tv;
    }

    private static void showDayDialog(Activity activity, String iso, List<Models.Task> tasks) {
        final AlertDialog[] holder = new AlertDialog[1];
        LinearLayout content = Ui.vertical(activity);
        int pad = Ui.dp(activity, 20);
        content.setPadding(pad, Ui.dp(activity, 8), pad, Ui.dp(activity, 8));

        if (tasks.isEmpty()) {
            content.addView(Ui.note(activity, "この日のやることはありません。"));
        } else {
            for (Models.Task t : tasks) {
                LinearLayout row = Ui.vertical(activity);
                int rowPad = Ui.dp(activity, 8);
                row.setPadding(0, rowPad, 0, rowPad);
                row.addView(Ui.body(activity, t.title));
                if (!t.note.isEmpty()) {
                    TextView memo = new TextView(activity);
                    memo.setText(t.note);
                    memo.setTextSize(13);
                    memo.setTextColor(Ui.HINT);
                    row.addView(memo);
                }
                Ui.tappable(row);
                final long taskId = t.id;
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(activity, TaskFormActivity.class);
                    intent.putExtra("task_id", taskId);
                    activity.startActivity(intent);
                    if (holder[0] != null) holder[0].dismiss();
                });
                content.addView(row);
            }
        }

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(content);
        LinearLayout wrapper = Ui.vertical(activity);
        wrapper.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                tasks.size() > 5 ? Ui.dp(activity, 400) : LinearLayout.LayoutParams.WRAP_CONTENT));

        holder[0] = Ui.dialog(activity)
                .setTitle(DateTools.formatShortDateWithWeekday(iso) + " のやること")
                .setView(wrapper)
                .setNegativeButton("閉じる", null)
                .create();
        holder[0].show();
    }
}
