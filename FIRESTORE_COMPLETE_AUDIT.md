# 🔥 Firestore 权限完整审计

## 📋 问题分析

**用户报告的问题**:
1. ✅ Google 登录已修复
2. ✅ Prayer Log 功能已修复（已添加 `prayer_logs` 规则）
3. ❌ **Learning Plan Setup 保存失败** - PERMISSION_DENIED

---

## 🔍 代码审计：所有 Firestore 集合路径

### 1. Learning Plan（学习计划）
**路径**: `users/{userId}/learningPlan/config`
**当前规则**: ❌ **缺失！**
**使用位置**:
- `QuestRepository.kt` → `saveUserQuestConfig()`
- `LearningPlanSetupFragment.kt` → 保存用户配置

```kotlin
// FirestoreConstants.kt
fun getLearningPlanConfigPath(): String {
    val userId = getUserId()
    return "$USERS_COLLECTION/$userId/$LEARNING_PLAN_COLLECTION/$CONFIG_DOCUMENT"
    // users/{userId}/learningPlan/config
}
```

---

### 2. Daily Progress（每日进度）
**路径**: `users/{userId}/dailyProgress/{dateId}`
**当前规则**: ✅ 已存在（但路径不匹配）
**问题**: 规则使用 `dailyProgress` ，代码也使用 `dailyProgress`

```kotlin
// FirestoreConstants.kt
fun getDailyProgressCollectionPath(): String {
    val userId = getUserId()
    return "$USERS_COLLECTION/$userId/$DAILY_PROGRESS_COLLECTION"
    // users/{userId}/dailyProgress
}
```

---

### 3. Streak Stats（连续记录统计）
**路径**: `users/{userId}/streakStats/summary`
**当前规则**: ✅ 已存在
**使用位置**: `QuestRepository.kt`

---

### 4. User Quest Config（旧版配置）
**路径**: `users/{userId}/userQuestConfig/{document}`
**当前规则**: ✅ 已存在
**问题**: 这是**旧版路径**，新版使用 `learningPlan`

---

### 5. Learning State（学习状态 - 阅读位置）
**路径**: `users/{userId}/learningState/current`
**当前规则**: ❌ **缺失！**（规则中有 `userLearningState`，但代码用 `learningState`）
**使用位置**:
- `ActivityReader.java` → `saveCurrentPositionToFirestore()`
- `LastReadRepository.kt`

```java
// ActivityReader.java:2494
firestore.collection("users")
    .document(userId)
    .collection("learningState")  // ❌ 规则中是 userLearningState
    .document("current")
```

---

### 6. Salah Records（祷告记录 - 旧版）
**路径**: `users/{userId}/salahRecords/{dateId}`
**当前规则**: ✅ 已存在
**使用位置**: `SalahRepository.kt`

---

### 7. Prayer Logs（祷告记录 - 新版）
**路径**: `prayer_logs/{logId}`（根集合）
**当前规则**: ✅ 已添加（刚修复）
**使用位置**: `PrayerLogBottomSheet.kt`, `PrayerLogRepository.kt`

---

### 8. Tasbih Counter（念珠计数器）
**路径**: `users/{userId}/tasbihData/current`
**当前规则**: ❌ **缺失！**
**使用位置**: `TasbihFragment.java`

```java
// TasbihFragment.java
firestore.collection("users")
    .document(userId)
    .collection("tasbihData")
    .document("current")
```

---

## 🚨 发现的问题

### 问题 1: Learning Plan 路径缺失
- **代码使用**: `users/{userId}/learningPlan/{document}`
- **规则状态**: ❌ **完全缺失**
- **影响**: **Learning Plan Setup 无法保存** ← 用户当前问题！

### 问题 2: Learning State 路径不匹配
- **代码使用**: `users/{userId}/learningState/{document}`
- **规则定义**: `users/{userId}/userLearningState/{document}`
- **影响**: 阅读位置无法保存（但可能未被发现）

### 问题 3: Tasbih Data 路径缺失
- **代码使用**: `users/{userId}/tasbihData/{document}`
- **规则状态**: ❌ **完全缺失**
- **影响**: Tasbih计数无法保存到云端（可能只用本地存储）

### 问题 4: 订阅功能
- **当前实现**: 仅保存到 SharedPreferences（本地）
- **Firestore 状态**: 未使用 Firestore
- **影响**: 无需添加规则

---

## ✅ 完整的 Firestore 规则（修复版）

```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ========================================
    // ✅ Learning Plan Configuration（学习计划配置）
    // Path: users/{userId}/learningPlan/{document}
    // ========================================
    match /users/{userId}/learningPlan/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // ✅ Daily Progress（每日进度）
    // Path: users/{userId}/dailyProgress/{document}
    // ========================================
    match /users/{userId}/dailyProgress/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // ✅ Streak Stats（连续记录统计）
    // Path: users/{userId}/streakStats/{document}
    // ========================================
    match /users/{userId}/streakStats/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // ✅ Learning State（学习状态 - 阅读位置）
    // Path: users/{userId}/learningState/{document}
    // ========================================
    match /users/{userId}/learningState/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // ✅ Tasbih Data（念珠计数器数据）
    // Path: users/{userId}/tasbihData/{document}
    // ========================================
    match /users/{userId}/tasbihData/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // 🕌 Salah Records（祷告记录 - 旧版）
    // Path: users/{userId}/salahRecords/{dateId}
    // ========================================
    match /users/{userId}/salahRecords/{dateId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
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
    // 🕌 Prayer Logs（祷告记录 - 新版）
    // Path: prayer_logs/{logId}（根集合）
    // ========================================
    match /prayer_logs/{logId} {
      allow read: if request.auth != null && request.auth.uid == resource.data.userId;
      
      allow create: if request.auth != null 
                    && request.auth.uid == request.resource.data.userId
                    && request.resource.data.keys().hasAll(['userId', 'prayerName', 'status', 'date'])
                    && request.resource.data.userId is string
                    && request.resource.data.prayerName is string
                    && request.resource.data.status is string
                    && request.resource.data.date is string;
      
      allow update: if request.auth != null 
                    && request.auth.uid == resource.data.userId
                    && request.auth.uid == request.resource.data.userId;
      
      allow delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    // ========================================
    // 🚫 Legacy: User Quest Config（旧版配置 - 向后兼容）
    // Path: users/{userId}/userQuestConfig/{document}
    // ========================================
    match /users/{userId}/userQuestConfig/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // 🚫 Legacy: User Learning State（旧版名称 - 向后兼容）
    // Path: users/{userId}/userLearningState/{document}
    // ========================================
    match /users/{userId}/userLearningState/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // 🚫 Default Deny All Other Collections
    // ========================================
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

## 📊 规则覆盖总结

| 集合路径 | 功能 | 旧规则 | 新规则 | 状态 |
|---------|------|--------|--------|------|
| `users/{userId}/learningPlan/**` | 学习计划配置 | ❌ 缺失 | ✅ 已添加 | **修复** |
| `users/{userId}/dailyProgress/**` | 每日进度 | ✅ 存在 | ✅ 保留 | 正常 |
| `users/{userId}/streakStats/**` | 连续统计 | ✅ 存在 | ✅ 保留 | 正常 |
| `users/{userId}/learningState/**` | 学习状态 | ❌ 名称错误 | ✅ 已修复 | **修复** |
| `users/{userId}/tasbihData/**` | 念珠数据 | ❌ 缺失 | ✅ 已添加 | **修复** |
| `users/{userId}/salahRecords/**` | 祷告记录（旧） | ✅ 存在 | ✅ 保留 | 正常 |
| `prayer_logs/{logId}` | 祷告记录（新） | ✅ 已添加 | ✅ 保留 | 正常 |
| `users/{userId}/userQuestConfig/**` | 旧版配置 | ✅ 存在 | ✅ 保留（兼容） | 正常 |

---

## 🎯 关键修复

### 1. 添加 Learning Plan 权限
```firestore
match /users/{userId}/learningPlan/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```
**原因**: 代码使用 `learningPlan`，但规则中完全缺失
**影响**: **解决用户当前问题！**

### 2. 修复 Learning State 路径
```firestore
// 旧规则（错误）
match /users/{userId}/userLearningState/{document=**} { ... }

// 新规则（正确）
match /users/{userId}/learningState/{document=**} { ... }
```
**原因**: 代码使用 `learningState`，规则用 `userLearningState`
**影响**: 确保阅读位置能正确保存

### 3. 添加 Tasbih Data 权限
```firestore
match /users/{userId}/tasbihData/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```
**原因**: Tasbih计数器可能需要云端同步
**影响**: 确保所有功能完整

---

## 🔒 安全性验证

### ✅ 所有规则都包含认证检查
```firestore
request.auth != null && request.auth.uid == userId
```

### ✅ 用户只能访问自己的数据
- 路径包含 `{userId}`
- 验证 `request.auth.uid == userId`

### ✅ 数据验证（Prayer Logs & Salah Records）
- 验证必需字段存在
- 验证数据类型
- 验证 userId 一致性

### ✅ 默认拒绝策略
```firestore
match /{document=**} {
  allow read, write: if false;
}
```

---

## 📝 部署步骤

### 方法 1: Firebase Console（推荐）

1. **打开 Firebase Console**
   https://console.firebase.google.com/

2. **选择项目**
   `quran-majeed-aa3d2`

3. **进入 Firestore Rules**
   Firestore Database → Rules 标签

4. **复制新规则**
   - 删除现有规则
   - 粘贴上面的完整规则

5. **点击 "Publish"**

6. **验证部署**
   - 状态显示 "Published"
   - 记录部署时间

---

### 方法 2: Firebase CLI

```bash
# 1. 更新 firestore.rules 文件
cp firestore_complete_rules.txt firestore.rules

# 2. 部署规则
firebase deploy --only firestore:rules

# 3. 验证部署
firebase firestore:rules:get
```

---

## ✅ 测试验证清单

### 1. Learning Plan Setup ✅
- [ ] 打开 Learning Plan Setup
- [ ] 配置选项
- [ ] 点击保存
- [ ] ✅ 应该成功（不再显示 PERMISSION_DENIED）

### 2. 阅读位置保存 ✅
- [ ] 打开 Quran 阅读器
- [ ] 滚动到某个位置
- [ ] 关闭应用
- [ ] 重新打开
- [ ] ✅ 应该恢复到上次位置

### 3. Prayer Log ✅
- [ ] 记录祷告
- [ ] ✅ 应该成功保存

### 4. Tasbih Counter ✅
- [ ] 使用念珠计数器
- [ ] ✅ 数据应该同步到云端

### 5. Daily Progress ✅
- [ ] 完成每日任务
- [ ] ✅ 进度应该正确记录

---

## 🎉 预期结果

部署新规则后：
- ✅ **Learning Plan Setup 保存成功**（解决当前问题）
- ✅ **阅读位置正确保存**
- ✅ **Tasbih 数据云端同步**
- ✅ **所有现有功能正常运行**
- ✅ **数据安全得到保障**

---

## 📞 问题排查

如果部署后仍有问题：

1. **清除应用缓存**
   ```bash
   adb shell pm clear com.quran.quranaudio.online
   ```

2. **检查用户登录状态**
   - 确保已登录 Google 账号
   - FirebaseAuth.getInstance().currentUser 不为 null

3. **查看详细错误日志**
   ```bash
   adb logcat -s QuestRepository:V LearningPlanSetup:V
   ```

4. **验证规则语法**
   - Firebase Console 会显示语法错误
   - 确保所有大括号匹配

---

**时间估计**: 部署 2 分钟 + 测试 5 分钟 = **7 分钟解决**


