# Daily Quests 测试指南

## ✅ 修复已完成并部署

**编译时间**: 2025-10-19 15:22  
**安装状态**: ✅ 已安装到 Pixel 7 设备  
**应用进程**: ✅ 正在运行 (PID: 20700)

---

## 🔧 已修复的问题

### 问题 1: Create Card 对所有用户显示 ✅
**修改内容**: `DailyQuestsManager.java` - `initialize()` 方法  
**效果**: 
- 未登录用户：显示 Create Card
- 已登录用户：根据是否有学习计划显示相应卡片

### 问题 2-4: 保存、登录、Go按钮 ✅
**状态**: 代码逻辑已确认正确，等待实际测试验证

---

## 📱 现在请在设备上测试

### 测试步骤 1: 验证 Create Card 显示

**请在 Pixel 7 设备上操作**:

1. **打开应用**
   - 应用图标：Quran Majeed
   - 或点击桌面上的应用

2. **查看主页（Home标签）**
   - 向下滚动，找到"Daily Quests"部分
   - ✅ **应该看到**: 黑色标题"Daily Quests"（在卡片外面）
   - ✅ **应该看到**: 绿色卡片，包含：
     - 左侧：白色方形背景的古兰经图标
     - 右侧：文字"Start your Quran journey! Set a goal, form a habit."
     - 右侧：白色按钮"Create My Learning Plan Now"

3. **如果看到了Create Card** ✅
   - 问题1已解决！
   - 继续测试步骤2

4. **如果没有看到Create Card** ❌
   - 截图发给我
   - 告诉我看到了什么（Streak Card? Today's Quests? 还是什么都没有？）

---

### 测试步骤 2: 测试点击跳转

**在设备上操作**:

1. **点击 "Create My Learning Plan Now" 按钮**

2. **应该跳转到配置页面**，包含：
   - 标题："Create Your Learning Plan"
   - Slider：Daily Reading Goal (1-50 pages)
   - Switch：Enable Tajweed Practice
   - Spinner：Practice Duration (15/30/45/60 minutes)
   - Switch：Enable Dua Reminder
   - Switch：Enable Tasbih Reminder
   - 底部显示：挑战天数（如"30 Days Challenge"）
   - 底部按钮："✓ Save and Start My Challenge"

3. **如果成功跳转** ✅
   - 继续测试步骤3

4. **如果没有跳转** ❌
   - 告诉我点击后的反应

---

### 测试步骤 3: 测试保存和登录流程

**在设备上操作**:

1. **配置学习计划**
   - 阅读页数：拖动 Slider 到 10 页
   - 朗诵练习：打开 Switch，选择 15 minutes

2. **点击 "Save and Start My Challenge" 按钮**

3. **如果您未登录**:
   - ✅ **应该弹出对话框**："Login Required"
   - 对话框文字："Please login with your Google account to save your learning plan..."
   - 两个按钮："Login with Google" 和 "Cancel"
   
4. **点击 "Login with Google"**
   - ✅ **应该打开 Google 登录界面**
   - 选择您的 Google 账户
   - 完成授权

5. **登录成功后**:
   - ✅ Toast 提示："Login successful! ✅"
   - ✅ 按钮显示："Saving..."
   - ✅ Toast 提示："Learning plan saved successfully! ✅"
   - ✅ 自动返回主页

6. **如果已经登录**:
   - 直接保存
   - Toast 提示："Learning plan saved successfully! ✅"
   - 自动返回主页

---

### 测试步骤 4: 验证任务列表显示

**保存成功并返回主页后**:

1. **查看主页的 Daily Quests 部分**

2. **应该看到两个卡片**:

   **卡片 1: Streak Card（绿色）**
   - 标题："Streak"（左侧）+ 设置图标（右侧）
   - 大字："0 Days"
   - 下方："Monthly Goal"
   - 右侧："0 / 31"
   - 进度条：空的（0%）

   **卡片 2: Today's Quests**
   - 标题："Today's Quests"（黑色）
   - 任务卡片 1（绿色）:
     * 图标：📖 古兰经
     * 标题："Quran Reading"
     * 描述："Read 10 pages"
     * 右侧：白色 [Go] 按钮
   - 任务卡片 2（绿色）:
     * 图标：🎧 耳机
     * 标题："Tajweed Practice"
     * 描述："Practice 15 minutes"
     * 右侧：白色 [Go] 按钮

3. **如果看到了这两个卡片** ✅
   - 继续测试步骤5

4. **如果没有看到** ❌
   - 告诉我看到了什么
   - 截图发给我

---

### 测试步骤 5: 测试 Go 按钮功能

**在设备上操作**:

1. **点击 Task 1 的 [Go] 按钮**（Quran Reading）
   - ✅ **应该打开古兰经阅读器页面**
   - 显示古兰经页面，可以阅读
   - 返回主页（按返回键）

2. **点击 Task 2 的 [Go] 按钮**（Tajweed Practice）
   - ✅ **应该打开 Tajweed Timer 页面**
   - 显示计时器："15:00"
   - 有"Start"、"Pause"、"Stop"按钮
   - 返回主页（按返回键）

3. **如果 Go 按钮工作正常** ✅
   - 所有功能都已修复成功！

4. **如果 Go 按钮没有反应** ❌
   - 告诉我点击后发生了什么

---

## 📊 测试结果反馈模板

请按以下格式告诉我测试结果：

```
测试步骤 1 (Create Card 显示):
- 结果：[✅ 成功 / ❌ 失败]
- 说明：[描述您看到的内容]

测试步骤 2 (点击跳转):
- 结果：[✅ 成功 / ❌ 失败]
- 说明：[...]

测试步骤 3 (保存和登录):
- 结果：[✅ 成功 / ❌ 失败]
- 说明：[...]

测试步骤 4 (任务列表显示):
- 结果：[✅ 成功 / ❌ 失败]
- 说明：[...]

测试步骤 5 (Go 按钮功能):
- Task 1: [✅ 成功 / ❌ 失败]
- Task 2: [✅ 成功 / ❌ 失败]
- 说明：[...]
```

---

## 🐛 如果遇到问题

### 如果应用崩溃
运行以下命令并发送日志给我：
```bash
adb logcat -d > crash_log.txt
```

### 如果某个功能不工作
1. 截图发给我
2. 描述您的操作步骤
3. 描述期望的结果 vs 实际结果

---

## 📸 建议的截图

为了更好地验证修复，建议截图以下内容：

1. **主页的 Daily Quests 部分**（显示 Create Card）
2. **学习计划配置页面**
3. **登录对话框**（如果弹出）
4. **保存成功后的主页**（显示 Streak Card + Today's Quests）
5. **点击 Go 按钮后的页面**（阅读器或计时器）

---

## ✅ 预期的完整流程

```
未登录用户 → 看到 Create Card 
            ↓
      点击按钮 → 进入配置页面
            ↓
      配置并保存 → 弹出登录对话框
            ↓
      Google登录 → 登录成功
            ↓
      自动保存 → 返回主页
            ↓
      显示任务列表 → Streak Card (0 Days)
            ↓              + Today's Quests (2个任务)
      点击 Go → 跳转到对应页面
            ↓
      完成任务 → 自动标记完成 (✓)
            ↓
      Streak更新 → 1 Days, 1/31
```

---

**请现在在设备上测试，并告诉我测试结果！** 🚀

如果所有步骤都成功，那么所有4个问题都已修复！

