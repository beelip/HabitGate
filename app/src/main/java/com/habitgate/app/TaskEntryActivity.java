package com.habitgate.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
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

import java.time.LocalDate;

/** やることタスク1件の実績入力・完了・削除を行う画面。 */
public class TaskEntryActivity extends android.app.Activity {
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
        infoCard.addView(Ui.note(this, "予定日: " + DateTools.formatShortDateWithWeekday(task.plannedDate)));
        if (!task.note.isEmpty()) {
            infoCard.addView(Ui.note(this, "メモ: " + task.note));
        }

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

        completeCheck = new CheckBox(this);
        completeCheck.setText("完了する");
        inputCard.addView(completeCheck);

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

    private void updateMemoPreview() {
        if (memo.isEmpty()) {
            memoPreview.setText("実績メモ（任意）");
            memoPreview.setTextColor(0xFF9CA3AF);
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
        new AlertDialog.Builder(this)
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
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            entryDate = LocalDate.of(year, month + 1, dayOfMonth).format(DateTools.DATE);
            buildUi();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
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
        new AlertDialog.Builder(this)
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

    private void confirmDelete() {
        new AlertDialog.Builder(this)
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
