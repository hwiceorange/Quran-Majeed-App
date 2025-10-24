# FragMain.java Daily Quests Integration Instructions

## 需要在 FragMain.java 中添加的代码

### 1. 添加导入语句（在文件顶部的 imports 区域）

```java
import com.quran.quranaudio.online.quests.ui.DailyQuestsManager;
import com.quran.quranaudio.online.quests.repository.QuestRepository;
import com.google.firebase.firestore.FirebaseFirestore;
```

### 2. 添加成员变量（在 FragMain 类中，其他成员变量附近，约第 130 行）

```java
// Daily Quests Manager
private DailyQuestsManager dailyQuestsManager;
private QuestRepository questRepository;
```

### 3. 在 onViewCreated 方法中添加初始化（约第 235 行，在 initializeMedinaLiveCard() 后面）

```java
// Initialize Daily Quests Feature
initializeDailyQuests();
```

### 4. 添加初始化方法（在 FragMain 类的末尾，约第 1400+ 行，在其他方法之后）

```java
/**
 * Initializes Daily Quests feature (Streak Card + Today's Quests)
 */
private void initializeDailyQuests() {
    try {
        // Initialize Quest Repository
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        questRepository = new QuestRepository(firestore);
        
        // Initialize Daily Quests Manager
        dailyQuestsManager = new DailyQuestsManager(
            this,                    // Fragment
            mBinding.getRoot(),      // Root View
            questRepository          // Quest Repository
        );
        
        // Initialize and start observing
        dailyQuestsManager.initialize();
        
        Log.d(TAG, "Daily Quests feature initialized");
        
    } catch (Exception e) {
        Log.e(TAG, "Failed to initialize Daily Quests feature", e);
    }
}
```

### 5. 在 onDestroyView 方法中添加清理（约第 183-192 行，在 super.onDestroy() 之前）

```java
// Cleanup Daily Quests
if (dailyQuestsManager != null) {
    dailyQuestsManager.onDestroy();
}
```

---

## 自动应用这些更改的步骤

1. 打开 `/Users/huwei/AndroidStudioProjects/quran0/app/src/main/java/com/quran/quranaudio/online/quran_module/frags/main/FragMain.java`

2. 在 imports 区域（约第 40 行附近）添加：
```java
import com.quran.quranaudio.online.quests.ui.DailyQuestsManager;
import com.quran.quranaudio.online.quests.repository.QuestRepository;
import com.google.firebase.firestore.FirebaseFirestore;
```

3. 在成员变量区域（约第 130 行附近，votdVerseNo 声明之后）添加：
```java
// Daily Quests Manager
private DailyQuestsManager dailyQuestsManager;
private QuestRepository questRepository;
```

4. 在 onViewCreated 方法中（约第 235 行，initializeMedinaLiveCard() 调用之后）添加：
```java
// Initialize Daily Quests Feature
initializeDailyQuests();
```

5. 在 FragMain 类的末尾（约第 1400+ 行）添加 initializeDailyQuests() 方法的完整实现。

6. 在 onDestroy 方法中（约第 184-191 行）的 super.onDestroy() 之前添加清理代码。

---

## 验证集成

运行应用后，检查：
1. ✅ 未登录用户：Daily Quests 不显示
2. ✅ 已登录但未创建计划：显示 "Create Your Daily Plan" 卡片
3. ✅ 已登录且创建了计划：显示 Streak Card 和 Today's Quests Card
4. ✅ Streak 和 Monthly Progress 数据正确显示
5. ✅ 今日任务状态正确反映（Go 按钮 vs ✓ 完成图标）

---

## 注意事项

- 确保 QuestComponent 已经在 ApplicationComponent 中注册
- 确保 Firebase Firestore 依赖已添加到 build.gradle
- 确保 google-services.json 文件已配置
- 确保用户已登录 Firebase Authentication

---

**下一步**: 实现任务完成检测逻辑（Quran Reading 页数统计、Tajweed Timer、Tasbih 集成）










