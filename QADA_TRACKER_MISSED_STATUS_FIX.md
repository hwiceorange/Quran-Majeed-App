# ✅ Qada' Tracker Missed 状态计算修复

## 🐛 问题描述

### 用户报告:
> "11月6日周四，Dhuhr 是灰色的状态，正常应该属于 Missed 状态。"

### 根本原因:
```
数据库查询结果（2025-11-06）:
✅ Fajr -> MISSED    (有记录)
✅ Asr -> ADA        (有记录)
✅ Maghrib -> QADA   (有记录)
❌ Dhuhr -> (无记录)  (数据库中不存在)

旧逻辑:
- 有记录 → 显示对应状态 ✅
- 无记录 → 显示灰色 Pending ❌

正确逻辑:
- 有记录 → 显示对应状态 ✅
- 无记录 + 时间已过 → 显示红色 Missed ✅
- 无记录 + 时间未过 → 显示灰色 Pending ✅
```

---

## 🔧 解决方案

### 核心逻辑

在 `getPrayerStatus()` 方法中添加**时间判断**：

```java
// 1. 数据库中有记录 → 返回数据库状态
if (dataSource.containsKey(date) && dayData.containsKey(prayerName)) {
    return dbStatus; // Ada', Qada', Missed
}

// 2. 数据库中无记录 → 根据时间判断
LocalDate prayerDate = LocalDate.parse(date);
LocalDate today = LocalDate.now();

// 2a. 未来日期 → Pending (灰色)
if (prayerDate.isAfter(today)) {
    return -1; // Pending
}

// 2b. 今天 → 检查祷告时间是否已过
if (prayerDate.isEqual(today)) {
    if (isPrayerTimePassed(prayerName)) {
        return 2; // Missed (红色)
    } else {
        return -1; // Pending (灰色)
    }
}

// 2c. 过去日期 → Missed (红色)
if (prayerDate.isBefore(today)) {
    return 2; // Missed
}
```

---

## 📝 详细修改

### Before (旧代码):

```java
private int getPrayerStatus(String date, String prayerName, boolean isWeekly) {
    Map<String, Map<String, PrayerLog.PrayerStatus>> dataSource = 
        isWeekly ? weeklyData : monthlyData;
    
    if (dataSource.containsKey(date)) {
        Map<String, PrayerLog.PrayerStatus> dayData = dataSource.get(date);
        
        if (dayData != null && dayData.containsKey(prayerName)) {
            PrayerLog.PrayerStatus status = dayData.get(prayerName);
            
            if (status == PrayerLog.PrayerStatus.ADA) {
                return 0;
            } else if (status == PrayerLog.PrayerStatus.QADA) {
                return 1;
            } else if (status == PrayerLog.PrayerStatus.MISSED) {
                return 2;
            }
        }
    }
    
    // ❌ 问题：无记录直接返回 Pending，没有判断时间
    return -1; // Pending (grey)
}
```

### After (新代码):

```java
private int getPrayerStatus(String date, String prayerName, boolean isWeekly) {
    Map<String, Map<String, PrayerLog.PrayerStatus>> dataSource = 
        isWeekly ? weeklyData : monthlyData;
    
    // 1. Check database records
    if (dataSource.containsKey(date)) {
        Map<String, PrayerLog.PrayerStatus> dayData = dataSource.get(date);
        
        if (dayData != null && dayData.containsKey(prayerName)) {
            PrayerLog.PrayerStatus status = dayData.get(prayerName);
            
            if (status == PrayerLog.PrayerStatus.ADA) {
                return 0;
            } else if (status == PrayerLog.PrayerStatus.QADA) {
                return 1;
            } else if (status == PrayerLog.PrayerStatus.MISSED) {
                return 2;
            }
        }
    }
    
    // 2. No record found - check if prayer time has passed
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        LocalDate prayerDate = LocalDate.parse(date);
        LocalDate today = LocalDate.now();
        
        // ✅ 未来日期 → Pending
        if (prayerDate.isAfter(today)) {
            return -1; // Pending (grey)
        }
        
        // ✅ 今天 → 检查时间
        if (prayerDate.isEqual(today)) {
            if (isPrayerTimePassedForDate(prayerName)) {
                return 2; // Missed (red)
            } else {
                return -1; // Pending (grey)
            }
        }
        
        // ✅ 过去日期 → Missed
        if (prayerDate.isBefore(today)) {
            return 2; // Missed (red)
        }
    }
    
    return -1;
}
```

---

## 🆕 新增辅助方法

### 1. `isPrayerTimePassedForDate()` - 检查祷告时间是否已过

```java
private boolean isPrayerTimePassedForDate(String prayerName) {
    Calendar now = Calendar.getInstance();
    
    // Get next prayer after current
    String nextPrayer = getNextPrayerName(prayerName);
    
    // Special case: Isha is the last prayer
    if (nextPrayer == null) {
        Calendar midnight = Calendar.getInstance();
        midnight.set(Calendar.HOUR_OF_DAY, 23);
        midnight.set(Calendar.MINUTE, 59);
        midnight.set(Calendar.SECOND, 59);
        return now.after(midnight);
    }
    
    // Simplified time checking
    int currentHour = now.get(Calendar.HOUR_OF_DAY);
    
    switch (prayerName) {
        case "Fajr":
            return currentHour >= 11; // After Dhuhr time
        case "Dhuhr":
            return currentHour >= 15; // After Asr time (15:00)
        case "Asr":
            return currentHour >= 18; // After Maghrib time
        case "Maghrib":
            return currentHour >= 20; // After Isha time
        case "Isha":
            return currentHour >= 23; // After midnight
        default:
            return false;
    }
}
```

### 2. `getNextPrayerName()` - 获取下一个祷告名称

```java
private String getNextPrayerName(String current) {
    switch (current) {
        case "Fajr":
            return "Dhuhr";
        case "Dhuhr":
            return "Asr";
        case "Asr":
            return "Maghrib";
        case "Maghrib":
            return "Isha";
        case "Isha":
            return null; // Last prayer
        default:
            return null;
    }
}
```

---

## 📊 状态计算逻辑表

| 情况 | 数据库 | 日期 | 时间判断 | 显示状态 |
|-----|-------|------|---------|---------|
| **有记录** | ✅ Ada' | - | - | 🟢 Ada' |
| **有记录** | ✅ Qada' | - | - | 🟠 Qada' |
| **有记录** | ✅ Missed | - | - | 🔴 Missed |
| **无记录** | ❌ | 未来 | - | ⚪ Pending |
| **无记录** | ❌ | 今天 | 时间已过 | 🔴 Missed |
| **无记录** | ❌ | 今天 | 时间未过 | ⚪ Pending |
| **无记录** | ❌ | 过去 | - | 🔴 Missed |

---

## 🧪 测试案例

### 案例 1: 11月6日 Dhuhr (无记录)

**数据**:
- 日期: 2025-11-06 (昨天)
- 祷告: Dhuhr
- 数据库: 无记录
- 当前时间: 2025-11-07 11:05

**判断逻辑**:
```java
prayerDate = 2025-11-06
today = 2025-11-07

// prayerDate.isBefore(today) = true
// → Return 2 (Missed)
```

**结果**: 🔴 Missed ✅

---

### 案例 2: 11月7日 Dhuhr (无记录，时间已过)

**数据**:
- 日期: 2025-11-07 (今天)
- 祷告: Dhuhr
- 数据库: 无记录
- 当前时间: 2025-11-07 15:30 (下午3:30)

**判断逻辑**:
```java
prayerDate = 2025-11-07
today = 2025-11-07

// prayerDate.isEqual(today) = true
// isPrayerTimePassed("Dhuhr")?
//   currentHour = 15
//   Dhuhr threshold = 15
//   15 >= 15 = true
// → Return 2 (Missed)
```

**结果**: 🔴 Missed ✅

---

### 案例 3: 11月7日 Isha (无记录，时间未过)

**数据**:
- 日期: 2025-11-07 (今天)
- 祷告: Isha
- 数据库: 无记录
- 当前时间: 2025-11-07 19:00 (晚上7点)

**判断逻辑**:
```java
prayerDate = 2025-11-07
today = 2025-11-07

// prayerDate.isEqual(today) = true
// isPrayerTimePassed("Isha")?
//   currentHour = 19
//   Isha threshold = 23
//   19 >= 23 = false
// → Return -1 (Pending)
```

**结果**: ⚪ Pending ✅

---

### 案例 4: 11月8日 Fajr (无记录，未来)

**数据**:
- 日期: 2025-11-08 (明天)
- 祷告: Fajr
- 数据库: 无记录
- 当前时间: 2025-11-07 11:05

**判断逻辑**:
```java
prayerDate = 2025-11-08
today = 2025-11-07

// prayerDate.isAfter(today) = true
// → Return -1 (Pending)
```

**结果**: ⚪ Pending ✅

---

## ✅ 修复效果

### Before (旧版):
```
11月6日 Dhuhr (无记录):
⚪ Pending (错误！应该是 Missed)
```

### After (新版):
```
11月6日 Dhuhr (无记录):
🔴 Missed ✅ (正确！因为日期在过去)
```

---

## 📈 完整状态判断流程图

```
开始
  ↓
数据库中有记录？
  ├─ 是 → 返回数据库状态 (Ada'/Qada'/Missed)
  └─ 否 ↓
       日期在未来？
         ├─ 是 → 返回 Pending ⚪
         └─ 否 ↓
              日期是今天？
                ├─ 是 ↓
                │    祷告时间已过？
                │      ├─ 是 → 返回 Missed 🔴
                │      └─ 否 → 返回 Pending ⚪
                └─ 否 → 返回 Missed 🔴 (过去日期)
```

---

## 🔍 验证步骤

1. **打开 Qada Tracker**
   - Salat 页面 → 点击 "Total Outstanding Qada'"

2. **查看 11月6日 Dhuhr**
   - ✅ 应该显示 🔴 Missed（红色圆点）
   - ❌ 不应该显示 ⚪ Pending（灰色圆点）

3. **查看未来日期的祷告**
   - 应该显示 ⚪ Pending（灰色圆点）

4. **查看今天已过时间的祷告（无记录）**
   - 应该显示 🔴 Missed（红色圆点）

5. **查看今天未过时间的祷告（无记录）**
   - 应该显示 ⚪ Pending（灰色圆点）

---

## 📝 相关文件

| 文件 | 修改内容 |
|-----|---------|
| `QadaTrackerActivity.java` | 修改 `getPrayerStatus()` 添加时间判断逻辑 |
| `QadaTrackerActivity.java` | 新增 `isPrayerTimePassedForDate()` 方法 |
| `QadaTrackerActivity.java` | 新增 `getNextPrayerName()` 方法 |

---

## 🎯 总结

### 问题
❌ 无数据库记录的祷告统一显示为 Pending（灰色），没有判断时间

### 修复
✅ 添加日期和时间判断逻辑：
- 未来日期 → Pending
- 今天 + 时间已过 → Missed
- 今天 + 时间未过 → Pending
- 过去日期 → Missed

### 效果
✅ 11月6日 Dhuhr (无记录) 正确显示为 🔴 Missed  
✅ 历史记录正确标记为 Missed  
✅ 未来日期正确显示为 Pending  
✅ 当天根据时间动态显示  

---

**修复时间**: 2025-11-07  
**版本**: v1.7.4 (versionCode: 66)  
**状态**: ✅ 完成并已安装





