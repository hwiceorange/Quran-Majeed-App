package com.quranaudio.quiz.quiz

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.animation.LinearInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.ad.FunctionTag
import com.quran.quranaudio.quiz.base.BaseBindingActivity
import com.quran.quranaudio.quiz.extension.getResColor
import com.quran.quranaudio.quiz.extension.getResString
import com.quran.quranaudio.quiz.extension.isValid
import com.quran.quranaudio.quiz.extension.reportClickEvent
import com.quran.quranaudio.quiz.extension.setDrawableLeft
import com.quran.quranaudio.quiz.extension.showGemAd
import com.quran.quranaudio.quiz.utils.RxBus
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.quran.quranaudio.quiz.activity.QuranQuestionFailActivity
import com.quran.quranaudio.quiz.databinding.ActivityQuestionRevivalBinding
import com.quran.quranaudio.quiz.fragments.QuranQuestionFragment

class QuranQuestionRevivalActivity :
    BaseBindingActivity<ActivityQuestionRevivalBinding>(ActivityQuestionRevivalBinding::inflate) {
    private val playTime = 8f
    private var isAutoClose = true
    private val countValueAnimator: ValueAnimator = ValueAnimator.ofFloat(0f, playTime).apply {
        addUpdateListener {
            val v = it.animatedValue as Float
            binding.circleProgress.setPercentage((1 - (v / playTime)) * 100)
            binding.countTimeTv.text = "${(playTime - v).toInt()}"
        }
        doOnEnd {
            if (isValid() && isAutoClose) {
                reportClickEvent("quiz_relive_auto_close")
                QuranQuestionFailActivity.open(this@QuranQuestionRevivalActivity)
                finish()
            }
        }
        duration = (playTime * 1000).toLong()
        interpolator = LinearInterpolator()
    }

    override fun initData() {
        super.initData()
        countValueAnimator.start()
    }

    @SuppressLint("SetTextI18n")
    override fun initView() {
        super.initView()
        BarUtils.setStatusBarColor(this, R.color.color_1b0c37.getResColor())
        binding.recoveryAdCl.isVisible = true
        binding.recoverGemCl.isVisible = false
        binding.gemCountTv.text = "${QuizGemManager.getGemCount()}"
        binding.recoverGemTv.text = "${QuizGemManager.RESTART_PLAY} ${R.string.quran_recovery.getResString()}"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                reportClickEvent("quiz_relive_back_close")
                isAutoClose = false
                QuranQuestionFailActivity.open(this@QuranQuestionRevivalActivity)
                finish()
            }
        })
        binding.recoveryAdCl.setOnClickListener {
            reportClickEvent("quiz_relive_recover_ad")
            val isShowAd = showGemAd(
                FunctionTag.QUESTION_FAIL_RELIVE_INTER,
                FunctionTag.QUESTION_FAIL_RELIVE_REWARD,
                0,
                getPageName(),
                getFormPageName()
            ) {
                isAutoClose = false
                countValueAnimator.cancel()
                RxBus.INSTANCE().post(QuestionFail(QuestionFail.TRY_AGAIN))
                finish()
            }
            if (isShowAd) {
                countValueAnimator.pause()
            } else {
                binding.recoveryAdCl.isVisible = false
                binding.recoverGemCl.isVisible = true

                binding.tipsTv.text = R.string.quran_quiz_revival_gem_tips.getResString()
            }
        }
        binding.recoverGemCl.setOnClickListener {
            reportClickEvent("quiz_relive_recover_gem")
            if (QuizGemManager.consumeCount(QuizGemManager.RESTART_PLAY)) {
                RxBus.INSTANCE().post(QuestionFail(QuestionFail.TRY_AGAIN))
                finish()
            } else {
                ToastUtils.showShort(R.string.quran_not_enough_gems)
            }
        }
        binding.noThanksTv.setOnClickListener {
            reportClickEvent("quiz_relive_no_thanks")
            isAutoClose = false
            countValueAnimator.cancel()
            QuranQuestionFailActivity.open(this)
            finish()
        }
        binding.gemCountTv.setDrawableLeft(
            R.drawable.bible_quiz_reward_gem_small,
            R.dimen.dp_24
        )
        binding.recoverGemTv.setDrawableLeft(
            R.drawable.bible_quiz_reward_gem_small,
            R.dimen.dp_24
        )
        binding.recoveryAdTv.setDrawableLeft(
            R.drawable.ic_question_revival_tv,
            R.dimen.dp_24
        )
        binding.adView.loadNativeAd(FunctionTag.NATIVE_QUIZ_RELIVE)
    }

    override fun getPageName(): String {
        return "QuestionRevivalActivity"
    }

    override fun getFormPageName(): String {
        return "quiz"
    }

    companion object {
        fun open(context: Context) {

            if (QuranQuestionFragment.isSelected) {
                context.startActivity(Intent(context, QuranQuestionRevivalActivity::class.java))
            }
        }
    }
}