package com.quran.quranaudio.online.subscription

import android.content.Context
import android.util.Log
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryPurchasesParams

/** Refreshes cached ad/premium entitlement from Google Play without opening the paywall. */
object SubscriptionEntitlementSync {
    private const val TAG = "SubscriptionSync"
    private const val MIN_SYNC_INTERVAL_MS = 6 * 60 * 60 * 1000L

    @JvmStatic
    fun syncIfNeeded(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong("last_check_time", 0L)
        if (System.currentTimeMillis() - lastCheck < MIN_SYNC_INTERVAL_MS) return

        lateinit var client: BillingClient
        client = BillingClient.newBuilder(appContext)
            .setListener(PurchasesUpdatedListener { _, _ -> })
            // Billing 8：enablePendingPurchases 需显式传入 PendingPurchasesParams。
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    client.endConnection()
                    return
                }
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
                client.queryPurchasesAsync(params) { queryResult, purchases ->
                    if (queryResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        val active = purchases.firstOrNull {
                            it.purchaseState == Purchase.PurchaseState.PURCHASED
                        }
                        prefs.edit()
                            .putBoolean("is_subscribed", active != null)
                            .putString("product_id", active?.products?.firstOrNull().orEmpty())
                            .putLong("last_check_time", System.currentTimeMillis())
                            .apply()
                        Log.d(TAG, "Entitlement refreshed: ${active != null}")
                    }
                    client.endConnection()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Preserve last-known entitlement; retry on a later foreground session.
            }
        })
    }
}
