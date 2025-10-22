# Quran Listening 任务实施计划

## 概述

将原有的 "Recitation/Tajweed Practice" 改造为独立的 "Quran Listening" 任务，支持：
- 自动导航到上次阅读位置
- 自动播放音频
- 计时/节数追踪
- 与 Reading Goal 联动但可独立配置

---

## 需求分析

### 需求 1: Quran Listening 任务按钮跳转
**当前状态**: Task 2 (Tajweed Practice) 跳转到 TajweedTimerActivity  
**目标状态**: Task 2 (Quran Listening) 跳转到 Quran Reader 并自动播放

**实施步骤**:
1. 修改 `DailyQuestsManager.java` 中的 Task 2 点击逻辑
2. 从 `UserLearningState` 获取 `lastReadSurah` 和 `lastReadAyah`
3. 使用 `ReaderFactory.startReadingMode()` 并传递 Surah/Ayah 参数
4. 传递额外参数 `autoPlayAudio=true` 和 `trackListeningTime=true`

### 需求 2: 自动定位到上次阅读位置
**实施步骤**:
1. 在 `QuestRepository.kt` 中添加 `observeUserLearningState()` 方法
2. 在 `DailyQuestsManager.java` 中订阅学习状态
3. 启动 Reader 时传递 Surah/Ayah 参数

### 需求 3: 音频自动播放并计时
**实施步骤**:
1. Quran Reader 接收 `autoPlayAudio` 参数
2. 在 `onViewCreated` 后自动启动音频播放
3. 启动计时器追踪收听时长
4. 收听完成后更新 `task2ListeningCompleted` 标志

### 需求 4: 独立配置 Reading 和 Listening
**当前状态**: `recitationMinutes` 字段用于 Tajweed Practice  
**目标状态**: 改为 `listeningGoal` 和 `listeningGoalUnit`

**数据模型变更**:
```kotlin
data class UserQuestConfig(
    // Reading Goal
    dailyReadingPages: Int = 10,
    readingGoalUnit: String = "PAGES",
    dailyReadingGoal: Int = 10,
    
    // Listening Goal (独立配置)
    listeningEnabled: Boolean = true,
    listeningGoalUnit: String = "VERSES", // VERSES or MINUTES
    listeningGoal: Int = 10, // 默认 10 verses 或 15 minutes
    
    // Legacy fields (向后兼容)
    recitationEnabled: Boolean = true,
    recitationMinutes: Int = 15,
    
    // ... 其他字段
)
```

### 需求 5: 默认联动 Reading 和 Listening
**实施步骤**:
1. 在 `LearningPlanSetupFragment` 中添加联动逻辑
2. 当用户首次设置时，`listeningGoal` 默认等于 `dailyReadingGoal`
3. 提供独立开关允许用户解除联动

---

## 实施阶段

### ✅ 阶段 1: 数据模型扩展 (优先级: 高)

#### 1.1 扩展 UserQuestConfig
**文件**: `QuestModels.kt`

```kotlin
data class UserQuestConfig(
    // Reading Goal
    @PropertyName("dailyReadingPages")
    val dailyReadingPages: Int = 10,
    
    @PropertyName("readingGoalUnit")
    val readingGoalUnit: String = "PAGES",
    
    @PropertyName("dailyReadingGoal")
    val dailyReadingGoal: Int = 10,
    
    // Listening Goal (新增)
    @PropertyName("listeningEnabled")
    val listeningEnabled: Boolean = true,
    
    @PropertyName("listeningGoalUnit")
    val listeningGoalUnit: String = "VERSES", // "VERSES" or "MINUTES"
    
    @PropertyName("listeningGoal")
    val listeningGoal: Int = 10,
    
    // Legacy fields
    @PropertyName("recitationEnabled")
    val recitationEnabled: Boolean = true,
    
    @PropertyName("recitationMinutes")
    val recitationMinutes: Int = 15,
    
    // ... 其他字段保持不变
)
```

#### 1.2 扩展 DailyProgressModel
**文件**: `QuestModels.kt`

```kotlin
data class DailyProgressModel(
    @PropertyName("task1ReadCompleted")
    val task1ReadCompleted: Boolean = false,
    
    @PropertyName("task2ListeningCompleted") // 重命名
    val task2ListeningCompleted: Boolean = false,
    
    @PropertyName("task2ListeningMinutes") // 新增
    val task2ListeningMinutes: Int = 0,
    
    @PropertyName("task2ListeningVerses") // 新增
    val task2ListeningVerses: Int = 0,
    
    // ... 其他字段
)
```

#### 1.3 Repository 方法扩展
**文件**: `QuestRepository.kt`

```kotlin
/**
 * 观察用户学习状态（上次阅读位置）
 */
fun observeUserLearningState(): Flow<UserLearningState?> {
    val userId = getUserId()
    if (userId == null) {
        return flowOf(null)
    }
    
    return firestore
        .collection("users")
        .document(userId)
        .collection(UserLearningState.COLLECTION_PATH)
        .document(UserLearningState.DOCUMENT_ID)
        .asFlow<UserLearningState>()
}

/**
 * 初始化学习状态（首次使用）
 */
suspend fun initializeLearningState() {
    val userId = getUserId() ?: return
    
    val state = UserLearningState(
        currentSurah = 1,
        currentAyah = 1,
        currentPage = 1,
        currentJuz = 1,
        totalVersesRead = 0,
        totalPagesRead = 0,
        lastReadAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )
    
    firestore
        .collection("users")
        .document(userId)
        .collection(UserLearningState.COLLECTION_PATH)
        .document(UserLearningState.DOCUMENT_ID)
        .set(state)
        .await()
}

/**
 * 更新收听进度
 */
suspend fun updateListeningProgress(
    versesListened: Int,
    minutesListened: Int
) {
    val userId = getUserId() ?: return
    val today = LocalDate.now().toString()
    
    val updates = hashMapOf(
        "task2ListeningMinutes" to FieldValue.increment(minutesListened.toLong()),
        "task2ListeningVerses" to FieldValue.increment(versesListened.toLong()),
        "updatedAt" to Timestamp.now()
    )
    
    firestore
        .collection("users")
        .document(userId)
        .collection("dailyProgress")
        .document(today)
        .update(updates)
        .await()
}
```

---

### ✅ 阶段 2: UI 层改造

#### 2.1 修改 Learning Plan Setup 页面
**文件**: `fragment_learning_plan_setup.xml`

```xml
<!-- Listening Goal Section (替换原 Recitation) -->
<TextView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:text="Quran Listening Goal"
    android:textColor="@android:color/black"
    android:textSize="18sp"
    android:textStyle="bold"
    android:layout_marginBottom="16dp" />

<!-- Enable Listening Switch -->
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:layout_marginBottom="12dp">

    <TextView
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="Enable Listening"
        android:textColor="@android:color/black"
        android:textSize="16sp" />

    <com.google.android.material.switchmaterial.SwitchMaterial
        android:id="@+id/sw_listening_enabled"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:checked="true"
        app:thumbTint="@android:color/white"
        app:trackTint="@color/switch_track_color" />

</LinearLayout>

<!-- Listening Goal Unit Selector -->
<RadioGroup
    android:id="@+id/rg_listening_unit"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:layout_marginBottom="12dp">

    <RadioButton
        android:id="@+id/rb_listening_verses"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="Verses"
        android:checked="true"
        android:buttonTint="#4B9B76" />

    <RadioButton
        android:id="@+id/rb_listening_minutes"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:text="Minutes"
        android:buttonTint="#4B9B76" />

</RadioGroup>

<!-- Listening Goal Value -->
<Spinner
    android:id="@+id/sp_listening_goal"
    android:layout_width="match_parent"
    android:layout_height="48dp"
    android:background="@drawable/spinner_background"
    android:padding="12dp"
    android:layout_marginBottom="24dp" />
```

#### 2.2 Fragment 逻辑更新
**文件**: `LearningPlanSetupFragment.kt`

```kotlin
// 添加监听器
binding.swListeningEnabled.setOnCheckedChangeListener { _, isChecked ->
    binding.rgListeningUnit.isEnabled = isChecked
    binding.spListeningGoal.isEnabled = isChecked
}

binding.rgListeningUnit.setOnCheckedChangeListener { _, checkedId ->
    val unit = when (checkedId) {
        R.id.rb_listening_verses -> "VERSES"
        R.id.rb_listening_minutes -> "MINUTES"
        else -> "VERSES"
    }
    updateListeningGoalOptions(unit)
}

private fun updateListeningGoalOptions(unit: String) {
    val options = if (unit == "VERSES") {
        arrayOf("5 verses", "10 verses", "15 verses", "20 verses", "30 verses")
    } else {
        arrayOf("5 minutes", "10 minutes", "15 minutes", "20 minutes", "30 minutes")
    }
    
    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    binding.spListeningGoal.adapter = adapter
}
```

---

### ✅ 阶段 3: DailyQuestsManager 更新

#### 3.1 修改 Task 2 点击逻辑
**文件**: `DailyQuestsManager.java`

```java
private void setupQuestsCardClickListeners(UserQuestConfig config) {
    // Task 2: Quran Listening (跳转到 Reader 并自动播放)
    if (btnTask2Go != null) {
        Log.d(TAG, "Setting up click listener for Task 2 Go button");
        btnTask2Go.setOnClickListener(v -> {
            try {
                Log.d(TAG, "Task 2 (Listening) Go button clicked!");
                
                // 1. 获取用户学习状态
                viewModel.getUserLearningState().observe(lifecycleOwner, state -> {
                    int surah = (state != null) ? state.getCurrentSurah() : 1;
                    int ayah = (state != null) ? state.getCurrentAyah() : 1;
                    
                    Log.d(TAG, "Starting Quran Listening from Surah " + surah + ", Ayah " + ayah);
                    
                    // 2. 启动 Quran Reader（自动播放模式）
                    Context context = fragment.requireContext();
                    ReaderFactory.startListeningMode(
                        context, 
                        surah, 
                        ayah,
                        config.getListeningGoal(),
                        config.getListeningGoalUnit()
                    );
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to start Quran Listening", e);
                Toast.makeText(fragment.requireContext(), 
                    "Failed to start listening: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
}
```

#### 3.2 更新任务描述生成
**文件**: `DailyQuestsManager.java`

```java
private void updateTodayQuestsCard(UserQuestConfig config, DailyProgressModel progress) {
    // Task 2: Quran Listening
    if (tvTask2Description != null) {
        String unit = config.getListeningGoalUnit();
        int goal = config.getListeningGoal();
        
        String description = unit.equals("VERSES") 
            ? "Listen to " + goal + " verses"
            : "Listen for " + goal + " minutes";
            
        tvTask2Description.setText(description);
        Log.d(TAG, "Task 2 description updated: " + description);
    }
    
    // 更新完成状态
    if (btnTask2Go != null) {
        boolean isCompleted = progress.getTask2ListeningCompleted();
        btnTask2Go.setEnabled(!isCompleted);
        btnTask2Go.setText(isCompleted ? "Done ✓" : "Go");
    }
}
```

---

### ✅ 阶段 4: ReaderFactory 扩展

#### 4.1 添加 Listening 模式启动方法
**文件**: `ReaderFactory.java`

```java
/**
 * 启动 Quran Listening 模式
 * 
 * @param context Context
 * @param surah 起始 Surah
 * @param ayah 起始 Ayah
 * @param goal 收听目标值（节数或分钟数）
 * @param goalUnit 目标单位（"VERSES" 或 "MINUTES"）
 */
public static void startListeningMode(
    Context context,
    int surah,
    int ayah,
    int goal,
    String goalUnit
) {
    Intent intent = new Intent(context, QuranReaderActivity.class);
    intent.putExtra("START_SURAH", surah);
    intent.putExtra("START_AYAH", ayah);
    intent.putExtra("AUTO_PLAY_AUDIO", true);
    intent.putExtra("LISTENING_MODE", true);
    intent.putExtra("LISTENING_GOAL", goal);
    intent.putExtra("LISTENING_GOAL_UNIT", goalUnit);
    context.startActivity(intent);
}
```

---

## 数据流示意图

```
用户点击 "Quran Listening" Go 按钮
         ↓
DailyQuestsManager 获取 UserLearningState
         ↓
读取 lastReadSurah/Ayah (例如: Surah 2, Ayah 100)
         ↓
调用 ReaderFactory.startListeningMode(context, 2, 100, 10, "VERSES")
         ↓
Quran Reader 启动并定位到 Surah 2, Ayah 100
         ↓
自动播放音频
         ↓
计时器开始追踪（10 verses 或 15 minutes）
         ↓
完成后更新 DailyProgressModel.task2ListeningCompleted = true
         ↓
更新 UserLearningState.lastReadSurah/Ayah (新位置)
```

---

## 向后兼容策略

### 旧配置迁移
```kotlin
// 如果用户有旧配置 (recitationMinutes)，自动迁移
val config = userConfig?.let {
    if (it.listeningGoal == 0 && it.recitationMinutes > 0) {
        // 迁移旧数据
        it.copy(
            listeningEnabled = it.recitationEnabled,
            listeningGoalUnit = "MINUTES",
            listeningGoal = it.recitationMinutes
        )
    } else {
        it
    }
}
```

---

## 测试验证清单

### 功能测试
- [ ] 点击 "Quran Listening" Go 按钮能正确跳转到 Reader
- [ ] Reader 能定位到上次阅读位置
- [ ] 音频能自动播放
- [ ] 收听完成后任务标记为完成
- [ ] UserLearningState 正确更新

### 配置测试
- [ ] Listening Goal 可以独立于 Reading Goal 设置
- [ ] Verses 单位正确计算完成进度
- [ ] Minutes 单位正确计算完成进度
- [ ] 配置保存和回显正确

### 兼容性测试
- [ ] 旧用户配置能正确迁移
- [ ] 新用户默认值正确
- [ ] Firebase 数据结构兼容

---

## 实施优先级

1. **P0 (立即实施)**: 数据模型扩展
2. **P0 (立即实施)**: DailyQuestsManager Task 2 跳转逻辑
3. **P1 (本周)**: Learning Plan Setup UI 改造
4. **P1 (本周)**: ReaderFactory Listening 模式
5. **P2 (下周)**: Reader 音频自动播放和计时
6. **P2 (下周)**: 完成状态追踪和进度更新

---

## 当前状态

**已完成**: 
- ✅ Learning Plan V2 (多单位支持)
- ✅ 数据模型基础（UserLearningState, UserQuestConfig）

**进行中**:
- ⏳ Quran Listening 任务优化

**待实施**:
- ⏳ 数据模型扩展（listeningGoal 字段）
- ⏳ UI 改造
- ⏳ 跳转逻辑
- ⏳ 音频自动播放

