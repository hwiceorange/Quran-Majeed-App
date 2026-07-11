package com.quran.quranaudio.online.prayertimes.ui;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;
import com.quran.quranaudio.online.prayertimes.timings.calculations.CalculationMethodEnum;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 混合教派国家的一次性"教法学派/计算方法"引导。
 *
 * 背景：伊拉克(约六成什叶)、巴林、黎巴嫩、阿塞拜疆等国是什叶/逊尼混居。此前 App 按国家
 * 默认给逊尼派(如 IQ→Muslim World League)，什叶用户看到的 Maghrib/Fajr 时间明显不对
 * (什叶派日落后约 4°/十几分钟才算 Maghrib)，会立刻判断"时间错了"而卸载——这是这些
 * 市场的留存生死线。但这些国家也有大量逊尼用户，不能粗暴默认成什叶。
 *
 * 正解：首次在祈祷页展示一次教派选择，尊重用户自选，之后可在设置里再改。只对混合教派
 * 国家展示；纯逊尼(沙特/巴基斯坦…)或纯什叶(伊朗，已默认 Tehran)国家不打扰。
 */
public final class SectMethodPromptHelper {

    private static final String TAG = "SectMethodPrompt";
    private static final String PREF_SHOWN_KEY = "sect_method_prompt_shown";

    // 什叶/逊尼混居、且默认易踩错的国家
    private static final List<String> MIXED_SECT_COUNTRIES =
            Arrays.asList("IQ", "BH", "LB", "AZ");

    private SectMethodPromptHelper() {
    }

    /**
     * 条件满足时展示一次教派选择弹窗。
     *
     * @param onMethodChanged 用户选择后触发(用于重算祈祷时间)；不改动时不回调
     */
    public static void maybeShow(Activity activity, PreferencesHelper preferencesHelper,
                                 Runnable onMethodChanged) {
        try {
            if (activity == null || activity.isFinishing()) {
                return;
            }
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(activity);
            if (sp.getBoolean(PREF_SHOWN_KEY, false)) {
                return;
            }

            String country = resolveCountryCode(activity);
            if (country == null || !MIXED_SECT_COUNTRIES.contains(country.toUpperCase(Locale.ENGLISH))) {
                return;
            }

            // 只弹一次(无论用户如何选择/取消)
            sp.edit().putBoolean(PREF_SHOWN_KEY, true).apply();

            showDialog(activity, preferencesHelper, onMethodChanged);
        } catch (Exception e) {
            Log.w(TAG, "maybeShow failed", e);
        }
    }

    private static void showDialog(Activity activity, PreferencesHelper preferencesHelper,
                                   Runnable onMethodChanged) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.sect_prompt_title)
                .setMessage(R.string.sect_prompt_message)
                .setCancelable(true)
                .setPositiveButton(R.string.sect_prompt_sunni, (d, w) ->
                        applyMethod(activity, preferencesHelper,
                                CalculationMethodEnum.MUSLIM_WORLD_LEAGUE, onMethodChanged))
                .setNegativeButton(R.string.sect_prompt_shia, (d, w) ->
                        applyMethod(activity, preferencesHelper,
                                CalculationMethodEnum.SHIA_ITHNA_ANSARI, onMethodChanged))
                .show();
    }

    private static void applyMethod(Context context, PreferencesHelper preferencesHelper,
                                    CalculationMethodEnum method, Runnable onMethodChanged) {
        try {
            // 与设置页改计算方法完全一致的写入方式：ListPreference 键 + 时间微调
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(PreferencesConstants.TIMINGS_CALCULATION_METHOD_PREFERENCE, method.name())
                    .putBoolean(PreferencesConstants.CALCULATION_PREFERENCES_INITIALIZED, true)
                    .apply();
            preferencesHelper.updateTimingAdjustmentPreference(method.name());

            if (onMethodChanged != null) {
                onMethodChanged.run();
            }
            Log.i(TAG, "Calculation method set to " + method.name());
        } catch (Exception e) {
            Log.e(TAG, "applyMethod failed", e);
        }
    }

    private static String resolveCountryCode(Context context) {
        SharedPreferences locationPrefs =
                context.getSharedPreferences(PreferencesConstants.LOCATION, Context.MODE_PRIVATE);
        String countryCode = locationPrefs.getString(PreferencesConstants.LAST_KNOWN_COUNTRY_CODE, null);
        if (countryCode == null || countryCode.trim().isEmpty()) {
            countryCode = Locale.getDefault().getCountry();
        }
        return countryCode;
    }
}
