package com.quranaudio.quiz.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.base.Constants
import com.quran.quranaudio.quiz.extension.SPTools
import com.quran.quranaudio.quiz.extension.getResString
import com.quran.quranaudio.quiz.extension.logd
import com.quran.quranaudio.quiz.QuestionBean
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class QuestionViewModel : ViewModel() {
    // 每个关卡的答题数
    private val QUESTION_COUNT_PRE = 3
    private var leveQuestions = listOf<QuestionBean>()
    val currentQuestionBean = MutableSharedFlow<QuestionBean?>()
    var currentQuestionIndex = 0
    fun initLevel() {
        viewModelScope.launch {
            val allQuestion = QuestionResponse.getAllQuestion()
            val startIndex = getStartIndex(allQuestion.size)
            logd(
                "end quiz range = [$startIndex, ${startIndex + QUESTION_COUNT_PRE - 1}], all count = ${allQuestion.size}",
                "quiz_log"
            )
            leveQuestions = allQuestion.subList(startIndex, startIndex + QUESTION_COUNT_PRE)
            currentQuestionBean.emit(leveQuestions[currentQuestionIndex])
        }
    }

    private fun getStartIndex(totalSize: Int): Int {
        var startIndex = SPTools.getInt(Constants.KEY_LAST_QUESTION_START_INDEX, 0)
        logd("start quiz startIndex = $startIndex, all count = $totalSize", "quiz_log")
        if (startIndex >= 0 && startIndex + QUESTION_COUNT_PRE <= totalSize) {
            return startIndex
        } else {
            startIndex = 0
            SPTools.put(Constants.KEY_LAST_QUESTION_START_INDEX, 0)
            return startIndex
        }
    }

    fun intoNextLevel() {
        val lastLevel = SPTools.getInt(Constants.KEY_LAST_QUESTION_LEVEL, 1)
        SPTools.put(Constants.KEY_LAST_QUESTION_LEVEL, lastLevel + 1)
        val lastLevelStartIndex = SPTools.getInt(Constants.KEY_LAST_QUESTION_START_INDEX, 0)
        SPTools.put(
            Constants.KEY_LAST_QUESTION_START_INDEX,
            lastLevelStartIndex + QUESTION_COUNT_PRE
        )
        currentQuestionIndex = 0
        initLevel()
    }

    fun showNextQuestion() {
        currentQuestionIndex += 1
        viewModelScope.launch {
            currentQuestionBean.emit(leveQuestions[currentQuestionIndex])
        }
    }

    fun tryAgainQuestion() {
        currentQuestionIndex = 0
        viewModelScope.launch {
            currentQuestionBean.emit(leveQuestions[currentQuestionIndex])
        }
    }

    fun isLastQuestionInLevel(): Boolean {
        return currentQuestionIndex >= leveQuestions.size - 1
    }

    fun getProgressQuestion() =
        R.string.quran_question.getResString("${currentQuestionIndex + 1}/$QUESTION_COUNT_PRE")

    class Factory() : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuestionViewModel() as T
        }
    }
}

class QuestionFail {
    companion object {
        val SKIP_QUESTION = 1
        val TRY_AGAIN = 2
        val QUIT_LEVEL = 3
    }

    var failStatus = -1

    constructor(failStatus: Int) {
        this.failStatus = failStatus
    }
}