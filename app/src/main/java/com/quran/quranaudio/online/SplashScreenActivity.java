package com.quran.quranaudio.online;


import static com.raiadnan.ads.sdk.util.Constant.ADMOB;
import static com.raiadnan.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.raiadnan.ads.sdk.util.Constant.APPLOVIN_MAX;
import static com.raiadnan.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.quranaudio.common.ad.AdConfig;
import com.quranaudio.common.ad.AdFactory;
import com.quranaudio.common.ad.AdShowCallback;
import com.quranaudio.common.ad.model.AdItem;
import com.quranaudio.common.ad.model.RewardItem;
import com.raiadnan.ads.sdk.format.AdNetwork;
import com.raiadnan.ads.sdk.format.AppOpenAd;
import com.quran.quranaudio.online.ads.callback.CallbackConfig;
import com.quran.quranaudio.online.ads.data.Constant;
import com.quran.quranaudio.online.ads.database.SharedPref;
import com.quran.quranaudio.online.ads.rest.RestAdapter;
import com.quran.quranaudio.online.BuildConfig;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.ui.MainActivity;
import com.quran.quranaudio.online.quran_module.activities.ActivityOnboarding;
import com.quran.quranaudio.online.quran_module.utils.Log;
import com.quran.quranaudio.quiz.utils.UserInfoUtils;
import com.quran.quranaudio.quiz.utils.AppConfig;
import com.quran.quranaudio.online.activities.OnboardingLoginActivity;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


/**
 * 重构后的 SplashScreenActivity
 * 
 * 🚀 优化要点：
 * 1. 状态机管理：STATE_LOADING → STATE_AD_SHOWING → STATE_COMPLETED
 * 2. 配置加载优化：异步非阻塞，2秒超时，使用缓存
 * 3. 广告生命周期保护：广告展示时禁止跳转
 * 4. 超时策略：5秒总闸，广告加载成功则取消
 * 5. 真实进度条：反映实际加载状态
 */
public class SplashScreenActivity extends AppCompatActivity {

    private static final String TAG = "SplashScreen";
    
    // 状态机
    private enum State {
        STATE_LOADING,      // 加载配置和广告
        STATE_AD_SHOWING,   // 广告展示中
        STATE_COMPLETED     // 已完成，准备跳转
    }
    
    private State currentState = State.STATE_LOADING;
    
    // 超时配置
    private static final int CONFIG_TIMEOUT_MS = 2000;  // 配置加载超时 2 秒
    private static final int MAX_WAIT_TIME_MS = 5000;   // 总闸超时 5 秒
    
    // 标志位
    private boolean hasJumpedToMain = false;
    private boolean isAdShowing = false;
    private boolean isConfigLoaded = false;
    private boolean isAdLoaded = false;
    
    // UI
    private ProgressBar progressBar;
    
    // 工具类
    private Handler handler = new Handler(Looper.getMainLooper());
    private SharedPref sharedPref;
    private Call<CallbackConfig> callbackConfigCall = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force the platform decor/content container to be created before AppCompat installs its
        // sub-decor. Some Android 15/16 builds can otherwise race during a cold launcher start and
        // throw "Window couldn't find content container view" from setContentView().
        getWindow().getDecorView();
        super.onCreate(savedInstanceState);

        long startTime = System.currentTimeMillis();
        android.util.Log.d("PERFORMANCE", "========================================");
        android.util.Log.d("PERFORMANCE", "⚡ SplashScreenActivity.onCreate() START");
        android.util.Log.d("PERFORMANCE", "========================================");

        setContentView(R.layout.activity_splash);

        // Activity/window initialization must finish before SDK initialization or analytics work.
        com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this)
                .logWorkflowStep("splash_start");
        
        sharedPref = new SharedPref(this);
        progressBar = findViewById(R.id.progressbar);
        
        android.util.Log.d("PERFORMANCE", "✅ SplashScreenActivity.onCreate() completed [" + (System.currentTimeMillis() - startTime) + "ms]");
        
        // 启动加载流程
        startLoadingSequence();
    }
    
    /**
     * 启动加载序列
     */
    private void startLoadingSequence() {
        currentState = State.STATE_LOADING;
        updateProgress(0, "Loading...");
        
        // 任务1: 异步加载配置（2秒超时）
        loadConfigAsync();
        
        // 任务2: 先完成 UMP 同意流程，再请求任何广告（避免缺失 TC string）
        AdFactory.gatherConsent(this, canRequestAds -> {
            if (canRequestAds) {
                loadAdAsync();
            } else {
                android.util.Log.w(TAG, "[CONSENT] Ads are not allowed; continuing without ads");
                updateProgress(80, "Privacy choice applied");
                checkAndProceed();
            }

            // 同意界面不计入启动超时，避免用户选择时 Splash 被强制跳转
            startMaxWaitTimeout();
        });
    }
    
    // ============================================================
    // 🔵 配置加载（异步，2秒超时）
    // ============================================================
    
    /**
     * 异步加载配置（2秒超时，失败使用缓存）
     */
    private void loadConfigAsync() {
        android.util.Log.d("PERFORMANCE", "→ [CONFIG] Starting async config load (2s timeout)");
        long configStartTime = System.currentTimeMillis();
        
        // 2秒超时保护
        handler.postDelayed(() -> {
            if (!isConfigLoaded) {
                android.util.Log.w("PERFORMANCE", "⚠️ [CONFIG] Timeout (2s), using cached config");
                isConfigLoaded = true;
                updateProgress(40, "Config timeout, using cache");
                checkAndProceed();
            }
        }, CONFIG_TIMEOUT_MS);
        
        // 异步请求配置（如果需要）
        // requestAPI("https://envato.shaheendevelopers.net/");
        
        // 🔥 暂时直接使用缓存（如果不需要动态配置）
        handler.postDelayed(() -> {
            if (!isConfigLoaded) {
                isConfigLoaded = true;
                android.util.Log.d("PERFORMANCE", "✅ [CONFIG] Config loaded (cached) [" + (System.currentTimeMillis() - configStartTime) + "ms]");
                updateProgress(40, "Config ready");
                checkAndProceed();
            }
        }, 100); // 模拟读取缓存（很快）
    }
    
    /**
     * 请求远程配置（可选）
     */
    private void requestAPI(String url) {
        if (url.startsWith("http://") || url.startsWith("https://")) {
            if (url.contains("https://drive.google.com")) {
                String driveUrl = url.replace("https://", "").replace("http://", "");
                List<String> data = Arrays.asList(driveUrl.split("/"));
                String googleDriveFileId = data.get(3);
                callbackConfigCall = RestAdapter.createApi().getDriveJsonFileId(googleDriveFileId);
            } else {
                callbackConfigCall = RestAdapter.createApi().getJsonUrl(url);
            }
        } else {
            callbackConfigCall = RestAdapter.createApi().getDriveJsonFileId(url);
        }
        
        callbackConfigCall.enqueue(new Callback<CallbackConfig>() {
            @Override
            public void onResponse(@NonNull Call<CallbackConfig> call, @NonNull Response<CallbackConfig> response) {
                CallbackConfig resp = response.body();
                if (resp != null) {
                    sharedPref.savePostList(resp.android);
                    android.util.Log.d(TAG, "✅ [CONFIG] Remote config loaded successfully");
                } else {
                    android.util.Log.w(TAG, "⚠️ [CONFIG] Remote config response null, using cache");
                }
                
                if (!isConfigLoaded) {
                    isConfigLoaded = true;
                    updateProgress(40, "Config ready");
                    checkAndProceed();
                }
            }

            @Override
            public void onFailure(@NonNull Call<CallbackConfig> call, @NonNull Throwable th) {
                android.util.Log.w(TAG, "⚠️ [CONFIG] Remote config failed: " + th.getMessage() + ", using cache");
                
                if (!isConfigLoaded) {
                    isConfigLoaded = true;
                    updateProgress(40, "Config ready (cached)");
                    checkAndProceed();
                }
            }
        });
    }
    
    // ============================================================
    // 🔵 广告加载（异步）
    // ============================================================
    
    /**
     * 异步加载广告
     */
    private void loadAdAsync() {
        android.util.Log.d("PERFORMANCE", "→ [AD] Starting async ad load");
        long adLoadStartTime = System.currentTimeMillis();
        
        String adId = AdConfig.INSTANCE.getAdIdByPosition(AdConfig.AD_APPOPEN);
        android.util.Log.d(TAG, "📱 [AD] Ad ID: " + adId);
        
        AdFactory.INSTANCE.loadAppOpenAd(this, AdConfig.AD_APPOPEN, new com.quranaudio.common.ad.AdLoadCallback() {
            @Override
            public void onAdLoaded(com.quranaudio.common.ad.model.AdItem adItem) {
                isAdLoaded = true;
                android.util.Log.d("PERFORMANCE", "✅ [AD] Ad loaded successfully [" + (System.currentTimeMillis() - adLoadStartTime) + "ms]");
                updateProgress(80, "Ad ready");
                checkAndProceed();
            }

            @Override
            public void onAdFailedToLoad(String adId) {
                android.util.Log.w("PERFORMANCE", "⚠️ [AD] Ad load failed [" + (System.currentTimeMillis() - adLoadStartTime) + "ms]");
                updateProgress(80, "Ad load failed");
                checkAndProceed();
            }
        });
    }
    
    // ============================================================
    // 🔵 状态检查和流程控制
    // ============================================================
    
    /**
     * 检查是否可以继续
     */
    private void checkAndProceed() {
        if (hasJumpedToMain || currentState != State.STATE_LOADING) {
            return;
        }
        
        // 检查是否所有任务完成
        if (isConfigLoaded && (isAdLoaded
                || AdFactory.INSTANCE.hasFullScreenNativeFallback(this)
                || !AdFactory.INSTANCE.hasAppOpenAd(AdConfig.AD_APPOPEN))) {
            // 配置已加载，且（广告已加载 或 没有广告）
            updateProgress(100, "Ready");
            proceedToShowAdOrMain();
        }
    }
    
    /**
     * 继续流程：展示广告或直接跳转主页
     */
    private void proceedToShowAdOrMain() {
        if (hasJumpedToMain) {
            return;
        }
        
        // 取消总闸超时（因为已经准备好了）
        handler.removeCallbacks(maxWaitTimeoutRunnable);
        
        if (AdFactory.INSTANCE.hasAppOpenAd(AdConfig.AD_APPOPEN)
                || AdFactory.INSTANCE.hasFullScreenNativeFallback(this)) {
            // 广告已就绪，展示广告
            showAd();
        } else {
            // 没有广告，直接跳转
            android.util.Log.d(TAG, "⚠️ [AD] No ad available, jumping to main");
            startMainActivity();
        }
    }
    
    /**
     * 展示广告（带保护机制）
     */
    private void showAd() {
        if (hasJumpedToMain || isAdShowing) {
            return;
        }
        
        currentState = State.STATE_AD_SHOWING;
        isAdShowing = true;
        
        android.util.Log.d(TAG, "📱 [AD] Showing app open ad...");
        final long showRequestTime = System.currentTimeMillis();
        
        AdFactory.INSTANCE.showAppOpenAd(this, AdConfig.AD_APPOPEN, new AdShowCallback() {
            private long impressionTime = 0;
            
            @Override
            public void onAdImpression(@Nullable AdItem adItem) {
                impressionTime = System.currentTimeMillis();
                android.util.Log.d(TAG, "📊 [AD] onAdImpression - Ad displayed [" + (impressionTime - showRequestTime) + "ms]");
                updateProgress(100, "Ad showing");
                
                com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(SplashScreenActivity.this)
                    .logAdExposure("open_ad", "splash");
                
                // 🔥 15秒绝对兜底保护（防止广告回调失败）
                handler.postDelayed(() -> {
                    if (isAdShowing && !hasJumpedToMain) {
                        android.util.Log.e(TAG, "❌ [FAILSAFE] Ad stuck for 15s, forcing close");
                        isAdShowing = false;
                        AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
                        startMainActivity();
                    }
                }, 15000);
            }

            @Override
            public void onAdClicked(@Nullable AdItem adItem) {
                android.util.Log.d(TAG, "👆 [AD] onAdClicked");
            }

            @Override
            public void onUserEarnedReward(@Nullable AdItem adItem, @Nullable RewardItem rewardItem) {
                // N/A for app open ads
            }

            @Override
            public void onAdClosed(@Nullable AdItem adItem) {
                long closeTime = System.currentTimeMillis();
                long displayDuration = closeTime - impressionTime;
                android.util.Log.d(TAG, "🔔 [AD] onAdClosed - Display duration: " + displayDuration + "ms");
                
                if (displayDuration < 1000 && impressionTime > 0) {
                    android.util.Log.w(TAG, "⚠️ [AD] Ad closed very quickly (" + displayDuration + "ms) - may be test ad");
                }
                
                isAdShowing = false;
                currentState = State.STATE_COMPLETED;
                
                // 预加载下一个广告（热启动用）
                AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
                
                // 跳转主页
                startMainActivity();
            }

            @Override
            public void onShow(@Nullable AdItem adItem) {
                android.util.Log.d(TAG, "📱 [AD] onShow");
            }

            @Override
            public void onShowFail() {
                android.util.Log.w(TAG, "❌ [AD] onShowFail - Ad failed to show");
                
                isAdShowing = false;
                currentState = State.STATE_COMPLETED;
                
                com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(SplashScreenActivity.this)
                    .logAdLoadFailed("open_ad", "splash", "show_failed");
                
                // 预加载下一个广告
                AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
                
                // 跳转主页
                startMainActivity();
            }
        });
    }
    
    // ============================================================
    // 🔵 超时保护（5秒总闸）
    // ============================================================
    
    private Runnable maxWaitTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (hasJumpedToMain) {
                return;
            }
            
            android.util.Log.w(TAG, "⏱️ [TIMEOUT] Max wait time (5s) reached");
            android.util.Log.w(TAG, "   Config loaded: " + isConfigLoaded);
            android.util.Log.w(TAG, "   Ad loaded: " + isAdLoaded);
            android.util.Log.w(TAG, "   Ad showing: " + isAdShowing);
            
            if (isAdShowing) {
                // 广告正在展示，不能跳转
                android.util.Log.w(TAG, "⚠️ [TIMEOUT] Ad is showing, waiting for ad callback");
                return;
            }
            
            // 超时直接跳转
            updateProgress(100, "Timeout, skipping");
            AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
            startMainActivity();
        }
    };
    
    /**
     * 启动总闸超时保护
     */
    private void startMaxWaitTimeout() {
        handler.postDelayed(maxWaitTimeoutRunnable, MAX_WAIT_TIME_MS);
        android.util.Log.d(TAG, "⏱️ [TIMEOUT] Started 5s max wait timeout");
    }
    
    // ============================================================
    // 🔵 跳转主页（单次保护）
    // ============================================================
    
    /**
     * 跳转主页（单次执行保护）
     */
    private void startMainActivity() {
        if (hasJumpedToMain) {
            android.util.Log.w(TAG, "⚠️ startMainActivity() already called, skipping");
            return;
        }
        
        if (isAdShowing) {
            android.util.Log.e(TAG, "❌ CRITICAL: Attempting to jump while ad is showing! Blocked.");
            return;
        }
        
        hasJumpedToMain = true;
        currentState = State.STATE_COMPLETED;
        
        // 清理所有定时器
        handler.removeCallbacks(maxWaitTimeoutRunnable);
        handler.removeCallbacksAndMessages(null);
        
        android.util.Log.d(TAG, "✅ Jumping to next activity");
        
        // 检查是否首次启动
        android.content.SharedPreferences prefs = getSharedPreferences(
            PreferencesConstants.LOCATION, 
            MODE_PRIVATE
        );
        boolean isFirstLaunch = prefs.getBoolean(
            PreferencesConstants.FIRST_LAUNCH, 
            true
        );
        
        Intent intent;
        if (isFirstLaunch) {
            android.util.Log.d(TAG, "🎯 First launch - Showing Onboarding");
            com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this).logWorkflowStep("onboarding_start");
            intent = new Intent(this, ActivityOnboarding.class);
        } else {
            android.util.Log.d(TAG, "✅ Existing user - Jumping to MainActivity");
            com.quran.quranaudio.online.analytics.AnalyticsManager.getInstance(this).logWorkflowStep("main_activity_start");
            intent = new Intent(this, MainActivity.class);
        }
        
        startActivity(intent);
        finish();
    }
    
    // ============================================================
    // 🔵 进度条更新（真实状态）
    // ============================================================
    
    /**
     * 更新进度条（反映真实加载状态）
     */
    private void updateProgress(int progress, String status) {
        if (progressBar != null) {
            progressBar.setProgress(progress);
        }
        android.util.Log.d("PERFORMANCE", "📊 Progress: " + progress + "% - " + status);
    }
    
    // ============================================================
    // 🔵 生命周期管理
    // ============================================================
    
    @Override
    protected void onPause() {
        super.onPause();
        // 暂停时不清理，因为广告可能正在展示
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 清理所有定时器和回调
        handler.removeCallbacksAndMessages(null);
        
        // 取消网络请求
        if (callbackConfigCall != null) {
            callbackConfigCall.cancel();
        }
    }
}
