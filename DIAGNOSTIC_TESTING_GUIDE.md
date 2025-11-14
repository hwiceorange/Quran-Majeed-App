# 🔍 古兰经版本过滤问题诊断测试指南

## 📅 创建日期
2025-11-13

---

## 🎯 目标

通过详细的日志输出，逐步诊断为什么选择任何语言后都显示所有版本的古兰经翻译。

---

## 📋 测试步骤

### 步骤 1: 准备测试环境

1. **清除应用数据**（如果是旧用户）
   ```bash
   adb shell pm clear com.quran.quranaudio.online
   ```

2. **打开 Logcat**
   - 在 Android Studio 中打开 Logcat
   - 过滤器设置为: `FragOnboardQuranVersion`

3. **启动应用**
   - 首次启动会进入引导页

---

### 步骤 2: 语言选择测试

#### 测试场景 A: 选择土耳其语

**操作步骤**:
1. 在语言选择页面选择 **"Türkçe"**（土耳其语）
2. 点击 "Continue" 按钮
3. 进入古兰经版本选择页面

**期望日志输出**:

```
═══════════════════════════════════════════════
🕌 STEP 1: 获取用户选择的语言代码
   selectedLanguageCode = 'tr'
═══════════════════════════════════════════════
```

**✅ 检查点 1.1**: 
- `selectedLanguageCode` 是否为 `'tr'`？
- ❌ 如果不是，说明语言选择页面没有正确保存
  - 检查 `FragOnboardLanguage.kt` 的 `selectLanguage()` 方法
  - 检查 `SPAppConfigs.setLocale()` 是否被调用

---

**期望日志输出**:

```
═══════════════════════════════════════════════
🔄 STEP 2: 从API获取翻译数据
   语言代码: app='tr' → API='tr'
   API返回的语言键: en, id, ar, ur, ms, tr, bn
═══════════════════════════════════════════════
✅ 找到语言键 'tr'，包含 N 个翻译版本
```

**✅ 检查点 2.1**: 
- API 返回的语言键中是否包含 `tr`？
- ❌ 如果包含但显示"语言键不存在"，说明代码有问题
  - 检查 `normalizedLangCode` 的值
  - 检查大小写是否匹配

**✅ 检查点 2.2**: 
- 是否成功找到语言键 `tr`？
- ❌ 如果找不到，检查以下情况：
  1. API 是否失败（会看到 "Primary API failed"）
  2. 是否回退到备用 API
  3. 备用 API 的语言参数是否正确传递

---

**期望日志输出**:

```
═══════════════════════════════════════════════
📋 STEP 3: 前端显示验证
   当前选择语言: tr
   availableVersions 总数: 5
   所有版本的语言代码:
     - Turkish Translation 1: languageCode='tr'
     - Turkish Translation 2: languageCode='tr'
     - Turkish Translation 3: languageCode='tr'
═══════════════════════════════════════════════

✅ STEP 3 结果: 过滤后剩余 5 个版本

🎨 开始创建UI卡片:
  1. Turkish Translation 1 (tr)
  2. Turkish Translation 2 (tr)
  3. Turkish Translation 3 (tr)
═══════════════════════════════════════════════
```

**✅ 检查点 3.1**: 
- `availableVersions` 中是否有其他语言的版本？
- ❌ 如果有，例如看到:
  ```
  - English Translation: languageCode='en'
  - Turkish Translation: languageCode='tr'
  ```
  说明前面的过滤没有生效，需要检查：
  1. `parseTranslationsJson()` 是否正确过滤
  2. API 返回的数据是否被正确解析

**✅ 检查点 3.2**: 
- 过滤逻辑是否生效？
- ❌ 如果看到:
  ```
  ⚠️ 过滤掉: English Translation (其语言='en', 期望='tr')
  ```
  说明过滤逻辑在工作，但前面的数据获取有问题

---

### 步骤 3: 关键问题诊断

#### 情况 A: 所有版本的 languageCode 都不是 'tr'

**日志特征**:
```
所有版本的语言代码:
  - English Translation: languageCode='en'
  - Indonesian Translation: languageCode='id'
  - Arabic Translation: languageCode='ar'
  ...
⚠️ 过滤掉: English Translation
⚠️ 过滤掉: Indonesian Translation
❌ 没有找到语言 'tr' 的翻译版本!
```

**问题原因**: 
- 主 API 返回了所有语言的数据，而不是只返回土耳其语
- `parseTranslationsJson()` 没有正确过滤

**解决方法**:
1. 检查 API 是否失败，回退到备用 API
2. 检查备用 API 是否传递了 `language` 参数
3. 检查 `parseQuranFoundationTranslations()` 的过滤逻辑

---

#### 情况 B: 没有任何日志输出

**可能原因**:
1. 应用崩溃
2. 代码没有被执行
3. 日志级别过滤器设置错误

**解决方法**:
1. 检查 Logcat 过滤器
2. 设置为 `No Filters` 查看所有日志
3. 搜索 `FragOnboardQuranVersion`

---

#### 情况 C: API 持续失败

**日志特征**:
```
❌ Primary API failed: java.net.UnknownHostException
📡 Trying fallback API (Quran Foundation)...
❌ Fallback API also failed: ...
📦 Using prebuilt versions as final fallback
```

**问题原因**:
- 网络连接问题
- API 服务器宕机
- 防火墙阻止

**解决方法**:
1. 检查网络连接
2. 使用浏览器测试 API:
   - 主 API: `https://apis.dochubai.com/quran/apis/translations/available_translations_info.json`
   - 备用 API: `https://api.quran.com/api/v4/resources/translations?language=turkish`

---

## 📊 日志输出示例（完整流程）

### ✅ 成功场景（土耳其语）

```
═══════════════════════════════════════════════
🕌 STEP 1: 获取用户选择的语言代码
   selectedLanguageCode = 'tr'
═══════════════════════════════════════════════

🔄 Loading translation versions for: tr
📡 Trying primary API...
🌐 API_REQUEST: GET https://apis.dochubai.com/quran/apis/translations/available_translations_info.json
✅ API_RESPONSE: 200 https://apis.dochubai.com/...

═══════════════════════════════════════════════
🔄 STEP 2: 从API获取翻译数据
   语言代码: app='tr' → API='tr'
   API返回的语言键: en, id, ar, ur, ms, tr, bn
═══════════════════════════════════════════════
✅ 找到语言键 'tr'，包含 5 个翻译版本

  ✅ Parsed: Turkish - Diyanet Vakfi (tr_diyanet)
  ✅ Parsed: Turkish - Suat Yildirim (tr_yildirim)
  ✅ Parsed: Turkish - Elmalili Hamdi Yazir (tr_elmalili)
  ✅ Parsed: Turkish - Suleyman Ates (tr_ates)
  ✅ Parsed: Turkish - Diyanet Isleri (tr_diyanet_isleri)

📊 Total parsed: 5 translations for language 'tr' (API key: 'tr')

✅ Loaded 5 translations from API for language: tr

  📖 Translation: Turkish - Diyanet Vakfi (lang: tr)
  📖 Translation: Turkish - Suat Yildirim (lang: tr)
  📖 Translation: Turkish - Elmalili Hamdi Yazir (lang: tr)
  📖 Translation: Turkish - Suleyman Ates (lang: tr)
  📖 Translation: Turkish - Diyanet Isleri (lang: tr)

═══════════════════════════════════════════════
📋 STEP 3: 前端显示验证
   当前选择语言: tr
   availableVersions 总数: 5
   所有版本的语言代码:
     - Turkish - Diyanet Vakfi: languageCode='tr'
     - Turkish - Suat Yildirim: languageCode='tr'
     - Turkish - Elmalili Hamdi Yazir: languageCode='tr'
     - Turkish - Suleyman Ates: languageCode='tr'
     - Turkish - Diyanet Isleri: languageCode='tr'
═══════════════════════════════════════════════

✅ STEP 3 结果: 过滤后剩余 5 个版本

🎨 开始创建UI卡片:
  1. Turkish - Diyanet Vakfi (tr)
  2. Turkish - Elmalili Hamdi Yazir (tr)
  3. Turkish - Suat Yildirim (tr)
  4. Turkish - Suleyman Ates (tr)
  5. Turkish - Diyanet Isleri (tr)
═══════════════════════════════════════════════
```

---

### ❌ 失败场景（显示所有语言）

```
═══════════════════════════════════════════════
🕌 STEP 1: 获取用户选择的语言代码
   selectedLanguageCode = 'tr'
═══════════════════════════════════════════════

🔄 Loading translation versions for: tr
📡 Trying primary API...
❌ Primary API failed: java.net.UnknownHostException: apis.dochubai.com

📡 Trying fallback API (Quran Foundation)...
📍 Requesting translations for language: turkish (code: tr)
🌐 API_REQUEST: GET https://api.quran.com/api/v4/resources/translations
                    ⚠️ 注意：这里没有 ?language=turkish 参数！

🔍 Filtering translations for target language: 'turkish' (from code: 'tr')
📊 Total translations in API response: 200
  ⏭️ Skipped: language_name='english' (expected: 'turkish')
  ⏭️ Skipped: language_name='arabic' (expected: 'turkish')
  ⏭️ Skipped: language_name='indonesian' (expected: 'turkish')
  ...
  ✅ Found: Turkish - Diyanet Vakfi (turkish)
  ...

📊 Matched 5 translations for 'turkish' from Quran Foundation API

✅ Loaded 5 translations from API for language: tr

═══════════════════════════════════════════════
📋 STEP 3: 前端显示验证
   当前选择语言: tr
   availableVersions 总数: 5
   ...
═══════════════════════════════════════════════
```

**❓ 问题分析**:
- 如果在这种情况下仍然显示所有语言，说明：
  1. 备用 API 的过滤逻辑有 bug
  2. 或者有其他地方在添加版本到 `availableVersions`

---

## 🛠️ 调试技巧

### 技巧 1: 使用 adb logcat

```bash
# 清除旧日志
adb logcat -c

# 实时查看日志
adb logcat | grep "FragOnboardQuranVersion"

# 保存日志到文件
adb logcat > logcat.txt
```

### 技巧 2: 检查具体的行数

在日志中搜索关键标记：
- `STEP 1`: 语言代码是否正确
- `STEP 2`: API 数据是否正确
- `STEP 3`: 过滤逻辑是否生效
- `⚠️ 过滤掉`: 哪些版本被过滤掉了
- `❌`: 错误信息

### 技巧 3: 比对日志

将实际日志与本文档中的示例日志对比，找出差异点。

---

## 📞 问题报告模板

如果问题仍然存在，请提供以下信息：

```
### 测试场景
- 选择的语言: [例如: 土耳其语]
- 显示的版本数量: [例如: 20 个]
- 是否显示了其他语言: [是/否]

### 日志输出
[请粘贴完整的日志输出，从 STEP 1 到 STEP 3]

### 观察到的问题
[详细描述看到的现象]
```

---

## ✅ 下一步

1. **编译应用**
2. **清除应用数据**
3. **按照本指南进行测试**
4. **收集日志**
5. **分析日志找出问题根源**

---

**创建日期**: 2025-11-13  
**适用版本**: v1.5.5+

