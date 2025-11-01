package com.quran.quranaudio.online.home.quiz

import android.content.Context

class QuizRepository(context: Context) {

    private val sharedPrefs = context.getSharedPreferences("QuizPrefs", Context.MODE_PRIVATE)
    private val KEY_LAST_ANSWERED_ID = "last_answered_quiz_id"

    private val allQuestions = listOf(
        QuizQuestion(
            id = 1,
            questionText = "Which Surah is also known as the Mother of the Quran?",
            options = listOf("Al-Fatiha", "Al-Baqarah", "Yasin", "Al-Ikhlas"),
            correctAnswerIndex = 0,
            chapterRef = "Al-Fatiha"
        ),
        QuizQuestion(
            id = 2,
            questionText = "How many times is the word 'Allah' mentioned in the Quran?",
            options = listOf("2698", "2699", "2700", "2701"),
            correctAnswerIndex = 1,
            chapterRef = "Various"
        ),
        QuizQuestion(
            id = 3,
            questionText = "What is the longest Surah in the Holy Quran?",
            options = listOf("Al-Fatiha", "Al-Baqarah", "Al-Imran", "An-Nisa"),
            correctAnswerIndex = 1,
            chapterRef = "Al-Baqarah"
        )
    )

    fun getCurrentQuestion(): QuizQuestion {
        val lastAnsweredId = sharedPrefs.getInt(KEY_LAST_ANSWERED_ID, 0)
        val nextQuestion = allQuestions.firstOrNull { it.id == lastAnsweredId + 1 }
        return nextQuestion ?: allQuestions.first()
    }

    fun markQuestionAnswered(questionId: Int) {
        val lastAnsweredId = sharedPrefs.getInt(KEY_LAST_ANSWERED_ID, 0)
        if (questionId > lastAnsweredId) {
            sharedPrefs.edit().putInt(KEY_LAST_ANSWERED_ID, questionId).apply()
        }
    }
}

