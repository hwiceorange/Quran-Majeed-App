package com.quran.quranaudio.online.navigation

import android.content.Context
import android.content.Intent
import com.quran.quranaudio.online.prayertimes.ui.MainActivity

/** Single entry point for opening the app's canonical Learn/Quiz experience. */
object QuizModuleNavigator {
    internal const val EXTRA_OPEN_QUIZ = "extra_open_quiz"

    @JvmStatic
    fun createIntent(context: Context): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_OPEN_QUIZ, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    @JvmStatic
    fun consumeOpenQuiz(intent: Intent?): Boolean {
        if (intent?.getBooleanExtra(EXTRA_OPEN_QUIZ, false) != true) return false
        intent.removeExtra(EXTRA_OPEN_QUIZ)
        return true
    }
}
