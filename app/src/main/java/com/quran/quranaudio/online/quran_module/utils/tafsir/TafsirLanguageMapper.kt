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
        "id" to "id-tafsir-kemenag",
        "ur" to "tafsir-bayan-ul-quran",
        "bn" to "bn-tafseer-ibn-e-kaseer",
        "ru" to "ru-tafseer-al-saddi",
        "ku" to "kurd-tafsir-rebar"
    )

    private val fallbackLanguages = mapOf(
        "id" to listOf("ar", "en"),
        "ms" to listOf("id", "ar", "en"),  // 马来语回退到印尼语或英语
        "tr" to listOf("ar", "en"),        // 土耳其语回退到阿拉伯语或英语
        "fa" to listOf("ar", "en"),
        "ru" to listOf("ar", "en"),
        "bn" to listOf("ur", "en"),        // 孟加拉语优先，回退乌尔都语或英语
        "ur" to listOf("ar", "en"),
        "ku" to listOf("ar", "en")
    )

    /**
     * 免费 Tafsir 的 slug 集合（商业模型：每语言至少一部权威 Tafsir 永久免费）。
     *
     * 唯一真相源 = preferredSlugByLanguage —— 即 App 本就为每种语言选定的"默认/首选"注释。
     * 这样做保证与现有默认选择、用户在引导页/设置里的选择完全兼容：
     * - 新用户默认落到首选注释 → 天然免费，首次打开不撞付费墙；
     * - 用户若主动选了非首选(高级)注释 → 维持付费/看广告解锁，尊重其选择。
     */
    fun isFreeTafsir(keyOrSlug: String?): Boolean {
        if (keyOrSlug.isNullOrEmpty()) return false
        val freeSet = preferredSlugByLanguage.values
        if (keyOrSlug in freeSet) return true
        // tafsirKey 可能与 slug 不同，转成 slug 再判一次
        val slug = try {
            TafsirUtils.getTafsirSlugFromKey(keyOrSlug)
        } catch (e: Exception) {
            null
        }
        return slug != null && slug in freeSet
    }

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

