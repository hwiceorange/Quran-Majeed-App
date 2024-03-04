package com.raiadnan.quranreader.prayertimes.quickaccess;

import android.preference.PreferenceManager;
import com.raiadnan.quranreader.prayertimes.App;

public enum QuickAccessManager {
    get;

    public void enable(boolean z) {
        PreferenceManager.getDefaultSharedPreferences(App.get()).edit().putBoolean("enableLockScreen", z).apply();
    }

    public boolean isEnable() {
        return PreferenceManager.getDefaultSharedPreferences(App.get()).getBoolean("enableLockScreen", true);
    }
}
