/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
package com.quran.quranaudio.online.quran_module.api.models.recitation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class RecitationTranslationInfoModel(
    @SerialName("lang-code") val langCode: String,
    @SerialName("lang-name") var langName: String,
    val book: String?,
) : RecitationInfoBaseModel()
