package com.quran.quranaudio.online.prayertimes.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * 祷告记录数据模型
 * 用于记录用户的祷告完成情况
 * 
 * @property id Firestore 文档 ID（自动生成）
 * @property userId 用户 ID
 * @property prayerName 祷告名称（Fajr, Dhuhr, Asr, Maghrib, Isha）
 * @property status 祷告状态（Ada', Qada', Missed）
 * @property performedAt 实际祷告时间（用户可修改）
 * @property loggedAt 记录时间（系统自动生成）
 * @property notes 备注（可选，最多100字符）
 * @property date 祷告日期（YYYY-MM-DD 格式）
 */
data class PrayerLog(
    @DocumentId
    val id: String = "",
    
    val userId: String = "",
    
    val prayerName: String = "",
    
    val status: PrayerStatus = PrayerStatus.ADA,
    
    val performedAt: Timestamp? = null,
    
    @ServerTimestamp
    val loggedAt: Timestamp? = null,
    
    val notes: String = "",
    
    val date: String = "", // YYYY-MM-DD (originalDate)
    
    val isToday: Boolean = false, // 标记是否为当日礼拜
    
    val tags: List<String> = emptyList() // 场景标签 (At Mosque, Traveling, etc.)
) {
    /**
     * 祷告状态枚举
     */
    enum class PrayerStatus(val displayName: String, val arabicName: String) {
        ADA("Ada'", "أداء"),        // 已完成，准时
        QADA("Qada'", "قضاء"),      // 弥补，延迟完成
        MISSED("Missed", "فائت")    // 错过，未完成
    }
    
    companion object {
        // Firestore collection 名称
        const val COLLECTION_NAME = "prayer_logs"
        
        // 常用标签
        val COMMON_TAGS = listOf(
            "At Mosque",
            "Traveling",
            "With Family",
            "At Work",
            "Sick",
            "Late"
        )
        
        /**
         * 创建新的祷告记录
         */
        fun create(
            userId: String,
            prayerName: String,
            status: PrayerStatus,
            performedAt: Timestamp,
            notes: String,
            date: String,
            isToday: Boolean = false,
            tags: List<String> = emptyList()
        ): PrayerLog {
            return PrayerLog(
                userId = userId,
                prayerName = prayerName,
                status = status,
                performedAt = performedAt,
                notes = notes.take(100), // 限制100字符
                date = date,
                isToday = isToday,
                tags = tags
            )
        }
    }
}

