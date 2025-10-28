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
import com.quran.quranaudio.quiz.fragments.QuranQuestionFragment

class QuranQuestionFailActivity :
    BaseBindingActivity<ActivityQuestionFailBinding>(ActivityQuestionFailBinding::inflate) {

    override fun initView() {
        super.initView()
        BarUtils.setStatusBarLightMode(this, true)
        BarUtils.setStatusBarColor(this, Color.TRANSPARENT)
        
        // 🔧 发送 TRY_AGAIN 事件以清除错误反馈状态
        // 这会让 Fragment 调用 updateQuestionUI(currentBean)，从而：
        // 1. 清除 "Wrong" 显示 (answerResultTv.invisible())
        // 2. 重置倒计时器（重新创建 countValueAnimator）
        // 3. 重新显示当前题目，准备好接受用户的下一步操作
        // 注意：题目会短暂显示一下，然后被失败界面遮挡，这是必要的状态清理过程
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
                ToastUtils.showLong(R.string.quran_no_ad_tips)
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
            // 🔧 修复死循环：移除 isSelected 检查
            // 原因：复活界面打开时，Fragment 进入 onPause，isSelected 变为 false
            // 导致失败界面无法打开，复活界面 finish() 后 Fragment 重新 onResume
            // isSelected 变为 true，倒计时重启后又打开复活界面，形成死循环
            context.startActivity(Intent(context, QuranQuestionFailActivity::class.java))
        }
    }
}