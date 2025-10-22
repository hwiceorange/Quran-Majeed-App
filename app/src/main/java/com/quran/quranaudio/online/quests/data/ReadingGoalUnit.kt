package com.quran.quranaudio.online.quests.data

/**
 * Enum representing different units for daily reading goals.
 * 
 * Users can choose to read:
 * - PAGES: Number of Mushaf pages (1-50)
 * - VERSES: Number of ayahs (1-100)
 * - JUZ: Number of Juz' parts (1-30)
 */
enum class ReadingGoalUnit(val displayName: String) {
    PAGES("Pages"),
    VERSES("Verses"),
    JUZ("Juz'");
    
    companion object {
        fun fromString(value: String): ReadingGoalUnit {
            return values().find { it.name.equals(value, ignoreCase = true) } ?: PAGES
        }
    }
}

