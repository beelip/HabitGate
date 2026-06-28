package com.habitgate.app;

import android.Manifest;
import android.app.AlarmManager;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MainActivity extends android.app.Activity {
    private HabitDb db;
    private LinearLayout doList;
    private LinearLayout reduceList;
    private Button reminderButton;
    private EditText webhookEdit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        ReminderScheduler.createNotificationChannel(this);
        requestNotificationPermissionIfNeeded();
        ReminderScheduler.scheduleNext(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLists();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = Ui.vertical(this);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 36));
        scroll.addView(root);

        root.addView(Ui.title(this, "HabitGate"));
        root.addView(Ui.note(this, "指定時刻に『今日は何をした？』を出し、やること／減らすことを記録します。"));

        root.addView(Ui.section(this, "通知時刻"));
        reminderButton = Ui.button(this, "通知時刻: " + ReminderScheduler.reminderTime(this));
        reminderButton.setOnClickListener(v -> openTimePicker());
        root.addView(reminderButton);

        Button exactAlarmButton = Ui.button(this, "正確なアラーム権限を開く");
        exactAlarmButton.setOnClickListener(v -> startActivity(ReminderScheduler.exactAlarmSettingsIntent(this)));
        root.addView(exactAlarmButton);
        root.addView(Ui.note(this, "Android の制限により、画面を完全に前面へ出せない機種があります。その場合は高優先度通知から入力画面を開いてください。"));

        root.addView(Ui.section(this, "明日のやることを追加"));
        LinearLayout addDoRow = Ui.horizontal(this);
        EditText addDoEdit = Ui.edit(this, "例: 30分走る / PM過去問1問");
        addDoRow.addView(addDoEdit, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button addDoButton = Ui.button(this, "追加");
        addDoRow.addView(addDoButton);
        root.addView(addDoRow);
        addDoButton.setOnClickListener(v -> {
            String title = addDoEdit.getText().toString().trim();
            if (title.isEmpty()) return;
            db.addDoTask(title, DateTools.tomorrow());
            addDoEdit.setText("");
            refreshLists();
        });

        doList = Ui.vertical(this);
        root.addView(doList);

        root.addView(Ui.section(this, "減らすことを追加"));
        LinearLayout addReduceRow = Ui.horizontal(this);
        EditText addReduceEdit = Ui.edit(this, "例: Twitter / 夜更かし / 食べ過ぎ");
        addReduceRow.addView(addReduceEdit, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Button addReduceButton = Ui.button(this, "追加");
        addReduceRow.addView(addReduceButton);
        root.addView(addReduceRow);
        addReduceButton.setOnClickListener(v -> {
            String title = addReduceEdit.getText().toString().trim();
            if (title.isEmpty()) return;
            db.addReduceItem(title);
            addReduceEdit.setText("");
            refreshLists();
        });

        reduceList = Ui.vertical(this);
        root.addView(reduceList);

        root.addView(Ui.section(this, "スプレッドシート連携"));
        webhookEdit = Ui.edit(this, "Google Apps Script Web App URL");
        webhookEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        webhookEdit.setText(ReminderScheduler.webhookUrl(this));
        root.addView(webhookEdit);
        Button saveWebhook = Ui.button(this, "連携URLを保存");
        saveWebhook.setOnClickListener(v -> {
            ReminderScheduler.saveWebhookUrl(this, webhookEdit.getText().toString());
            Toast.makeText(this, "保存しました", Toast.LENGTH_SHORT).show();
        });
        root.addView(saveWebhook);
        root.addView(Ui.note(this, "URLを入れると、記録保存時に未同期データを Google Sheets に送信します。設定前でもCSV出力できます。"));

        root.addView(Ui.section(this, "操作"));
        Button checkIn = Ui.button(this, "今日の入力を開く");
        checkIn.setOnClickListener(v -> startActivity(new Intent(this, CheckInActivity.class)));
        root.addView(checkIn);

        Button stats = Ui.button(this, "集計を見る");
        stats.setOnClickListener(v -> startActivity(new Intent(this, StatsActivity.class)));
        root.addView(stats);

        Button export = Ui.button(this, "CSVを出力・共有");
        export.setOnClickListener(v -> exportCsv());
        root.addView(export);

        setContentView(scroll);
        refreshLists();
    }

    private void openTimePicker() {
        int[] current = DateTools.parseTime(ReminderScheduler.reminderTime(this), 22, 30);
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            ReminderScheduler.saveReminderTime(this, hourOfDay, minute);
            reminderButton.setText("通知時刻: " + ReminderScheduler.reminderTime(this));
            Toast.makeText(this, "次回通知を設定しました", Toast.LENGTH_SHORT).show();
        }, current[0], current[1], true).show();
    }

    private void refreshLists() {
        if (doList == null || reduceList == null) return;
        doList.removeAllViews();
        List<Models.Task> tasks = db.getActiveDoTasks();
        if (tasks.isEmpty()) {
            doList.addView(Ui.note(this, "未登録です。"));
        } else {
            for (Models.Task t : tasks) {
                LinearLayout row = Ui.horizontal(this);
                TextView tv = new TextView(this);
                tv.setText(t.plannedDate + "  " + t.title);
                tv.setTextSize(15);
                row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                Button del = Ui.button(this, "削除");
                del.setOnClickListener(v -> { db.deleteDoTask(t.id); refreshLists(); });
                row.addView(del);
                doList.addView(row);
                Ui.addDivider(this, doList);
            }
        }

        reduceList.removeAllViews();
        List<Models.ReduceItem> items = db.getActiveReduceItems();
        if (items.isEmpty()) {
            reduceList.addView(Ui.note(this, "未登録です。"));
        } else {
            for (Models.ReduceItem item : items) {
                LinearLayout row = Ui.horizontal(this);
                TextView tv = new TextView(this);
                tv.setText(item.title);
                tv.setTextSize(15);
                row.addView(tv, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                Button del = Ui.button(this, "削除");
                del.setOnClickListener(v -> { db.deleteReduceItem(item.id); refreshLists(); });
                row.addView(del);
                reduceList.addView(row);
                Ui.addDivider(this, reduceList);
            }
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void exportCsv() {
        try {
            List<Models.Record> records = db.getAllRecords();
            StringBuilder sb = new StringBuilder();
            sb.append("category,title,duration_minutes,duration_hhmm,actual_date,created_at,synced\n");
            DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            for (Models.Record r : records) {
                String created = Instant.ofEpochMilli(r.createdAt).atZone(ZoneId.systemDefault()).format(formatter);
                sb.append(csv(r.category)).append(',')
                        .append(csv(r.title)).append(',')
                        .append(r.durationMinutes).append(',')
                        .append(csv(DateTools.formatMinutes(r.durationMinutes))).append(',')
                        .append(csv(r.actualDate)).append(',')
                        .append(csv(created)).append(',')
                        .append(r.synced ? "1" : "0")
                        .append('\n');
            }

            String fileName = "friction_habit_records_" + LocalDate.now() + ".csv";
            ContentResolver resolver = getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/csv");
            values.put(MediaStore.Downloads.IS_PENDING, 1);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) throw new IllegalStateException("CSV URI could not be created");
            try (OutputStream os = resolver.openOutputStream(uri)) {
                if (os == null) throw new IllegalStateException("CSV output stream could not be opened");
                os.write(new byte[]{(byte)0xEF, (byte)0xBB, (byte)0xBF});
                os.write(sb.toString().getBytes(StandardCharsets.UTF_8));
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

    private String csv(String raw) {
        if (raw == null) return "";
        String escaped = raw.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }
}
