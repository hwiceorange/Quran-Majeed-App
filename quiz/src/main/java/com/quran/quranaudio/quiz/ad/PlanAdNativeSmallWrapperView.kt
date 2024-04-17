package com.quran.quranaudio.quiz.ad

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.quran.quranaudio.quiz.extension.gone
import com.quran.quranaudio.quiz.extension.isValid
import com.quran.quranaudio.quiz.extension.reportEvent
import com.quran.quranaudio.quiz.utils.NativeAdTimeUtil
import com.google.android.gms.ads.nativead.NativeAd
import com.quranaudio.common.ad.AdConfig
import com.quranaudio.common.ad.AdFactory
import com.quranaudio.common.ad.AdShowCallback
import com.quranaudio.common.ad.model.AdItem
import com.quranaudio.common.ad.model.RewardItem
import com.quran.quranaudio.quiz.databinding.LayoutAdNativeSmallWrapper2Binding

class PlanAdNativeSmallWrapperView : LinearLayout {
    private val TAG = "small_ad"
    val binding =
        LayoutAdNativeSmallWrapper2Binding.inflate(LayoutInflater.from(context), this, true)
    var adTag = ""


    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    fun loadNativeAd(adTag: String) {
        reportEvent(adTag, "show_native_ad")
        if (NativeAdTimeUtil.isIntercept(adTag)) {
            Log.d(TAG, "loadNativeAd: 在间隔时间内，不刷新")
            reportEvent(adTag, "no_native_ad", "less_time")
            return
        }
        this.adTag = adTag
        val activity = context as Activity
        if (activity.isValid()) {
            AdFactory.showNativeAd(
                activity, AdConfig.AD_NATIVE,
                adTag, nativeShowCallback
            )
        }
    }

    private val nativeShowCallback = object : AdShowCallback {
        override fun onAdImpression(p0: AdItem?) {
            Log.d(TAG, "onAdImpression: ")
        }

        override fun onAdClicked(p0: AdItem?) {
            Log.d(TAG, "onAdClicked: ")
            NativeAdTimeUtil.saveTime(adTag, 0)

        }

        override fun onUserEarnedReward(p0: AdItem?, p1: RewardItem?) {
        }

        override fun onAdClosed(p0: AdItem?) {
            Log.d(TAG, "onAdClosed: ")
        }

        override fun onShow(p0: AdItem?) {
            Log.d(TAG, "onShow: ")
            NativeAdTimeUtil.saveTime(adTag, System.currentTimeMillis())
            val activity = context as Activity
            if (activity.isValid()){
                showNativeAd(p0)
            }
        }

        override fun onShowFail() {
            Log.d(TAG, "onShowFail: ")
        }

    }

    fun showNativeAd(p0: AdItem?) {
        val activity = context as Activity
        if (activity.isValid()) {
            if (p0 != null && p0.ad != null) {
                when (p0.ad) {
                    is NativeAd -> {
                        inflateView(p0.ad as NativeAd)
                        binding.adMaxFl.gone()
                    }

                    is MaxNativeAdView -> {
                        binding.adMaxFl.visibility = View.VISIBLE
                        binding.adMaxFl.removeAllViews()
                        binding.adMaxFl.addView(p0.ad as MaxNativeAdView)
                        binding.nativeAdView.gone()
                    }
                }
            }
        }
    }

    private fun inflateView(mNativeAd: NativeAd) {
        binding.nativeAdView.visibility = View.VISIBLE
        if (mNativeAd.icon != null) {
            binding.nativeAdIcon.setImageDrawable(mNativeAd.icon!!.drawable)
        }
        binding.nativeAdTitle.text = mNativeAd.headline
        binding.nativeAdBody.text = mNativeAd.body
        if (mNativeAd.mediaContent != null) {
            binding.coverview.mediaContent = mNativeAd.mediaContent
        } else {
            binding.nativeAdIcon.visibility = View.GONE
        }
        binding.nativeAdView.headlineView = binding.nativeAdTitle
        binding.nativeAdView.mediaView = binding.coverview
        binding.nativeAdView.bodyView = binding.nativeAdBody
        binding.nativeAdView.callToActionView = binding.cta
        binding.nativeAdView.setNativeAd(mNativeAd)
    }
}