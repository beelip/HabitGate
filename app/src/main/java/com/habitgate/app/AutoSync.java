package com.habitgate.app;

import android.content.Context;

/** データ変更のたびに、設定済みの CSV とスプレッドシートへ自動同期する。 */
public final class AutoSync {
    private AutoSync() {}

    public static void run(Context context) {
        Context app = context.getApplicationContext();
        new Thread(() -> {
            try {
                if (CsvBackupManager.hasBackupDirectory(app)) {
                    CsvBackupManager.writeBackupToConfiguredDirectory(app);
                }
            } catch (Exception ignored) {
            }
            try {
                SheetsSync.syncUnsynced(app, false);
            } catch (Exception ignored) {
            }
        }).start();
    }
}
