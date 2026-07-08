package com.habitgate.app;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

public class SettingsActivity extends ThemedActivity {
    private static final int REQUEST_BACKUP_DIRECTORY = 201;
    private static final int REQUEST_IMPORT_CSV = 202;

    private Button reminderButton;
    private Button autoCloseButton;
    private TextView backupInfo;
    private TextView sheetsInfo;
    private TextView usageInfo;
    private TextView themeInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void buildUi() {
        LinearLayout root = Ui.screen(this);

        root.addView(Ui.title(this, "設定"));

        root.addView(Ui.section(this, "デザイン"));
        LinearLayout designCard = Ui.card(this, root);
        themeInfo = Ui.body(this, "");
        designCard.addView(themeInfo);
        Ui.space(this, designCard, 8);
        Button themeButton = Ui.button(this, "テーマを選択");
        themeButton.setOnClickListener(v -> openThemePicker());
        designCard.addView(themeButton);
        Ui.space(this, designCard, 8);
        boolean followSystem = ReminderScheduler.prefs(this).getBoolean(Themes.KEY_FOLLOW_SYSTEM_DARK, false);
        CheckBox darkModeCheck = new CheckBox(this);
        darkModeCheck.setText("ダークモード");
        darkModeCheck.setTextColor(Ui.TEXT);
        darkModeCheck.setChecked(ReminderScheduler.prefs(this).getBoolean(Themes.KEY_DARK, false));
        darkModeCheck.setEnabled(!followSystem);
        darkModeCheck.setOnCheckedChangeListener((button, isChecked) -> {
            Themes.saveDark(this, isChecked);
            recreate();
        });
        designCard.addView(darkModeCheck);
        CheckBox followSystemCheck = new CheckBox(this);
        followSystemCheck.setText("端末のダークモードと連携する");
        followSystemCheck.setTextColor(Ui.TEXT);
        followSystemCheck.setChecked(followSystem);
        followSystemCheck.setOnCheckedChangeListener((button, isChecked) -> {
            Themes.saveFollowSystemDark(this, isChecked);
            recreate();
        });
        designCard.addView(followSystemCheck);
        designCard.addView(Ui.note(this, "テーマは全画面に反映されます。「端末のダークモードと連携する」を有効にすると、端末側の設定に合わせて自動で切り替わります。"));

        root.addView(Ui.section(this, "通知"));
        LinearLayout notifyCard = Ui.card(this, root);
        reminderButton = Ui.button(this, "通知時刻: " + ReminderScheduler.reminderTime(this));
        reminderButton.setOnClickListener(v -> openTimePicker());
        notifyCard.addView(reminderButton);
        Ui.space(this, notifyCard, 8);
        autoCloseButton = Ui.button(this, "一日の自動終了時刻: " + ReminderScheduler.autoCloseTime(this));
        autoCloseButton.setOnClickListener(v -> openAutoCloseTimePicker());
        notifyCard.addView(autoCloseButton);
        notifyCard.addView(Ui.note(this, "設定時刻を過ぎると、その日のサイクルを自動で終了して次の日へ移ります。"));
        Ui.space(this, notifyCard, 8);
        Button exactAlarmButton = Ui.button(this, "正確なアラームの権限設定");
        exactAlarmButton.setOnClickListener(v -> startActivity(ReminderScheduler.exactAlarmSettingsIntent(this)));
        notifyCard.addView(exactAlarmButton);
        notifyCard.addView(Ui.note(this, "指定時刻に「今日は何をした？」の通知を出します。Android の制限により、画面を完全に前面へ出せない機種があります。"));

        root.addView(Ui.section(this, "アプリ使用時間の計測"));
        LinearLayout usageCard = Ui.card(this, root);
        usageInfo = Ui.body(this, "");
        usageCard.addView(usageInfo);
        Ui.space(this, usageCard, 8);
        Button usageButton = Ui.button(this, "使用状況へのアクセス設定を開く");
        usageButton.setOnClickListener(v -> {
            try {
                startActivity(AppUsage.usageAccessSettingsIntent());
            } catch (Exception e) {
                Toast.makeText(this, "設定画面を開けませんでした", Toast.LENGTH_SHORT).show();
            }
        });
        usageCard.addView(usageButton);
        usageCard.addView(Ui.note(this, "減らすことにアプリ（LINE や Twitter など）を連携すると、その日の使用時間を自動計測して実績に計上します。計測には端末の「使用状況へのアクセス」許可が必要です。"));

        root.addView(Ui.section(this, "CSVバックアップ / 移行"));
        LinearLayout csvCard = Ui.card(this, root);
        backupInfo = Ui.note(this, "");
        csvCard.addView(backupInfo);
        Button export = Ui.button(this, "CSVを出力・共有");
        export.setOnClickListener(v -> exportCsv());
        csvCard.addView(export);
        Ui.space(this, csvCard, 8);
        Button chooseBackupDir = Ui.button(this, "CSV更新先フォルダを選択");
        chooseBackupDir.setOnClickListener(v -> chooseBackupDirectory());
        csvCard.addView(chooseBackupDir);
        Ui.space(this, csvCard, 8);
        Button updateBackup = Ui.button(this, "指定フォルダのCSVを今すぐ更新");
        updateBackup.setOnClickListener(v -> updateConfiguredCsvBackup());
        csvCard.addView(updateBackup);
        Ui.space(this, csvCard, 8);
        Button importCsv = Ui.button(this, "CSVからインポート");
        importCsv.setOnClickListener(v -> openCsvImporter());
        csvCard.addView(importCsv);
        csvCard.addView(Ui.note(this, "インポートは現在のアプリ内データをCSVの内容で置き換えます。移行前にCSV出力しておくと安全です。"));

        root.addView(Ui.section(this, "スプレッドシート連携"));
        LinearLayout sheetsCard = Ui.card(this, root);
        sheetsInfo = Ui.note(this, "");
        sheetsCard.addView(sheetsInfo);
        Button configureSheets = Ui.button(this, "連携先を設定");
        configureSheets.setOnClickListener(v -> openWebhookWizard());
        sheetsCard.addView(configureSheets);
        sheetsCard.addView(Ui.note(this, "Google Apps Script の Web App URL を設定すると、未同期データを Google Sheets へ送信します。CSVバックアップとは独立して動作します。"));

        refreshStatus();
    }

    private void refreshStatus() {
        if (backupInfo == null) return;
        if (themeInfo != null) {
            themeInfo.setText("テーマ: " + Themes.current(this).label);
        }
        backupInfo.setText("自動CSV更新先: " + CsvBackupManager.backupDirectoryLabel(this) + "\nファイル名: " + CsvBackupManager.BACKUP_FILE_NAME);
        String url = ReminderScheduler.webhookUrl(this);
        sheetsInfo.setText(url == null || url.trim().isEmpty() ? "連携URL: 未設定" : "連携URL: 設定済み");
        boolean granted = AppUsage.hasPermission(this);
        usageInfo.setText(granted ? "使用状況へのアクセス: 許可済み ✅" : "使用状況へのアクセス: 未許可");
        usageInfo.setTextColor(granted ? Ui.GOOD : Ui.DANGER);
    }

    private void openThemePicker() {
        List<Themes.Theme> themes = Themes.all();
        String currentId = Themes.current(this).id;

        int pad = Ui.dp(this, 18);
        LinearLayout wrapper = Ui.vertical(this);
        wrapper.setPadding(pad, Ui.dp(this, 6), pad, 0);

        ListView listView = new ListView(this);
        ThemeAdapter adapter = new ThemeAdapter(this, themes, currentId);
        listView.setAdapter(adapter);
        wrapper.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 420)));

        AlertDialog dialog = Ui.dialog(this)
                .setTitle("テーマを選択")
                .setView(wrapper)
                .setNegativeButton("キャンセル", null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Themes.Theme theme = themes.get(position);
            Themes.saveThemeId(this, theme.id);
            dialog.dismiss();
            recreate();
        });

        dialog.show();
    }

    private static class ThemeAdapter extends BaseAdapter {
        private final Context context;
        private final List<Themes.Theme> themes;
        private final String currentId;

        ThemeAdapter(Context context, List<Themes.Theme> themes, String currentId) {
            this.context = context;
            this.themes = themes;
            this.currentId = currentId;
        }

        @Override
        public int getCount() {
            return themes.size();
        }

        @Override
        public Object getItem(int position) {
            return themes.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Themes.Theme theme = themes.get(position);
            Themes.Palette preview = theme.preview();

            LinearLayout row = Ui.horizontal(context);
            int pad = Ui.dp(context, 10);
            row.setPadding(pad, pad, pad, pad);

            View swatch1 = new View(context);
            int swatchSize = Ui.dp(context, 20);
            swatch1.setBackground(Ui.rounded(preview.primary, 0, Ui.dp(context, 10)));
            LinearLayout.LayoutParams swatch1Lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            swatch1Lp.rightMargin = Ui.dp(context, 6);
            row.addView(swatch1, swatch1Lp);

            View swatch2 = new View(context);
            swatch2.setBackground(Ui.rounded(preview.bg, preview.border, Ui.dp(context, 10)));
            LinearLayout.LayoutParams swatch2Lp = new LinearLayout.LayoutParams(swatchSize, swatchSize);
            swatch2Lp.rightMargin = Ui.dp(context, 12);
            row.addView(swatch2, swatch2Lp);

            TextView label = new TextView(context);
            label.setText(theme.label);
            label.setTextSize(15);
            label.setTextColor(Ui.TEXT);
            row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            if (theme.id.equals(currentId)) {
                TextView check = new TextView(context);
                check.setText("✓");
                check.setTextColor(Ui.PRIMARY);
                row.addView(check);
            }

            return row;
        }
    }

    private void openTimePicker() {
        int[] current = DateTools.parseTime(ReminderScheduler.reminderTime(this), 22, 30);
        new TimePickerDialog(this, Ui.pickerTheme(), (view, hourOfDay, minute) -> {
            ReminderScheduler.saveReminderTime(this, hourOfDay, minute);
            reminderButton.setText("通知時刻: " + ReminderScheduler.reminderTime(this));
            Toast.makeText(this, "次回通知を設定しました", Toast.LENGTH_SHORT).show();
        }, current[0], current[1], true).show();
    }

    private void openAutoCloseTimePicker() {
        int[] current = DateTools.parseTime(ReminderScheduler.autoCloseTime(this), 5, 0);
        new TimePickerDialog(this, Ui.pickerTheme(), (view, hourOfDay, minute) -> {
            ReminderScheduler.saveAutoCloseTime(this, hourOfDay, minute);
            autoCloseButton.setText("一日の自動終了時刻: " + ReminderScheduler.autoCloseTime(this));
            Toast.makeText(this, "自動終了時刻を設定しました", Toast.LENGTH_SHORT).show();
        }, current[0], current[1], true).show();
    }

    private void openWebhookWizard() {
        String current = ReminderScheduler.webhookUrl(this);
        boolean configured = current != null && !current.trim().isEmpty();

        int pad = Ui.dp(this, 18);
        LinearLayout wrapper = Ui.vertical(this);
        wrapper.setPadding(pad, Ui.dp(this, 6), pad, 0);

        wrapper.addView(Ui.body(this, configured ? "現在: 連携済み" : "現在: 未設定"));
        wrapper.addView(Ui.note(this, "1. Google スプレッドシートを開き、拡張機能 → Apps Script を開く\n2. tools/google-sheets-webhook.gs の内容を貼り付けてデプロイ（ウェブアプリ、全員がアクセス可）\n3. 発行された URL を下に貼り付けて保存"));

        EditText input = Ui.edit(this, "Google Apps Script Web App URL");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(current);
        wrapper.addView(input);

        AlertDialog.Builder builder = Ui.dialog(this)
                .setTitle("スプレッドシート連携")
                .setView(wrapper)
                .setPositiveButton("保存", (dialog, which) -> {
                    String url = input.getText().toString().trim();
                    if (url.isEmpty()) {
                        ReminderScheduler.saveWebhookUrl(this, "");
                        refreshStatus();
                        Toast.makeText(this, "連携を解除しました", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (!url.startsWith("https://script.google.com/")) {
                        Toast.makeText(this, "Apps Script の Web App URL ではありません", Toast.LENGTH_LONG).show();
                        return;
                    }
                    ReminderScheduler.saveWebhookUrl(this, url);
                    refreshStatus();
                    Toast.makeText(this, "連携先を保存しました", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("キャンセル", null);
        if (configured) {
            builder.setNeutralButton("テスト送信", (dialog, which) -> SheetsSync.syncUnsynced(this, true));
        }
        builder.show();
    }

    private void exportCsv() {
        try {
            String csv = CsvBackupManager.buildBackupCsv(this);
            String fileName = "habit_gate_export_" + LocalDate.now() + ".csv";
            ContentResolver resolver = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("CSV URI could not be created");
            try (OutputStream os = resolver.openOutputStream(uri)) {
                if (os == null) throw new IllegalStateException("CSV output stream could not be opened");
                os.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                os.write(csv.getBytes(StandardCharsets.UTF_8));
            }
            values.clear();
            values.put(MediaStore.Downloads.IS_PENDING, 0);
            resolver.update(uri, values, null, null);

            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/csv");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(share, "CSVを共有"));
        } catch (Exception e) {
            Toast.makeText(this, "CSV出力に失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void chooseBackupDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION | Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_BACKUP_DIRECTORY);
    }

    private void openCsvImporter() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"text/*", "text/csv", "application/csv", "application/vnd.ms-excel", "application/octet-stream"});
        startActivityForResult(intent, REQUEST_IMPORT_CSV);
    }

    private void updateConfiguredCsvBackup() {
        if (!CsvBackupManager.hasBackupDirectory(this)) {
            Toast.makeText(this, "CSV更新先フォルダが未設定です", Toast.LENGTH_LONG).show();
            return;
        }
        try {
            CsvBackupManager.writeBackupToConfiguredDirectory(this);
            Toast.makeText(this, "指定フォルダのCSVを更新しました", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "CSV更新に失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (requestCode == REQUEST_BACKUP_DIRECTORY) {
            try {
                CsvBackupManager.saveBackupDirectory(this, uri, data.getFlags());
                CsvBackupManager.writeBackupToConfiguredDirectory(this);
                refreshStatus();
                Toast.makeText(this, "CSV更新先フォルダを保存しました", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "フォルダ設定に失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_IMPORT_CSV) {
            try {
                int rows = CsvBackupManager.importFromCsvUri(this, uri);
                ReminderScheduler.scheduleNext(this);
                AutoSync.run(this);
                refreshStatus();
                Toast.makeText(this, "CSVからインポートしました: " + rows + "行", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "CSVインポートに失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
