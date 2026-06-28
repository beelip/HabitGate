package com.habitgate.app;

import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class Ui {
    private Ui() {}

    public static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    public static TextView title(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(22);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(context, 18), 0, dp(context, 8));
        return tv;
    }

    public static TextView section(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setPadding(0, dp(context, 16), 0, dp(context, 6));
        return tv;
    }

    public static TextView note(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(14);
        tv.setPadding(0, dp(context, 4), 0, dp(context, 8));
        return tv;
    }

    public static EditText edit(Context context, String hint) {
        EditText et = new EditText(context);
        et.setHint(hint);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        return et;
    }

    public static EditText numberEdit(Context context, String hint, int ems) {
        EditText et = new EditText(context);
        et.setHint(hint);
        et.setEms(ems);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        return et;
    }

    public static Button button(Context context, String text) {
        Button b = new Button(context);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }

    public static LinearLayout vertical(Context context) {
        LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout horizontal(Context context) {
        LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(android.view.Gravity.CENTER_VERTICAL);
        return l;
    }

    public static void addDivider(Context context, LinearLayout parent) {
        View v = new View(context);
        v.setBackgroundColor(0xFFE0E0E0);
        parent.addView(v, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                Math.max(1, dp(context, 1))
        ));
    }
}
