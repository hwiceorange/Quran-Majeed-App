package com.quranaudio.quiz.quiz.dialog

import android.content.DialogInterface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ImageSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.ad.FunctionTag
import com.quran.quranaudio.quiz.base.BaseDialogFragment
import com.quran.quranaudio.quiz.extension.reportClickEvent
import com.quran.quranaudio.quiz.extension.setDrawableLeft
import com.quran.quranaudio.quiz.extension.showGemAd
import com.quranaudio.quiz.quiz.QuizGemManager
import com.quran.quranaudio.quiz.databinding.DialogFreeGemsBinding


/**
 * 更多钻石奖励
 */
class QuranFreeRewardDialog : BaseDialogFragment<DialogFreeGemsBinding>() {

    companion object{
        const val TAG = "FreeRewardDialog"
    }

    var dismissCallBack: (() -> Unit)? = null

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogFreeGemsBinding = DialogFreeGemsBinding.inflate(inflater, container, false)

    override fun ensureViewAndClicks() {
        mBinding.freeWatchTv.setDrawableLeft(
            R.drawable.ic_question_revival_tv,
            R.dimen.dp_24
        )
        val spannable = SpannableString("( +5)")
        val imageDrawable = ImageSpan(requireContext(), R.drawable.bible_quiz_reward_gem_small)
        spannable.setSpan(imageDrawable, 1,2, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        mBinding.freeReward.text = spannable
        mBinding.freeClose.setOnClickListener {
            reportClickEvent("quiz_free_dia_close")
            dismiss()
        }
        mBinding.watchAd.setOnClickListener {
            reportClickEvent("quiz_free_dia_ad")
            activity?.showGemAd(
                FunctionTag.QUESTION_FREE_INTERS,
                FunctionTag.QUESTION_FREE_REWARD, QuizGemManager.WATCH_AD_REWARD, getPageName(), getFromPageName()){
                dismiss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissCallBack?.invoke()
    }

    override fun getPageName(): String {
        return "FreeRewardDialog"
    }

    override fun getFromPageName(): String {
        return "QuestionFragment"
    }

}