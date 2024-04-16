package com.quran.quranaudio.online.prayertimes.ui.settings.method;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesHelper;

import javax.inject.Inject;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class CalculationMethodPreference extends ListPreference {

    @Inject
    PreferencesHelper preferencesHelper;

    public CalculationMethodPreference(Context context, AttributeSet attrs) {
        super(context, attrs);

        ((App) getContext()
                .getApplicationContext())
                .appComponent
                .settingsComponent()
                .create()
                .inject(this);

        OnPreferenceChangeListener onPreferenceChangeListener = (preference, newValue) -> {
            preferencesHelper.updateTimingAdjustmentPreference(String.valueOf(newValue));
            return true;
        };

        setOnPreferenceChangeListener(onPreferenceChangeListener);
    }
}
