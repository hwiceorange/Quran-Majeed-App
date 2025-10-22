package com.quran.quranaudio.online.quests.repository

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.quran.quranaudio.online.quests.constants.FirestoreConstants
import com.quran.quranaudio.online.quests.data.DailyProgressModel
import com.quran.quranaudio.online.quests.data.StreakStats
import com.quran.quranaudio.online.quests.data.UserQuestConfig
import com.quran.quranaudio.online.quests.data.UserLearningState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

/**
 * Repository for managing Daily Quests data in Firebase Firestore.
 * 
 * This repository handles:
 * - Learning plan configuration (CRUD operations)
 * - Daily progress tracking
 * - Streak statistics management
 * - Task completion with atomic transactions
 * - Cross-day streak reset detection
 */
class QuestRepository(
    private val firestore: FirebaseFirestore
) {
    
    companion object {
        private const val TAG = "QuestRepository"
    }
    
    // ============================================================
    // LEARNING PLAN CONFIGURATION
    // ============================================================
    
    /**
     * Saves the user's quest configuration to Firestore.
     * @param config The quest configuration to save
     * @throws Exception if the operation fails
     */
    suspend fun saveUserQuestConfig(config: UserQuestConfig) {
        try {
            val path = FirestoreConstants.getLearningPlanCollectionPath()
            val updatedConfig = config.copy(updatedAt = Timestamp.now())
            
            firestore.document("$path/config")
                .set(updatedConfig, SetOptions.merge())
                .await()
            
            Log.d(TAG, "Quest config saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save quest config", e)
            throw e
        }
    }
    
    /**
     * Observes the user's quest configuration in real-time.
     * @return Flow emitting UserQuestConfig updates (null if not exists or user not logged in)
     */
    fun observeUserQuestConfig(): Flow<UserQuestConfig?> = callbackFlow {
        try {
        val path = FirestoreConstants.getLearningPlanCollectionPath()
        val docRef = firestore.document("$path/config")
        
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing quest config", error)
                trySend(null)
                return@addSnapshotListener
            }
            
            val config = snapshot?.toObject(UserQuestConfig::class.java)
            trySend(config)
        }
        
        awaitClose { listener.remove() }
        } catch (e: IllegalStateException) {
            // User not logged in - send null and close the flow
            Log.w(TAG, "User not logged in, cannot observe config. Returning null.")
            trySend(null)
            close()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error setting up config observation", e)
            trySend(null)
            close()
        }
    }
    
    /**
     * Gets the user's quest configuration (one-time read).
     * @return UserQuestConfig or null if not exists
     */
    suspend fun getUserQuestConfig(): UserQuestConfig? {
        return try {
            val path = FirestoreConstants.getLearningPlanCollectionPath()
            val snapshot = firestore.document("$path/config").get().await()
            snapshot.toObject(UserQuestConfig::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get quest config", e)
            null
        }
    }
    
    /**
     * 同步获取用户配置（用于在非协程上下文中调用）
     * 注意：这会阻塞当前线程，只应在后台线程中调用
     */
    @JvmName("getUserQuestConfigSync")
    fun getUserQuestConfigSync(): UserQuestConfig? {
        return try {
            val path = FirestoreConstants.getLearningPlanCollectionPath()
            val task = firestore.document("$path/config").get()
            val snapshot = Tasks.await(task)
            snapshot.toObject(UserQuestConfig::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get quest config (sync)", e)
            null
        }
    }
    
    // ============================================================
    // DAILY PROGRESS TRACKING
    // ============================================================
    
    /**
     * Gets the daily progress for a specific date.
     * @param date The date to query (defaults to today)
     * @return DailyProgressModel or null if not exists
     */
    suspend fun getDailyProgress(date: LocalDate = LocalDate.now()): DailyProgressModel? {
        return try {
            val path = FirestoreConstants.getDailyProgressDocumentPath(date)
            val snapshot = firestore.document(path).get().await()
            snapshot.toObject(DailyProgressModel::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get daily progress for $date", e)
            null
        }
    }
    
    /**
     * Observes today's daily progress in real-time.
     * @return Flow emitting DailyProgressModel updates (null if not exists)
     */
    fun observeTodayProgress(): Flow<DailyProgressModel?> = callbackFlow {
        val path = FirestoreConstants.getDailyProgressDocumentPath()
        val docRef = firestore.document(path)
        
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing today's progress", error)
                trySend(null)
                return@addSnapshotListener
            }
            
            val progress = snapshot?.toObject(DailyProgressModel::class.java)
            trySend(progress)
        }
        
        awaitClose { listener.remove() }
    }
    
    // ============================================================
    // STREAK STATISTICS
    // ============================================================
    
    /**
     * Gets the user's streak statistics (one-time read).
     * @return StreakStats or default stats if not exists
     */
    suspend fun getStreakStats(): StreakStats {
        return try {
            val path = FirestoreConstants.getStreakStatsPath()
            val snapshot = firestore.document(path).get().await()
            snapshot.toObject(StreakStats::class.java) ?: StreakStats()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get streak stats", e)
            StreakStats()
        }
    }
    
    /**
     * Observes the user's streak statistics in real-time.
     * @return Flow emitting StreakStats updates
     */
    fun observeStreakStats(): Flow<StreakStats> = callbackFlow {
        val path = FirestoreConstants.getStreakStatsPath()
        val docRef = firestore.document(path)
        
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing streak stats", error)
                trySend(StreakStats())
                return@addSnapshotListener
            }
            
            val stats = snapshot?.toObject(StreakStats::class.java) ?: StreakStats()
            trySend(stats)
        }
        
        awaitClose { listener.remove() }
    }
    
    // ============================================================
    // TASK COMPLETION (ATOMIC TRANSACTION)
    // ============================================================
    
    /**
     * Simplified overload for Java access - automatically fetches user config.
     * 
     * @param taskId The task ID to mark as completed
     * @param isCompleted Must be true (we only process completions, not un-completions)
     */
    fun updateTaskCompletion(taskId: String, isCompleted: Boolean) {
        Log.d(TAG, "🔥 updateTaskCompletion() called - taskId: $taskId, isCompleted: $isCompleted")
        
        if (!isCompleted) {
            Log.w(TAG, "updateTaskCompletion called with isCompleted=false, ignoring")
            return
        }
        
        // Launch coroutine in IO dispatcher
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "🔥 Coroutine started - checking user authentication...")
                val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (userId == null) {
                    Log.w(TAG, "⚠️ User not authenticated, cannot update task completion")
                    return@launch
                }
                Log.d(TAG, "✅ User authenticated: $userId")
                
                // Fetch user's quest config
                Log.d(TAG, "🔥 Fetching user quest config...")
                val configPath = FirestoreConstants.getLearningPlanConfigPath()
                val configSnapshot = firestore.document(configPath).get().await()
                val config = configSnapshot.toObject(UserQuestConfig::class.java)
                
                if (config == null) {
                    Log.w(TAG, "⚠️ Quest config not found, cannot update task completion")
                    return@launch
                }
                Log.d(TAG, "✅ Quest config fetched successfully")
                
                // Call the suspend version with fetched config
                Log.d(TAG, "🔥 Calling suspend updateTaskCompletion()...")
                updateTaskCompletion(taskId, config, LocalDate.now())
                Log.d(TAG, "✅ Task $taskId marked as complete")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error updating task completion for $taskId", e)
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Marks a task as completed and updates streak statistics atomically.
     * 
     * This function performs a Firestore transaction to:
     * 1. Update the specific task's completion status
     * 2. Check if all enabled tasks are now completed
     * 3. If all tasks completed for the first time today:
     *    - Atomically increment currentStreak
     *    - Atomically increment monthlyProgress
     *    - Update lastCompletedDate
     *    - Update longestStreak if current > longest
     * 
     * @param taskId The task ID to mark as completed (use FirestoreConstants.TaskIds)
     * @param config The user's quest configuration (to determine enabled tasks)
     * @param date The date for the progress (defaults to today)
     * @throws Exception if the transaction fails
     */
    suspend fun updateTaskCompletion(
        taskId: String,
        config: UserQuestConfig,
        date: LocalDate = LocalDate.now()
    ) {
        try {
            val progressPath = FirestoreConstants.getDailyProgressDocumentPath(date)
            val streakPath = FirestoreConstants.getStreakStatsPath()
            
            val progressRef = firestore.document(progressPath)
            val streakRef = firestore.document(streakPath)
            
            firestore.runTransaction { transaction ->
                // 🔥 修复：所有读操作必须在写操作之前！
                
                // 1. Fetch current daily progress (READ)
                val progressSnapshot = transaction.get(progressRef)
                val currentProgress = progressSnapshot.toObject(DailyProgressModel::class.java)
                    ?: DailyProgressModel(date = date.toString())
                
                // 2. Mark the specific task as completed
                val updatedProgress = when (taskId) {
                    FirestoreConstants.TaskIds.TASK_1_READ -> 
                        currentProgress.copy(task1ReadCompleted = true)
                    FirestoreConstants.TaskIds.TASK_2_TAJWEED -> 
                        currentProgress.copy(task2TajweedCompleted = true)
                    FirestoreConstants.TaskIds.TASK_3_TASBIH -> 
                        currentProgress.copy(task3TasbihCompleted = true)
                    else -> {
                        Log.w(TAG, "Unknown taskId: $taskId")
                        currentProgress
                    }
                }
                
                // 3. Check if ALL enabled tasks are now completed
                val allTasksNowComplete = updatedProgress.areAllEnabledTasksCompleted(config)
                val wasAlreadyComplete = currentProgress.allTasksCompleted
                
                // 4. 🔥 如果需要更新streak，先读取 (READ)
                val streakSnapshot = if (allTasksNowComplete && !wasAlreadyComplete) {
                    transaction.get(streakRef)
                } else {
                    null
                }
                val currentStats = streakSnapshot?.toObject(StreakStats::class.java)
                    ?: StreakStats()
                
                // 5. 🔥 所有读操作完成后，开始写操作 (WRITE)
                
                // Update daily progress document
                val finalProgress = if (allTasksNowComplete && !wasAlreadyComplete) {
                    updatedProgress.copy(
                        allTasksCompleted = true,
                        completedAt = Timestamp.now()
                    )
                } else {
                    updatedProgress
                }
                
                transaction.set(progressRef, finalProgress, SetOptions.merge())
                
                // 6. If all tasks just completed (first time today), update streak stats (WRITE)
                if (allTasksNowComplete && !wasAlreadyComplete) {
                    
                    // Atomically increment streak and monthly progress
                    val newStreak = currentStats.currentStreak + 1
                    val newMonthlyProgress = currentStats.monthlyProgress + 1
                    val newLongestStreak = maxOf(newStreak, currentStats.longestStreak)
                    
                    val updatedStats = hashMapOf<String, Any>(
                        "currentStreak" to newStreak,
                        "longestStreak" to newLongestStreak,
                        "monthlyProgress" to newMonthlyProgress,
                        "totalDays" to FieldValue.increment(1),
                        "lastCompletedDate" to date.toString(),
                        "lastUpdatedAt" to Timestamp.now()
                    )
                    
                    transaction.set(streakRef, updatedStats, SetOptions.merge())
                    
                    Log.d(TAG, "✅ All tasks completed! Streak: $newStreak, Monthly: $newMonthlyProgress")
                } else {
                    Log.d(TAG, "Task $taskId marked complete. All tasks complete: $allTasksNowComplete")
                }
                
            }.await()
            
            Log.d(TAG, "Task completion transaction successful")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update task completion", e)
            throw e
        }
    }
    
    // ============================================================
    // STREAK RESET & CROSS-DAY DETECTION
    // ============================================================
    
    /**
     * Checks and resets streak if necessary based on yesterday's completion status.
     * 
     * This should be called when:
     * - App launches (MainActivity.onCreate or HomeViewModel.init)
     * - User returns to home screen (HomeFragment.onResume)
     * 
     * Logic:
     * 1. Check yesterday's dailyProgress document
     * 2. If yesterday's document exists but allTasksCompleted = false → Reset streak
     * 3. If lastCompletedDate is not yesterday and not today → Reset streak (user skipped a day)
     * 4. If today is the 1st of a new month → Reset monthly progress
     */
    suspend fun checkAndResetStreak() {
        try {
            val today = LocalDate.now()
            val yesterday = today.minusDays(1)
            
            val streakStats = getStreakStats()
            val lastCompletedDate = LocalDate.parse(streakStats.lastCompletedDate)
            
            // Check yesterday's progress
            val yesterdayProgress = getDailyProgress(yesterday)
            
            val isStreakBroken = when {
                // A) Yesterday's document exists but tasks not completed
                yesterdayProgress != null && !yesterdayProgress.allTasksCompleted -> {
                    Log.w(TAG, "Streak broken: Yesterday's tasks were not completed")
                    true
                }
                
                // B) Last completed date is not yesterday and not today (user skipped a day)
                lastCompletedDate != yesterday && lastCompletedDate != today -> {
                    Log.w(TAG, "Streak broken: User skipped a day (last: $lastCompletedDate, yesterday: $yesterday)")
                    true
                }
                
                else -> false
            }
            
            if (isStreakBroken) {
                resetStreak()
            }
            
            // Check if we need to reset monthly progress (new month)
            if (today.dayOfMonth == 1) {
                val newGoal = today.lengthOfMonth()
                resetMonthlyProgress(newGoal)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check and reset streak", e)
        }
    }
    
    /**
     * Resets the current streak to 0.
     */
    suspend fun resetStreak() {
        try {
            val path = FirestoreConstants.getStreakStatsPath()
            val updates = hashMapOf<String, Any>(
                "currentStreak" to 0,
                "lastUpdatedAt" to Timestamp.now()
            )
            
            firestore.document(path)
                .set(updates, SetOptions.merge())
                .await()
            
            Log.d(TAG, "Streak reset to 0")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset streak", e)
            throw e
        }
    }
    
    /**
     * Resets the monthly progress at the beginning of a new month.
     * @param newGoal The number of days in the new month
     */
    suspend fun resetMonthlyProgress(newGoal: Int) {
        try {
            val path = FirestoreConstants.getStreakStatsPath()
            val updates = hashMapOf<String, Any>(
                "monthlyProgress" to 0,
                "monthlyGoal" to newGoal,
                "lastUpdatedAt" to Timestamp.now()
            )
            
            firestore.document(path)
                .set(updates, SetOptions.merge())
                .await()
            
            Log.d(TAG, "Monthly progress reset. New goal: $newGoal days")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reset monthly progress", e)
            throw e
        }
    }
    
    /**
     * Initializes default streak stats document if it doesn't exist.
     * This should be called after the user creates their first learning plan.
     */
    suspend fun initializeStreakStats() {
        try {
            val path = FirestoreConstants.getStreakStatsPath()
            val docRef = firestore.document(path)
            
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val initialStats = StreakStats()
                docRef.set(initialStats).await()
                Log.d(TAG, "Streak stats initialized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize streak stats", e)
        }
    }
    
    /**
     * 保存用户学习状态（上次阅读位置）到 Firestore
     * 
     * @param surah 当前 Surah 编号 (1-114)
     * @param ayah 当前 Ayah 编号
     * @param page 当前 Mushaf 页码 (1-604)
     * @param juz 当前 Juz' 编号 (1-30)
     */
    suspend fun saveUserLearningState(
        surah: Int,
        ayah: Int,
        page: Int = 1,
        juz: Int = 1
    ) {
        try {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "User not authenticated, cannot save learning state")
                return
            }
            
            val learningState = hashMapOf(
                "lastReadSurah" to surah,
                "lastReadAyah" to ayah,
                "lastReadPage" to page,
                "lastReadJuz" to juz,
                "lastReadTimestamp" to com.google.firebase.Timestamp.now(),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore
                .collection("users")
                .document(userId)
                .collection("learningState")
                .document("current")
                .set(learningState, SetOptions.merge())
                .await()
            
            Log.d(TAG, "Learning state saved: Surah $surah, Ayah $ayah, Page $page, Juz $juz")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save learning state", e)
            throw e
        }
    }
    
    /**
     * 更新 Quran Listening 任务进度（收听时长）
     * 
     * @param minutesListened 已收听分钟数
     */
    suspend fun updateListeningProgress(minutesListened: Int) {
        try {
            val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                Log.w(TAG, "User not authenticated, cannot update listening progress")
                return
            }
            
            val today = LocalDate.now().toString()
            val updates = hashMapOf<String, Any>(
                "task2ListeningMinutes" to com.google.firebase.firestore.FieldValue.increment(minutesListened.toLong()),
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            firestore
                .collection("users")
                .document(userId)
                .collection("dailyProgress")
                .document(today)
                .set(updates, SetOptions.merge())
                .await()
            
            Log.d(TAG, "Listening progress updated: +$minutesListened minutes")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update listening progress", e)
            throw e
        }
    }
    
    // ============================================================
    // USER LEARNING STATE (阅读位置追踪)
    // ============================================================
    
    /**
     * 保存用户的阅读位置状态
     * 
     * @param state 当前阅读状态
     */
    suspend fun saveLearningState(state: UserLearningState) {
        try {
            val path = FirestoreConstants.getLearningPlanCollectionPath()
            val docRef = firestore.document("$path/${UserLearningState.COLLECTION_PATH}/${UserLearningState.DOCUMENT_ID}")
            
            docRef.set(state, SetOptions.merge()).await()
            Log.d(TAG, "Learning state saved: Surah ${state.currentSurah}:${state.currentAyah}, Page ${state.currentPage}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save learning state", e)
            throw e
        }
    }
    
    /**
     * 获取用户的阅读位置状态
     * 
     * @return UserLearningState 如果存在，否则返回默认状态（从第1页开始）
     */
    suspend fun getLearningState(): UserLearningState {
        return try {
            val path = FirestoreConstants.getLearningPlanCollectionPath()
            val docRef = firestore.document("$path/${UserLearningState.COLLECTION_PATH}/${UserLearningState.DOCUMENT_ID}")
            
            val snapshot = docRef.get().await()
            snapshot.toObject(UserLearningState::class.java) ?: UserLearningState()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get learning state, returning default", e)
            UserLearningState() // 返回默认状态（从第1页开始）
        }
    }
    
    /**
     * 同步获取用户的阅读位置（用于Java调用）
     */
    @JvmName("getLearningStateSync")
    fun getLearningStateSync(): UserLearningState {
        return try {
            val path = FirestoreConstants.getLearningPlanCollectionPath()
            val docRef = firestore.document("$path/${UserLearningState.COLLECTION_PATH}/${UserLearningState.DOCUMENT_ID}")
            
            val task = docRef.get()
            val snapshot = Tasks.await(task)
            snapshot.toObject(UserLearningState::class.java) ?: UserLearningState()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get learning state (sync), returning default", e)
            UserLearningState()
        }
    }
}


