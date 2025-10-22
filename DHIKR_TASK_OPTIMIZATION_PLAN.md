# Dhikr (Tasbih) 任务优化方案

**日期**: 2025-10-20  
**目标**: 确保 Dhikr 任务的计数、统计和状态更新逻辑健壮且可靠

---

## 当前实现检查结果

### ✅ 已实现的功能
1. **QuestRepository 集成**: TasbihFragment 已集成 QuestRepository
2. **任务完成标记**: 达到目标后调用 `updateTaskCompletion("task_3_tasbih", true)`
3. **计数持久化**: TasbihManager 使用 SharedPreferences 持久化计数
4. **每日重置**: 自动检测新的一天并重置计数器
5. **用户反馈**: 完成时显示 Toast 提示

### ❌ 需要修复的问题

#### 问题 1: 目标值硬编码
**当前状态**:
```java
private static final int DAILY_QUEST_TARGET = 50; // 硬编码
```

**问题**: 不从 UserQuestConfig 读取 `tasbihCount`

**影响**: 用户在 Learning Plan Setup 设置的 Tasbih 数量不生效

---

#### 问题 2: 缺少防作弊机制
**当前状态**: 每次点击立即计数，无延迟检查

**问题**: 用户可以快速连续点击作弊

**影响**: 
- 任务完成速度不真实
- 影响数据统计准确性
- 如果未来有奖励机制，可能被滥用

---

#### 问题 3: 线程使用不当
**当前状态**:
```java
new Thread(() -> {
    questRepository.updateTaskCompletion("task_3_tasbih", true);
}).start();
```

**问题**: 应使用协程而非原始线程

**影响**: 
- 线程管理不优雅
- 没有异常处理
- 内存泄漏风险

---

#### 问题 4: 禁用状态未处理
**当前状态**: 未检查用户是否禁用了 Tasbih Reminder

**问题**: 如果用户禁用 Tasbih，任务仍然可以完成

**影响**: 逻辑不一致

---

## 优化方案

### 优化 1: 从 Firestore 读取目标值

**修改文件**: `TasbihFragment.java`

**步骤**:
1. 在 `initDailyQuest()` 中从 Firestore 读取 UserQuestConfig
2. 获取 `tasbihCount` 和 `tasbihReminderEnabled`
3. 如果禁用，显示提示并禁用计数器
4. 动态设置目标值

**伪代码**:
```java
private void initDailyQuest() {
    questRepository = new QuestRepository(FirebaseFirestore.getInstance());
    
    // 从 Firestore 读取配置
    fetchUserQuestConfig(new ConfigCallback() {
        @Override
        public void onSuccess(UserQuestConfig config) {
            if (config.getTasbihReminderEnabled()) {
                dailyQuestTarget = config.getTasbihCount();
                Log.d(TAG, "Tasbih target: " + dailyQuestTarget);
            } else {
                // 禁用任务
                disableDailyQuest();
            }
        }
        
        @Override
        public void onError(Exception e) {
            // 使用默认值 50
            dailyQuestTarget = 50;
        }
    });
}
```

---

### 优化 2: 添加防作弊机制

**策略**: 限制点击间隔（最小 300ms）

**修改文件**: `TasbihFragment.java`

**实现**:
```java
public class TasbihFragment extends BaseFragment {
    private static final long MIN_CLICK_INTERVAL_MS = 300; // 最小点击间隔 300ms
    private long lastClickTime = 0;
    
    public void tasbihClick() {
        // 防作弊检查
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime < MIN_CLICK_INTERVAL_MS) {
            Log.w(TAG, "Click too fast, ignored (anti-cheat)");
            return; // 忽略过快的点击
        }
        lastClickTime = currentTime;
        
        // 正常计数逻辑
        AnimationDrawable animationDrawable = (AnimationDrawable) getResources().getDrawable(R.drawable.tasbih_animation);
        this.tasbihView.setImageDrawable(animationDrawable);
        animationDrawable.start();
        this.total++;
        TasbihManager.get().putTotal(this.total);
        
        // Daily Quest: Increment daily count
        int dailyCount = TasbihManager.get().incrementDailyCount();
        Log.d(TAG, "Tasbih clicked - Daily count: " + dailyCount + "/" + dailyQuestTarget);
        
        // Check if daily quest completed
        if (!dailyQuestCompleted && dailyCount >= dailyQuestTarget) {
            dailyQuestCompleted = true;
            onDailyQuestCompleted();
        }
        
        updateText(true);
    }
}
```

---

### 优化 3: 使用协程替代 Thread

**修改文件**: `TasbihFragment.java` → 转换为 `TasbihFragment.kt`

**实现**:
```kotlin
private fun onDailyQuestCompleted() {
    Log.d(TAG, "Daily Tasbih Quest completed!")
    
    // Show celebration toast
    Toast.makeText(requireContext(), "🎉 Daily Tasbih Quest completed! ($dailyCount/$dailyQuestTarget)", Toast.LENGTH_LONG).show()
    
    // Mark task as complete in Firebase (if user is logged in)
    if (FirebaseAuth.getInstance().currentUser != null) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                questRepository.updateTaskCompletion("task_3_tasbih", true)
                Log.d(TAG, "Task 3 (Tasbih) marked as complete in Firebase")
                
                withContext(Dispatchers.Main) {
                    // Optional: Show success feedback
                    Toast.makeText(requireContext(), "Progress synced! ✓", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark Tasbih task as complete", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Sync failed, will retry later", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
```

---

### 优化 4: 禁用状态处理

**修改文件**: `TasbihFragment.java`

**实现**:
```java
private void disableDailyQuest() {
    Log.d(TAG, "Tasbih Reminder is disabled in user config");
    
    // 可选：显示提示信息
    Toast.makeText(activity, "Tasbih task is not enabled in your learning plan", Toast.LENGTH_SHORT).show();
    
    // 可选：禁用计数器交互
    // tasbihView.setEnabled(false);
    // tasbihView.setAlpha(0.5f);
}
```

---

### 优化 5: 确保不影响 UserLearningState

**检查清单**:
- [ ] Tasbih 完成后不调用 `saveUserLearningState()`
- [ ] 只更新 `task3TasbihCompleted` 字段
- [ ] 不修改 `lastReadSurah/Ayah` 字段

**验证代码**:
```java
// ✅ 正确：只更新任务状态
questRepository.updateTaskCompletion("task_3_tasbih", true);

// ❌ 错误：不应该调用
// questRepository.saveUserLearningState(surah, ayah);
```

---

## 完整实施计划

### Phase 1: 配置读取 (优先级: 高)
- [ ] 在 `TasbihFragment` 添加 `fetchUserQuestConfig()` 方法
- [ ] 动态设置 `dailyQuestTarget`
- [ ] 处理禁用状态

### Phase 2: 防作弊机制 (优先级: 高)
- [ ] 添加 `lastClickTime` 变量
- [ ] 在 `tasbihClick()` 中添加间隔检查
- [ ] 设置最小间隔为 300ms

### Phase 3: 协程优化 (优先级: 中)
- [ ] 转换 `TasbihFragment` 为 Kotlin
- [ ] 使用 `lifecycleScope.launch` 替代 `new Thread()`
- [ ] 添加完善的异常处理

### Phase 4: 测试验证 (优先级: 高)
- [ ] 测试从 Firestore 读取配置
- [ ] 测试防作弊机制（快速点击）
- [ ] 测试任务完成后状态同步
- [ ] 验证不影响 UserLearningState

---

## 预期结果

### 用户体验改进
1. ✅ Tasbih 目标值与用户配置一致
2. ✅ 防止快速点击作弊
3. ✅ 完成后即时同步到 Firestore
4. ✅ 主页任务卡片自动刷新为 "已完成"

### 代码质量改进
1. ✅ 使用协程替代原始线程
2. ✅ 完善的异常处理
3. ✅ 符合 Android 最佳实践

### 数据一致性
1. ✅ Tasbih 任务状态正确更新
2. ✅ 不影响 Quran 学习状态
3. ✅ 支持跨设备同步

---

## 风险评估

### 低风险
- 添加防作弊机制（向后兼容）
- 从 Firestore 读取配置（有默认值）

### 中风险
- 转换为 Kotlin（需要全面测试）
- 协程使用（需要确保生命周期管理）

### 缓解措施
1. 保留 Java 版本作为备份
2. 充分测试所有场景
3. 添加详细日志记录
4. 使用默认值作为降级方案

---

## 时间估算

- **Phase 1**: 30 分钟
- **Phase 2**: 20 分钟
- **Phase 3**: 40 分钟
- **Phase 4**: 30 分钟

**总计**: 约 2 小时

---

## 后续优化建议

1. **进度保存**: 支持中断后继续（当前是每日重置）
2. **统计数据**: 记录历史完成次数、总计数等
3. **成就系统**: 达到里程碑时触发成就
4. **社交分享**: 分享每日完成状态

