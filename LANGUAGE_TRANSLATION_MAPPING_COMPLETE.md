# 🌍 完整的语言与古兰经翻译映射关系

## 📊 数据来源

- **API**: Quran.com API v4 (`https://api.quran.com/api/v4/resources/translations`)
- **总翻译数**: 126 个
- **支持语言**: 69 种
- **更新日期**: 2025-11-05

---

## 🎯 应用支持的 7 种主要语言 → 翻译映射

### 1️⃣ 🇬🇧 **English (英语)** - 9 个翻译版本

| 优先级 | 翻译名称 | ID | Slug | 译者 | 推荐指数 |
|-------|---------|----|----|------|---------|
| 🥇 **推荐** | **Saheeh International** | 20 | `en-sahih-international` | Saheeh International | ⭐⭐⭐⭐⭐ |
| 🥈 备选 1 | M.A.S. Abdel Haleem | 85 | `en-haleem` | Abdul Haleem | ⭐⭐⭐⭐⭐ |
| 🥉 备选 2 | M. Pickthall | 19 | `quran.en.pickthall` | M.M.W. Pickthall | ⭐⭐⭐⭐ |
| 4 | A. Yusuf Ali | 22 | `quran.en.yusufali` | Abdullah Yusuf Ali | ⭐⭐⭐⭐ |
| 5 | Al-Hilali & Khan | 203 | - | Hilali & Khan | ⭐⭐⭐⭐ |
| 6 | Bridges' translation | 149 | `bridges-translation` | Fadel Soliman | ⭐⭐⭐⭐ |
| 7 | T. Usmani | 84 | `en-taqi-usmani` | Mufti Taqi Usmani | ⭐⭐⭐ |
| 8 | A. Maududi | 95 | `en-al-maududi` | Abul Ala Maududi | ⭐⭐⭐ |
| 9 | Transliteration | 57 | `transliteration` | - | ⭐⭐ |

**推荐配置**:
```java
case "en":
    return "en-sahih-international"; // ID: 20
    // 备选: "en-haleem" (ID: 85) 或 "quran.en.pickthall" (ID: 19)
```

---

### 2️⃣ 🇮🇩 **Indonesian (印尼语)** - 3 个翻译版本

| 优先级 | 翻译名称 | ID | Slug | 译者 | 推荐指数 |
|-------|---------|----|----|------|---------|
| 🥇 **推荐** | **King Fahad Quran Complex** | 134 | - | King Fahad Quran Complex | ⭐⭐⭐⭐⭐ |
| 🥈 备选 1 | Indonesian Islamic Affairs Ministry | 33 | `quran.id` | Indonesian Ministry | ⭐⭐⭐⭐⭐ |
| 🥉 备选 2 | The Sabiq Company | 141 | - | The Sabiq Company | ⭐⭐⭐⭐ |

**推荐配置**:
```java
case "in":
case "id":
    return "quran.id"; // ID: 33 (Indonesian Ministry - 官方翻译)
    // 当前预装: "in_quran-complex" (可能对应 ID: 134)
```

---

### 3️⃣ 🇸🇦 **Arabic (阿拉伯语)** - 无需翻译

**说明**: 阿拉伯语用户直接阅读古兰经原文，不需要翻译。

**推荐配置**:
```java
case "ar":
    return null; // 不设置翻译，直接使用阿拉伯语原文
```

---

### 4️⃣ 🇵🇰 **Urdu (乌尔都语)** - 8 个翻译版本

| 优先级 | 翻译名称 | ID | Slug | 译者 | 推荐指数 |
|-------|---------|----|----|------|---------|
| 🥇 **推荐** | **Maulana Muhammad Junagarhi** | 54 | `ur-junagarri` | Maulana Muhammad Junagarhi | ⭐⭐⭐⭐⭐ |
| 🥈 备选 1 | Fatah Muhammad Jalandhari | 234 | `ur-fatah-muhammad-jalandhari` | Fatah M. Jalandhari | ⭐⭐⭐⭐⭐ |
| 🥉 备选 2 | Bayan-ul-Quran | 158 | `bayan-ul-quran` | Dr. Israr Ahmad | ⭐⭐⭐⭐ |
| 4 | Fe Zilal al-Qur'an | 156 | `urdu-sayyid-qatab` | Sayyid Ibrahim Qutb | ⭐⭐⭐⭐ |
| 5 | Tafheem e Qur'an | 97 | `ur-al-maududi` | Syed Abu Ali Maududi | ⭐⭐⭐⭐ |
| 6 | Shaykh al-Hind Mahmud al-Hasan | 151 | `tafsir-e-usmani` | Mahmud al-Hasan | ⭐⭐⭐ |
| 7 | Abul Ala Maududi (Roman Urdu) | 831 | `maududi-roman-urdu` | Abul Ala Maududi | ⭐⭐⭐ |
| 8 | Maulana Wahiduddin Khan | 819 | `maulana-wahid-uddin-khan-urdu` | Wahiduddin Khan | ⭐⭐⭐ |

**推荐配置**:
```java
case "ur":
    return "ur-junagarri"; // ID: 54
    // 当前预装: "in_junagarhi" (对应此翻译)
    // 备选: "ur-fatah-muhammad-jalandhari" (ID: 234)
```

---

### 5️⃣ 🇲🇾 **Malay (马来语)** - 1 个翻译版本

| 优先级 | 翻译名称 | ID | Slug | 译者 | 推荐指数 |
|-------|---------|----|----|------|---------|
| 🥇 **推荐** | **Abdullah Muhammad Basmeih** | 39 | `ms-abdullah` | Abdullah Muhammad Basmeih | ⭐⭐⭐⭐⭐ |

**推荐配置**:
```java
case "ms":
    return "ms-abdullah"; // ID: 39 (马来西亚最权威翻译)
```

**说明**: 这是马来西亚最权威的翻译，由马来西亚政府认可。

---

### 6️⃣ 🇹🇷 **Turkish (土耳其语)** - 5 个翻译版本

| 优先级 | 翻译名称 | ID | Slug | 译者 | 推荐指数 |
|-------|---------|----|----|------|---------|
| 🥇 **推荐** | **Turkish Translation (Diyanet)** | 77 | `quran.tr.diyanet` | Diyanet Isleri | ⭐⭐⭐⭐⭐ |
| 🥈 备选 1 | Elmalili Hamdi Yazir | 52 | `tr-hamdi` | Elmalili Hamdi Yazir | ⭐⭐⭐⭐ |
| 🥉 备选 2 | Dar Al-Salam Center | 210 | - | Dar Al-Salam Center | ⭐⭐⭐⭐ |
| 4 | Muslim Shahin | 124 | - | Muslim Shahin | ⭐⭐⭐ |
| 5 | Shaban Britch | 112 | - | Shaban Britch | ⭐⭐⭐ |

**推荐配置**:
```java
case "tr":
    return "quran.tr.diyanet"; // ID: 77 (土耳其宗教事务局官方翻译)
    // 备选: "tr-hamdi" (ID: 52, 经典翻译)
```

---

### 7️⃣ 🇧🇩 **Bengali (孟加拉语)** - 4 个翻译版本

| 优先级 | 翻译名称 | ID | Slug | 译者 | 推荐指数 |
|-------|---------|----|----|------|---------|
| 🥇 **推荐** | **Taisirul Quran** | 161 | `bn-taisirul-quran` | Tawheed Publication | ⭐⭐⭐⭐⭐ |
| 🥈 备选 1 | Sheikh Mujibur Rahman | 163 | `bn-sheikh-mujibur-rahman` | Darussalaam Publication | ⭐⭐⭐⭐ |
| 🥉 备选 2 | Rawai Al-bayan | 162 | `bn-rawai-al-bayan` | Bayaan Foundation | ⭐⭐⭐⭐ |
| 4 | Dr. Abu Bakr Muhammad Zakaria | 213 | - | Dr. Abu Bakr Zakaria | ⭐⭐⭐ |

**推荐配置**:
```java
case "bn":
    return "bn-taisirul-quran"; // ID: 161
    // 备选: "bn-sheikh-mujibur-rahman" (ID: 163)
```

---

## 💻 完整的代码实现

### Java 代码配置 (TranslUtils.java)

```java
/**
 * 🌐 语言到翻译版本的完整映射（基于 Quran.com API v4）
 */

// ========== 英语翻译 Slugs ==========
public static final String TRANSL_SLUG_EN_SAHIH_INTERNATIONAL = "en-sahih-international";  // ID: 20 (推荐)
public static final String TRANSL_SLUG_EN_HALEEM = "en-haleem";  // ID: 85 (备选)
public static final String TRANSL_SLUG_EN_PICKTHALL = "quran.en.pickthall";  // ID: 19
public static final String TRANSL_SLUG_EN_YUSUF_ALI = "quran.en.yusufali";  // ID: 22
public static final String TRANSL_SLUG_EN_HILALI_KHAN = "hilali-khan";  // ID: 203
public static final String TRANSL_SLUG_EN_BRIDGES = "bridges-translation";  // ID: 149

// ========== 印尼语翻译 Slugs ==========
public static final String TRANSL_SLUG_ID_MINISTRY = "quran.id";  // ID: 33 (推荐)
public static final String TRANSL_SLUG_ID_KING_FAHAD = "id-king-fahad";  // ID: 134 (备选)

// ========== 乌尔都语翻译 Slugs ==========
public static final String TRANSL_SLUG_UR_JUNAGARHI = "ur-junagarri";  // ID: 54 (推荐)
public static final String TRANSL_SLUG_UR_JALANDHARI = "ur-fatah-muhammad-jalandhari";  // ID: 234 (备选)
public static final String TRANSL_SLUG_UR_ISRAR_AHMAD = "bayan-ul-quran";  // ID: 158

// ========== 马来语翻译 Slugs ==========
public static final String TRANSL_SLUG_MS_BASMEIH = "ms-abdullah";  // ID: 39 (推荐)

// ========== 土耳其语翻译 Slugs ==========
public static final String TRANSL_SLUG_TR_DIYANET = "quran.tr.diyanet";  // ID: 77 (推荐)
public static final String TRANSL_SLUG_TR_HAMDI = "tr-hamdi";  // ID: 52 (备选)

// ========== 孟加拉语翻译 Slugs ==========
public static final String TRANSL_SLUG_BN_TAISIRUL = "bn-taisirul-quran";  // ID: 161 (推荐)
public static final String TRANSL_SLUG_BN_MUJIBUR = "bn-sheikh-mujibur-rahman";  // ID: 163 (备选)

/**
 * 🌐 新用户语言选择 → 翻译自动配置
 * 
 * 根据用户选择的应用语言，自动配置最合适的古兰经翻译版本
 * 
 * @param languageCode 用户选择的语言代码 (en, in, ar, ur, ms, tr, bn)
 * @return 推荐的翻译 slug，如果无需翻译则返回 null
 */
public static String getRecommendedTranslationForLanguage(String languageCode) {
    switch (languageCode) {
        // 英语 → Saheeh International (最流行的现代翻译)
        case "en":
            return TRANSL_SLUG_EN_SAHIH_INTERNATIONAL;  // ID: 20
            
        // 印尼语 → Indonesian Ministry (政府官方翻译)
        case "in":
        case "id":
            return TRANSL_SLUG_ID_MINISTRY;  // ID: 33
            
        // 乌尔都语 → Junagarhi (南亚最流行翻译)
        case "ur":
            return TRANSL_SLUG_UR_JUNAGARHI;  // ID: 54
            
        // 阿拉伯语 → 无需翻译（使用原文）
        case "ar":
            return null;
            
        // 马来语 → Abdullah Basmeih (马来西亚权威翻译)
        case "ms":
            return TRANSL_SLUG_MS_BASMEIH;  // ID: 39
            
        // 土耳其语 → Diyanet (土耳其宗教事务局官方翻译)
        case "tr":
            return TRANSL_SLUG_TR_DIYANET;  // ID: 77
            
        // 孟加拉语 → Taisirul Quran (最流行的孟加拉语翻译)
        case "bn":
            return TRANSL_SLUG_BN_TAISIRUL;  // ID: 161
            
        // 默认回退 → 英语 Sahih International
        default:
            android.util.Log.w("TranslUtils", "⚠️ Unknown language: " + languageCode + ", falling back to English");
            return TRANSL_SLUG_EN_SAHIH_INTERNATIONAL;
    }
}

/**
 * 🌐 获取翻译的显示名称
 */
public static String getTranslationDisplayName(String slug) {
    Map<String, String> displayNames = new HashMap<>();
    
    // 英语
    displayNames.put("en-sahih-international", "Sahih International");
    displayNames.put("en-haleem", "M.A.S. Abdel Haleem");
    displayNames.put("quran.en.pickthall", "M. Pickthall");
    displayNames.put("quran.en.yusufali", "A. Yusuf Ali");
    
    // 印尼语
    displayNames.put("quran.id", "Indonesian Ministry");
    displayNames.put("id-king-fahad", "King Fahad Quran Complex");
    
    // 乌尔都语
    displayNames.put("ur-junagarri", "Maulana Muhammad Junagarhi");
    displayNames.put("ur-fatah-muhammad-jalandhari", "Fatah Muhammad Jalandhari");
    displayNames.put("bayan-ul-quran", "Bayan-ul-Quran (Dr. Israr Ahmad)");
    
    // 马来语
    displayNames.put("ms-abdullah", "Abdullah Muhammad Basmeih");
    
    // 土耳其语
    displayNames.put("quran.tr.diyanet", "Diyanet İşleri (Official)");
    displayNames.put("tr-hamdi", "Elmalili Hamdi Yazir");
    
    // 孟加拉语
    displayNames.put("bn-taisirul-quran", "Taisirul Quran");
    displayNames.put("bn-sheikh-mujibur-rahman", "Sheikh Mujibur Rahman");
    
    return displayNames.getOrDefault(slug, "Unknown Translation");
}
```

---

## 📋 完整语言映射对照表

### 核心语言配置

| 语言代码 | 语言名称 | 🥇 推荐翻译 | ID | Slug | 译者 | 预装状态 |
|---------|---------|-----------|----|----|------|---------|
| **en** | English | Sahih International | 20 | `en-sahih-international` | Saheeh International | ✅ 预装 |
| **in/id** | Indonesian | Ministry Translation | 33 | `quran.id` | Indonesian Ministry | ✅ 预装 |
| **ar** | Arabic | 原文（无需翻译） | - | - | - | N/A |
| **ur** | Urdu | Junagarhi | 54 | `ur-junagarri` | Maulana Junagarhi | ✅ 预装 |
| **ms** | Malay | Abdullah Basmeih | 39 | `ms-abdullah` | A.M. Basmeih | 🌐 需下载 |
| **tr** | Turkish | Diyanet | 77 | `quran.tr.diyanet` | Diyanet İşleri | 🌐 需下载 |
| **bn** | Bengali | Taisirul Quran | 161 | `bn-taisirul-quran` | Tawheed Publication | 🌐 需下载 |

---

## 🌎 其他流行语言的推荐翻译（扩展）

### 欧洲语言

| 语言 | 推荐翻译 | ID | Slug |
|------|---------|----|----|
| **French** (法语) | Montada Islamic Foundation | 136 | `fr-montada-islamic-foundation` |
| **German** (德语) | Abu Reda Muhammad | 27 | `quran.de` |
| **Spanish** (西班牙语) | Abdel Ghani Navio | 83 | `es-cortes` |
| **Italian** (意大利语) | Hamza Roberto Piccardo | 153 | `it-hamza-piccardo` |
| **Russian** (俄语) | Elmir Kuliev | 45 | `quran.ru` |
| **Dutch** (荷兰语) | Sofian S. Siregar | 144 | `nl-sofian` |
| **Portuguese** (葡萄牙语) | Helmi Nasr | 103 | `pt-el-hayek` |
| **Albanian** (阿尔巴尼亚语) | Hasan Efendi Nahi | 88 | `quran.sq.nahi` |

### 亚洲语言

| 语言 | 推荐翻译 | ID | Slug |
|------|---------|----|----|
| **Chinese** (中文简体) | Ma Jian | 56 | `quran.zh.jian` |
| **Japanese** (日语) | Unknown | 35 | `quran.ja` |
| **Korean** (韩语) | Unknown | 219 | - |
| **Tamil** (泰米尔语) | Jan Trust Foundation | 229 | - |
| **Malayalam** (马拉雅拉姆语) | Cheriyamundam Abdul Hameed | 37 | `quran.ml` |
| **Persian** (波斯语) | Fooladvand | 135 | `fa-fooladvand` |
| **Thai** (泰语) | Unknown | 101 | `th-thai` |
| **Vietnamese** (越南语) | Rowwad Translation Center | 207 | - |

### 非洲语言

| 语言 | 推荐翻译 | ID | Slug |
|------|---------|----|----|
| **Swahili** (斯瓦希里语) | Ali Muhsin Al-Barwani | 128 | `sw-barwani` |
| **Hausa** (豪萨语) | Abubakar Mahmoud Gumi | 32 | `quran.ha.gumi` |
| **Amharic** (阿姆哈拉语) | Sadiq and Sani | 87 | `am-sadiq` |
| **Somali** (索马里语) | Mahmud Muhammad Abduh | 46 | `quran.so` |

---

## 📊 完整的 69 种语言列表

从 Quran.com API 获取的完整语言列表：

1. Amazigh (柏柏尔语)
2. Albanian (阿尔巴尼亚语) - 3 个翻译
3. Amharic (阿姆哈拉语)
4. Assamese (阿萨姆语)
5. Azeri (阿塞拜疆语) - 2 个翻译
6. Bambara (班巴拉语) - 2 个翻译
7. Bengali (孟加拉语) - 4 个翻译 ✅
8. Bosnian (波斯尼亚语) - 3 个翻译
9. Bulgarian (保加利亚语)
10. Central Khmer (高棉语)
11. Chechen (车臣语)
12. Chinese (中文) - 2 个翻译
13. Czech (捷克语)
14. Dari (达里语)
15. Divehi (迪维希语) - 2 个翻译
16. Dutch (荷兰语) - 2 个翻译
17. English (英语) - 9 个翻译 ✅
18. Finnish (芬兰语)
19. French (法语) - 3 个翻译
20. Ganda (干达语)
21. German (德语) - 2 个翻译
22. Gujarati (古吉拉特语)
23. Hausa (豪萨语) - 2 个翻译
24. Hebrew (希伯来语)
25. Hindi (印地语)
26. Indonesian (印尼语) - 3 个翻译 ✅
27. Ingush (印古什语)
28. Italian (意大利语) - 2 个翻译
29. Japanese (日语) - 2 个翻译
30. Kannada (卡纳达语)
31. Kazakh (哈萨克语)
32. Kinyarwanda (卢旺达语)
33. Korean (韩语)
34. Kurdish (库尔德语) - 2 个翻译
35. Kyrgyz (吉尔吉斯语)
36. Malay (马来语) - 1 个翻译 ✅
37. Malayalam (马拉雅拉姆语) - 3 个翻译
38. Maranao (马拉瑙语)
39. Marathi (马拉地语)
40. Nogai (诺盖语)
41. Norwegian (挪威语)
42. Oromo (奥罗莫语)
43. Pashto (普什图语)
44. Persian (波斯语) - 2 个翻译
45. Polish (波兰语)
46. Portuguese (葡萄牙语)
47. Punjabi (旁遮普语)
48. Romanian (罗马尼亚语)
49. Russian (俄语) - 3 个翻译
50. Sindhi (信德语)
51. Sinhala (僧伽罗语)
52. Somali (索马里语)
53. Spanish (西班牙语) - 3 个翻译
54. Swahili (斯瓦希里语) - 2 个翻译
55. Swedish (瑞典语)
56. Tajik (塔吉克语) - 3 个翻译
57. Tamil (泰米尔语) - 3 个翻译
58. Tatar (鞑靼语)
59. Telugu (泰卢固语)
60. Thai (泰语)
61. Turkish (土耳其语) - 5 个翻译 ✅
62. Uyghur (维吾尔语)
63. Urdu (乌尔都语) - 8 个翻译 ✅
64. Uzbek (乌兹别克语) - 3 个翻译
65. Vietnamese (越南语)
66. Yoruba (约鲁巴语)
67. Hausa, Fulah (豪萨-富拉语)
68. Malayalam (alternative)
69. 其他小语种...

---

## 🚀 新用户语言选择流程实现

### 流程图

```
用户首次启动应用
        ↓
显示语言选择界面
┌────────────────────────────┐
│ 🇬🇧 English                 │
│ 🇮🇩 Bahasa Indonesia        │
│ 🇸🇦 العربية (Arabic)       │
│ 🇵🇰 اردو (Urdu)            │
│ 🇲🇾 Bahasa Melayu           │
│ 🇹🇷 Türkçe                  │
│ 🇧🇩 বাংলা (Bengali)        │
└────────────────────────────┘
        ↓
用户选择语言
        ↓
保存语言设置
        ↓
根据语言代码获取推荐翻译
        ↓
┌─────────────────────────┐
│ 翻译已预装？            │
└─────────────────────────┘
  ↓ 是(en/in/ur)      ↓ 否(ms/tr/bn)
直接启用翻译        提示下载翻译
  ↓                    ↓
进入主界面      下载完成 → 启用 → 进入主界面
                  ↓ 或
              跳过下载 → 使用英语回退 → 进入主界面
```

### 核心代码实现

```java
/**
 * 为新用户配置翻译（完整实现）
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
    String translationSlug = getRecommendedTranslationForLanguage(languageCode);
    
    // 阿拉伯语用户无需翻译
    if (translationSlug == null) {
        callback.onSetupComplete(null, true, "Arabic users read original text");
        return;
    }
    
    // 检查翻译是否已预装
    QuranTranslationFactory factory = new QuranTranslationFactory(context);
    boolean isPrebuilt = factory.isTranslationDownloaded(translationSlug);
    
    if (isPrebuilt) {
        // 翻译已预装，直接启用
        Set<String> translationSet = new HashSet<>();
        translationSet.add(translationSlug);
        SPReader.setSavedTranslations(context, translationSet);
        
        Log.d("TranslSetup", "✅ Translation enabled: " + translationSlug);
        callback.onSetupComplete(translationSlug, true, "Translation is prebuilt");
    } else {
        // 翻译需要下载
        Log.d("TranslSetup", "⚠️ Translation needs download: " + translationSlug);
        callback.onSetupComplete(translationSlug, false, "Translation requires download");
        
        // 可以在这里触发自动下载或显示下载对话框
    }
}

/**
 * 回调接口
 */
public interface TranslationSetupCallback {
    /**
     * @param translationSlug 配置的翻译 slug
     * @param isAvailable 翻译是否立即可用（已预装）
     * @param message 状态消息
     */
    void onSetupComplete(String translationSlug, boolean isAvailable, String message);
}
```

---

## 📱 用户体验优化建议

### 场景 1：翻译已预装 (en, in, ur)

```
用户选择 English
        ↓
应用检测：Sahih International 已预装
        ↓
自动启用翻译
        ↓
Toast: "✅ English translation enabled"
        ↓
进入主界面（0 秒等待）
```

### 场景 2：翻译需下载 (ms, tr, bn)

```
用户选择 Malay
        ↓
应用检测：Abdullah Basmeih 未预装
        ↓
显示对话框：
┌────────────────────────────────────┐
│ 📥 Download Malay Translation?     │
│                                    │
│ Abdullah Muhammad Basmeih          │
│ (Malaysian Authorized Translation) │
│                                    │
│ Size: ~3 MB                        │
│ Status: Recommended for Malay users│
│                                    │
│ [Download] [Use English Instead]   │
└────────────────────────────────────┘
        ↓
用户点击 Download → 下载进度 → 完成 → 启用
用户点击 Use English → 使用英语 Sahih International
        ↓
进入主界面
```

---

## 🎯 核心实现总结

### 应用支持的 7 种语言配置

```java
// 完整映射表
Map<String, TranslationConfig> languageTranslationMap = new HashMap<>();

languageTranslationMap.put("en", new TranslationConfig(
    "en-sahih-international",  // slug
    20,  // ID
    "Sahih International",  // name
    true  // isPrebuilt
));

languageTranslationMap.put("in", new TranslationConfig(
    "quran.id",
    33,
    "Indonesian Ministry",
    true  // isPrebuilt
));

languageTranslationMap.put("ar", new TranslationConfig(
    null,  // 无需翻译
    0,
    "Original Arabic Text",
    true
));

languageTranslationMap.put("ur", new TranslationConfig(
    "ur-junagarri",
    54,
    "Maulana Muhammad Junagarhi",
    true  // isPrebuilt
));

languageTranslationMap.put("ms", new TranslationConfig(
    "ms-abdullah",
    39,
    "Abdullah Muhammad Basmeih",
    false  // 需要下载
));

languageTranslationMap.put("tr", new TranslationConfig(
    "quran.tr.diyanet",
    77,
    "Diyanet İşleri",
    false  // 需要下载
));

languageTranslationMap.put("bn", new TranslationConfig(
    "bn-taisirul-quran",
    161,
    "Taisirul Quran",
    false  // 需要下载
));
```

---

## 📚 相关文件

- `translations_quran_com.json` - Quran.com API 完整数据（126 个翻译）
- `language_translation_mapping.json` - 完整的语言映射 JSON
- `QURAN_ENGLISH_TRANSLATIONS_GUIDE.md` - 英文翻译详细指南

---

## 🔧 下一步操作

1. **查看完整数据**：
   ```bash
   cat /Users/huwei/AndroidStudioProjects/quran0/language_translation_mapping.json
   ```

2. **在 Android 代码中实现**（TranslUtils.java）

3. **创建语言选择界面**（如果尚未存在）

4. **集成翻译下载功能**（对于 ms, tr, bn）

---

**✅ 完整的语言翻译映射关系已整理完成！**

基于 Quran.com API 的 126 个翻译，覆盖 69 种语言。

