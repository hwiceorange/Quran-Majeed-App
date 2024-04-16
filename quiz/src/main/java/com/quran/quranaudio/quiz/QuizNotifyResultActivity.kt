package com.bible.tools.quiz

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.core.content.IntentCompat
import com.bible.tools.base.BaseBindingActivity
import com.bible.tools.base.Constants
import com.bible.tools.base.MainTabChangeEvent
import com.bible.tools.extension.visible
import com.bible.tools.utils.RxBus
import com.blankj.utilcode.util.BarUtils
import com.quran.quranaudio.quiz.QuestionBean
import com.quran.quranaudio.quiz.databinding.ActivityQuizNotifyResultBinding

class QuizNotifyResultActivity :
    BaseBindingActivity<ActivityQuizNotifyResultBinding>(ActivityQuizNotifyResultBinding::inflate) {

    override fun initView() {
        super.initView()
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setStatusBarColor(this, Color.TRANSPARENT)

        val rightAnswer = //intent.getParcelableExtra( Constants.INTENT_NOTIFY_QUIZ_BEAN,  QuestionBean::class.java)
        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Constants.INTENT_NOTIFY_QUIZ_BEAN, QuestionBean::class.java)
        } else{
            intent.getParcelableExtra<QuestionBean>(Constants.INTENT_NOTIFY_QUIZ_BEAN)
        }?.getRightAnswer()?:""


        val selectAnswer = intent.getStringExtra(Constants.INTENT_NOTIFY_QUIZ_SELECT_ANSWER) ?: ""
        if (rightAnswer.isEmpty() || selectAnswer.isEmpty()) {
            finish()
            return
        }
        if (rightAnswer == selectAnswer) {
            binding.notifyCorrectCl.visible()
        } else {
            binding.notifyFailCl.visible()
            binding.correctAnswerTv.text = rightAnswer
        }
        binding.quitTv.setOnClickListener { finish() }
        binding.playMoreTv.setOnClickListener { finish() }
    }

    override fun getPageName(): String {
        return "quiz_notify_result"
    }

    override fun getFormPageName(): String {
        return "splash"
    }

    override fun finish() {
        super.finish()
        RxBus.INSTANCE().post(MainTabChangeEvent(MainTabChangeEvent.TO_QUIZ))
    }

    companion object {
        fun open(context: Context, bundle: Bundle) {
            context.startActivity(Intent(context, QuizNotifyResultActivity::class.java).apply {
                putExtras(bundle)
            })
        }
    }
}