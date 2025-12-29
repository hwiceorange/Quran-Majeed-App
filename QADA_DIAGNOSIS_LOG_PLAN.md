# 🔧 Qada 统计诊断日志方案

## 📋 目标

在不修改任何业务逻辑的情况下，添加详细的诊断日志，以便：
1. 确认当前用户的 userId
2. 确认 Firestore 返回的数据
3. 确认 weeklyData 和 monthlyData 的内容
4. 确认圆形图表的计算过程

---

## 🔍 方案 1: QadaTrackerActivity.java 添加日志

### 修改位置 1: loadWeeklyData()

**文件**: `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/QadaTrackerActivity.java`  
**行号**: 1216-1260

**修改内容**:

```java
private void loadWeeklyData() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
    
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
        Log.e(TAG, "❌ User not authenticated");
        buildWeeklyPrayerGrid(); // Show empty grid
        return;
    }
    
    // ✅ 【诊断日志 1】用户信息
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════════════════════");
    Log.d("QADA_DIAGNOSIS", "📊 loadWeeklyData() - START");
    Log.d("QADA_DIAGNOSIS", "   🔐 User Info:");
    Log.d("QADA_DIAGNOSIS", "      User ID: " + user.getUid());
    Log.d("QADA_DIAGNOSIS", "      Is Anonymous: " + user.isAnonymous());
    Log.d("QADA_DIAGNOSIS", "      Email: " + (user.getEmail() != null ? user.getEmail() : "null"));
    
    LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate weekEnd = currentDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    
    // ✅ 【诊断日志 2】日期范围
    Log.d("QADA_DIAGNOSIS", "   📅 Date Range:");
    Log.d("QADA_DIAGNOSIS", "      Current Date: " + currentDate + " (" + currentDate.getDayOfWeek() + ")");
    Log.d("QADA_DIAGNOSIS", "      Week Start: " + weekStart + " (" + weekStart.getDayOfWeek() + ")");
    Log.d("QADA_DIAGNOSIS", "      Week End: " + weekEnd + " (" + weekEnd.getDayOfWeek() + ")");
    Log.d("QADA_DIAGNOSIS", "      Query Range: " + weekStart + " to " + weekEnd);
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════════════════════");
    
    Log.d(TAG, "Loading weekly data from " + weekStart + " to " + weekEnd);
    
    // Load data from Firestore with log IDs
    prayerLogRepository.getPrayerLogsByDateRangeWithIdsAsync(
        weekStart.toString(),
        weekEnd.toString(),
        new PrayerLogRepository.DateRangeWithIdsCallback() {
            @Override
            public void onResult(Map<String, Map<String, PrayerLogRepository.PrayerLogInfo>> data) {
                // ✅ 【诊断日志 3】Firestore 返回的数据
                Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════════════════════");
                Log.d("QADA_DIAGNOSIS", "📦 Firestore Query Result:");
                Log.d("QADA_DIAGNOSIS", "   Returned Dates: " + data.size());
                
                for (Map.Entry<String, Map<String, PrayerLogRepository.PrayerLogInfo>> dateEntry : data.entrySet()) {
                    String date = dateEntry.getKey();
                    Map<String, PrayerLogRepository.PrayerLogInfo> prayers = dateEntry.getValue();
                    Log.d("QADA_DIAGNOSIS", "   📆 " + date + " (" + prayers.size() + " prayers):");
                    
                    for (Map.Entry<String, PrayerLogRepository.PrayerLogInfo> prayerEntry : prayers.entrySet()) {
                        PrayerLogRepository.PrayerLogInfo info = prayerEntry.getValue();
                        Log.d("QADA_DIAGNOSIS", "      ✅ " + prayerEntry.getKey() + " -> " + info.getStatus() + " (docId: " + info.getLogId() + ")");
                    }
                }
                
                if (data.isEmpty()) {
                    Log.w("QADA_DIAGNOSIS", "   ⚠️ NO DATA RETURNED FROM FIRESTORE!");
                    Log.w("QADA_DIAGNOSIS", "   Possible reasons:");
                    Log.w("QADA_DIAGNOSIS", "   1. No prayer logs exist for this user in this date range");
                    Log.w("QADA_DIAGNOSIS", "   2. User ID mismatch (prayer logs saved with different userId)");
                    Log.w("QADA_DIAGNOSIS", "   3. Date format mismatch in Firestore");
                }
                Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════════════════════");
                
                weeklyData.clear();
                
                // Convert PrayerLogInfo to PrayerLogData
                for (Map.Entry<String, Map<String, PrayerLogRepository.PrayerLogInfo>> dateEntry : data.entrySet()) {
                    Map<String, PrayerLogData> dayData = new HashMap<>();
                    for (Map.Entry<String, PrayerLogRepository.PrayerLogInfo> prayerEntry : dateEntry.getValue().entrySet()) {
                        PrayerLogRepository.PrayerLogInfo info = prayerEntry.getValue();
                        dayData.put(prayerEntry.getKey(), new PrayerLogData(info.getLogId(), info.getStatus()));
                    }
                    weeklyData.put(dateEntry.getKey(), dayData);
                }
                
                Log.d(TAG, "Loaded " + weeklyData.size() + " days of weekly data with log IDs");
                
                // Update UI on main thread
                runOnUiThread(() -> {
                    buildWeeklyPrayerGrid();
                    updateWeeklyCompletion();
                });
            }
        }
    );
}
```

---

### 修改位置 2: updateWeeklyCompletion()

**文件**: `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/QadaTrackerActivity.java`  
**行号**: 1461-1569

**在方法开头添加**:

```java
private void updateWeeklyCompletion() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
    
    // ✅ 【诊断日志 4】weeklyData 的内容
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════════════════════");
    Log.d("QADA_DIAGNOSIS", "📊 updateWeeklyCompletion() - START");
    Log.d("QADA_DIAGNOSIS", "   weeklyData size: " + weeklyData.size() + " dates");
    
    if (weeklyData.isEmpty()) {
        Log.w("QADA_DIAGNOSIS", "   ⚠️ weeklyData is EMPTY!");
        Log.w("QADA_DIAGNOSIS", "   This means NO prayer logs were loaded from Firestore");
        Log.w("QADA_DIAGNOSIS", "   Result: completionRate will be 0%");
    } else {
        Log.d("QADA_DIAGNOSIS", "   📋 weeklyData content:");
        for (Map.Entry<String, Map<String, PrayerLogData>> dateEntry : weeklyData.entrySet()) {
            String date = dateEntry.getKey();
            Map<String, PrayerLogData> prayers = dateEntry.getValue();
            Log.d("QADA_DIAGNOSIS", "   📆 " + date + ":");
            
            for (Map.Entry<String, PrayerLogData> prayerEntry : prayers.entrySet()) {
                Log.d("QADA_DIAGNOSIS", "      " + prayerEntry.getKey() + " -> " + prayerEntry.getValue().status);
            }
        }
    }
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════════════════════");
    
    // ... 原有的计算逻辑 ...
    
    LocalDate weekStart = currentDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate weekEnd = currentDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
    
    LocalDate qadaStart = parseQadaStartDate();
    LocalDate today = LocalDate.now();
    
    String[] prayerNames = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
    
    int totalPrayers = 0;
    int completedPrayers = 0;
    
    LocalDate date = weekStart;
    while (!date.isAfter(weekEnd)) {
        String dateStr = date.toString();
        
        boolean isValidDate = true;
        if (qadaStart != null && date.isBefore(qadaStart)) {
            isValidDate = false;
        }
        if (date.isAfter(today)) {
            isValidDate = false;
        }
        
        if (isValidDate) {
            for (String prayerName : prayerNames) {
                if (!shouldIncludePrayerInDenominator(date, prayerName)) {
                    continue;
                }
                
                totalPrayers++;
                
                if (weeklyData.containsKey(dateStr)) {
                    Map<String, PrayerLogData> dayData = weeklyData.get(dateStr);
                    if (dayData != null && dayData.containsKey(prayerName)) {
                        PrayerLogData logData = dayData.get(prayerName);
                        if (logData != null) {
                            PrayerLog.PrayerStatus status = logData.status;
                            if (status == PrayerLog.PrayerStatus.ADA || status == PrayerLog.PrayerStatus.QADA) {
                                completedPrayers++;
                            }
                        }
                    }
                }
            }
        }
        
        date = date.plusDays(1);
    }
    
    int completionRate = totalPrayers > 0 ? (completedPrayers * 100 / totalPrayers) : 0;
    
    // ✅ 【诊断日志 5】计算结果
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════════════════════");
    Log.d("QADA_DIAGNOSIS", "📈 Completion Calculation:");
    Log.d("QADA_DIAGNOSIS", "   Total Prayers (denominator): " + totalPrayers);
    Log.d("QADA_DIAGNOSIS", "   Completed Prayers (numerator): " + completedPrayers);
    Log.d("QADA_DIAGNOSIS", "   Completion Rate: " + completionRate + "%");
    Log.d("QADA_DIAGNOSIS", "   Formula: (" + completedPrayers + " / " + totalPrayers + ") * 100 = " + completionRate + "%");
    
    if (completionRate == 0 && totalPrayers > 0) {
        Log.w("QADA_DIAGNOSIS", "   ⚠️ WARNING: 0% completion but totalPrayers > 0");
        Log.w("QADA_DIAGNOSIS", "   This means weeklyData does NOT contain any completed prayers");
        Log.w("QADA_DIAGNOSIS", "   Check if prayer names match between Firestore and code");
    }
    
    if (totalPrayers == 0) {
        Log.w("QADA_DIAGNOSIS", "   ℹ️ No prayers to count in this period");
        Log.w("QADA_DIAGNOSIS", "   Possible reasons:");
        Log.w("QADA_DIAGNOSIS", "   1. All days are before Qada start date");
        Log.w("QADA_DIAGNOSIS", "   2. All days are in the future");
        Log.w("QADA_DIAGNOSIS", "   3. Prayer windows have not started yet");
    }
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════════════════════");
    
    Log.d(TAG, "Weekly completion: " + completedPrayers + "/" + totalPrayers + " = " + completionRate + "%");
    
    // ... 原有的 UI 更新逻辑 ...
}
```

---

### 修改位置 3: loadMonthlyData()

**文件**: `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/QadaTrackerActivity.java`  
**行号**: 1265-1318

**添加类似的日志** (与 loadWeeklyData 相同的模式):

```java
private void loadMonthlyData() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
    
    long timestamp = System.currentTimeMillis();
    Log.d(TAG, "🔍 [LOAD-" + timestamp + "] ========== loadMonthlyData() START ==========");
    
    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
    if (user == null) {
        Log.e(TAG, "❌ User not authenticated");
        buildMonthlyPrayerTable(); // Show empty table
        return;
    }
    
    // ✅ 【诊断日志】月统计用户信息
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════════════════════");
    Log.d("QADA_DIAGNOSIS", "📊 loadMonthlyData() - START");
    Log.d("QADA_DIAGNOSIS", "   🔐 User Info:");
    Log.d("QADA_DIAGNOSIS", "      User ID: " + user.getUid());
    Log.d("QADA_DIAGNOSIS", "      Is Anonymous: " + user.isAnonymous());
    
    LocalDate monthStart = currentDate.withDayOfMonth(1);
    LocalDate monthEnd = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
    
    Log.d("QADA_DIAGNOSIS", "   📅 Date Range:");
    Log.d("QADA_DIAGNOSIS", "      Month Start: " + monthStart);
    Log.d("QADA_DIAGNOSIS", "      Month End: " + monthEnd);
    Log.d("QADA_DIAGNOSIS", "════════════════════════════════════════════════════════");
    
    // ... 其余逻辑类似 ...
}
```

---

## 🔍 方案 2: PrayerLogRepository.kt 添加日志

### 修改位置: getPrayerLogsByDateRangeWithIds()

**文件**: `app/src/main/java/com/quran/quranaudio/online/prayertimes/repository/PrayerLogRepository.kt`  
**行号**: 672-737

**修改内容**:

```kotlin
suspend fun getPrayerLogsByDateRangeWithIds(
    startDate: String,
    endDate: String
): Map<String, Map<String, PrayerLogInfo>> {
    val userId = auth.currentUser?.uid
    
    // ✅ 【诊断日志】Repository 层查询信息
    Log.d(TAG, "════════════════════════════════════════════════════════")
    Log.d(TAG, "🔍 getPrayerLogsByDateRangeWithIds()")
    Log.d(TAG, "   🔐 Query Parameters:")
    Log.d(TAG, "      User ID: $userId")
    Log.d(TAG, "      Start Date: $startDate")
    Log.d(TAG, "      End Date: $endDate")
    Log.d(TAG, "      Collection: prayer_logs")
    
    if (userId == null) {
        Log.e(TAG, "❌ User not authenticated - returning empty map")
        Log.e(TAG, "   This means FirebaseAuth.getCurrentUser() returned null")
        Log.e(TAG, "   Check if anonymous/Google sign-in was successful")
        Log.d(TAG, "════════════════════════════════════════════════════════")
        return emptyMap()
    }
    
    return try {
        Log.d(TAG, "   → Querying Firestore...")
        
        val result = mutableMapOf<String, MutableMap<String, PrayerLogInfo>>()
        
        val snapshot = firestore.collection("prayer_logs")
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.ASCENDING)
            .get()
            .await()
        
        Log.d(TAG, "   ✅ Firestore query completed")
        Log.d(TAG, "   📦 Found ${snapshot.documents.size} documents")
        
        if (snapshot.documents.isEmpty()) {
            Log.w(TAG, "   ⚠️ NO DOCUMENTS FOUND!")
            Log.w(TAG, "   Possible reasons:")
            Log.w(TAG, "   1. No prayer logs exist for userId=$userId in date range [$startDate, $endDate]")
            Log.w(TAG, "   2. Prayer logs were saved with a different userId")
            Log.w(TAG, "   3. Date format in Firestore doesn't match query format")
            Log.w(TAG, "   4. Firestore rules are blocking the read")
            Log.d(TAG, "════════════════════════════════════════════════════════")
            return emptyMap()
        }
        
        // ✅ 详细打印每个文档
        Log.d(TAG, "   📄 Document Details:")
        snapshot.documents.forEachIndexed { index, doc ->
            Log.d(TAG, "   [$index] Document ID: ${doc.id}")
            Log.d(TAG, "        userId: ${doc.getString("userId")}")
            Log.d(TAG, "        date: ${doc.getString("date")}")
            Log.d(TAG, "        prayerName: ${doc.getString("prayerName")}")
            Log.d(TAG, "        status: ${doc.getString("status")}")
            Log.d(TAG, "        loggedAt: ${doc.getTimestamp("loggedAt")}")
        }
        
        // ✅ 先按 loggedAt 排序（最新的在前），然后去重
        val sortedDocs = snapshot.documents.sortedByDescending { doc ->
            doc.getTimestamp("loggedAt")?.toDate()?.time ?: 0L
        }
        
        for (doc in sortedDocs) {
            val log = doc.toObject(PrayerLog::class.java)
            
            if (log != null) {
                val date = log.date
                val prayerName = log.prayerName
                
                // 跳过无效记录
                if (date.isEmpty() || prayerName.isEmpty()) {
                    Log.w(TAG, "  ⚠️ Skipping invalid log: doc.id=${doc.id}, date='$date', prayerName='$prayerName'")
                    continue
                }
                
                val logInfo = PrayerLogInfo(doc.id, log.status)
                
                if (!result.containsKey(date)) {
                    result[date] = mutableMapOf()
                }
                
                // ✅ 去重逻辑：如果同一天同一个祷告有多条记录，只保留第一条（最新的）
                if (!result[date]!!.containsKey(prayerName)) {
                    result[date]!![prayerName] = logInfo
                    Log.d(TAG, "  ✅ $date $prayerName -> ${log.status} (docId: ${doc.id})")
                } else {
                    Log.d(TAG, "  🔄 Duplicate log ignored: $date $prayerName (keeping the latest one)")
                }
            }
        }
        
        Log.d(TAG, "   📊 Processing Summary:")
        Log.d(TAG, "      Processed ${result.size} unique dates")
        Log.d(TAG, "      Total unique prayers: ${result.values.sumOf { it.size }}")
        Log.d(TAG, "════════════════════════════════════════════════════════")
        
        result
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error loading prayer logs with IDs", e)
        Log.e(TAG, "   Exception: ${e.message}")
        Log.e(TAG, "   Stack trace: ${e.stackTraceToString()}")
        Log.d(TAG, "════════════════════════════════════════════════════════")
        emptyMap()
    }
}
```

---

## 📋 完整的日志收集命令

运行应用后，使用以下命令收集日志：

```bash
# 清除旧日志
adb logcat -c

# 启动日志收集（保存到文件）
adb logcat -d | grep -E "QADA_DIAGNOSIS|PrayerLogRepository|QadaTrackerActivity" > qada_diagnosis_log.txt

# 或者实时查看
adb logcat | grep -E "QADA_DIAGNOSIS|PrayerLogRepository|QadaTrackerActivity"
```

---

## 🎯 预期日志输出示例

### 场景 1: 正常情况

```
QADA_DIAGNOSIS: ════════════════════════════════════════════════════════
QADA_DIAGNOSIS: 📊 loadWeeklyData() - START
QADA_DIAGNOSIS:    🔐 User Info:
QADA_DIAGNOSIS:       User ID: abc123xyz
QADA_DIAGNOSIS:       Is Anonymous: true
QADA_DIAGNOSIS:    📅 Date Range:
QADA_DIAGNOSIS:       Week Start: 2024-12-23 (MONDAY)
QADA_DIAGNOSIS:       Week End: 2024-12-29 (SUNDAY)

PrayerLogRepository: 🔍 getPrayerLogsByDateRangeWithIds()
PrayerLogRepository:    User ID: abc123xyz
PrayerLogRepository:    Found 4 documents

QADA_DIAGNOSIS: 📦 Firestore Query Result:
QADA_DIAGNOSIS:    📆 2024-12-29 (4 prayers):
QADA_DIAGNOSIS:       ✅ Fajr -> ADA
QADA_DIAGNOSIS:       ✅ Dhuhr -> ADA
QADA_DIAGNOSIS:       ✅ Asr -> ADA
QADA_DIAGNOSIS:       ✅ Maghrib -> ADA

QADA_DIAGNOSIS: 📊 updateWeeklyCompletion() - START
QADA_DIAGNOSIS:    weeklyData size: 1 dates
QADA_DIAGNOSIS:    📆 2024-12-29:
QADA_DIAGNOSIS:       Fajr -> ADA
QADA_DIAGNOSIS:       Dhuhr -> ADA

QADA_DIAGNOSIS: 📈 Completion Calculation:
QADA_DIAGNOSIS:    Total Prayers: 35 (7 days × 5 prayers)
QADA_DIAGNOSIS:    Completed Prayers: 4
QADA_DIAGNOSIS:    Completion Rate: 11%
```

---

### 场景 2: 用户 ID 不一致

```
QADA_DIAGNOSIS: 📊 loadWeeklyData() - START
QADA_DIAGNOSIS:    User ID: xyz789abc  ⚠️ 不同的 ID

PrayerLogRepository: 🔍 getPrayerLogsByDateRangeWithIds()
PrayerLogRepository:    User ID: xyz789abc
PrayerLogRepository:    Found 0 documents  ⚠️

QADA_DIAGNOSIS: ⚠️ NO DATA RETURNED FROM FIRESTORE!
QADA_DIAGNOSIS:    Possible reasons:
QADA_DIAGNOSIS:    2. User ID mismatch

QADA_DIAGNOSIS: 📊 updateWeeklyCompletion() - START
QADA_DIAGNOSIS:    weeklyData size: 0 dates  ⚠️
QADA_DIAGNOSIS:    ⚠️ weeklyData is EMPTY!

QADA_DIAGNOSIS: 📈 Completion Calculation:
QADA_DIAGNOSIS:    Completion Rate: 0%  ⚠️
```

---

## ✅ 优势

1. **无侵入性**: 只添加日志，不修改任何业务逻辑
2. **完整性**: 覆盖整个数据流（用户认证 → Firestore 查询 → 数据处理 → UI 更新）
3. **可读性**: 使用分隔线和图标，易于在大量日志中定位
4. **诊断性**: 包含多种可能原因的提示

---

**文档版本**: v1.0  
**创建时间**: 2024-12-29  
**状态**: 📝 方案设计完成，等待实施

