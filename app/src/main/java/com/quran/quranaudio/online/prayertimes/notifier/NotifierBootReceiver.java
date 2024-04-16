package com.quran.quranaudio.online.prayertimes.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quran.quranaudio.online.prayertimes.job.WorkCreator;

import java.util.Objects;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class NotifierBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (Objects.equals(action, Intent.ACTION_BOOT_COMPLETED)) {
            WorkCreator.schedulePeriodicPrayerUpdater(context);
        }
    }
}
