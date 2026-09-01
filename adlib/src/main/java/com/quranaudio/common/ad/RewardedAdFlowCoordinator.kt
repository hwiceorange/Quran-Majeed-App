package com.quranaudio.common.ad

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.quranaudio.common.ad.model.AdItem
import com.quranaudio.common.ad.model.RewardItem
import java.lang.ref.WeakReference

/**
 * One cache-first flow for every explicit rewarded placement.
 *
 * A standard InterstitialAd is deliberately never used here: rewards are only
 * granted from a real RewardedAd or RewardedInterstitialAd earned callback.
 */
object RewardedAdFlowCoordinator {
    internal const val REWARDED_WAIT_TIMEOUT_MS = 8_000L
    private const val POLL_INTERVAL_MS = 150L
    private val mainHandler = Handler(Looper.getMainLooper())
    private var activeAttempt: WeakReference<Attempt>? = null

    @JvmStatic
    fun preload(activity: Activity, rewardPosition: String) {
        if (!isUsable(activity)) return
        AdFactory.preloadRewardFlowAds(activity, rewardPosition)
    }

    @JvmStatic
    fun request(
        activity: Activity,
        rewardPosition: String,
        functionTag: String,
        rewardDescription: String,
        callback: AdShowCallback
    ) {
        if (!isUsable(activity)) {
            callback.onShowFail()
            return
        }
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            val subscriberItem = AdItem("subscriber_bypass")
            callback.onUserEarnedReward(subscriberItem, object : RewardItem() {
                override fun getAmount(): Int = 1
                override fun getType(): String = "subscriber"
            })
            callback.onAdClosed(subscriberItem)
            return
        }

        activeAttempt?.get()?.let { existing ->
            if (!existing.isFinished) {
                existing.bringForward()
                callback.onShowFail()
                return
            }
        }

        val attempt = Attempt(activity, rewardPosition, functionTag, rewardDescription, callback)
        activeAttempt = WeakReference(attempt)
        attempt.start()
    }

    private fun isUsable(activity: Activity): Boolean {
        if (activity.isFinishing || activity.isDestroyed || !AdFactory.isAppInForeground()) return false
        val lifecycleOwner = activity as? LifecycleOwner ?: return activity.hasWindowFocus()
        return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    }

    private class Attempt(
        activity: Activity,
        private val rewardPosition: String,
        private val functionTag: String,
        rewardDescription: String,
        private val callback: AdShowCallback
    ) {
        private val activityRef = WeakReference(activity)
        private val dialog = RewardedAdFlowDialog(
            activity,
            rewardDescription,
            onRetry = { retry() },
            onUserCancel = { cancel() }
        )
        private var startedAt = 0L
        private var earned = false
        private var adLaunching = false
        var isFinished = false
            private set

        private val pollRunnable = object : Runnable {
            override fun run() {
                val currentActivity = activityRef.get()
                if (isFinished || currentActivity == null || !isUsable(currentActivity)) {
                    cancel()
                    return
                }
                when (
                    decideRewardedFlowNext(
                        rewardedReady = AdFactory.hasRewardAd(rewardPosition),
                        fallbackReady = AdFactory.hasRewardedInterstitialAd(),
                        elapsedMs = SystemClock.elapsedRealtime() - startedAt
                    )
                ) {
                    RewardedFlowNext.SHOW_REWARDED -> showRewarded(currentActivity)
                    RewardedFlowNext.WAIT -> mainHandler.postDelayed(this, POLL_INTERVAL_MS)
                    RewardedFlowNext.SHOW_FALLBACK -> showFallback(currentActivity)
                    RewardedFlowNext.UNAVAILABLE -> dialog.showUnavailable()
                }
            }
        }

        fun start() {
            val activity = activityRef.get() ?: return cancel()
            if (AdFactory.hasRewardAd(rewardPosition)) {
                showRewarded(activity)
                return
            }
            dialog.show()
            beginLoading(activity)
        }

        fun bringForward() {
            if (dialog.isShowing) dialog.window?.decorView?.requestFocus()
        }

        private fun beginLoading(activity: Activity) {
            mainHandler.removeCallbacks(pollRunnable)
            startedAt = SystemClock.elapsedRealtime()
            dialog.showLoading()
            AdFactory.loadRewardAd(activity, rewardPosition, null)
            AdFactory.loadRewardedInterstitialAd(
                activity,
                AdConfig.AD_REWARDED_INTERSTITIAL_FALLBACK,
                null
            )
            mainHandler.post(pollRunnable)
        }

        private fun retry() {
            if (isFinished || adLaunching) return
            val activity = activityRef.get() ?: return cancel()
            beginLoading(activity)
        }

        private fun showRewarded(activity: Activity) {
            launchAd(activity, fallback = false)
        }

        private fun showFallback(activity: Activity) {
            launchAd(activity, fallback = true)
        }

        private fun launchAd(activity: Activity, fallback: Boolean) {
            if (adLaunching || isFinished) return
            adLaunching = true
            mainHandler.removeCallbacks(pollRunnable)
            if (dialog.isShowing) dialog.dismissForAd()

            val showCallback = object : AdShowCallback {
                override fun onAdImpression(adItem: AdItem?) = callback.onAdImpression(adItem)
                override fun onAdClicked(adItem: AdItem?) = callback.onAdClicked(adItem)
                override fun onShow(adItem: AdItem?) = callback.onShow(adItem)

                override fun onUserEarnedReward(adItem: AdItem?, rewardItem: RewardItem?) {
                    if (earned) return
                    earned = true
                    callback.onUserEarnedReward(adItem, rewardItem)
                }

                override fun onAdClosed(adItem: AdItem?) {
                    finish()
                    callback.onAdClosed(adItem)
                    preloadNext(activity)
                }

                override fun onShowFail() {
                    adLaunching = false
                    preloadNext(activity)
                    if (!isUsable(activity)) {
                        cancel()
                        return
                    }
                    if (!dialog.isShowing) dialog.show()
                    dialog.showUnavailable()
                }
            }

            if (fallback) {
                AdFactory.showRewardedInterstitialAd(
                    activity,
                    AdConfig.AD_REWARDED_INTERSTITIAL_FALLBACK,
                    functionTag,
                    showCallback
                )
            } else {
                AdFactory.showRewardAd(activity, rewardPosition, functionTag, showCallback)
            }
        }

        private fun preloadNext(activity: Activity) {
            if (isUsable(activity)) AdFactory.preloadRewardFlowAds(activity, rewardPosition)
        }

        private fun cancel() {
            if (isFinished || adLaunching) return
            finish()
            if (dialog.isShowing) dialog.dismissForAd()
            callback.onShowFail()
        }

        private fun finish() {
            if (isFinished) return
            isFinished = true
            mainHandler.removeCallbacks(pollRunnable)
            activeAttempt = null
        }
    }
}
