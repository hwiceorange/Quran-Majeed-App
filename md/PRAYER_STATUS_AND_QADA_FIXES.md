# Prayer Status and Qada' UI Fixes

## 修复日期
2025-11-06

## 修复内容

### 1. ✅ Qada' 计数器箭头更换为 ">"

**问题：** 使用图标显示箭头，不够清晰。

**修复：**
- 移除了 `fragment_prayers.xml` 中的 `ImageView` 箭头图标
- 在字符串资源 `qada_count_zero` 中直接添加 " >" 符号
- 在代码中为非零计数也添加 " >" 符号

**修改文件：**
- `app/src/main/res/layout/fragment_prayers.xml`
- `app/src/main/res/values/strings.xml`

**结果：**
- "0 Prayers · Alhamdulillah >"
- "X Prayers >" (当 X > 0)

---

### 2. ✅ 移除 Qada' 卡片边框

**问题：** Qada' 卡片有明显的边框。

**修复：**
- 将 `cardElevation` 从 `6dp` 降低到 `2dp`
- 添加 `app:strokeWidth="0dp"` 明确移除边框

**修改文件：**
- `app/src/main/res/layout/fragment_prayers.xml`

**结果：**
卡片显示更加简洁，无边框。

---

### 3. ✅ 修复新用户 Qada' 引导弹窗变形

**问题：** 弹窗布局严重变形，按钮和选项无法正常显示。

**原因：**
- 布局文件根元素使用了 `MaterialCardView`，与 Dialog 的 `setContentView` 冲突
- 没有设置 Dialog 的宽度，导致自适应失败

**修复：**
1. **重写布局文件** (`dialog_qada_onboarding.xml`)：
   - 将根元素从 `MaterialCardView` 改为 `LinearLayout`
   - 使用 `@drawable/bg_bottom_sheet` 作为背景（圆角白色背景）
   - 调整内边距和字体大小，使其更紧凑
   - 减小 Icon 从 64dp 到适当大小
   - 优化选项卡的内边距

2. **更新 Dialog 显示逻辑** (`QadaOnboardingDialog.kt`)：
   - 添加 Dialog 宽度设置：`width = (displayMetrics.widthPixels * 0.9).toInt()`
   - 确保背景透明
   - 正确设置 WRAP_CONTENT 高度

**修改文件：**
- `app/src/main/res/layout/dialog_qada_onboarding.xml`
- `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/QadaOnboardingDialog.kt`

**结果：**
- 弹窗现在正确显示，占屏幕宽度的 90%
- 所有 UI 元素对齐正确
- 圆角边框清晰可见
- 用户可以正常交互

---

### 4. ⏳ 祷告状态自动判断 (部分实现)

**问题：** Dhuhr 11:35 AM，当前时间 PM 10:43，状态应该显示为 Missed ❌，但还是显示 TRACK 按钮。

**问题分析：**
当前逻辑只检查数据库中是否有记录：
- 有记录 → 显示对应状态图标（Ada', Qada', Missed）
- 无记录 → 显示 TRACK 按钮（Pending）

**缺陷：**
- 没有检查祷告时间是否已经过去
- 如果用户未记录且时间已过，应该自动显示为 Missed

**尝试的修复：**
在 `updatePrayerStatusUI` 方法中添加了 `isPrayerTimePassed()` 时间判断逻辑。

**当前状态：**
- 由于缺少 `PrayerTimesManager` 实例和相关 API，暂时实现为总是返回 `false`
- 这意味着所有未记录的祷告仍会显示 TRACK 按钮
- 添加了 TODO 注释，标记为待完善功能

**修改文件：**
- `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/home/PrayersFragment.java`
  - 添加了 `isPrayerTimePassed()` 方法（临时实现）
  - 添加了 `getNextPrayer()` 辅助方法
  - 添加了 `import java.util.Calendar`

**下一步建议：**
1. 找到或实现获取祷告时间的 API
2. 实现完整的 `isPrayerTimePassed()` 逻辑：
   - 获取当前祷告的结束时间（下一个祷告的开始时间）
   - 比较当前时间
   - 如果已过期且无记录，返回 `true` → 显示 Missed ❌
   - 否则返回 `false` → 显示 TRACK 按钮

**预期行为：**
```
当前时间: 22:43 (PM 10:43)
Fajr    04:07 AM  → 已过期 → 无记录 → ❌ Missed
Dhuhr   11:35 AM  → 已过期 → 无记录 → ❌ Missed
Asr     14:56 PM  → 已过期 → 无记录 → ❌ Missed
Maghrib 17:43 PM  → 已过期 → 无记录 → ❌ Missed
Isha    18:55 PM  → 已过期 → 无记录 → ❌ Missed
```

---

## 测试建议

### 1. Qada' 卡片显示
- [x] ">" 符号正确显示
- [x] 无边框
- [x] 点击卡片正常工作

### 2. Qada' 引导弹窗
- [x] 新用户点击 Qada' 卡片时正确弹出
- [x] 弹窗布局正确，所有元素可见
- [x] "Start from Today" 选项可点击
- [x] "Start from: [Date Picker]" 选项可点击并打开日期选择器
- [x] "Confirm and Start Tracking" 按钮可点击
- [x] 确认后正确保存配置

### 3. 祷告状态自动判断
- [ ] 未完成 - 需要实现完整的时间判断逻辑
- [ ] 测试：过期未记录的祷告应显示 Missed ❌
- [ ] 测试：未到时间的祷告应显示 TRACK 按钮
- [ ] 测试：已记录的祷告应显示对应状态图标

---

## 技术债务

1. **祷告状态时间判断**：
   - 当前 `isPrayerTimePassed()` 是占位实现
   - 需要集成真实的祷告时间管理系统
   - 需要考虑时区和本地化

2. **Qada' Tracker 主页面**：
   - `QadaTrackerActivity.java` 和 `activity_qada_tracker.xml` 已删除
   - 点击 Qada' 卡片目前显示 "coming soon" 消息
   - 需要重新实现完整的 Qada' 历史和分析页面

---

## 版本信息
- 应用版本：1.7.3
- 编译日期：2025-11-06
- 编译状态：✅ 成功
- 安装状态：✅ 已安装到物理设备





