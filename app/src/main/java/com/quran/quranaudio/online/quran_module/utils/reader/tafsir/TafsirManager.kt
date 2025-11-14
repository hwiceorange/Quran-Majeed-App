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
        if (!force && availableTafsirsModel != null) {
            readyCallback()
            return
        }

        loadTafsirs(ctx, force) { availableTafsirsModel ->
            TafsirManager.availableTafsirsModel = availableTafsirsModel
            readyCallback()
        }
    }


    private fun loadTafsirs(
        ctx: Context,
        force: Boolean,
        callback: (AvailableTafsirsModel?) -> Unit
    ) {
        val fileUtils = FileUtils.newInstance(ctx)

        val tafsirsFile = fileUtils.tafsirsManifestFile
        if (force) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitInstance.quran.getAvailableTafsirs()
                    val availableTafsirs = buildAvailableTafsirsModel(response.tafsirs)
                    val stringData = JsonHelper.json.encodeToString(AvailableTafsirsModel.serializer(), availableTafsirs)

                    fileUtils.createFile(tafsirsFile)
                    tafsirsFile.writeText(stringData)

                    withContext(Dispatchers.Main) {
                        postTafsirsLoad(ctx, stringData, callback)
                    }
                } catch (e: Exception) {
                    Log.saveError(e, "loadTafsirs")
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            }
        } else {
            if (!tafsirsFile.exists()) {
                loadTafsirs(ctx, true, callback)
                return
            }

            try {
                val stringData = tafsirsFile.readText()
                if (stringData.isEmpty()) {
                    loadTafsirs(ctx, true, callback)
                    return
                }

                postTafsirsLoad(ctx, stringData, callback)
            } catch (e: IOException) {
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
        SPAppActions.setFetchTafsirsForce(ctx, false)
        val savedTafsirKey = SPReader.getSavedTafsirKey(ctx)

        try {
            val availableTafsirsModel = JsonHelper.json.decodeFromString<AvailableTafsirsModel>(
                stringData
            )

            availableTafsirsModel.tafsirs.values.forEach { tafsirModels ->
                tafsirModels.forEach { tafsirModel ->
                    tafsirModel.isChecked = tafsirModel.key == savedTafsirKey
                }

            }

            callback(availableTafsirsModel)
        } catch (e: Exception) {
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