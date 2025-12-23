# 订阅功能实现指南

## ✅ 已完成的功能

### 步骤1：布局开发 ✅
- ✅ 创建完整的订阅页面UI（`activity_subscription.xml`）
- ✅ 深紫色到黑色渐变背景
- ✅ 顶部 `topsub.png` 图形
- ✅ 功能列表（AI学习路径、无广告、深度解析、完整题库）
- ✅ 自定义免费试用开关
- ✅ 自定义单选按钮
- ✅ 年度和月度套餐卡片
- ✅ CTA按钮（黄色圆角）
- ✅ 法律信息和 NO PAYMENT NOW 提示

### 步骤2：Google Play Billing 集成 ✅
- ✅ 添加 Billing Library 依赖（v6.1.0）
- ✅ 创建 `BillingManager.kt` 管理订阅逻辑
- ✅ 实现订阅商品查询
- ✅ 实现购买流程
- ✅ 处理购买结果和验证
- ✅ 实现订阅恢复功能
- ✅ 更新 `SubscriptionActivity.kt` 集成 Billing
- ✅ 创建 `SubscriptionHelper.kt` 辅助工具类

---

## 📦 订阅商品ID

在 Google Play Console 中创建以下订阅商品：

| 订阅类型 | 商品ID | 建议价格 | 试用期 |
|---------|--------|---------|--------|
| 月度订阅 | `plan_monthly` | $2.99/月 | 7天免费 |
| 年度订阅 | `plan_year` | $17.99/年 | - |

---

## 🚀 使用方法

### 1. 启动订阅页面

```kotlin
// 方法1：使用 Intent
val intent = Intent(context, SubscriptionActivity::class.java)
startActivity(intent)

// 方法2：使用 SubscriptionHelper
SubscriptionHelper.launchSubscriptionPage(context)
```

### 2. 检查订阅状态

```kotlin
// 检查是否已订阅
if (SubscriptionHelper.isUserSubscribed(context)) {
    // 用户已订阅，解锁高级功能
}

// 获取订阅类型
val productId = SubscriptionHelper.getSubscribedProductId(context)
when (productId) {
    BillingManager.MONTHLY_PLAN_ID -> {
        // 月度订阅用户
    }
    BillingManager.YEARLY_PLAN_ID -> {
        // 年度订阅用户
    }
}

// 或者使用便捷方法
if (SubscriptionHelper.isYearlySubscriber(context)) {
    // 年度订阅用户
}

if (SubscriptionHelper.isMonthlySubscriber(context)) {
    // 月度订阅用户
}
```

### 3. 获取订阅状态字符串

```kotlin
val statusText = SubscriptionHelper.getSubscriptionStatusString(context)
// 返回: "Not Subscribed" 或 "Subscribed (Yearly)" 或 "Subscribed (Monthly)"
```

---

## 🎯 集成到应用中

### 选项A：从设置页面进入

在 `app/src/main/res/layout/frag_main.xml` 或设置页面添加订阅入口：

```xml
<LinearLayout
    android:id="@+id/btn_subscription"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:padding="16dp"
    android:background="?attr/selectableItemBackground"
    android:gravity="center_vertical">

    <ImageView
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:src="@drawable/ic_premium"
        app:tint="@color/colorPrimary" />

    <TextView
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginStart="12dp"
        android:text="Go Premium"
        android:textSize="16sp"
        android:textColor="@color/colorText" />

    <ImageView
        android:layout_width="20dp"
        android:layout_height="20dp"
        android:src="@drawable/dr_icon_arrow_right"
        app:tint="@color/colorTextSecondary" />
</LinearLayout>
```

Kotlin 代码：

```kotlin
binding.btnSubscription.setOnClickListener {
    SubscriptionHelper.launchSubscriptionPage(this)
}
```

### 选项B：首次启动时显示

在 `MainActivity` 或 `SplashScreenActivity` 中：

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // 检查是否首次启动且未订阅
    val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    val isFirstLaunch = prefs.getBoolean("is_first_launch", true)
    
    if (isFirstLaunch && !SubscriptionHelper.isUserSubscribed(this)) {
        // 延迟3秒后显示订阅页面
        Handler(Looper.getMainLooper()).postDelayed({
            SubscriptionHelper.launchSubscriptionPage(this)
        }, 3000)
        
        prefs.edit().putBoolean("is_first_launch", false).apply()
    }
}
```

### 选项C：限制功能，引导订阅

```kotlin
fun accessPremiumFeature() {
    if (SubscriptionHelper.isUserSubscribed(this)) {
        // 允许访问高级功能
        openPremiumFeature()
    } else {
        // 显示订阅提示对话框
        showSubscriptionPrompt()
    }
}

private fun showSubscriptionPrompt() {
    AlertDialog.Builder(this)
        .setTitle("Premium Feature")
        .setMessage("This feature is only available for premium subscribers.")
        .setPositiveButton("Subscribe") { _, _ ->
            SubscriptionHelper.launchSubscriptionPage(this)
        }
        .setNegativeButton("Cancel", null)
        .show()
}
```

---

## 📋 Google Play Console 配置步骤

### 1. 创建订阅商品

1. 登录 [Google Play Console](https://play.google.com/console)
2. 选择您的应用
3. 导航到 **获利 → 订阅**
4. 点击 **创建订阅**

#### 月度订阅配置：
- **订阅ID**: `plan_monthly`
- **名称**: Premium Monthly
- **说明**: Get unlimited access with monthly billing
- **计费周期**: 1个月
- **价格**: $2.99
- **免费试用**: 7天

#### 年度订阅配置：
- **订阅ID**: `plan_year`
- **名称**: Premium Yearly
- **说明**: Get unlimited access with yearly billing (Best Value!)
- **计费周期**: 1年
- **价格**: $17.99
- **免费试用**: 无（可选）

### 2. 设置基本 Plan

为每个订阅创建 **Base Plan**：
- **自动续订**: 是
- **宽限期**: 3天（推荐）
- **账户暂停**: 启用（推荐）

### 3. 添加测试账号

在 **设置 → 许可测试** 中添加测试账号：
- 添加您的 Google 账号邮箱
- 测试订阅不会产生真实费用

---

## 🧪 测试流程

### 测试环境准备：
1. ✅ 确保应用已签名（使用 release keystore）
2. ✅ 上传 AAB 到 Google Play Console（内部测试轨道）
3. ✅ 在 Google Play Console 中添加测试账号
4. ✅ 使用测试账号登录测试设备

### 测试步骤：

#### 1. 测试订阅流程
```
打开应用 → 进入订阅页面 → 选择套餐 → 点击订阅按钮
→ Google Play 弹出确认对话框 → 确认购买 → 验证订阅成功
```

#### 2. 测试订阅恢复
```
卸载应用 → 重新安装 → 打开应用 → 检查订阅状态是否恢复
```

#### 3. 测试取消订阅
```
Google Play Store → 订阅 → 取消订阅 → 验证应用中的订阅状态更新
```

#### 4. 查看日志
```bash
adb logcat | grep -E "BillingManager|SubscriptionActivity"
```

---

## 🐛 常见问题

### Q: 测试时显示 "商品不可用"？
**A:** 确保：
1. AAB 已上传到 Google Play Console
2. 订阅商品已创建并激活
3. 使用测试账号登录
4. 应用签名正确

### Q: 购买后状态不更新？
**A:** 检查：
1. `acknowledgePurchase()` 是否被正确调用
2. 查看 Logcat 日志中的错误信息
3. 确认网络连接正常

### Q: 如何测试免费试用？
**A:** 
- 测试账号的免费试用会立即到期（无需等待7天）
- 可以在 Google Play Console 中管理测试订阅

### Q: 如何取消测试订阅？
**A:**
```
Google Play Store → 菜单 → 订阅 → 选择订阅 → 取消订阅
```

---

## 📊 数据结构

### SharedPreferences: `subscription_prefs`

| Key | Type | 说明 |
|-----|------|------|
| `is_subscribed` | Boolean | 是否已订阅 |
| `product_id` | String | 订阅的产品ID |
| `last_check_time` | Long | 上次检查时间戳 |

---

## 🔒 安全建议

1. **服务器验证**（推荐）
   - 在后端验证购买凭证
   - 使用 Google Play Developer API

2. **定期检查**
   - 定期查询订阅状态
   - 处理订阅过期情况

3. **防止滥用**
   - 实施设备限制
   - 监控异常订阅行为

---

## 📝 版本历史

- **v1.7.0** (2025-11-04)
  - ✅ 添加订阅功能基础框架
  - ✅ 集成 Google Play Billing Library 6.1.0
  - ✅ 实现完整的订阅流程

---

## 📞 支持

如有问题，请联系：lecheng2019@gmail.com

