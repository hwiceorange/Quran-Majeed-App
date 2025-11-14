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
import org.json.JSONObject

/**
 * 古兰经翻译版本缓存管理器
 * 
 * 功能：
 * 1. 应用启动时预加载所有支持语言的翻译列表
 * 2. 缓存到内存，避免用户在引导页等待API加载
 * 3. 支持手动刷新缓存
 * 
 * 使用场景：
 * - 新用户引导流程
 * - 翻译版本选择页面
 */
object TranslationCacheManager {
    
    private const val TAG = "TranslationCacheManager"
    
    // 协程作用域
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // 内存缓存：语言代码 -> 翻译版本列表
    private val cache = mutableMapOf<String, List<QuranTranslationVersion>>()
    
    // 缓存是否已加载完成
    @Volatile
    private var isLoaded = false
    
    // 支持的语言列表（与 SPAppConfigs 保持一致）
    private val SUPPORTED_LANGUAGES = listOf("en", "id", "ar", "ur", "ms", "tr", "bn")
    
    /**
     * 预加载所有语言的翻译列表
     * 在应用启动时调用
     */
    fun preloadAllTranslations(context: Context) {
        if (isLoaded) {
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
                
                isLoaded = true
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
        isLoaded = false
        Log.d(TAG, "🗑️ 缓存已清空")
    }
    
    /**
     * 加载单个语言的翻译列表
     */
    private suspend fun loadTranslationsForLanguage(
        context: Context,
        languageCode: String
    ): List<QuranTranslationVersion> {
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
        
        return versions
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

