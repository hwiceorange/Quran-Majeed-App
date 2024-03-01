package com.quran.quranaudio.online.ads.application;

import android.app.Application;
import android.content.Context;

import androidx.multidex.MultiDex;

@SuppressWarnings("ConstantConditions")
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


}
