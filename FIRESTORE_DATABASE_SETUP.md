# 🔥 Firestore Database 创建指南

## 📅 问题发现时间
**2025-10-19 15:55**

---

## 🐛 问题诊断

### 错误信息
```
W Firestore: Stream closed with status: Status{code=NOT_FOUND, 
description=The database (default) does not exist for project quran-majeed-aa3d2
```

### 根本原因
**Firebase 项目中没有创建 Firestore 数据库**

您的应用连接到了 Firebase 项目 `quran-majeed-aa3d2`，但这个项目中没有启用 Firestore。

---

## ✅ 解决方案

### 方案 1: 在 Firebase Console 创建 Firestore 数据库（推荐）

#### 步骤 1: 访问 Firebase Console

1. **打开浏览器**，访问：
   ```
   https://console.firebase.google.com/
   ```

2. **登录** Google 账户（adochub@gmail.com）

3. **找到项目** `quran-majeed-aa3d2`
   - 如果看不到，点击左上角切换项目

#### 步骤 2: 创建 Firestore 数据库

1. **在左侧菜单中，点击 "Firestore Database"**

2. **点击 "创建数据库"（Create database）**

3. **选择位置**（推荐）:
   - 生产模式（Production mode）或 测试模式（Test mode）
   - **推荐测试模式**：30天内数据可读写，方便测试
   - 生产模式需要配置安全规则

4. **选择数据中心位置**：
   - 推荐：`asia-east1`（台湾）或 `asia-southeast1`（新加坡）
   - 原因：离中国大陆最近，延迟最低

5. **点击 "启用"（Enable）**
   - 等待 1-2 分钟创建完成

#### 步骤 3: 配置安全规则（如果选择了生产模式）

如果您选择了生产模式，需要配置安全规则：

1. **点击 "规则"（Rules）标签**

2. **粘贴以下规则**：
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // 允许已登录用户读写自己的数据
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // 其他数据拒绝访问
    match /{document=**} {
      allow read, write: if false;
    }
  }
}
```

3. **点击 "发布"（Publish）**

#### 步骤 4: 验证配置

1. **在 Firestore 控制台，点击 "数据"（Data）标签**
2. **应该看到空的数据库界面**（说明创建成功）

---

### 方案 2: 快速链接（直接跳转）

如果您想快速访问，点击此链接：

```
https://console.cloud.google.com/datastore/setup?project=quran-majeed-aa3d2
```

这会直接跳转到数据库设置页面。

---

### 方案 3: 检查是否有其他 Firebase 项目

可能您之前已经创建了另一个 Firebase 项目并配置好了 Firestore。

#### 步骤 1: 检查 google-services.json

```bash
cat app/google-services.json | grep project_id
```

应该显示：
```json
"project_id": "quran-majeed-aa3d2"
```

#### 步骤 2: 如果有其他项目

如果您还有另一个 Firebase 项目（比如 `quran-majeed-prod`）并且已经配置好了 Firestore：

1. **下载那个项目的 `google-services.json`**
2. **替换当前的 `google-services.json`**
3. **重新编译安装**

---

## 🧪 创建数据库后的测试

### Test 1: 验证数据库已创建

创建完成后，运行以下命令测试：

```bash
# 1. 清除应用数据
adb shell pm clear com.quran.quranaudio.online

# 2. 启动应用
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity

# 3. 查看日志
adb logcat -d | grep Firestore
```

**预期结果**:
- ✅ 不再出现 "database (default) does not exist" 错误
- ✅ 可能看到 "Firestore client initialized"

### Test 2: 测试保存功能

1. **打开应用**
2. **点击 Create Card**
3. **配置学习计划**
4. **登录** Google 账户
5. **点击 Save**

**预期结果**:
- ✅ 1-3秒内保存成功
- ✅ Toast 显示 "Learning plan saved successfully! ✅"
- ✅ 自动返回主页
- ✅ 显示 Streak Card + Today's Quests

### Test 3: 在 Firebase Console 验证数据

保存成功后，在 Firebase Console 中查看：

1. **打开 Firestore Database**
2. **点击 "数据" 标签**
3. **应该看到新的数据结构**：
   ```
   users/
     └── {userId}/
         ├── learningPlan/
         │   └── config/
         └── streakStats/
             └── summary/
   ```

---

## 🔧 安全规则详解

### 测试模式规则（30天）
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.time < timestamp.date(2025, 11, 18);
    }
  }
}
```
**说明**: 2025-11-18 之前所有人都可以读写，30天后自动禁止

### 生产模式规则（推荐）
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // 用户只能读写自己的数据
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```
**说明**: 只有登录用户可以读写自己的数据

---

## ⚠️ 重要提示

### 1. 数据库位置不可更改
一旦选择了数据中心位置（如 asia-east1），**不能再更改**。选择时请谨慎。

### 2. 测试模式会过期
测试模式的规则会在30天后自动禁止所有访问。记得在过期前切换到生产模式规则。

### 3. 安全规则很重要
如果使用生产模式，请务必配置安全规则，否则数据可能被恶意访问。

### 4. 成本
Firestore 有免费额度：
- 每天 50,000 次读取
- 每天 20,000 次写入
- 1 GB 存储

对于个人应用完全够用。

---

## 📊 预期的数据结构

创建学习计划后，Firestore 中的数据结构：

```
users/
  └── {userId}/  (例如: abc123def456)
      ├── learningPlan/
      │   └── config/
      │       ├── dailyReadingPages: 10
      │       ├── recitationEnabled: true
      │       ├── recitationMinutes: 15
      │       ├── duaReminderEnabled: true
      │       ├── tasbihReminderEnabled: true
      │       ├── tasbihCount: 50
      │       ├── totalChallengeDays: 33
      │       ├── startDate: "2025-10-19"
      │       ├── createdAt: Timestamp
      │       └── updatedAt: Timestamp
      │
      ├── dailyProgress/
      │   └── 2025-10-19/  (每天一个文档)
      │       ├── date: "2025-10-19"
      │       ├── task1ReadCompleted: false
      │       ├── task2TajweedCompleted: false
      │       ├── task3TasbihCompleted: false
      │       ├── allTasksCompleted: false
      │       └── completedAt: null
      │
      └── streakStats/
          └── summary/
              ├── currentStreak: 0
              ├── longestStreak: 0
              ├── totalDays: 0
              ├── lastCompletedDate: ""
              ├── monthlyGoal: 31
              ├── monthlyProgress: 0
              └── lastUpdatedAt: Timestamp
```

---

## 🚀 完成后的下一步

### 1. 立即测试
创建数据库后，立即测试保存功能

### 2. 验证数据
在 Firebase Console 查看保存的数据

### 3. 测试任务功能
- 点击 Go 按钮
- 完成任务
- 查看 Streak 更新

### 4. 继续开发
一切正常后，可以继续优化位置权限等其他功能

---

## 📞 如果遇到问题

### 问题 A: 创建数据库后仍然报错

**解决方案**:
1. 完全关闭应用
2. 清除应用数据：`adb shell pm clear com.quran.quranaudio.online`
3. 重新打开应用

### 问题 B: 无法访问 Firebase Console

**解决方案**:
1. 检查网络连接
2. 确认使用正确的 Google 账户
3. 尝试使用 VPN（如果在中国大陆）

### 问题 C: 提示权限不足

**解决方案**:
检查您的 Google 账户是否是该 Firebase 项目的所有者或编辑者

---

## ✅ 检查清单

完成以下步骤后，问题应该解决：

- [ ] 访问 Firebase Console
- [ ] 选择项目 quran-majeed-aa3d2
- [ ] 创建 Firestore Database
- [ ] 选择测试模式或生产模式
- [ ] 选择数据中心位置（asia-east1）
- [ ] 等待创建完成（1-2分钟）
- [ ] 配置安全规则（如果是生产模式）
- [ ] 重新测试应用保存功能
- [ ] 验证数据已保存到 Firestore

---

**创建指南**: Cursor AI Agent  
**问题**: Firestore 数据库不存在  
**解决方案**: 在 Firebase Console 创建数据库  
**预计时间**: 5分钟  
**最后更新**: 2025-10-19 15:55

