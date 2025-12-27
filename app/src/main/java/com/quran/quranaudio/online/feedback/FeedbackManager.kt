package com.quran.quranaudio.online.feedback

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth
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
 * ✅ 支持匿名提交（无需用户登录）
 * ✅ 自动重试机制（指数退避）
 * ✅ 详细错误日志
 * 
 * 用于诊断：
 * - 9.4% 次日留存率低的原因
 * - 52秒平均时长短的原因
 */
class FeedbackManager private constructor() {
    
    companion object {
        private const val TAG = "FeedbackManager"
        private const val COLLECTION_PATH = "feedback_submissions"  // 更清晰的路径
        private const val MAX_RETRIES = 3  // 减少重试次数，加快失败反馈
        
        // 🆕 本地缓存相关常量
        private const val PREFS_NAME = "feedback_cache"
        private const val KEY_PENDING_FEEDBACKS = "pending_feedbacks"
        private const val MAX_CACHED_FEEDBACKS = 10  // 最多缓存10条失败反馈
        
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
    
    private val firebaseAuth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
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
     * 提交反馈（异步，用于乐观更新）
     * 
     * 🚀 特性：
     * - 不阻塞UI线程
     * - 失败时自动保存到本地缓存
     * - 适用于"乐观更新"场景
     * 
     * @param context 上下文
     * @param emotion 用户选择的情绪
     * @param selectedTags 用户选择的标签
     * @param comment 用户输入的评论（可选）
     * @param onSuccess 成功回调（后台执行）
     * @param onFailure 失败回调（后台执行）
     */
    fun submitFeedbackAsync(
        context: Context,
        emotion: FeedbackEmotion,
        selectedTags: List<String>,
        comment: String?,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        Log.d(TAG, "🚀 [Async] Starting background feedback submission")
        
        // 🔥 异步执行，不阻塞主线程
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Step 1: 确保 Firebase Auth 已就绪
                ensureFirebaseAuthReady()
                
                // Step 2: 收集设备和应用信息
                val deviceInfo = collectDeviceInfo(context)
                val appState = collectAppState(context)
                
                // Step 3: 创建反馈数据
                val feedbackData = FeedbackData(
                    emotion = emotion,
                    selectedTags = selectedTags,
                    comment = comment,
                    deviceInfo = deviceInfo,
                    appState = appState
                )
                
                Log.d(TAG, "📝 [Async] Feedback data prepared")
                
                // Step 4: 提交到 Firestore（带重试）
                submitToFirestoreWithRetry(context, feedbackData, MAX_RETRIES)
                
                Log.d(TAG, "✅ [Async] Feedback submitted successfully to Firebase")
                
                // 回调成功（在主线程）
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ [Async] Background submission failed: ${e.message}")
                e.printStackTrace()
                
                // 回调失败（在主线程）
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }
    
    /**
     * 提交反馈
     * 
     * ✅ 自动进行 Firebase 匿名登录
     * ✅ 带重试机制
     * ✅ 详细错误日志
     * 
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
        Log.d(TAG, "═══════════════════════════════════════════════")
        Log.d(TAG, "📤 Starting feedback submission")
        Log.d(TAG, "═══════════════════════════════════════════════")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ Step 1: 确保 Firebase Auth 已就绪（匿名登录）
                ensureFirebaseAuthReady()
                
                // ✅ Step 2: 收集设备信息
                Log.d(TAG, "→ Collecting device info...")
                val deviceInfo = collectDeviceInfo(context)
                Log.d(TAG, "✅ Device info collected: ${deviceInfo.deviceName}")
                
                // ✅ Step 3: 收集应用状态
                Log.d(TAG, "→ Collecting app state...")
                val appState = collectAppState(context)
                Log.d(TAG, "✅ App state collected: page=${appState.currentPage}, session=${appState.sessionDuration}s")
                
                // ✅ Step 4: 创建反馈数据
                val feedbackData = FeedbackData(
                    emotion = emotion,
                    selectedTags = selectedTags,
                    comment = comment,
                    deviceInfo = deviceInfo,
                    appState = appState
                )
                
                Log.d(TAG, "📝 Feedback data created")
                Log.d(TAG, "   Emotion: ${emotion.name}")
                Log.d(TAG, "   Tags: $selectedTags")
                Log.d(TAG, "   Comment: ${comment?.take(50) ?: "null"}")
                
                // ✅ Step 5: 提交到 Firestore（带重试）
                submitToFirestoreWithRetry(context, feedbackData, MAX_RETRIES)
                
                Log.d(TAG, "═══════════════════════════════════════════════")
                Log.d(TAG, "✅ Feedback submitted successfully")
                Log.d(TAG, "═══════════════════════════════════════════════")
                
                // 回调成功
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "═══════════════════════════════════════════════")
                Log.e(TAG, "❌ Failed to submit feedback")
                Log.e(TAG, "═══════════════════════════════════════════════")
                Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                Log.e(TAG, "Exception message: ${e.message}")
                e.printStackTrace()
                
                // 回调失败
                withContext(Dispatchers.Main) {
                    onFailure(e)
                }
            }
        }
    }
    
    /**
     * 确保 Firebase Auth 已就绪（匿名登录）
     * 
     * 重要：Firestore 需要认证才能写入数据
     * 使用匿名登录可以在不要求用户注册的情况下提交反馈
     */
    private suspend fun ensureFirebaseAuthReady() {
        Log.d(TAG, "🔐 Checking Firebase Auth status...")
        
        val currentUser = firebaseAuth.currentUser
        
        if (currentUser != null) {
            Log.d(TAG, "✅ Already signed in anonymously: ${currentUser.uid}")
            return
        }
        
        // 匿名登录
        Log.d(TAG, "→ Signing in anonymously...")
        try {
            val authResult = firebaseAuth.signInAnonymously().await()
            val user = authResult.user
            
            if (user != null) {
                Log.d(TAG, "✅ Anonymous sign-in successful")
                Log.d(TAG, "   User ID: ${user.uid}")
                Log.d(TAG, "   Is Anonymous: ${user.isAnonymous}")
            } else {
                throw Exception("Sign-in succeeded but user is null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Anonymous sign-in failed", e)
            throw Exception("Firebase Auth failed: ${e.message}", e)
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
     * 
     * 路径：feedback_submissions/{documentId}
     * 权限：需要 Firebase Auth（匿名登录即可）
     */
    private suspend fun submitToFirestoreWithRetry(context: Context, data: FeedbackData, retriesLeft: Int) {
        try {
            Log.d(TAG, "→ Preparing Firestore document...")
            val document = FeedbackDocument.fromFeedbackData(data, context)
            
            // 添加用户 ID（匿名用户的 UID）
            val userId = firebaseAuth.currentUser?.uid ?: "unknown"
            val documentData = hashMapOf(
                "userId" to userId,
                "emotion" to document.emotion,
                "selectedTags" to document.selectedTags,
                "comment" to document.comment,
                "deviceName" to document.deviceName,
                "systemVersion" to document.systemVersion,
                "appVersion" to document.appVersion,
                "screenSize" to document.screenSize,
                "language" to document.language,
                "currentPage" to document.currentPage,
                "readingProgress" to document.readingProgress,
                "sessionDuration" to document.sessionDuration,
                "isFirstLaunch" to document.isFirstLaunch,
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            
            Log.d(TAG, "→ Submitting to Firestore...")
            Log.d(TAG, "   Collection: $COLLECTION_PATH")
            Log.d(TAG, "   User ID: $userId")
            
            val docRef = firestore.collection(COLLECTION_PATH)
                .add(documentData)
                .await()
                
            Log.d(TAG, "✅ Document saved successfully")
            Log.d(TAG, "   Document ID: ${docRef.id}")
            Log.d(TAG, "   Collection Path: $COLLECTION_PATH/${docRef.id}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore write failed (retries left: $retriesLeft)")
            Log.e(TAG, "   Error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "   Error message: ${e.message}")
            
            if (retriesLeft > 0) {
                val delay = (2.0.pow((MAX_RETRIES - retriesLeft).toDouble()) * 1000).toLong()
                Log.w(TAG, "⏳ Retrying in ${delay}ms...")
                
                kotlinx.coroutines.delay(delay)
                submitToFirestoreWithRetry(context, data, retriesLeft - 1)
            } else {
                Log.e(TAG, "❌ All retries exhausted, giving up")
                throw Exception("Firestore submission failed after $MAX_RETRIES attempts: ${e.message}", e)
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
     * 保存失败的反馈到本地缓存
     * 
     * 💾 用途：
     * - 网络失败时保存数据
     * - 下次启动时自动重试
     * - 防止用户反馈丢失
     */
    fun savePendingFeedback(
        context: Context,
        emotion: FeedbackEmotion,
        selectedTags: List<String>,
        comment: String?
    ) {
        try {
            Log.d(TAG, "💾 Saving failed feedback to local cache...")
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // 读取现有缓存
            val existingJson = prefs.getString(KEY_PENDING_FEEDBACKS, "[]") ?: "[]"
            val existingList = org.json.JSONArray(existingJson)
            
            // 限制缓存数量，防止占用过多空间
            if (existingList.length() >= MAX_CACHED_FEEDBACKS) {
                Log.w(TAG, "⚠️ Cache limit reached ($MAX_CACHED_FEEDBACKS), removing oldest feedback")
                existingList.remove(0) // 移除最旧的
            }
            
            // 创建新的反馈条目
            val feedbackJson = org.json.JSONObject().apply {
                put("emotion", emotion.name)
                put("selectedTags", org.json.JSONArray(selectedTags))
                put("comment", comment ?: "")
                put("timestamp", System.currentTimeMillis())
                put("deviceName", getDeviceName())
                put("appVersion", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            }
            
            existingList.put(feedbackJson)
            
            // 保存到 SharedPreferences
            prefs.edit()
                .putString(KEY_PENDING_FEEDBACKS, existingList.toString())
                .apply()
            
            Log.d(TAG, "✅ Feedback saved to local cache")
            Log.d(TAG, "   Total cached feedbacks: ${existingList.length()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to save feedback to cache: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 重试提交本地缓存的反馈
     * 
     * 🔄 时机：
     * - 应用启动时调用
     * - 网络恢复时调用
     * - 用户再次打开反馈系统时调用
     */
    fun retryPendingFeedbacks(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val cachedJson = prefs.getString(KEY_PENDING_FEEDBACKS, "[]") ?: "[]"
                val cachedList = org.json.JSONArray(cachedJson)
                
                if (cachedList.length() == 0) {
                    Log.d(TAG, "ℹ️ No pending feedbacks in cache")
                    return@launch
                }
                
                Log.d(TAG, "🔄 Found ${cachedList.length()} pending feedbacks, retrying...")
                
                val successfulIndices = mutableListOf<Int>()
                
                for (i in 0 until cachedList.length()) {
                    val feedbackJson = cachedList.getJSONObject(i)
                    
                    try {
                        val emotionName = feedbackJson.getString("emotion")
                        val emotion = FeedbackEmotion.valueOf(emotionName)
                        
                        val tagsArray = feedbackJson.getJSONArray("selectedTags")
                        val tags = mutableListOf<String>()
                        for (j in 0 until tagsArray.length()) {
                            tags.add(tagsArray.getString(j))
                        }
                        
                        val comment = feedbackJson.optString("comment", null)
                        
                        Log.d(TAG, "→ Retrying feedback #${i+1}: ${emotion.name}")
                        
                        // 确保认证就绪
                        ensureFirebaseAuthReady()
                        
                        // 收集最新的设备和应用信息
                        val deviceInfo = collectDeviceInfo(context)
                        val appState = collectAppState(context)
                        
                        val feedbackData = FeedbackData(
                            emotion = emotion,
                            selectedTags = tags,
                            comment = if (comment.isNullOrEmpty()) null else comment,
                            deviceInfo = deviceInfo,
                            appState = appState
                        )
                        
                        // 提交（不重试，失败就算了）
                        submitToFirestoreWithRetry(context, feedbackData, 1)
                        
                        Log.d(TAG, "✅ Feedback #${i+1} submitted successfully")
                        successfulIndices.add(i)
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to retry feedback #${i+1}: ${e.message}")
                        // 继续处理下一个
                    }
                }
                
                // 移除成功提交的反馈
                if (successfulIndices.isNotEmpty()) {
                    Log.d(TAG, "🧹 Removing ${successfulIndices.size} successful feedbacks from cache...")
                    
                    val newList = org.json.JSONArray()
                    for (i in 0 until cachedList.length()) {
                        if (i !in successfulIndices) {
                            newList.put(cachedList.getJSONObject(i))
                        }
                    }
                    
                    prefs.edit()
                        .putString(KEY_PENDING_FEEDBACKS, newList.toString())
                        .apply()
                    
                    Log.d(TAG, "✅ Cache updated, remaining: ${newList.length()}")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error during pending feedbacks retry: ${e.message}")
                e.printStackTrace()
            }
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

