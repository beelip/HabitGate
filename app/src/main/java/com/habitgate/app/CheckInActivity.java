package com.habitgate.app;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CheckInActivity extends android.app.Activity {
    private HabitDb db;
    private final List<DoRow> doRows = new ArrayList<>();
    private final List<ReduceRow> reduceRows = new ArrayList<>();
    private Models.Cycle cycle;
    // 記録の対象日。既定は現在のサイクル対象日で、過去日などに変更できる。
    private String targetDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        targetDate = db.getCurrentCycle().cycleDate;
        buildUi();
    }

    private void buildUi() {
        cycle = db.getCurrentCycle();
        doRows.clear();
        reduceRows.clear();
        boolean isCurrentCycleDate = targetDate.equals(cycle.cycleDate);

        LinearLayout root = Ui.screen(this);
        root.addView(Ui.title(this, "今日は何をした？"));

        // 対象日カード
        LinearLayout dateCard = Ui.card(this, root);
        LinearLayout dateRow = Ui.horizontal(this);
        TextView dateText = new TextView(this);
        dateText.setText("対象日: " + DateTools.formatShortDateWithWeekday(targetDate)
                + (isCurrentCycleDate ? "" : "（過去入力）"));
        dateText.setTextSize(17);
        dateText.setTypeface(Typeface.DEFAULT_BOLD);
        dateText.setTextColor(isCurrentCycleDate ? Ui.TEXT : Ui.PRIMARY);
        dateRow.addView(dateText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button changeDate = Ui.iconButton(this, "📅");
        changeDate.setContentDescription("対象日を変更");
        changeDate.setOnClickListener(v -> openTargetDatePicker());
        dateRow.addView(changeDate, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        dateCard.addView(dateRow);
        if (isCurrentCycleDate) {
            dateCard.addView(Ui.note(this, "開始: " + DateTools.formatDateTime(cycle.startAt) + " / 📅 から過去の日付を選んで、後から記録することもできます。"));
        } else {
            dateCard.addView(Ui.note(this, "選択した日付の実績として保存されます。一日の終了は現在の対象日でのみ行えます。"));
        }

        // この日のやることを追加
        root.addView(Ui.section(this, "この日のやることを追加"));
        LinearLayout addCard = Ui.card(this, root);
        EditText addTitle = Ui.edit(this, "例: 30分走る / PM過去問1問");
        EditText addNote = Ui.edit(this, "メモ（任意）");
        addCard.addView(addTitle);
        addCard.addView(addNote);
        Button addButton = Ui.tonalButton(this, DateTools.formatShortDateWithWeekday(targetDate) + " に追加");
        addButton.setOnClickListener(v -> {
            String title = addTitle.getText().toString().trim();
            if (title.isEmpty()) return;
            db.addDoTask(title, addNote.getText().toString(), targetDate);
            Toast.makeText(this, "追加しました", Toast.LENGTH_SHORT).show();
            buildUi();
        });
        addCard.addView(addButton);

        // やること
        root.addView(Ui.section(this, "やること"));
        LinearLayout doCard = Ui.card(this, root);
        List<Models.Task> tasks = db.getDueDoTasks(targetDate);
        if (tasks.isEmpty()) {
            doCard.addView(Ui.note(this, "この日に処理するタスクはありません。上の欄から追加できます。"));
        } else {
            boolean first = true;
            for (Models.Task task : tasks) {
                if (!first) Ui.addDivider(this, doCard);
                first = false;
                doRows.add(addTaskRow(doCard, task));
            }
        }

        // 減らすこと
        root.addView(Ui.section(this, "減らすこと"));
        LinearLayout reduceCard = Ui.card(this, root);
        List<Models.ReduceItem> items = db.getActiveReduceItems();
        if (items.isEmpty()) {
            reduceCard.addView(Ui.note(this, "減らすことが未登録です。メイン画面から追加できます。"));
        } else {
            boolean linkedWithoutPermission = false;
            boolean first = true;
            for (Models.ReduceItem item : items) {
                if (!first) Ui.addDivider(this, reduceCard);
                first = false;
                reduceRows.add(addReduceRow(reduceCard, item));
                if (item.hasLinkedApp() && !AppUsage.hasPermission(this)) linkedWithoutPermission = true;
            }
            if (linkedWithoutPermission) {
                reduceCard.addView(Ui.note(this, "⚠ アプリ連携済みの項目がありますが、「使用状況へのアクセス」が未許可のため計測できません。設定画面から許可してください。"));
            }
        }

        Ui.space(this, root, 6);
        Button save = Ui.primaryButton(this, "実績を保存する");
        save.setOnClickListener(v -> save(false));
        root.addView(save);

        if (isCurrentCycleDate) {
            Ui.space(this, root, 8);
            Button closeDay = Ui.tonalButton(this, "保存して、この日を終わらせる");
            closeDay.setOnClickListener(v -> save(true));
            root.addView(closeDay);
        }
    }

    private void openTargetDatePicker() {
        LocalDate initial = DateTools.parseOrToday(targetDate);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            targetDate = LocalDate.of(year, month + 1, dayOfMonth).format(DateTools.DATE);
            buildUi();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private DoRow addTaskRow(LinearLayout parent, Models.Task task) {
        LinearLayout wrapper = Ui.vertical(this);
        TextView title = new TextView(this);
        title.setText(task.title + "  （予定: " + DateTools.formatShortDateWithWeekday(task.plannedDate) + "）");
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Ui.TEXT);
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
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Ui.TEXT);
        wrapper.addView(title);
        if (!item.note.isEmpty()) {
            wrapper.addView(Ui.note(this, "メモ: " + item.note));
        }

        CheckBox done = new CheckBox(this);
        done.setText("この日やってしまった");
        wrapper.addView(done);

        LinearLayout duration = durationRow();
        EditText h = (EditText) duration.getChildAt(1);
        EditText m = (EditText) duration.getChildAt(3);
        wrapper.addView(duration);

        // アプリ連携済みなら計測時間を表示し、タップで時間欄に反映する
        if (item.hasLinkedApp() && AppUsage.hasPermission(this)) {
            int measured = AppUsage.foregroundMinutesOn(this, item.appPackage, targetDate);
            LinearLayout measuredRow = Ui.horizontal(this);
            TextView measuredText = new TextView(this);
            measuredText.setText("📱 " + AppUsage.appLabel(this, item.appPackage) + " 計測: " + DateTools.formatMinutes(measured));
            measuredText.setTextSize(14);
            measuredText.setTextColor(Ui.PRIMARY);
            measuredRow.addView(measuredText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            if (measured > 0) {
                Button apply = Ui.tonalButton(this, "反映");
                apply.setMinHeight(Ui.dp(this, 36));
                apply.setMinimumHeight(Ui.dp(this, 36));
                apply.setOnClickListener(v -> {
                    h.setText(String.valueOf(measured / 60));
                    m.setText(String.valueOf(measured % 60));
                    done.setChecked(true);
                });
                measuredRow.addView(apply, new LinearLayout.LayoutParams(Ui.dp(this, 80), LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            wrapper.addView(measuredRow);
        }

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
        EditText h = Ui.numberEdit(this, "0", 3);
        row.addView(h);
        TextView colon = new TextView(this);
        colon.setText(" 時間 ");
        row.addView(colon);
        EditText m = Ui.numberEdit(this, "0", 3);
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
                db.addRecord(HabitDb.CATEGORY_DO, row.task.title, mergeNotes(row.task.note, actualNote), minutes, targetDate);
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
                db.addRecord(HabitDb.CATEGORY_REDUCE, row.item.title, mergeNotes(row.item.note, actualNote), minutes, targetDate);
                records++;
            }
        }

        String extra = "";
        if (closeAfterSave) {
            // 手動未入力のアプリ連携項目は、計測時間で自動記録してから一日を閉じる
            int auto = AppUsage.autoRecordLinkedApps(this, db, cycle.cycleDate);
            if (auto > 0) extra += " / 自動計測" + auto + "件";
            Models.Cycle next = db.endCurrentCycleAndStartNext();
            ReminderScheduler.scheduleNext(this);
            extra += " / 次の対象日: " + DateTools.formatDisplayDate(next.cycleDate);
        }
        SheetsSync.syncUnsynced(this, false);
        if (closeAfterSave) {
            extra += updateConfiguredCsvBackupMessage();
        }
        Toast.makeText(this, "保存しました: 実績" + records + "件 / 完了" + completed + "件" + extra, Toast.LENGTH_LONG).show();
        finish();
    }

    private String updateConfiguredCsvBackupMessage() {
        if (!CsvBackupManager.hasBackupDirectory(this)) {
            return "";
        }
        try {
            CsvBackupManager.writeBackupToConfiguredDirectory(this);
            return " / CSV更新: 完了";
        } catch (Exception e) {
            return " / CSV更新: 失敗";
        }
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
