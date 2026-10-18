package com.habitgate.app;

import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;

/** 実績記録1件の編集・削除を行う画面。 */
public class RecordEditActivity extends ThemedActivity {
    private HabitDb db;
    private Models.Record record;
    private String entryDate;
    private String hoursText = "";
    private String minutesText = "";

    private EditText hoursEdit;
    private EditText minutesEdit;
    private MemoField recordMemo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        long id = getIntent().getLongExtra("record_id", -1);
        record = db.getRecord(id);
        if (record == null) {
            Toast.makeText(this, "記録が見つかりませんでした", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        entryDate = record.actualDate;
        buildUi();
    }

    private void buildUi() {
        String prevMemo = recordMemo == null ? record.note : recordMemo.get();
        LinearLayout root = Ui.screen(this);
        root.addView(Ui.title(this, "記録の編集"));

        // 記録情報
        LinearLayout infoCard = Ui.card(this, root);
        TextView categoryText = new TextView(this);
        categoryText.setText(HabitDb.CATEGORY_DO.equals(record.category) ? "やること" : "減らすこと");
        categoryText.setTextSize(13);
        categoryText.setTextColor(Ui.MUTED);
        infoCard.addView(categoryText);
        TextView titleText = new TextView(this);
        titleText.setText(record.title);
        titleText.setTextSize(17);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(Ui.TEXT);
        infoCard.addView(titleText);

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

        Forms.DurationInput duration = Forms.duration(this, "時間: ");
        hoursEdit = duration.hours;
        minutesEdit = duration.minutes;
        if (!hoursText.isEmpty()) {
            hoursEdit.setText(hoursText);
        } else {
            hoursEdit.setText(String.valueOf(record.durationMinutes / 60));
        }
        if (!minutesText.isEmpty()) {
            minutesEdit.setText(minutesText);
        } else {
            minutesEdit.setText(String.valueOf(record.durationMinutes % 60));
        }
        inputCard.addView(duration.row);

        recordMemo = new MemoField(this, "実績メモ", "実績メモ（任意）", "", Ui.TEXT, prevMemo);
        inputCard.addView(recordMemo.view());

        Ui.space(this, root, 6);
        Button save = Ui.primaryButton(this, "保存");
        save.setOnClickListener(v -> onSave());
        root.addView(save);

        Ui.space(this, root, 16);
        Button delete = Ui.button(this, "🗑 この記録を削除");
        delete.setTextColor(Ui.DANGER);
        delete.setOnClickListener(v -> confirmDelete());
        root.addView(delete);
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

    private void onSave() {
        int minutes = DateTools.parseMinutes(hoursEdit.getText().toString(), minutesEdit.getText().toString());
        db.updateRecord(record.id, minutes, recordMemo.get(), entryDate);
        AutoSync.run(this);
        Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void confirmDelete() {
        int minutes = DateTools.parseMinutes(hoursEdit.getText().toString(), minutesEdit.getText().toString());
        Ui.dialog(this)
                .setTitle("記録を削除しますか？")
                .setMessage(record.title + " / " + DateTools.formatMinutes(minutes))
                .setPositiveButton("削除", (dialog, which) -> {
                    db.deleteRecord(record.id);
                    AutoSync.run(this);
                    Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }
}
