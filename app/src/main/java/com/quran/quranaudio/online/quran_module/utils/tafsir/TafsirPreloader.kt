package com.quran.quranaudio.online.quran_module.utils.tafsir

import android.content.Context
import com.quran.quranaudio.online.quran_module.api.JsonHelper
import com.quran.quranaudio.online.quran_module.api.RetrofitInstance
import com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirModel
import com.quran.quranaudio.online.quran_module.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString

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
        val tafsirKey = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTafsirKey(context)
            ?: TafsirUtils.getPreferredTafsirKey(context)
            ?: return
        
        val slug = TafsirUtils.getTafsirSlugFromKey(tafsirKey)
        
        // 计算需要预加载的范围：当前Verse及前后3条
        val versesToPreload = mutableListOf<Pair<Int, Int>>()
        for (offset in -3..3) {
            val targetAyah = ayahId + offset
            if (targetAyah > 0) {  // 简单的边界检查
                versesToPreload.add(Pair(surahId, targetAyah))
            }
        }
        
        android.util.Log.d(TAG, "📦 开始预加载 Tafsir: Surah $surahId, 范围 ${ayahId-3}~${ayahId+3}")
        
        // 后台异步预加载
        CoroutineScope(Dispatchers.IO).launch {
            versesToPreload.forEach { (s, a) ->
                preloadSingleVerse(context, tafsirKey, slug, s, a)
            }
        }
    }
    
    /**
     * 预加载单条Verse的Tafsir
     */
    private suspend fun preloadSingleVerse(
        context: Context,
        tafsirKey: String,
        slug: String,
        surahId: Int,
        ayahId: Int
    ) {
        try {
            val fileUtils = FileUtils.newInstance(context)
            val tafsirFile = fileUtils.getTafsirFileSingleVerse(tafsirKey, surahId, ayahId)
            
            // 如果已缓存，跳过
            if (tafsirFile.length() > 0) {
                return
            }
            
            // 从网络加载
            val tafsir = when {
                slug.startsWith("id-") -> {
                    RetrofitInstance.customTafsir.getTafsir(slug, "$surahId:$ayahId")["tafsir"]!!
                }
                else -> {
                    RetrofitInstance.quran.getTafsir(slug, "$surahId:$ayahId")["tafsir"]!!
                }
            }
            
            // 保存到缓存
            fileUtils.createFile(tafsirFile)
            tafsirFile.writeText(JsonHelper.json.encodeToString(tafsir))
            android.util.Log.d(TAG, "✅ 预加载成功: $surahId:$ayahId")
        } catch (e: Exception) {
            // 静默失败，不影响用户体验
        }
    }
}

