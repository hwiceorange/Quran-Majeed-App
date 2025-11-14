package com.quran.quranaudio.online.quran_module.utils.tafsir

import android.util.Log
import com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirInfoModel
import java.util.Locale

object TafsirLanguageMapper {

    private const val TAG = "TafsirLanguageMapper"

    private val languageAliases = mapOf(
        "in" to "id",  // 将旧代码 "in" 映射到新代码 "id"
        "bahasa" to "id",
        "ms" to "ms",
        "tr" to "tr",
        "fa" to "fa",
        "ckb" to "ku",
        "ku" to "ku"
    )

    private val preferredSlugByLanguage = mapOf(
        "en" to "en-tafisr-ibn-kathir",
        "ar" to "ar-tafsir-muyassar",
        "ur" to "tafsir-bayan-ul-quran",
        "bn" to "bn-tafseer-ibn-e-kaseer",
        "ru" to "ru-tafseer-al-saddi",
        "ku" to "kurd-tafsir-rebar"
    )

    private val fallbackLanguages = mapOf(
        "id" to listOf("en"),  // 统一使用 "id" 表示印尼语
        "ms" to listOf("en"),
        "tr" to listOf("ar", "en"),
        "fa" to listOf("ar", "en"),
        "ru" to listOf("en"),
        "bn" to listOf("en"),
        "ur" to listOf("en"),
        "ku" to listOf("ar", "en")
    )

    fun pickBestTafsirKey(
        language: String?,
        available: Map<String, List<TafsirInfoModel>>?
    ): String? {
        if (available.isNullOrEmpty()) return null

        val normalizedLanguage = normalize(language)

        // Step 1: try preferred slug for this language
        val preferredSlug = preferredSlugByLanguage[normalizedLanguage]
        findBySlug(preferredSlug, available)?.let { return it }
        if (preferredSlug == null) {
            Log.w(TAG, "No direct tafsir mapping for language '$normalizedLanguage'. Using fallbacks.")
        }

        // Step 2: try languages in fallback chain
        val attempts = LinkedHashSet<String>()
        attempts.add(normalizedLanguage)
        fallbackLanguages[normalizedLanguage]?.let { attempts.addAll(it) }
        attempts.add("en") // ensure global fallback

        attempts.forEach { langCode ->
            preferredSlugByLanguage[langCode]?.let { slug ->
                findBySlug(slug, available)?.let { return it }
            }
            val list = available[langCode]
            if (!list.isNullOrEmpty()) {
                return list.first().key
            }
        }

        // Step 3: fallback to any available entry
        val first = available.values.firstOrNull { it.isNotEmpty() }?.first()?.key
        if (first == null) {
            Log.w(TAG, "No tafsir available after attempting fallbacks. Returning null.")
        }
        return first
    }

    fun resolvePreferredSlug(language: String?): String? {
        val normalized = normalize(language)
        preferredSlugByLanguage[normalized]?.let { return it }
        Log.w(TAG, "No preferred tafsir slug for '$normalized'. Falling back to defaults.")

        return fallbackLanguages[normalized]
            ?.firstNotNullOfOrNull { preferredSlugByLanguage[it] }
            ?: preferredSlugByLanguage["en"].also {
                if (it == null) {
                    Log.w(TAG, "English fallback missing from preferredSlugByLanguage. Returning null.")
                }
            }
    }

    fun normalize(language: String?): String {
        if (language.isNullOrBlank()) return "en"
        val lower = language.lowercase(Locale.ROOT)
        return languageAliases[lower] ?: lower
    }

    private fun findBySlug(
        slug: String?,
        available: Map<String, List<TafsirInfoModel>>
    ): String? {
        if (slug.isNullOrBlank()) return null

        available.values.forEach { list ->
            list.firstOrNull { tafsir ->
                tafsir.slug.equals(slug, ignoreCase = true) || tafsir.key.equals(slug, ignoreCase = true)
            }?.let { return it.key }
        }
        return null
    }
}

