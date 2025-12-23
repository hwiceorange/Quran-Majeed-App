# 🧪 祷告状态功能测试指南

**版本**: v1.7.4 (Build 66)  
**测试日期**: 2025-11-06  
**修复内容**: onResume() 刷新祷告状态

---

## 🎯 测试目标

验证祷告的 4 种状态是否正确显示和交互：

| 状态 | 图标 | 颜色 | 点击行为 |
|------|------|------|----------|
| ✅ **Ada' (准时完成)** | 白色圆圈+绿色打勾 | 绿色 | 进入编辑（可改 Qada'） |
| ⚠️ **Qada' (已弥补)** | 橙色警告三角 | 橙色 | 进入编辑（可改时间/备注） |
| ❌ **Missed (错过)** | 红色错误叉号 | 红色 | 立即进入 Qada' 对话框 |
| 📝 **Pending (待记录)** | TRACK 按钮 | 绿色 | 进入新建对话框 |

---

## 📱 测试步骤

### 准备工作
1. ✅ 确保应用已安装最新版本
2. ✅ 确保已登录 Google 账号
3. ✅ 打开 **Salat** 页面

---

### 测试 1: Pending → Ada' 状态 ✅

**操作步骤**:
1. 在 Salat 页面找到未记录的祷告（显示绿色 **TRACK** 按钮）
2. 点击 **TRACK** 按钮
3. 在弹出的对话框中：
   - 选择 **Ada** (准时)
   - 点击 **Save**

**预期结果**:
- ✅ TRACK 按钮消失
- ✅ 显示白色圆圈背景 + 绿色打勾图标
- ✅ 图标清晰可见

**如何验证**:
- [ ] 能看到绿色打勾图标
- [ ] 打勾图标在白色圆圈内
- [ ] 点击图标可进入编辑模式

**日志验证**:
```
✅ Fajr: Ada' (green check circle) - UPDATED
```

---

### 测试 2: Ada' → Qada' 状态 ⚠️

**操作步骤**:
1. 点击 Ada' 状态的图标（✅白色圆圈+绿色打勾）
2. 在编辑对话框中：
   - 改选 **Qada** (已弥补)
   - 点击 **Save**

**预期结果**:
- ⚠️ 绿色打勾消失
- ⚠️ 显示橙色警告三角图标
- ⚠️ 图标颜色为橙色 (#FF9800)

**如何验证**:
- [ ] 能看到橙色三角警告图标
- [ ] 点击图标可再次编辑
- [ ] 可以修改时间和备注

**日志验证**:
```
⚠️ Fajr: Qada' (orange warning) - UPDATED
```

---

### 测试 3: Qada' → Missed 状态 ❌

**操作步骤**:
1. 点击 Qada' 状态的图标（⚠️橙色警告）
2. 在编辑对话框中：
   - 改选 **Missed** (错过)
   - 点击 **Save**

**预期结果**:
- ❌ 橙色警告消失
- ❌ 显示红色错误叉号图标
- ❌ 图标颜色为红色 (#F44336)

**如何验证**:
- [ ] 能看到红色叉号图标
- [ ] 图标颜色明显比 Ada' 和 Qada' 暗淡

**日志验证**:
```
❌ Fajr: Missed (red error) - UPDATED
```

---

### 测试 4: Missed 点击行为 🔄

**操作步骤**:
1. 点击 Missed 状态的图标（❌红色错误）
2. 观察弹出的对话框

**预期结果**:
- 📝 立即弹出记录对话框
- 📝 默认选择 **Qada** 状态
- 📝 可以创建新的 Qada' 记录

**如何验证**:
- [ ] 对话框自动打开
- [ ] Qada' 按钮已被选中
- [ ] 保存后状态变为 Qada' (橙色警告)

**日志验证**:
```
❌ Missed state - showing Qada' log dialog
```

---

### 测试 5: 跨页面刷新 🔄

**操作步骤**:
1. 在 Salat 页面记录一个祷告（任意状态）
2. 切换到 **Quran** 页面
3. 再切换回 **Salat** 页面
4. 观察祷告状态是否保持

**预期结果**:
- ✅ 祷告状态正确显示
- ✅ 不需要重启应用

**如何验证**:
- [ ] 之前记录的状态仍然显示
- [ ] 图标类型和颜色正确
- [ ] 可以正常点击交互

**日志验证**:
```
🔄 onResume: Reloading prayer logs
🔍 loadTodayPrayerLogs() called
```

---

### 测试 6: 完整流程测试 🎯

**操作步骤**:
1. Pending → Ada' → Qada' → Missed → Qada' (新记录)
2. 每次状态转换后验证：
   - 图标是否正确
   - 颜色是否正确
   - 点击是否正确

**预期结果**:
- 📝 → ✅ → ⚠️ → ❌ → ⚠️
- 每个状态转换流畅
- UI 立即更新

**如何验证**:
- [ ] 所有状态转换成功
- [ ] 没有崩溃或错误
- [ ] 数据正确保存到 Firestore

---

## 🔍 监控日志

### 方法 1: 使用测试脚本（推荐）
```bash
cd /Users/huwei/AndroidStudioProjects/quran0
./test_prayer_status.sh
```

### 方法 2: 手动监控
```bash
adb logcat | grep -E "PrayersFragment|updatePrayerStatusUI|onSalahTrackClicked"
```

### 关键日志标记

**UI 更新**:
```
🎨 updatePrayerStatusUI called for FAJR
✅ FAJR: Ada' (green check circle) - UPDATED
⚠️ FAJR: Qada' (orange warning) - UPDATED
❌ FAJR: Missed (red error) - UPDATED
📝 FAJR: Pending (Track button) - UPDATED
```

**点击事件**:
```
🔘 Prayer clicked: Fajr
📝 Pending state - showing new log dialog
✅ Ada' state - showing edit dialog
⚠️ Qada' state - showing edit dialog
❌ Missed state - showing Qada' log dialog
```

**数据加载**:
```
🔍 loadTodayPrayerLogs() called
📥 Callback received with X logs
🔄 onResume: Reloading prayer logs
```

---

## ✅ 测试检查清单

### 基本功能
- [ ] Pending 状态显示 TRACK 按钮
- [ ] Ada' 状态显示绿色打勾图标
- [ ] Qada' 状态显示橙色警告图标
- [ ] Missed 状态显示红色错误图标

### 点击行为
- [ ] Pending 点击 → 新建对话框
- [ ] Ada' 点击 → 编辑对话框
- [ ] Qada' 点击 → 编辑对话框
- [ ] Missed 点击 → Qada' 对话框（默认选中 Qada'）

### 状态转换
- [ ] Pending → Ada' ✅
- [ ] Ada' → Qada' ✅
- [ ] Qada' → Missed ✅
- [ ] Missed → Qada' (新记录) ✅

### 数据持久化
- [ ] 状态保存到 Firestore
- [ ] 切换页面后状态保持
- [ ] 重启应用后状态保持

### UI/UX
- [ ] 图标清晰可见
- [ ] 颜色区分明显
- [ ] 动画流畅
- [ ] 无崩溃或错误

---

## 🐛 常见问题排查

### 问题 1: 状态不更新
**症状**: 点击保存后，图标没有变化

**排查**:
1. 检查是否已登录
2. 查看日志是否有 `loadTodayPrayerLogs()` 调用
3. 检查 Firestore 权限

**解决**:
```bash
# 查看日志
adb logcat | grep "PrayersFragment"
```

### 问题 2: 图标不显示
**症状**: 保存后仍显示 TRACK 按钮

**排查**:
1. 检查 `updatePrayerStatusUI` 日志
2. 验证图标资源文件存在
3. 检查布局文件中的 ImageView

**解决**:
```bash
# 验证资源文件
ls -la app/src/main/res/drawable/ic_check_circle.xml
ls -la app/src/main/res/drawable/ic_warning.xml
ls -la app/src/main/res/drawable/ic_error.xml
```

### 问题 3: 点击无响应
**症状**: 点击图标/按钮没有反应

**排查**:
1. 检查是否有 `onSalahTrackClicked` 日志
2. 验证点击事件绑定
3. 检查是否被其他视图遮挡

**解决**:
```bash
# 查看点击事件日志
adb logcat | grep "Prayer clicked"
```

---

## 📊 测试结果模板

### 测试报告
- **测试人员**: ___________
- **测试日期**: ___________
- **设备型号**: ___________
- **Android版本**: ___________

### 测试结果
| 测试项 | 状态 | 备注 |
|--------|------|------|
| Pending 状态 | ⬜ Pass / ⬜ Fail | |
| Ada' 状态 | ⬜ Pass / ⬜ Fail | |
| Qada' 状态 | ⬜ Pass / ⬜ Fail | |
| Missed 状态 | ⬜ Pass / ⬜ Fail | |
| Missed 点击 | ⬜ Pass / ⬜ Fail | |
| 跨页面刷新 | ⬜ Pass / ⬜ Fail | |
| 完整流程 | ⬜ Pass / ⬜ Fail | |

### 发现的问题
1. ___________________________________________
2. ___________________________________________
3. ___________________________________________

---

## 🎯 测试完成标准

✅ 所有 7 个测试项全部通过  
✅ 所有检查清单项目全部勾选  
✅ 没有崩溃或严重错误  
✅ UI 显示正确且流畅  
✅ 数据正确保存到 Firestore

---

**测试指南版本**: v1.0  
**最后更新**: 2025-11-06


