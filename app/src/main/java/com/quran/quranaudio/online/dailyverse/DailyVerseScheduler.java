package com.quran.quranaudio.online.dailyverse;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.quran.quranaudio.online.prayertimes.widget.PrayerTimesWidgetData;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * 每日经文通知的调度器（P1-2：非重度用户的第二条召回线）。
 *
 * 触发时刻：Fajr 后 1 小时 —— 晨礼后的诵读时段，是一天中最贴合"读一节经文"
 * 心境的时刻；无祈祷数据时回退到本地时间 09:00。
 *
 * 数据来源：复用 Widget 的 DayPrayer 缓存（PrayerTimesWidgetData），零额外计算；
 * 缓存由 PrayerAlarmScheduler 在每次祈祷时间重算时刷新，本调度器也挂在同一钩子上，
 * 因此每天都会用当天真实的 Fajr 时间校准。
 *
 * 合规：非精确闹钟（不需要 SCHEDULE_EXACT_ALARM 权限），用户可在设置中一键关闭，
 * 也可在系统通知渠道级别屏蔽。
 */
public final class DailyVerseScheduler {

    private static final String TAG = "DailyVerseScheduler";

    public static final String ACTION_SHOW_DAILY_VERSE =
            "com.quran.quranaudio.online.dailyverse.ACTION_SHOW_DAILY_VERSE";
    private static final int ALARM_REQUEST_CODE = 900732;

    private static final int HOURS_AFTER_FAJR = 1;
    private static final LocalTime FALLBACK_TIME = LocalTime.of(9, 0);

    private DailyVerseScheduler() {
    }

    /**
     * 依据当前设置调度下一次每日经文通知（幂等，可重复调用）。
     * 开关关闭时取消已有调度。
     */
    public static void scheduleNext(Context context) {
        try {
            if (!DailyVersePreferences.isEnabled(context)) {
                cancel(context);
                return;
            }

            LocalDateTime trigger = computeNextTrigger(context);
            long triggerMillis = trigger.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager == null) {
                return;
            }

            // 非精确 + 可在 Doze 中投递：早晨通知晚几分钟无碍，换来零权限依赖
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis,
                    createPendingIntent(context));

            Log.i(TAG, "📖 Daily verse scheduled at " + trigger);
        } catch (Exception e) {
            Log.e(TAG, "scheduleNext failed", e);
        }
    }

    public static void cancel(Context context) {
        try {
            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(createPendingIntent(context));
                Log.i(TAG, "📖 Daily verse schedule cancelled");
            }
        } catch (Exception e) {
            Log.e(TAG, "cancel failed", e);
        }
    }

    /**
     * 下一次触发时刻：今天的 Fajr+1h（未过），否则明天的（近似 +24h，
     * 次日 PrayerUpdater 重算后会用真实 Fajr 校准，偏差仅约1分钟）。
     */
    private static LocalDateTime computeNextTrigger(Context context) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = null;

        LocalDateTime fajr = PrayerTimesWidgetData.getFajr(PrayerTimesWidgetData.load(context));
        if (fajr != null) {
            // 缓存可能是昨天的：把 Fajr 平移到今天再算
            LocalDateTime todayFajr = LocalDateTime.of(now.toLocalDate(), fajr.toLocalTime());
            candidate = todayFajr.plusHours(HOURS_AFTER_FAJR);
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
        Intent intent = new Intent(context, DailyVerseReceiver.class);
        intent.setAction(ACTION_SHOW_DAILY_VERSE);
        return PendingIntent.getBroadcast(context, ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
