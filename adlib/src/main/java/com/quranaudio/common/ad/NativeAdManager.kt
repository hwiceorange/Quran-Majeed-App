package com.quranaudio.common.ad

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions

/**
 * ✅ 统一的原生广告管理器（优化版 v2）
 * 
 * 优化特性:
 * - ✅ 缓存池从 1 提升到 3，保证广告始终可用
 * - ✅ FIFO 机制，防止广告过期
 * - ✅ 自动补充，消费后立即加载
 * - ✅ 失败重试，确保缓存池健康
 * - ✅ 订阅状态检查，VIP 用户不加载广告
 * - 🆕 广告过期检查（58分钟自动丢弃）
 * - 🆕 Impression 监控
 * - 🆕 主线程安全保障
 * 
 * Usage:
 * - Call initialize() and preloadAd() in Application.onCreate()
 * - Call getCachedAd() to retrieve ad (only shows cached, no async loading)
 */
class NativeAdManager private constructor() {
    
    companion object {
        private const val TAG = "NativeAdManager"
        private const val CACHE_POOL_SIZE = 3  // ✅ 从 1 提升到 3
        private const val MIN_CACHE_THRESHOLD = 2  // ✅ 低于 2 立即补充
        private const val RETRY_DELAY_MS = 30000L  // ✅ 失败后 30 秒重试
        private const val AD_EXPIRATION_TIME_MS = 58 * 60 * 1000L  // 🆕 58 分钟过期（AdMob 60分钟限制）
        private const val CLEANUP_INTERVAL_MS = 5 * 60 * 1000L  // 🆕 5 分钟清理一次过期广告
        
        @Volatile
        private var instance: NativeAdManager? = null
        
        fun getInstance(): NativeAdManager {
            return instance ?: synchronized(this) {
                instance ?: NativeAdManager().also { instance = it }
            }
        }
    }
    
    /**
     * 🆕 包装类：NativeAd + 时间戳
     * 用于跟踪广告加载时间，防止展示过期广告
     */
    private data class CachedNativeAd(
        val ad: NativeAd,
        val loadTime: Long = System.currentTimeMillis()
    ) {
        /**
         * 检查广告是否过期（58分钟）
         */
        fun isExpired(): Boolean {
            val age = System.currentTimeMillis() - loadTime
            return age > AD_EXPIRATION_TIME_MS
        }
        
        /**
         * 获取广告年龄（分钟）
         */
        fun getAgeInMinutes(): Long {
            return (System.currentTimeMillis() - loadTime) / (60 * 1000)
        }
    }
    
    // Application context for ad loading
    private var appContext: Context? = null
    
    // ✅ 改为缓存池（支持多个广告 + 时间戳）
    private val cachedNativeAds = mutableListOf<CachedNativeAd>()
    
    // Loading state flag
    private var isLoading = false
    
    // Ad unit ID (reuse existing native ad ID from AdConfig)
    private val adUnitId: String
        get() = AdConfig.getAdIdByPosition(AdConfig.AD_NATIVE)
    
    // Pending load callbacks (for dynamic loading)
    private val pendingCallbacks = mutableListOf<(NativeAd?) -> Unit>()
    
    /**
     * Initialize the manager with application context.
     * Should be called in Application.onCreate().
     * 
     * 🆕 启动定期清理任务
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "✅ NativeAdManager initialized")
        
        // 🆕 启动定期清理过期广告的任务
        startPeriodicCleanup()
    }
    
    /**
     * 🆕 启动定期清理任务（5分钟一次）
     */
    private fun startPeriodicCleanup() {
        Handler(Looper.getMainLooper()).post(object : Runnable {
            override fun run() {
                cleanupExpiredAds()
                // 下次清理
                Handler(Looper.getMainLooper()).postDelayed(this, CLEANUP_INTERVAL_MS)
            }
        })
        Log.d(TAG, "🆕 Periodic cleanup task started (interval: ${CLEANUP_INTERVAL_MS / 60000} minutes)")
    }
    
    /**
     * 🆕 清理过期广告（58分钟以上）
     */
    private fun cleanupExpiredAds() {
        val expiredAds = cachedNativeAds.filter { it.isExpired() }
        
        if (expiredAds.isNotEmpty()) {
            Log.w(TAG, "⚠️ Found ${expiredAds.size} expired ads, cleaning up...")
            
            expiredAds.forEach { cachedAd ->
                try {
                    cachedAd.ad.destroy()
                    Log.d(TAG, "🗑️ Destroyed expired ad (age: ${cachedAd.getAgeInMinutes()} minutes)")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to destroy expired ad: ${e.message}")
                }
            }
            
            cachedNativeAds.removeAll(expiredAds)
            
            // 清理后如果缓存池变空，立即加载
            if (cachedNativeAds.isEmpty()) {
                Log.d(TAG, "📦 Cache empty after cleanup, loading new ads...")
                loadNewAd()
            }
        }
    }
    
    /**
     * Preload a native ad on cold start.
     * Should be called in Application.onCreate() after initialize().
     */
    fun preloadAd() {
        if (appContext == null) {
            Log.e(TAG, "❌ Cannot preload ad: NativeAdManager not initialized")
            return
        }
        
        loadNewAd()
        Log.d(TAG, "✅ Preload initiated")
    }
    
    /**
     * ✅ 加载新的原生广告到缓存池
     * 
     * 优化逻辑:
     * - 缓存池满了不加载
     * - 订阅用户不加载
     * - 加载成功后自动检查是否需要继续加载
     * - 加载失败自动重试
     */
    fun loadNewAd() {
        val context = appContext
        if (context == null) {
            Log.e(TAG, "❌ AppContext is null, cannot load ad")
            return
        }
        
        // ✅ 检查缓存池是否已满
        if (cachedNativeAds.size >= CACHE_POOL_SIZE) {
            Log.d(TAG, "✅ Cache pool is full (${cachedNativeAds.size}/$CACHE_POOL_SIZE), skipping load")
            return
        }
        
        if (isLoading) {
            Log.d(TAG, "⚠️ Ad is already loading, skipping duplicate request")
            return
        }
        
        // Check subscription status
        if (SubscriptionChecker.isUserSubscribed(context)) {
            Log.d(TAG, "🎁 User is subscribed, skipping native ad load")
            return
        }
        
        isLoading = true
        
        Log.d(TAG, "🔄 Loading new native ad (${cachedNativeAds.size}/$CACHE_POOL_SIZE)")
        
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                // ✅ AdMob 会在 NativeAdView 显示时自动追踪 impression
                // NativeAdView.setNativeAd() 触发时会自动记录 impression
                
                // ✅ 存储时添加时间戳
                val cachedAd = CachedNativeAd(nativeAd, System.currentTimeMillis())
                cachedNativeAds.add(cachedAd)
                isLoading = false
                
                Log.d(TAG, "✅ Native ad loaded successfully (cache: ${cachedNativeAds.size}/$CACHE_POOL_SIZE)")
                
                // Notify pending callbacks
                notifyPendingCallbacks(nativeAd)
                
                // ✅ 继续加载直到缓存池满
                if (cachedNativeAds.size < CACHE_POOL_SIZE) {
                    Log.d(TAG, "📦 Pool not full yet, loading next ad...")
                    loadNewAd()
                } else {
                    Log.d(TAG, "🎉 Cache pool full (${cachedNativeAds.size}/$CACHE_POOL_SIZE)")
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.e(TAG, "❌ Failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                    isLoading = false
                    
                    // Notify pending callbacks with null
                    notifyPendingCallbacks(null)
                    
                    // ✅ 如果缓存池为空，延迟重试
                    if (cachedNativeAds.isEmpty()) {
                        Log.d(TAG, "⏰ Pool empty, retrying in 30s...")
                        Handler(Looper.getMainLooper()).postDelayed({
                            loadNewAd()
                        }, RETRY_DELAY_MS)
                    }
                }
                
                override fun onAdClicked() {
                    Log.d(TAG, "👆 Native ad clicked")
                }
                
                override fun onAdImpression() {
                    Log.d(TAG, "👁️ Native ad impression recorded (AdListener)")
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setRequestCustomMuteThisAd(true)
                    .build()
            )
            .build()
        
        adLoader.loadAd(AdRequest.Builder().build())
    }
    
    /**
     * Notify all pending callbacks with the result.
     */
    private fun notifyPendingCallbacks(nativeAd: NativeAd?) {
        if (pendingCallbacks.isNotEmpty()) {
            Log.d(TAG, "📢 Notifying ${pendingCallbacks.size} pending callbacks")
            pendingCallbacks.forEach { callback ->
                try {
                    callback(nativeAd)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in callback: ${e.message}", e)
                }
            }
            pendingCallbacks.clear()
        }
    }
    
    /**
     * ✅ 动态加载广告（带回调）
     * 
     * 适用场景: Onboarding 页面等需要等待广告加载的场景
     * 
     * 优化逻辑:
     * - 有缓存立即返回（FIFO）
     * - 🆕 检查并跳过过期广告
     * - 无缓存则加入等待队列
     * - 自动触发加载和补充
     * - 🆕 确保回调在主线程
     */
    fun loadAdWithCallback(activity: Activity, callback: (NativeAd?) -> Unit) {
        // 🆕 确保在主线程执行（UI 安全）
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post {
                loadAdWithCallback(activity, callback)
            }
            return
        }
        
        // Check subscription status
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            Log.d(TAG, "🎁 User is subscribed, no ad to load")
            callback(null)
            return
        }
        
        // ✅ 如果有缓存，立即返回（FIFO，🆕 跳过过期广告）
        while (cachedNativeAds.isNotEmpty()) {
            val cachedAd = cachedNativeAds.removeFirst()
            
            // 🆕 检查是否过期
            if (cachedAd.isExpired()) {
                Log.w(TAG, "⚠️ Skipping expired ad (age: ${cachedAd.getAgeInMinutes()} minutes)")
                try {
                    cachedAd.ad.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to destroy expired ad: ${e.message}")
                }
                continue  // 跳过过期广告，继续取下一个
            }
            
            // ✅ 找到未过期的广告
            Log.d(TAG, "✅ Returning cached ad immediately (age: ${cachedAd.getAgeInMinutes()} min, ${cachedNativeAds.size} remaining)")
            
            // ✅ 如果缓存池低于阈值，立即补充
            if (cachedNativeAds.size < MIN_CACHE_THRESHOLD) {
                Log.d(TAG, "📦 Cache low, replenishing...")
                loadNewAd()
            }
            
            callback(cachedAd.ad)
            return
        }
        
        // ✅ 无可用缓存，加入等待队列
        Log.d(TAG, "⚠️ No valid cached ad, adding to wait queue")
        pendingCallbacks.add(callback)
        
        // If not loading, start loading
        if (!isLoading) {
            Log.d(TAG, "🔄 Starting dynamic ad load for callback")
            loadNewAd()
        } else {
            Log.d(TAG, "⏳ Ad is loading, callback queued (${pendingCallbacks.size} waiting)")
        }
    }
    
    /**
     * ✅ 获取缓存的原生广告（仅缓存，不异步加载）
     * 
     * 适用场景: Quiz Review、Learning Plan 等需要立即展示的场景
     * 
     * 优化逻辑:
     * - 只返回已缓存的广告（FIFO）
     * - 🆕 自动跳过过期广告（58分钟以上）
     * - 无缓存立即返回 null（不等待加载）
     * - 消费后自动补充
     * - 🆕 确保在主线程执行
     * 
     * @param activity Activity context for subscription check
     * @return NativeAd object if available, null otherwise
     */
    fun getCachedAd(activity: Activity): NativeAd? {
        // 🆕 确保在主线程执行（UI 安全）
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.w(TAG, "⚠️ getCachedAd called from background thread, switching to main thread")
            var result: NativeAd? = null
            Handler(Looper.getMainLooper()).post {
                result = getCachedAd(activity)
            }
            return result
        }
        
        // Check subscription status
        if (SubscriptionChecker.isUserSubscribed(activity)) {
            Log.d(TAG, "🎁 User is subscribed, no native ad to show")
            return null
        }
        
        // ✅ FIFO: 取第一个广告，🆕 自动跳过过期广告
        while (cachedNativeAds.isNotEmpty()) {
            val cachedAd = cachedNativeAds.removeFirst()
            
            // 🆕 检查是否过期
            if (cachedAd.isExpired()) {
                Log.w(TAG, "⚠️ Skipping expired ad (age: ${cachedAd.getAgeInMinutes()} minutes)")
                try {
                    cachedAd.ad.destroy()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Failed to destroy expired ad: ${e.message}")
                }
                continue  // 跳过过期广告，继续取下一个
            }
            
            // ✅ 找到未过期的广告
            Log.d(TAG, "📺 Returning cached native ad (age: ${cachedAd.getAgeInMinutes()} min, ${cachedNativeAds.size} remaining)")
            
            // ✅ 如果缓存池低于阈值，立即补充
            if (cachedNativeAds.size < MIN_CACHE_THRESHOLD) {
                Log.d(TAG, "📦 Cache low, replenishing...")
            loadNewAd()
            }
            
            return cachedAd.ad
        }
        
        // ✅ 无可用缓存
        Log.d(TAG, "⚠️ No valid cached native ad available")
        // ✅ 尝试补充缓存（异步）
        loadNewAd()
        return null
    }
    
    /**
     * ✅ 检查是否有缓存广告
     */
    fun hasCachedAd(context: Context): Boolean {
        if (SubscriptionChecker.isUserSubscribed(context)) {
            return false
        }
        return cachedNativeAds.isNotEmpty()
    }
    
    /**
     * ✅ 获取当前缓存池大小（用于监控）
     */
    fun getCacheSize(): Int {
        return cachedNativeAds.size
    }
    
    /**
     * ✅ 清理资源
     */
    fun destroy() {
        // ✅ 销毁所有缓存的广告
        cachedNativeAds.forEach { cachedAd ->
            try {
                cachedAd.ad.destroy()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to destroy ad: ${e.message}")
            }
        }
        cachedNativeAds.clear()
        pendingCallbacks.clear()
        appContext = null
        Log.d(TAG, "🗑️ NativeAdManager destroyed")
    }
}

