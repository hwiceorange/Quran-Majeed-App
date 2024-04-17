package com.quranaudio.common.ad

import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.quranaudio.common.ad.model.AdItem
import com.quranaudio.common.report.reportEvent

/**
 * 全屏广告回调监听
 */
internal class AdmobFullScreenContentCallback(
    private var adPosition: String?,
    private var functionTag: String?,
    private var adItem: AdItem,
    private val adShowListener: AdShowCallback?,
    private val adAdapterName: String?
) : FullScreenContentCallback() {
    private var _showedTime: Long = 0

    override fun onAdClicked() {
        super.onAdClicked()
        reportEvent(
            "onClick",
            adPosition,
            adItem.id,
            System.currentTimeMillis() - _showedTime,
            null,
            null,
            functionTag,
            adAdapterName
        )
        adShowListener?.onAdClicked(adItem)
    }

    override fun onAdImpression() {
        super.onAdImpression()
        _showedTime = System.currentTimeMillis()
        reportEvent(
            "onImpression",
            adPosition,
            adItem.id,
            0,
            null,
            null,
            functionTag,
            adAdapterName
        )
        adShowListener?.onAdImpression(adItem)
    }

    override fun onAdDismissedFullScreenContent() {
        super.onAdDismissedFullScreenContent()
        reportEvent(
            "onClose",
            adPosition,
            adItem.id,
            System.currentTimeMillis() - _showedTime,
            null,
            null,
            functionTag,
            adAdapterName
        )
        adShowListener?.onAdClosed(adItem)
    }

    override fun onAdShowedFullScreenContent() {
        super.onAdShowedFullScreenContent()
        reportEvent(
            "onShow",
            adPosition,
            adItem.id,
            0,
            null,
            null,
            functionTag,
            adAdapterName
        )
        Log.d("AdFactory", "$adPosition :${adItem.id}:interstitialAd was showed.")
        adShowListener?.onShow(adItem)
    }

    override fun onAdFailedToShowFullScreenContent(var1: AdError) {
        reportEvent(
            "onShowFailed",
            adPosition,
            adItem.id,
            0,
            var1.code.toString() + "",
            var1.message,
            functionTag,
            adAdapterName
        )
        adShowListener?.onShowFail()
    }
}