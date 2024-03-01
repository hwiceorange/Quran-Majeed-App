package com.quran.quranaudio.online.tasbih.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.FileProvider;


import com.quran.quranaudio.online.App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class Utils {
    public static void startMapByLocal(Context context, String str) {
        try {
            Intent intent = new Intent("android.intent.action.VIEW", Uri.parse("geo:0,0?q=" + str + ""));
            intent.setPackage("com.google.android.apps.maps");
            context.startActivity(intent);
        } catch (Exception unused) {
            Toast.makeText(context, "Google Maps not install!", Toast.LENGTH_SHORT).show();
        }
    }

    public static void vibrator() {
        Vibrator vibrator = (Vibrator) App.get().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(250);
        }
    }

    public static void vibratorLong() {
        Vibrator vibrator = (Vibrator) App.get().getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(3000);
        }
    }

    public static void showKeyboard(final SearchView editText) {
        new Handler().postDelayed(new Runnable() {

            public final void run() {
                editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), 0, (float) editText.getWidth(), 0.0f, 0));
                editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), 1, (float) editText.getWidth(), 0.0f, 0));
            }
        }, 200);
    }


    public static void hideKeyboard(Activity activity) {
        @SuppressLint("ResourceType") View findViewById = activity.findViewById(16908290);
        if (findViewById != null) {
            ((InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(findViewById.getWindowToken(), 0);
        }
    }

    @SuppressLint({"SetWorldReadable"})
    public static void shareBitmap(final Context context, final Bitmap bitmap, final String str,
                                   final App.SimpleCallback simpleCallback) {
        new Thread() {
            public void run() {
                super.run();
                try {
                    File file = new File(context.getCacheDir(), "muslim_calendar");
                    file.mkdirs();
                    FileOutputStream fileOutputStream = new FileOutputStream(file + "/muslim_calendar.png");
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                File file2 = new File(new File(context.getCacheDir(), "muslim_calendar"), "muslim_calendar.png");
            //    Context context = context;
                Uri uriForFile = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file2);
                if (uriForFile != null) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.SEND");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.setDataAndType(uriForFile, context.getContentResolver().getType(uriForFile));
                    intent.putExtra("android.intent.extra.STREAM", uriForFile);
                    context.startActivity(Intent.createChooser(intent, str));
                }
            //    App.SimpleCallback simpleCallback = simpleCallback;
                simpleCallback.callback(simpleCallback);
            }
        }.start();
    }


    public static String milliSecondsToTimer(long j) {
        String str;
        String str2 = "";
        int i = (int) (j / 3600000);
        long j2 = j % 3600000;
        int i2 = ((int) j2) / 60000;
        int i3 = (int) ((j2 % 60000) / 1000);
        if (i > 0) {
            str2 = i + ":";
        }
        if (i3 < 10) {
            str = "0" + i3;
        } else {
            str = "" + i3;
        }
        return str2 + i2 + ":" + str;
    }

    public static String milliSecondsToHourMinute(long j) {
        String str;
        String str2;
        int i = (int) (j / 3600000);
        int i2 = ((int) (j % 3600000)) / 60000;
        if (i > 1) {
            str = i + " hours ";
        } else if (i == 1) {
            str = i + " hour ";
        } else {
            str = "";
        }
        if (i2 > 1) {
            str2 = i2 + " minutes";
        } else {
            str2 = i2 + " minute";
        }
        return str + str2;
    }

    public static String milliSecondsToTimerDown(long j) {
        String str;
        String str2;
        String str3 = "";
        int i = (int) (j / 3600000);
        long j2 = j % 3600000;
        int i2 = ((int) j2) / 60000;
        int i3 = (int) ((j2 % 60000) / 1000);
        if (i > 0) {
            if (i < 10) {
                str3 = "0" + i + ":";
            } else {
                str3 = "" + i + ":";
            }
        }
        if (i2 < 10) {
            str = "0" + i2;
        } else {
            str = "" + i2;
        }
        if (i3 < 10) {
            str2 = "0" + i3;
        } else {
            str2 = "" + i3;
        }
        return str3 + str + ":" + str2;
    }

    public static int getName99Resource(Context context, int i) {
        return context.getResources().getIdentifier("a_" + (i + 1), "raw", context.getPackageName());
    }
}
