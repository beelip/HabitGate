package com.habitgate.app;

import android.app.Activity;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 依存ライブラリなしで組む、アプリ共通の小さなデザインシステム。 */
public final class Ui {
    public static final int BG = 0xFFF3F4F6;
    public static final int SURFACE = 0xFFFFFFFF;
    public static final int BORDER = 0xFFE5E7EB;
    public static final int PRIMARY = 0xFF4F46E5;
    public static final int PRIMARY_SOFT = 0xFFEEF2FF;
    public static final int TEXT = 0xFF111827;
    public static final int MUTED = 0xFF6B7280;
    public static final int DANGER = 0xFFDC2626;
    public static final int GOOD = 0xFF059669;

    private Ui() {}

    public static int dp(Context context, int dp) {
        return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }

    /** 画面共通のスクロール + ルート縦レイアウトを作り、Activity にセットする。 */
    public static LinearLayout screen(Activity activity) {
        ScrollView scroll = new ScrollView(activity);
        scroll.setBackgroundColor(BG);
        scroll.setFillViewport(true);
        LinearLayout root = vertical(activity);
        int pad = dp(activity, 16);
        root.setPadding(pad, dp(activity, 12), pad, dp(activity, 40));
        scroll.addView(root);
        activity.setContentView(scroll);
        return root;
    }

    public static TextView title(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(22);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(TEXT);
        tv.setGravity(Gravity.CENTER_HORIZONTAL);
        tv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        tv.setPadding(0, dp(context, 10), 0, dp(context, 12));
        return tv;
    }

    public static TextView section(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        tv.setTextColor(MUTED);
        tv.setLetterSpacing(0.05f);
        tv.setPadding(dp(context, 4), dp(context, 14), 0, dp(context, 6));
        return tv;
    }

    public static TextView note(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(MUTED);
        tv.setPadding(0, dp(context, 4), 0, dp(context, 6));
        return tv;
    }

    public static TextView body(Context context, String text) {
        TextView tv = new TextView(context);
        tv.setText(text);
        tv.setTextSize(15);
        tv.setTextColor(TEXT);
        return tv;
    }

    /** 白背景・角丸・枠線のカードを親に追加して返す。 */
    public static LinearLayout card(Context context, LinearLayout parent) {
        LinearLayout card = vertical(context);
        card.setBackground(rounded(SURFACE, BORDER, dp(context, 16)));
        int pad = dp(context, 14);
        card.setPadding(pad, pad, pad, pad);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(context, 10);
        parent.addView(card, lp);
        return card;
    }

    /** 塗りつぶしのメインボタン。 */
    public static Button primaryButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(0xFFFFFFFF);
        b.setBackground(ripple(rounded(PRIMARY, 0, dp(context, 12)), 0x33FFFFFF));
        return b;
    }

    /** 淡色のサブボタン。 */
    public static Button button(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTextColor(TEXT);
        b.setBackground(ripple(rounded(0xFFF9FAFB, BORDER, dp(context, 12)), 0x14000000));
        return b;
    }

    /** プライマリ淡色（トーナル）ボタン。 */
    public static Button tonalButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(PRIMARY);
        b.setBackground(ripple(rounded(PRIMARY_SOFT, 0, dp(context, 12)), 0x224F46E5));
        return b;
    }

    /** 絵文字などを載せる小さめの正方形ボタン。 */
    public static Button iconButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setMinHeight(dp(context, 40));
        b.setMinimumHeight(dp(context, 40));
        b.setMinWidth(dp(context, 44));
        b.setMinimumWidth(dp(context, 44));
        b.setPadding(0, 0, 0, 0);
        b.setTextColor(TEXT);
        b.setBackground(ripple(rounded(0xFFF9FAFB, BORDER, dp(context, 12)), 0x14000000));
        return b;
    }

    private static Button baseButton(Context context, String text) {
        Button b = new Button(context);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(15);
        b.setStateListAnimator(null);
        b.setMinHeight(dp(context, 46));
        b.setMinimumHeight(dp(context, 46));
        b.setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8));
        return b;
    }

    /** 期間切り替えなどのピル型トグル。setChipSelected で状態を切り替える。 */
    public static Button chip(Context context, String text) {
        Button b = new Button(context);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setStateListAnimator(null);
        b.setMinHeight(dp(context, 38));
        b.setMinimumHeight(dp(context, 38));
        b.setPadding(dp(context, 12), 0, dp(context, 12), 0);
        setChipSelected(b, false);
        return b;
    }

    public static void setChipSelected(Button chip, boolean selected) {
        Context context = chip.getContext();
        if (selected) {
            chip.setTextColor(0xFFFFFFFF);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setBackground(ripple(rounded(PRIMARY, 0, dp(context, 19)), 0x33FFFFFF));
        } else {
            chip.setTextColor(TEXT);
            chip.setTypeface(Typeface.DEFAULT);
            chip.setBackground(ripple(rounded(SURFACE, BORDER, dp(context, 19)), 0x14000000));
        }
    }

    public static EditText edit(Context context, String hint) {
        EditText et = new EditText(context);
        et.setHint(hint);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        style(et);
        return et;
    }

    public static EditText numberEdit(Context context, String hint, int ems) {
        EditText et = new EditText(context);
        et.setHint(hint);
        et.setEms(ems);
        et.setSingleLine(true);
        et.setInputType(InputType.TYPE_CLASS_NUMBER);
        style(et);
        return et;
    }

    private static void style(EditText et) {
        Context context = et.getContext();
        et.setTextSize(15);
        et.setTextColor(TEXT);
        et.setHintTextColor(0xFF9CA3AF);
        et.setBackground(rounded(0xFFF9FAFB, BORDER, dp(context, 10)));
        int padH = dp(context, 12);
        int padV = dp(context, 10);
        et.setPadding(padH, padV, padH, padV);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(context, 8);
        et.setLayoutParams(lp);
    }

    public static LinearLayout vertical(Context context) {
        LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout horizontal(Context context) {
        LinearLayout l = new LinearLayout(context);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static void addDivider(Context context, LinearLayout parent) {
        View v = new View(context);
        v.setBackgroundColor(0xFFF3F4F6);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(context, 1)));
        lp.topMargin = dp(context, 8);
        lp.bottomMargin = dp(context, 8);
        parent.addView(v, lp);
    }

    public static void space(Context context, LinearLayout parent, int heightDp) {
        View v = new View(context);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(context, heightDp)));
    }

    public static GradientDrawable rounded(int fillColor, int strokeColor, float radiusPx) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fillColor);
        if (strokeColor != 0) bg.setStroke(1, strokeColor);
        bg.setCornerRadius(radiusPx);
        return bg;
    }

    private static RippleDrawable ripple(GradientDrawable content, int rippleColor) {
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, null);
    }
}
