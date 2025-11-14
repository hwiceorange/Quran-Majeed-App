# 🔥 部署 Qada' Firestore 规则

## ❌ 当前问题
保存 Qada' 配置时出现错误：
```
Error: PERMISSION_DENIED: Missing or insufficient permissions.
```

## 🔍 原因分析
Firestore 规则文件已经包含了 `qadaConfig` 的权限配置（`firestore.rules` 第 94-96 行），但**规则还没有部署到 Firebase 服务器**。

## ✅ 解决方法：部署 Firestore 规则

### 方法 1：使用 Firebase Console（推荐）

1. **打开 Firebase Console**
   - 访问：https://console.firebase.google.com/
   - 选择您的项目

2. **进入 Firestore Database**
   - 左侧菜单 → **Firestore Database**
   - 点击顶部的 **规则（Rules）** 标签

3. **复制并粘贴规则**
   - 打开项目中的 `firestore.rules` 文件
   - 复制全部内容
   - 粘贴到 Firebase Console 的规则编辑器中

4. **发布规则**
   - 点击右上角的 **发布（Publish）** 按钮
   - 等待部署完成（通常几秒钟）

5. **验证部署**
   - 规则编辑器顶部会显示 "✅ 规则已发布" 消息
   - 可以看到发布时间戳

### 方法 2：使用 Firebase CLI（命令行）

```bash
# 1. 确保已安装 Firebase CLI
npm install -g firebase-tools

# 2. 登录 Firebase
firebase login

# 3. 在项目根目录初始化（如果还没有）
firebase init firestore

# 4. 部署 Firestore 规则
firebase deploy --only firestore:rules

# 5. 验证部署
firebase firestore:databases:list
```

## 📋 当前规则内容摘要

```javascript
// Qada Configuration (Qada 追溯配置)
// Path: users/{userId}/qadaConfig/{document}
match /users/{userId}/qadaConfig/{document=**} {
  allow read, write: if request.auth != null && request.auth.uid == userId;
}
```

这个规则允许：
- ✅ 已登录用户读取和写入自己的 Qada' 配置
- ❌ 未登录用户无法访问
- ❌ 用户无法访问其他用户的配置

## 🧪 测试部署是否成功

部署完成后，在应用中：

1. **打开 Salat 页面**
2. **点击 "Total Outstanding Qada" 卡片**
3. **选择起始日期**
   - 选择 "Start from Today" 或
   - 选择 "Start from: [自定义日期]"
4. **点击 "CONFIRM AND START TRACKING"**

**预期结果：**
- ✅ 不再出现 PERMISSION_DENIED 错误
- ✅ 显示成功消息
- ✅ Qada' 卡片显示正确的统计数据

**如果仍然失败：**
- 检查用户是否已登录（Firebase Auth）
- 检查网络连接
- 查看 Firebase Console 的 Firestore 规则是否正确部署
- 查看 logcat 日志获取详细错误信息

## 📱 完整的 Firestore 规则路径

当前项目使用以下 Firestore 集合：

| 集合路径 | 用途 | 规则状态 |
|---------|------|---------|
| `users/{userId}/learningPlan` | 学习计划配置 | ✅ 已配置 |
| `users/{userId}/dailyProgress` | 每日进度 | ✅ 已配置 |
| `users/{userId}/learningState` | 学习状态 | ✅ 已配置 |
| `users/{userId}/tasbihData` | 念珠数据 | ✅ 已配置 |
| `users/{userId}/salahRecords` | 祷告记录（旧版） | ✅ 已配置 |
| `users/{userId}/qadaConfig` | **Qada' 配置** | ✅ 已配置 (需部署) |
| `prayer_logs/{logId}` | 祷告记录（新版） | ✅ 已配置 |

## ⚠️ 注意事项

1. **部署时间**：规则部署通常需要几秒到1分钟
2. **缓存**：如果立即测试仍然失败，等待30秒后重试
3. **备份**：部署前建议备份当前规则
4. **测试环境**：建议先在测试项目中验证规则

## 🔗 相关文档

- Firebase Firestore 安全规则：https://firebase.google.com/docs/firestore/security/get-started
- Firebase CLI 文档：https://firebase.google.com/docs/cli





