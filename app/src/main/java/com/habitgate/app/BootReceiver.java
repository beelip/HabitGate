package com.habitgate.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** 端末再起動後に、通知アラームとサイクル自動終了アラームを再設定する。 */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        ReminderScheduler.scheduleNext(context);
        CycleAutoCloser.closeIfDueAndReschedule(context);
    }
}
