# 🌐 多语言设置功能 - 当前架构分析

## 📊 **当前多语言实现分析**

### 1. **语言配置管理** (`SPAppConfigs.kt`)

```kotlin
object SPAppConfigs {
    private const val KEY_APP_LANGUAGE = "key.app.language"
    const val LOCALE_DEFAULT = "en"
    
    // 获取当前语言
    fun getLocale(ctx: Context): String {
        val savedLanguage = sp.getString(KEY_APP_LANGUAGE, null)
        
        // 如果已保存，直接返回
        if (!savedLanguage.isNullOrEmpty()) {
            return savedLanguage
        }
        
        // 首次启动：检测设备语言
        var deviceLanguage = Locale.getDefault().language
        
        // 处理印尼语代码 (id → in)
        if (deviceLanguage == "id") {
            deviceLanguage = "in"
        }
        
        // 支持的语言列表
        val supportedLanguages = listOf("en", "in", "ar", "ur", "ms", "tr", "bn")
        
        // 匹配或使用默认
        val selectedLanguage = if (deviceLanguage in supportedLanguages) {
            deviceLanguage
        } else {
            LOCALE_DEFAULT  // "en"
        }
        
        // 自动保存
        setLocale(ctx, selectedLanguage)
        
        return selectedLanguage
    }
    
    // 保存语言配置
    fun setLocale(ctx: Context, locale: String?) {
        sp(ctx).edit().apply {
            putString(KEY_APP_LANGUAGE, locale)
            commit()
        }
    }
}
```

**关键点**：
- ✅ 首次启动自动检测设备语言
- ✅ 如果设备语言在支持列表中，使用设备语言
- ✅ 否则使用英语 (`en`) 作为默认
- ✅ 自动保存到 `SharedPreferences`
- ✅ 后续启动直接读取保存的语言

---

### 2. **语言应用机制** (`BaseActivity.java`)

```java
public abstract class BaseActivity extends ResHelperActivity {
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(initBeforeBaseAttach(base));
    }
    
    private Context initBeforeBaseAttach(Context base) {
        adjustFontScale(base);
        return updateBaseContextLocale(base);
    }
    
    private Context updateBaseContextLocale(Context context) {
        String language = SPAppConfigs.getLocale(context);
        
        if (language == null || language.isEmpty()) {
            return context;
        }
        
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N) {
            return updateResourcesLocale(context, locale);
        }
        return updateResourcesLocaleLegacy(context, locale);
    }
    
    @TargetApi(Build.VERSION_CODES.N_MR1)
    private Context updateResourcesLocale(Context context, Locale locale) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }
    
    private Context updateResourcesLocaleLegacy(Context context, Locale locale) {
        Resources resources = context.getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.locale = locale;
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        return context;
    }
}
```

**关键点**：
- ✅ 每个 `Activity` 启动时自动应用语言配置
- ✅ 通过 `attachBaseContext()` 注入新的 `Context`
- ✅ 使用 `createConfigurationContext()` (Android N+) 或 `updateConfiguration()` (旧版本)
- ✅ 支持 Android 所有版本

---

### 3. **支持的语言列表**

| 语言代码 | 语言名称 | 资源文件夹 | 状态 |
|---------|---------|-----------|------|
| `en` | English | `values/` (默认) | ✅ 支持 |
| `in` | Bahasa Indonesia | `values-in/` | ✅ 支持 |
| `ar` | العربية (Arabic) | `values-ar/` | ✅ 支持 |
| `ur` | اردو (Urdu) | `values-ur/` | ✅ 支持 |
| `ms` | Bahasa Melayu | `values-ms/` | ✅ 支持 |
| `tr` | Türkçe (Turkish) | `values-tr/` | ✅ 支持 |
| `bn` | বাংলা (Bengali) | `values-bn/` | ✅ 支持 |

---

## 🎯 **需要实现的功能**

### 功能需求

1. **在设置页面添加"语言"选项**
   - 显示当前选中的语言
   - 点击后弹出语言选择对话框

2. **语言选择对话框**
   - 显示所有支持的语言
   - 单选模式
   - 确认后保存并重启应用

3. **用户手动切换语言**
   - 调用 `SPAppConfigs.setLocale()` 保存
   - 重启 `Activity` 应用新语言

4. **不影响现有自动检测逻辑**
   - 只在首次启动且无保存时自动检测
   - 用户手动选择后优先使用保存的语言

---

## 🔧 **实现方案**

### 步骤1：创建语言管理工具类

**文件**：`app/src/main/java/com/quran/quranaudio/online/quran_module/utils/LanguageManager.kt`

```kotlin
object LanguageManager {
    
    // 支持的语言列表 (与 SPAppConfigs 保持一致)
    val SUPPORTED_LANGUAGES = linkedMapOf(
        "en" to "English",
        "in" to "Bahasa Indonesia",
        "ar" to "العربية",
        "ur" to "اردو",
        "ms" to "Bahasa Melayu",
        "tr" to "Türkçe",
        "bn" to "বাংলা"
    )
    
    /**
     * 获取当前选中的语言代码
     */
    fun getCurrentLanguageCode(context: Context): String {
        return SPAppConfigs.getLocale(context)
    }
    
    /**
     * 获取当前语言的显示名称
     */
    fun getCurrentLanguageName(context: Context): String {
        val code = getCurrentLanguageCode(context)
        return SUPPORTED_LANGUAGES[code] ?: "English"
    }
    
    /**
     * 保存语言并重启Activity
     */
    fun setLanguageAndRestart(activity: Activity, languageCode: String) {
        // 1. 保存语言配置
        SPAppConfigs.setLocale(activity, languageCode)
        
        // 2. 重启Activity
        activity.recreate()
    }
}
```

### 步骤2：在设置页面添加UI

**位置**：找到设置页面的主布局或Fragment

选项1：使用 `PreferenceScreen` (如果是 Preference-based 设置页面)
选项2：在现有布局中添加 `LinearLayout` 项

### 步骤3：实现语言选择对话框

在设置 `Fragment` 中添加：

```kotlin
private fun showLanguagePickerDialog() {
    val currentCode = LanguageManager.getCurrentLanguageCode(requireContext())
    val languages = LanguageManager.SUPPORTED_LANGUAGES
    val languageCodes = languages.keys.toList()
    val displayNames = languages.values.toTypedArray()
    
    var checkedItem = languageCodes.indexOf(currentCode)
    
    AlertDialog.Builder(requireContext())
        .setTitle(R.string.setting_language_title)
        .setSingleChoiceItems(displayNames, checkedItem) { _, which ->
            checkedItem = which
        }
        .setPositiveButton(R.string.ok) { dialog, _ ->
            val newCode = languageCodes[checkedItem]
            if (newCode != currentCode) {
                LanguageManager.setLanguageAndRestart(requireActivity(), newCode)
            }
            dialog.dismiss()
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}
```

---

## ✅ **兼容性保证**

### 1. **不影响首次启动自动检测**

```kotlin
// SPAppConfigs.getLocale() 的逻辑
val savedLanguage = sp.getString(KEY_APP_LANGUAGE, null)

// 如果已保存，直接返回 (用户手动选择的)
if (!savedLanguage.isNullOrEmpty()) {
    return savedLanguage  ✅
}

// 无保存时才自动检测设备语言
var deviceLanguage = Locale.getDefault().language  ✅
```

### 2. **BaseActivity 自动应用语言**

```java
// 每个 Activity 启动时自动调用
protected void attachBaseContext(Context base) {
    String language = SPAppConfigs.getLocale(base);
    // 创建新的 Context 并应用语言
    return createConfigurationContext(configuration);
}
```

**结论**：✅ 完全兼容，不会影响现有逻辑

---

## 🧪 **测试方案**

### 场景1：首次安装（自动检测）
1. 清除应用数据
2. 系统语言设为印尼语
3. 启动应用
4. **预期**：应用自动使用印尼语

### 场景2：用户手动切换
1. 进入设置页面
2. 点击"语言"选项
3. 选择"中文"（不支持的语言显示英语）
4. 点击确认
5. **预期**：应用重启并使用英语

### 场景3：手动选择后重启应用
1. 用户手动选择了乌尔都语
2. 关闭应用
3. 重新启动应用
4. **预期**：应用仍然使用乌尔都语（不会自动检测设备语言）

---

## 📝 **需要的资源文件**

### strings.xml (所有语言)

```xml
<!-- 英语 (values/strings.xml) -->
<string name="setting_language_title">Language</string>
<string name="setting_language_subtitle">Choose your preferred language</string>

<!-- 印尼语 (values-in/strings.xml) -->
<string name="setting_language_title">Bahasa</string>
<string name="setting_language_subtitle">Pilih bahasa yang Anda inginkan</string>

<!-- 阿拉伯语 (values-ar/strings.xml) -->
<string name="setting_language_title">اللغة</string>
<string name="setting_language_subtitle">اختر لغتك المفضلة</string>

<!-- 乌尔都语 (values-ur/strings.xml) -->
<string name="setting_language_title">زبان</string>
<string name="setting_language_subtitle">اپنی پسندیدہ زبان منتخب کریں</string>
```

---

## 🚀 **实施步骤总结**

1. ✅ **创建 `LanguageManager.kt`** - 语言管理工具类
2. ✅ **添加字符串资源** - 所有语言的翻译
3. ✅ **修改设置页面布局** - 添加语言选项UI
4. ✅ **实现对话框逻辑** - 语言选择和确认
5. ✅ **测试所有场景** - 首次启动、手动切换、重启应用

---

## 📦 **相关文件列表**

- **配置管理**：`SPAppConfigs.kt`
- **语言应用**：`BaseActivity.java`
- **设置页面**：`FragSettingsMain.java`
- **新增工具**：`LanguageManager.kt` (待创建)
- **字符串资源**：`values/strings.xml`, `values-in/strings.xml`, 等

---

**最后更新**：2025-01-15
**状态**：📋 分析完成，准备实施

