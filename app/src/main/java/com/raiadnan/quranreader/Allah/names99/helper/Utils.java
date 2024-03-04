package com.raiadnan.quranreader.Allah.names99.helper;

import android.content.Context;

public class Utils {
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
