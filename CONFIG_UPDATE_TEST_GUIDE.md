# 配置更新问题诊断指南

## 问题描述
从 Streak Card 右上角的 ⚙️ 设置中修改任务配置后，返回主页时配置没有被更新和显示。

## 新增的诊断日志

已在以下位置添加详细日志以追踪数据流：

### 1. Quest Config 接收日志（DailyQuestsManager）
当从 Firestore 接收到配置更新时：
```
=== Quest Config Received from Firestore ===
Reading Pages: XX
Recitation Minutes: XX min
Tasbih Enabled: true/false
Tasbih Count: XX
Challenge Days: XX
==========================================
```

### 2. 任务描述更新日志
当更新任务卡片时：
```
Updating today's quests with config: XX pages, XX min recitation, tasbih: true/false
Task 1 description updated: Read XX pages
Task 2 description updated: Practice XX minutes
Task 3 (Tasbih) visible/hidden - count: XX
```

### 3. 数据保存日志（已存在）
```
QuestRepository: Quest config saved successfully
LearningPlanSetupVM: Quest config saved successfully
LearningPlanSetupVM: 准备发送 Success 状态
```

## 测试步骤

### 步骤 1：记录初始配置
1. 打开应用，进入主页
2. 查看 **Today's Quests** 中的任务描述
   - **Quran Reading**: "Read XX pages"
   - **Tajweed Practice**: "Practice XX minutes"
3. 记录当前配置值

**预期日志：**
```
=== Quest Config Received from Firestore ===
Reading Pages: 10
Recitation Minutes: 15
...
Task 1 description updated: Read 10 pages
Task 2 description updated: Practice 15 minutes
```

### 步骤 2：修改配置
1. 点击 **Streak Card** 右上角的 **设置图标** ⚙️
2. 进入配置页面
3. 修改配置（例如：将阅读页数从 10 改为 20）
4. 点击 **"Start Challenge"** 保存

**预期日志：**
```
QuestRepository: Quest config saved successfully
LearningPlanSetupVM: Quest config saved successfully
LearningPlanSetupVM: 准备发送 Success 状态
LearningPlanSetupVM: Success 状态已发送
LearningPlanSetupFrag: 收到 Success 状态，准备显示 Toast 并返回
LearningPlanSetupFrag: 已成功返回主页
```

### 步骤 3：验证配置更新
返回主页后，**立即**检查：

#### A. 查看 UI 显示
- **Quran Reading** 任务描述是否更新为 "Read 20 pages"？
- **Tajweed Practice** 任务描述是否更新？

#### B. 检查日志
应该看到**新的配置接收日志**：
```
=== Quest Config Received from Firestore ===
Reading Pages: 20  ← 应该是新值
Recitation Minutes: XX
...
Task 1 description updated: Read 20 pages  ← 应该是新值
```

## 诊断场景

### 场景 A：日志显示新配置，但 UI 未更新
**症状：**
- 日志显示 `Reading Pages: 20`
- 但 UI 仍显示 "Read 10 pages"

**可能原因：**
- UI 更新时机问题
- TextView 引用丢失
- Fragment 生命周期问题

### 场景 B：日志显示旧配置
**症状：**
- 返回主页后，日志显示 `Reading Pages: 10`（旧值）
- UI 也显示旧值

**可能原因：**
1. **数据未保存成功**（检查保存日志）
2. **Flow 未触发更新**（Firestore 监听器问题）
3. **ViewModel Flow 生命周期问题**（SharingStarted.Lazily）

### 场景 C：没有看到新的配置接收日志
**症状：**
- 返回主页后，完全没有 "=== Quest Config Received ===" 日志

**可能原因：**
1. **Fragment 未重新订阅**（生命周期问题）
2. **ViewModel 被销毁**（状态丢失）
3. **Flow 停止监听**

## 收集信息

如果配置没有更新，请提供以下信息：

### 1. 完整日志序列
从点击设置图标开始，到返回主页后 5 秒内的所有日志：
```bash
adb logcat -v time | grep -E "(DailyQuestsManager|QuestRepository|LearningPlanSetup)" > log.txt
```

### 2. 关键日志点
- [ ] 是否看到 "Quest config saved successfully"？
- [ ] 是否看到 "已成功返回主页"？
- [ ] 返回主页后是否看到 "=== Quest Config Received ===" 日志？
- [ ] 如果看到，新配置值是否正确？
- [ ] 是否看到 "Task X description updated" 日志？

### 3. UI 实际显示
- 主页上 Quran Reading 任务的描述文本
- 主页上 Tajweed Practice 任务的描述文本

## 可能的修复方向

### 如果是 Flow 未触发更新
需要检查 `HomeQuestsViewModel` 的 Flow 生命周期管理：
- `observeUserQuestConfig()` 是否正确设置了 snapshot listener
- `callbackFlow` 是否在 Fragment pause/resume 时正确处理

### 如果是数据保存失败
需要检查 Firestore 权限和网络状态：
- Firestore security rules
- 网络连接（VPN可能导致问题）
- 用户认证状态

### 如果是 ViewModel 生命周期问题
可能需要在 Fragment 的 `onResume()` 中主动刷新：
```java
@Override
public void onResume() {
    super.onResume();
    if (dailyQuestsManager != null) {
        dailyQuestsManager.refreshData(); // 需要添加此方法
    }
}
```

## 下一步
1. 在设备上按照上述步骤测试
2. 记录所有日志输出
3. 报告具体是哪个场景（A、B 或 C）
4. 提供关键日志点的检查结果

---

**注意：** Flow 的实时更新机制应该是自动的，如果它没有触发，说明存在数据保存、网络、或生命周期管理的问题，而不应该通过添加手动刷新来掩盖问题。

