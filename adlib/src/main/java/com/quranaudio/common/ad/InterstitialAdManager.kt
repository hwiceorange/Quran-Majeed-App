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

/**
 * Central manager for interstitial ads with caching and display logic.
 *
 * Features:
 * - Maintains a cache pool with 1 available ad
 * - Preloads ad on cold start (foreground only)
 * - Immediately requests new ad when one is consumed
 * - Discards ads older than 58 minutes at show time (no background refresh loop)
 * - Checks premium subscription status before loading/showing ads
 */
class InterstitialAdManager private constructor() {

    companion object {
        private const val TAG = "InterstitialAdManager"
        private const val AD_MAX_AGE_MILLIS = 58 * 60 * 1000L // 58 minutes
        private const val RETRY_DELAY_MILLIS = 30 * 1000L // base retry delay, exponential backoff
        private const val MAX_RETRY_COUNT = 3
        
        @Volatile
        private var instance: InterstitialAdManager? = null
        
        fun getInstance(): InterstitialAdManager {
            return instance ?: synchronized(this) {
                instance ?: InterstitialAdManager().also { instance = it }
            }
        }

        /**
         * 插屏被用户关闭后的回调（仅在广告真的展示并被关闭时触发，展示失败不触发）。
         *
         * adlib 不能反向依赖 app 模块，所以用这个钩子把「关闭插屏」这个时机
         * 交给 app 侧决定做什么——目前用于弹去广告买断弹窗。
         * 由 App.onCreate 注册；未注册时这里为 null，行为与改动前一致。
         */
        @JvmStatic
        @Volatile
        var afterDismissListener: ((Activity) -> Unit)? = null
    }
    
    // Ad Unit ID - reusing existing interstitial ID from AdConfig
    private val adUnitId: String
        get() = AdConfig.getAdIdByPosition(AdConfig.AD_INTERS)
    
    // Cached ad object
    private var cachedAd: InterstitialAd? = null
    
    // Timestamp when current ad was loaded
    private var loadTimeMillis: Long = 0L

    // Flag to prevent multiple simultaneous load requests
    private var isLoading: Boolean = false

    // Consecutive load-failure count, reset on success or fresh user-triggered load
    private var retryCount: Int = 0
    
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
     * Preload the first ad on cold start.
     * Should be called in Application.onCreate() (only when app is in foreground).
     * Note: no background refresh timer anymore — expiry is checked at show time.
     */
    fun preloadAd() {
        loadNewAd()
        Log.d(TAG, "✅ Preload initiated")
    }
    
    /**
     * Load a new interstitial ad.
     * - Clears old cache
     * - Checks subscription status
     * - Attaches FullScreenContentCallback
     * - Retries on failure after 30 seconds
     */
    fun loadNewAd() {
        if (!ConsentManager.canRequestAds()) {
            Log.w(TAG, "Consent not ready; blocking interstitial ad request")
            return
        }
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
        if (SubscriptionChecker.shouldHideAds(context)) {
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
                    retryCount = 0

                    // Attach FullScreenContentCallback to monitor ad lifecycle
                    attachFullScreenCallback(interstitialAd)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "❌ Failed to load interstitial ad: ${loadAdError.message} (Code: ${loadAdError.code})")

                    cachedAd = null
                    isLoading = false

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
     * Check if the cached ad has expired (> 58 minutes old) and discard it if so.
     * Called at show time instead of a periodic background timer, so an idle
     * process no longer burns one ad request per hour forever.
     *
     * @return true if an expired ad was discarded
     */
    private fun discardIfExpired(): Boolean {
        if (cachedAd == null) return false

        val adAge = System.currentTimeMillis() - loadTimeMillis
        if (adAge > AD_MAX_AGE_MILLIS) {
            Log.d(TAG, "⏰ Cached ad expired (age: ${adAge / 1000 / 60} minutes), discarding")
            cachedAd = null
            loadTimeMillis = 0L
            return true
        }
        return false
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
        if (SubscriptionChecker.shouldHideAds(activity)) {
            Log.d(TAG, "🎁 User is subscribed, skipping ad display")
            return false
        }

        // 频控。此前这里完全没有：没有最小间隔、没有会话/单日上限、没有新装保护期，
        // 只要触发点命中就播，一个活跃新用户 D0 就可能吃到 4~5 个全屏广告。
        val block = InterstitialFrequencyCap.blockReason(activity)
        if (block != null) {
            Log.d(TAG, "⛔ Interstitial blocked by frequency cap: $block")
            return false
        }

        // Discard expired cached ad (expiry is checked here instead of a background timer)
        if (discardIfExpired()) {
            retryCount = 0
            loadNewAd()
            return showNativeFallback(activity, onAdClosed)
        }

        val ad = cachedAd
        if (ad == null) {
            Log.d(TAG, "⚠️ No cached ad available to show")
            // Still request a new ad if cache is empty (fresh user demand, reset retry budget)
            retryCount = 0
            loadNewAd()
            return showNativeFallback(activity, onAdClosed)
        }
        
        Log.d(TAG, "📺 Showing interstitial ad")
        
        // 获取调用页面位置（用于日志）
        val adLocation = activity.javaClass.simpleName
        Log.d(TAG, "📍 Ad location: $adLocation")
        
        // 总是挂回调。
        //
        // 原实现只在 onAdClosed != null 时才挂 FullScreenContentCallback，
        // 于是绝大多数调用点（都不传 onAdClosed）根本检测不到「广告被关闭」，
        // 缓存清理靠 show() 之后立即执行，也无法在关闭时机做任何事。
        // 现在统一挂上：既保证缓存/预加载行为一致，也让
        // afterDismissListener（去广告弹窗）有可靠的触发点。
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "✅ Ad dismissed by user")
                cachedAd = null
                loadTimeMillis = 0L
                loadNewAd()

                Handler(Looper.getMainLooper()).post {
                    onAdClosed?.invoke()
                    // 用户刚被一个全屏广告打断，正是「原来可以永久去掉」最有说服力的时刻。
                    // 是否真弹由 app 侧判定（订阅/已买断用户不弹、24h 间隔、累计关闭 3 次后不再弹）。
                    notifyAfterDismiss(activity)
                }
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e(TAG, "❌ Ad failed to show: ${adError.message}")
                cachedAd = null
                loadTimeMillis = 0L
                loadNewAd()

                Handler(Looper.getMainLooper()).post {
                    onAdClosed?.invoke()
                    // 广告没展示成功就不打扰用户谈付费，这里不触发弹窗
                }
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "📺 Ad showed full screen content at $adLocation")
            }
        }

        // Show the ad
        ad.show(activity)
        // 推进频控计数（会话数 / 单日数 / 上次展示时刻）
        InterstitialFrequencyCap.onShown(activity)

        return true
    }

    private fun notifyAfterDismiss(activity: Activity) {
        try {
            afterDismissListener?.invoke(activity)
        } catch (t: Throwable) {
            Log.w(TAG, "afterDismissListener failed", t)
        }
    }

    private fun showNativeFallback(activity: Activity, onAdClosed: (() -> Unit)?): Boolean {
        val fallbackCallback = if (onAdClosed == null) null else object : AdShowCallback {
            override fun onAdImpression(adItem: com.quranaudio.common.ad.model.AdItem?) = Unit
            override fun onAdClicked(adItem: com.quranaudio.common.ad.model.AdItem?) = Unit
            override fun onUserEarnedReward(adItem: com.quranaudio.common.ad.model.AdItem?, rewardItem: com.quranaudio.common.ad.model.RewardItem?) = Unit
            override fun onAdClosed(adItem: com.quranaudio.common.ad.model.AdItem?) = onAdClosed.invoke()
            override fun onShow(adItem: com.quranaudio.common.ad.model.AdItem?) = Unit
            override fun onShowFail() = Unit
        }
        return FullScreenNativeAdManager.showIfAvailable(activity, fallbackCallback)
    }
    
    /**
     * Schedule a retry with exponential backoff (30s, 60s, 120s), capped at
     * [MAX_RETRY_COUNT] attempts. Skipped when the app is in the background —
     * the next user-triggered show attempt resets the budget and reloads.
     */
    private fun scheduleRetry() {
        if (retryCount >= MAX_RETRY_COUNT) {
            Log.w(TAG, "⛔ Max retries reached ($MAX_RETRY_COUNT), waiting for next show attempt")
            return
        }
        val delay = RETRY_DELAY_MILLIS shl retryCount // 30s, 60s, 120s
        retryCount++
        Log.d(TAG, "⏳ Retrying ad load in ${delay / 1000}s (attempt $retryCount/$MAX_RETRY_COUNT)")

        mainHandler.postDelayed({
            if (!AdFactory.isAppInForeground()) {
                Log.d(TAG, "⏸️ App in background, skipping retry")
                return@postDelayed
            }
            loadNewAd()
        }, delay)
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
    
}
