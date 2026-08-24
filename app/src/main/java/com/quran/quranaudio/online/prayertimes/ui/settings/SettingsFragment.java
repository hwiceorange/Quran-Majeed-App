package com.quran.quranaudio.online.prayertimes.ui.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.quran.quranaudio.online.Utils.GoogleAuthManager;
import com.quran.quranaudio.online.account.AccountDeletionManager;
import com.google.firebase.auth.FirebaseAuth;
import com.takisoft.preferencex.PreferenceFragmentCompat;

import javax.inject.Inject;


public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String DIALOG_FRAGMENT_TAG = "PreferencesDialogFragment";

    @Inject
    LocaleHelper localeUtils;

    private GoogleAuthManager googleAuthManager;
    private AlertDialog deletionProgressDialog;
    private final ActivityResultLauncher<Intent> deletionAuthLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() != Activity.RESULT_OK || googleAuthManager == null) {
                    return;
                }
                googleAuthManager.reauthenticateCurrentUser(result.getData(), new GoogleAuthManager.AuthCallback() {
                    @Override public void onSuccess(com.google.firebase.auth.FirebaseUser user) {
                        performAccountDeletion();
                    }

                    @Override public void onFailure(String error) {
                        showDeletionError(error);
                    }
                });
            });


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
        setupAccountAndSubscriptionPreferences();

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    private void setupAccountAndSubscriptionPreferences() {
        Preference manageSubscription = getPreferenceScreen().findPreference("MANAGE_SUBSCRIPTION_PREFERENCE");
        if (manageSubscription != null) {
            manageSubscription.setOnPreferenceClickListener(preference -> {
                Uri uri = Uri.parse("https://play.google.com/store/account/subscriptions?package="
                        + requireContext().getPackageName());
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            });
        }

        Preference deleteAccount = getPreferenceScreen().findPreference("DELETE_ACCOUNT_PREFERENCE");
        if (deleteAccount != null) {
            deleteAccount.setVisible(FirebaseAuth.getInstance().getCurrentUser() != null);
            deleteAccount.setOnPreferenceClickListener(preference -> {
                showDeleteAccountConfirmation();
                return true;
            });
        }
    }

    private void showDeleteAccountConfirmation() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_account_dialog_title)
                .setMessage(R.string.delete_account_dialog_message)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.delete_account_confirm, (dialog, which) -> beginVerifiedDeletion())
                .show();
    }

    private void beginVerifiedDeletion() {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            showDeletionError(getString(R.string.delete_account_failed, "No signed-in account"));
            return;
        }
        if (user.isAnonymous()) {
            performAccountDeletion();
            return;
        }
        googleAuthManager = new GoogleAuthManager(requireContext());
        android.widget.Toast.makeText(requireContext(), R.string.delete_account_auth_required,
                android.widget.Toast.LENGTH_LONG).show();
        deletionAuthLauncher.launch(googleAuthManager.getSignInIntent());
    }

    private void performAccountDeletion() {
        deletionProgressDialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.delete_account_progress)
                .setCancelable(false)
                .show();
        AccountDeletionManager.deleteCurrentAccount(requireContext(), new AccountDeletionManager.Callback() {
            @Override public void onSuccess() {
                dismissDeletionProgress();
                if (!isAdded()) return;
                android.widget.Toast.makeText(requireContext(), R.string.delete_account_success,
                        android.widget.Toast.LENGTH_LONG).show();
                if (googleAuthManager != null) {
                    googleAuthManager.revokeAccess(SettingsFragment.this::restartAfterAccountDeletion);
                } else {
                    restartAfterAccountDeletion();
                }
            }

            @Override public void onError(String message) {
                dismissDeletionProgress();
                showDeletionError(message);
            }
        });
    }

    private void dismissDeletionProgress() {
        if (deletionProgressDialog != null) {
            deletionProgressDialog.dismiss();
            deletionProgressDialog = null;
        }
    }

    private void showDeletionError(String message) {
        if (!isAdded()) return;
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_account_dialog_title)
                .setMessage(getString(R.string.delete_account_failed, message))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void restartAfterAccountDeletion() {
        if (!isAdded()) return;
        Intent intent = new Intent(requireContext(), com.quran.quranaudio.online.SplashScreenActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
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
                    
                    // 🔄 同步语言设置（清除旧的翻译和 Tafsir 缓存）
                    com.quran.quranaudio.online.quran_module.utils.LanguageSyncHelper.INSTANCE.syncLanguageSettings(requireContext());
                    android.util.Log.d("SettingsFragment", "🔄 Language sync completed, recreating activity...");
                    
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
                intent.putExtra(com.quran.quranaudio.online.subscription.SubscriptionActivity.EXTRA_SOURCE, "settings");
                startActivity(intent);
                return true;
            });
        } else {
            android.util.Log.w("SettingsFragment", "⚠️ Premium subscription preference not found");
        }

        setupAdFreePreference();
        setupAddPrayerWidgetPreference();
    }

    /**
     * 去广告买断（removeads）的固定入口。
     *
     * 自动弹窗有防骚扰限制（24h 间隔、累计关闭 3 次后不再弹），
     * 所以必须有一个永远找得到的入口，否则想买的用户会买不到。
     *
     * 订阅用户和已买断用户不显示：他们已经无广告。
     */
    private void setupAdFreePreference() {
        Preference adFreePref = getPreferenceScreen().findPreference("AD_FREE_PREFERENCE");
        if (adFreePref == null || getContext() == null) {
            return;
        }

        boolean alreadyAdFree =
                com.quranaudio.common.ad.SubscriptionChecker.shouldHideAds(getContext());
        adFreePref.setVisible(!alreadyAdFree);
        if (alreadyAdFree) {
            return;
        }

        // 有缓存价格就直接标在副标题上。一次性买断的价格通常远低于用户
        // 对「订阅」的心理预期，把它提前暴露本身就是转化杠杆。
        String price = com.quran.quranaudio.online.subscription.AdFreeBilling
                .cachedPrice(getContext());
        if (price != null && !price.isEmpty()) {
            adFreePref.setSummary(getString(R.string.ad_free_cta_priced, price));
        }

        adFreePref.setOnPreferenceClickListener(preference -> {
            if (getActivity() != null) {
                new com.quran.quranaudio.online.subscription.AdFreeDialog(getActivity()).show();
            }
            return true;
        });
    }

    /**
     * 🏠 桌面 Widget 添加入口（长尾兜底：促活弹窗拒绝过的用户随时可从这里添加）
     */
    private void setupAddPrayerWidgetPreference() {
        Preference widgetPref = getPreferenceScreen().findPreference("ADD_PRAYER_WIDGET_PREFERENCE");

        if (widgetPref != null) {
            widgetPref.setOnPreferenceClickListener(preference -> {
                if (getActivity() != null) {
                    com.quran.quranaudio.online.prayertimes.widget.PrayerWidgetPromoHelper
                            .requestAddFromSettings(getActivity());
                }
                return true;
            });
        }

        // 常驻倒计时通知开关：切换时立即发/取消通知。
        Preference persistentPref = getPreferenceScreen().findPreference(
                com.quran.quranaudio.online.prayertimes.widget.PersistentPrayerNotification.PREF_KEY);
        if (persistentPref != null) {
            persistentPref.setOnPreferenceChangeListener((preference, newValue) -> {
                if (getContext() != null) {
                    com.quran.quranaudio.online.prayertimes.widget.PersistentPrayerNotification
                            .setEnabled(getContext(), Boolean.TRUE.equals(newValue));
                }
                return true;
            });
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
                        intent.putExtra(com.quran.quranaudio.online.subscription.SubscriptionActivity.EXTRA_SOURCE, "settings");
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
                        intent.putExtra(com.quran.quranaudio.online.subscription.SubscriptionActivity.EXTRA_SOURCE, "settings");
                        startActivity(intent);
                    });
                } else {
                    android.util.Log.w("SettingsFragment", "⚠️ Premium button not found");
                }
            }
        }, 300);
    }

    @Override
    @SuppressWarnings("deprecation")
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
            try {
                // Note: setTargetFragment() is deprecated, but it's required for PreferenceDialogFragmentCompat
                // to work correctly. Without it, the dialog crashes with "Target fragment must implement TargetFragment interface".
                // This is a known limitation until AndroidX provides a proper replacement.
                
                // ✅ 防御性检查：确保 Fragment 状态正常
                if (!isAdded() || isDetached()) {
                    android.util.Log.w("SettingsFragment", "⚠️ Fragment not in valid state, cannot show dialog");
                    return;
                }
                
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
            } catch (IllegalStateException e) {
                // 🆕 捕获任何状态异常，防止崩溃
                android.util.Log.e("SettingsFragment", "❌ Failed to show dialog: " + e.getMessage(), e);
                android.util.Log.w("SettingsFragment", "⚠️ Dialog will not be displayed, but app continues normally");
                // 优雅降级：不显示对话框，但应用继续运行
            }
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

        // 📖 每日经文开关：切换后立即生效（开→排下一次；关→取消已排闹钟）
        if (com.quran.quranaudio.online.dailyverse.DailyVersePreferences.KEY_ENABLED.equals(key)) {
            boolean enabled = sharedPreferences.getBoolean(
                    com.quran.quranaudio.online.dailyverse.DailyVersePreferences.KEY_ENABLED, true);
            if (enabled) {
                com.quran.quranaudio.online.dailyverse.DailyVerseScheduler.scheduleNext(requireContext());
            } else {
                com.quran.quranaudio.online.dailyverse.DailyVerseScheduler.cancel(requireContext());
            }
            try {
                java.util.Map<String, Object> params = new java.util.HashMap<>();
                params.put("action", enabled ? "enabled" : "disabled");
                com.quran.quranaudio.online.analytics.AnalyticsManager
                        .getInstance(requireContext()).logEvent("daily_verse_funnel", params);
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        PreferenceManager.getDefaultSharedPreferences(requireContext())
                .unregisterOnSharedPreferenceChangeListener(this);
    }
}
