# 🔥 Deploy Firestore Rules - Prayer Logs Fix

## 🚨 问题
**错误信息**: `PERMISSION_DENIED: Missing or insufficient permissions`

**原因**: Firestore 安全规则中缺少 `prayer_logs` 集合的访问权限

---

## ✅ 解决方案：部署更新的安全规则

### 方法 1: 使用 Firebase Console（推荐，最简单）

#### 步骤：

1. **打开 Firebase Console**
   ```
   https://console.firebase.google.com/
   ```

2. **选择项目**
   - 项目名称: `quran-majeed-aa3d2`

3. **进入 Firestore Database**
   - 左侧菜单 → `Firestore Database`
   - 点击顶部 `Rules` 标签

4. **复制新规则**
   - 打开本地文件: `/Users/huwei/AndroidStudioProjects/quran0/firestore.rules`
   - 复制全部内容

5. **粘贴并发布**
   - 在 Firebase Console 的规则编辑器中**替换所有内容**
   - 点击右上角 `Publish` 按钮
   - 等待部署完成（通常 10-30 秒）

6. **验证**
   - 部署成功后，在应用中重试保存祷告记录
   - 应该不再出现 PERMISSION_DENIED 错误

---

### 方法 2: 使用 Firebase CLI（自动化）

#### 前提条件：
```bash
# 检查是否已安装 Firebase CLI
firebase --version

# 如果未安装，先安装
npm install -g firebase-tools

# 登录 Firebase
firebase login
```

#### 部署步骤：

1. **初始化项目**（如果首次使用）
   ```bash
   cd /Users/huwei/AndroidStudioProjects/quran0
   firebase init firestore
   
   # 选择：
   # - Use existing project: quran-majeed-aa3d2
   # - Firestore rules file: firestore.rules
   # - Don't overwrite existing rules
   ```

2. **部署规则**
   ```bash
   cd /Users/huwei/AndroidStudioProjects/quran0
   firebase deploy --only firestore:rules
   ```

3. **等待部署完成**
   ```
   ✔  Deploy complete!
   
   Firestore Rules:
     Released and active
   ```

---

## 📝 添加的新规则详解

### Prayer Logs 集合权限

```javascript
match /prayer_logs/{logId} {
  // 读取权限：只能读取自己的记录
  allow read: if request.auth != null && request.auth.uid == resource.data.userId;
  
  // 创建权限：
  // 1. 必须登录
  // 2. userId 必须是当前用户
  // 3. 必须包含必要字段
  allow create: if request.auth != null 
                && request.auth.uid == request.resource.data.userId
                && request.resource.data.keys().hasAll(['userId', 'prayerName', 'status', 'date'])
                && request.resource.data.userId is string
                && request.resource.data.prayerName is string
                && request.resource.data.status is string
                && request.resource.data.date is string;
  
  // 更新权限：只能更新自己的记录
  allow update: if request.auth != null 
                && request.auth.uid == resource.data.userId
                && request.auth.uid == request.resource.data.userId;
  
  // 删除权限：只能删除自己的记录
  allow delete: if request.auth != null && request.auth.uid == resource.data.userId;
}
```

### 安全特性

✅ **用户隔离**: 每个用户只能访问自己的祷告记录
✅ **数据验证**: 确保必填字段存在且类型正确
✅ **防止篡改**: 用户不能修改别人的记录
✅ **认证要求**: 未登录用户无法读写

---

## 🧪 测试

### 部署后测试步骤

1. **在设备上重新尝试保存祷告记录**
   - 打开应用
   - 点击祷告时间卡片上的记录按钮
   - 填写信息
   - 点击 Save

2. **预期结果**
   - ✅ 显示 "✅ [Prayer Name] prayer logged successfully"
   - ✅ 弹窗自动关闭
   - ✅ 数据保存到 Firestore

3. **验证数据（可选）**
   - Firebase Console → Firestore Database → Data 标签
   - 查看 `prayer_logs` 集合
   - 应该能看到新创建的文档

---

## ⚠️ 常见问题

### Q: 部署后仍然显示权限错误？

**A**: 可能的原因：
1. **规则尚未生效**: 等待 30 秒后重试
2. **用户未登录**: 确保已通过 Google Sign-In 登录
3. **缓存问题**: 完全关闭应用后重新打开
4. **规则复制不完整**: 检查 Firebase Console 中的规则是否完整

### Q: 如何验证规则是否部署成功？

**A**: 
```bash
# 方法 1: Firebase Console
# 查看 Rules 标签，应该能看到新添加的 prayer_logs 规则

# 方法 2: Firebase CLI
firebase firestore:rules
```

### Q: 为什么之前没有这个问题？

**A**: 
- `prayer_logs` 是新功能（v1.7.3）
- 之前没有这个集合，所以也不需要规则
- 现在添加了功能，必须同步添加安全规则

---

## 📊 部署检查清单

部署前：
- [ ] 已修改 `firestore.rules` 文件
- [ ] 规则包含 `prayer_logs` 权限配置
- [ ] 用户已通过 Google 登录

部署：
- [ ] 通过 Firebase Console 或 CLI 部署规则
- [ ] 看到部署成功消息

测试：
- [ ] 在应用中尝试保存祷告记录
- [ ] 不再出现 PERMISSION_DENIED 错误
- [ ] 数据成功保存到 Firestore
- [ ] 在 Firebase Console 中能看到数据

---

## 🎯 快速操作指南

**最快的修复方法**（2分钟）：

1. 访问: https://console.firebase.google.com/
2. 选择项目: `quran-majeed-aa3d2`
3. 点击 `Firestore Database` → `Rules`
4. 复制 `/Users/huwei/AndroidStudioProjects/quran0/firestore.rules` 的全部内容
5. 粘贴到 Firebase Console 的规则编辑器（替换全部）
6. 点击 `Publish`
7. 等待 10-30 秒
8. 在应用中重试保存

---

**部署完成后，祷告记录功能将正常工作！** 🚀

---

## 📚 相关文档

- Firestore 安全规则: https://firebase.google.com/docs/firestore/security/get-started
- Firebase CLI: https://firebase.google.com/docs/cli
- 项目文档: `PRAYER_LOG_FEATURE_IMPLEMENTATION.md`


