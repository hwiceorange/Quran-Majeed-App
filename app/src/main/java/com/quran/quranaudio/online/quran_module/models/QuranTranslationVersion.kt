package com.quran.quranaudio.online.quran_module.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 古兰经翻译版本数据模型
 * 
 * 用于表示一个可选的古兰经翻译版本，包含：
 * - 版本标识符（用于API和本地存储）
 * - 显示名称（用于UI展示）
 * - 语言代码（用于按语言过滤）
 * - 下载URL（用于下载翻译文件）
 * - 下载状态（本地是否已下载）
 */
@Serializable
data class QuranTranslationVersion(
    /**
     * 版本唯一标识符
     * 格式：[语言代码]_[ID]_[slug名称]
     * 例如：en_101_sahih-international
     */
    @SerialName("slug")
    val versionId: String,
    
    /**
     * 翻译版本的显示名称
     * 例如：The Noble Quran (Muhsin Khan)
     */
    @SerialName("display-name")
    val displayName: String,
    
    /**
     * 翻译版本的完整书名
     * 例如：Sahih International
     */
    @SerialName("book")
    val bookName: String? = null,
    
    /**
     * 翻译者/作者名称
     * 例如：Dr. Mustafa Khattab
     */
    @SerialName("author")
    val authorName: String? = null,
    
    /**
     * 语言代码
     * 格式：ISO 639-1 两位字母代码
     * 例如：en, ar, in, ur, ms, tr, bn
     */
    @SerialName("lang-code")
    val languageCode: String,
    
    /**
     * 语言名称
     * 例如：English, Arabic
     */
    @SerialName("lang-name")
    val languageName: String,
    
    /**
     * 翻译文件的下载路径（相对路径）
     * 例如：apis/translations/en/101/translation_101_en_en_sahih-international.json
     */
    @SerialName("file-path")
    val downloadPath: String? = null,
    
    /**
     * 版本ID（数字）
     * 例如：101, 102
     */
    @SerialName("id")
    val numericId: Int = 0,
    
    /**
     * 本地下载状态（运行时确定，不从API返回）
     */
    @kotlinx.serialization.Transient
    var isDownloaded: Boolean = false,
    
    /**
     * 是否为预装版本（运行时确定）
     */
    @kotlinx.serialization.Transient
    var isPrebuilt: Boolean = false,
    
    /**
     * 是否来自 Quran Foundation API（用于区分数据源）
     */
    @kotlinx.serialization.Transient
    var isQuranFoundationApi: Boolean = false
) {
    /**
     * 获取完整的下载URL
     */
    fun getFullDownloadUrl(baseUrl: String = "https://apis.dochubai.com/quran/"): String {
        // API返回的downloadPath是错误的（inventory/... 而不是 apis/...）
        // 所以我们总是使用标准格式构建URL
        return "${baseUrl}apis/translations/${languageCode}/${versionId}.json"
    }
    
    /**
     * 获取本地文件名
     */
    fun getLocalFileName(): String {
        // 如果有numericId，使用传统格式；否则使用slug作为文件名
        return if (numericId > 0) {
            "translation_${numericId}_${languageCode}_${versionId}.json"
        } else {
            "${versionId}.json"
        }
    }
    
    /**
     * 获取显示文本（优先使用 displayName，否则使用 bookName）
     */
    fun getDisplayText(): String {
        return when {
            displayName.isNotEmpty() -> displayName
            !bookName.isNullOrEmpty() -> bookName
            else -> versionId
        }
    }
}

/**
 * 翻译版本列表的包装器
 */
@Serializable
data class QuranTranslationVersionsResponse(
    @SerialName("translations")
    val translations: List<QuranTranslationVersion>
)

