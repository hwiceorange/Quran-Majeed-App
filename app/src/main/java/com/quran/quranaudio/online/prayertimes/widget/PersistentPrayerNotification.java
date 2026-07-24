package com.quran.quranaudio.online.prayertimes.widget;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;

/**
 * 常驻「下一番倒计时」通知（可选，默认关闭）。
 *
 * 设计要点：
 *  - 完全复用 Widget 的本地缓存（{@link PrayerTimesWidgetData}）与到点刷新闹钟——不新增闹钟、不联网。
 *  - 倒计时用系统通知 Chronometer（setWhen + countDown），自走秒不需定时刷新。
 *  - 低优先级、静音渠道，不打扰；到点由 Widget 的 ACTION_REFRESH 闹钟顺带滚动到下一番。
 *  - 提供「Turn off」动作，嫌吵的用户可一键从通知关闭。
 *  - 开关默认 false（opt-in）。异常全量捕获，绝不影响调用方。
 */
public final class PersistentPrayerNotification {

    private static final String TAG = "PersistentPrayerNotif";
    public static final String PREF_KEY = "PERSISTENT_PRAYER_NOTIFICATION";
    private static final String CHANNEL_ID = "PRAYER_COUNTDOWN_CHANNEL";
    private static final int NOTIF_ID = 770011;

    private PersistentPrayerNotification() {}

    public static boolean isEnabled(Context context) {
        try {
            return PreferenceManager.getDefaultSharedPreferences(context)
                    .getBoolean(PREF_KEY, false);
        } catch (Exception e) {
            return false;
        }
    }

    /** 开关切换：写偏好并立即刷新/取消。 */
    public static void setEnabled(Context context, boolean enabled) {
        try {
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit().putBoolean(PREF_KEY, enabled).apply();
            if (enabled) {
                update(context);
            } else {
                cancel(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "setEnabled failed", e);
        }
    }

    /** 刷新常驻通知：读缓存、算下一番、发/更新通知。关闭或无数据时取消。 */
    public static void update(Context context) {
        try {
            if (!isEnabled(context)) {
                cancel(context);
                return;
            }
            PrayerTimesWidgetData.Snapshot snapshot = PrayerTimesWidgetData.load(context);
            if (snapshot == null || snapshot.prayerMillis == null) {
                cancel(context);
                return;
            }

            PrayerEnum[] prayers = PrayerEnum.values();
            long[] prayerMillis = snapshot.prayerMillis;
            long now = System.currentTimeMillis();

            // 下一番：今天第一个还没到的；都过了 → 明日 Fajr（≈今日 Fajr + 1 天）
            int nextIndex = -1;
            for (int i = 0; i < prayers.length && i < prayerMillis.length; i++) {
                if (prayerMillis[i] > 0 && now < prayerMillis[i]) {
                    nextIndex = i;
                    break;
                }
            }
            long nextMillis;
            int nameIndex;
            if (nextIndex >= 0) {
                nextMillis = prayerMillis[nextIndex];
                nameIndex = nextIndex;
            } else {
                long todayFajr = prayerMillis.length > 0 ? prayerMillis[0] : 0L;
                nextMillis = todayFajr > 0 ? todayFajr + 86_400_000L : now + 3_600_000L;
                nameIndex = 0;
            }

            String prayerName = getPrayerDisplayName(context, prayers[nameIndex]);

            createChannel(context);

            Intent openIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(context.getPackageName());
            PendingIntent contentPi = null;
            if (openIntent != null) {
                openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                contentPi = PendingIntent.getActivity(context, 0, openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            }

            // 「Turn off」动作：显式广播到 Widget Provider（已注册的 receiver）。
            Intent offIntent = new Intent(context, PrayerTimesWidgetProvider.class)
                    .setAction(PrayerTimesWidgetProvider.ACTION_DISABLE_PERSISTENT);
            PendingIntent offPi = PendingIntent.getBroadcast(context, 1, offIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder b = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.notification_ic)
                    .setContentTitle(context.getString(R.string.persistent_notif_next, prayerName))
                    .setContentText(formatMillis(context, nextMillis))
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setWhen(nextMillis)
                    .addAction(0, context.getString(R.string.persistent_notif_turn_off), offPi);
            // Chronometer 倒计时（API 24+）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                b.setChronometerCountDown(true);
            }
            if (contentPi != null) {
                b.setContentIntent(contentPi);
            }

            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(NOTIF_ID, b.build());
            }
        } catch (Exception e) {
            Log.e(TAG, "update failed", e);
        }
    }

    public static void cancel(Context context) {
        try {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIF_ID);
            }
        } catch (Exception e) {
            Log.e(TAG, "cancel failed", e);
        }
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null || nm.getNotificationChannel(CHANNEL_ID) != null) {
                return;
            }
            // 低优先级 + 静音：常驻但不打扰
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.persistent_notif_channel),
                    NotificationManager.IMPORTANCE_LOW);
            channel.setSound(null, null);
            channel.enableVibration(false);
            channel.setShowBadge(false);
            nm.createNotificationChannel(channel);
        }
    }

    private static String getPrayerDisplayName(Context context, PrayerEnum prayer) {
        int resId = context.getResources().getIdentifier(
                prayer.toString(), "string", context.getPackageName());
        return resId != 0 ? context.getString(resId) : prayer.toString();
    }

    private static String formatMillis(Context context, long epochMillis) {
        return com.quran.quranaudio.online.prayertimes.utils.TimingUtils.formatTiming(
                java.time.LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(epochMillis),
                        java.time.ZoneId.systemDefault()));
    }
}
