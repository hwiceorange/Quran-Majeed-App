package com.quran.quranaudio.online.prayertimes.ui;

import android.content.Intent;
import android.os.Bundle;

import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.SplashScreenActivity;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.quran_module.activities.ActivityOnboarding;
import com.quran.quranaudio.online.quran_module.activities.ActivityOnboarding;

import javax.inject.Inject;

/**
 * Author: le cheng
 * Whatsapp: +4407803311518
 * Email: lecheng2019@gmail.com
 */
public class DefaultActivity extends BaseActivity {

    @Inject
    PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((App) getApplicationContext())
                .defaultComponent
                .inject(this);

        super.onCreate(savedInstanceState);

        if (preferencesHelper.isFirstLaunch()) {
            Intent intent = new Intent(getApplicationContext(), ActivityOnboarding.class);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getApplicationContext(), SplashScreenActivity.class);
            startActivity(intent);
        }
    }
}