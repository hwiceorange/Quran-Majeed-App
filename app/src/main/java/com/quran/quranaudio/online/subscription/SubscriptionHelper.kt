package com.quran.quranaudio.online.subscription

import android.content.Context
import android.content.Intent

/**
 * 订阅功能辅助类
 * 提供订阅状态检查和导航功能
 */
object SubscriptionHelper {

    private const val PREFS_NAME = "subscription_prefs"
    private const val KEY_IS_SUBSCRIBED = "is_subscribed"
    private const val KEY_PRODUCT_ID = "product_id"
    private const val KEY_LAST_CHECK_TIME = "last_check_time"

    /**
     * 检查用户是否已订阅
     */
    fun isUserSubscribed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_SUBSCRIBED, false)
    }

    /**
     * 获取订阅的产品ID
     */
    fun getSubscribedProductId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PRODUCT_ID, null)
    }

    /**
     * 检查用户是否订阅了年度套餐
     */
    fun isYearlySubscriber(context: Context): Boolean {
        return getSubscribedProductId(context) == BillingManager.YEARLY_PLAN_ID
    }

    /**
     * 检查用户是否订阅了月度套餐
     */
    fun isMonthlySubscriber(context: Context): Boolean {
        return getSubscribedProductId(context) == BillingManager.MONTHLY_PLAN_ID
    }

    /**
     * 获取上次检查订阅状态的时间
     */
    fun getLastCheckTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_CHECK_TIME, 0L)
    }

    /**
     * 启动订阅页面
     */
    fun launchSubscriptionPage(context: Context) {
        val intent = Intent(context, SubscriptionActivity::class.java)
        context.startActivity(intent)
    }

    /**
     * 清除订阅状态（用于测试）
     */
    fun clearSubscriptionStatus(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    /**
     * 获取订阅状态的可读字符串
     */
    fun getSubscriptionStatusString(context: Context): String {
        return if (isUserSubscribed(context)) {
            val productId = getSubscribedProductId(context)
            when (productId) {
                BillingManager.YEARLY_PLAN_ID -> "Subscribed (Yearly)"
                BillingManager.MONTHLY_PLAN_ID -> "Subscribed (Monthly)"
                else -> "Subscribed (Unknown)"
            }
        } else {
            "Not Subscribed"
        }
    }
}

