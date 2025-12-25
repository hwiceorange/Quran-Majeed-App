package com.quranaudio.common.ad

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.Timer
import java.util.TimerTask

/**
 * Central manager for interstitial ads with caching, refresh, and display logic.
 * 
 * Features:
 * - Maintains a cache pool with 1 available ad
 * - Preloads ad on cold start
 * - Immediately requests new ad when one is consumed
 * - Deletes and refreshes ads older than 58 minutes
 * - Checks premium subscription status before loading/showing ads
 */
class InterstitialAdManager private constructor() {
    
    companion object {
        private const val TAG = "InterstitialAdManager"
        private const val AD_MAX_AGE_MILLIS = 58 * 60 * 1000L // 58 minutes
        private const val TIMER_CHECK_INTERVAL = 5 * 60 * 1000L // Check every 5 minutes
        private const val RETRY_DELAY_MILLIS = 30 * 1000L // 30 seconds retry delay
        
        @Volatile
        private var instance: InterstitialAdManager? = null
        
        fun getInstance(): InterstitialAdManager {
            return instance ?: synchronized(this) {
                instance ?: InterstitialAdManager().also { instance = it }
            }
        }
    }
    
    // Ad Unit ID - reusing existing interstitial ID from AdConfig
    private val adUnitId: String
        get() = AdConfig.getAdIdByPosition(AdConfig.AD_INTERS)
    
    // Cached ad object
    private var cachedAd: InterstitialAd? = null
    
    // Timestamp when current ad was loaded
    private var loadTimeMillis: Long = 0L
    
    // Timer for periodic expiry checks
    private var adRefreshTimer: Timer? = null
    
    // Flag to prevent multiple simultaneous load requests
    private var isLoading: Boolean = false
    
    // Application context
    private var appContext: Context? = null
    
    // Main thread handler for ad loading (Google Ads SDK requires main thread)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    /**
     * Initialize the manager with application context.
     * Should be called once in Application.onCreate()
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "✅ InterstitialAdManager initialized")
    }
    
    /**
     * Preload the first ad on cold start and start the refresh timer.
     * Should be called in Application.onCreate()
     */
    fun preloadAd() {
        loadNewAd()
        startAdTimer()
        Log.d(TAG, "✅ Preload initiated and timer started")
    }
    
    /**
     * Load a new interstitial ad.
     * - Clears old cache
     * - Checks subscription status
     * - Attaches FullScreenContentCallback
     * - Retries on failure after 30 seconds
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
            Log.d(TAG, "🎁 User is subscribed, skipping ad load")
            return
        }
        
        // Clear old cache
        cachedAd = null
        loadTimeMillis = 0L
        isLoading = true
        
        val adRequest = AdRequest.Builder().build()
        
        Log.d(TAG, "🔄 Loading new interstitial ad with ID: $adUnitId")
        
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "✅ Interstitial ad loaded successfully")
                    
                    cachedAd = interstitialAd
                    loadTimeMillis = System.currentTimeMillis()
                    isLoading = false
                    
                    // Attach FullScreenContentCallback to monitor ad lifecycle
                    attachFullScreenCallback(interstitialAd)
                }
                
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "❌ Failed to load interstitial ad: ${loadAdError.message} (Code: ${loadAdError.code})")
                    
                    cachedAd = null
                    isLoading = false
                    
                    // Retry after 30 seconds
                    Log.d(TAG, "⏳ Retrying ad load in 30 seconds...")
                    scheduleRetry()
                }
            }
        )
    }
    
    /**
     * Attach FullScreenContentCallback to monitor ad events.
     * Note: Replacement ad is requested in showAdIfAvailable(), not here.
     */
    private fun attachFullScreenCallback(ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "📴 Ad dismissed by user")
                // No action needed here - replacement already requested in showAdIfAvailable()
            }
            
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "❌ Ad failed to show: ${adError.message} (Code: ${adError.code})")
                // Request new ad immediately on show failure
                loadNewAd()
            }
            
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "✅ Ad showed full screen content")
            }
        }
    }
    
    /**
     * Start the periodic timer to check for expired ads.
     * Checks every 5 minutes if cached ad is older than 58 minutes.
     */
    fun startAdTimer() {
        // Cancel existing timer if any
        adRefreshTimer?.cancel()
        
        adRefreshTimer = Timer("InterstitialAdRefreshTimer", true)
        adRefreshTimer?.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    checkAndRefreshExpiredAd()
                }
            },
            TIMER_CHECK_INTERVAL, // Initial delay: 5 minutes
            TIMER_CHECK_INTERVAL  // Repeat every 5 minutes
        )
        
        Log.d(TAG, "⏰ Ad refresh timer started (checks every 5 minutes)")
    }
    
    /**
     * Check if cached ad has expired (> 58 minutes old).
     * If expired, delete and request a new ad.
     * Note: This is called from a background timer thread, so we must post to main thread.
     */
    private fun checkAndRefreshExpiredAd() {
        if (cachedAd == null) {
            Log.d(TAG, "🔍 No cached ad to check for expiry")
            return
        }
        
        val currentTime = System.currentTimeMillis()
        val adAge = currentTime - loadTimeMillis
        
        if (adAge > AD_MAX_AGE_MILLIS) {
            Log.d(TAG, "⏰ Cached ad expired (age: ${adAge / 1000 / 60} minutes), requesting new ad")
            
            // Delete expired ad and request new one on main thread
            cachedAd = null
            loadTimeMillis = 0L
            
            // ✅ Post to main thread - Google Ads SDK requires main thread
            mainHandler.post {
                loadNewAd()
            }
        } else {
            val remainingMinutes = (AD_MAX_AGE_MILLIS - adAge) / 1000 / 60
            Log.d(TAG, "✅ Cached ad is still valid ($remainingMinutes minutes remaining)")
        }
    }
    
    /**
     * Show the cached ad if available.
     * Immediately requests a new ad to maintain pool of 1 available ad.
     * 
     * @param activity The activity context to show the ad
     * @param onAdClosed Optional callback to invoke when ad is dismissed or fails to show
     * @return true if ad was shown, false otherwise
     */
    @JvmOverloads
    fun showAdIfAvailable(activity: Activity, onAdClosed: (() -> Unit)? = null): Boolean {
        // Check subscription status
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            Log.d(TAG, "🎁 User is subscribed, skipping ad display")
            return false
        }
        
        val ad = cachedAd
        if (ad == null) {
            Log.d(TAG, "⚠️ No cached ad available to show")
            // Still request a new ad if cache is empty
            loadNewAd()
            return false
        }
        
        Log.d(TAG, "📺 Showing interstitial ad")
        
        // 获取调用页面位置（用于日志）
        val adLocation = activity.javaClass.simpleName
        Log.d(TAG, "📍 Ad location: $adLocation")
        
        // Attach callback to handle ad dismissal
        if (onAdClosed != null) {
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "✅ Ad dismissed by user")
                    cachedAd = null
                    loadTimeMillis = 0L
                    loadNewAd()
                    
                    // Invoke callback on main thread
                    Handler(Looper.getMainLooper()).post {
                        onAdClosed.invoke()
                    }
                }
                
                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.e(TAG, "❌ Ad failed to show: ${adError.message}")
                    cachedAd = null
                    loadTimeMillis = 0L
                    loadNewAd()
                    
                    // Invoke callback on main thread
                    Handler(Looper.getMainLooper()).post {
                        onAdClosed.invoke()
                    }
                }
                
                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "📺 Ad showed full screen content at $adLocation")
                }
            }
        }
        
        // Show the ad
        ad.show(activity)
        
        // Clear cache and immediately request new ad to maintain pool (if no callback)
        if (onAdClosed == null) {
            cachedAd = null
            loadTimeMillis = 0L
            loadNewAd()
            
            Log.d(TAG, "📺 Interstitial ad shown at ${activity.javaClass.simpleName}")
        }
        
        return true
    }
    
    /**
     * Schedule a retry to load ad after 30 seconds delay.
     * Note: Timer runs on background thread, so we must post to main thread.
     */
    private fun scheduleRetry() {
        Timer().schedule(
            object : TimerTask() {
                override fun run() {
                    Log.d(TAG, "♻️ Retry: Loading ad after failure")
                    
                    // ✅ Post to main thread - Google Ads SDK requires main thread
                    mainHandler.post {
                        loadNewAd()
                    }
                }
            },
            RETRY_DELAY_MILLIS
        )
    }
    
    /**
     * Check if an ad is currently available in cache.
     * @return true if ad is cached and ready to show
     */
    fun isAdAvailable(): Boolean {
        return cachedAd != null
    }
    
    /**
     * Get the age of the current cached ad in minutes.
     * @return Age in minutes, or 0 if no ad is cached
     */
    fun getCachedAdAgeMinutes(): Int {
        if (cachedAd == null || loadTimeMillis == 0L) {
            return 0
        }
        return ((System.currentTimeMillis() - loadTimeMillis) / 1000 / 60).toInt()
    }
    
    /**
     * Stop the refresh timer (e.g., when app is destroyed).
     */
    fun stopTimer() {
        adRefreshTimer?.cancel()
        adRefreshTimer = null
        Log.d(TAG, "⏹️ Ad refresh timer stopped")
    }
}

