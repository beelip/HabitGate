package com.habitgate.app;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/** 子のタップを活かしたまま、横スワイプだけを検知する縦 LinearLayout。 */
public class SwipeLayout extends LinearLayout {
    public interface OnSwipe { void onSwipe(int direction); } // -1=右スワイプ（前へ）, 1=左スワイプ（次へ）

    private OnSwipe onSwipe;
    private float downX;
    private float downY;
    private boolean locked;

    public SwipeLayout(Context context) {
        super(context);
        setOrientation(VERTICAL);
    }

    public void setOnSwipe(OnSwipe cb) { this.onSwipe = cb; }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                locked = false;
                return false;
            case MotionEvent.ACTION_MOVE: {
                if (locked) return true;
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                // 横方向の意図が明確になったら子からタッチを奪い、親のスクロールにも奪わせない
                if (Math.abs(dx) > Ui.dp(getContext(), 24) && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                    locked = true;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                }
                return false;
            }
            default:
                return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                locked = false;
                return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_UP: {
                float dx = ev.getX() - downX;
                float dy = ev.getY() - downY;
                getParent().requestDisallowInterceptTouchEvent(false);
                if (onSwipe != null && Math.abs(dx) > Ui.dp(getContext(), 50) && Math.abs(dx) > Math.abs(dy)) {
                    onSwipe.onSwipe(dx < 0 ? 1 : -1);
                }
                locked = false;
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                locked = false;
                return true;
            default:
                return super.onTouchEvent(ev);
        }
    }
}
