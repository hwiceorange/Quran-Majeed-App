package com.quran.quranaudio.online.compass.helper;

import android.content.SharedPreferences;

import com.quran.quranaudio.online.App;

import java.util.Date;
import java.util.TimeZone;

public class LocationSave {
    public static void putLocation(double d, double d2) {
        SharedPreferences.Editor edit = App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).edit();
        SharedPreferences.Editor putString = edit.putString("lat", d + "");
        putString.putString("lon", d2 + "").apply();
    }

    public static double getLat() {
        return Double.parseDouble(App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).getString("lat", "0"));
    }

    public static double getLon() {
        return Double.parseDouble(App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).getString("lon", "0"));
    }

    public static String getCity() {
        return App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).getString("city", "Hanoi");
    }

    public static void setCity(String str) {
        App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).edit().putString("city", str).apply();
    }

    public static double getTimeZone() {
        return Double.parseDouble(App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).getString("timeZone", String.valueOf(((TimeZone.getDefault().getOffset(new Date().getTime()) / 1000) / 60) / 60)));
    }

    public static void setTimeZone(String str) {
        App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).edit().putString("timeZone", str).apply();
    }

    public static boolean isAutoUpdate() {
        return App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).getBoolean("auto_update", true);
    }

    public static void setAutoUpdate(boolean z) {
        App.get().getSharedPreferences(LocationSave.class.getSimpleName(), 0).edit().putBoolean("auto_update", z).apply();
    }
}
