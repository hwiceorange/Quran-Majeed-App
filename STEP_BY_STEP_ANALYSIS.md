# 🔍 古兰经版本过滤问题逐步分析报告

## 📅 分析日期
2025-11-13

---

## 步骤 1: 数据来源分析 ✅

### 1.1 数据获取流程

```
用户选择语言 (例如: 土耳其语 "tr")
    ↓
FragOnboardQuranVersion.loadTranslationVersions()
    ↓
┌─────────────────────────────────────────────┐
│ 方案1: 主 API (优先)                          │
│ URL: SHAHEEN_DEVELOPERS_URL/apis/...        │
│ 端点: /translations/available_translations_info.json │
└─────────────────────────────────────────────┘
    ↓ (如果失败)
┌─────────────────────────────────────────────┐
│ 方案2: 备用 API                               │
│ URL: https://api.quran.com/                 │
│ 端点: /api/v4/resources/translations       │
└─────────────────────────────────────────────┘
    ↓ (如果失败)
┌─────────────────────────────────────────────┐
│ 方案3: 预装版本 (硬编码)                      │
│ getPrebuiltVersions()                       │
└─────────────────────────────────────────────┘
```

### 1.2 主 API 数据结构

**URL**: `https://apis.dochubai.com/quran/apis/translations/available_translations_info.json`

**返回格式**:
```json
{
  "translations": {
    "en": {
      "en_sahih-international": { ... },
      "en_pickthall": { ... }
    },
    "id": {
      "id_indonesian-ministry": { ... }
    },
    "tr": {
      "tr_diyanet": { ... }
    },
    ...
  }
}
```

### 1.3 备用 API 数据结构

**URL**: `https://api.quran.com/api/v4/resources/translations?language=turkish`

**返回格式**:
```json
{
  "translations": [
    {
      "id": 77,
      "name": "Turkish",
      "author_name": "...",
      "slug": "tr.diyanet",
      "language_name": "turkish"
    }
  ]
}
```

### 1.4 预装版本

```kotlin
when (selectedLanguageCode) {
    "en" -> { Sahih International, The Clear Quran }
    "id" -> { Kompleks Al Quran Raja Fahd }
    "ur" -> { مولانا محمد جوناگڑهی }
    else -> { 无预装版本 }
}
```

---

## 问题分析

### 当前代码中可能的问题点：

#### 问题 A: 主 API 解析逻辑
在 `parseTranslationsJson()` 中：

```kotlin
// 获取目标语言的翻译对象
val langTranslations = translationsObject[normalizedLangCode]?.jsonObject

if (langTranslations == null) {
    // 如果找不到对应语言，返回空列表
    return emptyList()  // ⚠️ 这里应该记录详细日志
}
```

**潜在问题**: 
- 如果 API 返回的语言键与预期不匹配，会返回空列表
- 然后回退到预装版本或备用 API
- 但没有清楚地记录为什么失败

#### 问题 B: languageCode 值
需要确认 `selectedLanguageCode` 的实际值：
- 是 "tr" 还是其他值？
- 是否正确从上一个页面传递？

---

## 下一步行动

需要添加更详细的日志来追踪：
1. `selectedLanguageCode` 的实际值
2. API 返回的完整 JSON 键列表
3. 为什么 `langTranslations` 会是 null

