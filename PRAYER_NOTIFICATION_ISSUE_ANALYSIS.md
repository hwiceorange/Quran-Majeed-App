# 🔔 Salat 通知设置问题深度分析报告

## 📋 问题描述

**用户报告**：在 Salat 页面点击铃铛 → 进入通知设置页面 → 设置震动通知 + 提前 5 分钟提醒 → **没有实现**

---

## 🔍 深度代码分析

### 1️⃣ **新通知设置页面的实现**

#### 文件：`PrayerNotificationSettingsActivity.java`

**保存位置**：
```java
// SharedPreferences 文件名
private static final String PREF_NAME = "ADTHAN_CALLS";

// 保存的 Key（每个祷告独立）
prayerEnum + "_NOTIFICATION_TYPE"        // 例如：FAJR_NOTIFICATION_TYPE
prayerEnum + "_VOLUME"                   // 例如：FAJR_VOLUME
prayerEnum + "_PRE_REMINDER"             // 例如：FAJR_PRE_REMINDER ✅
prayerEnum + "_PRE_REMINDER_MINUTES"     // 例如：FAJR_PRE_REMINDER_MINUTES ✅
prayerEnum + "_AZAN_NAME"                // 例如：FAJR_AZAN_NAME
```

**保存代码**：
```java:509-527
private void savePreReminder(boolean enabled) {
    if (prayerEnum == null) {
        android.util.Log.w("PrayerNotificationSettings", "⚠️ Cannot save pre-reminder: prayerEnum is null");
        return;
    }
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(prayerEnum + PREF_PRE_REMINDER_SUFFIX, enabled);  // ✅ 保存到 ADTHAN_CALLS
    editor.apply();
}

private void saveReminderMinutes(int minutes) {
    if (prayerEnum == null) {
        android.util.Log.w("PrayerNotificationSettings", "⚠️ Cannot save reminder minutes: prayerEnum is null");
        return;
    }
    SharedPreferences.Editor editor = preferences.edit();
    editor.putInt(prayerEnum + PREF_PRE_REMINDER_MINUTES_SUFFIX, minutes);  // ✅ 保存到 ADTHAN_CALLS
    editor.apply();
}
```

---

### 2️⃣ **现有通知调度系统的实现**

#### 文件：`PrayerAlarmScheduler.java`

**读取位置**：
```java:55-57
if (preferencesHelper.isReminderEnabled()) {
    scheduleReminders(dayPrayer);
}
```

**调度提醒逻辑**：
```java:110-133
private void scheduleReminders(@NonNull DayPrayer dayPrayer) {
    Log.i(TAG, "Start scheduling Reminders for: " + dayPrayer.getDate());

    Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();
    int reminderInterval = preferencesHelper.getReminderInterval();  // ❌ 读取全局间隔

    int index = 10;
    for (PrayerEnum key : timings.keySet()) {
        index++;

        LocalDateTime prayerTiming = timings.get(key);
        LocalDateTime reminderTiming = Objects.requireNonNull(prayerTiming).minusMinutes(reminderInterval);

        if (LocalDateTime.now().isBefore(reminderTiming)) {
            Log.i(TAG, "Scheduling " + key.toString() + " Reminder at : " + TimingUtils.formatTiming(reminderTiming));

            scheduleNotifications(dayPrayer, prayerTiming, TimingType.STANDARD, key.toString(),
                    2000, index, reminderTiming, ReminderReceiver.class);
        }
    }

    Log.i(TAG, "End scheduling Reminders for: " + dayPrayer.getDate());
}
```

---

### 3️⃣ **PreferencesHelper 读取的配置来源**

#### 文件：`PreferencesHelper.java`

**震动设置**：
```java:200-203
public Boolean isVibrationActivated() {
    final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    return defaultSharedPreferences.getBoolean(PreferencesConstants.ADHAN_VIBRATION_PREFERENCE, true);  // ❌ 全局震动
}
```

**提醒开关**：
```java:230-233
public boolean isReminderEnabled() {
    final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    return defaultSharedPreferences.getBoolean(PreferencesConstants.ADTHAN_REMINDER_ENABLED, true);  // ❌ 全局提醒开关
}
```

**提醒间隔**：
```java:240-243
public int getReminderInterval() {
    final SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    return defaultSharedPreferences.getInt(PreferencesConstants.ADTHAN_REMINDER_INTERVAL, 10);  // ❌ 全局提醒间隔（默认10分钟）
}
```

---

### 4️⃣ **Settings.xml 的全局配置**

#### 文件：`settings.xml`

**震动配置**（第 72-77 行）：
```xml
<CheckBoxPreference
    android:dependency="NOTIFICATIONS_ENABLED"
    android:defaultValue="true"
    android:key="ADHAN_VIBRATION_PREFERENCE"
    android:layout="@layout/custom_switch_preference"
    android:title="@string/title_adhan_vibration_preference" />
```

**提醒配置**（第 122-140 行）：
```xml
<CheckBoxPreference
    android:defaultValue="true"
    android:key="ADTHAN_REMINDER_ENABLED"
    android:layout="@layout/custom_switch_preference"
    android:title="@string/title_adhan_reminder_preference" />

<com.quran.quranaudio.online.prayertimes.ui.settings.adhan.AdhanReminderPreference
    android:dependency="ADTHAN_REMINDER_ENABLED"
    android:dialogTitle="@string/title_adhan_reminder_interval_preference"
    android:key="ADTHAN_REMINDER_INTERVAL"
    android:layout="@layout/custom_preference"
    android:title="@string/title_adhan_reminder_interval_preference" />
```

---

## 🔥 **核心问题诊断**

### ❌ **数据不匹配问题**

| 项目 | 新设置页面 (保存) | 通知调度系统 (读取) | 结果 |
|------|------------------|-------------------|------|
| **震动** | `FAJR_VIBRATION` (未实现) | `ADHAN_VIBRATION_PREFERENCE` (全局) | ❌ 不匹配 |
| **预提醒开关** | `FAJR_PRE_REMINDER` | `ADTHAN_REMINDER_ENABLED` (全局) | ❌ 不匹配 |
| **预提醒间隔** | `FAJR_PRE_REMINDER_MINUTES` (5分钟) | `ADTHAN_REMINDER_INTERVAL` (全局, 默认10分钟) | ❌ 不匹配 |
| **通知类型** | `FAJR_NOTIFICATION_TYPE` | `FAJR_ADTHAN_CALL_ENABLED` (仅判断开关) | ⚠️ 部分匹配 |

### 🎯 **问题根源**

1. **设计不一致**：
   - 新页面：每个祷告独立配置（更符合用户需求）
   - 旧系统：全局统一配置（Settings 页面）

2. **存储位置不同**：
   - 新页面：`ADTHAN_CALLS` SharedPreferences
   - 旧系统：`DefaultSharedPreferences` (对应 `settings.xml`)

3. **数据流断裂**：
   ```
   用户设置（新页面）
         ↓
   保存到 ADTHAN_CALLS
         ↓
         ❌ 通知调度系统不读取这里
         ↓
   读取 DefaultSharedPreferences (Settings 全局配置)
         ↓
   使用全局配置调度通知
   ```

---

## 🛠️ **解决方案**

### 方案 A：修改通知调度系统（推荐）✅

**优势**：
- 支持每个祷告独立配置（更灵活）
- 保留新页面的设计
- 符合用户预期

**实施步骤**：

#### 1. 修改 `PreferencesHelper.java`

添加每个祷告独立的配置读取方法：

```java
// 震动设置（每个祷告独立）
public Boolean isVibrationActivatedForPrayer(PrayerEnum prayer) {
    // 优先读取祷告独立配置
    SharedPreferences prayerPrefs = context.getSharedPreferences(
        PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, Context.MODE_PRIVATE);
    
    String notificationType = prayerPrefs.getString(
        prayer + "_NOTIFICATION_TYPE", null);
    
    if (notificationType != null && "vibrate".equals(notificationType)) {
        return true;
    }
    
    // 回退到全局震动配置
    return isVibrationActivated();
}

// 预提醒开关（每个祷告独立）
public boolean isPreReminderEnabledForPrayer(PrayerEnum prayer) {
    SharedPreferences prayerPrefs = context.getSharedPreferences(
        PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, Context.MODE_PRIVATE);
    
    return prayerPrefs.getBoolean(prayer + "_PRE_REMINDER", false);
}

// 预提醒间隔（每个祷告独立）
public int getPreReminderMinutesForPrayer(PrayerEnum prayer) {
    SharedPreferences prayerPrefs = context.getSharedPreferences(
        PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, Context.MODE_PRIVATE);
    
    int minutes = prayerPrefs.getInt(prayer + "_PRE_REMINDER_MINUTES", 0);
    
    // 如果没有独立配置，回退到全局配置
    if (minutes == 0) {
        return getReminderInterval();
    }
    
    return minutes;
}
```

#### 2. 修改 `PrayerAlarmScheduler.java`

修改 `scheduleReminders` 方法支持每个祷告独立配置：

```java
private void scheduleReminders(@NonNull DayPrayer dayPrayer) {
    Log.i(TAG, "Start scheduling Reminders for: " + dayPrayer.getDate());

    Map<PrayerEnum, LocalDateTime> timings = dayPrayer.getTimings();

    int index = 10;
    for (PrayerEnum key : timings.keySet()) {
        index++;

        // ✅ 检查该祷告是否启用了预提醒
        boolean preReminderEnabled = preferencesHelper.isPreReminderEnabledForPrayer(key);
        if (!preReminderEnabled) {
            Log.i(TAG, "Pre-reminder disabled for " + key.toString() + ", skipping");
            continue;  // 跳过未启用预提醒的祷告
        }

        // ✅ 获取该祷告的预提醒间隔
        int reminderInterval = preferencesHelper.getPreReminderMinutesForPrayer(key);
        
        LocalDateTime prayerTiming = timings.get(key);
        LocalDateTime reminderTiming = Objects.requireNonNull(prayerTiming).minusMinutes(reminderInterval);

        if (LocalDateTime.now().isBefore(reminderTiming)) {
            Log.i(TAG, "Scheduling " + key.toString() + " Reminder at : " + TimingUtils.formatTiming(reminderTiming) + " (" + reminderInterval + " minutes before)");

            scheduleNotifications(dayPrayer, prayerTiming, TimingType.STANDARD, key.toString(),
                    2000, index, reminderTiming, ReminderReceiver.class);
        }
    }

    Log.i(TAG, "End scheduling Reminders for: " + dayPrayer.getDate());
}
```

#### 3. 修改 `ReminderNotification.java`

修改震动逻辑支持每个祷告独立配置：

```java
public void createNotification(String prayerKey, String prayerName, LocalDateTime prayerTiming, boolean isComplementaryTiming) {
    // ... (其他代码不变)

    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
    notificationManager.notify(notificationId, builder.build());

    // ✅ 检查该祷告是否启用震动
    PrayerEnum prayer = PrayerEnum.valueOf(prayerKey);
    boolean vibrationEnabled = preferencesHelper.isVibrationActivatedForPrayer(prayer);
    
    if (vibrationEnabled) {
        createVibration();
    }

    setupCall(!isComplementaryTiming, prayerKey);
}
```

---

### 方案 B：简化新页面（不推荐）❌

**缺点**：
- 失去每个祷告独立配置的能力
- 需要大幅修改新页面
- 用户体验倒退

---

## 📊 **修复验证步骤**

### 1. 测试场景

1. **Fajr 祷告**：开启震动 + 提前 5 分钟提醒
2. **Dhuhr 祷告**：关闭震动 + 提前 10 分钟提醒
3. **Asr 祷告**：开启震动 + 不开启预提醒

### 2. 验证点

- [ ] 设置保存成功（查看 Logcat）
- [ ] 通知调度系统读取到正确配置
- [ ] Fajr 提前 5 分钟收到震动提醒
- [ ] Dhuhr 提前 10 分钟收到静默提醒（无震动）
- [ ] Asr 无预提醒（仅祷告时间提醒）

### 3. Debug 日志

在关键位置添加日志：

```java
// PreferencesHelper
Log.d("PreferencesHelper", "✅ " + prayer + " pre-reminder: " + enabled + ", minutes: " + minutes);

// PrayerAlarmScheduler
Log.d("PrayerAlarmScheduler", "📅 Scheduling " + prayer + " reminder " + minutes + " minutes before at " + reminderTiming);

// ReminderNotification
Log.d("ReminderNotification", "📳 Vibration for " + prayer + ": " + vibrationEnabled);
```

---

## 🎯 **总结**

### 问题本质

新通知设置页面和旧通知调度系统使用了 **不同的 SharedPreferences 存储位置和 Key**，导致数据流断裂。

### 推荐方案

采用 **方案 A**：修改通知调度系统，支持每个祷告独立配置，同时保留全局配置作为回退。

### 优先级

🔴 **高优先级** - 影响核心通知功能，需要尽快修复。

---

## 📝 **相关文件清单**

| 文件 | 作用 | 需要修改 |
|------|------|---------|
| `PrayerNotificationSettingsActivity.java` | 设置页面 | ❌ 无需修改 |
| `PreferencesHelper.java` | 配置读取 | ✅ 添加独立方法 |
| `PrayerAlarmScheduler.java` | 通知调度 | ✅ 修改调度逻辑 |
| `ReminderNotification.java` | 提醒通知 | ✅ 修改震动逻辑 |
| `PrayerNotification.java` | 祷告通知 | ⚠️ 可能需要修改 |

---

**生成时间**: 2025-01-01  
**版本**: v1.6.8

