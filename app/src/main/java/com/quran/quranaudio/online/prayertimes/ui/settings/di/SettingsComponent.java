package com.quran.quranaudio.online.prayertimes.ui.settings.di;

import com.quran.quranaudio.online.prayertimes.ui.settings.location.AutoCompleteTextPreferenceDialog;
import com.quran.quranaudio.online.prayertimes.ui.settings.method.CalculationMethodPreference;
import com.quran.quranaudio.online.prayertimes.ui.settings.SettingsFragment;

import dagger.Subcomponent;


@Subcomponent
public interface SettingsComponent {

    @Subcomponent.Factory
    interface Factory {
        SettingsComponent create();
    }

    void inject(SettingsFragment settingsFragment);

    void inject(AutoCompleteTextPreferenceDialog autoCompleteTextPreferenceDialog);

    void inject(CalculationMethodPreference calculationMethodPreference);
}
