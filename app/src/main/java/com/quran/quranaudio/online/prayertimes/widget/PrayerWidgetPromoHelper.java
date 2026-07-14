package com.quran.quranaudio.online.prayertimes.widget;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.quran.quranaudio.online.R;

import java.util.HashMap;
import java.util.Map;

/**
 * 桌面 Widget 的添加引导（分发漏斗）。
 *
 * 三层触点：
 * 1. 上下文促活弹窗：用户第 {@link #MIN_PRAYER_TAB_VISITS} 次进入祈祷页（已证明关心
 *    祈祷时间的 aha 时刻）弹一次说服文案，点"添加"调起系统 Pin 确认框
 *    （AppWidgetManager.requestPinAppWidget，官方合规 API，系统 UI 用户可随时取消）。
 * 2. 设置页常驻入口：拒绝过的用户随时可回来添加（长尾兜底）。
 * 3. 埋点全覆盖：promo 曝光/接受/拒绝 + Widget 实际添加/移除（Provider 生命周期），
 *    支撑"Widget 用户 vs 非 Widget 用户留存"归因分析。
 *
 * 频控与合规约束：
 * - 促活弹窗一生只弹一次，用户任何明确决定（添加/以后再说）后永不再弹；
 * - Widget 已在桌面时一切引导静默；
 * - 与通知权限请求流互斥（本页有延迟 3s 的权限请求）：权限未解决前本会话不弹，
 *   避免弹窗叠加轰炸；
 * - launcher 不支持 Pin 时不弹促活（打扰性价比低），设置页入口降级为手动步骤指引。
 */
public final class PrayerWidgetPromoHelper {

    private static final String TAG = "PrayerWidgetPromo";

    private static final String PREFS_NAME = "PRAYER_WIDGET_PROMO";
    private static final String KEY_PRAYER_TAB_VISITS = "PRAYER_TAB_VISITS";
    private static final String KEY_PROMO_DECIDED = "PROMO_DECIDED";

    private static final int MIN_PRAYER_TAB_VISITS = 3;

    // 进程内一次性护栏：防止同一时刻两个触点（通知开启回调 + onResume 访问计数）
    // 同时弹出、叠加两个弹窗。KEY_PROMO_DECIDED 仅在用户操作后写入，无法覆盖这段竞态窗口。
    private static volatile boolean promoShownThisProcess = false;

    private PrayerWidgetPromoHelper() {
    }

    // ============================================================
    // 对外入口
    // ============================================================

    /**
     * 祈祷页每次可见时调用：累计访问次数，第 {@link #MIN_PRAYER_TAB_VISITS} 次起且条件满足
     * 则弹促活弹窗。作为"通知开启后触发"的长尾兜底（覆盖没开任何通知、但常来看时间的用户）。
     */
    public static void onPrayerTabVisible(Activity activity) {
        try {
            SharedPreferences prefs = getPrefs(activity);
            int visits = prefs.getInt(KEY_PRAYER_TAB_VISITS, 0) + 1;
            prefs.edit().putInt(KEY_PRAYER_TAB_VISITS, visits).apply();

            if (visits < MIN_PRAYER_TAB_VISITS) {
                return;
            }
            if (passesCommonGuards(activity)) {
                showPromoDialog(activity, "prayer_tab_visit");
            }
        } catch (Exception e) {
            // 引导绝不允许影响祈祷页本身
            Log.e(TAG, "onPrayerTabVisible failed", e);
        }
    }

    /**
     * 用户刚配置/开启祈祷通知后调用（最高意图时刻：他已明确要"祈祷提醒"，Widget 是天然补全，
     * 且此刻通知权限必已授予）。不受访问次数门槛限制，可在首个会话即触达。
     */
    public static void onPrayerNotificationEnabled(Activity activity) {
        try {
            if (passesCommonGuards(activity)) {
                showPromoDialog(activity, "notification_enabled");
            }
        } catch (Exception e) {
            // 引导绝不允许影响调用方
            Log.e(TAG, "onPrayerNotificationEnabled failed", e);
        }
    }

    /**
     * 设置页入口点击：支持 Pin 则直接调起系统确认框，否则展示手动添加步骤。
     */
    public static void requestAddFromSettings(Activity activity) {
        try {
            logEvent(activity, "settings_entry_click");
            if (isWidgetOnHomeScreen(activity)) {
                // 已添加：给手动指引对话框会让用户困惑，直接轻提示已在桌面
                android.widget.Toast.makeText(activity,
                        R.string.widget_prayer_times_label, android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            if (isPinSupported(activity)) {
                requestSystemPin(activity);
            } else {
                showManualInstructions(activity);
            }
        } catch (Exception e) {
            Log.e(TAG, "requestAddFromSettings failed", e);
        }
    }

    // ============================================================
    // 促活弹窗
    // ============================================================

    /**
     * 与触发时机无关的通用护栏（一生一次 / 已在桌面 / 支持 Pin / 通知权限已授予 / Activity 存活）。
     */
    private static boolean passesCommonGuards(Activity activity) {
        if (promoShownThisProcess) {
            return false;
        }
        if (getPrefs(activity).getBoolean(KEY_PROMO_DECIDED, false)) {
            return false;
        }
        if (isWidgetOnHomeScreen(activity)) {
            // 用户已自行添加：记为已决定，永不打扰
            markDecided(activity);
            return false;
        }
        if (!isPinSupported(activity)) {
            return false;
        }
        // Android 13+ 通知权限未授予时让位（不消耗促活机会）。通知开启后触发路径此时必已授予。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return !activity.isFinishing() && !activity.isDestroyed();
    }

    private static void showPromoDialog(Activity activity, String trigger) {
        promoShownThisProcess = true;
        logShown(activity, trigger);

        // 自定义内容视图：内嵌与真实 Widget 同构的预览卡（Show-don't-tell）
        android.view.View content = activity.getLayoutInflater()
                .inflate(R.layout.dialog_widget_promo, null);

        new AlertDialog.Builder(activity)
                .setView(content)
                .setPositiveButton(R.string.widget_promo_add, (dialog, which) -> {
                    markDecided(activity);
                    logEvent(activity, "promo_accept");
                    requestSystemPin(activity);
                })
                .setNegativeButton(R.string.widget_promo_later, (dialog, which) -> {
                    markDecided(activity);
                    logEvent(activity, "promo_later");
                })
                .setOnCancelListener(dialog -> {
                    // 返回键/点外部取消：同样视为明确拒绝，绝不二次打扰
                    markDecided(activity);
                    logEvent(activity, "promo_cancel");
                })
                .show();
    }

    // ============================================================
    // Pin 能力
    // ============================================================

    private static boolean isWidgetOnHomeScreen(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        if (manager == null) {
            return false;
        }
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(context, PrayerTimesWidgetProvider.class));
        return ids != null && ids.length > 0;
    }

    private static boolean isPinSupported(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        return manager != null && manager.isRequestPinAppWidgetSupported();
    }

    private static void requestSystemPin(Activity activity) {
        try {
            AppWidgetManager manager = AppWidgetManager.getInstance(activity);
            boolean requested = manager.requestPinAppWidget(
                    new ComponentName(activity, PrayerTimesWidgetProvider.class), null, null);
            // 实际添加成败以 Provider.onEnabled 埋点为准（系统确认框用户可取消）
            if (!requested) {
                showManualInstructions(activity);
            }
        } catch (Exception e) {
            Log.e(TAG, "requestSystemPin failed", e);
            showManualInstructions(activity);
        }
    }

    private static void showManualInstructions(Activity activity) {
        if (activity.isFinishing() || activity.isDestroyed()) {
            return;
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.widget_promo_title)
                .setMessage(R.string.widget_promo_manual_hint)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    // ============================================================
    // 状态与埋点
    // ============================================================

    private static SharedPreferences getPrefs(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private static void markDecided(Context context) {
        getPrefs(context).edit().putBoolean(KEY_PROMO_DECIDED, true).apply();
    }

    static void logEvent(Context context, String action) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("action", action);
            com.quran.quranaudio.online.analytics.AnalyticsManager
                    .getInstance(context).logEvent("prayer_widget_funnel", params);
        } catch (Exception e) {
            Log.e(TAG, "logEvent failed", e);
        }
    }

    /**
     * 曝光埋点带上触发来源（notification_enabled / prayer_tab_visit），
     * 便于分析哪个时机的 promo_shown→widget_added 转化更高。
     */
    private static void logShown(Context context, String trigger) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("action", "promo_shown");
            params.put("trigger", trigger);
            com.quran.quranaudio.online.analytics.AnalyticsManager
                    .getInstance(context).logEvent("prayer_widget_funnel", params);
        } catch (Exception e) {
            Log.e(TAG, "logShown failed", e);
        }
    }
}
