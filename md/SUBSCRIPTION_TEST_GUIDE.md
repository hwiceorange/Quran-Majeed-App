# 订阅功能测试指南

## 🎯 快速测试入口

### 方法1：通过代码直接启动（推荐用于初次测试）

在任意 Activity 中添加测试按钮：

```kotlin
// 在 onCreate 或任意位置添加
findViewById<View>(R.id.test_subscription_button).setOnClickListener {
    val intent = Intent(this, SubscriptionActivity::class.java)
    startActivity(intent)
}
```

### 方法2：通过 ADB 命令启动

```bash
# 直接启动订阅页面
adb shell am start -n com.quran.quranaudio.online/.subscription.SubscriptionActivity

# 查看实时日志
adb logcat | grep -E "BillingManager|SubscriptionActivity"
```

### 方法3：在主页添加临时测试按钮

修改 `FragMain.java` 添加：

```java
// 临时测试代码
Button testSubButton = new Button(getContext());
testSubButton.setText("🔥 Test Subscription");
testSubButton.setOnClickListener(v -> {
    Intent intent = new Intent(getContext(), SubscriptionActivity.class);
    startActivity(intent);
});
// 添加到某个 ViewGroup
```

---

## 📋 测试前准备清单

### ✅ Google Play Console 配置

- [ ] 创建订阅商品 `plan_monthly` ($2.99/月, 7天免费试用)
- [ ] 创建订阅商品 `plan_year` ($17.99/年)
- [ ] 上传 AAB 到内部测试轨道
- [ ] 添加测试账号（Settings → License testing）
- [ ] 确认订阅商品状态为 "已发布"

### ✅ 应用配置

- [ ] 应用已使用 release keystore 签名
- [ ] AAB versionCode 为 62, versionName 为 1.7.0
- [ ] AndroidManifest.xml 包含 BILLING 权限
- [ ] Google Services 配置正确

### ✅ 测试设备

- [ ] 测试设备已登录测试账号
- [ ] 网络连接正常
- [ ] Google Play Services 已更新

---

## 🧪 测试用例

### 测试用例 1：页面加载

**步骤:**
1. 启动订阅页面
2. 等待商品加载完成

**预期结果:**
```
✅ 页面正常显示
✅ 顶部图形显示正确
✅ 功能列表显示（AI学习、无广告、深度解析、完整题库）
✅ 年度和月度套餐显示
✅ 按钮从 "Loading..." 变为 "Start trial & plan"
✅ 日志显示: "Billing setup successful, querying products..."
✅ 日志显示: "Found 2 products"
```

### 测试用例 2：套餐选择

**步骤:**
1. 点击月度套餐
2. 观察UI变化
3. 点击年度套餐
4. 观察UI变化

**预期结果:**
```
✅ 单选按钮正确切换
✅ 套餐说明文本更新
✅ 选中的套餐卡片高亮显示
```

### 测试用例 3：免费试用开关

**步骤:**
1. 开启免费试用开关
2. 观察UI变化
3. 关闭免费试用开关
4. 观察UI变化

**预期结果:**
```
✅ 开关开启时，按钮文本为 "Start trial & plan"
✅ 开关开启时，"NO PAYMENT NOW" 提示可见
✅ 开关关闭时，按钮文本为 "Go Premium Now"
✅ 开关关闭时，"NO PAYMENT NOW" 提示隐藏
```

### 测试用例 4：购买流程（月度套餐）

**步骤:**
1. 选择月度套餐
2. 开启免费试用
3. 点击订阅按钮
4. 在 Google Play 对话框中确认购买

**预期结果:**
```
✅ Google Play 购买对话框弹出
✅ 显示正确的价格和试用信息
✅ 确认购买后，Toast 提示 "Subscription activated successfully!"
✅ 页面自动关闭
✅ 日志显示: "Purchase successful!"
```

### 测试用例 5：购买流程（年度套餐）

**步骤:**
1. 选择年度套餐
2. 点击订阅按钮
3. 在 Google Play 对话框中确认购买

**预期结果:**
```
✅ Google Play 购买对话框弹出
✅ 显示年度价格
✅ 确认购买后成功
```

### 测试用例 6：取消购买

**步骤:**
1. 点击订阅按钮
2. 在 Google Play 对话框中点击取消

**预期结果:**
```
✅ Toast 提示 "Purchase canceled"
✅ 页面保持打开状态
✅ 按钮恢复可用状态
```

### 测试用例 7：已拥有订阅

**步骤:**
1. 使用已订阅的账号
2. 打开订阅页面
3. 再次尝试订阅

**预期结果:**
```
✅ 日志显示: "You are already subscribed!"
✅ Toast 提示 "You already own this subscription"
```

### 测试用例 8：订阅恢复

**步骤:**
1. 订阅后，卸载应用
2. 重新安装应用
3. 使用相同账号登录
4. 检查订阅状态

**预期结果:**
```
✅ 应用启动后自动恢复订阅状态
✅ SubscriptionHelper.isUserSubscribed() 返回 true
✅ 日志显示: "Active subscription: [product_id]"
```

---

## 📱 实际测试流程

### 步骤 1: 构建和上传

```bash
# 1. 构建 AAB
cd /Users/huwei/AndroidStudioProjects/quran0
./gradlew clean bundleRelease

# 2. AAB 文件位置
# app/build/outputs/bundle/release/app-release.aab

# 3. 上传到 Google Play Console
# - 进入内部测试轨道
# - 上传 AAB
# - 等待处理完成（通常5-10分钟）
```

### 步骤 2: 设置测试账号

```
1. Google Play Console → 设置 → 许可测试
2. 添加测试账号（Gmail 地址）
3. 保存
```

### 步骤 3: 安装测试版本

```
1. 测试设备登录测试账号
2. 通过 Google Play Console 的内部测试链接安装
   或
   通过 ADB 安装 APK（需要先 bundleRelease 生成 APK）
```

### 步骤 4: 开始测试

```bash
# 方法1: 通过 ADB 启动订阅页面
adb shell am start -n com.quran.quranaudio.online/.subscription.SubscriptionActivity

# 方法2: 在应用内添加测试入口
# 参考上面的"快速测试入口"部分
```

### 步骤 5: 查看日志

```bash
# 实时查看订阅相关日志
adb logcat -s BillingManager SubscriptionActivity

# 或者使用 grep 过滤
adb logcat | grep -E "BillingManager|SubscriptionActivity|Billing"
```

---

## 🔍 日志示例

### 成功的日志流程：

```
D/BillingManager: 🔧 Initializing Billing Client...
D/BillingManager: ✅ Billing setup successful
D/SubscriptionActivity: ✅ Billing setup successful, querying products...
D/BillingManager: 🔍 Querying subscription products...
D/BillingManager: ✅ Found 2 products
D/BillingManager: 📦 Product: plan_monthly
D/BillingManager:   💰 Price: $2.99
D/BillingManager: 📦 Product: plan_year
D/BillingManager:   💰 Price: $17.99
D/SubscriptionActivity: 📦 Loaded 2 products
D/SubscriptionActivity: 💰 Monthly price: $2.99 / Month
D/SubscriptionActivity: 💰 Yearly price: $17.99 / year
```

### 购买成功的日志：

```
D/SubscriptionActivity: 🚀 Starting purchase flow...
D/BillingManager: 🚀 Launching purchase flow for: plan_monthly
D/BillingManager: ✅ Purchase successful: [plan_monthly]
D/BillingManager: 📝 Acknowledging purchase...
D/BillingManager: ✅ Purchase acknowledged
D/SubscriptionActivity: 🎉 Purchase successful!
D/BillingManager: 💾 Subscription status saved: true, plan_monthly
```

---

## ❌ 常见错误和解决方案

### 错误 1: "商品不可用"
```
日志: Query products failed: Item unavailable
```
**解决方案:**
- 确认订阅商品已在 Google Play Console 中创建
- 确认 AAB 已上传并处理完成
- 等待 24 小时让订阅商品生效

### 错误 2: "Service disconnected"
```
日志: Billing setup failed: SERVICE_DISCONNECTED
```
**解决方案:**
- 检查网络连接
- 更新 Google Play Services
- 重启设备

### 错误 3: "Not signed with release key"
```
日志: 签名不匹配
```
**解决方案:**
- 使用 release keystore 签名
- 确认签名与 Google Play Console 中的一致

### 错误 4: "Item already owned"
```
日志: ITEM_ALREADY_OWNED
```
**解决方案:**
- 这是正常情况（用户已订阅）
- 应用会自动查询现有订阅

---

## 🧹 清理测试数据

### 取消测试订阅

```
Google Play Store → 菜单 → 订阅 → 选择订阅 → 取消订阅
```

### 清除本地订阅状态

```kotlin
SubscriptionHelper.clearSubscriptionStatus(context)
```

或通过 ADB:

```bash
adb shell pm clear com.quran.quranaudio.online
```

---

## 📞 需要帮助？

遇到问题时，请收集以下信息：

1. 完整的 Logcat 日志
2. Google Play Console 截图
3. 设备信息（Android 版本、Google Play Services 版本）
4. 测试步骤描述

联系：lecheng2019@gmail.com

