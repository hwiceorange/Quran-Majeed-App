package com.quran.quranaudio.online.quran_module.utils

import android.content.Context
import com.quran.quranaudio.online.quran_module.utils.reader.TranslUtils
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader
import com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirUtils

/**
 * 语言同步助手
 * 
 * 确保翻译和 Tafsir 设置与应用语言保持同步
 * 当用户切换语言时，自动清除旧的翻译和 Tafsir 设置，
 * 让它们重新初始化为新语言的默认值
 */
object LanguageSyncHelper {
    
    private const val TAG = "LanguageSyncHelper"
    private const val PREF_NAME = "language_sync"
    private const val KEY_LAST_LANGUAGE = "last_language"
    
    /**
     * 检查语言是否发生变化，如果变化则清除翻译和 Tafsir 设置
     * 
     * 应该在 Application.onCreate() 或 BaseActivity.onCreate() 中调用
     * 
     * @param context Android Context
     */
    fun syncLanguageSettings(context: Context) {
        val currentLanguage = SPAppConfigs.getLocale(context)
        val lastLanguage = getLastLanguage(context)
        
        android.util.Log.d(TAG, "🔍 Checking language sync: last='$lastLanguage', current='$currentLanguage'")
        
        // 如果语言发生变化
        if (lastLanguage != null && lastLanguage != currentLanguage) {
            android.util.Log.d(TAG, "🌐 Language changed from '$lastLanguage' to '$currentLanguage'")
            android.util.Log.d(TAG, "🧹 Clearing old translation and Tafsir settings...")
            
            // 清除保存的翻译设置
            clearSavedTranslations(context)
            
            // 清除保存的 Tafsir 设置
            clearSavedTafsir(context)
            
            android.util.Log.d(TAG, "✅ Settings cleared. They will be re-initialized with new language defaults.")
        } else if (lastLanguage == null) {
            android.util.Log.d(TAG, "ℹ️ First run or language not tracked yet")
        } else {
            android.util.Log.d(TAG, "✅ Language unchanged, no sync needed")
        }
        
        // 更新记录的语言
        saveLastLanguage(context, currentLanguage)
    }
    
    /**
     * 清除保存的翻译设置
     */
    private fun clearSavedTranslations(context: Context) {
        try {
            val sp = context.getSharedPreferences(SPReader.SP_TRANSL, Context.MODE_PRIVATE)
            sp.edit().remove(TranslUtils.KEY_TRANSLATIONS).apply()
            android.util.Log.d(TAG, "🗑️ Cleared saved translations")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error clearing translations", e)
        }
    }
    
    /**
     * 清除保存的 Tafsir 设置
     */
    private fun clearSavedTafsir(context: Context) {
        try {
            val sp = context.getSharedPreferences(SPReader.SP_TAFSIR, Context.MODE_PRIVATE)
            sp.edit().remove(TafsirUtils.KEY_TAFSIR).apply()
            android.util.Log.d(TAG, "🗑️ Cleared saved Tafsir")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "❌ Error clearing Tafsir", e)
        }
    }
    
    /**
     * 获取上次记录的语言
     */
    private fun getLastLanguage(context: Context): String? {
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sp.getString(KEY_LAST_LANGUAGE, null)
    }
    
    /**
     * 保存当前语言
     */
    private fun saveLastLanguage(context: Context, language: String?) {
        if (language.isNullOrEmpty()) return
        
        val sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sp.edit().putString(KEY_LAST_LANGUAGE, language).apply()
        android.util.Log.d(TAG, "💾 Saved last language: $language")
    }
}

