# 🐛 孟加拉语翻译不显示 - 调试指南

## 问题描述

用户测试发现：在古兰经经文详情页，只有阿语原文，孟加拉语翻译的古兰经经文内容不显示。

## 已添加的调试日志

### 1. 下载和保存时的日志（FragOnboardQuranVersion.kt）

```kotlin
📊 QuranTranslBookInfo created:
   slug: 'bn_161_taisirul-quran'
   bookName: 'Taisirul Quran'
   displayName: 'তাইসীরুল কুরআন'
   langCode: 'bn'
   langName: 'বাংলা'

✅ Translation stored in database with slug: 'bn_161_taisirul-quran'

📋 All translations in database (X):
   - slug: 'en_101_sahih-international', displayName: 'Sahih International'
   - slug: 'bn_161_taisirul-quran', displayName: 'তাইসীরুল কুরআন'
   - ...
```

### 2. 读取翻译时的日志（QuranTranslationFactory.kt）

```kotlin
═══════════════════════════════════════
📖 Getting translations for verse 1:1
   Requested slugs: [bn_161_taisirul-quran]
   After validation/sort: [bn_161_taisirul-quran]
   
   🔍 Querying table 'bn_161_taisirul-quran'...
      SQL: SELECT * FROM `bn_161_taisirul-quran` WHERE chapterNo=? AND verseNo=?
      Args: [1,1]
      Result count: 1
      ✅ Found translation: বিসমিল্লাহির রাহমানির রাহীম...
   
   📊 Result: 1 translations found
═══════════════════════════════════════
```

## 重新测试步骤

```bash
# 1. 编译
cd /Users/huwei/AndroidStudioProjects/quran0
./gradlew assembleDebug

# 2. 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. 清除数据（重新测试）
adb shell pm clear com.quran.quranaudio.online

# 4. 启动应用
adb shell am start -n com.quran.quranaudio.online/.quran_module.activities.ActivitySplash

# 5. 监控日志
adb logcat -c
adb logcat | grep -E "FragOnboardQuranVersion|QuranTranslationFactory|TranslUtils"
```

## 操作步骤

1. **引导页**：选择 Bengali (বাংলা)
2. **翻译选择**：选择 তাইসীরুল কুরআন
3. **点击 Continue**（等待下载 2-3秒）
4. **进入主页**：打开古兰经功能
5. **查看列表页**：验证孟加拉语章节名称
6. **打开 Surah 1**：查看经文详情页

## 关键检查点

### ✅ 下载阶段

- [ ] API response received (>1MB)
- [ ] QuranTranslBookInfo created with slug: 'bn_161_taisirul-quran'
- [ ] Translation stored in database
- [ ] All translations in database includes 'bn_161_taisirul-quran'

### ✅ SharedPreferences

- [ ] Key: key.translations
- [ ] Value: [bn_161_taisirul-quran]

### ✅ 读取阶段（打开经文页面时）

- [ ] Requested slugs: [bn_161_taisirul-quran]
- [ ] After validation/sort: [bn_161_taisirul-quran]
- [ ] SQL query executed on table `bn_161_taisirul-quran`
- [ ] Result count > 0
- [ ] Found translation: [孟加拉语文本]

## 可能的问题和解决方案

### 问题 1: Slug 不匹配

**症状**：
```
❌ No translation found in table 'bn_161_taisirul-quran'
```

**原因**：数据库中的表名与请求的 slug 不一致

**解决**：确保以下三处使用相同的 slug：
1. `TranslUtils.TRANSL_SLUG_BN_TAISIRUL` = "bn_161_taisirul-quran"
2. `LocalTranslationData.kt` versionId = TranslUtils.TRANSL_SLUG_BN_TAISIRUL
3. `QuranTranslBookInfo(version.versionId)` 构造时使用相同值

### 问题 2: 表不存在

**症状**：
```
❌ Query failed: no such table: bn_161_taisirul-quran
```

**原因**：翻译数据未正确保存到数据库

**解决**：
- 检查 `storeTranslation()` 是否成功
- 检查 `createTranslTable()` 是否被调用
- 查看 "All translations in database" 日志

### 问题 3: 数据格式问题

**症状**：
```
Result count: 0
```

**原因**：数据存在但格式不正确，无法查询到

**解决**：
- 检查 API 响应数据格式
- 验证 `readAndInsertChapters()` 的数据解析逻辑
- 确保 chapter_no 和 verse_no 正确

### 问题 4: SharedPreferences 未保存

**症状**：没有查询日志，经文页面未尝试加载翻译

**原因**：用户选择未保存到 SharedPreferences

**解决**：
- 检查 `saveSelectedVersion()` 是否被调用
- 验证 SharedPreferences 保存逻辑
- 查看 "Value: [bn_161_taisirul-quran]" 日志

## 预期的完整日志流程

```
# 引导页 - 下载
FragOnboardQuranVersion: 🚀 Continue clicked, selected: তাইসীরুল কুরআন
FragOnboardQuranVersion: 💾 STEP 5: 保存用户选择到数据库
FragOnboardQuranVersion:    版本ID: bn_161_taisirul-quran
FragOnboardQuranVersion:    ✅ 已保存翻译到 SharedPreferences
FragOnboardQuranVersion:    Key: key.translations
FragOnboardQuranVersion:    Value: [bn_161_taisirul-quran]
FragOnboardQuranVersion: 📥 STEP 4: 开始下载古兰经翻译版本
FragOnboardQuranVersion:    📡 下载源: Quran Foundation API
FragOnboardQuranVersion:    Translation ID: 161
FragOnboardQuranVersion:    ✅ API response received (1036712 bytes)
FragOnboardQuranVersion:    📊 QuranTranslBookInfo created:
FragOnboardQuranVersion:       slug: 'bn_161_taisirul-quran'
FragOnboardQuranVersion:       bookName: 'Taisirul Quran'
FragOnboardQuranVersion:    ✅ Translation stored in database with slug: 'bn_161_taisirul-quran'
FragOnboardQuranVersion:    📋 All translations in database (5):
FragOnboardQuranVersion:       - slug: 'en_101_sahih-international', displayName: 'Sahih International'
FragOnboardQuranVersion:       - slug: 'en_102_the-clear-quran', displayName: 'The Clear Quran'
FragOnboardQuranVersion:       - slug: 'in_quran-complex', displayName: 'Kompleks Al Quran'
FragOnboardQuranVersion:       - slug: 'in_junagarhi', displayName: 'Junagarhi'
FragOnboardQuranVersion:       - slug: 'bn_161_taisirul-quran', displayName: 'তাইসীরুল কুরআন'

# 主页 - 自动选择翻译
TranslUtils: 🌐 App language: bn (from SPAppConfigs)
TranslUtils: 🌐 Auto-selected translation: Bengali (Taisirul Quran)

# 经文详情页 - 读取翻译
QuranTranslationFactory: ═══════════════════════════════════════
QuranTranslationFactory: 📖 Getting translations for verse 1:1
QuranTranslationFactory:    Requested slugs: [bn_161_taisirul-quran]
QuranTranslationFactory:    After validation/sort: [bn_161_taisirul-quran]
QuranTranslationFactory:    🔍 Querying table 'bn_161_taisirul-quran'...
QuranTranslationFactory:          SQL: SELECT * FROM `bn_161_taisirul-quran` WHERE chapterNo=? AND verseNo=?
QuranTranslationFactory:          Args: [1,1]
QuranTranslationFactory:          Result count: 1
QuranTranslationFactory:       ✅ Found translation: পরম করুণাময় অতি দয়ালু আল্লাহর নামে...
QuranTranslationFactory:    📊 Result: 1 translations found
QuranTranslationFactory: ═══════════════════════════════════════
```

## 下一步

重新编译和测试，收集完整日志，然后对比预期日志找出问题所在。

---

**日期**: 2024-11-29  
**状态**: 调试中

