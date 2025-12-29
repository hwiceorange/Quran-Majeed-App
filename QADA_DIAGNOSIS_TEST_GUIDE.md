# 📊 Qada 统计诊断日志 - 完整测试指南

## ✅ 已完成的修复

### 1. 匿名登录问题 ✅
- ✅ 移除了强制 Google 登录对话框
- ✅ 点击祷告记录时自动匿名登录
- ✅ 点击 Qada 统计时自动匿名登录

### 2. 诊断日志添加 ✅
- ✅ QadaTrackerActivity - 用户信息日志
- ✅ QadaTrackerActivity - 日期范围日志
- ✅ QadaTrackerActivity - Firestore 返回数据日志
- ✅ QadaTrackerActivity - weeklyData 内容日志
- ✅ QadaTrackerActivity - 完成率计算日志
- ✅ PrayerLogRepository - 查询参数日志
- ✅ PrayerLogRepository - 文档详情日志
- ✅ PrayerLogRepository - 处理摘要日志

---

## 🔍 日志收集方法

### 方法 1: 实时查看日志（推荐）

```bash
# 清除旧日志
adb logcat -c

# 实时查看 Qada 诊断日志
adb logcat | grep -E "QADA_DIAGNOSIS|PrayerLogRepository"
```

### 方法 2: 保存到文件

```bash
# 清除旧日志
adb logcat -c

# 开始记录（在后台运行）
adb logcat > qada_diagnosis_$(date +%Y%m%d_%H%M%S).log &

# 记录 PID，方便后续停止
echo $! > logcat_pid.txt

# 在应用中操作...

# 停止记录
kill $(cat logcat_pid.txt)

# 提取相关日志
grep -E "QADA_DIAGNOSIS|PrayerLogRepository" qada_diagnosis_*.log > qada_filtered.log
```

### 方法 3: 一键收集脚本

创建文件 `collect_qada_logs.sh`:

```bash
#!/bin/bash

# Qada 诊断日志收集脚本
echo "🔍 Starting Qada Diagnosis Log Collection..."
echo "================================================"

# 清除旧日志
adb logcat -c
echo "✅ Old logs cleared"

# 创建日志文件名（带时间戳）
LOG_FILE="qada_diagnosis_$(date +%Y%m%d_%H%M%S).log"
FILTERED_FILE="qada_filtered_$(date +%Y%m%d_%H%M%S).log"

echo "📝 Log file: $LOG_FILE"
echo ""
echo "⚠️  Please perform the following actions in the app:"
echo "   1. Go to Salat page"
echo "   2. Click on a prayer button (e.g., Fajr)"
echo "   3. Record the prayer as Ada'"
echo "   4. Go back to Salat page"
echo "   5. Click on 'Outstanding Qada' card"
echo "   6. View Weekly and Monthly statistics"
echo ""
echo "Press Ctrl+C when done..."
echo ""

# 开始收集日志
adb logcat > "$LOG_FILE" &
LOGCAT_PID=$!

# 等待用户操作
wait $LOGCAT_PID

echo ""
echo "✅ Log collection stopped"

# 提取相关日志
echo "📊 Filtering relevant logs..."
grep -E "QADA_DIAGNOSIS|PrayerLogRepository|GoogleAuthManager|DIAGNOSE" "$LOG_FILE" > "$FILTERED_FILE"

echo "✅ Filtered logs saved to: $FILTERED_FILE"
echo ""
echo "📋 Log Summary:"
echo "   Total lines: $(wc -l < "$LOG_FILE")"
echo "   Filtered lines: $(wc -l < "$FILTERED_FILE")"
echo ""
echo "🎯 Key sections to check:"
grep -c "loadWeeklyData() - START" "$FILTERED_FILE" && echo "   - loadWeeklyData calls: $(grep -c 'loadWeeklyData() - START' "$FILTERED_FILE")" || true
grep -c "Firestore Query Result" "$FILTERED_FILE" && echo "   - Firestore queries: $(grep -c 'Firestore Query Result' "$FILTERED_FILE")" || true
grep -c "Completion Calculation" "$FILTERED_FILE" && echo "   - Completion calculations: $(grep -c 'Completion Calculation' "$FILTERED_FILE")" || true
echo ""
echo "================================================"
echo "✅ Done! Review $FILTERED_FILE for details."
```

使用方法:
```bash
chmod +x collect_qada_logs.sh
./collect_qada_logs.sh
```

---

## 📋 测试步骤

### Step 1: 准备测试环境

```bash
# 清除应用数据（确保全新状态）
adb shell pm clear com.quran.quranaudio.online

# 或重新安装
adb uninstall com.quran.quranaudio.online
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Step 2: 启动日志收集

```bash
# 方法 A: 使用脚本
./collect_qada_logs.sh

# 或方法 B: 手动启动
adb logcat -c
adb logcat | grep -E "QADA_DIAGNOSIS|PrayerLogRepository" | tee qada_live.log
```

### Step 3: 在应用中执行操作

#### 操作 1: 记录祷告
1. 打开应用，进入 **Salat** 页面
2. 点击 **Fajr** 祷告按钮
3. 在弹出的对话框中，确认状态为 "Ada'" (已完成)
4. 点击 **Save** 保存

**预期日志**:
```
PrayersFragment: 🔘 Prayer clicked: Fajr
PrayersFragment: ⚠️ User not logged in, attempting automatic anonymous sign-in...
GoogleAuthManager: 🔓 Attempting anonymous sign-in...
GoogleAuthManager: ✅ Anonymous sign-in successful
GoogleAuthManager:    → User ID: abc123xyz
PrayersFragment: ✅ Anonymous sign-in successful: abc123xyz
```

#### 操作 2: 打开 Qada Tracker
1. 在 Salat 页面，向下滚动
2. 点击 **"Outstanding Qada'"** 卡片
3. 如果是首次使用，设置 Qada 开始日期为 **2024-12-28**
4. 进入 Qada Tracker 页面

**预期日志**:
```
QADA_DIAGNOSIS: ════════════════════════════════════════════════════════
QADA_DIAGNOSIS: 📊 loadWeeklyData() - START
QADA_DIAGNOSIS:    🔐 User Info:
QADA_DIAGNOSIS:       User ID: abc123xyz
QADA_DIAGNOSIS:       Is Anonymous: true
QADA_DIAGNOSIS:    📅 Date Range:
QADA_DIAGNOSIS:       Current Date: 2024-12-30 (MONDAY)
QADA_DIAGNOSIS:       Week Start: 2024-12-30 (MONDAY)
QADA_DIAGNOSIS:       Week End: 2025-01-05 (SUNDAY)
QADA_DIAGNOSIS: ════════════════════════════════════════════════════════
```

#### 操作 3: 查看周统计
1. 确认在 **Weekly** Tab
2. 观察圆形进度条显示的百分比

**预期日志**:
```
PrayerLogRepository: 🔍 getPrayerLogsByDateRangeWithIds()
PrayerLogRepository:    🔐 Query Parameters:
PrayerLogRepository:       User ID: abc123xyz
PrayerLogRepository:       Start Date: 2024-12-30
PrayerLogRepository:       End Date: 2025-01-05
PrayerLogRepository:    ✅ Firestore query completed
PrayerLogRepository:    📦 Found 1 documents
PrayerLogRepository:    📄 Document Details:
PrayerLogRepository:    [0] Document ID: xxx
PrayerLogRepository:         userId: abc123xyz
PrayerLogRepository:         date: 2024-12-30
PrayerLogRepository:         prayerName: Fajr
PrayerLogRepository:         status: ADA
```

```
QADA_DIAGNOSIS: 📦 Firestore Query Result:
QADA_DIAGNOSIS:    Returned Dates: 1
QADA_DIAGNOSIS:    📆 2024-12-30 (1 prayers):
QADA_DIAGNOSIS:       ✅ Fajr -> ADA (docId: xxx)
```

```
QADA_DIAGNOSIS: 📊 updateWeeklyCompletion() - START
QADA_DIAGNOSIS:    weeklyData size: 1 dates
QADA_DIAGNOSIS:    📋 weeklyData content:
QADA_DIAGNOSIS:    📆 2024-12-30:
QADA_DIAGNOSIS:       Fajr -> ADA
```

```
QADA_DIAGNOSIS: 📈 Completion Calculation:
QADA_DIAGNOSIS:    Qada Start Date: 2024-12-28
QADA_DIAGNOSIS:    Week Range: 2024-12-30 to 2025-01-05
QADA_DIAGNOSIS:    Today: 2024-12-30
QADA_DIAGNOSIS:    Total Prayers (denominator): 5
QADA_DIAGNOSIS:    Completed Prayers (numerator): 1
QADA_DIAGNOSIS:    Completion Rate: 20%
QADA_DIAGNOSIS:    Formula: (1 / 5) * 100 = 20%
```

### Step 4: 查看月统计
1. 点击顶部的 **Monthly** Tab
2. 观察月度圆形进度条

**预期**: 应该看到类似的日志，但日期范围是整个月

---

## 🔍 问题诊断指南

### 问题 1: 显示 0% 但有祷告记录

**日志特征**:
```
QADA_DIAGNOSIS: 📦 Firestore Query Result:
QADA_DIAGNOSIS:    Returned Dates: 0  ⚠️

或

QADA_DIAGNOSIS: weeklyData size: 0 dates  ⚠️

或

QADA_DIAGNOSIS: Completion Rate: 0%
QADA_DIAGNOSIS: ⚠️ WARNING: 0% completion but totalPrayers > 0
```

**可能原因 A: User ID 不一致**

检查日志中的 userId:
```
# 记录祷告时的 userId
GoogleAuthManager: → User ID: abc123xyz

# 查询时的 userId
PrayerLogRepository: User ID: xyz789abc  ⚠️ 不同！
```

**解决方案**: 匿名账户未持久化，需要修复持久化逻辑

---

**可能原因 B: 日期格式不匹配**

检查 Firestore 文档中的日期:
```
PrayerLogRepository: date: 12/30/2024  ⚠️ 格式错误

# 期望格式
PrayerLogRepository: date: 2024-12-30  ✅
```

**解决方案**: 确保保存时使用 ISO 8601 格式 (`yyyy-MM-dd`)

---

**可能原因 C: 祷告名称不匹配**

检查 Firestore 中的 prayerName:
```
PrayerLogRepository: prayerName: Fajar  ⚠️ 拼写错误

# 或本地化名称
PrayerLogRepository: prayerName: 晨礼  ⚠️ 中文名称

# 期望
PrayerLogRepository: prayerName: Fajr  ✅
```

**解决方案**: 统一使用英文标准名称: `Fajr`, `Dhuhr`, `Asr`, `Maghrib`, `Isha`

---

### 问题 2: Firestore 返回 0 个文档

**日志特征**:
```
PrayerLogRepository: ⚠️ NO DOCUMENTS FOUND!
PrayerLogRepository: Possible reasons:
PrayerLogRepository: 1. No prayer logs exist for userId=xxx in date range [...]
```

**检查步骤**:

1. **确认 userId 一致**:
   ```bash
   # 在日志中搜索所有 userId
   grep "User ID:" qada_filtered.log
   ```
   应该看到相同的 userId

2. **确认数据已保存**:
   - 打开 Firebase Console
   - 进入 Firestore Database
   - 查看 `prayer_logs` collection
   - 确认有对应 userId 的文档

3. **确认日期范围**:
   ```
   # 查询范围
   PrayerLogRepository: Start Date: 2024-12-30
   PrayerLogRepository: End Date: 2025-01-05
   
   # Firestore 中的文档日期应该在这个范围内
   ```

4. **检查 Firestore 规则**:
   - Firebase Console → Firestore → Rules
   - 确保允许匿名用户读取自己的数据

---

### 问题 3: 圆形进度条计算不正确

**日志特征**:
```
QADA_DIAGNOSIS: weeklyData size: 1 dates
QADA_DIAGNOSIS: 📆 2024-12-30:
QADA_DIAGNOSIS:    Fajr -> ADA

# 但是
QADA_DIAGNOSIS: Total Prayers: 0  ⚠️
# 或
QADA_DIAGNOSIS: Completed Prayers: 0  ⚠️
```

**可能原因**:

1. **祷告窗口未开始** (`shouldIncludePrayerInDenominator` 返回 false)
2. **Qada 开始日期晚于记录日期**
3. **记录日期在未来**

**检查**:
```
QADA_DIAGNOSIS: Qada Start Date: 2024-12-28
QADA_DIAGNOSIS: Week Range: 2024-12-30 to 2025-01-05
QADA_DIAGNOSIS: Today: 2024-12-30

# 确保 Qada Start Date <= 记录日期 <= Today
```

---

## 📊 日志分析模板

### 完整的成功日志应该包含：

```
# 1. 用户认证
✅ GoogleAuthManager: Anonymous sign-in successful
✅ PrayersFragment: User ID: abc123xyz

# 2. 数据加载
✅ QADA_DIAGNOSIS: loadWeeklyData() - START
✅ QADA_DIAGNOSIS: User ID: abc123xyz (一致)
✅ QADA_DIAGNOSIS: Week Range: ...

# 3. Firestore 查询
✅ PrayerLogRepository: User ID: abc123xyz (一致)
✅ PrayerLogRepository: Found N documents (N > 0)
✅ PrayerLogRepository: date: 2024-12-30 (ISO 格式)
✅ PrayerLogRepository: prayerName: Fajr (英文)
✅ PrayerLogRepository: status: ADA

# 4. 数据处理
✅ QADA_DIAGNOSIS: Returned Dates: N (N > 0)
✅ QADA_DIAGNOSIS: weeklyData size: N dates

# 5. 完成率计算
✅ QADA_DIAGNOSIS: Total Prayers: X (X > 0)
✅ QADA_DIAGNOSIS: Completed Prayers: Y (Y > 0)
✅ QADA_DIAGNOSIS: Completion Rate: Z% (Z > 0)
```

---

## 🎯 测试检查清单

### 匿名登录测试
- [ ] 点击祷告按钮不弹 Google 登录对话框
- [ ] 自动匿名登录成功
- [ ] 日志显示 `Anonymous sign-in successful`
- [ ] User ID 生成成功

### 祷告记录测试
- [ ] 祷告记录保存成功
- [ ] Firestore 中有对应文档
- [ ] 文档 `userId` 与登录的一致
- [ ] 文档 `date` 格式为 `yyyy-MM-dd`
- [ ] 文档 `prayerName` 为英文 (Fajr, Dhuhr, etc.)

### Qada Tracker 加载测试
- [ ] 日志显示 `loadWeeklyData() - START`
- [ ] User ID 一致
- [ ] 日期范围正确
- [ ] Firestore 返回文档数 > 0
- [ ] `weeklyData` 不为空

### 完成率计算测试
- [ ] `totalPrayers` > 0
- [ ] `completedPrayers` > 0
- [ ] `completionRate` > 0%
- [ ] 圆形进度条显示正确百分比

---

## 🆘 故障排查决策树

```
圆形进度条显示 0%
    |
    ├─ weeklyData 为空？
    |   ├─ YES → Firestore 查询问题
    |   |   ├─ 返回 0 个文档？
    |   |   |   ├─ YES → userId 不一致？
    |   |   |   |   ├─ YES → 修复匿名账户持久化
    |   |   |   |   └─ NO → 检查日期范围/格式
    |   |   |   └─ NO → 检查数据处理逻辑
    |   |   └─ 查询失败？
    |   |       └─ 检查 Firestore 规则/网络
    |   |
    |   └─ NO → 计算逻辑问题
    |       ├─ totalPrayers = 0？
    |       |   └─ 检查 shouldIncludePrayerInDenominator
    |       |
    |       └─ completedPrayers = 0？
    |           └─ 检查祷告名称是否匹配
```

---

## 📝 测试报告模板

```
### Qada 统计诊断测试报告

**测试日期**: 2024-12-30
**应用版本**: v1.9.26 (108)
**测试设备**: [填写]
**Android 版本**: [填写]

### 测试结果

#### 1. 匿名登录
- User ID: abc123xyz
- Is Anonymous: true
- 状态: ✅ 成功

#### 2. 祷告记录
- 记录祷告: Fajr
- 状态: Ada'
- 日期: 2024-12-30
- Firestore 文档 ID: xxx
- 状态: ✅ 成功

#### 3. Qada Tracker 加载
- Week Range: 2024-12-30 to 2025-01-05
- Firestore 返回文档数: 1
- weeklyData 大小: 1
- 状态: ✅ 成功

#### 4. 完成率计算
- Total Prayers: 5
- Completed Prayers: 1
- Completion Rate: 20%
- 圆形进度条显示: 20%
- 状态: ✅ 成功

### 发现的问题
[列出任何问题]

### 日志摘要
[粘贴关键日志片段]
```

---

## ✅ 成功标准

**测试通过的条件**:
1. ✅ 匿名登录自动完成，不弹 Google 登录对话框
2. ✅ 祷告记录成功保存到 Firestore
3. ✅ Qada Tracker 能加载祷告数据
4. ✅ weeklyData 不为空
5. ✅ 圆形进度条显示 > 0%（如果有记录）
6. ✅ User ID 在所有日志中保持一致
7. ✅ 应用重启后 User ID 不变

---

**文档版本**: v1.0  
**创建时间**: 2024-12-30  
**状态**: ✅ 诊断日志已添加，等待测试验证

