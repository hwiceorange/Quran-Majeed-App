# 🌍 新用户语言选择功能 - 完整实施指南

## 📋 概述

本指南详细说明如何实现"新用户首次启动应用选择语言，并根据选择语言自动配置对应的古兰经翻译版本"功能。

**基于数据**：
- ✅ Quran.com API v4：126 个翻译，69 种语言
- ✅ 应用预装翻译：5 个（英语2个、印尼语1个、乌尔都语2个）
- ✅ 应用支持界面语言：7 种

---

## 🎯 应用支持的 7 种语言 → 翻译映射（最终版）

### 完整映射表

| 语言代码 | 语言名称 | 🥇 推荐翻译 | API ID | API Slug | 译者 | 预装状态 | 文件大小 |
|---------|---------|-----------|--------|---------|------|---------|---------|
| **en** | English | **Sahih International** | 20 | `en-sahih-international` | Saheeh International | ✅ 预装 | ~3 MB |
| **in/id** | Bahasa Indonesia | **Ministry Translation** | 33 | `quran.id` | Indonesian Ministry | ✅ 预装 | ~3 MB |
| **ur** | اردو (Urdu) | **Junagarhi** | 54 | `ur-junagarri` | Maulana Junagarhi | ✅ 预装 | ~4 MB |
| **ar** | العربية (Arabic) | **原文（无翻译）** | - | - | - | N/A | - |
| **ms** | Bahasa Melayu | **Abdullah Basmeih** | 39 | `ms-abdullah` | A.M. Basmeih | 🌐 需下载 | ~3 MB |
| **tr** | Türkçe | **Diyanet İşleri** | 77 | `quran.tr.diyanet` | Diyanet Isleri | 🌐 需下载 | ~3 MB |
| **bn** | বাংলা (Bengali) | **Taisirul Quran** | 161 | `bn-taisirul-quran` | Tawheed Publication | 🌐 需下载 | ~4 MB |

---

## 💻 完整代码实现

### 1️⃣ 添加新的翻译常量 (TranslUtils.java)

在 `TranslUtils.java` 文件中添加以下常量：

```java
// ========== 从 Quran.com API 获取的翻译 Slugs ==========

// 英语翻译（8个可用）
public static final String TRANSL_SLUG_EN_SAHIH_INTERNATIONAL_API = "en-sahih-international";  // ID: 20 🥇
public static final String TRANSL_SLUG_EN_HALEEM = "en-haleem";  // ID: 85
public static final String TRANSL_SLUG_EN_PICKTHALL_API = "quran.en.pickthall";  // ID: 19
public static final String TRANSL_SLUG_EN_YUSUF_ALI_API = "quran.en.yusufali";  // ID: 22
public static final String TRANSL_SLUG_EN_BRIDGES = "bridges-translation";  // ID: 149
public static final String TRANSL_SLUG_EN_TAQI_USMANI = "en-taqi-usmani";  // ID: 84

// 印尼语翻译
public static final String TRANSL_SLUG_ID_MINISTRY_API = "quran.id";  // ID: 33 🥇

// 乌尔都语翻译（8个可用）
public static final String TRANSL_SLUG_UR_JALANDHARI = "ur-fatah-muhammad-jalandhari";  // ID: 234 🥇
public static final String TRANSL_SLUG_UR_JUNAGARHI_API = "ur-junagarri";  // ID: 54
public static final String TRANSL_SLUG_UR_ISRAR_AHMAD_API = "bayan-ul-quran";  // ID: 158
public static final String TRANSL_SLUG_UR_SAYYID_QUTB = "urdu-sayyid-qatab";  // ID: 156
public static final String TRANSL_SLUG_UR_USMANI = "tafsir-e-usmani";  // ID: 151
public static final String TRANSL_SLUG_UR_MAUDUDI = "ur-al-maududi";  // ID: 97

// 马来语翻译
public static final String TRANSL_SLUG_MS_BASMEIH = "ms-abdullah";  // ID: 39 🥇

// 土耳其语翻译（2个可用）
public static final String TRANSL_SLUG_TR_DIYANET = "quran.tr.diyanet";  // ID: 77 🥇
public static final String TRANSL_SLUG_TR_HAMDI = "tr-hamdi";  // ID: 52

// 孟加拉语翻译（3个可用）
public static final String TRANSL_SLUG_BN_TAISIRUL = "bn-taisirul-quran";  // ID: 161 🥇
public static final String TRANSL_SLUG_BN_MUJIBUR = "bn-sheikh-mujibur-rahman";  // ID: 163
public static final String TRANSL_SLUG_BN_RAWAI = "bn-rawai-al-bayan";  // ID: 162
```

---

### 2️⃣ 添加核心方法

```java
/**
 * 🌐 根据应用语言获取推荐的翻译版本
 * 
 * 此方法返回基于 Quran.com API 的标准 slug
 * 适用于新用户语言选择功能
 * 
 * @param languageCode 应用语言代码 (en, in, ar, ur, ms, tr, bn)
 * @return 推荐的翻译 slug，阿拉伯语返回 null
 */
public static String getRecommendedTranslationForLanguage(String languageCode) {
    switch (languageCode) {
        case "en":
            // 英语：Sahih International（最流行的现代翻译）
            return TRANSL_SLUG_EN_SAHIH_INTERNATIONAL_API;  // ID: 20
            
        case "in":
        case "id":
            // 印尼语：Indonesian Ministry（官方翻译）
            return TRANSL_SLUG_ID_MINISTRY_API;  // ID: 33
            
        case "ur":
            // 乌尔都语：Junagarhi（南亚最流行）
            // 注意：API 中推荐的是 Jalandhari (ID: 234)
            // 但应用预装的是 Junagarhi，为保持一致性，使用 Junagarhi
            return TRANSL_SLUG_UR_JUNAGARHI_API;  // ID: 54
            
        case "ar":
            // 阿拉伯语：无需翻译，用户直接阅读原文
            return null;
            
        case "ms":
            // 马来语：Abdullah Basmeih（马来西亚权威翻译）
            return TRANSL_SLUG_MS_BASMEIH;  // ID: 39
            
        case "tr":
            // 土耳其语：Diyanet İşleri（土耳其宗教事务局官方）
            return TRANSL_SLUG_TR_DIYANET;  // ID: 77
            
        case "bn":
            // 孟加拉语：Taisirul Quran（最流行）
            return TRANSL_SLUG_BN_TAISIRUL;  // ID: 161
            
        default:
            // 未知语言：默认使用英语
            android.util.Log.w("TranslUtils", "⚠️ Unknown language: " + languageCode + ", using English");
            return TRANSL_SLUG_EN_SAHIH_INTERNATIONAL_API;
    }
}

/**
 * 🌐 获取翻译的显示名称（用于 UI 展示）
 */
public static String getTranslationDisplayName(String slug) {
    if (slug == null) return "Arabic Original Text";
    
    switch (slug) {
        // 英语
        case TRANSL_SLUG_EN_SAHIH_INTERNATIONAL_API:
        case TRANSL_SLUG_EN_SAHIH_INTERNATIONAL:
            return "Sahih International";
        case TRANSL_SLUG_EN_HALEEM:
            return "M.A.S. Abdel Haleem";
        case TRANSL_SLUG_EN_PICKTHALL_API:
            return "M. Pickthall";
        case TRANSL_SLUG_EN_YUSUF_ALI_API:
            return "A. Yusuf Ali";
        case TRANSL_SLUG_EN_THE_CLEAR_QURAN:
            return "The Clear Quran";
            
        // 印尼语
        case TRANSL_SLUG_ID_MINISTRY_API:
        case TRANSL_SLUG_IN:
            return "Indonesian Ministry";
            
        // 乌尔都语
        case TRANSL_SLUG_UR_JUNAGARHI_API:
        case TRANSL_SLUG_UR_JUNAGARHI:
            return "Maulana Muhammad Junagarhi";
        case TRANSL_SLUG_UR_JALANDHARI:
            return "Fatah Muhammad Jalandhari";
        case TRANSL_SLUG_UR_ISRAR_AHMAD_API:
        case TRANSL_SLUG_UR_ISRAR_AHMAD:
            return "Bayan-ul-Quran (Dr. Israr Ahmad)";
            
        // 马来语
        case TRANSL_SLUG_MS_BASMEIH:
            return "Abdullah Muhammad Basmeih";
            
        // 土耳其语
        case TRANSL_SLUG_TR_DIYANET:
            return "Diyanet İşleri (Official)";
        case TRANSL_SLUG_TR_HAMDI:
            return "Elmalili Hamdi Yazir";
            
        // 孟加拉语
        case TRANSL_SLUG_BN_TAISIRUL:
            return "Taisirul Quran";
        case TRANSL_SLUG_BN_MUJIBUR:
            return "Sheikh Mujibur Rahman";
            
        default:
            return "Unknown Translation";
    }
}

/**
 * 🌐 为新用户配置翻译（完整实现，带下载支持）
 * 
 * @param context Android Context
 * @param languageCode 用户选择的语言代码
 * @param callback 配置完成回调
 */
public static void setupTranslationForNewUser(
    Context context,
    String languageCode,
    TranslationSetupCallback callback
) {
    android.util.Log.d("TranslSetup", "🌐 Setting up translation for language: " + languageCode);
    
    String translationSlug = getRecommendedTranslationForLanguage(languageCode);
    
    // 阿拉伯语用户无需翻译
    if (translationSlug == null) {
        android.util.Log.d("TranslSetup", "🇸🇦 Arabic user - no translation needed");
        callback.onSetupComplete(null, true, "Arabic users read original text");
        return;
    }
    
    // 检查翻译是否已预装或已下载
    QuranTranslationFactory factory = new QuranTranslationFactory(context);
    boolean isAvailable = factory.isTranslationDownloaded(translationSlug);
    
    if (isAvailable) {
        // 翻译已可用（预装或之前下载过），直接启用
        Set<String> translationSet = new HashSet<>();
        translationSet.add(translationSlug);
        SPReader.setSavedTranslations(context, translationSet);
        
        String displayName = getTranslationDisplayName(translationSlug);
        android.util.Log.d("TranslSetup", "✅ Translation enabled: " + displayName + " (" + translationSlug + ")");
        callback.onSetupComplete(translationSlug, true, "Translation is available");
    } else {
        // 翻译需要下载
        String displayName = getTranslationDisplayName(translationSlug);
        android.util.Log.d("TranslSetup", "📥 Translation needs download: " + displayName + " (" + translationSlug + ")");
        callback.onSetupComplete(translationSlug, false, "Translation requires download");
    }
}

/**
 * 回调接口
 */
public interface TranslationSetupCallback {
    /**
     * @param translationSlug 配置的翻译 slug（如果是阿拉伯语则为 null）
     * @param isAvailable 翻译是否立即可用（已预装或已下载）
     * @param message 状态消息
     */
    void onSetupComplete(String translationSlug, boolean isAvailable, String message);
}
```

---

## 📊 详细的语言翻译映射

### ✅ **1. English (英语) - 8 个翻译**

#### 推荐配置
```java
case "en":
    return "en-sahih-international"; // ID: 20 🥇
```

#### 所有可用翻译
| 排名 | 翻译名称 | ID | Slug | 译者 | 状态 |
|-----|---------|----|----|------|------|
| 🥇 | Sahih International | 20 | `en-sahih-international` | Saheeh International | ✅ 预装 |
| 🥈 | M.A.S. Abdel Haleem | 85 | `en-haleem` | Abdul Haleem | 🌐 可下载 |
| 🥉 | M. Pickthall | 19 | `quran.en.pickthall` | M.M.W. Pickthall | 🌐 可下载 |
| 4 | A. Yusuf Ali | 22 | `quran.en.yusufali` | Abdullah Yusuf Ali | 🌐 可下载 |
| 5 | Bridges' Translation | 149 | `bridges-translation` | Fadel Soliman | 🌐 可下载 |
| 6 | T. Usmani | 84 | `en-taqi-usmani` | Mufti Taqi Usmani | 🌐 可下载 |
| 7 | A. Maududi | 95 | `en-al-maududi` | Abul Ala Maududi | 🌐 可下载 |
| 8 | Transliteration | 57 | `transliteration` | - | 🌐 可下载 |

**注意**: 应用预装了 `en_101_sahih-international`，需要确认与 API 的 slug `en-sahih-international` (ID: 20) 是否为同一翻译。

---

### ✅ **2. Bahasa Indonesia (印尼语) - 1 个翻译**

#### 推荐配置
```java
case "in":
case "id":
    return "quran.id"; // ID: 33 🥇
```

#### 可用翻译
| 翻译名称 | ID | Slug | 译者 | 状态 |
|---------|----|----|------|------|
| 🥇 Indonesian Ministry | 33 | `quran.id` | Indonesian Islamic Affairs Ministry | ✅ 预装 |

**注意**: 应用预装了 `in_quran-complex`，需要确认与 API 的 `quran.id` 是否为同一翻译。

---

### ✅ **3. اردو (Urdu - 乌尔都语) - 8 个翻译**

#### 推荐配置
```java
case "ur":
    return "ur-junagarri"; // ID: 54 🥇
```

#### 所有可用翻译
| 排名 | 翻译名称 | ID | Slug | 译者 | 状态 |
|-----|---------|----|----|------|------|
| 🥇 | Maulana Muhammad Junagarhi | 54 | `ur-junagarri` | Maulana Junagarhi | ✅ 预装 |
| 🥈 | Fatah Muhammad Jalandhari | 234 | `ur-fatah-muhammad-jalandhari` | Fatah M. Jalandhari | 🌐 可下载 |
| 🥉 | Bayan-ul-Quran | 158 | `bayan-ul-quran` | Dr. Israr Ahmad | ✅ 预装 |
| 4 | Fe Zilal al-Qur'an | 156 | `urdu-sayyid-qatab` | Sayyid Ibrahim Qutb | 🌐 可下载 |
| 5 | Tafsir-e-Usmani | 151 | `tafsir-e-usmani` | Mahmud al-Hasan | 🌐 可下载 |
| 6 | Tafheem e Qur'an | 97 | `ur-al-maududi` | Syed Abu Ali Maududi | 🌐 可下载 |
| 7 | Maududi (Roman Urdu) | 831 | `maududi-roman-urdu` | Abul Ala Maududi | 🌐 可下载 |
| 8 | Maulana Wahiduddin Khan | 819 | `maulana-wahid-uddin-khan-urdu` | Wahiduddin Khan | 🌐 可下载 |

**注意**: API 推荐的是 Jalandhari (ID: 234)，但应用预装的是 Junagarhi (ID: 54)。为保持一致性，建议使用预装的 Junagarhi。

---

### ✅ **4. العربية (Arabic - 阿拉伯语) - 无需翻译**

#### 推荐配置
```java
case "ar":
    return null; // 阿拉伯语用户直接阅读原文
```

**说明**: 古兰经原文就是阿拉伯语，阿拉伯语用户不需要翻译。

---

### 🌐 **5. Bahasa Melayu (马来语) - 1 个翻译**

#### 推荐配置
```java
case "ms":
    return "ms-abdullah"; // ID: 39 🥇
```

#### 可用翻译
| 翻译名称 | ID | Slug | 译者 | 状态 |
|---------|----|----|------|------|
| 🥇 Abdullah Muhammad Basmeih | 39 | `ms-abdullah` | Abdullah Muhammad Basmeih | 🌐 需下载 |

**说明**: 这是马来西亚最权威的翻译，由马来西亚政府认可。

**实施方案**:
- **方案 A**: 首次启动时自动下载（需要网络）
- **方案 B**: 提示用户下载，暂时使用英语
- **方案 C**: 预装此翻译（增加 APK ~3 MB）

---

### 🌐 **6. Türkçe (Turkish - 土耳其语) - 2 个翻译**

#### 推荐配置
```java
case "tr":
    return "quran.tr.diyanet"; // ID: 77 🥇
```

#### 所有可用翻译
| 排名 | 翻译名称 | ID | Slug | 译者 | 状态 |
|-----|---------|----|----|------|------|
| 🥇 | Turkish Translation (Diyanet) | 77 | `quran.tr.diyanet` | Diyanet Isleri | 🌐 需下载 |
| 🥈 | Elmalili Hamdi Yazir | 52 | `tr-hamdi` | Elmalili Hamdi Yazir | 🌐 可下载 |

**说明**: Diyanet İşleri 是土耳其宗教事务局的官方翻译，最权威。

---

### 🌐 **7. বাংলা (Bengali - 孟加拉语) - 3 个翻译**

#### 推荐配置
```java
case "bn":
    return "bn-taisirul-quran"; // ID: 161 🥇
```

#### 所有可用翻译
| 排名 | 翻译名称 | ID | Slug | 译者 | 状态 |
|-----|---------|----|----|------|------|
| 🥇 | Taisirul Quran | 161 | `bn-taisirul-quran` | Tawheed Publication | 🌐 需下载 |
| 🥈 | Sheikh Mujibur Rahman | 163 | `bn-sheikh-mujibur-rahman` | Darussalaam Publication | 🌐 可下载 |
| 🥉 | Rawai Al-bayan | 162 | `bn-rawai-al-bayan` | Bayaan Foundation | 🌐 可下载 |

---

## 🚀 实施流程

### 流程图

```
┌─────────────────────────────────────┐
│      用户首次启动应用                │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│     显示语言选择界面                 │
│  ┌─────────────────────────────┐   │
│  │ 🇬🇧 English                  │   │
│  │ 🇮🇩 Bahasa Indonesia         │   │
│  │ 🇸🇦 العربية (Arabic)        │   │
│  │ 🇵🇰 اردو (Urdu)             │   │
│  │ 🇲🇾 Bahasa Melayu            │   │
│  │ 🇹🇷 Türkçe                   │   │
│  │ 🇧🇩 বাংলা (Bengali)         │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│   保存语言设置到 SharedPreferences    │
│   SPAppConfigs.setLocale(context, code) │
└─────────────────────────────────────┘
                 ↓
┌─────────────────────────────────────┐
│  调用 getRecommendedTranslationForLanguage() │
│  获取推荐的翻译 slug                 │
└─────────────────────────────────────┘
                 ↓
        ┌────────────────┐
        │ 是阿拉伯语？   │
        └────────────────┘
          ↓ 是      ↓ 否
    不设置翻译    检查翻译可用性
        ↓              ↓
                ┌──────────────┐
                │ 翻译已预装？  │
                └──────────────┘
              ↓ 是      ↓ 否
         直接启用     提示下载
              ↓           ↓
              │     ┌─────────────┐
              │     │ 显示下载对话框 │
              │     └─────────────┘
              │        ↓      ↓
              │     下载   使用英语
              │        ↓      ↓
              └────────┴──────┘
                      ↓
         ┌──────────────────────┐
         │   进入应用主界面      │
         └──────────────────────┘
```

---

### 用户体验设计

#### **场景 1: 英语/印尼语/乌尔都语用户（已预装）**

```
用户选择: English
    ↓
应用检测: Sahih International 已预装
    ↓
自动启用翻译
    ↓
Toast 提示: "✅ English translation enabled"
    ↓
进入主界面 (0秒等待)
```

---

#### **场景 2: 马来语/土耳其语/孟加拉语用户（需下载）**

```
用户选择: Malay
    ↓
应用检测: Abdullah Basmeih 未预装
    ↓
显示下载对话框:

┌────────────────────────────────────────┐
│ 📥 Download Malay Translation?         │
│                                        │
│ Abdullah Muhammad Basmeih              │
│ (Malaysian Authorized Translation)     │
│                                        │
│ This will provide you with the best    │
│ reading experience in your language.   │
│                                        │
│ Download size: ~3 MB                   │
│                                        │
│ [Download Now]  [Use English Instead]  │
└────────────────────────────────────────┘
    ↓                           ↓
[Download Now]          [Use English Instead]
    ↓                           ↓
显示进度条                  使用英语 Sahih International
    ↓                           ↓
下载完成 → 自动启用            启用英语翻译
    ↓                           ↓
Toast: "✅ Malay translation ready"    Toast: "✅ English translation enabled"
    ↓                           ↓
└─────────────┬─────────────────┘
              ↓
        进入主界面
```

---

## 📱 UI 实现示例

### Activity: LanguageSelectionActivity.java/kt

```kotlin
class LanguageSelectionActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selection)
        
        setupLanguageButtons()
    }
    
    private fun setupLanguageButtons() {
        // 英语
        binding.btnEnglish.setOnClickListener {
            selectLanguage("en", "English")
        }
        
        // 印尼语
        binding.btnIndonesian.setOnClickListener {
            selectLanguage("in", "Bahasa Indonesia")
        }
        
        // 阿拉伯语
        binding.btnArabic.setOnClickListener {
            selectLanguage("ar", "العربية")
        }
        
        // 乌尔都语
        binding.btnUrdu.setOnClickListener {
            selectLanguage("ur", "اردو")
        }
        
        // 马来语
        binding.btnMalay.setOnClickListener {
            selectLanguage("ms", "Bahasa Melayu")
        }
        
        // 土耳其语
        binding.btnTurkish.setOnClickListener {
            selectLanguage("tr", "Türkçe")
        }
        
        // 孟加拉语
        binding.btnBengali.setOnClickListener {
            selectLanguage("bn", "বাংলা")
        }
    }
    
    private fun selectLanguage(languageCode: String, languageName: String) {
        // 1. 保存语言设置
        SPAppConfigs.setLocale(this, languageCode)
        Log.d("LangSelection", "🌐 User selected: $languageName ($languageCode)")
        
        // 2. 配置翻译
        TranslUtils.setupTranslationForNewUser(
            this,
            languageCode,
            object : TranslUtils.TranslationSetupCallback {
                override fun onSetupComplete(
                    translationSlug: String?,
                    isAvailable: Boolean,
                    message: String
                ) {
                    runOnUiThread {
                        handleTranslationSetup(
                            languageCode,
                            languageName,
                            translationSlug,
                            isAvailable
                        )
                    }
                }
            }
        )
    }
    
    private fun handleTranslationSetup(
        languageCode: String,
        languageName: String,
        translationSlug: String?,
        isAvailable: Boolean
    ) {
        if (translationSlug == null) {
            // 阿拉伯语用户，直接进入
            Toast.makeText(this, "✅ Arabic selected", Toast.LENGTH_SHORT).show()
            proceedToMainActivity()
            return
        }
        
        if (isAvailable) {
            // 翻译已可用，直接进入
            val displayName = TranslUtils.getTranslationDisplayName(translationSlug)
            Toast.makeText(this, "✅ $displayName enabled", Toast.LENGTH_SHORT).show()
            proceedToMainActivity()
        } else {
            // 翻译需要下载，显示下载对话框
            showTranslationDownloadDialog(languageCode, languageName, translationSlug)
        }
    }
    
    private fun showTranslationDownloadDialog(
        languageCode: String,
        languageName: String,
        translationSlug: String
    ) {
        val displayName = TranslUtils.getTranslationDisplayName(translationSlug)
        
        AlertDialog.Builder(this)
            .setTitle("Download $languageName Translation?")
            .setMessage(
                "$displayName\n\n" +
                "This will provide you with the best reading experience in your language.\n\n" +
                "Download size: ~3-4 MB"
            )
            .setPositiveButton("Download Now") { _, _ ->
                // 启动下载
                startTranslationDownload(translationSlug)
            }
            .setNegativeButton("Use English Instead") { _, _ ->
                // 使用英语作为回退
                useEnglishTranslation()
            }
            .setCancelable(false)
            .show()
    }
    
    private fun startTranslationDownload(translationSlug: String) {
        // TODO: 实现翻译下载逻辑
        // 可以使用现有的 TranslationDownloadService
        Toast.makeText(this, "📥 Downloading translation...", Toast.LENGTH_SHORT).show()
        
        // 下载完成后进入主界面
        // proceedToMainActivity()
    }
    
    private fun useEnglishTranslation() {
        val englishSlug = TranslUtils.TRANSL_SLUG_EN_SAHIH_INTERNATIONAL_API
        val translationSet = setOf(englishSlug)
        SPReader.setSavedTranslations(this, translationSet)
        
        Toast.makeText(this, "✅ English translation enabled", Toast.LENGTH_SHORT).show()
        proceedToMainActivity()
    }
    
    private fun proceedToMainActivity() {
        // 标记首次启动已完成
        SPAppActions.setFirstTime(this, false)
        
        // 进入主界面
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
```

---

## 📊 最终配置总结

### 代码配置（TranslUtils.java）

```java
/**
 * 🌐 新用户语言选择 → 翻译自动配置（最终版）
 */
public static String getRecommendedTranslationForLanguage(String languageCode) {
    switch (languageCode) {
        case "en":
            return "en-sahih-international";  // ✅ 预装，ID: 20
            
        case "in":
        case "id":
            return "quran.id";  // ✅ 预装，ID: 33
            
        case "ur":
            return "ur-junagarri";  // ✅ 预装，ID: 54
            
        case "ar":
            return null;  // 原文，无需翻译
            
        case "ms":
            return "ms-abdullah";  // 🌐 需下载，ID: 39
            
        case "tr":
            return "quran.tr.diyanet";  // 🌐 需下载，ID: 77
            
        case "bn":
            return "bn-taisirul-quran";  // 🌐 需下载，ID: 161
            
        default:
            return "en-sahih-international";  // 默认英语
    }
}
```

---

### 翻译状态概览

| 语言 | 翻译 Slug | 预装状态 | 文件大小 | 用户体验 |
|------|----------|---------|---------|---------|
| 🇬🇧 English | `en-sahih-international` | ✅ 是 | ~3 MB | ⚡ 即时可用 |
| 🇮🇩 Indonesian | `quran.id` | ✅ 是 | ~3 MB | ⚡ 即时可用 |
| 🇵🇰 Urdu | `ur-junagarri` | ✅ 是 | ~4 MB | ⚡ 即时可用 |
| 🇸🇦 Arabic | `null` (原文) | N/A | - | ⚡ 即时可用 |
| 🇲🇾 Malay | `ms-abdullah` | ❌ 否 | ~3 MB | ⏳ 需下载 |
| 🇹🇷 Turkish | `quran.tr.diyanet` | ❌ 否 | ~3 MB | ⏳ 需下载 |
| 🇧🇩 Bengali | `bn-taisirul-quran` | ❌ 否 | ~4 MB | ⏳ 需下载 |

---

## ⚠️ 重要提示：Slug 不匹配问题

### 应用预装 vs API Slugs

应用当前预装的翻译使用的 slug 与 Quran.com API 的 slug **可能不同**：

| 语言 | 应用预装 Slug | API Slug | 是否匹配 |
|------|-------------|---------|---------|
| English | `en_101_sahih-international` | `en-sahih-international` | ⚠️ 不同 |
| Indonesian | `in_quran-complex` | `quran.id` | ⚠️ 不同 |
| Urdu | `in_junagarhi` | `ur-junagarri` | ⚠️ 不同 |

**解决方案**：

1. **方案 A（推荐）**: 保持使用预装翻译的 slug
   ```java
   // 对于预装翻译，使用原有的 slug
   case "en":
       return TRANSL_SLUG_EN_SAHIH_INTERNATIONAL;  // "en_101_sahih-international"
   case "in":
       return TRANSL_SLUG_IN;  // "in_quran-complex"
   case "ur":
       return TRANSL_SLUG_UR_JUNAGARHI;  // "in_junagarhi"
   ```

2. **方案 B**: 统一使用 API slug，并重命名预装文件

---

## 📚 生成的文档列表

1. ✅ **LANGUAGE_TRANSLATION_MAPPING_COMPLETE.md** - 7 种主要语言的详细映射
2. ✅ **ALL_TRANSLATIONS_MAPPING.md** - 所有 69 种语言的完整列表
3. ✅ **NEW_USER_LANGUAGE_SELECTION_GUIDE.md** - 本文档（实施指南）
4. ✅ **translations_quran_com.json** - Quran.com API 原始数据
5. ✅ **language_translation_mapping.json** - 结构化映射数据

---

## 🎯 推荐的实施步骤

### 阶段 1：基础实现（立即可用）

1. **使用预装翻译** (en, in, ur)
2. **其他语言回退到英语** (ms, tr, bn, ar)
3. **创建语言选择界面**
4. **实现语言保存逻辑**

```java
// 简化版本 - 使用预装翻译
case "en": return TRANSL_SLUG_EN_SAHIH_INTERNATIONAL;
case "in": return TRANSL_SLUG_IN;
case "ur": return TRANSL_SLUG_UR_JUNAGARHI;
case "ar": return null;
default: return TRANSL_SLUG_EN_SAHIH_INTERNATIONAL;  // ms, tr, bn 都用英语
```

---

### 阶段 2：完整实现（带下载功能）

1. **集成翻译下载功能** (ms, tr, bn)
2. **添加下载对话框**
3. **实现下载进度显示**
4. **处理下载失败情况**

---

### 阶段 3：优化体验（可选）

1. **预装热门翻译** (考虑预装 ms, tr, bn)
2. **后台预下载**（用户选择后静默下载）
3. **离线包**（提供包含所有翻译的离线安装包）

---

## 📞 相关文件

- `TranslUtils.java` - 翻译工具类（需要修改）
- `LanguageManager.kt` - 语言管理（已存在）
- `SPAppConfigs.kt` - 语言设置保存（已存在）
- `FragSettingsTranslationsDownload.kt` - 翻译下载功能（已存在）

---

**✅ 完整的语言与翻译映射关系已整理完成！**

基于 Quran.com API 的真实数据，涵盖 69 种语言的 126 个翻译版本。

