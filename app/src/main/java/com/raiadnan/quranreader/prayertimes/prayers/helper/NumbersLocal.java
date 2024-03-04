package com.raiadnan.quranreader.prayertimes.prayers.helper;

import android.content.Context;
import android.content.res.Resources;
import com.thin.downloadmanager.BuildConfig;

public class NumbersLocal {
    public static String convertNumberType(Context context, String str) {
        try {
            if (context.getResources().getConfiguration().locale.getDisplayLanguage().equals("العربية")) {
                return str.replaceAll("0", "٠").replaceAll(BuildConfig.VERSION_NAME, "١").replaceAll("2", "٢").replaceAll("3", "٣").replaceAll("4", "٤").replaceAll("5", "٥").replaceAll("6", "٦").replaceAll("7", "٧").replaceAll("8", "٨").replaceAll("9", "٩");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String convertToNumberTypeSystem(Context context, String str) {
        try {
            if (Resources.getSystem().getConfiguration().locale.getDisplayLanguage().equals("العربية")) {
                return str.replaceAll("0", "٠").replaceAll(BuildConfig.VERSION_NAME, "١").replaceAll("2", "٢").replaceAll("3", "٣").replaceAll("4", "٤").replaceAll("5", "٥").replaceAll("6", "٦").replaceAll("7", "٧").replaceAll("8", "٨").replaceAll("9", "٩");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    public static String convertNumberTypeToEnglish(Context context, String str) {
        try {
            if (context.getResources().getConfiguration().locale.getDisplayLanguage().equals("العربية")) {
                return str.replaceAll("٠", "0").replaceAll("١", BuildConfig.VERSION_NAME).replaceAll("٢", "2").replaceAll("٣", "3").replaceAll("٤", "4").replaceAll("٥", "5").replaceAll("٦", "6").replaceAll("٧", "7").replaceAll("٨", "8").replaceAll("٩", "9");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }
}
