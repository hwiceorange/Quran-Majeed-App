/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
package com.quran.quranaudio.online.quran_module.components.transls

class TranslationGroupModel(
    val langCode: String,
) {
    var langName = ""
    var translations: ArrayList<TranslModel> = ArrayList()
    var isExpanded = false
}
