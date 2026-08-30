package com.quran.quranaudio.online.ads;

import android.content.Context;

/**
 * 广告策略开关的唯一集中点。
 *
 * 建这个类的原因：广告的展示时机此前散落在各处（splash / 各 Fragment / 各 Activity），
 * 想调整一次策略要改十几个地方，也无法快速回滚。
 *
 * 所有开关都是常量，改一处即可全局生效；后续若接入 Firebase Remote Config
 * （firebase-config 依赖已在，但代码未接），只需把这里的常量读取换成 RC 取值，
 * 调用方一行都不用动。
 */
public final class AdPolicy {

    private AdPolicy() { }

    // ============================================================
    // 开屏广告
    // ============================================================

    /**
     * 安装后首次启动是否跳过开屏广告。
     *
     * 当前 = true（跳过）。
     *
     * 依据：GA4 显示 screen_view 用户数只有 session_start 的 52.9%，
     * 近一半的启动没能渲染出界面；而首启开屏广告正卡在新用户的必经路上
     * ——用户还没看到任何内容就先看一个全屏广告。
     * 同时这也贴近 AdMob 对 App Open 广告「不要在用户体验到 App 内容之前展示」的指引，
     * 以及 Play 的干扰性广告政策。
     *
     * 收入影响很小：14 天全部广告收入合计 $192.95，开屏只占其中一部分。
     *
     * 【要恢复首启开屏广告，把这里改回 false 即可，无需改其他任何代码。】
     */
    public static final boolean SKIP_APP_OPEN_AD_ON_FIRST_LAUNCH = true;

    // ============================================================
    // 插屏广告频控
    // ============================================================

    /**
     * 新装用户的插屏保护期（小时）。当前为 0：不再全局屏蔽新用户首日插屏。
     * 展示仍必须发生在答题结算、任务创建完成等自然断点，并继续受到下方
     * 最小间隔、会话上限、日上限和订阅状态保护。
     */
    public static final long INTERSTITIAL_NEW_USER_GRACE_HOURS = 0L;

    /** 两次插屏之间的最小间隔（分钟）。此前 InterstitialAdManager 完全没有频控。 */
    public static final long INTERSTITIAL_MIN_INTERVAL_MINUTES = 3L;

    /** 单次会话内插屏上限。 */
    public static final int INTERSTITIAL_MAX_PER_SESSION = 2;

    /** 单个自然日内插屏上限。 */
    public static final int INTERSTITIAL_MAX_PER_DAY = 5;

    // ============================================================
    // 去广告弹窗
    // ============================================================

    /** 关闭插屏后展示去广告弹窗的间隔（小时），避免打扰过频。 */
    public static final long AD_FREE_PROMO_MIN_INTERVAL_HOURS = 24L;

    /** 用户手动关掉去广告弹窗 N 次后不再自动弹（仍保留设置页的固定入口）。 */
    public static final int AD_FREE_PROMO_MAX_DISMISS = 3;

    /**
     * 把上面的频控参数同步给 adlib。在 Application 初始化时调用一次即可。
     *
     * adlib 不能反向依赖 app 模块，所以频控逻辑落在 adlib 的 InterstitialFrequencyCap，
     * 但唯一的调参入口保持在这里。即使本方法未被调用（例如某些后台进程路径），
     * adlib 侧也有与此处一致的默认值兜底，用户不会失去保护。
     */
    public static void applyToAdLib() {
        com.quranaudio.common.ad.InterstitialFrequencyCap.configure(
                INTERSTITIAL_NEW_USER_GRACE_HOURS,
                INTERSTITIAL_MIN_INTERVAL_MINUTES,
                INTERSTITIAL_MAX_PER_SESSION,
                INTERSTITIAL_MAX_PER_DAY);
    }

    /**
     * 是否应当跳过本次开屏广告。
     *
     * @param isFirstLaunch 是否安装后首次启动
     * @return 跳过时返回 true
     */
    public static boolean shouldSkipAppOpenAd(Context context, boolean isFirstLaunch) {
        if (isFirstLaunch && SKIP_APP_OPEN_AD_ON_FIRST_LAUNCH) {
            return true;
        }
        // 订阅用户 / 买断去广告用户
        return com.quranaudio.common.ad.SubscriptionChecker.shouldHideAds(context);
    }
}
