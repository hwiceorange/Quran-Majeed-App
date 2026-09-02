package com.quranaudio.quiz.quiz

import com.quran.quranaudio.quiz.QuestionBean
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionViewModelTest {
    @Test
    fun contextualRoundContainsOnlyRequestedSurah() {
        val questions = listOf(
            question("global", 0),
            question("1-a", 1),
            question("2-a", 2),
            question("1-b", 1),
            question("1-c", 1),
            question("1-d", 1)
        )

        val round = QuestionViewModel.selectSurahQuestions(questions, 1, 0, 3)

        assertEquals(listOf("1-a", "1-b", "1-c"), round.map { it.id })
        assertTrue(round.all { it.surah_id == 1 })
    }

    @Test
    fun contextualRoundWrapsWithoutLeakingAnotherSurah() {
        val questions = listOf(
            question("18-a", 18),
            question("18-b", 18),
            question("2-a", 2),
            question("18-c", 18),
            question("18-d", 18)
        )

        val round = QuestionViewModel.selectSurahQuestions(questions, 18, 3, 3)

        assertEquals(listOf("18-d", "18-a", "18-b"), round.map { it.id })
        assertTrue(round.all { it.surah_id == 18 })
    }

    @Test
    fun contextualRoundReturnsInsufficientSetForCallerToReject() {
        val questions = listOf(question("2-a", 2), question("2-b", 2))

        val round = QuestionViewModel.selectSurahQuestions(questions, 2, 0, 3)

        assertEquals(2, round.size)
        assertTrue(round.all { it.surah_id == 2 })
    }

    private fun question(id: String, surahId: Int) = QuestionBean(id = id, surah_id = surahId)
}
