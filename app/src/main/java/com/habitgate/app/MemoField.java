package com.habitgate.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** プレビュー行 + 📝 ボタン + 編集モーダルからなる共通メモ欄。 */
public class MemoField {
    public interface OnSaved {
        void onSaved(String value);
    }

    private final Activity activity;
    private final String dialogTitle;   // モーダルのタイトル（例「実績メモ」「タスクのメモ」）
    private final String hint;          // 空のときのプレビュー表示・入力欄ヒント
    private final String filledPrefix;  // 値があるときのプレビュー接頭辞
    private final int filledColor;      // 値があるときのプレビュー文字色
    private String value;
    private TextView preview;
    private OnSaved onSaved;            // null 可。保存時に呼ぶ

    public MemoField(Activity activity, String dialogTitle, String hint, String filledPrefix,
                     int filledColor, String initialValue) {
        this.activity = activity;
        this.dialogTitle = dialogTitle;
        this.hint = hint;
        this.filledPrefix = filledPrefix;
        this.filledColor = filledColor;
        this.value = initialValue == null ? "" : initialValue;
    }

    public MemoField setOnSaved(OnSaved cb) {
        this.onSaved = cb;
        return this;
    }

    /** 横一列: プレビュー(weight 1) + 📝 ボタン(48dp)。 */
    public LinearLayout view() {
        LinearLayout row = Ui.horizontal(activity);
        preview = new TextView(activity);
        preview.setMaxLines(2);
        preview.setEllipsize(TextUtils.TruncateAt.END);
        preview.setTextSize(14);
        updatePreview();
        preview.setOnClickListener(v -> openDialog());
        row.addView(preview, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button button = Ui.iconButton(activity, "📝");
        button.setContentDescription(dialogTitle + "を編集");
        button.setOnClickListener(v -> openDialog());
        row.addView(button, new LinearLayout.LayoutParams(Ui.dp(activity, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    public String get() {
        return value;
    }

    public void set(String v) {
        value = v == null ? "" : v;
        updatePreview();
    }

    private void updatePreview() {
        if (preview == null) return;
        if (value.isEmpty()) {
            preview.setText(hint);
            preview.setTextColor(Ui.HINT);
        } else {
            preview.setText(filledPrefix + value);
            preview.setTextColor(filledColor);
        }
    }

    /** 入力内容が保存済みの値から変わっているか（trim 同士で比較）。 */
    private boolean changed(EditText input) {
        return !input.getText().toString().trim().equals(value);
    }

    /** 入力内容を値として確定し、プレビュー更新とコールバックを行う。 */
    private void commit(EditText input) {
        value = input.getText().toString().trim();
        updatePreview();
        if (onSaved != null) onSaved.onSaved(value);
    }

    private void openDialog() {
        EditText input = Ui.multilineEdit(activity, hint);
        input.setText(value);
        int pad = Ui.dp(activity, 18);
        LinearLayout wrapper = Ui.vertical(activity);
        wrapper.setPadding(pad, Ui.dp(activity, 6), pad, 0);
        wrapper.addView(input);
        AlertDialog dialog = Ui.dialog(activity)
                .setTitle(dialogTitle)
                .setView(wrapper)
                .setPositiveButton("保存", null)
                .setNegativeButton("キャンセル", null)
                .create();
        dialog.setCanceledOnTouchOutside(true);
        // モーダル外タップ／戻るキー: 変更があれば保存確認（この時点でモーダルは既に閉じている）
        dialog.setOnCancelListener(d -> {
            if (!changed(input)) return;
            Ui.dialog(activity)
                    .setTitle("編集を保存しますか？")
                    .setPositiveButton("保存", (confirm, which) -> commit(input))
                    .setNegativeButton("キャンセル", null)
                    .show();
        });
        dialog.show();
        // 標準ではボタン押下で自動 dismiss されるため、show 後にリスナーを上書きして制御する
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            commit(input);
            dialog.dismiss();
        });
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            if (!changed(input)) {
                dialog.dismiss();
                return;
            }
            Ui.dialog(activity)
                    .setTitle("編集を保存しますか？")
                    .setPositiveButton("はい", (confirm, which) -> {
                        commit(input);
                        dialog.dismiss();
                    })
                    .setNegativeButton("いいえ", (confirm, which) -> dialog.dismiss())
                    .show();
        });
    }
}
