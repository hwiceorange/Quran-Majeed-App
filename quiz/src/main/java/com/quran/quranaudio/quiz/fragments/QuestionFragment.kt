package com.quran.quranaudio.quiz.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.quran.quranaudio.quiz.R
import com.bible.tools.ad.ExternalAdConfig
import com.bible.tools.ad.FunctionTag
import com.bible.tools.base.BaseBindingFragment
import com.bible.tools.base.CloudManager
import com.bible.tools.base.Constants
import com.bible.tools.extension.SPTools
import com.bible.tools.extension.daysBetween
import com.bible.tools.extension.getResColor
import com.bible.tools.extension.getResString
import com.bible.tools.extension.gone
import com.bible.tools.extension.hasInterAdByPool
import com.bible.tools.extension.hasRewardAdByPool
import com.bible.tools.extension.invisible
import com.bible.tools.extension.isValid
import com.bible.tools.extension.logd
import com.bible.tools.extension.reloadQuizInterstitial
import com.bible.tools.extension.reloadQuizRewardAd
import com.bible.tools.extension.reportClickEvent
import com.bible.tools.extension.reportEnterFunShowEvent
import com.bible.tools.extension.reportEvent
import com.bible.tools.extension.reportQuizLevelUp
import com.bible.tools.extension.setDrawableLeft
import com.bible.tools.extension.showGemAd
import com.bible.tools.extension.showInterAdByPoolNew
import com.bible.tools.extension.visible
import com.bible.tools.quiz.QuestionFail
import com.bible.tools.quiz.QuestionRevivalActivity
import com.bible.tools.quiz.QuestionViewModel
import com.bible.tools.quiz.QuizGemChange
import com.bible.tools.quiz.QuizGemManager
import com.bible.tools.quiz.QuizPropChange
import com.bible.tools.quiz.dialog.DailyRewardDialog
import com.bible.tools.quiz.dialog.FreeRewardDialog
import com.bible.tools.utils.AnimatorUtils
import com.bible.tools.utils.RxBus
import com.bible.tools.utils.Tasks
import com.bible.tools.utils.isDebug
import com.hydra.common.ad.AdConfig
import com.quran.quranaudio.quiz.QuestionBean
import com.quran.quranaudio.quiz.databinding.FragmentQuestionBinding
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class QuestionFragment :
    BaseBindingFragment<FragmentQuestionBinding>(FragmentQuestionBinding::inflate) {
        companion object{
            var isSelected=false
        }
    val TAG = "QuestionFragment"
    private var currentBean: QuestionBean? = null
    private var isSkipNextLevel = false

    // 倒计时25s
    private val STAY_TIME = 25f
    private var curStayTime = STAY_TIME

    // 每次加时时间
    private val PER_ADD_DELAY_TIME = 15f
    private var countValueAnimator: ValueAnimator? = null
    private val viewModel by viewModels<QuestionViewModel> {
        QuestionViewModel.Factory()
    }


    //校验并展示每日激励宝箱
    private fun checkAndShowDailyBox() {
        if (isShowDailyRewardDialog()) return
        val receivedReward = SPTools.getLong(Constants.LAST_RECEIVED_BOX_TIME, 0)
        val showDailyBox = System.currentTimeMillis().daysBetween(receivedReward) > 0
        binding.dailyBox.isVisible = showDailyBox
        if (showDailyBox) {
            reportEvent("daily_box_dialog", "show_quiz_reward", "auto")
            DailyRewardDialog().apply {
                dismissCallBack = { gem ->
                    timeStart()
                    binding.dailyBox.isVisible = false
                    if (gem > 0) {
                        addGemCountAnimator(gem)
                    }
                }
            }.show(childFragmentManager, DailyRewardDialog.TAG)
            timePause()
        }
    }

    //点击宝箱或者宝石不足弹窗
    private fun showDailyRewardDialog(): Boolean {
        val receivedReward = SPTools.getLong(Constants.LAST_RECEIVED_BOX_TIME, 0)
        if (System.currentTimeMillis().daysBetween(receivedReward) <= 0) {
            reportEvent("free_dialog", "show_quiz_reward")
            FreeRewardDialog().apply {
                dismissCallBack = {
                    timeStart()
                }
            }.show(childFragmentManager, FreeRewardDialog.TAG)
        } else {
            reportEvent("daily_box_dialog", "show_quiz_reward")
            DailyRewardDialog().apply {
                dismissCallBack = { gem ->
                    timeStart()
                    binding.dailyBox.isVisible = false
                    if (gem > 0) {
                        addGemCountAnimator(gem)
                    }
                }
            }.show(childFragmentManager, DailyRewardDialog.TAG)
        }
        timePause()
        return true
    }

    override fun initData() {
        super.initData()
        val isShowQuizUseInterAd = CloudManager.isShowQuizUseInterAd()
        if (!isShowQuizUseInterAd && !hasRewardAdByPool(ExternalAdConfig.AD_QUIZ_REWARD)) {
            activity?.reloadQuizRewardAd()
        } else if (isShowQuizUseInterAd && !hasInterAdByPool(ExternalAdConfig.AD_QUIZ_INTERS)) {
            activity?.reloadQuizInterstitial()
        }
        lifecycleScope.launch {
            viewModel.currentQuestionBean.collect {
                currentBean = it
                it?.run {
                    updateQuestionUI(this)
                    if (this@QuestionFragment.userVisibleHint && !isShowDailyRewardDialog()) {
                        countValueAnimator?.start()
                    }
                }
            }
        }
        binding.correctLav.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if (activity.isValid()) {
                    binding.correctLav.gone()
                }
            }
        })
        binding.optionsView.setAnswerResultListener { isRight, _ ->
            timePause()
            setAnswerResultStyle(isRight)
            if (isRight) {
                binding.correctLav.visible()
                binding.correctLav.playAnimation()
                Tasks.postDelayedByUI({
                    if (viewModel.isLastQuestionInLevel()) {
                        // todo Notification
                       // NormalMainNotifyManager.updateQuizDone()
                        val currentLevel = SPTools.getInt(Constants.KEY_LAST_QUESTION_LEVEL, 1)
                        reportQuizLevelUp(currentLevel)
                        binding.quizNsv.gone()
                        binding.levelThoughtCl.root.visible()
                        binding.levelThoughtCl.levelThoughtLav.playAnimation()
                    } else {
                        viewModel.showNextQuestion()
                    }
                }, 2000)
            } else {
                Tasks.postDelayedByUI({ countValueAnimator?.cancel() }, 1000)
            }
        }
        binding.levelThoughtCl.nextLevelTv.setOnClickListener {
            reportClickEvent("quiz_next_level")
            requireActivity().showInterAdByPoolNew(
                AdConfig.AD_INTERS,
                FunctionTag.QUESTION_NEXT_LEVEL,
                0,
                {
                    reportEnterFunShowEvent(
                        pageName,
                        fromPageName,
                        it,
                        FunctionTag.QUESTION_NEXT_LEVEL
                    )
                },
                {
                    isSkipNextLevel = false
                    viewModel.intoNextLevel()
                })
        }
        binding.levelThoughtCl.nextLevelAd.setOnClickListener {
            reportClickEvent("quiz_next_ad")
            activity?.showGemAd(
                FunctionTag.QUESTION_NEXT_INTERS,
                FunctionTag.QUESTION_NEXT_REWARD,
                QuizGemManager.WATCH_AD_REWARD,
                pageName,
                fromPageName
            ) {
                isSkipNextLevel = false
                viewModel.intoNextLevel()
            }
        }

        binding.dailyBox.setOnClickListener {
            reportClickEvent("quiz_daily_box")
            showDailyRewardDialog()
        }

        RxBus.INSTANCE().register(this, QuestionFail::class.java) {
            if (it.failStatus == QuestionFail.SKIP_QUESTION) {
                if (viewModel.isLastQuestionInLevel()) {
                    val currentLevel = SPTools.getInt(Constants.KEY_LAST_QUESTION_LEVEL, 1)
                    reportQuizLevelUp(currentLevel)
                    isSkipNextLevel = true
                    binding.quizNsv.gone()
                    binding.levelThoughtCl.root.visible()
                } else {
                    viewModel.showNextQuestion()
                }
                return@register
            }
            if (it.failStatus == QuestionFail.TRY_AGAIN) {
                currentBean?.run { updateQuestionUI(this) }
                return@register
            }
            if (it.failStatus == QuestionFail.QUIT_LEVEL) {
                viewModel.tryAgainQuestion()
            }
        }
        binding.quizGemCountTv.setDrawableLeft(
            R.drawable.bible_quiz_reward_gem_small,
            R.dimen.dp_24
        )

        binding.levelThoughtCl.nextLevelReward.setDrawableLeft(
            R.drawable.bible_quiz_reward_gem_small,
            R.dimen.dp_24
        )

        RxBus.INSTANCE().register(this, QuizGemChange::class.java) {
            binding.quizGemCountTv.text = QuizGemManager.getGemCount().toString()
        }

        RxBus.INSTANCE().register(this, QuizPropChange::class.java) {
            updatePropCount()
        }
        binding.timeCountPb.post {
            ensureStatusBarHeight()
        }
        viewModel.initLevel()
        setBottomPropUI()
//        binding.adGemAnimatorIv.isVisible = false
    }

    private fun ensureStatusBarHeight() {
        if (binding.statusBarPlaceHolder.height > 0) return
        val location = IntArray(2)
        binding.timeCountPb.getLocationOnScreen(location)
        binding.statusBarPlaceHolder.layoutParams.height = location[1]
    }

    @SuppressLint("SetTextI18n")
    private fun setBottomPropUI() {
        binding.watchAdRewardTipsTv.setDrawableLeft(
            R.drawable.bible_quiz_reward_gem_small,
            R.dimen.dp_20
        )
        binding.addTimeTipsTv.setDrawableLeft(
            R.drawable.bible_quiz_reward_gem_small,
            R.dimen.dp_20
        )
        binding.hideOptionTv.setDrawableLeft(
            R.drawable.bible_quiz_reward_gem_small,
            R.dimen.dp_20
        )
        binding.addTimeTipsTv.text = "${QuizGemManager.ADD_TIME}"
        binding.hideOptionTv.text = "${QuizGemManager.HIDE_ERROR_OPTION}"
        binding.watchAdRewardTipsTv.text = "+${QuizGemManager.WATCH_AD_REWARD}"
        binding.hideOptionAreaView.setOnClickListener {
            reportClickEvent("quiz_hide_option", QuizGemManager.getGemCount().toString())
            binding.optionsView.hideOption {
                showDailyRewardDialog()
            }
        }

        binding.addTimeAreaView.setOnClickListener {
            reportClickEvent("quiz_add_time", QuizGemManager.getGemCount().toString())
            if (curStayTime + PER_ADD_DELAY_TIME > 999) {
                logd("out max delay time：999")
                return@setOnClickListener
            }
            if (QuizGemManager.isEnableAddTimeProp()) {
                QuizGemManager.consumeAddTimeOptionPropCount()
                doAddTime()
                return@setOnClickListener
            }
            if (QuizGemManager.consumeCount(QuizGemManager.ADD_TIME)) {
                doAddTime()
            } else {
                showDailyRewardDialog()
            }
        }

        binding.watchAdAreaView.setOnClickListener {
            activity?.showGemAd(
                FunctionTag.QUESTION_MORE_INTERS,
                FunctionTag.QUESTION_MORE_REWARD,
                QuizGemManager.WATCH_AD_REWARD,
                pageName,
                fromPageName
            )
        }
        updatePropCount()
    }

    @SuppressLint("SetTextI18n")
    private fun doAddTime() {
        ensureStatusBarHeight()
        binding.lightTv.alpha = 0f
        binding.lightTv.text = "+${PER_ADD_DELAY_TIME.toInt()}"
        binding.lightCl.isVisible = true
        countValueAnimator?.pause()
        ValueAnimator.ofFloat(0f, 1f, 1f, 0f).apply {
            addUpdateListener {
                val fl = it.animatedValue as Float
                binding.lightTv.alpha = fl
            }
            duration = 2000
        }.start()
        ObjectAnimator.ofFloat(binding.lightTv, View.SCALE_X, 0.75f, 1f, 1.05f, 1f).apply {
            duration = 2000
        }.start()
        ObjectAnimator.ofFloat(binding.lightTv, View.SCALE_Y, 0.75f, 1f, 1.05f, 1f).apply {
            duration = 2000
        }.start()
        Tasks.postDelayedByUI({
            binding.timeCountTv.text = "${(curStayTime + PER_ADD_DELAY_TIME).toInt()}"
            binding.timeCountPb.setPercentage(100f)
        }, 1000)

        Tasks.postDelayedByUI({
            binding.lightCl.isVisible = false
            countValueAnimator = getQuizCountTimeAnimator(curStayTime + PER_ADD_DELAY_TIME)
            countValueAnimator?.start()
        }, 2100)
    }

    private fun addGemCountAnimator(gemCount: Int) {
        AnimatorUtils.animatorScale(binding.adGemAnimatorIv, 1f, 1.5f, 0.7f, duration = 500)
        AnimatorUtils.animatorTranslateByCenter(
            binding.adGemAnimatorIv,
            binding.quizGemCountTv,
            1000,
            500
        ) {
            binding.adGemAnimatorIv.visibility = View.INVISIBLE
            reportEvent("daily_box", "quiz_reward_gem", "$gemCount")
            QuizGemManager.addCount(gemCount)
        }
    }

    private fun updatePropCount() {
        binding.hideOptionPropCountTv.isVisible = QuizGemManager.isEnableHideProp()
        binding.hideOptionPropCountTv.text = "${QuizGemManager.getHidePropCount()}"
        binding.addTimePropCountTv.isVisible = QuizGemManager.isEnableAddTimeProp()
        binding.addTimePropCountTv.text = "${QuizGemManager.getAddTimePropCount()}"
    }

    private fun setAnswerResultStyle(isRight: Boolean) {
        binding.answerResultTv.visible()
        binding.questionContentTv.invisible()
        if (isRight) {
            binding.answerResultTv.text = R.string.bible_correct.getResString()
            binding.answerResultTv.setTextColor(R.color.color_0BC9B2.getResColor())
        } else {
            binding.answerResultTv.text = R.string.bible_wrong.getResString()
            binding.answerResultTv.setTextColor(R.color.color_FF567F.getResColor())
        }
    }

    private fun updateQuestionUI(questionBean: QuestionBean) {
        countValueAnimator?.pause()
        countValueAnimator = getQuizCountTimeAnimator(STAY_TIME)
        binding.levelThoughtCl.levelThoughtLav.cancelAnimation()
        binding.answerResultTv.invisible()
        binding.questionContentTv.visible()
        binding.levelThoughtCl.root.gone()
        binding.quizNsv.visible()
        val currentLevel = SPTools.getInt(Constants.KEY_LAST_QUESTION_LEVEL, 1)
        binding.levelTv.text = R.string.bible_level.getResString(currentLevel.toString())
        binding.levelThoughtCl.nextLevelTv.text =
            R.string.bible_level.getResString((currentLevel + 1).toString())
        binding.questionContentTv.text = questionBean.question
        binding.questionProgressTv.text = viewModel.getProgressQuestion()
        binding.optionsView.setData(questionBean)
        if (isDebug()) {
            binding.debugRightAnswerTv.text = questionBean.answer
        }
    }

    private fun getQuizCountTimeAnimator(playTime: Float): ValueAnimator {
        return ValueAnimator.ofFloat(0f, playTime).apply {
            addUpdateListener {
                val v = it.animatedValue as Float
                curStayTime = playTime - v
                binding.timeCountPb.setPercentage((1 - (v / playTime)) * 100)
                binding.timeCountTv.text = "${(playTime - v).toInt()}"
            }
            doOnEnd {
                if (context != null && activity.isValid()) {
                    QuestionRevivalActivity.open(requireContext())
                }
            }
            duration = (playTime * 1000).toLong()
            interpolator = LinearInterpolator()
        }
    }

    override fun onResume() {
        super.onResume()
        isSelected=true
        if (isSelected && this.userVisibleHint && !isSkipNextLevel && !isShowDailyRewardDialog()) {
            timeStart()
        }
    }

    private fun isShowDailyRewardDialog(): Boolean {
        childFragmentManager.findFragmentByTag(DailyRewardDialog.TAG)?.let {
            it as DialogFragment
            if (it.dialog?.isShowing == true) return true
        }
        return false
    }

    override fun fragmentVisibleListener(visible: Boolean) {
        super.fragmentVisibleListener(visible)
        if (visible) {
            binding.quizGemCountTv.text = QuizGemManager.getGemCount().toString()
            checkAndShowDailyBox()
        }
        isSelected=visible
    }

    private fun timeStart() {
        if (countValueAnimator?.isPaused == true) {
            countValueAnimator?.resume()
        } else {
            countValueAnimator?.start()
        }
    }

    override fun onPause() {
        super.onPause()
        timePause()
    }

    private fun timePause() {
        if (countValueAnimator?.isStarted == true) {
            countValueAnimator?.pause()
        }
    }

    override fun getPageName(): String {
        return "QuestionFragment"
    }

    override fun getFromPageName(): String {
        return "Home"
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            timePause()
            isSelected=false
        } else {
            timeStart()
            isSelected=true
        }
    }
}