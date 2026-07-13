package com.habitgate.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** やること1件の名前・メモ・日付・優先度・期限を編集する画面。 */
public class TaskFormActivity extends ThemedActivity {
    private HabitDb db;
    private Models.Task task;

    private EditText titleEdit;
    private EditText noteEdit;
    private TextView plannedDateText;
    private TextView dueText;
    private String plannedDate;
    private int priority;
    private long dueAt;
    private final Button[] priorityChips = new Button[4];

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
        plannedDate = task.plannedDate;
        priority = task.priority;
        dueAt = task.dueAt;
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = Ui.screen(this);
        root.addView(Ui.title(this, "やることを編集"));

        LinearLayout card = Ui.card(this, root);

        titleEdit = Ui.edit(this, "タスク名");
        titleEdit.setText(task.title);
        card.addView(titleEdit);

        noteEdit = Ui.multilineEdit(this, "メモ（任意）");
        noteEdit.setText(task.note);
        card.addView(noteEdit);

        LinearLayout plannedRow = Ui.horizontal(this);
        plannedDateText = new TextView(this);
        plannedDateText.setTextSize(15);
        plannedDateText.setTypeface(Typeface.DEFAULT_BOLD);
        plannedDateText.setTextColor(Ui.TEXT);
        updatePlannedDateText();
        plannedRow.addView(plannedDateText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button plannedPicker = Ui.iconButton(this, "📅");
        plannedPicker.setContentDescription("やる日を変更");
        plannedPicker.setOnClickListener(v -> openPlannedDatePicker());
        plannedRow.addView(plannedPicker, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(plannedRow);

        LinearLayout priorityRow = Ui.horizontal(this);
        TextView priorityLabel = new TextView(this);
        priorityLabel.setText("優先度: ");
        priorityLabel.setTextSize(15);
        priorityLabel.setTextColor(Ui.TEXT);
        priorityRow.addView(priorityLabel);
        String[] priorityLabels = {"なし", "低", "中", "高"};
        for (int i = 0; i < priorityLabels.length; i++) {
            final int value = i;
            Button chip = Ui.chip(this, priorityLabels[i]);
            priorityChips[i] = chip;
            chip.setOnClickListener(v -> {
                priority = value;
                updatePriorityChips();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            lp.rightMargin = Ui.dp(this, 6);
            priorityRow.addView(chip, lp);
        }
        card.addView(priorityRow);
        updatePriorityChips();

        LinearLayout dueRow = Ui.horizontal(this);
        dueText = new TextView(this);
        dueText.setTextSize(15);
        dueText.setTextColor(Ui.TEXT);
        updateDueText();
        dueRow.addView(dueText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button duePicker = Ui.iconButton(this, "📅");
        duePicker.setContentDescription("期限を設定");
        duePicker.setOnClickListener(v -> openDueDatePicker());
        LinearLayout.LayoutParams duePickerLp = new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT);
        duePickerLp.rightMargin = Ui.dp(this, 6);
        dueRow.addView(duePicker, duePickerLp);
        Button dueClear = Ui.button(this, "クリア");
        dueClear.setOnClickListener(v -> {
            dueAt = 0;
            updateDueText();
        });
        dueRow.addView(dueClear, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(dueRow);

        Ui.space(this, root, 6);
        Button save = Ui.primaryButton(this, "保存");
        save.setOnClickListener(v -> onSave());
        root.addView(save);
    }

    private void updatePlannedDateText() {
        plannedDateText.setText("やる日: " + DateTools.formatShortDateWithWeekday(plannedDate));
    }

    private void updatePriorityChips() {
        for (int i = 0; i < priorityChips.length; i++) {
            Ui.setChipSelected(priorityChips[i], priority == i);
        }
    }

    private void updateDueText() {
        if (dueAt <= 0) {
            dueText.setText("期限: なし");
        } else {
            dueText.setText("期限: " + DateTools.formatDateTime(dueAt));
        }
    }

    private void openPlannedDatePicker() {
        LocalDate initial = DateTools.parseOrToday(plannedDate);
        new DatePickerDialog(this, Ui.pickerTheme(), (view, year, month, dayOfMonth) -> {
            plannedDate = LocalDate.of(year, month + 1, dayOfMonth).format(DateTools.DATE);
            updatePlannedDateText();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void openDueDatePicker() {
        LocalDateTime initial = dueAt > 0
                ? Instant.ofEpochMilli(dueAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
                : LocalDate.parse(plannedDate, DateTools.DATE).atTime(23, 59);
        new DatePickerDialog(this, Ui.pickerTheme(), (view, year, month, dayOfMonth) -> {
            LocalDate date = LocalDate.of(year, month + 1, dayOfMonth);
            openDueTimePicker(date, initial.getHour(), initial.getMinute());
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void openDueTimePicker(LocalDate date, int initialHour, int initialMinute) {
        new TimePickerDialog(this, Ui.pickerTheme(), (view, hourOfDay, minute) -> {
            dueAt = date.atTime(hourOfDay, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            updateDueText();
        }, initialHour, initialMinute, true).show();
    }

    private void onSave() {
        String title = titleEdit.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "タスク名を入力してください", Toast.LENGTH_SHORT).show();
            return;
        }
        db.updateDoTask(task.id, title, noteEdit.getText().toString(), plannedDate, priority, dueAt);
        AutoSync.run(this);
        Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
        finish();
    }
}
