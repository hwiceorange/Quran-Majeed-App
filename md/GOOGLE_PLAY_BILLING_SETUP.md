# Google Play Billing Library 集成说明

## ✅ 已完成的配置

### 1. 添加 Google Play Billing Library 依赖

在 `app/build.gradle` 中已添加：
```gradle
// Google Play Billing Library for subscriptions
implementation 'com.android.billingclient:billing:6.1.0'
implementation 'com.android.billingclient:billing-ktx:6.1.0'
```

### 2. 添加 BILLING 权限

在 `app/src/main/AndroidManifest.xml` 中已添加：
```xml
<!-- Google Play Billing permission for subscriptions -->
<uses-permission android:name="com.android.vending.BILLING" />
```

### 3. 签名配置

项目已配置发布签名：
- Keystore: `quran_keystore`
- Key Alias: `key0`
- 已在 `build.gradle` 的 `signingConfigs.release` 中配置

## 📦 生成 AAB 包

### 方法一：使用脚本（推荐）

```bash
# 在项目根目录执行
./build_aab.sh
```

### 方法二：使用 Gradle 命令

```bash
# 清理并构建 Release AAB
./gradlew clean bundleRelease
```

## 📂 AAB 文件位置

构建成功后，AAB 文件位于：
```
app/build/outputs/bundle/release/app-release.aab
```

## 🔍 验证 AAB 包

### 检查 AAB 内容
```bash
# 使用 bundletool (需要单独安装)
bundletool build-apks \
  --bundle=app/build/outputs/bundle/release/app-release.aab \
  --output=app.apks \
  --ks=quran_keystore \
  --ks-pass=pass:Huwei123 \
  --ks-key-alias=key0 \
  --key-pass=pass:Huwei123
```

### 检查签名
```bash
# 列出 AAB 内容
unzip -l app/build/outputs/bundle/release/app-release.aab | head -20
```

## 📤 上传到 Google Play Console

1. **登录 Google Play Console**
   - 访问: https://play.google.com/console
   - 选择您的应用

2. **创建新版本**
   - 进入 "生产环境" 或 "内部测试" / "封闭测试"
   - 点击 "创建新版本"

3. **上传 AAB 文件**
   - 在 "应用包" 部分点击 "上传"
   - 选择 `app/build/outputs/bundle/release/app-release.aab`
   - 等待上传和处理完成

4. **填写版本信息**
   - 版本名称: `1.6.9` (当前 versionName)
   - 版本代码: `61` (当前 versionCode)
   - 添加"版本说明"

5. **检查订阅配置**
   - 确保在 Google Play Console 中已创建订阅商品
   - 商品 ID (SKU) 需要与代码中的 ID 匹配

## 🔧 订阅功能开发提示

### 1. 在 Google Play Console 中创建订阅商品

1. 进入应用 → "获利" → "订阅"
2. 创建订阅商品：
   - 商品 ID (例如: `premium_monthly`, `premium_yearly`)
   - 名称、描述
   - 价格和计费周期

### 2. 代码集成示例

```kotlin
// 初始化 BillingClient
val billingClient = BillingClient.newBuilder(context)
    .setListener(purchasesUpdatedListener)
    .enablePendingPurchases()
    .build()

// 启动连接
billingClient.startConnection(object : BillingClientStateListener {
    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // 查询可用订阅
            queryAvailableSubscriptions()
        }
    }
    
    override fun onBillingServiceDisconnected() {
        // 重连逻辑
    }
})

// 查询订阅
fun queryAvailableSubscriptions() {
    val params = SkuDetailsParams.newBuilder()
        .setSkusList(listOf("premium_monthly", "premium_yearly"))
        .setType(BillingClient.SkuType.SUBS)
        .build()
    
    billingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            // 处理订阅详情
            skuDetailsList?.forEach { skuDetails ->
                // 显示订阅选项
            }
        }
    }
}

// 启动购买流程
fun launchPurchaseFlow(activity: Activity, skuDetails: SkuDetails) {
    val flowParams = BillingFlowParams.newBuilder()
        .setSkuDetails(skuDetails)
        .build()
    
    billingClient.launchBillingFlow(activity, flowParams)
}
```

### 3. 处理购买结果

```kotlin
private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
        for (purchase in purchases) {
            handlePurchase(purchase)
        }
    } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
        // 用户取消
    } else {
        // 其他错误
    }
}

private fun handlePurchase(purchase: Purchase) {
    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
        if (!purchase.isAcknowledged) {
            // 确认购买
            acknowledgePurchase(purchase)
        }
        // 解锁高级功能
        unlockPremiumFeatures()
    }
}
```

## 📝 注意事项

1. **测试订阅**
   - 在 Google Play Console 中添加测试账号
   - 使用测试账号测试订阅流程
   - 测试订阅不会产生真实费用

2. **版本号管理**
   - 每次上传前更新 `versionCode` 和 `versionName`
   - 当前版本: `versionCode 61`, `versionName "1.6.9"`

3. **签名密钥安全**
   - 妥善保管 `quran_keystore` 文件
   - 不要将密钥文件提交到版本控制
   - 建议备份密钥到安全位置

4. **AAB vs APK**
   - Google Play 现在要求上传 AAB 格式（不是 APK）
   - AAB 允许 Google Play 为不同设备生成优化的 APK
   - 文件大小通常比通用 APK 更小

## 🐛 常见问题

### Q: 构建失败，提示找不到签名文件？
A: 确保 `quran_keystore` 文件在项目根目录（`app/` 目录下）

### Q: 上传到 Google Play 失败？
A: 检查：
- AAB 是否使用正确的签名
- 版本号是否比之前的版本更高
- 是否包含 BILLING 权限

### Q: 如何验证订阅是否配置正确？
A: 在 Google Play Console → "获利" → "订阅" 中查看已创建的订阅商品，确保商品 ID 与代码中使用的 ID 一致。

