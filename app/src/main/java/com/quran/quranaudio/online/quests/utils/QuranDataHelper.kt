package com.quran.quranaudio.online.quests.utils

import com.quran.quranaudio.online.quests.data.ReadingGoalUnit
import kotlin.math.ceil

/**
 * Helper class for Quran data calculations and conversions.
 * 
 * Provides utilities for:
 * - Converting between different reading units (Pages, Verses, Juz)
 * - Calculating challenge duration based on reading goals
 * - Getting Quran metadata
 */
object QuranDataHelper {
    
    /** Total number of verses in Quran */
    const val TOTAL_VERSES = 6236
    
    /** Total number of pages in Mushaf */
    const val TOTAL_PAGES = 604
    
    /** Total number of Juz' in Quran */
    const val TOTAL_JUZ = 30
    
    /** Maximum challenge duration (3 years) */
    const val MAX_CHALLENGE_DAYS = 1095
    
    /** Average verses per page (approximate) */
    private const val AVG_VERSES_PER_PAGE = TOTAL_VERSES / TOTAL_PAGES // ~10.3
    
    /** Average pages per Juz' */
    private const val AVG_PAGES_PER_JUZ = TOTAL_PAGES / TOTAL_JUZ // ~20.1
    
    /** Average verses per Juz' */
    private const val AVG_VERSES_PER_JUZ = TOTAL_VERSES / TOTAL_JUZ // ~207.8
    
    /**
     * Range configurations for different reading units.
     */
    data class UnitRange(
        val min: Int,
        val max: Int,
        val defaultValue: Int,
        val totalQuantity: Int,
        val displaySuffix: String
    )
    
    fun getRangeForUnit(unit: ReadingGoalUnit): UnitRange {
        return when (unit) {
            ReadingGoalUnit.PAGES -> UnitRange(
                min = 1,
                max = 50,
                defaultValue = 10,
                totalQuantity = TOTAL_PAGES,
                displaySuffix = "pages"
            )
            ReadingGoalUnit.VERSES -> UnitRange(
                min = 1,
                max = 100,
                defaultValue = 10,
                totalQuantity = TOTAL_VERSES,
                displaySuffix = "verses"
            )
            ReadingGoalUnit.JUZ -> UnitRange(
                min = 1,
                max = TOTAL_JUZ,
                defaultValue = 1,
                totalQuantity = TOTAL_JUZ,
                displaySuffix = "juz'"
            )
        }
    }
    
    /**
     * Calculate challenge duration based on daily reading goal.
     * 
     * @param unit The reading goal unit (PAGES, VERSES, or JUZ)
     * @param dailyGoal How many units per day (e.g., 10 pages, 20 verses, 1 juz)
     * @return Estimated days to complete Quran, capped at MAX_CHALLENGE_DAYS
     */
    fun calculateChallengeDays(unit: ReadingGoalUnit, dailyGoal: Int): Int {
        val safeGoal = dailyGoal.coerceAtLeast(1)
        
        val totalQuantity = when (unit) {
            ReadingGoalUnit.PAGES -> TOTAL_PAGES
            ReadingGoalUnit.VERSES -> TOTAL_VERSES
            ReadingGoalUnit.JUZ -> TOTAL_JUZ
        }
        
        // Ceiling division: total / dailyGoal (rounded up)
        val days = ceil(totalQuantity.toDouble() / safeGoal).toInt()
        
        // Cap at maximum (3 years)
        return days.coerceAtMost(MAX_CHALLENGE_DAYS)
    }
    
    /**
     * Convert reading goal from one unit to another (approximate).
     * Used for UI synchronization when switching units.
     * 
     * @param value The value in the source unit
     * @param fromUnit The source unit
     * @param toUnit The target unit
     * @return Approximate equivalent value in the target unit
     */
    fun convertUnit(value: Int, fromUnit: ReadingGoalUnit, toUnit: ReadingGoalUnit): Int {
        if (fromUnit == toUnit) return value
        
        // First convert to pages as intermediate unit
        val pages = when (fromUnit) {
            ReadingGoalUnit.PAGES -> value
            ReadingGoalUnit.VERSES -> value / AVG_VERSES_PER_PAGE
            ReadingGoalUnit.JUZ -> value * AVG_PAGES_PER_JUZ
        }
        
        // Then convert from pages to target unit
        val result = when (toUnit) {
            ReadingGoalUnit.PAGES -> pages
            ReadingGoalUnit.VERSES -> pages * AVG_VERSES_PER_PAGE
            ReadingGoalUnit.JUZ -> pages / AVG_PAGES_PER_JUZ
        }
        
        // Ensure result is within valid range
        val range = getRangeForUnit(toUnit)
        return result.coerceIn(range.min, range.max)
    }
    
    /**
     * Get the description text for the reading goal.
     * Used to update Today's Quests task description.
     * 
     * @param unit The reading goal unit
     * @param goal The daily reading goal value
     * @return Formatted description (e.g., "Read 10 pages", "Read 20 verses", "Read 1 juz'")
     */
    fun getReadingDescription(unit: ReadingGoalUnit, goal: Int): String {
        return when (unit) {
            ReadingGoalUnit.PAGES -> "Read $goal ${if (goal == 1) "page" else "pages"}"
            ReadingGoalUnit.VERSES -> "Read $goal ${if (goal == 1) "verse" else "verses"}"
            ReadingGoalUnit.JUZ -> "Read $goal ${if (goal == 1) "juz'" else "juz'"}"
        }
    }
}

