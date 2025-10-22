# 🧪 Firestore 创建后测试指南

## ✅ 已完成
- Firestore Database 已创建
- 模式：测试模式（30天有效期）
- 位置：雅加达（asia-southeast2）

---

## 📱 测试步骤

### 步骤 1: 打开应用并查看 Create Card

**在 Pixel 7 设备上**：
1. 打开应用（Quran Majeed）
2. 向下滚动到 "Daily Quests" 部分
3. **确认**：看到绿色的 Create Card

**预期结果**：
- ✅ 看到 "Daily Quests" 黑色标题
- ✅ 看到绿色卡片
- ✅ 看到按钮 "Create My Learning Plan Now"

---

### 步骤 2: 配置学习计划

1. **点击** "Create My Learning Plan Now" 按钮
2. **进入配置页面后**：
   - Daily Reading Goal: 拖动 Slider 到 **10 pages**
   - Enable Tajweed Practice: **打开** Switch
   - Practice Duration: 选择 **15 minutes**
   - Enable Tasbih Reminder: **打开**（可选）

3. **确认配置**：
   - 挑战天数显示：约 **33 Days Challenge**

---

### 步骤 3: 保存并登录

1. **点击** "✓ Save and Start My Challenge" 按钮

2. **如果弹出登录对话框**：
   - 点击 "Login with Google"
   - 选择 Google 账户（adochub@gmail.com）
   - 完成授权

3. **观察按钮状态**：
   - 应显示 "Saving..."

---

### 步骤 4: 验证保存结果

#### ✅ 成功的迹象：

**1-3秒内**：
- ✅ Toast 提示："Learning plan saved successfully! ✅"
- ✅ 自动返回主页
- ✅ 主页显示两个新卡片：

   **Streak Card**（绿色）：
   ```
   Streak
   0 Days
   
   Monthly Goal    0 / 31
   ▓▓▓▓▓▓▓▓▓▓▓░░░░ (0%)
   ```

   **Today's Quests**：
   ```
   Today's Quests
   
   📖 Quran Reading
      Read 10 pages          [Go]
   
   🎧 Tajweed Practice
      Practice 15 minutes    [Go]
   
   🤲 Dhikr (如果启用)
      Complete 50 Dhikr      [Go]
   ```

#### ❌ 如果失败：

**15秒后**：
- ❌ Toast 提示："Error: 保存超时，请检查网络连接"
- ❌ 按钮恢复为："✓ Save and Start My Challenge"
- ❌ 留在配置页面

**如果失败，请告诉我！**

---

## 🔍 日志监控（可选）

如果想查看详细日志，在电脑上运行：

```bash
# 实时监控保存过程
adb logcat | grep -E "(LearningPlan|QuestRepository|Firestore)"
```

**成功的日志**应该包含：
```
D LearningPlanSetupVM: 开始保存配置: UserQuestConfig(...)
D QuestRepository: Quest config saved successfully
D QuestRepository: Streak stats initialized
D LearningPlanSetupVM: 准备发送 Success 状态
D LearningPlanSetupVM: Success 状态已发送
D LearningPlanSetupFrag: 收到 Success 状态，准备显示 Toast 并返回
D LearningPlanSetupFrag: 已成功返回主页
```

**不应该再出现**：
```
W Firestore: The database (default) does not exist  ← 这个错误应该消失了
```

---

## 🎯 关键测试点

| 测试点 | 预期时间 | 状态 |
|--------|---------|------|
| 显示 Create Card | 立即 | ⏳ 待测 |
| 跳转配置页面 | 立即 | ⏳ 待测 |
| 弹出登录对话框（未登录时） | 立即 | ⏳ 待测 |
| Google 登录完成 | 5-10秒 | ⏳ 待测 |
| 保存配置到 Firestore | **1-3秒** | ⏳ 关键 |
| 返回主页 | 0.5秒 | ⏳ 待测 |
| 显示任务列表 | 立即 | ⏳ 待测 |

---

## 🔧 如果仍然失败

### 情况 A: 仍然超时

**可能原因**：
1. 设备网络不稳定
2. Firestore 规则配置问题
3. Firebase 初始化问题

**解决方案**：
```bash
# 1. 检查网络
adb shell ping -c 3 8.8.8.8

# 2. 检查 Firestore 错误
adb logcat | grep Firestore
```

### 情况 B: 保存成功但不返回主页

**可能原因**：导航逻辑问题

**解决方案**：查看日志
```bash
adb logcat | grep "LearningPlanSetupFrag"
```

应该看到：
```
D LearningPlanSetupFrag: 收到 Success 状态，准备显示 Toast 并返回
D LearningPlanSetupFrag: 已成功返回主页
```

### 情况 C: 其他错误

**请提供**：
1. 错误提示的完整文字
2. 日志输出
3. 当前在哪个页面

---

## ✅ 测试完成后

### 如果成功 ✅

**下一步测试 Go 按钮**：

1. **点击 Task 1 "Go" 按钮**
   - 应该打开古兰经阅读器

2. **点击 Task 2 "Go" 按钮**
   - 应该打开 Tajweed Timer 页面

3. **点击 Task 3 "Go" 按钮**（如果有）
   - 应该跳转到 Tasbih 页面

### 如果失败 ❌

**请告诉我**：
1. 在哪一步失败的
2. 看到什么错误提示
3. 是否超时（15秒）还是立即报错

---

## 📊 验证数据保存（可选）

### 在 Firebase Console 验证

1. **访问** Firebase Console
   ```
   https://console.firebase.google.com/
   ```

2. **选择项目** `quran-majeed-aa3d2`

3. **点击** "Firestore Database"

4. **点击** "数据" 标签

5. **应该看到**：
   ```
   users/
     └── {您的userId}/
         ├── learningPlan/
         │   └── config/
         │       ├── dailyReadingPages: 10
         │       ├── recitationEnabled: true
         │       ├── recitationMinutes: 15
         │       └── ...
         └── streakStats/
             └── summary/
                 ├── currentStreak: 0
                 ├── longestStreak: 0
                 └── ...
   ```

---

## 🎉 预期的完整流程

```
1. 打开应用
   ↓
2. 看到 Create Card ✅
   ↓
3. 点击按钮 → 进入配置页面 ✅
   ↓
4. 配置学习计划（10页，15分钟）✅
   ↓
5. 点击 Save → 弹出登录（未登录时）✅
   ↓
6. Google 登录成功 ✅
   ↓
7. 按钮显示 "Saving..." (1-3秒) ✅
   ↓
8. Toast: "Learning plan saved successfully! ✅" ✅
   ↓
9. 自动返回主页 ✅
   ↓
10. 显示 Streak Card (0 Days) ✅
    + Today's Quests (2-3个任务) ✅
```

---

## 📞 反馈模板

请按以下格式告诉我测试结果：

```
步骤 1 (Create Card 显示): ✅ 成功 / ❌ 失败
步骤 2 (配置页面): ✅ 成功 / ❌ 失败
步骤 3 (登录): ✅ 成功 / ❌ 失败
步骤 4 (保存): 
  - 耗时: ___ 秒
  - 结果: ✅ 成功 / ❌ 失败
  - 错误信息（如果有）: ___
  
主页显示: 
  - Streak Card: ✅ 显示 / ❌ 未显示
  - Today's Quests: ✅ 显示 / ❌ 未显示
```

---

**现在请在设备上测试保存功能，并告诉我结果！** 🚀

如果一切正常，这将是每日任务功能的重大突破！

