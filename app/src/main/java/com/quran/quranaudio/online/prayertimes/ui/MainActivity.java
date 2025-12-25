package com.quran.quranaudio.online.prayertimes.ui;

import android.Manifest;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.Utils.GifImageView;
import com.quran.quranaudio.online.prayertimes.job.WorkCreator;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.prayertimes.ui.home.HomeViewModel;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.quiz.utils.RxBus;
import com.quran.quranaudio.quiz.base.MainTabChangeEvent;
import com.quran.quranaudio.online.feedback.FeedbackFloatingButton;
import com.quran.quranaudio.online.feedback.FeedbackManager;
import com.quran.quranaudio.online.feedback.ExitInterceptor;

import javax.inject.Inject;

import androidx.lifecycle.ViewModelProvider;


@SuppressWarnings("deprecation")
public class MainActivity extends BaseActivity {

   /* ActivityResultLauncher<String[]> mPermissionResultLauncher;
    private boolean isLocationPermissionGranted = false;*/

    @Inject
    PreferencesHelper preferencesHelper;

    private PrayerDataPreloader prayerDataPreloader;
    private NavController navController;
    private BottomNavigationView navView;
    
    // 💬 反馈系统组件
    private FeedbackFloatingButton feedbackFloatingButton;
    private ExitInterceptor exitInterceptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        android.util.Log.d("NATIVE_AD_TRACK", "🎯 MainActivity.onCreate() START");
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        
        // 🌐 强制更新 Application Resources 的语言配置
        // 因为 Application 实例不会重新创建，必须手动更新
        forceUpdateApplicationLanguage();
        
        ((App) getApplicationContext())
                .defaultComponent
                .inject(this);

        // Inject PrayerDataPreloader from HomeComponent
        prayerDataPreloader = ((App) getApplicationContext())
                .appComponent
                .homeComponent()
                .create()
                .getPrayerDataPreloader();

        super.onCreate(savedInstanceState);
        
        // 🔧 自动初始化 Tafsir：在首次启动或引导完成后，根据应用语言自动选择默认 Tafsir
        initializeDefaultTafsirIfNeeded();

        setContentView(R.layout.activity_main);
        navView = findViewById(R.id.nav_view);

     /*   //PermissionStart

        mPermissionResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                if (result.get(Manifest.permission.ACCESS_FINE_LOCATION) !=null) {
                    isLocationPermissionGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }
        });

        //Permission End*/

     //   requestPermission();


        navController = Navigation.findNavController(this, R.id.home_host_fragment);
        android.util.Log.d("NATIVE_AD_TRACK", "→ NavController found: " + navController);
        android.util.Log.d("NATIVE_AD_TRACK", "→ Current destination: " + navController.getCurrentDestination());
        
        NavigationUI.setupWithNavController(navView, navController);
        android.util.Log.d("NATIVE_AD_TRACK", "✅ NavigationUI setup completed");
        
        // 设置统一的白色状态栏 + 深色图标（所有页面统一效果）
        setupUnifiedStatusBar();
        android.util.Log.e("MainActivity", "✅ 初始化：设置统一白色状态栏");
        
        // Add navigation item selection listener with logging
        navView.setOnItemSelectedListener(item -> {
            android.util.Log.d("MainActivity", "Bottom nav item clicked: " + item.getTitle() + " (ID: " + item.getItemId() + ")");
            
            // Let NavigationUI handle all navigation items (including Quran)
            boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
            
            if (handled) {
                android.util.Log.d("MainActivity", "Navigation handled by NavigationUI");
            } else {
                android.util.Log.w("MainActivity", "Navigation NOT handled by NavigationUI, trying manual navigation");
                // Fallback: manually navigate
                try {
                    navController.navigate(item.getItemId());
                    android.util.Log.d("MainActivity", "Manual navigation successful");
                    return true;
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Manual navigation failed", e);
                    return false;
                }
            }
            
            return handled;
        });

        NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graphmain);

        // Set correct start destination: Home page for normal launch
        if (displaySettingsScreenFirst()) {
            android.util.Log.d("NATIVE_AD_TRACK", "→ Setting start destination: SETTINGS");
            navGraph.setStartDestination(R.id.navigation_settings);
        } else {
            android.util.Log.d("NATIVE_AD_TRACK", "→ Setting start destination: HOME (R.id.nav_home)");
            navGraph.setStartDestination(R.id.nav_home);  // Fixed: Start at Home page, not Learn page
        }

        navController.setGraph(navGraph);
        android.util.Log.d("NATIVE_AD_TRACK", "✅ NavGraph set, current destination: " + navController.getCurrentDestination());
        if (navController.getCurrentDestination() != null) {
            android.util.Log.d("NATIVE_AD_TRACK", "   Destination label: " + navController.getCurrentDestination().getLabel());
            android.util.Log.d("NATIVE_AD_TRACK", "   Destination ID: " + navController.getCurrentDestination().getId());
        }
        
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        android.util.Log.d("NATIVE_AD_TRACK", "✅ MainActivity.onCreate() COMPLETED");
        android.util.Log.d("NATIVE_AD_TRACK", "═══════════════════════════════════════════════");
        preferencesHelper.setFirstTimeLaunch(false);

        WorkCreator.schedulePeriodicPrayerUpdater(this);

        // Preload HomeViewModel at app startup to fetch prayer data in background
        // This ensures data is ready when user navigates to Home page
        preloadPrayerData();
        
        // Register RxBus listener for MainTabChangeEvent (from quiz result page)
        registerQuizResultListener();
        
        // 💬 Initialize feedback system
        initFeedbackSystem();
    }

    /**
     * Preload prayer data at app startup for faster Home page display
     * Delegates to PrayerDataPreloader which creates HomeViewModel in background
     */
    private void preloadPrayerData() {
        if (prayerDataPreloader != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            prayerDataPreloader.preloadPrayerData(this);
        }
    }

    /**
     * Register RxBus listener to handle navigation from quiz result page to Discover tab
     */
    private void registerQuizResultListener() {
        RxBus.INSTANCE().register(this, MainTabChangeEvent.class, event -> {
            if (MainTabChangeEvent.TO_QUIZ.equals(event.toType)) {
                android.util.Log.d("MainActivity", "📱 Received MainTabChangeEvent.TO_QUIZ - navigating to Discover tab");
                
                // Navigate to Discover tab (nav_name_99)
                if (navController != null && navView != null) {
                    try {
                        navController.navigate(R.id.nav_name_99);
                        navView.setSelectedItemId(R.id.nav_name_99);
                        android.util.Log.d("MainActivity", "✅ Successfully navigated to Discover tab");
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "❌ Failed to navigate to Discover tab", e);
                    }
                }
            }
        });
        android.util.Log.d("MainActivity", "✅ Quiz result listener registered");
    }
/*
    private void requestPermission(){
        isLocationPermissionGranted = ContextCompat.checkSelfPermission(
                this,
              Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;

        List<String> permissionRequest = new ArrayList<String>();

        if (!isLocationPermissionGranted) {
            permissionRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (!permissionRequest.isEmpty()) {

            mPermissionResultLauncher.launch(permissionRequest.toArray(new String[0]));
        }
    }
*/

    private boolean displaySettingsScreenFirst() {
        // Always start at Home page for new users
        // The Welcome dialog will guide them to grant location permission
        // Only show Settings first if explicitly needed (currently never)
        return false;
    }
    
    /**
     * 设置统一的白色状态栏（所有页面统一效果）
     * 白色背景 + 深色图标 + 内容不延伸到状态栏下方
     */
    private void setupUnifiedStatusBar() {
        try {
            Window window = getWindow();
            View decorView = window.getDecorView();
            
            // 确保内容不延伸到状态栏下方（非沉浸式）
            WindowCompat.setDecorFitsSystemWindows(window, true);
            
            // 设置状态栏为白色
            window.setStatusBarColor(0xFFFFFFFF);
            
            // 设置图标为深色（lightStatusBar = true 表示浅色背景需要深色图标）
            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
            wic.setAppearanceLightStatusBars(true);
            
            android.util.Log.e("MainActivity", "✅ 统一状态栏设置: 白色背景 + 深色图标");
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ 设置统一状态栏失败", e);
        }
    }

    public void onBackPressed() {
        // Exit interceptor logic
        if (exitInterceptor != null && exitInterceptor.onBackPressed()) {
            // Intercepted, do not perform default back action
            android.util.Log.d("MainActivity", "⚠️ Back press intercepted by ExitInterceptor");
            return;
        }
        
        // Allow, perform default back action (finish app)
        finish();
    }
    
    /**
     * 🌐 强制更新 Application 级别的语言配置
     * 
     * 原因：Application 实例在 Activity 切换时不会重新创建，
     * 所以必须手动更新 Application 的 Resources
     */
    private void forceUpdateApplicationLanguage() {
        try {
            String language = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(this);
            
            android.util.Log.d("MainActivity", "🔍 forceUpdateApplicationLanguage() - Language from SPAppConfigs: " + language);
            
            if (language == null || language.isEmpty()) {
                android.util.Log.d("MainActivity", "⚠️ Language is null or empty");
                return;
            }
            
            java.util.Locale locale = new java.util.Locale(language);
            java.util.Locale.setDefault(locale);
            
            // 更新 Application 的 Resources
            android.content.res.Resources appResources = getApplicationContext().getResources();
            android.content.res.Configuration appConfig = appResources.getConfiguration();
            
            android.util.Log.d("MainActivity", "📊 Application locale before: " + appConfig.getLocales().get(0));
            
            appConfig.setLocale(locale);
            appResources.updateConfiguration(appConfig, appResources.getDisplayMetrics());
            
            android.util.Log.d("MainActivity", "✅ Application Resources updated to: " + language);
            android.util.Log.d("MainActivity", "📊 Application locale after: " + appResources.getConfiguration().getLocales().get(0));
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Failed to update application language", e);
        }
    }
    
    /**
     * 🔧 自动初始化默认 Tafsir
     * 
     * 在首次启动或引导完成后，根据用户设置的应用语言自动选择并保存默认 Tafsir，
     * 避免用户点击注释时弹出 "Tafsir Not Available" 对话框
     * 
     * 优先级：
     * 1. 如果已有保存的 Tafsir key，则跳过
     * 2. 根据应用语言自动选择最佳 Tafsir
     * 3. 保存到 SharedPreferences
     */
    private void initializeDefaultTafsirIfNeeded() {
        try {
            // 检查是否已有保存的 Tafsir key
            String savedTafsirKey = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTafsirKey(this);
            
            if (savedTafsirKey != null && !savedTafsirKey.isEmpty()) {
                android.util.Log.d("MainActivity", "✅ Tafsir already initialized: " + savedTafsirKey);
                return;
            }
            
            android.util.Log.d("MainActivity", "🔧 No Tafsir selected, initializing default Tafsir...");
            
            // 异步准备 Tafsir 列表并选择默认值
            com.quran.quranaudio.online.quran_module.utils.reader.tafsir.TafsirManager.prepare(this, false, new kotlin.jvm.functions.Function0<kotlin.Unit>() {
                @Override
                public kotlin.Unit invoke() {
                    // 获取用户设置的语言
                    String userLanguage = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(MainActivity.this);
                    String systemLanguage = java.util.Locale.getDefault().getLanguage();
                    String targetLanguage;
                    if (userLanguage != null && !userLanguage.isEmpty()) {
                        targetLanguage = userLanguage;
                    } else {
                        targetLanguage = systemLanguage;
                    }
                    
                    android.util.Log.d("MainActivity", "🌍 Target language for Tafsir: " + targetLanguage);
                    
                    // 获取所有可用的 Tafsir 模型
                    java.util.Map<String, java.util.List<com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirInfoModel>> tafsirModels = 
                        com.quran.quranaudio.online.quran_module.utils.reader.tafsir.TafsirManager.getModels();
                    
                    if (tafsirModels != null && !tafsirModels.isEmpty()) {
                        // 根据语言选择最佳 Tafsir（使用 INSTANCE 访问 Kotlin object）
                        String selectedKey = com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirLanguageMapper.INSTANCE.pickBestTafsirKey(
                            targetLanguage, 
                            tafsirModels
                        );
                        
                        if (selectedKey != null) {
                            // 保存选择的 Tafsir key
                            com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.setSavedTafsirKey(MainActivity.this, selectedKey);
                            android.util.Log.d("MainActivity", "✅ Auto-selected and saved Tafsir: " + selectedKey + " for language: " + targetLanguage);
                        } else {
                            android.util.Log.w("MainActivity", "⚠️ No suitable Tafsir found for language: " + targetLanguage);
                        }
                    } else {
                        android.util.Log.w("MainActivity", "⚠️ No Tafsir models available");
                    }
                    
                    return kotlin.Unit.INSTANCE;
                }
            });
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Failed to initialize default Tafsir", e);
        }
    }
    
    /**
     * 💬 Initialize feedback system
     */
    private void initFeedbackSystem() {
        try {
            android.util.Log.d("MainActivity", "💬 Initializing feedback system...");
            
            // Set current page name (for feedback data collection)
            // Note: Use Companion.getInstance() to access Kotlin companion object from Java
            FeedbackManager.Companion.getInstance().setCurrentPage("MainActivity");
            
            // Initialize floating feedback button (delayed 3 seconds to avoid startup interference)
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                feedbackFloatingButton = new FeedbackFloatingButton(this);
                feedbackFloatingButton.show();
                android.util.Log.d("MainActivity", "✅ Feedback floating button shown");
            }, 3000);
            
            // Initialize exit interceptor
            exitInterceptor = new ExitInterceptor(this);
            
            android.util.Log.d("MainActivity", "✅ Feedback system initialized");
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "❌ Failed to initialize feedback system", e);
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Cleanup feedback floating button
        if (feedbackFloatingButton != null) {
            feedbackFloatingButton.destroy();
            android.util.Log.d("MainActivity", "✅ Feedback floating button destroyed");
        }
    }

}
