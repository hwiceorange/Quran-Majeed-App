package com.quran.quranaudio.online.quran_module.components.search

import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo

class VerseResultCountModel(val bookInfo: QuranTranslBookInfo?) : com.quran.quranaudio.online.quran_module.components.search.SearchResultModelBase() {
    @JvmField
    var resultCount = 0
}
