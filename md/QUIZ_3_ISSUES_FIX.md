# Quiz Module - 3个问题修复

## 📋 问题总结

### 问题1: Quiz题目语言未切换
- **现象：** 在引导页选择印尼语后，Quiz题目仍显示英语
- **原因：** `AppConfig.lan` 只在应用启动时设置一次，题目加载时没有重新读取语言设置

### 问题2: 翻译经文显示乱码
- **现象：** 答题错误结果页显示 `Segala puji<fn index="1">1</fn> bagi Allah, Tuhan<fn index="2">2</fn> semesta alam.`
- **原因：** 翻译文本包含HTML脚注标签 `<fn index="...">...</fn>`，未进行清理

### 问题3: Full Tafsir页面显示 "Invalid params"
- **现象：** 订阅用户点击 "Full Tafsir (Premium)" 后打开空白页，显示 "Invalid params"
- **原因：** Intent参数名称错误，使用了 `"chapter_no"` 而不是 `"reader.chapter_no"`

---

## ✅ 修复方案

### 修复1: 动态刷新Quiz语言

**文件：** `quiz/src/main/java/com/quran/quranaudio/quiz/QuestionTools.kt`

**修改：** 在 `getQuestionStr()` 方法中添加语言刷新

```kotlin
fun getQuestionStr(): String {
    // 🔧 每次调用时重新读取语言设置，确保语言切换后能及时更新
    AppConfig.setLanguage()
    
    val appLanguage = AppConfig.lan
    val planFileName = getQuizFileNameByLanguage(appLanguage)
    // ... 后续代码
}
```

**原理：**
- 每次加载题目时，重新从 `SharedPreferences` 读取语言设置
- 确保语言切换后能立即生效
- 不依赖应用重启

**日志增强：**
```kotlin
android.util.Log.d("QuestionTools", "═════════════════════════════════════")
android.util.Log.d("QuestionTools", "🔍 getQuestionStr() 调用")
android.util.Log.d("QuestionTools", "  - 应用语言: $appLanguage")
android.util.Log.d("QuestionTools", "  - 题目文件: $planFileName")
android.util.Log.d("QuestionTools", "  - 内容前100字符: ${content.take(100)}")
```

---

### 修复2: 清理翻译文本的HTML标签

**文件：** `app/src/main/java/com/quran/quranaudio/online/quran_module/quiz/QuranDataRepositoryImpl.kt`

**修改1：添加清理方法**

```kotlin
/**
 * 清理HTML标签
 * 移除如 <fn index="1">1</fn> 这样的脚注标签
 */
private fun cleanHtmlTags(text: String): String {
    return text
        // 移除 <fn ...>...</fn> 脚注标签
        .replace(Regex("<fn[^>]*>[^<]*</fn>"), "")
        // 移除其他HTML标签
        .replace(Regex("<[^>]+>"), "")
        // 清理多余的空格
        .replace(Regex("\\s+"), " ")
        .trim()
}
```

**修改2：在加载翻译时使用**

```kotlin
private fun loadTranslation(surahId: Int, ayahId: Int): String {
    // ... 获取translationText ...
    
    // 🔧 清理HTML标签（如 <fn index="1">1</fn>）
    val cleanedText = cleanHtmlTags(translationText)
    Log.d(TAG, "🧹 Cleaned HTML tags from translation")
    
    return cleanedText
}
```

**处理前后对比：**

| 处理前 | 处理后 |
|--------|--------|
| `Segala puji<fn index="1">1</fn> bagi Allah, Tuhan<fn index="2">2</fn> semesta alam.` | `Segala puji bagi Allah, Tuhan semesta alam.` |

**支持的清理：**
- ✅ 移除 `<fn>` 脚注标签
- ✅ 移除任何其他HTML标签
- ✅ 规范化空白字符
- ✅ 去除首尾空格

---

### 修复3: 修正Tafsir页面参数

**文件：** `quiz/src/main/java/com/quran/quranaudio/quiz/activity/QuizReviewLearnActivity.kt`

**修改前（错误的参数名称）：**
```kotlin
intent.putExtra("chapter_no", surahId)
intent.putExtra("verse_no", ayahId)
intent.putExtra("tafsir_key", tafsirSlug)
```

**修改后（正确的参数名称）：**
```kotlin
// 🔧 修复参数名称，使用ActivityTafsir期望的Keys
intent.putExtra("reader.chapter_no", surahId)
intent.putExtra("reader.verse_no", ayahId)
intent.putExtra("tafsirKey", tafsirSlug)  // 注意是 "tafsirKey" 不是 "tafsir_key"
```

**参数名称对照表：**

| 错误名称 | 正确名称 | 说明 |
|---------|---------|------|
| `"chapter_no"` | `"reader.chapter_no"` | Surah章节编号 |
| `"verse_no"` | `"reader.verse_no"` | Ayah经文编号 |
| `"tafsir_key"` | `"tafsirKey"` | Tafsir注释Slug |

**参数来源：**
- 来自 `app/src/main/java/com/quran/quranaudio/online/quran_module/utils/univ/Keys.kt`
- `READER_KEY_CHAPTER_NO = "reader.chapter_no"`
- `READER_KEY_VERSE_NO = "reader.verse_no"`

**添加日志：**
```kotlin
android.util.Log.d(TAG, "✅ Opening Tafsir - Surah:$surahId, Ayah:$ayahId, Slug:$tafsirSlug")
```

---

## 📊 修改文件清单

| 文件 | 修改内容 | 行数变化 |
|------|----------|---------|
| `QuestionTools.kt` | 添加动态语言刷新 + 增强日志 | +8 |
| `QuranDataRepositoryImpl.kt` | 添加HTML清理方法 | +16 |
| `QuizReviewLearnActivity.kt` | 修正Tafsir参数名称 | +4 |

**总计：** 3个文件，+28行

---

## 🧪 测试指南

### 测试1: Quiz语言切换

**步骤：**
1. 在引导页选择「印尼语 (Indonesian)」
2. 完成引导流程
3. 进入Quiz模块
4. 答题

**预期结果：**
- ✅ 题目显示印尼语内容
- ✅ 选项显示印尼语
- ✅ 不再显示英语题目

**日志验证：**
```bash
adb logcat | grep "QuestionTools"
```

**成功日志示例：**
```
QuestionTools: ═════════════════════════════════════
QuestionTools: 🔍 getQuestionStr() 调用
QuestionTools:   - 应用语言: id
QuestionTools:   - 题目文件: quiz_all_id.txt
QuestionTools:   - 完整路径: /data/user/0/.../files/quiz/quiz_all_id.txt
QuestionTools:   - 文件是否存在: true
QuestionTools:   ✅ 成功读取题目文件，内容长度: XXXXX
QuestionTools:   - 内容前100字符: [印尼语内容]
QuestionTools: ═════════════════════════════════════
```

---

### 测试2: 翻译文本清理

**步骤：**
1. 进入Quiz模块
2. 故意答错一道题
3. 进入答题错误结果页
4. 查看 "Related Verse" 下的翻译文本

**预期结果：**
- ✅ 翻译文本干净整洁
- ❌ 不显示 `<fn index="1">1</fn>` 这样的HTML标签
- ✅ 文本流畅可读

**修复前：**
```
Segala puji<fn index="1">1</fn> bagi Allah, 
Tuhan<fn index="2">2</fn> semesta alam.
```

**修复后：**
```
Segala puji bagi Allah, Tuhan semesta alam.
```

**日志验证：**
```bash
adb logcat | grep "QuranDataRepository"
```

**成功日志示例：**
```
QuranDataRepository: ✅ Translation loaded (XXX chars)
QuranDataRepository: 🧹 Cleaned HTML tags from translation
```

---

### 测试3: Full Tafsir页面

**前提条件：**
- 使用测试订阅账户登录

**步骤：**
1. 进入Quiz模块
2. 故意答错一道题
3. 进入答题错误结果页
4. 点击 "Full Tafsir (Premium)" 按钮

**预期结果：**
- ✅ 正常打开Tafsir页面
- ✅ 显示对应章节的注释内容
- ❌ **不再显示** "Invalid params"

**日志验证：**
```bash
adb logcat | grep "QuizReviewLearn"
```

**成功日志示例：**
```
QuizReviewLearn: ✅ Opening Tafsir - Surah:1, Ayah:2, Slug:ar-tafsir-al-muyassar
```

---

## 🔍 问题诊断

### 如果Quiz语言仍未切换

**诊断步骤：**

1. **检查SharedPreferences：**
```bash
adb shell run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/sp_app_configs.xml
```

查找 `<string name="key.app.language">id</string>`

2. **检查日志：**
```bash
adb logcat -c
adb logcat | grep -E "QuestionTools|AppConfig|lan_config"
```

3. **关键日志：**
```
AppConfig: 📱 从用户设置读取语言: id
AppConfig: ✅ 最终使用语言: id (isID=true)
QuestionTools:   - 应用语言: id
QuestionTools:   - 题目文件: quiz_all_id.txt
```

---

### 如果翻译仍显示HTML标签

**诊断步骤：**

1. **检查翻译源：**
- HTML标签可能来自数据库本身
- 检查 `QuranTranslationFactory.getTranslationsSingleVerse()` 返回值

2. **检查清理逻辑：**
```bash
adb logcat | grep "QuranDataRepository"
```

应该看到：
```
QuranDataRepository: 🧹 Cleaned HTML tags from translation
```

3. **手动验证正则表达式：**
```kotlin
val testText = "Segala puji<fn index=\"1\">1</fn> bagi Allah"
val cleaned = testText.replace(Regex("<fn[^>]*>[^<]*</fn>"), "")
// 应该得到: "Segala puji bagi Allah"
```

---

### 如果Tafsir页面仍显示 "Invalid params"

**诊断步骤：**

1. **检查Intent参数：**
```bash
adb logcat | grep "Opening Tafsir"
```

应该看到：
```
QuizReviewLearn: ✅ Opening Tafsir - Surah:1, Ayah:2, Slug:ar-tafsir-al-muyassar
```

2. **检查ActivityTafsir接收：**
```bash
adb logcat | grep "ActivityTafsir"
```

3. **检查数值：**
- `surahId` 应该 >= 1
- `ayahId` 应该 >= 1
- `tafsirSlug` 不应为空

如果仍显示 "Invalid params"，检查 `ActivityTafsir` 的代码：
```kotlin
val chapterNo = intent.getIntExtra("reader.chapter_no", -1)
val verseNo = intent.getIntExtra("reader.verse_no", -1)

if (chapterNo < 1 || verseNo < 1) {
    showInvalidParams()
}
```

---

## 📚 技术细节

### Quiz语言动态刷新原理

**问题根源：**
- `AppConfig.lan` 是静态变量，只在应用启动时初始化一次
- 用户切换语言后，`Activity.recreate()` 重新创建Activity，但不重启整个应用
- Quiz模块的题目加载在应用启动早期就缓存了，不会自动更新

**解决方案：**
- 在 `getQuestionStr()` 每次调用时，重新调用 `AppConfig.setLanguage()`
- `setLanguage()` 会从 `SharedPreferences` 重新读取用户设置的语言
- 确保语言切换实时生效

**性能影响：**
- `SharedPreferences.getString()` 是内存操作，性能损耗极小（< 1ms）
- 文件IO只在缓存未命中时发生
- 可接受的性能代价

---

### HTML标签清理正则表达式

**正则模式：**
```kotlin
Regex("<fn[^>]*>[^<]*</fn>")
```

**解释：**
- `<fn` - 匹配开始标签
- `[^>]*` - 匹配任意属性（如 `index="1"`）
- `>` - 标签结束
- `[^<]*` - 匹配标签内容（如 `1`）
- `</fn>` - 匹配闭合标签

**匹配示例：**
- ✅ `<fn index="1">1</fn>`
- ✅ `<fn index="2">2</fn>`
- ✅ `<fn>text</fn>`

**不匹配：**
- ❌ `<div>...</div>` （由第二个正则处理）
- ❌ 普通文本

**通用清理：**
```kotlin
Regex("<[^>]+>")
```
移除所有其他HTML标签。

---

### Intent参数名称规范

**Android Intent参数最佳实践：**

1. **使用命名空间防止冲突：**
```kotlin
// ✅ 好的实践
"reader.chapter_no"
"reader.verse_no"

// ❌ 不好的实践
"chapter_no"
"verse_no"
```

2. **使用常量而非硬编码：**
```kotlin
// ✅ 好的实践
object Keys {
    const val READER_KEY_CHAPTER_NO = "reader.chapter_no"
}
intent.putExtra(Keys.READER_KEY_CHAPTER_NO, value)

// ❌ 不好的实践
intent.putExtra("reader.chapter_no", value)
```

3. **驼峰命名 vs 点分隔：**
- 驼峰：`"tafsirKey"` - 适合单个配置
- 点分隔：`"reader.chapter_no"` - 适合分组配置

---

## ✅ 验证清单

### 编译测试
- [x] **编译成功** - BUILD SUCCESSFUL
- [x] **无错误** - No compilation errors
- [x] **警告可忽略** - Warnings are non-critical

### 功能测试
- [ ] **Quiz语言切换** - 印尼语题目正常显示
- [ ] **翻译文本清理** - 无HTML标签显示
- [ ] **Tafsir页面打开** - 订阅用户可查看完整注释

### 日志验证
- [ ] **语言日志** - 显示 "应用语言: id"
- [ ] **文件日志** - 显示 "quiz_all_id.txt"
- [ ] **清理日志** - 显示 "Cleaned HTML tags"
- [ ] **参数日志** - 显示 "Opening Tafsir - Surah:X, Ayah:Y"

---

## 📦 安装测试

### 完整测试流程

```bash
# 1. 编译APK
cd /Users/huwei/AndroidStudioProjects/quran0
./gradlew :app:assembleDebug

# 2. 卸载旧版本
adb uninstall com.quran.quranaudio.online

# 3. 安装新版本
adb install app/build/outputs/apk/debug/app-debug.apk

# 4. 清空日志
adb logcat -c

# 5. 启动日志监控
adb logcat | grep -E "QuestionTools|QuranDataRepository|QuizReviewLearn|ActivityTafsir"
```

### 完整测试步骤

1. **测试语言切换：**
   - 在引导页选择「印尼语」
   - 进入Quiz模块
   - 确认题目为印尼语

2. **测试翻译清理：**
   - 答错一道题
   - 查看"Related Verse"
   - 确认无HTML标签

3. **测试Tafsir页面：**
   - 使用订阅账户
   - 点击"Full Tafsir (Premium)"
   - 确认页面正常显示

---

## 🎯 修复总结

| 问题 | 状态 | 解决方案 | 影响文件 |
|------|------|----------|---------|
| Quiz语言未切换 | ✅ | 动态刷新语言设置 | QuestionTools.kt |
| 翻译显示HTML标签 | ✅ | 添加HTML清理方法 | QuranDataRepositoryImpl.kt |
| Tafsir页面Invalid params | ✅ | 修正Intent参数名称 | QuizReviewLearnActivity.kt |

**总体影响：**
- ✅ 代码修改量小（3个文件，28行）
- ✅ 无破坏性更改
- ✅ 向后兼容
- ✅ 性能影响极小

---

**修复完成时间：** 2025-11-18  
**修复人员：** AI Assistant  
**版本：** v1.8.1  
**状态：** ✅ 编译成功，等待测试验证  
**APK位置：** `app/build/outputs/apk/debug/app-debug.apk`

