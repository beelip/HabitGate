package com.habitgate.app;

import android.os.Bundle;

/** テーマ適用と、テーマ変更検知時の再生成を担う基底 Activity。 */
public abstract class ThemedActivity extends android.app.Activity {
    private String appliedTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Themes.apply(this);
        appliedTheme = Themes.signature(this);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        String current = Themes.signature(this);
        if (!current.equals(appliedTheme)) {
            appliedTheme = current;
            recreate();
        }
    }
}
