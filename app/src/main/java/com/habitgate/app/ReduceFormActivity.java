package com.habitgate.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** 減らすこと1件の名前・メモ・連携アプリを編集する画面。 */
public class ReduceFormActivity extends ThemedActivity {
    private HabitDb db;
    private Models.ReduceItem item;

    private EditText titleEdit;
    private EditText noteEdit;
    private TextView linkText;

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
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = Ui.screen(this);
        root.addView(Ui.title(this, "減らすことを編集"));

        LinearLayout card = Ui.card(this, root);

        titleEdit = Ui.edit(this, "項目名");
        titleEdit.setText(item.title);
        card.addView(titleEdit);

        noteEdit = Ui.multilineEdit(this, "メモ（任意）");
        noteEdit.setText(item.note);
        card.addView(noteEdit);

        LinearLayout linkRow = Ui.horizontal(this);
        linkText = new TextView(this);
        linkText.setTextSize(14);
        linkText.setTextColor(Ui.MUTED);
        updateLinkText();
        linkRow.addView(linkText, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button changeLink = Ui.tonalButton(this, "変更");
        changeLink.setOnClickListener(v -> openAppPicker());
        linkRow.addView(changeLink, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(linkRow);

        Ui.space(this, root, 6);
        Button save = Ui.primaryButton(this, "保存");
        save.setOnClickListener(v -> onSave());
        root.addView(save);
    }

    private void updateLinkText() {
        linkText.setText(item.hasLinkedApp() ? "📱 " + AppUsage.appLabel(this, item.appPackage) : "連携アプリ: なし");
    }

    private void openAppPicker() {
        AppPickerDialog.show(this, item.appPackage, packageName -> {
            db.setReduceItemAppPackage(item.id, packageName);
            AutoSync.run(this);
            item = db.getReduceItem(item.id);
            updateLinkText();
            if (!packageName.isEmpty() && !AppUsage.hasPermission(this)) {
                promptUsageAccess(AppUsage.appLabel(this, packageName));
            }
        });
    }

    private void promptUsageAccess(String appLabel) {
        Ui.dialog(this)
                .setTitle("使用状況へのアクセスが必要です")
                .setMessage(appLabel + " の使用時間を自動計測するには、設定で HabitGate に「使用状況へのアクセス」を許可してください。")
                .setPositiveButton("設定を開く", (dialog, which) -> {
                    try {
                        startActivity(AppUsage.usageAccessSettingsIntent());
                    } catch (Exception e) {
                        Toast.makeText(this, "設定画面を開けませんでした", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("あとで", null)
                .show();
    }

    private void onSave() {
        String title = titleEdit.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "項目名を入力してください", Toast.LENGTH_SHORT).show();
            return;
        }
        db.updateReduceItem(item.id, title, noteEdit.getText().toString());
        AutoSync.run(this);
        Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
        finish();
    }
}
