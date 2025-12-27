package com.quran.quranaudio.online.Utils

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * Streak Manager - 连续打卡天数统计
 * 
 * 功能：
 * - 记录用户每日打卡
 * - 计算连续天数
 * - 判断是否达到7天（触发账户升级提示）
 * - 保存到 Firestore: /users/{uid}/streakStats
 */
class StreakManager private constructor() {
    
    companion object {
        private const val TAG = "StreakManager"
        private const val COLLECTION_PATH = "users"
        private const val STREAK_SUBCOLLECTION = "streakStats"
        private const val DOCUMENT_ID = "current"
        
        // 触发升级提示的天数
        const val UPGRADE_PROMPT_DAYS = 7
        
        @Volatile
        private var instance: StreakManager? = null
        
        @JvmStatic
        fun getInstance(): StreakManager {
            return instance ?: synchronized(this) {
                instance ?: StreakManager().also { instance = it }
            }
        }
    }
    
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    /**
     * 记录今日打卡
     * 
     * @param context Context
     * @param callback 回调（返回当前连续天数）
     */
    suspend fun recordCheckIn(context: Context, callback: (currentStreak: Int, shouldPromptUpgrade: Boolean) -> Unit) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "⚠️ No user signed in, cannot record check-in")
                callback(0, false)
                return
            }
            
            Log.d(TAG, "📝 Recording check-in for user: $userId")
            
            val today = dateFormat.format(Date())
            val docRef = firestore.collection(COLLECTION_PATH)
                .document(userId)
                .collection(STREAK_SUBCOLLECTION)
                .document(DOCUMENT_ID)
            
            // 获取当前 streak 数据
            val snapshot = docRef.get().await()
            
            if (!snapshot.exists()) {
                // 首次打卡
                Log.d(TAG, "🎉 First check-in!")
                val initialData = hashMapOf(
                    "currentStreak" to 1,
                    "longestStreak" to 1,
                    "lastCheckInDate" to today,
                    "totalDays" to 1,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
                docRef.set(initialData).await()
                callback(1, false)
                return
            }
            
            // 读取现有数据
            val lastCheckInDate = snapshot.getString("lastCheckInDate") ?: ""
            val currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0
            val longestStreak = snapshot.getLong("longestStreak")?.toInt() ?: 0
            val totalDays = snapshot.getLong("totalDays")?.toInt() ?: 0
            
            Log.d(TAG, "📊 Current stats:")
            Log.d(TAG, "   → Last check-in: $lastCheckInDate")
            Log.d(TAG, "   → Current streak: $currentStreak")
            Log.d(TAG, "   → Longest streak: $longestStreak")
            Log.d(TAG, "   → Total days: $totalDays")
            
            // 检查是否今天已打卡
            if (lastCheckInDate == today) {
                Log.d(TAG, "✅ Already checked in today")
                val isAnonymous = auth.currentUser?.isAnonymous == true
                val shouldPrompt = isAnonymous && currentStreak >= UPGRADE_PROMPT_DAYS
                callback(currentStreak, shouldPrompt)
                return
            }
            
            // 检查是否昨天打卡（连续）
            val lastDate = dateFormat.parse(lastCheckInDate)
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }.time
            
            val isConsecutive = lastDate != null && dateFormat.format(lastDate) == dateFormat.format(yesterday)
            
            val newStreak = if (isConsecutive) {
                currentStreak + 1
            } else {
                1 // 重新开始
            }
            
            val newLongestStreak = maxOf(newStreak, longestStreak)
            
            Log.d(TAG, "📈 Updating streak:")
            Log.d(TAG, "   → Is consecutive: $isConsecutive")
            Log.d(TAG, "   → New streak: $newStreak")
            Log.d(TAG, "   → New longest: $newLongestStreak")
            
            // 更新数据
            val updateData = hashMapOf<String, Any>(
                "currentStreak" to newStreak,
                "longestStreak" to newLongestStreak,
                "lastCheckInDate" to today,
                "totalDays" to (totalDays + 1),
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            docRef.update(updateData).await()
            
            Log.d(TAG, "✅ Check-in recorded successfully")
            
            // 检查是否应该提示升级
            val isAnonymous = auth.currentUser?.isAnonymous == true
            val shouldPrompt = isAnonymous && newStreak >= UPGRADE_PROMPT_DAYS && newStreak < UPGRADE_PROMPT_DAYS + 3
            // 只在第7、8、9天提示，避免频繁打扰
            
            if (shouldPrompt) {
                Log.d(TAG, "🎉 User has $newStreak day streak and is anonymous - should prompt upgrade!")
            }
            
            callback(newStreak, shouldPrompt)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to record check-in", e)
            callback(0, false)
        }
    }
    
    /**
     * 获取当前连续天数
     */
    suspend fun getCurrentStreak(): Int {
        try {
            val userId = auth.currentUser?.uid ?: return 0
            
            val docRef = firestore.collection(COLLECTION_PATH)
                .document(userId)
                .collection(STREAK_SUBCOLLECTION)
                .document(DOCUMENT_ID)
            
            val snapshot = docRef.get().await()
            
            if (!snapshot.exists()) {
                return 0
            }
            
            return snapshot.getLong("currentStreak")?.toInt() ?: 0
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get current streak", e)
            return 0
        }
    }
    
    /**
     * 获取完整的 Streak 统计数据
     */
    suspend fun getStreakStats(): StreakStats {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return StreakStats()
            }
            
            val docRef = firestore.collection(COLLECTION_PATH)
                .document(userId)
                .collection(STREAK_SUBCOLLECTION)
                .document(DOCUMENT_ID)
            
            val snapshot = docRef.get().await()
            
            if (!snapshot.exists()) {
                return StreakStats()
            }
            
            return StreakStats(
                currentStreak = snapshot.getLong("currentStreak")?.toInt() ?: 0,
                longestStreak = snapshot.getLong("longestStreak")?.toInt() ?: 0,
                lastCheckInDate = snapshot.getString("lastCheckInDate") ?: "",
                totalDays = snapshot.getLong("totalDays")?.toInt() ?: 0
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to get streak stats", e)
            return StreakStats()
        }
    }
    
    /**
     * 检查是否应该显示升级提示
     */
    suspend fun shouldShowUpgradePrompt(): Boolean {
        val isAnonymous = auth.currentUser?.isAnonymous == true
        if (!isAnonymous) {
            return false
        }
        
        val streak = getCurrentStreak()
        return streak >= UPGRADE_PROMPT_DAYS
    }
}

/**
 * Streak 统计数据
 */
data class StreakStats(
    val currentStreak: Int = 0,        // 当前连续天数
    val longestStreak: Int = 0,        // 历史最长连续天数
    val lastCheckInDate: String = "",  // 最后打卡日期 (yyyy-MM-dd)
    val totalDays: Int = 0             // 总打卡天数
)

