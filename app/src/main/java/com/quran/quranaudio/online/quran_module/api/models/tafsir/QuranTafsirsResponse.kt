package com.quran.quranaudio.online.quran_module.api.models.tafsir

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuranTafsirsResponse(
    val tafsirs: List<QuranTafsirDto>
)

@Serializable
data class QuranTafsirDto(
    val id: Int,
    val name: String,
    @SerialName("author_name") val authorName: String,
    val slug: String,
    @SerialName("language_name") val languageName: String,
    @SerialName("translated_name") val translatedName: QuranTranslatedName? = null
)

@Serializable
data class QuranTranslatedName(
    val name: String,
    @SerialName("language_name") val languageName: String? = null
)

