# Learning Plan V2 升级进度总结

**更新时间**: 2025-10-20 16:42

---

## ✅ 已完成的工作

### 1. 数据模型层 (100%)
- ✅ **`ReadingGoalUnit.kt`**: 学习单位枚举 (PAGES, VERSES, JUZ)
- ✅ **`UserLearningState.kt`**: 用户学习进度追踪模型
- ✅ **`QuestModels.kt`**: 扩展 `UserQuestConfig` 支持 `readingGoalUnit` 和 `dailyReadingGoal`

### 2. 工具类 (100%)
- ✅ **`QuranDataHelper.kt`**: 
  - 古兰经数据常量（604页, 6236节, 30卷）
  - `getRangeForUnit()` - 获取各单位的范围配置
  - `calculateChallengeDays()` - 多单位天数计算
  - `convertUnit()` - 单位转换
  - `getReadingDescription()` - 生成任务描述

### 3. UI 布局 (100%)
- ✅ **`fragment_learning_plan_setup.xml`**: 
  - 添加 `Spinner` 单位选择器（符合设计稿）
  - 左右并排布局：单位选择器(左) + 数值显示(右)
  - 动态 Slider 范围标签 (`tv_slider_min`, `tv_slider_max`)

### 4. ViewModel 逻辑 (100%)
- ✅ **`LearningPlanSetupViewModel.kt`**:
  - 添加 `selectedUnit: LiveData<ReadingGoalUnit>`
  - 添加 `sliderRange: LiveData<UnitRange>`
  - 新增 `setReadingUnit(unit)` 方法
  - 重构 `calculateChallengeDays()` 支持多种单位
  - 保持向后兼容（旧的 Pages-only 方法仍然可用）

---

## ⏳ 待完成的工作

### 5. Fragment 交互逻辑 (0%)
**文件**: `LearningPlanSetupFragment.kt`

#### 需要添加的代码:

```kotlin
// ===== 1. 设置 Spinner 适配器 =====
private fun setupUI() {
    // Unit Spinner
    val unitAdapter = ArrayAdapter(
        requireContext(),
        android.R.layout.simple_spinner_item,
        arrayOf("Pages", "Verses", "Juz'")
    )
    unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    binding.spReadingUnit.adapter = unitAdapter
    
    // Unit Spinner 监听器
    binding.spReadingUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            val unit = when (position) {
                0 -> ReadingGoalUnit.PAGES
                1 -> ReadingGoalUnit.VERSES
                2 -> ReadingGoalUnit.JUZ
                else -> ReadingGoalUnit.PAGES
            }
            viewModel.setReadingUnit(unit)
        }
        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }
    
    // Slider 监听器
    binding.sbReadingGoal.addOnChangeListener { _, value, fromUser ->
        if (fromUser) {
            updateReadingGoalDisplay(value.toInt())
            updateChallengeDays()
        }
    }
}

// ===== 2. 观察 ViewModel 状态变化 =====
private fun observeViewModel() {
    // 观察单位范围变化
    viewModel.sliderRange.observe(viewLifecycleOwner) { range ->
        // 更新 Slider 范围
        binding.sbReadingGoal.valueFrom = range.min.toFloat()
        binding.sbReadingGoal.valueTo = range.max.toFloat()
        binding.sbReadingGoal.value = range.defaultValue.toFloat()
        
        // 更新范围标签
        binding.tvSliderMin.text = range.min.toString()
        binding.tvSliderMax.text = range.max.toString()
        
        // 更新显示
        updateReadingGoalDisplay(range.defaultValue)
    }
    
    // 观察配置回显
    lifecycleScope.launch {
        viewModel.userConfig.collectLatest { config ->
            config?.let {
                // 1. 设置单位 Spinner
                val unitPosition = when (it.getReadingUnitEnum()) {
                    ReadingGoalUnit.PAGES -> 0
                    ReadingGoalUnit.VERSES -> 1
                    ReadingGoalUnit.JUZ -> 2
                }
                binding.spReadingUnit.setSelection(unitPosition)
                
                // 2. 通知 ViewModel 更新单位
                viewModel.setReadingUnit(it.getReadingUnitEnum())
                
                // 3. 设置 Slider 值
                binding.sbReadingGoal.value = it.dailyReadingGoal.toFloat()
                
                // 4. 更新显示
                updateReadingGoalDisplay(it.dailyReadingGoal)
            }
        }
    }
}

// ===== 3. 更新显示方法 =====
private fun updateReadingGoalDisplay(value: Int) {
    binding.tvReadingPagesValue.text = value.toString()
}

private fun updateChallengeDays() {
    val unit = viewModel.selectedUnit.value ?: ReadingGoalUnit.PAGES
    val goal = binding.sbReadingGoal.value.toInt()
    val recitationMinutes = recitationValues[binding.spRecitationMinutes.selectedItemPosition]
    val recitationEnabled = binding.swRecitationEnabled.isChecked
    
    viewModel.calculateChallengeDays(unit, goal, recitationMinutes, recitationEnabled)
}

// ===== 4. 保存配置（包含新字段） =====
private fun saveConfiguration() {
    val unit = viewModel.selectedUnit.value ?: ReadingGoalUnit.PAGES
    val goal = binding.sbReadingGoal.value.toInt()
    // ... (其他字段)
    
    val config = UserQuestConfig(
        dailyReadingPages = goal, // 向后兼容
        readingGoalUnit = unit.name,
        dailyReadingGoal = goal,
        recitationEnabled = recitationEnabled,
        recitationMinutes = recitationMinutes,
        // ... (其他字段)
    )
    
    viewModel.saveUserQuest(config)
}
```

---

### 6. 主页任务描述更新 (0%)
**文件**: `DailyQuestsManager.java`

#### 需要修改的方法:

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
    
    // Task 2 和 Task 3 保持不变
    // ...
}
```

---

## 📊 完成度

| 模块 | 完成度 | 状态 |
|------|--------|------|
| 数据模型层 | 100% | ✅ |
| 工具类 | 100% | ✅ |
| UI 布局 | 100% | ✅ |
| ViewModel 逻辑 | 100% | ✅ |
| Fragment 交互 | 0% | ⏳ |
| 主页集成 | 0% | ⏳ |

**总体完成度**: 66% (4/6)

---

## 🔥 下一步快速实施指南

### 方案 A: 完整实施（推荐）

1. **编辑 `LearningPlanSetupFragment.kt`**
   - 复制上述 Fragment 交互代码
   - 整合到现有 Fragment 中
   - 编译测试

2. **编辑 `DailyQuestsManager.java`**
   - 修改 `updateTodayQuestsCard()` 方法
   - 使用 `QuranDataHelper.getReadingDescription()`
   - 编译测试

3. **编译安装验证**
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### 方案 B: 分阶段实施

1. **第一阶段**: 只实现 Pages 单位的 Spinner 版本
   - Spinner 固定显示 "Pages"
   - 保持现有逻辑不变
   - 验证 UI 正常工作

2. **第二阶段**: 添加 Verses 和 Juz' 支持
   - 实现单位切换逻辑
   - 测试各单位计算正确性

3. **第三阶段**: 主页集成
   - 更新任务描述生成逻辑

---

## 🧪 验证清单

### Fragment 交互验证
- [ ] Spinner 可以正确切换单位 (Pages/Verses/Juz')
- [ ] 切换单位时 Slider 范围正确更新
- [ ] 切换单位时数值显示正确
- [ ] Slider 拖动时挑战天数实时更新
- [ ] 配置回显时单位和数值正确显示

### 计算逻辑验证
- [ ] Pages: 1页 = 604天, 10页 = 61天, 50页 = 13天
- [ ] Verses: 1节 = 6236天, 10节 = 624天, 100节 = 63天
- [ ] Juz': 1卷 = 30天, 5卷 = 6天, 30卷 = 1天

### 数据持久化验证
- [ ] 保存配置包含 `readingGoalUnit` 和 `dailyReadingGoal`
- [ ] 再次打开页面时单位和数值正确回显
- [ ] 主页任务描述根据单位正确显示

---

## 📝 已知问题

1. **底部导航栏隐藏** - 尚未实现
   - 需要在 `MainActivity` 添加 `hideBottomNavigation()` 方法
   - 在 Fragment 的 `onViewCreated` 和 `onDestroyView` 中调用

2. **用户学习进度追踪** - 尚未实现
   - `UserLearningState` 模型已创建
   - Repository 方法尚未添加
   - 可作为未来功能开发

---

## 🎯 关键文件路径

| 文件 | 路径 | 状态 |
|------|------|------|
| ReadingGoalUnit | `app/src/main/java/com/quran/quranaudio/online/quests/data/ReadingGoalUnit.kt` | ✅ |
| UserLearningState | `app/src/main/java/com/quran/quranaudio/online/quests/data/UserLearningState.kt` | ✅ |
| QuestModels | `app/src/main/java/com/quran/quranaudio/online/quests/data/QuestModels.kt` | ✅ |
| QuranDataHelper | `app/src/main/java/com/quran/quranaudio/online/quests/utils/QuranDataHelper.kt` | ✅ |
| LearningPlanSetupViewModel | `app/src/main/java/com/quran/quranaudio/online/quests/viewmodel/LearningPlanSetupViewModel.kt` | ✅ |
| LearningPlanSetupFragment | `app/src/main/java/com/quran/quranaudio/online/quests/ui/LearningPlanSetupFragment.kt` | ⏳ |
| fragment_learning_plan_setup.xml | `app/src/main/res/layout/fragment_learning_plan_setup.xml` | ✅ |
| DailyQuestsManager | `app/src/main/java/com/quran/quranaudio/online/quests/ui/DailyQuestsManager.java` | ⏳ |

---

## 💡 建议

由于目前已完成：
- ✅ 所有数据模型和工具类
- ✅ UI 布局完全符合设计稿
- ✅ ViewModel 逻辑完整

**建议立即进行**:
1. 快速实现 Fragment 交互逻辑（约30分钟）
2. 更新主页任务描述（约10分钟）
3. 编译测试验证（约10分钟）

**预计总时间**: 50分钟可完成全部功能

---

## 🔗 相关文档

- 详细实施计划: `LEARNING_PLAN_V2_IMPLEMENTATION_PLAN.md`
- 挑战天数计算修复: `CHALLENGE_DAYS_CALCULATION_FIX.md`

