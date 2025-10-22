# 🧪 每日任务修复测试指南

## ✅ 已完成的5个修复

| 问题 | 修复内容 | 验证方法 |
|------|---------|---------|
| 1️⃣ 返回箭头缺失 | 添加 Toolbar + 返回按钮 | 进入配置页面，查看左上角是否有返回箭头 |
| 2️⃣ Switch 颜色不变 | 创建 ColorStateList | 切换 Dua Reminder，查看颜色是否变化（绿/灰） |
| 3️⃣ Settings 按钮无响应 | 添加日志+异常处理 | 点击 Streak Card 右上角设置图标 |
| 4️⃣ Task 1 Go 无响应 | 添加日志+异常处理 | 点击 Quran Reading 的 Go 按钮 |
| 5️⃣ Task 2 Go 无响应 | 添加日志+异常处理 | 点击 Tajweed Practice 的 Go 按钮 |

---

## 📱 测试步骤

### 🟢 测试 1: Create Card 和返回箭头

#### 步骤 1.1: 查看 Create Card
1. 打开应用（已启动）
2. 向下滚动到 "Daily Quests" 区域
3. **验证**：
   - ✅ 看到 "Daily Quests" 标题（20sp 字体）
   - ✅ 看到绿色 Create Card
   - ✅ 左侧图标大小适中（85dp）
   - ✅ 按钮文字完整显示

#### 步骤 1.2: 点击进入配置页面
1. 点击 "Create My Learning Plan Now" 按钮
2. **验证**：
   - ✅ 跳转到 Learning Plan Setup 页面
   - ✅ **左上角有返回箭头** ⭐（新功能）
   - ✅ 页面显示 "Learning Plan Setup" 标题

**如果失败**：查看日志是否有 "Card clicked - Navigating to Learning Plan Setup"

#### 步骤 1.3: 测试返回按钮
1. 点击左上角返回箭头
2. **验证**：
   - ✅ 返回主页
   - ✅ 仍然显示 Create Card

**如果失败**：查看日志是否有 "Toolbar back button clicked"

---

### 🔵 测试 2: Switch 颜色变化

#### 步骤 2.1: 进入配置页面
1. 点击 Create Card 进入配置页面
2. 找到 "Extra Dhikr" 部分

#### 步骤 2.2: 测试 Dua Reminder Switch
1. **初始状态**：Dua Reminder 开关应该是关闭（灰色）
2. **点击开关**：切换为打开
3. **验证**：
   - ✅ 开关轨道变为**绿色** ⭐（新功能）
   - ✅ 不再是灰色
4. **再次点击**：切换为关闭
5. **验证**：
   - ✅ 开关轨道变为**灰色** ⭐（新功能）

#### 步骤 2.3: 测试其他 Switch
1. 切换 "Practice Recitation" 开关
   - ✅ 打开时：绿色
   - ✅ 关闭时：灰色
2. 切换 "Tasbih Reminder" 开关
   - ✅ 打开时：绿色
   - ✅ 关闭时：灰色

**如果颜色不变**：说明 ColorStateList 没有正确应用

---

### 🟡 测试 3: 保存配置并查看任务列表

#### 步骤 3.1: 配置学习计划
1. Daily Reading Goal: **10 pages**
2. Practice Recitation: **打开**
3. Practice Duration: **15 minutes**
4. Tasbih Reminder: **打开**

#### 步骤 3.2: 保存
1. 点击 "✓ Save and Start My Challenge"
2. 如果弹出登录对话框：
   - 点击 "Login with Google"
   - 选择账户并授权
3. **验证**：
   - ✅ 按钮显示 "Saving..."
   - ✅ 1-3秒后显示 Toast："Learning plan saved successfully! ✅"
   - ✅ 自动返回主页

**如果失败**：
- 超时：查看 Firestore 连接
- 卡住：查看日志错误信息

#### 步骤 3.3: 查看主页任务列表
1. 返回主页后，向下滚动到 Daily Quests
2. **验证**：
   - ✅ **不再显示** Create Card
   - ✅ 显示 **Streak Card**（绿色，显示 "0 Days"）
   - ✅ 显示 **Today's Quests** 列表
   - ✅ 看到 3 个任务：
     - 📖 Quran Reading - Read 10 pages - [Go]
     - 🎧 Tajweed Practice - Practice 15 minutes - [Go]
     - 🤲 Dhikr - Complete 50 Tasbih - [Go]

---

### 🔴 测试 4: Streak Card 设置按钮

#### 步骤 4.1: 找到设置图标
1. 在主页 Streak Card 右上角
2. 找到设置图标（齿轮图标）

#### 步骤 4.2: 点击设置
1. 点击设置图标
2. **验证**：
   - ✅ 跳转到 Learning Plan Setup 页面（编辑模式）
   - ✅ 显示之前的配置（10页，15分钟等）

**如果无响应**：
- 查看日志：应该有 "Streak Settings icon clicked!"
- 查看日志：应该有 "ivStreakSettings found successfully"
- 如果显示 "ivStreakSettings is NULL!"，说明 View 没找到

---

### 🟣 测试 5: Task 1 Go 按钮（Quran Reading）

#### 步骤 5.1: 找到 Task 1
1. 在 Today's Quests 列表中
2. 找到 "Quran Reading" 卡片

#### 步骤 5.2: 点击 Go 按钮
1. 点击 "Go" 按钮
2. **验证**：
   - ✅ 打开古兰经阅读器（ActivityReader）
   - ✅ 可以阅读古兰经

**如果无响应**：
- 查看日志：应该有 "Task 1 Go button clicked!"
- 查看日志：应该有 "Launching Quran Reader for Task 1"
- 查看日志：应该有 "btnTask1Go found successfully"
- 如果显示 "btnTask1Go is NULL!"，说明 View 没找到

---

### 🟠 测试 6: Task 2 Go 按钮（Tajweed Practice）

#### 步骤 6.1: 找到 Task 2
1. 在 Today's Quests 列表中
2. 找到 "Tajweed Practice" 卡片

#### 步骤 6.2: 点击 Go 按钮
1. 点击 "Go" 按钮
2. **验证**：
   - ✅ 打开 Tajweed Timer Activity
   - ✅ 显示倒计时器（15 minutes）
   - ✅ 可以开始计时

**如果无响应**：
- 查看日志：应该有 "Task 2 Go button clicked!"
- 查看日志：应该有 "Launching Tajweed Timer for Task 2"
- 查看日志：应该有 "btnTask2Go found successfully"
- 如果显示 "btnTask2Go is NULL!"，说明 View 没找到

---

## 📊 测试结果记录表

请按照以下格式记录测试结果：

```
【测试 1: Create Card 和返回箭头】
1.1 Create Card 显示: ✅/❌
1.2 点击跳转: ✅/❌
1.3 返回箭头: ✅/❌ ⭐

【测试 2: Switch 颜色变化】
2.1 Dua Reminder 开关: ✅/❌ ⭐
    - 打开时颜色: 绿色/灰色/不变
    - 关闭时颜色: 灰色/绿色/不变
2.2 其他开关: ✅/❌

【测试 3: 保存配置】
3.1 配置: ✅/❌
3.2 保存: ✅/❌
    - 耗时: ___ 秒
    - 错误: ______
3.3 任务列表显示: ✅/❌
    - Streak Card: ✅/❌
    - Today's Quests: ✅/❌
    - 任务数量: ___

【测试 4: Settings 按钮】
4.1 找到图标: ✅/❌
4.2 点击响应: ✅/❌ ⭐

【测试 5: Task 1 Go】
5.1 找到按钮: ✅/❌
5.2 点击响应: ✅/❌ ⭐
    - 打开阅读器: ✅/❌

【测试 6: Task 2 Go】
6.1 找到按钮: ✅/❌
6.2 点击响应: ✅/❌ ⭐
    - 打开计时器: ✅/❌
```

---

## 🐛 常见问题排查

### 问题 A: 按钮点击无响应

**排查步骤**：
1. 查看日志是否有 "is NULL!"
2. 查看日志是否有 "found successfully"
3. 查看日志是否有 "clicked!"

**可能原因**：
- View 没有被正确初始化
- `setupQuestsCardClickListeners` 没有被调用
- Navigation 配置错误

### 问题 B: Switch 颜色不变

**排查步骤**：
1. 确认 `switch_track_color.xml` 文件存在
2. 确认布局文件使用 `@color/switch_track_color`

**可能原因**：
- ColorStateList 没有正确应用
- 布局文件没有更新

### 问题 C: 保存失败

**排查步骤**：
1. 查看日志是否有 "保存超时"
2. 查看日志是否有 Firestore 错误
3. 检查网络连接

**可能原因**：
- Firestore 数据库问题
- 网络不稳定
- 登录失败

---

## 📞 日志监控

在测试过程中，后台日志监控正在运行，会自动记录所有关键事件。

如果需要查看完整日志：
```bash
adb logcat -v time | grep -E "(DailyQuests|LearningPlan)"
```

如果需要查看错误日志：
```bash
adb logcat -v time | grep -E "(ERROR|FATAL|Exception)"
```

---

## ✅ 预期结果

所有测试应该通过：
1. ✅ 返回箭头正常工作
2. ✅ Switch 颜色正确变化（绿色↔灰色）
3. ✅ Settings 按钮可以点击
4. ✅ Task 1 Go 按钮可以点击
5. ✅ Task 2 Go 按钮可以点击
6. ✅ 所有导航正常
7. ✅ 保存功能正常
8. ✅ UI 显示正确

---

**请开始测试，并告诉我每个测试的结果！** 🚀

**特别注意标记 ⭐ 的测试项，这些是本次修复的重点功能。**

