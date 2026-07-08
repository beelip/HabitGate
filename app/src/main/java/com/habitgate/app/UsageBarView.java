package com.habitgate.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** 使用時間を表すセグメント棒グラフ。しきい値に応じて色を変える。 */
public class UsageBarView extends View {
    private static final int COLOR_BLUE = 0xFF3B82F6;
    private static final int COLOR_GREEN = 0xFF22C55E;
    private static final int COLOR_YELLOW = 0xFFEAB308;
    private static final int COLOR_RED = 0xFFDC2626;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int minutes;
    private int maxMinutes = 1;

    public UsageBarView(Context context) {
        super(context);
    }

    public void setValues(int minutes, int maxMinutes) {
        this.minutes = minutes;
        this.maxMinutes = maxMinutes;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;

        float radius = h / 2f;
        RectF trackRect = new RectF(0, 0, w, h);
        Path trackPath = new Path();
        trackPath.addRoundRect(trackRect, radius, radius, Path.Direction.CW);

        paint.setColor(Ui.BORDER);
        canvas.drawPath(trackPath, paint);

        float f = maxMinutes <= 0 ? 0f : minutes / (float) maxMinutes;

        canvas.save();
        canvas.clipPath(trackPath);

        if (f >= 1.0f) {
            paint.setColor(COLOR_RED);
            canvas.drawRect(0, 0, w, h, paint);
        } else if (f > 0f) {
            drawSegment(canvas, w, h, 0f, Math.min(f, 0.10f), COLOR_BLUE);
            drawSegment(canvas, w, h, 0.10f, Math.min(f, 0.25f), COLOR_GREEN);
            drawSegment(canvas, w, h, 0.25f, Math.min(f, 0.50f), COLOR_YELLOW);
            drawSegment(canvas, w, h, 0.50f, Math.min(f, 1.00f), COLOR_RED);
        }

        canvas.restore();
    }

    private void drawSegment(Canvas canvas, int w, int h, float fromFraction, float toFraction, int color) {
        if (toFraction <= fromFraction) return;
        paint.setColor(color);
        canvas.drawRect(w * fromFraction, 0, w * toFraction, h, paint);
    }
}
