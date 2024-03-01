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
import androidx.multidex.MultiDexApplication;
import androidx.work.Configuration;
import androidx.work.WorkManager;

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


public class App extends MultiDexApplication {

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
    public ApplicationComponent appComponent = DaggerApplicationComponent
            .builder()
            .appModule(new AppModule(this))
            .build();

    @JvmField
    public ReceiverComponent receiverComponent = DaggerReceiverComponent
            .builder()
            .appModule(new AppModule(this))
            .build();

    @JvmField
    public AdapterComponent adapterComponent = DaggerAdapterComponent
            .builder()
            .appModule(new AppModule(this))
            .build();
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
        super.onCreate();
        //Ads
        if (!Constant.FORCE_TO_SHOW_APP_OPEN_AD_ON_START) {
            registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
            ProcessLifecycleOwner.get().getLifecycle().addObserver(lifecycleObserver);
            appOpenAdMob = new AppOpenAdMob();
            appOpenAdManager = new AppOpenAdManager();
            appOpenAdAppLovin = new AppOpenAdAppLovin();
        }
        //Ads*
        app = this;

        this.faceRobotoL = Typeface.createFromAsset(getAssets(), "Roboto_Light.ttf");
        this.faceRobotoB = Typeface.createFromAsset(getAssets(), "Roboto_Bold.ttf");
        this.faceRobotoR = Typeface.createFromAsset(getAssets(), "Roboto_Regular.ttf");
        this.faceArabic = Typeface.createFromAsset(getAssets(), "XBZarIndoPak.ttf");

        // enable TLS1.1/1.2 for kitkat devices
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            TLSSocketFactoryCompat.setAsDefault();
        }

        CaocConfig
                .Builder
                .create()
                .apply();

        configureWorkManager();

        //QM

        NotificationUtils.INSTANCE.createNotificationChannels((Context)this);
        if (Build.VERSION.SDK_INT >= 28) {
            String process = Application.getProcessName();
            if (Intrinsics.areEqual(this.getPackageName(), process) ^ true) {
                WebView.setDataDirectorySuffix(process);
            }
        }

        //QM*
    }


    private void configureWorkManager() {
        WorkerProviderFactory factory = appComponent.workerProviderFactory();
        Configuration config = new Configuration.Builder()
                .setWorkerFactory(factory)
                .build();

        WorkManager.initialize(this, config);
    }

    //Ads
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
        }

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
            if (Constant.OPEN_ADS_ON_START) {
                if (Constant.AD_STATUS.equals(AD_STATUS_ON)) {
                    switch (Constant.AD_NETWORK) {
                        case ADMOB:
                            if (!Constant.ADMOB_APP_OPEN_AD_ID.equals("0")) {
                                if (!appOpenAdMob.isShowingAd) {
                                    currentActivity = activity;
                                }
                            }
                            break;
                        case GOOGLE_AD_MANAGER:
                            if (!Constant.GOOGLE_AD_MANAGER_APP_OPEN_AD_ID.equals("0")) {
                                if (!appOpenAdManager.isShowingAd) {
                                    currentActivity = activity;
                                }
                            }
                            break;
                        case APPLOVIN:
                        case APPLOVIN_MAX:
                            if (!Constant.APPLOVIN_APP_OPEN_AP_ID.equals("0")) {
                                if (!appOpenAdAppLovin.isShowingAd) {
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
