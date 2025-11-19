package com.quran.quranaudio.quiz.utils

import android.content.Context
import android.util.Log
import com.quran.quranaudio.quiz.data.QuranDataProviderHolder
import com.quran.quranaudio.quiz.data.QuizVerseData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 经文加载助手 - 使用稳定的接口加载古兰经数据
 * 
 * 设计改进：
 * ✅ 不使用反射 - 通过稳定的接口调用
 * ✅ 依赖注入 - 使用 QuranDataProvider 接口
 * ✅ 封装性好 - 所有复杂逻辑在 app 模块的 Repository 中
 * ✅ 类型安全 - 使用明确的数据类 QuizVerseData
 */
object VerseLoaderHelper {
    private const val TAG = "VerseLoaderHelper"
    
    /**
     * 加载经文数据（阿拉伯文 + 翻译）
     * 
     * @param context Context
     * @param surahId 章节号 (1-114)
     * @param ayahId Ayah号
     * @return 经文数据
     */
    suspend fun loadVerse(context: Context, surahId: Int, ayahId: Int): QuizVerseData = withContext(Dispatchers.IO) {
        Log.d(TAG, "🔍 Loading verse - Surah:$surahId, Ayah:$ayahId")
        
        try {
            // 获取数据提供者（由 app 模块注入）
            val dataProvider = QuranDataProviderHolder.getInstance()
            
            // 确保数据已初始化
            if (!dataProvider.isQuranDataReady()) {
                Log.d(TAG, "⏳ Quran data not ready, initializing...")
                suspendCancellableCoroutine<Unit> { continuation ->
                    dataProvider.ensureQuranDataInitialized {
                        Log.d(TAG, "✅ Quran data initialization completed")
                        continuation.resume(Unit)
                    }
                }
            }
            
            // 加载经文数据
            val verseData = dataProvider.getVerseData(surahId, ayahId)
            
            if (verseData != null) {
                Log.d(TAG, "✅ Verse loaded successfully")
                Log.d(TAG, "   📖 Arabic: ${verseData.arabicText.take(50)}${if (verseData.arabicText.length > 50) "..." else ""}")
                Log.d(TAG, "   🌍 Translation: ${verseData.translationText.take(50)}${if (verseData.translationText.length > 50) "..." else ""}")
                return@withContext verseData
            } else {
                Log.w(TAG, "⚠️ Failed to load verse data")
                return@withContext QuizVerseData(
                    surahId = surahId,
                    ayahId = ayahId,
                    arabicText = "Verse not found",
                    translationText = "Please check if Quran data is downloaded"
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading verse", e)
            e.printStackTrace()
            return@withContext QuizVerseData(
                surahId = surahId,
                ayahId = ayahId,
                arabicText = "Error loading verse",
                translationText = "An error occurred: ${e.message}"
            )
        }
    }
}
