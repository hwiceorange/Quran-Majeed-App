package com.quran.quranaudio.online.feedback

import com.google.firebase.Timestamp

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
enum class FeedbackEmotion(val emoji: String, val label: String) {
    LOVE("😍", "好用"),
    NEUTRAL("😐", "一般"),
    HATE("😡", "难用")
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
 * 预设标签配置
 */
object FeedbackTags {
    // 😡 难用 - 对应的问题标签
    val HATE_TAGS = listOf(
        "找不到书签",
        "音频断续",
        "字体太小",
        "搜索不准",
        "广告太多",
        "加载太慢"
    )
    
    // 😐 一般 - 对应的改进建议
    val NEUTRAL_TAGS = listOf(
        "功能太少",
        "操作复杂",
        "没发现新功能",
        "界面不美观",
        "缺少提醒"
    )
    
    // 😍 好用 - 对应的优点
    val LOVE_TAGS = listOf(
        "内容丰富",
        "操作流畅",
        "功能实用",
        "设计美观",
        "音频清晰"
    )
    
    fun getTagsForEmotion(emotion: FeedbackEmotion): List<String> {
        return when (emotion) {
            FeedbackEmotion.HATE -> HATE_TAGS
            FeedbackEmotion.NEUTRAL -> NEUTRAL_TAGS
            FeedbackEmotion.LOVE -> LOVE_TAGS
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
        fun fromFeedbackData(data: FeedbackData): FeedbackDocument {
            return FeedbackDocument(
                emotion = data.emotion.label,
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

