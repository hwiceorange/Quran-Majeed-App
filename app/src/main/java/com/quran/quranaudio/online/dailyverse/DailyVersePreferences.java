package com.quran.quranaudio.online.dailyverse;

import android.content.Context;

import androidx.preference.PreferenceManager;

/**
 * 每日经文通知的开关（默认开启——它是内容不是营销，且用户可随时在
 * App 设置或系统通知渠道两级关闭）。
 */
public final class DailyVersePreferences {

    public static final String KEY_ENABLED = "DAILY_VERSE_NOTIFICATION_ENABLED";

    private DailyVersePreferences() {
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_ENABLED, true);
    }
}
