package com.quran.quranaudio.online.dailyverse;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.activities.ActivityReader;
import com.quran.quranaudio.online.quran_module.components.quran.Quran;
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta;
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse;
import com.quran.quranaudio.online.quran_module.utils.reader.factory.ReaderFactory;
import com.quran.quranaudio.online.quran_module.utils.verse.VerseUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 每日经文通知的投递端。
 *
 * 内容一致性：经文选取复用 VerseUtils.getVOTD —— 与首页"今日经文"卡片
 * 是同一节（同一天内 VOTD 结果持久化），用户点通知进 App 后看到的内容自洽。
 *
 * 点击行为：直接打开 ActivityReader 定位到该节经文（复用 ReaderFactory 的
 * Intent 构造，走 Activity PendingIntent，符合 Android 12+ 通知 trampoline 限制）。
 *
 * 异步安全：QuranMeta/Quran 为异步加载，使用 goAsync() 保活并设 8 秒兜底，
 * 无论成败都会调度下一天。
 */
public class DailyVerseReceiver extends BroadcastReceiver {

    private static final String TAG = "DailyVerseReceiver";

    private static final String CHANNEL_ID = "daily_verse_channel";
    private static final int NOTIFICATION_ID = 900733;
    private static final int CONTENT_INTENT_REQUEST_CODE = 900734;
    private static final long ASYNC_TIMEOUT_MS = 8000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!DailyVerseScheduler.ACTION_SHOW_DAILY_VERSE.equals(intent.getAction())) {
            return;
        }

        // 无论本次成败，先把明天排上（召回线不能断）
        DailyVerseScheduler.scheduleNext(context);

        if (!DailyVersePreferences.isEnabled(context)) {
            return;
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.w(TAG, "Notifications disabled at OS level, skipping");
            return;
        }

        final PendingResult pendingResult = goAsync();
        final boolean[] finished = {false};
        final Handler handler = new Handler(Looper.getMainLooper());

        Runnable finishOnce = () -> {
            if (!finished[0]) {
                finished[0] = true;
                try {
                    pendingResult.finish();
                } catch (Exception ignored) {
                }
            }
        };

        // 8 秒兜底：异步加载卡住时释放广播，本日放弃（明天已排上）
        handler.postDelayed(finishOnce, ASYNC_TIMEOUT_MS);

        try {
            final Context appContext = context.getApplicationContext();
            QuranMeta.prepareInstance(appContext, quranMeta ->
                    Quran.prepareInstance(appContext, quranMeta, quran ->
                            VerseUtils.getVOTD(appContext, quranMeta, quran, (chapterNo, verseNo) -> {
                                try {
                                    showNotification(appContext, quranMeta, quran, chapterNo, verseNo);
                                } catch (Exception e) {
                                    Log.e(TAG, "showNotification failed", e);
                                } finally {
                                    handler.post(finishOnce);
                                }
                            })));
        } catch (Exception e) {
            Log.e(TAG, "Daily verse load failed", e);
            finishOnce.run();
        }
    }

    private void showNotification(Context context, QuranMeta quranMeta, Quran quran,
                                  int chapterNo, int verseNo) {
        if (chapterNo <= 0 || verseNo <= 0) {
            return;
        }

        Verse verse = quran.getVerse(chapterNo, verseNo);
        if (verse == null || verse.arabicText == null || verse.arabicText.isEmpty()) {
            return;
        }

        // 引用格式与首页 VOTD 卡片一致："章名 章:节"
        String reference = quranMeta.getChapterName(context, chapterNo)
                + " " + chapterNo + ":" + verseNo;

        createChannel(context);

        // 点击直达该节经文（与 VOTD 卡片同路径），Activity PendingIntent 合规
        Intent readerIntent = ReaderFactory.prepareSingleVerseIntent(chapterNo, verseNo)
                .setClass(context, ActivityReader.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context, CONTENT_INTENT_REQUEST_CODE, readerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_on_24dp_blue)
                .setContentTitle(context.getString(R.string.verse_of_day))
                .setContentText(reference)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(verse.arabicText + "\n\n" + reference))
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
            logEvent(context, "posted");
            Log.i(TAG, "📖 Daily verse posted: " + reference);
        } catch (SecurityException e) {
            // POST_NOTIFICATIONS 在投递瞬间被撤销的竞态
            Log.w(TAG, "Notification permission revoked", e);
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.verse_of_day),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.daily_verse_settings_summary));
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private static void logEvent(Context context, String action) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("action", action);
            com.quran.quranaudio.online.analytics.AnalyticsManager
                    .getInstance(context).logEvent("daily_verse_funnel", params);
        } catch (Exception e) {
            Log.e(TAG, "logEvent failed", e);
        }
    }
}
