package com.raiadnan.quranreader.prayertimes.quickaccess;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import com.raiadnan.quranreader.activities.QuickAccessActivity;

public class QuickAccessReceiver extends BroadcastReceiver {
    @SuppressLint("WrongConstant")
    public void onReceive(Context context, Intent intent) {
        if ((intent.getAction() + "").equals("android.intent.action.SCREEN_OFF") && QuickAccessManager.get.isEnable() && !isScreenOn(context)) {
            Intent intent2 = new Intent(context, QuickAccessActivity.class);
            intent2.setFlags(268795908);
            context.startActivity(intent2);
        }
    }

    private boolean isScreenOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= 20) {
            return powerManager.isInteractive();
        }
        return powerManager.isScreenOn();
    }
}
