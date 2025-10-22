# Learning Plan Setup V2 升级实施计划

## 概述

升级 Learning Plan Setup 以支持多种学习单位（Pages/Verses/Juz'）和完整的进度追踪系统。

---

## ✅ 阶段 1: 数据模型层 (已完成)

### 1.1 枚举定义 ✅
**文件**: `ReadingGoalUnit.kt`
- ✅ 定义 `PAGES`, `VERSES`, `JUZ` 三种学习单位
- ✅ 提供 `displayName` 和 `fromString()` 方法

### 1.2 用户学习状态模型 ✅
**文件**: `UserLearningState.kt`
- ✅ 追踪当前 Surah/Ayah/Page/Juz 位置
- ✅ 记录总阅读进度（总页数、总节数）
- ✅ 记录最后阅读时间
- ✅ Firestore 路径: `users/{userId}/learning_state/current`

### 1.3 配置模型扩展 ✅
**文件**: `QuestModels.kt` (`UserQuestConfig`)
- ✅ 添加 `readingGoalUnit: String` 字段
- ✅ 添加 `dailyReadingGoal: Int` 字段
- ✅ 保留 `dailyReadingPages` 以保持向后兼容
- ✅ 添加 `getReadingUnitEnum()` 方法

### 1.4 工具类创建 ✅
**文件**: `QuranDataHelper.kt`
- ✅ 常量定义: `TOTAL_PAGES = 604`, `TOTAL_VERSES = 6236`, `TOTAL_JUZ = 30`
- ✅ `getRangeForUnit()` - 获取每种单位的范围配置
  - Pages: 1-50, 默认 10
  - Verses: 1-100, 默认 10
  - Juz': 1-30, 默认 1
- ✅ `calculateChallengeDays()` - 根据单位和目标值计算挑战天数
  - Pages: ⌈604 ÷ 目标⌉
  - Verses: ⌈6236 ÷ 目标⌉
  - Juz': ⌈30 ÷ 目标⌉
- ✅ `convertUnit()` - 单位转换（用于UI切换时同步）
- ✅ `getReadingDescription()` - 生成任务描述文本

---

## 🔄 阶段 2: UI 层改造 (待实施)

### 2.1 布局文件修改
**文件**: `fragment_learning_plan_setup.xml`

#### 需要添加的组件:

```xml
<!-- 单位选择器 (RadioGroup) -->
<RadioGroup
    android:id="@+id/rg_reading_unit"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal">
    
    <RadioButton
        android:id="@+id/rb_unit_pages"
        android:text="Pages"
        android:checked="true" />
    
    <RadioButton
        android:id="@+id/rb_unit_verses"
        android:text="Verses" />
    
    <RadioButton
        android:id="@+id/rb_unit_juz"
        android:text="Juz'" />
</RadioGroup>

<!-- Reading Goal Slider (动态范围) -->
<Slider
    android:id="@+id/sb_reading_goal"
    android:valueFrom="1"
    android:valueTo="50"
    android:stepSize="1"
    android:value="10" />

<!-- Reading Goal Value Display -->
<TextView
    android:id="@+id/tv_reading_goal_value"
    android:text="10 pages" />
```

#### 需要修改的组件:
- ✅ 将 `sb_reading_goal` 替换原 `sbReadingGoal`
- ✅ 添加单位选择器到 Daily Reading Goal 区域上方
- ✅ 修改 `tv_reading_goal_value` 显示格式（"10 pages" / "20 verses" / "1 juz'"）

### 2.2 隐藏底部导航栏

**文件**: `LearningPlanSetupFragment.kt`

```kotlin
override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    
    // Hide bottom navigation bar
    (activity as? MainActivity)?.hideBottomNavigation()
}

override fun onDestroyView() {
    super.onDestroyView()
    
    // Show bottom navigation bar when leaving
    (activity as? MainActivity)?.showBottomNavigation()
}
```

---

## 🧠 阶段 3: ViewModel 逻辑更新 (待实施)

### 3.1 LearningPlanSetupViewModel 修改
**文件**: `LearningPlanSetupViewModel.kt`

#### 需要添加的字段:

```kotlin
// Current selected reading unit
private val _selectedUnit = MutableLiveData<ReadingGoalUnit>(ReadingGoalUnit.PAGES)
val selectedUnit: LiveData<ReadingGoalUnit> = _selectedUnit

// Dynamic slider range
private val _sliderRange = MutableLiveData<QuranDataHelper.UnitRange>()
val sliderRange: LiveData<QuranDataHelper.UnitRange> = _sliderRange
```

#### 需要修改的方法:

```kotlin
/**
 * 修改计算逻辑以支持多种单位
 */
fun calculateChallengeDays(
    unit: ReadingGoalUnit,
    dailyGoal: Int,
    recitationMinutes: Int,
    recitationEnabled: Boolean
): Int {
    // 使用 QuranDataHelper 进行计算
    val days = QuranDataHelper.calculateChallengeDays(unit, dailyGoal)
    _challengeDays.value = days
    
    Log.d(TAG, "Challenge days calculated: $days (${unit.displayName}: $dailyGoal per day)")
    return days
}

/**
 * 切换学习单位
 */
fun setReadingUnit(unit: ReadingGoalUnit) {
    _selectedUnit.value = unit
    
    // Update slider range
    val range = QuranDataHelper.getRangeForUnit(unit)
    _sliderRange.value = range
    
    Log.d(TAG, "Reading unit changed to: ${unit.displayName}, range: ${range.min}-${range.max}")
}
```

### 3.2 保存配置时包含新字段

```kotlin
fun saveUserQuest(config: UserQuestConfig) {
    viewModelScope.launch {
        try {
            _isLoading.postValue(true)
            
            // 包含 readingGoalUnit 和 dailyReadingGoal
            val configToSave = config.copy(
                readingGoalUnit = _selectedUnit.value?.name ?: "PAGES",
                dailyReadingGoal = currentGoalValue, // 从 UI 获取
                updatedAt = Timestamp.now()
            )
            
            withTimeout(15000L) {
                questRepository.saveUserQuestConfig(configToSave)
                Log.d(TAG, "Quest config saved with unit: ${configToSave.readingGoalUnit}")
                
                questRepository.initializeStreakStats()
                Log.d(TAG, "Streak stats initialized")
            }
            
            _saveStatus.postValue(SaveStatus.Success)
            _isLoading.postValue(false)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save quest config", e)
            _saveStatus.postValue(SaveStatus.Error(e.message ?: "Failed to save"))
            _isLoading.postValue(false)
        }
    }
}
```

---

## 🔗 阶段 4: Fragment 交互逻辑 (待实施)

### 4.1 LearningPlanSetupFragment 修改
**文件**: `LearningPlanSetupFragment.kt`

#### 需要添加的监听器:

```kotlin
private fun setupUI() {
    // ... 现有代码 ...
    
    // ===== 新增：单位选择器监听 =====
    binding.rgReadingUnit.setOnCheckedChangeListener { _, checkedId ->
        val selectedUnit = when (checkedId) {
            R.id.rb_unit_pages -> ReadingGoalUnit.PAGES
            R.id.rb_unit_verses -> ReadingGoalUnit.VERSES
            R.id.rb_unit_juz -> ReadingGoalUnit.JUZ
            else -> ReadingGoalUnit.PAGES
        }
        
        // 通知 ViewModel 更新单位
        viewModel.setReadingUnit(selectedUnit)
    }
    
    // ===== 观察 Slider 范围变化 =====
    viewModel.sliderRange.observe(viewLifecycleOwner) { range ->
        // 动态更新 Slider 范围
        binding.sbReadingGoal.valueFrom = range.min.toFloat()
        binding.sbReadingGoal.valueTo = range.max.toFloat()
        binding.sbReadingGoal.value = range.defaultValue.toFloat()
        
        // 更新显示文本
        updateReadingGoalDisplay(range.defaultValue, viewModel.selectedUnit.value)
    }
    
    // ===== Slider 监听器（更新显示） =====
    binding.sbReadingGoal.addOnChangeListener { _, value, fromUser ->
        if (fromUser) {
            val unit = viewModel.selectedUnit.value ?: ReadingGoalUnit.PAGES
            updateReadingGoalDisplay(value.toInt(), unit)
            updateChallengeDays()
        }
    }
}

private fun updateReadingGoalDisplay(value: Int, unit: ReadingGoalUnit?) {
    val unitObj = unit ?: ReadingGoalUnit.PAGES
    val range = QuranDataHelper.getRangeForUnit(unitObj)
    binding.tvReadingGoalValue.text = "$value ${range.displaySuffix}"
}
```

#### 配置回显逻辑更新:

```kotlin
private fun observeViewModel() {
    lifecycleScope.launch {
        viewModel.userConfig.collectLatest { config ->
            config?.let {
                Log.d(TAG, "加载已保存的配置: 单位=${it.readingGoalUnit}, 目标=${it.dailyReadingGoal}")
                
                // 1. 设置单位选择器
                val unit = it.getReadingUnitEnum()
                when (unit) {
                    ReadingGoalUnit.PAGES -> binding.rbUnitPages.isChecked = true
                    ReadingGoalUnit.VERSES -> binding.rbUnitVerses.isChecked = true
                    ReadingGoalUnit.JUZ -> binding.rbUnitJuz.isChecked = true
                }
                
                // 2. 通知 ViewModel 更新单位（触发范围更新）
                viewModel.setReadingUnit(unit)
                
                // 3. 设置 Slider 值
                binding.sbReadingGoal.value = it.dailyReadingGoal.toFloat()
                
                // 4. 更新显示
                updateReadingGoalDisplay(it.dailyReadingGoal, unit)
                
                // 5. 回显其他配置（Recitation, Tasbih 等）
                // ... (保持现有逻辑)
                
                Log.d(TAG, "配置回显完成")
            }
        }
    }
    
    // ... 其他观察者
}
```

---

## 📱 阶段 5: 主页任务描述更新 (待实施)

### 5.1 DailyQuestsManager 修改
**文件**: `DailyQuestsManager.java`

#### 修改任务描述生成逻辑:

```java
private void updateTodayQuestsCard(UserQuestConfig config, DailyProgressModel progress) {
    if (tvTask1Description != null) {
        // 使用 QuranDataHelper 生成描述
        ReadingGoalUnit unit = ReadingGoalUnit.fromString(config.getReadingGoalUnit());
        String description = QuranDataHelper.INSTANCE.getReadingDescription(
            unit,
            config.getDailyReadingGoal()
        );
        tvTask1Description.setText(description);
        
        Log.d(TAG, "Task 1 description updated: " + description);
    }
    
    // Task 2 和 Task 3 逻辑保持不变
    // ...
}
```

---

## 💾 阶段 6: 进度追踪系统 (待实施)

### 6.1 QuestRepository 扩展
**文件**: `QuestRepository.kt`

#### 添加学习状态管理方法:

```kotlin
/**
 * Get user's current learning state
 */
fun observeUserLearningState(): Flow<UserLearningState?> {
    val userId = getUserId()
    if (userId == null) {
        Log.w(TAG, "Cannot observe learning state: user not logged in")
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
 * Initialize learning state for new users
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
 * Update learning state after reading session
 */
suspend fun updateLearningState(
    newSurah: Int,
    newAyah: Int,
    newPage: Int,
    newJuz: Int,
    versesRead: Int,
    pagesRead: Int
) {
    val userId = getUserId() ?: return
    
    val updates = hashMapOf(
        "current_surah" to newSurah,
        "current_ayah" to newAyah,
        "current_page" to newPage,
        "current_juz" to newJuz,
        "total_verses_read" to FieldValue.increment(versesRead.toLong()),
        "total_pages_read" to FieldValue.increment(pagesRead.toLong()),
        "last_read_at" to Timestamp.now(),
        "updated_at" to Timestamp.now()
    )
    
    firestore
        .collection("users")
        .document(userId)
        .collection(UserLearningState.COLLECTION_PATH)
        .document(UserLearningState.DOCUMENT_ID)
        .update(updates)
        .await()
}
```

### 6.2 阅读页面集成 (未来工作)

当用户完成阅读任务时，调用 `updateLearningState()` 更新进度。

---

## 🎨 阶段 7: UI/UX 优化 (待实施)

### 7.1 隐藏底部导航栏

**文件**: `MainActivity.kt` (或主 Activity)

```kotlin
fun hideBottomNavigation() {
    findViewById<BottomNavigationView>(R.id.bottom_nav)?.visibility = View.GONE
}

fun showBottomNavigation() {
    findViewById<BottomNavigationView>(R.id.bottom_nav)?.visibility = View.VISIBLE
}
```

### 7.2 样式统一

确保三种单位的 UI 样式一致：
- RadioButton 样式统一
- Slider 样式统一
- 文本显示格式统一

---

## 📊 验证清单

### 数据层验证 ✅
- ✅ `ReadingGoalUnit` 枚举可以正确创建和转换
- ✅ `UserQuestConfig` 可以保存和读取新字段
- ✅ `UserLearningState` 模型结构正确
- ✅ `QuranDataHelper` 计算逻辑准确

### UI 层验证 ⏳
- ⏳ 单位切换时 Slider 范围正确更新
- ⏳ Slider 值变化时显示文本正确更新
- ⏳ 挑战天数实时计算正确
- ⏳ 配置回显正确（包括单位和目标值）
- ⏳ 底部导航栏正确隐藏/显示

### 业务逻辑验证 ⏳
- ⏳ 配置保存包含所有新字段
- ⏳ 主页任务描述根据单位正确显示
- ⏳ 向后兼容：旧配置仍可正常工作

### 进度追踪验证 ⏳
- ⏳ 学习状态可以正确初始化
- ⏳ 学习状态可以正确更新
- ⏳ 学习状态可以正确读取

---

## 📝 实施顺序建议

1. **先实施 UI 层** (阶段 2)
   - 添加单位选择器
   - 修改布局
   - 隐藏底部导航栏

2. **然后实施 ViewModel** (阶段 3)
   - 更新计算逻辑
   - 添加单位切换逻辑
   - 修改保存逻辑

3. **接着实施 Fragment** (阶段 4)
   - 添加监听器
   - 更新配置回显
   - 动态范围调整

4. **更新主页显示** (阶段 5)
   - 修改任务描述生成

5. **最后实施进度追踪** (阶段 6)
   - 可以作为独立功能后续添加

---

## 🔥 当前状态总结

### ✅ 已完成:
1. 数据模型层完全实现
2. 工具类完全实现
3. 挑战天数计算逻辑已修正

### 🔄 进行中:
- 准备开始 UI 层改造

### ⏳ 待实施:
1. UI 布局修改
2. ViewModel 逻辑更新
3. Fragment 交互实现
4. 主页集成
5. 进度追踪系统

---

## 预计工作量

- **阶段 2 (UI)**:Human: 继续
