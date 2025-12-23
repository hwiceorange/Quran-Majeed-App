# 🕌 Prayer Status UI - 实现总结

## 📅 实施日期
2025-11-05

## 版本信息
- **Version**: 1.7.3 (Build 65)
- **Status**: ✅ 已编译并安装到设备

---

## 🎯 需求

修改 Salat 页面（每日详情）的展示逻辑，使其在不同的记录状态下，显示不同的 UI 元素：

| 状态 | UI 显示 | 点击行为 |
|------|---------|----------|
| **Ada' (准时完成)** | ✅ 绿色圆圈图标 | 进入编辑模式（可改为 Qada'） |
| **Qada' (已弥补)** | ⚠️ 橙色警告图标 | 进入编辑模式（可修改时间/备注） |
| **Missed (错过)** | ❌ 红色错误图标 | 直接进入 Qada' Log 模态框 |
| **Pending (未记录)** | TRACK 按钮 | 进入 Log Prayer 模态框 |

---

## ✅ 实现细节

### 1. 新增图标资源

#### `ic_warning.xml` - Qada' 状态
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="36dp"
    android:height="36dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#FF9800"
        android:pathData="M1,21h22L12,2 1,21zM13,18h-2v-2h2v2zM13,14h-2v-4h2v4z"/>
</vector>
```

#### `ic_error.xml` - Missed 状态
```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="36dp"
    android:height="36dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="#F44336"
        android:pathData="M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10,-4.48 10,-10S17.52,2 12,2zM13,17h-2v-2h2v2zM13,13h-2L11,7h2v6z"/>
</vector>
```

---

### 2. PrayerLogRepository - 状态查询

创建新的 Repository 用于查询 `prayer_logs` 集合：

**文件**: `app/src/main/java/com/quran/quranaudio/online/prayertimes/repository/PrayerLogRepository.kt`

**核心方法**:

#### A. 查询今天的单个祷告记录
```kotlin
suspend fun getTodayPrayerLog(prayerName: String): PrayerLog? {
    val userId = auth.currentUser?.uid ?: return null
    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    
    val query = firestore.collection(PrayerLog.COLLECTION_NAME)
        .whereEqualTo("userId", userId)
        .whereEqualTo("prayerName", prayerName)
        .whereEqualTo("date", today)
        .orderBy("loggedAt", Query.Direction.DESCENDING)
        .limit(1)
    
    val snapshot = query.get().await()
    return if (snapshot.isEmpty) null else snapshot.documents[0].toObject(PrayerLog::class.java)
}
```

#### B. Java 兼容回调接口
```kotlin
interface PrayerLogsCallback {
    fun onResult(logs: Map<String, PrayerLog>)
}

fun getTodayPrayerLogsAsync(callback: PrayerLogsCallback) {
    CoroutineScope(Dispatchers.IO).launch {
        val logs = getTodayPrayerLogs()
        CoroutineScope(Dispatchers.Main).launch {
            callback.onResult(logs)
        }
    }
}
```

---

### 3. PrayersFragment - UI 更新逻辑

#### A. 添加字段
```java
private PrayerLogRepository prayerLogRepository;
private ImageView fajrStatusIcon;
private ImageView dhuhrStatusIcon;
private ImageView asrStatusIcon;
private ImageView maghribStatusIcon;
private ImageView ishaStatusIcon;
private Map<String, PrayerLog> todayPrayerLogs = new HashMap<>();
```

#### B. 初始化
```java
// Initialize repository
prayerLogRepository = new PrayerLogRepository();

// Make status icons clickable
fajrStatusIcon.setClickable(true);
fajrStatusIcon.setOnClickListener(v -> onSalahTrackClicked(SalahName.FAJR, fajrTrackButton));
// ... 其他祷告同理
```

#### C. 加载状态
```java
private void loadTodayPrayerLogs() {
    if (prayerLogRepository == null || FirebaseAuth.getInstance().getCurrentUser() == null) {
        resetAllPrayersToPending();
        return;
    }
    
    prayerLogRepository.getTodayPrayerLogsAsync(new PrayerLogRepository.PrayerLogsCallback() {
        @Override
        public void onResult(Map<String, PrayerLog> logs) {
            getActivity().runOnUiThread(() -> {
                todayPrayerLogs.clear();
                todayPrayerLogs.putAll(logs);
                
                // Update UI for each prayer
                updatePrayerStatusUI(SalahName.FAJR, logs.get("Fajr"));
                updatePrayerStatusUI(SalahName.DHUHR, logs.get("Dhuhr"));
                updatePrayerStatusUI(SalahName.ASR, logs.get("Asr"));
                updatePrayerStatusUI(SalahName.MAGHRIB, logs.get("Maghrib"));
                updatePrayerStatusUI(SalahName.ISHA, logs.get("Isha"));
            });
        }
    });
}
```

#### D. 更新 UI
```java
private void updatePrayerStatusUI(SalahName salahName, PrayerLog log) {
    MaterialButton button = getTrackButton(salahName);
    ImageView statusIcon = getStatusIcon(salahName);
    
    if (log == null) {
        // Pending: Show Track button
        button.setVisibility(View.VISIBLE);
        statusIcon.setVisibility(View.GONE);
    } else {
        // Has log: Hide button, show icon
        button.setVisibility(View.GONE);
        statusIcon.setVisibility(View.VISIBLE);
        
        // Set icon and color based on status
        PrayerLog.PrayerStatus status = log.getStatus();
        if (status == PrayerLog.PrayerStatus.ADA) {
            statusIcon.setImageResource(R.drawable.ic_correct);
            statusIcon.setColorFilter(0xFF4CAF50); // Green
        } else if (status == PrayerLog.PrayerStatus.QADA) {
            statusIcon.setImageResource(R.drawable.ic_warning);
            statusIcon.setColorFilter(0xFFFF9800); // Orange
        } else if (status == PrayerLog.PrayerStatus.MISSED) {
            statusIcon.setImageResource(R.drawable.ic_error);
            statusIcon.setColorFilter(0xFFF44336); // Red
        }
    }
}
```

#### E. 点击逻辑
```java
private void onSalahTrackClicked(SalahName salahName, MaterialButton button) {
    // Check login
    if (FirebaseAuth.getInstance().getCurrentUser() == null) {
        showLoginDialog(salahName, button);
        return;
    }
    
    String prayerName = salahName.getDisplayName();
    PrayerLog existingLog = todayPrayerLogs.get(prayerName);
    
    if (existingLog == null) {
        // Pending: Show new log dialog
        showPrayerLogBottomSheet(prayerName, null, null);
    } else {
        PrayerLog.PrayerStatus status = existingLog.getStatus();
        if (status == PrayerLog.PrayerStatus.ADA) {
            // Ada': Edit mode
            showPrayerLogBottomSheet(prayerName, existingLog.getId(), null);
        } else if (status == PrayerLog.PrayerStatus.QADA) {
            // Qada': Edit mode
            showPrayerLogBottomSheet(prayerName, existingLog.getId(), null);
        } else if (status == PrayerLog.PrayerStatus.MISSED) {
            // Missed: Create Qada' log
            showPrayerLogBottomSheet(prayerName, null, PrayerLog.PrayerStatus.QADA);
        }
    }
}
```

---

### 4. PrayerLogBottomSheet - 编辑模式

#### A. 支持编辑现有记录
```kotlin
private var existingLogId: String? = null
private var isEditMode: Boolean = false

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    prayerName = arguments?.getString(ARG_PRAYER_NAME) ?: "Unknown"
    existingLogId = arguments?.getString(ARG_EXISTING_LOG_ID)
    isEditMode = existingLogId != null
    
    // 支持初始状态（Missed -> Qada 转换）
    arguments?.getString(ARG_INITIAL_STATUS)?.let {
        selectedStatus = PrayerLog.PrayerStatus.valueOf(it)
    }
}
```

#### B. 加载现有数据
```kotlin
private fun loadExistingLog() {
    if (existingLogId == null) return
    
    firestore.collection(PrayerLog.COLLECTION_NAME)
        .document(existingLogId!!)
        .get()
        .addOnSuccessListener { document ->
            val log = document.toObject(PrayerLog::class.java)
            if (log != null) {
                selectedStatus = log.status
                selectStatus(log.status)
                
                if (log.performedAt != null) {
                    performedAtTimestamp = log.performedAt
                }
                
                binding.etNotes.setText(log.notes)
                updatePerformedAtDisplay()
                updateRecordedAtDisplay()
            }
        }
}
```

#### C. 保存逻辑
```kotlin
private fun savePrayerLog() {
    val collectionRef = firestore.collection(PrayerLog.COLLECTION_NAME)
    
    if (isEditMode && existingLogId != null) {
        // 更新现有文档
        collectionRef.document(existingLogId!!)
            .set(prayerLog.copy(id = existingLogId!!))
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "✅ $prayerName prayer updated successfully", Toast.LENGTH_SHORT).show()
                dismiss()
                (parentFragment as? OnPrayerLoggedListener)?.onPrayerLogged(prayerName)
            }
    } else {
        // 创建新文档
        collectionRef.add(prayerLog)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(requireContext(), "✅ $prayerName prayer logged successfully", Toast.LENGTH_SHORT).show()
                dismiss()
                (parentFragment as? OnPrayerLoggedListener)?.onPrayerLogged(prayerName)
            }
    }
}
```

#### D. 新的工厂方法
```kotlin
companion object {
    // 新建模式（支持初始状态）
    fun newInstance(prayerName: String, initialStatus: PrayerLog.PrayerStatus? = null): PrayerLogBottomSheet
    
    // 编辑模式
    fun newInstanceForEdit(prayerName: String, existingLogId: String): PrayerLogBottomSheet
}
```

---

## 🎨 UI 状态流程图

```
用户打开 Salat 页面
    ↓
Firebase 查询 prayer_logs 集合
    ↓
┌────────────────────────────────────┐
│  遍历 5 个祷告（Fajr - Isha）      │
└────────────────────────────────────┘
    ↓
每个祷告检查状态：
    ↓
┌─────────────────┬──────────────────┬──────────────────┬──────────────────┐
│ 无记录 (null)   │ Ada' 记录        │ Qada' 记录       │ Missed 记录      │
│ ↓              │ ↓               │ ↓               │ ↓               │
│ 显示 TRACK 按钮 │ 显示 ✅ 绿色图标 │ 显示 ⚠️ 橙色图标 │ 显示 ❌ 红色图标 │
└─────────────────┴──────────────────┴──────────────────┴──────────────────┘
```

---

## 🖱️ 点击交互流程

### Pending (未记录)
```
点击 TRACK 按钮
    ↓
检查登录状态
    ↓
已登录：打开 Log Prayer 弹窗（新建模式，默认 Ada'）
未登录：显示登录对话框
```

### Ada' (准时完成)
```
点击 ✅ 图标
    ↓
打开 Log Prayer 弹窗（编辑模式）
    ↓
加载现有数据：
  - Status: Ada' (可改为 Qada')
  - Time: 原始祷告时间
  - Notes: 原始备注
    ↓
用户修改后保存
    ↓
更新 Firestore 文档
    ↓
刷新 UI
```

### Qada' (已弥补)
```
点击 ⚠️ 图标
    ↓
打开 Log Prayer 弹窗（编辑模式）
    ↓
加载现有数据：
  - Status: Qada'
  - Time: 实际祷告时间
  - Notes: 原始备注
    ↓
用户可修改时间和备注
    ↓
保存更新
```

### Missed (错过)
```
点击 ❌ 图标
    ↓
打开 Log Prayer 弹窗（新建模式，默认 Qada'）
    ↓
Status 自动选中 Qada'
    ↓
用户填写时间和备注
    ↓
保存为新的 Qada' 记录
    ↓
UI 更新为 ⚠️ 图标
```

---

## 📁 修改的文件

### 新增文件
1. `app/src/main/java/com/quran/quranaudio/online/prayertimes/repository/PrayerLogRepository.kt`
   - 查询 prayer_logs 集合
   - Java 兼容的回调接口

2. `app/src/main/res/drawable/ic_warning.xml`
   - Qada' 状态的橙色警告图标

3. `app/src/main/res/drawable/ic_error.xml`
   - Missed 状态的红色错误图标

### 修改文件
4. `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/home/PrayersFragment.java`
   - 添加 `PrayerLogRepository` 集成
   - 实现 `loadTodayPrayerLogs()` 方法
   - 实现 `updatePrayerStatusUI()` 方法
   - 修改 `onSalahTrackClicked()` 点击逻辑
   - 状态图标可点击

5. `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/PrayerLogBottomSheet.kt`
   - 添加编辑模式支持
   - 添加初始状态参数（Missed -> Qada 转换）
   - 实现 `loadExistingLog()` 方法
   - 更新保存逻辑（区分新建/更新）
   - 新增 `newInstanceForEdit()` 工厂方法

---

## 🔄 数据流

### 保存祷告记录
```
用户在 Bottom Sheet 中保存
    ↓
写入 prayer_logs 集合
    ↓
回调 onPrayerLogged(prayerName)
    ↓
PrayersFragment 调用 loadTodayPrayerLogs()
    ↓
查询 Firestore 获取最新状态
    ↓
updatePrayerStatusUI() 更新每个祷告的显示
    ↓
UI 刷新显示正确的图标
```

### 查询策略
```
1. 每次保存后重新查询所有祷告记录
2. 按 loggedAt 降序排序，取最新的记录
3. 每个祷告名称只保留一条最新记录
4. 存储在 todayPrayerLogs Map 中供点击使用
```

---

## 🎨 UI 视觉规格

### 图标颜色
- **Ada' (✅)**: #4CAF50 (绿色) - 成功、准时
- **Qada' (⚠️)**: #FF9800 (橙色) - 警告、延迟完成
- **Missed (❌)**: #F44336 (红色) - 错误、未完成

### 图标尺寸
- **宽度**: 36dp
- **高度**: 36dp
- **缩放**: fitCenter
- **边距**: marginEnd="8dp"

### 按钮样式（Pending 状态）
- **背景色**: @color/salah_track_button
- **文字**: "Track"
- **文字颜色**: 白色
- **圆角**: 16dp
- **高度**: 32dp

---

## 🧪 测试场景

### 场景 1: 新用户首次使用
```
状态: 所有祷告都是 Pending
显示: 5 个 TRACK 按钮
点击: 打开新建模态框
```

### 场景 2: 用户完成 Fajr (Ada')
```
操作: 点击 Fajr TRACK → 保存 Ada' 状态
结果: Fajr 显示 ✅ 绿色图标
点击: 打开编辑模态框（可改为 Qada'）
```

### 场景 3: 用户延迟完成 Dhuhr (Qada')
```
操作: 点击 Dhuhr TRACK → 选择 Qada' → 保存
结果: Dhuhr 显示 ⚠️ 橙色图标
点击: 打开编辑模态框（可修改时间）
```

### 场景 4: 用户标记 Asr 为 Missed
```
操作: 点击 Asr TRACK → 选择 Missed → 保存
结果: Asr 显示 ❌ 红色图标
点击: 直接打开 Qada' 模态框（Status 预选为 Qada'）
```

### 场景 5: 用户从 Missed 转为 Qada'
```
状态: Asr 显示 ❌ 红色图标
操作: 点击 ❌ → 自动打开 Qada' 模态框 → 填写时间 → 保存
结果: Asr 从 ❌ 变为 ⚠️ 图标
```

### 场景 6: 用户修改 Ada' 为 Qada'
```
状态: Fajr 显示 ✅ 绿色图标
操作: 点击 ✅ → 打开编辑模态框 → 改为 Qada' → 保存
结果: Fajr 从 ✅ 变为 ⚠️ 图标
```

---

## 🔧 技术实现要点

### 1. Java-Kotlin 互操作
**挑战**: Java Fragment 调用 Kotlin suspend 函数

**解决方案**:
```kotlin
// Kotlin Repository 提供回调接口
interface PrayerLogsCallback {
    fun onResult(logs: Map<String, PrayerLog>)
}

fun getTodayPrayerLogsAsync(callback: PrayerLogsCallback) {
    CoroutineScope(Dispatchers.IO).launch {
        val logs = getTodayPrayerLogs()
        CoroutineScope(Dispatchers.Main).launch {
            callback.onResult(logs)
        }
    }
}
```

```java
// Java Fragment 使用匿名内部类实现回调
prayerLogRepository.getTodayPrayerLogsAsync(new PrayerLogRepository.PrayerLogsCallback() {
    @Override
    public void onResult(Map<String, PrayerLog> logs) {
        // 处理结果
    }
});
```

### 2. 状态图标复用
- 复用现有的 `completed_icon` ImageView
- 动态设置图标资源和颜色过滤器
- 节省布局修改工作

### 3. 编辑模式实现
- 新增 `existingLogId` 参数区分新建/编辑
- 编辑模式加载现有数据
- 保存时使用 `set()` 而非 `add()`

### 4. Missed -> Qada 转换
- 新增 `initialStatus` 参数
- Missed 状态点击时传入 `QADA` 作为初始状态
- 弹窗默认选中 Qada' 按钮

---

## 📊 数据库查询优化

### 查询策略
```
1. 单次查询所有今天的祷告记录
2. 按 loggedAt 降序排序
3. 客户端去重（每个祷告只取最新记录）
4. 缓存在 Map 中供点击使用
```

### Firestore 查询
```kotlin
firestore.collection("prayer_logs")
    .whereEqualTo("userId", userId)
    .whereEqualTo("date", "2025-11-05")
    .orderBy("loggedAt", Query.Direction.DESCENDING)
    .get()
```

### 索引需求
建议在 Firebase Console 创建复合索引：
```
Collection: prayer_logs
Fields:
  - userId: Ascending
  - date: Ascending
  - loggedAt: Descending
```

---

## ⚠️ 重要提醒

### 1. Firestore 规则必须部署
**错误**: `PERMISSION_DENIED: Missing or insufficient permissions`

**解决**: 在 Firebase Console 部署 `firestore_rules_to_deploy.txt`

**步骤**:
1. https://console.firebase.google.com/
2. 选择项目: quran-majeed-aa3d2
3. Firestore Database → Rules
4. 复制 `firestore_rules_to_deploy.txt` 内容
5. 粘贴并 Publish

### 2. 首次使用需要索引
首次查询时可能会收到索引缺失错误，Firebase 会在日志中提供创建索引的链接，点击即可自动创建。

---

## 📱 用户体验改进

### Before（之前）
```
✅ 所有祷告只有两种状态：
   - Pending: TRACK 按钮
   - Completed: ✅ 图标
   
❌ 无法区分 Ada' 和 Qada'
❌ 无法标记 Missed
❌ 点击 ✅ 无反应
```

### After（现在）
```
✅ 四种状态清晰区分：
   - Pending: TRACK 按钮
   - Ada': ✅ 绿色（准时）
   - Qada': ⚠️ 橙色（延迟）
   - Missed: ❌ 红色（错过）
   
✅ 所有状态都可点击
✅ Ada'/Qada' 可编辑
✅ Missed 可转为 Qada'
✅ 图标颜色直观表达状态
```

---

## 🔍 调试日志

### 加载状态
```
D/PrayersFragment: 📝 Pending state - showing new log dialog
D/PrayersFragment: ✅ Ada' state - showing edit dialog
D/PrayersFragment: ⚠️ Qada' state - showing edit dialog
D/PrayersFragment: ❌ Missed state - showing Qada' log dialog
```

### UI 更新
```
D/PrayersFragment: 📝 Fajr: Pending (Track button)
D/PrayersFragment: ✅ Dhuhr: Ada' (green check)
D/PrayersFragment: ⚠️ Asr: Qada' (orange warning)
D/PrayersFragment: ❌ Maghrib: Missed (red error)
```

---

## 📊 构建结果

```bash
BUILD SUCCESSFUL in 3m
129 actionable tasks: 10 executed, 119 up-to-date
Installing APK on 'Pixel 7 - 16'
✅ Installed on 1 device
```

---

## ✅ 实现检查清单

- [x] ✅ 创建 PrayerLogRepository
- [x] ✅ 创建 ic_warning.xml 图标
- [x] ✅ 创建 ic_error.xml 图标
- [x] ✅ PrayersFragment 查询状态
- [x] ✅ 实现 updatePrayerStatusUI()
- [x] ✅ 修改点击逻辑
- [x] ✅ 状态图标可点击
- [x] ✅ PrayerLogBottomSheet 编辑模式
- [x] ✅ 支持初始状态参数
- [x] ✅ 加载现有数据
- [x] ✅ 保存/更新逻辑
- [x] ✅ 编译成功
- [x] ✅ 安装到设备

---

## 🎯 下一步测试

### 测试步骤

1. **部署 Firestore 规则**（必须先完成）
   - 访问 Firebase Console
   - 部署 `firestore_rules_to_deploy.txt`

2. **测试 Pending → Ada'**
   - 点击 Fajr TRACK 按钮
   - 保持 Ada' 状态
   - 保存
   - 验证显示 ✅ 绿色图标

3. **测试 Ada' 编辑**
   - 点击 Fajr ✅ 图标
   - 弹窗应显示现有数据
   - 修改为 Qada'
   - 保存
   - 验证显示 ⚠️ 橙色图标

4. **测试 Qada' 创建**
   - 点击 Dhuhr TRACK 按钮
   - 选择 Qada' 状态
   - 填写时间
   - 保存
   - 验证显示 ⚠️ 橙色图标

5. **测试 Missed → Qada' 转换**
   - 点击 Asr TRACK 按钮
   - 选择 Missed 状态
   - 保存
   - 验证显示 ❌ 红色图标
   - 再次点击 ❌ 图标
   - 弹窗应自动选中 Qada'
   - 保存
   - 验证显示 ⚠️ 橙色图标

6. **测试未登录状态**
   - 退出登录
   - 验证所有祷告显示 TRACK 按钮
   - 点击 TRACK
   - 验证显示登录对话框

---

## 📚 相关文档

1. `PRAYER_LOG_FIXES_SUMMARY.md` - 之前修复的问题
2. `FIRESTORE_RULES_QUICK_FIX.md` - Firestore 规则部署指南
3. `PRAYER_LOG_FEATURE_IMPLEMENTATION.md` - 原始功能实现
4. `RELEASE_NOTES_v1.7.3.md` - 版本发布说明

---

**状态 UI 优化已完成并安装到设备！** 🚀

**记得先部署 Firestore 规则，然后测试所有状态的显示和交互。**


