# 订阅 Offer 修复说明

## 问题描述

**症状**：月订阅点击订阅按钮时弹出 "No valid offer available" 错误，而年订阅正常工作。

**根本原因**：代码在查找月订阅的免费试用 offer 时，使用了错误的查找逻辑。

## Google Play 后台配置

根据后台截图，订阅配置如下：

### 月订阅 (plan_monthly)
- **基础方案 ID**: `plan-month`
- **优惠 ID**: `free` (7天免费试用)

### 年订阅 (plan_year)
- **基础方案 ID**: (基础方案，没有特殊优惠)

## 原始代码问题

```kotlin
// ❌ 错误的查找方式
val trialOffer = selectedProduct.subscriptionOfferDetails?.firstOrNull { offer ->
    offer.pricingPhases.pricingPhaseList.any { phase ->
        phase.priceAmountMicros == 0L  // 仅通过价格判断
    }
}
```

**问题**：
1. Google Play 的免费试用 offer 不一定在第一个 pricing phase 中体现为 0 价格
2. 应该通过 `offerId` 来精确查找特定的优惠
3. 没有区分基础方案和促销优惠

## 修复方案

### 月订阅免费试用

```kotlin
// ✅ 正确的查找方式
val trialOffer = selectedProduct.subscriptionOfferDetails?.firstOrNull { offer ->
    offer.offerId == "free"  // 通过优惠 ID 精确查找
}
```

### 基础方案（年订阅或月订阅不开启试用）

```kotlin
// ✅ 查找基础方案（没有 offerId）
val basePlanOffer = selectedProduct.subscriptionOfferDetails?.firstOrNull { offer ->
    offer.offerId == null || offer.offerId.isEmpty()
}
```

## 修复后的逻辑流程

### 1. 月订阅 + 免费试用开启
```
查找 offerId == "free" 的 offer
  ↓ 找到
使用免费试用 offer token
  ↓ 未找到
回退到基础方案 offer
```

### 2. 月订阅 + 免费试用关闭
```
查找基础方案 (offerId == null)
  ↓ 找到
使用基础方案 offer token
```

### 3. 年订阅（不受影响）
```
查找基础方案 (offerId == null)
  ↓ 找到
使用基础方案 offer token
```

## 关键改进

1. **精确匹配**：通过 `offerId == "free"` 精确查找月订阅的免费试用优惠
2. **区分类型**：明确区分基础方案（无 offerId）和促销优惠（有 offerId）
3. **详细日志**：添加详细的调试日志，包括：
   - 当前查找的 offer 类型
   - 每个 offer 的 basePlanId 和 offerId
   - Pricing phases 信息
4. **多重回退**：
   - 优先使用目标 offer
   - 回退到基础方案
   - 最终回退到第一个可用 offer
5. **保护年订阅**：年订阅逻辑不受影响，继续正常工作

## 测试建议

### 测试用例 1：月订阅 + 免费试用
1. 选择月订阅
2. 开启免费试用开关
3. 点击订阅按钮
4. **预期**：成功弹出支付弹窗，显示 7 天免费试用

### 测试用例 2：月订阅 + 不开启免费试用
1. 选择月订阅
2. 关闭免费试用开关
3. 点击订阅按钮
4. **预期**：成功弹出支付弹窗，显示标准月订阅价格

### 测试用例 3：年订阅（回归测试）
1. 选择年订阅
2. 点击订阅按钮
3. **预期**：成功弹出支付弹窗，显示年订阅价格（不受影响）

## 调试日志示例

成功找到免费试用 offer：
```
🔍 Looking for trial offer with ID 'free'...
  Checking offer: basePlanId=plan-month, offerId=free
✅ Found trial offer: basePlanId=plan-month, offerId=free
  Phase 0: $0.00, billingPeriod=P7D
  Phase 1: $4.99, billingPeriod=P1M
```

未找到免费试用 offer（回退到基础方案）：
```
🔍 Looking for trial offer with ID 'free'...
  Checking offer: basePlanId=plan-month, offerId=null
⚠️ No trial offer with ID 'free' found
  Available offer 0: basePlanId=plan-month, offerId=null
📦 Using base plan offer: basePlanId=plan-month
```

## Google Play Billing 关键概念

### Product（产品）
- 例如：`plan_monthly`, `plan_year`

### Base Plan（基础方案）
- 每个产品可以有一个或多个基础方案
- 例如：`plan-month`, `plan-year`

### Offer（优惠）
- 附加在基础方案上的促销活动
- 可以包含免费试用、介绍价格等
- 通过 `offerId` 识别
- 例如：`free` (7天免费试用)

### Offer Token
- 用于启动支付流程的令牌
- 每个 offer 都有唯一的 offerToken

## 相关文件

- `SubscriptionActivity.kt` - 修复的主要文件
- `BillingManager.kt` - 定义了产品 ID 常量

## 参考文档

- [Google Play Billing Library](https://developer.android.com/google/play/billing)
- [Subscriptions with multiple base plans](https://developer.android.com/google/play/billing/subscriptions)
- [Offer types](https://support.google.com/googleplay/android-developer/answer/12124625)

