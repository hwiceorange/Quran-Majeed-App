package com.raiadnan.quranreader.prayertimes.services;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.raiadnan.quranreader.activities.NotificationActivity;
import com.raiadnan.quranreader.activities.SplashActivity;
import com.raiadnan.quranreader.prayertimes.locations.helper.LocationSave;
import com.raiadnan.quranreader.prayertimes.prayers.helper.CalculatePrayerTime;
import com.raiadnan.quranreader.prayertimes.prayers.helper.HGDate;
import com.raiadnan.quranreader.prayertimes.prayers.helper.PrayerTimeSettingsPref;
import com.raiadnan.quranreader.prayertimes.quickaccess.QuickAccessReceiver;
import com.raiadnan.quranreader.prayertimes.utils.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class PrayerService extends Service {
    private SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private QuickAccessReceiver mQuickAccessListen = new QuickAccessReceiver();
    private int nextPostition = 0;
    private BroadcastReceiver timeChange = new BroadcastReceiver() {
        public void onReceive(Context var1, Intent var2) {
            PrayerService.this.check();
        }
    };

    @SuppressLint("WrongConstant")
    private void check() {
        if(LocationSave.getLon() != 0.0D) {
            String var6 = "";
            HGDate var7 = new HGDate();
            var7.toGregorian();
            Date[] var4 = this.getDate(var7, (new CalculatePrayerTime(this)).NamazTimings(var7, LocationSave.getLat(), LocationSave.getLon()));
            Date[] var5 = var4;
            if(System.currentTimeMillis() > var4[var4.length - 1].getTime()) {
                var7.nextDay();
                var7.toGregorian();
                var5 = this.getDate(var7, (new CalculatePrayerTime(this)).NamazTimings(var7, LocationSave.getLat(), LocationSave.getLon()));
                var6 = "(  tomorrow )";
            }

            for(int var1 = 0; var1 < var5.length; ++var1) {
                if(Math.abs(var5[var1].getTime() - System.currentTimeMillis()) < 30000L || var5[var1].getTime() > System.currentTimeMillis()) {
                    this.nextPostition = var1;
                    break;
                }
            }

            String var9 = "";
            switch(this.nextPostition) {
                case 0:
                    var9 = "Fajr";
                    break;
                case 1:
                    var9 = "Sunrise";
                    break;
                case 2:
                    var9 = "Dhuhr";
                    break;
                case 3:
                    var9 = "Asr";
                    break;
                case 4:
                    var9 = "Maghrib";
                    break;
                case 5:
                    var9 = "Isha\'a";
            }

            long var2 = Math.abs(var5[this.nextPostition].getTime() - System.currentTimeMillis());
            if(TextUtils.isEmpty(var6) && var2 < 30000L && PrayerTimeSettingsPref.get().getTone(var9) != 0) {
                Intent var11 = new Intent(this, NotificationActivity.class);
                var11.setFlags(268468224);
                var11.putExtra("key", var9);
                var11.putExtra("time", var5[this.nextPostition].getTime());
                this.startActivity(var11);
            }

            String var12 = Utils.milliSecondsToHourMinute(var2);
            StringBuilder var8 = new StringBuilder(); //prayer name start
            var8.append(var9);
            var8.append(" at ");
            var8.append(this.format.format(var5[this.nextPostition])); //prayer time
          //  var8.append(" ");
            var8.append(var6 );
        //    var8.append("  (â³");
           // var8.append(var12);
          //  var8.append(")");
            var6 = var8.toString();
            updateNotification(this, var6);
            StringBuilder var10 = new StringBuilder();
          //  var10.append(PrayerTimeSettingsPref.get().getAdhanReminder(var9));
          //  var10.append(" minutes");
            if(var12.equals(var10.toString())) {
                this.showNotificationReminder(var9, var6);
            }

        }
    }

    private Date[] getDate(HGDate var1, ArrayList var2) {
        Date[] var6 = new Date[6];

        for(int var3 = 0; var3 < var2.size(); ++var3) {
            String[] var7 = ((String)var2.get(var3)).split(":");
            int var4 = Integer.parseInt(var7[0]);
            int var5 = Integer.parseInt(var7[1]);
            Calendar var8 = Calendar.getInstance();
            var8.set(var1.getYear(), var1.getMonth() - 1, var1.getDay(), var4, var5, 0);
            var6[var3] = var8.getTime();
        }

        return var6;
    }

    @SuppressLint("ResourceType")
    private void showNotification(Service var1) {
        Intent var2 = new Intent(var1, SplashActivity.class);
        var2.addFlags(32768);
        var2.addFlags(268435456);
        @SuppressLint("WrongConstant") PendingIntent var5 = PendingIntent.getActivity(var1, 0, var2, 134217728);
        @SuppressLint("WrongConstant") NotificationManager var3 = (NotificationManager)var1.getSystemService("notification");
        if(var3 != null) {
            if(Build.VERSION.SDK_INT >= 26) {
                @SuppressLint("ResourceType") NotificationChannel var4 = new NotificationChannel("10001", var1.getString(2131755035), 2);
                var4.setSound((Uri)null, (AudioAttributes)null);
                var4.setShowBadge(false);
                var4.setLightColor(ContextCompat.getColor(var1, 2131099703));
                var3.createNotificationChannel(var4);
            }

            NotificationCompat.Builder var6 = new NotificationCompat.Builder(var1.getApplicationContext(), "10001");
            var6.setSmallIcon(2131230917).setAutoCancel(true).setChannelId("10001").setPriority(-1).setContentIntent(var5);
            var6.setContentText("New update release soon.");
            var1.startForeground(1, var6.build());
        }

    }

    private void showNotificationReminder(String var1, String var2) {
        int var5 = PrayerTimeSettingsPref.get().getTone(var1);
        if(var5 != 0) {
            @SuppressLint("WrongConstant") NotificationManager var7 = (NotificationManager)this.getSystemService("notification");
            int var3 = Build.VERSION.SDK_INT;
            byte var4 = 1;
            if(var3 >= 26) {
                byte var10;
                if(var5 == 1) {
                    var10 = 1;
                } else {
                    var10 = 4;
                }

                NotificationChannel var6 = new NotificationChannel("Muslim", "Reminder", var10);
                var6.enableVibration(true);
                var6.setShowBadge(true);
                var6.setLightColor(-16776961);
                var7.createNotificationChannel(var6);
            }

            NotificationCompat.Builder var12 = new NotificationCompat.Builder(this, "Muslim");
            NotificationCompat.Builder var8 = var12.setSmallIcon(2131230917).setDefaults(-1)
                   .setWhen(System.currentTimeMillis());
            byte var11 = var4;
            if(var5 == 1) {
                var11 = -2;
            }

            var8.setPriority(var11);
            Notification var9 = var12.build();
            var9.flags = 16;
            var7.notify(5, var9);
        }
    }

    @SuppressLint("ResourceType")
    public static void updateNotification(Context var0, String var1) {
        Intent var2 = new Intent(var0, SplashActivity.class);
        var2.addFlags(32768);
        var2.addFlags(268435456);
        PendingIntent var5 = PendingIntent.getActivity(var0, 0, var2, 134217728);
        Object var3 = var0.getSystemService("notification");
        if(var3 != null) {
            NotificationManager var6 = (NotificationManager)var3;
            if(Build.VERSION.SDK_INT >= 26) {
                NotificationChannel var4 = new NotificationChannel("10001", var0.getString(2131755035), 2);
                var4.setSound((Uri)null, (AudioAttributes)null);
                var4.setShowBadge(false);
                var4.setLightColor(ContextCompat.getColor(var0, 2131099703));
                var6.createNotificationChannel(var4);
            }

            NotificationCompat.Builder var7 = new NotificationCompat.Builder(var0.getApplicationContext(), "10001");
            var7.setSmallIcon(2131230917).setAutoCancel(true).setChannelId("10001").setPriority(-1).setContentIntent(var5);

            var7.setContentText(var1);
            var6.notify(1, var7.build());
        }

    }

    @Nullable
    public IBinder onBind(Intent var1) {
        return null;
    }

    public void onCreate() {
        super.onCreate();
        IntentFilter var1 = new IntentFilter();
        var1.addAction("android.intent.action.TIME_TICK");
        var1.addAction("android.intent.action.TIME_SET");
        this.registerReceiver(this.timeChange, var1);
        var1 = new IntentFilter();
        var1.addAction("android.intent.action.SCREEN_OFF");
        this.registerReceiver(this.mQuickAccessListen, var1);
    }

    public void onDestroy() {
        super.onDestroy();

        try {
            this.unregisterReceiver(this.timeChange);
        } catch (IllegalArgumentException var3) {
        }

        try {
            this.unregisterReceiver(this.mQuickAccessListen);
        } catch (IllegalArgumentException var2) {
        }

    }

    @SuppressLint("WrongConstant")
    public int onStartCommand(Intent var1, int var2, int var3) {
        this.showNotification(this);
        this.check();
        return 1;
    }

    @SuppressLint("WrongConstant")
    public void onTaskRemoved(Intent var1) {
        super.onTaskRemoved(var1);
        Intent var2 = new Intent(this.getApplicationContext(), this.getClass());
        var2.setPackage(this.getPackageName());
        @SuppressLint("WrongConstant") PendingIntent var3 = PendingIntent.getService(this.getApplicationContext(), 1, var2, 1073741824);
        @SuppressLint("WrongConstant") AlarmManager var4 = (AlarmManager)this.getApplicationContext().getSystemService("alarm");
        if(var4 != null) {
            var4.set(3, SystemClock.elapsedRealtime() + 1000L, var3);
        }

        super.onTaskRemoved(var1);
    }
}
