package com.quran.quranaudio.online.subscription

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Google Play Billing 管理器
 * 处理所有订阅相关的逻辑
 */
class BillingManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) : PurchasesUpdatedListener {

    private var billingClient: BillingClient? = null
    private var isConnected = false
    
    // 订阅商品ID和优惠ID
    companion object {
        const val MONTHLY_PLAN_ID = "plan_monthly"
        const val YEARLY_PLAN_ID = "plan_year"
        const val FREE_TRIAL_OFFER_ID = "free"  // 月订阅7天免费试用优惠ID
        private const val TAG = "BillingManager"
    }

    // 回调接口
    interface BillingListener {
        fun onBillingSetupFinished(success: Boolean)
        fun onProductsLoaded(products: List<ProductDetails>)
        fun onPurchaseSuccess(purchase: Purchase)
        fun onPurchaseFailure(errorCode: Int, errorMessage: String)
        fun onSubscriptionStatusChanged(isSubscribed: Boolean, productId: String?)
    }

    private var billingListener: BillingListener? = null

    fun setBillingListener(listener: BillingListener) {
        this.billingListener = listener
    }

    /**
     * 初始化 Billing Client
     */
    fun initialize() {
        Log.d(TAG, "🔧 Initializing Billing Client...")
        
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            // Billing 8：enablePendingPurchases 需显式传入 PendingPurchasesParams。
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()

        startConnection()
    }

    /**
     * 启动连接到 Google Play
     */
    private fun startConnection() {
        billingClient?.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    isConnected = true
                    Log.d(TAG, "✅ Billing setup successful")
                    billingListener?.onBillingSetupFinished(true)
                    
                    // 查询现有订阅
                    queryExistingPurchases()
                } else {
                    isConnected = false
                    Log.e(TAG, "❌ Billing setup failed: ${billingResult.debugMessage}")
                    billingListener?.onBillingSetupFinished(false)
                }
            }

            override fun onBillingServiceDisconnected() {
                isConnected = false
                Log.w(TAG, "⚠️ Billing service disconnected, will retry...")
                // 可以实现重连逻辑
            }
        })
    }

    /**
     * 查询可用的订阅商品
     */
    fun querySubscriptionProducts() {
        if (!isConnected) {
            Log.e(TAG, "❌ Billing not connected")
            return
        }

        Log.d(TAG, "🔍 Querying subscription products...")
        
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(MONTHLY_PLAN_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(YEARLY_PLAN_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        coroutineScope.launch {
            billingClient?.queryProductDetailsAsync(params) { billingResult, queryResult ->
                // Billing 8：回调第二参由 List<ProductDetails> 改为 QueryProductDetailsResult。
                val productDetailsList = queryResult.productDetailsList
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Found ${productDetailsList.size} products")
                    productDetailsList.forEach { product ->
                        Log.d(TAG, "📦 Product: ${product.productId}")
                        product.subscriptionOfferDetails?.forEach { offer ->
                            Log.d(TAG, "  💰 Price: ${offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice}")
                        }
                    }
                    billingListener?.onProductsLoaded(productDetailsList)
                } else {
                    Log.e(TAG, "❌ Query products failed: ${billingResult.debugMessage}")
                    billingListener?.onProductsLoaded(emptyList())
                }
            }
        }
    }

    /**
     * 启动购买流程
     */
    fun launchPurchaseFlow(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String
    ) {
        if (!isConnected) {
            Log.e(TAG, "❌ Billing not connected")
            billingListener?.onPurchaseFailure(
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
                "Billing service not connected"
            )
            return
        }

        Log.d(TAG, "🚀 Launching purchase flow for: ${productDetails.productId}")

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.e(TAG, "❌ Failed to launch billing flow: ${billingResult?.debugMessage}")
            billingListener?.onPurchaseFailure(
                billingResult?.responseCode ?: -1,
                billingResult?.debugMessage ?: "Unknown error"
            )
        }
    }

    /**
     * 处理购买更新回调
     */
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    Log.d(TAG, "✅ Purchase successful: ${purchase.products}")
                    handlePurchase(purchase)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Log.d(TAG, "⚠️ User canceled purchase")
                billingListener?.onPurchaseFailure(
                    billingResult.responseCode,
                    "User canceled"
                )
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                Log.d(TAG, "⚠️ Item already owned")
                // 查询现有订阅
                queryExistingPurchases()
            }
            else -> {
                Log.e(TAG, "❌ Purchase failed: ${billingResult.debugMessage}")
                billingListener?.onPurchaseFailure(
                    billingResult.responseCode,
                    billingResult.debugMessage
                )
            }
        }
    }

    /**
     * 处理购买
     */
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                acknowledgePurchase(purchase)
            } else {
                Log.d(TAG, "✅ Purchase already acknowledged")
                billingListener?.onPurchaseSuccess(purchase)
                updateSubscriptionStatus(purchase)
            }
        } else if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "⏳ Purchase pending...")
        }
    }

    /**
     * 确认购买
     */
    private fun acknowledgePurchase(purchase: Purchase) {
        Log.d(TAG, "📝 Acknowledging purchase...")
        
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        coroutineScope.launch {
            billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Purchase acknowledged")
                    billingListener?.onPurchaseSuccess(purchase)
                    updateSubscriptionStatus(purchase)
                } else {
                    Log.e(TAG, "❌ Failed to acknowledge purchase: ${billingResult.debugMessage}")
                }
            }
        }
    }

    /**
     * 查询现有购买（订阅状态）
     */
    fun queryExistingPurchases() {
        if (!isConnected) {
            Log.e(TAG, "❌ Billing not connected")
            return
        }

        Log.d(TAG, "🔍 Querying existing purchases...")

        coroutineScope.launch {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient?.queryPurchasesAsync(params) { billingResult, purchasesList ->
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "✅ Found ${purchasesList.size} existing purchases")
                    
                    val activePurchase = purchasesList.firstOrNull {
                        it.purchaseState == Purchase.PurchaseState.PURCHASED
                    }
                    if (activePurchase == null) {
                        // A successful Play response is authoritative. Clear stale entitlement
                        // only here; transient billing/network failures keep the last known state.
                        saveSubscriptionStatus(false, "")
                        billingListener?.onSubscriptionStatusChanged(false, null)
                    } else {
                        Log.d(TAG, "✅ Active subscription: ${activePurchase.products}")
                        if (!activePurchase.isAcknowledged) {
                            acknowledgePurchase(activePurchase)
                        } else {
                            updateSubscriptionStatus(activePurchase)
                        }
                    }
                } else {
                    Log.e(TAG, "❌ Query purchases failed: ${billingResult.debugMessage}")
                    // Do not revoke a paid entitlement because Google Play is temporarily unavailable.
                }
            }
        }
    }

    /**
     * 更新订阅状态
     */
    private fun updateSubscriptionStatus(purchase: Purchase) {
        val productId = purchase.products.firstOrNull()
        billingListener?.onSubscriptionStatusChanged(true, productId)
        
        // 保存订阅状态到本地
        saveSubscriptionStatus(true, productId ?: "")
    }

    /**
     * 保存订阅状态到 SharedPreferences
     */
    private fun saveSubscriptionStatus(isSubscribed: Boolean, productId: String) {
        val prefs = context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("is_subscribed", isSubscribed)
            putString("product_id", productId)
            putLong("last_check_time", System.currentTimeMillis())
            apply()
        }
        Log.d(TAG, "💾 Subscription status saved: $isSubscribed, $productId")
    }

    /**
     * 获取本地保存的订阅状态
     */
    fun isSubscribed(): Boolean {
        val prefs = context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("is_subscribed", false)
    }

    /**
     * 获取订阅的产品ID
     */
    fun getSubscribedProductId(): String? {
        val prefs = context.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
        return prefs.getString("product_id", null)
    }

    /**
     * 释放资源
     */
    fun destroy() {
        Log.d(TAG, "🧹 Destroying Billing Manager")
        billingClient?.endConnection()
        billingClient = null
        isConnected = false
    }
}
