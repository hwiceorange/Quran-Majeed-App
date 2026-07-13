package com.quran.quranaudio.online.prayertimes.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.slider.Slider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.quran.quranaudio.online.R;
import com.quran.quranaudio.online.quran_module.utils.sharedPrefs.SPAppConfigs;
import com.quran.quranaudio.online.prayertimes.common.PrayerEnum;
import com.quran.quranaudio.online.prayertimes.preferences.PreferencesConstants;
import com.quran.quranaudio.online.prayertimes.utils.UiUtils;
import android.preference.PreferenceManager;
import android.net.Uri;
import android.app.AlarmManager;
import android.content.Intent;
import android.provider.Settings;

/**
 * Prayer notification settings page
 * Allows users to configure notification type, volume, and pre-prayer reminder for each prayer time
 */
public class PrayerNotificationSettingsActivity extends AppCompatActivity {

    // Intent extra keys
    public static final String EXTRA_PRAYER_NAME = "prayer_name";
    public static final String EXTRA_PRAYER_ENUM = "prayer_enum";

    // SharedPreferences keys
    private static final String PREF_NOTIFICATION_TYPE_SUFFIX = "_NOTIFICATION_TYPE";
    private static final String PREF_VOLUME_SUFFIX = "_VOLUME";
    private static final String PREF_PRE_REMINDER_SUFFIX = "_PRE_REMINDER";
    private static final String PREF_PRE_REMINDER_MINUTES_SUFFIX = "_PRE_REMINDER_MINUTES";
    private static final String PREF_AZAN_NAME_SUFFIX = "_AZAN_NAME";
    
    // Pre-reminder minutes range
    private static final int MIN_REMINDER_MINUTES = 1;
    private static final int MAX_REMINDER_MINUTES = 30;
    private static final int DEFAULT_REMINDER_MINUTES = 5;

    // Notification types
    public static final String TYPE_NONE = "none";
    public static final String TYPE_AZAN = "azan";
    public static final String TYPE_VIBRATE = "vibrate";
    public static final String TYPE_SILENT = "silent";
    public static final String TYPE_TEXT_TONE = "text_tone";
    public static final String TYPE_CLOCK = "clock";

    // UI elements
    private Toolbar toolbar;
    private LinearLayout optionNone, optionAzan, optionVibrate, optionSilent, optionTextTone, optionClock;
    private ImageView iconNone, iconAzan, iconVibrate, iconSilent, iconTextTone, iconClock;
    private TextView textNone, textAzan, textVibrate, textSilent, textTextTone, textClock;
    private LinearLayout expandedOptionsContainer;
    private LinearLayout azanNameContainer;
    private TextView tvAzanName;
    private LinearLayout volumeContainer;
    private Slider volumeSlider;
    private TextView tvVolumeValue;
    private SwitchMaterial switchPreReminder;
    private LinearLayout minutesSelectorContainer;
    private TextView tvMinutesValue;
    private ImageView btnMinutesDecrease, btnMinutesIncrease;

    // Data
    private String prayerName;
    private PrayerEnum prayerEnum;
    private String selectedType = TYPE_NONE;
    private int reminderMinutes = DEFAULT_REMINDER_MINUTES;
    private SharedPreferences preferences;
    
    // 闹钟权限请求码
    private static final int ALARM_PERMISSION_REQUEST_CODE = 1001;
    
    // 标记设置是否已更改（用于触发重新调度）
    private boolean settingsChanged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_prayer_notification_settings);

        // Get prayer info from intent (需要在setupToolbar之前获取，因为标题需要用到祷告名称)
        prayerName = getIntent().getStringExtra(EXTRA_PRAYER_NAME);
        String prayerEnumStr = getIntent().getStringExtra(EXTRA_PRAYER_ENUM);
        if (prayerEnumStr != null) {
            try {
                prayerEnum = PrayerEnum.valueOf(prayerEnumStr);
            } catch (IllegalArgumentException e) {
                android.util.Log.e("PrayerNotificationSettings", "❌ Invalid PrayerEnum: " + prayerEnumStr, e);
                prayerEnum = null;
            }
        }

        // 🔄 统一设计风格：状态栏颜色与 Toolbar 一致 (完全复制Learning Plan Setup模式)
        setupStatusBar();
        
        // 🔄 统一设计风格：设置 Toolbar 和返回按钮 (完全复制Learning Plan Setup模式)
        setupToolbar();

        // Initialize SharedPreferences
        preferences = getSharedPreferences(PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, MODE_PRIVATE);

        initViews();
        loadSettings();
        setupListeners();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }
    
    @Override
    public void onBackPressed() {
        // 🔔 返回前设置结果，告知父级需要重新调度
        finishWithResult();
        super.onBackPressed();
    }
    
    @Override
    public void finish() {
        // 🔔 退出前设置结果，告知父级需要重新调度
        finishWithResult();
        super.finish();
    }
    
    /**
     * 完成并设置返回结果
     * 如果设置已更改，返回 RESULT_OK 触发父级重新调度闹钟
     */
    private void finishWithResult() {
        if (settingsChanged) {
            android.util.Log.d("PrayerNotificationSettings", "🔔 Settings changed, notifying parent to reschedule alarms");
            setResult(RESULT_OK);
        } else {
            android.util.Log.d("PrayerNotificationSettings", "ℹ️ No settings changed, returning RESULT_CANCELED");
            setResult(RESULT_CANCELED);
        }
    }

    private Context updateBaseContextLocale(Context context) {
        String locale = SPAppConfigs.getLocale(context);
        if (locale != null && !locale.isEmpty()) {
            // minSdk 26：直接用现代 API(旧的 legacy 方法在 API 24+ 永不执行，已移除)
            return updateResourcesLocale(context, locale);
        }
        return context;
    }

    private Context updateResourcesLocale(Context context, String localeCode) {
        java.util.Locale locale = new java.util.Locale(localeCode);
        android.content.res.Configuration configuration = new android.content.res.Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);
        return context.createConfigurationContext(configuration);
    }
    
    /**
     * 🔄 统一设计风格：状态栏设置 (完全复制MainActivity的白色状态栏统一样式)
     */
    @SuppressWarnings("deprecation")
    private void setupStatusBar() {
        try {
            Window window = getWindow();
            View decorView = window.getDecorView();
            
            // 确保内容不延伸到状态栏下方（非沉浸式）
            WindowCompat.setDecorFitsSystemWindows(window, true);
            
            // 设置状态栏为白色 (MainActivity统一样式)
            window.setStatusBarColor(0xFFFFFFFF);
            
            // 设置图标为深色（lightStatusBar = true 表示浅色背景需要深色图标）
            WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
            wic.setAppearanceLightStatusBars(true);
            
            android.util.Log.e("PrayerNotificationSettings", "✅ 统一状态栏设置: 白色背景 + 深色图标 (与MainActivity一致)");
        } catch (Exception e) {
            android.util.Log.e("PrayerNotificationSettings", "❌ 设置统一状态栏失败", e);
        }
    }
    
    /**
     * 🔄 统一设计风格：设置 Toolbar 和返回按钮 (完全复制Learning Plan Setup模式)
     */
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                // 动态设置祷告名作为标题
                String title = prayerName != null ? prayerName : "Notification Settings";
                getSupportActionBar().setTitle(title);
            }
            toolbar.setNavigationOnClickListener(v -> finish());
        }
    }

    private void initViews() {
        try {
            // Toolbar已在setupToolbar()中设置，此处不需要重复设置
            toolbar = findViewById(R.id.toolbar);

            // Notification type options
            optionNone = findViewById(R.id.option_none);
            optionAzan = findViewById(R.id.option_azan);
            optionVibrate = findViewById(R.id.option_vibrate);
            optionSilent = findViewById(R.id.option_silent);
            optionTextTone = findViewById(R.id.option_text_tone);
            optionClock = findViewById(R.id.option_clock);

            // Icons
            iconNone = findViewById(R.id.icon_none);
            iconAzan = findViewById(R.id.icon_azan);
            iconVibrate = findViewById(R.id.icon_vibrate);
            iconSilent = findViewById(R.id.icon_silent);
            iconTextTone = findViewById(R.id.icon_text_tone);
            iconClock = findViewById(R.id.icon_clock);

            // Texts
            textNone = findViewById(R.id.text_none);
            textAzan = findViewById(R.id.text_azan);
            textVibrate = findViewById(R.id.text_vibrate);
            textSilent = findViewById(R.id.text_silent);
            textTextTone = findViewById(R.id.text_text_tone);
            textClock = findViewById(R.id.text_clock);

            // Expanded options
            expandedOptionsContainer = findViewById(R.id.expanded_options_container);
            azanNameContainer = findViewById(R.id.azan_name_container);
            tvAzanName = findViewById(R.id.tv_azan_name);
            volumeContainer = findViewById(R.id.volume_container);
            volumeSlider = findViewById(R.id.volume_seekbar);
            tvVolumeValue = findViewById(R.id.tv_volume_value);
            switchPreReminder = findViewById(R.id.switch_pre_reminder);
            minutesSelectorContainer = findViewById(R.id.minutes_selector_container);
            tvMinutesValue = findViewById(R.id.tv_minutes_value);
            btnMinutesDecrease = findViewById(R.id.btn_minutes_decrease);
            btnMinutesIncrease = findViewById(R.id.btn_minutes_increase);
            
            android.util.Log.d("PrayerNotificationSettings", "✅ initViews completed successfully");
        } catch (Exception e) {
            android.util.Log.e("PrayerNotificationSettings", "❌ Error in initViews", e);
            throw e; // Re-throw to let the system handle it properly
        }
    }

    private void loadSettings() {
        try {
            String prayerKey = (prayerEnum != null ? prayerEnum.toString() : "DEFAULT");
            
            // Load saved notification type
            String savedType = preferences.getString(prayerKey + PREF_NOTIFICATION_TYPE_SUFFIX, TYPE_NONE);
            selectedType = savedType;

            // Load volume
            int volume = preferences.getInt(prayerKey + PREF_VOLUME_SUFFIX, 80);
            volumeSlider.setValue(volume);
            tvVolumeValue.setText(getString(R.string.volume_percentage_format, volume));

            // Load pre-reminder setting
            boolean preReminder = preferences.getBoolean(prayerKey + PREF_PRE_REMINDER_SUFFIX, false);
            switchPreReminder.setChecked(preReminder);

            // Load pre-reminder minutes
            reminderMinutes = preferences.getInt(prayerKey + PREF_PRE_REMINDER_MINUTES_SUFFIX, DEFAULT_REMINDER_MINUTES);
            updateMinutesDisplay();
            
            // Load Azan name
            loadAzanName();

            // Update UI
            updateSelectedOption();
            updateExpandedOptions();
            updateMinutesSelectorVisibility();
            
            android.util.Log.d("PrayerNotificationSettings", "✅ loadSettings completed for " + prayerKey);
        } catch (Exception e) {
            android.util.Log.e("PrayerNotificationSettings", "❌ Error in loadSettings", e);
        }
    }
    
    /**
     * 加载并显示当前选中的Azan名称
     */
    private void loadAzanName() {
        if (prayerEnum == null) {
            return;
        }
        
        try {
            // 判断是否是Fajr祷告
            boolean isFajr = prayerEnum == PrayerEnum.FAJR;
            
            // 获取对应的音频列表
            String[] azanNames;
            String[] azanValues;
            
            if (isFajr) {
                azanNames = getResources().getStringArray(R.array.entries_fajr_adhan_list_preference);
                azanValues = getResources().getStringArray(R.array.entryvalues_fajr_adhan_list_preference);
            } else {
                azanNames = getResources().getStringArray(R.array.entries_adhan_list_preference);
                azanValues = getResources().getStringArray(R.array.entryvalues_adhan_list_preference);
            }
            
            // 获取当前选中的Azan
            String currentAzan = preferences.getString(prayerEnum + PREF_AZAN_NAME_SUFFIX, azanValues[0]);
            int selectedIndex = java.util.Arrays.asList(azanValues).indexOf(currentAzan);
            
            // 更新UI显示
            if (selectedIndex >= 0 && selectedIndex < azanNames.length) {
                tvAzanName.setText(azanNames[selectedIndex]);
            } else {
                tvAzanName.setText(azanNames[0]);
            }
        } catch (Exception e) {
            android.util.Log.e("PrayerNotificationSettings", "❌ Error loading Azan name", e);
            tvAzanName.setText(getString(R.string.random_azan));
        }
    }

    private void setupListeners() {
        // Option click listeners
        optionNone.setOnClickListener(v -> selectOption(TYPE_NONE));
        optionAzan.setOnClickListener(v -> selectOption(TYPE_AZAN));
        optionVibrate.setOnClickListener(v -> selectOption(TYPE_VIBRATE));
        optionSilent.setOnClickListener(v -> selectOption(TYPE_SILENT));
        optionTextTone.setOnClickListener(v -> selectOption(TYPE_TEXT_TONE));
        optionClock.setOnClickListener(v -> selectOption(TYPE_CLOCK));
        
        // Azan name selection - 直接在onTouch中处理
        if (azanNameContainer != null) {
            azanNameContainer.setOnTouchListener((v, event) -> {
                if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                    android.util.Log.d("PrayerNotificationSettings", "👆 Touch UP detected, calling showAzanNameDialog");
                    showAzanNameDialog();
                    return true; // 消费事件
                }
                return false;
            });
            
            android.util.Log.d("PrayerNotificationSettings", "✅ Azan name touch listener set");
        } else {
            android.util.Log.e("PrayerNotificationSettings", "❌ azanNameContainer is null!");
        }

        // Volume slider
        volumeSlider.addOnChangeListener((slider, value, fromUser) -> {
            int volume = (int) value;
            tvVolumeValue.setText(getString(R.string.volume_percentage_format, volume));
            if (fromUser) {
                saveVolume(volume);
            }
        });

        // Pre-reminder switch
        switchPreReminder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            savePreReminder(isChecked);
            updateMinutesSelectorVisibility();
            
            // 开启预提醒时，请求闹钟权限
            if (isChecked) {
                checkAndRequestAlarmPermission();
            }
        });

        // Minutes increase/decrease buttons
        btnMinutesDecrease.setOnClickListener(v -> {
            if (reminderMinutes > MIN_REMINDER_MINUTES) {
                reminderMinutes--;
                updateMinutesDisplay();
                saveReminderMinutes(reminderMinutes);
            }
        });

        btnMinutesIncrease.setOnClickListener(v -> {
            if (reminderMinutes < MAX_REMINDER_MINUTES) {
                reminderMinutes++;
                updateMinutesDisplay();
                saveReminderMinutes(reminderMinutes);
            }
        });

        // Azan name click (TODO: Implement Azan selection dialog)
        azanNameContainer.setOnClickListener(v -> {
            // Will implement Azan selection later
        });
    }

    private void selectOption(String type) {
        selectedType = type;
        saveNotificationType(type);
        updateSelectedOption();
        updateExpandedOptions();
        
        // 选择Clock Sound时，请求闹钟权限
        if (TYPE_CLOCK.equals(type)) {
            checkAndRequestAlarmPermission();
        }
    }

    private void updateSelectedOption() {
        // Define colors
        int unselectedTextColor = ContextCompat.getColor(this, android.R.color.black); // #212121
        int selectedTextColor = ContextCompat.getColor(this, android.R.color.white);
        int unselectedIconColor = ContextCompat.getColor(this, android.R.color.black);
        int selectedIconColor = ContextCompat.getColor(this, android.R.color.white);

        // Reset all options to unselected state
        setOptionStyle(optionNone, iconNone, textNone, false);
        setOptionStyle(optionAzan, iconAzan, textAzan, false);
        setOptionStyle(optionVibrate, iconVibrate, textVibrate, false);
        setOptionStyle(optionSilent, iconSilent, textSilent, false);
        setOptionStyle(optionTextTone, iconTextTone, textTextTone, false);
        setOptionStyle(optionClock, iconClock, textClock, false);

        // Highlight selected option
        switch (selectedType) {
            case TYPE_NONE:
                setOptionStyle(optionNone, iconNone, textNone, true);
                break;
            case TYPE_AZAN:
                setOptionStyle(optionAzan, iconAzan, textAzan, true);
                break;
            case TYPE_VIBRATE:
                setOptionStyle(optionVibrate, iconVibrate, textVibrate, true);
                break;
            case TYPE_SILENT:
                setOptionStyle(optionSilent, iconSilent, textSilent, true);
                break;
            case TYPE_TEXT_TONE:
                setOptionStyle(optionTextTone, iconTextTone, textTextTone, true);
                break;
            case TYPE_CLOCK:
                setOptionStyle(optionClock, iconClock, textClock, true);
                break;
        }
    }

    private void setOptionStyle(LinearLayout option, ImageView icon, TextView text, boolean isSelected) {
        if (isSelected) {
            option.setBackgroundResource(R.drawable.bg_notification_option_selected);
            text.setTextColor(ContextCompat.getColor(this, android.R.color.white));
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.white));
        } else {
            option.setBackgroundResource(R.drawable.bg_notification_option_unselected);
            text.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            icon.setColorFilter(ContextCompat.getColor(this, android.R.color.black));
        }
    }

    private void updateExpandedOptions() {
        android.util.Log.d("PrayerNotificationSettings", "📊 updateExpandedOptions called, selectedType = " + selectedType);
        
        // Show/hide expanded options based on selected type
        if (TYPE_NONE.equals(selectedType)) {
            expandedOptionsContainer.setVisibility(View.GONE);
            android.util.Log.d("PrayerNotificationSettings", "❌ Type is NONE, hiding all options");
        } else {
            expandedOptionsContainer.setVisibility(View.VISIBLE);
            android.util.Log.d("PrayerNotificationSettings", "✅ Type is not NONE, showing expanded options");

            // Show Azan name only for Azan type
            if (TYPE_AZAN.equals(selectedType)) {
                azanNameContainer.setVisibility(View.VISIBLE);
                android.util.Log.d("PrayerNotificationSettings", "✅ Type is AZAN, showing azanNameContainer");
            } else {
                azanNameContainer.setVisibility(View.GONE);
                android.util.Log.d("PrayerNotificationSettings", "❌ Type is " + selectedType + ", hiding azanNameContainer");
            }

            // Hide volume for Vibrate, Silent Notification and Standard Text Tone
            if (TYPE_VIBRATE.equals(selectedType) || TYPE_SILENT.equals(selectedType) || TYPE_TEXT_TONE.equals(selectedType)) {
                volumeContainer.setVisibility(View.GONE);
            } else {
                volumeContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    private void saveNotificationType(String type) {
        if (prayerEnum == null) {
            android.util.Log.w("PrayerNotificationSettings", "⚠️ Cannot save notification type: prayerEnum is null");
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(prayerEnum + PREF_NOTIFICATION_TYPE_SUFFIX, type);
        
        // Also update the old simple boolean for backward compatibility
        boolean enabled = TYPE_AZAN.equals(type);
        String callPreferenceKey = prayerEnum.toString() + PreferencesConstants.ADTHAN_CALL_ENABLED_KEY;
        editor.putBoolean(callPreferenceKey, enabled);
        
        editor.apply();
        
        // 标记设置已更改
        settingsChanged = true;
        android.util.Log.d("PrayerNotificationSettings", "✅ Notification type changed, marked for rescheduling");
    }

    private void saveVolume(int volume) {
        if (prayerEnum == null) {
            android.util.Log.w("PrayerNotificationSettings", "⚠️ Cannot save volume: prayerEnum is null");
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(prayerEnum + PREF_VOLUME_SUFFIX, volume);
        editor.apply();
        
        // 音量变化不需要重新调度闹钟，只标记为需要通知
        settingsChanged = true;
    }

    private void savePreReminder(boolean enabled) {
        if (prayerEnum == null) {
            android.util.Log.w("PrayerNotificationSettings", "⚠️ Cannot save pre-reminder: prayerEnum is null");
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(prayerEnum + PREF_PRE_REMINDER_SUFFIX, enabled);
        editor.apply();
        
        // 🔔 预提醒开关变化，需要重新调度闹钟
        settingsChanged = true;
        android.util.Log.d("PrayerNotificationSettings", "✅ Pre-reminder changed, marked for rescheduling");
    }

    private void saveReminderMinutes(int minutes) {
        if (prayerEnum == null) {
            android.util.Log.w("PrayerNotificationSettings", "⚠️ Cannot save reminder minutes: prayerEnum is null");
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(prayerEnum + PREF_PRE_REMINDER_MINUTES_SUFFIX, minutes);
        editor.apply();
        
        // 🔔 预提醒时间变化，需要重新调度闹钟
        settingsChanged = true;
        android.util.Log.d("PrayerNotificationSettings", "✅ Reminder minutes changed, marked for rescheduling");
    }

    private void updateMinutesDisplay() {
        tvMinutesValue.setText(getString(R.string.reminder_minutes_format, reminderMinutes));
    }

    private void updateMinutesSelectorVisibility() {
        if (switchPreReminder.isChecked()) {
            minutesSelectorContainer.setVisibility(View.VISIBLE);
        } else {
            minutesSelectorContainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示Azan名称选择对话框
     * 根据祷告类型（Fajr或其他）显示不同的音频列表
     */
    @SuppressWarnings("deprecation")
    private void showAzanNameDialog() {
        android.util.Log.d("PrayerNotificationSettings", "🔔 showAzanNameDialog called!");
        android.util.Log.d("PrayerNotificationSettings", "prayerEnum = " + prayerEnum);
        
        if (prayerEnum == null) {
            android.util.Log.e("PrayerNotificationSettings", "❌ prayerEnum is null, cannot show dialog");
            return;
        }
        
        // 判断是否是Fajr祷告
        boolean isFajr = prayerEnum == PrayerEnum.FAJR;
        android.util.Log.d("PrayerNotificationSettings", "isFajr = " + isFajr);
        
        // 获取对应的音频列表
        String[] azanNames;
        String[] azanValues;
        
        if (isFajr) {
            // Fajr Adhan 列表
            azanNames = getResources().getStringArray(R.array.entries_fajr_adhan_list_preference);
            azanValues = getResources().getStringArray(R.array.entryvalues_fajr_adhan_list_preference);
        } else {
            // Other prayers Adhan 列表
            azanNames = getResources().getStringArray(R.array.entries_adhan_list_preference);
            azanValues = getResources().getStringArray(R.array.entryvalues_adhan_list_preference);
        }
        
        // 获取当前选中的Azan
        String currentAzan = preferences.getString(prayerEnum + PREF_AZAN_NAME_SUFFIX, azanValues[0]);
        int selectedIndex = java.util.Arrays.asList(azanValues).indexOf(currentAzan);
        
        android.util.Log.d("PrayerNotificationSettings", "📋 Azan list size: " + azanNames.length);
        android.util.Log.d("PrayerNotificationSettings", "📋 Current Azan: " + currentAzan);
        android.util.Log.d("PrayerNotificationSettings", "📋 Selected index: " + selectedIndex);
        
        // 创建对话框
        android.util.Log.d("PrayerNotificationSettings", "🎨 Creating dialog...");
        new android.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.azan_name))
                .setSingleChoiceItems(azanNames, selectedIndex, (dialog, which) -> {
                    android.util.Log.d("PrayerNotificationSettings", "✅ User selected: " + azanNames[which]);
                    
                    // 获取选中的Azan值并转换为URI
                    String selectedAzanValue = azanValues[which];
                    Uri azanUri = UiUtils.uriFromRaw(selectedAzanValue, this);
                    
                    // 保存到系统使用的SharedPreferences键
                    SharedPreferences defaultPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = defaultPrefs.edit();
                    
                    if (isFajr) {
                        editor.putString(PreferencesConstants.ADTHAN_FAJR_CALLER, azanUri.toString());
                        android.util.Log.d("PrayerNotificationSettings", "💾 Saved Fajr Azan: " + azanUri);
                    } else {
                        editor.putString(PreferencesConstants.ADTHAN_CALLER, azanUri.toString());
                        android.util.Log.d("PrayerNotificationSettings", "💾 Saved Other Prayer Azan: " + azanUri);
                    }
                    editor.apply();
                    
                    // 同时保存到我们自己的preference作为备份
                    preferences.edit()
                            .putString(prayerEnum + PREF_AZAN_NAME_SUFFIX, selectedAzanValue)
                            .apply();
                    
                    // 更新UI显示
                    tvAzanName.setText(azanNames[which]);
                    
                    dialog.dismiss();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    /**
     * 检查并请求闹钟权限（Android 12+）
     * 用于精确定时闹钟功能
     */
    private void checkAndRequestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                android.util.Log.d("PrayerNotificationSettings", "⏰ Requesting exact alarm permission...");
                try {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    startActivity(intent);
                } catch (Exception e) {
                    android.util.Log.e("PrayerNotificationSettings", "❌ Failed to request alarm permission", e);
                }
            } else {
                android.util.Log.d("PrayerNotificationSettings", "✅ Exact alarm permission already granted");
            }
        }
    }
}

