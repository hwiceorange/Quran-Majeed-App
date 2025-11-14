package com.quran.quranaudio.online.prayertimes.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
import com.quran.quranaudio.online.subscription.SubscriptionActivity;
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

        // 🌐 Setup App Language Preference
        setupAppLanguagePreference();
        
        // 🌟 Setup Premium Subscription Preference
        setupPremiumSubscriptionPreference();

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * 🌐 设置应用语言选择器
     * 显示当前选中的语言，并在用户选择后切换语言并重启应用
     */
    private void setupAppLanguagePreference() {
        androidx.preference.ListPreference appLanguagePref = getPreferenceScreen().findPreference("APP_LANGUAGE_PREFERENCE");
        
        if (appLanguagePref != null) {
            // 获取当前语言代码
            String currentLanguageCode = com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.getLocale(requireContext());
            
            // 设置当前选中的值
            appLanguagePref.setValue(currentLanguageCode);
            
            // 设置摘要显示当前语言名称
            updateLanguageSummary(appLanguagePref, currentLanguageCode);
            
            // 监听语言选择变化
            appLanguagePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String newLanguageCode = (String) newValue;
                    android.util.Log.d("SettingsFragment", "🌐 Language changed to: " + newLanguageCode);
                    
                    // 保存新语言
                    com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs.setLocale(requireContext(), newLanguageCode);
                    
                    // 更新摘要
                    updateLanguageSummary(appLanguagePref, newLanguageCode);
                    
                    // 重启应用以应用新语言
                    requireActivity().recreate();
                    
                    return true;
                }
            });
        }
    }

    /**
     * 🌐 更新语言选择器的摘要文本
     * 显示当前选中的语言名称
     */
    private void updateLanguageSummary(androidx.preference.ListPreference preference, String languageCode) {
        // 从 LanguageManager 获取语言名称
        String languageName = com.quran.quranaudio.online.quran_module.utils.LanguageManager.INSTANCE.getSUPPORTED_LANGUAGES().get(languageCode);
        if (languageName != null) {
            preference.setSummary(languageName);
        } else {
            preference.setSummary("English");
        }
    }

    /**
     * 🌟 设置订阅入口点击事件
     */
    private void setupPremiumSubscriptionPreference() {
        Preference premiumPref = getPreferenceScreen().findPreference("PREMIUM_SUBSCRIPTION_PREFERENCE");
        
        if (premiumPref != null) {
            premiumPref.setOnPreferenceClickListener(preference -> {
                android.util.Log.d("SettingsFragment", "🌟 Premium subscription clicked");
                Intent intent = new Intent(getContext(), SubscriptionActivity.class);
                startActivity(intent);
                return true;
            });
        } else {
            android.util.Log.w("SettingsFragment", "⚠️ Premium subscription preference not found");
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        
        // 🌟 添加订阅入口点击事件
        if (getActivity() != null) {
            View rootView = getActivity().findViewById(R.id.container);
            if (rootView != null) {
                View premiumButton = rootView.findViewById(R.id.btn_premium_subscription);
                if (premiumButton != null) {
                    premiumButton.setOnClickListener(v -> {
                        android.util.Log.d("SettingsFragment", "🌟 Premium subscription button clicked");
                        Intent intent = new Intent(getContext(), SubscriptionActivity.class);
                        startActivity(intent);
                    });
                }
            }
        }
        
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDivider(new ColorDrawable(Color.TRANSPARENT));
        setDividerHeight(0);
        
        // 🌟 设置订阅按钮点击事件（延迟执行，确保布局已加载）
        view.postDelayed(() -> {
            View rootView = getActivity() != null ? getActivity().findViewById(R.id.container) : null;
            if (rootView != null) {
                View premiumButton = rootView.findViewById(R.id.btn_premium_subscription);
                if (premiumButton != null) {
                    premiumButton.setOnClickListener(v -> {
                        android.util.Log.d("SettingsFragment", "🌟 Premium subscription button clicked");
                        Intent intent = new Intent(getContext(), SubscriptionActivity.class);
                        startActivity(intent);
                    });
                } else {
                    android.util.Log.w("SettingsFragment", "⚠️ Premium button not found");
                }
            }
        }, 300);
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
            // Note: setTargetFragment() is deprecated in newer Android versions.
            // PreferenceDialogFragmentCompat automatically finds the parent fragment,
            // so we don't need to set it explicitly.
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
