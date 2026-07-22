package com.quran.quranaudio.online.subscription

import android.content.Context
import com.android.billingclient.api.ProductDetails

/**
 * 折扣挽回状态机（年订阅首次 5 折）。
 *
 * 合规底线：
 *  - 倒计时不可作弊。起始时间戳只写一次；关掉重进、卸载重装都不重置。窗口一过即永久失效。
 *  - 折扣价格一律取自 Play 返回的 `off` offer，本类只负责“是否/何时展示”，不产出任何价格。
 *  - 挽回页在整个生命周期内只主动弹一次（用户主动关闭付费页时），之后不再拦截。
 *
 * 「1 小时后恢复原价」在客户端的真实含义是：只在 1 小时窗口内展示这个折扣入口。
 * Play 侧的优惠资格由服务端判定，本类不声称过期后 Play 会拒绝该 offer。
 */
object DiscountManager {

    /** Play Console 中年订阅的折扣优惠 ID。改成「开发者决定」权限后即随 ProductDetails 下发。 */
    const val DISCOUNT_OFFER_ID = "off"

    private const val PREFS = "discount_recovery_prefs"
    private const val KEY_WINDOW_START = "window_start_ms"   // 只写一次
    private const val KEY_CONSUMED = "consumed"              // 永久一次性

    /** 折扣展示窗口：1 小时。 */
    const val WINDOW_MS = 60L * 60 * 1000

    /** 功修避让：距任一礼拜时间 ±10 分钟内不打扰。 */
    private const val WORSHIP_GUARD_MS = 10L * 60 * 1000

    // HomeFragment 顺手缓存的今日礼拜时间（epoch millis，逗号分隔），供避让判断读取。
    private const val PRAYER_PREFS = "prayer_times_cache"
    private const val KEY_PRAYER_EPOCHS = "today_prayer_epochs"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** 窗口是否已经启动过（不论是否已过期）。 */
    fun hasWindowStarted(context: Context): Boolean =
        prefs(context).contains(KEY_WINDOW_START)

    /**
     * 已终结：显式消费过，或窗口已启动且超过 1 小时。
     * 命中过期的一刻顺便落盘 consumed，让主页角标与后续判断都停下来。
     */
    fun isConsumed(context: Context): Boolean {
        val p = prefs(context)
        if (p.getBoolean(KEY_CONSUMED, false)) return true
        val start = p.getLong(KEY_WINDOW_START, -1L)
        if (start > 0 && System.currentTimeMillis() - start >= WINDOW_MS) {
            p.edit().putBoolean(KEY_CONSUMED, true).apply()
            return true
        }
        return false
    }

    /** 窗口剩余毫秒；已终结或未启动返回 0。 */
    fun remainingMillis(context: Context): Long {
        if (isConsumed(context)) return 0
        val start = prefs(context).getLong(KEY_WINDOW_START, -1L)
        if (start <= 0) return 0
        return (start + WINDOW_MS - System.currentTimeMillis()).coerceAtLeast(0)
    }

    /** 折扣是否正在有效展示窗口内（主页角标与折扣页据此显示）。 */
    fun isActive(context: Context): Boolean = remainingMillis(context) > 0

    /**
     * 用户主动关闭付费页时，是否应拦截并弹出折扣挽回页。
     * 仅一次：窗口从未启动、未消费、未订阅、Play 确有折扣 offer、不在功修避让窗口内。
     */
    fun shouldInterceptClose(
        context: Context,
        discountOfferAvailable: Boolean,
        isSubscribed: Boolean
    ): Boolean {
        if (isSubscribed) return false
        if (!discountOfferAvailable) return false
        if (hasWindowStarted(context) || isConsumed(context)) return false
        if (isWithinWorshipWindow(context)) return false
        return true
    }

    /** 启动倒计时窗口——只在从未启动时写入，杜绝二次刷新。返回是否成功启动。 */
    fun startWindow(context: Context): Boolean {
        val p = prefs(context)
        if (p.contains(KEY_WINDOW_START) || p.getBoolean(KEY_CONSUMED, false)) return false
        p.edit().putLong(KEY_WINDOW_START, System.currentTimeMillis()).apply()
        return true
    }

    /**
     * 在 Play 返回的年订阅 ProductDetails 中查找折扣 offer。
     * 先按 Console 的 offerId 精确匹配，再兜底到“含一个有限周期折扣阶段 + 一个无限续订阶段”的 offer。
     */
    fun findDiscountOffer(yearlyProduct: ProductDetails?): ProductDetails.SubscriptionOfferDetails? {
        val offers = yearlyProduct?.subscriptionOfferDetails ?: return null
        offers.firstOrNull { it.offerId == DISCOUNT_OFFER_ID }?.let { return it }
        // 兜底：任意“既有折扣阶段又有全价续订阶段”的 offer（迁移后 offerId 可能不同）
        return offers.firstOrNull { offer ->
            val phases = offer.pricingPhases.pricingPhaseList
            val hasIntro = phases.any { it.priceAmountMicros > 0 && it.recurrenceMode != INFINITE_RECURRENCE }
            val hasRenewal = phases.any { it.priceAmountMicros > 0 && it.recurrenceMode == INFINITE_RECURRENCE }
            hasIntro && hasRenewal
        }
    }

    /** BillingClient 中 INFINITE_RECURRING 的枚举值。 */
    const val INFINITE_RECURRENCE = 1

    private const val KEY_PERCENT = "discount_percent"

    /** 折扣阶段：有限周期且价格 > 0。 */
    fun introPhase(offer: ProductDetails.SubscriptionOfferDetails) =
        offer.pricingPhases.pricingPhaseList.firstOrNull {
            it.priceAmountMicros > 0 && it.recurrenceMode != INFINITE_RECURRENCE
        }

    /** 续订阶段：无限周期且价格 > 0；兜底取最后一个付费阶段。 */
    fun renewalPhase(offer: ProductDetails.SubscriptionOfferDetails) =
        offer.pricingPhases.pricingPhaseList.firstOrNull {
            it.priceAmountMicros > 0 && it.recurrenceMode == INFINITE_RECURRENCE
        } ?: offer.pricingPhases.pricingPhaseList.lastOrNull { it.priceAmountMicros > 0 }

    /** 从真实 offer 计算折扣百分比；结构不完整返回 null。全程不硬编码数字。 */
    fun computePercent(offer: ProductDetails.SubscriptionOfferDetails): Int? {
        val intro = introPhase(offer)?.priceAmountMicros ?: return null
        val renewal = renewalPhase(offer)?.priceAmountMicros ?: return null
        if (renewal <= 0 || intro >= renewal) return null
        return ((renewal - intro) * 100 / renewal).toInt()
    }

    /** 缓存真实折扣百分比，供主页角标读取（避免主页单独发起 billing 查询）。 */
    fun cacheDiscountPercent(context: Context, percent: Int) {
        prefs(context).edit().putInt(KEY_PERCENT, percent).apply()
    }

    /** 主页角标用的折扣百分比；未缓存返回 0。 */
    fun cachedDiscountPercent(context: Context): Int =
        prefs(context).getInt(KEY_PERCENT, 0)

    // ---- 功修避让：读 HomeFragment 缓存的今日礼拜时间 ----

    /** 由 HomeFragment 在算得当日礼拜时间后调用，缓存供避让判断使用。 */
    fun cacheTodayPrayerEpochs(context: Context, epochs: List<Long>) {
        context.getSharedPreferences(PRAYER_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRAYER_EPOCHS, epochs.joinToString(","))
            .apply()
    }

    /** 当前是否处于任一礼拜时间 ±10 分钟内。无缓存时安全返回 false（不阻断）。 */
    fun isWithinWorshipWindow(context: Context): Boolean {
        val raw = context.getSharedPreferences(PRAYER_PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PRAYER_EPOCHS, null) ?: return false
        val now = System.currentTimeMillis()
        return raw.split(",").mapNotNull { it.toLongOrNull() }.any { prayer ->
            kotlin.math.abs(now - prayer) <= WORSHIP_GUARD_MS
        }
    }
}
