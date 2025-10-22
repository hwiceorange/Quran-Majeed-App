package com.quran.quranaudio.online.tasbih.helper;


import com.quran.quranaudio.online.App;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TasbihManager {
    private static TasbihManager tasbihManager;
    private static final String KEY_DAILY_COUNT = "daily_tasbih_count";
    private static final String KEY_DAILY_DATE = "daily_tasbih_date";

    public static TasbihManager get() {
        if (tasbihManager == null) {
            tasbihManager = new TasbihManager();
        }
        return tasbihManager;
    }

    public int getSpeak() {
        return App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).getInt("speak", 0);
    }

    public void putSpeak(int i) {
        App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).edit().putInt("speak", i).apply();
    }

    public boolean is33() {
        return App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).getBoolean("33", true);
    }

    public void put33(boolean z) {
        App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).edit().putBoolean("33", z).apply();
    }

    public int getTotal() {
        return App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).getInt("total", 0);
    }

    public void putTotal(int i) {
        App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).edit().putInt("total", i).apply();
    }

    /**
     * Get today's Tasbih count (for Daily Quest tracking).
     * Automatically resets if it's a new day.
     */
    public int getDailyCount() {
        String today = getTodayDateString();
        String savedDate = App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0)
                .getString(KEY_DAILY_DATE, "");

        // If it's a new day, reset daily count
        if (!today.equals(savedDate)) {
            resetDailyCount();
            return 0;
        }

        return App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0)
                .getInt(KEY_DAILY_COUNT, 0);
    }

    /**
     * Increment today's Tasbih count by 1.
     * Returns the new count.
     */
    public int incrementDailyCount() {
        String today = getTodayDateString();
        int currentCount = getDailyCount(); // This will auto-reset if new day
        int newCount = currentCount + 1;

        App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).edit()
                .putInt(KEY_DAILY_COUNT, newCount)
                .putString(KEY_DAILY_DATE, today)
                .apply();

        return newCount;
    }

    /**
     * Reset daily count to 0 (used when a new day starts).
     */
    private void resetDailyCount() {
        String today = getTodayDateString();
        App.get().getSharedPreferences(TasbihManager.class.getSimpleName(), 0).edit()
                .putInt(KEY_DAILY_COUNT, 0)
                .putString(KEY_DAILY_DATE, today)
                .apply();
    }

    /**
     * Get today's date as a string (YYYY-MM-DD).
     */
    private String getTodayDateString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        return sdf.format(new Date());
    }
}
