package com.quran.quranaudio.online.home.quiz

data class QuizQuestion(
    val id: Int,
    val questionText: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val chapterRef: String
)

