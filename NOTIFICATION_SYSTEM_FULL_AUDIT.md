# 🔔 通知系统全面排查报告

## 📋 排查范围

用户在新设置页面可以配置的所有通知类型：

1. **None** - 无通知
2. **Azan** - 宣礼（播放音频）
3. **Vibrate** - 震动
4. **Silent** - 静默通知
5. **Text Tone** - 短信音
6. **Clock Sound** - 闹钟音

**附加配置**：
- 音量（Volume）
- Azan 音频选择
- 预提醒开关（Pre-reminder）
- 预提醒时间（1-30分钟）

---

## 🔍 逐项排查结果

### 1️⃣ **None（无通知）**

#### 新页面保存：
```java
// PrayerNotificationSettingsActivity.java:483-496
private void saveNotificationType(String type) {
    SharedPreferences.Editor editor = preferences.edit();
    editor.putString(prayerEnum + "_NOTIFICATION_TYPE", type);  // 例如：FAJR_NOTIFICATION_TYPE = "none"
    
    // 同时更新旧的布尔值（向后兼容）
    boolean enabled = !TYPE_NONE.equals(type);
    String callPreferenceKey = prayerEnum + PreferencesConstants.ADTHAN_CALL_ENABLED_KEY;
    editor.putBoolean(callPreferenceKey, enabled);  // 例如：FAJR_ADTHAN_CALL_ENABLED = false
    
    editor.apply();
}
```

#### 通知系统读取：
```java
// PrayerNotification.java:132-142
private void setupAdhanCall(String prayerKey) {
    String callPreferenceKey = prayerKey + PreferencesConstants.ADTHAN_CALL_ENABLED_KEY;
    
    final SharedPreferences sharedPreferences = context.getSharedPreferences(
        PreferencesConstants.ADTHAN_CALLS_SHARED_PREFERENCES, MODE_PRIVATE);
    boolean callEnabled = sharedPreferences.getBoolean(callPreferenceKey, false);
    
    if (callEnabled) {
        adhanPlayer.playAdhan(PrayerEnum.FAJR.toString().equals(prayerKey));
    }
}
```

**结果**：✅ **生效** - 通过向后兼容的 `FAJR_ADTHAN_CALL_ENABLED` 字段

---

### 2️⃣ **Azan（宣礼）**

#### 新页面保存：
```java
// 保存的配置：
FAJR_NOTIFICATION_TYPE = "azan"
FAJR_ADTHAN_CALL_ENABLED = true  // 向后兼容
FAJR_AZAN_NAME = "random_fajr_adhan"  // 音频文件
FAJR_VOLUME = 80
```

#### 通知系统读取：
```java
// PrayerNotification.java:132-142
private void setupAdhanCall(String prayerKey) {
    // ✅ 读取开关：FAJR_ADTHAN_CALL_ENABLED
    boolean callEnabled = sharedPreferences.getBoolean(callPreferenceKey, false);
    
    if (callEnabled) {
        adhanPlayer.playAdhan(PrayerEnum.FAJR.toString().equals(prayerKey));  // ✅ 播放音频
    }
}

// PreferencesHelper.java:205-213
public String getFajrAdhanCaller() {
    return defaultSharedPreferences.getString(
        PreferencesConstants.ADTHAN_FAJR_CALLER,  // ❌ 读取全局配置
        UiUtils.uriFromRaw(PreferencesConstants.SHORT_PRAYER_CALL, context).toString()
    );
}

public String getAdhanCaller() {
    return defaultSharedPreferences.getString(
        PreferencesConstants.ADTHAN_CALLER,  // ❌ 读取全局配置
        UiUtils.uriFromRaw(PreferencesConstants.SHORT_PRAYER_CALL, context).toString()
    );
}
```

**结果**：
- ✅ **Azan 开关生效** - 通过 `FAJR_ADTHAN_CALL_ENABLED` 
- ❌ **Azan 音频选择不生效** - 读取的是全局配置，而非独立配置
- ❌ **音量不生效** - 没有读取 `FAJR_VOLUME`

---

### 3️⃣ **Vibrate（震动）**

#### 新页面保存：
```java
FAJR_NOTIFICATION_TYPE = "vibrate"
FAJR_ADTHAN_CALL_ENABLED = true
```

#### 通知系统读取：
```java
// PrayerNotification.java:106-108
if (preferencesHelper.isVibrationActivated()) {  // ❌ 读取全局配置
    createVibration();
}

// PreferencesHelper.java:200-203
public Boolean isVibrationActivated() {
    return defaultSharedPreferences.getBoolean(
        PreferencesConstants.ADHAN_VIBRATION_PREFERENCE, true);  // ❌ 全局震动配置
}
```

**结果**：❌ **不生效** - 系统只检查全局震动配置，不检查独立的 `FAJR_NOTIFICATION_TYPE`

---

### 4️⃣ **Silent（静默通知）**

#### 新页面保存：
```java
FAJR_NOTIFICATION_TYPE = "silent"
FAJR_ADTHAN_CALL_ENABLED = true
```

#### 通知系统读取：
```java
// PrayerNotification.java:104-110
notificationManager.notify(notificationId, builder.build());  // ✅ 显示通知

if (preferencesHelper.isVibrationActivated()) {  // ❌ 仍然可能震动（如果全局开启）
    createVibration();
}

setupAdhanCall(prayerKey);  // ❌ 仍然可能播放音频（如果开关开启）
```

**结果**：❌ **不生效** - 没有读取 `NOTIFICATION_TYPE`，无法区分 Silent 和 Azan

---

### 5️⃣ **Text Tone（短信音）**

#### 新页面保存：
```java
FAJR_NOTIFICATION_TYPE = "text_tone"
FAJR_ADTHAN_CALL_ENABLED = true
```

#### 通知系统读取：
```java
// PrayerNotification.java - 没有实现短信音逻辑
```

**结果**：❌ **未实现** - 系统没有短信音的播放逻辑

---

### 6️⃣ **Clock Sound（闹钟音）**

#### 新页面保存：
```java
FAJR_NOTIFICATION_TYPE = "clock"
FAJR_ADTHAN_CALL_ENABLED = true
FAJR_VOLUME = 80
```

#### 通知系统读取：
```java
// PrayerNotification.java - 没有实现闹钟音逻辑
```

**结果**：❌ **未实现** - 系统没有闹钟音的播放逻辑

---

### 7️⃣ **Volume（音量）**

#### 新页面保存：
```java
FAJR_VOLUME = 80
```

#### 通知系统读取：
```java
// AdhanPlayer.java - 没有找到读取独立音量的代码
// 可能使用系统音量或全局配置
```

**结果**：❌ **不生效** - 没有读取独立的 `FAJR_VOLUME` 配置

---

### 8️⃣ **Azan Name（音频选择）**

#### 新页面保存：
```java
// PrayerNotificationSettingsActivity.java:596-601
if (isFajr) {
    editor.putString(PreferencesConstants.ADTHAN_FAJR_CALLER, azanUri.toString());
} else {
    editor.putString(PreferencesConstants.ADTHAN_CALLER, azanUri.toString());
}
```

#### 通知系统读取：
```java
// PreferencesHelper.java:205-213
public String getFajrAdhanCaller() {
    return defaultSharedPreferences.getString(
        PreferencesConstants.ADTHAN_FAJR_CALLER,  // ✅ 正确的 Key
        ...
    );
}
```

**结果**：✅ **生效** - 保存到了正确的全局配置位置

---

### 9️⃣ **Pre-Reminder（预提醒）**

#### 新页面保存：
```java
FAJR_PRE_REMINDER = true
FAJR_PRE_REMINDER_MINUTES = 5
```

#### 通知系统读取：
```java
// PrayerAlarmScheduler.java:55-57
if (preferencesHelper.isReminderEnabled()) {  // ❌ 读取全局开关
    scheduleReminders(dayPrayer);
}

// PrayerAlarmScheduler.java:114
int reminderInterval = preferencesHelper.getReminderInterval();  // ❌ 读取全局间隔（默认10分钟）

// PreferencesHelper.java:230-243
public boolean isReminderEnabled() {
    return defaultSharedPreferences.getBoolean(
        PreferencesConstants.ADTHAN_REMINDER_ENABLED, true);  // ❌ 全局配置
}

public int getReminderInterval() {
    return defaultSharedPreferences.getInt(
        PreferencesConstants.ADTHAN_REMINDER_INTERVAL, 10);  // ❌ 全局配置
}
```

**结果**：❌ **不生效** - 完全读取全局配置，独立设置被忽略

---

### 🔟 **Pre-Reminder Vibration（预提醒震动）**

#### 新页面保存：
```java
FAJR_NOTIFICATION_TYPE = "vibrate"  // 或其他类型
```

#### 通知系统读取：
```java
// ReminderNotification.java:110-112
if (preferencesHelper.isVibrationActivated()) {  // ❌ 读取全局配置
    createVibration();
}
```

**结果**：❌ **不生效** - 预提醒使用全局震动配置

---

## 📊 **问题汇总表**

| 功能 | 新页面配置 Key | 系统读取 Key | 是否生效 | 问题类型 |
|------|---------------|--------------|---------|---------|
| 通知开关 | `FAJR_ADTHAN_CALL_ENABLED` | `FAJR_ADTHAN_CALL_ENABLED` | ✅ 生效 | 无 |
| Azan 音频 | `FAJR_AZAN_NAME` | `ADTHAN_FAJR_CALLER` (全局) | ⚠️ 部分生效 | 保存到了正确位置 |
| 音量 | `FAJR_VOLUME` | 未读取 | ❌ 不生效 | 未实现 |
| 震动（正时） | `FAJR_NOTIFICATION_TYPE` | `ADHAN_VIBRATION_PREFERENCE` (全局) | ❌ 不生效 | Key 不匹配 |
| 静默通知 | `FAJR_NOTIFICATION_TYPE = "silent"` | 未读取 | ❌ 不生效 | 未实现逻辑 |
| 短信音 | `FAJR_NOTIFICATION_TYPE = "text_tone"` | 未读取 | ❌ 不生效 | 未实现功能 |
| 闹钟音 | `FAJR_NOTIFICATION_TYPE = "clock"` | 未读取 | ❌ 不生效 | 未实现功能 |
| 预提醒开关 | `FAJR_PRE_REMINDER` | `ADTHAN_REMINDER_ENABLED` (全局) | ❌ 不生效 | Key 不匹配 |
| 预提醒时间 | `FAJR_PRE_REMINDER_MINUTES` | `ADTHAN_REMINDER_INTERVAL` (全局) | ❌ 不生效 | Key 不匹配 |
| 预提醒震动 | `FAJR_NOTIFICATION_TYPE` | `ADHAN_VIBRATION_PREFERENCE` (全局) | ❌ 不生效 | Key 不匹配 |

---

## 🔥 **核心问题分类**

### 类型 A：Key 不匹配（需要修改读取逻辑）
1. ❌ 震动（正时通知）
2. ❌ 预提醒开关
3. ❌ 预提醒时间
4. ❌ 预提醒震动

### 类型 B：功能未实现（需要新增逻辑）
1. ❌ 静默通知（Silent）
2. ❌ 短信音（Text Tone）
3. ❌ 闹钟音（Clock Sound）
4. ❌ 独立音量控制

### 类型 C：已生效（无需修改）
1. ✅ 通知开关
2. ✅ Azan 音频选择

---

## 🛠️ **修复方案优先级**

### 🔴 **P0 - 高优先级（影响核心功能）**
1. ✅ 预提醒开关和时间
2. ✅ 震动通知类型识别
3. ✅ 预提醒震动

### 🟡 **P1 - 中优先级（完善功能）**
4. ⚠️ 静默通知逻辑
5. ⚠️ 独立音量控制

### 🟢 **P2 - 低优先级（扩展功能）**
6. ⏸️ 短信音（可选）
7. ⏸️ 闹钟音（可选）

---

## 📝 **修复计划**

### 阶段 1：修复 Key 不匹配问题（P0）

#### 1. 修改 `PreferencesHelper.java`
添加读取每个祷告独立配置的方法：
```java
// 读取通知类型
public String getNotificationTypeForPrayer(PrayerEnum prayer);

// 读取震动设置
public boolean isVibrationEnabledForPrayer(PrayerEnum prayer);

// 读取预提醒开关
public boolean isPreReminderEnabledForPrayer(PrayerEnum prayer);

// 读取预提醒时间
public int getPreReminderMinutesForPrayer(PrayerEnum prayer);

// 读取独立音量
public int getVolumeForPrayer(PrayerEnum prayer);
```

#### 2. 修改 `PrayerNotification.java`
根据 `NOTIFICATION_TYPE` 决定通知行为：
```java
public void createNotification(Intent intent) {
    // ... 现有代码
    
    PrayerEnum prayer = PrayerEnum.valueOf(prayerKey);
    String notificationType = preferencesHelper.getNotificationTypeForPrayer(prayer);
    
    switch (notificationType) {
        case "none":
            return;  // 不显示通知
        case "azan":
            setupAdhanCall(prayerKey);
            break;
        case "vibrate":
            createVibration();
            break;
        case "silent":
            // 仅显示通知，无声音和震动
            break;
        case "text_tone":
            playTextTone();
            break;
        case "clock":
            playClockSound();
            break;
    }
}
```

#### 3. 修改 `ReminderNotification.java`
支持独立的预提醒配置：
```java
public void createNotification(...) {
    // ... 现有代码
    
    PrayerEnum prayer = PrayerEnum.valueOf(prayerKey);
    String notificationType = preferencesHelper.getNotificationTypeForPrayer(prayer);
    
    // 根据通知类型决定震动
    if ("vibrate".equals(notificationType)) {
        createVibration();
    }
}
```

#### 4. 修改 `PrayerAlarmScheduler.java`
支持每个祷告独立的预提醒时间：
```java
private void scheduleReminders(@NonNull DayPrayer dayPrayer) {
    for (PrayerEnum key : timings.keySet()) {
        // 检查该祷告是否启用预提醒
        if (!preferencesHelper.isPreReminderEnabledForPrayer(key)) {
            continue;
        }
        
        // 获取该祷告的预提醒时间
        int reminderInterval = preferencesHelper.getPreReminderMinutesForPrayer(key);
        
        // 调度提醒
        LocalDateTime reminderTiming = prayerTiming.minusMinutes(reminderInterval);
        scheduleNotifications(...);
    }
}
```

### 阶段 2：实现静默通知和音量控制（P1）

1. 静默通知：仅显示通知栏，无声音和震动
2. 音量控制：使用 `AudioManager` 设置播放音量

### 阶段 3：实现短信音和闹钟音（P2，可选）

---

## ✅ **修复验证测试**

### 测试场景 1：预提醒
- **Fajr**：开启预提醒，提前 5 分钟，震动
- **Dhuhr**：开启预提醒，提前 10 分钟，无震动
- **Asr**：关闭预提醒

### 测试场景 2：通知类型
- **Fajr**：Azan（宣礼音频）
- **Dhuhr**：Vibrate（仅震动）
- **Asr**：Silent（静默）
- **Maghrib**：None（无通知）

### 测试场景 3：音量
- **Fajr**：音量 50%
- **Dhuhr**：音量 80%

---

**生成时间**: 2025-01-01  
**版本**: v1.6.8

