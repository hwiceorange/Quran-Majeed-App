package com.quran.quranaudio.online.prayertimes.widget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.SplashScreenActivity;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.job.PrayerUpdater;
import com.quran.quranaudio.online.prayertimes.timings.DayPrayer;
import com.quran.quranaudio.online.prayertimes.utils.TimingUtils;
import com.quran.quranaudio.online.prayertimes.utils.UiUtils;
import android.widget.RemoteViews;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 祈祷时间桌面 Widget（4x1）：五番时间 + 下一番倒计时 + Hijri 日期。
 *
 * 数据流：PrayerAlarmScheduler.scheduleAlarmsAndReminders()（所有祈祷时间重算的唯一
 * 汇聚点）→ PrayerTimesWidgetData 缓存 → notifyPrayerDataChanged() 刷新，保证 Widget
 * 与 App 内页面、通知闹钟展示同一份数据（含 Hijri 校准、计算方法等用户设置）。
 *
 * 刷新策略：
 * - 每个祈祷时间点 +60s 自排一次非精确闹钟（切换"下一番"高亮与倒计时基准）；
 * - 午夜 +90s 刷新日期并触发数据重算；
 * - updatePeriodMillis 30 分钟作为系统级兜底；
 * - 倒计时本身由 Chronometer 在launcher进程内走时，不消耗本应用电量。
 */
public class PrayerTimesWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "PrayerTimesWidget";
    private static final String ACTION_REFRESH =
            "com.quran.quranaudio.online.widget.ACTION_PRAYER_WIDGET_REFRESH";
    private static final String WORK_NAME_WIDGET_DATA_REFRESH = "WIDGET_PRAYER_DATA_REFRESH";
    private static final int REFRESH_ALARM_REQUEST_CODE = 900731;

    private static final int[] CELL_IDS = {
            R.id.widget_cell_0, R.id.widget_cell_1, R.id.widget_cell_2,
            R.id.widget_cell_3, R.id.widget_cell_4};
    private static final int[] NAME_IDS = {
            R.id.widget_name_0, R.id.widget_name_1, R.id.widget_name_2,
            R.id.widget_name_3, R.id.widget_name_4};
    private static final int[] TIME_IDS = {
            R.id.widget_time_0, R.id.widget_time_1, R.id.widget_time_2,
            R.id.widget_time_3, R.id.widget_time_4};

    // 颜色统一从 colors.xml 读取（widget_prayer_* 色组，与 App 品牌绿 #4e8545 同族），
    // 布局 XML 与代码动态染色共用同一来源，改主题只需改一处

    // ============================================================
    // 生命周期与广播
    // ============================================================

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        renderAll(context);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.i(TAG, "First widget added");
        // 漏斗终点埋点：真实添加（区别于 promo_accept 后在系统确认框取消的用户）
        PrayerWidgetPromoHelper.logEvent(context, "widget_added");
        renderAll(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.i(TAG, "Last widget removed, cancelling refresh alarm");
        PrayerWidgetPromoHelper.logEvent(context, "widget_removed");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(createRefreshPendingIntent(context));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (ACTION_REFRESH.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            renderAll(context);
        }
    }

    // ============================================================
    // 对外入口：数据更新钩子（由 PrayerAlarmScheduler 调用）
    // ============================================================

    /**
     * 祈祷数据重算完成后调用：写缓存，并在有 Widget 实例时刷新。
     * 任何异常都不允许影响调用方（闹钟调度）。
     */
    public static void notifyPrayerDataChanged(Context context, DayPrayer dayPrayer) {
        try {
            PrayerTimesWidgetData.save(context, dayPrayer);
            if (hasActiveWidgets(context)) {
                renderAll(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "notifyPrayerDataChanged failed", e);
        }
    }

    /**
     * 立即重渲染（有 Widget 时）。由 NotifierReceiver 在祈祷时间点的精确闹钟中调用，
     * 使"下一番"高亮切换与宣礼通知同一瞬间发生（自维护的非精确闹钟仅作兜底，
     * 覆盖用户关闭了某番通知因而没有精确闹钟的情况）。
     * 任何异常都不允许影响调用方（通知展示）。
     */
    public static void requestRefresh(Context context) {
        try {
            if (hasActiveWidgets(context)) {
                renderAll(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "requestRefresh failed", e);
        }
    }

    // ============================================================
    // 渲染
    // ============================================================

    private static boolean hasActiveWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return false;
        }
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(context, PrayerTimesWidgetProvider.class));
        return ids != null && ids.length > 0;
    }

    private static void renderAll(Context context) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            if (manager == null) {
                return;
            }
            int[] ids = manager.getAppWidgetIds(
                    new ComponentName(context, PrayerTimesWidgetProvider.class));
            if (ids == null || ids.length == 0) {
                return;
            }

            RemoteViews views = buildViews(context);
            manager.updateAppWidget(ids, views);
        } catch (Exception e) {
            Log.e(TAG, "renderAll failed", e);
        }
    }

    private static RemoteViews buildViews(Context context) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_prayer_times);

        // 点击整个 Widget 打开 App（经 Splash 保证首启/老用户路由正确）
        Intent launchIntent = new Intent(context, SplashScreenActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent launchPendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widget_root, launchPendingIntent);

        DayPrayer dayPrayer = PrayerTimesWidgetData.load(context);
        if (dayPrayer == null) {
            // 空状态：App 还没算出过祈祷时间（未完成定位），引导用户点击进入
            views.setViewVisibility(R.id.widget_empty_state, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.widget_content, android.view.View.GONE);
            return views;
        }

        views.setViewVisibility(R.id.widget_empty_state, android.view.View.GONE);
        views.setViewVisibility(R.id.widget_content, android.view.View.VISIBLE);

        Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();
        PrayerEnum[] prayers = PrayerEnum.values();
        LocalDateTime now = LocalDateTime.now();

        // 找下一番：今天第一个还没到的；五番都过了则指向明日 Fajr
        int nextIndex = -1;
        for (int i = 0; i < prayers.length; i++) {
            LocalDateTime timing = timings.get(prayers[i]);
            if (timing != null && now.isBefore(timing)) {
                nextIndex = i;
                break;
            }
        }

        LocalDateTime nextTiming;
        if (nextIndex >= 0) {
            nextTiming = timings.get(prayers[nextIndex]);
        } else {
            // Icha 之后：明日 Fajr ≈ 今日 Fajr + 1 天（逐日偏移约1分钟，
            // 午夜数据刷新后即精确；倒计时短暂近似优于无内容）
            nextIndex = 0;
            LocalDateTime todayFajr = timings.get(PrayerEnum.FAJR);
            nextTiming = (todayFajr != null) ? todayFajr.plusDays(1) : now.plusHours(1);
        }

        // 五番名称与时间；下一番列高亮
        for (int i = 0; i < prayers.length; i++) {
            views.setTextViewText(NAME_IDS[i], getPrayerDisplayName(context, prayers[i]));

            LocalDateTime timing = timings.get(prayers[i]);
            views.setTextViewText(TIME_IDS[i],
                    timing != null ? TimingUtils.formatTiming(timing) : "--:--");

            boolean isNext = (i == nextIndex);
            views.setInt(CELL_IDS[i], "setBackgroundResource",
                    isNext ? R.drawable.widget_next_prayer_bg : 0);
            views.setTextColor(TIME_IDS[i], androidx.core.content.ContextCompat.getColor(context,
                    isNext ? R.color.widget_prayer_accent : R.color.widget_prayer_text_primary));
            views.setTextColor(NAME_IDS[i], androidx.core.content.ContextCompat.getColor(context,
                    isNext ? R.color.widget_prayer_accent : R.color.widget_prayer_text_secondary));
        }

        // 头部：下一番名称 + 倒计时
        views.setTextViewText(R.id.widget_next_prayer_name,
                getPrayerDisplayName(context, prayers[nextIndex]));

        long nextTimingMillis = nextTiming.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        views.setChronometerCountDown(R.id.widget_countdown, true);
        views.setChronometer(R.id.widget_countdown,
                SystemClock.elapsedRealtime() + (nextTimingMillis - System.currentTimeMillis()),
                null, true);

        // Hijri 日期：直接使用 DayPrayer 内与 App 一致的（含用户校准）三元组
        views.setTextViewText(R.id.widget_hijri_date, formatHijriDate(context, dayPrayer));

        // 在下一番到点后 60s（或午夜后 90s，取先到者）自刷新
        scheduleNextRefresh(context, nextTimingMillis);

        // 缓存跨天了：触发一次后台重算（复用现有 PrayerUpdater），期间先展示缓存值
        if (!PrayerTimesWidgetData.isForToday(dayPrayer)) {
            enqueueDataRefresh(context);
        }

        return views;
    }

    private static String getPrayerDisplayName(Context context, PrayerEnum prayer) {
        int resId = context.getResources().getIdentifier(
                prayer.toString(), "string", context.getPackageName());
        return resId != 0 ? context.getString(resId) : prayer.toString();
    }

    private static String formatHijriDate(Context context, DayPrayer dayPrayer) {
        try {
            int monthResId = context.getResources().getIdentifier(
                    "hijri_month_" + dayPrayer.getHijriMonthNumber(), "string",
                    context.getPackageName());
            String monthName = monthResId != 0
                    ? context.getString(monthResId)
                    : String.valueOf(dayPrayer.getHijriMonthNumber());
            return UiUtils.formatHijriDate(
                    dayPrayer.getHijriDay(), monthName, dayPrayer.getHijriYear());
        } catch (Exception e) {
            Log.e(TAG, "formatHijriDate failed", e);
            return "";
        }
    }

    // ============================================================
    // 刷新调度
    // ============================================================

    private static void scheduleNextRefresh(Context context, long nextPrayerMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return;
        }

        long midnightMillis = LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli();
        long triggerAt = Math.min(nextPrayerMillis + 60_000L, midnightMillis + 90_000L);
        long now = System.currentTimeMillis();
        if (triggerAt <= now) {
            triggerAt = now + 60_000L;
        }

        // Widget 刷新用非精确闹钟即可：不依赖 SCHEDULE_EXACT_ALARM 权限，不占精确闹钟配额
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, triggerAt,
                createRefreshPendingIntent(context));
    }

    private static PendingIntent createRefreshPendingIntent(Context context) {
        Intent intent = new Intent(context, PrayerTimesWidgetProvider.class);
        intent.setAction(ACTION_REFRESH);
        return PendingIntent.getBroadcast(context, REFRESH_ALARM_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void enqueueDataRefresh(Context context) {
        try {
            OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(PrayerUpdater.class).build();
            WorkManager.getInstance(context).enqueueUniqueWork(
                    WORK_NAME_WIDGET_DATA_REFRESH, ExistingWorkPolicy.KEEP, request);
        } catch (Exception e) {
            // WorkManager 未初始化等极端情况：下个 30 分钟周期兜底会刷新
            Log.e(TAG, "enqueueDataRefresh failed", e);
        }
    }
}
