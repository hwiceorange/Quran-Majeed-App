package com.quran.quranaudio.online.prayertimes.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceManager;

import com.quran.quranaudio.online.App;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.ui.settings.adhan.AdhanAudioPreference;
import com.quran.quranaudio.online.prayertimes.ui.settings.adhan.AdhanAudioPreferenceDialog;
import com.quran.quranaudio.online.prayertimes.ui.settings.adhan.AdhanReminderPreference;
import com.quran.quranaudio.online.prayertimes.ui.settings.adhan.AdhanReminderPreferenceDialog;
import com.quran.quranaudio.online.prayertimes.ui.settings.common.NumberPickerPreference;
import com.quran.quranaudio.online.prayertimes.ui.settings.common.NumberPickerPreferenceDialog;
import com.quran.quranaudio.online.prayertimes.ui.settings.location.AutoCompleteTextPreference;
import com.quran.quranaudio.online.prayertimes.ui.settings.location.AutoCompleteTextPreferenceDialog;
import com.quran.quranaudio.online.prayertimes.ui.settings.timings.MultipleNumberPickerPreference;
import com.quran.quranaudio.online.prayertimes.ui.settings.timings.MultipleNumberPickerPreferenceDialog;
import com.quran.quranaudio.online.prayertimes.utils.LocaleHelper;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.prayertimes.ui.BaseActivity;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import javax.inject.Inject;


public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String DIALOG_FRAGMENT_TAG = "PreferencesDialogFragment";

    @Inject
    LocaleHelper localeUtils;


    @Override
    public void onAttach(@NonNull Context context) {
        ((App) requireContext().getApplicationContext())
                .appComponent
                .settingsComponent()
                .create()
                .inject(this);

        super.onAttach(context);
    }

    @Override
    public void onCreatePreferencesFix(@Nullable Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);

        Preference preference = getPreferenceScreen().findPreference(PreferencesConstants.THEME_PREFERENCE);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && preference != null) {
            preference.setEnabled(false);
        }

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDivider(new ColorDrawable(Color.TRANSPARENT));
        setDividerHeight(0);
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;

        if (preference instanceof AutoCompleteTextPreference) {
            dialogFragment = new AutoCompleteTextPreferenceDialog((AutoCompleteTextPreference) preference);
        }
        if (preference instanceof NumberPickerPreference) {
            dialogFragment = new NumberPickerPreferenceDialog((NumberPickerPreference) preference);
        }
        if (preference instanceof MultipleNumberPickerPreference) {
            dialogFragment = new MultipleNumberPickerPreferenceDialog((MultipleNumberPickerPreference) preference);
        }
        if (preference instanceof AdhanAudioPreference) {
            dialogFragment = new AdhanAudioPreferenceDialog((AdhanAudioPreference) preference);
        }
        if (preference instanceof AdhanReminderPreference) {
            dialogFragment = new AdhanReminderPreferenceDialog((AdhanReminderPreference) preference);
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);

            dialogFragment.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PreferencesConstants.THEME_PREFERENCE.equals(key)) {
            requireActivity().recreate();
        }

        if (PreferencesConstants.USE_ARABIC_LOCALE.equals(key) || PreferencesConstants.ARABIC_NUMERALS_TYPE.equals(key)) {
            BaseActivity baseActivity = (BaseActivity) requireActivity();
            localeUtils.refreshLocale(requireContext(), baseActivity);
            requireActivity().recreate();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
