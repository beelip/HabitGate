package com.habitgate.app;

import android.content.Context;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** 画面間で共用する入力フォーム部品。 */
public final class Forms {
    private Forms() {}

    /** 時間（時/分）入力行。 */
    public static class DurationInput {
        public final LinearLayout row;
        public final EditText hours;
        public final EditText minutes;

        DurationInput(LinearLayout row, EditText hours, EditText minutes) {
            this.row = row;
            this.hours = hours;
            this.minutes = minutes;
        }
    }

    /** 「<label>[時] 時間 [分] 分」形式の入力行を作る。 */
    public static DurationInput duration(Context context, String labelText) {
        LinearLayout row = Ui.horizontal(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = new TextView(context);
        label.setText(labelText);
        row.addView(label);
        EditText h = Ui.numberEdit(context, "0", 99);
        row.addView(h);
        TextView colon = new TextView(context);
        colon.setText(" 時間 ");
        row.addView(colon);
        EditText m = Ui.numberEdit(context, "0", 59);
        row.addView(m);
        TextView suffix = new TextView(context);
        suffix.setText(" 分");
        row.addView(suffix);
        return new DurationInput(row, h, m);
    }

    /** 見積もり（日/時/分）入力行。 */
    public static class EstimateInput {
        public final LinearLayout row;
        public final EditText days;
        public final EditText hours;
        public final EditText minutes;

        EstimateInput(LinearLayout row, EditText days, EditText hours, EditText minutes) {
            this.row = row;
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
        }

        /** 日*1440 + 時*60 + 分。 */
        public int totalMinutes() {
            return DateTools.parseNonNegative(days.getText().toString()) * 1440
                    + DateTools.parseNonNegative(hours.getText().toString()) * 60
                    + DateTools.parseNonNegative(minutes.getText().toString());
        }
    }

    /** 「<label>[日] 日 [時] 時間 [分] 分」形式の見積もり入力行を作る。現在値（分）を分解してプリセットする。 */
    public static EstimateInput estimate(Context context, String labelText, int currentMinutes) {
        LinearLayout row = Ui.horizontal(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (labelText != null && !labelText.isEmpty()) {
            TextView label = new TextView(context);
            label.setText(labelText);
            label.setTextColor(Ui.TEXT);
            row.addView(label);
        }
        int total = Math.max(0, currentMinutes);
        EditText d = Ui.numberEdit(context, "0", 99);
        EditText h = Ui.numberEdit(context, "0", 23);
        EditText m = Ui.numberEdit(context, "0", 59);
        int w = Ui.dp(context, 56);
        d.setLayoutParams(new LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT));
        h.setLayoutParams(new LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT));
        m.setLayoutParams(new LinearLayout.LayoutParams(w, LinearLayout.LayoutParams.WRAP_CONTENT));
        if (total > 0) {
            d.setText(String.valueOf(Math.min(99, total / 1440)));
            h.setText(String.valueOf((total % 1440) / 60));
            m.setText(String.valueOf(total % 60));
        }
        row.addView(d);
        row.addView(unitLabel(context, "日 "));
        row.addView(h);
        row.addView(unitLabel(context, "時間 "));
        row.addView(m);
        row.addView(unitLabel(context, "分"));
        return new EstimateInput(row, d, h, m);
    }

    private static TextView unitLabel(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextColor(Ui.TEXT);
        return tv;
    }

    /** 項目メモと実績メモの結合。 */
    public static String mergeNotes(String itemNote, String actualNote) {
        String left = itemNote == null ? "" : itemNote.trim();
        String right = actualNote == null ? "" : actualNote.trim();
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;
        return "項目メモ: " + left + " / 実績メモ: " + right;
    }
}
