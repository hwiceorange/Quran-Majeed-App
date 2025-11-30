package com.quran.quranaudio.online.quran_module.frags.onboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.quran.quranaudio.online.R
import com.quran.quranaudio.online.databinding.FragmentOnboardQuranVersionSelectionBinding
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants
import com.quran.quranaudio.online.prayertimes.ui.MainActivity
import com.quran.quranaudio.online.quran_module.api.RetrofitInstance
import com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo
import com.quran.quranaudio.online.quran_module.data.LocalTranslationData
import com.quran.quranaudio.online.quran_module.models.QuranTranslationVersion
import com.quran.quranaudio.online.quran_module.utils.reader.TranslUtils
import com.quran.quranaudio.online.quran_module.utils.reader.factory.QuranTranslationFactory
import com.quran.quranaudio.online.quran_module.utils.services.TranslationDownloadService
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs
import com.quran.quranaudio.online.quran_module.utils.univ.FileUtils
import com.quran.quranaudio.online.quran_module.utils.TranslationCacheManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * 🕌 古兰经翻译版本选择引导页
 * 
 * 功能：
 * 1. 根据用户选择的语言，从服务端获取对应语言的古兰经翻译版本列表
 * 2. 显示可用的翻译版本供用户选择
 * 3. 用户选择后，保存首选版本并开始后台下载
 * 4. 导航到下一个引导步骤（不阻塞下载过程）
 */
class FragOnboardQuranVersion : FragOnboardBase() {
    
    private var _binding: FragmentOnboardQuranVersionSelectionBinding? = null
    private val binding get() = _binding!!
    
    // 用户在上一步选择的语言代码
    private var selectedLanguageCode: String = ""  // 初始为空字符串，首次加载时会更新
    
    // 可用的翻译版本列表
    private val availableVersions = mutableListOf<QuranTranslationVersion>()
    
    // 当前选中的版本
    private var selectedVersion: QuranTranslationVersion? = null
    
    // 版本卡片映射
    private val versionCardViews = mutableMapOf<String, VersionCardViews>()
    
    private data class VersionCardViews(
        val card: MaterialCardView,
        val checkIcon: View,
        val nameText: TextView,
        val descriptionText: TextView
    )
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardQuranVersionSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupContinueButton()
        // 不在这里加载数据，等到 onResume
    }
    
    override fun onResume() {
        super.onResume()
        
        // 从上一步获取选择的语言代码
        val currentLanguageCode = SPAppConfigs.getLocale(requireContext())
        
        // 如果语言改变了，或者是第一次加载，重新加载数据
        if (selectedLanguageCode != currentLanguageCode || availableVersions.isEmpty()) {
            selectedLanguageCode = currentLanguageCode
            loadTranslationVersions()
        }
    }
    
    /**
     * 从服务端加载翻译版本列表（优先使用本地数据，后台加载API）
     * 
     * 优化方案：
     * 1. 立即显示本地数据（无等待）
     * 2. 后台异步加载API数据
     * 3. 合并并更新列表
     */
    private fun loadTranslationVersions() {
        android.util.Log.d("FragOnboardQuranVersion", "🚀 Fast loading: displaying local data immediately")
        android.util.Log.d("FragOnboardQuranVersion", "📍 Selected language: $selectedLanguageCode")
        
        // ⚡ 步骤1：立即加载并显示本地数据（无需等待，用户体验优先）
        val localVersions = LocalTranslationData.getVersions(selectedLanguageCode)
        android.util.Log.d("FragOnboardQuranVersion", "✅ Local data loaded: ${localVersions.size} versions")
        
        // 立即显示本地版本
        availableVersions.clear()
        availableVersions.addAll(localVersions)
        displayTranslationVersions()
        
        // 🌐 步骤2：后台异步加载API数据（不阻塞UI）
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("FragOnboardQuranVersion", "🔄 Background: fetching API data for: $selectedLanguageCode")
                
                // 方案A：优先从缓存加载
                var apiTranslations = TranslationCacheManager.getTranslations(
                    requireContext(),
                    selectedLanguageCode,
                    forceRefresh = false
                )
                
                if (apiTranslations != null) {
                    android.util.Log.d("FragOnboardQuranVersion", "⚡ API data from cache: ${apiTranslations.size} translations")
                } else {
                    // 方案B：从主API获取
                    apiTranslations = try {
                        android.util.Log.d("FragOnboardQuranVersion", "📡 Fetching from primary API...")
                        val responseBody = RetrofitInstance.github.getAvailableTranslations()
                        val jsonString = responseBody.string()
                        parseTranslationsJson(jsonString, selectedLanguageCode)
                    } catch (primaryError: Exception) {
                        android.util.Log.e("FragOnboardQuranVersion", "❌ Primary API failed: ${primaryError.message}")
                        
                        // 方案C：尝试备用API
                        try {
                            android.util.Log.d("FragOnboardQuranVersion", "📡 Trying fallback API...")
                            val languageMap = mapOf(
                                "en" to "english",
                                "id" to "indonesian",
                                "ar" to "arabic",
                                "ur" to "urdu",
                                "ms" to "malay",
                                "tr" to "turkish",
                                "bn" to "bengali"
                            )
                            val apiLanguage = languageMap[selectedLanguageCode] ?: "english"
                            val responseBody = RetrofitInstance.quranFoundation.getTranslations(apiLanguage)
                            val jsonString = responseBody.string()
                            parseQuranFoundationTranslations(jsonString, selectedLanguageCode)
                        } catch (fallbackError: Exception) {
                            android.util.Log.e("FragOnboardQuranVersion", "❌ Fallback API failed: ${fallbackError.message}")
                            emptyList()
                        }
                    }
                }
                
                // 使用安全的空值处理
                val finalApiTranslations = apiTranslations ?: emptyList()
                
                withContext(Dispatchers.Main) {
                    if (finalApiTranslations.isNotEmpty()) {
                        // 🔄 步骤3：合并本地数据和API数据
                        android.util.Log.d("FragOnboardQuranVersion", "🔄 Merging local (${localVersions.size}) + API (${finalApiTranslations.size}) versions")
                        val mergedVersions = LocalTranslationData.mergeVersions(localVersions, finalApiTranslations)
                        
                        android.util.Log.d("FragOnboardQuranVersion", "✅ Merged total: ${mergedVersions.size} versions")
                        
                        // 只有真正有新版本时才刷新UI
                        if (mergedVersions.size > localVersions.size) {
                            android.util.Log.d("FragOnboardQuranVersion", "🔄 Found ${mergedVersions.size - localVersions.size} new versions, updating UI")
                            
                            availableVersions.clear()
                            availableVersions.addAll(mergedVersions) // 不排序，保持本地顺序
                            
                            // 刷新UI显示新版本
                            displayTranslationVersions()
                        } else {
                            android.util.Log.d("FragOnboardQuranVersion", "✓ No new versions from API, keeping local data unchanged")
                        }
                    } else {
                        // API无数据，保持本地数据
                        android.util.Log.d("FragOnboardQuranVersion", "ℹ️ No API data, keeping local versions displayed")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("FragOnboardQuranVersion", "❌ Background API load failed, but local data already displayed: ${e.message}")
                // 用户已经看到本地数据，不影响用户体验
            }
        }
    }
    
    /**
     * 解析 Quran Foundation API 返回的翻译列表
     * API响应格式：
     * {
     *   "translations": [
     *     {
     *       "id": 131,
     *       "name": "Saheeh International",
     *       "author_name": "Saheeh International",
     *       "slug": "saheeh-international",
     *       "language_name": "english",
     *       "translated_name": { "name": "Saheeh International", "language_name": "english" }
     *     }
     *   ]
     * }
     */
    private fun parseQuranFoundationTranslations(jsonString: String, languageCode: String): List<QuranTranslationVersion> {
        val json = Json { ignoreUnknownKeys = true }
        val translations = mutableListOf<QuranTranslationVersion>()
        
        try {
            val jsonElement = json.parseToJsonElement(jsonString)
            val jsonObject = jsonElement.jsonObject
            val translationsArray = jsonObject["translations"]?.jsonArray ?: return emptyList()
            
            // 语言代码映射 (Quran Foundation API 使用全称)
            val languageMap = mapOf(
                "en" to "english",
                "id" to "indonesian",  // 应用统一使用 "id" 表示印尼语
                "ar" to "arabic",
                "ur" to "urdu",
                "ms" to "malay",
                "tr" to "turkish",
                "bn" to "bengali"
            )
            
            val targetLanguage = languageMap[languageCode] ?: "english"
            
            android.util.Log.d("FragOnboardQuranVersion", "🔍 Filtering translations for target language: '$targetLanguage' (from code: '$languageCode')")
            android.util.Log.d("FragOnboardQuranVersion", "📊 Total translations in API response: ${translationsArray.size}")
            
            var matchedCount = 0
            val uniqueLanguages = mutableSetOf<String>()
            
            for (translationElement in translationsArray) {
                val translObj = translationElement.jsonObject
                
                // 获取语言名称
                val langName = translObj["language_name"]?.jsonPrimitive?.content ?: ""
                uniqueLanguages.add(langName)
                
                // 只选择匹配语言的翻译
                if (langName.equals(targetLanguage, ignoreCase = true)) {
                    matchedCount++
                    val id = translObj["id"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val name = translObj["name"]?.jsonPrimitive?.content ?: ""
                    val authorName = translObj["author_name"]?.jsonPrimitive?.content ?: ""
                    val slug = translObj["slug"]?.jsonPrimitive?.content ?: ""
                    
                    val version = QuranTranslationVersion(
                        versionId = slug.ifEmpty { "qf_$id" },
                        displayName = name.ifEmpty { "Translation $id" },
                        bookName = name,
                        authorName = authorName,
                        languageCode = languageCode,
                        languageName = langName,
                        downloadPath = null, // Quran Foundation API 需要单独获取
                        numericId = id,
                        isQuranFoundationApi = true // 标记为来自 Quran Foundation API
                    )
                    
                    // 检查是否已下载或预装
                    version.isDownloaded = checkIfDownloaded(version)
                    version.isPrebuilt = checkIfPrebuilt(version)
                    
                    translations.add(version)
                    
                    android.util.Log.d("FragOnboardQuranVersion", "  ✅ Found: $name ($langName)")
                }
            }
            
            // 记录所有发现的语言（帮助调试）
            android.util.Log.d("FragOnboardQuranVersion", "📋 All available languages in API: ${uniqueLanguages.sorted().joinToString(", ")}")
            
            android.util.Log.d("FragOnboardQuranVersion", "📊 Matched $matchedCount translations for '$targetLanguage' from Quran Foundation API (total parsed: ${translations.size})")
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("FragOnboardQuranVersion", "Failed to parse Quran Foundation translations JSON", e)
        }
        
        return translations.sortedBy { it.displayName }
    }
    
    /**
     * 解析翻译JSON数据
     * API返回格式：
     * {
     *   "translations": {
     *     "en": {
     *       "en_sahih-international": { "book": "...", "author": "...", ... },
     *       "en_pickthall": { ... }
     *     }
     *   }
     * }
     */
    private fun parseTranslationsJson(jsonString: String, languageCode: String): List<QuranTranslationVersion> {
        val json = Json { ignoreUnknownKeys = true }
        val translations = mutableListOf<QuranTranslationVersion>()
        
        try {
            val jsonElement = json.parseToJsonElement(jsonString)
            val rootObject = jsonElement.jsonObject
            
            // 获取 translations 对象
            val translationsObject = rootObject["translations"]?.jsonObject
            
            if (translationsObject == null) {
                android.util.Log.e("FragOnboardQuranVersion", "❌ STEP 2 ERROR: 'translations' key not found in API response!")
                android.util.Log.e("FragOnboardQuranVersion", "   Root object keys: ${rootObject.keys}")
                return emptyList()
            }
            
            // 语言代码映射：应用使用 "id"，但主 API 使用 "in"
            val normalizedLangCode = when (languageCode) {
                "id" -> "in"  // 印尼语：应用用 id，API 用 in
                else -> languageCode
            }
            
            android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
            android.util.Log.d("FragOnboardQuranVersion", "🔄 STEP 2: 从API获取翻译数据")
            android.util.Log.d("FragOnboardQuranVersion", "   语言代码: app='$languageCode' → API='$normalizedLangCode'")
            android.util.Log.d("FragOnboardQuranVersion", "   API返回的语言键: ${translationsObject.keys.joinToString(", ")}")
            android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
            
            // 获取目标语言的翻译对象
            val langTranslations = translationsObject[normalizedLangCode]?.jsonObject
            
            if (langTranslations == null) {
                android.util.Log.e("FragOnboardQuranVersion", "❌ 语言键 '$normalizedLangCode' 在API中不存在!")
                android.util.Log.e("FragOnboardQuranVersion", "   可用的语言键: ${translationsObject.keys.joinToString(", ")}")
                android.util.Log.e("FragOnboardQuranVersion", "   → 将返回空列表，回退到其他方案")
                return emptyList()
            }
            
            android.util.Log.d("FragOnboardQuranVersion", "✅ 找到语言键 '$normalizedLangCode'，包含 ${langTranslations.size} 个翻译版本")
            
            // 遍历该语言下的所有翻译版本
            for ((slug, translationElement) in langTranslations) {
                try {
                    val translObj = translationElement.jsonObject
                    
                    // 提取字段（注意：API返回的字段名是驼峰命名，不是kebab-case）
                    val book = translObj["book"]?.jsonPrimitive?.content ?: ""
                    val author = translObj["author"]?.jsonPrimitive?.content ?: ""
                    val displayName = translObj["displayName"]?.jsonPrimitive?.content ?: book
                    val langCode = translObj["langCode"]?.jsonPrimitive?.content ?: normalizedLangCode
                    val langName = translObj["langName"]?.jsonPrimitive?.content ?: ""
                    val downloadPath = translObj["downloadPath"]?.jsonPrimitive?.content ?: ""
                    
                    val version = QuranTranslationVersion(
                        versionId = slug, // 使用key作为versionId
                        displayName = displayName.ifEmpty { book },
                        bookName = book,
                        authorName = author,
                        languageCode = languageCode, // 使用原始语言代码，保持与应用一致
                        languageName = langName,
                        downloadPath = downloadPath,
                        numericId = 0 // 这个API没有提供numericId
                    )
                    
                    // 检查是否已下载或预装
                    version.isDownloaded = checkIfDownloaded(version)
                    version.isPrebuilt = checkIfPrebuilt(version)
                    
                    translations.add(version)
                    
                    android.util.Log.d("FragOnboardQuranVersion", "  ✅ Parsed: $displayName ($slug)")
                } catch (e: Exception) {
                    android.util.Log.e("FragOnboardQuranVersion", "Failed to parse translation: $slug", e)
                }
            }
            
            android.util.Log.d("FragOnboardQuranVersion", "📊 Total parsed: ${translations.size} translations for language '$languageCode' (API key: '$normalizedLangCode')")
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("FragOnboardQuranVersion", "Failed to parse translations JSON", e)
        }
        
        return translations.sortedBy { it.displayName }
    }
    
    /**
     * 检查翻译版本是否已下载
     */
    private fun checkIfDownloaded(version: QuranTranslationVersion): Boolean {
        val fileUtils = FileUtils.newInstance(requireContext())
        val translFile = File(fileUtils.translationDir, version.getLocalFileName())
        return translFile.exists()
    }
    
    /**
     * 检查是否为预装版本
     */
    private fun checkIfPrebuilt(version: QuranTranslationVersion): Boolean {
        val prebuiltSlugs = listOf(
            TranslUtils.TRANSL_SLUG_EN_SAHIH_INTERNATIONAL,
            TranslUtils.TRANSL_SLUG_EN_THE_CLEAR_QURAN,
            TranslUtils.TRANSL_SLUG_UR_JUNAGARHI,
            TranslUtils.TRANSL_SLUG_IN
        )
        return prebuiltSlugs.contains(version.versionId)
    }
    
    /**
     * 加载预装版本（当网络失败时）
     */
    /**
     * 获取预装版本列表（不显示UI，只返回列表）
     */
    private fun getPrebuiltVersions(): List<QuranTranslationVersion> {
        val prebuiltVersions = mutableListOf<QuranTranslationVersion>()
        
        when (selectedLanguageCode) {
            "en" -> {
                prebuiltVersions.add(createPrebuiltVersion(
                    TranslUtils.TRANSL_SLUG_EN_SAHIH_INTERNATIONAL,
                    "Sahih International",
                    "en"
                ))
                prebuiltVersions.add(createPrebuiltVersion(
                    TranslUtils.TRANSL_SLUG_EN_THE_CLEAR_QURAN,
                    "The Clear Quran (Dr. Mustafa Khattab)",
                    "en"
                ))
            }
            "id" -> {  // 应用统一使用 "id" 表示印尼语
                prebuiltVersions.add(createPrebuiltVersion(
                    TranslUtils.TRANSL_SLUG_IN,
                    "Kompleks Al Quran Raja Fahd",
                    "id"
                ))
            }
            "ur" -> {
                prebuiltVersions.add(createPrebuiltVersion(
                    TranslUtils.TRANSL_SLUG_UR_JUNAGARHI,
                    "مولانا محمد جوناگڑهی",
                    "ur"
                ))
            }
            "ar" -> {
                // 阿语用户：显示阿语原文（内置，不需要下载）
                android.util.Log.d("FragOnboardQuranVersion", "✅ Adding Arabic Quran text (built-in)")
                prebuiltVersions.add(createPrebuiltVersion(
                    "quran_arabic_text",  // 特殊标识符，表示阿语原文
                    "القرآن الكريم - النص العربي",  // 阿语：古兰经 - 阿拉伯文本
                    "ar"
                ))
            }
            else -> {
                // 对于没有预装版本的语言（如土耳其语、马来语、孟加拉语），
                // 添加英语版本作为 fallback，确保用户至少能看到一些选项
                android.util.Log.d("FragOnboardQuranVersion", "⚠️ No prebuilt version for language '$selectedLanguageCode', adding English fallback")
                prebuiltVersions.add(createPrebuiltVersion(
                    TranslUtils.TRANSL_SLUG_EN_SAHIH_INTERNATIONAL,
                    "Sahih International",
                    "en"
                ))
            }
        }
        
        return prebuiltVersions
    }
    
    private fun loadPrebuiltVersions() {
        val prebuiltVersions = getPrebuiltVersions()
        
        if (prebuiltVersions.isNotEmpty()) {
            availableVersions.clear()
            availableVersions.addAll(prebuiltVersions)
            displayTranslationVersions()
        }
    }
    
    /**
     * 创建预装版本对象
     */
    private fun createPrebuiltVersion(slug: String, name: String, langCode: String): QuranTranslationVersion {
        return QuranTranslationVersion(
            versionId = slug,
            displayName = name,
            languageCode = langCode,
            languageName = "",
            isDownloaded = true,
            isPrebuilt = true
        )
    }
    
    /**
     * 显示翻译版本列表
     */
    private fun displayTranslationVersions() {
        val container = binding.containerVersions
        container.removeAllViews()
        versionCardViews.clear()
        
        android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
        android.util.Log.d("FragOnboardQuranVersion", "📋 STEP 3: 前端显示验证")
        android.util.Log.d("FragOnboardQuranVersion", "   当前选择语言: $selectedLanguageCode")
        android.util.Log.d("FragOnboardQuranVersion", "   availableVersions 总数: ${availableVersions.size}")
        
        // 打印所有版本的语言代码用于调试
        if (availableVersions.isNotEmpty()) {
            android.util.Log.d("FragOnboardQuranVersion", "   所有版本的语言代码:")
            availableVersions.forEach { version ->
                android.util.Log.d("FragOnboardQuranVersion", "     - ${version.displayName}: languageCode='${version.languageCode}'")
            }
        }
        android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
        
        // 🔍 验证：只显示与当前选择语言匹配的版本
        val filteredVersions = availableVersions.filter { version ->
            val matches = version.languageCode == selectedLanguageCode
            if (!matches) {
                android.util.Log.w("FragOnboardQuranVersion", "⚠️ 过滤掉: ${version.displayName} (其语言='${version.languageCode}', 期望='$selectedLanguageCode')")
            }
            matches
        }
        
        android.util.Log.d("FragOnboardQuranVersion", "")
        android.util.Log.d("FragOnboardQuranVersion", "✅ STEP 3 结果: 过滤后剩余 ${filteredVersions.size} 个版本")
        android.util.Log.d("FragOnboardQuranVersion", "")
        
        if (filteredVersions.isEmpty()) {
            // 显示空状态
            android.util.Log.e("FragOnboardQuranVersion", "❌ 没有找到语言 '$selectedLanguageCode' 的翻译版本!")
            showError("No translations available for the selected language.")
            return
        }
        
        // 为每个版本创建卡片
        android.util.Log.d("FragOnboardQuranVersion", "🎨 开始创建UI卡片:")
        filteredVersions.forEachIndexed { index, version ->
            android.util.Log.d("FragOnboardQuranVersion", "  ${index + 1}. ${version.displayName} (${version.languageCode})")
            val cardView = createVersionCard(version)
            container.addView(cardView)
            
            // 默认选中第一个
            if (index == 0 && selectedVersion == null) {
                selectVersion(version)
            }
        }
        android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
    }
    
    /**
     * 创建版本选择卡片（新UI：显示版本名 — 作者 + 简短说明）
     * 特殊处理：阿拉伯语版本不显示作者和描述
     */
    private fun createVersionCard(version: QuranTranslationVersion): View {
        val cardView = MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_medium)
            }
            radius = resources.getDimension(R.dimen.main_item_card_borders)
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.language_card_unselected_bg))
        }
        
        // 创建内部容器
        val container = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                resources.getDimensionPixelSize(R.dimen.spacing_large),
                resources.getDimensionPixelSize(R.dimen.spacing_large),
                resources.getDimensionPixelSize(R.dimen.spacing_large),
                resources.getDimensionPixelSize(R.dimen.spacing_large)
            )
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        
        // 左侧：文本内容（垂直布局）
        val textContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }
        
        // 判断是否为阿拉伯语原文（不显示作者和描述）
        val isArabicOriginal = version.languageCode == "ar" && version.authorName.isNullOrEmpty()
        
        // 主标题：版本名称 或 版本名称 — 作者
        val titleText = TextView(requireContext()).apply {
            val titleString = if (isArabicOriginal) {
                // 阿拉伯语原文：只显示版本名称
                version.displayName
            } else {
                // 其他版本：显示版本名称 — 作者
                "${version.displayName} — ${version.authorName ?: ""}"
            }
            text = titleString
            textSize = 16f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }
        
        // 副标题：简短说明（阿拉伯语原文不显示）
        val descriptionText = TextView(requireContext()).apply {
            text = version.shortDescription ?: ""
            textSize = 13f
            // 使用80%透明度的白色，与主标题形成视觉层次
            setTextColor(android.graphics.Color.argb(204, 255, 255, 255)) // 80% white (0.8 * 255 = 204)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
            }
            visibility = if (isArabicOriginal || version.shortDescription.isNullOrEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
        
        textContainer.addView(titleText)
        textContainer.addView(descriptionText)
        
        // 右侧：选中图标
        val checkIcon = View(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.dmnActionButtonTiny),
                resources.getDimensionPixelSize(R.dimen.dmnActionButtonTiny)
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.spacing_medium)
            }
            setBackgroundResource(R.drawable.ic_check_circle)
            visibility = View.GONE
        }
        
        container.addView(textContainer)
        container.addView(checkIcon)
        cardView.addView(container)
        
        // 保存视图引用（包含标题和描述文本）
        versionCardViews[version.versionId] = VersionCardViews(cardView, checkIcon, titleText, descriptionText)
        
        // 设置点击事件
        cardView.setOnClickListener {
            selectVersion(version)
        }
        
        return cardView
    }
    
    /**
     * 选择版本
     */
    private fun selectVersion(version: QuranTranslationVersion) {
        android.util.Log.d("FragOnboardQuranVersion", "🔘 Version selected: ${version.displayName}")
        
        selectedVersion = version
        
        // 更新UI
        updateVersionSelection(version.versionId)
        
        // 启用继续按钮
        binding.btnContinue.isEnabled = true
    }
    
    /**
     * 更新版本选择状态
     */
    private fun updateVersionSelection(selectedVersionId: String) {
        val primaryColor = ContextCompat.getColor(requireContext(), R.color.primary_color)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        val whiteColor80 = android.graphics.Color.argb(204, 255, 255, 255) // 80% white
        val unselectedBgColor = ContextCompat.getColor(requireContext(), R.color.language_card_unselected_bg)
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.language_card_stroke_width)
        
        versionCardViews.forEach { (versionId, views) ->
            val isSelected = (versionId == selectedVersionId)
            
            if (isSelected) {
                // ✅ 选中状态：白色背景 + 绿色边框 + 绿色文字（标题和描述） + 绿色对勾
                views.card.setCardBackgroundColor(whiteColor)
                views.card.strokeColor = primaryColor
                views.card.strokeWidth = strokeWidth
                views.nameText.setTextColor(primaryColor)
                views.descriptionText.setTextColor(primaryColor) // 描述文本也是绿色
                views.checkIcon.visibility = View.VISIBLE
            } else {
                // ⭕ 未选中状态：深绿色背景 + 无边框 + 白色文字 + 对勾隐藏
                views.card.setCardBackgroundColor(unselectedBgColor)
                views.card.strokeWidth = 0
                views.nameText.setTextColor(whiteColor)
                views.descriptionText.setTextColor(whiteColor80) // 描述文本80%白色
                views.checkIcon.visibility = View.GONE
            }
        }
    }
    
    /**
     * 设置继续按钮
     */
    private fun setupContinueButton() {
        binding.btnContinue.setOnClickListener {
            onContinueClicked()
        }
    }
    
    /**
     * 点击继续按钮
     */
    private fun onContinueClicked() {
        val version = selectedVersion ?: return
        
        android.util.Log.d("FragOnboardQuranVersion", "🚀 Continue clicked, selected: ${version.displayName}")
        
        // 1. 保存选择的版本到偏好设置
        saveSelectedVersion(version)
        
        // 2. 如果版本未下载且不是预装版本，启动后台下载
        if (!version.isDownloaded && !version.isPrebuilt) {
            startDownload(version)
        }
        
        // 3. 导航到下一个引导页（Istiqamah）
        val activity = activity as? com.quran.quranaudio.online.quran_module.activities.ActivityOnboarding
        activity?.navigateToNextPage()
    }
    
    /**
     * 保存选择的版本
     */
    private fun saveSelectedVersion(version: QuranTranslationVersion) {
        android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
        android.util.Log.d("FragOnboardQuranVersion", "💾 STEP 5: 保存用户选择到数据库")
        android.util.Log.d("FragOnboardQuranVersion", "   版本ID: ${version.versionId}")
        android.util.Log.d("FragOnboardQuranVersion", "   版本名称: ${version.displayName}")
        android.util.Log.d("FragOnboardQuranVersion", "   语言代码: ${version.languageCode}")
        
        val prefs = requireContext().getSharedPreferences(
            TranslUtils.KEY_TRANSLATIONS,
            android.content.Context.MODE_PRIVATE
        )
        
        // 保存选中的翻译版本（作为集合，因为用户可能会选择多个）
        val selectedSlugs = setOf(version.versionId)
        prefs.edit()
            .putStringSet(TranslUtils.KEY_TRANSLATIONS, selectedSlugs)
            .apply()
        
        // 验证保存
        val saved = prefs.getStringSet(TranslUtils.KEY_TRANSLATIONS, null)
        android.util.Log.d("FragOnboardQuranVersion", "   ✅ 已保存翻译到 SharedPreferences:")
        android.util.Log.d("FragOnboardQuranVersion", "   Key: ${TranslUtils.KEY_TRANSLATIONS}")
        android.util.Log.d("FragOnboardQuranVersion", "   Value: $saved")
        
        // 🆕 同时配置对应语言的默认 Tafsir（注释）
        configureDefaultTafsir(version.languageCode)
        
        android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
    }
    
    /**
     * 根据选择的语言配置默认的 Tafsir（注释）
     */
    private fun configureDefaultTafsir(languageCode: String) {
        android.util.Log.d("FragOnboardQuranVersion", "📖 配置默认 Tafsir...")
        android.util.Log.d("FragOnboardQuranVersion", "   目标语言: $languageCode")
        
        // 准备 Tafsir Manager（加载可用的 Tafsir 列表）
        com.quran.quranaudio.online.quran_module.utils.reader.tafsir.TafsirManager.prepare(
            requireContext(),
            false  // 不强制刷新，使用缓存
        ) {
            val availableTafsirs = com.quran.quranaudio.online.quran_module.utils.reader.tafsir.TafsirManager.getModels()
            
            if (availableTafsirs.isNullOrEmpty()) {
                android.util.Log.w("FragOnboardQuranVersion", "   ⚠️ 没有可用的 Tafsir")
                return@prepare Unit
            }
            
            // 根据语言选择最佳的 Tafsir
            val tafsirKey = com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirLanguageMapper.pickBestTafsirKey(
                languageCode,
                availableTafsirs
            )
            
            if (tafsirKey != null) {
                android.util.Log.d("FragOnboardQuranVersion", "   ✅ 选择的 Tafsir: $tafsirKey")
                
                // 保存到 SharedPreferences
                com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.setSavedTafsirKey(
                    requireContext(),
                    tafsirKey
                )
                
                // 验证保存
                val savedTafsir = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTafsirKey(requireContext())
                android.util.Log.d("FragOnboardQuranVersion", "   ✅ Tafsir 已保存: $savedTafsir")
                
                // 获取 Tafsir 名称用于日志
                val tafsirName = com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirUtils.getTafsirName(tafsirKey)
                android.util.Log.d("FragOnboardQuranVersion", "   📖 Tafsir 名称: $tafsirName")
            } else {
                android.util.Log.w("FragOnboardQuranVersion", "   ⚠️ 没有找到适合语言 '$languageCode' 的 Tafsir")
            }
            
            Unit
        }
    }
    
    /**
     * 启动下载（修复：使用正确的 QuranTranslBookInfo 格式）
     */
    private fun startDownload(version: QuranTranslationVersion) {
        android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
        android.util.Log.d("FragOnboardQuranVersion", "📥 STEP 4: 开始下载古兰经翻译版本")
        android.util.Log.d("FragOnboardQuranVersion", "   版本: ${version.displayName}")
        android.util.Log.d("FragOnboardQuranVersion", "   是否已下载: ${version.isDownloaded}")
        android.util.Log.d("FragOnboardQuranVersion", "   是否预装: ${version.isPrebuilt}")
        
        try {
            if (version.isQuranFoundationApi) {
                // 从 Quran Foundation API 下载
                android.util.Log.d("FragOnboardQuranVersion", "   📡 下载源: Quran Foundation API")
                android.util.Log.d("FragOnboardQuranVersion", "   Translation ID: ${version.numericId}")
                downloadFromQuranFoundation(version)
            } else {
                // 从主API下载 - 使用正确的 QuranTranslBookInfo 格式
                val downloadPath = version.downloadPath ?: ""
                android.util.Log.d("FragOnboardQuranVersion", "   📡 下载源: 主 API")
                android.util.Log.d("FragOnboardQuranVersion", "   下载路径: $downloadPath")
                
                // 创建 QuranTranslBookInfo 对象（TranslationDownloadService 需要此格式）
                val bookInfo = com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo(version.versionId).apply {
                    bookName = version.bookName ?: version.displayName
                    authorName = version.authorName ?: ""
                    displayName = version.displayName
                    langName = version.languageName
                    langCode = version.languageCode
                    this.downloadPath = downloadPath
                }
                
                val intent = Intent(requireContext(), TranslationDownloadService::class.java).apply {
                    putExtra(com.quran.quranaudio.online.quran_module.utils.receivers.TranslDownloadReceiver.KEY_TRANSL_BOOK_INFO, bookInfo)
                }
                requireContext().startService(intent)
                
                android.util.Log.d("FragOnboardQuranVersion", "   ✅ 后台下载服务已启动（使用QuranTranslBookInfo）")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("FragOnboardQuranVersion", "   ❌ 启动下载失败: ${e.message}", e)
        }
        android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
    }
    
    /**
     * 从 Quran Foundation API 下载翻译
     * 
     * ⚠️ 优化：逐章下载并立即保存，用户可以立即查看已下载的章节
     */
    private fun downloadFromQuranFoundation(version: QuranTranslationVersion) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
                android.util.Log.d("FragOnboardQuranVersion", "🔄 Downloading translation from Quran Foundation API")
                android.util.Log.d("FragOnboardQuranVersion", "   Translation ID: ${version.numericId}")
                android.util.Log.d("FragOnboardQuranVersion", "   Version: ${version.displayName}")
                android.util.Log.d("FragOnboardQuranVersion", "   Strategy: Download chapter by chapter, save immediately")
                
                // 创建 QuranTranslBookInfo（数据库需要）
                val bookInfo = com.quran.quranaudio.online.quran_module.components.quran.subcomponents.QuranTranslBookInfo(version.versionId).apply {
                    bookName = version.bookName ?: version.displayName
                    authorName = version.authorName ?: ""
                    displayName = version.displayName
                    langName = version.languageName
                    langCode = version.languageCode
                }
                
                android.util.Log.d("FragOnboardQuranVersion", "   📊 QuranTranslBookInfo: slug='${bookInfo.slug}'")
                
                // 打开数据库连接（保持连接以提高性能）
                val factory = QuranTranslationFactory(requireContext())
                val db = factory.dbHelper.writableDatabase
                
                try {
                    // 先保存翻译信息到元数据表
                    factory.dbHelper.storeTranslationInfo(bookInfo, db)
                    
                    // 创建翻译数据表
                    factory.dbHelper.createTranslTable(db, bookInfo)
                    
                    android.util.Log.d("FragOnboardQuranVersion", "   ✅ Database table created: ${bookInfo.slug}")
                    android.util.Log.d("FragOnboardQuranVersion", "   🚀 Starting chapter-by-chapter download...")
                    
                    var totalVerses = 0
                    
                    // 逐章下载并立即保存
                    for (chapterNo in 1..114) {
                        var retryCount = 0
                        var success = false
                        
                        while (!success && retryCount < 3) {
                            try {
                                // 下载单个章节
                                val url = "https://api.quran.com/api/v4/verses/by_chapter/$chapterNo?translations=${version.numericId}"
                                val response = java.net.URL(url).readText()
                                val json = org.json.JSONObject(response)
                                val verses = json.getJSONArray("verses")
                                
                                // 立即插入到数据库
                                for (i in 0 until verses.length()) {
                                    val verseObj = verses.getJSONObject(i)
                                    val translations = verseObj.getJSONArray("translations")
                                    
                                    if (translations.length() > 0) {
                                        val translation = translations.getJSONObject(0)
                                        val verseKey = verseObj.getString("verse_key")
                                        val verseNo = verseKey.split(":")[1].toInt()
                                        
                                        // 直接插入数据库
                                        factory.dbHelper.insertTranslationQuery(
                                            db,
                                            bookInfo.slug,
                                            chapterNo,
                                            verseNo,
                                            translation.getString("text"),
                                            "[]" // footnotes
                                        )
                                        totalVerses++
                                    }
                                }
                                
                                success = true
                                
                                // 每下载完一章就显示进度
                                if (chapterNo == 1 || chapterNo % 10 == 0 || chapterNo == 114) {
                                    android.util.Log.d("FragOnboardQuranVersion", "   📥 Progress: $chapterNo/114 chapters ($totalVerses verses) ✅ Available now!")
                                }
                                
                            } catch (e: Exception) {
                                retryCount++
                                android.util.Log.w("FragOnboardQuranVersion", "   ⚠️ Failed chapter $chapterNo (attempt $retryCount/3): ${e.message}")
                                if (retryCount < 3) {
                                    Thread.sleep(2000)
                                } else {
                                    android.util.Log.e("FragOnboardQuranVersion", "   ❌ Chapter $chapterNo failed after 3 attempts")
                                    throw e
                                }
                            }
                        }
                    }
                    
                    android.util.Log.d("FragOnboardQuranVersion", "   ✅ All chapters downloaded: 114 chapters, $totalVerses verses")
                    
                    // 验证保存的数据
                    val allTransls = factory.getAvailableTranslationBooksInfo()
                    android.util.Log.d("FragOnboardQuranVersion", "   📋 All translations in database (${allTransls.size}):")
                    allTransls.values.forEach { transl ->
                        android.util.Log.d("FragOnboardQuranVersion", "      - slug: '${transl.slug}', displayName: '${transl.displayName}'")
                    }
                    
                } finally {
                    db.close()
                    factory.close()
                }
                
                android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
                
                withContext(Dispatchers.Main) {
                    // 显示下载成功的提示
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Translation downloaded: ${version.displayName}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
                android.util.Log.e("FragOnboardQuranVersion", "❌ Failed to download from Quran Foundation API", e)
                android.util.Log.e("FragOnboardQuranVersion", "   Error: ${e.message}")
                android.util.Log.e("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Failed to download translation: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    
    /**
     * 标记引导完成
     */
    private fun markOnboardingComplete() {
        val prefs = requireContext().getSharedPreferences(
            PreferencesConstants.LOCATION,
            android.content.Context.MODE_PRIVATE
        )
        prefs.edit().putBoolean(PreferencesConstants.FIRST_LAUNCH, false).commit()
        
        android.util.Log.d("FragOnboardQuranVersion", "✅ Onboarding marked as complete")
    }
    
    /**
     * 导航到主页面
     */
    private fun navigateToMainActivity() {
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        
        android.util.Log.d("FragOnboardQuranVersion", "🏠 Navigating to MainActivity")
        startActivity(intent)
        
        // 结束 ActivityOnboarding
        activity?.finish()
    }
    
    /**
     * 显示/隐藏加载指示器
     */
    private fun showLoading(show: Boolean) {
        binding.progressLoading.visibility = if (show) View.VISIBLE else View.GONE
        binding.scrollVersions.visibility = if (show) View.GONE else View.VISIBLE
    }
    
    /**
     * 显示错误信息
     */
    private fun showError(message: String) {
        // 可以在这里显示一个错误提示
        android.util.Log.e("FragOnboardQuranVersion", "Error: $message")
        // TODO: 显示友好的错误界面
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

