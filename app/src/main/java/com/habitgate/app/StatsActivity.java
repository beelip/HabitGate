package com.habitgate.app;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsActivity extends ThemedActivity {
    private HabitDb db;
    private LinearLayout list;
    private BarChartView chart;
    private String mode = "week";
    private String selectedDate;
    private Button dayChip;
    private Button weekChip;
    private Button monthChip;
    private Button dateChip;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        selectedDate = db.getCurrentCycle().cycleDate;
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null || e2 == null) return false;
                float dx = e2.getX() - e1.getX();
                float dy = e2.getY() - e1.getY();
                if (Math.abs(dx) > Math.abs(dy) * 2 && Math.abs(dx) > Ui.dp(StatsActivity.this, 60)) {
                    swipeMode(dx < 0 ? 1 : -1);
                    return true;
                }
                return false;
            }
        });
        buildUi();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        return super.dispatchTouchEvent(ev);
    }

    private void swipeMode(int delta) {
        String[] order = {"day", "week", "month"};
        int index = indexOf(order, mode);
        int next = Math.max(0, Math.min(order.length - 1, index + delta));
        if (!order[next].equals(mode)) {
            mode = order[next];
            refresh();
        }
    }

    private int indexOf(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equals(value)) return i;
        }
        return 0;
    }

    private void buildUi() {
        LinearLayout root = Ui.screen(this);

        root.addView(Ui.title(this, "集計"));

        LinearLayout chips = Ui.horizontal(this);
        dayChip = Ui.chip(this, "今日");
        weekChip = Ui.chip(this, "今週");
        monthChip = Ui.chip(this, "今月");
        dateChip = Ui.chip(this, "📅");
        dateChip.setContentDescription("日付を選んで表示");
        dayChip.setOnClickListener(v -> { mode = "day"; refresh(); });
        weekChip.setOnClickListener(v -> { mode = "week"; refresh(); });
        monthChip.setOnClickListener(v -> { mode = "month"; refresh(); });
        dateChip.setOnClickListener(v -> openDatePicker());
        addChip(chips, dayChip, true);
        addChip(chips, weekChip, true);
        addChip(chips, monthChip, true);
        chips.addView(dateChip, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        root.addView(chips);
        Ui.space(this, root, 10);

        LinearLayout chartCard = Ui.card(this, root);
        chart = new BarChartView(this);
        chartCard.addView(chart, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 240)));
        chartCard.addView(Ui.note(this, "やることは積み上げ、減らすことは発生量として見ます。"));

        list = Ui.vertical(this);
        root.addView(list);
    }

    private void addChip(LinearLayout parent, Button chip, boolean withMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        if (withMargin) lp.rightMargin = Ui.dp(this, 6);
        parent.addView(chip, lp);
    }

    private void openDatePicker() {
        LocalDate initial = DateTools.parseOrToday(selectedDate);
        new DatePickerDialog(this, Ui.pickerTheme(), (view, year, month, dayOfMonth) -> {
            selectedDate = LocalDate.of(year, month + 1, dayOfMonth).format(DateTools.DATE);
            mode = "day";
            refresh();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private List<Models.DayTotal> expandDaily(String from, String to) {
        Map<String, Models.DayTotal> byDate = new HashMap<>();
        for (Models.DayTotal t : db.getDayTotals(from, to)) {
            byDate.put(t.date, t);
        }
        List<Models.DayTotal> result = new ArrayList<>();
        String date = from;
        int guard = 0;
        while (date.compareTo(to) <= 0 && guard < 62) {
            Models.DayTotal t = byDate.get(date);
            result.add(t != null ? t : new Models.DayTotal(date, 0, 0));
            date = DateTools.addDaysTo(date, 1);
            guard++;
        }
        return result;
    }

    private void refresh() {
        String todayRef = db.getCurrentCycle().cycleDate;
        dayChip.setText(selectedDate.equals(todayRef) ? "今日" : DateTools.formatShortDateWithWeekday(selectedDate));
        weekChip.setText(DateTools.sameWeek(selectedDate, todayRef) ? "今週" : "この週");
        monthChip.setText(DateTools.sameMonth(selectedDate, todayRef) ? "今月" : "この月");

        Ui.setChipSelected(dayChip, "day".equals(mode));
        Ui.setChipSelected(weekChip, "week".equals(mode));
        Ui.setChipSelected(monthChip, "month".equals(mode));

        String from;
        String to;
        String label;
        List<Models.DayTotal> chartTotals;
        List<String> chartLabels = null;

        if ("day".equals(mode)) {
            from = selectedDate;
            to = selectedDate;
            label = dayChip.getText().toString();
            chartTotals = expandDaily(from, to);
        } else if ("month".equals(mode)) {
            from = DateTools.startOfMonthOf(selectedDate);
            to = DateTools.endOfMonthOf(selectedDate);
            label = monthChip.getText().toString();

            LocalDate monthStart = DateTools.parseOrToday(from);
            int monthLength = monthStart.lengthOfMonth();
            int weeks = ((monthLength - 1) / 7) + 1;
            int[] doSums = new int[weeks];
            int[] reduceSums = new int[weeks];
            for (Models.DayTotal t : db.getDayTotals(from, to)) {
                int dayOfMonth = DateTools.parseOrToday(t.date).getDayOfMonth();
                int idx = (dayOfMonth - 1) / 7;
                if (idx >= 0 && idx < weeks) {
                    doSums[idx] += t.doMinutes;
                    reduceSums[idx] += t.reduceMinutes;
                }
            }
            List<Models.DayTotal> weekTotals = new ArrayList<>();
            List<String> weekLabels = new ArrayList<>();
            int totalDo = 0;
            int totalReduce = 0;
            for (int w = 0; w < weeks; w++) {
                String weekStart = DateTools.addDaysTo(from, w * 7);
                weekTotals.add(new Models.DayTotal(weekStart, doSums[w], reduceSums[w]));
                weekLabels.add((w + 1) + "週");
                totalDo += doSums[w];
                totalReduce += reduceSums[w];
            }
            weekTotals.add(new Models.DayTotal(to, totalDo, totalReduce));
            weekLabels.add("合計");
            chartTotals = weekTotals;
            chartLabels = weekLabels;
        } else {
            from = DateTools.startOfWeekOf(selectedDate);
            to = DateTools.endOfWeekOf(selectedDate);
            label = weekChip.getText().toString();
            chartTotals = expandDaily(from, to);
        }

        // アプリ連携の計測中（未保存）分をリアルタイムに反映する
        int liveExtra = 0;
        boolean periodIncludesToday = from.compareTo(todayRef) <= 0 && todayRef.compareTo(to) <= 0;
        if (periodIncludesToday) {
            liveExtra = AppUsage.liveUnsavedReduceMinutes(this, db, todayRef);
        }
        if (liveExtra > 0) {
            chartTotals = applyLiveExtraToChart(chartTotals, todayRef, liveExtra);
        }

        if (chartLabels == null) {
            chart.setTotals(chartTotals);
        } else {
            chart.setTotals(chartTotals, chartLabels);
        }

        List<Models.Record> records = db.getRecords(from, to);
        List<Models.Cycle> cycles = db.getCycles(from, to);
        List<Models.CompletedTask> completedTasks = db.getCompletedTaskTotals(from, to);
        list.removeAllViews();

        // 合計サマリー
        list.addView(Ui.section(this, label + "の合計"));
        LinearLayout summaryCard = Ui.card(this, list);
        LinearLayout summaryRow = Ui.horizontal(this);
        int doTotal = 0;
        int reduceTotal = 0;
        for (Models.Record r : records) {
            if (HabitDb.CATEGORY_DO.equals(r.category)) doTotal += r.durationMinutes;
            if (HabitDb.CATEGORY_REDUCE.equals(r.category)) reduceTotal += r.durationMinutes;
        }
        if (liveExtra > 0) {
            reduceTotal += liveExtra;
        }
        summaryRow.addView(summaryCell("やること", doTotal, Ui.GOOD),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        summaryRow.addView(summaryCell("減らすこと", reduceTotal, Ui.DANGER),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        summaryCard.addView(summaryRow);
        if (liveExtra > 0) {
            summaryCard.addView(Ui.note(this, "📱 計測中（未保存）の " + DateTools.formatMinutes(liveExtra) + " を含みます。一日の終了時に自動保存されます。"));
        }

        // 完了したタスク
        list.addView(Ui.section(this, "完了したタスク"));
        LinearLayout completedCard = Ui.card(this, list);
        if (completedTasks.isEmpty()) {
            completedCard.addView(Ui.note(this, "この期間に完了したタスクはありません。"));
        } else {
            for (Models.CompletedTask ct : completedTasks) {
                LinearLayout row = Ui.horizontal(this);
                TextView left = new TextView(this);
                left.setText(DateTools.formatShortDateWithWeekday(ct.completedDate) + "  " + ct.title);
                left.setTextSize(14);
                left.setTextColor(Ui.TEXT);
                row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                TextView right = new TextView(this);
                right.setText(DateTools.formatMinutes(ct.minutes));
                right.setTextSize(14);
                right.setTextColor(Ui.TEXT);
                right.setGravity(android.view.Gravity.END);
                row.addView(right, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                row.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
                completedCard.addView(row);
            }
        }

        // 実績一覧
        list.addView(Ui.section(this, label + "の記録"));
        LinearLayout recordsCard = Ui.card(this, list);
        if (records.isEmpty()) {
            recordsCard.addView(Ui.note(this, "この期間の実績記録はまだありません。"));
        } else {
            String currentDate = "";
            for (Models.Record r : records) {
                if (!currentDate.equals(r.actualDate)) {
                    currentDate = r.actualDate;
                    TextView date = new TextView(this);
                    date.setText(DateTools.formatShortDateWithWeekday(currentDate));
                    date.setTextSize(15);
                    date.setTypeface(Typeface.DEFAULT_BOLD);
                    date.setTextColor(Ui.TEXT);
                    date.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
                    recordsCard.addView(date);
                }
                boolean isDo = HabitDb.CATEGORY_DO.equals(r.category);
                LinearLayout row = Ui.horizontal(this);
                row.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
                TextView left = new TextView(this);
                String text = (isDo ? "✅ " : "⚠ ") + r.title;
                if (!r.note.isEmpty()) text += "\n　メモ: " + r.note;
                left.setText(text);
                left.setTextSize(14);
                left.setTextColor(Ui.TEXT);
                row.addView(left, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                TextView right = new TextView(this);
                right.setText(DateTools.formatMinutes(r.durationMinutes));
                right.setTextSize(14);
                right.setTextColor(Ui.TEXT);
                right.setGravity(android.view.Gravity.END);
                row.addView(right, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                Ui.tappable(row);
                long recordId = r.id;
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(StatsActivity.this, RecordEditActivity.class);
                    intent.putExtra("record_id", recordId);
                    startActivity(intent);
                });
                recordsCard.addView(row);
            }
        }

        // サイクル履歴
        list.addView(Ui.section(this, "サイクル履歴"));
        LinearLayout cyclesCard = Ui.card(this, list);
        if (cycles.isEmpty()) {
            cyclesCard.addView(Ui.note(this, "この期間のサイクル記録はまだありません。"));
        } else {
            for (Models.Cycle c : cycles) {
                TextView row = new TextView(this);
                String end = c.closed ? DateTools.formatDateTime(c.endAt) : "進行中";
                row.setText("・" + DateTools.formatDisplayDate(c.cycleDate) + "\n　開始: " + DateTools.formatDateTime(c.startAt) + " / 終了: " + end);
                row.setTextSize(14);
                row.setTextColor(Ui.TEXT);
                row.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
                cyclesCard.addView(row);
            }
        }

        SheetsSync.syncUnsynced(this, false);
    }

    /** チャート集計に、当日分の計測中（未保存）分を加算した新しいリストを返す。 */
    private List<Models.DayTotal> applyLiveExtraToChart(List<Models.DayTotal> totals, String todayRef, int liveExtra) {
        List<Models.DayTotal> result = new ArrayList<>();
        if ("month".equals(mode)) {
            int dayOfMonth = DateTools.parseOrToday(todayRef).getDayOfMonth();
            int weekIdx = (dayOfMonth - 1) / 7;
            int lastIdx = totals.size() - 1;
            for (int i = 0; i < totals.size(); i++) {
                Models.DayTotal t = totals.get(i);
                if (i == weekIdx || i == lastIdx) {
                    result.add(new Models.DayTotal(t.date, t.doMinutes, t.reduceMinutes + liveExtra));
                } else {
                    result.add(t);
                }
            }
        } else {
            for (Models.DayTotal t : totals) {
                if (t.date.equals(todayRef)) {
                    result.add(new Models.DayTotal(t.date, t.doMinutes, t.reduceMinutes + liveExtra));
                } else {
                    result.add(t);
                }
            }
        }
        return result;
    }

    private LinearLayout summaryCell(String label, int minutes, int color) {
        LinearLayout cell = Ui.vertical(this);
        TextView caption = new TextView(this);
        caption.setText(label);
        caption.setTextSize(13);
        caption.setTextColor(Ui.MUTED);
        cell.addView(caption);
        TextView value = new TextView(this);
        value.setText(DateTools.formatMinutes(minutes));
        value.setTextSize(19);
        value.setTypeface(Typeface.DEFAULT_BOLD);
        value.setTextColor(color);
        cell.addView(value);
        return cell;
    }
}
