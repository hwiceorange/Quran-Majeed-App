package com.quran.quranaudio.online.prayertimes.ui.settings.common;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceDialogFragmentCompat;

/**
 * Author: Rai Adnan
 * Whatsapp: +923002375907
 * Email: officialshaheendevelopers@gmail.com
 * Portfolio: https://codecanyon.net/user/shaheendevelopers/portfolio
 * 
 * 🔧 Fix: Removed preference field to fix "Target fragment must implement TargetFragment interface" crash
 * PreferenceDialogFragmentCompat provides getPreference() method to dynamically get the preference
 * 
 * 🔧 Fix v2: Added public no-arg constructor for Fragment recreation
 */
public class NumberPickerPreferenceDialog extends PreferenceDialogFragmentCompat {

    private NumberPickerView numberPickerView;

    /**
     * ✅ Public no-arg constructor required by Android Fragment framework
     */
    public NumberPickerPreferenceDialog() {
        // Empty constructor - arguments will be restored from savedInstanceState
    }

    /**
     * Factory constructor used when creating the dialog programmatically
     */
    public NumberPickerPreferenceDialog(NumberPickerPreference preference) {
        // ✅ Only set arguments, don't store preference reference
        final Bundle b = new Bundle();
        b.putString(ARG_KEY, preference.getKey());
        setArguments(b);
    }
    
    /**
     * ✅ Helper method to get the preference safely
     */
    private NumberPickerPreference getNumberPickerPreference() {
        return (NumberPickerPreference) getPreference();
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
    protected View onCreateDialogView(@NonNull Context context) {

        numberPickerView = new NumberPickerView(context);

        setPickerInitialValues();

        return numberPickerView;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            updatePreferenceValues();
            // ✅ Get preference dynamically
            getNumberPickerPreference().persist();
        }
    }

    private void setPickerInitialValues() {
        // ✅ Get preference dynamically
        NumberPickerPreference pref = getNumberPickerPreference();
        numberPickerView.setValue(pref.getValue());
        numberPickerView.setMaxValue(pref.getMaxValue());
        numberPickerView.setMinValue(pref.getMinValue());
        numberPickerView.setUnitValue(pref.getUnitValue());
    }

    private void updatePreferenceValues() {
        int numberPickerValue = numberPickerView.getValue();
        // ✅ Get preference dynamically
        getNumberPickerPreference().setValue(numberPickerValue);
    }
}
