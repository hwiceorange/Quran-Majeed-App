# 📱 Quran App v1.7.2 - 测试指南

## 版本信息
- **版本号**: v1.7.2 (64)
- **构建日期**: 2025-11-05
- **主要更新**: 
  1. 订阅功能（Billing Library 7.1.1）
  2. 祷告记录功能（Prayer Log）
  3. Google 登录修复
  4. 语言翻译映射（69 种语言）

---

## 🧪 测试清单

### 1. ✅ Google 登录功能

**修复内容**: Debug 版本使用 Release 签名，解决 SHA-1 不匹配问题

**测试步骤**:
1. 打开应用
2. 进入 Settings → Login 或点击 Daily Quests
3. 点击 "Login with Google"
4. 选择 Google 邮箱
5. **期望结果**: 
   - ✅ 成功登录
   - ✅ 显示 "Login successful!"
   - ✅ 不再显示 "Sign-in canceled"

**验证登录状态**:
- 用户头像显示
- 用户名/邮箱显示
- Daily Quests 功能可用

---

### 2. 🕌 祷告记录功能（新功能）

**功能说明**: 点击 Salat 页面的 Track 按钮记录祷告

**测试步骤**:
1. 导航到 Salat 页面（底部导航）
2. 点击任一祷告的 "Track" 按钮（如 Asr）
3. **验证 Bottom Sheet 弹出**:
   - ✅ 标题: "Log Prayer"
   - ✅ Prayer 显示: "Asr"
   - ✅ Status 默认选中: "Ada'"（浅绿色背景）
   - ✅ Prayed At: 显示当前时间
   - ✅ Recorded At: 显示 UTC 时间
4. **测试交互**:
   - 切换状态（Ada'/Qada'/Missed）
   - 修改时间（点击 "4:30 PM ▾"）
   - 点击快捷标签（+ At Mosque）
   - 输入自定义备注
5. 点击 "Save"
6. **期望结果**:
   - ✅ Toast: "✅ Asr prayer logged successfully"
   - ✅ Bottom Sheet 自动关闭
   - ✅ 数据保存到 Firestore

**验证 Firestore 数据**:
- Firebase Console → Firestore → prayer_logs
- 查看文档包含: prayerName, status, performedAt, loggedAt, notes, date

---

### 3. 📱 订阅功能

**测试步骤**:
1. 进入 Settings → Go Premium
2. **验证 UI**:
   - ✅ 头图与背景无缝融合
   - ✅ Enable Free trial 卡片清晰可见
   - ✅ 价格套餐无遮挡
3. 测试交互:
   - 切换年度/月度套餐
   - 开关 Enable Free trial
   - 验证单选按钮（白色）
4. **验证错误提示**（如未配置产品）:
   - 应该显示详细的配置指导对话框
   - 包含 Product IDs 和配置步骤

---

### 4. 🌍 语言与翻译

**已支持的语言**: 7 种
- 🇬🇧 English → Sahih International
- 🇮🇩 Indonesian → Ministry Translation
- 🇵🇰 Urdu → Junagarhi
- 🇸🇦 Arabic → 原文
- 🇲🇾 Malay → Abdullah Basmeih
- 🇹🇷 Turkish → Diyanet
- 🇧🇩 Bengali → Taisirul Quran

**测试**:
1. Settings → Language
2. 切换语言
3. 验证界面语言变化
4. 验证古兰经翻译自动切换

---

## 🔍 调试工具

### 实时日志查看

```bash
# Google 登录日志
adb logcat | grep -E "(GoogleAuthManager|FirebaseAuth|Sign-in)"

# 祷告记录日志
adb logcat | grep -E "(PrayerLog|PrayersFragment|BottomSheet)"

# 订阅功能日志
adb logcat | grep -E "(BillingManager|SubscriptionActivity)"

# 所有关键日志
adb logcat | grep -E "(Google|Prayer|Billing|Subscription)"
```

### 清除应用数据

```bash
# 完全重置应用（测试首次启动）
adb shell pm clear com.quran.quranaudio.online
```

---

## ⚠️ 已知问题

### 1. 订阅产品未配置
- **现象**: "No subscription plans available"
- **原因**: Google Play Console 未配置产品
- **解决**: 参考 `SUBSCRIPTION_SETUP_GUIDE.md`

### 2. 签名变更影响
- **现象**: 无法覆盖安装之前的 Debug 版本
- **原因**: Debug 现在使用 Release 签名
- **解决**: 先卸载再安装（已自动处理）

---

## 📚 相关文档

### Google 登录
- `GOOGLE_LOGIN_FIX_IMMEDIATE.md` - 立即修复方案
- `GOOGLE_SIGN_IN_TROUBLESHOOTING_COMPLETE.md` - 完整故障排查

### 祷告记录
- `PRAYER_LOG_FEATURE_IMPLEMENTATION.md` - 功能实施文档

### 订阅功能
- `SUBSCRIPTION_SETUP_GUIDE.md` - 配置指南
- `RELEASE_NOTES_v1.7.2.md` - 版本说明

### 语言翻译
- `COMPLETE_TRANSLATIONS_LIST_69_LANGUAGES.md` - 69 种语言完整列表
- `NEW_USER_LANGUAGE_SELECTION_GUIDE.md` - 语言选择实施指南
- `LANGUAGE_TRANSLATION_QUICK_REFERENCE.md` - 快速参考

---

## ✅ 测试完成标志

- [ ] Google 登录成功
- [ ] 祷告记录功能正常
- [ ] 订阅页面 UI 正确
- [ ] 语言切换正常
- [ ] 所有功能无崩溃

---

**🎉 v1.7.2 已准备就绪，请按照指南测试所有功能！**
