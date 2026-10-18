package com.habitgate.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.Toast;

import java.util.regex.Pattern;

/** プリセットパレットとカラーコード入力による色選択ダイアログ。 */
public final class ColorPickerDialog {
    private ColorPickerDialog() {}

    public interface Callback {
        void onPicked(int color);
    }

    private static final int[] PRESETS = {
            0xFFEF4444, 0xFFF97316, 0xFFF59E0B, 0xFFEAB308, 0xFF84CC16,
            0xFF22C55E, 0xFF10B981, 0xFF14B8A6, 0xFF06B6D4, 0xFF0EA5E9,
            0xFF3B82F6, 0xFF6366F1, 0xFF8B5CF6, 0xFFA855F7, 0xFFD946EF,
            0xFFEC4899, 0xFFF43F5E, 0xFF78716C, 0xFF6B7280, 0xFF475569
    };

    private static final Pattern HEX = Pattern.compile("^#?[0-9a-fA-F]{6}$");

    public static void show(Activity activity, int currentColor, Callback callback) {
        int pad = Ui.dp(activity, 18);
        LinearLayout wrapper = Ui.vertical(activity);
        wrapper.setPadding(pad, pad, pad, pad);

        final AlertDialog[] holder = new AlertDialog[1];

        GridLayout grid = new GridLayout(activity);
        grid.setColumnCount(5);
        int swatchSize = Ui.dp(activity, 36);
        int margin = Ui.dp(activity, 6);
        for (int color : PRESETS) {
            View swatch = new View(activity);
            swatch.setBackground(Ui.rounded(color, Ui.BORDER, Ui.dp(activity, 18)));
            swatch.setOnClickListener(v -> {
                callback.onPicked(color);
                if (holder[0] != null) holder[0].dismiss();
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = swatchSize;
            lp.height = swatchSize;
            lp.setMargins(margin, margin, margin, margin);
            grid.addView(swatch, lp);
        }
        wrapper.addView(grid);

        wrapper.addView(Ui.note(activity, "またはカラーコードを入力"));

        LinearLayout hexRow = Ui.horizontal(activity);
        EditText hexEdit = Ui.edit(activity, "#RRGGBB");
        hexEdit.setText(String.format("#%06X", currentColor & 0xFFFFFF));
        LinearLayout.LayoutParams hexLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        hexLp.rightMargin = Ui.dp(activity, 8);
        hexLp.bottomMargin = 0;
        hexRow.addView(hexEdit, hexLp);
        Button ok = Ui.tonalButton(activity, "決定");
        ok.setOnClickListener(v -> {
            String input = hexEdit.getText().toString().trim();
            if (!HEX.matcher(input).matches()) {
                Toast.makeText(activity, "カラーコードは #RRGGBB 形式で入力してください", Toast.LENGTH_SHORT).show();
                return;
            }
            String digits = input.startsWith("#") ? input.substring(1) : input;
            int parsed = Integer.parseInt(digits, 16);
            callback.onPicked(0xFF000000 | parsed);
            if (holder[0] != null) holder[0].dismiss();
        });
        hexRow.addView(ok, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        wrapper.addView(hexRow);

        holder[0] = Ui.dialog(activity)
                .setTitle("色を選択")
                .setView(wrapper)
                .setNegativeButton("キャンセル", null)
                .create();
        holder[0].show();
    }
}
