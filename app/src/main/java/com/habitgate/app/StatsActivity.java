package com.habitgate.app;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.List;

public class StatsActivity extends android.app.Activity {
    private HabitDb db;
    private LinearLayout list;
    private BarChartView chart;
    private String mode = "week";
    private String pickedDate;
    private Button dayChip;
    private Button weekChip;
    private Button monthChip;
    private Button dateChip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        buildUi();
        refresh();
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
        addChip(chips, dateChip, false);
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
        LocalDate initial = pickedDate == null ? LocalDate.now() : DateTools.parseOrToday(pickedDate);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            pickedDate = LocalDate.of(year, month + 1, dayOfMonth).format(DateTools.DATE);
            mode = "date";
            refresh();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void refresh() {
        Ui.setChipSelected(dayChip, "day".equals(mode));
        Ui.setChipSelected(weekChip, "week".equals(mode));
        Ui.setChipSelected(monthChip, "month".equals(mode));
        Ui.setChipSelected(dateChip, "date".equals(mode));
        if ("date".equals(mode) && pickedDate != null) {
            dateChip.setText(DateTools.formatShortDateWithWeekday(pickedDate));
        } else {
            dateChip.setText("📅");
        }

        String today = DateTools.today();
        Models.Cycle active = db.getCurrentCycle();
        String to = DateTools.maxDate(today, active.cycleDate);
        String from;
        String label;
        if ("day".equals(mode)) {
            from = active.cycleDate;
            to = active.cycleDate;
            label = "現在の対象日";
        } else if ("date".equals(mode) && pickedDate != null) {
            from = pickedDate;
            to = pickedDate;
            label = DateTools.formatDisplayDate(pickedDate);
        } else if ("month".equals(mode)) {
            from = DateTools.startOfMonth();
            label = "今月";
        } else {
            from = DateTools.startOfWeek();
            label = "今週";
        }

        List<Models.DayTotal> totals = db.getDayTotals(from, to);
        chart.setTotals(totals);
        List<Models.Record> records = db.getRecords(from, to);
        List<Models.Cycle> cycles = db.getCycles(from, to);
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
        summaryRow.addView(summaryCell("やること", doTotal, Ui.GOOD),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        summaryRow.addView(summaryCell("減らすこと", reduceTotal, Ui.DANGER),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        summaryCard.addView(summaryRow);

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
                TextView row = new TextView(this);
                String text = (isDo ? "✅ " : "⚠ ") + r.title + " / " + DateTools.formatMinutes(r.durationMinutes) + (r.synced ? "" : " / 未同期");
                if (!r.note.isEmpty()) text += "\n　メモ: " + r.note;
                row.setText(text);
                row.setTextSize(14);
                row.setTextColor(Ui.TEXT);
                row.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
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
                row.setText("・" + DateTools.formatDisplayDate(c.cycleDate) + "\n　開始: " + DateTools.formatDateTime(c.startAt) + " / 終了: " + end + (c.synced ? "" : " / 未同期"));
                row.setTextSize(14);
                row.setTextColor(Ui.TEXT);
                row.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
                cyclesCard.addView(row);
            }
        }

        SheetsSync.syncUnsynced(this, false);
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
