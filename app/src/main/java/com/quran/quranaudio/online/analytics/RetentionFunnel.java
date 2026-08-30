package com.quran.quranaudio.online.analytics;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import java.util.HashMap;
import java.util.Map;

/**
 * 留存漏斗埋点。
 *
 * 存在的理由：现有埋点能告诉我们「多少人流失」，但答不出「死在哪一步」。
 * 这里只做三件事，每一件都对应一个此前无法回答的问题：
 *
 *   1. 启动漏斗（rf_launch → rf_first_render）
 *      GA4 显示 screen_view 用户数只有 session_start 的 52.9%，
 *      即近一半的启动没能渲染出界面。以前无法定位死在哪，
 *      现在 rf_launch / rf_launch_ad / rf_first_render 三点带耗时，可以逐段看。
 *
 *   2. 引导逐页漏斗（rf_onb_page / rf_onb_next）
 *      以前只有 language_selected（点语言卡片才触发），
 *      first_open 42,641 → language_selected 14,266 这个缺口说不清是
 *      「没点卡片」还是「根本没走到」。现在每页的「看到」和「走完」分开记。
 *
 *   3. 邦克通知投递链（rf_adhan_shown / rf_adhan_open）+ 用户属性
 *      这是本文件最重要的部分。礼拜提醒是这个品类的核心留存杠杆，
 *      但我们从来不知道用户到底有没有真的收到过。
 *      rf_adhan_ok 这个用户属性一旦上报，就能在 GA4 里做
 *      「收到过邦克 vs 没收到过」的 D1/D7 留存分层——这是定位留存的第一优先分析。
 *
 * 设计约束（与既有的崩溃/ANR 修复一致）：
 *   - 所有公开方法全程 try/catch(Throwable)，埋点永远不能影响业务；
 *   - 不在启动关键路径做磁盘 IO 之外的重活，用独立的小 prefs 文件（非默认大文件）；
 *   - 只读不写业务数据，不碰任何 UI / 数据库 / 广告逻辑。
 */
public final class RetentionFunnel {

    private static final String PREFS = "retention_funnel";

    // 只用于「首次发生」判定的粘性标记
    private static final String K_FIRST_OPEN_AT = "first_open_at";
    private static final String K_ADHAN_EVER    = "adhan_ever_shown";
    private static final String K_VALUE_EVER    = "value_ever_reached";
    private static final String K_ONB_DONE      = "onboarding_done";

    /** 进程启动时刻，用于给启动漏斗打相对耗时 */
    private static final long PROCESS_START_UPTIME = SystemClock.uptimeMillis();

    /** 本次启动进入 splash 的时刻，rf_first_render 用它算「从启动到首帧」 */
    private static volatile long sLaunchUptime = 0L;

    private RetentionFunnel() { }

    // ============================================================
    // 启动漏斗
    // ============================================================

    /**
     * 用户主动打开 App（splash onCreate）。只在前台启动时调用。
     *
     * @param isFirstOpen 是否是安装后首次打开
     */
    public static void launch(Context ctx, boolean isFirstOpen) {
        try {
            sLaunchUptime = SystemClock.uptimeMillis();
            Map<String, Object> p = new HashMap<>();
            p.put("is_first_open", isFirstOpen);
            // 从进程创建到 splash 的耗时：Application.onCreate 那段有多重
            p.put("process_to_splash_ms", sLaunchUptime - PROCESS_START_UPTIME);
            p.put("device_tier", deviceTier(ctx));
            log(ctx, "rf_launch", p);

            if (isFirstOpen) {
                SharedPreferences sp = prefs(ctx);
                if (sp.getLong(K_FIRST_OPEN_AT, 0L) == 0L) {
                    sp.edit().putLong(K_FIRST_OPEN_AT, System.currentTimeMillis()).apply();
                }
            }
        } catch (Throwable ignored) { }
    }

    /**
     * 开屏广告的处置结果。shown=false 时 reason 说明为什么没展示
     * （first_launch_skip / no_consent / not_loaded / timeout / subscribed …）,
     * 这样「广告拖慢启动」和「广告没展示」可以分开归因。
     */
    public static void launchAd(Context ctx, boolean shown, String reason, long waitMs) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("shown", shown);
            p.put("reason", reason == null ? "none" : reason);
            p.put("wait_ms", waitMs);
            log(ctx, "rf_launch_ad", p);
        } catch (Throwable ignored) { }
    }

    /**
     * 第一个真正渲染出来的业务界面。
     *
     * 这是整套埋点里最重要的一个计数：
     * rf_first_render 的用户数 ÷ rf_launch 的用户数 = 「打开 App 的人里有多少真的看到了 App」。
     * 这个比值直接替代此前只能靠 screen_view/session_start 粗估的 52.9%。
     */
    public static void firstRender(Context ctx, String screen, boolean isFirstOpen) {
        try {
            long launch = sLaunchUptime;
            Map<String, Object> p = new HashMap<>();
            p.put("screen", screen);
            p.put("is_first_open", isFirstOpen);
            p.put("launch_to_render_ms", launch > 0 ? SystemClock.uptimeMillis() - launch : -1);
            p.put("device_tier", deviceTier(ctx));
            log(ctx, "rf_first_render", p);
        } catch (Throwable ignored) { }
    }

    // ============================================================
    // 引导逐页漏斗
    // ============================================================

    /** 引导页「被看到」。与 rf_onb_next 的差值 = 该页的流失。 */
    public static void onbPage(Context ctx, int index, String name) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("index", index);
            p.put("name", name);
            log(ctx, "rf_onb_page", p);
        } catch (Throwable ignored) { }
    }

    /** 引导页「被走完」。dwellMs 能看出用户是在犹豫还是在乱点。 */
    public static void onbNext(Context ctx, int index, String name, long dwellMs) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("index", index);
            p.put("name", name);
            p.put("dwell_ms", dwellMs);
            log(ctx, "rf_onb_next", p);
        } catch (Throwable ignored) { }
    }

    /** 引导全部走完。同时落一个粘性用户属性，用于分层留存。 */
    public static void onbDone(Context ctx, long totalMs) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("total_ms", totalMs);
            log(ctx, "rf_onb_done", p);
            prefs(ctx).edit().putBoolean(K_ONB_DONE, true).apply();
            userProp(ctx, "rf_onb_done", "true");
        } catch (Throwable ignored) { }
    }

    // ============================================================
    // 首次价值时刻
    // ============================================================

    /**
     * 用户第一次真正拿到东西（看到礼拜时间 / 读到经文 / 听到邦克）。
     * 只在每种 type 首次发生时上报，带「距首次安装多久」。
     *
     * 这是回答「用户装了但没得到任何价值就走了」的唯一手段。
     */
    public static void firstValue(Context ctx, String type) {
        try {
            SharedPreferences sp = prefs(ctx);
            String key = K_VALUE_EVER + "_" + type;
            if (sp.getBoolean(key, false)) {
                return;
            }
            sp.edit().putBoolean(key, true).apply();

            long firstOpen = sp.getLong(K_FIRST_OPEN_AT, 0L);
            Map<String, Object> p = new HashMap<>();
            p.put("type", type);
            p.put("ms_since_install", firstOpen > 0 ? System.currentTimeMillis() - firstOpen : -1);
            log(ctx, "rf_value", p);
        } catch (Throwable ignored) { }
    }

    /**
     * 真实价值行为。仅在用户确实完成动作后调用；duration/count 均来自业务现场，
     * 不用页面曝光或权限状态代替价值。事件参数保持低基数，便于 GA4 漏斗和留存分层。
     */
    public static void valueAction(Context ctx, String action, long durationMs, int count) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("action", safe(action));
            p.put("duration_bucket", durationBucket(durationMs));
            p.put("count_bucket", countBucket(count));
            log(ctx, "rf_value_action", p);
        } catch (Throwable ignored) { }
    }

    private static String durationBucket(long durationMs) {
        if (durationMs < 30_000L) return "lt_30s";
        if (durationMs < 60_000L) return "30_59s";
        if (durationMs < 180_000L) return "1_2m";
        if (durationMs < 300_000L) return "3_4m";
        if (durationMs < 600_000L) return "5_9m";
        return "10m_plus";
    }

    private static String countBucket(int count) {
        if (count <= 0) return "0";
        if (count == 1) return "1";
        if (count <= 3) return "2_3";
        if (count <= 10) return "4_10";
        return "11_plus";
    }

    // ============================================================
    // 邦克通知投递链（留存第一杠杆）
    // ============================================================

    /**
     * 邦克通知「真的弹出来了」。在 NotificationManagerCompat.notify() 之后调用。
     *
     * 注意：notify() 在 POST_NOTIFICATIONS 被拒时是静默失败，不抛异常。
     * 所以这里额外带上 permission 状态，才能区分
     * 「调用了但用户没权限」和「调用了且真的送达」。
     */
    public static void adhanShown(Context ctx, String prayer, boolean hasSound, boolean notifPermitted) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("prayer", prayer == null ? "unknown" : prayer);
            p.put("has_sound", hasSound);
            p.put("notif_permitted", notifPermitted);
            log(ctx, "rf_adhan_shown", p);

            // 只在真的有权限时才认为「收到过」，否则这个分层维度会被污染
            if (notifPermitted) {
                SharedPreferences sp = prefs(ctx);
                if (!sp.getBoolean(K_ADHAN_EVER, false)) {
                    sp.edit().putBoolean(K_ADHAN_EVER, true).apply();
                    // 粘性用户属性：GA4 里据此做「收到过邦克 vs 没收到过」的留存分层
                    userProp(ctx, "rf_adhan_ok", "true");
                    firstValue(ctx, "adhan_heard");
                }
            }
        } catch (Throwable ignored) { }
    }

    /** 用户点开了邦克通知——比「弹出来」更强的留存信号。 */
    public static void adhanOpened(Context ctx, String prayer) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("prayer", prayer == null ? "unknown" : prayer);
            log(ctx, "rf_adhan_open", p);
        } catch (Throwable ignored) { }
    }

    // ============================================================
    // Widget
    // ============================================================

    /** action: added / removed。触达率只有 1.3%，需要知道是没人加还是加了又删。 */
    public static void widget(Context ctx, String action) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("action", action);
            log(ctx, "rf_widget", p);
            userProp(ctx, "rf_has_widget", "added".equals(action) ? "true" : "false");
        } catch (Throwable ignored) { }
    }

    // ============================================================
    // Firebase 留存通知与订阅转化漏斗
    // ============================================================

    /** FCM 统一漏斗：received -> displayed -> opened。 */
    public static void push(Context ctx, String stage, String campaign, String target, String source) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("stage", safe(stage));
            p.put("campaign", safe(campaign));
            p.put("target", safe(target));
            p.put("source", safe(source));
            log(ctx, "rf_push", p);
        } catch (Throwable ignored) { }
    }

    /** 订阅统一漏斗：prompt/page/plan/trial/checkout/result/close。 */
    public static void subscription(Context ctx, String stage, String source,
                                    String plan, String offer, String result) {
        try {
            Map<String, Object> p = new HashMap<>();
            p.put("stage", safe(stage));
            p.put("source", safe(source));
            p.put("plan", safe(plan));
            p.put("offer", safe(offer));
            p.put("result", safe(result));
            log(ctx, "rf_subscription", p);
        } catch (Throwable ignored) { }
    }

    // ============================================================
    // 用户属性：留存分层的维度（此前几乎为零）
    // ============================================================

    /**
     * 在主界面就绪后调用一次，补齐所有分层维度。
     * 有了这些，GA4 的「用户留存」报告才能按人群拆开看。
     */
    public static void syncUserProps(Context ctx, boolean notifPermitted) {
        try {
            SharedPreferences sp = prefs(ctx);
            userProp(ctx, "rf_notif_perm", notifPermitted ? "granted" : "denied");
            userProp(ctx, "rf_adhan_ok", String.valueOf(sp.getBoolean(K_ADHAN_EVER, false)));
            userProp(ctx, "rf_onb_done", String.valueOf(sp.getBoolean(K_ONB_DONE, false)));
            userProp(ctx, "rf_device_tier", deviceTier(ctx));
            userProp(ctx, "rf_install_age", installAgeBucket(sp));
            userProp(ctx, "rf_app_lang", currentLanguage(ctx));
            boolean subscribed = ctx.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
                    .getBoolean("is_subscribed", false);
            userProp(ctx, "rf_subscribed", subscribed ? "true" : "false");
        } catch (Throwable ignored) { }
    }

    // ============================================================
    // 内部
    // ============================================================

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void log(Context ctx, String event, Map<String, Object> params) {
        AnalyticsManager.getInstance(ctx.getApplicationContext()).logEvent(event, params);
    }

    private static void userProp(Context ctx, String name, String value) {
        AnalyticsManager.getInstance(ctx.getApplicationContext()).setUserProperty(name, value);
    }

    private static String safe(String value) {
        if (value == null || value.trim().isEmpty()) return "unknown";
        String clean = value.trim();
        return clean.length() <= 80 ? clean : clean.substring(0, 80);
    }

    /**
     * 低端机分层。崩溃和 ANR 有 94% 集中在传音系低端机上，
     * 但我们一直没法把「低端机用户」当成一个可分析的人群。
     */
    private static String deviceTier(Context ctx) {
        try {
            ActivityManager am = (ActivityManager) ctx.getApplicationContext()
                    .getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) {
                return "unknown";
            }
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long gb = mi.totalMem / (1024L * 1024L * 1024L);
            if (gb <= 2) return "low";
            if (gb <= 4) return "mid";
            return "high";
        } catch (Throwable ignored) {
            return "unknown";
        }
    }

    private static String installAgeBucket(SharedPreferences sp) {
        long firstOpen = sp.getLong(K_FIRST_OPEN_AT, 0L);
        if (firstOpen <= 0L) {
            return "unknown";
        }
        long days = (System.currentTimeMillis() - firstOpen) / (24L * 60 * 60 * 1000);
        if (days < 1) return "d0";
        if (days < 2) return "d1";
        if (days < 8) return "d2_7";
        if (days < 31) return "d8_30";
        return "d30plus";
    }

    private static String currentLanguage(Context ctx) {
        try {
            return ctx.getResources().getConfiguration().getLocales().get(0).getLanguage();
        } catch (Throwable ignored) {
            return "unknown";
        }
    }
}
