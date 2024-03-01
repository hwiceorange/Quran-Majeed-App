package com.quran.quranaudio.online.prayertimes.utils;

import android.content.SharedPreferences;


public class UserPreferencesUtils {

    public static SharedPreferences.Editor putDouble(final SharedPreferences.Editor editor, final String key, final double value) {
        return editor.putLong(key, Double.doubleToRawLongBits(value));
    }

    public static double getDouble(final SharedPreferences prefs, final String key, final double defaultValue) {
        return Double.longBitsToDouble(prefs.getLong(key, Double.doubleToLongBits(defaultValue)));
    }
}
