package com.habitgate.app;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

/** 通知から開く入力画面。やること／減らすことの行をタップして個別に実績を入力する。 */
public class CheckInActivity extends ThemedActivity {
    private HabitDb db;
    private LinearLayout doList;
    private LinearLayout reduceList;
    private TextView cycleDateText;
    private TextView cycleStartText;
    // クイック追加の入力参照
    private EditText addTitle;
    private EditText addNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (db != null) refreshLists();
    }

    private void buildUi() {
        LinearLayout root = Ui.screen(this);
        root.addView(Ui.titleWithBack(this, "今日は何をした？"));

        // 対象日カード
        LinearLayout dateCard = Ui.card(this, root);
        cycleDateText = new TextView(this);
        cycleDateText.setTextSize(17);
        cycleDateText.setTypeface(Typeface.DEFAULT_BOLD);
        cycleDateText.setTextColor(Ui.TEXT);
        dateCard.addView(cycleDateText);
        cycleStartText = Ui.note(this, "");
        dateCard.addView(cycleStartText);

        // この日のやることを追加
        root.addView(Ui.section(this, "この日のやることを追加"));
        LinearLayout addCard = Ui.card(this, root);
        addTitle = Ui.edit(this, "例: 30分走る / PM過去問1問");
        addNote = Ui.memoEdit(this, "メモ（任意）");
        addCard.addView(addTitle);
        addCard.addView(addNote);
        Button addButton = Ui.tonalButton(this, "今日に追加");
        addButton.setOnClickListener(v -> addTaskToToday());
        addCard.addView(addButton);

        // やること
        root.addView(Ui.section(this, "やること"));
        LinearLayout doCard = Ui.card(this, root);
        doList = Ui.vertical(this);
        doCard.addView(doList);

        // 減らすこと
        root.addView(Ui.section(this, "減らすこと"));
        LinearLayout reduceCard = Ui.card(this, root);
        reduceList = Ui.vertical(this);
        reduceCard.addView(reduceList);

        Ui.space(this, root, 6);
        Button closeDay = Ui.tonalButton(this, "一日を終えて次の日へ");
        closeDay.setOnClickListener(v -> confirmCloseCurrentCycle());
        root.addView(closeDay);

        refreshLists();
    }

    private void addTaskToToday() {
        String title = addTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "タスク名を入力してください", Toast.LENGTH_SHORT).show();
            return;
        }
        db.addDoTask(title, addNote.getText().toString(), db.getCurrentCycle().cycleDate);
        addTitle.setText("");
        addNote.setText("");
        AutoSync.run(this);
        refreshLists();
        Toast.makeText(this, "追加しました", Toast.LENGTH_SHORT).show();
    }

    private void confirmCloseCurrentCycle() {
        Models.Cycle cycle = db.getCurrentCycle();
        Ui.dialog(this)
                .setTitle("一日を終えますか？")
                .setMessage(DateTools.formatDisplayDate(cycle.cycleDate) + " を終了して次の日へ移ります。未完了のやることは繰り越されます。")
                .setPositiveButton("終了する", (dialog, which) -> closeCurrentCycle())
                .setNegativeButton("キャンセル", null)
                .show();
    }

    private void closeCurrentCycle() {
        Models.Cycle next = CycleAutoCloser.closeCycleNow(this, db);
        refreshLists();
        Toast.makeText(this, "一日を終了しました。次の対象日: " + DateTools.formatDisplayDate(next.cycleDate), Toast.LENGTH_LONG).show();
    }

    private void refreshLists() {
        if (doList == null || reduceList == null) return;
        Models.Cycle cycle = db.getCurrentCycle();
        cycleDateText.setText("対象日: " + DateTools.formatShortDateWithWeekday(cycle.cycleDate));
        cycleStartText.setText("開始: " + DateTools.formatDateTime(cycle.startAt) + " / 項目をタップして実績を入力します。");

        doList.removeAllViews();
        List<Models.Task> tasks = db.getDueDoTasks(cycle.cycleDate);
        if (tasks.isEmpty()) {
            doList.addView(Ui.note(this, "この日に処理するタスクはありません。上の欄から追加できます。"));
        } else {
            for (Models.Task task : tasks) {
                Ui.addDivider(this, doList);
                doList.addView(ListRows.taskRow(this, db, task, cycle, this::refreshLists));
            }
        }

        reduceList.removeAllViews();
        List<Models.ReduceItem> items = db.getActiveReduceItems();
        if (items.isEmpty()) {
            reduceList.addView(Ui.note(this, "減らすことが未登録です。「タスク編集」から追加できます。"));
        } else {
            boolean linkedWithoutPermission = false;
            for (Models.ReduceItem item : items) {
                Ui.addDivider(this, reduceList);
                reduceList.addView(ListRows.reduceRow(this, db, item, cycle.cycleDate));
                if (item.hasLinkedApp() && !AppUsage.hasPermission(this)) linkedWithoutPermission = true;
            }
            if (linkedWithoutPermission) {
                reduceList.addView(Ui.note(this, "⚠ アプリ連携済みの項目がありますが、「使用状況へのアクセス」が未許可のため計測できません。設定画面から許可してください。"));
            }
        }
    }
}
