package com.bible.tools.quiz

import android.util.Log
import com.bible.tools.base.Constants
import com.bible.tools.extension.SPTools
import com.blankj.utilcode.util.GsonUtils
import com.quran.quranaudio.quiz.QuestionBean
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception

object QuestionResponse {
    private var allQuestions = listOf<QuestionBean>()
    private const val limitQuizLength = 120

    @OptIn(DelicateCoroutinesApi::class)
    fun getRandomQuiz(callback: (QuestionBean?)->Unit) {
        GlobalScope.launch() {
            getAllQuestion()
            launch(Dispatchers.Main) {
                if (allQuestions.isEmpty()) {
                    callback.invoke(null)
                } else {
                    val quizIds = SPTools.getString(Constants.KEY_ALREADY_SHOW_PUSH_QUIZ_ID, "")
                    Log.d("PushLog", "getRandomQuiz: quizIds = $quizIds")
                    val stringList = quizIds.split("|").toMutableList()
                    var filterList = allQuestions.filter { it.question.length <= limitQuizLength && !stringList.contains("${it.id}")}
                    Log.d("PushLog", "getRandomQuiz: filterList = ${filterList.map { it.id }.joinToString("|")}")

                    if (filterList.isEmpty()) {
                        Log.d("PushLog", "getRandomQuiz: quiz 随机题目已经展示完，重新开始")
                        stringList.clear()
                        filterList = allQuestions.filter { it.question.length <= limitQuizLength}
                    }
                    Log.d("PushLog", "getRandomQuiz: filterList.size = ${filterList.size}")
                    val result = filterList.random()
                    stringList.add("${result.id}")
                    SPTools.put(Constants.KEY_ALREADY_SHOW_PUSH_QUIZ_ID, stringList.joinToString("|"))
                    callback.invoke(result)
                }
            }
        }

    }

    suspend fun getAllQuestion() = withContext(Dispatchers.Default) {
        if (allQuestions.isEmpty()) {
            allQuestions = initAllQuestions()
        }
        allQuestions
    }

    private suspend fun initAllQuestions(): List<QuestionBean> = withContext(Dispatchers.IO) {
        val questionStr = QuestionTools.getQuestionStr()
        if (questionStr.isEmpty()) {
            listOf()
        } else {
            try {
                GsonUtils.fromJson(
                    questionStr,
                    GsonUtils.getListType(QuestionBean::class.java)
                )
            }catch (e: Exception) {
                listOf()
            }
        }

    }
}