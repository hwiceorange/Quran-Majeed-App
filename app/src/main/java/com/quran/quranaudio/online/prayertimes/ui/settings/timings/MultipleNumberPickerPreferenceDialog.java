package com.quran.quranaudio.online.prayertimes.ui.settings.timings;

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
 */
public class MultipleNumberPickerPreferenceDialog extends PreferenceDialogFragmentCompat {

    private MultipleNumberPickerView multipleNumberPickerView;

    /**
     * ✅ Public no-arg constructor required by Android Fragment framework
     */
    public MultipleNumberPickerPreferenceDialog() {
        // Empty constructor - arguments will be restored from savedInstanceState
    }

    /**
     * Factory constructor used when creating the dialog programmatically
     * 
     * @param preference The MultipleNumberPickerPreference to edit
     */
    public MultipleNumberPickerPreferenceDialog(MultipleNumberPickerPreference preference) {
        // ✅ Only set arguments, don't store preference reference
        // The preference will be retrieved via getPreference() when needed
        final Bundle b = new Bundle();
        b.putString(ARG_KEY, preference.getKey());
        setArguments(b);
    }
    
    /**
     * ✅ Helper method to get the preference safely
     */
    private MultipleNumberPickerPreference getMultipleNumberPickerPreference() {
        return (MultipleNumberPickerPreference) getPreference();
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

        multipleNumberPickerView = new MultipleNumberPickerView(context);
        setPickersInitialValues();

        return multipleNumberPickerView;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            updatePreferenceValues();
            // ✅ Get preference dynamically instead of using stored reference
            getMultipleNumberPickerPreference().persist();
        }
    }

    private void setPickersInitialValues() {
        // ✅ Get preference dynamically
        MultipleNumberPickerPreference pref = getMultipleNumberPickerPreference();
        
        int fajrTimingAdjustment = pref.getFajrTimingAdjustment();
        int dohrTimingAdjustment = pref.getDohrTimingAdjustment();
        int asrTimingAdjustment = pref.getAsrTimingAdjustment();
        int maghrebTimingAdjustment = pref.getMaghrebTimingAdjustment();
        int ichaTimingAdjustment = pref.getIchaTimingAdjustment();

        multipleNumberPickerView.setFajrNumberPickerValue(fajrTimingAdjustment);
        multipleNumberPickerView.setDohrNumberPickerValue(dohrTimingAdjustment);
        multipleNumberPickerView.setAsrNumberPickerValue(asrTimingAdjustment);
        multipleNumberPickerView.setMaghrebNumberPickerValue(maghrebTimingAdjustment);
        multipleNumberPickerView.setIchaNumberPickerValue(ichaTimingAdjustment);
    }

    private void updatePreferenceValues() {
        int fajrNumberPickerValue = multipleNumberPickerView.getFajrNumberPickerValue();
        int dohrNumberPickerValue = multipleNumberPickerView.getDohrNumberPickerValue();
        int asrNumberPickerValue = multipleNumberPickerView.getAsrNumberPickerValue();
        int maghrebNumberPickerValue = multipleNumberPickerView.getMaghrebNumberPickerValue();
        int ichaNumberPickerValue = multipleNumberPickerView.getIchaNumberPickerValue();

        // ✅ Get preference dynamically
        MultipleNumberPickerPreference pref = getMultipleNumberPickerPreference();
        
        pref.setFajrTimingAdjustment(fajrNumberPickerValue);
        pref.setDohrTimingAdjustment(dohrNumberPickerValue);
        pref.setAsrTimingAdjustment(asrNumberPickerValue);
        pref.setMaghrebTimingAdjustment(maghrebNumberPickerValue);
        pref.setIchaTimingAdjustment(ichaNumberPickerValue);
    }
}
