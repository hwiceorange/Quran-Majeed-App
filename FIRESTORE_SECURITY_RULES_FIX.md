# Firestore安全规则配置 - 解锁内容功能

## ❌ 问题诊断

从日志中发现的错误：
```
com.google.firebase.firestore.FirebaseFirestoreException: PERMISSION_DENIED: Missing or insufficient permissions.
```

**原因：** Firestore安全规则未配置，不允许用户写入 `unlocked_content` 数据。

---

## ✅ 解决方案：添加Firestore安全规则

### 步骤1：打开Firebase Console

1. 访问 [Firebase Console](https://console.firebase.google.com/)
2. 选择你的项目（Quran0）
3. 左侧菜单选择 **Firestore Database**
4. 点击顶部的 **规则（Rules）** 选项卡

### 步骤2：添加安全规则

将以下规则添加到Firestore安全规则中：

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // 现有的 prayer_logs 规则（如果有的话，保留）
    match /prayer_logs/{logId} {
      allow read, write: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    // ✅ 新增：unlocked_content 规则
    match /users/{userId}/unlocked_content/{contentId} {
      // 用户只能读写自己的解锁记录
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // 如果有其他规则，也保留在这里
  }
}
```

### 步骤3：发布规则

1. 点击 **发布（Publish）** 按钮
2. 确认更改

---

## 📋 完整的Firestore安全规则示例

如果你想要一个更完整的规则配置，可以使用这个：

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ========================================
    // 用户数据规则
    // ========================================
    
    // Prayer logs - 祷告记录
    match /prayer_logs/{logId} {
      allow read: if request.auth != null && request.auth.uid == resource.data.userId;
      allow create: if request.auth != null && request.auth.uid == request.resource.data.userId;
      allow update, delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    // Unlocked content - 解锁内容记录
    match /users/{userId}/unlocked_content/{contentId} {
      // 允许用户读写自己的解锁记录
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      // 数据验证：确保contentId字段存在
      allow create: if request.auth != null 
                    && request.auth.uid == userId
                    && request.resource.data.contentId is string
                    && request.resource.data.unlockedBy is string
                    && request.resource.data.timestamp is timestamp;
    }
    
    // 用户配置
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // 公共只读数据（如果有的话）
    // ========================================
    
    // Quran translations, tafsirs (只读)
    match /translations/{translationId} {
      allow read: if true;
    }
    
    match /tafsirs/{tafsirId} {
      allow read: if true;
    }
  }
}
```

---

## 🧪 测试步骤

### 1. 发布规则后，重新测试应用

```bash
# 重新安装应用（清除缓存）
adb uninstall com.quran.quranaudio.online
adb install app/build/outputs/apk/debug/app-debug.apk

# 启动日志监控
adb logcat | grep -E "ActivityTafsir|UnlockedContentRepo"
```

### 2. 测试解锁流程

1. ✅ 打开任意Tafsir页面
2. ✅ 点击"Watch Ad to Unlock"按钮
3. ✅ 观看完整广告
4. ✅ 查看日志，应该看到：

**成功日志：**
```
D UnlockedContentRepo: 🔐 Attempting to unlock content:
D UnlockedContentRepo:   - userId: A79QknedAnhVr13MTkRKm1nRXxq1
D UnlockedContentRepo:   - surahId: 1
D UnlockedContentRepo:   - ayahId: 1
D UnlockedContentRepo:   - contentId: 1:1
D UnlockedContentRepo: 💾 Saving to Firestore: UnlockedContent(...)
D UnlockedContentRepo: ✅ Successfully saved to Firestore with ID: xxx
D ActivityTafsir: 📝 Firestore save result: true
D ActivityTafsir: ✅ Content unlocked successfully in Firestore
D ActivityTafsir: ✅ UI updated, overlay should be hidden now
```

**不应该再看到：**
```
❌ PERMISSION_DENIED: Missing or insufficient permissions.
```

### 3. 验证数据已保存

在Firebase Console中：
1. 进入 **Firestore Database**
2. 导航到 `users/{your-user-id}/unlocked_content`
3. 应该能看到新创建的文档

---

## 🔍 检查现有规则

如果你不确定当前的Firestore规则，可以：

1. 在Firebase Console的Firestore Rules选项卡查看
2. 确保没有覆盖现有的重要规则
3. 如果有 `prayer_logs` 相关规则，保留它们

---

## 📊 Firestore数据结构（供参考）

配置规则后，数据将保存为：

```
Firestore
└── users/
    └── {userId}/
        └── unlocked_content/
            └── {auto-generated-id}/
                ├── contentId: "1:1"
                ├── unlockedBy: "REWARDED_AD"
                └── timestamp: Timestamp(...)
```

---

## ⚠️ 重要提示

1. **不要使用测试模式规则（`allow read, write: if true;`）** - 这会让所有人都能访问所有数据
2. **确保用户已登录** - 规则中使用了 `request.auth.uid`，需要用户认证
3. **如果是生产环境** - 考虑添加更严格的数据验证规则

---

## 🎯 下一步

配置好Firestore规则后：

1. ✅ 重新测试应用
2. ✅ 验证解锁功能正常工作
3. ✅ 检查其他三个修复（滚动限制、UI布局、透明度）是否也正常

---

**配置完成后，所有功能应该完全正常工作！** 🚀

