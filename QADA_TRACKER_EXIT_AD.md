# Qada Tracker 退出插屏广告实现文档

## 📋 需求说明

用户退出 Qada Tracker 页面流程时显示插屏广告：
1. 用户点击返回按钮或按下物理返回键时触发
2. 关闭插屏后返回到 Salat 页面
3. 如果没有插屏则不阻挡用户体验（直接返回）
4. 插屏ID共用应用内插屏ID (`AD_INTERS`)
5. **留存用户在第3天开始**展示请求并显示退出 Qada Tracker 页面插屏广告

---

## ✅ 实现内容

### 1. **添加必要的导入**

**文件**: `QadaTrackerActivity.java`

```java
// 插屏广告相关导入
import com.quranaudio.common.ad.AdConfig;
import com.quranaudio.common.ad.AdFactory;
import com.quranaudio.common.ad.AdShowCallback;
import com.quranaudio.common.ad.model.AdItem;
import com.quranaudio.common.ad.model.RewardItem;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
```

---

### 2. **修改 Toolbar 返回按钮逻辑**

**修改前**（第190行）：
```java
toolbar.setNavigationOnClickListener(v -> finish());
```

**修改后**（第199行）：
```java
toolbar.setNavigationOnClickListener(v -> handleExit());
```

---

### 3. **新增核心方法**

#### **`handleExit()` - 处理退出流程**

```java
/**
 * 处理退出Qada Tracker页面流程
 * 留存用户（第3天及以后）显示插屏广告
 */
private void handleExit() {
    // 检查是否是留存用户（第3天及以后）
    if (shouldShowExitAd()) {
        showExitInterstitialAd();
    } else {
        // 新用户（前2天）直接退出，不阻挡用户体验
        finish();
    }
}
```

#### **`shouldShowExitAd()` - 判断是否显示广告**

```java
/**
 * 判断是否应该显示退出插屏广告
 * @return true 如果用户是留存用户（第3天及以后）
 */
private boolean shouldShowExitAd() {
    int installDays = getInstallDays();
    Log.d(TAG, "📅 Install days: " + installDays + ", shouldShowExitAd: " + (installDays >= 3));
    return installDays >= 3;
}
```

#### **`getInstallDays()` - 获取安装天数**

```java
/**
 * 获取应用安装天数
 * @return 安装天数（0表示安装当天）
 */
private int getInstallDays() {
    try {
        PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        long firstInstallTime = packageInfo.firstInstallTime;
        long currentTime = System.currentTimeMillis();
        int days = (int) ((currentTime - firstInstallTime) / (24 * 60 * 60 * 1000));
        Log.d(TAG, "📱 First install: " + firstInstallTime + ", Current: " + currentTime + ", Days: " + days);
        return days;
    } catch (PackageManager.NameNotFoundException e) {
        Log.e(TAG, "❌ Failed to get install days", e);
        return 0;
    }
}
```

#### **`showExitInterstitialAd()` - 显示插屏广告**

```java
/**
 * 显示退出插屏广告
 * 使用应用内共用插屏ID (AD_INTERS)
 */
private void showExitInterstitialAd() {
    Log.d(TAG, "🎬 Showing exit interstitial ad...");
    
    AdFactory.showInterstitialAd(
        this,
        AdConfig.AD_INTERS,  // 共用应用内插屏ID
        "qada_tracker_exit",  // 功能标签
        new AdShowCallback() {
            @Override
            public void onAdImpression(AdItem adItem) {
                Log.d(TAG, "📊 Ad impression recorded");
            }
            
            @Override
            public void onAdClicked(AdItem adItem) {
                Log.d(TAG, "👆 Ad clicked");
            }
            
            @Override
            public void onUserEarnedReward(AdItem adItem, RewardItem rewardItem) {
                // 插屏广告不需要奖励
            }
            
            @Override
            public void onAdClosed(AdItem adItem) {
                Log.d(TAG, "✅ Ad closed, finishing activity");
                finish();  // 广告关闭后返回到 Salat 页面
            }
            
            @Override
            public void onShow(AdItem adItem) {
                Log.d(TAG, "📺 Ad shown");
            }
            
            @Override
            public void onShowFail() {
                Log.d(TAG, "❌ Ad failed to show, finishing activity directly");
                // 如果广告展示失败，直接退出，不阻挡用户体验
                finish();
            }
        }
    );
}
```

---

### 4. **重写 `onBackPressed()` 方法**

```java
/**
 * 重写返回键行为，同样显示插屏广告
 */
@Override
public void onBackPressed() {
    handleExit();
}
```

---

## 🎯 功能特点

### ✅ **用户体验优化**
1. **新用户保护**（第1-2天）：直接退出，不显示广告，避免影响新用户体验
2. **留存用户变现**（第3天及以后）：显示插屏广告，最大化广告收益
3. **失败降级**：广告加载失败时自动跳过，不阻挡用户退出

### ✅ **覆盖所有退出场景**
- Toolbar 左上角返回按钮 ✅
- 物理返回键 ✅
- 系统手势返回 ✅

### ✅ **广告配置**
- **广告ID**: `AdConfig.AD_INTERS` (共用应用内插屏ID)
- **功能标签**: `"qada_tracker_exit"` (用于数据分析和追踪)
- **AdMob广告单元ID**: `ca-app-pub-3966802724737141/2182661506`

---

## 📊 日志追踪

实现中添加了详细的日志输出，便于调试和追踪：

| 日志标签 | 日志内容 | 说明 |
|---------|---------|------|
| `📅 Install days` | 安装天数和是否显示广告的判断 | 用于验证用户留存天数 |
| `📱 First install` | 首次安装时间、当前时间、计算天数 | 验证天数计算逻辑 |
| `🎬 Showing exit` | 开始显示插屏广告 | 广告请求发起 |
| `📊 Ad impression` | 广告展示成功 | 广告曝光记录 |
| `👆 Ad clicked` | 用户点击广告 | 广告点击记录 |
| `📺 Ad shown` | 广告显示回调 | 广告展示回调 |
| `✅ Ad closed` | 广告关闭，退出页面 | 正常流程完成 |
| `❌ Ad failed` | 广告展示失败，直接退出 | 失败降级处理 |

---

## 🔍 测试要点

### 1. **新用户测试**（安装后第1-2天）
- [ ] 点击 Toolbar 返回按钮，应直接退出，无广告
- [ ] 按物理返回键，应直接退出，无广告
- [ ] 查看日志：`Install days: 0` 或 `1`, `shouldShowExitAd: false`

### 2. **留存用户测试**（安装后第3天及以后）
- [ ] 点击 Toolbar 返回按钮，应显示插屏广告
- [ ] 按物理返回键，应显示插屏广告
- [ ] 广告关闭后，应返回到 Salat 页面
- [ ] 查看日志：`Install days: 3+`, `shouldShowExitAd: true`

### 3. **广告失败测试**
- [ ] 模拟网络断开或广告未加载
- [ ] 应该直接退出，不阻挡用户
- [ ] 查看日志：`Ad failed to show, finishing activity directly`

### 4. **ADB 日志监控**

```bash
# 监控 Qada Tracker 相关日志
adb logcat | grep "QadaTrackerActivity"

# 监控广告相关日志
adb logcat | grep -E "QadaTrackerActivity|AdFactory"
```

---

## 📈 数据分析指标

通过 Firebase Analytics 和 AdMob 可追踪以下指标：

1. **广告展示率**: 第3天及以后用户的广告展示次数 / 退出次数
2. **广告点击率**: 广告点击次数 / 广告展示次数
3. **eCPM**: 千次展示收益
4. **用户体验**: 新用户留存率（确保新用户不受广告影响）

---

## 🎉 实现完成总结

### ✅ **核心功能**
- ✅ 第3天及以后用户退出时显示插屏广告
- ✅ 新用户（前2天）直接退出，不显示广告
- ✅ 共用应用内插屏ID (`AD_INTERS`)
- ✅ 覆盖所有退出场景（返回按钮、物理返回键）
- ✅ 广告失败时自动降级，不阻挡用户

### ✅ **用户体验**
- ✅ 新用户保护机制
- ✅ 广告失败降级处理
- ✅ 平滑的退出流程

### ✅ **可维护性**
- ✅ 详细的日志追踪
- ✅ 清晰的代码注释
- ✅ 模块化的方法设计

---

## 📝 修改文件清单

| 文件路径 | 修改内容 |
|---------|---------|
| `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/QadaTrackerActivity.java` | 添加广告导入、实现退出插屏广告逻辑、重写 `onBackPressed()` |

---

## 🚀 下一步

1. **测试验证**：在测试设备上验证新用户和留存用户的不同行为
2. **数据监控**：通过 Firebase Analytics 监控广告展示数据
3. **A/B测试**：可以考虑调整天数阈值（如第2天、第4天）以优化收益和用户体验的平衡

---

**实现日期**: 2025-11-15  
**开发者**: AI Assistant  
**状态**: ✅ 已完成

