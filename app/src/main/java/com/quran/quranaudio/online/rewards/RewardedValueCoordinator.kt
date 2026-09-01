package com.quran.quranaudio.online.rewards

import android.app.Activity
import com.quran.quranaudio.online.analytics.AnalyticsManager
import com.quranaudio.common.ad.AdShowCallback
import com.quranaudio.common.ad.RewardedAdFlowCoordinator
import com.quranaudio.common.ad.model.AdItem
import com.quranaudio.common.ad.model.RewardItem

/** Explicit, reusable opt-in flow. Clicking the entry starts the shared reward flow directly. */
object RewardedValueCoordinator {
    fun request(
        activity: Activity,
        placement: String,
        rewardDescription: String,
        onEarned: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) return
        log(activity, placement, "direct_request")
        var earned = false
        RewardedAdFlowCoordinator.request(
            activity,
            placement,
            placement,
            rewardDescription,
            object : AdShowCallback {
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
                override fun onShowFail() = log(activity, placement, "cancel_or_unavailable")
            }
        )
    }

    private fun log(activity: Activity, placement: String, outcome: String) {
        AnalyticsManager.getInstance(activity).logEvent(
            "reward_value_funnel",
            mapOf("placement" to placement, "outcome" to outcome)
        )
    }
}
