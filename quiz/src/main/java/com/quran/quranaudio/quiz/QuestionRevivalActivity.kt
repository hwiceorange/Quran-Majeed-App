package com.bible.tools.quiz

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.animation.LinearInterpolator
import androidx.activity.OnBackPressedCallback
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import com.quran.quranaudio.quiz.R
import com.bible.tools.ad.FunctionTag
import com.bible.tools.base.BaseBindingActivity
import com.bible.tools.extension.getResColor
import com.bible.tools.extension.getResString
import com.bible.tools.extension.isValid
import com.bible.tools.extension.reportClickEvent
import com.bible.tools.extension.setDrawableLeft
import com.bible.tools.extension.showGemAd
import com.bible.tools.utils.RxBus
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.quran.quranaudio.quiz.activity.QuestionFailActivity
import com.quran.quranaudio.quiz.databinding.ActivityQuestionRevivalBinding
import com.quran.quranaudio.quiz.fragments.QuestionFragment

class QuestionRevivalActivity :
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
                QuestionFailActivity.open(this@QuestionRevivalActivity)
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
        binding.recoverGemTv.text = "${QuizGemManager.RESTART_PLAY} ${R.string.bible_recovery.getResString()}"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                reportClickEvent("quiz_relive_back_close")
                isAutoClose = false
                QuestionFailActivity.open(this@QuestionRevivalActivity)
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

                binding.tipsTv.text = R.string.bible_quiz_revival_gem_tips.getResString()
            }
        }
        binding.recoverGemCl.setOnClickListener {
            reportClickEvent("quiz_relive_recover_gem")
            if (QuizGemManager.consumeCount(QuizGemManager.RESTART_PLAY)) {
                RxBus.INSTANCE().post(QuestionFail(QuestionFail.TRY_AGAIN))
                finish()
            } else {
                ToastUtils.showShort(R.string.bible_not_enough_gems)
            }
        }
        binding.noThanksTv.setOnClickListener {
            reportClickEvent("quiz_relive_no_thanks")
            isAutoClose = false
            countValueAnimator.cancel()
            QuestionFailActivity.open(this)
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

            if (QuestionFragment.isSelected) {
                context.startActivity(Intent(context, QuestionRevivalActivity::class.java))
            }
        }
    }
}