# 🌍 语言与翻译快速参考卡

## 📋 应用支持的 7 种语言 → 古兰经翻译映射

| # | 语言 | 翻译 | ID | Slug | 状态 | 大小 |
|---|------|-----|----|----|------|-----|
| 1 | 🇬🇧 English | Sahih International | 20 | `en-sahih-international` | ✅ 预装 | ~3MB |
| 2 | 🇮🇩 Indonesian | Ministry Translation | 33 | `quran.id` | ✅ 预装 | ~3MB |
| 3 | 🇵🇰 Urdu | Junagarhi | 54 | `ur-junagarri` | ✅ 预装 | ~4MB |
| 4 | 🇸🇦 Arabic | 原文（无翻译） | - | `null` | N/A | - |
| 5 | 🇲🇾 Malay | Abdullah Basmeih | 39 | `ms-abdullah` | 🌐 需下载 | ~3MB |
| 6 | 🇹🇷 Turkish | Diyanet İşleri | 77 | `quran.tr.diyanet` | 🌐 需下载 | ~3MB |
| 7 | 🇧🇩 Bengali | Taisirul Quran | 161 | `bn-taisirul-quran` | 🌐 需下载 | ~4MB |

---

## 💻 代码配置（一键复制）

### TranslUtils.java - 新增方法

\`\`\`java
/**
 * 🌐 根据应用语言获取推荐的翻译版本
 * @param languageCode 语言代码 (en, in, ur, ar, ms, tr, bn)
 * @return 翻译 slug，阿拉伯语返回 null
 */
public static String getRecommendedTranslationForLanguage(String languageCode) {
    switch (languageCode) {
        case "en": return "en-sahih-international";     // ID: 20
        case "in":
        case "id": return "quran.id";                    // ID: 33
        case "ur": return "ur-junagarri";                // ID: 54
        case "ar": return null;                          // 原文
        case "ms": return "ms-abdullah";                 // ID: 39
        case "tr": return "quran.tr.diyanet";            // ID: 77
        case "bn": return "bn-taisirul-quran";           // ID: 161
        default:   return "en-sahih-international";      // 默认英语
    }
}
\`\`\`

---

## 🚀 快速开始

### 1. 在 TranslUtils.java 添加方法
复制上面的 `getRecommendedTranslationForLanguage()` 方法

### 2. 在新用户流程中调用
\`\`\`java
String langCode = LanguageManager.getCurrentLanguageCode(context);
String translSlug = TranslUtils.getRecommendedTranslationForLanguage(langCode);

if (translSlug != null) {
    Set<String> translSet = new HashSet<>();
    translSet.add(translSlug);
    SPReader.setSavedTranslations(context, translSet);
}
\`\`\`

---

## 📊 数据来源

- **API**: Quran.com API v4
- **总翻译数**: 126 个
- **支持语言**: 69 种
- **查询日期**: 2025-11-05

---

## 📚 详细文档

- `NEW_USER_LANGUAGE_SELECTION_GUIDE.md` - 完整实施指南
- `ALL_TRANSLATIONS_MAPPING.md` - 所有 69 种语言列表
- `translations_quran_com.json` - API 原始数据
