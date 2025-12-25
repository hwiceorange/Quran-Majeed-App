package com.quran.quranaudio.online.feedback

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.google.firebase.firestore.FirebaseFirestore
import com.quran.quranaudio.online.BuildConfig
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 反馈管理器 - 负责收集和提交用户反馈
 * 集成 Firebase Firestore，自动收集设备和应用状态信息
 * 
 * 用于诊断：
 * - 9.4% 次日留存率低的原因
 * - 52秒平均时长短的原因
 */
class FeedbackManager private constructor() {
    
    companion object {
        private const val TAG = "FeedbackManager"
        private const val COLLECTION_PATH = "user_feedback"
        private const val MAX_RETRIES = 5
        
        @Volatile
        private var instance: FeedbackManager? = null
        
        fun getInstance(): FeedbackManager {
            return instance ?: synchronized(this) {
                instance ?: FeedbackManager().also { instance = it }
            }
        }
    }
    
    private val firestore: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance()
    }
    
    // 应用启动时间（用于计算会话时长）
    private var appStartTime: Long = System.currentTimeMillis()
    
    // 当前页面名称（由各 Activity/Fragment 设置）
    private var currentPage: String = "Unknown"
    
    // 当前阅读进度（由阅读器页面设置）
    private var readingProgress: String? = null
    
    /**
     * 重置启动时间（新会话开始时调用）
     */
    fun resetStartTime() {
        appStartTime = System.currentTimeMillis()
    }
    
    /**
     * 设置当前页面
     */
    fun setCurrentPage(pageName: String) {
        currentPage = pageName
        Log.d(TAG, "Current page: $pageName")
    }
    
    /**
     * 设置阅读进度
     */
    fun setReadingProgress(surahId: Int, ayahId: Int) {
        readingProgress = "Surah $surahId, Ayah $ayahId"
        Log.d(TAG, "Reading progress: $readingProgress")
    }
    
    /**
     * 提交反馈
     * @param context 上下文
     * @param emotion 用户选择的情绪
     * @param selectedTags 用户选择的标签
     * @param comment 用户输入的评论（可选）
     * @param onSuccess 成功回调
     * @param onFailure 失败回调
     */
    fun submitFeedback(
        context: Context,
        emotion: FeedbackEmotion,
        selectedTags: List<String>,
        comment: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 收集设备信息
                val deviceInfo = collectDeviceInfo(context)
                
                // 收集应用状态
                val appState = collectAppState(context)
                
                // 创建反馈数据
                val feedbackData = FeedbackData(
                    emotion = emotion,
                    selectedTags = selectedTags,
                    comment = comment,
                    deviceInfo = deviceInfo,
                    appState = appState
                )
                
                // 提交到 Firestore（带重试）
                submitToFirestoreWithRetry(feedbackData, MAX_RETRIES)
                
                Log.d(TAG, "✅ Feedback submitted successfully")
                
                // 回调成功
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to submit feedback", e)
                
                // 回调失败
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }
    
    /**
     * 收集设备信息
     */
    private fun collectDeviceInfo(context: Context): DeviceInfo {
        return DeviceInfo(
            deviceName = getDeviceName(),
            systemVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            screenSize = getScreenSize(context),
            language = SPAppConfigs.getLocale(context)
        )
    }
    
    /**
     * 收集应用状态
     */
    private fun collectAppState(context: Context): AppState {
        val sessionDuration = (System.currentTimeMillis() - appStartTime) / 1000 // 秒
        
        val prefs = context.getSharedPreferences(PreferencesConstants.LOCATION, Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean(PreferencesConstants.FIRST_LAUNCH, true)
        
        return AppState(
            currentPage = currentPage,
            readingProgress = readingProgress,
            sessionDuration = sessionDuration,
            isFirstLaunch = isFirstLaunch
        )
    }
    
    /**
     * 提交到 Firestore（带指数退避重试）
     */
    private suspend fun submitToFirestoreWithRetry(data: FeedbackData, retriesLeft: Int) {
        try {
            val document = FeedbackDocument.fromFeedbackData(data)
            
            firestore.collection(COLLECTION_PATH)
                .add(document)
                .await()
                
            Log.d(TAG, "📤 Feedback document saved to Firestore")
            
        } catch (e: Exception) {
            if (retriesLeft > 0) {
                val delay = (2.0.pow((MAX_RETRIES - retriesLeft).toDouble()) * 1000).toLong()
                Log.w(TAG, "⚠️ Retry $retriesLeft left, waiting ${delay}ms...")
                
                kotlinx.coroutines.delay(delay)
                submitToFirestoreWithRetry(data, retriesLeft - 1)
            } else {
                throw e
            }
        }
    }
    
    /**
     * 获取设备名称
     */
    private fun getDeviceName(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.capitalize()
        } else {
            "${manufacturer.capitalize()} $model"
        }
    }
    
    /**
     * 获取屏幕尺寸
     */
    private fun getScreenSize(context: Context): String {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(metrics)
        
        val widthPixels = metrics.widthPixels
        val heightPixels = metrics.heightPixels
        val densityDpi = metrics.densityDpi
        
        // 计算屏幕对角线英寸
        val widthInches = widthPixels / densityDpi.toDouble()
        val heightInches = heightPixels / densityDpi.toDouble()
        val diagonalInches = sqrt(widthInches.pow(2) + heightInches.pow(2))
        
        return "${widthPixels}x${heightPixels} (%.1f\")".format(diagonalInches)
    }
    
    /**
     * 字符串首字母大写
     */
    private fun String.capitalize(): String {
        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

