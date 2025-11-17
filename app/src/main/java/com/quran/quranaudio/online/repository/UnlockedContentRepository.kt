package com.quran.quranaudio.online.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.quran.quranaudio.online.model.UnlockedContent
import kotlinx.coroutines.tasks.await

/**
 * 经文注释解锁状态管理仓库
 * 
 * 负责管理用户通过广告或订阅解锁的经文注释内容
 * 数据存储在 Firestore: /users/{userId}/unlocked_content
 */
class UnlockedContentRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    
    companion object {
        private const val TAG = "UnlockedContentRepo"
        private const val COLLECTION_USERS = "users"
        
        @Volatile
        private var INSTANCE: UnlockedContentRepository? = null
        
        fun getInstance(): UnlockedContentRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UnlockedContentRepository().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 获取用户的unlocked_content集合引用
     * 路径: /users/{userId}/unlocked_content
     */
    private fun getUserUnlockedContentCollection() = firestore
        .collection(COLLECTION_USERS)
        .document(auth.currentUser?.uid ?: "")
        .collection(UnlockedContent.COLLECTION_NAME)
    
    /**
     * 检查指定经文是否已解锁
     * 
     * @param surahId 章节ID
     * @param ayahId 经文ID
     * @return true 如果已解锁，false 如果未解锁
     */
    suspend fun isContentUnlocked(surahId: Int, ayahId: Int): Boolean {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "User not authenticated")
            return false
        }
        
        return try {
            val contentId = UnlockedContent.generateContentId(surahId, ayahId)
            
            val snapshot = getUserUnlockedContentCollection()
                .whereEqualTo("contentId", contentId)
                .limit(1)
                .get()
                .await()
            
            val isUnlocked = !snapshot.isEmpty
            Log.d(TAG, "Content $contentId unlock status: $isUnlocked")
            isUnlocked
        } catch (e: Exception) {
            Log.e(TAG, "Error checking unlock status", e)
            false
        }
    }
    
    /**
     * 解锁指定经文
     * 
     * @param surahId 章节ID
     * @param ayahId 经文ID
     * @param unlockedBy 解锁方式
     * @return true 如果解锁成功，false 如果失败
     */
    suspend fun unlockContent(
        surahId: Int,
        ayahId: Int,
        unlockedBy: UnlockedContent.UnlockMethod
    ): Boolean {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "❌ Cannot unlock content: User not authenticated")
            return false
        }
        
        Log.d(TAG, "🔐 Attempting to unlock content:")
        Log.d(TAG, "  - userId: $userId")
        Log.d(TAG, "  - surahId: $surahId")
        Log.d(TAG, "  - ayahId: $ayahId")
        Log.d(TAG, "  - unlockedBy: $unlockedBy")
        
        return try {
            val contentId = UnlockedContent.generateContentId(surahId, ayahId)
            Log.d(TAG, "  - contentId: $contentId")
            
            // 检查是否已解锁
            val alreadyUnlocked = isContentUnlocked(surahId, ayahId)
            if (alreadyUnlocked) {
                Log.d(TAG, "✅ Content $contentId already unlocked, returning true")
                return true
            }
            
            Log.d(TAG, "📝 Creating new unlock record...")
            
            // 创建解锁记录
            val unlockedContent = UnlockedContent.create(
                contentId = contentId,
                unlockedBy = unlockedBy
            )
            
            Log.d(TAG, "💾 Saving to Firestore: $unlockedContent")
            
            // 保存到Firestore
            val docRef = getUserUnlockedContentCollection()
                .add(unlockedContent)
                .await()
            
            Log.d(TAG, "✅ Successfully saved to Firestore with ID: ${docRef.id}")
            Log.d(TAG, "✅ Content $contentId unlocked by $unlockedBy")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error unlocking content", e)
            Log.e(TAG, "  Exception type: ${e.javaClass.simpleName}")
            Log.e(TAG, "  Exception message: ${e.message}")
            Log.e(TAG, "  Stack trace: ${e.stackTraceToString()}")
            false
        }
    }
    
    /**
     * 批量解锁内容（用于订阅用户）
     * 注意：订阅用户通常不需要在Firestore中记录每个经文的解锁状态
     * 直接通过订阅状态判断即可
     */
    suspend fun unlockAllContent(): Boolean {
        // 对于订阅用户，我们不需要在Firestore中记录所有经文
        // 只需要在应用逻辑中检查订阅状态即可
        Log.d(TAG, "User subscribed, all content unlocked")
        return true
    }
    
    /**
     * 获取用户所有已解锁的内容ID列表
     * 
     * @return 已解锁的内容ID列表
     */
    suspend fun getAllUnlockedContentIds(): List<String> {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.w(TAG, "User not authenticated")
            return emptyList()
        }
        
        return try {
            val snapshot = getUserUnlockedContentCollection()
                .get()
                .await()
            
            val contentIds = snapshot.documents.mapNotNull { doc ->
                doc.toObject(UnlockedContent::class.java)?.contentId
            }
            
            Log.d(TAG, "Retrieved ${contentIds.size} unlocked content IDs")
            contentIds
        } catch (e: Exception) {
            Log.e(TAG, "Error retrieving unlocked content", e)
            emptyList()
        }
    }
    
    /**
     * 清除用户的所有解锁记录（用于测试或重置）
     */
    suspend fun clearAllUnlockedContent(): Boolean {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e(TAG, "Cannot clear content: User not authenticated")
            return false
        }
        
        return try {
            val snapshot = getUserUnlockedContentCollection()
                .get()
                .await()
            
            snapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            
            Log.d(TAG, "Cleared ${snapshot.size()} unlocked content records")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing unlocked content", e)
            false
        }
    }
}

