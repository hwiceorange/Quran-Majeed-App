# 每日任务重置逻辑分析报告

## 📅 重置规则总结

### ✅ 当前实现逻辑

**重置时间**: 每天 **00:00:00**（本地时区）  
**重置方式**: **被动重置**（下次访问时检查日期）  
**时区依赖**: 使用设备本地时区  

对于 **UTC+8（北京时间）** 的设备：
- 🕐 重置时间点：**每天 00:00:00 北京时间**
- 📍 判断依据：`SimpleDateFormat("yyyy-MM-dd", Locale.US)` 格式化的日期字符串

---

## 🔍 代码实现详解

### 1️⃣ 日期获取方式

**QuranReadingTracker.java & QuranListeningTracker.java:**
```java
private String getTodayDateString() {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    return sdf.format(new Date());
}
```

**特点**：
- ✅ 使用 `SimpleDateFormat` 格式化 `new Date()`
- ✅ 默认使用**系统本地时区**（Asia/Shanghai = UTC+8）
- ✅ 返回格式：`"2025-10-22"`

**QuestRepository.kt:**
```kotlin
val date: LocalDate = LocalDate.now()
val dateId = date.toString() // "2025-10-22"
```

**特点**：
- ✅ 使用 `LocalDate.now()` 获取本地日期
- ✅ 默认使用**系统默认时区**
- ✅ 与 `SimpleDateFormat` 结果一致

---

### 2️⃣ 重置触发逻辑

**位置**: `QuranReadingTracker.getTodayPagesRead()` / `QuranListeningTracker.getTodaySecondsListened()`

```java
public int getTodayPagesRead() {
    String today = getTodayDateString();              // "2025-10-22"
    String savedDate = prefs.getString(KEY_LAST_DATE, ""); // "2025-10-21"
    
    // 如果日期不匹配 → 重置
    if (!today.equals(savedDate)) {
        resetDailyProgress();
        return 0;
    }
    
    return prefs.getInt(KEY_PAGES_READ_TODAY, 0);
}
```

**重置条件**: `today != savedDate`

**重置动作**:
```java
private void resetDailyProgress() {
    String today = getTodayDateString();
    prefs.edit()
        .putInt(KEY_PAGES_READ_TODAY, 0)           // ✅ 清零进度
        .putString(KEY_LAST_DATE, today)            // ✅ 更新日期
        .putBoolean(KEY_TASK_COMPLETED_TODAY, false) // ✅ 重置完成状态
        .apply();
}
```

---

### 3️⃣ 重置时机（被动触发）

**关键点**: 不是主动定时重置，而是**被动检查**

**触发时机**:
1. 用户打开应用进入主页
2. `DailyQuestsManager` 初始化
3. 调用 `getTodayPagesRead()` / `getTodaySecondsListened()`
4. **此时才比较日期并可能重置**

**示例场景**:

| 时间 | 动作 | last_date | 当前日期 | 是否重置 |
|------|------|-----------|----------|----------|
| 10-21 23:50 | 完成任务 | 2025-10-21 | 2025-10-21 | ❌ |
| 10-21 23:59 | 关闭应用 | 2025-10-21 | 2025-10-21 | ❌ |
| **(跨天)** | - | - | - | - |
| 10-22 00:00 | *(应用未打开)* | 2025-10-21 | 2025-10-22 | ⏳ 待触发 |
| 10-22 08:00 | **打开应用** | 2025-10-21 | 2025-10-22 | ✅ **重置** |

---

## 📊 当前设备状态分析

### 设备信息
```
时区: Asia/Shanghai (UTC+8)
当前时间: 2025-10-22 12:11:34 CST
重置时间点: 每天 00:00:00 北京时间
```

### SharedPreferences 实际数据

**阅读任务**:
```xml
<int name="pages_read_today" value="7" />
<string name="last_date">2025-10-22</string>
<boolean name="task_completed_today" value="true" />
<string name="completed_date">2025-10-22</string>
```

**听力任务**:
```xml
<int name="seconds_listened_today" value="904" />
<int name="minutes_listened_today" value="15" />
<string name="last_date">2025-10-22</string>
<boolean name="task_completed_today" value="true" />
<string name="completed_date">2025-10-22</string>
```

### 状态判断

| 项目 | 值 | 分析 |
|------|-----|------|
| 存储日期 | `2025-10-22` | ✅ |
| 当前日期 | `2025-10-22` | ✅ |
| 日期匹配 | **YES** | ✅ 今天的任务记录 |
| 显示完成状态 | **正确** | ✅ 今天已完成 |

**结论**: 
- ✅ **任务状态正确**
- 📅 记录日期是 `2025-10-22`（今天）
- 🕐 下次重置时间: **明天 00:00:00 北京时间**

---

## ❓ 用户报告场景分析

### 用户描述
> "昨晚23点左右进行了3个任务测试并都是完成状态，Streak +1。目前还是完成状态"

### 可能情况分析

#### 情况A: 完成时间在 10-22 00:00 之后 ✅
```
10-21 23:00 → 10-22 00:30 之间的某个时间完成任务
记录日期: 2025-10-22
当前日期: 2025-10-22
状态: 显示完成 → ✅ 正确
```

#### 情况B: 完成时间在 10-21 23:xx ❌
```
10-21 23:xx 完成任务
记录日期应该是: 2025-10-21
但实际记录是: 2025-10-22 ← 有问题
```

### 验证方法

**请用户确认**:
1. ❓ 昨晚完成任务的确切时间？
   - 是 **10-21 23:xx**（10月21日晚上11点）？
   - 还是 **10-22 00:xx**（10月22日凌晨0点后）？

2. ❓ 完成任务后是否重启过应用？
   - 如果在 10-21 23:xx 完成，然后 10-22 00:xx 后重启，日期会更新为 10-22

3. ❓ 主页任务卡当前显示状态？
   - 是否显示 ✓（完成标记）？
   - Streak 数字是多少？

---

## 🧪 测试重置逻辑

### 测试步骤

我可以为您创建一个测试来验证重置逻辑是否正常工作：

```bash
# 1. 手动设置 last_date 为昨天
adb shell "run-as com.quran.quranaudio.online ..."

# 2. 重启应用
adb shell am force-stop com.quran.quranaudio.online
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity

# 3. 检查是否自动重置
adb shell "run-as com.quran.quranaudio.online cat .../QuranReadingQuestPrefs.xml"
```

**预期结果**:
- `last_date` 应更新为今天
- `task_completed_today` 应重置为 `false`
- `pages_read_today` 应重置为 `0`

---

## 🔧 潜在问题和建议

### 当前实现的问题

#### 1️⃣ 被动重置可能导致的问题

**场景**: 用户在跨天时刻不打开应用
```
10-21 完成任务 → 不打开应用 → 10-23 打开应用
```

**当前行为**:
- ✅ 10-23 打开时会重置
- ⚠️ 但 10-22 的记录丢失（没有创建 10-22 的 Firebase 文档）
- ⚠️ Streak 检测可能误判为"跳过一天"

#### 2️⃣ 时区切换问题

**场景**: 用户跨时区旅行

```
北京时间 10-21 23:00 完成任务 (UTC+8)
→ 飞到伦敦 (UTC+0)
→ 伦敦时间 10-21 16:00 打开应用
```

**当前行为**:
- ⚠️ 系统认为还是 10-21，不会重置
- ⚠️ 但实际上北京已经到 10-22

#### 3️⃣ Firebase 文档日期与本地不一致

**可能情况**:
- 本地 SharedPreferences 使用 `SimpleDateFormat`（系统时区）
- Firebase 使用 `LocalDate.now()`（系统时区）
- 如果用户在跨时区时完成任务，可能不一致

---

## ✅ 当前实现的正确性

### 对于固定时区用户（大多数情况）

**结论**: ✅ **实现正确**

**理由**:
1. ✅ 使用本地时区，符合用户直觉
2. ✅ 重置时间为每天 00:00:00 本地时间
3. ✅ SharedPreferences 和 Firebase 使用相同的日期逻辑
4. ✅ 被动重置虽然不是最优，但足够可靠

### 对于您的设备（UTC+8 北京）

**重置时间**: **每天 00:00:00 北京时间**

**时间轴**:
```
10-21 23:59:59 CST → 任务状态保留（10-21的记录）
10-22 00:00:00 CST → 日期切换（但应用未打开，未重置）
10-22 08:00:00 CST → 打开应用 → 检测到日期变化 → 自动重置
```

**当前状态**:
- ✅ `last_date = 2025-10-22`
- ✅ `current_date = 2025-10-22`
- ✅ 显示完成状态是正确的（因为是今天完成的）

---

## 🎯 结论和建议

### 结论

1. **重置规则正确**: ✅ 每天 00:00:00 本地时区重置
2. **当前状态正确**: ✅ 您的设备显示今天的完成状态
3. **用户记忆可能有误**: 
   - 可能在 10-22 凌晨 00:xx 之后才完成任务
   - 或者在 10-22 早上重启应用后才记录为 10-22

### 建议

#### 立即验证方案

**请用户确认以下信息**:
1. 昨晚完成任务的准确时间（是 21号晚上还是 22号凌晨）
2. 完成任务后是否重启过应用
3. Firebase Console 中查看 `dailyProgress` 集合，查看文档ID和时间戳

#### 可选优化（如需要）

如果确认有问题，可以考虑：

1. **添加详细日志**:
```java
Log.d(TAG, "📅 Date check: saved=" + savedDate + ", today=" + today + 
           ", time=" + System.currentTimeMillis());
```

2. **添加主动重置（WorkManager）**:
```kotlin
// 每天凌晨 00:01 主动检查并重置
WorkManager.enqueue(
    PeriodicWorkRequest.Builder(ResetQuestWorker::class.java, 24, TimeUnit.HOURS)
        .setInitialDelay(calculateDelayUntilMidnight(), TimeUnit.MILLISECONDS)
        .build()
)
```

3. **在 UI 显示重置倒计时**:
```kotlin
"任务将在 XX小时XX分后重置"
```

---

## 📝 测试验证脚本

如需验证重置逻辑，请运行以下命令：

```bash
# 1. 查看当前状态
adb shell "run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml"

# 2. 手动设置为昨天
YESTERDAY=$(date -v-1d '+%Y-%m-%d' 2>/dev/null || date -d 'yesterday' '+%Y-%m-%d')
# ... 修改 SharedPreferences ...

# 3. 重启应用并检查
adb shell am force-stop com.quran.quranaudio.online
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
sleep 5

# 4. 验证是否重置
adb shell "run-as com.quran.quranaudio.online cat /data/data/com.quran.quranaudio.online/shared_prefs/QuranReadingQuestPrefs.xml"
```

---

**生成时间**: 2025-10-22 12:11  
**设备时区**: Asia/Shanghai (UTC+8)  
**当前日期**: 2025-10-22


