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
        // ⚠️ 池子从 3 降回 1：实际展示速度远低于消费/补池速度，池 3 导致大量请求从未展示
        private const val CACHE_POOL_SIZE = 1
        private const val MIN_CACHE_THRESHOLD = 1  // 消费后池空则补充
        private const val RETRY_DELAY_MS = 30000L  // 失败重试基准延迟，指数退避
        private const val MAX_RETRY_COUNT = 3  // 连续失败最多重试 3 次
        private const val AD_EXPIRATION_TIME_MS = 58 * 60 * 1000L  // 58 分钟过期（AdMob 60分钟限制）
        
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

    // 连续加载失败次数，成功或新的用户触发加载时重置
    private var retryCount = 0
    
    // Ad unit ID (reuse existing native ad ID from AdConfig)
    private val adUnitId: String
        get() = AdConfig.getAdIdByPosition(AdConfig.AD_NATIVE)
    
    // Pending load callbacks (for dynamic loading)
    private val pendingCallbacks = mutableListOf<(NativeAd?) -> Unit>()
    
    /**
     * Initialize the manager with application context.
     * Should be called in Application.onCreate().
     *
     * ⚠️ 不再启动定期清理任务：后台每 5 分钟轮询 + 过期即重载会让存活的后台进程
     * 每小时白烧一池广告请求。过期广告已在消费时（getCachedAd/loadAdWithCallback）
     * 检查并跳过，无需定时器。
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        Log.d(TAG, "✅ NativeAdManager initialized")
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
            android.util.Log.e("NATIVE_AD_TRACK", "❌ NativeAdManager.loadNewAd() - AppContext is null")
            Log.e(TAG, "❌ AppContext is null, cannot load ad")
            return
        }
        
        // ✅ 检查缓存池是否已满
        if (cachedNativeAds.size >= CACHE_POOL_SIZE) {
            android.util.Log.d("NATIVE_AD_TRACK", "✅ Cache pool is full (${cachedNativeAds.size}/$CACHE_POOL_SIZE), skipping load")
            Log.d(TAG, "✅ Cache pool is full (${cachedNativeAds.size}/$CACHE_POOL_SIZE), skipping load")
            return
        }
        
        if (isLoading) {
            android.util.Log.d("NATIVE_AD_TRACK", "⚠️ Ad is already loading, skipping duplicate request")
            Log.d(TAG, "⚠️ Ad is already loading, skipping duplicate request")
            return
        }
        
        // Check subscription status
        if (SubscriptionChecker.isUserSubscribed(context)) {
            android.util.Log.d("NATIVE_AD_TRACK", "🎁 User is subscribed, skipping native ad load")
            Log.d(TAG, "🎁 User is subscribed, skipping native ad load")
            return
        }
        
        isLoading = true
        
        android.util.Log.d("NATIVE_AD_TRACK", "🔄 NativeAdManager.loadNewAd() - Starting ad load (${cachedNativeAds.size}/$CACHE_POOL_SIZE)")
        android.util.Log.d("NATIVE_AD_TRACK", "   Ad Unit ID: $adUnitId")
        Log.d(TAG, "🔄 Loading new native ad (${cachedNativeAds.size}/$CACHE_POOL_SIZE)")
        
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { nativeAd ->
                android.util.Log.d("NATIVE_AD_TRACK", "✅ Native ad loaded from AdMob")
                android.util.Log.d("NATIVE_AD_TRACK", "   Headline: ${nativeAd.headline}")
                android.util.Log.d("NATIVE_AD_TRACK", "   Body: ${nativeAd.body?.take(50)}")
                android.util.Log.d("NATIVE_AD_TRACK", "   Advertiser: ${nativeAd.advertiser}")
                
                // ✅ AdMob 会在 NativeAdView 显示时自动追踪 impression
                // NativeAdView.setNativeAd() 触发时会自动记录 impression
                
                // ✅ 存储时添加时间戳
                val cachedAd = CachedNativeAd(nativeAd, System.currentTimeMillis())
                cachedNativeAds.add(cachedAd)
                isLoading = false
                retryCount = 0
                
                android.util.Log.d("NATIVE_AD_TRACK", "✅ Ad added to cache pool (${cachedNativeAds.size}/$CACHE_POOL_SIZE)")
                android.util.Log.d("NATIVE_AD_TRACK", "   Pending callbacks: ${pendingCallbacks.size}")
                Log.d(TAG, "✅ Native ad loaded successfully (cache: ${cachedNativeAds.size}/$CACHE_POOL_SIZE)")
                
                // Notify pending callbacks
                notifyPendingCallbacks(nativeAd)
                
                // ✅ 继续加载直到缓存池满
                if (cachedNativeAds.size < CACHE_POOL_SIZE) {
                    android.util.Log.d("NATIVE_AD_TRACK", "📦 Pool not full yet, loading next ad...")
                    Log.d(TAG, "📦 Pool not full yet, loading next ad...")
                    loadNewAd()
                } else {
                    android.util.Log.d("NATIVE_AD_TRACK", "🎉 Cache pool full (${cachedNativeAds.size}/$CACHE_POOL_SIZE)")
                    Log.d(TAG, "🎉 Cache pool full (${cachedNativeAds.size}/$CACHE_POOL_SIZE)")
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    android.util.Log.e("NATIVE_AD_TRACK", "❌ Native ad load failed")
                    android.util.Log.e("NATIVE_AD_TRACK", "   Error code: ${loadAdError.code}")
                    android.util.Log.e("NATIVE_AD_TRACK", "   Error message: ${loadAdError.message}")
                    android.util.Log.e("NATIVE_AD_TRACK", "   Domain: ${loadAdError.domain}")
                    Log.e(TAG, "❌ Failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                    isLoading = false
                    
                    // Notify pending callbacks with null
                    android.util.Log.d("NATIVE_AD_TRACK", "   Notifying ${pendingCallbacks.size} pending callbacks with null")
                    notifyPendingCallbacks(null)

                    // ✅ 缓存池为空时重试：指数退避（30s/60s/120s），最多 3 次，后台不重试
                    if (cachedNativeAds.isEmpty()) {
                        if (retryCount >= MAX_RETRY_COUNT) {
                            Log.w(TAG, "⛔ Max retries reached ($MAX_RETRY_COUNT), waiting for next ad request")
                            return
                        }
                        val delay = RETRY_DELAY_MS shl retryCount
                        retryCount++
                        Log.d(TAG, "⏰ Pool empty, retrying in ${delay / 1000}s (attempt $retryCount/$MAX_RETRY_COUNT)")
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (!AdFactory.isAppInForeground()) {
                                Log.d(TAG, "⏸️ App in background, skipping retry")
                                return@postDelayed
                            }
                            loadNewAd()
                        }, delay)
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
        
        // If not loading, start loading (fresh user demand, reset retry budget)
        if (!isLoading) {
            Log.d(TAG, "🔄 Starting dynamic ad load for callback")
            retryCount = 0
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
        // ✅ 只允许主线程消费：原实现 post 到主线程后立即 return null，
        // post 过去的那次调用会从池里取走一个广告然后丢弃（消费了却永远不展示）
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Log.w(TAG, "⚠️ getCachedAd called from background thread, returning null (call from main thread)")
            return null
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
        // ✅ 尝试补充缓存（异步，新的用户需求重置重试预算）
        retryCount = 0
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

