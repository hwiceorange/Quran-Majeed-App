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
 * 
 * 🔧 Fix: Removed preference field to fix "Target fragment must implement TargetFragment interface" crash
 * PreferenceDialogFragmentCompat provides getPreference() method to dynamically get the preference
 * 
 * 🔧 Fix v2: Added public no-arg constructor for Fragment recreation
 * Android requires Fragments to have a public no-arg constructor for system recreation (e.g., after configuration change)
 */
public class AdhanReminderPreferenceDialog extends PreferenceDialogFragmentCompat {

    private AdhanReminderView numberPickerView;

    /**
     * ✅ Public no-arg constructor required by Android Fragment framework
     * This is used when the system recreates the dialog (e.g., after screen rotation)
     */
    public AdhanReminderPreferenceDialog() {
        // Empty constructor - arguments will be restored from savedInstanceState
    }

    /**
     * Factory constructor used when creating the dialog programmatically
     */
    public AdhanReminderPreferenceDialog(AdhanReminderPreference preference) {
        // ✅ Only set arguments, don't store preference reference
        final Bundle b = new Bundle();
        b.putString(ARG_KEY, preference.getKey());
        setArguments(b);
    }
    
    /**
     * ✅ Helper method to get the preference safely
     */
    private AdhanReminderPreference getAdhanReminderPreference() {
        return (AdhanReminderPreference) getPreference();
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
            // ✅ Get preference dynamically
            getAdhanReminderPreference().persist();
        }
    }

    private void setPickerInitialValues() {
        // ✅ Get preference dynamically
        int adjustment = getAdhanReminderPreference().getAdjustment();
        numberPickerView.setNumberPickerValue(adjustment);
    }

    private void updatePreferenceValues() {
        int numberPickerValue = numberPickerView.getNumberPickerValue();
        // ✅ Get preference dynamically
        getAdhanReminderPreference().setAdjustment(numberPickerValue);
    }
}
