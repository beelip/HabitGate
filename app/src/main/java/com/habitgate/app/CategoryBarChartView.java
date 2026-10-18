package com.habitgate.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** カテゴリ別（やること）の実績時間を棒グラフで表示する。 */
public class CategoryBarChartView extends View {
    private static final int OTHER_COLOR = 0xFF9CA3AF;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<String> names = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();
    private final List<Integer> minutes = new ArrayList<>();

    public CategoryBarChartView(Context context) {
        super(context);
        setMinimumHeight(Ui.dp(context, 200));
    }

    /** totals は {categoryId, minutes}。categoryId=0 や未定義のカテゴリは「その他」に合算する。 */
    public void setData(List<Models.Category> categories, List<long[]> totals) {
        names.clear();
        colors.clear();
        minutes.clear();
        long otherMinutes = 0;
        List<Models.Category> cats = categories == null ? new ArrayList<>() : categories;
        long[] sums = new long[cats.size()];
        if (totals != null) {
            for (long[] row : totals) {
                if (row == null || row.length < 2) continue;
                int idx = -1;
                for (int i = 0; i < cats.size(); i++) {
                    if (cats.get(i).id == row[0] && row[0] != 0) {
                        idx = i;
                        break;
                    }
                }
                if (idx >= 0) sums[idx] += row[1];
                else otherMinutes += row[1];
            }
        }
        for (int i = 0; i < cats.size(); i++) {
            names.add(cats.get(i).name);
            colors.add(cats.get(i).color);
            minutes.add((int) Math.min(Integer.MAX_VALUE, sums[i]));
        }
        if (otherMinutes > 0) {
            names.add("その他");
            colors.add(OTHER_COLOR);
            minutes.add((int) Math.min(Integer.MAX_VALUE, otherMinutes));
        }
        invalidate();
    }

    private boolean isEmptyData() {
        for (int m : minutes) {
            if (m > 0) return false;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Context ctx = getContext();
        int w = getWidth();
        int h = getHeight();
        int padLeft = Ui.dp(ctx, 40);
        int padRight = Ui.dp(ctx, 16);
        int padTop = Ui.dp(ctx, 16);
        int padBottom = Ui.dp(ctx, 56);
        int chartW = Math.max(1, w - padLeft - padRight);
        int chartH = Math.max(1, h - padTop - padBottom);

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(Ui.BORDER);
        canvas.drawLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH, paint);
        canvas.drawLine(padLeft, padTop, padLeft, padTop + chartH, paint);

        if (names.isEmpty() || isEmptyData()) {
            paint.setColor(Ui.MUTED);
            paint.setTextSize(Ui.dp(ctx, 14));
            canvas.drawText("この期間の記録はまだありません", padLeft, padTop + chartH / 2f, paint);
            return;
        }

        int max = 1;
        for (int m : minutes) max = Math.max(max, m);

        int n = names.size();
        float groupW = chartW / (float) n;
        float barW = groupW * 0.55f;
        for (int i = 0; i < n; i++) {
            float centerX = padLeft + i * groupW + groupW / 2f;
            float barH = chartH * (minutes.get(i) / (float) max);
            paint.setColor(colors.get(i));
            canvas.drawRect(centerX - barW / 2f, padTop + chartH - barH,
                    centerX + barW / 2f, padTop + chartH, paint);

            String label = names.get(i);
            if (label.length() > 6) label = label.substring(0, 6) + "…";
            paint.setColor(Ui.MUTED);
            paint.setTextSize(Ui.dp(ctx, 11));
            paint.setTextAlign(Paint.Align.RIGHT);
            float pivotY = padTop + chartH + Ui.dp(ctx, 8);
            canvas.save();
            canvas.rotate(-45, centerX, pivotY);
            canvas.drawText(label, centerX, pivotY, paint);
            canvas.restore();
            paint.setTextAlign(Paint.Align.LEFT);
        }

        paint.setColor(Ui.MUTED);
        paint.setTextSize(Ui.dp(ctx, 11));
        canvas.drawText(DateTools.formatMinutes(max), Ui.dp(ctx, 4), padTop + Ui.dp(ctx, 8), paint);
    }
}
