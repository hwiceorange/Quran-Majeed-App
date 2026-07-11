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

        // 🏠 祈祷到点的精确时刻同步刷新桌面 Widget（"下一番"高亮与倒计时切换），
        // 与宣礼通知同一瞬间发生；无 Widget 时为空操作，异常不影响通知
        com.quran.quranaudio.online.prayertimes.widget.PrayerTimesWidgetProvider
                .requestRefresh(context);
    }
}