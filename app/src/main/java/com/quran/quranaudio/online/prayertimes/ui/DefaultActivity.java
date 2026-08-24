package com.quran.quranaudio.online.prayertimes.ui;

import android.content.Intent;
import android.os.Bundle;

import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.SplashScreenActivity;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;

import javax.inject.Inject;


public class DefaultActivity extends BaseActivity {

    @Inject
    PreferencesHelper preferencesHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((App) getApplicationContext())
                .defaultComponent
                .inject(this);

        super.onCreate(savedInstanceState);

        // All entries share the same splash policy. Splash handles first-launch language
        // initialization and routes directly to Quran without the legacy onboarding gate.
        Intent intent = new Intent(getApplicationContext(), SplashScreenActivity.class);
        startActivity(intent);
        finish();
    }
}
