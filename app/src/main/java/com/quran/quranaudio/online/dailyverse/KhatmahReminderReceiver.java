package com.quran.quranaudio.online.dailyverse;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.widget.PrayerTimesWidgetData;
import com.quran.quranaudio.online.quran_module.utils.KhatmahPlanManager;

/**
 * Khatmah 每日提醒投递端 —— 触发时做最终"是否该打扰"的多重判断。
 *
 * 严格遵守伊斯兰真实场景的保守原则：宁可不发，绝不冒犯。发出前必须全部满足：
 * - 计划激活 + 提醒开启；
 * - 系统通知权限已授予；
 * - 今日尚未读(已完成今日份则不打扰)；
 * - 非斋月最后 10 夜(Laylat al-Qadr，最神圣夜晚，静默不扰)。
 *
 * 措辞：温和鼓励(只报"今日诵读 Juz X")，绝不施压/说"落后几个"。斋月用特殊文案。
 */
public class KhatmahReminderReceiver extends BroadcastReceiver {

    private static final String TAG = "KhatmahReminder";
    private static final String CHANNEL_ID = "khatmah_reminder_channel";
    private static final int NOTIFICATION_ID = 900741;
    private static final int CONTENT_REQUEST_CODE = 900742;

    private static final int RAMADAN_HIJRI_MONTH = 9;
    private static final int LAST_TEN_NIGHTS_START_DAY = 21;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!KhatmahReminderScheduler.ACTION_KHATMAH_REMINDER.equals(intent.getAction())) {
            return;
        }
        // 无论成败，先把明天排上(链不断)
        KhatmahReminderScheduler.scheduleNext(context);

        try {
            if (!KhatmahPlanManager.isActive(context) || !KhatmahPlanManager.isReminderEnabled(context)) {
                return;
            }
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                return;
            }
            // 今天已读今日份 → 不打扰
            if (KhatmahPlanManager.hasReadToday(context)) {
                Log.d(TAG, "Already read today, skipping reminder");
                return;
            }
            // 斋月最后 10 夜 → 静默不扰(最神圣的礼拜时段)
            if (isRamadanLastTenNights(context)) {
                Log.d(TAG, "Ramadan last 10 nights, staying silent");
                return;
            }

            showReminder(context);
        } catch (Exception e) {
            Log.e(TAG, "onReceive failed", e);
        }
    }

    private boolean isRamadanLastTenNights(Context context) {
        try {
            DayPrayer dp = PrayerTimesWidgetData.load(context);
            if (dp == null) return false;
            return dp.getHijriMonthNumber() == RAMADAN_HIJRI_MONTH
                    && dp.getHijriDay() >= LAST_TEN_NIGHTS_START_DAY;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isRamadan(Context context) {
        try {
            DayPrayer dp = PrayerTimesWidgetData.load(context);
            return dp != null && dp.getHijriMonthNumber() == RAMADAN_HIJRI_MONTH;
        } catch (Exception e) {
            return false;
        }
    }

    private void showReminder(Context context) {
        int day = KhatmahPlanManager.getElapsedDay(context);
        int target = KhatmahPlanManager.getTargetDays(context);
        int todayJuz = Math.min(day, KhatmahPlanManager.TOTAL_JUZ); // 温和：报"今日建议诵读的 Juz"，不提落后

        // 温和鼓励措辞：斋月特殊 vs 平时；均不施压
        String title;
        String body;
        if (isRamadan(context)) {
            title = context.getString(R.string.khatmah_notif_ramadan_title, day);
            body = context.getString(R.string.khatmah_notif_body, todayJuz);
        } else {
            title = context.getString(R.string.khatmah_notif_title);
            body = context.getString(R.string.khatmah_notif_body, todayJuz);
        }

        createChannel(context);

        // 点击打开 App(首页 Khatmah 卡)
        Intent open = new Intent(context, com.quran.quranaudio.online.SplashScreenActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, CONTENT_REQUEST_CODE, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_on_24dp_blue)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, b.build());
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission revoked", e);
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.khatmah_notif_channel),
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager m = context.getSystemService(NotificationManager.class);
            if (m != null) m.createNotificationChannel(ch);
        }
    }
}
