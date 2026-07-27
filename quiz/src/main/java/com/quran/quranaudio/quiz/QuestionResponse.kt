package com.quranaudio.quiz.quiz

import android.util.Log
import com.quran.quranaudio.quiz.base.Constants
import com.quran.quranaudio.quiz.extension.SPTools
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
    private var cachedLanguage: String? = null  // 🔧 跟踪缓存的语言
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
        // 🔧 获取当前语言（支持 en, id, ar 及其他语言，fallback 到 en）
        com.quran.quranaudio.quiz.utils.AppConfig.setLanguage()
        val currentLanguage = com.quran.quranaudio.quiz.utils.AppConfig.lan
        
        // 🔧 如果语言改变了，清空缓存并重新加载
        if (cachedLanguage != currentLanguage) {
            android.util.Log.d("QuestionResponse", "🔄 语言已切换: $cachedLanguage -> $currentLanguage，重新加载题目")
            allQuestions = listOf()  // 清空缓存
            cachedLanguage = currentLanguage
        }
        
        if (allQuestions.isEmpty()) {
            android.util.Log.d("QuestionResponse", "📚 开始加载题目文件...")
            allQuestions = initAllQuestions()
            android.util.Log.d("QuestionResponse", "✅ 题目加载完成，共 ${allQuestions.size} 题")
        } else {
            android.util.Log.d("QuestionResponse", "♻️ 使用缓存的题目，共 ${allQuestions.size} 题")
        }
        allQuestions
    }

    /** 情境化联动：按 surah 过滤该章题目（供阅读器"本章测验"使用）。当前语言题库。 */
    suspend fun getQuestionsBySurah(surahId: Int): List<QuestionBean> = withContext(Dispatchers.Default) {
        getAllQuestion().filter { it.surah_id == surahId }
    }

    /** 该章可用题目数——用于决定是否展示"本章测验"入口（题量太少不值得展示）。 */
    suspend fun countBySurah(surahId: Int): Int = getQuestionsBySurah(surahId).size

    /** Java 友好回调接口（供阅读器等 Java 侧使用）。 */
    fun interface CountCallback { fun onCount(count: Int) }

    /**
     * Java 友好：异步统计该章题量并在主线程回调。供阅读器决定是否显示"本章测验"入口。
     * 任何异常都回调 0（调用方据此不显示入口），绝不影响调用方。
     */
    @JvmStatic
    fun countBySurahAsync(surahId: Int, cb: CountCallback) {
        GlobalScope.launch(Dispatchers.Main) {
            val n = try { getQuestionsBySurah(surahId).size } catch (e: Exception) { 0 }
            cb.onCount(n)
        }
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