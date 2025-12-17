package com.quranaudio.common.ad

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/**
 * Central manager for native ads with caching and preload logic for onboarding pages.
 * 
 * Features:
 * - Maintains a cache pool with 1 available native ad
 * - Preloads ad on cold start
 * - Immediately requests new ad when one is consumed
 * - Checks premium subscription status before loading/showing ads
 * 
 * Usage:
 * - Call initialize() and preloadAd() in Application.onCreate()
 * - Call getCachedAd() in Fragment/Activity to retrieve and display the ad
 */
class NativeAdManager private constructor() {
    
    companion object {
        private const val TAG = "NativeAdManager"
        
        @Volatile
        private var instance: NativeAdManager? = null
        
        fun getInstance(): NativeAdManager {
            return instance ?: synchronized(this) {
                instance ?: NativeAdManager().also { instance = it }
            }
        }
    }
    
    // Application context for ad loading
    private var appContext: Context? = null
    
    // Cached native ad object (only one at a time)
    private var cachedNativeAd: NativeAd? = null
    
    // Loading state flag
    private var isLoading = false
    
    // Ad unit ID (reuse existing native ad ID from AdConfig)
    private val adUnitId: String
        get() = AdConfig.getAdIdByPosition(AdConfig.AD_NATIVE)
    
    // Pending load callbacks (for dynamic loading)
    private val pendingCallbacks = mutableListOf<(NativeAd?) -> Unit>()
    
    /**
     * Initialize the manager with application context.
     * Should be called in Application.onCreate().
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "✅ NativeAdManager initialized")
    }
    
    /**
     * Preload a native ad on cold start.
     * Should be called in Application.onCreate() after initialize().
     */
    fun preloadAd() {
        if (appContext == null) {
            Log.e(TAG, "❌ Cannot preload ad: NativeAdManager not initialized")
            return
        }
        
        loadNewAd()
        Log.d(TAG, "✅ Preload initiated")
    }
    
    /**
     * Load a new native ad.
     * - Clears old cache
     * - Checks subscription status
     * - Handles load success/failure
     */
    fun loadNewAd() {
        val context = appContext
        if (context == null) {
            Log.e(TAG, "❌ AppContext is null, cannot load ad")
            return
        }
        
        if (isLoading) {
            Log.d(TAG, "⚠️ Ad is already loading, skipping duplicate request")
            return
        }
        
        // Check subscription status
        if (SubscriptionChecker.isUserSubscribed(context)) {
            Log.d(TAG, "🎁 User is subscribed, skipping native ad load")
            return
        }
        
        // Clear old cache
        cachedNativeAd?.destroy()
        cachedNativeAd = null
        isLoading = true
        
        Log.d(TAG, "🔄 Loading new native ad with ID: $adUnitId")
        
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "✅ Native ad loaded successfully")
                cachedNativeAd = nativeAd
                isLoading = false
                
                // Notify pending callbacks
                notifyPendingCallbacks(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "❌ Failed to load native ad: ${loadAdError.message} (Code: ${loadAdError.code})")
                    cachedNativeAd = null
                    isLoading = false
                    
                    // Notify pending callbacks with null
                    notifyPendingCallbacks(null)
                }
                
                override fun onAdClicked() {
                    Log.d(TAG, "👆 Native ad clicked")
                }
                
                override fun onAdImpression() {
                    Log.d(TAG, "👁️ Native ad impression recorded")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setRequestCustomMuteThisAd(true)
                    .build()
            )
            .build()
        
        adLoader.loadAd(AdRequest.Builder().build())
    }
    
    /**
     * Notify all pending callbacks with the result.
     */
    private fun notifyPendingCallbacks(nativeAd: NativeAd?) {
        if (pendingCallbacks.isNotEmpty()) {
            Log.d(TAG, "📢 Notifying ${pendingCallbacks.size} pending callbacks")
            pendingCallbacks.forEach { callback ->
                try {
                    callback(nativeAd)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in callback: ${e.message}", e)
                }
            }
            pendingCallbacks.clear()
        }
    }
    
    /**
     * Load ad with callback for dynamic loading.
     * If ad is cached, callback is invoked immediately.
     * If ad is loading, callback is queued.
     * If ad needs to load, starts loading and queues callback.
     * 
     * @param activity Activity context for subscription check
     * @param callback Callback to invoke when ad is ready (or fails)
     */
    fun loadAdWithCallback(activity: Activity, callback: (NativeAd?) -> Unit) {
        // Check subscription status
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            Log.d(TAG, "🎁 User is subscribed, no ad to load")
            callback(null)
            return
        }
        
        // If ad is cached, return it immediately
        if (cachedNativeAd != null) {
            Log.d(TAG, "✅ Returning cached ad immediately")
            val ad = cachedNativeAd
            cachedNativeAd = null
            loadNewAd() // Request new ad
            callback(ad)
            return
        }
        
        // Add callback to pending list
        pendingCallbacks.add(callback)
        
        // If not loading, start loading
        if (!isLoading) {
            Log.d(TAG, "🔄 Starting dynamic ad load")
            loadNewAd()
        } else {
            Log.d(TAG, "⏳ Ad is loading, callback queued")
        }
    }
    
    /**
     * Get the cached native ad and clear it from cache.
     * Immediately triggers loadNewAd() to maintain pool of 1 available ad.
     * 
     * @param activity Activity context for subscription check
     * @return NativeAd object if available, null otherwise
     */
    fun getCachedAd(activity: Activity): NativeAd? {
        // Check subscription status
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            Log.d(TAG, "🎁 User is subscribed, no native ad to show")
            return null
        }
        
        val ad = cachedNativeAd
        if (ad == null) {
            Log.d(TAG, "⚠️ No cached native ad available")
            // Try to load one for next time
            loadNewAd()
            return null
        }
        
        Log.d(TAG, "📺 Returning cached native ad")
        
        // Clear cache (each NativeAd object can only be shown once)
        cachedNativeAd = null
        
        // Immediately request new ad to maintain pool
        loadNewAd()
        
        return ad
    }
    
    /**
     * Check if a cached ad is available (for UI state management).
     */
    fun hasCachedAd(context: Context): Boolean {
        if (SubscriptionChecker.isUserSubscribed(context)) {
            return false
        }
        return cachedNativeAd != null
    }
    
    /**
     * Cleanup resources.
     * Should be called when no longer needed.
     */
    fun destroy() {
        cachedNativeAd?.destroy()
        cachedNativeAd = null
        appContext = null
        Log.d(TAG, "🗑️ NativeAdManager destroyed")
    }
}

