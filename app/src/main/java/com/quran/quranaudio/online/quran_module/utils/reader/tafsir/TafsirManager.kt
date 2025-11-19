package com.quran.quranaudio.online.quran_module.utils.reader.tafsir

import android.content.Context
import com.quran.quranaudio.online.quran_module.api.JsonHelper
import com.quran.quranaudio.online.quran_module.api.RetrofitInstance
import com.quran.quranaudio.online.quran_module.api.models.tafsir.AvailableTafsirsModel
import com.quran.quranaudio.online.quran_module.api.models.tafsir.QuranTafsirDto
import com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirInfoModel
import com.quran.quranaudio.online.quran_module.utils.Log
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppActions
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader
import com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirLanguageMapper
import com.quran.quranaudio.online.quran_module.utils.univ.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.util.Locale

object TafsirManager {
    private var availableTafsirsModel: AvailableTafsirsModel? = null

    @JvmStatic
    fun prepare(
        ctx: Context,
        force: Boolean,
        readyCallback: () -> Unit
    ) {
        android.util.Log.d("TafsirManager", "🔧 prepare called: force=$force, hasModel=${availableTafsirsModel != null}")
        
        if (!force && availableTafsirsModel != null) {
            android.util.Log.d("TafsirManager", "✅ Using cached model")
            readyCallback()
            return
        }

        loadTafsirs(ctx, force) { availableTafsirsModel ->
            android.util.Log.d("TafsirManager", "📦 Tafsir loaded, isNull=${availableTafsirsModel == null}")
            TafsirManager.availableTafsirsModel = availableTafsirsModel
            if (availableTafsirsModel != null) {
                android.util.Log.d("TafsirManager", "✅ Model assigned, calling readyCallback")
            } else {
                android.util.Log.w("TafsirManager", "⚠️ Model is null, calling readyCallback anyway")
            }
            readyCallback()
        }
    }


    private fun loadTafsirs(
        ctx: Context,
        force: Boolean,
        callback: (AvailableTafsirsModel?) -> Unit
    ) {
        android.util.Log.d("TafsirManager", "📥 loadTafsirs called: force=$force")
        val fileUtils = FileUtils.newInstance(ctx)

        val tafsirsFile = fileUtils.tafsirsManifestFile
        android.util.Log.d("TafsirManager", "📂 Tafsir manifest file: ${tafsirsFile.absolutePath}, exists=${tafsirsFile.exists()}")
        
        if (force) {
            android.util.Log.d("TafsirManager", "🌐 Force loading from network...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 使用 GitHub API 而不是 Quran.com API（更稳定）
                    val response = RetrofitInstance.github.getAvailableTafsirs()
                    val responseString = response.string()
                    android.util.Log.d("TafsirManager", "📥 Received response, length=${responseString.length}")
                    
                    // 保存原始 JSON
                    fileUtils.createFile(tafsirsFile)
                    tafsirsFile.writeText(responseString)

                    android.util.Log.d("TafsirManager", "✅ Network load successful")
                    withContext(Dispatchers.Main) {
                        postTafsirsLoad(ctx, responseString, callback)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TafsirManager", "❌ Network load failed: ${e.message}")
                    android.util.Log.w("TafsirManager", "⚠️ Attempting to load from assets as fallback...")
                    Log.saveError(e, "loadTafsirs")
                    e.printStackTrace()
                    
                    // 尝试从 assets 加载备用清单
                    try {
                        val assetsData = ctx.assets.open("tafsir/available_tafsirs_info.json").bufferedReader().use { it.readText() }
                        android.util.Log.d("TafsirManager", "✅ Loaded from assets, length=${assetsData.length}")
                        
                        fileUtils.createFile(tafsirsFile)
                        tafsirsFile.writeText(assetsData)
                        
                        withContext(Dispatchers.Main) {
                            postTafsirsLoad(ctx, assetsData, callback)
                        }
                    } catch (assetsError: Exception) {
                        android.util.Log.e("TafsirManager", "❌ Assets fallback also failed: ${assetsError.message}")
                        withContext(Dispatchers.Main) {
                            callback(null)
                        }
                    }
                }
            }
        } else {
            if (!tafsirsFile.exists()) {
                android.util.Log.d("TafsirManager", "⚠️ Manifest file not found, forcing network load")
                loadTafsirs(ctx, true, callback)
                return
            }

            android.util.Log.d("TafsirManager", "📄 Loading from local file...")
            try {
                val stringData = tafsirsFile.readText()
                if (stringData.isEmpty()) {
                    android.util.Log.d("TafsirManager", "⚠️ File is empty, forcing network load")
                    loadTafsirs(ctx, true, callback)
                    return
                }

                android.util.Log.d("TafsirManager", "✅ Local file loaded successfully")
                postTafsirsLoad(ctx, stringData, callback)
            } catch (e: IOException) {
                android.util.Log.e("TafsirManager", "❌ File read error: ${e.message}")
                Log.saveError(e, "loadTafsirs")
                e.printStackTrace()
                loadTafsirs(ctx, true, callback)
            }
        }
    }

    private fun postTafsirsLoad(
        ctx: Context,
        stringData: String,
        callback: (AvailableTafsirsModel?) -> Unit
    ) {
        android.util.Log.d("TafsirManager", "📊 postTafsirsLoad called")
        SPAppActions.setFetchTafsirsForce(ctx, false)
        val savedTafsirKey = SPReader.getSavedTafsirKey(ctx)
        android.util.Log.d("TafsirManager", "🔑 Saved Tafsir key: $savedTafsirKey")

        try {
            val availableTafsirsModel = JsonHelper.json.decodeFromString<AvailableTafsirsModel>(
                stringData
            )

            android.util.Log.d("TafsirManager", "✅ Parsed ${availableTafsirsModel.tafsirs.size} language groups")
            availableTafsirsModel.tafsirs.forEach { (lang, models) ->
                android.util.Log.d("TafsirManager", "   - $lang: ${models.size} tafsirs")
            }

            availableTafsirsModel.tafsirs.values.forEach { tafsirModels ->
                tafsirModels.forEach { tafsirModel ->
                    tafsirModel.isChecked = tafsirModel.key == savedTafsirKey
                }

            }

            callback(availableTafsirsModel)
        } catch (e: Exception) {
            android.util.Log.e("TafsirManager", "❌ JSON parse error: ${e.message}")
            Log.saveError(e, "postTafsirsLoad")
            e.printStackTrace()
            callback(null)
        }
    }

    @JvmStatic
    fun getModel(key: String): TafsirInfoModel? {
        val tafsirListForLangCodes = availableTafsirsModel?.tafsirs?.values ?: return null

        for (tafsirList in tafsirListForLangCodes) {
            val tafsir = tafsirList.firstOrNull { it.key == key }
            if (tafsir != null) return tafsir
        }

        return null
    }

    @JvmStatic
    fun getModels(): Map<String, List<TafsirInfoModel>>? {
        return availableTafsirsModel?.tafsirs
    }


    @JvmStatic
    fun getModels(lang: String?): List<TafsirInfoModel>? {
        return availableTafsirsModel?.tafsirs?.get(lang!!)
    }


    @JvmStatic
    fun setSavedTafsirKey(key: String) {
        availableTafsirsModel?.tafsirs?.values?.forEach { tafsirModels ->
            tafsirModels.forEach { tafsirModel ->
                tafsirModel.isChecked = tafsirModel.key == key
            }
        }
    }


    private fun buildAvailableTafsirsModel(dtos: List<QuranTafsirDto>): AvailableTafsirsModel {
        val grouped = mutableMapOf<String, MutableList<TafsirInfoModel>>()

        dtos.forEach { dto ->
            val languageCode = resolveLanguageCode(dto.languageName, dto.slug)
            val languageName = formatLanguageName(dto.languageName, languageCode)
            val info = TafsirInfoModel(
                key = dto.slug,
                name = dto.name,
                author = dto.authorName,
                langCode = languageCode,
                langName = languageName,
                slug = dto.slug
            )

            grouped.getOrPut(languageCode) { mutableListOf() }.apply {
                if (none { it.key == info.key }) {
                    add(info)
                }
            }
        }

        val sorted = grouped.toSortedMap().mapValues { entry ->
            entry.value.sortedBy { it.name.lowercase(Locale.ROOT) }
        }

        return AvailableTafsirsModel(sorted)
    }

    private fun resolveLanguageCode(languageName: String?, slug: String): String {
        val normalized = languageName?.lowercase(Locale.ROOT)?.trim() ?: ""
        LANGUAGE_CODE_MAP[normalized]?.let { return TafsirLanguageMapper.normalize(it) }

        val slugPrefix = slug.substringBefore('-', missingDelimiterValue = slug)
        if (slugPrefix.length in 2..5 && slugPrefix.all { it.isLetter() }) {
            return TafsirLanguageMapper.normalize(slugPrefix.lowercase(Locale.ROOT))
        }

        val fallback = normalized.take(2).ifEmpty { "en" }
        return TafsirLanguageMapper.normalize(fallback)
    }

    private fun formatLanguageName(languageName: String?, languageCode: String): String {
        if (languageName.isNullOrBlank()) {
            return languageCode.uppercase(Locale.ROOT)
        }

        return languageName.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
        }
    }

    private val LANGUAGE_CODE_MAP = mapOf(
        "arabic" to "ar",
        "english" to "en",
        "urdu" to "ur",
        "bengali" to "bn",
        "indonesian" to "id",  // 统一使用 "id" 表示印尼语
        "malay" to "ms",
        "turkish" to "tr",
        "persian" to "fa",
        "french" to "fr",
        "spanish" to "es",
        "russian" to "ru",
        "kurdish" to "ku",
        "somali" to "so",
        "swahili" to "sw",
        "bosnian" to "bs",
        "german" to "de",
        "italian" to "it",
        "portuguese" to "pt",
        "uzbek" to "uz",
        "albanian" to "sq",
        "chinese" to "zh",
        "hindi" to "hi",
        "azerbaijani" to "az",
        "malayalam" to "ml",
        "korean" to "ko",
        "japanese" to "ja",
        "dutch" to "nl",
        "tajik" to "tg",
        "thai" to "th"
    )


    fun emptyModel(
        key: String = "",
        name: String = "",
        author: String = "",
        langCode: String = "",
        langName: String = "",
        slug: String = "",
    ): TafsirInfoModel {
        return TafsirInfoModel(
            key = key,
            name = name,
            author = author,
            langCode = langCode,
            langName = langName,
            slug = slug,
        )
    }
}