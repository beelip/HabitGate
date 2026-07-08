package com.habitgate.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 自動終了時刻に発火し、期限を過ぎたサイクルを終了する。 */
public class AutoCloseReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        CycleAutoCloser.closeIfDueAndReschedule(context);
    }
}
