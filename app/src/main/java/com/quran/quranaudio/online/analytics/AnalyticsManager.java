package com.quran.quranaudio.online.analytics;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Firebase Analytics 统一管理类
 * 用于诊断用户留存率低（9.4%）和使用时长短（52秒）的问题
 * 
 * 核心分析目标：
 * 1. 启动漏斗：找出用户在哪个步骤流失
 * 2. 登录门槛：是否因强制登录导致流失
 * 3. 广告干扰：广告是否过早或过多打断用户体验
 * 4. 内容性能：内容加载慢是否导致用户离开
 * 5. 异常体验：错误提示是否让用户沮丧
 */
public class AnalyticsManager {
    private static final String TAG = "AnalyticsManager";
    
    private static volatile AnalyticsManager INSTANCE;
    private FirebaseAnalytics firebaseAnalytics;
    private ExecutorService executorService;
    
    // 应用启动时间戳（用于计算各步骤耗时）
    private long appStartTimestamp = 0;

    /**
     * 本进程创建时刻（单调时钟）。类加载即固定，不可被重置，也不受用户改系统时间影响。
     * logWorkflowStep 的耗时基于它，语义明确：距本进程创建多久。
     */
    private static final long PROCESS_START_ELAPSED = android.os.SystemClock.elapsedRealtime();
    
    private AnalyticsManager(Context context) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(context.getApplicationContext());
        executorService = Executors.newSingleThreadExecutor();
        appStartTimestamp = System.currentTimeMillis();
    }
    
    public static AnalyticsManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AnalyticsManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new AnalyticsManager(context);
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 重置启动时间（用于新的启动流程）
     */
    public void resetStartTimestamp() {
        appStartTimestamp = System.currentTimeMillis();
    }
    
    /**
     * 获取从启动到现在的毫秒数
     */
    public long getTimeSinceStart() {
        return System.currentTimeMillis() - appStartTimestamp;
    }
    
    /**
     * 获取从启动到现在的秒数
     */
    public long getSecondsSinceStart() {
        return getTimeSinceStart() / 1000;
    }
    
    /**
     * 通用的事件记录方法（异步执行，不阻塞主线程）
     */
    public void logEvent(String eventName, Map<String, Object> params) {
        executorService.execute(() -> {
            try {
                Bundle bundle = new Bundle();
                if (params != null) {
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        String key = entry.getKey();
                        Object value = entry.getValue();
                        
                        if (value instanceof String) {
                            bundle.putString(key, (String) value);
                        } else if (value instanceof Integer) {
                            bundle.putInt(key, (Integer) value);
                        } else if (value instanceof Long) {
                            bundle.putLong(key, (Long) value);
                        } else if (value instanceof Double) {
                            bundle.putDouble(key, (Double) value);
                        } else if (value instanceof Boolean) {
                            bundle.putBoolean(key, (Boolean) value);
                        } else if (value != null) {
                            bundle.putString(key, value.toString());
                        }
                    }
                }
                firebaseAnalytics.logEvent(eventName, bundle);
                Log.d(TAG, "✅ Event logged: " + eventName + " | params: " + params);
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to log event: " + eventName, e);
            }
        });
    }
    
    /**
     * 设置 Firebase 用户属性，用于"X vs 留存"分群交叉分析（如通知开启率 vs 次留）。
     * 用户属性会绑定到用户，Firebase 留存报告可据此分群对比。
     */
    public void setUserProperty(String name, String value) {
        executorService.execute(() -> {
            try {
                firebaseAnalytics.setUserProperty(name, value);
                Log.d(TAG, "✅ User property set: " + name + " = " + value);
            } catch (Exception e) {
                Log.e(TAG, "❌ Failed to set user property: " + name, e);
            }
        });
    }

    // ==================== 1. 启动与漏斗打点 ====================
    
    /**
     * 记录启动流程中的各个步骤
     * 分析价值：定位用户在启动过程中的流失点（冷启动太慢？卡在加载？）
     * 
     * @param stepName 步骤名称：splash_start, splash_finish, onboarding_language, 
     *                 onboarding_complete, home_view, main_content_loaded
     */
    public void logWorkflowStep(String stepName) {
        // 修复记录（此前该事件在 GA4 里达到 1,317 万条 / 每用户 127 次，数据不可用）：
        //
        // 1. 删除 timestamp 参数。GA4 本就给每条事件自带时间戳，这个参数完全冗余；
        //    而且它每条都是唯一值（高基数），作为数值型参数还会白占自定义指标配额
        //    （GA4 每媒体资源自定义维度/指标各上限 50 个），注册它毫无意义、不注册则纯属浪费。
        //
        // 2. duration_from_start 改用单调时钟。原实现基于 System.currentTimeMillis()，
        //    用户改系统时间或 NTP 校时都会污染它，甚至出现负值；
        //    SystemClock.elapsedRealtime() 不受这些影响。
        //    语义也更正为「距本进程创建」——原来的 appStartTimestamp 是
        //    AnalyticsManager 单例首次创建的时刻，在后台被闹钟/Widget 拉起的进程里
        //    和「应用启动」毫无关系，还能被 resetStartTimestamp() 任意重置。
        //
        // 3. 调用点污染（已在 App.java 修复）：Application.onCreate 里那两条会在
        //    每次进程创建时上报，包括后台唤醒，这是 127 次/人的主因。
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("step_name", stepName);
        params.put("ms_since_process_start",
                android.os.SystemClock.elapsedRealtime() - PROCESS_START_ELAPSED);
        logEvent("app_workflow_step", params);
    }
    
    // ==================== 2. 登录门槛分析 ====================
    
    /**
     * 记录登录相关行为
     * 分析价值：判断是否因登录门槛过高导致新用户流失
     * 
     * @param action 操作类型：show_login（显示登录页）, click_login（点击登录）, 
     *               click_skip（跳过登录）, click_third_party（第三方登录）
     * @param isForced 是否为强制登录（true=必须登录才能继续）
     */
    public void logLoginBarrier(String action, boolean isForced) {
        logEvent("login_barrier_stat", Map.of(
            "action", action,
            "is_forced", isForced,
            "seconds_since_start", getSecondsSinceStart()
        ));
    }
    
    // ==================== 3. 广告干扰诊断 ====================
    
    /**
     * 记录广告展示时机
     * 分析价值：判断广告是否过早打断用户，或频率过高导致用户离开
     * 
     * @param adType 广告类型：open_ad, interstitial, rewarded, native, banner
     * @param adLocation 广告出现的页面：splash, home, surah_detail, quiz_complete 等
     */
    public void logAdExposure(String adType, String adLocation) {
        logEvent("ad_exposure_metric", Map.of(
            "ad_type", adType,
            "seconds_since_launch", getSecondsSinceStart(),
            "ad_location", adLocation,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 记录广告加载失败（可能影响用户体验的卡顿）
     */
    public void logAdLoadFailed(String adType, String adLocation, String errorMsg) {
        logEvent("ad_load_failed", Map.of(
            "ad_type", adType,
            "ad_location", adLocation,
            "error_msg", errorMsg,
            "seconds_since_launch", getSecondsSinceStart()
        ));
    }
    
    // ==================== 4. 核心功能加载 ====================
    
    /**
     * 记录内容加载性能
     * 分析价值：判断内容加载慢是否导致用户在前 52 秒就离开
     * 
     * @param contentType 内容类型：surah_text（章节文本）, audio_stream（音频流）, 
     *                    audio_download（音频下载）, translation（翻译）, 
     *                    tafseer（注释）, prayer_times（祈祷时间）
     * @param status 状态：success, fail, timeout
     * @param latencyMs 加载耗时（毫秒）
     * @param errorMsg 失败原因（成功时为 null）
     */
    public void logContentPerformance(String contentType, String status, long latencyMs, String errorMsg) {
        if (errorMsg == null || errorMsg.isEmpty()) {
            logEvent("content_performance", Map.of(
                "content_type", contentType,
                "status", status,
                "latency_ms", latencyMs,
                "seconds_since_start", getSecondsSinceStart()
            ));
        } else {
            logEvent("content_performance", Map.of(
                "content_type", contentType,
                "status", status,
                "latency_ms", latencyMs,
                "error_msg", errorMsg,
                "seconds_since_start", getSecondsSinceStart()
            ));
        }
    }
    
    // ==================== 5. 异常 UI 弹出 ====================
    
    /**
     * 记录错误弹窗
     * 分析价值：统计用户遇到的错误类型和频率，判断是否因错误体验导致流失
     * 
     * @param dialogTitle 弹窗标题
     * @param message 提示信息（如：网络连接失败、权限被拒绝）
     * @param location 出现的页面位置
     */
    public void logUIException(String dialogTitle, String message, String location) {
        logEvent("ui_exception_alert", Map.of(
            "dialog_title", dialogTitle,
            "message", message,
            "location", location,
            "seconds_since_start", getSecondsSinceStart(),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    // ==================== 6. 页面停留时长 ====================
    
    /**
     * 记录页面停留时长
     * 分析价值：了解用户在哪个页面停留时间长（有价值内容）或短（无吸引力）
     * 
     * @param pageName 页面名称：home, surah_detail, audio_player, quiz, prayer_times 等
     * @param stayDurationMs 停留时长（毫秒）
     */
    public void logPageStayDuration(String pageName, long stayDurationMs) {
        logEvent("page_stay_duration", Map.of(
            "page_name", pageName,
            "stay_duration_ms", stayDurationMs,
            "stay_duration_sec", stayDurationMs / 1000,
            "seconds_since_start", getSecondsSinceStart()
        ));
    }
    
    // ==================== 7. 核心功能使用 ====================
    
    /**
     * 记录核心功能的使用情况
     * 分析价值：了解用户是否使用了核心功能（阅读、听音频、答题等）
     * 
     * @param featureName 功能名称：read_quran, play_audio, take_quiz, set_bookmark, 
     *                    share_verse, download_audio, set_reminder 等
     */
    public void logFeatureUsage(String featureName) {
        logEvent("feature_usage", Map.of(
            "feature_name", featureName,
            "seconds_since_start", getSecondsSinceStart(),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    // ==================== 8. 用户退出分析 ====================
    
    /**
     * 记录用户退出应用的原因和时机
     * 分析价值：了解用户为什么在 52 秒后就离开
     * 
     * @param exitReason 退出原因：back_button（按返回键）, app_switched（切换应用）, 
     *                   crash（崩溃）, normal（正常退出）
     * @param lastPage 最后停留的页面
     */
    public void logUserExit(String exitReason, String lastPage) {
        logEvent("user_exit", Map.of(
            "exit_reason", exitReason,
            "last_page", lastPage,
            "total_session_duration_sec", getSecondsSinceStart(),
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 关闭资源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}

