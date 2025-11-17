package com.quranaudio.common.ad

import android.content.Context

/**
 * Subscription status checker for ad module
 * Reads subscription status from SharedPreferences to avoid module dependency
 */
object SubscriptionChecker {
    
    private const val PREFS_NAME = "subscription_prefs"
    private const val KEY_IS_SUBSCRIBED = "is_subscribed"
    
    /**
     * Check if user is subscribed (premium user)
     * @param context Application or Activity context
     * @return true if user is subscribed, false otherwise
     */
    fun isUserSubscribed(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isSubscribed = prefs.getBoolean(KEY_IS_SUBSCRIBED, false)
        android.util.Log.d("SubscriptionChecker", "📊 Subscription check: $isSubscribed")
        return isSubscribed
    }
}

