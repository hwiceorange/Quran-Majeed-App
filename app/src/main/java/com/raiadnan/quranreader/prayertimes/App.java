package com.raiadnan.quranreader.prayertimes;

import static com.shaheendevelopers.ads.sdk.util.Constant.ADMOB;
import static com.shaheendevelopers.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
//import com.flurry.android.FlurryAgent;
import androidx.lifecycle.ProcessLifecycleOwner;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.multidex.MultiDex;

import com.raiadnan.quranreader.Constant;
import com.raiadnan.quranreader.prayertimes.locations.helper.CityDatabase;
import com.shaheendevelopers.ads.sdk.format.AppOpenAdManager;
import com.shaheendevelopers.ads.sdk.format.AppOpenAdMob;
import com.shaheendevelopers.ads.sdk.util.OnShowAdCompleteListener;

public class App extends Application implements Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private static App app;
    public Typeface faceArabic;
    public Typeface faceRobotoB;
    public Typeface faceRobotoL;
    public Typeface faceRobotoR;
    private AppOpenAdMob appOpenAdMob;
    private AppOpenAdManager appOpenAdManager;
    Activity currentActivity;

    public App() {
        app = this;
    }

    public interface SimpleCallback {
        void callback(Object obj);
    }

    public static App get() {
        return app;
    }

    public void onCreate() {
        super.onCreate();
        app = this;
        this.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        appOpenAdMob = new AppOpenAdMob();
        appOpenAdManager = new AppOpenAdManager();
        CityDatabase.get();
        //  startPrayerListen();
        this.faceRobotoL = Typeface.createFromAsset(getAssets(), "Roboto_Light.ttf");
        this.faceRobotoB = Typeface.createFromAsset(getAssets(), "Roboto_Bold.ttf");
        this.faceRobotoR = Typeface.createFromAsset(getAssets(), "Roboto_Regular.ttf");
        this.faceArabic = Typeface.createFromAsset(getAssets(), "XBZarIndoPak.ttf");

        // new FlurryAgent.Builder().withLogEnabled(true).build(this, "5YXS3G3CMRB4SJ5JW3YQ");
    }
    /* public void startPrayerListen() {
     if (Build.VERSION.SDK_INT >= 26) {
         startForegroundService(new Intent(this, PrayerService.class));
     } else {
         startService(new Intent(this, PrayerService.class));
     }
 } */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static synchronized App getInstance() {
        return app;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected void onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        if (Constant.AD_NETWORK.equals(ADMOB)) {
            appOpenAdMob.showAdIfAvailable(currentActivity, Constant.ADMOB_APP_OPEN_AD_ID);
        } else if (Constant.AD_NETWORK.equals(GOOGLE_AD_MANAGER)) {
            appOpenAdManager.showAdIfAvailable(currentActivity, Constant.GOOGLE_AD_MANAGER_APP_OPEN_AD_ID);
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
    }
    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        if (Constant.AD_NETWORK.equals(ADMOB)) {
            if (!appOpenAdMob.isShowingAd) {
                currentActivity = activity;
            }
        } else if (Constant.AD_NETWORK.equals(GOOGLE_AD_MANAGER)) {
            if (!appOpenAdManager.isShowingAd) {
                currentActivity = activity;
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
    public void showAdIfAvailable(@NonNull Activity activity, @NonNull OnShowAdCompleteListener onShowAdCompleteListener) {
        // We wrap the showAdIfAvailable to enforce that other classes only interact with MyApplication
        // class.
        if (Constant.AD_NETWORK.equals(ADMOB)) {
            appOpenAdMob.showAdIfAvailable(activity, Constant.ADMOB_APP_OPEN_AD_ID, onShowAdCompleteListener);
        } else if (Constant.AD_NETWORK.equals(GOOGLE_AD_MANAGER)) {
            appOpenAdManager.showAdIfAvailable(activity, Constant.GOOGLE_AD_MANAGER_APP_OPEN_AD_ID, onShowAdCompleteListener);
        }
    }

}
