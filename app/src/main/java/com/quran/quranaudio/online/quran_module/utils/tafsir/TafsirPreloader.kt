package com.quran.quranaudio.online.quran_module.utils.tafsir

import android.content.Context
/**
 * Tafsir 预加载器
 * 在用户答题时，预先加载当前题目前后3条Verse的Tafsir，实现0延迟
 */
object TafsirPreloader {
    private const val TAG = "TafsirPreloader"
    
    /**
     * 预加载指定Verse及其前后3条Verse的Tafsir
     * 
     * @param context Android Context
     * @param surahId 章节号
     * @param ayahId 节号
     */
    fun preload(context: Context, surahId: Int, ayahId: Int) {
        val tafsirKey = resolveTafsirKey(context) ?: return
        val orderedVerses = listOf(ayahId, ayahId - 1, ayahId + 1, ayahId - 2, ayahId + 2, ayahId - 3, ayahId + 3)
            .filter { it > 0 }
            .distinct()

        android.util.Log.d(TAG, "📦 开始按使用优先级预加载 Tafsir: Surah $surahId, Ayah $ayahId")
        orderedVerses.forEach { targetAyah ->
            TafsirCacheManager.prefetchVerse(context, tafsirKey, surahId, targetAyah)
        }
    }

    /**
     * Navigation hot path: start only the exact requested verse before Activity startup.
     * The activity warms valid adjacent verses after the first content is visible.
     */
    fun preloadCurrent(context: Context, surahId: Int, ayahId: Int) {
        val tafsirKey = resolveTafsirKey(context) ?: return
        TafsirCacheManager.prefetchVerse(context.applicationContext, tafsirKey, surahId, ayahId)
    }

    private fun resolveTafsirKey(context: Context): String? {
        return com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTafsirKey(context)
            ?: TafsirUtils.getPreferredTafsirKey(context)
    }
}
