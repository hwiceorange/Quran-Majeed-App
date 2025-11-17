package com.quran.quranaudio.online.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * 经文注释解锁记录
 * 
 * 用于记录用户通过观看广告或订阅解锁的经文注释
 * 存储路径: /users/{userId}/unlocked_content/{docId}
 * 
 * @property id Firestore文档ID
 * @property contentId 内容ID（格式: "surah_id:ayah_id"，如 "1:6"）
 * @property unlockedBy 解锁方式 ("rewarded_ad" 或 "subscription")
 * @property timestamp 解锁时间
 */
data class UnlockedContent(
    @DocumentId
    val id: String = "",
    
    val contentId: String = "",  // Format: "surah_id:ayah_id" (e.g., "1:6")
    
    val unlockedBy: UnlockMethod = UnlockMethod.REWARDED_AD,
    
    val timestamp: Timestamp = Timestamp.now()
) {
    /**
     * 解锁方式枚举
     */
    enum class UnlockMethod {
        REWARDED_AD,      // 观看激励广告解锁
        SUBSCRIPTION      // 订阅解锁
    }
    
    companion object {
        const val COLLECTION_NAME = "unlocked_content"
        
        /**
         * 生成内容ID
         * @param surahId 章节ID
         * @param ayahId 经文ID
         * @return 格式化的内容ID（如 "1:6"）
         */
        fun generateContentId(surahId: Int, ayahId: Int): String {
            return "$surahId:$ayahId"
        }
        
        /**
         * 解析内容ID
         * @param contentId 格式化的内容ID（如 "1:6"）
         * @return Pair<surahId, ayahId>
         */
        fun parseContentId(contentId: String): Pair<Int, Int>? {
            return try {
                val parts = contentId.split(":")
                if (parts.size == 2) {
                    Pair(parts[0].toInt(), parts[1].toInt())
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
        
        /**
         * 创建新的解锁记录
         */
        fun create(
            contentId: String,
            unlockedBy: UnlockMethod
        ): UnlockedContent {
            return UnlockedContent(
                contentId = contentId,
                unlockedBy = unlockedBy,
                timestamp = Timestamp.now()
            )
        }
    }
}

