package com.quran.quranaudio.online.prayertimes.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;

import javax.inject.Inject;



public class NotifierReceiver extends BroadcastReceiver {

    @Inject
    PrayerNotification prayerNotification;


    @Inject
    PreferencesHelper preferencesHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        ((App) context.getApplicationContext())
                .receiverComponent
                .inject(this);

        if(preferencesHelper.isNotificationsEnabled()) {
            prayerNotification.createNotificationChannel();
            prayerNotification.createNotification(intent);
        }
    }
}