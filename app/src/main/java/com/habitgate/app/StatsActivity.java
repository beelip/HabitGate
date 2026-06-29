package com.habitgate.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public class StatsActivity extends android.app.Activity {
    private HabitDb db;
    private LinearLayout list;
    private BarChartView chart;
    private String mode = "week";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        buildUi();
        refresh();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.vertical(this);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 36));
        scroll.addView(root);

        root.addView(Ui.title(this, "集計"));
        root.addView(Ui.note(this, "やることは積み上げ、減らすことは発生量として見ます。サイクルの開始・終了時刻も確認できます。"));

        LinearLayout buttons = Ui.horizontal(this);
        Button day = Ui.button(this, "今日");
        Button week = Ui.button(this, "今週");
        Button month = Ui.button(this, "今月");
        day.setOnClickListener(v -> { mode = "day"; refresh(); });
        week.setOnClickListener(v -> { mode = "week"; refresh(); });
        month.setOnClickListener(v -> { mode = "month"; refresh(); });
        buttons.addView(day, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        buttons.addView(week, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        buttons.addView(month, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(buttons);

        chart = new BarChartView(this);
        root.addView(chart, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 260)));

        list = Ui.vertical(this);
        root.addView(list);
        setContentView(scroll);
    }

    private void refresh() {
        String today = DateTools.today();
        Models.Cycle active = db.getCurrentCycle();
        String to = DateTools.maxDate(today, active.cycleDate);
        String from;
        String label;
        if ("day".equals(mode)) {
            from = active.cycleDate;
            to = active.cycleDate;
            label = "現在の対象日";
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
        list.addView(Ui.section(this, label + "の記録"));

        int doTotal = 0;
        int reduceTotal = 0;
        for (Models.Record r : records) {
            if (HabitDb.CATEGORY_DO.equals(r.category)) doTotal += r.durationMinutes;
            if (HabitDb.CATEGORY_REDUCE.equals(r.category)) reduceTotal += r.durationMinutes;
        }
        TextView summary = new TextView(this);
        summary.setText("やること: " + DateTools.formatMinutes(doTotal) + " / 減らすこと: " + DateTools.formatMinutes(reduceTotal));
        summary.setTextSize(16);
        summary.setPadding(0, 0, 0, Ui.dp(this, 10));
        list.addView(summary);

        if (records.isEmpty()) {
            list.addView(Ui.note(this, "この期間の実績記録はまだありません。"));
        } else {
            String currentDate = "";
            for (Models.Record r : records) {
                if (!currentDate.equals(r.actualDate)) {
                    currentDate = r.actualDate;
                    TextView date = new TextView(this);
                    date.setText(currentDate);
                    date.setTextSize(17);
                    date.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 4));
                    list.addView(date);
                }
                TextView row = new TextView(this);
                String category = HabitDb.CATEGORY_DO.equals(r.category) ? "やること" : "減らすこと";
                String text = "・" + category + " / " + r.title + " / " + DateTools.formatMinutes(r.durationMinutes) + (r.synced ? " / 同期済" : " / 未同期");
                if (!r.note.isEmpty()) text += "\n  メモ: " + r.note;
                row.setText(text);
                row.setTextSize(15);
                row.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
                list.addView(row);
            }
        }

        list.addView(Ui.section(this, "サイクル履歴"));
        if (cycles.isEmpty()) {
            list.addView(Ui.note(this, "この期間のサイクル記録はまだありません。"));
        } else {
            for (Models.Cycle c : cycles) {
                TextView row = new TextView(this);
                String end = c.closed ? DateTools.formatDateTime(c.endAt) : "進行中";
                row.setText("・" + c.cycleDate + " / 開始: " + DateTools.formatDateTime(c.startAt) + " / 終了: " + end + (c.synced ? " / 同期済" : " / 未同期"));
                row.setTextSize(15);
                row.setPadding(0, Ui.dp(this, 3), 0, Ui.dp(this, 3));
                list.addView(row);
            }
        }

        SheetsSync.syncUnsynced(this, false);
    }
}
