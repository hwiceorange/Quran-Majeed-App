# 🔍 Qada 祷告统计问题诊断报告

## 📋 问题描述

**用户反馈**:
- 匿名登录后，Qada 统计显示不准确
- 初始设定：12月28日开始统计
- 实际情况：12月29日（周一）完成了4次祷告
- 显示问题：周统计的圆形图表显示 **0%**
- 疑问：月统计是否也存在同样的问题

**截图显示**:
- Weekly Tab: Dec 29 - Jan 04
- 圆形进度条: **0% Completed**
- 趋势: "This Week ↓ -11%"
- Prayer Breakdown 显示：
  - Monday (12/30 或 12/23?): Fajr ✅, Dhuhr ✅
  - Tuesday (12/31 或 12/24?): Fajr ✅
  - 其他天: 无记录（灰色方块）

---

## 🔍 问题分析

### 1. 数据加载流程

#### QadaTrackerActivity 的数据加载链路

```
QadaTrackerActivity.loadWeeklyData()
    ↓
检查 FirebaseAuth.getInstance().getCurrentUser()
    ↓
prayerLogRepository.getPrayerLogsByDateRangeWithIdsAsync(weekStart, weekEnd, callback)
    ↓
PrayerLogRepository.getPrayerLogsByDateRangeWithIds(startDate, endDate)
    ↓
获取 auth.currentUser?.uid
    ↓
Firestore 查询: 
    collection("prayer_logs")
    .whereEqualTo("userId", userId)
    .whereGreaterThanOrEqualTo("date", startDate)
    .whereLessThanOrEqualTo("date", endDate)
    ↓
返回数据到 QadaTrackerActivity
    ↓
buildWeeklyPrayerGrid()  // 构建祷告方格显示
updateWeeklyCompletion()  // 更新圆形进度条
```

**关键代码位置**:

**QadaTrackerActivity.java** (行 1216-1260):
```java
private void loadWeeklyData() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
        Log.e(TAG, "User not authenticated");
        buildWeeklyPrayerGrid(); // Show empty grid
        return;
    }
    
    LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate weekEnd = currentDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    
    Log.d(TAG, "Loading weekly data from " + weekStart + " to " + weekEnd);
    
    prayerLogRepository.getPrayerLogsByDateRangeWithIdsAsync(...);
}
```

**PrayerLogRepository.kt** (行 672-737):
```kotlin
suspend fun getPrayerLogsByDateRangeWithIds(
    startDate: String,
    endDate: String
): Map<String, Map<String, PrayerLogInfo>> {
    val userId = auth.currentUser?.uid
    if (userId == null) {
        Log.e(TAG, "User not authenticated")
        return emptyMap()
    }
    
    val snapshot = firestore.collection("prayer_logs")
        .whereEqualTo("userId", userId)
        .whereGreaterThanOrEqualTo("date", startDate)
        .whereLessThanOrEqualTo("date", endDate)
        .orderBy("date", Query.Direction.ASCENDING)
        .get()
        .await()
    
    Log.d(TAG, "Found ${snapshot.documents.size} prayer logs in date range")
    
    // ...处理数据...
}
```

---

### 2. 圆形图表计算逻辑

#### updateWeeklyCompletion() 的计算规则

**QadaTrackerActivity.java** (行 1461-1569):

```java
private void updateWeeklyCompletion() {
    // 1. 获取本周的日期范围 (Monday-Sunday)
    LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate weekEnd = currentDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    
    // 2. 获取 Qada 开始日期
    LocalDate qadaStart = parseQadaStartDate();
    LocalDate today = LocalDate.now();
    
    // 3. 遍历本周每一天
    LocalDate date = weekStart;
    int totalPrayers = 0;
    int completedPrayers = 0;
    
    while (!date.isAfter(weekEnd)) {
        String dateStr = date.toString();
        
        // ✅ 只计入有效日期范围内的祷告：
        // 1. 日期 >= Qada 开始日期
        // 2. 日期 <= 今天
        boolean isValidDate = true;
        if (qadaStart != null && date.isBefore(qadaStart)) {
            isValidDate = false; // Qada 开始日期之前，不计入
        }
        if (date.isAfter(today)) {
            isValidDate = false; // 未来日期，不计入
        }
        
        if (isValidDate) {
            for (String prayerName : prayerNames) {
                // ✅ 关键：只计入"祷告窗口已开始"的祷告
                if (!shouldIncludePrayerInDenominator(date, prayerName)) {
                    continue;
                }
                
                totalPrayers++; // 分母 +1
                
                // ✅ 检查是否完成 (Ada' 或 Qada')
                if (weeklyData.containsKey(dateStr)) {
                    Map<String, PrayerLogData> dayData = weeklyData.get(dateStr);
                    if (dayData != null && dayData.containsKey(prayerName)) {
                        PrayerLogData logData = dayData.get(prayerName);
                        if (logData != null) {
                            PrayerLog.PrayerStatus status = logData.status;
                            if (status == PrayerLog.PrayerStatus.ADA || 
                                status == PrayerLog.PrayerStatus.QADA) {
                                completedPrayers++; // 分子 +1
                            }
                        }
                    }
                }
            }
        }
        
        date = date.plusDays(1);
    }
    
    // 4. 计算完成率
    int completionRate = totalPrayers > 0 ? (completedPrayers * 100 / totalPrayers) : 0;
    
    // 5. 更新 UI
    circularProgress.setProgress(completionRate);
    tvPercentage.setText(completionRate + "%");
}
```

**关键逻辑**:
1. **分母 (totalPrayers)**: 统计所有"应该完成"的祷告数
   - 日期在 `[qadaStart, today]` 范围内
   - 祷告窗口已开始 (`shouldIncludePrayerInDenominator()`)
   
2. **分子 (completedPrayers)**: 统计已完成的祷告数
   - 在 `weeklyData` 中有记录
   - 状态为 `ADA` 或 `QADA`
   
3. **完成率**: `completedPrayers / totalPrayers * 100%`

---

### 3. 潜在问题点

#### 问题点 1: 匿名用户 ID 不一致 ⚠️

**可能原因**:
- 用户在**记录祷告**时使用的 `userId` 与 **QadaTracker 查询**时使用的 `userId` 不一致

**记录祷告的逻辑** (PrayersFragment.java):
```java
// 当用户点击记录祷告按钮时
private void onPrayerLogged(String prayerName, int newStatus) {
    // 内部会调用 PrayerLogRepository 保存记录
    // 使用 FirebaseAuth.getInstance().getCurrentUser().getUid()
}
```

**查询祷告的逻辑** (PrayerLogRepository.kt):
```kotlin
suspend fun getPrayerLogsByDateRangeWithIds(...): Map<...> {
    val userId = auth.currentUser?.uid  // 获取当前用户 ID
    if (userId == null) {
        return emptyMap()  // ⚠️ 如果为空，返回空结果
    }
    
    val snapshot = firestore.collection("prayer_logs")
        .whereEqualTo("userId", userId)  // ⚠️ 查询条件
        ...
}
```

**诊断建议**:
1. 检查用户在记录祷告时的 `userId`
2. 检查 QadaTracker 查询时的 `userId`
3. 确认两者是否一致

---

#### 问题点 2: 匿名登录时机问题 ⚠️

**当前逻辑** (App.java, 行 395-424):
```java
// 在 App.onCreate() 中，延迟 1 秒后才进行匿名登录
new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
    if (!authManager.isUserSignedIn()) {
        authManager.signInAnonymously(new AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                Log.d("DIAGNOSE", "✅ Anonymous sign-in successful");
                Log.d("DIAGNOSE", "   → User ID: " + user.getUid());
            }
            ...
        });
    }
}, 1000);
```

**潜在问题**:
- 如果用户**在匿名登录完成前**就打开了 QadaTrackerActivity
- QadaTrackerActivity 的 `loadWeeklyData()` 会执行：
  ```java
  FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
  if (user == null) {
      Log.e(TAG, "User not authenticated");
      buildWeeklyPrayerGrid(); // ⚠️ 显示空表格
      return;
  }
  ```
- 导致显示空数据

**诊断建议**:
1. 检查 QadaTrackerActivity 启动时，匿名登录是否已完成
2. 检查是否有 "User not authenticated" 的日志

---

#### 问题点 3: Firestore 数据结构不匹配 ⚠️

**可能原因**:
- 记录的祷告数据中，`date` 字段格式与查询不一致
- 例如：记录时使用 "2024-12-29"，查询时使用 "12/29/2024"

**Firestore 查询条件**:
```kotlin
.whereGreaterThanOrEqualTo("date", startDate)  // startDate = "2024-12-29"
.whereLessThanOrEqualTo("date", endDate)       // endDate = "2024-01-04"
```

**数据存储格式** (PrayerLog.kt):
```kotlin
data class PrayerLog(
    val date: String = "",  // 格式: "yyyy-MM-dd" (ISO 8601)
    val prayerName: String = "",
    val status: PrayerStatus = PrayerStatus.MISSED,
    val userId: String = "",
    ...
)
```

**诊断建议**:
1. 检查 Firestore 中实际存储的 `date` 字段格式
2. 检查 `startDate` 和 `endDate` 的格式
3. 确认是否能正确匹配

---

#### 问题点 4: weeklyData 未加载 ⚠️

**可能原因**:
- `loadWeeklyData()` 调用了 `getPrayerLogsByDateRangeWithIdsAsync()`
- 但由于某些原因，callback 未返回数据
- `weeklyData` 仍为空

**表现**:
```java
// 在 updateWeeklyCompletion() 中
if (weeklyData.containsKey(dateStr)) {
    // ⚠️ 如果 weeklyData 为空，这里永远不会执行
    completedPrayers++;
}

// 结果：completedPrayers = 0，completionRate = 0%
```

**诊断建议**:
1. 检查 `loadWeeklyData()` 的日志，看是否有 "Loaded X days of weekly data"
2. 检查 `weeklyData` 的内容
3. 检查 Firestore 返回的文档数量

---

#### 问题点 5: Prayer Breakdown 显示与实际日期不符 ⚠️

**截图显示**:
- Monday 显示有 4 个绿色点 (Fajr, Dhuhr, Asr, Maghrib)
- Tuesday 显示有 1 个绿色点 (Fajr)

**但用户说**:
- 12月29日（周日）完成了 4 次祷告

**可能原因**:
1. **日期对应错误**：
   - 截图中的 "Monday" 可能不是 12/29
   - 12/29/2024 实际是**周日**，不是周一
   - 周统计的范围可能有问题

2. **周起始日不一致**:
   - 代码中使用 `previousOrSame(DayOfWeek.MONDAY)` (周一为第一天)
   - 但显示时可能从周日开始

**诊断建议**:
1. 确认 12/29/2024 是星期几
2. 检查 `currentDate` 的值
3. 检查 `weekStart` 和 `weekEnd` 的计算结果

---

## 🎯 核心问题猜测

### 最可能的原因：匿名用户 ID 不一致

**场景还原**:

1. **12月28日**：用户首次启动应用
   - App 延迟 1 秒后进行匿名登录
   - 生成 userId = "abc123"（假设）
   - 用户设置 Qada 开始日期为 12/28

2. **12月29日**：用户记录祷告
   - 此时 Firebase Auth 的 userId = "abc123"
   - 祷告记录保存到 Firestore:
     ```json
     {
       "userId": "abc123",
       "date": "2024-12-29",
       "prayerName": "Fajr",
       "status": "ADA"
     }
     ```

3. **用户重启应用**（或清除缓存）:
   - App 再次进行匿名登录
   - ⚠️ **生成了新的 userId = "def456"**（不同于之前）
   - 原因：匿名账户未持久化，或被清除

4. **打开 QadaTracker**:
   - `loadWeeklyData()` 查询时使用 userId = "def456"
   - Firestore 查询：
     ```
     WHERE userId = "def456" AND date >= "2024-12-23" AND date <= "2024-12-29"
     ```
   - ⚠️ **返回 0 条记录**（因为之前的记录是 "abc123"）
   - 结果：`weeklyData` 为空，`completedPrayers = 0`，**显示 0%**

5. **Prayer Breakdown 显示问题**:
   - 截图中的绿色点可能是**之前的测试数据**
   - 或者是用户使用**另一个账户**（Google 登录）记录的数据

---

## 🔧 诊断方法

### 步骤 1: 检查当前用户 ID

在 QadaTrackerActivity 的 `loadWeeklyData()` 开头添加日志：

```java
private void loadWeeklyData() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
        Log.e(TAG, "❌ User not authenticated");
        buildWeeklyPrayerGrid();
        return;
    }
    
    // ✅ 添加诊断日志
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════");
    Log.d("QADA_DIAGNOSIS", "📊 loadWeeklyData() - User Info");
    Log.d("QADA_DIAGNOSIS", "   User ID: " + user.getUid());
    Log.d("QADA_DIAGNOSIS", "   Is Anonymous: " + user.isAnonymous());
    Log.d("QADA_DIAGNOSIS", "   Email: " + (user.getEmail() != null ? user.getEmail() : "null"));
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════");
    
    LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate weekEnd = currentDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    
    // ✅ 添加日期范围日志
    Log.d("QADA_DIAGNOSIS", "   Week Start: " + weekStart + " (" + weekStart.getDayOfWeek() + ")");
    Log.d("QADA_DIAGNOSIS", "   Week End: " + weekEnd + " (" + weekEnd.getDayOfWeek() + ")");
    Log.d("QADA_DIAGNOSIS", "   Current Date: " + currentDate + " (" + currentDate.getDayOfWeek() + ")");
    
    prayerLogRepository.getPrayerLogsByDateRangeWithIdsAsync(...);
}
```

---

### 步骤 2: 检查 Firestore 返回的数据

在 `getPrayerLogsByDateRangeWithIds()` 中添加详细日志：

```kotlin
suspend fun getPrayerLogsByDateRangeWithIds(
    startDate: String,
    endDate: String
): Map<String, Map<String, PrayerLogInfo>> {
    val userId = auth.currentUser?.uid
    
    // ✅ 添加诊断日志
    Log.d(TAG, "════════════════════════════════════════")
    Log.d(TAG, "🔍 getPrayerLogsByDateRangeWithIds()")
    Log.d(TAG, "   User ID: $userId")
    Log.d(TAG, "   Start Date: $startDate")
    Log.d(TAG, "   End Date: $endDate")
    
    if (userId == null) {
        Log.e(TAG, "❌ User not authenticated - returning empty map")
        return emptyMap()
    }
    
    val snapshot = firestore.collection("prayer_logs")
        .whereEqualTo("userId", userId)
        .whereGreaterThanOrEqualTo("date", startDate)
        .whereLessThanOrEqualTo("date", endDate)
        .orderBy("date", Query.Direction.ASCENDING)
        .get()
        .await()
    
    // ✅ 添加返回数据日志
    Log.d(TAG, "   Firestore returned: ${snapshot.documents.size} documents")
    
    // ✅ 打印所有文档的详细信息
    snapshot.documents.forEachIndexed { index, doc ->
        Log.d(TAG, "   Doc $index:")
        Log.d(TAG, "      ID: ${doc.id}")
        Log.d(TAG, "      userId: ${doc.getString("userId")}")
        Log.d(TAG, "      date: ${doc.getString("date")}")
        Log.d(TAG, "      prayerName: ${doc.getString("prayerName")}")
        Log.d(TAG, "      status: ${doc.getString("status")}")
    }
    
    // ...处理数据...
    
    Log.d(TAG, "   Processed result: ${result.size} dates")
    Log.d(TAG, "════════════════════════════════════════")
    
    return result
}
```

---

### 步骤 3: 检查 weeklyData 的内容

在 `updateWeeklyCompletion()` 开头添加日志：

```java
private void updateWeeklyCompletion() {
    // ✅ 添加诊断日志
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════");
    Log.d("QADA_DIAGNOSIS", "📊 updateWeeklyCompletion()");
    Log.d("QADA_DIAGNOSIS", "   weeklyData size: " + weeklyData.size());
    
    // ✅ 打印 weeklyData 的内容
    for (Map.Entry<String, Map<String, PrayerLogData>> dateEntry : weeklyData.entrySet()) {
        String date = dateEntry.getKey();
        Map<String, PrayerLogData> prayers = dateEntry.getValue();
        Log.d("QADA_DIAGNOSIS", "   " + date + ":");
        for (Map.Entry<String, PrayerLogData> prayerEntry : prayers.entrySet()) {
            Log.d("QADA_DIAGNOSIS", "      " + prayerEntry.getKey() + " -> " + prayerEntry.getValue().status);
        }
    }
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════");
    
    // ...计算逻辑...
}
```

---

### 步骤 4: 检查月统计是否有同样问题

月统计使用类似的逻辑：

```java
private void loadMonthlyData() {
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
        Log.e(TAG, "User not authenticated");
        buildMonthlyPrayerTable();
        return;
    }
    
    // ✅ 添加同样的诊断日志
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════");
    Log.d("QADA_DIAGNOSIS", "📊 loadMonthlyData() - User Info");
    Log.d("QADA_DIAGNOSIS", "   User ID: " + user.getUid());
    Log.d("QADA_DIAGNOSIS", "   Is Anonymous: " + user.isAnonymous());
    // ...
}

private void updateMonthlyCompletion() {
    // ✅ 添加同样的数据检查日志
    Log.d("QADA_DIAGNOSIS", "   monthlyData size: " + monthlyData.size());
    // ...
}
```

---

## 🎯 预期结果

### 如果问题是"用户 ID 不一致"：

**日志输出**:
```
QADA_DIAGNOSIS: 📊 loadWeeklyData() - User Info
QADA_DIAGNOSIS:    User ID: xyz789
QADA_DIAGNOSIS:    Is Anonymous: true

PrayerLogRepository: 🔍 getPrayerLogsByDateRangeWithIds()
PrayerLogRepository:    User ID: xyz789
PrayerLogRepository:    Start Date: 2024-12-23
PrayerLogRepository:    End Date: 2024-12-29
PrayerLogRepository:    Firestore returned: 0 documents  ⚠️

QADA_DIAGNOSIS: 📊 updateWeeklyCompletion()
QADA_DIAGNOSIS:    weeklyData size: 0  ⚠️
```

**解决方案**:
1. 使用 Firebase Console 检查 `prayer_logs` collection
2. 查找是否有不同 `userId` 的记录
3. 实现匿名账户持久化，防止重新生成

---

### 如果问题是"日期格式不匹配"：

**日志输出**:
```
PrayerLogRepository: 🔍 getPrayerLogsByDateRangeWithIds()
PrayerLogRepository:    User ID: abc123
PrayerLogRepository:    Start Date: 2024-12-23
PrayerLogRepository:    End Date: 2024-12-29
PrayerLogRepository:    Firestore returned: 4 documents
PrayerLogRepository:    Doc 0:
PrayerLogRepository:       date: 12/29/2024  ⚠️ 格式不匹配
PrayerLogRepository:       prayerName: Fajr
```

**解决方案**:
统一日期格式为 ISO 8601 (`yyyy-MM-dd`)

---

### 如果问题是"祷告名称不匹配"：

**日志输出**:
```
PrayerLogRepository:    Doc 0:
PrayerLogRepository:       prayerName: Fajar  ⚠️ 拼写错误
PrayerLogRepository:       status: ADA

QADA_DIAGNOSIS:    weeklyData size: 1
QADA_DIAGNOSIS:    2024-12-29:
QADA_DIAGNOSIS:       Fajar -> ADA  ⚠️ 与代码中的 "Fajr" 不匹配
```

**解决方案**:
标准化祷告名称（代码中使用 `prayerNames = ["Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"]`）

---

## 📝 总结

### 统计规则

**Weekly Tab**:
- **分母**: 本周内，Qada 开始日期之后，祷告窗口已开始的所有祷告数
- **分子**: 上述祷告中，状态为 `ADA` 或 `QADA` 的数量
- **公式**: `completionRate = (completedPrayers / totalPrayers) * 100%`

**Monthly Tab**:
- 逻辑相同，但范围是当月 (`monthStart` 到 `monthEnd`)

**圆形图表规则**:
- 直接使用计算出的 `completionRate` 作为进度条的值 (0-100)
- UI 更新: `circularProgress.setProgress(completionRate)`

---

### 最可能的问题原因

1. **匿名用户 ID 不一致**（最可能，概率 70%）
   - 记录祷告时的 userId ≠ 查询时的 userId
   - 导致 Firestore 返回 0 条记录

2. **匿名登录未完成**（概率 20%）
   - QadaTracker 打开时，匿名登录还未完成
   - `FirebaseAuth.getCurrentUser()` 返回 null

3. **日期范围计算错误**（概率 5%）
   - `weekStart` 或 `weekEnd` 计算有误
   - 12/29 不在查询范围内

4. **祷告名称或日期格式不匹配**（概率 5%）
   - Firestore 中的数据格式与查询条件不一致

---

### 下一步行动

1. ✅ **添加诊断日志**（不修改逻辑，只添加日志）
2. ✅ **运行应用并复现问题**
3. ✅ **收集完整日志**（使用 `adb logcat | grep "QADA_DIAGNOSIS\|PrayerLogRepository"`）
4. ✅ **分析日志输出**，定位具体问题
5. ✅ **针对性修复**

---

**文档版本**: v1.0  
**创建时间**: 2024-12-29  
**状态**: 🔍 等待日志诊断

