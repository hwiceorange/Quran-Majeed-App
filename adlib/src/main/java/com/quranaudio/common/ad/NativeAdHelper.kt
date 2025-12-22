package com.quranaudio.common.ad

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Helper class to display native ads in ViewGroups with dynamic loading support.
 * 
 * Usage:
 * val container = findViewById<ViewGroup>(R.id.native_ad_container)
 * NativeAdHelper.displayNativeAdWithAutoLoad(activity, container, R.layout.native_ad_onboarding)
 */
object NativeAdHelper {
    
    private const val TAG = "NativeAdHelper"
    
    /**
     * Display a native ad in the given container with automatic loading.
     * If no cached ad is available, loads one dynamically.
     * 
     * @param activity Activity context
     * @param container ViewGroup to hold the ad view
     * @param layoutResId Layout resource ID for the native ad
     */
    fun displayNativeAdWithAutoLoad(
        activity: Activity,
        container: ViewGroup,
        layoutResId: Int
    ) {
        Log.d(TAG, "🔄 Attempting to display native ad with auto-load")
        
        // Load ad with callback (will use cache if available, or load dynamically)
        NativeAdManager.getInstance().loadAdWithCallback(activity) { nativeAd ->
            if (nativeAd == null) {
                Log.d(TAG, "⚠️ No ad available (subscribed or failed to load)")
                container.visibility = View.GONE
                return@loadAdWithCallback
            }
            
            try {
                Log.d(TAG, "📺 Displaying native ad")
                
                // ✅ AdMob 会在 NativeAdView 显示时自动追踪 impression
                // 无需手动设置监听器
                
                // Inflate the ad layout
                val adView = LayoutInflater.from(activity).inflate(layoutResId, container, false) as NativeAdView
                
                // Populate the ad view with native ad data
                populateNativeAdView(nativeAd, adView)
                
                // Setup click listener to refresh ad when user returns
                setupAdClickRefresh(nativeAd, activity, container, layoutResId)
                
                // Add the ad view to container
                container.removeAllViews()
                container.addView(adView)
                container.visibility = View.VISIBLE
                
                Log.d(TAG, "✅ Native ad displayed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to display ad: ${e.message}", e)
                container.visibility = View.GONE
            }
        }
    }
    
    /**
     * Setup ad click listener to refresh ad when user returns.
     */
    private fun setupAdClickRefresh(
        nativeAd: NativeAd,
        activity: Activity,
        container: ViewGroup,
        layoutResId: Int
    ) {
        // Note: We can't directly listen to ad clicks, but we can prepare for refresh
        // The ad will be refreshed when the fragment resumes (handled in Fragment)
        Log.d(TAG, "🔧 Ad click refresh setup (refresh will happen on resume)")
    }
    
    /**
     * Display a native ad in the given container (legacy method - uses cache only).
     * 
     * @param activity Activity context
     * @param container ViewGroup to hold the ad view
     * @param layoutResId Layout resource ID for the native ad
     * @return true if ad was displayed, false otherwise
     */
    @Deprecated("Use displayNativeAdWithAutoLoad() instead for better user experience")
    fun displayNativeAd(
        activity: Activity,
        container: ViewGroup,
        layoutResId: Int
    ): Boolean {
        // Get cached ad from NativeAdManager
        val nativeAd = NativeAdManager.getInstance().getCachedAd(activity)
        
        if (nativeAd == null) {
            Log.d(TAG, "⚠️ No cached native ad available, hiding container")
            container.visibility = View.GONE
            return false
        }
        
        Log.d(TAG, "📺 Displaying native ad")
        
        // Inflate the ad layout
        val adView = LayoutInflater.from(activity).inflate(layoutResId, container, false) as NativeAdView
        
        // Populate the ad view with native ad data
        populateNativeAdView(nativeAd, adView)
        
        // Add the ad view to container
        container.removeAllViews()
        container.addView(adView)
        container.visibility = View.VISIBLE
        
        Log.d(TAG, "✅ Native ad displayed successfully")
        return true
    }
    
    /**
     * Populate NativeAdView with NativeAd data.
     * Maps ad components to their respective views using dynamic IDs.
     */
    private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
        // Get resource IDs dynamically from the app's R class
        val context = adView.context
        val packageName = context.packageName
        
        // Set the media view
        try {
            val mediaViewId = context.resources.getIdentifier("ad_media", "id", packageName)
            val mediaView = adView.findViewById<MediaView>(mediaViewId)
            mediaView?.let {
                adView.mediaView = it
            }
        } catch (e: Exception) {
            Log.w(TAG, "Media view not found: ${e.message}")
        }
        
        // Set the headline
        try {
            val headlineViewId = context.resources.getIdentifier("ad_headline", "id", packageName)
            val headlineView = adView.findViewById<TextView>(headlineViewId)
            headlineView?.let {
                it.text = nativeAd.headline
                adView.headlineView = it
            }
        } catch (e: Exception) {
            Log.w(TAG, "Headline view not found: ${e.message}")
        }
        
        // Set the body
        try {
            val bodyViewId = context.resources.getIdentifier("ad_body", "id", packageName)
            val bodyView = adView.findViewById<TextView>(bodyViewId)
            bodyView?.let {
                it.text = nativeAd.body
                adView.bodyView = it
            }
        } catch (e: Exception) {
            Log.w(TAG, "Body view not found: ${e.message}")
        }
        
        // Set the call to action button
        try {
            val ctaViewId = context.resources.getIdentifier("ad_call_to_action", "id", packageName)
            val callToActionView = adView.findViewById<Button>(ctaViewId)
            callToActionView?.let {
                it.text = nativeAd.callToAction
                adView.callToActionView = it
            }
        } catch (e: Exception) {
            Log.w(TAG, "CTA button not found: ${e.message}")
        }
        
        // Register the native ad with the ad view
        adView.setNativeAd(nativeAd)
        
        Log.d(TAG, "📋 Native ad view populated: headline=${nativeAd.headline}")
    }
    
    /**
     * Hide the native ad container.
     * 
     * @param container ViewGroup to hide
     */
    fun hideNativeAdContainer(container: ViewGroup) {
        container.removeAllViews()
        container.visibility = View.GONE
        Log.d(TAG, "🙈 Native ad container hidden")
    }
}

