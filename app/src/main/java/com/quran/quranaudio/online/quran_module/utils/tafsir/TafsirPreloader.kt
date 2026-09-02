package com.quran.quranaudio.online.quran_module.utils.tafsir

import android.content.Context
import com.quran.quranaudio.online.quran_module.utils.reader.tafsir.TafsirManager
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
        val orderedVerses = listOf(ayahId, ayahId - 1, ayahId + 1, ayahId - 2, ayahId + 2, ayahId - 3, ayahId + 3)
            .filter { it > 0 }
            .distinct()

        withPreparedTafsir(context) { appContext, tafsirKey ->
            android.util.Log.d(TAG, "📦 开始按使用优先级预加载 Tafsir: Surah $surahId, Ayah $ayahId")
            orderedVerses.forEach { targetAyah ->
                TafsirCacheManager.prefetchVerse(appContext, tafsirKey, surahId, targetAyah)
            }
        }
    }

    /**
     * Navigation hot path: start only the exact requested verse before Activity startup.
     * The activity warms valid adjacent verses after the first content is visible.
     */
    fun preloadCurrent(context: Context, surahId: Int, ayahId: Int) {
        withPreparedTafsir(context) { appContext, tafsirKey ->
            TafsirCacheManager.prefetchVerse(appContext, tafsirKey, surahId, ayahId)
        }
    }

    private fun withPreparedTafsir(
        context: Context,
        action: (Context, String) -> Unit
    ) {
        val appContext = context.applicationContext
        // A saved key can exist before the process-local manifest model. Resolving
        // its slug or starting a request before prepare completes poisons the first
        // page request; join the same local-first prepare used by ActivityTafsir.
        TafsirManager.prepare(appContext, false) {
            val tafsirKey = resolveTafsirKey(appContext)
            if (tafsirKey.isNullOrBlank()) {
                android.util.Log.w(TAG, "Skipping Tafsir prefetch: no valid key after manifest prepare")
                return@prepare
            }
            action(appContext, tafsirKey)
        }
    }

    private fun resolveTafsirKey(context: Context): String? {
        val savedKey = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader
            .getSavedTafsirKey(context)
            ?.takeIf { TafsirManager.getModel(it) != null }
        return savedKey ?: TafsirUtils.getPreferredTafsirKey(context)
    }
}
