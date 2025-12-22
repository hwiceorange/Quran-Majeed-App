package com.quran.quranaudio.quiz.utils

import android.util.Log
import com.quran.quranaudio.quiz.base.CloudManager

/**
 * ✅ 原生广告时间间隔管理器（按场景区分）
 * 
 * 优化特性:
 * - ✅ 按场景（Tag）独立计时，互不影响
 * - ✅ 默认间隔 5 分钟（可通过云端配置）
 * - ✅ 支持不同场景不同间隔
 * - ✅ 提供详细日志和管理方法
 */
object NativeAdTimeUtil {
    private const val TAG = "NativeAdTimeUtil"
    
    // ✅ 按场景存储上次展示时间
    private val showTimeMapByTag = hashMapOf<String, Long>()
    
    // ✅ 默认间隔时间：5 分钟
    private const val DEFAULT_INTERVAL_MS = 5 * 60 * 1000L  // 5 minutes
    
    /**
     * ✅ 按场景（Tag）检查是否应该拦截
     * 
     * @param tag 场景标识（如 "quiz_review_learn", "onboarding_language"）
     * @param customInterval 自定义间隔时间（毫秒），null 则使用云端配置或默认值
     * @return true 表示应该拦截，false 表示可以展示
     */
    fun isInterceptByTag(tag: String, customInterval: Long? = null): Boolean {
        // 获取间隔配置（优先级：自定义 > 云端 > 默认）
        val intervalMs = customInterval 
            ?: CloudManager.getNativeIntervalTime()?.takeIf { it > 0 }
            ?: DEFAULT_INTERVAL_MS
        
        // 获取该场景的上次展示时间
        val lastTime = showTimeMapByTag[tag]
        
        if (lastTime == null) {
            // 第一次展示，不拦截
            Log.d(TAG, "✅ First show for tag: $tag")
            return false
        }
        
        // 计算时间间隔
        val elapsedTime = System.currentTimeMillis() - lastTime
        val shouldIntercept = elapsedTime < intervalMs
        
        if (shouldIntercept) {
            val remainingTime = (intervalMs - elapsedTime) / 1000  // seconds
            Log.d(TAG, "⏱️ Intercept tag: $tag (${remainingTime}s remaining, interval: ${intervalMs / 1000}s)")
        } else {
            Log.d(TAG, "✅ Allow show for tag: $tag (${elapsedTime / 1000}s elapsed, interval: ${intervalMs / 1000}s)")
        }
        
        return shouldIntercept
    }
    
    /**
     * ✅ 保存场景的展示时间
     */
    fun saveTimeByTag(tag: String, time: Long) {
        showTimeMapByTag[tag] = time
        Log.d(TAG, "💾 Saved show time for tag: $tag")
    }
    
    /**
     * ✅ 清除某个场景的时间记录（用于测试或重置）
     */
    fun clearTimeForTag(tag: String) {
        showTimeMapByTag.remove(tag)
        Log.d(TAG, "🗑️ Cleared time for tag: $tag")
    }
    
    /**
     * ✅ 清除所有时间记录
     */
    fun clearAllTimes() {
        showTimeMapByTag.clear()
        Log.d(TAG, "🗑️ Cleared all show times")
    }
    
    /**
     * ✅ 获取某个场景的上次展示时间
     */
    fun getLastShowTime(tag: String): Long? {
        return showTimeMapByTag[tag]
    }
    
    /**
     * ✅ 获取某个场景距离上次展示的时间（秒）
     */
    fun getElapsedTimeSinceLastShow(tag: String): Long? {
        val lastTime = showTimeMapByTag[tag] ?: return null
        return (System.currentTimeMillis() - lastTime) / 1000
    }
    
    // ⚠️ 保留旧方法以兼容现有代码，但标记为废弃
    @Deprecated(
        message = "Use isInterceptByTag() instead for better scene management",
        replaceWith = ReplaceWith("isInterceptByTag(functionTag)")
    )
    fun isIntercept(functionTag: String): Boolean {
        return isInterceptByTag(functionTag)
    }
    
    @Deprecated(
        message = "Use saveTimeByTag() instead",
        replaceWith = ReplaceWith("saveTimeByTag(functionTag, time)")
    )
    fun saveTime(functionTag: String, time: Long) {
        saveTimeByTag(functionTag, time)
    }
}