
package com.quran.quranaudio.online.quran_module.components.storageCleanup

import com.quran.quranaudio.online.quran_module.api.models.recitation.RecitationInfoModel

data class RecitationCleanupItemModel(
    val recitationModel: RecitationInfoModel,
    val downloadsCount: Int,
    var isCleared: Boolean = false
)
