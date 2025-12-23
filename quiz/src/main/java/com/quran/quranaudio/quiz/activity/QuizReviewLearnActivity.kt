package com.quran.quranaudio.quiz.activity

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.quran.quranaudio.quiz.R
import com.quran.quranaudio.quiz.QuestionBean
import com.quran.quranaudio.quiz.ad.FunctionTag
import com.quran.quranaudio.quiz.base.BaseBindingActivity
import com.quran.quranaudio.quiz.databinding.ActivityQuizReviewLearnBinding
import com.quran.quranaudio.quiz.extension.reportClickEvent
import com.quran.quranaudio.quiz.extension.reportExitFunShowEvent
import com.quran.quranaudio.quiz.extension.hasRewardAdByPool
import com.quran.quranaudio.quiz.extension.showRewardAd
import com.quran.quranaudio.quiz.extension.reloadQuizRewardAd
import com.quran.quranaudio.quiz.ad.ExternalAdConfig
import com.quranaudio.quiz.quiz.QuestionFail
import com.quran.quranaudio.quiz.utils.RxBus
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.ToastUtils
import kotlinx.coroutines.launch

/**
 * Review & Learn Activity
 * 显示错误题目的结果页，提供学习内容和重试机会
 */
class QuizReviewLearnActivity :
    BaseBindingActivity<ActivityQuizReviewLearnBinding>(ActivityQuizReviewLearnBinding::inflate) {

    private var currentQuestion: QuestionBean? = null
    private var ayahId: Int = 0
    private var isRewardAdLoaded = false
    private var pendingAction: PendingAction = PendingAction.NONE

    enum class PendingAction {
        NONE, TRY_AGAIN, SKIP
    }
    
    /**
     * 订阅页面启动器
     * 用于处理订阅完成后自动打开 Tafsir 页面
     */
    private val subscriptionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d(TAG, "🔙 Returned from subscription page, result code: ${result.resultCode}")
        
        // 检查订阅状态（无论result code如何）
        val isSubscribed = checkSubscriptionStatus()
        
        if (isSubscribed) {
            // 订阅成功，自动打开 Tafsir 页面
            android.util.Log.d(TAG, "✅ Subscription successful, opening Tafsir page...")
            val question = currentQuestion
            if (question != null) {
                openTafsirDetailPage(question.surah_id, question.ayah_id)
            }
        } else {
            android.util.Log.d(TAG, "ℹ️ Subscription not completed or cancelled")
        }
    }

    override fun initView() {
        super.initView()
        
        // Setup status bar
        BarUtils.setStatusBarLightMode(this, false)
        BarUtils.setStatusBarColor(this, Color.TRANSPARENT)
        
        // Get data from intent
        currentQuestion = intent.getParcelableExtra(KEY_QUESTION)
        ayahId = intent.getIntExtra(KEY_AYAH_ID, 0)
        
        if (currentQuestion == null) {
            finish()
            return
        }
        
        setupViews()
        setupClickListeners()
        preloadNativeAd()  // 🔥 新增：预加载原生广告（确保第一时间展示）
        preloadRewardedAd()
        
        // Handle back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleQuitLevel()
            }
        })
    }
    
    private fun setupViews() {
        val question = currentQuestion ?: return
        
        android.util.Log.d(TAG, "📋 Setting up views for question: ${question.id}")
        android.util.Log.d(TAG, "   📍 Surah: ${question.surah_id}, Ayah: ${question.ayah_id}")
        android.util.Log.d(TAG, "   ✅ Correct Answer: ${question.getRightAnswer()}")
        android.util.Log.d(TAG, "   📝 Explanation: ${question.explanation.take(50)}...")
        
        // Set correct answer
        binding.correctAnswerTv.text = question.getRightAnswer()
        
        // Set Arabic verse and translation
        loadVerseData(question.surah_id, question.ayah_id)
        
        // Set simplified tafsir (explanation)
        binding.tafsirBriefTv.text = if (question.explanation.isNotEmpty()) {
            question.explanation
        } else {
            getString(R.string.quiz_review_default_explanation)
        }
    }
    
    private fun setupClickListeners() {
        // Back button
        binding.backBtn.setOnClickListener {
            reportClickEvent("quiz_review_back")
            handleQuitLevel()
        }
        
        // Try Again button - Rewarded Ad
        binding.tryAgainBtn.setOnClickListener {
            reportClickEvent("quiz_review_try_again_click")
            handleTryAgainClick()
        }
        
        // Skip button - Rewarded Ad
        binding.skipBtn.setOnClickListener {
            reportClickEvent("quiz_review_skip_click")
            handleSkipClick()
        }
        
        // Quit Level button
        binding.quitBtn.setOnClickListener {
            reportClickEvent("quiz_review_quit")
            handleQuitLevel()
        }
        
        // Full Tafsir button - Check subscription and navigate
        binding.fullTafsirBtn.setOnClickListener {
            reportClickEvent("quiz_review_full_tafsir")
            handleFullTafsirClick()
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 🔥 移除重复调用（initView 中的 preloadNativeAd 已经处理）
        android.util.Log.d(TAG, "ℹ️ onResume: Native ad handled in initView")
    }
    
    /**
     * 🔥 预加载原生广告 - 页面打开时立即展示
     * 
     * 优化:
     * - 非付费用户每次都展示
     * - 优先使用缓存（快）
     * - 没有缓存动态加载（保证显示）
     * - 无时间间隔限制
     */
    private fun preloadNativeAd() {
        try {
            android.util.Log.d(TAG, "📡 Loading native ad...")
            
            // 🔥 使用自定义View的加载方法（已优化为自动加载）
            binding.nativeAdView.loadNativeAd(FunctionTag.NATIVE_QUIZ_REVIEW_LEARN)
            
            android.util.Log.d(TAG, "✅ Native ad load initiated")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to load native ad: ${e.message}", e)
        }
    }
    
    /**
     * 预加载激励广告
     */
    private fun preloadRewardedAd() {
        android.util.Log.d(TAG, "📡 Preloading reward ad...")
        
        // 使用quiz模块的广告加载扩展函数
        this.reloadQuizRewardAd()
        isRewardAdLoaded = true
    }
    
    /**
     * 处理Try Again点击 - 步骤三第4点
     * 用户点击加载并展示激励广告，完成后返回当前错误的题目重新作答
     */
    private fun handleTryAgainClick() {
        if (!hasRewardAdByPool(ExternalAdConfig.AD_QUIZ_REWARD)) {
            ToastUtils.showLong(R.string.quran_loading_ad)
            // 广告未加载，尝试重新加载
            preloadRewardedAd()
            return
        }
        
        // 展示激励广告
        this.showRewardAd(
            adPosition = ExternalAdConfig.AD_QUIZ_REWARD,
            functionTag = FunctionTag.QUIZ_REVIEW_TRY_AGAIN_REWARD,
            beforeShowCallbacks = {
                android.util.Log.d(TAG, "📺 Before show reward ad: $it")
                reportExitFunShowEvent(getPageName(), getFormPageName(), it, FunctionTag.QUIZ_REVIEW_TRY_AGAIN_REWARD, ExternalAdConfig.AD_QUIZ_REWARD)
            },
            callbacks = { success ->
                if (success) {
                    // 用户完成激励广告播放
                    android.util.Log.d(TAG, "🎁 Reward ad completed - Try Again")
                    reportClickEvent("quiz_review_try_again_success")
                    
                    // 🔧 使用 setResult 返回结果，避免 RxBus 被 Fragment 可见性检查拦截
                    val resultIntent = Intent().apply {
                        putExtra(RESULT_ACTION, ACTION_TRY_AGAIN)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    android.util.Log.e(TAG, "❌ Reward ad failed")
                    ToastUtils.showLong(R.string.quran_no_ad_tips)
                    // 重新加载广告
                    preloadRewardedAd()
                }
            },
            skipNewUserCheck = true  // Quiz模块不受新用户限制
        )
    }
    
    /**
     * 处理Skip点击 - 步骤三第4点
     * 用户完成激励广告后更新题目回答状态为正确，继续下一步
     */
    private fun handleSkipClick() {
        if (!hasRewardAdByPool(ExternalAdConfig.AD_QUIZ_REWARD)) {
            ToastUtils.showLong(R.string.quran_loading_ad)
            // 广告未加载，尝试重新加载
            preloadRewardedAd()
            return
        }
        
        // 展示激励广告
        this.showRewardAd(
            adPosition = ExternalAdConfig.AD_QUIZ_REWARD,
            functionTag = FunctionTag.QUIZ_REVIEW_SKIP_REWARD,
            beforeShowCallbacks = {
                android.util.Log.d(TAG, "📺 Before show reward ad: $it")
                reportExitFunShowEvent(getPageName(), getFormPageName(), it, FunctionTag.QUIZ_REVIEW_SKIP_REWARD, ExternalAdConfig.AD_QUIZ_REWARD)
            },
            callbacks = { success ->
                if (success) {
                    // 用户完成激励广告播放
                    android.util.Log.d(TAG, "🎁 Reward ad completed - Skip")
                    reportClickEvent("quiz_review_skip_success")
                    
                    // 🔧 使用 setResult 返回结果，避免 RxBus 被 Fragment 可见性检查拦截
                    val resultIntent = Intent().apply {
                        putExtra(RESULT_ACTION, ACTION_SKIP)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } else {
                    android.util.Log.e(TAG, "❌ Reward ad failed")
                    ToastUtils.showLong(R.string.quran_no_ad_tips)
                    // 重新加载广告
                    preloadRewardedAd()
                }
            },
            skipNewUserCheck = true  // Quiz模块不受新用户限制
        )
    }
    
    /**
     * 处理Quit Level - 步骤三第4点
     * 重新回到当前Lv的第一道题目
     */
    private fun handleQuitLevel() {
        // 🔧 使用 setResult 返回结果，避免 RxBus 被 Fragment 可见性检查拦截
        val resultIntent = Intent().apply {
            putExtra(RESULT_ACTION, ACTION_QUIT)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
    
    /**
     * 处理Full Tafsir点击 - 步骤三第3点
     * 检查订阅状态，未订阅跳转订阅页，已订阅显示详细注释页面
     */
    private fun handleFullTafsirClick() {
        val question = currentQuestion ?: return
        
        // 检查用户订阅状态
        val isSubscribed = checkSubscriptionStatus()
        
        if (isSubscribed) {
            // 已订阅用户，跳转到详细Tafsir页面
            android.util.Log.d(TAG, "✅ User is subscribed, opening Tafsir detail")
            // 🔧 不传递 tafsir_detailed（它是占位符），让 ActivityTafsir 根据应用语言自动选择
            openTafsirDetailPage(question.surah_id, question.ayah_id)
        } else {
            // 未订阅用户，跳转到订阅页面
            android.util.Log.d(TAG, "🔒 User not subscribed, opening subscription page")
            goToSubscriptionPage()
        }
    }
    
    /**
     * 检查用户订阅状态
     */
    private fun checkSubscriptionStatus(): Boolean {
        return try {
            // 使用SharedPreferences检查订阅状态（与adlib的SubscriptionChecker一致）
            val prefs = getSharedPreferences("subscription_prefs", Context.MODE_PRIVATE)
            val isSubscribed = prefs.getBoolean("is_subscribed", false)
            android.util.Log.d(TAG, "📊 Subscription status: $isSubscribed")
            isSubscribed
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error checking subscription status", e)
            false
        }
    }
    
    /**
     * 跳转到订阅页面
     * 使用 ActivityResultLauncher 来监听订阅完成事件
     */
    private fun goToSubscriptionPage() {
        try {
            val intent = Intent(this, Class.forName("com.quran.quranaudio.online.subscription.SubscriptionActivity"))
            android.util.Log.d(TAG, "🚀 Launching subscription page with result launcher...")
            subscriptionLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to open subscription page", e)
            ToastUtils.showShort("Premium feature - Subscribe to unlock")
        }
    }
    
    /**
     * 打开Tafsir详情页面
     * 直接复用经文模块的 ActivityTafsir，保持UI和功能一致
     * 包括订阅锁定逻辑、语言自动切换等
     * 
     * 🔧 不传递 tafsirKey，让 ActivityTafsir 根据应用语言自动选择合适的 Tafsir
     */
    private fun openTafsirDetailPage(surahId: Int, ayahId: Int) {
        try {
            val intent = Intent(this, Class.forName("com.quran.quranaudio.online.quran_module.activities.ActivityTafsir"))
            
            // 使用 ActivityTafsir 期望的参数键
            intent.putExtra("reader.chapter_no", surahId)  // Keys.READER_KEY_CHAPTER_NO
            intent.putExtra("reader.verse_no", ayahId)     // Keys.READER_KEY_VERSE_NO
            
            // 🔧 不传递 tafsirKey，让 ActivityTafsir 自动选择：
            // 1. 首先尝试用户保存的 Tafsir key（来自 SharedPreferences）
            // 2. 如果没有，TafsirUtils.getPreferredTafsirKey() 会根据应用语言自动选择
            // 3. 如果还是没有，TafsirManager 会显示引导对话框
            
            android.util.Log.d(TAG, "📖 Opening Tafsir for Surah:$surahId, Ayah:$ayahId (auto language selection)")
            startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Failed to open Tafsir detail", e)
            e.printStackTrace()
            ToastUtils.showShort("Failed to open Tafsir")
        }
    }
    
    /**
     * 加载经文数据 - 步骤三第2点
     * 从本地Quran数据库加载阿拉伯语经文和翻译
     * 经文数据在新用户安装时已下载并存储在本地，可以直接调用，实现即时、0延迟加载
     */
    private fun loadVerseData(surahId: Int, ayahId: Int) {
        // 立即显示章节引用（使用多语言格式化字符串）
        binding.verseReferenceTv.text = getString(R.string.quiz_verse_reference, surahId, ayahId)
        binding.verseReferenceTv.visibility = android.view.View.VISIBLE
        
        // 🔧 预加载当前及前后3条Verse的Tafsir，实现0延迟
        try {
            Class.forName("com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirPreloader")
                .getDeclaredMethod("preload", android.content.Context::class.java, Int::class.java, Int::class.java)
                .invoke(null, this, surahId, ayahId)
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Tafsir preload not available: ${e.message}")
        }
        
        lifecycleScope.launch {
            try {
                android.util.Log.d(TAG, "📖 Loading verse data for Surah:$surahId, Ayah:$ayahId")
                
                // 从本地加载经文数据（阿拉伯语 + 用户偏好翻译）
                val verseData = com.quran.quranaudio.quiz.utils.VerseLoaderHelper.loadVerse(
                    this@QuizReviewLearnActivity,
                    surahId,
                    ayahId
                )
                
                // 更新UI - 阿拉伯语经文
                binding.verseArabicTv.text = verseData.arabicText
                
                // 更新UI - 翻译文本
                if (verseData.translationText.isNotEmpty()) {
                    binding.verseTranslationTv.text = verseData.translationText
                    binding.verseTranslationTv.visibility = android.view.View.VISIBLE
                } else {
                    // 如果没有翻译，显示提示
                    binding.verseTranslationTv.text = "Translation not available"
                    binding.verseTranslationTv.visibility = android.view.View.VISIBLE
                    android.util.Log.w(TAG, "⚠️ No translation available for this verse")
                }
                
                android.util.Log.d(TAG, "✅ Verse data loaded successfully")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "❌ Exception while loading verse data", e)
                e.printStackTrace()
                // 异常情况，显示默认文本
                binding.verseArabicTv.text = "قُلْ هُوَ ٱللَّهُ أَحَدٌ"
                binding.verseTranslationTv.text = "Error loading verse. Please try again."
                binding.verseTranslationTv.visibility = android.view.View.VISIBLE
            }
        }
    }
    
    override fun getPageName(): String {
        return "quiz_review_learn"
    }
    
    override fun getFormPageName(): String {
        return "quiz"
    }
    
    companion object {
        private const val TAG = "QuizReviewLearn"
        private const val KEY_QUESTION = "key_question"
        private const val KEY_AYAH_ID = "key_ayah_id"
        private const val KEY_QUESTION_ID = "key_question_id"
        
        // Result keys
        const val RESULT_ACTION = "result_action"
        const val ACTION_TRY_AGAIN = "try_again"
        const val ACTION_SKIP = "skip"
        const val ACTION_QUIT = "quit"
        
        /**
         * Open Review & Learn activity
         * @param context Context
         * @param question Current question bean
         * @param ayahId Ayah ID for the question group
         */
        fun open(context: Context, question: QuestionBean, ayahId: Int) {
            val intent = Intent(context, QuizReviewLearnActivity::class.java).apply {
                putExtra(KEY_QUESTION, question)
                putExtra(KEY_AYAH_ID, ayahId)
                putExtra(KEY_QUESTION_ID, question.id)
            }
            context.startActivity(intent)
        }
    }
}
