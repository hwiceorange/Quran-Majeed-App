package com.quran.quranaudio.online.quran_module.api.models.recitation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RecitationsCommonUrlInfoModel(
    @SerialName("common-host") val commonHost: String
)
