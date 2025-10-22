package com.quran.quranaudio.online.quests.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
import java.time.LocalDate

/**
 * Learning Plan Configuration Model.
 * Stored at: users/{userId}/learningPlan/config
 * 
 * Supports multiple reading goal units:
 * - PAGES: Mushaf pages (1-50 per day)
 * - VERSES: Ayahs (1-100 per day)
 * - JUZ: Juz' parts (1-30 per day)
 */
data class UserQuestConfig(
    /** Legacy field for backward compatibility */
    @PropertyName("dailyReadingPages")
    val dailyReadingPages: Int = 10,
    
    /** Reading goal unit: PAGES, VERSES, or JUZ */
    @PropertyName("readingGoalUnit")
    val readingGoalUnit: String = "VERSES",
    
    /** Daily reading goal value (pages, verses, or juz depending on unit) */
    @PropertyName("dailyReadingGoal")
    val dailyReadingGoal: Int = 10,
    
    @PropertyName("recitationEnabled")
    val recitationEnabled: Boolean = true,
    
    @PropertyName("recitationMinutes")
    val recitationMinutes: Int = 15,
    
    @PropertyName("duaReminderEnabled")
    val duaReminderEnabled: Boolean = false,
    
    @PropertyName("tasbihReminderEnabled")
    val tasbihReminderEnabled: Boolean = true,
    
    @PropertyName("tasbihCount")
    val tasbihCount: Int = 50,
    
    @PropertyName("totalChallengeDays")
    val totalChallengeDays: Int = 0,
    
    @PropertyName("startDate")
    val startDate: String = LocalDate.now().toString(),
    
    @PropertyName("createdAt")
    val createdAt: Timestamp = Timestamp.now(),
    
    @PropertyName("updatedAt")
    val updatedAt: Timestamp = Timestamp.now()
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this(
        dailyReadingPages = 10,
        readingGoalUnit = "VERSES",
        dailyReadingGoal = 10,
        recitationEnabled = true,
        recitationMinutes = 15,
        duaReminderEnabled = false,
        tasbihReminderEnabled = true,
        tasbihCount = 50,
        totalChallengeDays = 0,
        startDate = LocalDate.now().toString(),
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )
    
    /**
     * Get the reading goal unit as an enum.
     */
    fun getReadingUnitEnum(): ReadingGoalUnit {
        return ReadingGoalUnit.fromString(readingGoalUnit)
    }
}

/**
 * Daily Progress Model.
 * Stored at: users/{userId}/dailyProgress/{YYYY-MM-DD}
 */
data class DailyProgressModel(
    @PropertyName("task1ReadCompleted")
    val task1ReadCompleted: Boolean = false,
    
    @PropertyName("task2TajweedCompleted")
    val task2TajweedCompleted: Boolean = false,
    
    @PropertyName("task3TasbihCompleted")
    val task3TasbihCompleted: Boolean = false,
    
    @PropertyName("allTasksCompleted")
    val allTasksCompleted: Boolean = false,
    
    @PropertyName("completedAt")
    val completedAt: Timestamp? = null,
    
    @PropertyName("date")
    val date: String = LocalDate.now().toString()
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this(
        task1ReadCompleted = false,
        task2TajweedCompleted = false,
        task3TasbihCompleted = false,
        allTasksCompleted = false,
        completedAt = null,
        date = LocalDate.now().toString()
    )
    
    /**
     * Checks if all enabled tasks are completed based on the user's configuration.
     * @param config The user's quest configuration
     * @return true if all enabled tasks are completed
     */
    fun areAllEnabledTasksCompleted(config: UserQuestConfig): Boolean {
        // Task 1 and Task 2 are always enabled
        val task1And2Complete = task1ReadCompleted && task2TajweedCompleted
        
        // Task 3 is optional (depends on tasbihReminderEnabled)
        val task3Complete = if (config.tasbihReminderEnabled) {
            task3TasbihCompleted
        } else {
            true // If not enabled, consider it "completed"
        }
        
        return task1And2Complete && task3Complete
    }
}

/**
 * Streak Statistics Model.
 * Stored at: users/{userId}/streakStats/summary
 */
data class StreakStats(
    @PropertyName("currentStreak")
    val currentStreak: Int = 0,
    
    @PropertyName("longestStreak")
    val longestStreak: Int = 0,
    
    @PropertyName("totalDays")
    val totalDays: Int = 0,
    
    @PropertyName("lastCompletedDate")
    val lastCompletedDate: String = LocalDate.now().toString(),
    
    @PropertyName("monthlyGoal")
    val monthlyGoal: Int = LocalDate.now().lengthOfMonth(),
    
    @PropertyName("monthlyProgress")
    val monthlyProgress: Int = 0,
    
    @PropertyName("lastUpdatedAt")
    val lastUpdatedAt: Timestamp = Timestamp.now()
) {
    // No-argument constructor required for Firestore deserialization
    constructor() : this(
        currentStreak = 0,
        longestStreak = 0,
        totalDays = 0,
        lastCompletedDate = LocalDate.now().toString(),
        monthlyGoal = LocalDate.now().lengthOfMonth(),
        monthlyProgress = 0,
        lastUpdatedAt = Timestamp.now()
    )
}

/**
 * UI model for displaying quest tasks on the home screen.
 */
data class QuestTask(
    val taskId: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean,
    val targetClass: Class<*>? = null
)





