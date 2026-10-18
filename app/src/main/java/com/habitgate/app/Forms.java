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

    /** 項目メモと実績メモの結合。 */
    public static String mergeNotes(String itemNote, String actualNote) {
        String left = itemNote == null ? "" : itemNote.trim();
        String right = actualNote == null ? "" : actualNote.trim();
        if (left.isEmpty()) return right;
        if (right.isEmpty()) return left;
        return "項目メモ: " + left + " / 実績メモ: " + right;
    }
}
