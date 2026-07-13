package com.habitgate.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.util.List;

/** やること／減らすことの追加・一覧・削除・アプリ連携を行う編集画面。 */
public class TaskEditActivity extends ThemedActivity {
    private HabitDb db;
    private LinearLayout doList;
    private LinearLayout reduceList;
    private EditText addDoTitle;
    private EditText addDoNote;
    private EditText addReduceTitle;
    private EditText addReduceNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLists();
    }

    private void buildUi() {
        LinearLayout root = Ui.screen(this);
        root.addView(Ui.title(this, "タスク編集"));

        // やることを追加
        root.addView(Ui.section(this, "やることを追加"));
        LinearLayout doAddCard = Ui.card(this, root);
        addDoTitle = Ui.edit(this, "例: 30分走る / PM過去問1問");
        addDoNote = Ui.edit(this, "メモ（任意）");
        doAddCard.addView(addDoTitle);
        doAddCard.addView(addDoNote);
        LinearLayout addDoButtons = Ui.horizontal(this);
        Button addDoToday = Ui.tonalButton(this, "今日やる");
        addDoToday.setOnClickListener(v -> addDoTaskForDate(db.getCurrentCycle().cycleDate));
        Button addDoNext = Ui.tonalButton(this, "明日やる");
        addDoNext.setOnClickListener(v -> addDoTaskForDate(DateTools.nextDay(db.getCurrentCycle().cycleDate)));
        Button addDoByDate = Ui.iconButton(this, "📅");
        addDoByDate.setContentDescription("日付を選んで追加");
        addDoByDate.setOnClickListener(v -> openTaskDatePicker());
        LinearLayout.LayoutParams grow = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        grow.rightMargin = Ui.dp(this, 8);
        LinearLayout.LayoutParams grow2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        grow2.rightMargin = Ui.dp(this, 8);
        addDoButtons.addView(addDoToday, grow);
        addDoButtons.addView(addDoNext, grow2);
        addDoButtons.addView(addDoByDate, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
        doAddCard.addView(addDoButtons);
        doAddCard.addView(Ui.note(this, "項目をタップすると実績入力、長押しすると編集を開きます。"));

        // やること一覧
        root.addView(Ui.section(this, "やること"));
        LinearLayout doCard = Ui.card(this, root);
        doList = Ui.vertical(this);
        doCard.addView(doList);

        // 減らすことを追加
        root.addView(Ui.section(this, "減らすことを追加"));
        LinearLayout reduceAddCard = Ui.card(this, root);
        addReduceTitle = Ui.edit(this, "例: Twitter / 夜更かし / 食べ過ぎ");
        addReduceNote = Ui.edit(this, "メモ（任意）");
        reduceAddCard.addView(addReduceTitle);
        reduceAddCard.addView(addReduceNote);
        Button addReduceButton = Ui.tonalButton(this, "追加");
        addReduceButton.setOnClickListener(v -> addReduceItem());
        reduceAddCard.addView(addReduceButton);
        reduceAddCard.addView(Ui.note(this, "📱 でアプリを連携すると、そのアプリの使用時間を自動計測します。項目をタップすると実績入力、長押しすると編集を開きます。"));

        // 減らすこと一覧
        root.addView(Ui.section(this, "減らすこと"));
        LinearLayout reduceCard = Ui.card(this, root);
        reduceList = Ui.vertical(this);
        reduceCard.addView(reduceList);

        Ui.space(this, root, 8);
        Button back = Ui.button(this, "戻る");
        back.setOnClickListener(v -> finish());
        root.addView(back);

        refreshLists();
    }

    private void openTaskDatePicker() {
        Models.Cycle cycle = db.getCurrentCycle();
        LocalDate initial = DateTools.parseOrToday(cycle.cycleDate).plusDays(1);
        new DatePickerDialog(this, Ui.pickerTheme(), (view, year, month, dayOfMonth) -> {
            LocalDate selected = LocalDate.of(year, month + 1, dayOfMonth);
            addDoTaskForDate(selected.format(DateTools.DATE));
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void addDoTaskForDate(String plannedDate) {
        String title = addDoTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "タスク名を入力してください", Toast.LENGTH_SHORT).show();
            return;
        }
        db.addDoTask(title, addDoNote.getText().toString(), plannedDate);
        addDoTitle.setText("");
        addDoNote.setText("");
        AutoSync.run(this);
        refreshLists();
        Toast.makeText(this, DateTools.formatShortDateWithWeekday(plannedDate) + " に追加しました", Toast.LENGTH_SHORT).show();
    }

    private void addReduceItem() {
        String title = addReduceTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "項目名を入力してください", Toast.LENGTH_SHORT).show();
            return;
        }
        db.addReduceItem(title, addReduceNote.getText().toString());
        addReduceTitle.setText("");
        addReduceNote.setText("");
        AutoSync.run(this);
        refreshLists();
        Toast.makeText(this, "追加しました", Toast.LENGTH_SHORT).show();
    }

    private void refreshLists() {
        if (doList == null || reduceList == null) return;

        doList.removeAllViews();
        List<Models.Task> tasks = db.getActiveDoTasks();
        if (tasks.isEmpty()) {
            doList.addView(Ui.note(this, "登録されているやることはありません。"));
        } else {
            for (Models.Task t : tasks) {
                Ui.addDivider(this, doList);
                LinearLayout row = Ui.horizontal(this);

                LinearLayout col = Ui.vertical(this);
                col.addView(Ui.body(this, DateTools.formatShortDateWithWeekday(t.plannedDate) + "  " + t.title));
                if (t.dueAt > 0) {
                    TextView dueLine = new TextView(this);
                    dueLine.setText("⏰ 期限: " + DateTools.formatDateTime(t.dueAt));
                    dueLine.setTextSize(13);
                    dueLine.setTextColor(t.dueAt < System.currentTimeMillis() ? Ui.DANGER : Ui.HINT);
                    col.addView(dueLine);
                }
                if (!t.note.isEmpty()) {
                    TextView noteLine = new TextView(this);
                    noteLine.setText("メモ: " + t.note);
                    noteLine.setTextSize(13);
                    noteLine.setTextColor(Ui.HINT);
                    col.addView(noteLine);
                }
                row.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                Button del = Ui.iconButton(this, "🗑");
                del.setContentDescription("削除");
                del.setOnClickListener(v -> confirmDelete("やること", t.title, () -> {
                    db.deleteDoTask(t.id);
                    AutoSync.run(this);
                    refreshLists();
                }));
                row.addView(del, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));

                int tint = Ui.priorityTint(t.priority);
                Ui.tappableRow(row, tint);
                if (tint != 0) {
                    row.setPadding(Ui.dp(this, 10), Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8));
                }
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(this, TaskEntryActivity.class);
                    intent.putExtra("task_id", t.id);
                    startActivity(intent);
                });
                row.setOnLongClickListener(v -> {
                    Intent intent = new Intent(this, TaskFormActivity.class);
                    intent.putExtra("task_id", t.id);
                    startActivity(intent);
                    return true;
                });
                doList.addView(row);
            }
        }

        reduceList.removeAllViews();
        List<Models.ReduceItem> items = db.getActiveReduceItems();
        if (items.isEmpty()) {
            reduceList.addView(Ui.note(this, "登録されている減らすことはありません。"));
        } else {
            for (Models.ReduceItem item : items) {
                Ui.addDivider(this, reduceList);
                LinearLayout row = Ui.horizontal(this);
                String text = item.title;
                if (item.hasLinkedApp()) text += "\n📱 " + AppUsage.appLabel(this, item.appPackage);
                if (!item.note.isEmpty()) text += "\nメモ: " + item.note;
                row.addView(Ui.body(this, text), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                Button link = Ui.iconButton(this, "📱");
                link.setContentDescription("アプリを連携");
                link.setOnClickListener(v -> openAppLinkDialog(item));
                LinearLayout.LayoutParams linkLp = new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT);
                linkLp.rightMargin = Ui.dp(this, 6);
                row.addView(link, linkLp);

                Button del = Ui.iconButton(this, "🗑");
                del.setContentDescription("削除");
                del.setOnClickListener(v -> confirmDelete("減らすこと", item.title, () -> {
                    db.deleteReduceItem(item.id);
                    AutoSync.run(this);
                    refreshLists();
                }));
                row.addView(del, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));

                Ui.tappable(row);
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ReduceEntryActivity.class);
                    intent.putExtra("item_id", item.id);
                    startActivity(intent);
                });
                row.setOnLongClickListener(v -> {
                    Intent intent = new Intent(this, ReduceFormActivity.class);
                    intent.putExtra("item_id", item.id);
                    startActivity(intent);
                    return true;
                });
                reduceList.addView(row);
            }
        }
    }

    private void openAppLinkDialog(Models.ReduceItem item) {
        AppPickerDialog.show(this, item.appPackage, packageName -> {
            db.setReduceItemAppPackage(item.id, packageName);
            AutoSync.run(this);
            refreshLists();
            if (packageName.isEmpty()) {
                Toast.makeText(this, "連携を解除しました", Toast.LENGTH_SHORT).show();
            } else if (AppUsage.hasPermission(this)) {
                Toast.makeText(this, AppUsage.appLabel(this, packageName) + " を連携しました", Toast.LENGTH_SHORT).show();
            } else {
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

    private void confirmDelete(String type, String title, Runnable onConfirm) {
        Ui.dialog(this)
                .setTitle(type + "を削除しますか？")
                .setMessage("「" + title + "」を削除します。")
                .setPositiveButton("削除", (dialog, which) -> {
                    onConfirm.run();
                    Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }
}
