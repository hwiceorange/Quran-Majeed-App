package com.quran.quranaudio.quiz.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.ToastUtils
import com.quran.quranaudio.quiz.QuestionBean
import com.quran.quranaudio.quiz.R
import com.quranaudio.quiz.quiz.QuestionOptionView
import com.quranaudio.quiz.quiz.QuestionResponse
import kotlinx.coroutines.launch

/**
 * 情境化"本章测验" —— 读完/听完某段后就该章出题。完全独立于全局闯关流程：
 * 不涉及关卡进度、宝石、广告、登录。仅取该 surah 的题目逐题作答并出成绩。
 * 任何异常/无题都优雅结束，绝不影响调用方(阅读器)。
 */
class SurahQuizActivity : AppCompatActivity() {

    private var surahId = 0
    private var surahName = ""
    private var questions = listOf<QuestionBean>()
    private var index = 0
    private var score = 0
    private val ui = Handler(Looper.getMainLooper())

    private lateinit var titleTv: TextView
    private lateinit var progressTv: TextView
    private lateinit var questionTv: TextView
    private lateinit var options: QuestionOptionView
    private lateinit var resultView: View
    private lateinit var scoreTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_surah_quiz)
            surahId = intent.getIntExtra(KEY_SURAH_ID, 0)
            surahName = intent.getStringExtra(KEY_SURAH_NAME) ?: getString(R.string.quiz_surah_title)

            titleTv = findViewById(R.id.sqTitle)
            progressTv = findViewById(R.id.sqProgress)
            questionTv = findViewById(R.id.sqQuestion)
            options = findViewById(R.id.sqOptions)
            resultView = findViewById(R.id.sqResult)
            scoreTv = findViewById(R.id.sqScoreTv)
            titleTv.text = surahName

            findViewById<ImageView>(R.id.sqBack).setOnClickListener { finish() }
            findViewById<TextView>(R.id.sqRetry).apply {
                text = getString(R.string.quran_try_again)
                setOnClickListener { startRound() }
            }
            findViewById<TextView>(R.id.sqDone).apply {
                text = getString(R.string.quiz_surah_done)
                setOnClickListener { finish() }
            }

            options.setAnswerResultListener { isRight, _ ->
                if (isRight) score++
                ui.postDelayed({ if (!isFinishing) next() }, 1300)
            }

            loadQuestions()
        } catch (e: Throwable) {
            // 兜底：任何初始化异常都直接结束，不崩溃
            android.util.Log.e("SurahQuiz", "init failed", e)
            finish()
        }
    }

    private fun loadQuestions() {
        lifecycleScope.launch {
            val all = try {
                QuestionResponse.getQuestionsBySurah(surahId)
            } catch (e: Throwable) {
                android.util.Log.e("SurahQuiz", "load failed", e); emptyList()
            }
            if (all.isEmpty()) {
                ToastUtils.showShort(getString(R.string.quiz_surah_not_enough))
                finish(); return@launch
            }
            questions = all.shuffled().take(10)
            startRound()
        }
    }

    private fun startRound() {
        index = 0; score = 0
        resultView.visibility = View.GONE
        bind()
    }

    private fun bind() {
        val q = questions.getOrNull(index) ?: run { showResult(); return }
        progressTv.text = getString(R.string.quran_question, "${index + 1}/${questions.size}")
        questionTv.text = q.question
        options.setData(q)
    }

    private fun next() {
        index++
        if (index >= questions.size) showResult() else bind()
    }

    private fun showResult() {
        scoreTv.text = "$score / ${questions.size}"
        resultView.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        ui.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val KEY_SURAH_ID = "surah_id"
        private const val KEY_SURAH_NAME = "surah_name"

        /** 供阅读器调用：启动本章测验。调用方无需关心内部实现。 */
        fun start(context: Context, surahId: Int, surahName: String?) {
            try {
                context.startActivity(
                    Intent(context, SurahQuizActivity::class.java)
                        .putExtra(KEY_SURAH_ID, surahId)
                        .putExtra(KEY_SURAH_NAME, surahName ?: "")
                )
            } catch (e: Throwable) {
                android.util.Log.e("SurahQuiz", "start failed", e)
            }
        }
    }
}
