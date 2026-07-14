package com.quran.quranaudio.online.prayertimes.widget;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 桌面 Widget 的祈祷数据缓存。
 *
 * ⚠️ 历史 Bug（本次修复）：旧实现用 {@code new Gson().toJson(dayPrayer)} 整体序列化
 * DayPrayer。DayPrayer 内含 {@code Map<PrayerEnum, LocalDateTime>}，而 Gson 默认反射
 * 无法访问 {@code java.time.LocalDateTime} 的私有字段——在 JVM 上直接抛
 * {@code JsonIOException: Failed making field 'java.time.LocalDateTime#date' accessible}，
 * 在 Android 9+/高 targetSdk 上同样因非 SDK 反射限制而失败。结果：save() 抛异常被上层
 * 吞掉 → 数据从未落盘 → load() 恒为空 → Widget 永远停留在 initialLayout（空表头、
 * "00:00"、全 "--:--"），且"应用里更新了祈祷时间也不刷新"。
 *
 * 现改为只存 Widget 真正需要的原语（各番 epoch 毫秒 + Hijri/公历三元组），
 * 完全不经过 java.time 反射序列化，跨所有 Android 版本稳定。
 */
public final class PrayerTimesWidgetData {

    private static final String PREFS_NAME = "PRAYER_WIDGET_DATA";

    // v2 前缀：与旧的 Gson JSON 键隔离，避免残留脏数据被误读
    private static final String KEY_SAVED = "v2_saved";
    private static final String KEY_PRAYER_MILLIS_PREFIX = "v2_prayer_millis_"; // + ordinal(0..4)
    private static final String KEY_HIJRI_DAY = "v2_hijri_day";
    private static final String KEY_HIJRI_MONTH = "v2_hijri_month";
    private static final String KEY_HIJRI_YEAR = "v2_hijri_year";
    private static final String KEY_GREG_YEAR = "v2_greg_year";
    private static final String KEY_GREG_MONTH = "v2_greg_month";
    private static final String KEY_GREG_DAY = "v2_greg_day";

    private PrayerTimesWidgetData() {
    }

    /**
     * 写缓存。从内存中的 DayPrayer 直接取 LocalDateTime 转 epoch 毫秒——不做任何
     * java.time 反射序列化。任一步骤都不允许抛异常影响调用方（闹钟调度）。
     */
    public static void save(Context context, DayPrayer dayPrayer) {
        if (dayPrayer == null) {
            return;
        }
        Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();
        if (timings == null || timings.isEmpty()) {
            return;
        }

        try {
            PrayerEnum[] all = PrayerEnum.values();
            long[] millis = new long[all.length];
            int validCount = 0;
            for (int i = 0; i < all.length; i++) {
                LocalDateTime t = timings.get(all[i]);
                if (t != null) {
                    millis[i] = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                    if (millis[i] > 0) {
                        validCount++;
                    }
                }
            }
            // 一个有效时间都没有：不写，避免把 Widget 打成脏空态
            if (validCount == 0) {
                return;
            }

            SharedPreferences.Editor e =
                    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
            for (int i = 0; i < all.length; i++) {
                e.putLong(KEY_PRAYER_MILLIS_PREFIX + i, millis[i]);
            }
            e.putInt(KEY_HIJRI_DAY, dayPrayer.getHijriDay());
            e.putInt(KEY_HIJRI_MONTH, dayPrayer.getHijriMonthNumber());
            e.putInt(KEY_HIJRI_YEAR, dayPrayer.getHijriYear());
            e.putInt(KEY_GREG_YEAR, dayPrayer.getGregorianYear());
            e.putInt(KEY_GREG_MONTH, dayPrayer.getGregorianMonthNumber());
            e.putInt(KEY_GREG_DAY, dayPrayer.getGregorianDay());
            e.putBoolean(KEY_SAVED, true);
            e.apply();
        } catch (Exception ex) {
            android.util.Log.e("PrayerWidgetData", "save failed", ex);
        }
    }

    /**
     * 读缓存。返回 null 表示尚无有效数据（Widget 应展示空态引导）。
     */
    @Nullable
    public static Snapshot load(Context context) {
        try {
            SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            if (!p.getBoolean(KEY_SAVED, false)) {
                return null;
            }
            PrayerEnum[] all = PrayerEnum.values();
            long[] millis = new long[all.length];
            boolean anyValid = false;
            for (int i = 0; i < all.length; i++) {
                millis[i] = p.getLong(KEY_PRAYER_MILLIS_PREFIX + i, 0L);
                if (millis[i] > 0) {
                    anyValid = true;
                }
            }
            if (!anyValid) {
                return null;
            }
            Snapshot s = new Snapshot();
            s.prayerMillis = millis;
            s.hijriDay = p.getInt(KEY_HIJRI_DAY, 0);
            s.hijriMonthNumber = p.getInt(KEY_HIJRI_MONTH, 0);
            s.hijriYear = p.getInt(KEY_HIJRI_YEAR, 0);
            s.gregorianYear = p.getInt(KEY_GREG_YEAR, 0);
            s.gregorianMonthNumber = p.getInt(KEY_GREG_MONTH, 0);
            s.gregorianDay = p.getInt(KEY_GREG_DAY, 0);
            return s;
        } catch (Exception ex) {
            android.util.Log.e("PrayerWidgetData", "load failed", ex);
            return null;
        }
    }

    /**
     * 便捷取 Fajr 的 LocalDateTime（每日经文/Khatmah 提醒按 Fajr 校准触发时刻）。
     * FAJR 为 {@link PrayerEnum#values()} 的第 0 项。缺失时返回 null。
     */
    @Nullable
    public static LocalDateTime getFajr(@Nullable Snapshot snapshot) {
        if (snapshot == null || snapshot.prayerMillis == null
                || snapshot.prayerMillis.length == 0 || snapshot.prayerMillis[0] <= 0) {
            return null;
        }
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(snapshot.prayerMillis[0]), ZoneId.systemDefault());
    }

    /**
     * 缓存是否是今天的数据（用公历三元组比对）。
     */
    public static boolean isForToday(Snapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return snapshot.gregorianYear == today.getYear()
                && snapshot.gregorianMonthNumber == today.getMonthValue()
                && snapshot.gregorianDay == today.getDayOfMonth();
    }

    /**
     * Widget 渲染所需的最小快照。prayerMillis 按 {@link PrayerEnum#values()} 顺序，
     * 元素为对应番次的 epoch 毫秒；0 表示该番次缺失。
     */
    public static final class Snapshot {
        public long[] prayerMillis;
        public int hijriDay;
        public int hijriMonthNumber;
        public int hijriYear;
        public int gregorianYear;
        public int gregorianMonthNumber;
        public int gregorianDay;
    }
}
