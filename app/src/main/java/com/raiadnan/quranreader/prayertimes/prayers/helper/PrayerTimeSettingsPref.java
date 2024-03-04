package com.raiadnan.quranreader.prayertimes.prayers.helper;

import android.content.SharedPreferences;
import com.raiadnan.quranreader.prayertimes.App;
import java.util.HashMap;

public class PrayerTimeSettingsPref {
    public static final String AUTO_EDIT_TIME = "auto_edit_time";
    public static final String AUTO_SETTINGS = "auto_settings";
    public static final String CALCULATION_METHOD = "CalculationMethod";
    public static final String CALCULATION_METHOD_INDEX = "CalculationMethodIndex";
    public static final String CORRECTIONS_ASAR = "corrections_asar";
    public static final String CORRECTIONS_FAJR = "corrections_fajr";
    public static final String CORRECTIONS_ISHA = "corrections_isha";
    public static final String CORRECTIONS_MAGHRIB = "corrections_maghrib";
    public static final String CORRECTIONS_SUNRIZE = "corrections_sunrize";
    public static final String CORRECTIONS_ZUHAR = "corrections_zuhr";
    public static final String DAYLIGHT_SAVING = "DaylightSaving";
    public static final String JURISTIC = "Juristic";
    public static final String JURISTIC_DEFAULT = "Juristic_default";
    public static final String LATITUDE_ADJUSTMENT = "LatitudeAdjustment";
    private static final String PREF_NAME = "NamazTimeettingsPref";
    public static final String VIBRATE = "Vibrate";
    private static PrayerTimeSettingsPref prayerTimeSettingsPref;

    private SharedPreferences pref = App.get().getSharedPreferences(PREF_NAME, 0);
    private SharedPreferences.Editor editor = this.pref.edit();
    public static PrayerTimeSettingsPref get() {
        if (prayerTimeSettingsPref == null) {
            prayerTimeSettingsPref = new PrayerTimeSettingsPref();
        }
        return prayerTimeSettingsPref;
    }

    private PrayerTimeSettingsPref() {
    }

    public void saveSettings(int i, int i2, int i3) {
        this.editor.putInt(JURISTIC, i);
        this.editor.putInt(CALCULATION_METHOD, i2);
        this.editor.putInt(LATITUDE_ADJUSTMENT, i3);
        this.editor.commit();
    }

    public void setJuristicDefault(int i) {
        this.editor.putInt(JURISTIC_DEFAULT, i);
        this.editor.commit();
    }

    public int getJuristicDefault() {
        return this.pref.getInt(JURISTIC_DEFAULT, 2);
    }

    public void setJuristic(int i) {
        this.editor.putInt(JURISTIC, i);
        this.editor.commit();
    }

    public int getJuristic() {
        return this.pref.getInt(JURISTIC, 2);
    }

    public int getCalculationMethod() {
        return this.pref.getInt(CALCULATION_METHOD, 3);
    }

    public void setCalculationMethod(int i) {
        this.editor.putInt(CALCULATION_METHOD, i);
        this.editor.commit();
    }

    public int getCalculationMethodIndex() {
        return this.pref.getInt(CALCULATION_METHOD_INDEX, 4);
    }

    public void setCalculationMethodIndex(int i) {
        this.editor.putInt(CALCULATION_METHOD_INDEX, i);
        this.editor.commit();
    }

    public int getLatdAdjst() {
        return this.pref.getInt(LATITUDE_ADJUSTMENT, 2);
    }

    public void setsLatdAdjst(int i) {
        this.editor.putInt(LATITUDE_ADJUSTMENT, i);
        this.editor.commit();
    }

    public HashMap<String, Integer> getSettings() {
        HashMap<String, Integer> hashMap = new HashMap<>();
        hashMap.put(JURISTIC, Integer.valueOf(this.pref.getInt(JURISTIC, 2)));
        hashMap.put(CALCULATION_METHOD, Integer.valueOf(this.pref.getInt(CALCULATION_METHOD, 3)));
        hashMap.put(LATITUDE_ADJUSTMENT, Integer.valueOf(this.pref.getInt(LATITUDE_ADJUSTMENT, 2)));
        return hashMap;
    }

    public void setDaylightSaving(boolean z) {
        this.editor.putBoolean(DAYLIGHT_SAVING, z);
        this.editor.commit();
    }

    public boolean isDaylightSaving() {
        return this.pref.getBoolean(DAYLIGHT_SAVING, false);
    }

    public void setAutoSettings(boolean z) {
        this.editor.putBoolean(AUTO_SETTINGS, z);
        this.editor.commit();
    }

    public boolean isAutoSettings() {
        return this.pref.getBoolean(AUTO_SETTINGS, true);
    }

    public int getAutoEditTime() {
        return this.pref.getInt(AUTO_EDIT_TIME, 0);
    }

    public void setAutoEditTime(int i) {
        this.editor.putInt(AUTO_EDIT_TIME, i);
        this.editor.commit();
    }

    public void setCorrectionsFajr(int i) {
        this.editor.putInt(CORRECTIONS_FAJR, i);
        this.editor.commit();
    }

    public int getCorrectionsFajr() {
        return this.pref.getInt(CORRECTIONS_FAJR, 0);
    }

    public void setCorrectionsSunrize(int i) {
        this.editor.putInt(CORRECTIONS_SUNRIZE, i);
        this.editor.commit();
    }

    public int getCorrectionsSunrize() {
        return this.pref.getInt(CORRECTIONS_SUNRIZE, 0);
    }

    public void setCorrectionsZuhar(int i) {
        this.editor.putInt(CORRECTIONS_ZUHAR, i);
        this.editor.commit();
    }

    public int getCorrectionsZuhar() {
        return this.pref.getInt(CORRECTIONS_ZUHAR, 0);
    }

    public void setCorrectionsAsar(int i) {
        this.editor.putInt(CORRECTIONS_ASAR, i);
        this.editor.commit();
    }

    public int getCorrectionsAsar() {
        return this.pref.getInt(CORRECTIONS_ASAR, 0);
    }

    public void setCorrectionsMaghrib(int i) {
        this.editor.putInt(CORRECTIONS_MAGHRIB, i);
        this.editor.commit();
    }

    public int getCorrectionsMaghrib() {
        return this.pref.getInt(CORRECTIONS_MAGHRIB, 0);
    }

    public void setCorrectionsIsha(int i) {
        this.editor.putInt(CORRECTIONS_ISHA, i);
        this.editor.commit();
    }

    public int getCorrectionsIsha() {
        return this.pref.getInt(CORRECTIONS_ISHA, 0);
    }

    public void setVibrate(boolean z) {
        this.editor.putBoolean(VIBRATE, z);
        this.editor.commit();
    }

    public boolean getVibrate() {
        return this.pref.getBoolean(VIBRATE, true);
    }

    public void setTone(String str, int i) {
        SharedPreferences.Editor editor2 = this.editor;
        editor2.putInt(str + "_tone", i);
        this.editor.commit();
    }

    public int getTone(String str) {
        SharedPreferences sharedPreferences = this.pref;
        return sharedPreferences.getInt(str + "_tone", 3);
    }

    public void setAdhanReminder(String str, int i) {
        SharedPreferences.Editor editor2 = this.editor;
        editor2.putInt(str + "_reminder", i);
        this.editor.commit();
    }

    public int getAdhanReminder(String str) {
        SharedPreferences sharedPreferences = this.pref;
        return sharedPreferences.getInt(str + "_reminder", 0);
    }

    public void clearStoredData() {
        this.editor.clear();
        this.editor.commit();
    }
}
