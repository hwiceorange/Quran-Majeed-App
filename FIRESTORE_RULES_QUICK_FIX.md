# 🚨 Prayer Logs 权限错误快速修复

## 错误信息
```
Failed to save: PERMISSION_DENIED: Missing or insufficient permissions
```

---

## ✅ 2分钟快速修复

### 步骤 1: 打开 Firebase Console

访问: https://console.firebase.google.com/

登录并选择项目: **quran-majeed-aa3d2**

---

### 步骤 2: 进入 Firestore 规则编辑器

1. 点击左侧菜单 **Firestore Database**
2. 点击顶部 **Rules** 标签
3. 你会看到当前的安全规则

---

### 步骤 3: 添加新规则

在现有规则中，找到这部分：

```javascript
    match /users/{userId}/salahRecords/{dateId} {
      // ... 现有规则 ...
    }
    
    // ========================================
    // Default Deny All Other Collections
    // ========================================
    match /{document=**} {
      allow read, write: if false;
    }
```

**在 "Default Deny" 之前**，添加以下新规则：

```javascript
    // ========================================
    // 🕌 Prayer Logs (Individual Prayer Records) - NEW!
    // ========================================
    match /prayer_logs/{logId} {
      // Allow authenticated users to read and write their own prayer logs
      allow read: if request.auth != null && request.auth.uid == resource.data.userId;
      
      // Allow create if authenticated and userId matches
      allow create: if request.auth != null 
                    && request.auth.uid == request.resource.data.userId
                    && request.resource.data.keys().hasAll(['userId', 'prayerName', 'status', 'date'])
                    && request.resource.data.userId is string
                    && request.resource.data.prayerName is string
                    && request.resource.data.status is string
                    && request.resource.data.date is string;
      
      // Allow update only for own records
      allow update: if request.auth != null 
                    && request.auth.uid == resource.data.userId
                    && request.auth.uid == request.resource.data.userId;
      
      // Allow delete only for own records
      allow delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }
```

---

### 步骤 4: 发布规则

1. 点击右上角绿色 **Publish** 按钮
2. 等待部署完成（10-30秒）
3. 看到 "Rules deployed successfully" 提示

---

### 步骤 5: 测试

1. **在设备上重新打开应用**
2. **尝试保存祷告记录**
3. **应该成功显示**: "✅ [Prayer] prayer logged successfully"

---

## 🔍 完整规则文件（可选：完全替换）

如果你想完全替换规则文件，复制以下完整内容到 Firebase Console：

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // ========================================
    // Daily Quests - User Quest Configurations
    // ========================================
    match /users/{userId}/userQuestConfig/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // Daily Quests - Daily Progress
    // ========================================
    match /users/{userId}/dailyProgress/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // Daily Quests - Streak Stats
    // ========================================
    match /users/{userId}/streakStats/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // Daily Quests - User Learning State
    // ========================================
    match /users/{userId}/userLearningState/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // ========================================
    // 📿 Salah Records (Prayer Tracking)
    // ========================================
    match /users/{userId}/salahRecords/{dateId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
      
      allow create: if request.auth != null 
                    && request.auth.uid == userId
                    && request.resource.data.keys().hasAll(['userId', 'dateId', 'fajr', 'dhuhr', 'asr', 'maghrib', 'isha'])
                    && request.resource.data.userId == userId
                    && request.resource.data.dateId is string
                    && request.resource.data.fajr is bool
                    && request.resource.data.dhuhr is bool
                    && request.resource.data.asr is bool
                    && request.resource.data.maghrib is bool
                    && request.resource.data.isha is bool;
      
      allow update: if request.auth != null 
                    && request.auth.uid == userId
                    && request.resource.data.userId == userId;
    }
    
    // ========================================
    // 🕌 Prayer Logs (Individual Prayer Records) - NEW!
    // ========================================
    match /prayer_logs/{logId} {
      // Allow authenticated users to read and write their own prayer logs
      allow read: if request.auth != null && request.auth.uid == resource.data.userId;
      
      // Allow create if authenticated and userId matches
      allow create: if request.auth != null 
                    && request.auth.uid == request.resource.data.userId
                    && request.resource.data.keys().hasAll(['userId', 'prayerName', 'status', 'date'])
                    && request.resource.data.userId is string
                    && request.resource.data.prayerName is string
                    && request.resource.data.status is string
                    && request.resource.data.date is string;
      
      // Allow update only for own records
      allow update: if request.auth != null 
                    && request.auth.uid == resource.data.userId
                    && request.auth.uid == request.resource.data.userId;
      
      // Allow delete only for own records
      allow delete: if request.auth != null && request.auth.uid == resource.data.userId;
    }
    
    // ========================================
    // Default Deny All Other Collections
    // ========================================
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

---

## 📱 关于"数据库表"的说明

**重要**: Firestore 是 **NoSQL** 数据库，不需要预先创建"表"。

### Firestore 的工作方式：

1. **自动创建集合**: 当你第一次保存文档时，集合自动创建
2. **无需 Schema**: 不需要定义字段结构
3. **安全规则控制**: 权限通过安全规则控制，不是表存在性

### 为什么出现 PERMISSION_DENIED？

```
❌ 不是因为: 表不存在
❌ 不是因为: 数据未同步
✅ 是因为: Firestore 安全规则没有允许写入 prayer_logs
```

### 数据保存流程：

```
用户点击 Save
    ↓
应用尝试写入 Firestore
    ↓
Firestore 检查安全规则
    ↓
如果规则允许 → ✅ 保存成功，集合自动创建
如果规则拒绝 → ❌ PERMISSION_DENIED
```

---

## 🎯 总结

### 问题根源
- ✅ 代码正确
- ✅ 用户已登录
- ✅ 数据格式正确
- ❌ **Firestore 安全规则缺失** ← 唯一问题

### 解决方案
1. 在 Firebase Console 添加 `prayer_logs` 权限规则
2. 发布规则
3. 重试保存

### 预计修复时间
- **2-3 分钟**（手动在 Console 操作）

---

**按照步骤操作后，问题即可解决！** 🚀


