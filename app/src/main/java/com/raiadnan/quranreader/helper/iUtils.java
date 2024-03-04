package com.raiadnan.quranreader.helper;


import android.content.Context;

import android.os.Build;


public class iUtils {

    public static String UserAgentsList[] ={"Mozilla/5.0 (Linux; Android 10; SM-A205U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Mobile Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/92.0.4515.107 Safari/537.36",
    "Mozilla/5.0 (Linux; Android 10; SM-A102U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.71 Mobile Safari/537.36",
    "Mozilla/5.0 (iPad; CPU OS 12_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/71.0.3578.77 Mobile/15E148 Safari/605.1"};


    public static boolean hasMarsallow() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }


    public static void ShowToast(Context context, String str) {
        ShowToast(context, str);
    }

    public static void ShowToastError(Context context, String str) {
        ShowToast(context, str);
    }

}
