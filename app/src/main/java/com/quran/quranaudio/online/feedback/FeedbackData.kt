package com.quran.quranaudio.online.feedback

import com.google.firebase.Timestamp
import com.quran.quranaudio.online.R

/**
 * 用户反馈数据模型
 * 用于诊断 9.4% 次日留存和 52秒平均时长问题
 */
data class FeedbackData(
    // 用户选择的情绪
    val emotion: FeedbackEmotion,
    
    // 用户选择的标签列表
    val selectedTags: List<String>,
    
    // 用户输入的额外评论
    val comment: String?,
    
    // 自动收集的设备信息
    val deviceInfo: DeviceInfo,
    
    // 自动收集的应用状态
    val appState: AppState,
    
    // 提交时间
    val timestamp: Any = com.google.firebase.firestore.FieldValue.serverTimestamp()
)

/**
 * 用户情绪类型
 */
enum class FeedbackEmotion(val emoji: String, val labelResId: Int) {
    LOVE("😍", R.string.feedback_emotion_love),
    NEUTRAL("😐", R.string.feedback_emotion_neutral),
    HATE("😡", R.string.feedback_emotion_hate)
}

/**
 * 设备信息
 */
data class DeviceInfo(
    val deviceName: String,        // 设备型号 (e.g., "Samsung Galaxy S21")
    val systemVersion: String,     // Android 版本 (e.g., "Android 12")
    val appVersion: String,        // 应用版本 (e.g., "1.9.20")
    val screenSize: String,        // 屏幕尺寸 (e.g., "1080x2400")
    val language: String           // 当前语言设置
)

/**
 * 应用状态信息
 */
data class AppState(
    val currentPage: String,       // 当前页面 (e.g., "MainActivity", "QuizActivity")
    val readingProgress: String?,  // 阅读进度 (e.g., "Surah 2, Ayah 255")
    val sessionDuration: Long,     // 当前会话时长（秒）
    val isFirstLaunch: Boolean     // 是否首次启动
)

/**
 * 预设标签配置（支持多语言）
 */
object FeedbackTags {
    // 😡 Hate/Dislike - 对应的问题标签
    val HATE_TAG_RES_IDS = listOf(
        R.string.feedback_tag_verse_translation_accuracy,  // Verse/Translation Accuracy
        R.string.feedback_tag_adhan_prayer_time_error,     // Adhan/Prayer Time Error
        R.string.feedback_tag_qibla_direction_inaccurate,  // Qibla Direction Inaccurate
        R.string.feedback_tag_login_privacy_concern,       // Login Issues / Privacy Concern
        R.string.feedback_tag_data_sync_failed,            // Data Sync Failed
        R.string.feedback_tag_inappropriate_ads,           // Inappropriate/Intrusive Ads
        R.string.feedback_tag_storage_space_usage,         // Storage/Space Usage
        R.string.feedback_tag_app_lag_slow_response,       // App Lag/Slow Response
        R.string.feedback_tag_search_results_irrelevant    // Search Results Irrelevant
    )
    
    // 😐 Neutral/Confused - 对应的问题标签（与Poor相同）
    val NEUTRAL_TAG_RES_IDS = listOf(
        R.string.feedback_tag_verse_translation_accuracy,  // Verse/Translation Accuracy
        R.string.feedback_tag_adhan_prayer_time_error,     // Adhan/Prayer Time Error
        R.string.feedback_tag_qibla_direction_inaccurate,  // Qibla Direction Inaccurate
        R.string.feedback_tag_login_privacy_concern,       // Login Issues / Privacy Concern
        R.string.feedback_tag_data_sync_failed,            // Data Sync Failed
        R.string.feedback_tag_inappropriate_ads,           // Inappropriate/Intrusive Ads
        R.string.feedback_tag_storage_space_usage,         // Storage/Space Usage
        R.string.feedback_tag_app_lag_slow_response,       // App Lag/Slow Response
        R.string.feedback_tag_search_results_irrelevant    // Search Results Irrelevant
    )
    
    // 😍 Love/Like - 对应的优点
    val LOVE_TAG_RES_IDS = listOf(
        R.string.feedback_tag_good_reading,    // 阅读体验好
        R.string.feedback_tag_clean_ui,        // 界面很干净
        R.string.feedback_tag_good_learning    // 学习功能好
    )
    
    fun getTagResIdsForEmotion(emotion: FeedbackEmotion): List<Int> {
        return when (emotion) {
            FeedbackEmotion.HATE -> HATE_TAG_RES_IDS
            FeedbackEmotion.NEUTRAL -> NEUTRAL_TAG_RES_IDS
            FeedbackEmotion.LOVE -> LOVE_TAG_RES_IDS
        }
    }
}

/**
 * Firestore 文档模型（用于序列化）
 */
data class FeedbackDocument(
    val emotion: String,
    val selectedTags: List<String>,
    val comment: String?,
    val deviceName: String,
    val systemVersion: String,
    val appVersion: String,
    val screenSize: String,
    val language: String,
    val currentPage: String,
    val readingProgress: String?,
    val sessionDuration: Long,
    val isFirstLaunch: Boolean,
    val timestamp: Any
) {
    companion object {
        fun fromFeedbackData(data: FeedbackData, context: android.content.Context): FeedbackDocument {
            return FeedbackDocument(
                emotion = context.getString(data.emotion.labelResId),
                selectedTags = data.selectedTags,
                comment = data.comment,
                deviceName = data.deviceInfo.deviceName,
                systemVersion = data.deviceInfo.systemVersion,
                appVersion = data.deviceInfo.appVersion,
                screenSize = data.deviceInfo.screenSize,
                language = data.deviceInfo.language,
                currentPage = data.appState.currentPage,
                readingProgress = data.appState.readingProgress,
                sessionDuration = data.appState.sessionDuration,
                isFirstLaunch = data.appState.isFirstLaunch,
                timestamp = data.timestamp
            )
        }
    }
}

