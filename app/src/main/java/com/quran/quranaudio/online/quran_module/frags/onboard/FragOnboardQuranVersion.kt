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
import com.quran.quranaudio.online.quran_module.models.QuranTranslationVersion
import com.quran.quranaudio.online.quran_module.utils.reader.TranslUtils
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
        val nameText: TextView
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
     * 从服务端加载翻译版本列表（优先使用缓存）
     */
    private fun loadTranslationVersions() {
        showLoading(true)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("FragOnboardQuranVersion", "🔄 Loading translation versions for: $selectedLanguageCode")
                
                // 🚀 方案0：优先从缓存加载
                var translations = TranslationCacheManager.getTranslations(
                    requireContext(),
                    selectedLanguageCode,
                    forceRefresh = false
                )
                
                if (translations != null) {
                    android.util.Log.d("FragOnboardQuranVersion", "⚡ Loaded from cache: ${translations.size} translations")
                } else {
                    android.util.Log.d("FragOnboardQuranVersion", "📡 Cache miss, fetching from API...")
                    
                    // 方案1：尝试从主API获取
                    translations = try {
                        android.util.Log.d("FragOnboardQuranVersion", "📡 Trying primary API...")
                        val responseBody = RetrofitInstance.github.getAvailableTranslations()
                        val jsonString = responseBody.string()
                        parseTranslationsJson(jsonString, selectedLanguageCode)
                    } catch (primaryError: Exception) {
                        android.util.Log.e("FragOnboardQuranVersion", "❌ Primary API failed: ${primaryError.message}", primaryError)
                        
                        // 方案2：尝试备用API (Quran Foundation)
                        try {
                            android.util.Log.d("FragOnboardQuranVersion", "📡 Trying fallback API (Quran Foundation)...")
                            // 🔑 关键修复：传递语言参数，让 API 只返回指定语言的翻译
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
                            android.util.Log.d("FragOnboardQuranVersion", "📍 Requesting translations for language: $apiLanguage (code: $selectedLanguageCode)")
                            
                            val responseBody = RetrofitInstance.quranFoundation.getTranslations(apiLanguage)
                            val jsonString = responseBody.string()
                            parseQuranFoundationTranslations(jsonString, selectedLanguageCode)
                        } catch (fallbackError: Exception) {
                            android.util.Log.e("FragOnboardQuranVersion", "❌ Fallback API also failed: ${fallbackError.message}", fallbackError)
                            
                            // 方案3：使用预装版本作为最后的fallback
                            android.util.Log.d("FragOnboardQuranVersion", "📦 Using prebuilt versions as final fallback")
                            emptyList()
                        }
                    }
                }
                
                // 使用安全的空值处理
                val finalTranslations = translations ?: emptyList()
                
                android.util.Log.d("FragOnboardQuranVersion", "✅ Loaded ${finalTranslations.size} translations for language: $selectedLanguageCode")
                
                // 【调试】打印所有翻译的语言代码
                finalTranslations.forEach { 
                    android.util.Log.d("FragOnboardQuranVersion", "  📖 Translation: ${it.displayName} (lang: ${it.languageCode})")
                }
                
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    
                    if (finalTranslations.isEmpty()) {
                        // 没有API数据，使用预装版本
                        android.util.Log.w("FragOnboardQuranVersion", "⚠️ No translations from API, loading prebuilt versions for: $selectedLanguageCode")
                        loadPrebuiltVersions()
                    } else {
                        // 合并API数据和预装版本（缓存已包含预装版本，无需重复添加）
                        availableVersions.clear()
                        availableVersions.addAll(finalTranslations)
                        
                        // 去重（以versionId为key）
                        val uniqueVersions = availableVersions.distinctBy { it.versionId }
                        availableVersions.clear()
                        availableVersions.addAll(uniqueVersions.sortedBy { it.displayName })
                        
                        android.util.Log.d("FragOnboardQuranVersion", "📊 Total versions to display: ${availableVersions.size}")
                        displayTranslationVersions()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("FragOnboardQuranVersion", "❌ Unexpected error in loadTranslationVersions", e)
                
                withContext(Dispatchers.Main) {
                    showLoading(false)
                    showError("Failed to load translations.")
                    
                    // 最终fallback：显示预装版本
                    loadPrebuiltVersions()
                }
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
            for (translationElement in translationsArray) {
                val translObj = translationElement.jsonObject
                
                // 获取语言名称
                val langName = translObj["language_name"]?.jsonPrimitive?.content ?: ""
                
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
                } else {
                    // 记录不匹配的翻译（仅在调试时）
                    if (matchedCount == 0 && translations.size < 3) {
                        android.util.Log.d("FragOnboardQuranVersion", "  ⏭️ Skipped: language_name='$langName' (expected: '$targetLanguage')")
                    }
                }
            }
            
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
     * 创建版本选择卡片
     */
    private fun createVersionCard(version: QuranTranslationVersion): View {
        val inflater = LayoutInflater.from(requireContext())
        val cardView = inflater.inflate(R.layout.item_quran_version_card, binding.containerVersions, false) as MaterialCardView
        
        val nameText = cardView.findViewById<TextView>(R.id.tv_version_name)
        val checkIcon = cardView.findViewById<View>(R.id.icon_check)
        
        nameText.text = version.getDisplayText()
        
        // 保存视图引用
        versionCardViews[version.versionId] = VersionCardViews(cardView, checkIcon, nameText)
        
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
        val unselectedBgColor = ContextCompat.getColor(requireContext(), R.color.language_card_unselected_bg)
        val strokeWidth = resources.getDimensionPixelSize(R.dimen.language_card_stroke_width)
        
        versionCardViews.forEach { (versionId, views) ->
            val isSelected = (versionId == selectedVersionId)
            
            if (isSelected) {
                // ✅ 选中状态：白色背景 + 绿色边框 + 绿色文字 + 绿色对勾
                views.card.setCardBackgroundColor(whiteColor)
                views.card.strokeColor = primaryColor
                views.card.strokeWidth = strokeWidth
                views.nameText.setTextColor(primaryColor)
                views.checkIcon.visibility = View.VISIBLE
            } else {
                // ⭕ 未选中状态：深绿色背景 + 无边框 + 白色文字 + 对勾隐藏
                views.card.setCardBackgroundColor(unselectedBgColor)
                views.card.strokeWidth = 0
                views.nameText.setTextColor(whiteColor)
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
        android.util.Log.d("FragOnboardQuranVersion", "   ✅ 已保存到 SharedPreferences:")
        android.util.Log.d("FragOnboardQuranVersion", "   Key: ${TranslUtils.KEY_TRANSLATIONS}")
        android.util.Log.d("FragOnboardQuranVersion", "   Value: $saved")
        android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
    }
    
    /**
     * 启动下载
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
                // 从主API下载
                val downloadUrl = version.getFullDownloadUrl()
                android.util.Log.d("FragOnboardQuranVersion", "   📡 下载源: 主 API")
                android.util.Log.d("FragOnboardQuranVersion", "   下载 URL: $downloadUrl")
                
                val intent = Intent(requireContext(), TranslationDownloadService::class.java).apply {
                    putExtra("translation_slug", version.versionId)
                    putExtra("translation_name", version.displayName)
                    putExtra("download_url", downloadUrl)
                }
                requireContext().startService(intent)
                
                android.util.Log.d("FragOnboardQuranVersion", "   ✅ 后台下载服务已启动")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.util.Log.e("FragOnboardQuranVersion", "   ❌ 启动下载失败: ${e.message}", e)
        }
        android.util.Log.d("FragOnboardQuranVersion", "═══════════════════════════════════════════════")
    }
    
    /**
     * 从 Quran Foundation API 下载翻译
     */
    private fun downloadFromQuranFoundation(version: QuranTranslationVersion) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                android.util.Log.d("FragOnboardQuranVersion", "🔄 Fetching translation from Quran Foundation API: ID ${version.numericId}")
                
                // 调用 Quran Foundation API 获取完整翻译数据
                val responseBody = RetrofitInstance.quranFoundation.getQuranTranslation(version.numericId)
                val jsonString = responseBody.string()
                
                // 保存到本地文件
                val fileUtils = FileUtils.newInstance(requireContext())
                val localFile = File(fileUtils.translationDir, version.getLocalFileName())
                localFile.writeText(jsonString)
                
                android.util.Log.d("FragOnboardQuranVersion", "✅ Translation downloaded successfully: ${localFile.absolutePath}")
                
                withContext(Dispatchers.Main) {
                    // 可以在这里显示下载成功的提示
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Translation downloaded: ${version.displayName}",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                android.util.Log.e("FragOnboardQuranVersion", "❌ Failed to download from Quran Foundation API", e)
                
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(
                        requireContext(),
                        "Failed to download translation",
                        android.widget.Toast.LENGTH_SHORT
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

