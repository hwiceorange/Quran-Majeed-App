package com.quran.quranaudio.online.quran_module.quiz

import android.content.Context
import android.util.Log
import com.quran.quranaudio.online.quran_module.components.quran.Quran
import com.quran.quranaudio.online.quran_module.components.quran.QuranMeta
import com.quran.quranaudio.online.quran_module.utils.reader.factory.QuranTranslationFactory
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader
import com.quran.quranaudio.quiz.data.QuranDataProvider
import com.quran.quranaudio.quiz.data.QuizVerseData

/**
 * 古兰经数据仓库实现 - 为Quiz模块提供稳定的数据访问
 * 
 * 职责：
 * 1. 实现 QuranDataProvider 接口
 * 2. 封装Quran和Translation的复杂性
 * 3. 处理数据初始化和错误情况
 */
class QuranDataRepositoryImpl private constructor(
    private val context: Context
) : QuranDataProvider {
    
    companion object {
        private const val TAG = "QuranDataRepository"
        
        @Volatile
        private var INSTANCE: QuranDataRepositoryImpl? = null
        
        /**
         * 获取单例实例
         */
        @JvmStatic
        fun getInstance(context: Context): QuranDataRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: QuranDataRepositoryImpl(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    private var quranRef: Quran? = null
    private var isInitializing = false
    private val initCallbacks = mutableListOf<() -> Unit>()
    
    override fun isQuranDataReady(): Boolean {
        return quranRef != null
    }
    
    override fun ensureQuranDataInitialized(onComplete: () -> Unit) {
        // 如果已经初始化，直接回调
        if (quranRef != null) {
            onComplete()
            return
        }
        
        // 添加到回调列表
        synchronized(initCallbacks) {
            initCallbacks.add(onComplete)
        }
        
        // 如果正在初始化，等待完成
        if (isInitializing) {
            return
        }
        
        // 开始初始化
        isInitializing = true
        Log.d(TAG, "🔄 Initializing Quran data...")
        
        QuranMeta.prepareInstance(context, object : com.quran.quranaudio.online.quran_module.interfaceUtils.OnResultReadyCallback<QuranMeta> {
            override fun onReady(quranMeta: QuranMeta) {
                Quran.prepareInstance(context, quranMeta, object : com.quran.quranaudio.online.quran_module.interfaceUtils.OnResultReadyCallback<Quran> {
                    override fun onReady(quran: Quran) {
                        quranRef = quran
                        isInitializing = false
                        Log.d(TAG, "✅ Quran data initialized successfully")
                        
                        // 执行所有回调
                        synchronized(initCallbacks) {
                            initCallbacks.forEach { it() }
                            initCallbacks.clear()
                        }
                    }
                })
            }
        })
    }
    
    override fun getVerseData(surahId: Int, ayahId: Int): QuizVerseData? {
        Log.d(TAG, "📖 Getting verse data - Surah:$surahId, Ayah:$ayahId")
        val startTime = System.currentTimeMillis()
        
        // 检查Quran是否已初始化
        val quran = quranRef
        if (quran == null) {
            Log.w(TAG, "⚠️ Quran data not initialized")
            
            // 🎯 Firebase Analytics: 古兰经数据未初始化
            try {
                com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(context)
                    .logContentPerformance("surah_text", "fail", System.currentTimeMillis() - startTime, "quran_not_initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Analytics logging failed: ${e.message}")
            }
            
            return null
        }
        
        try {
            // 1. 获取阿拉伯文
            val verse = quran.getVerse(surahId, ayahId)
            if (verse == null) {
                Log.w(TAG, "⚠️ Verse not found - Surah:$surahId, Ayah:$ayahId")
                
                // 🎯 Firebase Analytics: 经文未找到
                try {
                    com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(context)
                        .logContentPerformance("surah_text", "fail", System.currentTimeMillis() - startTime, "verse_not_found")
                } catch (e: Exception) {
                    Log.e(TAG, "Analytics logging failed: ${e.message}")
                }
                
                return null
            }
            
            val arabicText = verse.arabicText
            Log.d(TAG, "✅ Arabic text loaded (${arabicText.length} chars)")
            
            // 2. 获取翻译
            val translationText = loadTranslation(surahId, ayahId)
            Log.d(TAG, "✅ Translation loaded (${translationText.length} chars)")
            
            val latency = System.currentTimeMillis() - startTime
            
            // 🎯 Firebase Analytics: 经文加载成功（监控性能）
            try {
                com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(context)
                    .logContentPerformance("surah_text", "success", latency, null)
            } catch (e: Exception) {
                Log.e(TAG, "Analytics logging failed: ${e.message}")
            }
            
            return QuizVerseData(
                surahId = surahId,
                ayahId = ayahId,
                arabicText = arabicText,
                translationText = translationText
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading verse data", e)
            
            // 🎯 Firebase Analytics: 经文加载异常
            try {
                com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(context)
                    .logContentPerformance("surah_text", "fail", System.currentTimeMillis() - startTime, e.message ?: "unknown_error")
            } catch (analyticsEx: Exception) {
                Log.e(TAG, "Analytics logging failed: ${analyticsEx.message}")
            }
            
            return null
        }
    }
    
    /**
     * 清理HTML标签
     * 移除如 <fn index="1">1</fn> 这样的脚注标签
     */
    private fun cleanHtmlTags(text: String): String {
        return text
            // 移除 <fn ...>...</fn> 脚注标签
            .replace(Regex("<fn[^>]*>[^<]*</fn>"), "")
            // 移除其他HTML标签
            .replace(Regex("<[^>]+>"), "")
            // 清理多余的空格
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * 加载翻译文本
     */
    private fun loadTranslation(surahId: Int, ayahId: Int): String {
        return try {
            // 获取用户保存的翻译slugs
            val savedSlugs = SPReader.getSavedTranslations(context)
            
            if (savedSlugs.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ No translations saved by user")
                return "No translation available.\nPlease download translations in Settings."
            }
            
            Log.d(TAG, "📚 Found ${savedSlugs.size} translation(s): ${savedSlugs.joinToString()}")
            
            // 使用QuranTranslationFactory加载翻译
            val factory = QuranTranslationFactory(context)
            val translations = factory.getTranslationsSingleVerse(savedSlugs, surahId, ayahId)
            factory.close()
            
            if (translations.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ No translations found for verse")
                return "Translation not found.\nPlease check if translations are downloaded."
            }
            
            // 获取第一个翻译的文本
            val firstTranslation = translations[0]
            val translationText = firstTranslation.text
            
            if (translationText.isNullOrEmpty()) {
                Log.w(TAG, "⚠️ Translation text is empty")
                return "Translation text is empty."
            }
            
            // 🔧 清理HTML标签（如 <fn index="1">1</fn>）
            val cleanedText = cleanHtmlTags(translationText)
            Log.d(TAG, "🧹 Cleaned HTML tags from translation")
            
            return cleanedText
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading translation", e)
            return "Error loading translation: ${e.message}"
        }
    }
}

