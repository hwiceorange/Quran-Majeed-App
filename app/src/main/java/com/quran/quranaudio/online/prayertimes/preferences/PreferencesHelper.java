package com.quran.quranaudio.online.prayertimes.preferences;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Address;
import android.media.AudioManager;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.timings.calculations.CalculationMethodEnum;
import com.quran.quranaudio.online.prayertimes.timings.calculations.CountryCalculationMethod;
import com.quran.quranaudio.online.prayertimes.timings.calculations.LatitudeAdjustmentMethod;
import com.quran.quranaudio.online.prayertimes.timings.calculations.MidnightModeAdjustmentMethod;
import com.quran.quranaudio.online.prayertimes.timings.calculations.SchoolAdjustmentMethod;
import com.quran.quranaudio.online.prayertimes.timings.calculations.TimingsTuneEnum;
import com.quran.quranaudio.online.prayertimes.utils.UiUtils;
import com.quran.quranaudio.online.prayertimes.utils.UserPreferencesUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class PreferencesHelper {

    public static final String TYPE_NONE = "none";
    public static final String TYPE_AZAN = "azan";
    public static final String TYPE_VIBRATE = "vibrate";
    public static final String TYPE_SILENT = "silent";
    public static final String TYPE_TEXT_TONE = "text_tone";
    public static final String TYPE_CLOCK = "clock";

    private final Context context;

    @Inject
    public PreferencesHelper(Context context) {
        this.context = context;
    }

    public void setFirstTimeLaunch(boolean isFirstTime) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.LOCATION, MODE_PRIVATE);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putBoolean(PreferencesConstants.FIRST_LAUNCH, isFirstTime);

        edit.apply();
    }

    public boolean isFirstLaunch() {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.LOCATION, MODE_PRIVATE);
        return sharedPreferences.getBoolean(PreferencesConstants.FIRST_LAUNCH, true);
    }

    public CalculationMethodEnum getCalculationMethod() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String timingsCalculationMethodId = defaultSharedPreferences.getString(PreferencesConstants.TIMINGS_CALCULATION_METHOD_PREFERENCE, String.valueOf(CalculationMethodEnum.getDefault()));

        return CalculationMethodEnum.valueOf(timingsCalculationMethodId);
    }

    public String getTune() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.TIMING_ADJUSTMENT, Context.MODE_PRIVATE);

        int fajrTimingAdjustment = sharedPreferences.getInt(PreferencesConstants.FAJR_TIMING_ADJUSTMENT, 0);
        int dohrTimingAdjustment = sharedPreferences.getInt(PreferencesConstants.DOHR_TIMING_ADJUSTMENT, 0);
        int asrTimingAdjustment = sharedPreferences.getInt(PreferencesConstants.ASR_TIMING_ADJUSTMENT, 0);
        int maghrebTimingAdjustment = sharedPreferences.getInt(PreferencesConstants.MAGHREB_TIMING_ADJUSTMENT, 0);
        int ichaTimingAdjustment = sharedPreferences.getInt(PreferencesConstants.ICHA_TIMING_ADJUSTMENT, 0);

        return fajrTimingAdjustment + "," + fajrTimingAdjustment + ",0," + dohrTimingAdjustment + "," + asrTimingAdjustment + "," + maghrebTimingAdjustment + ",0," + ichaTimingAdjustment + ",0";
    }

    public Map<String, Integer> getTuneMap() {
        HashMap<String, Integer> map = new HashMap<>();
        SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.TIMING_ADJUSTMENT, Context.MODE_PRIVATE);

        int fajrTimingAdjustment = sharedPreferences.getInt(PreferencesConstants.FAJR_TIMING_ADJUSTMENT, 0);
        int dohrTimingAdjustment = sharedPreferences.getInt(PreferencesConstants.DOHR_TIMING_ADJUSTMENT, 0);
        int asrTimingAdjustment = sharedPreferences.getInt(PreferencesConstants.ASR_TIMING_ADJUSTMENT, 0);
        int maghrebTimingAdjustment = sharedPreferences.getInt(PreferencesConstants.MAGHREB_TIMING_ADJUSTMENT, 0);
        int ichaTimingAdjustment = sharedPreferences.getInt(PreferencesConstants.ICHA_TIMING_ADJUSTMENT, 0);

        map.put(PreferencesConstants.FAJR_TIMING_ADJUSTMENT, fajrTimingAdjustment);
        map.put(PreferencesConstants.DOHR_TIMING_ADJUSTMENT, dohrTimingAdjustment);
        map.put(PreferencesConstants.ASR_TIMING_ADJUSTMENT, asrTimingAdjustment);
        map.put(PreferencesConstants.MAGHREB_TIMING_ADJUSTMENT, maghrebTimingAdjustment);
        map.put(PreferencesConstants.ICHA_TIMING_ADJUSTMENT, ichaTimingAdjustment);

        return map;
    }

    public int getHijriAdjustment() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        return defaultSharedPreferences.getInt(PreferencesConstants.HIJRI_DAY_ADJUSTMENT_PREFERENCE, 0);
    }

    public LatitudeAdjustmentMethod getLatitudeAdjustmentMethod() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String latitudeAdjustmentMethod = defaultSharedPreferences.getString(PreferencesConstants.TIMINGS_LATITUDE_ADJUSTMENT_METHOD_PREFERENCE, LatitudeAdjustmentMethod.getDefault().toString());

        return LatitudeAdjustmentMethod.valueOf(latitudeAdjustmentMethod);
    }

    public SchoolAdjustmentMethod getSchoolAdjustmentMethod() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String schoolAdjustmentMethod = defaultSharedPreferences.getString(PreferencesConstants.SCHOOL_ADJUSTMENT_METHOD_PREFERENCE, SchoolAdjustmentMethod.getDefault().toString());

        return SchoolAdjustmentMethod.valueOf(schoolAdjustmentMethod);
    }

    public MidnightModeAdjustmentMethod getMidnightModeAdjustmentMethod() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String midnightModeAdjustmentMethod = defaultSharedPreferences.getString(PreferencesConstants.MIDNIGHT_MODE_ADJUSTMENT_METHOD_PREFERENCE, MidnightModeAdjustmentMethod.getDefault().toString());

        return MidnightModeAdjustmentMethod.valueOf(midnightModeAdjustmentMethod);
    }

    @NonNull
    public Address getLastKnownAddress() {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.LOCATION, MODE_PRIVATE);
        final String locality = sharedPreferences.getString(PreferencesConstants.LAST_KNOWN_LOCALITY, null);
        final String country = sharedPreferences.getString(PreferencesConstants.LAST_KNOWN_COUNTRY, null);
        final double latitude = UserPreferencesUtils.getDouble(sharedPreferences, PreferencesConstants.LAST_KNOWN_LATITUDE, 0);
        final double longitude = UserPreferencesUtils.getDouble(sharedPreferences, PreferencesConstants.LAST_KNOWN_LONGITUDE, 0);

        Address address = new Address(Locale.getDefault());
        address.setCountryName(country);
        address.setLocality(locality);
        address.setLatitude(latitude);
        address.setLongitude(longitude);

        return address;
    }

    public void updateTimingAdjustmentPreference(String methodName) {
        if (!isCalculationPreferenceInitialized()) {
            TimingsTuneEnum timingsTuneEnum = TimingsTuneEnum.getValueByName(methodName);

            SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.TIMING_ADJUSTMENT, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putInt(PreferencesConstants.FAJR_TIMING_ADJUSTMENT, timingsTuneEnum.getFajr());
            editor.putInt(PreferencesConstants.DOHR_TIMING_ADJUSTMENT, timingsTuneEnum.getDhuhr());
            editor.putInt(PreferencesConstants.ASR_TIMING_ADJUSTMENT, timingsTuneEnum.getAsr());
            editor.putInt(PreferencesConstants.MAGHREB_TIMING_ADJUSTMENT, timingsTuneEnum.getMaghrib());
            editor.putInt(PreferencesConstants.ICHA_TIMING_ADJUSTMENT, timingsTuneEnum.getIsha());

            editor.apply();
        }
    }

    public void updateAddressPreferences(Address address) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences(PreferencesConstants.LOCATION, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PreferencesConstants.LAST_KNOWN_LOCALITY, address.getLocality());
        editor.putString(PreferencesConstants.LAST_KNOWN_COUNTRY, address.getCountryName());
        editor.putString(PreferencesConstants.LAST_KNOWN_COUNTRY_CODE, address.getCountryCode());
        editor.putString(PreferencesConstants.LAST_KNOWN_STATE, address.getAddressLine(1));

        UserPreferencesUtils.putDouble(editor, PreferencesConstants.LAST_KNOWN_LATITUDE, address.getLatitude());
        UserPreferencesUtils.putDouble(editor, PreferencesConstants.LAST_KNOWN_LONGITUDE, address.getLongitude());
        editor.apply();

        CalculationMethodEnum calculationMethodByAddress = CountryCalculationMethod.getCalculationMethodByAddress(address);

        updateCalculationMethodPreferenceByAddress(String.valueOf(calculationMethodByAddress));
        updateTimingAdjustmentPreference(String.valueOf(calculationMethodByAddress));

        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor defaultEditor = defaultSharedPreferences.edit();

        if (address.getLocality() != null && address.getCountryName() != null) {
            defaultEditor.putString(PreferencesConstants.LOCATION_PREFERENCE, address.getLocality() + ", " + address.getCountryName());
        } else {
            defaultEditor.putString(PreferencesConstants.LOCATION_PREFERENCE, address.getLatitude() + ", " + address.getLongitude());
        }

        defaultEditor.putBoolean(PreferencesConstants.CALCULATION_PREFERENCES_INITIALIZED, true);
        defaultEditor.apply();
    }

    public void ensureDefaultCalculationMethod() {
        if (isCalculationPreferenceInitialized()) {
            return;
        }

        SharedPreferences locationPrefs = context.getSharedPreferences(PreferencesConstants.LOCATION, MODE_PRIVATE);
        String countryCode = locationPrefs.getString(PreferencesConstants.LAST_KNOWN_COUNTRY_CODE, null);

        if (countryCode == null || countryCode.trim().isEmpty()) {
            countryCode = Locale.getDefault().getCountry();
        }

        if (countryCode == null || countryCode.trim().isEmpty()) {
            return;
        }

        CalculationMethodEnum calculationMethodEnum = CountryCalculationMethod.getCalculationMethodByCountryCode(countryCode);

        updateCalculationMethodPreferenceByAddress(calculationMethodEnum.name());
        updateTimingAdjustmentPreference(calculationMethodEnum.name());

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.edit()
                .putBoolean(PreferencesConstants.CALCULATION_PREFERENCES_INITIALIZED, true)
                .apply();
    }

    public boolean isLocationSetManually() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.LOCATION_SET_MANUALLY_PREFERENCE, false);
    }

    public void setNightModeActivated(boolean activated) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = defaultSharedPreferences.edit();
        edit.putBoolean(PreferencesConstants.QURAN_NIGHT_MODE_ACTIVATED, activated);
        edit.apply();
    }

    public boolean isNightModeActivated() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.QURAN_NIGHT_MODE_ACTIVATED, false);
    }

    public Boolean isVibrationActivated() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.ADHAN_VIBRATION_PREFERENCE, true);
    }

    public String getFajrAdhanCaller() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getString(PreferencesConstants.ADTHAN_FAJR_CALLER, UiUtils.uriFromRaw(PreferencesConstants.SHORT_PRAYER_CALL, context).toString());
    }

    public String getAdhanCaller() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getString(PreferencesConstants.ADTHAN_CALLER, UiUtils.uriFromRaw(PreferencesConstants.SHORT_PRAYER_CALL, context).toString());
    }

    public boolean isDouaeAfterAdhanEnabled() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.DOUAE_AFTER_ADHAN_PREFERENCE, true);
    }

    public boolean isDohaReminderEnabled() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.DOHA_TIMING_REMINDER_ENABLED, false);
    }

    public boolean isLastThirdOfTheNightReminderEnabled() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.LAST_THIRD_OF_THE_NIGHT_TIMING_REMINDER_ENABLED, false);
    }

    public boolean isReminderEnabled() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.ADTHAN_REMINDER_ENABLED, true);
    }

    public boolean isReminderCallEnabled() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.ADTHAN_REMINDER_CALL_ENABLED, false);
    }

    public int getReminderInterval() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getInt(PreferencesConstants.ADTHAN_REMINDER_INTERVAL, 10);
    }

    private boolean isCalculationPreferenceInitialized() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.CALCULATION_PREFERENCES_INITIALIZED, false);
    }

    private void updateCalculationMethodPreferenceByAddress(String methodName) {
        if (!isCalculationPreferenceInitialized()) {
            final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor defaultEditor = defaultSharedPreferences.edit();
            defaultEditor.putString(PreferencesConstants.TIMINGS_CALCULATION_METHOD_PREFERENCE, methodName);
            defaultEditor.apply();
        }
    }

    public int getThemePreferenceId() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String preferencesString = defaultSharedPreferences.getString(PreferencesConstants.THEME_PREFERENCE, PreferencesConstants.THEME_PREFERENCE_NAME_THEME_WHITE_BLUE);

        switch (preferencesString) {
            case PreferencesConstants.THEME_PREFERENCE_NAME_THEME_WHITE_BLUE:
            default:
                return R.style.PrayerTimes;
        }
    }

    public String getThemePreference() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getString(PreferencesConstants.THEME_PREFERENCE, PreferencesConstants.THEME_PREFERENCE_NAME_THEME_WHITE_BLUE);
    }

    public boolean useArabicLocale() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.USE_ARABIC_LOCALE, false);
    }

    public String getArabicNumeralsType() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getString(PreferencesConstants.ARABIC_NUMERALS_TYPE, PreferencesConstants.ARABIC_NUMERALS_TYPE_ARABIC);
    }

    public boolean isSilenterEnabled() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.SILENTER_ENABLED, false);
    }

    public void savePreviousRingerModeBeforeSilent(int ringerMode) {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = defaultSharedPreferences.edit();
        edit.putInt(PreferencesConstants.PREVIOUS_RINGER_MODE_BEFORE_SILENT, ringerMode);
        edit.apply();
    }

    public int getPreviousRingerModeBeforeSilent() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getInt(PreferencesConstants.PREVIOUS_RINGER_MODE_BEFORE_SILENT, AudioManager.RINGER_MODE_NORMAL);
    }

    public int getSilenterStartTime() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getInt(PreferencesConstants.SILENT_START_TIME, 5);
    }

    public int getSilenterInterval() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getInt(PreferencesConstants.SILENT_TIME_INTERVAL, 15);
    }

    public int getSilenterIntervalForFridayPrayer() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getInt(PreferencesConstants.SILENT_TIME_INTERVAL_FOR_FRIDAY_PRAYER, 45);
    }

    public boolean isNotificationsEnabled() {
        final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return defaultSharedPreferences.getBoolean(PreferencesConstants.NOTIFICATIONS_ENABLED, true);
    }

    // ========================================
    // 🆕 每个祷告独立配置的读取方法（支持新通知设置页面）
    // ========================================

    /**
     * 获取祷告的通知类型（每个祷告独立配置）
     * @param prayer 祷告枚举
     * @return 通知类型："none", "azan", "vibrate", "silent", "text_tone", "clock"
     */
    public String getNotificationTypeForPrayer(PrayerEnum prayer) {
        final SharedPreferences prayerPrefs = context.getSharedPreferences(
                PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        
        // 读取独立配置
        String notificationType = prayerPrefs.getString(prayer + "_NOTIFICATION_TYPE", null);
        
        if (notificationType != null) {
            android.util.Log.d("PreferencesHelper", "✅ " + prayer + " notification type: " + notificationType);
            return notificationType;
        }
        
        // 回退：检查旧的开关配置
        boolean callEnabled = prayerPrefs.getBoolean(
                prayer + PreferencesConstants.ADTHAN_CALL_ENABLED_KEY, false);
        
        android.util.Log.d("PreferencesHelper", "⚠️ " + prayer + " using legacy config, callEnabled: " + callEnabled);
        return callEnabled ? TYPE_AZAN : TYPE_NONE;
    }

    /**
     * 检查祷告是否启用震动（每个祷告独立配置）
     * @param prayer 祷告枚举
     * @return true 如果该祷告应该震动
     */
    public boolean isVibrationEnabledForPrayer(PrayerEnum prayer) {
        String notificationType = getNotificationTypeForPrayer(prayer);
        
        // 震动类型直接返回 true
        if (TYPE_VIBRATE.equals(notificationType)) {
            android.util.Log.d("PreferencesHelper", "✅ " + prayer + " vibration enabled (type=vibrate)");
            return true;
        }
        
        // 其他类型：回退到全局震动配置
        boolean globalVibration = isVibrationActivated();
        android.util.Log.d("PreferencesHelper", "⚠️ " + prayer + " using global vibration: " + globalVibration);
        return globalVibration;
    }

    /**
     * 检查祷告是否启用预提醒（每个祷告独立配置）
     * @param prayer 祷告枚举
     * @return true 如果该祷告启用了预提醒
     */
    public boolean isPreReminderEnabledForPrayer(PrayerEnum prayer) {
        final SharedPreferences prayerPrefs = context.getSharedPreferences(
                PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        
        // 读取独立配置
        boolean preReminderEnabled = prayerPrefs.getBoolean(prayer + "_PRE_REMINDER", false);

        // 如果通知类型为 none，则认为预提醒关闭
        if (TYPE_NONE.equals(getNotificationTypeForPrayer(prayer))) {
            android.util.Log.d("PreferencesHelper", "📅 " + prayer + " pre-reminder disabled because notification type is none");
            return false;
        }

        android.util.Log.d("PreferencesHelper", "📅 " + prayer + " pre-reminder enabled: " + preReminderEnabled);
        return preReminderEnabled;
    }

    /**
     * 获取祷告的预提醒时间（分钟）（每个祷告独立配置）
     * @param prayer 祷告枚举
     * @return 预提醒分钟数（1-30），如果未配置则回退到全局配置
     */
    public int getPreReminderMinutesForPrayer(PrayerEnum prayer) {
        final SharedPreferences prayerPrefs = context.getSharedPreferences(
                PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        
        // 读取独立配置
        int minutes = prayerPrefs.getInt(prayer + "_PRE_REMINDER_MINUTES", 0);
        
        // 如果没有独立配置，回退到全局配置
        if (minutes == 0) {
            minutes = getReminderInterval();
            android.util.Log.d("PreferencesHelper", "⚠️ " + prayer + " using global reminder interval: " + minutes + " minutes");
        } else {
            android.util.Log.d("PreferencesHelper", "✅ " + prayer + " pre-reminder minutes: " + minutes);
        }
        
        return minutes;
    }

    /**
     * 获取祷告的音量（每个祷告独立配置）
     * @param prayer 祷告枚举
     * @return 音量（0-100）
     */
    public int getVolumeForPrayer(PrayerEnum prayer) {
        final SharedPreferences prayerPrefs = context.getSharedPreferences(
                PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, Context.MODE_PRIVATE);
        
        // 读取独立配置，默认 80
        int volume = prayerPrefs.getInt(prayer + "_VOLUME", 80);
        
        android.util.Log.d("PreferencesHelper", "🔊 " + prayer + " volume: " + volume);
        return volume;
    }

}
