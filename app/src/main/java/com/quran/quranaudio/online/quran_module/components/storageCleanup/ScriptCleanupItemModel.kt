
package com.quran.quranaudio.online.quran_module.components.storageCleanup

data class ScriptCleanupItemModel(
    val scriptKey: String,
    val fontDownloadsCount: Int,
    var isCleared: Boolean = false
)
