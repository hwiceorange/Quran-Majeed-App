# Daily Quests Feature - Phase 3 Implementation Complete

## ✅ 已完成功能总结

### Phase 1 & 2 回顾
- ✅ Firebase Firestore 数据模型（`FirestoreConstants`, `QuestModels`, `QuestRepository`）
- ✅ Learning Plan Setup 页面（UI + ViewModel + Fragment）
- ✅ 延迟强制登录逻辑
- ✅ Dagger 依赖注入配置

### Phase 3: 主页 UI 集成 - 已完成 ✅

#### 1. 创建的 UI 布局文件

| 文件 | 说明 | 状态 |
|------|------|------|
| `layout_daily_quests_create_card.xml` | "Create Your Daily Plan" 卡片 | ✅ |
| `layout_streak_card.xml` | 连续天数卡片（Streak Card） | ✅ |
| `layout_today_quests_card.xml` | 今日任务列表卡片（Today's Quests） | ✅ |

#### 2. 创建的 Drawable 资源

| 文件 | 说明 | 状态 |
|------|------|------|
| `circular_white_background.xml` | 圆形白色背景 | ✅ |
| `progress_bar_monthly.xml` | 月度进度条样式 | ✅ |
| `mosque_pattern.xml` | 清真寺图案背景 | ✅ |
| `ic_task_list.xml` | 任务列表图标 | ✅ |
| `ic_check_circle.xml` | 完成图标（✓） | ✅ |
| `ic_headphones.xml` | 耳机图标（Tajweed） | ✅ |

#### 3. 核心业务逻辑类

##### A. `DailyQuestsManager.java` ⭐⭐⭐
**路径**: `quests/ui/DailyQuestsManager.java`

**职责**:
- 检测用户是否已创建学习计划
- 根据配置状态显示不同UI（Create Card vs Quests Cards）
- 观察 Firebase 数据并更新 UI
- 处理导航和点击事件

**核心方法**:
```java
public void initialize()                      // 初始化并开始观察
private void observeQuestConfig()             // 观察学习计划配置
private void showCreateCard()                 // 显示创建计划卡片
private void showQuestsCards()                // 显示任务卡片
private void observeStreakStats()             // 观察 Streak 统计
private void observeTodayProgress()           // 观察今日进度
private void updateStreakCard()               // 更新 Streak 卡片 UI
private void updateTodayQuestsCard()          // 更新今日任务卡片 UI
private void updateTaskCompletionStatus()     // 更新任务完成状态
```

##### B. `HomeQuestsViewModel.kt` ⭐⭐
**路径**: `quests/viewmodel/HomeQuestsViewModel.kt`

**职责**:
- 暴露 `questConfig`, `streakStats`, `todayProgress` 为 LiveData
- 触发跨天 Streak 检测
- 管理加载状态

**核心功能**:
```kotlin
fun initializeRepository(repository: QuestRepository)
fun checkAndResetStreak()                     // 跨天检测
fun getQuestConfig(): LiveData<UserQuestConfig?>
fun getStreakStats(): LiveData<StreakStats>
fun getTodayProgress(): LiveData<DailyProgressModel?>
```

#### 4. FragMain.java 集成 ✅

**添加的成员变量**:
```java
private DailyQuestsManager dailyQuestsManager;
private QuestRepository questRepository;
```

**添加的方法**:
```java
private void initializeDailyQuests()
```

**onViewCreated 调用**:
```java
initializeDailyQuests();  // 在 initializeMedinaLiveCard() 后调用
```

**onDestroyView 清理**:
```java
if (dailyQuestsManager != null) {
    dailyQuestsManager.onDestroy();
}
```

#### 5. 布局文件集成 ✅

**修改**: `frag_main.xml`

添加了两个容器：
```xml
<!-- Create Card Container -->
<include
    android:id="@+id/daily_quests_create_card"
    layout="@layout/layout_daily_quests_create_card"
    android:visibility="gone" />

<!-- Quests Cards Container -->
<LinearLayout
    android:id="@+id/daily_quests_cards_container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:visibility="gone">
    
    <include android:id="@+id/streak_card" 
             layout="@layout/layout_streak_card" />
    
    <include android:id="@+id/today_quests_card" 
             layout="@layout/layout_today_quests_card" />
    
</LinearLayout>
```

#### 6. 导航配置 ✅

**修改**: `nav_graphmain.xml`

添加了 Learning Plan Setup Fragment 定义和导航动作：
```xml
<fragment
    android:id="@+id/nav_home"
    android:name="com.quran.quranaudio.online.quran_module.frags.main.FragMain"
    android:label="HomeFragment">
    
    <action
        android:id="@+id/action_nav_home_to_learning_plan_setup"
        app:destination="@id/nav_learning_plan_setup" />
</fragment>

<fragment
    android:id="@+id/nav_learning_plan_setup"
    android:name="com.quran.quranaudio.online.quests.ui.LearningPlanSetupFragment"
    android:label="Learning Plan Setup" />
```

---

## 📊 功能流程图

```
用户打开主页 (FragMain)
    ↓
DailyQuestsManager.initialize()
    ↓
检查登录状态
    ↓
╔═══════════════════════════════════════════╗
║ 未登录？                                    ║
║  → 隐藏所有 Daily Quests UI                ║
╚═══════════════════════════════════════════╝
    ↓ (已登录)
观察 Firebase: questConfig
    ↓
╔═══════════════════════════════════════════╗
║ questConfig == null?                       ║
║  → 显示 "Create Your Daily Plan" Card     ║
║  → 点击 → 导航到 Learning Plan Setup       ║
╚═══════════════════════════════════════════╝
    ↓ (config 存在)
隐藏 Create Card，显示 Quests Cards
    ├─ Streak Card
    │   ├─ 显示连续天数（13 Days）
    │   ├─ 显示月度进度（13 / 31）
    │   └─ 进度条更新
    │
    └─ Today's Quests Card
        ├─ Task 1: Quran Reading (Read X pages)
        ├─ Task 2: Tajweed Practice (Practice Y minutes)
        └─ Task 3: Dhikr (Complete 50 Dhikr) [可选]
    ↓
实时观察数据变化
    ├─ streakStats 更新 → 更新 Streak Card
    └─ todayProgress 更新 → 更新任务完成状态
        ├─ 未完成：显示 [Go] 按钮
        └─ 已完成：显示 [✓] 图标
```

---

## 🎨 UI 显示逻辑

### 场景 1：未登录用户
```
主页正常显示
Daily Quests 相关 UI 完全隐藏
```

### 场景 2：已登录但未创建计划
```
主页显示：
├─ Header
├─ Prayer Card
├─ [✨ Create Your Daily Plan Card] ← 点击跳转到 Setup
├─ Verse of The Day
├─ Mecca Live
└─ Medina Live
```

### 场景 3：已登录且已创建计划
```
主页显示：
├─ Header
├─ Prayer Card
├─ [📊 Streak Card]
│   ├─ Streak: 13 Days
│   ├─ Monthly Goal: 13 / 31
│   ├─ Progress Bar: ████████░░░░░░░
│   └─ [⚙️ Settings Icon] ← 点击编辑计划
│
├─ [✅ Today's Quests]
│   ├─ Quran Reading (Read 10 pages) [Go]
│   ├─ Tajweed Practice (Practice 15 min) [Go]
│   └─ Dhikr (Complete 50 Dhikr) [✓]
│
├─ Verse of The Day
├─ Mecca Live
└─ Medina Live
```

---

## 🔄 数据流

```
Firebase Firestore
    ↓
QuestRepository
    ├─ observeUserQuestConfig() → Flow<UserQuestConfig?>
    ├─ observeStreakStats() → Flow<StreakStats>
    └─ observeTodayProgress() → Flow<DailyProgressModel?>
    ↓
HomeQuestsViewModel
    ├─ questConfig: LiveData
    ├─ streakStats: LiveData
    └─ todayProgress: LiveData
    ↓
DailyQuestsManager (观察 LiveData)
    ↓
更新 FragMain UI
    ├─ Create Card 显示/隐藏
    ├─ Streak Card 数据更新
    └─ Today's Quests 任务状态更新
```

---

## ✅ 完成清单

| 功能 | 状态 | 说明 |
|------|------|------|
| ✅ Create Card UI | 完成 | 绿色卡片，图标 + 文字 + 按钮 |
| ✅ Streak Card UI | 完成 | 连续天数 + 月度进度 + 进度条 |
| ✅ Today's Quests UI | 完成 | 3 个任务卡片，动态显示 |
| ✅ DailyQuestsManager | 完成 | 业务逻辑管理器 |
| ✅ HomeQuestsViewModel | 完成 | LiveData 暴露 |
| ✅ FragMain 集成 | 完成 | 初始化 + 观察 + 清理 |
| ✅ 导航配置 | 完成 | nav_graphmain.xml |
| ✅ 布局集成 | 完成 | frag_main.xml |
| ✅ 跨天检测触发 | 完成 | 在 ViewModel.init() 中 |
| ✅ 任务完成状态显示 | 完成 | Go 按钮 vs ✓ 图标 |
| ✅ Task 3 条件显示 | 完成 | 根据 tasbihReminderEnabled |

---

## 🚧 待实现功能（Phase 4）

### 任务完成检测逻辑

| 任务 | 实现方式 | 优先级 |
|------|----------|--------|
| Quran Reading | ReadHistoryDBHelper + Mushaf 映射 | P1 |
| Tajweed Practice | 新建 TajweedTimerActivity | P1 |
| Dhikr (Tasbih) | 修改现有 TasbihActivity | P1 |

### 点击事件处理

| 事件 | 目标 | 优先级 |
|------|------|--------|
| Task 1 Go 按钮 | 跳转到 Quran Reader | P1 |
| Task 2 Go 按钮 | 跳转到 Tajweed Timer | P1 |
| Task 3 Go 按钮 | 跳转到 Tasbih Page | P1 |
| Settings 图标 | 跳转到 Learning Plan Setup (编辑) | P2 |

---

## 🐛 已知问题

1. **任务完成检测逻辑未实现**
   - 当前只显示任务状态，但不会自动标记完成
   - 需要在各个页面集成任务完成回调

2. **图标资源可能缺失**
   - `ic_settings` 图标可能需要确认是否存在
   - `ic_book_open` 图标可能需要添加

3. **动画效果**
   - 需要确认导航动画资源文件是否存在
   - 可能需要创建 `anim/slide_in_right.xml` 等

---

## 📝 测试建议

### 手动测试步骤

1. **未登录用户测试**
   ```
   1. 清除应用数据
   2. 打开应用（不登录）
   3. 验证：Daily Quests UI 完全隐藏
   ```

2. **首次创建计划测试**
   ```
   1. 使用 Google 登录
   2. 验证：显示 "Create Your Daily Plan" 卡片
   3. 点击卡片
   4. 验证：跳转到 Learning Plan Setup
   5. 填写配置并保存
   6. 验证：返回主页，显示 Streak Card + Today's Quests
   ```

3. **数据观察测试**
   ```
   1. 在 Firebase Console 中修改 streakStats/summary
   2. 验证：Streak Card 实时更新
   3. 修改 dailyProgress/{today}
   4. 验证：任务状态实时更新（Go ↔ ✓）
   ```

### Logcat 调试标签

```
adb logcat -s DailyQuestsManager HomeQuestsViewModel QuestRepository
```

---

## 🎯 下一步行动

1. **实现任务完成检测**（Phase 4）
   - Quran Reading 页数统计
   - Tajweed Timer 页面
   - Tasbih 任务集成

2. **添加缺失图标资源**
   - 检查并添加所有图标

3. **端到端测试**
   - 完整流程测试
   - 边界情况测试
   - 低端设备测试

4. **性能优化**
   - Firebase 查询优化
   - UI 渲染性能优化

---

**实现时间**: 2025-10-17  
**状态**: ✅ Phase 3 完成，准备进入 Phase 4  
**下一阶段**: 任务完成检测与跳转逻辑实现












