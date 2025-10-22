# Daily Quests 功能测试指南

## 📋 测试前准备

### 1. Firebase 配置检查

确保以下Firebase服务已启用：

- ✅ **Firestore Database**：规则配置正确
- ✅ **Firebase Authentication**：Google登录已配置
- ✅ **google-services.json**：最新版本已放置在 `app/` 目录

**Firestore 规则示例：**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // 用户学习计划数据
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 2. 安装应用

```bash
# 连接设备/模拟器后执行
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🧪 测试用例

### Test Case 1: 创建学习计划（未登录用户）

**步骤：**
1. 打开应用
2. 在主页找到 "Daily Quests Card"
3. 点击卡片
4. 应跳转到 "Learning Plan Setup" 页面
5. 设置以下参数：
   - Daily Reading Goal: 10 pages
   - Recitation Practice: 15 minutes
   - Tasbih Reminder: 开启
6. 点击 "✓ Save and Start My Challenge"
7. **预期结果**：弹出Google登录对话框

**验证点：**
- ✅ UI正确显示学习计划设置选项
- ✅ 滑块和Spinner正常工作
- ✅ Challenge Days自动计算（应显示约30天）
- ✅ 未登录时提示登录

---

### Test Case 2: 创建学习计划（已登录用户）

**步骤：**
1. 完成Google登录
2. 返回学习计划设置页面
3. 设置参数后点击保存
4. **预期结果**：
   - Toast提示 "Learning plan saved successfully! ✅"
   - 自动返回主页
   - "Daily Quests Card" 消失
   - 显示 "Streak Card" 和 "Today's Quests Card"

**验证点：**
- ✅ 数据成功保存到Firestore
- ✅ UI切换正确
- ✅ Streak Card显示 "0 Days", "Monthly Goal: 0 / 31"
- ✅ Today's Quests显示3个任务：
  - Task 1: "Quran Reading (Read 10 pages)" - 显示 "Go" 按钮
  - Task 2: "Tajweed Practice (Practice 15 minutes)" - 显示 "Go" 按钮
  - Task 3: "Dhikr (Complete 50 Tasbih)" - 显示 "Go" 按钮

**Firestore 数据验证：**
```
users/{userId}/learningPlan/config
{
  "dailyReadingPages": 10,
  "recitationEnabled": true,
  "recitationMinutes": 15,
  "tasbihReminderEnabled": true,
  "tasbihCount": 50,
  "totalChallengeDays": 30,
  "startDate": <Timestamp>,
  "updatedAt": <Timestamp>
}
```

---

### Test Case 3: 完成 Task 1 - Quran Reading

**步骤：**
1. 在 "Today's Quests Card" 点击 Task 1的 "Go" 按钮
2. **预期结果**：打开 Quran Reader Activity
3. 阅读古兰经至少 4 分钟（2页 × 2分钟/页 = 4分钟）
4. 切换到其他应用或返回主页（触发 `onPause`）

**验证点：**
- ✅ `QuranReadingTracker` 记录了阅读时长
- ✅ 如果时长 ≥ 20分钟（10页），Task 1自动标记为完成
- ✅ 主页 Task 1 显示绿色勾号 ✓

**日志检查：**
```
QuranReadingTracker: Recorded 2 pages read today. Total: 2 pages
```

---

### Test Case 4: 完成 Task 2 - Tajweed Practice

**步骤：**
1. 在 "Today's Quests Card" 点击 Task 2的 "Go" 按钮
2. **预期结果**：打开 Tajweed Timer Activity
3. 点击 "Start" 开始计时
4. 等待15分钟（或修改目标时间为1分钟进行快速测试）
5. 计时器达到目标后自动标记完成

**验证点：**
- ✅ 计时器正确显示倒计时
- ✅ 进度条正常更新
- ✅ 达到目标时显示 Toast："🎉 Tajweed Practice completed!"
- ✅ 主页 Task 2 显示绿色勾号 ✓

**日志检查：**
```
TajweedTimerActivity: Timer completed! Marking task as complete
QuestRepository: Task task_2_tajweed marked as complete
```

---

### Test Case 5: 完成 Task 3 - Tasbih (Dhikr)

**步骤：**
1. 在 "Today's Quests Card" 点击 Task 3的 "Go" 按钮
2. **预期结果**：跳转到 Tasbih Fragment
3. 点击计数器 50 次
4. **预期结果**：
   - Toast提示："🎉 Daily Tasbih Quest completed! (50/50)"
   - 主页 Task 3 显示绿色勾号 ✓

**验证点：**
- ✅ `TasbihManager` 正确统计每日计数
- ✅ 达到50次后触发完成逻辑
- ✅ 主页UI更新

**日志检查：**
```
TasbihFragment: Daily quest completed: 50/50
QuestRepository: Task task_3_tasbih marked as complete
```

---

### Test Case 6: 所有任务完成 → Streak更新

**前提条件：** Task 1, 2, 3 全部完成

**预期结果：**
1. **Firestore 自动更新：**
   ```
   users/{userId}/dailyProgress/2025-10-18
   {
     "date": "2025-10-18",
     "task1ReadCompleted": true,
     "task2TajweedCompleted": true,
     "task3TasbihCompleted": true,
     "allTasksCompleted": true,
     "completedAt": <Timestamp>
   }
   
   users/{userId}/streakStats/summary
   {
     "currentStreak": 1,
     "longestStreak": 1,
     "totalDays": 1,
     "lastCompletedDate": "2025-10-18",
     "monthlyGoal": 31,
     "monthlyProgress": 1,
     "lastUpdatedAt": <Timestamp>
   }
   ```

2. **主页UI更新：**
   - Streak Card 显示 "1 Day"
   - 进度条："Monthly Goal: 1 / 31"

**验证点：**
- ✅ Firestore事务原子性更新成功
- ✅ `currentStreak` 增加 1
- ✅ `monthlyProgress` 增加 1
- ✅ UI实时刷新

---

### Test Case 7: 跨天重置逻辑

**步骤：**
1. 修改设备时间到第二天（或等待真实第二天）
2. 打开应用
3. **预期结果**：
   - Today's Quests 所有任务重置为未完成状态
   - 如果昨天没完成所有任务，`currentStreak` 重置为 0
   - 如果昨天完成了所有任务，`currentStreak` 保持（不增加，等今天完成）

**验证点：**
- ✅ `QuestRepository.checkAndResetStreak()` 正确执行
- ✅ 昨日数据保留在 Firestore
- ✅ 新的一天创建新的 `dailyProgress` 文档

---

### Test Case 8: 修改学习计划

**步骤：**
1. 在 Streak Card 点击右上角设置图标 ⚙️
2. **预期结果**：跳转回 Learning Plan Setup 页面
3. 修改参数（例如：将阅读目标从10页改为20页）
4. 保存
5. 返回主页

**验证点：**
- ✅ Firestore `config` 文档更新
- ✅ Today's Quests 显示新的目标值
- ✅ 已完成的任务状态不受影响

---

## 🐛 常见问题排查

### 问题1: 登录失败
**检查：**
- Firebase Console → Authentication → Sign-in method → Google已启用
- `google-services.json` 中的 `client_id` 配置正确
- SHA-1指纹已添加到Firebase项目

### 问题2: Firestore写入失败
**检查：**
- Firestore规则是否允许写入
- 网络连接正常
- 查看Logcat中的错误日志

### 问题3: 任务不自动标记完成
**检查：**
- 用户已登录
- Firestore中存在 `config` 文档
- 查看Logcat中的 `QuestRepository` 日志

### 问题4: Streak不更新
**检查：**
- 所有启用的任务都已完成
- Firestore事务没有冲突
- `checkAndResetStreak()` 在应用启动时执行

---

## 📊 测试完成标准

✅ **所有测试用例通过**
✅ **Firestore数据结构正确**
✅ **UI显示符合预期**
✅ **无崩溃或ANR**
✅ **日志无严重错误**

---

## 🎯 下一步优化建议

1. **添加单元测试** - 覆盖 `QuestRepository` 的核心逻辑
2. **优化计时器** - 在后台运行时保持计时
3. **添加通知** - 提醒用户完成每日任务
4. **数据分析** - 显示历史趋势图表
5. **离线支持** - 任务数据本地缓存

---

**测试日期：** _________________  
**测试人员：** _________________  
**测试结果：** ✅ 通过 / ❌ 失败  
**备注：** ___________________________________



