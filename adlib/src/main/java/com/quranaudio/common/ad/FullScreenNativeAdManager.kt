package com.quranaudio.common.ad

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.quranaudio.common.ad.model.AdItem
import com.quranaudio.common.ad.model.LoadingState

/** Displays one already-cached native ad as an immersive fallback ad. */
object FullScreenNativeAdManager {
    private const val CLOSE_DELAY_SECONDS = 5
    private var showingDialog: Dialog? = null

    fun hasCachedAd(activity: Activity): Boolean =
        showingDialog?.isShowing != true && NativeAdManager.getInstance().hasCachedAd(activity)

    fun showIfAvailable(activity: Activity, callback: AdShowCallback?): Boolean {
        if (activity.isFinishing || activity.isDestroyed || showingDialog?.isShowing == true) {
            return false
        }

        val nativeAd = NativeAdManager.getInstance().getCachedAd(activity) ?: return false
        val adItem = AdItem(
            id = AdConfig.getAdIdByPosition(AdConfig.AD_NATIVE),
            loadingState = LoadingState.LOADED,
            ad = nativeAd
        )

        return try {
            showDialog(activity, nativeAd, adItem, callback)
            true
        } catch (error: Throwable) {
            android.util.Log.e("FullScreenNativeAd", "Unable to show native fallback", error)
            nativeAd.destroy()
            showingDialog = null
            false
        }
    }

    private fun showDialog(
        activity: Activity,
        nativeAd: NativeAd,
        adItem: AdItem,
        callback: AdShowCallback?
    ) {
        val dialog = Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)

        val nativeAdView = NativeAdView(activity).apply {
            setBackgroundColor(Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val page = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(activity, 20), 0, dp(activity, 20), dp(activity, 28))
        }

        val topBar = FrameLayout(activity)
        page.addView(topBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 64)))

        val adLabel = TextView(activity).apply {
            text = "Ad"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(55, 55, 55), dp(activity, 4).toFloat())
            setPadding(dp(activity, 8), dp(activity, 3), dp(activity, 8), dp(activity, 3))
        }
        topBar.addView(adLabel, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.START or Gravity.CENTER_VERTICAL))

        // 关闭按钮不再放进 NativeAdView 内部（会被广告点击拦截，导致 X 无反应）。
        // 改为在根布局中置于 NativeAdView 之上，保证独立接收点击。见下方 root。
        val close = TextView(activity).apply {
            text = CLOSE_DELAY_SECONDS.toString()
            setTextColor(Color.WHITE)
            textSize = 24f
            gravity = Gravity.CENTER
            isEnabled = false
            contentDescription = "Close ad"
        }

        val mediaView = MediaView(activity).apply {
            setBackgroundColor(Color.rgb(18, 18, 18))
        }
        page.addView(mediaView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.15f))

        val content = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(activity, 8), dp(activity, 22), dp(activity, 8), 0)
        }
        page.addView(content, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        val icon = ImageView(activity).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = rounded(Color.rgb(35, 35, 35), dp(activity, 16).toFloat())
            clipToOutline = true
        }
        content.addView(icon, LinearLayout.LayoutParams(dp(activity, 76), dp(activity, 76)))

        val headline = TextView(activity).apply {
            text = nativeAd.headline
            setTextColor(Color.WHITE)
            textSize = 25f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            maxLines = 2
        }
        content.addView(headline, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(activity, 18) })

        val advertiser = TextView(activity).apply {
            text = nativeAd.advertiser?.let { "Sponsored by $it" } ?: "Sponsored"
            setTextColor(Color.LTGRAY)
            textSize = 14f
            gravity = Gravity.CENTER
            maxLines = 1
        }
        content.addView(advertiser, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(activity, 6) })

        val body = TextView(activity).apply {
            text = nativeAd.body
            setTextColor(Color.rgb(210, 210, 210))
            textSize = 17f
            gravity = Gravity.CENTER
            maxLines = 3
        }
        content.addView(body, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(activity, 22)
            marginStart = dp(activity, 16)
            marginEnd = dp(activity, 16)
        })

        val callToAction = TextView(activity).apply {
            text = nativeAd.callToAction ?: "Learn more"
            setTextColor(Color.WHITE)
            textSize = 19f
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            background = rounded(Color.rgb(20, 122, 255), dp(activity, 28).toFloat())
        }
        content.addView(callToAction, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(activity, 56)).apply {
            topMargin = dp(activity, 28)
            marginStart = dp(activity, 26)
            marginEnd = dp(activity, 26)
        })

        nativeAdView.addView(page)
        nativeAdView.mediaView = mediaView
        nativeAdView.iconView = icon
        nativeAdView.headlineView = headline
        nativeAdView.advertiserView = advertiser
        nativeAdView.bodyView = body
        nativeAdView.callToActionView = callToAction
        icon.setImageDrawable(nativeAd.icon?.drawable)
        icon.visibility = if (nativeAd.icon == null) View.GONE else View.VISIBLE
        body.visibility = if (nativeAd.body.isNullOrBlank()) View.GONE else View.VISIBLE
        nativeAdView.setNativeAd(nativeAd)

        val handler = Handler(Looper.getMainLooper())
        var remaining = CLOSE_DELAY_SECONDS
        val countdown = object : Runnable {
            override fun run() {
                remaining--
                if (remaining <= 0) {
                    close.text = "×"
                    close.isEnabled = true
                    close.setOnClickListener { dialog.dismiss() }
                    // 倒计时结束后允许返回键关闭，作为 X 之外的安全退出，避免链路卡死
                    dialog.setCancelable(true)
                } else {
                    close.text = remaining.toString()
                    handler.postDelayed(this, 1000)
                }
            }
        }

        dialog.setOnShowListener {
            callback?.onShow(adItem)
            callback?.onAdImpression(adItem)
            handler.postDelayed(countdown, 1000)
        }
        dialog.setOnDismissListener {
            handler.removeCallbacks(countdown)
            nativeAd.destroy()
            showingDialog = null
            callback?.onAdClosed(adItem)
        }
        // 根布局：NativeAdView 在下、关闭按钮在上，确保 X 始终独立接收点击（不被广告拦截）
        val root = FrameLayout(activity)
        root.addView(
            nativeAdView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        root.addView(
            close,
            FrameLayout.LayoutParams(dp(activity, 52), dp(activity, 52)).apply {
                gravity = Gravity.TOP or Gravity.END
                topMargin = dp(activity, 6)
                marginEnd = dp(activity, 20)
            }
        )
        dialog.setContentView(root)
        dialog.window?.apply {
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            statusBarColor = Color.BLACK
            navigationBarColor = Color.BLACK
        }
        showingDialog = dialog
        dialog.show()
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
    }

    private fun rounded(color: Int, radius: Float) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
    }

    private fun dp(activity: Activity, value: Int): Int =
        (value * activity.resources.displayMetrics.density).toInt()
}
