package com.quran.quranaudio.online.quran_module.data

import com.quran.quranaudio.online.quran_module.models.QuranTranslationVersion
import com.quran.quranaudio.online.quran_module.utils.reader.TranslUtils

/**
 * 本地古兰经翻译版本数据源
 * 
 * 功能：
 * 1. 提供所有7种语言的预装版本和常用版本信息
 * 2. 用于快速显示，无需等待API加载
 * 3. API数据会在后台加载并合并更新
 * 
 * 优点：
 * - 用户体验：立即显示版本列表，无加载等待
 * - 离线支持：即使没有网络也能显示基本选项
 * - 性能优化：减少API依赖，提升响应速度
 */
object LocalTranslationData {
    
    /**
     * 获取指定语言的本地翻译版本列表
     * 
     * @param languageCode 语言代码 (en, id, ar, ur, ms, tr, bn)
     * @return 该语言的翻译版本列表
     */
    fun getVersions(languageCode: String): List<QuranTranslationVersion> {
        return when (languageCode) {
            "en" -> getEnglishVersions()
            "id" -> getIndonesianVersions()
            "ar" -> getArabicVersions()
            "ur" -> getUrduVersions()
            "ms" -> getMalayVersions()
            "tr" -> getTurkishVersions()
            "bn" -> getBengaliVersions()
            else -> getEnglishVersions() // 默认返回英语版本
        }
    }
    
    /**
     * 英语版本 (English)
     * 顺序优化：1. The Clear Quran, 2. Abdul Haleem, 3. Sahih International, 4-9. 其他
     */
    private fun getEnglishVersions(): List<QuranTranslationVersion> {
        return listOf(
            // 第1位：The Clear Quran（预装版本）
            QuranTranslationVersion(
                versionId = TranslUtils.TRANSL_SLUG_EN_THE_CLEAR_QURAN,
                displayName = "The Clear Quran",
                bookName = "The Clear Quran",
                authorName = "Dr. Mustafa Khattab",
                languageCode = "en",
                languageName = "English",
                shortDescription = "Easy-to-read, Flowing Context.",
                downloadPath = null,
                isPrebuilt = true,
                isDownloaded = true
            ),
            // 第2位：Abdul Haleem
            QuranTranslationVersion(
                versionId = "en_abdul-haleem",
                displayName = "Abdul Haleem",
                bookName = "The Quran: A New Translation",
                authorName = "M.A.S. Abdel Haleem",
                languageCode = "en",
                languageName = "English",
                shortDescription = "Modern, Accessible, UK Standard.",
                downloadPath = "apis/translations/en/en_abdul-haleem.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            // 第3位：Sahih International（预装版本）
            QuranTranslationVersion(
                versionId = TranslUtils.TRANSL_SLUG_EN_SAHIH_INTERNATIONAL,
                displayName = "Sahih International",
                bookName = "Sahih International",
                authorName = "Al-Muntada Al-Islami",
                languageCode = "en",
                languageName = "English",
                shortDescription = "Clear, Modern English.",
                downloadPath = null,
                isPrebuilt = true,
                isDownloaded = true
            ),
            // 第4位：Yusuf Ali
            QuranTranslationVersion(
                versionId = "en_yusuf-ali",
                displayName = "Yusuf Ali",
                bookName = "The Holy Quran",
                authorName = "Abdullah Yusuf Ali",
                languageCode = "en",
                languageName = "English",
                shortDescription = "Widely Popular, Poetic Classic.",
                downloadPath = "apis/translations/en/en_yusuf-ali.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            // 第5位：Hilali & Khan
            QuranTranslationVersion(
                versionId = "en_hilali-khan",
                displayName = "Hilali & Khan",
                bookName = "The Noble Quran",
                authorName = "Hilali & Khan",
                languageCode = "en",
                languageName = "English",
                shortDescription = "Orthodox, Detailed Hadith Notes.",
                downloadPath = "apis/translations/en/en_hilali-khan.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            // 第6位：Pickthall
            QuranTranslationVersion(
                versionId = "en_pickthall",
                displayName = "Pickthall",
                bookName = "The Meaning of the Glorious Quran",
                authorName = "Mohammed Marmaduke Pickthall",
                languageCode = "en",
                languageName = "English",
                shortDescription = "Historical, Literal Translation.",
                downloadPath = "apis/translations/en/en_pickthall.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            // 第7位：T.B. Irving
            QuranTranslationVersion(
                versionId = "en_irving",
                displayName = "T.B. Irving",
                bookName = "The First American Version",
                authorName = "Thomas Ballantyne Irving",
                languageCode = "en",
                languageName = "English",
                shortDescription = "American Academic, Contemporary.",
                downloadPath = "apis/translations/en/en_irving.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            // 第8位：Taqi Usmani
            QuranTranslationVersion(
                versionId = "en_taqi-usmani",
                displayName = "Taqi Usmani",
                bookName = "Tafsir Anwaar-ul-Bayan",
                authorName = "Mufti Muhammad Taqi Usmani",
                languageCode = "en",
                languageName = "English",
                shortDescription = "Deobandi School Perspective.",
                downloadPath = "apis/translations/en/en_taqi-usmani.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            // 第9位：Mahmoud Ghali
            QuranTranslationVersion(
                versionId = "en_mahmoud-ghali",
                displayName = "Mahmoud Ghali",
                bookName = "Towards Understanding the Ever-Glorious Quran",
                authorName = "Dr. Mahmoud Ghali",
                languageCode = "en",
                languageName = "English",
                shortDescription = "Egyptian Scholar, Clear Arabic.",
                downloadPath = "apis/translations/en/en_mahmoud-ghali.json",
                isPrebuilt = false,
                isDownloaded = false
            )
        )
    }
    
    /**
     * 印尼语版本 (Indonesian)
     * 顺序优化：1. Tafsir Al-Qur'an Kemenag, 2. Kompleks Al Quran Raja Fahd
     */
    private fun getIndonesianVersions(): List<QuranTranslationVersion> {
        return listOf(
            // 第1位：Tafsir Al-Qur'an Kemenag
            QuranTranslationVersion(
                versionId = "id_indonesian_islamic_affairs",
                displayName = "Tafsir Al-Qur'an Kemenag",
                bookName = "Tafsir Al-Qur'an Indonesia",
                authorName = "Kementerian Agama RI",
                languageCode = "id",
                languageName = "Bahasa Indonesia",
                shortDescription = "Penjelasan Mendalam, Kontemporer.",
                downloadPath = "apis/translations/in/id_indonesian_islamic_affairs.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            // 第2位：Kompleks Al Quran Raja Fahd（预装版本）
            QuranTranslationVersion(
                versionId = TranslUtils.TRANSL_SLUG_IN,
                displayName = "Kompleks Al Quran Raja Fahd",
                bookName = "Al-Qur'an Terjemahan",
                authorName = "Kompleks Al Quran Raja Fahd",
                languageCode = "id",
                languageName = "Bahasa Indonesia",
                shortDescription = "Teks Resmi Pemerintah.",
                downloadPath = null,
                isPrebuilt = true,
                isDownloaded = true
            )
        )
    }
    
    /**
     * 阿拉伯语版本 (Arabic) - 原文（不显示作者和说明）
     */
    private fun getArabicVersions(): List<QuranTranslationVersion> {
        return listOf(
            // 阿拉伯原文（内置）
            QuranTranslationVersion(
                versionId = "quran_arabic_text",
                displayName = "القرآن الكريم",
                bookName = "القرآن الكريم",
                authorName = null,  // 原文不显示作者
                languageCode = "ar",
                languageName = "العربية",
                shortDescription = null,  // 原文不显示说明
                downloadPath = null,
                isPrebuilt = true,
                isDownloaded = true
            )
        )
    }
    
    /**
     * 乌尔都语版本 (Urdu)
     * 顺序优化：1. تفہیم القرآن, 2. ڈاکٹر اسرار احمد, 3. مولانا محمد جوناگڑھی
     */
    private fun getUrduVersions(): List<QuranTranslationVersion> {
        return listOf(
            // 第1位：تفہیم القرآن
            QuranTranslationVersion(
                versionId = "ur_maududi",
                displayName = "تفہیم القرآن",
                bookName = "تفہیم القرآن",
                authorName = "سید ابوالاعلی مودودی",
                languageCode = "ur",
                languageName = "اردو",
                shortDescription = "تفصیلی تفسیر اور وضاحت.",
                downloadPath = "apis/translations/ur/ur_maududi.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            // 第2位：ڈاکٹر اسرار احمد
            QuranTranslationVersion(
                versionId = "ur_dr_israr_ahmad",
                displayName = "ڈاکٹر اسرار احمد",
                bookName = "بیان القرآن",
                authorName = "ڈاکٹر اسرار احمد",
                languageCode = "ur",
                languageName = "اردو",
                shortDescription = "سلیس، عصری اُردو.",
                downloadPath = "apis/translations/ur/ur_dr_israr_ahmad.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            // 第3位：مولانا محمد جوناگڑھی（预装版本）
            QuranTranslationVersion(
                versionId = TranslUtils.TRANSL_SLUG_UR_JUNAGARHI,
                displayName = "مولانا محمد جوناگڑھی",
                bookName = "ترجمہ شیخ الاسلام",
                authorName = "مولانا محمد جوناگڑھی",
                languageCode = "ur",
                languageName = "اردو",
                shortDescription = "برصغیر کی معروف ترجمانی.",
                downloadPath = null,
                isPrebuilt = true,
                isDownloaded = true
            )
        )
    }
    
    /**
     * 马来语版本 (Malay)
     */
    private fun getMalayVersions(): List<QuranTranslationVersion> {
        return listOf(
            QuranTranslationVersion(
                versionId = "ms_jakim",
                displayName = "JAKIM",
                bookName = "Al-Quran JAKIM",
                authorName = "Jabatan Kemajuan Islam Malaysia",
                languageCode = "ms",
                languageName = "Bahasa Melayu",
                shortDescription = "Terjemahan Rasmi Malaysia.",
                downloadPath = "apis/translations/ms/ms_jakim.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            QuranTranslationVersion(
                versionId = "ms_abdullah_muhammad_basmeih",
                displayName = "Abdullah Muhammad Basmeih",
                bookName = "Tafsir Pimpinan Ar-Rahman",
                authorName = "Abdullah Muhammad Basmeih",
                languageCode = "ms",
                languageName = "Bahasa Melayu",
                shortDescription = "Klasik, Melayu Tradisional.",
                downloadPath = "apis/translations/ms/ms_abdullah_muhammad_basmeih.json",
                isPrebuilt = false,
                isDownloaded = false
            )
        )
    }
    
    /**
     * 土耳其语版本 (Turkish)
     */
    private fun getTurkishVersions(): List<QuranTranslationVersion> {
        return listOf(
            QuranTranslationVersion(
                versionId = "tr_diyanet_isleri",
                displayName = "Diyanet İşleri",
                bookName = "Kur'an-ı Kerim Meali",
                authorName = "Türkiye Cumhuriyeti Diyanet İşleri Başkanlığı",
                languageCode = "tr",
                languageName = "Türkçe",
                shortDescription = "Türkiye Resmi Çevirisi.",
                downloadPath = "apis/translations/tr/tr_diyanet_isleri.json",
                isPrebuilt = false,
                isDownloaded = false
            ),
            QuranTranslationVersion(
                versionId = "tr_elmalili_hamdi_yazir",
                displayName = "Elmalılı Hamdi Yazır",
                bookName = "Hak Dini Kur'an Dili",
                authorName = "Elmalılı Hamdi Yazır",
                languageCode = "tr",
                languageName = "Türkçe",
                shortDescription = "Klasik, Tarihsel Perspektif.",
                downloadPath = "apis/translations/tr/tr_elmalili_hamdi_yazir.json",
                isPrebuilt = false,
                isDownloaded = false
            )
        )
    }
    
    /**
     * 孟加拉语版本 (Bengali)
     */
    private fun getBengaliVersions(): List<QuranTranslationVersion> {
        return listOf(
            QuranTranslationVersion(
                versionId = "bn_muhiuddin_khan",
                displayName = "মুহিউদ্দিন খান",
                bookName = "পবিত্র কুরআন শরীফ",
                authorName = "মাওলানা মুহিউদ্দিন খান",
                languageCode = "bn",
                languageName = "বাংলা",
                shortDescription = "বাংলাদেশে সুপরিচিত অনুবাদ.",
                downloadPath = "apis/translations/bn/bn_muhiuddin_khan.json",
                isPrebuilt = false,
                isDownloaded = false
            )
        )
    }
    
    /**
     * 合并本地版本和API版本
     * 
     * 规则：
     * 1. 本地版本优先，保持原有顺序和内容不变
     * 2. 只添加API中真正"新的"版本（本地不存在的versionId）
     * 3. 新版本追加到列表最后
     * 4. 去重：以 versionId 为唯一标识
     * 
     * @param localVersions 本地版本列表
     * @param apiVersions API版本列表
     * @return 合并后的版本列表
     */
    fun mergeVersions(
        localVersions: List<QuranTranslationVersion>,
        apiVersions: List<QuranTranslationVersion>
    ): List<QuranTranslationVersion> {
        val result = mutableListOf<QuranTranslationVersion>()
        val localVersionIds = localVersions.map { it.versionId }.toSet()
        
        // 1. 先添加所有本地版本（保持原有顺序）
        result.addAll(localVersions)
        
        // 2. 只添加API中的"新"版本（本地不存在的）到列表最后
        apiVersions.forEach { apiVersion ->
            if (apiVersion.versionId !in localVersionIds) {
                result.add(apiVersion)
            }
        }
        
        // 3. 不排序，保持本地定义的顺序
        return result
    }
}

