package com.quranaudio.common.ad

import android.content.Context

/**
 * 广告模块的付费状态门禁。读 SharedPreferences，避免 adlib 反向依赖 app 模块。
 *
 * 这里有两种互相独立的权益，不能混为一谈：
 *
 *   1. 订阅（is_subscribed）
 *      去掉全部广告，含激励广告——订阅用户不需要靠看激励广告解锁 Tafsir。
 *
 *   2. 一次性买断「去广告」（is_ad_free，商品 ID: removeads）
 *      终身去掉 开屏 / 原生 / Banner / 插屏，
 *      但 **保留激励广告**——用户仍可通过看激励广告解锁 Tafsir 等内容。
 *
 * 之所以要拆开：AdFactory.loadRewardAd / showRewardAd 原本也在查 isUserSubscribed。
 * 如果把买断标记直接并进那个方法，买了 removeads 的用户会连激励广告都加载不出来，
 * Tafsir 将永久锁死。所以激励广告继续只看 isUserSubscribed()，
 * 其余广告类型改看 shouldHideAds()。
 */
object SubscriptionChecker {

    private const val PREFS_NAME = "subscription_prefs"
    private const val KEY_IS_SUBSCRIBED = "is_subscribed"

    /** 一次性买断「去广告」。与 app 模块的 AdFreeEntitlement 写同一个 key。 */
    const val KEY_IS_AD_FREE = "is_ad_free"

    /**
     * 是否订阅用户。
     *
     * 语义保持不变：这是「全部广告都不展示（含激励广告）」的判定。
     * 仅供激励广告链路使用；其余广告类型请改用 [shouldHideAds]。
     */
    @JvmStatic
    fun isUserSubscribed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_SUBSCRIBED, false)
    }

    /** 是否买断了「去广告」。 */
    @JvmStatic
    fun isAdFree(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_AD_FREE, false)
    }

    /**
     * 是否应当隐藏「开屏 / 原生 / Banner / 插屏」。
     *
     * 订阅用户和买断去广告的用户都命中。
     * 激励广告**不要**用这个判定——用 [isUserSubscribed]。
     */
    @JvmStatic
    fun shouldHideAds(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_SUBSCRIBED, false) ||
                prefs.getBoolean(KEY_IS_AD_FREE, false)
    }
}
