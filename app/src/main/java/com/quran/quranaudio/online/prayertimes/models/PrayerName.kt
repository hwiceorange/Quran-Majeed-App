package com.quran.quranaudio.online.prayertimes.models

import android.content.Context
import com.quran.quranaudio.online.R

/**
 * Prayer name constants and utilities
 * 
 * CRITICAL: Always use English prayer names as database keys!
 * - Database storage: "Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"
 * - UI display: Use getLocalizedName() for localized display
 * 
 * This ensures data consistency across language changes.
 */
object PrayerName {
    // Fixed English prayer names for database keys
    const val FAJR = "Fajr"
    const val DHUHR = "Dhuhr"
    const val ASR = "Asr"
    const val MAGHRIB = "Maghrib"
    const val ISHA = "Isha"
    
    /**
     * All prayer names in fixed order (English, for database)
     */
    @JvmField
    val ALL_PRAYERS = arrayOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
    
    /**
     * Get localized prayer name for UI display
     * @param englishName English prayer name (e.g., "Fajr")
     * @param context Android context for resource access
     * @return Localized prayer name for current language
     */
    @JvmStatic
    fun getLocalizedName(englishName: String, context: Context): String {
        return when (englishName) {
            FAJR -> context.getString(R.string.prayer_fajr)
            DHUHR -> context.getString(R.string.prayer_dhuhr)
            ASR -> context.getString(R.string.prayer_asr)
            MAGHRIB -> context.getString(R.string.prayer_maghrib)
            ISHA -> context.getString(R.string.prayer_isha)
            else -> englishName // Fallback to English name
        }
    }
    
    /**
     * Get all localized prayer names for UI display
     * @param context Android context for resource access
     * @return Array of localized prayer names in order
     */
    @JvmStatic
    fun getAllLocalizedNames(context: Context): Array<String> {
        return arrayOf(
            context.getString(R.string.prayer_fajr),
            context.getString(R.string.prayer_dhuhr),
            context.getString(R.string.prayer_asr),
            context.getString(R.string.prayer_maghrib),
            context.getString(R.string.prayer_isha)
        )
    }
    
    /**
     * Convert localized prayer name back to English (for backward compatibility)
     * @param localizedName Localized prayer name
     * @param context Android context for resource access
     * @return English prayer name, or original if no match found
     */
    @JvmStatic
    fun toEnglishName(localizedName: String, context: Context): String {
        return when (localizedName) {
            context.getString(R.string.prayer_fajr) -> FAJR
            context.getString(R.string.prayer_dhuhr) -> DHUHR
            context.getString(R.string.prayer_asr) -> ASR
            context.getString(R.string.prayer_maghrib) -> MAGHRIB
            context.getString(R.string.prayer_isha) -> ISHA
            else -> localizedName // Already English or unknown
        }
    }
    
    /**
     * Check if a string is a valid English prayer name
     */
    @JvmStatic
    fun isValidEnglishName(name: String): Boolean {
        return name in ALL_PRAYERS
    }
}

