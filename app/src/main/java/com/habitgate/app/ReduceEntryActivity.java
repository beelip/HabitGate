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

/** 減らすこと項目1件の実績入力・編集を行う画面。 */
public class ReduceEntryActivity extends ThemedActivity {
    private HabitDb db;
    private Models.ReduceItem item;
    private String entryDate;
    private String hoursText = "";
    private String minutesText = "";
    private boolean gaugeTouched = false;
    private String gaugeHoursText = "";
    private String gaugeMinutesText = "";

    private EditText hoursEdit;
    private EditText minutesEdit;
    private EditText gaugeHoursEdit;
    private EditText gaugeMinutesEdit;
    private MemoField recordMemo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        long id = getIntent().getLongExtra("item_id", -1);
        item = db.getReduceItem(id);
        if (item == null) {
            Toast.makeText(this, "項目が見つかりませんでした", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        entryDate = db.getCurrentCycle().cycleDate;
        buildUi();
    }

    private void buildUi() {
        String prevMemo = recordMemo == null ? "" : recordMemo.get();
        LinearLayout root = Ui.screen(this);
        root.addView(Ui.titleWithBack(this, "実績入力"));

        // 項目情報
        LinearLayout infoCard = Ui.card(this, root);
        TextView titleText = new TextView(this);
        titleText.setText(item.title);
        titleText.setTextSize(17);
        titleText.setTypeface(Typeface.DEFAULT_BOLD);
        titleText.setTextColor(Ui.TEXT);
        infoCard.addView(titleText);
        if (!item.note.isEmpty()) {
            infoCard.addView(Ui.note(this, "メモ: " + item.note));
        }

        LinearLayout linkRow = Ui.horizontal(this);
        TextView linkText = new TextView(this);
        linkText.setText(item.hasLinkedApp() ? "📱 " + AppUsage.appLabel(this, item.appPackage) : "連携アプリ: なし");
        linkText.setTextSize(14);
        linkText.setTextColor(Ui.MUTED);
        linkRow.addView(linkText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button changeLink = Ui.tonalButton(this, "変更");
        changeLink.setOnClickListener(v -> openAppPicker());
        linkRow.addView(changeLink, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        infoCard.addView(linkRow);

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

        if (item.hasLinkedApp()) {
            TextView measuredText = new TextView(this);
            measuredText.setTextSize(15);
            if (AppUsage.hasPermission(this)) {
                int measured = AppUsage.foregroundMinutesOn(this, item.appPackage, entryDate);
                measuredText.setText("📱 計測: " + DateTools.formatMinutes(measured) + "（自動保存されます）");
                measuredText.setTextColor(Ui.PRIMARY);
            } else {
                measuredText.setText("📱 計測: 権限がないため計測できません");
                measuredText.setTextColor(Ui.DANGER);
            }
            inputCard.addView(measuredText);

            Forms.DurationInput duration = Forms.duration(this, "手動追加: ");
            hoursEdit = duration.hours;
            minutesEdit = duration.minutes;
            inputCard.addView(duration.row);
            if (!hoursText.isEmpty()) hoursEdit.setText(hoursText);
            if (!minutesText.isEmpty()) minutesEdit.setText(minutesText);
        } else {
            Forms.DurationInput duration = Forms.duration(this, "時間: ");
            hoursEdit = duration.hours;
            minutesEdit = duration.minutes;
            inputCard.addView(duration.row);
            if (!hoursText.isEmpty()) hoursEdit.setText(hoursText);
            if (!minutesText.isEmpty()) minutesEdit.setText(minutesText);
        }

        recordMemo = new MemoField(this, "実績メモ", "実績メモ（任意）", "", Ui.TEXT, prevMemo);
        inputCard.addView(recordMemo.view());

        Forms.DurationInput gauge = Forms.duration(this, "グラフ上限: ");
        gaugeHoursEdit = gauge.hours;
        gaugeMinutesEdit = gauge.minutes;
        inputCard.addView(gauge.row);

        if (gaugeTouched) {
            if (!gaugeHoursText.isEmpty()) gaugeHoursEdit.setText(gaugeHoursText);
            if (!gaugeMinutesText.isEmpty()) gaugeMinutesEdit.setText(gaugeMinutesText);
        } else {
            gaugeHoursEdit.setText(String.valueOf(item.gaugeMaxMinutes / 60));
            gaugeMinutesEdit.setText(String.valueOf(item.gaugeMaxMinutes % 60));
        }

        Ui.space(this, root, 6);
        Button save = Ui.primaryButton(this, "保存");
        save.setOnClickListener(v -> onSave());
        root.addView(save);
    }

    private void stashInputs() {
        hoursText = hoursEdit.getText().toString();
        minutesText = minutesEdit.getText().toString();
        gaugeHoursText = gaugeHoursEdit.getText().toString();
        gaugeMinutesText = gaugeMinutesEdit.getText().toString();
        gaugeTouched = true;
    }

    private void openDatePicker() {
        stashInputs();
        LocalDate initial = DateTools.parseOrToday(entryDate);
        new DatePickerDialog(this, Ui.pickerTheme(), (view, year, month, dayOfMonth) -> {
            entryDate = LocalDate.of(year, month + 1, dayOfMonth).format(DateTools.DATE);
            buildUi();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void openAppPicker() {
        stashInputs();
        AppPickerDialog.show(this, item.appPackage, packageName -> {
            db.setReduceItemAppPackage(item.id, packageName);
            AutoSync.run(this);
            item = db.getReduceItem(item.id);
            buildUi();
        });
    }

    private void onSave() {
        int gaugeMinutes = DateTools.parseMinutes(gaugeHoursEdit.getText().toString(), gaugeMinutesEdit.getText().toString());
        if (gaugeMinutes <= 0) gaugeMinutes = 480;
        if (gaugeMinutes != item.gaugeMaxMinutes) {
            db.setReduceItemGaugeMax(item.id, gaugeMinutes);
        }

        if (item.hasLinkedApp()) {
            // 計測値は自動計測（一日の終了時）で保存されるため、ここでは手動追加分のみを保存する。
            int manualMinutes = DateTools.parseMinutes(hoursEdit.getText().toString(), minutesEdit.getText().toString());
            if (manualMinutes > 0 || !recordMemo.get().isEmpty()) {
                db.addRecord(HabitDb.CATEGORY_REDUCE, item.title,
                        Forms.mergeNotes(item.note, recordMemo.get().isEmpty() ? "手動追加" : recordMemo.get()), manualMinutes, entryDate);
            }
        } else {
            int minutes = DateTools.parseMinutes(hoursEdit.getText().toString(), minutesEdit.getText().toString());
            if (minutes > 0 || !recordMemo.get().isEmpty()) {
                db.addRecord(HabitDb.CATEGORY_REDUCE, item.title, Forms.mergeNotes(item.note, recordMemo.get()), minutes, entryDate);
            }
        }

        AutoSync.run(this);
        Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
        finish();
    }
}
