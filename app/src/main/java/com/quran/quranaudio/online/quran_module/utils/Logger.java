/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */

package com.quran.quranaudio.online.quran_module.utils;

import android.os.Bundle;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.quran.quranaudio.online.BuildConfig;
import com.quran.quranaudio.online.quran_module.utils.univ.StringUtils;

public abstract class Logger {
    private static final String TAG = "QuranAppLogs";

    public static void reportError(@NonNull Throwable throwable) {
    }


    public static void logMsg(String msg) {
    }

    public static void i(@Nullable Object... msgs) {
        Log.i(TAG, prepareLogMsg(msgs));
    }

    public static void d(@Nullable Object... msgs) {
        Log.d(TAG, prepareLogMsg(msgs));
    }

    public static void print(@Nullable Object... msgs) {
        if (BuildConfig.DEBUG) {
            String msg = prepareLogMsg(msgs);
            System.out.println("TRACKING: " + msg);
        }
    }

    private static String prepareLogMsg(@Nullable Object... msgs) {
        StringBuilder sb = new StringBuilder();

        StackTraceElement trc = Thread.currentThread().getStackTrace()[4];
        String className = trc.getClassName();
        className = className.substring(className.lastIndexOf(".") + 1);
        sb.append("(").append(className).append("=>").append(trc.getMethodName()).append(":").append(
            trc.getLineNumber()).append(
            "): ");

        if (msgs != null) {
            int len = msgs.length;
            for (int i = 0; i < len; i++) {
                Object msg = msgs[i];
                if (msg != null) {
                    if (msg instanceof Bundle) {
                        sb.append(StringUtils.bundle2String((Bundle) msg));
                    } else {
                        sb.append(msg);
                    }
                } else sb.append("null");
                if (i < len - 1) sb.append(", ");
            }
        } else sb.append("null");
        return sb.toString();
    }
}
