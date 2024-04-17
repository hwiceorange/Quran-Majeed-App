package com.quranaudio.common.ad

import com.quranaudio.common.ad.model.AdItem
import com.quranaudio.common.ad.model.RewardItem

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