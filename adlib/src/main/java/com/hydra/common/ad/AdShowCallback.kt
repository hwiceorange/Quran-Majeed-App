package com.hydra.common.ad

import com.hydra.common.ad.model.AdItem
import com.hydra.common.ad.model.RewardItem

/**
 * create by microspark 10/9/22
 */
interface AdShowCallback {
    fun onAdImpression(adItem: AdItem?)
    fun onAdClicked(adItem: AdItem?)
    fun onUserEarnedReward(adItem: AdItem?, rewardItem: RewardItem?)
    fun onAdClosed(adItem: AdItem?)
    fun onShow(adItem: AdItem?)
    fun onShowFail()
}