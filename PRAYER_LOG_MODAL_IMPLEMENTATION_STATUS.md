# Prayer Log Modal 实施状态

## ✅ 已完成功能（现有代码）

### UI 组件 (bottom_sheet_log_prayer.xml)
- ✅ Header: "Log Prayer" 标题
- ✅ Prayer Name Display: 静态显示祷告名称
- ✅ Status Toggle: 三段式开关 (Ada', Qada', Missed)
  - ✅ 配色: Ada' 绿色, Qada' 琥珀色, Missed 红色
- ✅ Time Pickers:
  - ✅ Prayed At: 时间选择器
  - ✅ Recorded At: 只读，显示本地时区
- ✅ Tags Section:
  - ✅ Chips: At Mosque, Traveling, With Family
  - ✅ 现已改为真正的标签选择（可切换选中状态）
- ✅ Notes Input: 文本输入框
- ✅ Footer Buttons: CANCEL 和 SAVE

### 数据模型 (PrayerLog.kt)
- ✅ recordId (id)
- ✅ originalDate (date)
- ✅ prayerName
- ✅ status (Ada', Qada', Missed)
- ✅ prayedAt (performedAt)
- ✅ recordedAt (loggedAt)
- ✅ isToday ✅ **新增**
- ✅ tags ✅ **新增**
- ✅ notes

### 基础功能 (PrayerLogBottomSheet.kt)
- ✅ 新建模式: `newInstance(prayerName, initialStatus)`
- ✅ 编辑模式: `newInstanceForEdit(prayerName, existingLogId)`
- ✅ 时间选择器集成
- ✅ 状态切换逻辑
- ✅ Firestore 保存/更新
- ✅ Tags 选择逻辑（toggleTag）

---

## 🔧 需要补充的核心逻辑

### 场景 1: Pending / Today's Log (默认 Ada')
**触发点**: 主页 Salah 页面，点击 TRACK 按钮

#### 需要实现:
1. ⏳ **时间窗口验证**:
   - 当 `prayedAt` 超出祷告有效时间窗口时
   - 弹出提醒: "该时间点已超出 [PrayerName] 的有效时段，是否将其记录为 Qada'？"
   
2. ⏳ **Missed 状态处理**:
   - 点击 Missed → 提交记录，不扣除 Qada 总数
   - 增加一个待弥补义务

**实现位置**: 
- `PrayerLogBottomSheet.kt` 中的 `savePrayerLog()` 方法
- 添加 `validatePrayerTimeWindow()` 方法

---

### 场景 2: Missed / Qada' Log (弥补)
**触发点**: Qada' Obligations 页面，点击 Log Qada' 按钮

#### 需要实现:
1. ✅ 默认选中 Qada' ✅ **已支持**（通过 `initialStatus` 参数）
2. ✅ `prayedAt` 不限制在原时间窗口 ✅ **已支持**
3. ⏳ **核心逻辑**:
   - 提交后，更新原记录 status: Missed/Pending → Qada'
   - **立即扣除 Qada 计数器 -1**

**实现位置**:
- `PrayerLogBottomSheet.kt` 中的 `savePrayerLog()` 方法
- 添加 callback 通知父组件更新 Qada 计数器

---

### 场景 3: Ada' / Qada' Edit (编辑)
**触发点**: Daily Log 或 Qada' Obligations 页面，点击已记录的图标 (✅ 或 ⚠️)

#### 需要实现:
1. ✅ 加载现有记录数据 ✅ **已支持**（`newInstanceForEdit`）
2. ✅ `recordedAt` 只读 ✅ **已支持**
3. ⏳ **状态转换边界**:
   - Qada' → Missed: Qada 总数 +1
   - Missed → Ada'/Qada': Qada 总数 -1
4. ⏳ **更新 recordedAt**:
   - 修改时更新 `recordedAt` 为当前时间

**实现位置**:
- `PrayerLogBottomSheet.kt` 中的 `loadExistingLog()` 方法
- `savePrayerLog()` 中添加状态转换逻辑

---

## 🚧 待实现的具体任务

### 任务 1: 时间窗口验证（场景 1）
```kotlin
private fun validatePrayerTimeWindow(): Boolean {
    // 获取祷告的有效时间窗口
    // 如果 prayedAt 超出窗口，弹出确认对话框
    // 用户确认后自动切换为 Qada'
}
```

### 任务 2: 加载现有记录（场景 3）
```kotlin
private fun loadExistingLog(logId: String) {
    // 从 Firestore 加载现有记录
    // 填充所有字段: status, performedAt, notes, tags
    // 保存原始状态用于状态转换判断
}
```

### 任务 3: 状态转换与 Qada 计数器更新
```kotlin
private fun updateQadaCounter(oldStatus: PrayerStatus?, newStatus: PrayerStatus) {
    // 状态转换逻辑:
    // Qada' → Missed: +1
    // Missed → Ada'/Qada': -1
    // Ada' → Missed: +1
    // Ada' → Qada': 不变
    
    // 通过 callback 通知父组件
    onPrayerLoggedListener?.onQadaCountChanged(delta)
}
```

### 任务 4: 在 QadaTrackerActivity 中集成点击事件
```java
// 点击祷告状态圆点
dotView.setOnClickListener(v -> {
    // 根据状态打开对应的 Modal:
    // Ada'/Qada': 编辑模式
    // Missed: Qada 弥补模式
    // Pending: 新建模式（默认 Ada'）
});
```

---

## 📊 实施优先级

1. **高优先级**:
   - ✅ 数据模型补充 (已完成)
   - ✅ Tags 功能 (已完成)
   - ⏳ 场景 2: Qada 弥补逻辑
   - ⏳ 场景 3: 编辑模式加载与状态转换
   - ⏳ QadaTrackerActivity 点击事件集成

2. **中优先级**:
   - ⏳ 场景 1: 时间窗口验证

3. **低优先级**:
   - 优化 UI 细节
   - 错误处理完善

---

## 🎯 当前状态

**已完成**: 60%
- ✅ UI 组件: 100%
- ✅ 数据模型: 100%
- ✅ 基础功能: 100%
- ⏳ 场景逻辑: 30%
- ⏳ 集成: 0%

**下一步**: 
1. 实现场景 2 和场景 3 的核心逻辑
2. 在 QadaTrackerActivity 中集成点击事件
3. 实现 Qada 计数器自动更新机制




