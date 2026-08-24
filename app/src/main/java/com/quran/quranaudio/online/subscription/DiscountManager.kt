package com.quran.quranaudio.online.subscription

import android.content.Context
import android.os.SystemClock
import com.android.billingclient.api.ProductDetails

/**
 * 折扣挽回状态机（按方案区分：月订阅首月5折 / 年订阅首年5折）。
 *
 * 合规底线：
 *  - 倒计时不可作弊。每个方案的起始时间戳只写一次；关掉重进、进程重启和应用升级都不重置；
 *    窗口一过即永久失效。卸载会清除本地 SharedPreferences，跨卸载资格仍须由 Play 侧约束。
 *  - 折扣价格一律取自 Play 返回的折扣 offer，本类只负责“是否/何时展示”，不产出任何价格。
 *  - 每个方案（月/年）各自一生只主动弹一次（用户主动关闭对应方案的付费时）。
 *
 * 「1 小时后恢复原价」在客户端的真实含义是：只在 1 小时窗口内展示这个折扣入口。
 * Play 侧的优惠资格由服务端判定，本类不声称过期后 Play 会拒绝该 offer。
 */
object DiscountManager {

    // Play Console 折扣优惠 ID：月/年的「首月/首年5折」优惠 ID 均为 "off"
    //（月订阅另有 "free" 是7天试用；结构探测会天然排除试用与基础方案）。
    const val YEARLY_DISCOUNT_OFFER_ID = "off"
    const val MONTHLY_DISCOUNT_OFFER_ID = "off"

    /** BillingClient 中 INFINITE_RECURRING 的枚举值。 */
    const val INFINITE_RECURRENCE = 1

    /** 折扣展示窗口：1 小时。 */
    const val WINDOW_MS = 60L * 60 * 1000

    /** 功修避让：距任一礼拜时间 ±10 分钟内不打扰。 */
    private const val WORSHIP_GUARD_MS = 10L * 60 * 1000

    private const val PREFS = "discount_recovery_prefs"
    private const val PRAYER_PREFS = "prayer_times_cache"
    private const val KEY_PRAYER_EPOCHS = "today_prayer_epochs"

    /** 挽回方案：月 / 年。各自持有独立的窗口、消费标志、折扣百分比缓存。 */
    enum class Plan(val key: String, val productId: String, val offerId: String) {
        MONTHLY("monthly", BillingManager.MONTHLY_PLAN_ID, MONTHLY_DISCOUNT_OFFER_ID),
        YEARLY("yearly", BillingManager.YEARLY_PLAN_ID, YEARLY_DISCOUNT_OFFER_ID);

        val startKey get() = "${key}_window_start_ms"
        val elapsedStartKey get() = "${key}_window_start_elapsed_ms"
        val consumedKey get() = "${key}_consumed"
        val percentKey get() = "${key}_percent"

        companion object {
            fun of(name: String?): Plan? = values().firstOrNull { it.key == name }
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---- 每方案独立的窗口状态 ----

    /** 该方案窗口是否已启动过（不论是否已过期）。 */
    fun hasWindowStarted(context: Context, plan: Plan): Boolean =
        prefs(context).contains(plan.startKey)

    /**
     * 该方案已终结：显式消费过，或窗口已启动且超过 1 小时。
     * 命中过期的一刻顺便落盘 consumed，让主页角标与后续判断都停下来。
     */
    fun isConsumed(context: Context, plan: Plan): Boolean {
        val p = prefs(context)
        if (p.getBoolean(plan.consumedKey, false)) return true
        val start = p.getLong(plan.startKey, -1L)
        val elapsedStart = p.getLong(plan.elapsedStartKey, -1L)
        val elapsedNow = SystemClock.elapsedRealtime()
        val expiredByWallClock =
            start > 0 && System.currentTimeMillis() - start >= WINDOW_MS
        // elapsedRealtime 不受用户修改系统时间影响；设备重启后数值会回绕，此时退回墙钟判断。
        val expiredByMonotonicClock =
            elapsedStart > 0 && elapsedNow >= elapsedStart &&
                elapsedNow - elapsedStart >= WINDOW_MS
        if (expiredByWallClock || expiredByMonotonicClock) {
            p.edit().putBoolean(plan.consumedKey, true).apply()
            return true
        }
        return false
    }

    /** 该方案窗口剩余毫秒；已终结或未启动返回 0。 */
    fun remainingMillis(context: Context, plan: Plan): Long {
        if (isConsumed(context, plan)) return 0
        val p = prefs(context)
        val start = p.getLong(plan.startKey, -1L)
        if (start <= 0) return 0
        val wallRemaining = start + WINDOW_MS - System.currentTimeMillis()
        val elapsedStart = p.getLong(plan.elapsedStartKey, -1L)
        val elapsedNow = SystemClock.elapsedRealtime()
        val elapsedRemaining =
            if (elapsedStart > 0 && elapsedNow >= elapsedStart) {
                elapsedStart + WINDOW_MS - elapsedNow
            } else {
                Long.MAX_VALUE
            }
        return minOf(wallRemaining, elapsedRemaining).coerceAtLeast(0)
    }

    /** 该方案折扣是否正在有效展示窗口内。 */
    fun isActive(context: Context, plan: Plan): Boolean = remainingMillis(context, plan) > 0

    /**
     * 当前处于有效展示窗口的方案（供主页角标显示）。都不活跃返回 null；
     * 若同时活跃（极少见），取剩余时间较长（即较晚启动）的一个。
     */
    fun activePlan(context: Context): Plan? =
        Plan.values()
            .filter { isActive(context, it) }
            .maxByOrNull { remainingMillis(context, it) }

    /**
     * 用户主动关闭该方案付费时，是否应拦截并弹出对应折扣挽回页。
     * 该方案：窗口从未启动、未消费、未订阅、Play 确有该方案折扣 offer、不在功修避让窗口内。
     */
    fun shouldInterceptClose(
        context: Context,
        plan: Plan,
        discountOfferAvailable: Boolean,
        isSubscribed: Boolean
    ): Boolean {
        if (isSubscribed) return false
        if (!discountOfferAvailable) return false
        if (hasWindowStarted(context, plan) || isConsumed(context, plan)) return false
        if (isWithinWorshipWindow(context)) return false
        return true
    }

    /** 启动该方案倒计时窗口——只在从未启动时写入，杜绝二次刷新。返回是否成功启动。 */
    fun startWindow(context: Context, plan: Plan): Boolean {
        val p = prefs(context)
        if (p.contains(plan.startKey) || p.getBoolean(plan.consumedKey, false)) return false
        p.edit()
            .putLong(plan.startKey, System.currentTimeMillis())
            .putLong(plan.elapsedStartKey, SystemClock.elapsedRealtime())
            .apply()
        return true
    }

    /** 标记该方案已消费（购买成功 / 窗口自然结束时调用），角标与后续拦截随之停止。 */
    fun markConsumed(context: Context, plan: Plan) {
        prefs(context).edit().putBoolean(plan.consumedKey, true).apply()
    }

    // ---- 折扣 offer 查找与价格（对任意方案通用） ----

    /**
     * 只接受 Play 返回且 offerId 精确为 off 的优惠。
     * Play Console 必须把 off 配置为“新客户获取/从未购买过本应用订阅”；Play 未返回即无资格。
     * 禁止按价格结构猜测，以免未来新增促销时误选其他 offerToken。
     */
    fun findDiscountOffer(product: ProductDetails?, plan: Plan): ProductDetails.SubscriptionOfferDetails? {
        val offers = product?.subscriptionOfferDetails ?: return null
        val expectedPeriod = when (plan) {
            Plan.MONTHLY -> "P1M"
            Plan.YEARLY -> "P1Y"
        }
        return offers.firstOrNull { offer ->
            val intro = introPhase(offer)
            val renewal = renewalPhase(offer)
            offer.offerId == plan.offerId &&
                intro?.billingPeriod == expectedPeriod &&
                intro.billingCycleCount == 1 &&
                renewal?.billingPeriod == expectedPeriod &&
                computePercent(offer) != null
        }
    }

    /** 折扣阶段：有限周期且价格 > 0（首月/首年的折后价）。 */
    fun introPhase(offer: ProductDetails.SubscriptionOfferDetails) =
        offer.pricingPhases.pricingPhaseList.firstOrNull {
            it.priceAmountMicros > 0 && it.recurrenceMode != INFINITE_RECURRENCE
        }

    /** 续订阶段：必须是无限周期且价格 > 0（原价），不以其他有限付费阶段兜底。 */
    fun renewalPhase(offer: ProductDetails.SubscriptionOfferDetails) =
        offer.pricingPhases.pricingPhaseList.firstOrNull {
            it.priceAmountMicros > 0 && it.recurrenceMode == INFINITE_RECURRENCE
        }

    /** 从真实 offer 计算折扣百分比；结构不完整返回 null。全程不硬编码数字。 */
    fun computePercent(offer: ProductDetails.SubscriptionOfferDetails): Int? {
        val intro = introPhase(offer)?.priceAmountMicros ?: return null
        val renewal = renewalPhase(offer)?.priceAmountMicros ?: return null
        if (renewal <= 0 || intro >= renewal) return null
        return ((renewal - intro) * 100 / renewal).toInt()
    }

    /** 缓存该方案真实折扣百分比，供主页角标读取（避免主页单独发起 billing 查询）。 */
    fun cacheDiscountPercent(context: Context, plan: Plan, percent: Int) {
        prefs(context).edit().putInt(plan.percentKey, percent).apply()
    }

    /** 主页角标用的该方案折扣百分比；未缓存返回 0。 */
    fun cachedDiscountPercent(context: Context, plan: Plan): Int =
        prefs(context).getInt(plan.percentKey, 0)

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
