package com.quran.quranaudio.online.dailyverse;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.widget.PrayerTimesWidgetData;
import com.quran.quranaudio.online.quran_module.utils.KhatmahPlanManager;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Khatmah 每日提醒调度器 —— 严格按伊斯兰真实场景设计的"最保守、不冒犯"方案。
 *
 * 时机：上午 Fajr 后 3 小时。此时人已醒、白天空闲；绝不碰礼拜、开斋(Maghrib)、
 * Taraweeh(Isha 后)、封斋饭(Suhoor)等神圣/私人时刻。斋月长斋戒地区用真实本地 Fajr 自适应。
 *
 * 是否真正发出通知由 {@link KhatmahReminderReceiver} 在触发时二次判断
 * (计划激活 + 提醒开启 + 非最后10夜 + 今日未读)，本调度器只负责"何时唤醒去判断"。
 */
public final class KhatmahReminderScheduler {

    private static final String TAG = "KhatmahReminder";
    public static final String ACTION_KHATMAH_REMINDER =
            "com.quran.quranaudio.online.dailyverse.ACTION_KHATMAH_REMINDER";
    private static final int ALARM_REQUEST_CODE = 900740;

    private static final int HOURS_AFTER_FAJR = 3;
    private static final LocalTime FALLBACK_TIME = LocalTime.of(10, 0);

    private KhatmahReminderScheduler() {
    }

    public static void scheduleNext(Context context) {
        try {
            // 无激活计划 / 关了提醒 → 取消，不打扰
            if (!KhatmahPlanManager.isActive(context) || !KhatmahPlanManager.isReminderEnabled(context)) {
                cancel(context);
                return;
            }

            LocalDateTime trigger = computeNextTrigger(context);
            long millis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;
            // 非精确闹钟(不需要 SCHEDULE_EXACT_ALARM 权限)；早晨提醒晚几分钟无碍
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, createPendingIntent(context));
            Log.i(TAG, "📿 Khatmah reminder scheduled at " + trigger);
        } catch (Exception e) {
            Log.e(TAG, "scheduleNext failed", e);
        }
    }

    public static void cancel(Context context) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (am != null) am.cancel(createPendingIntent(context));
        } catch (Exception e) {
            Log.e(TAG, "cancel failed", e);
        }
    }

    private static LocalDateTime computeNextTrigger(Context context) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = null;

        DayPrayer dayPrayer = PrayerTimesWidgetData.load(context);
        if (dayPrayer != null && dayPrayer.getTimings() != null) {
            LocalDateTime fajr = dayPrayer.getTimings().get(PrayerEnum.FAJR);
            if (fajr != null) {
                LocalDateTime todayFajr = LocalDateTime.of(now.toLocalDate(), fajr.toLocalTime());
                candidate = todayFajr.plusHours(HOURS_AFTER_FAJR);
            }
        }
        if (candidate == null) {
            candidate = LocalDateTime.of(now.toLocalDate(), FALLBACK_TIME);
        }
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    private static PendingIntent createPendingIntent(Context context) {
        Intent intent = new Intent(context, KhatmahReminderReceiver.class);
        intent.setAction(ACTION_KHATMAH_REMINDER);
        return PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
