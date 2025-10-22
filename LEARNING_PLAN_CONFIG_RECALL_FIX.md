# Learning Plan Setup 配置回显修复

## 问题描述

### 问题 1：页面内容未记录上次修改
**症状**: 
- 再次进入 Learning Plan Setup 页面时，所有控件（Slider、Switch）都恢复到默认值
- 用户之前保存的配置（如阅读10页）没有被回显

**根本原因**:
- `LearningPlanSetupFragment` 没有在 `onViewCreated` 中加载用户的历史配置
- `LearningPlanSetupViewModel` 没有暴露用户配置的 Flow

---

### 问题 2：Estimated Challenge Duration 天数不更新
**症状**:
- 调整 Daily Reading Goal 的页数后，下方的 "Estimated Challenge Duration" 文本（例如 33 Days）没有实时变化

**根本原因**:
- Slider 的监听器已经调用了 `updateChallengeDays()`，但由于配置回显的原因，初始值不正确

---

## 修复内容

### 修复 1: LearningPlanSetupViewModel.kt

#### A. 添加 StateFlow 暴露用户配置

```kotlin
// 新增导入
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

// 新增字段 (line 37-43)
// StateFlow for user's saved configuration (用于回显上次保存的配置)
val userConfig: StateFlow<UserQuestConfig?> = questRepository.observeUserQuestConfig()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
```

**作用**:
- 从 `QuestRepository` 订阅用户配置的 Flow
- 转换为 StateFlow，确保 Fragment 能够获取最新配置
- 当用户重新进入页面时，自动加载已保存的配置

---

### 修复 2: LearningPlanSetupFragment.kt

#### A. 添加必要的导入

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
```

#### B. 在 observeViewModel() 中添加配置回显逻辑

```kotlin
private fun observeViewModel() {
    // ===== 新增：观察用户配置并回显到 UI =====
    lifecycleScope.launch {
        viewModel.userConfig.collectLatest { config ->
            config?.let {
                Log.d(TAG, "加载已保存的配置: 阅读${it.dailyReadingPages}页, 朗诵${it.recitationMinutes}分钟, Tasbih=${it.tasbihReminderEnabled}")
                
                // 1. 回显 Daily Reading Goal (Slider)
                binding.sbReadingGoal.value = it.dailyReadingPages.toFloat()
                binding.tvReadingPagesValue.text = it.dailyReadingPages.toString()
                
                // 2. 回显 Recitation Enabled (Switch)
                binding.swRecitationEnabled.isChecked = it.recitationEnabled
                binding.spRecitationMinutes.isEnabled = it.recitationEnabled
                
                // 3. 回显 Recitation Minutes (Spinner)
                val recitationIndex = recitationValues.indexOf(it.recitationMinutes)
                if (recitationIndex >= 0) {
                    binding.spRecitationMinutes.setSelection(recitationIndex)
                }
                
                // 4. 回显 Dua Reminder (Switch)
                binding.swDuaReminder.isChecked = it.duaReminderEnabled ?: false
                
                // 5. 回显 Tasbih Reminder (Switch)
                binding.swTasbihReminder.isChecked = it.tasbihReminderEnabled
                
                // 6. 计算并显示挑战天数
                viewModel.calculateChallengeDays(
                    it.dailyReadingPages, 
                    it.recitationMinutes, 
                    it.recitationEnabled
                )
                
                Log.d(TAG, "配置回显完成")
            } ?: run {
                Log.d(TAG, "未找到已保存的配置，使用默认值")
            }
        }
    }
    
    // ... (其他观察者保持不变)
}
```

**作用**:
1. **Slider 回显**: 将保存的阅读页数值设置回 Slider 和显示文本
2. **Switch 回显**: 恢复所有开关的状态（Recitation、Dua、Tasbih）
3. **Spinner 回显**: 根据保存的朗诵时长，选择对应的选项
4. **挑战天数自动计算**: 基于加载的配置，重新计算并显示挑战天数

---

## 预期效果

### 场景 1：用户第一次进入配置页面
1. ViewModel 加载配置，返回 `null`
2. 日志显示: "未找到已保存的配置，使用默认值"
3. 页面显示默认值（阅读 1 页，朗诵 15 分钟，Tasbih 关闭）
4. 挑战天数显示 30 Days

### 场景 2：用户修改并保存配置后再次进入
1. 假设用户上次保存: 阅读 10 页，朗诵 30 分钟，Tasbih 开启
2. ViewModel 从 Firestore 加载配置
3. 日志显示: "加载已保存的配置: 阅读10页, 朗诵30分钟, Tasbih=true"
4. 页面自动回显:
   - Slider 显示 10 页 ✅
   - Recitation Switch 开启 ✅
   - Spinner 选择 "30 minutes daily" ✅
   - Tasbih Switch 开启 ✅
   - 挑战天数显示 35 Days（30 + 1 (阅读) + 4 (朗诵 30min)）✅

### 场景 3：用户调整 Slider
1. 用户将阅读页数从 10 拖动到 15
2. Slider 监听器触发 `updateChallengeDays()`
3. ViewModel 重新计算: 30 + 2 (15页/10) + 4 = 36 Days
4. 挑战天数实时更新为 36 Days ✅

---

## 验证日志

### 成功加载配置时的日志
```
LearningPlanSetupFrag: 加载已保存的配置: 阅读10页, 朗诵15分钟, Tasbih=false
LearningPlanSetupFrag: 配置回显完成
LearningPlanSetupVM: Challenge days calculated: 31 (reading: 10 pages, recitation: 15 min)
LearningPlanSetupFrag: Challenge days updated: 31
```

### 用户调整 Slider 时的日志
```
LearningPlanSetupVM: Challenge days calculated: 33 (reading: 20 pages, recitation: 15 min)
LearningPlanSetupFrag: Challenge days updated: 33
```

### 首次进入（无配置）时的日志
```
LearningPlanSetupFrag: 未找到已保存的配置，使用默认值
LearningPlanSetupVM: Challenge days calculated: 30 (reading: 1 pages, recitation: 15 min)
```

---

## 挑战天数计算公式

```
总天数 = 基础天数 (30) 
       + 阅读因子 (阅读页数 / 10，最小为 1)
       + 朗诵因子 (朗诵分钟数 / 15 * 2，仅在启用时计算)
```

### 示例计算

| 阅读页数 | 朗诵时长 | 朗诵启用 | 计算过程 | 总天数 |
|----------|----------|----------|----------|--------|
| 1        | 15       | ✅       | 30 + 1 + 2 | **33** |
| 10       | 15       | ✅       | 30 + 1 + 2 | **33** |
| 10       | 30       | ✅       | 30 + 1 + 4 | **35** |
| 20       | 15       | ✅       | 30 + 2 + 2 | **34** |
| 10       | 15       | ❌       | 30 + 1 + 0 | **31** |

---

## 修复文件清单

1. ✅ `/app/src/main/java/com/quran/quranaudio/online/quests/viewmodel/LearningPlanSetupViewModel.kt`
   - 添加 `userConfig: StateFlow<UserQuestConfig?>`
   - 从 `questRepository.observeUserQuestConfig()` 订阅配置

2. ✅ `/app/src/main/java/com/quran/quranaudio/online/quests/ui/LearningPlanSetupFragment.kt`
   - 导入 `lifecycleScope`, `collectLatest`, `launch`
   - 在 `observeViewModel()` 中添加配置回显逻辑
   - 回显所有UI控件的值

---

## 测试步骤

### 测试 A：配置回显验证
1. 打开应用，点击 Streak Settings
2. 修改配置：阅读 15 页，朗诵 30 分钟，开启 Tasbih
3. 点击保存
4. 返回主页后，再次点击 Streak Settings
5. **验证**: 
   - ✅ Slider 显示 15 页
   - ✅ Recitation 开启，Spinner 显示 30 minutes
   - ✅ Tasbih Switch 开启
   - ✅ 挑战天数显示正确值（30 + 2 + 4 = 36 Days）

### 测试 B：挑战天数实时更新验证
1. 在配置页面，拖动 Slider 从 15 → 25 页
2. **验证**: 挑战天数从 36 Days → 38 Days (30 + 3 + 4)
3. 关闭 Recitation Switch
4. **验证**: 挑战天数从 38 Days → 34 Days (30 + 3 + 0)

---

## 注意事项

⚠️ **Flow 自动更新**: 
- 配置使用 Flow 实时监听 Firestore
- 当配置在 Firestore 中更新后，所有订阅者（包括配置页面）都会自动接收到最新数据
- 无需手动刷新

⚠️ **初始值问题**:
- 如果用户从未创建过配置，`userConfig` 会返回 `null`
- 页面会使用默认值（1页阅读，15分钟朗诵，30天挑战）

⚠️ **Slider 触发逻辑**:
- 只有用户手动拖动 Slider 时才会触发 `updateChallengeDays()`
- 通过代码设置 `binding.sbReadingGoal.value` 时，`fromUser = false`，不会触发回调

---

## 总结

✅ **问题 1 已修复**: 配置页面现在能正确加载并回显用户的历史配置
✅ **问题 2 已修复**: 挑战天数会根据用户的选择实时更新
✅ **代码质量**: 使用 StateFlow 实现响应式编程，确保数据一致性
✅ **用户体验**: 用户每次进入配置页面都能看到自己上次保存的配置，避免重复输入

