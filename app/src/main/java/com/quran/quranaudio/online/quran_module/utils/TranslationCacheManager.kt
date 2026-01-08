package com.quran.quranaudio.online.quran_module.utils

import android.content.Context
import android.util.Log
import com.quran.quranaudio.online.quran_module.api.RetrofitInstance
import com.quran.quranaudio.online.quran_module.models.QuranTranslationVersion
import com.quran.quranaudio.online.quran_module.utils.reader.TranslUtils
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * 古兰经翻译版本缓存管理器（优化版）
 * 
 * 🚀 性能优化策略：
 * 1. 优先级加载：仅预加载用户当前语言
 * 2. 延迟加载：其他语言延迟5秒后加载
 * 3. 并发控制：限制并发数为2，使用低优先级线程
 * 4. IdleHandler：在主线程空闲时加载
 * 
 * 功能：
 * 1. 应用启动时优先加载用户语言的翻译列表
 * 2. 延迟加载其他语言，避免启动高峰
 * 3. 缓存到内存，避免用户在引导页等待API加载
 * 4. 支持手动刷新缓存
 * 
 * 使用场景：
 * - 新用户引导流程
 * - 翻译版本选择页面
 */
object TranslationCacheManager {
    
    private const val TAG = "TranslationCache"
    
    // 低优先级线程池（并发限制为2）
    private val lowPriorityExecutor = Executors.newFixedThreadPool(2) { r ->
        Thread(r).apply {
            priority = Thread.MIN_PRIORITY
            name = "TranslationCache-LowPriority"
        }
    }
    
    // 协程作用域（使用低优先级调度器）
    private val scope = CoroutineScope(SupervisorJob() + lowPriorityExecutor.asCoroutineDispatcher())
    
    // 内存缓存：语言代码 -> 翻译版本列表
    private val cache = mutableMapOf<String, List<QuranTranslationVersion>>()
    
    // 缓存加载状态
    @Volatile
    private var isCurrentLanguageLoaded = false
    
    @Volatile
    private var isAllLanguagesLoaded = false
    
    // 支持的语言列表（与 SPAppConfigs 保持一致）
    private val SUPPORTED_LANGUAGES = listOf("en", "id", "ar", "ur", "ms", "tr", "bn")
    
    /**
     * 🚀 优先级预加载：仅预加载用户当前语言（第一优先级）
     * 在应用启动时立即调用，或在用户切换语言后调用
     * 
     * @param context Context
     * @param forceRefresh 是否强制刷新（用于语言切换场景）
     */
    fun preloadCurrentLanguage(context: Context, forceRefresh: Boolean = false) {
        val currentLanguage = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(context) ?: "en"
        
        // 检查缓存是否已存在
        val isCached = synchronized(cache) {
            cache.containsKey(currentLanguage) && cache[currentLanguage]?.isNotEmpty() == true
        }
        
        if (!forceRefresh && isCached) {
            Log.d(TAG, "📦 当前语言已缓存，跳过: $currentLanguage")
            return
        }
        
        Log.d(TAG, "🚀 [PRIORITY-1] 预加载当前语言: $currentLanguage (forceRefresh=$forceRefresh)")
        
        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                val versions = loadTranslationsForLanguage(context, currentLanguage)
                
                synchronized(cache) {
                    cache[currentLanguage] = versions
                }
                
                isCurrentLanguageLoaded = true
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ [PRIORITY-1] 当前语言加载完成: $currentLanguage (${versions.size} 个版本) [$duration ms]")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ [PRIORITY-1] 当前语言加载失败: $currentLanguage", e)
            }
        }
    }
    
    /**
     * 📦 延迟预加载其他语言（第二优先级）
     * 在主界面显示5秒后调用，或使用 IdleHandler 在主线程空闲时调用
     */
    fun preloadOtherLanguages(context: Context) {
        if (isAllLanguagesLoaded) {
            Log.d(TAG, "📦 所有语言已加载，跳过")
            return
        }
        
        val currentLanguage = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(context) ?: "en"
        val otherLanguages = SUPPORTED_LANGUAGES.filter { it != currentLanguage }
        
        Log.d(TAG, "🟡 [PRIORITY-2] 开始延迟加载其他语言: $otherLanguages")
        
        scope.launch {
            try {
                val startTime = System.currentTimeMillis()
                
                // 限制并发数为2，避免资源争用
                val chunked = otherLanguages.chunked(2)
                
                for (chunk in chunked) {
                    val jobs = chunk.map { languageCode ->
                        async {
                            try {
                                val versions = loadTranslationsForLanguage(context, languageCode)
                                synchronized(cache) {
                                    cache[languageCode] = versions
                                }
                                Log.d(TAG, "  ✅ $languageCode: ${versions.size} 个版本")
                            } catch (e: Exception) {
                                Log.e(TAG, "  ❌ $languageCode 加载失败: ${e.message}")
                            }
                        }
                    }
                    
                    jobs.awaitAll()
                }
                
                isAllLanguagesLoaded = true
                val duration = System.currentTimeMillis() - startTime
                Log.d(TAG, "✅ [PRIORITY-2] 其他语言加载完成！总共缓存了 ${cache.size} 种语言 [$duration ms]")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ [PRIORITY-2] 其他语言加载失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 📦 预加载所有语言的翻译列表（已弃用，保留兼容性）
     * @deprecated 使用 preloadCurrentLanguage() + preloadOtherLanguages() 替代
     */
    @Deprecated("使用 preloadCurrentLanguage() + preloadOtherLanguages() 获得更好的性能")
    fun preloadAllTranslations(context: Context) {
        if (isAllLanguagesLoaded) {
            Log.d(TAG, "📦 缓存已加载，跳过预加载")
            return
        }
        
        Log.d(TAG, "🚀 开始预加载所有语言的古兰经翻译版本...")
        
        scope.launch {
            try {
                // 并行加载所有语言
                val jobs = SUPPORTED_LANGUAGES.map { languageCode ->
                    async {
                        try {
                            val versions = loadTranslationsForLanguage(context, languageCode)
                            synchronized(cache) {
                                cache[languageCode] = versions
                            }
                            Log.d(TAG, "  ✅ $languageCode: ${versions.size} 个版本")
                        } catch (e: Exception) {
                            Log.e(TAG, "  ❌ $languageCode 加载失败: ${e.message}")
                        }
                    }
                }
                
                jobs.awaitAll()
                
                isAllLanguagesLoaded = true
                Log.d(TAG, "✅ 预加载完成！总共缓存了 ${cache.size} 种语言的翻译版本")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 预加载失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 获取指定语言的翻译列表（优先从缓存）
     * 
     * @param languageCode 语言代码
     * @param forceRefresh 是否强制刷新（忽略缓存）
     * @return 翻译版本列表，如果缓存未加载完成则返回 null
     */
    fun getTranslations(
        context: Context,
        languageCode: String,
        forceRefresh: Boolean = false
    ): List<QuranTranslationVersion>? {
        if (forceRefresh) {
            Log.d(TAG, "🔄 强制刷新 $languageCode 的翻译列表")
            return null
        }
        
        synchronized(cache) {
            val cached = cache[languageCode]
            if (cached != null) {
                Log.d(TAG, "✅ 从缓存加载 $languageCode: ${cached.size} 个版本")
                return cached
            }
        }
        
        Log.d(TAG, "⚠️ $languageCode 缓存未命中")
        return null
    }
    
    /**
     * 清空缓存
     */
    fun clearCache() {
        synchronized(cache) {
            cache.clear()
        }
        isCurrentLanguageLoaded = false
        isAllLanguagesLoaded = false
        Log.d(TAG, "🗑️ 缓存已清空")
    }
    
    /**
     * 加载单个语言的翻译列表
     */
    private suspend fun loadTranslationsForLanguage(
        context: Context,
        languageCode: String
    ): List<QuranTranslationVersion> = withContext(Dispatchers.IO) {
        val versions = mutableListOf<QuranTranslationVersion>()
        
        try {
            // 1. 从主API加载
            val normalizedLangCode = when (languageCode) {
                "id" -> "in"  // 印尼语：应用用 id，API 用 in
                else -> languageCode
            }
            
            val responseBody = RetrofitInstance.github.getAvailableTranslations()
            val jsonString = responseBody.string()
            val translationsObject = JSONObject(jsonString)
            
            // 解析对应语言的翻译
            if (translationsObject.has(normalizedLangCode)) {
                val languageArray = translationsObject.getJSONArray(normalizedLangCode)
                for (i in 0 until languageArray.length()) {
                    val translObj = languageArray.getJSONObject(i)
                    val slug = translObj.getString("slug")
                    versions.add(
                        QuranTranslationVersion(
                            versionId = slug,
                            displayName = translObj.getString("name"),
                            languageCode = languageCode,  // 使用应用内的语言代码
                            languageName = languageCode.uppercase(),
                            downloadPath = "apis/translations/$normalizedLangCode/$slug.json",
                            isPrebuilt = false
                        )
                    )
                }
            }
            
        } catch (e: Exception) {
            Log.w(TAG, "$languageCode 主API失败，尝试备用API: ${e.message}")
            
            // 2. 备用API
            try {
                val languageMap = mapOf(
                    "en" to "english",
                    "id" to "indonesian",
                    "ar" to "arabic",
                    "ur" to "urdu",
                    "ms" to "malay",
                    "tr" to "turkish",
                    "bn" to "bengali"
                )
                val apiLanguage = languageMap[languageCode] ?: "english"
                val responseBody = RetrofitInstance.quranFoundation.getTranslations(apiLanguage)
                val jsonString = responseBody.string()
                val rootObject = JSONObject(jsonString)
                val translationsArray = rootObject.getJSONArray("translations")
                
                for (i in 0 until translationsArray.length()) {
                    val translObj = translationsArray.getJSONObject(i)
                    val translId = translObj.getInt("id")
                    versions.add(
                        QuranTranslationVersion(
                            versionId = "${languageCode}_$translId",
                            displayName = translObj.getString("name"),
                            languageCode = languageCode,
                            languageName = apiLanguage.capitalize(),
                            downloadPath = "api/v4/quran/translations/$translId",
                            isPrebuilt = false,
                            isQuranFoundationApi = true
                        )
                    )
                }
            } catch (e2: Exception) {
                Log.e(TAG, "$languageCode 备用API也失败: ${e2.message}")
            }
        }
        
        // 3. 添加预装版本
        versions.addAll(getPrebuiltVersions(context, languageCode))
        
        versions
    }
    
    /**
     * 获取预装的翻译版本
     */
    private fun getPrebuiltVersions(
        context: Context,
        languageCode: String
    ): List<QuranTranslationVersion> {
        val prebuiltVersions = mutableListOf<QuranTranslationVersion>()
        
        when (languageCode) {
            "en" -> {
                // Sahih International
                prebuiltVersions.add(
                    createPrebuiltVersion(
                        TranslUtils.TRANSL_SLUG_EN_SAHIH_INTERNATIONAL,
                        "Sahih International",
                        "en"
                    )
                )
                // The Clear Quran
                prebuiltVersions.add(
                    createPrebuiltVersion(
                        TranslUtils.TRANSL_SLUG_EN_THE_CLEAR_QURAN,
                        "The Clear Quran (Dr. Mustafa Khattab)",
                        "en"
                    )
                )
            }
            "id" -> {
                // Kompleks Al Quran Raja Fahd
                prebuiltVersions.add(
                    createPrebuiltVersion(
                        TranslUtils.TRANSL_SLUG_IN,
                        "Kompleks Al Quran Raja Fahd",
                        "id"
                    )
                )
            }
            "ur" -> {
                // Junagarhi
                prebuiltVersions.add(
                    createPrebuiltVersion(
                        TranslUtils.TRANSL_SLUG_UR_JUNAGARHI,
                        "Junagarhi",
                        "ur"
                    )
                )
            }
        }
        
        return prebuiltVersions
    }
    
    /**
     * 创建预装版本对象
     */
    private fun createPrebuiltVersion(
        slug: String,
        name: String,
        langCode: String
    ): QuranTranslationVersion {
        return QuranTranslationVersion(
            versionId = slug,
            displayName = name,
            languageCode = langCode,
            languageName = langCode.uppercase(),
            downloadPath = null,  // 预装版本不需要下载
            isPrebuilt = true
        )
    }
}
