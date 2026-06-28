package com.habitgate.app;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class CheckInActivity extends android.app.Activity {
    private HabitDb db;
    private final List<DoRow> doRows = new ArrayList<>();
    private final List<ReduceRow> reduceRows = new ArrayList<>();
    private String actualDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        actualDate = DateTools.today();
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.vertical(this);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 36));
        scroll.addView(root);

        root.addView(Ui.title(this, "今日は何をした？"));
        root.addView(Ui.note(this, "実績日: " + actualDate + "。チェックしたものだけ時間つきで記録します。"));

        root.addView(Ui.section(this, "やること"));
        List<Models.Task> tasks = db.getDueDoTasks(actualDate);
        if (tasks.isEmpty()) {
            root.addView(Ui.note(this, "今日分のタスクはありません。メイン画面で明日のやることを追加できます。"));
        } else {
            for (Models.Task task : tasks) {
                DoRow row = addTaskRow(root, task);
                doRows.add(row);
                Ui.addDivider(this, root);
            }
        }

        root.addView(Ui.section(this, "減らすこと"));
        List<Models.ReduceItem> items = db.getActiveReduceItems();
        if (items.isEmpty()) {
            root.addView(Ui.note(this, "減らすことが未登録です。メイン画面から追加できます。"));
        } else {
            for (Models.ReduceItem item : items) {
                ReduceRow row = addReduceRow(root, item);
                reduceRows.add(row);
                Ui.addDivider(this, root);
            }
        }

        Button save = Ui.button(this, "保存する");
        save.setOnClickListener(v -> save());
        root.addView(save);

        setContentView(scroll);
    }

    private DoRow addTaskRow(LinearLayout parent, Models.Task task) {
        LinearLayout wrapper = Ui.vertical(this);
        CheckBox cb = new CheckBox(this);
        cb.setText(task.title + "  （予定: " + task.plannedDate + "）");
        cb.setTextSize(16);
        wrapper.addView(cb);
        LinearLayout duration = durationRow();
        EditText h = (EditText) duration.getChildAt(1);
        EditText m = (EditText) duration.getChildAt(3);
        wrapper.addView(duration);
        parent.addView(wrapper);
        return new DoRow(task, cb, h, m);
    }

    private ReduceRow addReduceRow(LinearLayout parent, Models.ReduceItem item) {
        LinearLayout wrapper = Ui.vertical(this);
        CheckBox cb = new CheckBox(this);
        cb.setText(item.title);
        cb.setTextSize(16);
        wrapper.addView(cb);
        LinearLayout duration = durationRow();
        EditText h = (EditText) duration.getChildAt(1);
        EditText m = (EditText) duration.getChildAt(3);
        wrapper.addView(duration);
        parent.addView(wrapper);
        return new ReduceRow(item, cb, h, m);
    }

    private LinearLayout durationRow() {
        LinearLayout row = Ui.horizontal(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = new TextView(this);
        label.setText("時間: ");
        row.addView(label);
        EditText h = Ui.numberEdit(this, "時", 3);
        row.addView(h);
        TextView colon = new TextView(this);
        colon.setText(" 時 ");
        row.addView(colon);
        EditText m = Ui.numberEdit(this, "分", 3);
        row.addView(m);
        TextView suffix = new TextView(this);
        suffix.setText(" 分");
        row.addView(suffix);
        return row;
    }

    private void save() {
        int saved = 0;
        String nextDate = DateTools.nextDay(actualDate);
        for (DoRow row : doRows) {
            if (row.checkBox.isChecked()) {
                int minutes = DateTools.parseMinutes(row.hours.getText().toString(), row.minutes.getText().toString());
                db.addRecord(HabitDb.CATEGORY_DO, row.task.title, minutes, actualDate);
                db.completeDoTask(row.task.id);
                saved++;
            } else {
                db.carryOverDoTask(row.task.id, nextDate);
            }
        }
        for (ReduceRow row : reduceRows) {
            if (row.checkBox.isChecked()) {
                int minutes = DateTools.parseMinutes(row.hours.getText().toString(), row.minutes.getText().toString());
                db.addRecord(HabitDb.CATEGORY_REDUCE, row.item.title, minutes, actualDate);
                saved++;
            }
        }
        ReminderScheduler.scheduleNext(this);
        SheetsSync.syncUnsynced(this, false);
        Toast.makeText(this, "保存しました: " + saved + "件", Toast.LENGTH_LONG).show();
        finish();
    }

    private static class DoRow {
        final Models.Task task;
        final CheckBox checkBox;
        final EditText hours;
        final EditText minutes;

        DoRow(Models.Task task, CheckBox checkBox, EditText hours, EditText minutes) {
            this.task = task;
            this.checkBox = checkBox;
            this.hours = hours;
            this.minutes = minutes;
        }
    }

    private static class ReduceRow {
        final Models.ReduceItem item;
        final CheckBox checkBox;
        final EditText hours;
        final EditText minutes;

        ReduceRow(Models.ReduceItem item, CheckBox checkBox, EditText hours, EditText minutes) {
            this.item = item;
            this.checkBox = checkBox;
            this.hours = hours;
            this.minutes = minutes;
        }
    }
}
