# Firebase Salah Recording Setup Guide

## 🔥 问题诊断

从日志中发现 **Firebase Firestore 权限错误**：

```
PERMISSION_DENIED: Missing or insufficient permissions.
```

这是因为 Firestore 安全规则中没有配置 `salahRecords` 集合的访问权限。

---

## ✅ 解决方案：更新 Firestore 安全规则

### 步骤 1：打开 Firebase Console

1. 访问 [Firebase Console](https://console.firebase.google.com/)
2. 选择你的项目（Quran Majeed App）

### 步骤 2：导航到 Firestore Database

1. 在左侧菜单中，点击 **Firestore Database**
2. 点击顶部的 **规则（Rules）** 标签

### 步骤 3：更新安全规则

将以下规则**添加**到现有规则中（或替换整个规则文件）：

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ========================================
    // Daily Quests - User Quest Configurations
    // ========================================
    match /users/{userId}/userQuestConfig/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // Daily Quests - Daily Progress
    // ========================================
    match /users/{userId}/dailyProgress/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // Daily Quests - Streak Stats
    // ========================================
    match /users/{userId}/streakStats/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // Daily Quests - User Learning State
    // ========================================
    match /users/{userId}/userLearningState/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // 📿 Salah Records (Prayer Tracking) - NEW!
    // ========================================
    match /users/{userId}/salahRecords/{dateId} {
      // Allow read and write only if the user is authenticated and accessing their own data
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // Validate data structure for writes
      allow create: if request.auth != null 
                    && request.auth.uid == userId
                    && request.resource.data.keys().hasAll(['userId', 'dateId', 'fajr', 'dhuhr', 'asr', 'maghrib', 'isha'])
                    && request.resource.data.userId == userId
                    && request.resource.data.dateId is string
                    && request.resource.data.fajr is bool
                    && request.resource.data.dhuhr is bool
                    && request.resource.data.asr is bool
                    && request.resource.data.maghrib is bool
                    && request.resource.data.isha is bool;
      
      allow update: if request.auth != null 
                    && request.auth.uid == userId
                    && request.resource.data.userId == userId;
    }
    
    // ========================================
    // Default Deny All Other Collections
    // ========================================
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

### 步骤 4：发布规则

1. 点击 **发布（Publish）** 按钮
2. 等待规则生效（通常是即时的）

### 步骤 5：验证规则

1. 在 Firebase Console 中，点击 **规则测试（Rules Playground）** 标签
2. 测试以下场景：

**测试 1：读取自己的祷告记录**
```
Location: /users/YOUR_USER_ID/salahRecords/2025-10-23
Operation: get
Auth: Authenticated as YOUR_USER_ID
Expected Result: ✅ Allow
```

**测试 2：写入自己的祷告记录**
```
Location: /users/YOUR_USER_ID/salahRecords/2025-10-23
Operation: create
Auth: Authenticated as YOUR_USER_ID
Data:
{
  "userId": "YOUR_USER_ID",
  "dateId": "2025-10-23",
  "fajr": true,
  "dhuhr": false,
  "asr": false,
  "maghrib": false,
  "isha": false
}
Expected Result: ✅ Allow
```

**测试 3：尝试读取他人的祷告记录**
```
Location: /users/OTHER_USER_ID/salahRecords/2025-10-23
Operation: get
Auth: Authenticated as YOUR_USER_ID
Expected Result: ❌ Deny
```

---

## 📝 数据结构

### Firestore 路径
```
/users/{userId}/salahRecords/{dateId}
```

### 文档结构
```json
{
  "userId": "A79QknedAnhVr13MTkRKm1nRXxq1",
  "dateId": "2025-10-23",
  "fajr": true,
  "dhuhr": true,
  "asr": true,
  "maghrib": false,
  "isha": true,
  "lastUpdatedUtc": "2025-10-23T08:34:18.573Z",
  "createdAt": "2025-10-23T00:00:00.000Z"
}
```

---

## 🔧 代码修复总结

### 1. **PrayersFragment.java** - 崩溃修复
- ✅ 添加空指针检查
- ✅ 添加 Fragment 生命周期检查（`isAdded()`, `getContext()`）
- ✅ 改进异常处理
- ✅ 添加资源清理（`onDestroy()`）

### 2. **防止崩溃的关键改进**
```java
// 在所有 Google Sign-In 相关方法中添加：
if (!isAdded() || getContext() == null) {
    Log.w("PrayersFragment", "Fragment not attached");
    return;
}

if (googleAuthManager == null) {
    Log.e("PrayersFragment", "GoogleAuthManager is null!");
    return;
}
```

---

## 🧪 测试步骤

### 1. 更新 Firebase 规则后
```bash
# 1. 编译并安装新版本
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 2. 清空日志
adb logcat -c

# 3. 启动应用
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity

# 4. 监控日志
adb logcat | grep -E "(PrayersFragment|SalahRepository|SalahViewModel)"
```

### 2. 测试场景

#### 场景 A：未登录用户
1. 打开 Salat 页面
2. **预期**：所有按钮显示 "Track"（绿色按钮，颜色 #52BF95）
3. 点击任意 "Track" 按钮
4. **预期**：弹出 "Login Required" 对话框
5. 点击 "Login with Google"
6. **预期**：打开 Google 登录对话框（不应崩溃）
7. 完成登录
8. **预期**：
   - Toast 显示 "Login successful! ✅"
   - 按钮状态自动更新（如果之前有记录）

#### 场景 B：已登录用户
1. 打开 Salat 页面
2. **预期**：
   - 按钮立即显示（默认"Track"状态）
   - 1-2秒后，按钮状态根据 Firebase 数据更新
   - 已完成的祷告显示 "✓"（绿色背景）
   - 未完成的祷告显示 "Track"（#52BF95 背景）
3. 点击未完成的祷告按钮
4. **预期**：
   - 按钮立即变为 "✓"
   - Firebase 数据库更新
   - 无崩溃，无权限错误
5. 再次点击同一按钮
6. **预期**：
   - 按钮变回 "Track"
   - Firebase 数据库更新

---

## ❌ 常见问题排查

### 问题 1：仍然显示 PERMISSION_DENIED
**解决方案**：
- 确认 Firebase 规则已发布
- 确认用户已登录（`FirebaseAuth.getInstance().getCurrentUser() != null`）
- 确认 Firestore 路径格式正确
- 等待 1-2 分钟，Firebase 规则缓存可能需要时间更新

### 问题 2：Google Sign-In 仍然崩溃
**解决方案**：
- 确认已应用所有代码修复
- 检查 `GoogleAuthManager` 是否正确初始化
- 检查 Web Client ID 是否正确配置
- 重新编译并完全卸载应用再安装

### 问题 3：按钮状态不更新
**解决方案**：
- 检查 Firebase 规则是否正确
- 检查日志中的 `SalahRepository` 输出
- 确认 `startObservingSalahRecords()` 被调用
- 确认 LiveData 观察者正常工作

---

## 📊 日志关键字

监控以下关键字以诊断问题：

```bash
# 成功的登录流程
✅ GoogleAuthManager initialized successfully
Google Sign-In intent launched successfully
Firebase authentication successful

# 成功的数据同步
Observing salah record at: users/.../salahRecords/2025-10-23
Salah record updated: 4/5 completed
🎨 Updating button ... to ✓ (completed)

# 错误
❌ PERMISSION_DENIED
GoogleAuthManager is null
Fragment not attached
```

---

## 🎯 预期最终结果

更新 Firebase 规则并应用代码修复后：

1. ✅ 未登录用户可以看到所有 "Track" 按钮
2. ✅ 点击按钮触发 Google 登录（无崩溃）
3. ✅ 登录后，按钮状态立即同步
4. ✅ 已登录用户打开页面时，按钮立即显示并在1-2秒内更新
5. ✅ 点击按钮切换状态，Firebase 实时同步
6. ✅ 无权限错误，无崩溃
7. ✅ 按钮颜色正确：Track (#52BF95), Completed (绿色)

---

**重要提示**：请先在 Firebase Console 中更新 Firestore 规则，然后再重新编译和测试应用！

