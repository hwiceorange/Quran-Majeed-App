package com.quran.quranaudio.online.prayertimes.ui.settings.adhan;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.preference.PreferenceDialogFragmentCompat;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 */
public class AdhanReminderPreferenceDialog extends PreferenceDialogFragmentCompat {

    private final AdhanReminderPreference preference;
    private AdhanReminderView numberPickerView;

    public AdhanReminderPreferenceDialog(AdhanReminderPreference preference) {
        this.preference = preference;

        final Bundle b = new Bundle();
        b.putString(ARG_KEY, preference.getKey());
        setArguments(b);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Nexus 7 needs the keyboard hiding explicitly.
        // A flag on the activity in the manifest doesn't
        // apply to the dialog, so needs to be in code:
        Window window = requireActivity().getWindow();
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    protected View onCreateDialogView(Context context) {

        numberPickerView = new AdhanReminderView(context);

        setPickerInitialValues();

        return numberPickerView;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            updatePreferenceValues();
            preference.persist();
        }
    }

    private void setPickerInitialValues() {
        int adjustment = preference.getAdjustment();
        numberPickerView.setNumberPickerValue(adjustment);
    }

    private void updatePreferenceValues() {
        int numberPickerValue = numberPickerView.getNumberPickerValue();
        preference.setAdjustment(numberPickerValue);
    }
}
