package com.quran.quranaudio.online.rewards

import android.content.Context
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

object RewardEntitlementStore {
    private const val PREFS = "reward_value_entitlements"
    private const val FIRST_AUDIO_USED = "first_audio_used"
    private const val FIRST_TRANSLATION_USED = "first_translation_used"

    fun canUseFirstAudioFree(context: Context) = !prefs(context).getBoolean(FIRST_AUDIO_USED, false)
    fun markFirstAudioUsed(context: Context) = prefs(context).edit().putBoolean(FIRST_AUDIO_USED, true).apply()
    fun canUseFirstTranslationFree(context: Context) = !prefs(context).getBoolean(FIRST_TRANSLATION_USED, false)
    fun markFirstTranslationUsed(context: Context) = prefs(context).edit().putBoolean(FIRST_TRANSLATION_USED, true).apply()

    fun isUnlocked(context: Context, type: String, contentId: String): Boolean =
        prefs(context).getBoolean("unlock_${type}_${contentId}", false)

    fun unlock(context: Context, type: String, contentId: String) =
        prefs(context).edit().putBoolean("unlock_${type}_${contentId}", true).apply()

    @JvmStatic
    fun isDeepInsightUnlockedThisWeek(context: Context): Boolean =
        prefs(context).getString("deep_insight_week", "") == currentWeekKey()

    @JvmStatic
    fun unlockDeepInsightThisWeek(context: Context) =
        prefs(context).edit().putString("deep_insight_week", currentWeekKey()).apply()

    private fun currentWeekKey(): String {
        val now = LocalDate.now()
        val fields = WeekFields.of(Locale.getDefault())
        return "${now.get(fields.weekBasedYear())}-${now.get(fields.weekOfWeekBasedYear())}"
    }

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
