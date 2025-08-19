package com.quranaudio.common.ad

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.Context
import android.os.Bundle
import android.util.Log
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
        initAdmob(application)
        AdConfig.isTest = testMode
    }

    private fun initAdmob(context: Context) {
        try {
            MobileAds.initialize(context) {
                Log.d(TAG, "MobileAds init: successful")
            }
        }catch (e:Exception){

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
        val adId = AdConfig.getAdIdByPosition(adPosition)
        if (adId.isBlank()) {
            callback?.onAdFailedToLoad(adId)
            return
        }
        val adView = AdView(
            bannerContainer!!.context
        )
        if (width == 0) {
            adView.setAdSize(AdSize.LARGE_BANNER)
        } else {
            val density = activity.resources.displayMetrics.density
            val adWidth = (width * 1.0f / density).toInt()
            val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
            adView.setAdSize(adSize)
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
    }

    fun loadAppOpenAd(activity: Activity, adPosition: String, callback: AdLoadCallback?) {
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

    fun showNativeAd(
        activity: Activity,
        adPosition: String,
        functionTag: String,
        showCallback: AdShowCallback,
        loadAndShowNext: Boolean = true
    ) {
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

    fun loadNativeAd(
        activity: Activity,
        adPosition: String,
        functionTag: String,
        callback: AdLoadCallback?,
        showCallback: AdShowCallback?
    ) {
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

    fun hasNativeAd(adPosition: String): Boolean {
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
        loadNativeAd(activity, AdConfig.AD_NATIVE, "", null, null)
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