# 🔥 Firestore 安全规则更新说明

## ✅ 新增规则：Feedback Submissions

### 规则位置
```
feedback_submissions/{feedbackId}
```

### 权限说明

#### ✅ Create（创建）
- **允许**: 任何已认证用户（包括匿名用户）
- **条件**: `request.auth != null`
- **用途**: 用户可以提交反馈，无需完整注册

#### ❌ Read（读取）
- **拒绝**: 所有普通用户
- **管理员**: 可通过 Firebase Console 或 Admin SDK 查看
- **原因**: 保护用户隐私

#### ❌ Update（更新）
- **拒绝**: 所有用户
- **原因**: 反馈一旦提交不可修改，保证数据完整性

#### ❌ Delete（删除）
- **拒绝**: 所有用户
- **原因**: 防止用户删除反馈记录，保证数据完整性

---

## 📋 完整规则列表

### 用户相关集合（需要用户认证）
1. ✅ `users/{userId}/learningPlan/{document}` - 学习计划配置
2. ✅ `users/{userId}/dailyProgress/{document}` - 每日进度
3. ✅ `users/{userId}/streakStats/{document}` - 连续记录统计
4. ✅ `users/{userId}/learningState/{document}` - 学习状态
5. ✅ `users/{userId}/tasbihData/{document}` - 念珠计数器数据
6. ✅ `users/{userId}/salahRecords/{dateId}` - 祷告记录（旧版）
7. ✅ `users/{userId}/qadaConfig/{document}` - Qada 追溯配置
8. ✅ `users/{userId}/unlocked_content/{contentId}` - 解锁内容记录
9. 🚫 `users/{userId}/userQuestConfig/{document}` - 旧版配置（向后兼容）
10. 🚫 `users/{userId}/userLearningState/{document}` - 旧版学习状态（向后兼容）

### 根集合（公共数据）
1. ✅ `prayer_logs/{logId}` - 祷告记录（新版，根集合）
2. 💬 `feedback_submissions/{feedbackId}` - **新增：用户反馈**

---

## 🔄 规则部署步骤

### 1. 在 Firebase Console 部署

1. 打开 [Firebase Console](https://console.firebase.google.com/)
2. 选择你的项目
3. 左侧菜单 → **Firestore Database**
4. 点击顶部 **Rules** 标签
5. 将上面的完整规则复制粘贴到编辑器中
6. 点击 **Publish** 按钮

### 2. 使用 Firebase CLI 部署

```bash
# 1. 将规则保存到项目根目录的 firestore.rules 文件

# 2. 部署规则
firebase deploy --only firestore:rules

# 或者先测试规则
firebase firestore:rules:test
```

---

## ✅ 规则验证清单

部署后，确认以下功能正常：

### 现有功能（不应受影响）
- [ ] 用户可以读写自己的学习计划数据
- [ ] 用户可以记录每日进度
- [ ] 祷告记录可以正常创建和查询
- [ ] Tafsir 解锁记录可以正常保存

### 新增功能
- [ ] 匿名用户可以提交反馈
- [ ] 反馈提交成功后在 Firebase Console 可见
- [ ] 普通用户无法读取其他用户的反馈
- [ ] 反馈一旦提交无法修改或删除

---

## 🧪 测试命令

### 测试反馈提交

在应用中提交反馈后，检查日志：

```bash
adb logcat | grep "FeedbackManager"
```

**成功日志应包含**:
```
FeedbackManager: ✅ Document saved successfully
FeedbackManager:    Document ID: K9fZm2x7D...
FeedbackManager:    Collection Path: feedback_submissions/K9fZm2x7D...
```

### 在 Firebase Console 验证

```
Firebase Console
→ Firestore Database
→ Data 标签
→ 查看 feedback_submissions 集合
→ 应该看到新提交的文档
```

---

## ⚠️ 重要注意事项

### 1. 默认拒绝规则必须在最后
```javascript
match /{document=**} {
  allow read, write: if false;
}
```
这条规则必须放在所有其他规则之后，作为"默认拒绝"的兜底策略。

### 2. 匿名认证必须启用
```
Firebase Console
→ Authentication
→ Sign-in method
→ Anonymous
→ ✅ Enable
```

如果未启用匿名认证，反馈提交会失败并显示：
```
PERMISSION_DENIED: Missing or insufficient permissions
```

### 3. 规则冲突检查

部署新规则前，Firebase 会自动检查规则语法和冲突。如果有问题，会在发布时提示：
- ✅ **绿色**: 规则有效
- ⚠️ **黄色**: 警告（可以发布，但需注意）
- ❌ **红色**: 错误（无法发布）

---

## 📈 数据查询示例

### 在 Firebase Console 查询反馈

1. Firestore Database → Data
2. 选择 `feedback_submissions` 集合
3. 查看所有反馈文档

### 按条件过滤（使用 Console 或 Admin SDK）

**按情绪统计**:
```javascript
// 仅在 Admin SDK 或 Console 中可用
db.collection('feedback_submissions')
  .where('emotion', '==', 'Poor')
  .get()
```

**按时间范围**:
```javascript
const startDate = new Date('2025-12-20');
const endDate = new Date('2025-12-27');

db.collection('feedback_submissions')
  .where('timestamp', '>=', startDate)
  .where('timestamp', '<=', endDate)
  .get()
```

---

## 🔒 安全性说明

### ✅ 安全的设计
1. **匿名用户隔离**: 每个匿名用户有唯一 UID，数据不会混淆
2. **写入验证**: 通过 `request.auth != null` 确保只有认证用户可写入
3. **读取保护**: 普通用户无法读取任何反馈，保护隐私
4. **数据不可变**: 提交后无法修改，防止恶意篡改

### ⚠️ 潜在风险与缓解措施

**风险**: 恶意用户可能大量提交垃圾反馈

**缓解措施**:
1. 客户端限流（应用中已实现 3 次重试上限）
2. 可在 Admin SDK 中实现监控和清理
3. 可添加每日提交次数限制（高级规则）

**高级限流规则示例**（可选）:
```javascript
match /feedback_submissions/{feedbackId} {
  allow create: if request.auth != null
                && request.time >= resource.data.lastSubmitTime + duration.value(1, 'm');
  // 限制每个用户最多每分钟提交 1 次
}
```

---

## 📝 变更历史

| 日期 | 版本 | 变更内容 |
|------|------|----------|
| 2025-12-26 | v1.1 | 新增 `feedback_submissions` 规则 |
| 2025-XX-XX | v1.0 | 初始规则（学习计划、祷告记录等） |

---

**规则已准备就绪，请在 Firebase Console 部署！** 🚀

