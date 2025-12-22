package com.quranaudio.common.ad

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.quranaudio.common.ad.AdConfig.AD_APP_OPEN_CACHE_MAX_TIME
import com.quranaudio.common.ad.model.RewardItem
import com.quranaudio.common.ad.model.AdItem
import com.quranaudio.common.ad.model.LoadingState
import com.quranaudio.common.report.reportEvent

object AdFactory : ActivityLifecycleCallbacks {
    private const val TAG = "AdFactory"
    var adRevenueListener: AdRevenueListener? = null

    //adId to AdItem
    private val adsCache = mutableMapOf<String, AdItem>()

    fun init(application: Application, testMode: Boolean) {
        application.registerActivityLifecycleCallbacks(this)
        AdConfig.isTest = testMode
        
        // 🔍 Check for legacy SDKs that cause problems
        if (LegacySDKDetector.hasLegacySDKs()) {
            Log.w(TAG, "⚠️⚠️⚠️ LEGACY SDK DETECTED ⚠️⚠️⚠️")
            Log.w(TAG, LegacySDKDetector.getLegacySDKWarningMessage() ?: "")
            Log.w(TAG, "⚠️ Skipping AdMob initialization to prevent deadlock")
            Log.w(TAG, "⚠️ User MUST uninstall and reinstall app for ads to work")
            // Don't initialize ads - would cause deadlock with legacy StartApp
            return
        }
        
        // ✅ Initialize AdMob on main thread with delay (5 seconds)
        // 
        // Delay rationale:
        // 1. App startup fully completes
        // 2. All legacy/background SDKs finish (including StartApp if present)
        // 3. All locks released
        // 4. UI is interactive before ads load
        // 
        // 🆕 WebView 预热已移除（可能阻塞主线程导致 ANR）
        // 改为依赖增强的异常捕获机制，确保不影响产品功能
        // 
        // Trade-off: Ads load 5 seconds after app start, NO DEADLOCK, NO ANR
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "🕐 5-second delay completed, starting AdMob initialization")
            initAdmobOnMainThread(application)
        }, 5000) // ⚠️ 5 second delay - 不再需要等待 WebView 预热
    }
    

    /**
     * Initialize Google AdMob SDK on main thread with timeout protection.
     * ⚠️ MUST be called on main thread - Google Ads requires WebView which needs main thread.
     * 
     * Protection mechanisms:
     * - Try-catch for all operations
     * - Delayed initialization (5 seconds) to avoid lock contention
     * - Graceful failure handling
     * - IllegalStateException catch for WebView issues
     * - ⚠️ WebView 预热已移除（防止主线程阻塞导致 ANR）
     * 
     * Note: We use a simple delay instead of background thread to avoid:
     * - Thread synchronization issues
     * - Deadlock with other SDKs (e.g., legacy StartApp)
     * - Race conditions during initialization
     * - Main thread ANR (removed WebView pre-warming)
     */
    private fun initAdmobOnMainThread(context: Context) {
        try {
            // ✅ Verify we're on main thread
            if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
                Log.e(TAG, "❌ initAdmobOnMainThread called from wrong thread, aborting")
                return
            }
            
            Log.d(TAG, "🔄 Initializing AdMob on main thread")
            
            // Configure AdMob RequestConfiguration with test devices
            val testDeviceIds = mutableListOf(com.google.android.gms.ads.AdRequest.DEVICE_ID_EMULATOR)
            
            val requestConfiguration = com.google.android.gms.ads.RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            
            try {
                MobileAds.setRequestConfiguration(requestConfiguration)
                Log.d(TAG, "✅ AdMob RequestConfiguration set successfully")
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Failed to set RequestConfiguration (non-fatal): ${e.message}")
                // Continue with initialization even if this fails
            }
            
            // Initialize MobileAds with comprehensive error handling
            try {
                MobileAds.initialize(context) { initStatus ->
                    try {
                        val statusMap = initStatus.adapterStatusMap
                        for ((className, status) in statusMap) {
                            Log.d(TAG, "Adapter: $className | State: ${status.initializationState} | Latency: ${status.latency}")
                        }
                        Log.d(TAG, "✅ MobileAds initialization successful")
                    } catch (e: Exception) {
                        Log.e(TAG, "⚠️ Error logging adapter status: ${e.message}")
                    }
                }
                Log.d(TAG, "📱 AdMob initialization started successfully")
            } catch (e: IllegalStateException) {
                // 🆕 Specific catch for WebView initialization crashes
                Log.e(TAG, "❌ WebView IllegalStateException during AdMob init: ${e.message}", e)
                Log.w(TAG, "⚠️ This usually means WebView provider (Chrome) has issues")
                Log.w(TAG, "⚠️ Ads will not load, but app will continue normally")
                // App can still function, ads just won't load
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to initialize MobileAds: ${e.message}", e)
                // App can still function, ads just won't load
            }
        } catch (e: IllegalStateException) {
            // 🆕 Catch WebView-related IllegalStateException at top level
            Log.e(TAG, "❌ Critical IllegalStateException during AdMob initialization: ${e.message}", e)
            Log.w(TAG, "⚠️ Likely WebView provider issue - continuing without ads")
            // Fail gracefully - app continues without ads if initialization fails
        } catch (e: Exception) {
            Log.e(TAG, "❌ Critical error during AdMob initialization: ${e.message}", e)
            // Fail gracefully - app continues without ads if initialization fails
        }
    }

    fun loadBannerAd(
        activity: Activity,
        width: Int,
        bannerContainer: ViewGroup?,
        adPosition: String,
        functionTag: String?,
        callback: AdLoadCallback?,
        showCallback: AdShowCallback?
    ) {
        // Check if user is subscribed (premium user)
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            Log.d(TAG, "🎁 User is subscribed, skipping banner ad for $functionTag")
            bannerContainer?.visibility = View.GONE
            callback?.onAdFailedToLoad("user_subscribed")
            return
        }
        
        val adId = AdConfig.getAdIdByPosition(adPosition)
        Log.d(TAG, "📢 loadBannerAd: position=$adPosition, functionTag=$functionTag, adId=$adId")
        
        if (adId.isBlank()) {
            Log.e(TAG, "❌ Banner Ad ID is blank for position: $adPosition")
            bannerContainer?.visibility = View.GONE
            callback?.onAdFailedToLoad(adId)
            return
        }
        
        // Initially hide the container, will be shown when ad loads successfully
        bannerContainer?.visibility = View.GONE
        Log.d(TAG, "🔄 Banner container initially hidden, will show when ad loads")
        
        val adView = AdView(
            bannerContainer!!.context
        )
        when {
            width == -1 -> {
                // Use MREC (Medium Rectangle) 300x250
                adView.setAdSize(AdSize.MEDIUM_RECTANGLE)
                Log.d(TAG, "📏 Using MREC (Medium Rectangle) size: 300x250dp")
            }
            width == 0 -> {
                // Use LARGE_BANNER 320x100
            adView.setAdSize(AdSize.LARGE_BANNER)
                Log.d(TAG, "📏 Using LARGE_BANNER size: 320x100dp")
            }
            else -> {
                // Use adaptive banner with specified width
                val adWidth = width
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
            adView.setAdSize(adSize)
                Log.d(TAG, "📏 Using adaptive banner size: width=${adWidth}dp, expected height=${adSize.height}dp")
            }
        }
        adView.adUnitId = adId
        adView.adListener = SimpleBannerAdListener(
            adView,
            bannerContainer,
            adPosition,
            adId,
            functionTag,
            callback,
            showCallback
        )
        bannerContainer.addView(adView)
        reportEvent("startLoadAd", adPosition, functionTag, adId)
        val request = AdRequest.Builder().build()
        adView.loadAd(request)
        Log.d(TAG, "🚀 Banner ad request sent for $functionTag")
    }

    fun loadAppOpenAd(activity: Activity, adPosition: String, callback: AdLoadCallback?) {
        // Check if user is subscribed (premium user)
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            Log.d(TAG, "🎁 User is subscribed, skipping app open ad")
            callback?.onAdFailedToLoad("user_subscribed")
            return
        }
        
        val adId = AdConfig.getAdIdByPosition(adPosition)
        if (adId.isBlank()) {
            callback?.onAdFailedToLoad(adId)
            return
        }
        if (!needLoadAd(adId)) return
        val appOpenItem = AdItem(adId)
        adsCache[adId] = appOpenItem

        val request = AdRequest.Builder().build()
        reportEvent("startLoadAd", adPosition, null, adId)
        AppOpenAd.load(
            activity, adId, request,
            //AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                val startTime = System.currentTimeMillis()
                override fun onAdLoaded(ad: AppOpenAd) {
                    // Called when an app open ad has loaded.
                    Log.d(TAG, "AppOpenAd was loaded.")
                    reportEvent(
                        "onAdLoaded",
                        adPosition,
                        adId,
                        System.currentTimeMillis() - startTime,
                        null,
                        null,
                        "",
                        ad.responseInfo.mediationAdapterClassName ?: ""
                    )
                    appOpenItem.ad = ad
                    appOpenItem.loadingState = LoadingState.LOADED
                    callback?.onAdLoaded(appOpenItem)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Called when an app open ad has failed to load.
                    reportEvent(
                        "onAdFailedToLoad",
                        adPosition,
                        adId,
                        System.currentTimeMillis() - startTime,
                        loadAdError.code.toString() + "",
                        loadAdError.message,
                        null,
                        null
                    )
                    Log.d(TAG, loadAdError.message)
                    appOpenItem.loadingState = LoadingState.FAILED
                    callback?.onAdFailedToLoad(adPosition)
                }
            })
    }

    fun loadInterstitialAd(activity: Activity, adPosition: String, callback: AdLoadCallback?) {
        // Check if user is subscribed (premium user)
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            Log.d(TAG, "🎁 User is subscribed, skipping interstitial ad")
            callback?.onAdFailedToLoad("user_subscribed")
            return
        }
        
        val adId = AdConfig.getAdIdByPosition(adPosition)
        if (adId.isBlank()) {
            callback?.onAdFailedToLoad(adId)
            return
        }
        if (!needLoadAd(adId)) return
        val interItem = AdItem(adId)
        adsCache[adId] = interItem
        val adRequest = AdRequest.Builder().build()
        reportEvent("startLoadAd", adPosition, null, adId)
        InterstitialAd.load(activity, adId, adRequest, object : InterstitialAdLoadCallback() {
            val startTime = System.currentTimeMillis()
            override fun onAdFailedToLoad(adError: LoadAdError) {
                super.onAdFailedToLoad(adError)
                Log.d(TAG, adError.toString())
                reportEvent(
                    "onAdFailedToLoad",
                    adPosition,
                    adId,
                    System.currentTimeMillis() - startTime,
                    adError.code.toString() + "",
                    adError.message,
                    null,
                    null
                )
                interItem.loadingState = LoadingState.FAILED
                callback?.onAdFailedToLoad(adPosition)
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                super.onAdLoaded(interstitialAd)
                reportEvent(
                    "onAdLoaded",
                    adPosition,
                    adId,
                    System.currentTimeMillis() - startTime,
                    null,
                    null,
                    null,
                    interstitialAd.responseInfo.mediationAdapterClassName ?: ""
                )
                Log.d(TAG, "$adPosition :interstitialAd was loaded.")
                interItem.ad = interstitialAd
                interItem.loadingState = LoadingState.LOADED
                callback?.onAdLoaded(interItem)
            }
        })
    }

    private fun needLoadAd(adId: String): Boolean {
        adsCache[adId]?.let {
            if (it.isValid() || (it.loadingState == LoadingState.LOADING && !it.isTimeOut())) {
                return false
            } else {
                adsCache.remove(adId)
            }
        }
        return true
    }

    fun loadRewardAd(activity: Activity, adPosition: String, callback: AdLoadCallback?) {
        // Check if user is subscribed (premium user)
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            Log.d(TAG, "🎁 User is subscribed, skipping reward ad")
            callback?.onAdFailedToLoad("user_subscribed")
            return
        }
        
        val adId = AdConfig.getAdIdByPosition(adPosition)
        if (adId.isBlank()){
            callback?.onAdFailedToLoad(adId)
            return
        }
        if (!needLoadAd(adId)) return
        val rewardItem = AdItem(adId)
        adsCache[adId] = rewardItem
        val request = AdRequest.Builder().build()
        reportEvent("startLoadAd", adPosition, null, adId)
        RewardedAd.load(activity, adId, request, object : RewardedAdLoadCallback(){
            var startTime = System.currentTimeMillis()
            override fun onAdFailedToLoad(adError: LoadAdError) {
                super.onAdFailedToLoad(adError)
                reportEvent(
                    "onAdFailedToLoad",
                    adPosition,
                    adId,
                    System.currentTimeMillis() - startTime,
                    adError.code.toString() + "",
                    adError.message,
                    null,
                    null
                )
                rewardItem.loadingState = LoadingState.FAILED
                callback?.onAdFailedToLoad(adPosition)
            }

            override fun onAdLoaded(rewardedAd: RewardedAd) {
                super.onAdLoaded(rewardedAd)
                reportEvent(
                    "onAdLoaded",
                    adPosition,
                    adId,
                    System.currentTimeMillis() - startTime,
                    null,
                    null,
                    null,
                    rewardedAd.responseInfo.mediationAdapterClassName ?: ""
                )
                Log.d(TAG, "rewardedAd was loaded.")
                rewardItem.ad = rewardedAd
                rewardItem.loadingState = LoadingState.LOADED
                callback?.onAdLoaded(rewardItem)
            }
        })
    }

    private fun consumeAd(adId: String, maxCacheTime: Long = AdConfig.AD_CACHE_MAX_TIME): AdItem? {
        adsCache[adId]?.let {
            if (it.isValid(maxCacheTime)) {
                return adsCache.remove(adId)
            }
        }
        return null
    }

    fun showInterstitialAd(
        activity: Activity,
        adPosition: String,
        functionTag: String,
        callback: AdShowCallback?
    ) {
        val adId = AdConfig.getAdIdByPosition(adPosition)
        consumeAd(adId)?.let { adItem ->
            (adItem.ad as? InterstitialAd)?.let {
                it.fullScreenContentCallback = AdmobFullScreenContentCallback(
                    adPosition,
                    functionTag,
                    adItem,
                    callback,
                    it.responseInfo.mediationAdapterClassName
                )
                it.onPaidEventListener = AdmobOnPaidEventListener(adPosition, functionTag, adId, it)
                reportEvent("startShowAd", adPosition, adId, 0, null, null, functionTag, null)
                it.show(activity)
                return
            }
        }
        callback?.onShowFail()
    }

    fun showAppOpenAd(activity: Activity, adPosition: String, callback: AdShowCallback?) {
        val adId = AdConfig.getAdIdByPosition(adPosition)
        consumeAd(adId, AD_APP_OPEN_CACHE_MAX_TIME)?.let { adItem ->
            (adItem.ad as? AppOpenAd)?.let {
                it.fullScreenContentCallback = AdmobFullScreenContentCallback(
                    adPosition,
                    "",
                    adItem,
                    callback,
                    it.responseInfo.mediationAdapterClassName
                )
                it.onPaidEventListener = AdmobOnPaidEventListener(adPosition, "", adId, it)
                reportEvent("startShowAd", adPosition, adId, 0, null, null, "", null)
                it.show(activity)
                return
            }
        }
        callback?.onShowFail()
    }

    fun showRewardAd(
        activity: Activity,
        adPosition: String,
        functionTag: String,
        callback: AdShowCallback?
    ) {
        val adId = AdConfig.getAdIdByPosition(adPosition)
        consumeAd(adId)?.let { adItem ->
            (adItem.ad as? RewardedAd)?.let {
                it.fullScreenContentCallback = AdmobFullScreenContentCallback(
                    adPosition,
                    functionTag,
                    adItem,
                    callback,
                    it.responseInfo.mediationAdapterClassName
                )
                it.onPaidEventListener = AdmobOnPaidEventListener(adPosition, functionTag, adId, it)
                reportEvent("startShowAd", adPosition, adId, 0, null, null, functionTag, null)
                it.show(activity) {
                    callback?.onUserEarnedReward(
                        adItem,
                        object : RewardItem() {
                            override fun getAmount(): Int {
                                return it.amount
                            }

                            override fun getType(): String {
                                return it.type
                            }

                        })
                }
                return
            }
        }
        callback?.onShowFail()
    }

    /**
     * ⚠️ DEPRECATED: Use NativeAdManager instead for unified native ad management
     * 
     * This method is kept for backward compatibility but should NOT be used in new code.
     * 
     * Migration guide:
     * ```
     * // Old way (deprecated):
     * AdFactory.showNativeAd(activity, AdConfig.AD_NATIVE, tag, callback)
     * 
     * // New way (recommended):
     * val nativeAd = NativeAdManager.getInstance().getCachedAd(activity)
     * if (nativeAd != null) {
     *     // Display the ad
     * }
     * ```
     */
    @Deprecated(
        message = "Use NativeAdManager.getInstance().getCachedAd() instead for unified native ad management",
        replaceWith = ReplaceWith(
            "NativeAdManager.getInstance().getCachedAd(activity)",
            "com.quranaudio.common.ad.NativeAdManager"
        )
    )
    fun showNativeAd(
        activity: Activity,
        adPosition: String,
        functionTag: String,
        showCallback: AdShowCallback,
        loadAndShowNext: Boolean = true
    ) {
        Log.w(TAG, "⚠️ DEPRECATED: showNativeAd() called. Use NativeAdManager instead.")
        
        val adId = AdConfig.getAdIdByPosition(adPosition)
        consumeAd(adId)?.let { adItem ->
            (adItem.ad as? NativeAd)?.run {
                (adItem.listener as? SimpleNativeAdListener)?.let {
                    it.showCallback = showCallback
                    it.mFunctionTag = functionTag
                }
                reportEvent("startShowAd", adPosition, adId, 0, null, null, functionTag, null)
                showCallback.onShow(adItem)
                this@run.setOnPaidEventListener(
                    AdmobOnPaidEventListener(adPosition, functionTag, adId, this@run)
                )
                Log.d(TAG, "showNativeAd: success load next")
                loadNativeAd(activity, adPosition, functionTag, null, null)
                return
            }
        }
        if (!loadAndShowNext) showCallback.onShowFail()
        Log.d(TAG, "showNativeAd: failed load next")
        loadNativeAd(activity, adPosition, functionTag, null, if (loadAndShowNext) showCallback else null)
    }

    /**
     * ⚠️ DEPRECATED: Use NativeAdManager instead for unified native ad management
     * 
     * This method is kept for backward compatibility but should NOT be used in new code.
     */
    @Deprecated(
        message = "Use NativeAdManager.getInstance().loadNewAd() instead for unified native ad management",
        replaceWith = ReplaceWith(
            "NativeAdManager.getInstance().loadNewAd()",
            "com.quranaudio.common.ad.NativeAdManager"
        )
    )
    fun loadNativeAd(
        activity: Activity,
        adPosition: String,
        functionTag: String,
        callback: AdLoadCallback?,
        showCallback: AdShowCallback?
    ) {
        Log.w(TAG, "⚠️ DEPRECATED: loadNativeAd() called. Use NativeAdManager instead.")
        
        // Check if user is subscribed (premium user)
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            Log.d(TAG, "🎁 User is subscribed, skipping native ad for $functionTag")
            callback?.onAdFailedToLoad("user_subscribed")
            showCallback?.onShowFail()
            return
        }
        
        val adId = AdConfig.getAdIdByPosition(adPosition)
        if (adId.isBlank()){
            callback?.onAdFailedToLoad(adId)
            return
        }
        if (!needLoadAd(adId)) return
        val nativeAdItem = AdItem(adId)
        adsCache[adId] = nativeAdItem

        val builder = AdLoader.Builder(activity, adId)
        val adListener =
            SimpleNativeAdListener(adPosition, adId, functionTag, callback, showCallback, nativeAdItem)
        nativeAdItem.listener = adListener
        builder.forNativeAd { nativeAd ->
            nativeAdItem.loadingState = LoadingState.LOADED
            nativeAdItem.ad = nativeAd
            showCallback?.run {
                nativeAd.setOnPaidEventListener(
                    AdmobOnPaidEventListener(adPosition, functionTag, adId, nativeAd)
                )
            }
            callback?.onAdLoaded(nativeAdItem)
            adListener.setNativeAd(nativeAd)
            showCallback?.onShow(nativeAdItem)
        }
        builder.withAdListener(adListener)
        builder.withNativeAdOptions(
            NativeAdOptions.Builder()
                .setRequestCustomMuteThisAd(true)
                .build()
        )
        val adLoader = builder.build()
        reportEvent("startLoadAd", adPosition, functionTag, adId)
        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * ⚠️ DEPRECATED: Use NativeAdManager instead for unified native ad management
     */
    @Deprecated(
        message = "Use NativeAdManager.getInstance().hasCachedAd(context) instead",
        replaceWith = ReplaceWith(
            "NativeAdManager.getInstance().hasCachedAd(context)",
            "com.quranaudio.common.ad.NativeAdManager"
        )
    )
    fun hasNativeAd(adPosition: String): Boolean {
        Log.w(TAG, "⚠️ DEPRECATED: hasNativeAd() called. Use NativeAdManager instead.")
        
        val adId = AdConfig.getAdIdByPosition(adPosition)
        adsCache[adId]?.let {
            return it.isValid() && it.ad is NativeAd
        }
        return false
    }

    fun hasInterAd(adPosition: String): Boolean {
        val adId = AdConfig.getAdIdByPosition(adPosition)
        adsCache[adId]?.let {
            return it.isValid() && it.ad is InterstitialAd
        }
        return false
    }

    fun hasAppOpenAd(adPosition: String): Boolean {
        val adId = AdConfig.getAdIdByPosition(adPosition)
        adsCache[adId]?.let {
            return it.isValid(AD_APP_OPEN_CACHE_MAX_TIME) && it.ad is AppOpenAd
        }
        return false
    }

    fun hasRewardAd(adPosition: String): Boolean {
        val adId = AdConfig.getAdIdByPosition(adPosition)
        adsCache[adId]?.let {
            return it.isValid() && it.ad is RewardedAd
        }
        return false
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        loadInterstitialAd(activity, AdConfig.AD_INTERS, null)
//        loadInterstitialAd(activity, AdConfig.AD_INTERS_HIGH, null)
        loadAppOpenAd(activity, AdConfig.AD_APPOPEN, null)
        // ❌ 移除：loadNativeAd(activity, AdConfig.AD_NATIVE, "", null, null)
        // ✅ 原生广告由 NativeAdManager 统一管理，不需要在这里加载
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }


}