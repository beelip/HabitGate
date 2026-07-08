package com.habitgate.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

/** アイコンと名前検索付きのアプリ選択ダイアログ。 */
public final class AppPickerDialog {
    private AppPickerDialog() {}

    public interface Callback {
        void onSelected(String packageName);
    }

    public static void show(Activity activity, String currentPackage, Callback callback) {
        PackageManager pm = activity.getPackageManager();
        List<Entry> all = new ArrayList<>();
        try {
            Intent main = new Intent(Intent.ACTION_MAIN);
            main.addCategory(Intent.CATEGORY_LAUNCHER);
            HashSet<String> seen = new HashSet<>();
            for (ResolveInfo info : pm.queryIntentActivities(main, 0)) {
                if (info.activityInfo == null) continue;
                String pkg = info.activityInfo.packageName;
                if (pkg == null || pkg.equals(activity.getPackageName()) || !seen.add(pkg)) continue;
                String label = String.valueOf(info.loadLabel(pm));
                all.add(new Entry(pkg, label.isEmpty() ? pkg : label, info));
            }
        } catch (Exception ignored) {
        }
        Collator collator = Collator.getInstance(Locale.JAPAN);
        all.sort((a, b) -> collator.compare(a.label, b.label));

        Context context = activity;
        int pad = Ui.dp(context, 18);
        LinearLayout wrapper = Ui.vertical(context);
        wrapper.setPadding(pad, Ui.dp(context, 6), pad, 0);

        EditText search = Ui.edit(context, "アプリ名で検索");
        wrapper.addView(search);

        EntryAdapter adapter = new EntryAdapter(activity, pm, all);
        ListView listView = new ListView(context);
        listView.setAdapter(adapter);
        wrapper.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(context, 400)));

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle("連携するアプリを選択")
                .setView(wrapper)
                .setNegativeButton("キャンセル", null);
        if (currentPackage != null && !currentPackage.isEmpty()) {
            builder.setNeutralButton("連携を解除", (dialog, which) -> callback.onSelected(""));
        }
        AlertDialog dialog = builder.create();

        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            Entry entry = adapter.getItemEntry(position);
            callback.onSelected(entry.packageName);
            dialog.dismiss();
        });

        dialog.show();
    }

    private static class Entry {
        final String packageName;
        final String label;
        final ResolveInfo resolveInfo;
        Drawable icon;

        Entry(String packageName, String label, ResolveInfo resolveInfo) {
            this.packageName = packageName;
            this.label = label;
            this.resolveInfo = resolveInfo;
        }
    }

    private static class EntryAdapter extends BaseAdapter {
        private final Activity activity;
        private final PackageManager pm;
        private final List<Entry> all;
        private List<Entry> shown;

        EntryAdapter(Activity activity, PackageManager pm, List<Entry> all) {
            this.activity = activity;
            this.pm = pm;
            this.all = all;
            this.shown = all;
        }

        void filter(String query) {
            String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
            if (q.isEmpty()) {
                shown = all;
            } else {
                List<Entry> filtered = new ArrayList<>();
                for (Entry e : all) {
                    if (e.label.toLowerCase(Locale.ROOT).contains(q) || e.packageName.toLowerCase(Locale.ROOT).contains(q)) {
                        filtered.add(e);
                    }
                }
                shown = filtered;
            }
            notifyDataSetChanged();
        }

        Entry getItemEntry(int position) {
            return shown.get(position);
        }

        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public Object getItem(int position) {
            return shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Entry entry = shown.get(position);
            LinearLayout row;
            ImageView icon;
            TextView label;
            if (convertView instanceof LinearLayout) {
                row = (LinearLayout) convertView;
                icon = (ImageView) row.getChildAt(0);
                label = (TextView) row.getChildAt(1);
            } else {
                row = Ui.horizontal(activity);
                int pad = Ui.dp(activity, 10);
                row.setPadding(pad, pad, pad, pad);
                icon = new ImageView(activity);
                int iconSize = Ui.dp(activity, 32);
                LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
                iconLp.rightMargin = Ui.dp(activity, 12);
                row.addView(icon, iconLp);
                label = new TextView(activity);
                label.setTextSize(15);
                label.setTextColor(Ui.TEXT);
                row.addView(label, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            }
            if (entry.icon == null) {
                try {
                    entry.icon = entry.resolveInfo.loadIcon(pm);
                } catch (Exception ignored) {
                }
            }
            icon.setImageDrawable(entry.icon);
            label.setText(entry.label);
            return row;
        }
    }
}
