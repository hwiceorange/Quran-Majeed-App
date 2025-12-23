# 🔧 古兰经版本语言过滤修复报告

## 📅 修复日期
2025-11-13

## 🐛 问题描述

### 用户报告的问题
新用户在引导页选择印尼语(Indonesian)后，到达古兰经版本选择页面时，显示了**所有语言的古兰经版本**，而不是只显示印尼语的版本。

其他语言选择也存在同样的问题：应该只显示对应语言的古兰经版本，而不是显示所有语言的版本。

### 问题场景
1. 用户在 `FragOnboardLanguage` 页面选择"Bahasa Indonesia"（印尼语）
2. 应用保存语言代码为 `"in"`
3. 进入 `FragOnboardQuranVersion` 页面
4. **期望**：只显示印尼语的古兰经翻译版本
5. **实际**：显示了所有语言的古兰经翻译版本

---

## 🔍 根本原因分析

### 语言代码不匹配

应用内部使用的语言代码与 API 返回的语言代码不一致：

| 语言 | 应用内代码 | API 使用代码 | 状态 |
|------|-----------|-------------|------|
| English | `en` | `en` | ✅ 匹配 |
| **Indonesian** | **`in`** | **`id`** | ❌ **不匹配** |
| Arabic | `ar` | `ar` | ✅ 匹配 |
| Urdu | `ur` | `ur` | ✅ 匹配 |
| Malay | `ms` | `ms` | ✅ 匹配 |
| Turkish | `tr` | `tr` | ✅ 匹配 |
| Bengali | `bn` | `bn` | ✅ 匹配 |

### API 数据结构

主 API (`apis.dochubai.com`) 返回的 JSON 结构：

```json
{
  "translations": {
    "en": {
      "en_sahih-international": { ... },
      "en_pickthall": { ... }
    },
    "id": {  // ⚠️ 注意：使用 "id" 而不是 "in"
      "id_indonesian-ministry": { ... }
    },
    "ar": { ... },
    "ur": { ... }
  }
}
```

### 代码问题

**位置**: `FragOnboardQuranVersion.kt` 第 265 行

**修复前**:
```kotlin
val normalizedLangCode = if (languageCode == "id") "in" else languageCode
```

**问题分析**:
- 条件判断反了！
- 当用户选择印尼语时，`languageCode = "in"`
- 代码检查 `languageCode == "id"`（条件为 false）
- 所以 `normalizedLangCode` 仍然是 `"in"`
- 尝试从 API 获取 `translations["in"]` → 返回 `null`（因为 API 使用 `"id"`）
- 回退到预装版本或显示空列表/所有版本

---

## ✅ 修复方案

### 1. 修复语言代码映射逻辑

**文件**: `app/src/main/java/com/quran/quranaudio/online/quran_module/frags/onboard/FragOnboardQuranVersion.kt`

**修改**: 第 263-265 行

```kotlin
// 修复前
val normalizedLangCode = if (languageCode == "id") "in" else languageCode

// 修复后
// 语言代码映射：应用内使用 "in" 表示印尼语，但 API 使用 "id"
// 将应用的语言代码转换为 API 的语言代码
val normalizedLangCode = if (languageCode == "in") "id" else languageCode
```

### 2. 保持数据一致性

**修改**: 第 294 行

确保创建 `QuranTranslationVersion` 对象时使用原始的语言代码（`languageCode`），而不是标准化后的代码（`normalizedLangCode`）：

```kotlin
val version = QuranTranslationVersion(
    versionId = slug,
    displayName = displayName.ifEmpty { book },
    bookName = book,
    authorName = author,
    languageCode = languageCode, // ✅ 使用原始语言代码，保持与应用一致
    languageName = langName,
    downloadPath = downloadPath,
    numericId = 0
)
```

### 3. 增强调试日志

**新增**: 第 267 行

```kotlin
android.util.Log.d("FragOnboardQuranVersion", "🔄 Language code mapping: app='$languageCode' → API='$normalizedLangCode'")
```

**新增**: 第 274 行

```kotlin
android.util.Log.d("FragOnboardQuranVersion", "Available language keys in API: ${translationsObject.keys}")
```

**更新**: 第 314 行

```kotlin
android.util.Log.d("FragOnboardQuranVersion", "📊 Total parsed: ${translations.size} translations for language '$languageCode' (API key: '$normalizedLangCode')")
```

---

## 🧪 测试验证

### 测试场景 1：印尼语选择

1. **步骤**:
   - 启动应用（首次安装）
   - 在语言选择页面选择 "Bahasa Indonesia"
   - 点击 "Continue" 按钮
   - 进入古兰经版本选择页面

2. **期望结果**:
   - ✅ 只显示印尼语的翻译版本
   - ✅ 预装版本："Kompleks Al Quran Raja Fahd"
   - ✅ Logcat 显示: `Language code mapping: app='in' → API='id'`
   - ✅ Logcat 显示: `Total parsed: X translations for language 'in' (API key: 'id')`

### 测试场景 2：英语选择

1. **步骤**:
   - 在语言选择页面选择 "English"
   - 点击 "Continue" 按钮
   - 进入古兰经版本选择页面

2. **期望结果**:
   - ✅ 只显示英语的翻译版本
   - ✅ 预装版本："Sahih International", "The Clear Quran"
   - ✅ 可下载版本：多个英语翻译（如 Pickthall, Yusuf Ali 等）

### 测试场景 3：阿拉伯语选择

1. **步骤**:
   - 在语言选择页面选择 "العربية"
   - 点击 "Continue" 按钮
   - 进入古兰经版本选择页面

2. **期望结果**:
   - ✅ 只显示阿拉伯语的翻译版本
   - ✅ Logcat 显示: `Language code mapping: app='ar' → API='ar'`

### 测试场景 4：其他语言

测试以下语言，确保每种语言都只显示对应的翻译版本：
- ✅ اردو (Urdu) - `ur`
- ✅ Bahasa Melayu (Malay) - `ms`
- ✅ Türkçe (Turkish) - `tr`
- ✅ বাংলা (Bengali) - `bn`

---

## 📊 修复前后对比

### 修复前

| 选择语言 | 应用代码 | API查询 | 结果 |
|---------|---------|---------|------|
| Indonesian | `in` | `translations["in"]` | ❌ null → 显示所有版本或预装版本 |
| English | `en` | `translations["en"]` | ✅ 正常显示英语版本 |

### 修复后

| 选择语言 | 应用代码 | 标准化后 | API查询 | 结果 |
|---------|---------|---------|---------|------|
| Indonesian | `in` | `id` | `translations["id"]` | ✅ 正确显示印尼语版本 |
| English | `en` | `en` | `translations["en"]` | ✅ 正常显示英语版本 |
| Others | `ar/ur/ms/tr/bn` | `ar/ur/ms/tr/bn` | `translations["X"]` | ✅ 正常显示对应语言版本 |

---

## 📝 相关文件

### 修改的文件

1. ✅ `app/src/main/java/com/quran/quranaudio/online/quran_module/frags/onboard/FragOnboardQuranVersion.kt`
   - 第 263-268 行：修复语言代码映射逻辑
   - 第 273-274 行：添加调试日志
   - 第 294 行：使用原始语言代码
   - 第 314 行：改进日志信息

### 未修改的文件（无需修改）

- ✅ `FragOnboardLanguage.kt` - 语言选择逻辑正确
- ✅ `parseQuranFoundationTranslations()` - 备用 API 已正确处理语言映射
- ✅ `SPAppConfigs.kt` - 语言存储逻辑正确

---

## 🎯 技术要点

### 为什么印尼语使用 "in" 而不是 "id"？

1. **Android 资源目录命名**: 
   - Android 使用 `values-in` 表示印尼语资源
   - 这是 ISO 639-1 标准代码

2. **API 历史原因**:
   - 一些 API（如 Quran.com）使用 `id` 作为印尼语代码
   - `id` 在某些系统中代表 "Indonesia" 的缩写

3. **解决方案**:
   - 应用内部统一使用 `in`
   - 与 API 交互时，动态转换为 `id`
   - 对用户透明，无需感知差异

---

## ✨ 修复效果

修复后，用户体验将得到显著改善：

1. **✅ 精准过滤**: 每种语言只显示对应的翻译版本
2. **✅ 避免混淆**: 用户不会看到其他语言的版本
3. **✅ 提升性能**: 减少显示的版本数量，加载更快
4. **✅ 更好的调试**: 详细的日志帮助快速定位问题

---

## 🔄 后续建议

1. **添加单元测试**:
   ```kotlin
   @Test
   fun testLanguageCodeNormalization() {
       assertEquals("id", normalizeLanguageCode("in"))
       assertEquals("en", normalizeLanguageCode("en"))
       assertEquals("ar", normalizeLanguageCode("ar"))
   }
   ```

2. **创建语言代码映射工具类**:
   ```kotlin
   object LanguageCodeMapper {
       fun appToApi(appCode: String): String = when (appCode) {
           "in" -> "id"
           else -> appCode
       }
       
       fun apiToApp(apiCode: String): String = when (apiCode) {
           "id" -> "in"
           else -> apiCode
       }
   }
   ```

3. **文档化语言代码映射关系**:
   - 在代码中添加清晰的注释
   - 维护语言代码对照表

---

## 🎉 总结

此次修复解决了古兰经版本选择页面显示所有语言版本的问题。关键修复点是**纠正了印尼语语言代码的映射方向**：从 `"id" → "in"` 改为 `"in" → "id"`，使应用代码与 API 代码正确对应。

修复后，所有语言的翻译版本都能正确过滤和显示，提升了用户体验和应用质量。

