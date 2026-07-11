package com.quran.quranaudio.online.prayertimes.notifier;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.prayertimes.common.ComplementaryTimingEnum;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.common.TimingType;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.utils.TimingUtils;
import com.quran.quranaudio.online.prayertimes.utils.UiUtils;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class PrayerAlarmScheduler {

    public static final String TAG = "PrayerAlarmScheduler";
    private final Context context;
    private final PreferencesHelper preferencesHelper;
    private final CannotScheduleExactAlarmNotification cannotScheduleExactAlarmNotification;

    @Inject
    public PrayerAlarmScheduler(Context context, PreferencesHelper preferencesHelper, CannotScheduleExactAlarmNotification cannotScheduleExactAlarmNotification) {
        this.context = context;
        this.preferencesHelper = preferencesHelper;
        this.cannotScheduleExactAlarmNotification = cannotScheduleExactAlarmNotification;
    }

    public void scheduleAlarmsAndReminders(@NonNull DayPrayer dayPrayer) {
        // Android 14+（targetSdk 33+）新安装默认不授予 SCHEDULE_EXACT_ALARM。
        // 此前这里直接 return，导致新装机一条祈祷通知都不排（静默失效）。
        // 现在改为：无精确闹钟权限时继续调度，scheduleAlarm() 内部降级为非精确闹钟
        // （可能延迟几分钟，但远好于完全不提醒）；canScheduleExactAlarms() 仍会
        // 弹出引导通知，用户授权后 AlarmPermissionReceiver 会自动重新精确调度。
        if (!canScheduleExactAlarms()) {
            Log.w(TAG, "⚠️ Exact alarm permission missing, falling back to inexact scheduling");
        }

        scheduleNextPrayerAlarms(dayPrayer);

        if (preferencesHelper.isReminderEnabled()) {
            scheduleReminders(dayPrayer);
        }

        if (preferencesHelper.isDohaReminderEnabled()) {
            scheduleComplementaryTiming(dayPrayer, ComplementaryTimingEnum.DOHA, 1);
        }

        if (preferencesHelper.isLastThirdOfTheNightReminderEnabled()) {
            scheduleComplementaryTiming(dayPrayer, ComplementaryTimingEnum.LAST_THIRD_OF_THE_NIGHT, 2);
        }

        if (preferencesHelper.isSilenterEnabled()) {
            scheduleSilenter(dayPrayer);
        }

        // 🏠 同步桌面 Widget：这里是所有祈祷时间重算路径的唯一汇聚点，
        // 在此更新缓存可保证 Widget 与通知闹钟、App 内页面永远同源。
        // Widget 异常绝不允许影响闹钟调度，内部已全量捕获。
        com.quran.quranaudio.online.prayertimes.widget.PrayerTimesWidgetProvider
                .notifyPrayerDataChanged(context, dayPrayer);

        // 📖 每日经文通知：用当天真实 Fajr 校准下一次触发（Fajr+1h）。
        // 必须在 Widget 缓存写入之后调用（它读同一份 DayPrayer 缓存）。
        com.quran.quranaudio.online.dailyverse.DailyVerseScheduler.scheduleNext(context);
    }

    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmMgr.canScheduleExactAlarms()) {
                return true;
            } else {
                cannotScheduleExactAlarmNotification.createNotificationChannel();
                cannotScheduleExactAlarmNotification.createNotification();
                return false;
            }
        }
        return true;
    }

    private void scheduleNextPrayerAlarms(@NonNull DayPrayer dayPrayer) {
        Log.i(TAG, "Start scheduling Alarm for: " + dayPrayer.getDate());

        Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();

        int index = 0;
        for (PrayerEnum key : timings.keySet()) {
            index++;

            LocalDateTime prayerTiming = timings.get(key);

            String notificationType = preferencesHelper.getNotificationTypeForPrayer(key);
            if (PreferencesHelper.TYPE_NONE.equals(notificationType)) {
                Log.i(TAG, "⏭️ Skipping " + key.toString() + " Alarm (notification type = none)");
                continue;
            }

            if (prayerTiming != null && LocalDateTime.now().isBefore(prayerTiming)) {
                Log.i(TAG, "Scheduling " + key.toString() + " Alarm at : " + TimingUtils.formatTiming(prayerTiming));

                scheduleNotifications(dayPrayer, prayerTiming, TimingType.STANDARD, key.toString(),
                        1000, index, prayerTiming, NotifierReceiver.class);
            }
        }

        Log.i(TAG, "End scheduling Alarm for: " + dayPrayer.getDate());
    }

    private void scheduleReminders(@NonNull DayPrayer dayPrayer) {
        Log.i(TAG, "Start scheduling Reminders for: " + dayPrayer.getDate());

        Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();

        int index = 10;
        for (PrayerEnum key : timings.keySet()) {
            index++;

            String notificationType = preferencesHelper.getNotificationTypeForPrayer(key);
            if (PreferencesHelper.TYPE_NONE.equals(notificationType)) {
                Log.i(TAG, "⏭️ Skipping " + key.toString() + " Reminder (notification type = none)");
                continue;
            }

            // ✅ 检查该祷告是否启用了预提醒（独立配置）
            boolean preReminderEnabled = preferencesHelper.isPreReminderEnabledForPrayer(key);
            if (!preReminderEnabled) {
                Log.i(TAG, "⏭️ Skipping " + key.toString() + " Reminder (disabled in independent config)");
                continue;  // 跳过未启用预提醒的祷告
            }

            // ✅ 获取该祷告的预提醒间隔（独立配置）
            int reminderInterval = preferencesHelper.getPreReminderMinutesForPrayer(key);

            LocalDateTime prayerTiming = timings.get(key);
            LocalDateTime reminderTiming = Objects.requireNonNull(prayerTiming).minusMinutes(reminderInterval);

            if (LocalDateTime.now().isBefore(reminderTiming)) {

                Log.i(TAG, "✅ Scheduling " + key.toString() + " Reminder at : " + TimingUtils.formatTiming(reminderTiming) + " (" + reminderInterval + " minutes before)");

                scheduleNotifications(dayPrayer, prayerTiming, TimingType.STANDARD, key.toString(),
                        2000, index, reminderTiming, ReminderReceiver.class);
            } else {
                Log.i(TAG, "⏭️ Skipping " + key.toString() + " Reminder (time already passed)");
            }
        }

        Log.i(TAG, "End scheduling Reminders for: " + dayPrayer.getDate());
    }

    private void scheduleComplementaryTiming(@NonNull DayPrayer dayPrayer, ComplementaryTimingEnum complementaryTimingEnum, int requestCode) {
        Log.i(TAG, "Start scheduling Complementary Timings for: " + dayPrayer.getDate());

        Map<ComplementaryTimingEnum, LocalDateTime> complementaryTimings = dayPrayer.getComplementaryTiming();
        LocalDateTime complementaryTiming = complementaryTimings.get(complementaryTimingEnum);

        if (complementaryTiming != null && LocalDateTime.now().isBefore(complementaryTiming)) {

            Log.i(TAG, "Scheduling " + complementaryTimingEnum.toString() + " Reminder at : " + TimingUtils.formatTiming(complementaryTiming));

            scheduleNotifications(dayPrayer, complementaryTiming, TimingType.COMPLEMENTARY, complementaryTimingEnum.toString(),
                    3000, requestCode, complementaryTiming, ReminderReceiver.class);
        }

        Log.i(TAG, "End scheduling Complementary Timings for: " + dayPrayer.getDate());
    }

    private void scheduleNotifications(@NonNull DayPrayer dayPrayer, LocalDateTime prayerTiming, TimingType timingType,
                                       String prayerKey, int notificationId, int requestCode, LocalDateTime timingToSchedule,
                                       Class<? extends BroadcastReceiver> receiverClass) {

        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, receiverClass);
        intent.putExtra("prayerType", timingType.toString());
        intent.putExtra("prayerKey", prayerKey);
        intent.putExtra("prayerTiming", UiUtils.formatTiming(prayerTiming));
        intent.putExtra("prayerCity", dayPrayer.getCity());
        intent.putExtra("notificationId", notificationId);

        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        PendingIntent alarmIntent = PendingIntentCreator.getBroadcast(context, requestCode, intent, FLAG_UPDATE_CURRENT);
        alarmMgr.cancel(alarmIntent);

        scheduleAlarm(timingToSchedule, alarmMgr, alarmIntent);
    }

    private void scheduleSilenter(DayPrayer dayPrayer) {
        Log.i(TAG, "Start scheduling Silenter for: " + dayPrayer.getDate());

        Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();

        int index = 20;
        for (PrayerEnum key : timings.keySet()) {
            index++;

            LocalDateTime prayerTiming = timings.get(key);

            if (prayerTiming != null && LocalDateTime.now().isBefore(prayerTiming)) {
                AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                PendingIntent silentAlarmIntent = getSilenterPendingIntent(index, alarmMgr, true);
                PendingIntent unSilentAlarmIntent = getSilenterPendingIntent(index + 10, alarmMgr, false);

                LocalDateTime silentTiming = prayerTiming.plus(preferencesHelper.getSilenterStartTime(), ChronoUnit.MINUTES);
                scheduleAlarm(silentTiming, alarmMgr, silentAlarmIntent);
                Log.i(TAG, "Scheduling " + key.toString() + " Silenter at : " + TimingUtils.formatTiming(silentTiming));

                LocalDateTime unSilentTiming = getUnSilentTiming(prayerTiming, key);
                scheduleAlarm(unSilentTiming, alarmMgr, unSilentAlarmIntent);
                Log.i(TAG, "Scheduling " + key + " Un-Silenter at : " + TimingUtils.formatTiming(unSilentTiming));
            }
        }

        Log.i(TAG, "End scheduling Silenter for: " + dayPrayer.getDate());
    }


    private PendingIntent getSilenterPendingIntent(int index, AlarmManager alarmMgr, boolean turnToSilent) {
        Intent silentIntent = new Intent(context, SilenterReceiver.class);
        silentIntent.putExtra("TURN_TO_SILENT", turnToSilent);
        silentIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

        PendingIntent silentAlarmIntent = PendingIntentCreator.getBroadcast(context, index, silentIntent, FLAG_UPDATE_CURRENT);
        alarmMgr.cancel(silentAlarmIntent);
        return silentAlarmIntent;
    }

    private LocalDateTime getUnSilentTiming(LocalDateTime prayerTiming, PrayerEnum key) {
        int silenterInterval;

        if (prayerTiming.getDayOfWeek().equals(DayOfWeek.FRIDAY) && PrayerEnum.DHOHR.equals(key)) {
            silenterInterval = preferencesHelper.getSilenterIntervalForFridayPrayer();
        } else {
            silenterInterval = preferencesHelper.getSilenterInterval();
        }

        return prayerTiming.plus(preferencesHelper.getSilenterStartTime(), ChronoUnit.MINUTES).plus(silenterInterval, ChronoUnit.MINUTES);
    }

    private void scheduleAlarm(LocalDateTime timingToSchedule, AlarmManager alarmMgr, PendingIntent pendingIntent) {
        long triggerAtMillis = TimingUtils.getTimeInMilliIgnoringSeconds(timingToSchedule);

        // Android 12+ 无精确闹钟权限时调用 setExact* 会抛 SecurityException，
        // 降级为非精确闹钟保证通知仍会送达（系统可能延迟数分钟触发）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && !((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).canScheduleExactAlarms()) {
            alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmMgr.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmMgr.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmMgr.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (SecurityException e) {
            // 权限在检查与调用之间被回收的兜底
            Log.e(TAG, "SecurityException on exact alarm, falling back to inexact", e);
            alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
    }
}
