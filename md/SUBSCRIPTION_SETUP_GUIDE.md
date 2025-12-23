# 📱 订阅功能配置完整指南

## 🎯 问题说明

当前订阅页面显示以下错误：
- ❌ **"No subscription plans available"** - 进入页面时
- ❌ **"Selected plan is not available"** - 点击按钮时

## 🔍 根本原因

这是因为 **Google Play Console 中还未配置订阅产品**，或配置未生效。

---

## ✅ 完整配置步骤

### 📋 第一步：创建订阅产品

1. **登录 Google Play Console**
   - 访问：https://play.google.com/console
   - 选择您的应用

2. **进入订阅产品页面**
   ```
   应用 → 通过应用内产品获利 → 订阅
   ```

3. **创建月度订阅**
   - 点击 **"创建订阅"**
   - 产品 ID：**`plan_monthly`** （必须完全匹配！）
   - 名称：`Monthly Premium Subscription`
   - 说明：`Get premium features with monthly subscription`
   - 价格：`$2.99` (或您期望的价格)
   - 订阅周期：`1 个月`
   - 免费试用期：`7 天`（可选）
   - 状态：设置为 **"有效"**

4. **创建年度订阅**
   - 点击 **"创建订阅"**
   - 产品 ID：**`plan_year`** （必须完全匹配！）
   - 名称：`Yearly Premium Subscription`
   - 说明：`Get premium features with yearly subscription and save 50%`
   - 价格：`$17.99` (或您期望的价格)
   - 订阅周期：`1 年`
   - 状态：设置为 **"有效"**

---

### 🚀 第二步：发布应用到测试轨道

**重要：订阅功能只能在发布到测试轨道后才能测试！**

1. **构建签名的 Release 版本**
   ```bash
   cd /Users/huwei/AndroidStudioProjects/quran0
   ./gradlew bundleRelease
   ```

2. **上传到 Google Play Console**
   - 进入：`发布 → 测试 → 内部测试` (或封闭式测试)
   - 点击 **"创建新版本"**
   - 上传 AAB 文件：`app/build/outputs/bundle/release/app-release.aab`
   - 填写版本说明
   - 点击 **"审核并发布"**

3. **添加测试账号**
   - 进入：`测试 → 内部测试 → 测试人员`
   - 点击 **"创建列表"**
   - 添加测试 Gmail 账号（用于测试的 Google 账号）
   - 保存

---

### ⏰ 第三步：等待生效

**关键：配置需要时间传播！**

- 📋 **产品创建后**：等待 **1-2 小时**
- 🚀 **应用发布后**：等待 **2-4 小时**
- 🌍 **全球生效**：最多 **24 小时**

**在此期间，您会看到 "No subscription plans available" 错误，这是正常的！**

---

### 📱 第四步：测试订阅功能

1. **安装应用**
   - 使用测试账号登录 Google Play Store
   - 通过内部测试链接安装应用
   - **或** 通过测试轨道直接安装

2. **验证产品加载**
   - 打开应用
   - 进入 Settings → Go Premium
   - 查看 Logcat 日志：
     ```
     adb logcat | grep -i "SubscriptionActivity\|BillingManager"
     ```

3. **期望的日志输出**
   ```
   ✅ Billing setup successful
   🔍 Querying subscription products...
   ✅ Found 2 products
   📦 Product: plan_monthly
   💰 Price: $2.99
   📦 Product: plan_year
   💰 Price: $17.99
   ✅ Products loaded successfully
   ```

4. **测试购买流程**
   - 选择套餐
   - 点击 "GO PREMIUM NOW"
   - Google Play 弹出支付界面
   - **测试账号不会真实扣费！**

---

## 🔧 故障排查

### ❌ 问题 1："No subscription plans available"

**可能原因：**
1. ⏰ 产品配置未生效（等待 1-2 小时）
2. 📋 产品 ID 不匹配
3. ⚙️ 产品状态不是"有效"
4. 🚀 应用未发布到测试轨道
5. 📱 使用的是 debug 版本（应使用 release 版本）

**解决方法：**
```bash
# 1. 检查 Product IDs
# 确保 Google Play Console 中的产品 ID 完全匹配：
# - plan_monthly
# - plan_year

# 2. 检查应用签名
keytool -list -v -keystore ~/.android/debug.keystore

# 3. 构建 Release 版本
./gradlew bundleRelease

# 4. 查看详细日志
adb logcat -s BillingManager:D SubscriptionActivity:D
```

---

### ❌ 问题 2："Selected plan is not available"

**可能原因：**
1. 📦 只有一个产品配置成功（另一个未生效）
2. 🔄 产品信息未正确加载

**解决方法：**
- 查看日志确认哪个产品加载成功
- 尝试选择另一个套餐
- 点击错误对话框的 "Retry" 按钮重新加载

---

### ❌ 问题 3：Google Play 显示 "Item not available"

**可能原因：**
1. 🚀 应用未发布到测试轨道
2. 👤 测试账号未添加到测试人员列表
3. ⏰ 配置未完全生效

**解决方法：**
1. 确认应用已发布到内部测试轨道
2. 确认测试 Gmail 账号已添加
3. 确认测试设备使用该账号登录 Play Store
4. 等待 2-4 小时后重试

---

## 📊 检查清单

在测试前，请确认以下所有项目：

### Google Play Console
- [ ] 创建了 `plan_monthly` 订阅产品
- [ ] 创建了 `plan_year` 订阅产品
- [ ] 两个产品状态都是 **"有效"**
- [ ] 产品 ID 完全匹配（区分大小写）
- [ ] 已设置价格和周期
- [ ] 已添加产品说明

### 应用发布
- [ ] 构建了签名的 Release 版本 (AAB)
- [ ] 上传到内部测试（或封闭式测试）轨道
- [ ] 发布状态为 **"已发布"**
- [ ] 已等待 2-4 小时

### 测试配置
- [ ] 添加了测试 Gmail 账号
- [ ] 测试设备使用该账号登录 Play Store
- [ ] 通过测试轨道链接安装应用
- [ ] 网络连接正常

### 代码配置
- [ ] `BillingManager.MONTHLY_PLAN_ID` = `"plan_monthly"`
- [ ] `BillingManager.YEARLY_PLAN_ID` = `"plan_year"`
- [ ] 应用已正确集成 Google Play Billing Library
- [ ] 已添加必要权限 `com.android.vending.BILLING`

---

## 🎯 快速测试命令

```bash
# 1. 查看实时日志
adb logcat -s BillingManager:* SubscriptionActivity:* | grep -i "product\|billing\|purchase"

# 2. 构建并安装 Release 版本（测试用）
./gradlew assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk

# 3. 打开订阅页面并查看日志
adb shell am start -n com.quran.quranaudio.online/.subscription.SubscriptionActivity
adb logcat -s BillingManager:D SubscriptionActivity:D
```

---

## 📞 仍然无法解决？

**查看详细日志：**
```bash
# 过滤订阅相关日志
adb logcat | grep -E "(BillingManager|SubscriptionActivity|Purchase|Product)"

# 保存日志到文件
adb logcat -d > subscription_debug.log
```

**关键日志指标：**
- ✅ `Billing setup successful` - Billing 连接成功
- ✅ `Found X products` - 找到 X 个产品（期望值：2）
- ✅ `Product: plan_monthly` - 月度产品已加载
- ✅ `Product: plan_year` - 年度产品已加载

**如果看到：**
- ❌ `Billing setup failed` → Google Play 服务问题
- ❌ `Found 0 products` → 产品配置问题
- ❌ `Query products failed` → 网络或配置问题

---

## 💡 最佳实践

1. **先完成 Google Play Console 配置**，再测试应用
2. **使用内部测试轨道**进行快速迭代
3. **等待足够时间**让配置生效（至少 2 小时）
4. **使用测试账号**避免真实扣费
5. **查看详细日志**快速定位问题
6. **保留错误截图和日志**便于排查

---

## 📚 相关文档

- [Google Play Billing 官方文档](https://developer.android.com/google/play/billing)
- [测试订阅功能](https://developer.android.com/google/play/billing/test)
- [创建和管理订阅](https://support.google.com/googleplay/android-developer/answer/140504)
- [应用内购买配置指南](https://developer.android.com/google/play/billing/getting-ready)

---

## ✅ 成功标志

当配置正确且生效后，您会看到：

1. **应用启动时：**
   ```
   ✅ Billing setup successful
   ✅ Found 2 products
   ✅ Products loaded successfully
   ```

2. **订阅页面：**
   - 价格卡片正常显示
   - 可以切换套餐
   - 按钮状态为 "GO PREMIUM NOW"（非 "Loading..."）

3. **点击购买按钮：**
   - 弹出 Google Play 支付界面
   - 显示正确的产品信息和价格
   - 测试账号可以完成"购买"（不扣费）

---

**🎉 祝配置顺利！如有问题，请查看详细日志或联系技术支持。**

