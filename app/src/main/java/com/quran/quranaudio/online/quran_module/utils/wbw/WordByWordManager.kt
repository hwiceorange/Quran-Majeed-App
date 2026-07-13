package com.quran.quranaudio.online.quran_module.utils.wbw

import android.content.Context
import android.util.Log
import com.quran.quranaudio.online.quran_module.api.RetrofitInstance
import com.quran.quranaudio.online.quran_module.api.models.VerseWords
import com.quran.quranaudio.online.quran_module.api.models.WbwWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * 逐词翻译数据引擎：从 quran.foundation 拉取某节逐词数据并做文件缓存。
 *
 * 严守约束：纯文件缓存(filesDir/wbw/)，不进任何数据库、不改经文渲染。
 * 只在用户主动打开"逐词"面板时按需拉取；离线用缓存。
 */
object WordByWordManager {

    private const val TAG = "WordByWord"

    /** quran.foundation 词义支持的语言(其余回退英语)。 */
    private val SUPPORTED = setOf("en", "ur", "id", "bn", "tr", "ms", "ar")

    fun mapLang(appLang: String?): String {
        val l = appLang?.lowercase()?.take(2) ?: "en"
        return if (l in SUPPORTED) l else "en"
    }

    suspend fun getVerseWords(context: Context, chapterNo: Int, verseNo: Int, lang: String): VerseWords? =
        withContext(Dispatchers.IO) {
            val verseKey = "$chapterNo:$verseNo"
            val cached = readCache(context, chapterNo, verseNo, lang)
            if (cached != null) return@withContext parse(verseKey, cached)

            try {
                val body = RetrofitInstance.quranFoundation
                    .getVerseWords(verseKey, true, lang, "text_uthmani,transliteration")
                    .string()
                writeCache(context, chapterNo, verseNo, lang, body)
                parse(verseKey, body)
            } catch (e: Exception) {
                Log.w(TAG, "getVerseWords failed for $verseKey/$lang", e)
                null
            }
        }

    /** 解析 quran.foundation verses/by_key 响应的 words 数组。 */
    private fun parse(verseKey: String, json: String): VerseWords? {
        return try {
            val verse = JSONObject(json).optJSONObject("verse") ?: return null
            val wordsArr = verse.optJSONArray("words") ?: return null
            val out = ArrayList<WbwWord>()
            for (i in 0 until wordsArr.length()) {
                val w = wordsArr.optJSONObject(i) ?: continue
                // 跳过节末的序号标记(char_type_name = "end")，只取真正的词
                if (w.optString("char_type_name") != "word") continue
                val arabic = w.optString("text_uthmani").ifEmpty { w.optString("text") }
                val translation = w.optJSONObject("translation")?.optString("text") ?: ""
                val translit = w.optJSONObject("transliteration")?.optString("text") ?: ""
                if (arabic.isNotEmpty()) {
                    out.add(WbwWord(arabic, translation, translit))
                }
            }
            if (out.isEmpty()) null else VerseWords(verseKey, out)
        } catch (e: Exception) {
            Log.w(TAG, "parse failed", e)
            null
        }
    }

    private fun cacheFile(context: Context, chapterNo: Int, verseNo: Int, lang: String): File {
        val dir = File(context.filesDir, "wbw/$lang")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "${chapterNo}_$verseNo.json")
    }

    private fun readCache(context: Context, chapterNo: Int, verseNo: Int, lang: String): String? {
        return try {
            val f = cacheFile(context, chapterNo, verseNo, lang)
            if (f.exists() && f.length() > 0) f.readText() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun writeCache(context: Context, chapterNo: Int, verseNo: Int, lang: String, body: String) {
        try {
            cacheFile(context, chapterNo, verseNo, lang).writeText(body)
        } catch (e: Exception) {
            Log.w(TAG, "writeCache failed", e)
        }
    }
}
