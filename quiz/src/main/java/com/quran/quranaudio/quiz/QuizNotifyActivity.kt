package com.quranaudio.quiz.quiz

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.base.BaseBindingActivity
import com.quran.quranaudio.quiz.base.Constants
import com.quran.quranaudio.quiz.base.MainTabChangeEvent
import com.quran.quranaudio.quiz.extension.getResString
import com.quran.quranaudio.quiz.utils.RxBus
import com.quran.quranaudio.quiz.utils.Tasks
import com.quran.quranaudio.quiz.utils.isDebug
import com.blankj.utilcode.util.BarUtils
import com.quran.quranaudio.quiz.QuestionBean
import com.quran.quranaudio.quiz.databinding.ActivityQuizNotifyBinding

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class QuizNotifyActivity :
    BaseBindingActivity<ActivityQuizNotifyBinding>(ActivityQuizNotifyBinding::inflate) {

    private val questionBean by lazy {
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.INTENT_NOTIFY_QUIZ_BEAN, QuestionBean::class.java)
        } else{
            intent.getParcelableExtra<QuestionBean>(Constants.INTENT_NOTIFY_QUIZ_BEAN)
        }
//        IntentCompat.getParcelableExtra(
//            intent,
//            Constants.INTENT_NOTIFY_QUIZ_BEAN,
//            QuestionBean::class.java
//        )
    }

    override fun initView() {
        super.initView()
        if (questionBean == null) {
            finish()
            return
        }
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setStatusBarColor(this, Color.TRANSPARENT)
        binding.closeIv.setOnClickListener {
            RxBus.INSTANCE().post(MainTabChangeEvent(MainTabChangeEvent.TO_QUIZ))
            finish()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                RxBus.INSTANCE().post(MainTabChangeEvent(MainTabChangeEvent.TO_QUIZ))
                finish()
            }
        })
        updateQuestionUI(questionBean!!)
    }

    private fun updateQuestionUI(questionBean: QuestionBean) {
        binding.questionProgressTv.text = R.string.bible_question.getResString("").trim()
        binding.questionContentTv.text = questionBean.question
        binding.optionsView.setData(questionBean)
        if (isDebug()) {
            binding.debugRightAnswerTv.text = questionBean.answer
        }
        binding.optionsView.setAnswerResultListener {_, selectAnswer ->
            Tasks.postDelayedByUI({
                QuizNotifyResultActivity.open(this, Bundle().apply {
                    putParcelable(Constants.INTENT_NOTIFY_QUIZ_BEAN, questionBean)
                    putString(Constants.INTENT_NOTIFY_QUIZ_SELECT_ANSWER, selectAnswer)
                })
                finish()
            },500)
        }
    }

    override fun getPageName(): String {
        return "quiz_notify"
    }

    override fun getFormPageName(): String {
        return "splash"
    }

    companion object {
        fun open(context: Context, bundle: Bundle) {
            context.startActivity(Intent(context, QuizNotifyActivity::class.java).apply {
                putExtras(bundle)
            })
        }
    }
}