

package com.quran.quranaudio.online.quran_module.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AppUpdate(
    val version: Long,
    @SerialName("updatePriority") val priority: Int
)
