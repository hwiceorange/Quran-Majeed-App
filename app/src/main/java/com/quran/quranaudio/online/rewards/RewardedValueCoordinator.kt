package com.quran.quranaudio.online.rewards

import android.app.Activity
import android.app.AlertDialog
import android.widget.Toast
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.analytics.AnalyticsManager
import com.quranaudio.common.ad.AdFactory
import com.quranaudio.common.ad.AdLoadCallback
import com.quranaudio.common.ad.AdShowCallback
import com.quranaudio.common.ad.SubscriptionChecker
import com.quranaudio.common.ad.model.AdItem
import com.quranaudio.common.ad.model.RewardItem

/** Explicit, reusable opt-in flow. It never runs unrelated actions on cancel or no-fill. */
object RewardedValueCoordinator {
    fun request(
        activity: Activity,
        placement: String,
        rewardDescription: String,
        onEarned: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            onEarned()
            log(activity, placement, "subscriber_bypass")
            return
        }
        log(activity, placement, "prompt_view")
        AlertDialog.Builder(activity)
            .setTitle(activity.getString(R.string.reward_ad_title))
            .setMessage(activity.getString(R.string.reward_ad_message, rewardDescription))
            .setPositiveButton(R.string.reward_ad_watch) { _, _ ->
                log(activity, placement, "accept")
                loadAndShow(activity, placement, onEarned)
            }
            .setNegativeButton(R.string.strLabelCancel) { _, _ -> log(activity, placement, "cancel") }
            .show()
    }

    private fun loadAndShow(activity: Activity, placement: String, onEarned: () -> Unit) {
        if (AdFactory.hasRewardAd(placement)) {
            show(activity, placement, onEarned)
            return
        }
        val loading = AlertDialog.Builder(activity)
            .setMessage(R.string.reward_ad_loading)
            .setNegativeButton(R.string.strLabelCancel, null)
            .create()
        loading.setOnCancelListener { log(activity, placement, "loading_cancel") }
        loading.show()
        loading.getButton(AlertDialog.BUTTON_NEGATIVE)?.setOnClickListener {
            log(activity, placement, "loading_cancel")
            loading.dismiss()
        }
        loading.window?.decorView?.postDelayed({
            if (loading.isShowing) {
                loading.dismiss()
                log(activity, placement, "load_timeout")
                Toast.makeText(activity, R.string.reward_ad_unavailable, Toast.LENGTH_SHORT).show()
            }
        }, 12_000L)
        AdFactory.loadRewardAd(activity, placement, object : AdLoadCallback {
            override fun onAdLoaded(adItem: AdItem?) {
                if (activity.isFinishing || activity.isDestroyed || !loading.isShowing) return
                loading.dismiss()
                show(activity, placement, onEarned)
            }

            override fun onAdFailedToLoad(adId: String?) {
                if (loading.isShowing) loading.dismiss()
                log(activity, placement, "no_fill")
                Toast.makeText(activity, R.string.reward_ad_unavailable, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun show(activity: Activity, placement: String, onEarned: () -> Unit) {
        var earned = false
        AdFactory.showRewardAd(activity, placement, placement, object : AdShowCallback {
            override fun onAdImpression(adItem: AdItem?) = log(activity, placement, "impression")
            override fun onAdClicked(adItem: AdItem?) = log(activity, placement, "click")
            override fun onUserEarnedReward(adItem: AdItem?, rewardItem: RewardItem?) {
                if (earned) return
                earned = true
                log(activity, placement, "earned")
                onEarned()
            }
            override fun onAdClosed(adItem: AdItem?) = log(activity, placement, if (earned) "close_earned" else "close_no_reward")
            override fun onShow(adItem: AdItem?) = log(activity, placement, "show")
            override fun onShowFail() {
                log(activity, placement, "show_fail")
                Toast.makeText(activity, R.string.reward_ad_unavailable, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun log(activity: Activity, placement: String, outcome: String) {
        AnalyticsManager.getInstance(activity).logEvent(
            "reward_value_funnel",
            mapOf("placement" to placement, "outcome" to outcome)
        )
    }
}
