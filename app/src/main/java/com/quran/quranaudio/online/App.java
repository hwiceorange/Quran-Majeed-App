package com.quran.quranaudio.online;

import static com.raiadnan.ads.sdk.util.Constant.ADMOB;
import static com.raiadnan.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.raiadnan.ads.sdk.util.Constant.APPLOVIN;
import static com.raiadnan.ads.sdk.util.Constant.APPLOVIN_MAX;
import static com.raiadnan.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.work.Configuration;
import androidx.work.WorkManager;

import com.quran.quranaudio.quiz.base.BaseApp;
import com.quranaudio.common.ad.AdFactory;
import com.raiadnan.ads.sdk.format.AppOpenAdAppLovin;
import com.raiadnan.ads.sdk.format.AppOpenAdManager;
import com.raiadnan.ads.sdk.format.AppOpenAdMob;
import com.raiadnan.ads.sdk.util.OnShowAdCompleteListener;
import com.quran.quranaudio.online.ads.data.Constant;
import com.quran.quranaudio.online.prayertimes.di.module.AppModule;
import com.quran.quranaudio.online.prayertimes.common.api.TLSSocketFactoryCompat;
import com.quran.quranaudio.online.prayertimes.di.component.AdapterComponent;
import com.quran.quranaudio.online.prayertimes.di.component.ApplicationComponent;
import com.quran.quranaudio.online.prayertimes.di.component.DaggerAdapterComponent;
import com.quran.quranaudio.online.prayertimes.di.component.DaggerApplicationComponent;
import com.quran.quranaudio.online.prayertimes.di.component.DaggerDefaultComponent;
import com.quran.quranaudio.online.prayertimes.di.component.DaggerReceiverComponent;
import com.quran.quranaudio.online.prayertimes.di.component.DefaultComponent;
import com.quran.quranaudio.online.prayertimes.di.component.ReceiverComponent;
import com.quran.quranaudio.online.prayertimes.di.factory.worker.WorkerProviderFactory;
import com.quran.quranaudio.online.quran_module.utils.app.NotificationUtils;
import com.quran.quranaudio.online.quran_module.utils.app.ThemeUtils;
import com.quran.quranaudio.online.quran_module.utils.univ.FileUtils;

import org.jetbrains.annotations.NotNull;

import cat.ereza.customactivityoncrash.config.CaocConfig;
import kotlin.jvm.JvmField;
import kotlin.jvm.internal.Intrinsics;


public class App extends BaseApp {

    //Ads
    private AppOpenAdMob appOpenAdMob;
    private AppOpenAdManager appOpenAdManager;
    private AppOpenAdAppLovin appOpenAdAppLovin;
    Activity currentActivity;


    //Ads*
    private static App app;
    public Typeface faceArabic;
    public Typeface faceRobotoB;
    public Typeface faceRobotoL;
    public Typeface faceRobotoR;



    public interface SimpleCallback {
        void callback(Object obj);
    }

    public static App get() {
        return app;
    }

    @JvmField
    @SuppressWarnings("deprecation")
    public ApplicationComponent appComponent = DaggerApplicationComponent
            .builder()
            .appModule(new AppModule(this))
            .build();

    @SuppressWarnings("deprecation")
    @JvmField
    public ReceiverComponent receiverComponent = DaggerReceiverComponent
            .builder()
            .appModule(new AppModule(this))
            .build();

    @SuppressWarnings("deprecation")
    @JvmField
    public AdapterComponent adapterComponent = DaggerAdapterComponent
            .builder()
            .appModule(new AppModule(this))
            .build();
    
    @SuppressWarnings("deprecation")
    @JvmField
    public DefaultComponent defaultComponent = DaggerDefaultComponent
            .builder()
            .appModule(new AppModule(this))
            .build();


    //QM

    protected void attachBaseContext(@NotNull Context base) {
        Intrinsics.checkNotNullParameter(base, "base");
        this.initBeforeBaseAttach(base);
        super.attachBaseContext(base);
    }

    private final void initBeforeBaseAttach(Context base) {
        FileUtils.appFilesDir = base.getFilesDir();
        this.updateTheme(base);
    }

    private final void updateTheme(Context base) {
        AppCompatDelegate.setDefaultNightMode(ThemeUtils.resolveThemeModeFromSP(base));
    }
    //QM*
    @Override
    public void onCreate() {
        android.util.Log.d("DIAGNOSE", "========================================");
        android.util.Log.d("DIAGNOSE", "App.onCreate() START");
        android.util.Log.d("DIAGNOSE", "========================================");
        
        // 🎯 Firebase Analytics: 记录应用启动（用于留存率分析）
        com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this).logWorkflowStep("app_start");
        
        try {
            super.onCreate();
            android.util.Log.d("DIAGNOSE", "✅ super.onCreate() completed");
            
            // 🎯 Firebase Analytics: 基础初始化完成
            com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this).logWorkflowStep("base_init_complete");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ super.onCreate() FAILED", e);
            throw e;
        }
        
        // 🚨 关键修复：必须在最开始进行 WebView 多进程隔离
        // 这必须在任何可能使用 WebView 的代码（如广告SDK）之前执行
        android.util.Log.d("DIAGNOSE", "→ Starting WebView isolation check...");
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                String currentProcess = Application.getProcessName();
                String mainProcess = this.getPackageName();
                
                android.util.Log.d("DIAGNOSE", "→ Current process: " + currentProcess);
                android.util.Log.d("DIAGNOSE", "→ Main process: " + mainProcess);
                android.util.Log.d("App", "🔍 Process Check - Current: " + currentProcess + ", Main: " + mainProcess);
                
                // 为非主进程设置独立的 WebView 数据目录后缀
                if (currentProcess != null && !currentProcess.equals(mainProcess)) {
                    // 提取进程后缀，例如 "com.quran.quranaudio.online:error_activity" -> "error_activity"
                    String suffix = currentProcess.replace(mainProcess, "").replace(":", "");
                    if (!suffix.isEmpty()) {
                        WebView.setDataDirectorySuffix(suffix);
                        android.util.Log.d("DIAGNOSE", "✅ WebView suffix set: " + suffix);
                        android.util.Log.d("App", "✅ WebView data directory suffix set for CHILD process: [" + suffix + "]");
                    } else {
                        android.util.Log.w("DIAGNOSE", "⚠️ Child process but suffix is empty");
                        android.util.Log.w("App", "⚠️ Child process but suffix is empty");
                    }
                } else {
                    android.util.Log.d("DIAGNOSE", "✅ MAIN process - using default WebView");
                    android.util.Log.d("App", "✅ MAIN process - using default WebView data directory");
                    
                    // 🔥 关键修复：在主进程主线程中提前初始化 WebView（增强版）
                    // 防止广告SDK（如StartApp）在后台线程调用WebSettings.getDefaultUserAgent()时死锁
                    try {
                        android.util.Log.d("DIAGNOSE", "→ Pre-initializing WebView to prevent deadlock...");
                        
                        // 🆕 方法1: 预初始化 UserAgent（最轻量级）
                        String userAgent = android.webkit.WebSettings.getDefaultUserAgent(this);
                        android.util.Log.d("DIAGNOSE", "✅ UserAgent retrieved: " + (userAgent != null ? userAgent.substring(0, Math.min(50, userAgent.length())) + "..." : "null"));
                        
                        // 🆕 方法2: 创建一个临时 WebView 确保完全初始化（更彻底）
                        // 这会触发 WebView provider (Chrome) 的完整加载
                        try {
                            android.webkit.WebView tempWebView = new android.webkit.WebView(this);
                            tempWebView.getSettings().getJavaScriptEnabled(); // 触发设置初始化
                            tempWebView.destroy(); // 立即销毁
                            android.util.Log.d("DIAGNOSE", "✅ Temporary WebView created and destroyed successfully");
                        } catch (Exception tempWebViewEx) {
                            android.util.Log.w("DIAGNOSE", "⚠️ Temporary WebView creation failed (fallback to method 1): " + tempWebViewEx.getMessage());
                            // 失败不致命，方法1已执行
                        }
                        
                        android.util.Log.d("DIAGNOSE", "✅ WebView pre-initialized successfully (dual method)");
                        android.util.Log.d("App", "✅ WebView pre-initialized in main thread (prevents SDK deadlock)");
                        
                    } catch (Exception webViewInitEx) {
                        android.util.Log.e("DIAGNOSE_ERROR", "⚠️ WebView pre-init warning (non-fatal): " + webViewInitEx.getMessage());
                        android.util.Log.w("App", "⚠️ WebView initialization failed - ads may not work properly");
                        // 非致命错误，应用继续启动，但广告可能无法加载
                    }
                }
            } catch (IllegalStateException e) {
                // WebView 已经被初始化（这不应该在 onCreate 开始时发生）
                android.util.Log.e("DIAGNOSE_ERROR", "❌ WebView already initialized!", e);
                android.util.Log.e("App", "❌ WebView already initialized before onCreate!", e);
            } catch (Exception e) {
                android.util.Log.e("DIAGNOSE_ERROR", "❌ WebView isolation failed", e);
                android.util.Log.e("App", "❌ Failed to configure WebView isolation", e);
            }
        }
        
        android.util.Log.d("DIAGNOSE", "→ Starting AdFactory initialization...");
        try {
            AdFactory.INSTANCE.init(this,BuildConfig.DEBUG);
            android.util.Log.d("DIAGNOSE", "✅ AdFactory.init() completed");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ AdFactory.init() FAILED", e);
            throw e;
        }
        
        // 🎯 Initialize and preload interstitial ad manager
        android.util.Log.d("DIAGNOSE", "→ Starting InterstitialAdManager initialization...");
        try {
            com.quranaudio.common.ad.InterstitialAdManager.Companion.getInstance().initialize(this);
            android.util.Log.d("DIAGNOSE", "✅ InterstitialAdManager.initialize() completed");
            com.quranaudio.common.ad.InterstitialAdManager.Companion.getInstance().preloadAd();
            android.util.Log.d("DIAGNOSE", "✅ InterstitialAdManager.preloadAd() completed");
            android.util.Log.d("App", "✅ InterstitialAdManager initialized and preload started");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ InterstitialAdManager initialization FAILED", e);
            throw e;
        }
        
        // 🎯 Initialize and preload native ad manager (优化版：缓存池)
        android.util.Log.d("DIAGNOSE", "→ Starting NativeAdManager initialization...");
        try {
            com.quranaudio.common.ad.NativeAdManager.Companion.getInstance().initialize(this);
            android.util.Log.d("DIAGNOSE", "✅ NativeAdManager.initialize() completed");
            com.quranaudio.common.ad.NativeAdManager.Companion.getInstance().preloadAd();
            android.util.Log.d("DIAGNOSE", "✅ NativeAdManager.preloadAd() completed");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ NativeAdManager initialization FAILED", e);
            throw e;
        }
        
        // ✅ 延迟加载以填满缓存池（3个广告）
        android.util.Log.d("DIAGNOSE", "→ Scheduling delayed NativeAdManager loads...");
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    android.util.Log.d("DIAGNOSE", "→ NativeAdManager.loadNewAd() #2 starting...");
                    com.quranaudio.common.ad.NativeAdManager.Companion.getInstance().loadNewAd();
                    android.util.Log.d("DIAGNOSE", "✅ NativeAdManager.loadNewAd() #2 completed");
                } catch (Exception e) {
                    android.util.Log.e("DIAGNOSE_ERROR", "❌ NativeAdManager.loadNewAd() #2 FAILED", e);
                }
            }
        }, 2000); // 2秒后加载第二个
        
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    android.util.Log.d("DIAGNOSE", "→ NativeAdManager.loadNewAd() #3 starting...");
                    com.quranaudio.common.ad.NativeAdManager.Companion.getInstance().loadNewAd();
                    android.util.Log.d("DIAGNOSE", "✅ NativeAdManager.loadNewAd() #3 completed");
                } catch (Exception e) {
                    android.util.Log.e("DIAGNOSE_ERROR", "❌ NativeAdManager.loadNewAd() #3 FAILED", e);
                }
            }
        }, 4000); // 4秒后加载第三个
        
        android.util.Log.d("DIAGNOSE", "✅ NativeAdManager pool preloading scheduled");
        android.util.Log.d("App", "✅ NativeAdManager initialized with pool preloading (target: 3 ads)");
        
        // 注入 QuranDataProvider 实现给 Quiz 模块
        android.util.Log.d("DIAGNOSE", "→ Starting QuranDataProvider injection...");
        try {
            com.quran.quranaudio.quiz.data.QuranDataProviderHolder.INSTANCE.setInstance(
                com.quran.quranaudio.online.quran_module.quiz.QuranDataRepositoryImpl.getInstance(this)
            );
            android.util.Log.d("DIAGNOSE", "✅ QuranDataProvider injection completed");
            android.util.Log.d("App", "✅ QuranDataProvider injected for Quiz module");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ QuranDataProvider injection FAILED", e);
            throw e;
        }
        
        //Ads
        // 🔥 始终注册Activity生命周期回调（用于跟踪当前Activity）
        android.util.Log.d("DIAGNOSE", "→ Registering ActivityLifecycleCallbacks...");
        try {
            registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
            android.util.Log.d("DIAGNOSE", "✅ ActivityLifecycleCallbacks registered");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ ActivityLifecycleCallbacks registration FAILED", e);
            throw e;
        }
        
        android.util.Log.d("DIAGNOSE", "→ Checking FORCE_TO_SHOW_APP_OPEN_AD_ON_START: " + Constant.FORCE_TO_SHOW_APP_OPEN_AD_ON_START);
        if (!Constant.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
            // 旧的SDK方式（目前不使用因为FORCE_TO_SHOW_APP_OPEN_AD_ON_START=true）
            android.util.Log.d("DIAGNOSE", "→ Using old SDK app open ad approach");
            ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleObserver);
            appOpenAdMob = new AppOpenAdMob();
            appOpenAdManager = new AppOpenAdManager();
            appOpenAdAppLovin = new AppOpenAdAppLovin();
        } else {
            // 🔥 新增：使用新的AdFactory API处理热启动开屏广告
            android.util.Log.d("DIAGNOSE", "→ Using new AdFactory app open ad approach");
            ProcessLifecycleOwner.get().getLifecycle().addObserver(resumeAdObserver);
            android.util.Log.d("DIAGNOSE", "✅ Resume ad observer registered");
            android.util.Log.d("App", "✅ Hot start app open ad observer registered");
        }
        //Ads*
        app = this;

        android.util.Log.d("DIAGNOSE", "→ Loading Typefaces...");
        try {
            this.faceRobotoL = Typeface.createFromAsset(getAssets(), "Roboto_Light.ttf");
            this.faceRobotoB = Typeface.createFromAsset(getAssets(), "Roboto_Bold.ttf");
            this.faceRobotoR = Typeface.createFromAsset(getAssets(), "Roboto_Regular.ttf");
            this.faceArabic = Typeface.createFromAsset(getAssets(), "XBZarIndoPak.ttf");
            android.util.Log.d("DIAGNOSE", "✅ All Typefaces loaded");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ Typeface loading FAILED", e);
            throw e;
        }

        // enable TLS1.1/1.2 for kitkat devices
        android.util.Log.d("DIAGNOSE", "→ SDK version: " + Build.VERSION.SDK_INT);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            android.util.Log.d("DIAGNOSE", "→ Setting TLS for KitKat...");
            try {
                TLSSocketFactoryCompat.setAsDefault();
                android.util.Log.d("DIAGNOSE", "✅ TLS configured for KitKat");
            } catch (Exception e) {
                android.util.Log.e("DIAGNOSE_ERROR", "❌ TLS configuration FAILED", e);
            }
        }

        android.util.Log.d("DIAGNOSE", "→ Configuring CaocConfig...");
        try {
            CaocConfig
                    .Builder
                    .create()
                    .apply();
            android.util.Log.d("DIAGNOSE", "✅ CaocConfig configured");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ CaocConfig FAILED", e);
            throw e;
        }

        android.util.Log.d("DIAGNOSE", "→ Configuring WorkManager...");
        try {
            configureWorkManager();
            android.util.Log.d("DIAGNOSE", "✅ WorkManager configured");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ WorkManager configuration FAILED", e);
            throw e;
        }

        //QM

        android.util.Log.d("DIAGNOSE", "→ Creating NotificationChannels...");
        try {
            NotificationUtils.INSTANCE.createNotificationChannels((Context)this);
            android.util.Log.d("DIAGNOSE", "✅ NotificationChannels created");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ NotificationChannels creation FAILED", e);
            throw e;
        }

        //QM*
        android.util.Log.d("DIAGNOSE", "========================================");
        android.util.Log.d("DIAGNOSE", "✅ App.onCreate() COMPLETED SUCCESSFULLY");
        android.util.Log.d("DIAGNOSE", "========================================");
        
        // 🎯 Firebase Analytics: 应用完全启动成功
        com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this).logWorkflowStep("app_init_complete");
    }


    private void configureWorkManager() {
        WorkerProviderFactory factory = appComponent.workerProviderFactory();
        Configuration config = new Configuration.Builder()
                .setWorkerFactory(factory)
                .build();

        WorkManager.initialize(this, config);
    }

    //Ads
    // 🔥 用于热启动（从后台恢复）时展示开屏广告的生命周期观察器
    LifecycleObserver resumeAdObserver = new DefaultLifecycleObserver() {
        private boolean isFirstLaunch = true;
        
        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStart(owner);
            
            // 🔥 首次启动：不展示但要预加载
            if (isFirstLaunch) {
                isFirstLaunch = false;
                android.util.Log.d("App", "🚀 First launch, preloading app open ad");
                if (currentActivity != null) {
                    AdFactory.INSTANCE.loadAppOpenAd(currentActivity, com.quranaudio.common.ad.AdConfig.AD_APPOPEN, null);
                }
                return;
            }
            
            // 🔥 检查当前Activity是否有效
            if (currentActivity == null) {
                android.util.Log.w("App", "⚠️ currentActivity is null");
                return;
            }
            
            String activityName = currentActivity.getClass().getSimpleName();
            android.util.Log.d("App", "🔄 App onStart, current: " + activityName);
            
            // 🔥 只排除SplashScreenActivity（避免重复展示）
            if (activityName.equals("SplashScreenActivity")) {
                android.util.Log.d("App", "🚫 Skip ad on SplashScreenActivity");
                return;
            }
            
            // 🔥 所有其他页面都展示开屏广告
            android.util.Log.d("App", "✅ Showing app open ad on: " + activityName);
            
            if (AdFactory.INSTANCE.hasAppOpenAd(com.quranaudio.common.ad.AdConfig.AD_APPOPEN)) {
                android.util.Log.d("App", "📱 Ad ready, showing...");
                AdFactory.INSTANCE.showAppOpenAd(currentActivity, com.quranaudio.common.ad.AdConfig.AD_APPOPEN, new com.quranaudio.common.ad.AdShowCallback() {
                    @Override
                    public void onAdImpression(@Nullable com.quranaudio.common.ad.model.AdItem adItem) {
                        android.util.Log.d("App", "📱 Ad impression");
                    }

                    @Override
                    public void onAdClicked(@Nullable com.quranaudio.common.ad.model.AdItem adItem) {
                        android.util.Log.d("App", "👆 Ad clicked");
                    }

                    @Override
                    public void onUserEarnedReward(@Nullable com.quranaudio.common.ad.model.AdItem adItem, @Nullable com.quranaudio.common.ad.model.RewardItem rewardItem) {
                        // N/A for app open ads
                    }

                    @Override
                    public void onShow(@Nullable com.quranaudio.common.ad.model.AdItem adItem) {
                        android.util.Log.d("App", "📱 Ad shown");
                    }

                    @Override
                    public void onShowFail() {
                        android.util.Log.w("App", "❌ Ad show failed, reloading");
                        AdFactory.INSTANCE.loadAppOpenAd(currentActivity, com.quranaudio.common.ad.AdConfig.AD_APPOPEN, null);
                    }

                    @Override
                    public void onAdClosed(@Nullable com.quranaudio.common.ad.model.AdItem adItem) {
                        android.util.Log.d("App", "📱 Ad closed, preloading next");
                        AdFactory.INSTANCE.loadAppOpenAd(currentActivity, com.quranaudio.common.ad.AdConfig.AD_APPOPEN, null);
                    }
                });
            } else {
                android.util.Log.w("App", "⚠️ Ad not ready, loading now");
                AdFactory.INSTANCE.loadAppOpenAd(currentActivity, com.quranaudio.common.ad.AdConfig.AD_APPOPEN, null);
            }
        }
    };
    
    // 保留旧的生命周期观察器（用于兼容性，但实际不会被注册因为FORCE_TO_SHOW_APP_OPEN_AD_ON_START=true）
    LifecycleObserver lifecycleObserver = new DefaultLifecycleObserver() {
        @Override
        public void onStart(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStart(owner);
            if (Constant.isAppOpen) {
                if (Constant.OPEN_ADS_ON_RESUME) {
                    if (Constant.AD_STATUS.equals(AD_STATUS_ON)) {
                        switch (Constant.AD_NETWORK) {
                            case ADMOB:
                                if (!Constant.ADMOB_APP_OPEN_AD_ID.equals("0")) {
                                    if (!currentActivity.getIntent().hasExtra("unique_id")) {
                                        appOpenAdMob.showAdIfAvailable(currentActivity, Constant.ADMOB_APP_OPEN_AD_ID);
                                    }
                                }
                                break;
                            case GOOGLE_AD_MANAGER:
                                if (!Constant.GOOGLE_AD_MANAGER_APP_OPEN_AD_ID.equals("0")) {
                                    if (!currentActivity.getIntent().hasExtra("unique_id")) {
                                        appOpenAdManager.showAdIfAvailable(currentActivity, Constant.GOOGLE_AD_MANAGER_APP_OPEN_AD_ID);
                                    }
                                }
                                break;
                            case APPLOVIN:
                            case APPLOVIN_MAX:
                                if (!Constant.APPLOVIN_APP_OPEN_AP_ID.equals("0")) {
                                    if (!currentActivity.getIntent().hasExtra("unique_id")) {
                                        appOpenAdAppLovin.showAdIfAvailable(currentActivity, Constant.APPLOVIN_APP_OPEN_AP_ID);
                                    }
                                }
                                break;

                        }
                    }
                }
            }
        }
    };

    ActivityLifecycleCallbacks activityLifecycleCallbacks = new ActivityLifecycleCallbacks() {
        @Override
        public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
            // 🔥 更新当前Activity引用（用于热启动展示广告）
            currentActivity = activity;
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            // 🔥 更新当前Activity引用（用于热启动展示广告）
            currentActivity = activity;
            
            // 🔥 仅在旧的SDK方式下才需要检查appOpenAdMob对象
            // 新逻辑使用AdFactory和resumeAdObserver，不需要这些检查
            if (!Constant.FORCE_TO_SHOW_APP_OPEN_AD_ON_START && Constant.OPEN_ADS_ON_START) {
                if (Constant.AD_STATUS.equals(AD_STATUS_ON)) {
                    switch (Constant.AD_NETWORK) {
                        case ADMOB:
                            if (!Constant.ADMOB_APP_OPEN_AD_ID.equals("0")) {
                                if (appOpenAdMob != null && !appOpenAdMob.isShowingAd) {
                                    currentActivity = activity;
                                }
                            }
                            break;
                        case GOOGLE_AD_MANAGER:
                            if (!Constant.GOOGLE_AD_MANAGER_APP_OPEN_AD_ID.equals("0")) {
                                if (appOpenAdManager != null && !appOpenAdManager.isShowingAd) {
                                    currentActivity = activity;
                                }
                            }
                            break;
                        case APPLOVIN:
                        case APPLOVIN_MAX:
                            if (!Constant.APPLOVIN_APP_OPEN_AP_ID.equals("0")) {
                                if (appOpenAdAppLovin != null && !appOpenAdAppLovin.isShowingAd) {
                                    currentActivity = activity;
                                }
                            }
                            break;
                    }
                }
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            // 🔥 更新当前Activity引用（用于热启动展示广告）
            currentActivity = activity;
            
            // ✅ 检查原生广告缓存池，不足则补充
            int cacheSize = com.quranaudio.common.ad.NativeAdManager.Companion.getInstance().getCacheSize();
            if (cacheSize < 2) {
                android.util.Log.d("App", "⚠️ Native ad cache low (" + cacheSize + "), replenishing...");
                com.quranaudio.common.ad.NativeAdManager.Companion.getInstance().loadNewAd();
            }
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
            // 🔥 清理当前Activity引用（避免内存泄漏）
            if (currentActivity == activity) {
                currentActivity = null;
            }
        }
    };

    public void showAdIfAvailable(@NonNull Activity activity, @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        if (Constant.OPEN_ADS_ON_START) {
            if (Constant.AD_STATUS.equals(AD_STATUS_ON)) {
                switch (Constant.AD_NETWORK) {
                    case ADMOB:
                        if (!Constant.ADMOB_APP_OPEN_AD_ID.equals("0")) {
                            appOpenAdMob.showAdIfAvailable(activity, Constant.ADMOB_APP_OPEN_AD_ID, onShowAdCompleteListener);
                            Constant.isAppOpen = true;
                        }
                        break;
                    case GOOGLE_AD_MANAGER:
                        if (!Constant.GOOGLE_AD_MANAGER_APP_OPEN_AD_ID.equals("0")) {
                            appOpenAdManager.showAdIfAvailable(activity, Constant.GOOGLE_AD_MANAGER_APP_OPEN_AD_ID, onShowAdCompleteListener);
                            Constant.isAppOpen = true;
                        }
                        break;
                    case APPLOVIN:
                    case APPLOVIN_MAX:
                        if (!Constant.APPLOVIN_APP_OPEN_AP_ID.equals("0")) {
                            appOpenAdAppLovin.showAdIfAvailable(activity, Constant.APPLOVIN_APP_OPEN_AP_ID, onShowAdCompleteListener);
                            Constant.isAppOpen = true;
                        }
                        break;
                }
            }
        }
    }

    //Ads*
}
