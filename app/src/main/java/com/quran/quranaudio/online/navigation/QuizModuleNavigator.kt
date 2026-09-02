package com.quran.quranaudio.online.navigation

import android.content.Context
import android.content.Intent
import com.quran.quranaudio.online.prayertimes.ui.MainActivity

/** Single entry point for opening the app's canonical Learn/Quiz experience. */
object QuizModuleNavigator {
    internal const val EXTRA_OPEN_QUIZ = "extra_open_quiz"
    internal const val EXTRA_SURAH_ID = "extra_quiz_surah_id"
    internal const val EXTRA_SURAH_NAME = "extra_quiz_surah_name"

    data class Destination(
        val surahId: Int,
        val surahName: String?
    )

    @JvmStatic
    fun createIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_QUIZ, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    @JvmStatic
    fun createIntent(context: Context, surahId: Int, surahName: String?): Intent =
        createIntent(context)
            .putExtra(EXTRA_SURAH_ID, surahId)
            .putExtra(EXTRA_SURAH_NAME, surahName)

    @JvmStatic
    fun consumeDestination(intent: Intent?): Destination? {
        if (intent?.getBooleanExtra(EXTRA_OPEN_QUIZ, false) != true) return null
        val destination = Destination(
            surahId = intent.getIntExtra(EXTRA_SURAH_ID, 0),
            surahName = intent.getStringExtra(EXTRA_SURAH_NAME)
        )
        intent.removeExtra(EXTRA_OPEN_QUIZ)
        intent.removeExtra(EXTRA_SURAH_ID)
        intent.removeExtra(EXTRA_SURAH_NAME)
        return destination
    }
}
