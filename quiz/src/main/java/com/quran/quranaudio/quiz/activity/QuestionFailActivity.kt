package com.quran.quranaudio.quiz.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.activity.OnBackPressedCallback
import com.quran.quranaudio.quiz.R
import com.bible.tools.ad.FunctionTag
import com.bible.tools.base.BaseBindingActivity
import com.bible.tools.extension.hasInterAdByPool
import com.bible.tools.extension.reportClickEvent
import com.bible.tools.extension.reportExitFunShowEvent
import com.bible.tools.extension.showInterAdByPoolNew
import com.bible.tools.quiz.QuestionFail
import com.bible.tools.utils.RxBus
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import com.hydra.common.ad.AdConfig
import com.quran.quranaudio.quiz.databinding.ActivityQuestionFailBinding

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

            // todo MainActivity
           // if (MainActivity.isSelectQuizTab()) {
                context.startActivity(Intent(context, QuestionFailActivity::class.java))
         //   }
        }
    }
}