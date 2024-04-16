package com.quran.quranaudio.online.prayertimes.notifier;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.quran.quranaudio.online.prayertimes.job.WorkCreator;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class AlarmPermissionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (action.equals(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)) {
            WorkCreator.schedulePeriodicPrayerUpdater(context);
        }
    }
}
