/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
package com.quran.quranaudio.online.quran_module.components.storageCleanup

data class ScriptCleanupItemModel(
    val scriptKey: String,
    val fontDownloadsCount: Int,
    var isCleared: Boolean = false
)
