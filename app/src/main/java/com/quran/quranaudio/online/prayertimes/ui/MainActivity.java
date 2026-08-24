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
import android.os.Handler;
import android.os.Looper;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * 优化后的 MainActivity
 * 
 * 🚀 性能优化策略：
 * 1. 延迟非关键初始化到 view.post()
 * 2. 后台线程执行 Tafsir 初始化
 * 3. 分阶段预加载（500ms 延迟）
 * 4. Lazy 初始化反馈系统
 * 5. UI 渲染优先，业务逻辑延后
 * 
 * 📊 目标：主线程阻塞时间 < 100ms
 */
@SuppressWarnings("deprecation")
public class MainActivity extends BaseActivity {

    /** Splash-only signal used to keep first-open analytics accurate after onboarding removal. */
    public static final String EXTRA_FIRST_LAUNCH = "extra_first_launch";
    public static final String EXTRA_PUSH_CAMPAIGN = "push_campaign";
    public static final String EXTRA_PUSH_TARGET = "push_target";

   /* ActivityResultLauncher<String[]> mPermissionResultLauncher;
    private boolean isLocationPermissionGranted = false;*/

    @Inject
    PreferencesHelper preferencesHelper;

    private PrayerDataPreloader prayerDataPreloader;
    private NavController navController;
    private BottomNavigationView navView;
    
    // 💬 反馈系统组件（Lazy 初始化）
    private FeedbackFloatingButton feedbackFloatingButton;
    private ExitInterceptor exitInterceptor;
    
    // 🚀 后台线程池（用于非 UI 操作）
    private static final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(2);
    
    // Handler for delayed tasks
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long startTime = System.currentTimeMillis();
        android.util.Log.d("PERFORMANCE", "========================================");
        android.util.Log.d("PERFORMANCE", "⚡ MainActivity.onCreate() START");
        android.util.Log.d("PERFORMANCE", "========================================");
        
        // ============================================================
        // 🟢 IMMEDIATE：主线程必须执行（< 100ms）
        // ============================================================
        
        // 语言配置更新（必须在 super.onCreate 前）
        forceUpdateApplicationLanguageOptimized();
        
        // Dagger 注入
        ((App) getApplicationContext())
                .defaultComponent
                .inject(this);

        // Inject PrayerDataPreloader
        prayerDataPreloader = ((App) getApplicationContext())
                .appComponent
                .homeComponent()
                .create()
                .getPrayerDataPreloader();

        // 🔔 为未配置的祈祷写入默认通知类型（提示音）。
        // 必须在任何 UI（PrayersFragment 图标直接读这份 prefs）和闹钟调度之前执行，
        // 保证"界面显示的开关状态"和"实际会响的闹钟"一致。幂等，重复调用无副作用。
        preferencesHelper.ensureDefaultPrayerNotificationTypes();

        super.onCreate(savedInstanceState);

        // 在 AppCompat 装配 subDecor 之前，先强制平台创建 decor / content 容器。
        //
        // 原因：AppCompatDelegateImpl.createSubDecor() 只有在
        // mWindow.findViewById(android.R.id.content) 非空时，才会把 android.R.id.content
        // 这个 id 挪到自己的 ContentFrameLayout 上。某些机型（传音系为主）冷启动时该容器
        // 尚未安装，id 迁移被跳过，随后 applyFixedSizeWindow() 里
        // mSubDecor.findViewById(android.R.id.content) 取到 null，调用
        // ContentFrameLayout.setDecorPadding() 时抛 NPE，导致 MainActivity 起不来。
        //
        // 位置很关键：必须在 super.onCreate() 之后。BaseActivity.onCreate() 会在它自己的
        // super.onCreate() 前调用 setTheme()，decor 一旦创建就按当时的主题定型；
        // 若提前到 super.onCreate() 之前强制创建，会用错主题渲染。
        // （SplashScreenActivity 直接继承 AppCompatActivity、主题走 manifest，
        //   所以那边放在 super.onCreate() 之前是安全的，不能照搬到这里。）
        getWindow().getDecorView();

        // UI 初始化（必须在主线程）
        setContentView(R.layout.activity_main);

        // 启动漏斗终点：第一个真正渲染出来的业务界面。
        //
        // rf_first_render 的用户数 ÷ rf_launch 的用户数 =「打开 App 的人里有多少真的看到了 App」。
        // 这个比值是留存诊断的第一指标：GA4 里 screen_view/session_start 只有 52.9%，
        // 但那是粗估且被后台进程污染，这一对事件给出的是干净的数。
        boolean isFirstLaunchEntry = getIntent().getBooleanExtra(EXTRA_FIRST_LAUNCH, false);
        com.quran.quranaudio.online.analytics.RetentionFunnel
                .firstRender(this, isFirstLaunchEntry ? "quran" : "main", isFirstLaunchEntry);

        navView = findViewById(R.id.nav_view);

        // 🕌 导航必须在首帧渲染前设置好起始页：布局已去掉 app:navGraph(不自动加载 nav_home)，
        // 这里同步设置图与起始页，避免"先闪祈祷主页再切到古兰经"。导航设置很轻量，不影响启动。
        setupNavigation();

        android.util.Log.d("PERFORMANCE", "✅ Immediate init completed [" + (System.currentTimeMillis() - startTime) + "ms]");

        // ============================================================
        // 🔵 POST-RENDER：UI 渲染后执行（不阻塞首帧）
        // ============================================================
        
        // 使用 view.post() 确保 UI 先渲染
        navView.post(() -> {
            long postRenderStart = System.currentTimeMillis();
            android.util.Log.d("PERFORMANCE", "→ [POST-RENDER] Starting deferred tasks...");
            
            // 设置统一状态栏
            setupUnifiedStatusBar();
            
            // 注册监听器
            registerQuizResultListener();

            // 📊 P0 埋点：记录通知状态用户属性，用于"通知开启率 vs 留存"分群分析
            reportNotificationUserProperties();

            android.util.Log.d("PERFORMANCE", "✅ [POST-RENDER] Deferred tasks completed [" + (System.currentTimeMillis() - postRenderStart) + "ms]");
            
            // ============================================================
            // 🟡 DELAYED：延迟执行（500ms+ 后）
            // ============================================================
            
            scheduleDelayedInitialization();
        });
        
        long totalTime = System.currentTimeMillis() - startTime;
        android.util.Log.d("PERFORMANCE", "========================================");
        android.util.Log.d("PERFORMANCE", "✅ MainActivity.onCreate() COMPLETED in " + totalTime + "ms (target: <100ms)");
        android.util.Log.d("PERFORMANCE", "========================================");
    }
    
    // ============================================================
    // 🟢 IMMEDIATE: 主线程必须执行
    // ============================================================
    
    /**
     * 优化版语言配置更新（减少日志，提升速度）
     */
    private void forceUpdateApplicationLanguageOptimized() {
        try {
            String language = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(this);
            
            if (language != null && !language.isEmpty()) {
                java.util.Locale locale = new java.util.Locale(language);
                java.util.Locale.setDefault(locale);
                
                android.content.res.Resources appResources = getApplicationContext().getResources();
                android.content.res.Configuration appConfig = appResources.getConfiguration();
                appConfig.setLocale(locale);
                appResources.updateConfiguration(appConfig, appResources.getDisplayMetrics());
            }
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ Language update failed (non-fatal)", e);
        }
    }
    
    // ============================================================
    // 🔵 POST-RENDER: UI 渲染后执行
    // ============================================================
    
    /**
     * 设置 Navigation（延迟到 UI 渲染后）
     */
    private void setupNavigation() {
        try {
            // 用 NavHostFragment 直接取 NavController：在 onCreate 早期同步调用时可靠，
            // 而 Navigation.findNavController 此时可能尚未就绪。
            androidx.navigation.fragment.NavHostFragment navHostFragment =
                    (androidx.navigation.fragment.NavHostFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.home_host_fragment);
            if (navHostFragment == null) {
                android.util.Log.e("PERFORMANCE", "❌ NavHostFragment not found");
                return;
            }
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(navView, navController);
            
            // Navigation item selection listener
            navView.setOnItemSelectedListener(item -> {
                boolean handled = NavigationUI.onNavDestinationSelected(item, navController);
                
                if (!handled) {
                    try {
                        navController.navigate(item.getItemId());
                        return true;
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Navigation failed", e);
                        return false;
                    }
                }
                
                return handled;
            });

            NavGraph navGraph = navController.getNavInflater().inflate(R.navigation.nav_graphmain);

            // Set start destination
            if (displaySettingsScreenFirst()) {
                navGraph.setStartDestination(R.id.navigation_settings);
            } else if (shouldLandOnQuranFirst()) {
                // 🕌 新用户首次进入落古兰经索引页，兑现"古兰经"App 的核心承诺、修正身份错乱
                // (此前落祈祷仪表盘，Quran 埋在第三 tab)。仅一次，之后正常落祈祷首页(日常钩子)。
                navGraph.setStartDestination(R.id.nav_quran);
            } else {
                navGraph.setStartDestination(R.id.nav_home);
            }

            navController.setGraph(navGraph);
            handlePushIntent(getIntent());
            
            // 🔥 使用 apply() 而不是隐式的 commit()
            preferencesHelper.setFirstTimeLaunch(false);
            
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ Navigation setup failed", e);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handlePushIntent(intent);
    }

    private void handlePushIntent(Intent intent) {
        if (intent == null || navController == null) return;
        String campaign = intent.getStringExtra(EXTRA_PUSH_CAMPAIGN);
        String target = intent.getStringExtra(EXTRA_PUSH_TARGET);
        if (campaign == null || target == null) return;

        // Consume once so configuration changes cannot double-count the open.
        intent.removeExtra(EXTRA_PUSH_CAMPAIGN);
        intent.removeExtra(EXTRA_PUSH_TARGET);
        com.quran.quranaudio.online.analytics.RetentionFunnel
                .push(this, "opened", campaign, target, "notification_tap");
        try {
            if ("subscription".equals(target)) {
                com.quran.quranaudio.online.subscription.SubscriptionHelper.INSTANCE
                        .launchSubscriptionPage(this, "push_" + campaign);
            } else if ("tasbih".equals(target)) {
                navController.navigate(R.id.nav_tasbih);
            } else if ("prayer".equals(target)) {
                navController.navigate(R.id.nav_home);
            } else {
                navController.navigate(R.id.nav_quran);
            }
        } catch (Exception e) {
            android.util.Log.w("MainActivity", "Push target navigation failed: " + target, e);
        }
    }
    
    /**
     * 设置统一的白色状态栏
     */
    private void setupUnifiedStatusBar() {
        try {
            Window window = getWindow();
            View decorView = window.getDecorView();
            
            WindowCompat.setDecorFitsSystemWindows(window, true);
            window.setStatusBarColor(0xFFFFFFFF);
            
            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
            wic.setAppearanceLightStatusBars(true);
            
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ Status bar setup failed (non-fatal)", e);
        }
    }
    
    /**
     * 📊 记录通知相关用户属性，供 Firebase 留存报告分群交叉分析：
     * - notif_os_permission：系统级通知权限是否授予(true/false)
     * - notif_prayer_enabled：是否至少一番祈祷会实际发通知(true/false)
     * 每次会话更新，反映当前真实召回状态。
     */
    private void reportNotificationUserProperties() {
        try {
            boolean osGranted = true;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                osGranted = ContextCompat.checkSelfPermission(this,
                        android.Manifest.permission.POST_NOTIFICATIONS)
                        == android.content.pm.PackageManager.PERMISSION_GRANTED;
            }
            boolean prayerEnabled = preferencesHelper != null
                    && preferencesHelper.hasAnyPrayerNotificationEnabled();

            com.quran.quranaudio.online.analytics.AnalyticsManager am =
                    com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this);
            am.setUserProperty("notif_os_permission", String.valueOf(osGranted));
            am.setUserProperty("notif_prayer_enabled", String.valueOf(prayerEnabled));

            // 补齐留存分层维度：是否收到过邦克、是否走完引导、机型档位、安装天数。
            // 此前全 App 只有上面两个用户属性，导致 GA4 的留存报告无法按人群拆分——
            // 「收到过邦克 vs 没收到过」的 D1/D7 留存对比（留存诊断第一优先项）跑不了。
            com.quran.quranaudio.online.analytics.RetentionFunnel.syncUserProps(this, osGranted);

            // 用户看到了礼拜时间 = 本 App 的首个价值时刻，只在首次发生时上报
            com.quran.quranaudio.online.analytics.RetentionFunnel.firstValue(this, "prayer_times");
        } catch (Exception e) {
            android.util.Log.w("MainActivity", "reportNotificationUserProperties failed", e);
        }
    }

    /**
     * 注册 RxBus 监听器
     */
    private void registerQuizResultListener() {
        try {
            RxBus.INSTANCE().register(this, MainTabChangeEvent.class, event -> {
                if (MainTabChangeEvent.TO_QUIZ.equals(event.toType)) {
                    if (navController != null && navView != null) {
                        try {
                            navController.navigate(R.id.nav_name_99);
                            navView.setSelectedItemId(R.id.nav_name_99);
                        } catch (Exception e) {
                            android.util.Log.e("MainActivity", "❌ Navigation to Discover failed", e);
                        }
                    }
                }
            });
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ Quiz listener registration failed", e);
        }
    }
    
    // ============================================================
    // 🟡 DELAYED: 延迟执行（500ms+ 后）
    // ============================================================
    
    /**
     * 调度延迟初始化任务
     */
    private void scheduleDelayedInitialization() {
        android.util.Log.d("PERFORMANCE", "🟡 [DELAY] Scheduling delayed tasks...");
        
        // 延迟 500ms：WorkManager 调度（避开 UI 渲染高峰）
        mainHandler.postDelayed(() -> {
            android.util.Log.d("PERFORMANCE", "→ [DELAY-500ms] WorkManager scheduling...");
            scheduleWorkManagerTasks();
        }, 500);
        
        // 延迟 500ms：预加载祷告数据（后台线程）
        mainHandler.postDelayed(() -> {
            android.util.Log.d("PERFORMANCE", "→ [DELAY-500ms] Prayer data preloading...");
            preloadPrayerDataAsync();
        }, 500);
        
        // 延迟 1000ms：Tafsir 初始化（完全后台）
        mainHandler.postDelayed(() -> {
            android.util.Log.d("PERFORMANCE", "→ [DELAY-1s] Tafsir initialization...");
            initializeDefaultTafsirAsync();
        }, 1000);
        
        // 延迟 2000ms：反馈系统初始化
        mainHandler.postDelayed(() -> {
            android.util.Log.d("PERFORMANCE", "→ [DELAY-2s] Feedback system init...");
            initFeedbackSystemLazy();
        }, 2000);
        
        android.util.Log.d("PERFORMANCE", "✅ [DELAY] 4 delayed tasks scheduled (500ms, 1s, 2s)");
    }
    
    /**
     * 调度 WorkManager 任务（后台线程）
     */
    private void scheduleWorkManagerTasks() {
        backgroundExecutor.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                WorkCreator.schedulePeriodicPrayerUpdater(this);
                android.util.Log.d("PERFORMANCE", "✅ [DELAY-500ms] WorkManager scheduled [" + (System.currentTimeMillis() - startTime) + "ms]");
            } catch (Exception e) {
                android.util.Log.e("PERFORMANCE", "❌ [DELAY-500ms] WorkManager scheduling failed", e);
            }
        });
    }
    
    /**
     * 异步预加载祷告数据（后台线程）
     */
    private void preloadPrayerDataAsync() {
        backgroundExecutor.execute(() -> {
            try {
                if (prayerDataPreloader != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    long startTime = System.currentTimeMillis();
                    prayerDataPreloader.preloadPrayerData(this);
                    android.util.Log.d("PERFORMANCE", "✅ [DELAY-500ms] Prayer data preloaded [" + (System.currentTimeMillis() - startTime) + "ms]");
                }
            } catch (Exception e) {
                android.util.Log.e("PERFORMANCE", "❌ [DELAY-500ms] Prayer data preload failed", e);
            }
        });
    }
    
    /**
     * 异步初始化默认 Tafsir（完全后台）
     */
    private void initializeDefaultTafsirAsync() {
        backgroundExecutor.execute(() -> {
            try {
                long startTime = System.currentTimeMillis();
                
                // 检查是否已有保存的 Tafsir key
                String savedTafsirKey = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.getSavedTafsirKey(this);
                
                if (savedTafsirKey != null && !savedTafsirKey.isEmpty()) {
                    android.util.Log.d("PERFORMANCE", "✅ [DELAY-1s] Tafsir already initialized: " + savedTafsirKey);
                    return;
                }
                
                android.util.Log.d("PERFORMANCE", "→ [DELAY-1s] Initializing default Tafsir...");
                
                // 异步准备 Tafsir 列表
                com.quran.quranaudio.online.quran_module.utils.reader.tafsir.TafsirManager.prepare(this, false, new kotlin.jvm.functions.Function0<kotlin.Unit>() {
                    @Override
                    public kotlin.Unit invoke() {
                        // 获取用户语言
                        String userLanguage = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(MainActivity.this);
                        String targetLanguage = (userLanguage != null && !userLanguage.isEmpty()) 
                            ? userLanguage 
                            : java.util.Locale.getDefault().getLanguage();
                        
                        // 获取所有可用的 Tafsir 模型
                        java.util.Map<String, java.util.List<com.quran.quranaudio.online.quran_module.api.models.tafsir.TafsirInfoModel>> tafsirModels = 
                            com.quran.quranaudio.online.quran_module.utils.reader.tafsir.TafsirManager.getModels();
                        
                        if (tafsirModels != null && !tafsirModels.isEmpty()) {
                            String selectedKey = com.quran.quranaudio.online.quran_module.utils.tafsir.TafsirLanguageMapper.INSTANCE.pickBestTafsirKey(
                                targetLanguage, 
                                tafsirModels
                            );
                            
                            if (selectedKey != null) {
                                com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPReader.setSavedTafsirKey(MainActivity.this, selectedKey);
                                android.util.Log.d("PERFORMANCE", "✅ [DELAY-1s] Tafsir initialized: " + selectedKey + " [" + (System.currentTimeMillis() - startTime) + "ms]");
                            }
                        }
                        
                        return kotlin.Unit.INSTANCE;
                    }
                });
                
            } catch (Exception e) {
                android.util.Log.e("PERFORMANCE", "❌ [DELAY-1s] Tafsir initialization failed", e);
            }
        });
    }
    
    /**
     * Lazy 初始化反馈系统
     */
    private void initFeedbackSystemLazy() {
        try {
            // Set current page
            FeedbackManager.Companion.getInstance().setCurrentPage("MainActivity");
            
            // 延迟 3 秒显示浮动按钮（总计 5 秒：2s 初始延迟 + 3s 额外延迟）
            mainHandler.postDelayed(() -> {
                try {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    
                    if (getWindow() == null || getWindow().getDecorView().getWindowToken() == null) {
                        return;
                    }
                    
                    long startTime = System.currentTimeMillis();
                    feedbackFloatingButton = new FeedbackFloatingButton(this);
                    feedbackFloatingButton.show();
                    android.util.Log.d("PERFORMANCE", "✅ [DELAY-5s] Feedback button shown [" + (System.currentTimeMillis() - startTime) + "ms]");
                    
                } catch (Exception e) {
                    android.util.Log.e("PERFORMANCE", "❌ [DELAY-5s] Feedback button failed", e);
                }
            }, 3000); // 额外 3 秒
            
            // 初始化退出拦截器
            exitInterceptor = new ExitInterceptor(this);
            
            android.util.Log.d("PERFORMANCE", "✅ [DELAY-2s] Feedback system initialized");
            
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ [DELAY-2s] Feedback system init failed", e);
        }
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private boolean displaySettingsScreenFirst() {
        return false;
    }

    /**
     * 是否让本次启动落在古兰经索引页(仅新用户首次)。
     *
     * 逻辑：用专用标志位，只对"从未落过 Quran-first"的用户生效一次；
     * 已有阅读记录或已保存国家的老用户视为已建立习惯，跳过，避免打扰其祈祷日常动线。
     */
    private boolean shouldLandOnQuranFirst() {
        try {
            android.content.SharedPreferences sp =
                    getSharedPreferences("QURAN_FIRST_LANDING", MODE_PRIVATE);
            if (sp.getBoolean("shown", false)) {
                return false;
            }
            sp.edit().putBoolean("shown", true).apply();

            // 老用户(已有阅读记录)不打扰，只对真正的新用户兑现 Quran-first
            int lastSurah = com.quran.quranaudio.online.features.Helper.LastSurahAndAyahHelper
                    .getLastSurah(this);
            return lastSurah <= 0;
        } catch (Exception e) {
            return false;
        }
    }

    public void onBackPressed() {
        if (exitInterceptor != null && exitInterceptor.onBackPressed()) {
            return;
        }
        finish();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 清理 Handler 回调
        mainHandler.removeCallbacksAndMessages(null);
        
        // 清理反馈按钮
        if (feedbackFloatingButton != null) {
            feedbackFloatingButton.destroy();
        }
    }
}
