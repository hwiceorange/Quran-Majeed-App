# 🔓 匿名登录改造 - 第一阶段

## 📊 改造进度

### ✅ 已完成
1. **扩展 GoogleAuthManager 支持匿名登录**
   - 添加 `signInAnonymously()` 方法
   - 添加 `linkAnonymousWithGoogle()` 方法（账户关联）
   - 添加 `isAnonymous()` 判断方法
   - 优化 `getUserDisplayName()` 和 `getUserEmail()` 支持匿名用户显示

2. **App启动时自动匿名登录**
   - 在 `App.onCreate()` 中添加自动匿名登录逻辑
   - 延迟1秒执行，不影响启动性能
   - 如果已登录则跳过

### 🔄 进行中
3. **移除学习计划和Qada的登录强制要求**
4. **添加连续打卡天数统计（Streak）**
5. **创建7天提示账户升级弹窗**
6. **更新UI显示游客模式状态**

---

## 🎯 核心功能说明

### 1. 匿名登录（Anonymous Sign-In）

**作用**：
- 用户无需Google账号即可使用所有功能
- 数据保存到Firestore（使用匿名UID）
- 降低使用门槛，提升转化率

**实现**：
```java
// 自动匿名登录
authManager.signInAnonymously(new AuthCallback() {
    @Override
    public void onSuccess(FirebaseUser user) {
        // user.isAnonymous() == true
        // user.getUid() - 唯一ID，用于Firestore
        Log.d(TAG, "Anonymous user ID: " + user.getUid());
    }
    
    @Override
    public void onFailure(String error) {
        Log.e(TAG, "Failed: " + error);
    }
});
```

**特点**:
- ✅ 自动创建Firebase用户（匿名）
- ✅ 获得唯一UID
- ✅ 可以读写Firestore（需要规则配置）
- ✅ 数据与UID绑定，不会丢失

---

### 2. 账户关联（Account Linking）

**作用**：
- 将匿名账户升级为Google账户
- 保留所有匿名期间的数据
- UID保持不变，数据自动同步

**实现**：
```java
// 用户点击"关联Google账号"
authManager.linkAnonymousWithGoogle(signInIntent, new AuthCallback() {
    @Override
    public void onSuccess(FirebaseUser user) {
        // user.isAnonymous() == false
        // user.getEmail() - Google邮箱
        // user.getUid() - 与匿名时相同！
        Log.d(TAG, "Linked! Email: " + user.getEmail());
        Log.d(TAG, "UID unchanged: " + user.getUid());
    }
    
    @Override
    public void onFailure(String error) {
        Log.e(TAG, "Linking failed: " + error);
    }
});
```

**关键点**：
- ✅ 使用 `user.linkWithCredential()` 而非 `signInWithCredential()`
- ✅ UID保持不变
- ✅ Firestore数据路径不变（`/users/{uid}/...`）
- ✅ 所有历史数据自动保留

---

### 3. UI 适配

**匿名用户显示**：
```java
String displayName = authManager.getUserDisplayName();
// 匿名用户: "Guest User"
// Google用户: "John Doe"

String email = authManager.getUserEmail();
// 匿名用户: "anonymous@guest.com"
// Google用户: "john@gmail.com"

boolean isAnonymous = authManager.isAnonymous();
// 用于判断是否显示"升级账户"按钮
```

---

## 📋 下一步任务

### Task 3: 移除学习计划和Qada的登录强制要求

**修改文件**：
- `LearningPlanSetupFragment.kt`
- `PrayersFragment.java`

**修改逻辑**：
```kotlin
// 原来
private fun onSaveButtonClicked() {
    val currentUser = auth.currentUser
    if (currentUser == null) {
        showLoginRequiredDialog() // ❌ 强制登录
    } else {
        saveConfiguration()
    }
}

// 改为
private fun onSaveButtonClicked() {
    val currentUser = auth.currentUser
    if (currentUser == null) {
        // 🔓 自动匿名登录后保存
        authManager.signInAnonymously { user ->
            saveConfiguration()
        }
    } else {
        saveConfiguration() // ✅ 匿名用户也可以保存
    }
}
```

---

### Task 4: 添加连续打卡天数统计（Streak）

**创建新文件**: `StreakManager.kt`

**功能**：
- 记录用户每日打卡（学习计划/祷告记录）
- 计算连续天数
- 保存到Firestore: `/users/{uid}/streakStats`

**数据结构**：
```kotlin
data class StreakStats(
    val currentStreak: Int = 0,        // 当前连续天数
    val longestStreak: Int = 0,        // 历史最长连续天数
    val lastCheckInDate: String = "",   // 最后打卡日期 (yyyy-MM-dd)
    val totalDays: Int = 0              // 总打卡天数
)
```

---

### Task 5: 创建7天提示账户升级弹窗

**创建新文件**: `AccountUpgradeDialog.kt`

**触发条件**：
- `isAnonymous == true`
- `currentStreak >= 7`

**UI设计**：
```
┌────────────────────────────────────────┐
│  🎉 恭喜坚持7天！                       │
│                                        │
│  为了永久保存您的学习进度和祷告记录，  │
│  建议关联Google账号。                   │
│                                        │
│  ✅ 数据不会丢失                       │
│  ✅ 跨设备同步                         │
│  ✅ 永久保存                           │
│                                        │
│  [ 立即关联 ]  [ 稍后提醒 ]           │
└────────────────────────────────────────┘
```

**实现**：
```kotlin
fun showUpgradePrompt() {
    AlertDialog.Builder(context)
        .setTitle("🎉 恭喜坚持7天!")
        .setMessage("为了永久保存您的数据，建议关联Google账号")
        .setPositiveButton("立即关联") { _, _ ->
            // 启动Google登录 + 账户关联
            startGoogleSignInForLinking()
        }
        .setNegativeButton("稍后提醒") { dialog, _ ->
            dialog.dismiss()
        }
        .show()
}
```

---

### Task 6: 更新UI显示游客模式状态

**修改位置**：
- 个人中心头像
- 顶部导航栏
- 学习计划页面

**UI变化**：
```
匿名用户：
  头像: 🧑 (游客图标)
  用户名: "Guest User"
  提示条: [点击关联Google账号以同步数据]

Google用户：
  头像: (Google头像)
  用户名: "John Doe"
  提示条: (无)
```

---

## 🔒 Firestore 规则更新

**需要更新 `firestore.rules`**:

```javascript
// 允许匿名用户读写自己的数据
match /users/{userId}/learningPlan/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
  // ✅ 匿名用户也有 request.auth.uid
}

match /users/{userId}/dailyProgress/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}

match /users/{userId}/streakStats/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}

// ... 其他规则保持不变
```

**关键点**：
- ✅ 匿名用户有 `request.auth.uid`
- ✅ 现有规则已经支持匿名用户
- ✅ 账户关联后UID不变，规则仍然有效

---

## ⚠️ 注意事项

### 1. 数据迁移

**问题**: 如果用户先匿名使用，后来又用另一个Google账号登录（而非关联），数据会丢失吗？

**答案**: 是的，所以必须强调使用"关联"功能，而非"重新登录"。

**解决方案**:
- UI明确标注"关联账号"而非"登录"
- 提供数据导出功能（备选方案）

### 2. 账户关联失败

**情况**: Google账号已被其他账户使用

**处理**:
```java
if (exception.getMessage().contains("already in use")) {
    showDialog("此Google账号已绑定其他账户，请使用其他账号");
}
```

### 3. 匿名账户过期

**Firebase规则**: 匿名账户不会自动过期，除非：
- 用户清除应用数据
- 用户卸载应用

**缓解措施**:
- 在用户坚持7天后强烈建议关联
- 提供"备份码"功能（高级特性）

---

## 📊 预期效果

### 用户体验
- ⬆️ **新用户转化率**: 预计提升 50%+（无需登录门槛）
- ⬆️ **7日留存率**: 匿名用户可以立即使用，更容易养成习惯
- ⬇️ **流失率**: 减少"需要登录"导致的流失

### 数据指标
- **匿名用户比例**: 预计 70-80%（初期）
- **账户关联率**: 预计 30-40%（坚持7天的用户）
- **数据完整性**: 100%（关联后数据不丢失）

---

## 🧪 测试计划

### 场景1: 新用户匿名使用
1. 安装应用
2. 自动匿名登录
3. 创建学习计划 ✅
4. 记录祷告 ✅
5. 打卡7天 ✅

### 场景2: 账户关联
1. 匿名用户打卡7天
2. 弹出升级提示
3. 点击"立即关联"
4. Google登录
5. 验证数据保留 ✅

### 场景3: 数据同步
1. 设备A: 匿名用户创建数据
2. 设备A: 关联Google账号
3. 设备B: Google登录
4. 验证数据同步 ✅

---

**当前版本: v1.9.23 (partial)**  
**下一版本: v1.9.24 (complete anonymous auth)**

**继续实现剩余任务...**

