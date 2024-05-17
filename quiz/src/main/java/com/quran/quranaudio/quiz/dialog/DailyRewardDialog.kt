package com.quranaudio.quiz.quiz.dialog

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.ad.FunctionTag
import com.quran.quranaudio.quiz.base.BaseDialogFragment
import com.quran.quranaudio.quiz.base.Constants
import com.quran.quranaudio.quiz.extension.SPTools
import com.quran.quranaudio.quiz.extension.daysBetween
import com.quran.quranaudio.quiz.extension.reportClickEvent
import com.quran.quranaudio.quiz.extension.showGemAd
import com.quranaudio.quiz.quiz.QuizGemManager
import com.quran.quranaudio.quiz.databinding.DialogDailyRewardBinding
import kotlin.math.max

/**
 * 每日宝箱奖励弹窗
 */
class DailyRewardDialog : BaseDialogFragment<DialogDailyRewardBinding>() {

    companion object {
        const val TAG = "DailyRewardDialog"
    }

    var dismissCallBack: ((Int) -> Unit)? = null
    override fun getPageName(): String {
        return "DailyRewardDialog"
    }

    override fun getFromPageName(): String {
        return "QuestionFragment"
    }

    //七日奖励
    private val list = arrayListOf(10, 12, 14, 16, 18, 20, 30)
    private var toDayGem = 0 //今日奖励

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): DialogDailyRewardBinding = DialogDailyRewardBinding.inflate(inflater, container, false)

    override fun ensureViewAndClicks() {
        initData()
        val spannable = SpannableString(getString(R.string.quran_get_gem_with_video))
        val colorSpan = ForegroundColorSpan("#FFE792".toColorInt())
        val start = getString(R.string.quran_get_gem_with_video).indexOf("30")
        val end = start + 2
        spannable.setSpan(colorSpan, start,end, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)
        mBinding.dailyWatchAdTv.text = spannable
        mBinding.noThanks.setOnClickListener {
            reportClickEvent("quiz_daily_box_no_thanks")
            dismiss()
        }
        mBinding.dailyClose.setOnClickListener {
            reportClickEvent("quiz_daily_box_close")
            dismiss()
        }
        mBinding.watchAd.setOnClickListener {
            reportClickEvent("quiz_free_dia_ad")
            //需要在关闭弹窗的时候给奖励，所以此次不在内部发放: rewardGems=0
            activity?.showGemAd(
                FunctionTag.QUESTION_DAILY_INTERS,
                FunctionTag.QUESTION_DAILY_REWARD,
                0,
                getPageName(),
                getFromPageName()
            ){
                toDayGem = max(QuizGemManager.WATCH_DAILY_AD_REWARD, toDayGem)
                dismiss()
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initData(){
        var dailyNums =
            SPTools.getBytes(Constants.DAILY_REWARD_NUM, byteArrayOf(0, 0, 0, 0, 0, 0, 0))
        val receivedReward = SPTools.getLong(Constants.LAST_RECEIVED_BOX_TIME, 0)
        val daysBetween = System.currentTimeMillis().daysBetween(receivedReward)
        if (daysBetween > 0){
            var lastDayIndex = 0
            dailyNums.forEachIndexed { index, byte ->
                if (byte.toInt() == 1) lastDayIndex = index
            }
            val newIndex = if (lastDayIndex + daysBetween > 6) { //超出一个循环，重置
                dailyNums = byteArrayOf(0, 0, 0, 0, 0, 0, 0)
                0
            } else lastDayIndex + daysBetween
            dailyNums[newIndex] = 1
            SPTools.put(Constants.DAILY_REWARD_NUM, dailyNums)
            toDayGem = list[newIndex]
//            QuizGemManager.addCount(list[newIndex])
        }
        val dayTvList = listOf(mBinding.day1,mBinding.day2,mBinding.day3,mBinding.day4,
            mBinding.day5,mBinding.day6,mBinding.day7)
        val dayTopTvList = listOf(mBinding.day1Tv,mBinding.day2Tv,mBinding.day3Tv,mBinding.day4Tv,
            mBinding.day5Tv,mBinding.day6Tv,mBinding.day7Tv)
        val dayIVList = listOf(mBinding.day1Iv,mBinding.day2Iv,mBinding.day3Iv,mBinding.day4Iv,
            mBinding.day5Iv,mBinding.day6Iv,mBinding.day7Iv)
        val dayCheckedList = listOf(mBinding.day1Checked,mBinding.day2Checked,mBinding.day3Checked,mBinding.day4Checked,
            mBinding.day5Checked,mBinding.day6Checked,mBinding.day7Checked)
        val dayGroupList = listOf(mBinding.day1Group,mBinding.day2Group,mBinding.day3Group,mBinding.day4Group,
            mBinding.day5Group,mBinding.day6Group,mBinding.day7Group)
        val dayMarkList = listOf(mBinding.day1Mark,mBinding.day2Mark,mBinding.day3Mark,mBinding.day4Mark,
        mBinding.day5Mark,mBinding.day6Mark,mBinding.day7Mark)
        val todayIndex = dailyNums.indexOfLast { it.toInt() == 1 }
        dailyNums.forEachIndexed { index, byte ->
            if (todayIndex == index){
                dayTopTvList[index].isSelected = true
                dayTvList[index].isSelected = true
                dayGroupList[index].isSelected = true
                dayTvList[index].text = "+${list[index]}"
//                dayCheckedList[index].isVisible = true
                dayIVList[index].setImageResource(R.drawable.ic_quiz_daily_dia_day_checked_today)
                dayMarkList[index].isVisible = false
            }else if (byte.toInt() == 1){
                dayTopTvList[index].isEnabled = false
                dayGroupList[index].isEnabled = false
                dayTvList[index].isInvisible = true
                dayIVList[index].isInvisible = true
                dayCheckedList[index].isVisible = true
                dayMarkList[index].isVisible = false
            }else if (index < todayIndex) {
                dayMarkList[index].isVisible = true
                dayTvList[index].text = "+${list[index]}"
            }else {
                dayMarkList[index].isVisible = false
                dayTvList[index].text = "+${list[index]}"
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        SPTools.put(Constants.LAST_RECEIVED_BOX_TIME, System.currentTimeMillis())
        dismissCallBack?.invoke(toDayGem)
    }

}