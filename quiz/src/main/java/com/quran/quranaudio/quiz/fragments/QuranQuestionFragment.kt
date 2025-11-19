package com.quran.quranaudio.quiz.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.ad.ExternalAdConfig
import com.quran.quranaudio.quiz.ad.FunctionTag
import com.quran.quranaudio.quiz.base.BaseBindingFragment
import com.quran.quranaudio.quiz.base.CloudManager
import com.quran.quranaudio.quiz.base.Constants
import com.quran.quranaudio.quiz.extension.SPTools
import com.quran.quranaudio.quiz.extension.daysBetween
import com.quran.quranaudio.quiz.extension.getResColor
import com.quran.quranaudio.quiz.extension.getResString
import com.quran.quranaudio.quiz.extension.gone
import com.quran.quranaudio.quiz.extension.hasInterAdByPool
import com.quran.quranaudio.quiz.extension.hasRewardAdByPool
import com.quran.quranaudio.quiz.extension.invisible
import com.quran.quranaudio.quiz.extension.isValid
import com.quran.quranaudio.quiz.extension.logd
import com.quran.quranaudio.quiz.extension.reloadQuizInterstitial
import com.quran.quranaudio.quiz.extension.reloadQuizRewardAd
import com.quran.quranaudio.quiz.extension.reportClickEvent
import com.quran.quranaudio.quiz.extension.reportEnterFunShowEvent
import com.quran.quranaudio.quiz.extension.reportEvent
import com.quran.quranaudio.quiz.extension.reportQuizLevelUp
import com.quran.quranaudio.quiz.extension.setDrawableLeft
import com.quran.quranaudio.quiz.extension.showGemAd
import com.quran.quranaudio.quiz.extension.showInterAdByPoolNew
import com.quran.quranaudio.quiz.extension.visible
import com.quranaudio.quiz.quiz.QuestionFail
import com.quran.quranaudio.quiz.activity.QuizReviewLearnActivity
import com.quranaudio.quiz.quiz.QuestionViewModel
import com.quranaudio.quiz.quiz.QuizGemChange
import com.quranaudio.quiz.quiz.QuizGemManager
import com.quranaudio.quiz.quiz.QuizPropChange
import com.quranaudio.quiz.quiz.dialog.QuranDailyRewardDialog
import com.quranaudio.quiz.quiz.dialog.QuranFreeRewardDialog
import com.quran.quranaudio.quiz.utils.AnimatorUtils
import com.quran.quranaudio.quiz.utils.RxBus
import com.quran.quranaudio.quiz.utils.Tasks
import com.quran.quranaudio.quiz.utils.isDebug
import com.quran.quranaudio.quiz.utils.UserInfoUtils
import com.quran.quranaudio.quiz.utils.AppConfig
import com.quranaudio.common.ad.AdConfig
import com.quranaudio.common.ad.AdFactory
import com.quran.quranaudio.quiz.QuestionBean
import com.quran.quranaudio.quiz.databinding.FragmentQuestionBinding
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
class QuranQuestionFragment :
    BaseBindingFragment<FragmentQuestionBinding>(FragmentQuestionBinding::inflate) {
        companion object{
            var isSelected=false
        }
    val TAG = "QuestionFragment"
    private var currentBean: QuestionBean? = null
    private var isSkipNextLevel = false

    // 🔧 ActivityResultLauncher for QuizReviewLearnActivity
    private val reviewLearnLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val action = result.data?.getStringExtra(QuizReviewLearnActivity.RESULT_ACTION)
            android.util.Log.d(TAG, "📬 Received result from QuizReviewLearnActivity: action=$action")
            
            when (action) {
                QuizReviewLearnActivity.ACTION_TRY_AGAIN -> {
                    // 🔧 重置状态，重新显示当前题目
                    hasNavigatedToReview = false  // 重置标志位
                    currentBean?.run { updateQuestionUI(this) }
                }
                QuizReviewLearnActivity.ACTION_SKIP -> {
                    // 🔧 重置状态，跳过当前题目，继续下一题或升级
                    hasNavigatedToReview = false  // 重置标志位
                    if (viewModel.isLastQuestionInLevel()) {
                        val currentLevel = SPTools.getInt(Constants.KEY_LAST_QUESTION_LEVEL, 1)
                        reportQuizLevelUp(currentLevel)
                        isSkipNextLevel = true
                        binding.quizNsv.gone()
                        binding.levelThoughtCl.root.visible()
                    } else {
                        viewModel.showNextQuestion()
                    }
                }
                QuizReviewLearnActivity.ACTION_QUIT -> {
                    // 🔧 完全重置状态，返回第一题
                    hasNavigatedToReview = false  // 重置标志位
                    countValueAnimator?.cancel()   // 取消倒计时
                    
                    // 🔧 取消所有pending的打开错误页面任务
                    pendingReviewRunnable?.let { 
                        Tasks.cancelUITask(it)
                        pendingReviewRunnable = null
                        logd("📌 Cancelled pending review task on Quit")
                    }
                    
                    viewModel.tryAgainQuestion()   // 返回第一题
                }
            }
        }
    }

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
            QuranDailyRewardDialog().apply {
                dismissCallBack = { gem ->
                    timeStart()
                    binding.dailyBox.isVisible = false
                    if (gem > 0) {
                        addGemCountAnimator(gem)
                    }
                }
            }.show(childFragmentManager, QuranDailyRewardDialog.TAG)
            timePause()
        }
    }

    //点击宝箱或者宝石不足弹窗
    private fun showDailyRewardDialog(): Boolean {
        val receivedReward = SPTools.getLong(Constants.LAST_RECEIVED_BOX_TIME, 0)
        if (System.currentTimeMillis().daysBetween(receivedReward) <= 0) {
            reportEvent("free_dialog", "show_quiz_reward")
            QuranFreeRewardDialog().apply {
                dismissCallBack = {
                    timeStart()
                }
            }.show(childFragmentManager, QuranFreeRewardDialog.TAG)
        } else {
            reportEvent("daily_box_dialog", "show_quiz_reward")
            QuranDailyRewardDialog().apply {
                dismissCallBack = { gem ->
                    timeStart()
                    binding.dailyBox.isVisible = false
                    if (gem > 0) {
                        addGemCountAnimator(gem)
                    }
                }
            }.show(childFragmentManager, QuranDailyRewardDialog.TAG)
        }
        timePause()
        return true
    }

    override fun initData() {
        super.initData()
        val currentLevel = SPTools.getInt(Constants.KEY_LAST_QUESTION_LEVEL, 1)
        val isShowQuizUseInterAd = CloudManager.isShowQuizUseInterAd()
        
        // 🎯 插屏广告限制：Level 10+ 才请求插屏广告
        val shouldUseInterAd = isShowQuizUseInterAd && currentLevel >= 10
        
        if (!shouldUseInterAd && !hasRewardAdByPool(ExternalAdConfig.AD_QUIZ_REWARD)) {
            activity?.reloadQuizRewardAd()
            android.util.Log.d(TAG, "✅ Preloading reward ad (Level: $currentLevel)")
        } else if (shouldUseInterAd && !hasInterAdByPool(ExternalAdConfig.AD_QUIZ_INTERS)) {
            activity?.reloadQuizInterstitial()
            android.util.Log.d(TAG, "✅ Preloading interstitial ad (Level: $currentLevel >= 10)")
        } else if (isShowQuizUseInterAd && currentLevel < 10) {
            android.util.Log.d(TAG, "🚫 Skipping interstitial ad - Level $currentLevel < 10")
        }
        lifecycleScope.launch {
            viewModel.currentQuestionBean.collect {
                currentBean = it
                it?.run {
                    updateQuestionUI(this)
                    if (this@QuranQuestionFragment.userVisibleHint && !isShowDailyRewardDialog()) {
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
                // 🔧 Step 1: Remove revival logic, navigate directly to Review & Learn page
                // Cancel countdown immediately
                countValueAnimator?.cancel()
                
                // 🔧 防止重复打开：如果已经打开了，就不再打开
                if (hasNavigatedToReview) {
                    logd("Answer incorrect but already navigated to Review page, skipping")
                    return@setAnswerResultListener
                }
                hasNavigatedToReview = true
                
                // 🔧 取消之前pending的任务（如果有）
                pendingReviewRunnable?.let { Tasks.cancelUITask(it) }
                
                // Navigate to Review & Learn activity with current question data
                pendingReviewRunnable = Runnable {
                    // 🔧 检查Fragment是否仍然可见
                    if (!isAdded || !isVisible || view == null || !userVisibleHint) {
                        logd("⚠️ Pending review task but Fragment not visible, skipping")
                        hasNavigatedToReview = false  // 重置标志，因为没有真正打开
                        pendingReviewRunnable = null
                        return@Runnable
                    }
                    
                    if (context != null && activity.isValid()) {
                        currentBean?.let { question ->
                            // 🔧 使用 launcher 启动，以便接收返回结果
                            val intent = Intent(requireContext(), QuizReviewLearnActivity::class.java).apply {
                                putExtra("key_question", question)
                                putExtra("key_ayah_id", question.ayah_id)
                                putExtra("key_question_id", question.id)
                            }
                            reviewLearnLauncher.launch(intent)
                            pendingReviewRunnable = null  // 清除引用
                        }
                    }
                }
                Tasks.postDelayedByUI(pendingReviewRunnable!!, 500) // Small delay to show wrong answer feedback
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
            // 🔧 双重保护: 只在 Fragment 可见且已添加时处理事件
            if (!isAdded || !isVisible || view == null || !userVisibleHint) {
                logd("QuestionFail event ignored: Fragment not visible")
                return@register
            }
            
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
            // 🔧 双重保护: 只在 Fragment 可见时更新 UI
            if (!isAdded || view == null) return@register
            binding.quizGemCountTv.text = QuizGemManager.getGemCount().toString()
        }

        RxBus.INSTANCE().register(this, QuizPropChange::class.java) {
            // 🔧 双重保护: 只在 Fragment 可见时更新 UI
            if (!isAdded || view == null) return@register
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
            binding.answerResultTv.text = R.string.quran_correct.getResString()
            binding.answerResultTv.setTextColor(R.color.color_0BC9B2.getResColor())
        } else {
            binding.answerResultTv.text = R.string.quran_wrong.getResString()
            binding.answerResultTv.setTextColor(R.color.color_FF567F.getResColor())
        }
    }

    private fun updateQuestionUI(questionBean: QuestionBean) {
        hasNavigatedToReview = false  // 🔧 重置标志，为新题目做准备
        
        // 🔧 取消之前pending的任务（切换到新题目时）
        pendingReviewRunnable?.let {
            Tasks.cancelUITask(it)
            pendingReviewRunnable = null
            logd("📌 Cancelled pending review task on updateQuestionUI")
        }
        
        countValueAnimator?.pause()
        countValueAnimator = getQuizCountTimeAnimator(STAY_TIME)
        binding.levelThoughtCl.levelThoughtLav.cancelAnimation()
        binding.answerResultTv.invisible()
        binding.questionContentTv.visible()
        binding.levelThoughtCl.root.gone()
        binding.quizNsv.visible()
        val currentLevel = SPTools.getInt(Constants.KEY_LAST_QUESTION_LEVEL, 1)
        binding.levelTv.text = R.string.quran_level.getResString(currentLevel.toString())
        binding.levelThoughtCl.nextLevelTv.text =
            R.string.quran_level.getResString((currentLevel + 1).toString())
        binding.questionContentTv.text = questionBean.question
        binding.questionProgressTv.text = viewModel.getProgressQuestion()
        binding.optionsView.setData(questionBean)
        if (isDebug()) {
            // 显示随机化后的正确答案
            binding.debugRightAnswerTv.text = binding.optionsView.getShuffledAnswer()
        }
    }

    private var hasNavigatedToReview = false  // 防止重复打开Review页面
    private var pendingReviewRunnable: Runnable? = null  // 🔧 保存pending的打开错误页面任务

    private fun getQuizCountTimeAnimator(playTime: Float): ValueAnimator {
        return ValueAnimator.ofFloat(0f, playTime).apply {
            addUpdateListener {
                val v = it.animatedValue as Float
                curStayTime = playTime - v
                binding.timeCountPb.setPercentage((1 - (v / playTime)) * 100)
                binding.timeCountTv.text = "${(playTime - v).toInt()}"
            }
            doOnEnd {
                // 🔧 防止重复打开：如果已经因为回答错误打开了，就不再打开
                if (hasNavigatedToReview) {
                    logd("Countdown ended but already navigated to Review page, skipping")
                    return@doOnEnd
                }
                
                // 🔧 检查Fragment是否可见：如果用户已经切换到其他页面，不打开错误页
                if (!isAdded || !isVisible || view == null || !userVisibleHint) {
                    logd("⚠️ Countdown ended but Fragment not visible, skipping Review page")
                    return@doOnEnd
                }
                
                // If countdown finishes without answer, treat as wrong answer
                // Navigate directly to Review & Learn page
                if (context != null && activity.isValid()) {
                    currentBean?.let { question ->
                        hasNavigatedToReview = true
                        // 🔧 使用 launcher 启动，以便接收返回结果
                        val intent = Intent(requireContext(), QuizReviewLearnActivity::class.java).apply {
                            putExtra("key_question", question)
                            putExtra("key_ayah_id", question.ayah_id)
                            putExtra("key_question_id", question.id)
                        }
                        reviewLearnLauncher.launch(intent)
                    }
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
        childFragmentManager.findFragmentByTag(QuranDailyRewardDialog.TAG)?.let {
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
        isSelected = false  // 🔧 离开页面时重置状态，防止误触发
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
        } else {
            timeStart()
            isSelected=true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 🔧 重要修复: RxBus 会在 onDestroy 时自动解绑（LifecycleObserver）
        // 但我们仍需手动清理动画和状态，防止内存泄漏
        isSelected = false
        countValueAnimator?.cancel()
        countValueAnimator = null
    }
}