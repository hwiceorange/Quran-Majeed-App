package com.quran.quranaudio.online.quran_module.utils

import android.app.Activity
import android.content.Context
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs

/**
 * 🌐 语言管理工具类
 * 
 * 负责管理应用的多语言配置，包括：
 * - 支持的语言列表
 * - 获取当前语言
 * - 保存并应用新语言
 * 
 * @author AI Assistant (Cursor)
 * @version 1.5.5
 */
object LanguageManager {
    
    /**
     * 🌍 应用支持的语言列表
     * 
     * 格式：语言代码 -> 显示名称
     * 
     * 注意：
     * - 保持与 SPAppConfigs.supportedLanguages 一致
     * - 使用 LinkedHashMap 保持顺序
     * - 语言名称使用原生语言（非英文翻译）
     */
    val SUPPORTED_LANGUAGES = linkedMapOf(
        "en" to "English",
        "id" to "Bahasa Indonesia",  // 统一使用 "id" 表示印尼语
        "ar" to "العربية",
        "ur" to "اردو",
        "ms" to "Bahasa Melayu",
        "tr" to "Türkçe",
        "bn" to "বাংলা"
    )
    
    /**
     * 📱 获取当前应用使用的语言代码
     * 
     * @param context Android Context
     * @return 语言代码 (如: "en", "id", "ar", "ur", 等)
     */
    fun getCurrentLanguageCode(context: Context): String {
        return SPAppConfigs.getLocale(context)
    }
    
    /**
     * 🏷️ 获取当前语言的显示名称
     * 
     * 用于在UI中显示当前选中的语言
     * 
     * @param context Android Context
     * @return 语言显示名称 (如: "English", "العربية", 等)
     */
    fun getCurrentLanguageName(context: Context): String {
        val code = getCurrentLanguageCode(context)
        return SUPPORTED_LANGUAGES[code] ?: "English"
    }
    
    /**
     * 💾 保存语言配置并重启Activity
     * 
     * 工作流程：
     * 1. 调用 SPAppConfigs.setLocale() 保存语言配置
     * 2. 调用 Activity.recreate() 重启Activity
     * 3. BaseActivity.attachBaseContext() 自动应用新语言
     * 
     * 注意：
     * - 此方法会立即重启Activity
     * - 不会丢失Activity状态（通过 onSaveInstanceState）
     * - 新语言会在整个应用中生效
     * 
     * @param activity 当前Activity
     * @param languageCode 新的语言代码 (如: "en", "id", "ar", 等)
     */
    fun setLanguageAndRestart(activity: Activity, languageCode: String) {
        android.util.Log.d("LanguageManager", "🌐 Switching language to: $languageCode (${SUPPORTED_LANGUAGES[languageCode]})")
        
        // 1. 保存语言配置到 SharedPreferences
        SPAppConfigs.setLocale(activity, languageCode)
        
        // 2. 重启Activity以应用新语言
        // Activity.recreate() 会：
        //   - 调用 onSaveInstanceState() 保存状态
        //   - 调用 onDestroy()
        //   - 调用 onCreate() 重新创建
        //   - 调用 onRestoreInstanceState() 恢复状态
        //   - BaseActivity.attachBaseContext() 会自动应用新语言
        activity.recreate()
        
        android.util.Log.d("LanguageManager", "✅ Activity recreated successfully")
    }
    
    /**
     * 🔍 检查语言代码是否被支持
     * 
     * @param languageCode 语言代码
     * @return true 如果支持，false 如果不支持
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return SUPPORTED_LANGUAGES.containsKey(languageCode)
    }
    
    /**
     * 📋 获取支持的语言代码列表
     * 
     * @return 语言代码列表 (如: ["en", "id", "ar", "ur", ...])
     */
    fun getSupportedLanguageCodes(): List<String> {
        return SUPPORTED_LANGUAGES.keys.toList()
    }
    
    /**
     * 📋 获取支持的语言显示名称列表
     * 
     * @return 语言显示名称数组 (如: ["English", "Bahasa Indonesia", ...])
     */
    fun getSupportedLanguageNames(): Array<String> {
        return SUPPORTED_LANGUAGES.values.toTypedArray()
    }
}

