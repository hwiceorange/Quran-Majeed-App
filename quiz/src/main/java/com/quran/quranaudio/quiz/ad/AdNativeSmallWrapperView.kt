package com.quran.quranaudio.quiz.ad

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.marginBottom
import com.applovin.mediation.nativeAds.MaxNativeAdView
import com.quran.quranaudio.quiz.extension.gone
import com.quran.quranaudio.quiz.extension.isValid
import com.quran.quranaudio.quiz.extension.reportEvent
import com.quran.quranaudio.quiz.extension.visible
import com.quran.quranaudio.quiz.utils.NativeAdTimeUtil
import com.google.android.gms.ads.nativead.NativeAd
import com.quranaudio.common.ad.NativeAdManager
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.databinding.LayoutAdNativeSmallWrapperBinding

class AdNativeSmallWrapperView : LinearLayout {
    private val TAG = "small_ad"
    val binding =
        LayoutAdNativeSmallWrapperBinding.inflate(LayoutInflater.from(context), this, true)

    var isShowAd = false
    var isLoadAd = true
    var defaultMarginTop = -1f
    var defaultMarginBottom = -1f
    var adTag = ""

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        attrs?.let {
            val ob = context.obtainStyledAttributes(it, R.styleable.AdNativeSmallWrapperView)
            isShowAd = ob.getBoolean(R.styleable.AdNativeSmallWrapperView_default_show, false)
            isLoadAd = ob.getBoolean(R.styleable.AdNativeSmallWrapperView_default_load, true)
            defaultMarginTop =
                ob.getDimension(R.styleable.AdNativeSmallWrapperView_default_margin_top, -1f)
            defaultMarginBottom =
                ob.getDimension(R.styleable.AdNativeSmallWrapperView_default_margin_bottom, -1f)
            ob.recycle()
        }
        if (defaultMarginTop == -1f) {
            defaultMarginTop = resources.getDimension(R.dimen.dp_20)
        }
        (binding.root.layoutParams as LayoutParams).apply {
            val customMarginBottom =
                if (defaultMarginBottom == -1f) marginBottom else defaultMarginBottom
            setMargins(marginStart, defaultMarginTop.toInt(), marginEnd, customMarginBottom.toInt())
            binding.root.layoutParams = this
        }
        if (isShowAd) {
            binding.root.visible()
            binding.nativeAdView.visible()
        } else {
            binding.root.gone()
            binding.nativeAdView.gone()
        }
    }

    /**
     * 🔥 加载并展示原生广告 - 优化版
     * 
     * 改进:
     * - ❌ 移除时间间隔限制（用户要求：每次都展示）
     * - ✅ 优先使用缓存（快速）
     * - ✅ 没有缓存时动态加载（保证展示）
     * - ✅ 确保 UI 操作在主线程
     */
    fun loadNativeAd(adTag: String) {
        // 确保在主线程执行
        if (android.os.Looper.myLooper() != android.os.Looper.getMainLooper()) {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                loadNativeAd(adTag)
            }
            return
        }
        
        reportEvent(adTag, "show_native_ad")
        this.adTag = adTag
        
        val activity = context as? Activity
        if (activity == null || !activity.isValid()) {
            Log.w(TAG, "⚠️ Activity invalid")
            binding.root.gone()
            return
        }
        
        if (!isLoadAd) {
            Log.d(TAG, "⚠️ isLoadAd = false")
            binding.root.gone()
            return
        }
        
        // 🔥 改进：使用 NativeAdHelper 的自动加载方法
        // 优先使用缓存，没有缓存会动态加载
        try {
            com.quranaudio.common.ad.NativeAdHelper.displayNativeAdWithAutoLoad(
                activity,
                this,
                R.layout.layout_ad_native_small_wrapper
            )
            Log.d(TAG, "✅ Ad display initiated for: $adTag")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to load ad: ${e.message}", e)
            reportEvent(adTag, "native_ad_error", e.message ?: "load_failed")
            binding.root.gone()
        }
    }

    /**
     * ✅ 渲染原生广告视图
     */
    private fun inflateView(mNativeAd: NativeAd) {
        try {
            // 显示容器
        binding.root.visibility = View.VISIBLE
        binding.adMaxFl.visibility = View.GONE
        binding.nativeAdView.visibility = View.VISIBLE
            
            // Icon
        if (mNativeAd.icon != null) {
            binding.nativeAdIcon.setImageDrawable(mNativeAd.icon!!.drawable)
                binding.nativeAdIcon.visibility = View.VISIBLE
            } else {
                binding.nativeAdIcon.visibility = View.GONE
        }
            
            // Headline
        binding.nativeAdTitle.text = mNativeAd.headline
            
            // Body
        binding.nativeAdBody.text = mNativeAd.body
            
            // Media
        if (mNativeAd.mediaContent != null) {
            binding.coverview.mediaContent = mNativeAd.mediaContent
                binding.coverview.visibility = View.VISIBLE
        } else {
                binding.coverview.visibility = View.GONE
        }
            
            // Call to Action
            binding.cta.text = mNativeAd.callToAction
            
            // Bind views
        binding.nativeAdView.headlineView = binding.nativeAdTitle
        binding.nativeAdView.mediaView = binding.coverview
        binding.nativeAdView.bodyView = binding.nativeAdBody
        binding.nativeAdView.callToActionView = binding.cta
            binding.nativeAdView.iconView = binding.nativeAdIcon
            
            // Set native ad
            binding.nativeAdView.setNativeAd(mNativeAd)
            
            Log.d(TAG, "✅ Native ad inflated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to inflate native ad", e)
            binding.root.gone()
            throw e
        }
    }
}