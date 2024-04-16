/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
package com.quran.quranaudio.online.quran_module.components.storageCleanup

import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo
import com.quran.quranaudio.online.quran_module.components.transls.TranslBaseModel

class TranslationCleanupItemModel(val bookInfo: QuranTranslBookInfo) : TranslBaseModel() {
    var isDeleted: Boolean = false
}
