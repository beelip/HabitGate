package com.habitgate.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** やることタスク1件の実績入力・完了・削除を行う画面。 */
public class TaskEntryActivity extends ThemedActivity {
    private HabitDb db;
    private Models.Task task;
    private String entryDate;
    private String memo = "";
    private String hoursText = "";
    private String minutesText = "";

    private EditText hoursEdit;
    private EditText minutesEdit;
    private TextView memoPreview;
    private CheckBox completeCheck;
    private TextView plannedText;
    private Button priorityButton;
    private Button dueButton;
    private TextView taskNoteText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        long id = getIntent().getLongExtra("task_id", -1);
        task = db.getDoTask(id);
        if (task == null) {
            Toast.makeText(this, "タスクが見つかりませんでした", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        entryDate = db.getCurrentCycle().cycleDate;
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = Ui.screen(this);
        root.addView(Ui.title(this, "実績入力"));

        // タスク情報
        LinearLayout infoCard = Ui.card(this, root);
        TextView titleText = new TextView(this);
        titleText.setText(task.title);
        titleText.setTextSize(17);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(Ui.TEXT);
        infoCard.addView(titleText);

        LinearLayout plannedRow = Ui.horizontal(this);
        plannedText = new TextView(this);
        plannedText.setText("予定日: " + DateTools.formatShortDateWithWeekday(task.plannedDate));
        plannedText.setTextSize(13);
        plannedText.setTextColor(Ui.MUTED);
        plannedRow.addView(plannedText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button plannedDateButton = Ui.iconButton(this, "📅");
        plannedDateButton.setContentDescription("予定日を変更");
        plannedDateButton.setOnClickListener(v -> openPlannedDatePicker());
        plannedRow.addView(plannedDateButton, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        infoCard.addView(plannedRow);

        LinearLayout taskNoteRow = Ui.horizontal(this);
        taskNoteText = new TextView(this);
        taskNoteText.setTextSize(13);
        taskNoteText.setMaxLines(3);
        taskNoteText.setEllipsize(TextUtils.TruncateAt.END);
        updateTaskNoteText();
        taskNoteText.setOnClickListener(v -> openTaskNoteDialog());
        taskNoteRow.addView(taskNoteText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button taskNoteButton = Ui.iconButton(this, "📝");
        taskNoteButton.setContentDescription("タスクのメモを編集");
        taskNoteButton.setOnClickListener(v -> openTaskNoteDialog());
        taskNoteRow.addView(taskNoteButton, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        infoCard.addView(taskNoteRow);

        LinearLayout taskOptionsRow = Ui.horizontal(this);
        priorityButton = Ui.button(this, "優先度: " + DateTools.priorityLabel(task.priority));
        priorityButton.setOnClickListener(v -> openPriorityDialog());
        LinearLayout.LayoutParams priorityLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        priorityLp.rightMargin = Ui.dp(this, 8);
        taskOptionsRow.addView(priorityButton, priorityLp);
        dueButton = Ui.button(this, "期限: " + DateTools.formatDueShort(task.dueAt));
        dueButton.setOnClickListener(v -> openTaskDueDatePicker());
        dueButton.setOnLongClickListener(v -> {
            db.updateDoTask(task.id, task.title, task.note, task.plannedDate, task.priority, 0);
            AutoSync.run(this);
            task = db.getDoTask(task.id);
            dueButton.setText("期限: " + DateTools.formatDueShort(task.dueAt));
            Toast.makeText(this, "期限をクリアしました", Toast.LENGTH_SHORT).show();
            return true;
        });
        taskOptionsRow.addView(dueButton, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        infoCard.addView(taskOptionsRow);

        // 入力
        LinearLayout inputCard = Ui.card(this, root);

        LinearLayout dateRow = Ui.horizontal(this);
        TextView dateText = new TextView(this);
        dateText.setText("入力日: " + DateTools.formatShortDateWithWeekday(entryDate));
        dateText.setTextSize(15);
        dateText.setTypeface(Typeface.DEFAULT_BOLD);
        dateText.setTextColor(Ui.TEXT);
        dateRow.addView(dateText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button changeDate = Ui.iconButton(this, "📅");
        changeDate.setContentDescription("入力日を変更");
        changeDate.setOnClickListener(v -> openDatePicker());
        dateRow.addView(changeDate, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        inputCard.addView(dateRow);

        LinearLayout duration = durationRow();
        hoursEdit = (EditText) duration.getChildAt(1);
        minutesEdit = (EditText) duration.getChildAt(3);
        if (!hoursText.isEmpty()) hoursEdit.setText(hoursText);
        if (!minutesText.isEmpty()) minutesEdit.setText(minutesText);
        inputCard.addView(duration);

        LinearLayout memoRow = Ui.horizontal(this);
        memoPreview = new TextView(this);
        memoPreview.setMaxLines(2);
        memoPreview.setEllipsize(TextUtils.TruncateAt.END);
        memoPreview.setTextSize(14);
        updateMemoPreview();
        memoPreview.setOnClickListener(v -> openMemoDialog());
        memoRow.addView(memoPreview, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button memoButton = Ui.iconButton(this, "📝");
        memoButton.setContentDescription("実績メモを編集");
        memoButton.setOnClickListener(v -> openMemoDialog());
        memoRow.addView(memoButton, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        inputCard.addView(memoRow);

        LinearLayout completeRow = Ui.horizontal(this);
        completeCheck = new CheckBox(this);
        completeCheck.setText("完了する");
        completeRow.addView(completeCheck, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button carryOver = Ui.tonalButton(this, "明日に繰り越す");
        carryOver.setOnClickListener(v -> confirmCarryOver());
        completeRow.addView(carryOver, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        inputCard.addView(completeRow);

        Ui.space(this, root, 6);
        Button save = Ui.primaryButton(this, "保存");
        save.setOnClickListener(v -> onSave());
        root.addView(save);

        Ui.space(this, root, 16);
        Button delete = Ui.button(this, "🗑 このタスクを削除");
        delete.setTextColor(Ui.DANGER);
        delete.setOnClickListener(v -> confirmDelete());
        root.addView(delete);
    }

    private LinearLayout durationRow() {
        LinearLayout row = Ui.horizontal(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = new TextView(this);
        label.setText("時間: ");
        row.addView(label);
        EditText h = Ui.numberEdit(this, "0", 99);
        row.addView(h);
        TextView colon = new TextView(this);
        colon.setText(" 時間 ");
        row.addView(colon);
        EditText m = Ui.numberEdit(this, "0", 59);
        row.addView(m);
        TextView suffix = new TextView(this);
        suffix.setText(" 分");
        row.addView(suffix);
        return row;
    }

    private void updateTaskNoteText() {
        if (task.note.isEmpty()) {
            taskNoteText.setText("メモ（任意）");
            taskNoteText.setTextColor(Ui.HINT);
        } else {
            taskNoteText.setText("メモ: " + task.note);
            taskNoteText.setTextColor(Ui.MUTED);
        }
    }

    private void openTaskNoteDialog() {
        EditText input = Ui.multilineEdit(this, "メモ（任意）");
        input.setText(task.note);
        int pad = Ui.dp(this, 18);
        LinearLayout wrapper = Ui.vertical(this);
        wrapper.setPadding(pad, Ui.dp(this, 6), pad, 0);
        wrapper.addView(input);
        Ui.dialog(this)
                .setTitle("タスクのメモ")
                .setView(wrapper)
                .setPositiveButton("保存", (dialog, which) -> {
                    String newNote = input.getText().toString().trim();
                    db.updateDoTask(task.id, task.title, newNote, task.plannedDate, task.priority, task.dueAt);
                    AutoSync.run(this);
                    task = db.getDoTask(task.id);
                    updateTaskNoteText();
                    Toast.makeText(this, "メモを保存しました", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void updateMemoPreview() {
        if (memo.isEmpty()) {
            memoPreview.setText("実績メモ（任意）");
            memoPreview.setTextColor(Ui.HINT);
        } else {
            memoPreview.setText(memo);
            memoPreview.setTextColor(Ui.TEXT);
        }
    }

    private void openMemoDialog() {
        EditText input = Ui.multilineEdit(this, "実績メモ（任意）");
        input.setText(memo);
        int pad = Ui.dp(this, 18);
        LinearLayout wrapper = Ui.vertical(this);
        wrapper.setPadding(pad, Ui.dp(this, 6), pad, 0);
        wrapper.addView(input);
        Ui.dialog(this)
                .setTitle("実績メモ")
                .setView(wrapper)
                .setPositiveButton("保存", (dialog, which) -> {
                    memo = input.getText().toString().trim();
                    updateMemoPreview();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void openDatePicker() {
        hoursText = hoursEdit.getText().toString();
        minutesText = minutesEdit.getText().toString();
        LocalDate initial = DateTools.parseOrToday(entryDate);
        new DatePickerDialog(this, Ui.pickerTheme(), (view, year, month, dayOfMonth) -> {
            entryDate = LocalDate.of(year, month + 1, dayOfMonth).format(DateTools.DATE);
            buildUi();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void openPlannedDatePicker() {
        LocalDate initial = DateTools.parseOrToday(task.plannedDate);
        new DatePickerDialog(this, Ui.pickerTheme(), (view, year, month, dayOfMonth) -> {
            String newDate = LocalDate.of(year, month + 1, dayOfMonth).format(DateTools.DATE);
            db.updateDoTask(task.id, task.title, task.note, newDate, task.priority, task.dueAt);
            AutoSync.run(this);
            task = db.getDoTask(task.id);
            plannedText.setText("予定日: " + DateTools.formatShortDateWithWeekday(task.plannedDate));
            Toast.makeText(this, "予定日を変更しました", Toast.LENGTH_SHORT).show();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void openPriorityDialog() {
        Ui.dialog(this)
                .setTitle("優先度")
                .setItems(new String[]{"なし", "低", "中", "高"}, (d, which) -> {
                    db.updateDoTask(task.id, task.title, task.note, task.plannedDate, which, task.dueAt);
                    AutoSync.run(this);
                    task = db.getDoTask(task.id);
                    priorityButton.setText("優先度: " + DateTools.priorityLabel(task.priority));
                    Toast.makeText(this, "優先度を変更しました", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void openTaskDueDatePicker() {
        LocalDateTime initial = task.dueAt > 0
                ? Instant.ofEpochMilli(task.dueAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
                : LocalDate.parse(task.plannedDate, DateTools.DATE).atTime(23, 59);
        new DatePickerDialog(this, Ui.pickerTheme(), (view, year, month, dayOfMonth) -> {
            LocalDate date = LocalDate.of(year, month + 1, dayOfMonth);
            openTaskDueTimePicker(date, initial.getHour(), initial.getMinute());
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void openTaskDueTimePicker(LocalDate date, int initialHour, int initialMinute) {
        new TimePickerDialog(this, Ui.pickerTheme(), (view, hourOfDay, minute) -> {
            long newDueAt = date.atTime(hourOfDay, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            db.updateDoTask(task.id, task.title, task.note, task.plannedDate, task.priority, newDueAt);
            AutoSync.run(this);
            task = db.getDoTask(task.id);
            dueButton.setText("期限: " + DateTools.formatDueShort(task.dueAt));
            Toast.makeText(this, "期限を設定しました", Toast.LENGTH_SHORT).show();
        }, initialHour, initialMinute, true).show();
    }

    private void onSave() {
        int minutes = DateTools.parseMinutes(hoursEdit.getText().toString(), minutesEdit.getText().toString());
        if (!completeCheck.isChecked()) {
            if (minutes == 0 && memo.isEmpty()) {
                Toast.makeText(this, "入力がありません", Toast.LENGTH_SHORT).show();
                return;
            }
            db.addRecord(HabitDb.CATEGORY_DO, task.title, mergeNotes(task.note, memo), minutes, entryDate);
            AutoSync.run(this);
            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        Ui.dialog(this)
                .setTitle("完了しますか？")
                .setMessage("「" + task.title + "」を完了します。")
                .setPositiveButton("完了", (dialog, which) -> {
                    db.addRecord(HabitDb.CATEGORY_DO, task.title, mergeNotes(task.note, memo), minutes, entryDate);
                    db.completeDoTask(task.id, entryDate);
                    AutoSync.run(this);
                    Toast.makeText(this, "完了しました", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void confirmCarryOver() {
        String nextDate = DateTools.nextDay(DateTools.maxDate(task.plannedDate, db.getCurrentCycle().cycleDate));
        Ui.dialog(this)
                .setTitle("翌日に繰り越しますか？")
                .setMessage("「" + task.title + "」を " + DateTools.formatShortDateWithWeekday(nextDate) + " に移動します。")
                .setPositiveButton("繰り越す", (d, w) -> {
                    db.carryOverDoTask(task.id, nextDate);
                    AutoSync.run(this);
                    Toast.makeText(this, "繰り越しました", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void confirmDelete() {
        Ui.dialog(this)
                .setTitle("やることを削除しますか？")
                .setMessage("「" + task.title + "」を削除します。")
                .setPositiveButton("削除", (dialog, which) -> {
                    db.deleteDoTask(task.id);
                    AutoSync.run(this);
                    Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private String mergeNotes(String itemNote, String actualNote) {
        String left = itemNote == null ? "" : itemNote.trim();
        String right = actualNote == null ? "" : actualNote.trim();
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;
        return "項目メモ: " + left + " / 実績メモ: " + right;
    }
}
