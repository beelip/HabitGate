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
    private Models.Cycle cycle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        buildUi();
    }

    private void buildUi() {
        cycle = db.getCurrentCycle();
        doRows.clear();
        reduceRows.clear();

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.vertical(this);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 36));
        scroll.addView(root);

        root.addView(Ui.title(this, "今日は何をした？"));
        root.addView(Ui.note(this, "対象日: " + cycle.cycleDate + " / 開始: " + DateTools.formatDateTime(cycle.startAt)));
        root.addView(Ui.note(this, "この画面は通知時刻以外でもいつでも開けます。保存しても一日は終了しません。"));

        root.addView(Ui.section(this, "この日のやることを追加"));
        EditText addTitle = Ui.edit(this, "例: 30分走る / PM過去問1問");
        EditText addNote = Ui.edit(this, "メモ（任意）");
        root.addView(addTitle);
        root.addView(addNote);
        Button addToday = Ui.button(this, cycle.cycleDate + " に追加");
        addToday.setOnClickListener(v -> {
            String title = addTitle.getText().toString().trim();
            if (title.isEmpty()) return;
            db.addDoTask(title, addNote.getText().toString(), cycle.cycleDate);
            Toast.makeText(this, "追加しました", Toast.LENGTH_SHORT).show();
            buildUi();
        });
        root.addView(addToday);

        root.addView(Ui.section(this, "やること"));
        List<Models.Task> tasks = db.getDueDoTasks(cycle.cycleDate);
        if (tasks.isEmpty()) {
            root.addView(Ui.note(this, "この日に処理するタスクはありません。上の欄から当日タスクを追加できます。"));
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

        Button save = Ui.button(this, "実績を保存する");
        save.setOnClickListener(v -> save(false));
        root.addView(save);

        Button closeDay = Ui.button(this, "保存して、この日を終わらせる");
        closeDay.setOnClickListener(v -> save(true));
        root.addView(closeDay);

        setContentView(scroll);
    }

    private DoRow addTaskRow(LinearLayout parent, Models.Task task) {
        LinearLayout wrapper = Ui.vertical(this);
        TextView title = new TextView(this);
        title.setText(task.title + "  （予定: " + task.plannedDate + "）");
        title.setTextSize(16);
        wrapper.addView(title);
        if (!task.note.isEmpty()) {
            wrapper.addView(Ui.note(this, "メモ: " + task.note));
        }

        CheckBox log = new CheckBox(this);
        log.setText("実績時間を追加する");
        wrapper.addView(log);

        CheckBox complete = new CheckBox(this);
        complete.setText("完了する");
        wrapper.addView(complete);

        LinearLayout duration = durationRow();
        EditText h = (EditText) duration.getChildAt(1);
        EditText m = (EditText) duration.getChildAt(3);
        wrapper.addView(duration);

        EditText note = Ui.edit(this, "実績メモ（任意）");
        wrapper.addView(note);

        parent.addView(wrapper);
        return new DoRow(task, log, complete, h, m, note);
    }

    private ReduceRow addReduceRow(LinearLayout parent, Models.ReduceItem item) {
        LinearLayout wrapper = Ui.vertical(this);
        TextView title = new TextView(this);
        title.setText(item.title);
        title.setTextSize(16);
        wrapper.addView(title);
        if (!item.note.isEmpty()) {
            wrapper.addView(Ui.note(this, "メモ: " + item.note));
        }

        CheckBox done = new CheckBox(this);
        done.setText("今日やってしまった");
        wrapper.addView(done);

        LinearLayout duration = durationRow();
        EditText h = (EditText) duration.getChildAt(1);
        EditText m = (EditText) duration.getChildAt(3);
        wrapper.addView(duration);

        EditText note = Ui.edit(this, "実績メモ（任意）");
        wrapper.addView(note);

        parent.addView(wrapper);
        return new ReduceRow(item, done, h, m, note);
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

    private void save(boolean closeAfterSave) {
        int records = 0;
        int completed = 0;
        for (DoRow row : doRows) {
            int minutes = DateTools.parseMinutes(row.hours.getText().toString(), row.minutes.getText().toString());
            String actualNote = row.note.getText().toString().trim();
            boolean shouldLog = row.logCheck.isChecked() || row.completeCheck.isChecked() || minutes > 0 || !actualNote.isEmpty();
            if (shouldLog) {
                db.addRecord(HabitDb.CATEGORY_DO, row.task.title, mergeNotes(row.task.note, actualNote), minutes, cycle.cycleDate);
                records++;
            }
            if (row.completeCheck.isChecked()) {
                db.completeDoTask(row.task.id);
                completed++;
            }
        }
        for (ReduceRow row : reduceRows) {
            int minutes = DateTools.parseMinutes(row.hours.getText().toString(), row.minutes.getText().toString());
            String actualNote = row.note.getText().toString().trim();
            boolean shouldLog = row.doneCheck.isChecked() || minutes > 0 || !actualNote.isEmpty();
            if (shouldLog) {
                db.addRecord(HabitDb.CATEGORY_REDUCE, row.item.title, mergeNotes(row.item.note, actualNote), minutes, cycle.cycleDate);
                records++;
            }
        }

        String extra = "";
        if (closeAfterSave) {
            Models.Cycle next = db.endCurrentCycleAndStartNext();
            ReminderScheduler.scheduleNext(this);
            extra = " / 次の対象日: " + next.cycleDate;
        }
        SheetsSync.syncUnsynced(this, false);
        Toast.makeText(this, "保存しました: 実績" + records + "件 / 完了" + completed + "件" + extra, Toast.LENGTH_LONG).show();
        finish();
    }

    private String mergeNotes(String itemNote, String actualNote) {
        String left = itemNote == null ? "" : itemNote.trim();
        String right = actualNote == null ? "" : actualNote.trim();
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;
        return "項目メモ: " + left + " / 実績メモ: " + right;
    }

    private static class DoRow {
        final Models.Task task;
        final CheckBox logCheck;
        final CheckBox completeCheck;
        final EditText hours;
        final EditText minutes;
        final EditText note;

        DoRow(Models.Task task, CheckBox logCheck, CheckBox completeCheck, EditText hours, EditText minutes, EditText note) {
            this.task = task;
            this.logCheck = logCheck;
            this.completeCheck = completeCheck;
            this.hours = hours;
            this.minutes = minutes;
            this.note = note;
        }
    }

    private static class ReduceRow {
        final Models.ReduceItem item;
        final CheckBox doneCheck;
        final EditText hours;
        final EditText minutes;
        final EditText note;

        ReduceRow(Models.ReduceItem item, CheckBox doneCheck, EditText hours, EditText minutes, EditText note) {
            this.item = item;
            this.doneCheck = doneCheck;
            this.hours = hours;
            this.minutes = minutes;
            this.note = note;
        }
    }
}
