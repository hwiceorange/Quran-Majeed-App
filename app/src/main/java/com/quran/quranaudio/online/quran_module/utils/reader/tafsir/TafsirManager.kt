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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.io.IOException
import java.util.Locale

object TafsirManager {
    @Volatile
    private var availableTafsirsModel: AvailableTafsirsModel? = null

    private val prepareScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prepareLock = Any()
    private val pendingCallbacks = mutableListOf<PrepareRequest>()
    private var prepareInFlight = false

    private data class PrepareRequest(
        val force: Boolean,
        val callback: () -> Unit
    )

    @JvmStatic
    fun prepare(
        ctx: Context,
        force: Boolean,
        readyCallback: () -> Unit
    ) {
        android.util.Log.d("TafsirManager", "🔧 prepare called: force=$force, hasModel=${availableTafsirsModel != null}")

        if (!force && availableTafsirsModel != null) {
            android.util.Log.d("TafsirManager", "✅ Using cached model")
            prepareScope.launch(Dispatchers.Main.immediate) { readyCallback() }
            return
        }

        val shouldStart = synchronized(prepareLock) {
            pendingCallbacks += PrepareRequest(force, readyCallback)
            if (prepareInFlight) {
                false
            } else {
                prepareInFlight = true
                true
            }
        }

        if (shouldStart) {
            startNextPrepare(ctx.applicationContext, force)
        }
    }

    private fun startNextPrepare(ctx: Context, force: Boolean) {
        prepareScope.launch {
            val startedAt = System.currentTimeMillis()
            val loaded = if (force) {
                loadNetworkManifest(ctx) ?: loadLocalManifest(ctx)
            } else {
                loadLocalManifest(ctx)
            }

            if (loaded != null) {
                availableTafsirsModel = loaded
                android.util.Log.d(
                    "TafsirManager",
                    "✅ Manifest ready: force=$force, groups=${loaded.tafsirs.size}, elapsed=${System.currentTimeMillis() - startedAt}ms"
                )
            } else {
                android.util.Log.e("TafsirManager", "❌ No valid Tafsir manifest available")
            }

            val callbacksToRun: List<() -> Unit>
            val runForcedFollowUp: Boolean
            synchronized(prepareLock) {
                val completedRequests = if (force) {
                    pendingCallbacks.toList()
                } else {
                    pendingCallbacks.filterNot { it.force }
                }
                pendingCallbacks.removeAll(completedRequests.toSet())
                callbacksToRun = completedRequests.map { it.callback }
                runForcedFollowUp = !force && pendingCallbacks.any { it.force }
                if (!runForcedFollowUp) {
                    prepareInFlight = false
                }
            }

            withContext(Dispatchers.Main.immediate) {
                callbacksToRun.forEach { callback ->
                    runCatching(callback).onFailure {
                        android.util.Log.e("TafsirManager", "Prepare callback failed", it)
                    }
                }
            }

            if (runForcedFollowUp) {
                startNextPrepare(ctx, true)
            }
        }
    }

    private fun loadLocalManifest(ctx: Context): AvailableTafsirsModel? {
        val fileUtils = FileUtils.newInstance(ctx)
        val tafsirsFile = fileUtils.tafsirsManifestFile

        if (tafsirsFile.exists() && tafsirsFile.length() > 0L) {
            try {
                val stringData = tafsirsFile.readText()
                parseManifest(ctx, stringData, "file")?.let {
                    return it
                }
            } catch (e: Exception) {
                android.util.Log.e("TafsirManager", "❌ File read error: ${e.message}")
                Log.saveError(e, "loadLocalTafsirs")
            }
        }

        return try {
            val assetsData = ctx.assets.open("tafsir/available_tafsirs_info.json")
                .bufferedReader()
                .use { it.readText() }
            val model = parseManifest(ctx, assetsData, "asset")
            if (model != null) {
                runCatching {
                    fileUtils.createFile(tafsirsFile)
                    tafsirsFile.writeText(assetsData)
                }.onFailure {
                    android.util.Log.w("TafsirManager", "Unable to persist bundled manifest", it)
                }
            }
            model
        } catch (e: Exception) {
            android.util.Log.e("TafsirManager", "❌ Bundled manifest load failed", e)
            Log.saveError(e, "loadBundledTafsirs")
            null
        }
    }

    private suspend fun loadNetworkManifest(ctx: Context): AvailableTafsirsModel? {
        return try {
            android.util.Log.d("TafsirManager", "🌐 Explicit Tafsir manifest refresh")
            val responseString = RetrofitInstance.github.getAvailableTafsirs().string()
            val model = parseManifest(ctx, responseString, "network")
            if (model != null) {
                val fileUtils = FileUtils.newInstance(ctx)
                fileUtils.createFile(fileUtils.tafsirsManifestFile)
                fileUtils.tafsirsManifestFile.writeText(responseString)
            }
            model
        } catch (e: Exception) {
            android.util.Log.e("TafsirManager", "❌ Manifest refresh failed: ${e.message}")
            Log.saveError(e, "refreshTafsirs")
            null
        }
    }

    private fun parseManifest(
        ctx: Context,
        stringData: String,
        source: String
    ): AvailableTafsirsModel? {
        val savedTafsirKey = SPReader.getSavedTafsirKey(ctx)

        return try {
            val availableTafsirsModel = JsonHelper.json.decodeFromString<AvailableTafsirsModel>(
                stringData
            )

            availableTafsirsModel.tafsirs.values.forEach { tafsirModels ->
                tafsirModels.forEach { tafsirModel ->
                    tafsirModel.isChecked = tafsirModel.key == savedTafsirKey
                }
            }

            SPAppActions.setFetchTafsirsForce(ctx, false)
            android.util.Log.d(
                "TafsirManager",
                "✅ Parsed ${availableTafsirsModel.tafsirs.size} Tafsir language groups from $source"
            )
            availableTafsirsModel
        } catch (e: Exception) {
            android.util.Log.e("TafsirManager", "❌ $source manifest parse error: ${e.message}")
            Log.saveError(e, "parseTafsirs-$source")
            null
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
