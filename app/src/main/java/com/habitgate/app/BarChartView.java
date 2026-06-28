package com.habitgate.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class BarChartView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<Models.DayTotal> totals = new ArrayList<>();

    public BarChartView(Context context) {
        super(context);
        setMinimumHeight(Ui.dp(context, 240));
    }

    public void setTotals(List<Models.DayTotal> totals) {
        this.totals = totals == null ? new ArrayList<>() : totals;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int padLeft = Ui.dp(getContext(), 40);
        int padRight = Ui.dp(getContext(), 16);
        int padTop = Ui.dp(getContext(), 24);
        int padBottom = Ui.dp(getContext(), 46);
        int chartW = Math.max(1, w - padLeft - padRight);
        int chartH = Math.max(1, h - padTop - padBottom);

        paint.setTextSize(Ui.dp(getContext(), 12));
        paint.setColor(0xFF666666);
        canvas.drawText("やること", padLeft, Ui.dp(getContext(), 14), paint);
        canvas.drawText("減らすこと", padLeft + Ui.dp(getContext(), 90), Ui.dp(getContext(), 14), paint);

        paint.setColor(0xFFE0E0E0);
        canvas.drawLine(padLeft, padTop + chartH, padLeft + chartW, padTop + chartH, paint);
        canvas.drawLine(padLeft, padTop, padLeft, padTop + chartH, paint);

        if (totals.isEmpty()) {
            paint.setColor(0xFF777777);
            paint.setTextSize(Ui.dp(getContext(), 14));
            canvas.drawText("この期間の記録はまだありません", padLeft, padTop + chartH / 2f, paint);
            return;
        }

        int max = 1;
        for (Models.DayTotal t : totals) {
            max = Math.max(max, Math.max(t.doMinutes, t.reduceMinutes));
        }

        int n = totals.size();
        float groupW = chartW / (float) n;
        float barW = Math.max(4, groupW * 0.28f);
        for (int i = 0; i < n; i++) {
            Models.DayTotal t = totals.get(i);
            float baseX = padLeft + i * groupW + groupW / 2f;
            float doH = chartH * (t.doMinutes / (float) max);
            float reduceH = chartH * (t.reduceMinutes / (float) max);

            paint.setColor(0xFF2E7D32);
            canvas.drawRect(baseX - barW - 1, padTop + chartH - doH, baseX - 1, padTop + chartH, paint);
            paint.setColor(0xFFC62828);
            canvas.drawRect(baseX + 1, padTop + chartH - reduceH, baseX + barW + 1, padTop + chartH, paint);

            if (n <= 14 || i % Math.max(1, n / 8) == 0) {
                paint.setColor(0xFF555555);
                paint.setTextSize(Ui.dp(getContext(), 10));
                String label = t.date.substring(5);
                canvas.save();
                canvas.rotate(-45, baseX, padTop + chartH + Ui.dp(getContext(), 24));
                canvas.drawText(label, baseX - Ui.dp(getContext(), 18), padTop + chartH + Ui.dp(getContext(), 24), paint);
                canvas.restore();
            }
        }

        paint.setColor(0xFF777777);
        paint.setTextSize(Ui.dp(getContext(), 11));
        canvas.drawText(DateTools.formatMinutes(max), Ui.dp(getContext(), 4), padTop + Ui.dp(getContext(), 8), paint);
    }
}
