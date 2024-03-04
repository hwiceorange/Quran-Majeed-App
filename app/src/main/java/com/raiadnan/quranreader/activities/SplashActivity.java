package com.raiadnan.quranreader.activities;

import static com.shaheendevelopers.ads.sdk.util.Constant.ADMOB;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.github.kayvannj.permission_utils.Func;
import com.github.kayvannj.permission_utils.PermissionUtil;
import com.raiadnan.quranreader.BuildConfig;
import com.raiadnan.quranreader.Constant;
import com.raiadnan.quranreader.R;
import com.raiadnan.quranreader.prayertimes.App;
import com.raiadnan.quranreader.prayertimes.locations.dialog.DialogLocation;
import com.raiadnan.quranreader.prayertimes.locations.dialog.DialogSelectCity;
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave;
import com.shaheendevelopers.ads.sdk.format.AdNetwork;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private View btnGps;
    private int delay = 1000;
    public DialogLocation dialogLocation;
    public DialogSelectCity dialogSelectCity;
    private boolean isRunning;
    public PermissionUtil.PermissionRequestObject mRequestObject;
    private View tvInfo;
    Application application;
    AdNetwork.Initialize adNetwork;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView((int) R.layout.activity_splash);

        initAds();

        if (Constant.AD_NETWORK.equals(ADMOB)) {
            application = getApplication();
            ((App) application).showAdIfAvailable(SplashActivity.this, this::createTimer);
        } else {
            startMain();
        }


        //  FlurryAgent.logEvent(getClass().getSimpleName());
        this.tvInfo = findViewById(R.id.tv_info);
        this.btnGps = findViewById(R.id.bt_use_gps);
        //  LocationSave.putLocation(Double.parseDouble("0.0"), Double.parseDouble("0.0"));
        if (LocationSave.getLat() == 0.0d) {
            this.tvInfo.setVisibility(View.VISIBLE);
            this.btnGps.setVisibility(View.VISIBLE);
            this.btnGps.setOnClickListener(new View.OnClickListener() {
                public final void onClick(View view) {
                    SplashActivity.this.mRequestObject = PermissionUtil.with(SplashActivity.this).request("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION").onAllGranted(new Func() {
                        /* access modifiers changed from: protected */
                        @SuppressLint({"MissingPermission", "SetTextI18n"})
                        public void call() {
                            SplashActivity.this.getLocation();
                        }
                    }).ask(12);
                }
            });
        } else {
            startMain();
        }
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

    private void createTimer() {


    }

    @SuppressLint("WrongConstant")
    public void startMain() {
        this.tvInfo.setVisibility(View.GONE);
        this.btnGps.setVisibility(View.GONE);

        SplashActivity.this.startActivity(new Intent(SplashActivity.this,
                HomeActivity.class).setFlags(335544320));
        SplashActivity.this.finish();
    }

    public void onResume() {
        super.onResume();
        this.isRunning = true;
    }

    /* access modifiers changed from: protected */
    public void onPause() {
        super.onPause();
        this.isRunning = false;
    }

    @SuppressLint({"SetTextI18n"})
    public void getLocation() {
        DialogLocation dialogLocation2 = this.dialogLocation;
        if (dialogLocation2 == null || !dialogLocation2.isShowing()) {
            this.dialogLocation = new DialogLocation(this, new DialogLocation.Callback() {
                public void OnLoctionGetSuccess() {
                    SplashActivity.this.dialogLocation.dismiss();
                    SplashActivity.this.startMain();
                }

                public void OnSelectCityClick() {
                    SplashActivity.this.dialogLocation.dismiss();
                    if (SplashActivity.this.dialogSelectCity == null || !SplashActivity.this.dialogSelectCity.isShowing()) {
                        SplashActivity splashActivity = SplashActivity.this;
                        DialogSelectCity unused = splashActivity.dialogSelectCity = new DialogSelectCity(splashActivity);
                        SplashActivity.this.dialogSelectCity.setOnDismissListener(new DialogInterface.OnDismissListener() {
                            public final void onDismiss(DialogInterface dialogInterface) {
                                SplashActivity.this.startMain();
                            }
                        });
                        SplashActivity.this.dialogSelectCity.show();
                    }
                }
            });
            this.dialogLocation.show();
        }
    }

    public void onRequestPermissionsResult(int i, @NonNull String[] strArr, @NonNull int[] iArr) {
        super.onRequestPermissionsResult(i, strArr, iArr);
        PermissionUtil.PermissionRequestObject permissionRequestObject = this.mRequestObject;
        if (permissionRequestObject != null) {
            permissionRequestObject.onRequestPermissionsResult(i, strArr, iArr);
        }
    }
}
