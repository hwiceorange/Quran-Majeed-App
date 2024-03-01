package com.quran.quranaudio.online.quran_module.views.reader.juzSpinner

import com.quran.quranaudio.online.quran_module.utils.univ.StringUtils
import com.quran.quranaudio.online.quran_module.views.reader.spinner.ReaderSpinnerItem

class JuzSpinnerItem(label: CharSequence) : ReaderSpinnerItem() {
    var juzNumber = -1
    var nameArabic = ""
    var nameTransliterated = ""
        set(nameTransliterated) {
            field = nameTransliterated
            searchKeyword = StringUtils.stripDiacritics(nameTransliterated)
        }

    var searchKeyword: String? = null
        private set

    init {
        super.label = label
    }
}
