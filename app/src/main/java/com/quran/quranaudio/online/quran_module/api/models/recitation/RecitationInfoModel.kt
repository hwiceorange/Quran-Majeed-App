
package com.quran.quranaudio.online.quran_module.api.models.recitation

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class RecitationInfoModel(
    val style: String?,
    val styleTranslations: Map<String, String> = mapOf(),
) : RecitationInfoBaseModel() {
    fun getStyleName(): String? {
        return styleTranslations[Locale.getDefault().toLanguageTag()] ?: this.style
    }
}
