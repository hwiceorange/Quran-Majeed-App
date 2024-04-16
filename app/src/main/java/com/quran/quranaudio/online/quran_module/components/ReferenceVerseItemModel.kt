package com.quran.quranaudio.online.quran_module.components

class ReferenceVerseItemModel(
    val viewType: Int,
    val verse: com.quran.quranaudio.online.quran_module.components.quran.subcomponents.Verse?,
    val chapterNo: Int,
    val fromVerse: Int,
    val toVerse: Int,
    val titleText: String?,
    var bookmarked: Boolean,
)
