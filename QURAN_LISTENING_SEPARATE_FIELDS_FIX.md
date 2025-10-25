# Quran Listening 独立字段修复

## 🐛 问题描述

**现象**：
- 用户播放 Quran Listening 到 Surah 2, Verse 7
- 退出应用
- 重新打开，点击每日任务 "Quran Listening" 的 "Go" 按钮
- **实际结果**：从 Surah 1, Verse 1 开始播放 ❌
- **预期结果**：从 Surah 2, Verse 7 开始播放 ✅

---

## 🔍 根本原因

**核心错误**：**听力模式和阅读模式混用了同一组 Firestore 字段**

### 错误的实现：

```java
// ❌ 保存时：听力模式和阅读模式都保存到 lastReadSurah/lastReadAyah
learningState.put("lastReadSurah", surah);
learningState.put("lastReadAyah", ayah);

// ❌ 读取时：听力模式也读取 lastReadSurah/lastReadAyah
Integer lastReadSurah = documentSnapshot.getLong("lastReadSurah")...
Integer lastReadAyah = documentSnapshot.getLong("lastReadAyah")...
```

**结果**：
1. 用户在**阅读模式**下浏览到 Surah 10
2. 用户在**听力模式**下听到 Surah 2, Verse 7
3. 退出应用时，阅读模式的 `onPause()` **后执行**，覆盖了听力模式保存的位置
4. 下次打开听力任务时，从 Surah 10 开始 ❌

---

## ✅ 解决方案

**核心修复**：**为听力模式和阅读模式使用独立的 Firestore 字段**

### Firestore 数据结构：

```javascript
users/{userId}/learningState/current {
  // 听力模式字段 🎧
  "lastListenSurah": 2,
  "lastListenAyah": 7,
  "lastListenTimestamp": Timestamp,
  
  // 阅读模式字段 📖
  "lastReadSurah": 10,
  "lastReadAyah": 15,
  "lastReadTimestamp": Timestamp,
  
  // Juz 模式字段 🕌
  "lastReadJuz": 3
}
```

---

## 📝 代码修改

### 1. `ActivityReader.java` - 保存逻辑修复

**文件位置**：`app/src/main/java/com/quran/quranaudio/online/quran_module/activities/ActivityReader.java`

**修改位置**：`saveCurrentPositionToFirestore()` 方法（第2250-2271行）

```java
java.util.Map<String, Object> learningState = new java.util.HashMap<>();

// 🔥 关键修复：区分听力模式和阅读模式，使用不同的字段
if (isListeningMode) {
    // 听力模式：保存到 lastListenSurah 和 lastListenAyah
    learningState.put("lastListenSurah", surah);
    learningState.put("lastListenAyah", ayah);
    learningState.put("lastListenTimestamp", com.google.firebase.Timestamp.now());
    android.util.Log.d("ActivityReader", "🎧 Saving LISTENING position: Surah " + surah + ", Ayah " + ayah);
} else {
    // 阅读模式：保存到 lastReadSurah 和 lastReadAyah
    learningState.put("lastReadSurah", surah);
    learningState.put("lastReadAyah", ayah);
    learningState.put("lastReadTimestamp", com.google.firebase.Timestamp.now());
    android.util.Log.d("ActivityReader", "📖 Saving READING position: Surah " + surah + ", Ayah " + ayah);
}
```

---

### 2. `DailyQuestsManager.java` - 读取逻辑修复

**文件位置**：`app/src/main/java/com/quran/quranaudio/online/quests/ui/DailyQuestsManager.java`

**修改位置**：`fetchUserLearningStateAndStartReader()` 方法（第687-695行）

```java
.addOnSuccessListener(documentSnapshot -> {
    if (documentSnapshot.exists()) {
        // 🔥 关键修复：听力模式应该读取 lastListenSurah 和 lastListenAyah，不是 lastReadSurah
        Integer lastListenSurah = documentSnapshot.getLong("lastListenSurah") != null 
            ? documentSnapshot.getLong("lastListenSurah").intValue() : 1;
        Integer lastListenAyah = documentSnapshot.getLong("lastListenAyah") != null 
            ? documentSnapshot.getLong("lastListenAyah").intValue() : 1;
        
        Log.d(TAG, "🎧 Listening position found: Surah " + lastListenSurah + ", Ayah " + lastListenAyah);
        startQuranReaderWithAudio(context, lastListenSurah, lastListenAyah, config);
    } else {
        Log.d(TAG, "🎧 Listening position not found, using default (Surah 1, Ayah 1)");
        startQuranReaderWithAudio(context, 1, 1, config);
    }
})
```

---

## 🧪 测试验证

### 测试步骤：

1. **打开应用**，创建每日任务 "Quran Listening"，目标 5 分钟
2. **点击 "Go" 按钮**，播放到 **Surah 2, Verse 7**
3. **退出应用**（完全退出）
4. **重新打开应用**，点击每日任务 "Quran Listening" 的 **"Go" 按钮**
5. **验证**：应该从 **Surah 2, Verse 7** 开始播放 ✅

### 预期日志：

```
# 保存时（onPause）：
🎧 Saving LISTENING position: Surah 2, Ayah 7
✅ Learning state saved to Firestore: Surah 2, Ayah 7

# 读取时（再次打开）：
🎧 Fetching LISTENING UserLearningState from Firestore
🎧 Listening position found: Surah 2, Ayah 7
🔵 startQuranReaderWithAudio called: Surah 2, Ayah 7
🔵 Intent extra set: START_VERSE = 7

# 启动时（ActivityReader）：
🟢 preReaderReady: Received intent extras:
🟢   LISTENING_MODE = true
🟢   AUTO_PLAY_AUDIO = true
🟢   START_VERSE = 7

# 自动播放时：
🎧 AUTO_PLAY_AUDIO: Triggering automatic playback
🎧 Preparing auto-play: Surah 2, Verse 7
🎧 Executing auto-play: Surah 2, Verse 7
```

---

## 🎯 关键技术点

### 1. **为什么不需要 Ayat Timing Data？**

根据代码分析（`RecitationUtils.prepareAudioUrl()`）：
- ✅ 音频文件是**按 verse 分开的**（每个 verse 一个独立的 `.mp3` 文件）
- ✅ URL 格式：`.../surah_{chapter}/ayah_{verse}.mp3`
- ✅ `reciteVerse(ChapterVersePair(2, 7))` 会直接加载 `surah_2/ayah_7.mp3`
- ✅ **不需要 seekTo() 操作**

### 2. **为什么要独立字段？**

**场景**：用户同时使用阅读和听力功能
- **阅读模式**：用户浏览到 Surah 10, Verse 20
- **听力模式**：用户听到 Surah 2, Verse 7

如果共用字段：
- ❌ 两个模式会互相覆盖对方的进度
- ❌ 用户无法在不同模式下保持独立的进度

使用独立字段：
- ✅ 阅读进度和听力进度互不干扰
- ✅ 用户体验更好

---

## 📦 相关文件

1. `ActivityReader.java` - 主要的阅读器活动，处理阅读和听力模式
2. `DailyQuestsManager.java` - 管理每日任务的启动逻辑
3. `RecitationService.kt` - 音频播放服务
4. `QuranListeningTracker.java` - 听力时长追踪器

---

## 🏁 修复状态

- ✅ **已修复**：听力模式和阅读模式使用独立的 Firestore 字段
- ✅ **已修复**：DailyQuestsManager 读取正确的听力字段
- ✅ **已修复**：ActivityReader 保存到正确的听力字段
- ✅ **已验证**：音频文件按 verse 分开，不需要 Ayat Timing Data

---

## 📅 修复日期

2025-10-25

## 📌 版本信息

- **Version Code**: 44
- **Version Name**: 1.5.2

