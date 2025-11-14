package com.quran.quranaudio.online.prayertimes.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

/**
 * Qada' 追溯配置数据模型
 * 存储路径: users/{userId}/qadaConfig/settings
 * 
 * 用于定义用户的 Qada' 统计起始日期
 * 
 * @property userId 用户 ID
 * @property qadaStartDate Qada' 追溯起始日期 (YYYY-MM-DD 格式)
 * @property createdAt 配置创建时间
 * @property updatedAt 配置更新时间
 */
data class QadaConfig(
    @PropertyName("userId")
    val userId: String = "",
    
    @PropertyName("qadaStartDate")
    val qadaStartDate: String = "", // YYYY-MM-DD
    
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("updatedAt")
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    companion object {
        // Firestore collection 路径
        const val COLLECTION_PATH = "qadaConfig"
        const val DOCUMENT_ID = "settings"
        
        /**
         * 创建默认配置（起始日期为今天）
         */
        fun createDefault(userId: String, startDate: String): QadaConfig {
            return QadaConfig(
                userId = userId,
                qadaStartDate = startDate,
                createdAt = Timestamp.now()
            )
        }
    }
}





