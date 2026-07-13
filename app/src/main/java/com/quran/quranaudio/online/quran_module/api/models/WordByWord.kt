package com.quran.quranaudio.online.quran_module.api.models

/**
 * 逐词翻译的单个词。数据来自 quran.foundation(api.quran.com)verses/by_key?words=true。
 * 纯内存/文件缓存，不进任何数据库，不影响现有经文渲染。
 */
data class WbwWord(
    val arabic: String,          // 该词的阿拉伯文(uthmani)
    val translation: String,     // 词义(按用户语言)
    val transliteration: String  // 转写(拉丁)
)

/** 一节经文的逐词列表。 */
data class VerseWords(
    val verseKey: String,        // "2:255"
    val words: List<WbwWord>
)
