# Quiz Module - 多语言支持说明

## ✅ 当前支持的语言

Quiz 模块目前支持以下语言的题目内容：

| 语言 | 代码 | 题目文件 | 状态 |
|------|------|----------|------|
| 英语 (English) | `en` | `quiz_all_en.txt` | ✅ 已支持 |
| 印尼语 (Indonesian) | `id`, `in` | `quiz_all_id.txt` | ✅ 已支持 |
| 阿拉伯语 (Arabic) | `ar` | `quiz_all_ar.txt` | ✅ 已支持 |
| 其他语言 | - | `quiz_all_en.txt` | ⚠️ 默认英语 |

---

## 🔧 实现原理

### 统一的语言处理机制

**文件：** `quiz/src/main/java/com/quran/quranaudio/quiz/QuestionTools.kt`

```kotlin
/**
 * 根据应用语言获取题目内容
 * - 英语 (en): quiz_all_en.txt
 * - 印尼语 (id/in): quiz_all_id.txt
 * - 阿拉伯语 (ar): quiz_all_ar.txt
 * - 其他语言: 默认使用英语
 */
fun getQuestionStr(): String {
    // 🔧 每次调用时重新读取语言设置，确保语言切换后能及时更新
    AppConfig.setLanguage()
    
    val appLanguage = AppConfig.lan
    val planFileName = getQuizFileNameByLanguage(appLanguage)
    val readPath = "${saveRootPath}${File.separator}quiz${File.separator}$planFileName"
    
    // ...读取文件内容
}

/**
 * 根据语言代码获取题目文件名
 * @param languageCode 语言代码 (en, id, ar, etc.)
 * @return 题目文件名 (包含扩展名.txt)
 */
private fun getQuizFileNameByLanguage(languageCode: String): String {
    return when (languageCode) {
        "id", "in" -> "quiz_all_id.txt"  // 印尼语
        "ar" -> "quiz_all_ar.txt"         // 阿拉伯语
        else -> "quiz_all_en.txt"          // 英语（默认）
    }
}
```

---

### 语言设置读取

**文件：** `quiz/src/main/java/com/quran/quranaudio/quiz/utils/AppConfig.kt`

```kotlin
fun setLanguage() {
    // 🔧 优先从用户设置的语言配置读取
    val sp = context.getSharedPreferences("sp_app_configs", MODE_PRIVATE)
    val savedLanguage = sp.getString("key.app.language", null)
    
    if (!savedLanguage.isNullOrEmpty()) {
        // 使用用户设置的语言
        lan = savedLanguage
    } else {
        // 回退到系统语言
        lan = context.resources.configuration.locale.language
    }
    
    // 语言代码标准化
    when(lan) {
        "in" -> lan = "id"  // 印尼语标准化
    }
}
```

---

### 主应用语言管理

**文件：** `app/.../SPAppConfigs.kt`

```kotlin
val supportedLanguages = listOf("en", "id", "ar", "ur", "ms", "tr", "bn")

fun getLocale(ctx: Context): String {
    val savedLanguage = sp.getString(KEY_APP_LANGUAGE, null)
    
    if (!savedLanguage.isNullOrEmpty()) {
        return savedLanguage  // 返回用户设置的语言
    }
    
    // 首次启动：检测设备语言
    var deviceLanguage = Locale.getDefault().language
    
    // 语言代码统一
    if (deviceLanguage == "in") {
        deviceLanguage = "id"
    }
    
    // 如果设备语言在支持列表中，使用设备语言；否则使用英语
    return if (deviceLanguage in supportedLanguages) {
        deviceLanguage
    } else {
        "en"  // 默认英语
    }
}
```

---

## 🎯 关键特性

### 1. 自动语言切换

- ✅ **每次调用时读取最新语言设置**
  ```kotlin
  fun getQuestionStr(): String {
      AppConfig.setLanguage()  // 🔧 每次都重新读取
      // ...
  }
  ```

### 2. 统一的处理逻辑

- ✅ **所有语言通过同一个方法处理**
  ```kotlin
  private fun getQuizFileNameByLanguage(languageCode: String): String {
      return when (languageCode) {
          "id", "in" -> "quiz_all_id.txt"
          "ar" -> "quiz_all_ar.txt"
          else -> "quiz_all_en.txt"
      }
  }
  ```

- ✅ **新增语言只需添加一个 case**
  ```kotlin
  "ur" -> "quiz_all_ur.txt"  // 乌尔都语（示例）
  ```

### 3. 语言代码标准化

- ✅ **印尼语：`in` → `id`**
- ✅ **阿拉伯语：`ar`（无需转换）**
- ✅ **其他语言默认英语**

---

## 🧪 测试方法

### 测试阿拉伯语题目

#### 方法 1：通过应用设置切换

1. 打开应用
2. 进入 **Settings** → **Language**
3. 选择 **العربية (Arabic)**
4. 应用会重启并应用新语言
5. 进入 **Quiz** 模块
6. 观察题目内容

**预期结果：**
- ✅ 题目显示阿拉伯语内容
- ✅ 题目从 `quiz_all_ar.txt` 加载

---

#### 方法 2：手动验证日志

```bash
# 1. 清空日志
adb logcat -c

# 2. 打开Quiz模块

# 3. 查看日志
adb logcat | grep "QuestionTools"
```

**预期日志：**
```
QuestionTools: ═════════════════════════════════════
QuestionTools: 🔍 getQuestionStr() 调用
QuestionTools:   - 应用语言: ar
QuestionTools:   - 题目文件: quiz_all_ar.txt
QuestionTools:   - 完整路径: /data/user/0/.../files/quiz/quiz_all_ar.txt
QuestionTools:   - 文件是否存在: true
QuestionTools:   ✅ 成功读取题目文件，内容长度: XXXXX
QuestionTools: ═════════════════════════════════════
```

---

#### 方法 3：手动检查 SharedPreferences

```bash
# 查看保存的语言设置
adb shell run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/sp_app_configs.xml
```

**预期内容：**
```xml
<string name="key.app.language">ar</string>
```

---

### 测试其他语言

**英语 (en):**
```
- 应用语言: en
- 题目文件: quiz_all_en.txt
- ✅ 正常显示
```

**印尼语 (id):**
```
- 应用语言: id
- 题目文件: quiz_all_id.txt
- ✅ 正常显示
```

**阿拉伯语 (ar):**
```
- 应用语言: ar
- 题目文件: quiz_all_ar.txt
- ✅ 应该正常显示
```

**其他语言（例如乌尔都语 ur）:**
```
- 应用语言: ur
- 题目文件: quiz_all_en.txt  (默认英语)
- ⚠️ 显示英语内容（因为没有 ur 题目文件）
```

---

## 🔍 故障排查

### 问题 1：切换到阿语后题目仍显示英语

**可能原因：**

1. **语言设置未保存**
   ```bash
   # 检查 SharedPreferences
   adb shell run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/sp_app_configs.xml | grep language
   ```
   
   **解决方案：** 确保应用正确保存了语言设置

2. **题目文件不存在**
   ```bash
   # 检查文件是否存在
   adb shell run-as com.quran.quranaudio.online ls /data/data/com.quran.quranaudio.online/files/quiz/
   ```
   
   **预期输出：**
   ```
   quiz_all_en.txt
   quiz_all_id.txt
   quiz_all_ar.txt
   ```
   
   **解决方案：** 如果缺少 `quiz_all_ar.txt`，需要重新安装应用

3. **语言代码不匹配**
   ```bash
   # 查看日志
   adb logcat | grep "AppConfig.*语言"
   ```
   
   **预期日志：**
   ```
   📱 从用户设置读取语言: ar
   ✅ 最终使用语言: ar (isID=false)
   ```

---

### 问题 2：题目文件缺失

**诊断：**
```bash
adb logcat | grep "QuestionTools.*❌"
```

**可能看到：**
```
QuestionTools:   ❌ 题目文件不存在！
```

**解决方案：**

1. **清除应用数据并重新安装**
   ```bash
   adb uninstall com.quran.quranaudio.online
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **验证 quiz.zip 中包含 ar 文件**
   ```bash
   unzip -l quiz/src/main/assets/quiz.zip
   ```
   
   **预期输出：**
   ```
   quiz/quiz_all_en.txt
   quiz/quiz_all_id.txt
   quiz/quiz_all_ar.txt
   ```

---

## 📦 Quiz 资源文件结构

### assets 中的 quiz.zip

**位置：** `quiz/src/main/assets/quiz.zip`

**内部结构：**
```
quiz.zip
└── quiz/
    ├── quiz_all_en.txt  (英语题目)
    ├── quiz_all_id.txt  (印尼语题目)
    └── quiz_all_ar.txt  (阿拉伯语题目)
```

### 解压后的文件位置

**运行时位置：**
```
/data/data/com.quran.quranaudio.online/files/quiz/
├── quiz_all_en.txt
├── quiz_all_id.txt
└── quiz_all_ar.txt
```

---

## ✅ 添加新语言的步骤

### 1. 准备题目文件

创建新的题目文件，例如 `quiz_all_ur.txt`（乌尔都语）

**格式：** 与现有文件格式一致（JSON 数组）

---

### 2. 更新 quiz.zip

```bash
cd quiz/src/main/assets/quiz/
# 添加新文件
cp quiz_all_ur.txt quiz/
# 重新打包
zip -r ../quiz.zip quiz/
```

---

### 3. 更新 QuestionTools.kt

```kotlin
private fun getQuizFileNameByLanguage(languageCode: String): String {
    return when (languageCode) {
        "id", "in" -> "quiz_all_id.txt"
        "ar" -> "quiz_all_ar.txt"
        "ur" -> "quiz_all_ur.txt"  // ✅ 新增
        else -> "quiz_all_en.txt"
    }
}
```

---

### 4. （可选）更新 SPAppConfigs.kt

如果希望主应用支持该语言：

```kotlin
val supportedLanguages = listOf("en", "id", "ar", "ur", "ms", "tr", "bn")
//                                               ↑ 确保在支持列表中
```

---

### 5. 测试

1. 卸载旧版本应用
2. 安装新版本应用
3. 切换到新语言
4. 验证题目内容

---

## 📊 总结

### 当前状态

| 功能 | 状态 |
|------|------|
| 英语支持 | ✅ 完全支持 |
| 印尼语支持 | ✅ 完全支持 |
| 阿拉伯语支持 | ✅ **已完全支持** |
| 自动语言切换 | ✅ 完全支持 |
| 统一处理逻辑 | ✅ 完全支持 |
| 新增语言简便性 | ✅ 只需添加一个 case |

---

### 关键优势

1. ✅ **统一的语言处理机制**
   - 所有语言通过同一个方法处理
   - 新增语言只需修改一处代码

2. ✅ **自动语言切换**
   - 每次加载题目时自动读取最新语言设置
   - 无需重启应用

3. ✅ **语言代码标准化**
   - 统一处理语言代码变体（如 `in` → `id`）

4. ✅ **默认回退机制**
   - 不支持的语言自动回退到英语
   - 避免崩溃或空白内容

---

## 🎯 验证清单

测试阿拉伯语支持时，请确认：

- [ ] 应用语言设置为阿拉伯语
- [ ] SharedPreferences 中 `key.app.language` 为 `"ar"`
- [ ] 日志显示 `应用语言: ar`
- [ ] 日志显示 `题目文件: quiz_all_ar.txt`
- [ ] 日志显示 `文件是否存在: true`
- [ ] Quiz 页面显示阿拉伯语题目内容
- [ ] 题目内容与 `quiz_all_ar.txt` 中的内容一致

---

**文档创建时间：** 2025-11-18  
**版本：** v1.8.1 (versionCode=73)  
**状态：** ✅ 阿拉伯语已完全支持  
**测试状态：** 等待用户验证

---

## 🚀 下一步

**如果阿拉伯语题目仍不显示，请提供：**

1. **日志输出**
   ```bash
   adb logcat | grep "QuestionTools\|AppConfig.*语言"
   ```

2. **SharedPreferences 内容**
   ```bash
   adb shell run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/sp_app_configs.xml
   ```

3. **文件列表**
   ```bash
   adb shell run-as com.quran.quranaudio.online ls -la /data/data/com.quran.quranaudio.online/files/quiz/
   ```

我会根据这些信息进一步诊断问题！

