# ⚠️ 每日任务功能需要登录

## 📊 当前状态

✅ **每日任务功能已正确集成并编译成功**  
❌ **用户未登录，功能被隐藏（符合预期）**

根据日志输出：
```
DailyQuestsManager: User not logged in - Daily Quests feature disabled
```

这是**正常的预期行为**，符合设计文档要求：
- 未登录用户 → Daily Quests UI 完全隐藏
- 已登录用户 → 根据是否有学习计划显示相应卡片

---

## 🔐 如何启用每日任务功能

### 方式一：在设备上手动登录（推荐）⭐

1. **打开应用**
   ```
   设备上点击 Quran Majeed 应用图标
   ```

2. **进入登录**
   - 方法1：点击右上角**头像图标**
   - 方法2：点击**菜单** → 选择**登录选项**
   - 方法3：某些功能会提示需要登录

3. **选择 Google 登录**
   - 点击 "Sign in with Google" 按钮
   - 选择你的 Google 账户
   - 完成授权流程

4. **验证功能显示**
   - 登录成功后返回主页
   - 主页应显示以下之一：
     * **"Create Your Daily Plan"** 绿色卡片（首次使用）
     * **Streak Card + Today's Quests Card**（已创建计划）

---

### 方式二：使用 ADB 命令触发登录界面

如果你无法在设备上找到登录入口，可以使用以下命令：

```bash
# 方法1：直接打开登录Activity（如果有独立登录页面）
adb shell am start -n com.quran.quranaudio.online/.LoginActivity

# 方法2：打开设置页面（通常包含登录选项）
adb shell am start -n com.quran.quranaudio.online/.SettingsActivity

# 方法3：打开主Activity并导航到个人中心
adb shell am start -n com.quran.quranaudio.online/.prayertimes.ui.MainActivity
```

---

## 📱 登录后的预期行为

### 场景 A：首次使用（未创建学习计划）

登录后，主页会显示**绿色的创建计划卡片**：

```
╔════════════════════════════════════════════════╗
║  ✨ Create Your Daily Plan                   ║
║                                                ║
║  📝 Set daily reading goals                   ║
║     and track your progress                    ║
║                                                ║
║  [Get Started] 按钮                           ║
╚════════════════════════════════════════════════╝
```

**操作步骤：**
1. 点击 **"Get Started"** 按钮
2. 进入学习计划设置页面
3. 配置以下参数：
   - Daily Reading Goal: 1-50 页（滑块）
   - Recitation Practice: 15/30/45/60 分钟（下拉选择）
   - Tasbih Reminder: 开启/关闭（开关）
4. 点击 **"Save and Start My Challenge"**
5. 数据保存到 Firebase Firestore
6. 自动返回主页，显示 Streak Card 和 Today's Quests

---

### 场景 B：已创建学习计划

登录后，主页会显示**两个卡片**：

#### 1️⃣ Streak Card（连续天数卡片）
```
╔════════════════════════════════════════════════╗
║  📊 Streak Card                      ⚙️        ║
║                                                ║
║  🔥 13 Days                                    ║
║                                                ║
║  Monthly Goal: 13 / 31                         ║
║  ████████░░░░░░░ (42%)                         ║
╚════════════════════════════════════════════════╝
```

#### 2️⃣ Today's Quests Card（今日任务卡片）
```
╔════════════════════════════════════════════════╗
║  ✅ Today's Quests                            ║
║                                                ║
║  📖 Quran Reading                             ║
║     Read 10 pages                  [Go] 按钮  ║
║                                                ║
║  🎤 Tajweed Practice                          ║
║     Practice 15 minutes            [Go] 按钮  ║
║                                                ║
║  📿 Dhikr                                      ║
║     Complete 50 Tasbih             [Go] 按钮  ║
╚════════════════════════════════════════════════╝
```

---

## 🧪 验证功能是否正常工作

### 方法 1：使用测试脚本（已创建）

```bash
cd /Users/huwei/AndroidStudioProjects/quran0
./test_daily_quests.sh
```

该脚本会：
- ✅ 检查设备连接
- ✅ 启动应用
- ✅ 检查用户登录状态
- ✅ 验证每日任务功能初始化
- ✅ 显示预期的UI状态

---

### 方法 2：手动查看日志

登录后，执行以下命令查看日志：

```bash
# 清空日志
adb logcat -c

# 重启应用
adb shell am force-stop com.quran.quranaudio.online
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity

# 等待5秒后查看日志
sleep 5
adb logcat -d | grep -E "(DailyQuestsManager|HomeQuestsViewModel|QuestRepository)"
```

**预期日志输出：**

```
✅ 未登录（当前状态）：
DailyQuestsManager: User not logged in - Daily Quests feature disabled

✅ 已登录且无计划：
DailyQuestsManager: Daily Quests initialized successfully
DailyQuestsManager: No learning plan found - showing create card

✅ 已登录且有计划：
DailyQuestsManager: Daily Quests initialized successfully
DailyQuestsManager: Learning plan found - showing quests cards
HomeQuestsViewModel: Streak stats loaded: currentStreak=13
HomeQuestsViewModel: Today's progress loaded: 2/3 tasks completed
```

---

## 🐛 如果登录后仍然不显示

### 检查清单：

1. **Firebase Authentication 配置**
   - Firebase Console → Authentication → Sign-in method
   - 确认 Google 登录已启用
   - 确认 Support Email 已配置

2. **SHA-1 指纹配置**（参考 `签名问题快速修复.md`）
   ```
   Firebase Console → Project Settings → Your apps → Android
   → SHA certificate fingerprints
   
   需要添加：
   - Debug SHA-1: 8A:E5:E2:C3:9E:28:4C:7C:32:77:ED:2E:89:57:BF:08:AB:4F:9E:45
   - Play Store SHA-1: (从 Google Play Console 获取)
   ```

3. **google-services.json 配置**
   ```bash
   # 验证文件是否最新
   cat app/google-services.json | grep -A 10 "oauth_client"
   
   # 应该看到 client_id，而不是空数组
   ```

4. **Firestore 规则配置**
   ```javascript
   // Firebase Console → Firestore Database → Rules
   
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{userId}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
     }
   }
   ```

5. **查看详细错误日志**
   ```bash
   adb logcat -d | grep -E "(ERROR|Exception|FATAL)" | grep -i "quran\|firebase\|auth"
   ```

---

## 📚 相关文档

- ✅ **功能实现总结**：`DAILY_QUESTS_IMPLEMENTATION_SUMMARY.md`
- ✅ **详细测试指南**：`DAILY_QUESTS_TEST_GUIDE.md`
- ✅ **Google 登录修复**：`签名问题快速修复.md`
- ✅ **测试脚本**：`test_daily_quests.sh`

---

## ✅ 下一步操作

1. **登录 Google 账户**（在设备上）
2. **运行测试脚本验证**：`./test_daily_quests.sh`
3. **创建学习计划**（如果是首次使用）
4. **测试任务完成功能**（参考 `DAILY_QUESTS_TEST_GUIDE.md`）

---

## 💡 为什么要求登录？

每日任务功能依赖 **Firebase Firestore** 存储用户数据：
- ✅ 学习计划配置（dailyReadingPages, recitationMinutes, tasbihCount）
- ✅ 每日进度追踪（task1/2/3完成状态）
- ✅ Streak 统计（currentStreak, longestStreak, monthlyProgress）

没有登录就没有 `userId`，无法将数据保存到 Firestore，因此功能被禁用。

---

**创建时间：** 2025-10-18  
**状态：** ✅ 功能正常，等待用户登录  
**下一步：** 请在设备上完成 Google 登录

