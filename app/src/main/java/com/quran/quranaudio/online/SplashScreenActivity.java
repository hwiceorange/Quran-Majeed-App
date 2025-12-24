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



public class SplashScreenActivity extends AppCompatActivity {


    private static final String TAG = "ActivitySplash";
    Call<CallbackConfig> callbackConfigCall = null;
    public static int DELAY_PROGRESS = 100;
    AdNetwork.Initialize adNetwork;
    AppOpenAd.Builder appOpenAdBuilder;
    SharedPref sharedPref;

    ProgressBar pbView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        android.util.Log.d("DIAGNOSE", "========================================");
        android.util.Log.d("DIAGNOSE", "SplashScreenActivity.onCreate() START");
        android.util.Log.d("DIAGNOSE", "========================================");
        
        try {
            super.onCreate(savedInstanceState);
            android.util.Log.d("DIAGNOSE", "✅ super.onCreate() completed");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ super.onCreate() FAILED", e);
            throw e;
        }
        
        android.util.Log.d("DIAGNOSE", "→ Setting content view...");
        try {
            setContentView(R.layout.activity_splash);
            android.util.Log.d("DIAGNOSE", "✅ setContentView() completed");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ setContentView() FAILED", e);
            throw e;
        }
        
        android.util.Log.d("DIAGNOSE", "→ Initializing SharedPref...");
        try {
            sharedPref = new SharedPref(this);
            android.util.Log.d("DIAGNOSE", "✅ SharedPref initialized");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ SharedPref initialization FAILED", e);
            throw e;
        }
      //  initAds();

        android.util.Log.d("DIAGNOSE", "→ Finding ProgressBar view...");
        try {
            pbView=findViewById(R.id.progressbar);
            android.util.Log.d("DIAGNOSE", "✅ ProgressBar found: " + (pbView != null));
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ ProgressBar findViewById FAILED", e);
            throw e;
        }
        
        // ⭐ 所有用户（包括新用户首次安装）都展示开屏广告
        android.util.Log.d("DIAGNOSE", "→ Getting AdConfig ad ID...");
        try {
            String adId = AdConfig.INSTANCE.getAdIdByPosition(AdConfig.AD_APPOPEN);
            android.util.Log.d("DIAGNOSE", "✅ Ad ID retrieved: " + adId);
            boolean isTestAd = adId.contains("3940256099942544"); // Google测试广告ID
            android.util.Log.d(TAG, "✅ Loading AppOpen Ad for all users (including first install)");
            android.util.Log.d(TAG, "📱 Ad ID: " + adId);
            android.util.Log.d(TAG, "🧪 Is Test Ad: " + isTestAd);
            android.util.Log.d("DIAGNOSE", "→ Is Test Ad: " + isTestAd);
            if (isTestAd) {
                android.util.Log.w(TAG, "⚠️ Using TEST Ad - test ads may auto-close quickly. Use Release build for production ads.");
            }
            
            android.util.Log.d("DIAGNOSE", "→ Calling AdFactory.loadAppOpenAd()...");
            AdFactory.INSTANCE.loadAppOpenAd(this, AdConfig.AD_APPOPEN,null);
            android.util.Log.d("DIAGNOSE", "✅ AdFactory.loadAppOpenAd() called successfully");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ AdFactory.loadAppOpenAd() FAILED", e);
            throw e;
        }
        
        /*
        if (Constant.AD_STATUS.equals(AD_STATUS_ON) && Constant.OPEN_ADS_ON_START) {
            if (!Constant.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    switch (Constant.AD_NETWORK) {
                        case ADMOB:
                            if (!Constant.ADMOB_APP_OPEN_AD_ID.equals("0")) {
                                ((App) getApplication()).showAdIfAvailable(SplashScreenActivity.this, this::requestConfig);
                            } else {
                                requestConfig();
                            }
                            break;
                        case GOOGLE_AD_MANAGER:
                            if (!Constant.GOOGLE_AD_MANAGER_APP_OPEN_AD_ID.equals("0")) {
                                ((App) getApplication()).showAdIfAvailable(SplashScreenActivity.this, this::requestConfig);
                            } else {
                                requestConfig();
                            }
                            break;
                        case APPLOVIN_MAX:
                            if (!Constant.APPLOVIN_APP_OPEN_AP_ID.equals("0")) {
                                ((App) getApplication()).showAdIfAvailable(SplashScreenActivity.this, this::requestConfig);
                            } else {
                                requestConfig();
                            }
                            break;

                        default:
                            requestConfig();
                            break;
                    }
                }, DELAY_PROGRESS);
            } else {
                requestConfig();
            }
        } else {
            requestConfig();
        }*/

        android.util.Log.d("DIAGNOSE", "→ Calling requestConfig()...");
        try {
            requestConfig();
            android.util.Log.d("DIAGNOSE", "✅ requestConfig() called successfully");
        } catch (Exception e) {
            android.util.Log.e("DIAGNOSE_ERROR", "❌ requestConfig() FAILED", e);
            throw e;
        }
        
        android.util.Log.d("DIAGNOSE", "========================================");
        android.util.Log.d("DIAGNOSE", "✅ SplashScreenActivity.onCreate() COMPLETED");
        android.util.Log.d("DIAGNOSE", "========================================");
    }

    Handler handler=new Handler(Looper.getMainLooper());
    int count=0;
    private boolean hasJumpedToMain = false; // 防止重复跳转
    
    Runnable r=new Runnable() {
        @Override public void run() {
            if(hasJumpedToMain) {
                return; // 已经跳转，不再处理
            }
            
            // ⭐ 所有用户（包括新用户首次安装）都尝试展示开屏广告
            if (AdFactory.INSTANCE.hasAppOpenAd(AdConfig.AD_APPOPEN)){
               final long showRequestTime = System.currentTimeMillis();
               android.util.Log.d(TAG, "✅ AppOpen Ad is ready, requesting to show...");
               AdFactory.INSTANCE.showAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, new AdShowCallback() {
                   private long impressionTime = 0;
                   
                   @Override public void onAdImpression(@Nullable AdItem adItem) {
                       impressionTime = System.currentTimeMillis();
                       progressBarRunning=false;
                       pbView.setProgress(100);
                       
                       // 🔥 修复：不完全取消超时，而是设置15秒绝对兜底保护
                       handler.removeCallbacks(absoluteTimeoutRunnable);
                       handler.removeCallbacks(r);
                       
                       // ⚠️ 绝对兜底保护：即使广告正在展示，15秒后强制跳转（防止SDK回调失败导致卡死）
                       handler.postDelayed(new Runnable() {
                           @Override
                           public void run() {
                               if(!hasJumpedToMain) {
                                   android.util.Log.e(TAG, "❌ [FAILSAFE] Ad did not close after 15s, forcing jump to main");
                                   android.util.Log.e(TAG, "❌ [FAILSAFE] This may indicate: 1) Test ad issue, 2) Ad SDK callback failure, 3) Ad creative issue");
                                   // 🔥 强制跳转也要预加载
                                   AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
                                   startMainActivity();
                               }
                           }
                       }, 15000); // 15秒绝对超时
                       
                       android.util.Log.d(TAG, "📊 [AppOpen] onAdImpression - Ad displayed to user");
                       android.util.Log.d(TAG, "📊 [AppOpen] Time from show request to impression: " + (impressionTime - showRequestTime) + "ms");
                       android.util.Log.d(TAG, "✅ [AppOpen] 15s failsafe timeout set (prevents permanent freeze)");
                   }

                   @Override public void onAdClicked(@Nullable AdItem adItem) {
                       long clickTime = System.currentTimeMillis();
                       android.util.Log.d(TAG, "👆 [AppOpen] onAdClicked - User clicked on ad");
                       android.util.Log.d(TAG, "👆 [AppOpen] Time from impression to click: " + (clickTime - impressionTime) + "ms");
                   }

                   @Override public void onUserEarnedReward(@Nullable AdItem adItem, @Nullable RewardItem rewardItem) {

                   }

                   @Override public void onAdClosed(@Nullable AdItem adItem) {
                       long closeTime = System.currentTimeMillis();
                       long displayDuration = closeTime - impressionTime;
                       // ⭐ 广告只能由用户手动关闭，不会自动关闭
                       // 如果广告快速关闭(<1秒)，可能是测试广告或广告素材问题
                       android.util.Log.d(TAG, "🔔 [AppOpen] onAdClosed - Ad closed");
                       android.util.Log.d(TAG, "🔔 [AppOpen] Display duration: " + displayDuration + "ms");
                       if (displayDuration < 1000 && impressionTime > 0) {
                           android.util.Log.w(TAG, "⚠️ [AppOpen] Ad closed very quickly (" + displayDuration + "ms)!");
                           android.util.Log.w(TAG, "⚠️ [AppOpen] This may be a TEST AD or the ad creative is very short");
                           android.util.Log.w(TAG, "⚠️ [AppOpen] Production ads should require user to manually close");
                       }
                       
                       // 🔥 预加载下一个开屏广告（用于热启动）
                       android.util.Log.d(TAG, "🔄 [AppOpen] Preloading next app open ad for hot start");
                       AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
                       
                       startMainActivity();
                   }

                   @Override public void onShow(@Nullable AdItem adItem) {
                       android.util.Log.d(TAG, "📱 [AppOpen] onShow - Ad show callback triggered");
                       
                       // 🔥 额外保护：在onShow时也取消超时定时器（防止onAdImpression延迟）
                       handler.removeCallbacks(absoluteTimeoutRunnable);
                       handler.removeCallbacks(r);
                       
                       // ⚠️ 绝对兜底保护：即使广告正在展示，15秒后强制跳转（防止SDK回调失败导致卡死）
                       handler.postDelayed(new Runnable() {
                           @Override
                           public void run() {
                               if(!hasJumpedToMain) {
                                   android.util.Log.e(TAG, "❌ [FAILSAFE-onShow] Ad did not close after 15s, forcing jump to main");
                                   // 🔥 强制跳转也要预加载
                                   AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
                                   startMainActivity();
                               }
                           }
                       }, 15000); // 15秒绝对超时
                       
                       android.util.Log.d(TAG, "✅ [AppOpen] 15s failsafe timeout set at onShow");
                   }

                   @Override public void onShowFail() {
                       android.util.Log.w(TAG, "❌ [AppOpen] onShowFail - Ad failed to show");
                       // 🔥 失败后也要预加载，确保热启动有广告
                       android.util.Log.d(TAG, "🔄 [AppOpen] Preloading ad for hot start after failure");
                       AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
                       startMainActivity();
                   }
               });
            } else if(count>=5){ // 🔥 修改：8秒 → 5秒
                android.util.Log.d(TAG, "⏱️ Timeout reached (5s)");
                // 🔥 超时也要预加载，确保热启动有广告
                android.util.Log.d(TAG, "🔄 Preloading ad for hot start after timeout");
                AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
                startMainActivity();
            } else {
                count++;
                handler.removeCallbacks(r);
                handler.postDelayed(r,1000);
            }

        }
    };

    private void requestConfig() {

       // requestAPI("https://envato.shaheendevelopers.net/");
        handler.postDelayed(r,1000);

    }

    private void requestAPI(@SuppressWarnings("SameParameterValue") String url) {
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
            public void onResponse(@NonNull Call<CallbackConfig> call, @NonNull Response<CallbackConfig> response) {
                CallbackConfig resp = response.body();
                if (resp != null) {
                    sharedPref.savePostList(resp.android);
                    loadOpenAds();
                    android.util.Log.d(TAG, "responses success");
                } else {
                    loadOpenAds();
                    android.util.Log.d(TAG, "responses null");
                }
            }

            public void onFailure(@NonNull Call<CallbackConfig> call, @NonNull Throwable th) {
                Log.d(TAG, "responses failed: " + th.getMessage());
                loadOpenAds();
            }
        });
    }

    private void initAds() {
        adNetwork = new AdNetwork.Initialize(this)
                .setAdStatus(Constant.AD_STATUS)
                .setAdNetwork(Constant.AD_NETWORK)
                .setBackupAdNetwork(Constant.BACKUP_AD_NETWORK)
                .setAdMobAppId(null)
                .setStartappAppId(Constant.STARTAPP_APP_ID)
                .setUnityGameId(Constant.UNITY_GAME_ID)
                .setAppLovinSdkKey(getResources().getString(R.string.applovin_sdk_key))
                .setDebug(BuildConfig.DEBUG)
                .build();
    }

    private void loadOpenAds() {
        if (Constant.FORCE_TO_SHOW_APP_OPEN_AD_ON_START && Constant.OPEN_ADS_ON_START) {
            appOpenAdBuilder = new AppOpenAd.Builder(this)
                    .setAdStatus(Constant.AD_STATUS)
                    .setAdNetwork(Constant.AD_NETWORK)
                    .setBackupAdNetwork(Constant.BACKUP_AD_NETWORK)
                    .setAdMobAppOpenId(Constant.ADMOB_APP_OPEN_AD_ID)
                    .setAdManagerAppOpenId(Constant.GOOGLE_AD_MANAGER_APP_OPEN_AD_ID)
                    .setApplovinAppOpenId(Constant.APPLOVIN_APP_OPEN_AP_ID)
                    .build(this::startMainActivity);
        } else {
            // 🔥 不展示广告时也要预加载，确保热启动有广告
            android.util.Log.d(TAG, "🔄 Ad not enabled, preloading for hot start");
            AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
            startMainActivity();
        }
    }

    @SuppressWarnings("deprecation")
    public void startMainActivity() {
        if(hasJumpedToMain) {
            return; // 防止重复跳转
        }
        hasJumpedToMain = true;
        progressBarRunning = false;
        
        // 清理所有定时器
        handler.removeCallbacks(r);
        handler.removeCallbacks(updateProgress);
        handler.removeCallbacks(absoluteTimeoutRunnable);
        
        // ⭐ 检查是否首次启动，决定显示 Onboarding 还是直接进入主界面
        android.content.SharedPreferences prefs = getSharedPreferences(
            com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants.LOCATION, 
            MODE_PRIVATE
        );
        boolean isFirstLaunch = prefs.getBoolean(
            com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants.FIRST_LAUNCH, 
            true
        );
        
        if (isFirstLaunch) {
            // 首次启动：显示语言选择等引导页
            android.util.Log.d(TAG, "🎯 First launch detected - Showing Onboarding (Language Selection)");
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, com.quran.quranaudio.online.quran_module.activities.ActivityOnboarding.class);
                startActivity(intent);
                finish();
            }, DELAY_PROGRESS);
        } else {
            // 老用户：直接进入主界面
            android.util.Log.d(TAG, "✅ Existing user - Jumping directly to MainActivity");
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }, DELAY_PROGRESS);
        }
        
        /* 原登录页面逻辑（已隐藏）
        boolean hasShownLogin = OnboardingLoginActivity.hasShownLoginScreen(this);
        
        if (!hasShownLogin) {
            android.util.Log.d(TAG, "🎯 New user (hasShownLogin=false) - Jumping to OnboardingLoginActivity");
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, OnboardingLoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }, DELAY_PROGRESS);
        } else {
            android.util.Log.d(TAG, "✅ Existing user (hasShownLogin=true) - Jumping to MainActivity");
            new Handler().postDelayed(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }, DELAY_PROGRESS);
        }
        */
    }

    boolean progressBarRunning=true;
    int progressStage=1; // 阶段：1=前2秒，2=后1秒
    int progressCount=0;
    long progressStartTime=0;
    
    // 🔥 新进度条逻辑：3秒到100%（前2秒到85%，后1秒到100%）
    Runnable updateProgress=new Runnable() {
        @Override public void run() {
            if(!progressBarRunning){
                return;
            }
            
            long elapsedTime = System.currentTimeMillis() - progressStartTime;
            int targetProgress = 0;
            long nextDelay = 0;
            
            if (elapsedTime < 2000) {
                // 阶段1：前2秒快速到85%
                // 2000ms达到85%，每约23.5ms更新1%
                targetProgress = (int)(elapsedTime * 85 / 2000);
                if(targetProgress > 85) targetProgress = 85;
                nextDelay = 25; // 每25ms更新一次
                progressStage = 1;
            } else if (elapsedTime < 3000) {
                // 阶段2：后1秒匀速从85%到100%
                // 1000ms从85%到100%（增加15%），每约67ms更新1%
                long stage2Time = elapsedTime - 2000;
                int stage2Progress = (int)(stage2Time * 15 / 1000);
                targetProgress = 85 + stage2Progress;
                if(targetProgress > 100) targetProgress = 100;
                nextDelay = 70; // 每70ms更新一次
                progressStage = 2;
            } else {
                // 3秒后，确保到100%
                targetProgress = 100;
                progressBarRunning = false;
            }
            
            pbView.setProgress(targetProgress);
            
            if(progressBarRunning && targetProgress < 100) {
                handler.removeCallbacks(updateProgress);
                handler.postDelayed(this, nextDelay);
            }
        }
    };
    
    // 🔥 绝对超时保护：新用户3秒，老用户5秒后强制跳转
    Runnable absoluteTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            android.util.Log.w(TAG, "⚠️ Absolute timeout reached, force jumping to main");
            pbView.setProgress(100);
            // 🔥 绝对超时也要预加载
            AdFactory.INSTANCE.loadAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, null);
            startMainActivity();
        }
    };
    @Override
    public void onResume(){
        super.onResume();
        if(progressBarRunning && !hasJumpedToMain) {
            // 重置进度条
            pbView.setProgress(0);
            progressStartTime = System.currentTimeMillis();
            
            // 启动进度条动画
            handler.removeCallbacks(updateProgress);
            handler.postDelayed(updateProgress, 25);
            
            // 🔥 启动绝对超时保护：所有用户统一5秒超时
            int timeoutDuration = 5000;
            handler.removeCallbacks(absoluteTimeoutRunnable);
            handler.postDelayed(absoluteTimeoutRunnable, timeoutDuration);
            
            android.util.Log.d(TAG, "📊 Progress bar started with " + (timeoutDuration/1000) + "s timeout protection for all users");
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        
        // 清理所有定时器
        handler.removeCallbacks(updateProgress);
        handler.removeCallbacks(r);
        handler.removeCallbacks(absoluteTimeoutRunnable);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 清理所有定时器，防止内存泄漏
        handler.removeCallbacks(updateProgress);
        handler.removeCallbacks(r);
        handler.removeCallbacks(absoluteTimeoutRunnable);
    }
}
