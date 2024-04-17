package com.quran.quranaudio.quiz.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.activity.OnBackPressedCallback
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.ad.FunctionTag
import com.quran.quranaudio.quiz.base.BaseBindingActivity
import com.quran.quranaudio.quiz.extension.hasInterAdByPool
import com.quran.quranaudio.quiz.extension.reportClickEvent
import com.quran.quranaudio.quiz.extension.reportExitFunShowEvent
import com.quran.quranaudio.quiz.extension.showInterAdByPoolNew
import com.quranaudio.quiz.quiz.QuestionFail
import com.quran.quranaudio.quiz.utils.RxBus
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.quranaudio.common.ad.AdConfig
import com.quran.quranaudio.quiz.databinding.ActivityQuestionFailBinding
import com.quran.quranaudio.quiz.fragments.QuestionFragment

class QuestionFailActivity :
    BaseBindingActivity<ActivityQuestionFailBinding>(ActivityQuestionFailBinding::inflate) {

    override fun initView() {
        super.initView()
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setStatusBarColor(this, Color.TRANSPARENT)
        RxBus.INSTANCE().post(QuestionFail(QuestionFail.TRY_AGAIN))
        binding.skipQuestionTv.setOnClickListener {
            reportClickEvent("quiz_skip")
            if (hasInterAdByPool()) {
                showInterAdByPoolNew(AdConfig.AD_INTERS, FunctionTag.QUESTION_FAIL_SKIP, 0, {
                    reportExitFunShowEvent(getPageName(), getFormPageName(), it, FunctionTag.QUESTION_FAIL_SKIP)
                }, {
                    RxBus.INSTANCE().post(QuestionFail(QuestionFail.SKIP_QUESTION))
                    finish()
                })
            } else {
                ToastUtils.showLong(R.string.bible_no_ad_tips)
            }

        }
        binding.tryAgainTv.setOnClickListener {
            reportClickEvent("quiz_again")
            showInterAdByPoolNew(AdConfig.AD_INTERS, FunctionTag.QUESTION_FAIL_TRY_AGAIN, 0, {
                reportExitFunShowEvent(getPageName(), getFormPageName(), it, FunctionTag.QUESTION_FAIL_TRY_AGAIN)
            }, {
                RxBus.INSTANCE().post(QuestionFail(QuestionFail.TRY_AGAIN))
                finish()
            })
        }
        binding.quitTv.setOnClickListener {
            reportClickEvent("quiz_quit")
            RxBus.INSTANCE().post(QuestionFail(QuestionFail.QUIT_LEVEL))
            finish()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                RxBus.INSTANCE().post(QuestionFail(QuestionFail.QUIT_LEVEL))
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.adView.loadNativeAd(FunctionTag.NATIVE_QUIZ_FAIL)
    }

    override fun getPageName(): String {
        return "quiz_fail"
    }

    override fun getFormPageName(): String {
        return "quiz"
    }

    companion object {
        fun open(context: Context) {


            if (QuestionFragment.isSelected) {
                context.startActivity(Intent(context, QuestionFailActivity::class.java))
            }
        }
    }
}