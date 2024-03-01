package com.quran.quranaudio.online.prayertimes.notifier;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quran.quranaudio.online.App;

import java.util.Objects;

import javax.inject.Inject;


public class NotifierActionReceiver extends BroadcastReceiver {

    @Inject
    AdhanPlayer adhanPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        ((App) context.getApplicationContext())
                .receiverComponent
                .inject(this);

        String action = intent.getAction();
        int notificationId = intent.getIntExtra("notificationId", 0);

        if (Objects.equals(action, NotifierConstants.ADTHAN_NOTIFICATION_CANCEL_ADHAN_ACTION)) {
            muteAdhanCaller();
            closeNotification(notificationId, context);
        }
    }

    private void closeNotification(int notificationId, Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(notificationId);
    }

    private void muteAdhanCaller() {
        adhanPlayer.stopAdhan();
    }
}
