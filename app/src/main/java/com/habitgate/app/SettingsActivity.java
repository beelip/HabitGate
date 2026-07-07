package com.habitgate.app;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

public class SettingsActivity extends android.app.Activity {
    private static final int REQUEST_BACKUP_DIRECTORY = 201;
    private static final int REQUEST_IMPORT_CSV = 202;

    private Button reminderButton;
    private TextView backupInfo;
    private TextView sheetsInfo;
    private TextView usageInfo;

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

        root.addView(Ui.section(this, "通知"));
        LinearLayout notifyCard = Ui.card(this, root);
        reminderButton = Ui.button(this, "通知時刻: " + ReminderScheduler.reminderTime(this));
        reminderButton.setOnClickListener(v -> openTimePicker());
        notifyCard.addView(reminderButton);
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
        Button configureSheets = Ui.button(this, "連携URLを設定");
        configureSheets.setOnClickListener(v -> openWebhookDialog());
        sheetsCard.addView(configureSheets);
        sheetsCard.addView(Ui.note(this, "Google Apps Script の Web App URL を設定すると、未同期データを Google Sheets へ送信します。CSVバックアップとは独立して動作します。"));

        refreshStatus();
    }

    private void refreshStatus() {
        if (backupInfo == null) return;
        backupInfo.setText("自動CSV更新先: " + CsvBackupManager.backupDirectoryLabel(this) + "\nファイル名: " + CsvBackupManager.BACKUP_FILE_NAME);
        String url = ReminderScheduler.webhookUrl(this);
        sheetsInfo.setText(url == null || url.trim().isEmpty() ? "連携URL: 未設定" : "連携URL: 設定済み");
        boolean granted = AppUsage.hasPermission(this);
        usageInfo.setText(granted ? "使用状況へのアクセス: 許可済み ✅" : "使用状況へのアクセス: 未許可");
        usageInfo.setTextColor(granted ? Ui.GOOD : Ui.DANGER);
    }

    private void openTimePicker() {
        int[] current = DateTools.parseTime(ReminderScheduler.reminderTime(this), 22, 30);
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            ReminderScheduler.saveReminderTime(this, hourOfDay, minute);
            reminderButton.setText("通知時刻: " + ReminderScheduler.reminderTime(this));
            Toast.makeText(this, "次回通知を設定しました", Toast.LENGTH_SHORT).show();
        }, current[0], current[1], true).show();
    }

    private void openWebhookDialog() {
        EditText input = Ui.edit(this, "Google Apps Script Web App URL");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(ReminderScheduler.webhookUrl(this));
        int pad = Ui.dp(this, 18);
        LinearLayout wrapper = Ui.vertical(this);
        wrapper.setPadding(pad, Ui.dp(this, 6), pad, 0);
        wrapper.addView(input);
        wrapper.addView(Ui.note(this, "空欄で保存すると連携を解除します。"));

        new AlertDialog.Builder(this)
                .setTitle("スプレッドシート連携URL")
                .setView(wrapper)
                .setPositiveButton("保存", (dialog, which) -> {
                    ReminderScheduler.saveWebhookUrl(this, input.getText().toString());
                    refreshStatus();
                    Toast.makeText(this, "連携URLを保存しました", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("キャンセル", null)
                .show();
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
                refreshStatus();
                Toast.makeText(this, "CSVからインポートしました: " + rows + "行", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "CSVインポートに失敗: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }
}
