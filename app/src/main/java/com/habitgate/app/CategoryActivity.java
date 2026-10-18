package com.habitgate.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** タスクに設定できるカテゴリ（名前 + 色）の追加・削除を行う画面。 */
public class CategoryActivity extends ThemedActivity {
    public interface CategoryPicked {
        void onPicked(long categoryId);
    }

    private HabitDb db;
    private EditText nameEdit;
    private View colorSwatch;
    private LinearLayout categoryList;
    private int pendingColor = 0xFF4F46E5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = new HabitDb(this);
        buildUi();
    }

    private void buildUi() {
        LinearLayout root = Ui.screen(this);
        root.addView(Ui.titleWithBack(this, "カテゴリ編集"));

        root.addView(Ui.section(this, "カテゴリを追加"));
        LinearLayout addCard = Ui.card(this, root);
        nameEdit = Ui.edit(this, "カテゴリ名");
        addCard.addView(nameEdit);

        LinearLayout addRow = Ui.horizontal(this);
        colorSwatch = new View(this);
        updateSwatch();
        LinearLayout.LayoutParams swatchLp = new LinearLayout.LayoutParams(Ui.dp(this, 24), Ui.dp(this, 24));
        swatchLp.rightMargin = Ui.dp(this, 10);
        addRow.addView(colorSwatch, swatchLp);
        Button colorButton = Ui.button(this, "色を選択");
        colorButton.setOnClickListener(v -> ColorPickerDialog.show(this, pendingColor, color -> {
            pendingColor = color;
            updateSwatch();
        }));
        LinearLayout.LayoutParams colorLp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        colorLp.rightMargin = Ui.dp(this, 8);
        addRow.addView(colorButton, colorLp);
        Button addButton = Ui.tonalButton(this, "追加");
        addButton.setOnClickListener(v -> addCategory());
        addRow.addView(addButton, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        addCard.addView(addRow);

        root.addView(Ui.section(this, "カテゴリ"));
        LinearLayout listCard = Ui.card(this, root);
        categoryList = Ui.vertical(this);
        listCard.addView(categoryList);

        refresh();
    }

    private void updateSwatch() {
        colorSwatch.setBackground(Ui.rounded(pendingColor, Ui.BORDER, Ui.dp(this, 12)));
    }

    private void addCategory() {
        String name = nameEdit.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "カテゴリ名を入力してください", Toast.LENGTH_SHORT).show();
            return;
        }
        db.addCategory(name, pendingColor);
        nameEdit.setText("");
        AutoSync.run(this);
        refresh();
        Toast.makeText(this, "追加しました", Toast.LENGTH_SHORT).show();
    }

    private void refresh() {
        categoryList.removeAllViews();
        List<Models.Category> categories = db.getCategories();
        if (categories.isEmpty()) {
            categoryList.addView(Ui.note(this, "カテゴリがありません。"));
            return;
        }
        boolean first = true;
        for (Models.Category c : categories) {
            if (!first) Ui.addDivider(this, categoryList);
            first = false;
            LinearLayout row = Ui.horizontal(this);

            View dot = new View(this);
            dot.setBackground(Ui.rounded(c.color, Ui.BORDER, Ui.dp(this, 10)));
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(Ui.dp(this, 20), Ui.dp(this, 20));
            dotLp.rightMargin = Ui.dp(this, 12);
            row.addView(dot, dotLp);

            row.addView(Ui.body(this, c.name), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            Button del = Ui.iconButton(this, "🗑");
            del.setContentDescription("削除");
            del.setOnClickListener(v -> confirmDelete(c));
            row.addView(del, new LinearLayout.LayoutParams(Ui.dp(this, 48), LinearLayout.LayoutParams.WRAP_CONTENT));
            categoryList.addView(row);
        }
    }

    private void confirmDelete(Models.Category c) {
        Ui.dialog(this)
                .setTitle("カテゴリを削除しますか？")
                .setMessage("「" + c.name + "」を削除します。タスクからの紐付けは外れます。")
                .setPositiveButton("削除", (dialog, which) -> {
                    db.deleteCategory(c.id);
                    AutoSync.run(this);
                    refresh();
                    Toast.makeText(this, "削除しました", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("キャンセル", null)
                .show();
    }

    /** カテゴリ選択ダイアログ。「なし」+ 登録済みカテゴリ。選択で callback(カテゴリid。なし=0)。 */
    public static void showPicker(Activity activity, HabitDb db, long currentId, CategoryPicked callback) {
        List<Models.Category> items = new ArrayList<>(db.getCategories());
        PickerAdapter adapter = new PickerAdapter(activity, items, currentId);

        int pad = Ui.dp(activity, 18);
        LinearLayout wrapper = Ui.vertical(activity);
        wrapper.setPadding(pad, Ui.dp(activity, 6), pad, 0);
        ListView listView = new ListView(activity);
        listView.setAdapter(adapter);
        int rows = Math.min(items.size() + 1, 8);
        wrapper.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(activity, 48) * rows));

        AlertDialog dialog = Ui.dialog(activity)
                .setTitle("カテゴリを選択")
                .setView(wrapper)
                .setNegativeButton("キャンセル", null)
                .create();
        listView.setOnItemClickListener((parent, view, position, id) -> {
            callback.onPicked(adapter.categoryIdAt(position));
            dialog.dismiss();
        });
        dialog.show();
    }

    private static class PickerAdapter extends BaseAdapter {
        private final Activity activity;
        private final List<Models.Category> items;
        private final long currentId;

        PickerAdapter(Activity activity, List<Models.Category> items, long currentId) {
            this.activity = activity;
            this.items = items;
            this.currentId = currentId;
        }

        long categoryIdAt(int position) {
            return position == 0 ? 0 : items.get(position - 1).id;
        }

        @Override
        public int getCount() {
            return items.size() + 1;
        }

        @Override
        public Object getItem(int position) {
            return position == 0 ? null : items.get(position - 1);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row = Ui.horizontal(activity);
            int pad = Ui.dp(activity, 10);
            row.setPadding(pad, 0, pad, 0);
            row.setMinimumHeight(Ui.dp(activity, 48));

            long id = categoryIdAt(position);
            int color = position == 0 ? Ui.BORDER : items.get(position - 1).color;
            String name = position == 0 ? "なし" : items.get(position - 1).name;

            View dot = new View(activity);
            dot.setBackground(Ui.rounded(color, Ui.BORDER, Ui.dp(activity, 10)));
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(Ui.dp(activity, 20), Ui.dp(activity, 20));
            dotLp.rightMargin = Ui.dp(activity, 12);
            row.addView(dot, dotLp);

            TextView label = new TextView(activity);
            label.setText(name);
            label.setTextSize(15);
            label.setTextColor(Ui.TEXT);
            row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

            if (id == currentId) {
                TextView check = new TextView(activity);
                check.setText("✓");
                check.setTextSize(16);
                check.setTextColor(Ui.PRIMARY);
                row.addView(check, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            return row;
        }
    }
}
