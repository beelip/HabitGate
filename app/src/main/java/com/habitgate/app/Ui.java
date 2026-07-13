package com.habitgate.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** 依存ライブラリなしで組む、アプリ共通の小さなデザインシステム。 */
public final class Ui {
    public static int BG = 0xFFF3F4F6;
    public static int SURFACE = 0xFFFFFFFF;
    public static int BORDER = 0xFFE5E7EB;
    public static int PRIMARY = 0xFF4F46E5;
    public static int PRIMARY_SOFT = 0xFFEEF2FF;
    public static int TEXT = 0xFF111827;
    public static int MUTED = 0xFF6B7280;
    public static int DANGER = 0xFFDC2626;
    public static int GOOD = 0xFF059669;
    public static int ON_PRIMARY = 0xFFFFFFFF;   // primary 上の文字色
    public static int EDIT_BG = 0xFFF9FAFB;      // 入力欄・サブボタンの背景
    public static int HINT = 0xFF9CA3AF;         // ヒント・無効文字
    public static boolean DARK = false;          // 現在ダークパレットか

    private Ui() {}

    /** テーマパレットを反映する。 */
    public static void applyTheme(Themes.Palette p) {
        BG = p.bg; SURFACE = p.surface; BORDER = p.border;
        PRIMARY = p.primary; PRIMARY_SOFT = p.primarySoft; ON_PRIMARY = p.onPrimary;
        TEXT = p.text; MUTED = p.muted; DANGER = p.danger; GOOD = p.good;
        EDIT_BG = p.editBg; HINT = p.hint; DARK = p.dark;
    }

    /** テーマに追従したダイアログビルダー。 */
    public static AlertDialog.Builder dialog(Context context) {
        return new AlertDialog.Builder(context, pickerTheme());
    }

    public static int pickerTheme() {
        return DARK ? android.R.style.Theme_Material_Dialog_Alert
                : android.R.style.Theme_Material_Light_Dialog_Alert;
    }

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

        activity.getWindow().setBackgroundDrawable(new ColorDrawable(BG));
        activity.getWindow().setStatusBarColor(BG);
        activity.getWindow().setNavigationBarColor(BG);
        View decor = activity.getWindow().getDecorView();
        int flags = decor.getSystemUiVisibility();
        if (!DARK) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            flags &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        }
        decor.setSystemUiVisibility(flags);

        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
                top = bars.top;
                bottom = bars.bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(0, top, 0, bottom);
            return insets;
        });
        scroll.requestApplyInsets();

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
        b.setTextColor(ON_PRIMARY);
        b.setBackground(ripple(rounded(PRIMARY, 0, dp(context, 12)), 0x33FFFFFF));
        return b;
    }

    /** 淡色のサブボタン。 */
    public static Button button(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTextColor(TEXT);
        b.setBackground(ripple(rounded(EDIT_BG, BORDER, dp(context, 12)), DARK ? 0x24FFFFFF : 0x14000000));
        return b;
    }

    /** プライマリ淡色（トーナル）ボタン。 */
    public static Button tonalButton(Context context, String text) {
        Button b = baseButton(context, text);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setTextColor(PRIMARY);
        b.setBackground(ripple(rounded(PRIMARY_SOFT, 0, dp(context, 12)), (PRIMARY & 0x00FFFFFF) | 0x22000000));
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
        b.setBackground(ripple(rounded(EDIT_BG, BORDER, dp(context, 12)), DARK ? 0x24FFFFFF : 0x14000000));
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
            chip.setTextColor(ON_PRIMARY);
            chip.setTypeface(Typeface.DEFAULT_BOLD);
            chip.setBackground(ripple(rounded(PRIMARY, 0, dp(context, 19)), 0x33FFFFFF));
        } else {
            chip.setTextColor(TEXT);
            chip.setTypeface(Typeface.DEFAULT);
            chip.setBackground(ripple(rounded(SURFACE, BORDER, dp(context, 19)), DARK ? 0x24FFFFFF : 0x14000000));
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

    public static EditText numberEdit(Context context, String hint, int maxValue) {
        EditText et = new EditText(context);
        et.setHint(hint);
        et.setSingleLine(true);
        // 全角数字の入力も受け付けるため TEXT にし、TextWatcher で半角数字のみに正規化する
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setTextSize(15);
        et.setTextColor(TEXT);
        et.setHintTextColor(HINT);
        et.setBackground(rounded(EDIT_BG, BORDER, dp(context, 10)));
        int padH = dp(context, 12);
        int padV = dp(context, 10);
        et.setPadding(padH, padV, padH, padV);
        et.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(context, 72), LinearLayout.LayoutParams.WRAP_CONTENT);
        et.setLayoutParams(lp);
        et.addTextChangedListener(new TextWatcher() {
            private boolean editing;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (editing) return;
                String raw = s.toString();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < raw.length() && sb.length() < 4; i++) {
                    char ch = raw.charAt(i);
                    if (ch >= '0' && ch <= '9') sb.append(ch);
                    else if (ch >= '０' && ch <= '９') sb.append((char) ('0' + (ch - '０')));
                }
                String cleaned = sb.toString();
                if (!cleaned.isEmpty()) {
                    try {
                        if (Integer.parseInt(cleaned) > maxValue) cleaned = String.valueOf(maxValue);
                    } catch (NumberFormatException e) {
                        cleaned = "";
                    }
                }
                if (!cleaned.equals(raw)) {
                    editing = true;
                    s.replace(0, s.length(), cleaned);
                    editing = false;
                }
            }
        });
        return et;
    }

    public static EditText multilineEdit(Context context, String hint) {
        EditText et = new EditText(context);
        et.setHint(hint);
        et.setSingleLine(false);
        et.setMinLines(4);
        et.setGravity(Gravity.TOP);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        style(et);
        return et;
    }

    /** 行タップのリップルを付ける。 */
    public static void tappable(View v) {
        v.setClickable(true);
        v.setBackground(new RippleDrawable(
                ColorStateList.valueOf(DARK ? 0x24FFFFFF : 0x14000000), null,
                new ColorDrawable(0xFFFFFFFF)));
    }

    /** 優先度に応じた行の背景色。0 なら 0（色なし）。ダークテーマでは暗色トーン。 */
    public static int priorityTint(int priority) {
        if (priority == 3) return DARK ? 0xFF42201E : 0xFFFEE2E2; // 高=淡い赤
        if (priority == 2) return DARK ? 0xFF3E3712 : 0xFFFEF9C3; // 中=淡い黄
        if (priority == 1) return DARK ? 0xFF1B3524 : 0xFFDCFCE7; // 低=淡い緑
        return 0;
    }

    /** タップ可能な行。fillColor が 0 以外なら角丸の塗り背景 + リップル。0 なら従来の透明リップル。 */
    public static void tappableRow(View v, int fillColor) {
        v.setClickable(true);
        if (fillColor == 0) {
            tappable(v);
            return;
        }
        v.setBackground(new RippleDrawable(
                ColorStateList.valueOf(DARK ? 0x24FFFFFF : 0x14000000),
                rounded(fillColor, 0, dp(v.getContext(), 10)), null));
    }

    private static void style(EditText et) {
        Context context = et.getContext();
        et.setTextSize(15);
        et.setTextColor(TEXT);
        et.setHintTextColor(HINT);
        et.setBackground(rounded(EDIT_BG, BORDER, dp(context, 10)));
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
        v.setBackgroundColor(BG);
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
