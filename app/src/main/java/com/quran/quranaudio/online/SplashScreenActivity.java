package com.quran.quranaudio.online;


import static com.raiadnan.ads.sdk.util.Constant.ADMOB;
import static com.raiadnan.ads.sdk.util.Constant.AD_STATUS_ON;
import static com.raiadnan.ads.sdk.util.Constant.APPLOVIN_MAX;
import static com.raiadnan.ads.sdk.util.Constant.GOOGLE_AD_MANAGER;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

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
import com.quran.quranaudio.online.prayertimes.ui.MainActivity;
import com.quran.quranaudio.online.quran_module.utils.Log;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

public class SplashScreenActivity extends AppCompatActivity {


    private static final String TAG = "ActivitySplash";
    Call<CallbackConfig> callbackConfigCall = null;
    public static int DELAY_PROGRESS = 500;
    AdNetwork.Initialize adNetwork;
    AppOpenAd.Builder appOpenAdBuilder;
    SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        sharedPref = new SharedPref(this);
      //  initAds();

        AdFactory.INSTANCE.loadAppOpenAd(this, AdConfig.AD_APPOPEN,null);
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

        requestConfig();

    }

    Handler handler=new Handler(Looper.getMainLooper());
    int count=0;
    Runnable r=new Runnable() {
        @Override public void run() {
            if(AdFactory.INSTANCE.hasAppOpenAd(AdConfig.AD_APPOPEN)){
               AdFactory.INSTANCE.showAppOpenAd(SplashScreenActivity.this, AdConfig.AD_APPOPEN, new AdShowCallback() {
                   @Override public void onAdImpression(@Nullable AdItem adItem) {

                   }

                   @Override public void onAdClicked(@Nullable AdItem adItem) {

                   }

                   @Override public void onUserEarnedReward(@Nullable AdItem adItem, @Nullable RewardItem rewardItem) {

                   }

                   @Override public void onAdClosed(@Nullable AdItem adItem) {
                       startMainActivity();
                   }

                   @Override public void onShow(@Nullable AdItem adItem) {

                   }

                   @Override public void onShowFail() {

                   }
               });
            } else if(count>=5){
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
            startMainActivity();
        }
    }

    public void startMainActivity() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }, DELAY_PROGRESS);
    }

}
