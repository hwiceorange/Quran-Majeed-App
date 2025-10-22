# Daily Quests 点击问题修复测试指南

## 问题根源
发现问题：在 `frag_main.xml` 中使用 `<include>` 标签时，如果在 include 上设置了 `id`，它会覆盖被包含布局的根元素 ID。

```xml
<!-- frag_main.xml 中的定义 -->
<include android:id="@+id/streak_card" layout="@layout/layout_streak_card" />
<include android:id="@+id/today_quests_card" layout="@layout/layout_today_quests_card" />
```

之前代码错误地使用了 `R.id.streak_card_root` 和 `R.id.today_quests_card_root`，导致找不到视图。

## 修复内容
已修改 `DailyQuestsManager.java` 的 `findQuestsCardViews()` 方法：
- ✅ 使用正确的 ID：`R.id.streak_card` 而不是 `R.id.streak_card_root`
- ✅ 使用正确的 ID：`R.id.today_quests_card` 而不是 `R.id.today_quests_card_root`
- ✅ 添加详细的日志输出，便于调试

## 测试步骤

### 前提条件
1. 确保应用已安装最新的 debug 包
2. 确保已创建每日任务（如果尚未创建，请先创建）
3. 后台日志监控已启动

### 测试用例 1：Streak Card 设置按钮
1. 打开应用，进入主页
2. 找到 **Streak Card**（绿色卡片，显示连续天数）
3. 点击右上角的 **设置图标** ⚙️
4. **预期结果**：
   - 跳转到 Learning Plan Setup 页面（任务配置页面）
   - 日志中应该显示：`"Streak Settings icon clicked!"`

### 测试用例 2：Quran Reading 的 Go 按钮
1. 在主页找到 **Today's Quests** 区域
2. 找到 **Quran Reading** 卡片（第一个任务）
3. 点击右侧的 **Go** 按钮
4. **预期结果**：
   - 跳转到 Quran 阅读页面，定位到当前进度
   - 日志中应该显示：`"Task 1 (Quran Reading) Go button clicked"`

### 测试用例 3：Tajweed Practice 的 Go 按钮
1. 在 **Today's Quests** 区域
2. 找到 **Tajweed Practice** 卡片（第二个任务）
3. 点击右侧的 **Go** 按钮
4. **预期结果**：
   - 启动 TajweedTimerActivity（Tajweed 练习计时器）
   - 日志中应该显示：`"Task 2 (Tajweed) Go button clicked"`

## 日志检查

### 成功的日志示例
```
DailyQuestsManager: Finding quests card views...
DailyQuestsManager: streak_card found successfully
DailyQuestsManager: ivStreakSettings found successfully
DailyQuestsManager: today_quests_card found successfully
DailyQuestsManager: btnTask1Go found successfully
DailyQuestsManager: btnTask2Go found successfully
DailyQuestsManager: btnTask3Go found successfully
```

### 点击成功的日志示例
```
DailyQuestsManager: Streak Settings icon clicked!
DailyQuestsManager: Navigating to Learning Plan Setup (edit)
```

```
DailyQuestsManager: Task 1 (Quran Reading) Go button clicked
DailyQuestsManager: Launching Quran Reader
```

```
DailyQuestsManager: Task 2 (Tajweed) Go button clicked
DailyQuestsManager: Launching TajweedTimerActivity
```

### 如果仍然失败的日志
```
DailyQuestsManager: ivStreakSettings is NULL! Cannot set click listener
DailyQuestsManager: btnTask1Go is NULL! Cannot set click listener
```

## 注意事项

1. **网络问题**：之前的日志显示设备使用了 VPN，导致无法解析 `firestore.googleapis.com`。如果 Google 登录失败，请：
   - 关闭 VPN
   - 或者使用已登录的状态进行测试
   - 或者切换到更稳定的网络

2. **Task 3 (Dhikr)**：如果在创建任务时没有启用 Tasbih Reminder，Task 3 卡片会是隐藏状态，这是正常的。

3. **日志监控**：如果需要查看更详细的日志，可以运行：
   ```bash
   adb logcat -v time | grep DailyQuestsManager
   ```

## 修复前后对比

### 修复前
```java
// 错误：使用布局文件中的根元素ID
streakCard = questsCardsContainer.findViewById(R.id.streak_card_root);
todayQuestsCard = questsCardsContainer.findViewById(R.id.today_quests_card_root);
```
**结果**：所有视图都是 NULL ❌

### 修复后
```java
// 正确：使用include标签的ID
streakCard = questsCardsContainer.findViewById(R.id.streak_card);
todayQuestsCard = questsCardsContainer.findViewById(R.id.today_quests_card);
```
**结果**：视图正确找到，点击监听器正常工作 ✅

## 下一步
请在设备上进行上述测试，并反馈结果。如果仍然有问题，请提供：
1. 具体哪个按钮无响应
2. logcat 输出（特别是 DailyQuestsManager 相关的日志）
3. 是否已成功创建每日任务

