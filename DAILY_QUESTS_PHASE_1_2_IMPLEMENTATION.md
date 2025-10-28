# Daily Quests Feature - Phase 1 & 2 Implementation Report

## ✅ 指令 1：Firebase 数据模型与 QuestRepository 搭建 - 已完成

### 创建的文件

#### 1. FirestoreConstants.kt
**路径**: `app/src/main/java/com/quran/quranaudio/online/quests/constants/FirestoreConstants.kt`

**功能**:
- 定义 Firestore 路径常量
- 自动获取当前登录用户的 UID
- 提供路径生成函数：
  - `getLearningPlanConfigPath()` → `users/{userId}/learningPlan/config`
  - `getDailyProgressCollectionPath()` → `users/{userId}/dailyProgress`
  - `getDailyProgressDocumentPath(date)` → `users/{userId}/dailyProgress/{YYYY-MM-DD}`
  - `getStreakStatsPath()` → `users/{userId}/streakStats/summary`
- 提供日期格式化函数 `getDateId()`
- 定义任务 ID 常量 (TaskIds)

#### 2. QuestModels.kt
**路径**: `app/src/main/java/com/quran/quranaudio/online/quests/data/QuestModels.kt`

**数据模型**:

```kotlin
// 学习计划配置
data class UserQuestConfig(
    dailyReadingPages: Int,
    recitationEnabled: Boolean,
    recitationMinutes: Int,
    duaReminderEnabled: Boolean,
    tasbihReminderEnabled: Boolean,
    tasbihCount: Int,
    totalChallengeDays: Int,
    startDate: String,
    createdAt: Timestamp,
    updatedAt: Timestamp
)

// 每日进度
data class DailyProgressModel(
    task1ReadCompleted: Boolean,
    task2TajweedCompleted: Boolean,
    task3TasbihCompleted: Boolean,
    allTasksCompleted: Boolean,
    completedAt: Timestamp?,
    date: String
) {
    // 检查所有已启用任务是否完成
    fun areAllEnabledTasksCompleted(config: UserQuestConfig): Boolean
}

// 连续统计
data class StreakStats(
    currentStreak: Int,
    longestStreak: Int,
    totalDays: Int,
    lastCompletedDate: String,
    monthlyGoal: Int,
    monthlyProgress: Int,
    lastUpdatedAt: Timestamp
)

// UI 显示模型
data class QuestTask(
    taskId: String,
    title: String,
    description: String,
    isCompleted: Boolean,
    targetClass: Class<*>?
)
```

#### 3. QuestRepository.kt
**路径**: `app/src/main/java/com/quran/quranaudio/online/quests/repository/QuestRepository.kt`

**核心功能**:

##### A. 学习计划配置管理
- `saveUserQuestConfig(config)` - 保存配置
- `observeUserQuestConfig()` - 实时观察配置 (Flow)
- `getUserQuestConfig()` - 一次性读取配置

##### B. 每日进度追踪
- `getDailyProgress(date)` - 获取指定日期进度
- `observeTodayProgress()` - 实时观察今日进度 (Flow)

##### C. 连续统计管理
- `getStreakStats()` - 获取连续统计
- `observeStreakStats()` - 实时观察连续统计 (Flow)

##### D. 任务完成（原子事务）⭐
```kotlin
suspend fun updateTaskCompletion(
    taskId: String,
    config: UserQuestConfig,
    date: LocalDate = LocalDate.now()
)
```

**事务逻辑**:
1. 获取当前每日进度
2. 标记指定任务为完成
3. 检查所有已启用任务是否完成
4. 如果全部完成且首次完成：
   - 原子递增 `currentStreak` + 1
   - 原子递增 `monthlyProgress` + 1
   - 更新 `lastCompletedDate`
   - 更新 `longestStreak`（如果当前 > 历史最长）

##### E. 跨天检测与 Streak 重置 ⭐
```kotlin
suspend fun checkAndResetStreak()
```

**检测逻辑**:
1. 获取昨天的 `dailyProgress` 文档
2. 如果昨天文档存在但 `allTasksCompleted = false` → 重置 Streak
3. 如果 `lastCompletedDate` 不是昨天也不是今天 → 重置 Streak（跨天中断）
4. 如果今天是月初（1号）→ 重置月度进度

```kotlin
suspend fun resetStreak()
suspend fun resetMonthlyProgress(newGoal: Int)
suspend fun initializeStreakStats()
```

---

## ✅ 指令 2：任务创建、登录检测与配置保存 - 已完成

### 创建的文件

#### 1. fragment_learning_plan_setup.xml
**路径**: `app/src/main/res/layout/fragment_learning_plan_setup.xml`

**UI 组件**:
- **Daily Reading Goal 部分**:
  - `Slider` (ID: `sb_reading_goal`) - 滑块选择页数 (1-50)
  - `TextView` (ID: `tv_reading_pages_value`) - 显示当前选择的页数
  
- **Recitation & Tajweed Practice 部分**:
  - `SwitchMaterial` (ID: `sw_recitation_enabled`) - 开关朗读练习
  - `Spinner` (ID: `sp_recitation_minutes`) - 下拉选择时长 (15/30/45/60 分钟)
  
- **Extra Dhikr 部分**:
  - `SwitchMaterial` (ID: `sw_dua_reminder`) - Dua 提醒开关
  - `SwitchMaterial` (ID: `sw_tasbih_reminder`) - Tasbih 提醒开关
  
- **Challenge Duration Display**:
  - `TextView` (ID: `tv_challenge_days_display`) - 实时显示计算的挑战天数
  
- **Save Button**:
  - `MaterialButton` (ID: `btn_save_challenge`) - 保存并开始挑战

**样式**:
- 使用统一的绿色主题 (#4B9B76)
- Material Design 风格
- 圆角卡片和按钮
- 响应式布局

#### 2. LearningPlanSetupViewModel.kt
**路径**: `app/src/main/java/com/quran/quranaudio/online/quests/viewmodel/LearningPlanSetupViewModel.kt`

**核心功能**:

##### A. 挑战天数计算 ⭐
```kotlin
fun calculateChallengeDays(
    readingPages: Int,
    recitationMinutes: Int,
    recitationEnabled: Boolean
): Int
```

**计算公式**:
```
基础天数 = 30 天
阅读难度系数 = (readingPages / 10) 天
朗读难度系数 = (recitationMinutes / 15) * 2 天

总天数 = 基础天数 + 阅读难度系数 + 朗读难度系数（如果启用）
```

**示例**:
- 10 页 + 15 分钟 → 30 + 1 + 2 = 33 天
- 50 页 + 60 分钟 → 30 + 5 + 8 = 43 天

##### B. 保存配置
```kotlin
fun saveUserQuest(config: UserQuestConfig)
```

**保存流程**:
1. 调用 `questRepository.saveUserQuestConfig(config)`
2. 调用 `questRepository.initializeStreakStats()` 初始化统计
3. 更新 `saveStatus` LiveData
4. 通知 UI 保存结果

##### C. LiveData 状态管理
- `challengeDays: LiveData<Int>` - 挑战天数
- `saveStatus: LiveData<SaveStatus>` - 保存状态
- `isLoading: LiveData<Boolean>` - 加载状态

#### 3. LearningPlanSetupFragment.kt
**路径**: `app/src/main/java/com/quran/quranaudio/online/quests/ui/LearningPlanSetupFragment.kt`

**核心功能**:

##### A. 延迟强制登录 ⭐⭐⭐
```kotlin
private fun onSaveButtonClicked() {
    val currentUser = auth.currentUser
    
    if (currentUser == null) {
        // 未登录 - 弹出登录对话框
        showLoginRequiredDialog()
    } else {
        // 已登录 - 直接保存
        saveConfiguration()
    }
}
```

**登录流程**:
1. 用户点击"Save and Start My Challenge"
2. 检查 `FirebaseAuth.getInstance().currentUser`
3. 如果为 `null`：
   - 显示 `AlertDialog` 提示需要登录
   - 用户点击"Login with Google" → 调用 `initiateGoogleSignIn()`
   - 使用 `ActivityResultLauncher` 启动 Google Sign-In Intent
   - 处理登录结果 → 调用 `handleSignInResult()`
   - 登录成功 → 调用 `saveConfiguration()`
4. 如果已登录：
   - 直接调用 `saveConfiguration()`

##### B. UI 实时反馈
```kotlin
private fun updateChallengeDays() {
    val readingPages = binding.sbReadingGoal.value.toInt()
    val recitationMinutes = recitationValues[binding.spRecitationMinutes.selectedItemPosition]
    val recitationEnabled = binding.swRecitationEnabled.isChecked
    
    viewModel.calculateChallengeDays(readingPages, recitationMinutes, recitationEnabled)
}
```

- 监听 Slider 变化
- 监听 Switch 状态变化
- 监听 Spinner 选择变化
- 实时更新挑战天数显示

##### C. 配置保存与导航
```kotlin
private fun saveConfiguration() {
    val config = UserQuestConfig(
        dailyReadingPages = readingPages,
        recitationEnabled = recitationEnabled,
        recitationMinutes = recitationMinutes,
        duaReminderEnabled = duaReminderEnabled,
        tasbihReminderEnabled = tasbihReminderEnabled,
        tasbihCount = 50,
        totalChallengeDays = challengeDays,
        startDate = LocalDate.now().toString(),
        createdAt = Timestamp.now(),
        updatedAt = Timestamp.now()
    )
    
    viewModel.saveUserQuest(config)
}
```

**保存成功后**:
- 显示 Toast: "Learning plan saved successfully! ✅"
- 调用 `findNavController().popBackStack()` 返回主页

---

### 资源文件

#### 1. rounded_background_light.xml
**路径**: `app/src/main/res/drawable/rounded_background_light.xml`

圆角浅灰色背景，用于显示数值的卡片。

#### 2. spinner_background.xml
**路径**: `app/src/main/res/drawable/spinner_background.xml`

Spinner 的白色圆角边框背景。

---

### Dagger 依赖注入配置

#### 1. QuestModule.kt
**路径**: `app/src/main/java/com/quran/quranaudio/online/quests/di/QuestModule.kt`

**提供的依赖**:
- `FirebaseFirestore` (Singleton)
- `QuestRepository` (Singleton)
- `LearningPlanSetupViewModel`
- `ViewModelProvider.Factory`

#### 2. QuestComponent.kt
**路径**: `app/src/main/java/com/quran/quranaudio/online/quests/di/QuestComponent.kt`

**注入目标**:
- `LearningPlanSetupFragment`

#### 3. 集成到 ApplicationComponent ✅

**修改的文件**:
- `SubcomponentsModule.java` - 添加 `QuestComponent.class`
- `ApplicationComponent.java` - 添加 `QuestComponent.Factory questComponent();`

---

## 🎯 已实现的核心功能总结

### ✅ 数据层 (Data Layer)
1. ✅ Firebase Firestore 路径管理
2. ✅ 完整的数据模型（3 个核心模型）
3. ✅ QuestRepository 完整实现
4. ✅ 原子事务任务完成逻辑
5. ✅ 跨天检测与 Streak 重置逻辑

### ✅ UI 层 (Presentation Layer)
1. ✅ Learning Plan Setup 页面 UI
2. ✅ 实时挑战天数计算
3. ✅ 延迟强制登录流程
4. ✅ Google Sign-In 集成
5. ✅ 配置保存与导航

### ✅ 依赖注入 (Dependency Injection)
1. ✅ QuestModule 配置
2. ✅ QuestComponent 配置
3. ✅ 集成到 ApplicationComponent

---

## 📝 使用说明

### 在 Fragment 中使用

```kotlin
// 在 LearningPlanSetupFragment 中
@Inject
lateinit var viewModelFactory: ViewModelProvider.Factory

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Dagger 注入
    ((requireActivity().application) as App).appComponent
        .questComponent()
        .create()
        .inject(this)
    
    // 创建 ViewModel
    viewModel = ViewModelProvider(this, viewModelFactory)
        .get(LearningPlanSetupViewModel::class.java)
}
```

### Repository 直接使用

```kotlin
// 在其他需要访问任务数据的地方
class HomeFragment : Fragment() {
    @Inject
    lateinit var questRepository: QuestRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 注入 Repository
        ((requireActivity().application) as App).appComponent
            .questComponent()
            .create()
            .inject(this)
    }
    
    private fun observeQuestConfig() {
        lifecycleScope.launch {
            questRepository.observeUserQuestConfig().collect { config ->
                // 处理配置更新
            }
        }
    }
}
```

---

## 🚧 下一步（Phase 3）

### 待实现的功能

1. **主页 UI 集成**:
   - 创建 Streak Card UI 组件
   - 创建 Today's Quests Card UI 组件
   - 集成到 FragMain.java

2. **任务检测逻辑**:
   - Quran Reading 页数统计（ReadHistoryDBHelper + Mushaf 映射）
   - Tajweed Timer 页面创建
   - Tasbih 任务集成

3. **跨天检测触发**:
   - 在 MainActivity.onCreate() 或 HomeViewModel.init() 调用 `checkAndResetStreak()`

---

## ✅ 验证清单

### 编译验证
- [ ] 运行 `./gradlew assembleDebug` 确保无编译错误
- [ ] 检查 Dagger 注解处理器生成的代码
- [ ] 验证 Firebase 依赖正确配置

### 功能验证
- [ ] 测试 Learning Plan Setup 页面 UI 显示
- [ ] 测试挑战天数实时计算
- [ ] 测试延迟登录流程
- [ ] 测试配置保存到 Firestore
- [ ] 验证 Firestore 数据结构正确创建

### 集成验证
- [ ] 验证从主页跳转到 Setup 页面
- [ ] 验证保存后返回主页
- [ ] 验证未登录用户弹出登录对话框
- [ ] 验证登录成功后自动保存配置

---

## 📚 技术栈

- **语言**: Kotlin
- **数据库**: Firebase Firestore
- **认证**: Firebase Authentication (Google Sign-In)
- **依赖注入**: Dagger 2
- **异步处理**: Kotlin Coroutines + Flow
- **UI**: Material Design Components
- **架构**: MVVM (ViewModel + Repository)

---

**实现时间**: 2025-10-17  
**状态**: ✅ Phase 1 & 2 完成  
**下一阶段**: Phase 3 - 主页 UI 集成与任务检测












