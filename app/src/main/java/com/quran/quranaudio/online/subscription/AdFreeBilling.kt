package com.quran.quranaudio.online.subscription

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.quranaudio.common.ad.SubscriptionChecker

/**
 * 一次性买断「去广告」（商品 ID: removeads）。
 *
 * 权益范围：终身去掉 开屏 / 原生 / Banner / 插屏；**保留激励广告**，
 * 用户仍可通过看激励广告解锁 Tafsir 等内容。
 * 门禁拆分见 [SubscriptionChecker]：其余广告走 shouldHideAds()，激励广告继续走 isUserSubscribed()。
 *
 * 刻意做成自包含（自建 BillingClient），不去改 BillingManager：
 * 那条链路承载着订阅支付，任何改动都可能影响现有付费用户，风险不对等。
 * 这也与既有的 SubscriptionEntitlementSync 保持同一种模式。
 */
object AdFreeBilling {

    private const val TAG = "AdFreeBilling"
    const val PRODUCT_ID = "removeads"

    private const val PREFS = "subscription_prefs"
    private const val KEY_PRICE = "ad_free_price"
    private const val KEY_LAST_RESTORE = "ad_free_last_restore"

    /** 权益复核间隔。比订阅侧（6h）更短：买断是终身权益，复核失败的代价是付费用户看广告。 */
    private const val RESTORE_INTERVAL_MS = 3 * 60 * 60 * 1000L

    /** 购买流程回调。全部在主线程调用。 */
    interface Callback {
        fun onSuccess()
        /** userCancelled 为 true 时不要弹错误提示——用户主动取消不是错误。 */
        fun onFailure(code: Int, message: String, userCancelled: Boolean)
    }

    @JvmStatic
    fun isAdFree(context: Context): Boolean = SubscriptionChecker.isAdFree(context)

    /** 缓存的本地化价格（如 "US$3.99"）。取不到时返回空串，UI 应能容忍。 */
    @JvmStatic
    fun cachedPrice(context: Context): String =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PRICE, "").orEmpty()

    /**
     * 启动时的权益复核 + 价格预取。带节流，不会每次启动都连 Billing。
     *
     * 为什么必须有：换机 / 重装 / 清除数据之后，本地的 is_ad_free 是 false，
     * 已付费用户会重新看到广告——这是 1 星差评的标准生成器。
     * 本地缓存只是加速手段，Google Play 才是权益的唯一事实来源。
     *
     * 说明一个无法消除的窗口：Billing 查询是异步的，重装后到复核返回之间
     * （通常 1~2 秒）原生/Banner 广告仍可能出现一次。阻塞等待会带来 ANR 风险，
     * 代价不对等，所以选择容忍这个窗口。开屏广告不受影响（首启已跳过），
     * 插屏也不受影响（新装 24h 保护期内本就不展示）。
     */
    @JvmStatic
    fun syncIfNeeded(context: Context) {
        val app = context.applicationContext
        val sp = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = sp.getLong(KEY_LAST_RESTORE, 0L)
        if (System.currentTimeMillis() - last < RESTORE_INTERVAL_MS) return
        sp.edit().putLong(KEY_LAST_RESTORE, System.currentTimeMillis()).apply()

        restore(app)
        // 顺带预取本地化价格，供设置页入口与弹窗 CTA 标价使用
        prefetchPrice(app)
    }

    /**
     * 从 Google Play 恢复已购买的去广告权益。
     *
     * 必须调用的场景：换机、重装、清除数据。
     * 否则已付费用户会重新看到广告——这是 1 星差评的标准生成器。
     */
    @JvmStatic
    fun restore(context: Context, onDone: ((Boolean) -> Unit)? = null) {
        val app = context.applicationContext
        runWithClient(app, onFail = { onDone?.invoke(isAdFree(app)) }) { client ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            client.queryPurchasesAsync(params) { result, purchases ->
                var owned = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    val p = purchases.firstOrNull {
                        it.products.contains(PRODUCT_ID) &&
                                it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    if (p != null) {
                        owned = true
                        grant(app)
                        // 未确认的购买在 3 天后会被 Google Play 自动退款，必须补确认
                        if (!p.isAcknowledged) acknowledge(client, p)
                    }
                    // 刻意「只授予、不撤销」：查不到购买时不把 is_ad_free 置回 false。
                    //
                    // 撤销看似能防退款白嫖，但代价不对等——网络异常、Play 服务暂时不可用、
                    // 用户临时切到别的 Google 账号，都会让查询返回空，
                    // 从而把已付费用户的权益抹掉、让他重新看到广告。
                    // 「付费用户绝不能看到广告」的优先级高于堵住少量退款漏洞。
                }
                Log.d(TAG, "restore: owned=$owned")
                client.endConnection()
                onDone?.invoke(owned)
            }
        }
    }

    /** 预取本地化价格，供入口按钮/弹窗展示。取不到不影响功能。 */
    @JvmStatic
    fun prefetchPrice(context: Context) {
        val app = context.applicationContext
        runWithClient(app, onFail = {}) { client ->
            queryDetails(client) { details ->
                details?.oneTimePurchaseOfferDetails?.formattedPrice?.let { price ->
                    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .edit().putString(KEY_PRICE, price).apply()
                }
                client.endConnection()
            }
        }
    }

    /** 拉起购买流程。 */
    @JvmStatic
    fun purchase(activity: Activity, callback: Callback) {
        val app = activity.applicationContext
        lateinit var client: BillingClient

        val listener = PurchasesUpdatedListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val p = purchases?.firstOrNull { it.products.contains(PRODUCT_ID) }
                    if (p != null && p.purchaseState == Purchase.PurchaseState.PURCHASED) {
                        grant(app)
                        if (!p.isAcknowledged) acknowledge(client, p)
                        activity.runOnUiThread { callback.onSuccess() }
                    } else {
                        // PENDING：如现金支付，权益要等支付完成后由 restore() 补上
                        activity.runOnUiThread {
                            callback.onFailure(result.responseCode, "pending", false)
                        }
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED ->
                    activity.runOnUiThread {
                        callback.onFailure(result.responseCode, "cancelled", true)
                    }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    // 已拥有但本地没记：直接补权益，不要让用户重复付费
                    grant(app)
                    activity.runOnUiThread { callback.onSuccess() }
                }
                else -> activity.runOnUiThread {
                    callback.onFailure(result.responseCode, result.debugMessage.orEmpty(), false)
                }
            }
        }

        client = BillingClient.newBuilder(app)
            .setListener(listener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()

        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    activity.runOnUiThread {
                        callback.onFailure(result.responseCode, "billing unavailable", false)
                    }
                    client.endConnection()
                    return
                }
                queryDetails(client) { details ->
                    if (details == null) {
                        activity.runOnUiThread {
                            callback.onFailure(-1, "product not found", false)
                        }
                        client.endConnection()
                        return@queryDetails
                    }
                    details.oneTimePurchaseOfferDetails?.formattedPrice?.let { price ->
                        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .edit().putString(KEY_PRICE, price).apply()
                    }
                    val flowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(details)
                                    .build()
                            )
                        )
                        .build()
                    activity.runOnUiThread { client.launchBillingFlow(activity, flowParams) }
                }
            }

            override fun onBillingServiceDisconnected() {
                // 保留最后已知权益；下次前台会话再试
            }
        })
    }

    // ============================================================
    // 内部
    // ============================================================

    private fun grant(app: Context) {
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(SubscriptionChecker.KEY_IS_AD_FREE, true)
            .apply()
        Log.d(TAG, "ad-free entitlement granted")
    }

    private fun acknowledge(client: BillingClient, purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        client.acknowledgePurchase(params) { r ->
            Log.d(TAG, "acknowledge: ${r.responseCode}")
        }
    }

    private fun queryDetails(client: BillingClient, onResult: (ProductDetails?) -> Unit) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, productDetailsResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                onResult(null)
                return@queryProductDetailsAsync
            }
            onResult(productDetailsResult.productDetailsList.firstOrNull())
        }
    }

    private inline fun runWithClient(
        app: Context,
        crossinline onFail: () -> Unit,
        crossinline block: (BillingClient) -> Unit
    ) {
        lateinit var client: BillingClient
        client = BillingClient.newBuilder(app)
            .setListener(PurchasesUpdatedListener { _, _ -> })
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    client.endConnection()
                    onFail()
                    return
                }
                block(client)
            }

            override fun onBillingServiceDisconnected() {
                onFail()
            }
        })
    }
}
