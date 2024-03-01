
package com.quran.quranaudio.online.quran_module.components.storageCleanup

import com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirInfoModel

data class TafsirCleanupItemModel(
    val tafsirModel: TafsirInfoModel,
    val downloadsCount: Int,
    var isCleared: Boolean = false
)
