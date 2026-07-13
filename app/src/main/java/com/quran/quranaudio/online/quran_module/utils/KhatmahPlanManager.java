package com.quran.quranaudio.online.quran_module.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Khatmah(通读古兰经)计划：在 N 天内读完全本(默认 30 天，斋月引擎)。
 *
 * 纯 SharedPreferences 存储，不碰任何现有数据库/经文数据；进度用"已读到第几 Juz"衡量
 * (复用首页已有的 last-read → Juz 计算)。提供每日目标与领先/落后状态。
 */
public final class KhatmahPlanManager {

    private static final String PREFS = "khatmah_plan";
    private static final String KEY_ACTIVE = "active";
    private static final String KEY_START_DAY = "start_epoch_day";  // 自 1970 的天数
    private static final String KEY_TARGET_DAYS = "target_days";
    private static final String KEY_REMINDER_ENABLED = "reminder_enabled";
    private static final String KEY_MAX_JUZ = "max_juz_reached";
    private static final String KEY_PROGRESS_DAY = "last_progress_epoch_day";

    public static final int TOTAL_JUZ = 30;
    public static final int DEFAULT_TARGET_DAYS = 30;

    public enum Status { ON_TRACK, AHEAD, BEHIND, COMPLETED }

    private KhatmahPlanManager() {
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static int today() {
        return (int) (System.currentTimeMillis() / (24L * 60 * 60 * 1000));
    }

    public static boolean isActive(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ACTIVE, false);
    }

    public static void startPlan(Context ctx, int targetDays) {
        if (targetDays <= 0) targetDays = DEFAULT_TARGET_DAYS;
        prefs(ctx).edit()
                .putBoolean(KEY_ACTIVE, true)
                .putInt(KEY_START_DAY, today())
                .putInt(KEY_TARGET_DAYS, targetDays)
                .putBoolean(KEY_REMINDER_ENABLED, true) // 开始计划默认开启温和提醒(可在设置关闭)
                .apply();
    }

    // ===== 每日提醒开关 =====
    public static boolean isReminderEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_REMINDER_ENABLED, true);
    }

    public static void setReminderEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_REMINDER_ENABLED, enabled).apply();
    }

    // ===== "今日是否已读"判断(已完成今日份则不再提醒，避免骚扰) =====
    /** 记录当前读到的 Juz；若较历史最高有推进，标记"今天读过了"。 */
    public static void recordProgress(Context ctx, int currentJuz) {
        if (currentJuz <= 0) return;
        SharedPreferences p = prefs(ctx);
        int maxJuz = p.getInt(KEY_MAX_JUZ, 0);
        if (currentJuz > maxJuz) {
            p.edit()
                    .putInt(KEY_MAX_JUZ, currentJuz)
                    .putInt(KEY_PROGRESS_DAY, today())
                    .apply();
        }
    }

    /** 今天是否已推进过 Juz(读过今日份)。 */
    public static boolean hasReadToday(Context ctx) {
        return prefs(ctx).getInt(KEY_PROGRESS_DAY, -1) == today();
    }

    public static void stopPlan(Context ctx) {
        prefs(ctx).edit().putBoolean(KEY_ACTIVE, false).apply();
    }

    public static int getTargetDays(Context ctx) {
        return prefs(ctx).getInt(KEY_TARGET_DAYS, DEFAULT_TARGET_DAYS);
    }

    /** 计划已进行的天数(第几天，从 1 开始)。 */
    public static int getElapsedDay(Context ctx) {
        int start = prefs(ctx).getInt(KEY_START_DAY, today());
        return Math.max(1, today() - start + 1);
    }

    /** 到今天为止"应该"读到第几 Juz(1..30)。 */
    public static int getExpectedJuz(Context ctx) {
        int target = getTargetDays(ctx);
        int elapsed = getElapsedDay(ctx);
        int expected = (int) Math.ceil(elapsed * (double) TOTAL_JUZ / target);
        return Math.max(1, Math.min(TOTAL_JUZ, expected));
    }

    /** 根据"实际已读到第几 Juz"给出领先/落后/完成状态。 */
    public static Status getStatus(Context ctx, int actualJuz) {
        if (actualJuz >= TOTAL_JUZ) return Status.COMPLETED;
        int expected = getExpectedJuz(ctx);
        if (actualJuz >= expected) {
            return actualJuz > expected ? Status.AHEAD : Status.ON_TRACK;
        }
        return Status.BEHIND;
    }

    /** 落后的 Juz 数(>=0)。 */
    public static int getBehindBy(Context ctx, int actualJuz) {
        return Math.max(0, getExpectedJuz(ctx) - actualJuz);
    }
}
