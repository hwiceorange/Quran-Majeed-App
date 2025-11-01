package com.quran.quranaudio.online.quran_module.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Last Read Record - 记录用户上次阅读位置
 * 
 * Firestore path: users/{userId}/learningState/current
 * 
 * 区分三种阅读模式：
 * - Surah 模式：lastReadSurah + lastReadAyah
 * - Juz 模式：lastReadJuz
 * - Page 模式：通过 Surah/Ayah 计算得出
 */
data class LastReadRecord(
    /** Last read Surah number (1-114) */
    @PropertyName("lastReadSurah")
    val lastReadSurah: Int = 0,
    
    /** Last read Ayah number within the Surah */
    @PropertyName("lastReadAyah")
    val lastReadAyah: Int = 0,
    
    /** Last read Juz number (1-30) */
    @PropertyName("lastReadJuz")
    val lastReadJuz: Int = 0,
    
    /** Last reading mode: "SURAH", "JUZ", or "VERSES" */
    @PropertyName("lastReadMode")
    val lastReadMode: String = "",
    
    /** Last reading timestamp */
    @PropertyName("lastReadTimestamp")
    val lastReadTimestamp: Timestamp? = null,
    
    /** Listening position (separate from reading) */
    @PropertyName("lastListenSurah")
    val lastListenSurah: Int = 0,
    
    @PropertyName("lastListenAyah")
    val lastListenAyah: Int = 0
) {
    companion object {
        const val COLLECTION_PATH = "learningState"
        const val DOCUMENT_ID = "current"
        
        // Reading mode constants
        const val MODE_SURAH = "SURAH"
        const val MODE_JUZ = "JUZ"
        const val MODE_VERSES = "VERSES"
    }
    
    /**
     * 判断是否有有效的阅读记录
     */
    fun hasValidRecord(): Boolean {
        return lastReadSurah > 0 && lastReadAyah > 0
    }
    
    /**
     * 获取阅读模式标识（向后兼容：如果没有 lastReadMode，则从 lastReadJuz 推断）
     */
    fun getReadingMode(): String {
        return when {
            lastReadMode.isNotEmpty() -> lastReadMode
            lastReadJuz > 0 -> MODE_JUZ
            lastReadSurah > 0 -> MODE_SURAH
            else -> MODE_SURAH // 默认
        }
    }
}

