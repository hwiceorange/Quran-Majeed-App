package com.quran.quranaudio.online.quests.constants

import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Firestore path constants for Daily Quests feature.
 * All paths are scoped to the authenticated user.
 */
object FirestoreConstants {
    
    // Root collection
    private const val USERS_COLLECTION = "users"
    
    // Learning Plan paths
    private const val LEARNING_PLAN_COLLECTION = "learningPlan"
    private const val CONFIG_DOCUMENT = "config"
    
    // Daily Progress paths
    private const val DAILY_PROGRESS_COLLECTION = "dailyProgress"
    
    // Streak Stats paths
    private const val STREAK_STATS_COLLECTION = "streakStats"
    private const val SUMMARY_DOCUMENT = "summary"
    
    /**
     * Gets the current authenticated user's ID.
     * @throws IllegalStateException if user is not logged in
     */
    private fun getUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid
            ?: throw IllegalStateException("User not logged in. Firebase operations require authentication.")
    }
    
    /**
     * Returns the Firestore path for the user's learning plan configuration.
     * Path: users/{userId}/learningPlan/config
     */
    fun getLearningPlanConfigPath(): String {
        val userId = getUserId()
        return "$USERS_COLLECTION/$userId/$LEARNING_PLAN_COLLECTION/$CONFIG_DOCUMENT"
    }
    
    /**
     * Returns the Firestore path for the user's learning plan collection.
     * Path: users/{userId}/learningPlan
     */
    fun getLearningPlanCollectionPath(): String {
        val userId = getUserId()
        return "$USERS_COLLECTION/$userId/$LEARNING_PLAN_COLLECTION"
    }
    
    /**
     * Returns the Firestore path for the user's daily progress collection.
     * Path: users/{userId}/dailyProgress
     */
    fun getDailyProgressCollectionPath(): String {
        val userId = getUserId()
        return "$USERS_COLLECTION/$userId/$DAILY_PROGRESS_COLLECTION"
    }
    
    /**
     * Returns the Firestore path for a specific date's progress document.
     * Path: users/{userId}/dailyProgress/{YYYY-MM-DD}
     * @param date The date for the progress document (defaults to today)
     */
    fun getDailyProgressDocumentPath(date: LocalDate = LocalDate.now()): String {
        val userId = getUserId()
        val dateId = getDateId(date)
        return "$USERS_COLLECTION/$userId/$DAILY_PROGRESS_COLLECTION/$dateId"
    }
    
    /**
     * Returns the Firestore path for the user's streak statistics.
     * Path: users/{userId}/streakStats/summary
     */
    fun getStreakStatsPath(): String {
        val userId = getUserId()
        return "$USERS_COLLECTION/$userId/$STREAK_STATS_COLLECTION/$SUMMARY_DOCUMENT"
    }
    
    /**
     * Returns the Firestore path for the streak stats collection.
     * Path: users/{userId}/streakStats
     */
    fun getStreakStatsCollectionPath(): String {
        val userId = getUserId()
        return "$USERS_COLLECTION/$userId/$STREAK_STATS_COLLECTION"
    }
    
    /**
     * Converts a LocalDate to Firestore document ID format (YYYY-MM-DD).
     * @param date The date to convert (defaults to today)
     * @return Date string in YYYY-MM-DD format
     */
    fun getDateId(date: LocalDate = LocalDate.now()): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
    
    /**
     * Task IDs for daily quests
     */
    object TaskIds {
        const val TASK_1_READ = "task1ReadCompleted"
        const val TASK_2_TAJWEED = "task2TajweedCompleted"
        const val TASK_3_TASBIH = "task3TasbihCompleted"
    }
}











