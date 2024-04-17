package com.quranaudio.common.ad

import com.quranaudio.common.ad.model.AdItem

/**
 * create by microspark 10/9/22
 */
interface AdLoadCallback {
    fun onAdLoaded(adItem: AdItem?)
    fun onAdFailedToLoad(adId: String?)
}