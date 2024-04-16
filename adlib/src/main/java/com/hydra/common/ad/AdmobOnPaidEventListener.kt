package com.hydra.common.ad

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd

/**
 * 广告变现回调监听
 */
class AdmobOnPaidEventListener(
    private var adPosition: String,
    private var functionTag: String,
    private val adId: String,
    private var adObject: Any?
) : OnPaidEventListener {
    override fun onPaidEvent(adValue: AdValue) {
        var valueMicros = adValue.valueMicros
        val currencyCode = adValue.currencyCode
        val precision = adValue.precisionType
        //设置adjust广告收入跟踪，这里是admob的设置方法
       // val adRevenue = AdjustAdRevenue(AdjustConfig.AD_REVENUE_ADMOB)

        val singleValue = (valueMicros * 1.0f / 1000000).toDouble()
       // adRevenue.setRevenue(singleValue, adValue.currencyCode)

    }
}