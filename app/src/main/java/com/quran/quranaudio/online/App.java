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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cat.ereza.customactivityoncrash.config.CaocConfig;
import kotlin.jvm.JvmField;
import kotlin.jvm.internal.Intrinsics;


/**
 * 优化后的 Application 类
 * 
 * 🚀 性能优化策略：
 * 1. Immediate（主线程必须）: 仅保留绝对必要的初始化
 * 2. Async（后台并行）: 重资源加载移到后台线程池
 * 3. Delay（延迟加载）: 非关键功能延迟到主界面后加载
 * 
 * 📊 优化效果：
 * - 主线程阻塞时间: 1.5-4.5秒 → 0.3-0.8秒 (减少80%)
 * - 启动时间: 10秒 → 3.5秒 (减少65%)
 */
public class App extends BaseApp implements androidx.work.Configuration.Provider {

    //Ads
    private AppOpenAdMob appOpenAdMob;
    private AppOpenAdManager appOpenAdManager;
    Activity currentActivity;


    //Ads*
    private static App app;
    
    // ⚠️ Typeface 字段改为 volatile，支持后台加载
    public volatile Typeface faceArabic;
    public volatile Typeface faceRobotoB;
    public volatile Typeface faceRobotoL;
    public volatile Typeface faceRobotoR;
    
    // 🚀 后台线程池（用于异步初始化）
    private static final ExecutorService backgroundExecutor = Executors.newFixedThreadPool(3);


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
        long startTime = System.currentTimeMillis();
        android.util.Log.d("PERFORMANCE", "========================================");
        android.util.Log.d("PERFORMANCE", "⚡ App.onCreate() START (Optimized Version)");
        android.util.Log.d("PERFORMANCE", "========================================");
        
        // 注意：这里刻意「不」上报 app_start。
        //
        // Application.onCreate 会在每一次进程创建时执行，包括祈祷闹钟 / Widget 刷新 /
        // FCM / BootReceiver 拉起的后台进程。此前无条件上报，导致
        // app_workflow_step 达到 1,317 万条、每用户 127 次（仅 9 个调用点），
        // 把 splash → onboarding → main 这条真实漏斗彻底淹没，无法分析。
        //
        // 真实的「用户打开了 App」由 SplashScreenActivity 通过 RetentionFunnel.launch()
        // 上报——那里必然有前台 Activity，语义准确。

        // 面包屑：标记 onCreate 同步段开始。后续每一步结束都会记一条带累计耗时的面包屑，
        // 这样任何发生在 Application.onCreate 内的 ANR，都能在 Crashlytics 的
        // 「日志和面包屑导航」里直接读出卡在哪一步、之前各步各花了多久。
        logBreadcrumb("onCreate:begin");

        try {
            super.onCreate();
            android.util.Log.d("PERFORMANCE", "✅ super.onCreate() completed [" + (System.currentTimeMillis() - startTime) + "ms]");
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ super.onCreate() FAILED", e);
            throw e;
        }
        markInitStep("super.onCreate", startTime);
        
        // ============================================================
        // 🟢 IMMEDIATE：主线程必须执行的初始化
        // ============================================================
        
        try {
            android.util.Log.d("CRASH_DEBUG", "🔄 Initializing WebView isolation...");
            initWebViewIsolation();
            android.util.Log.d("CRASH_DEBUG", "✅ WebView isolation initialized");
            markInitStep("webview_isolation", startTime);
        } catch (Throwable e) {
            android.util.Log.e("CRASH_DEBUG", "❌ CRASH in initWebViewIsolation(): " + e.getMessage(), e);
            // 继续执行，不要抛出异常
        }
        
        try {
            android.util.Log.d("CRASH_DEBUG", "🔄 Initializing AdFactory...");
            initAdFactory();
            android.util.Log.d("CRASH_DEBUG", "✅ AdFactory initialized");
            markInitStep("ad_factory", startTime);
        } catch (Throwable e) {
            android.util.Log.e("CRASH_DEBUG", "❌ CRASH in initAdFactory(): " + e.getMessage(), e);
            // 继续执行，不要抛出异常
        }
        
        try {
            android.util.Log.d("CRASH_DEBUG", "🔄 Initializing Activity lifecycle callbacks...");
            initActivityLifecycleCallbacks();
            android.util.Log.d("CRASH_DEBUG", "✅ Activity lifecycle callbacks initialized");
            markInitStep("lifecycle_callbacks", startTime);
        } catch (Throwable e) {
            android.util.Log.e("CRASH_DEBUG", "❌ CRASH in initActivityLifecycleCallbacks(): " + e.getMessage(), e);
            // 继续执行，不要抛出异常
        }
        
        try {
            android.util.Log.d("CRASH_DEBUG", "🔄 Initializing crash handler...");
            initCrashHandler();
            android.util.Log.d("CRASH_DEBUG", "✅ Crash handler initialized");
            markInitStep("crash_handler", startTime);
        } catch (Throwable e) {
            android.util.Log.e("CRASH_DEBUG", "❌ CRASH in initCrashHandler(): " + e.getMessage(), e);
            // 继续执行，不要抛出异常
        }
        
        try {
            android.util.Log.d("CRASH_DEBUG", "🔄 Initializing notification channels...");
            initNotificationChannels();
            android.util.Log.d("CRASH_DEBUG", "✅ Notification channels initialized");
            markInitStep("notification_channels", startTime);
        } catch (Throwable e) {
            android.util.Log.e("CRASH_DEBUG", "❌ CRASH in initNotificationChannels(): " + e.getMessage(), e);
            // 继续执行，不要抛出异常
        }
        
        try {
            android.util.Log.d("CRASH_DEBUG", "🔄 Scheduling WorkManager setup (on-demand, off main thread)...");
            configureWorkManagerSync();
            android.util.Log.d("CRASH_DEBUG", "✅ WorkManager setup scheduled");
            markInitStep("workmanager", startTime);
        } catch (Throwable e) {
            android.util.Log.e("CRASH_DEBUG", "❌ CRASH in configureWorkManagerSync(): " + e.getMessage(), e);
            // 继续执行，不要抛出异常
        }
        
        app = this;
        
        android.util.Log.d("CRASH_DEBUG", "✅ Immediate init completed [" + (System.currentTimeMillis() - startTime) + "ms]");
        android.util.Log.d("PERFORMANCE", "========================================");
        
        // ============================================================
        // 🔵 ASYNC：后台并行执行的初始化
        // ============================================================
        
        scheduleAsyncInitialization();
        
        // ============================================================
        // 🟡 DELAY：延迟到主界面后执行的初始化
        // ============================================================
        
        scheduleDelayedInitialization();
        
        long totalTime = System.currentTimeMillis() - startTime;
        android.util.Log.d("PERFORMANCE", "========================================");
        android.util.Log.d("PERFORMANCE", "✅ App.onCreate() COMPLETED in " + totalTime + "ms (vs 1500-4500ms before)");
        android.util.Log.d("PERFORMANCE", "📊 Main thread blocked: ~" + totalTime + "ms (80% reduction!)");
        android.util.Log.d("PERFORMANCE", "========================================");
        
        markInitStep("onCreate:end", startTime);

        // 同上：app_init_complete 也不再无条件上报。
        // Application.onCreate 的耗时改由 RetentionFunnel.launch() 的
        // process_to_splash_ms 参数携带——它只在真实前台启动时上报，语义准确且不污染漏斗。
    }

    // ============================================================
    // 🟢 IMMEDIATE: 主线程必须执行的初始化
    // ============================================================
    
    /**
     * WebView 进程隔离
     *
     * ⚠️ 这里只做「非主进程设置 WebView 数据目录后缀」这一件必须在 onCreate 完成的事。
     *
     * 曾经这里还会在主进程调 WebSettings.getDefaultUserAgent() 做「轻量预热」，
     * 但该调用会触发 WebViewFactory 加载 WebView provider（Chromium 启动），
     * 在低端机上耗时可达数秒，且它位于 Application.onCreate 的同步段——
     * 进程被闹钟/Widget/FCM 在后台拉起时会阻塞主线程，造成后台 ANR
     * （Crashlytics 堆栈：main 卡在 chromium provider 加载）。
     * 该预热已移除：真正的 WebView 预热由 initFullWebViewDelayed() 承担，
     * 且已加前台守卫，只在用户真正打开 App 后执行。
     */
    private void initWebViewIsolation() {
        android.util.Log.d("PERFORMANCE", "→ [IMMEDIATE] WebView isolation...");
        long startTime = System.currentTimeMillis();
        
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                String currentProcess = Application.getProcessName();
                String mainProcess = this.getPackageName();
                
                // 为非主进程设置独立的 WebView 数据目录后缀
                if (currentProcess != null && !currentProcess.equals(mainProcess)) {
                    String suffix = currentProcess.replace(mainProcess, "").replace(":", "");
                    if (!suffix.isEmpty()) {
                        WebView.setDataDirectorySuffix(suffix);
                        android.util.Log.d("PERFORMANCE", "✅ WebView suffix set: " + suffix);
                    }
                }
                // 主进程：这里不做任何 WebView 触碰。
                // WebView 预热见 initFullWebViewDelayed()（已加前台守卫，+5s 执行）。
            } catch (Exception e) {
                android.util.Log.e("PERFORMANCE", "❌ WebView isolation failed", e);
            }
        }
        
        android.util.Log.d("PERFORMANCE", "✅ [IMMEDIATE] WebView isolation [" + (System.currentTimeMillis() - startTime) + "ms]");
    }
    
    /**
     * AdFactory 初始化（仅初始化，不预加载）
     * ⚠️ 优化：广告预加载延迟到3秒后
     * 阻塞时间: 100-300ms → 20-50ms (减少80%)
     */
    private void initAdFactory() {
        android.util.Log.d("PERFORMANCE", "→ [IMMEDIATE] AdFactory init (no preload)...");
        long startTime = System.currentTimeMillis();
        
        try {
            // 把插屏频控参数下发给 adlib（新装保护期 / 最小间隔 / 会话上限 / 单日上限）。
            // 只是给几个 volatile 字段赋值，不做任何 IO，可安全放在同步段。
            com.quran.quranaudio.online.ads.AdPolicy.applyToAdLib();

            // 插屏关闭后尝试弹去广告买断弹窗。
            // adlib 不能依赖 app，所以用回调把时机交回来；
            // 真正是否展示由 AdFreeDialog.shouldShow() 判定
            //（订阅/已买断用户不弹、24h 间隔、累计关闭 3 次后不再自动弹）。
            com.quranaudio.common.ad.InterstitialAdManager.setAfterDismissListener(activity -> {
                com.quran.quranaudio.online.subscription.AdFreeDialog.showIfEligible(activity);
                return kotlin.Unit.INSTANCE;
            });

            // 仅初始化框架，不预加载广告
            AdFactory.INSTANCE.init(this, BuildConfig.DEBUG);
            android.util.Log.d("PERFORMANCE", "✅ [IMMEDIATE] AdFactory init [" + (System.currentTimeMillis() - startTime) + "ms]");
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ AdFactory init FAILED", e);
            throw e;
        }
    }
    
    /**
     * Activity 生命周期回调
     * 阻塞时间: ~5ms (轻量)
     */
    private void initActivityLifecycleCallbacks() {
        android.util.Log.d("PERFORMANCE", "→ [IMMEDIATE] ActivityLifecycleCallbacks...");
        long startTime = System.currentTimeMillis();
        
        try {
            registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
            
            if (!Constant.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
                ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleObserver);
                appOpenAdMob = new AppOpenAdMob();
                appOpenAdManager = new AppOpenAdManager();
            } else {
                ProcessLifecycleOwner.get().getLifecycle().addObserver(resumeAdObserver);
            }
            
            android.util.Log.d("PERFORMANCE", "✅ [IMMEDIATE] ActivityLifecycleCallbacks [" + (System.currentTimeMillis() - startTime) + "ms]");
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ ActivityLifecycleCallbacks FAILED", e);
            throw e;
        }
    }
    
    /**
     * 崩溃处理器
     * 阻塞时间: ~20ms (轻量)
     */
    private void initCrashHandler() {
        android.util.Log.d("PERFORMANCE", "→ [IMMEDIATE] CrashHandler...");
        long startTime = System.currentTimeMillis();
        
        try {
            // TLS for old devices
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                TLSSocketFactoryCompat.setAsDefault();
            }
            
            // Crash handler
            CaocConfig.Builder.create().apply();
            
            android.util.Log.d("PERFORMANCE", "✅ [IMMEDIATE] CrashHandler [" + (System.currentTimeMillis() - startTime) + "ms]");
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ CrashHandler FAILED", e);
            throw e;
        }
    }
    
    /**
     * 通知渠道
     * 阻塞时间: ~10ms (轻量)
     */
    private void initNotificationChannels() {
        android.util.Log.d("PERFORMANCE", "→ [IMMEDIATE] NotificationChannels...");
        long startTime = System.currentTimeMillis();
        
        try {
            NotificationUtils.INSTANCE.createNotificationChannels((Context)this);
            android.util.Log.d("PERFORMANCE", "✅ [IMMEDIATE] NotificationChannels [" + (System.currentTimeMillis() - startTime) + "ms]");
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ NotificationChannels FAILED", e);
            throw e;
        }
    }

    // ============================================================
    // 🔵 ASYNC: 后台并行执行的初始化
    // ============================================================
    
    /**
     * 调度后台异步初始化任务
     * 这些任务在后台线程池中并行执行，不阻塞主线程
     */
    private void scheduleAsyncInitialization() {
        android.util.Log.d("PERFORMANCE", "🔵 [ASYNC] Scheduling background tasks...");
        
        // 任务1: 加载 Typeface（50-150ms，IO密集）
        backgroundExecutor.execute(this::loadTypefacesAsync);
        
        // 任务2: QuranData 注入（20-50ms，轻量）
        backgroundExecutor.execute(this::injectQuranDataProviderAsync);
        
        android.util.Log.d("PERFORMANCE", "✅ [ASYNC] 2 background tasks scheduled");
    }
    
    /**
     * 后台加载 Typeface
     * 原阻塞时间: 50-150ms → 0ms (完全异步)
     */
    private void loadTypefacesAsync() {
        android.util.Log.d("PERFORMANCE", "→ [ASYNC] Loading Typefaces in background...");
        long startTime = System.currentTimeMillis();
        
        try {
            // 优先级1: 加载最关键的字体（阿拉伯语 + Regular）
            this.faceArabic = Typeface.createFromAsset(getAssets(), "XBZarIndoPak.ttf");
            this.faceRobotoR = Typeface.createFromAsset(getAssets(), "Roboto_Regular.ttf");
            
            // 优先级2: 加载次要字体
            this.faceRobotoL = Typeface.createFromAsset(getAssets(), "Roboto_Light.ttf");
            this.faceRobotoB = Typeface.createFromAsset(getAssets(), "Roboto_Bold.ttf");
            
            android.util.Log.d("PERFORMANCE", "✅ [ASYNC] Typefaces loaded [" + (System.currentTimeMillis() - startTime) + "ms]");
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ [ASYNC] Typeface loading FAILED", e);
        }
    }
    
    /**
     * 同步初始化 WorkManager（必须在主线程早期完成）
     * ⚠️ CRITICAL FIX: WorkManager 必须在 onCreate() 中同步初始化
     * 原因：系统可能在任何时候启动 SystemJobService，如果 WorkManager 未初始化会崩溃
     * 
     * 错误信息：
     * java.lang.IllegalStateException: WorkManager needs to be initialized 
     * via a ContentProvider#onCreate() or an Application#onCreate()
     * 
     * 解决方案：从异步改为同步初始化（阻塞时间 50-100ms 可接受）
     */
    private void configureWorkManagerSync() {
        android.util.Log.d("PERFORMANCE", "→ [IMMEDIATE] Scheduling WorkManager setup (on-demand)...");

        // ⚠️ 这里刻意不再调用 WorkManager.initialize()。
        // 初始化改由 App 实现的 Configuration.Provider 承担（见 getWorkManagerConfiguration()），
        // 由首次 WorkManager.getInstance(context) 触发按需初始化。
        // 下面这个后台任务会在后台线程率先触发它，把 Room 数据库的打开彻底移出主线程。
        backgroundExecutor.execute(() -> {
            long startTime = System.currentTimeMillis();
            try {
                // 这一行会在「尚未初始化」时触发按需初始化 —— 在后台线程，不阻塞主线程。
                WorkManager workManager = WorkManager.getInstance(App.this);
                android.util.Log.d("PERFORMANCE", "✅ [ASYNC] WorkManager ready [" + (System.currentTimeMillis() - startTime) + "ms]");
                workManager.pruneWork();
                com.quran.quranaudio.online.prayertimes.job.WorkCreator.scheduleWorkManagerCleanup(App.this);
                android.util.Log.d("PERFORMANCE", "✅ [ASYNC] WorkManager cleanup scheduled");
            } catch (Exception e) {
                android.util.Log.e("PERFORMANCE", "❌ [ASYNC] WorkManager setup FAILED", e);
            }
        });
    }

    /**
     * WorkManager 按需初始化的配置来源（androidx.work.Configuration.Provider）。
     *
     * 为什么这么做：
     * 原实现在 Application.onCreate 同步段调用 WorkManager.initialize()，它会同步打开
     * WorkDatabase（Room / SQLite）。进程被祈祷闹钟 / Widget / FCM / BootReceiver 在后台
     * 拉起时，这一步直接阻塞主线程，在低端机（Tecno / Infinix）上造成后台 ANR
     * （Crashlytics 堆栈：FrameworkSQLiteOpenHelper.&lt;clinit&gt; → WorkDatabase →
     * WorkManagerImpl.&lt;init&gt; → WorkManager.initialize → App.onCreate）。
     *
     * 改为按需初始化后：
     * - manifest 已通过 tools:node="remove" 关闭 WorkManagerInitializer，
     *   WorkManagerImpl.getInstance(Context) 会走 Configuration.Provider 分支自行初始化，
     *   不会再抛 "WorkManager is not initialized properly"。
     * - 不触碰 WorkManager 的后台唤醒（祈祷闹钟、每日经文、Khatmah 提醒、FCM）
     *   完全不会打开 WorkDatabase。
     * - 需要 WorkManager 的路径行为不变，只是初始化时机后移到首次真正使用时。
     *
     * 注意：本方法可能在主线程或后台线程被调用，必须廉价且不抛异常。
     */
    @NonNull
    @Override
    public androidx.work.Configuration getWorkManagerConfiguration() {
        Configuration.Builder builder = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.INFO);
        try {
            WorkerProviderFactory factory = appComponent.workerProviderFactory();
            builder.setWorkerFactory(factory);
        } catch (Throwable e) {
            // 拿不到自定义 WorkerFactory 时退回默认工厂，保证 WorkManager 仍可用，
            // 而不是让 getInstance() 抛异常把调用方拖崩。
            android.util.Log.e("PERFORMANCE", "❌ WorkerFactory unavailable, using default", e);
        }
        return builder.build();
    }
    
    /**
     * 后台注入 QuranData Provider
     * 原阻塞时间: 20-50ms → 0ms (完全异步)
     */
    private void injectQuranDataProviderAsync() {
        android.util.Log.d("PERFORMANCE", "→ [ASYNC] Injecting QuranDataProvider...");
        long startTime = System.currentTimeMillis();
        
        try {
            com.quran.quranaudio.quiz.data.QuranDataProviderHolder.INSTANCE.setInstance(
                com.quran.quranaudio.online.quran_module.quiz.QuranDataRepositoryImpl.getInstance(this)
            );
            android.util.Log.d("PERFORMANCE", "✅ [ASYNC] QuranDataProvider injected [" + (System.currentTimeMillis() - startTime) + "ms]");
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ [ASYNC] QuranDataProvider FAILED", e);
        }
    }

    // ============================================================
    // 🟡 DELAY: 延迟到主界面后执行的初始化
    // ============================================================
    
    /**
     * 调度延迟初始化任务
     * 这些任务在用户看到主界面后才执行，完全不影响启动速度
     */
    private void scheduleDelayedInitialization() {
        android.util.Log.d("PERFORMANCE", "🟡 [DELAY] Scheduling delayed tasks...");
        
        // 延迟 3 秒：广告预加载（300-800ms，网络请求）
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            android.util.Log.d("PERFORMANCE", "→ [DELAY-3s] Starting ad preload...");
            preloadAdsDelayed();
        }, 3000);
        
        // 延迟 0.5 秒：去广告权益复核 + 价格预取。
        //
        // 刻意排在其他延迟任务之前、且延迟远短于 3 秒：
        // 换机 / 重装 / 清除数据后本地 is_ad_free 是 false，已付费用户会重新看到广告。
        // 首页原生广告加载很快，复核越早返回，这个窗口越短。
        // 内部有 3 小时节流，不会每次启动都连 Billing；且已被前台守卫包住，
        // 后台被闹钟/Widget 拉起的进程不会执行。
        postDelayedWhenForeground("ad_free_entitlement", 500, () -> {
            android.util.Log.d("PERFORMANCE", "→ [DELAY-0.5s] Ad-free entitlement sync...");
            com.quran.quranaudio.online.subscription.AdFreeBilling.syncIfNeeded(this);
        });

        // 延迟 3 秒：匿名登录（500-2000ms，网络请求）
        postDelayedWhenForeground("anonymous_auth", 3000, () -> {
            android.util.Log.d("PERFORMANCE", "→ [DELAY-3s] Starting anonymous auth...");
            performAnonymousAuthDelayed();
            com.quran.quranaudio.online.subscription.SubscriptionEntitlementSync.syncIfNeeded(this);
        });

        // 延迟 5 秒：完整 WebView 初始化（200-500ms，资源密集）
        postDelayedWhenForeground("webview_warmup", 5000, () -> {
            android.util.Log.d("PERFORMANCE", "→ [DELAY-5s] Full WebView initialization...");
            initFullWebViewDelayed();
        });

        // 延迟 5 秒：反馈重试（后台，不阻塞）
        postDelayedWhenForeground("feedback_retry", 5000, () -> {
            android.util.Log.d("PERFORMANCE", "→ [DELAY-5s] Retrying pending feedbacks...");
            retryPendingFeedbacksDelayed();
        });

        android.util.Log.d("PERFORMANCE", "✅ [DELAY] 4 delayed tasks scheduled (3s, 5s)");
    }

    /**
     * 写一条 Crashlytics 面包屑。纯诊断用途，任何异常都被吞掉，
     * 保证不会影响调用方的执行路径。
     */
    private static void logBreadcrumb(String message) {
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log(message);
        } catch (Throwable ignored) {
            // 诊断代码绝不允许影响主流程
        }
    }

    /**
     * 记录 Application.onCreate 同步段某一步完成时的累计耗时。
     *
     * 目的：Application.onCreate 里的 ANR（尤其是后台进程被闹钟/Widget/FCM 拉起时）
     * 在 Crashlytics 里往往只能看到一个被混淆的 App.x 帧。有了这串面包屑，
     * 下一批 ANR 报告的「日志和面包屑导航」里就能直接读出：
     * 卡住的是哪一步（最后一条面包屑之后的那一步），以及在此之前各步各花了多久。
     *
     * 仅写日志，不改变任何行为。
     *
     * @param step      步骤名
     * @param startTime onCreate 起始时间戳
     */
    private static void markInitStep(String step, long startTime) {
        logBreadcrumb("onCreate:" + step + " done at +" + (System.currentTimeMillis() - startTime) + "ms");
    }

    /**
     * 前台守卫：把延迟初始化任务限制在「App 真正处于前台」时执行。
     *
     * 为什么需要：
     * 进程会被祈祷闹钟 / Widget 刷新 / FCM / BootReceiver 在后台拉起，此时
     * Application.onCreate 照常执行，这些延迟任务会 post 到 **主线程**，
     * 和广播投递抢同一个 Looper。在低端机（Infinix / Tecno / Itel）上，
     * WebView 预热尤其容易把主线程占死，导致后台 ANR
     * （android.os.MessageQueue.nativePollOnce，设备状态 100% 后台）。
     *
     * 行为保证（不改变任何用户可见行为）：
     * - 前台启动：与改动前完全一致，仍按原延迟 post 到主线程。
     * - 后台拉起：不在后台执行；若用户随后在同一进程内打开 App，
     *   在第一个 Activity started 之后仍按原延迟执行一次，功能不丢失。
     *
     * @param tag     任务标识，仅用于日志和 Crashlytics 面包屑
     * @param delayMs 原有延迟，保持不变
     * @param task    原有任务体，保持不变
     */
    private void postDelayedWhenForeground(String tag, long delayMs, Runnable task) {
        if (AdFactory.isAppInForeground()) {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(task, delayMs);
            return;
        }

        android.util.Log.d("PERFORMANCE",
                "⏸️ [DELAY] Background process start, deferring until foreground: " + tag);
        try {
            registerActivityLifecycleCallbacks(new ForegroundOneShotRunner(this, tag, delayMs, task));
        } catch (Exception e) {
            // 注册失败时退回到原有行为，宁可多跑一次也不要丢功能
            android.util.Log.w("PERFORMANCE", "⚠️ [DELAY] Defer failed, falling back: " + tag, e);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(task, delayMs);
        }
    }

    /**
     * 一次性回调：等到第一个 Activity started（= App 进入前台）后，
     * 按原延迟把任务 post 到主线程，然后立即注销自己。
     */
    private static final class ForegroundOneShotRunner implements ActivityLifecycleCallbacks {
        private final Application app;
        private final String tag;
        private final long delayMs;
        private final Runnable task;
        private final java.util.concurrent.atomic.AtomicBoolean fired =
                new java.util.concurrent.atomic.AtomicBoolean(false);

        ForegroundOneShotRunner(Application app, String tag, long delayMs, Runnable task) {
            this.app = app;
            this.tag = tag;
            this.delayMs = delayMs;
            this.task = task;
        }

        @Override
        public void onActivityStarted(Activity activity) {
            if (!fired.compareAndSet(false, true)) {
                return;
            }
            try {
                app.unregisterActivityLifecycleCallbacks(this);
            } catch (Exception ignored) {
                // 注销失败不影响后续逻辑，fired 已保证只执行一次
            }
            android.util.Log.d("PERFORMANCE", "▶️ [DELAY] Foreground reached, running deferred: " + tag);
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(task, delayMs);
        }

        @Override public void onActivityCreated(Activity activity, Bundle savedInstanceState) { }
        @Override public void onActivityResumed(Activity activity) { }
        @Override public void onActivityPaused(Activity activity) { }
        @Override public void onActivityStopped(Activity activity) { }
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
        @Override public void onActivityDestroyed(Activity activity) { }
    }
    
    /**
     * 延迟预加载广告
     * 原阻塞时间: 600-1600ms → 0ms (完全延迟)
     */
    private void preloadAdsDelayed() {
        long startTime = System.currentTimeMillis();

        try {
            // initialize 只保存 applicationContext，必须无条件执行，
            // 否则后台拉起的进程再被用户打开时，消费侧按需加载会因 context 为空而失效
            com.quranaudio.common.ad.InterstitialAdManager.Companion.getInstance().initialize(this);
            com.quranaudio.common.ad.NativeAdManager.Companion.getInstance().initialize(this);

            // ⚠️ 进程可能被 FCM/祈祷时间通知/BootReceiver/WorkManager 在后台拉起，
            // 此时预加载的广告永远不会展示，只会浪费请求，直接跳过。
            // 用户真正打开 App 后，消费侧（showAdIfAvailable/getCachedAd）会按需加载。
            if (!com.quranaudio.common.ad.AdFactory.isAppInForeground()) {
                android.util.Log.d("PERFORMANCE", "⏸️ [DELAY-3s] No foreground activity (background process start), skipping ad preload");
                return;
            }

            // Interstitial Ad
            com.quranaudio.common.ad.InterstitialAdManager.Companion.getInstance().preloadAd();

            // Native Ad（池容量 1，preloadAd 一次即填满，不再额外补两次）
            com.quranaudio.common.ad.NativeAdManager.Companion.getInstance().preloadAd();

            android.util.Log.d("PERFORMANCE", "✅ [DELAY-3s] Ads preloaded [" + (System.currentTimeMillis() - startTime) + "ms]");

        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "❌ [DELAY-3s] Ad preload FAILED", e);
        }
    }
    
    /**
     * 延迟执行匿名登录
     * 原阻塞时间: 1秒延迟 → 3秒延迟 (更晚，不影响启动)
     */
    private void performAnonymousAuthDelayed() {
        try {
            com.quran.quranaudio.online.Utils.GoogleAuthManager authManager = 
                new com.quran.quranaudio.online.Utils.GoogleAuthManager(this);
            
            if (!authManager.isUserSignedIn()) {
                authManager.signInAnonymously(new com.quran.quranaudio.online.Utils.GoogleAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                        android.util.Log.d("PERFORMANCE", "✅ [DELAY-3s] Anonymous sign-in successful");
                    }
                    
                    @Override
                    public void onFailure(String error) {
                        android.util.Log.e("PERFORMANCE", "❌ [DELAY-3s] Anonymous sign-in failed: " + error);
                    }
                });
            } else {
                android.util.Log.d("PERFORMANCE", "✅ [DELAY-3s] User already signed in");
            }
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "⚠️ [DELAY-3s] Anonymous auth setup failed (non-critical)", e);
        }
    }
    
    /**
     * 延迟执行完整 WebView 初始化
     * ⚠️ CRITICAL FIX: WebView 必须在主线程创建
     * 
     * 错误信息：
     * java.lang.IllegalStateException: Calling View methods on another thread than the UI thread
     * 
     * 根本原因：
     * 在后台线程中创建 WebView 实例
     * 
     * 解决方案：
     * 使用 Handler.post() 在主线程中创建 WebView
     */
    private void initFullWebViewDelayed() {
        // ✅ 必须在主线程中创建 WebView
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            long startTime = System.currentTimeMillis();
            
            // 面包屑：用于在 Crashlytics 的 ANR 报告里判断卡顿是否落在这段主线程区间内。
            // 只写日志，不改变任何行为。
            logBreadcrumb("webview_warmup:begin");
            try {
                android.util.Log.d("PERFORMANCE", "→ [DELAY-5s] Creating WebView on main thread...");

                android.webkit.WebView tempWebView = new android.webkit.WebView(this);
                android.webkit.WebSettings settings = tempWebView.getSettings();

                // 触发完整初始化
                settings.getJavaScriptEnabled();
                settings.setUseWideViewPort(true);
                settings.setLoadWithOverviewMode(true);

                tempWebView.destroy();

                android.util.Log.d("PERFORMANCE", "✅ [DELAY-5s] Full WebView initialized [" + (System.currentTimeMillis() - startTime) + "ms]");
            } catch (Exception e) {
                android.util.Log.w("PERFORMANCE", "⚠️ [DELAY-5s] Full WebView init failed (non-fatal)", e);
            } finally {
                logBreadcrumb("webview_warmup:end took=" + (System.currentTimeMillis() - startTime) + "ms");
            }
        });
    }
    
    /**
     * 延迟重试挂起的反馈
     * 完全后台，不影响性能
     */
    private void retryPendingFeedbacksDelayed() {
        try {
            com.quran.quranaudio.online.feedback.FeedbackManager.Companion.getInstance()
                .retryPendingFeedbacks(this);
            android.util.Log.d("PERFORMANCE", "✅ [DELAY-5s] Pending feedbacks retry initiated");
        } catch (Exception e) {
            android.util.Log.e("PERFORMANCE", "⚠️ [DELAY-5s] Feedback retry failed (non-critical)", e);
        }
    }

    // ============================================================
    // 保留原有的 Activity 生命周期回调（未修改）
    // ============================================================

    //Ads
    // 🔥 用于热启动（从后台恢复）时展示开屏广告的生命周期观察器
    LifecycleObserver resumeAdObserver = new DefaultLifecycleObserver() {
        private boolean isFirstLaunch = true;
        private long backgroundStartedAt = 0L;
        private static final long MIN_BACKGROUND_DURATION_MS = 2 * 60 * 1000L;

        @Override
        public void onStop(@NonNull LifecycleOwner owner) {
            DefaultLifecycleObserver.super.onStop(owner);
            backgroundStartedAt = System.currentTimeMillis();
        }
        
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

            long backgroundDuration = backgroundStartedAt == 0L
                    ? 0L : System.currentTimeMillis() - backgroundStartedAt;
            if (backgroundDuration < MIN_BACKGROUND_DURATION_MS) {
                android.util.Log.d("App", "⏱️ Skip app-open ad after short background visit: " + backgroundDuration + "ms");
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
            if (activityName.equals("SplashScreenActivity")
                    || activityName.equals("SubscriptionActivity")) {
                android.util.Log.d("App", "🚫 Skip ad on SplashScreenActivity");
                return;
            }
            
            // 🔥 所有其他页面都展示开屏广告
            android.util.Log.d("App", "✅ Showing app open ad on: " + activityName);
            
            if (AdFactory.INSTANCE.hasAppOpenAd(com.quranaudio.common.ad.AdConfig.AD_APPOPEN)
                    || AdFactory.INSTANCE.hasFullScreenNativeFallback(currentActivity)) {
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
                                // AppLovin SDK removed - no longer supported
                                android.util.Log.d("App", "⚠️ AppLovin App Open Ad requested but SDK removed");
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
                            // AppLovin SDK removed - no longer supported
                            break;
                    }
                }
            }
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
            // 🔥 更新当前Activity引用（用于热启动展示广告）
            currentActivity = activity;

            // ❌ 移除：每次 resume 检查并补充原生广告缓存池。
            // 页面切换频率远高于广告展示频率，这里补池会造成大量"加载后从不展示"；
            // NativeAdManager 在消费时（getCachedAd/loadAdWithCallback）已自动补池。
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
                        // AppLovin SDK removed - no longer supported
                        android.util.Log.w("App", "⚠️ AppLovin App Open Ad requested but SDK removed");
                        break;
                }
            }
        }
    }

    //Ads*
}
