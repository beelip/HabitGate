package com.habitgate.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
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
    private String memo = "";
    private String hoursText = "";
    private String minutesText = "";
    private boolean gaugeTouched = false;
    private String gaugeHoursText = "";
    private String gaugeMinutesText = "";

    private EditText hoursEdit;
    private EditText minutesEdit;
    private EditText gaugeHoursEdit;
    private EditText gaugeMinutesEdit;
    private TextView memoPreview;

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
        LinearLayout root = Ui.screen(this);
        root.addView(Ui.title(this, "実績入力"));

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

        LinearLayout duration = durationRow();
        hoursEdit = (EditText) duration.getChildAt(1);
        minutesEdit = (EditText) duration.getChildAt(3);
        inputCard.addView(duration);

        if (item.hasLinkedApp()) {
            if (AppUsage.hasPermission(this)) {
                int measured = AppUsage.foregroundMinutesOn(this, item.appPackage, entryDate);
                hoursEdit.setText(String.valueOf(measured / 60));
                minutesEdit.setText(String.valueOf(measured % 60));
                hoursEdit.setEnabled(false);
                minutesEdit.setEnabled(false);
                hoursEdit.setTextColor(Ui.HINT);
                minutesEdit.setTextColor(Ui.HINT);
                inputCard.addView(Ui.note(this, "アプリ連携中は使用時間を自動計測します"));
            } else {
                hoursEdit.setText("0");
                minutesEdit.setText("0");
                hoursEdit.setEnabled(false);
                minutesEdit.setEnabled(false);
                hoursEdit.setTextColor(Ui.HINT);
                minutesEdit.setTextColor(Ui.HINT);
                inputCard.addView(Ui.note(this, "使用状況へのアクセスが未許可のため計測できません"));
            }
        } else {
            if (!hoursText.isEmpty()) hoursEdit.setText(hoursText);
            if (!minutesText.isEmpty()) minutesEdit.setText(minutesText);
        }

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

        LinearLayout gaugeRow = Ui.horizontal(this);
        gaugeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView gaugeLabel = new TextView(this);
        gaugeLabel.setText("グラフ上限: ");
        gaugeRow.addView(gaugeLabel);
        gaugeHoursEdit = Ui.numberEdit(this, "0", 99);
        gaugeRow.addView(gaugeHoursEdit);
        TextView gaugeHourSuffix = new TextView(this);
        gaugeHourSuffix.setText(" 時間 ");
        gaugeRow.addView(gaugeHourSuffix);
        gaugeMinutesEdit = Ui.numberEdit(this, "0", 59);
        gaugeRow.addView(gaugeMinutesEdit);
        TextView gaugeMinuteSuffix = new TextView(this);
        gaugeMinuteSuffix.setText(" 分");
        gaugeRow.addView(gaugeMinuteSuffix);
        inputCard.addView(gaugeRow);

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

    private void stashInputs() {
        if (!item.hasLinkedApp()) {
            hoursText = hoursEdit.getText().toString();
            minutesText = minutesEdit.getText().toString();
        }
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

        // 連携済みなら計測値、そうでなければ入力値（どちらも hoursEdit/minutesEdit に反映済み）
        int minutes = DateTools.parseMinutes(hoursEdit.getText().toString(), minutesEdit.getText().toString());

        if (minutes == 0 && memo.isEmpty()) {
            AutoSync.run(this);
            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        db.addRecord(HabitDb.CATEGORY_REDUCE, item.title, mergeNotes(item.note, memo), minutes, entryDate);
        AutoSync.run(this);
        Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String mergeNotes(String itemNote, String actualNote) {
        String left = itemNote == null ? "" : itemNote.trim();
        String right = actualNote == null ? "" : actualNote.trim();
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;
        return "項目メモ: " + left + " / 実績メモ: " + right;
    }
}
