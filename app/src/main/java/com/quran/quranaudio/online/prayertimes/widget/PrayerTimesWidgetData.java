package com.quran.quranaudio.online.prayertimes.widget;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;

import java.time.LocalDate;

/**
 * 桌面 Widget 的祈祷数据缓存。
 *
 * 写入时机：PrayerAlarmScheduler.scheduleAlarmsAndReminders() —— 它是所有祈祷时间
 * 重算路径（定位变更/设置变更/30分钟周期任务/开机重排）的唯一汇聚点，保证 Widget
 * 展示的数据与 App 内页面、通知闹钟永远同源。
 *
 * 序列化沿用 WorkCreator/InstantPrayerScheduler 已在生产验证的 Gson 直转 DayPrayer 范式。
 */
public final class PrayerTimesWidgetData {

    private static final String PREFS_NAME = "PRAYER_WIDGET_DATA";
    private static final String KEY_DAY_PRAYER_JSON = "DAY_PRAYER_JSON";

    private PrayerTimesWidgetData() {
    }

    public static void save(Context context, DayPrayer dayPrayer) {
        if (dayPrayer == null || dayPrayer.getTimings() == null || dayPrayer.getTimings().isEmpty()) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_DAY_PRAYER_JSON, new Gson().toJson(dayPrayer))
                .apply();
    }

    @Nullable
    public static DayPrayer load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_DAY_PRAYER_JSON, null);
        if (json == null) {
            return null;
        }
        try {
            DayPrayer dayPrayer = new Gson().fromJson(json, DayPrayer.class);
            if (dayPrayer == null || dayPrayer.getTimings() == null || dayPrayer.getTimings().isEmpty()) {
                return null;
            }
            return dayPrayer;
        } catch (Exception e) {
            android.util.Log.e("PrayerWidgetData", "Failed to parse cached DayPrayer", e);
            return null;
        }
    }

    /**
     * 缓存是否是今天的数据（用公历三元组比对，不依赖 date 字符串格式）。
     */
    public static boolean isForToday(DayPrayer dayPrayer) {
        LocalDate today = LocalDate.now();
        return dayPrayer.getGregorianYear() == today.getYear()
                && dayPrayer.getGregorianMonthNumber() == today.getMonthValue()
                && dayPrayer.getGregorianDay() == today.getDayOfMonth();
    }
}
