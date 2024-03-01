package com.quran.quranaudio.online.prayertimes.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quran.quranaudio.online.App;

import javax.inject.Inject;


public class NotificationDismissedReceiver extends BroadcastReceiver {

    @Inject
    AdhanPlayer adhanPlayer;

    @Inject
    ReminderPlayer reminderPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        ((App) context.getApplicationContext())
                .receiverComponent
                .inject(this);

        adhanPlayer.stopAdhan();
        reminderPlayer.stop();
    }
}
