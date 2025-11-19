package com.quran.quranaudio.online.ads.application;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;

import androidx.multidex.MultiDex;

import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs;
import com.quran.quranaudio.online.quran_module.utils.TranslationCacheManager;

import java.util.Locale;

@SuppressWarnings("ConstantConditions")
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // 🌐 应用启动时立即应用保存的语言配置
        applyLanguageConfiguration();
        android.util.Log.d("MyApplication", "🚀 Application.onCreate() called");
        
        // 🔄 同步语言设置（如果语言改变，清除翻译和 Tafsir 缓存）
        com.quran.quranaudio.online.quran_module.utils.LanguageSyncHelper.INSTANCE.syncLanguageSettings(this);
        android.util.Log.d("MyApplication", "🔄 Language sync check completed");
        
        // 📦 预加载所有语言的古兰经翻译版本（后台异步）
        TranslationCacheManager.INSTANCE.preloadAllTranslations(this);
        android.util.Log.d("MyApplication", "📦 Translation cache preloading started");
    }
    
    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 🌐 配置变化时重新应用语言（确保切换后立即生效）
        applyLanguageConfiguration();
        android.util.Log.d("MyApplication", "🔄 Configuration changed, language reapplied");
    }

    @Override
    protected void attachBaseContext(Context base) {
        // 🌐 在 attachBaseContext 时就应用语言配置
        Context context = updateBaseContextLocale(base);
        super.attachBaseContext(context);
        MultiDex.install(this);
    }

    /**
     * 🌐 更新 Application Context 的语言配置
     */
    private Context updateBaseContextLocale(Context context) {
        String language = SPAppConfigs.getLocale(context);
        
        if (language == null || language.isEmpty()) {
            return context;
        }
        
        // 🔄 资源目录映射：应用使用 "id"，但 Android 资源使用 "in"
        String resourceLanguage = "id".equals(language) ? "in" : language;
        Locale locale = new Locale(resourceLanguage);
        Locale.setDefault(locale);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return updateResourcesLocale(context, locale);
        }
        return updateResourcesLocaleLegacy(context, locale);
    }
    
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
    
    /**
     * 🌐 在 Application onCreate 时应用语言配置
     * 确保 Application 的 Resources 使用正确的语言
     */
    private void applyLanguageConfiguration() {
        String language = SPAppConfigs.getLocale(this);
        
        android.util.Log.d("MyApplication", "🔍 applyLanguageConfiguration() called, language from SPAppConfigs: " + language);
        
        if (language == null || language.isEmpty()) {
            android.util.Log.d("MyApplication", "⚠️ Language is null or empty, skipping");
            return;
        }
        
        // 🔄 资源目录映射：应用使用 "id"，但 Android 资源使用 "in"
        String resourceLanguage = "id".equals(language) ? "in" : language;
        Locale locale = new Locale(resourceLanguage);
        Locale.setDefault(locale);
        
        android.util.Log.d("MyApplication", "📍 Language mapping: app='" + language + "' → resource='" + resourceLanguage + "'");
        
        android.util.Log.d("MyApplication", "✅ Locale.setDefault() called: " + locale);
        
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        
        // 记录当前配置
        android.util.Log.d("MyApplication", "📊 Current locale before update: " + configuration.locale);
        
        configuration.setLocale(locale);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            createConfigurationContext(configuration);
            android.util.Log.d("MyApplication", "📱 createConfigurationContext() called (Android N+)");
        }
        
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
        
        android.util.Log.d("MyApplication", "🌐 Language applied at Application level: " + language);
        android.util.Log.d("MyApplication", "📊 New locale after update: " + resources.getConfiguration().locale);
    }
}
