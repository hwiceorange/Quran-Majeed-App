# 🔍 原生广告详细追踪日志

## ✅ 已添加的追踪点

### 1️⃣ NativeAdHelper.kt - 广告展示入口
每次调用`displayNativeAdWithAutoLoad()`时记录：
- ✅ 调用者信息（类名.方法名:行号）
- ✅ Activity名称
- ✅ Container信息（类型、ID、可见性、父容器）
- ✅ 布局资源ID
- ✅ 广告对象是否为null
- ✅ 广告内容（headline、body）
- ✅ AdView填充过程
- ✅ 添加到容器的过程
- ✅ 成功/失败状态

### 2️⃣ NativeAdManager.kt - 广告加载管理
每次加载广告时记录：
- ✅ 缓存池状态（当前数量/最大数量）
- ✅ 是否正在加载
- ✅ 订阅状态检查
- ✅ Ad Unit ID
- ✅ 加载成功后的广告信息
- ✅ 加载失败的错误详情
- ✅ 待处理回调数量

---

## 🚀 使用方法

### 方法1：使用自动化脚本（推荐）

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App
./GET_NATIVE_AD_LOG.sh
```

### 方法2：手动命令

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App

# 清空日志
adb logcat -c

# 实时查看原生广告日志
adb logcat | grep -E "NATIVE_AD_TRACK|NativeAdManager|NativeAdHelper"
```

---

## 📊 日志格式说明

### 🎯 广告请求日志

```
NATIVE_AD_TRACK: ═══════════════════════════════════════════════
NATIVE_AD_TRACK: 🎯 Native Ad Request
NATIVE_AD_TRACK:    Caller: FragOnboardLanguage.setupNativeAd:299
NATIVE_AD_TRACK:    Activity: ActivityOnboarding
NATIVE_AD_TRACK:    Container: FrameLayout@123456789
NATIVE_AD_TRACK:    Layout ResId: 2130903456
NATIVE_AD_TRACK:    Container visibility: 0 (0=VISIBLE, 4=INVISIBLE, 8=GONE)
NATIVE_AD_TRACK:    Container parent: LinearLayout
NATIVE_AD_TRACK: ═══════════════════════════════════════════════
```

### 🔄 广告加载日志

```
NATIVE_AD_TRACK: 🔄 NativeAdManager.loadNewAd() - Starting ad load (0/3)
NATIVE_AD_TRACK:    Ad Unit ID: ca-app-pub-3940256099942544/2247696110
```

### ✅ 广告加载成功

```
NATIVE_AD_TRACK: ✅ Native ad loaded from AdMob
NATIVE_AD_TRACK:    Headline: Test Ad
NATIVE_AD_TRACK:    Body: This is a test ad for development
NATIVE_AD_TRACK:    Advertiser: Google Ads
NATIVE_AD_TRACK: ✅ Ad added to cache pool (1/3)
NATIVE_AD_TRACK:    Pending callbacks: 2
```

### ❌ 广告加载失败

```
NATIVE_AD_TRACK: ❌ Native ad load failed
NATIVE_AD_TRACK:    Error code: 3
NATIVE_AD_TRACK:    Error message: No fill
NATIVE_AD_TRACK:    Domain: com.google.android.gms.ads
```

### 📺 广告展示日志

```
NATIVE_AD_TRACK: → Callback received for: FragOnboardLanguage.setupNativeAd:299
NATIVE_AD_TRACK:    Ad object: NOT NULL
NATIVE_AD_TRACK: → Inflating ad view for: FragOnboardLanguage.setupNativeAd:299
NATIVE_AD_TRACK:    Ad headline: Test Ad
NATIVE_AD_TRACK:    Ad body: This is a test ad for development
NATIVE_AD_TRACK:    AdView inflated: NativeAdView
NATIVE_AD_TRACK: → Populating ad view...
NATIVE_AD_TRACK:    ✅ Ad view populated
NATIVE_AD_TRACK: → Adding ad view to container...
NATIVE_AD_TRACK:    Container child count before: 0
NATIVE_AD_TRACK:    Container child count after: 1
NATIVE_AD_TRACK:    Container visibility set to: VISIBLE
NATIVE_AD_TRACK: ✅ Native ad displayed successfully for: FragOnboardLanguage.setupNativeAd:299
```

---

## 🎯 原生广告位置清单

### 1️⃣ 多语言选择页底部
- **文件**: `FragOnboardLanguage.kt`
- **方法**: `setupNativeAd()`
- **容器**: `native_ad_container`
- **布局**: `native_ad_onboarding.xml`

### 2️⃣ 版本选择页底部
- **文件**: `FragOnboardQuranVersion.kt`
- **方法**: `setupNativeAd()`
- **容器**: `native_ad_container`
- **布局**: `native_ad_onboarding.xml`

### 3️⃣ 主页 Verse of the Day 卡片底部
- **文件**: `HomeFragment.java`
- **方法**: `loadVOTDNativeAd()`
- **容器**: `votd_native_ad_container`
- **布局**: `layout_ad_native_small_wrapper.xml`

### 4️⃣ Quiz 答题结果页
- **文件**: `QuizReviewLearnActivity.kt`
- **方法**: `preloadNativeAd()`
- **容器**: `AdNativeSmallWrapperView`
- **布局**: `layout_ad_native_small_wrapper.xml`

---

## 🔍 问题诊断流程

### 步骤1：检查广告是否被请求

查找日志：
```
NATIVE_AD_TRACK: 🎯 Native Ad Request
NATIVE_AD_TRACK:    Caller: [位置名称]
```

**如果没有**：说明代码没有调用`displayNativeAdWithAutoLoad()`

### 步骤2：检查广告是否加载

查找日志：
```
NATIVE_AD_TRACK: 🔄 NativeAdManager.loadNewAd() - Starting ad load
```

**如果没有**：
- 可能用户已订阅
- 可能缓存池已满
- 可能正在加载中

### 步骤3：检查加载结果

**成功**：
```
NATIVE_AD_TRACK: ✅ Native ad loaded from AdMob
```

**失败**：
```
NATIVE_AD_TRACK: ❌ Native ad load failed
NATIVE_AD_TRACK:    Error code: [错误码]
```

常见错误码：
- **0**: 内部错误
- **1**: 无效请求
- **2**: 网络错误
- **3**: 无广告填充（No fill）

### 步骤4：检查回调是否收到

查找日志：
```
NATIVE_AD_TRACK: → Callback received for: [位置名称]
NATIVE_AD_TRACK:    Ad object: NOT NULL/NULL
```

**如果是NULL**：广告加载失败或用户已订阅

### 步骤5：检查广告是否展示

查找日志：
```
NATIVE_AD_TRACK: ✅ Native ad displayed successfully for: [位置名称]
```

**如果没有**：检查是否有异常日志：
```
NATIVE_AD_TRACK: ❌ Failed to display ad for: [位置名称]
```

---

## 📋 完整测试流程

### 测试1：多语言页原生广告

```bash
# 1. 启动日志监控
adb logcat -c && adb logcat | grep "NATIVE_AD_TRACK"

# 2. 卸载应用
adb uninstall com.quran.quranaudio.online

# 3. 在Android Studio重新运行

# 4. 进入多语言页面，向下滚动查看底部
```

**预期日志**：
```
NATIVE_AD_TRACK: 🎯 Native Ad Request
NATIVE_AD_TRACK:    Caller: FragOnboardLanguage.setupNativeAd
NATIVE_AD_TRACK: ✅ Native ad loaded from AdMob
NATIVE_AD_TRACK: ✅ Native ad displayed successfully
```

### 测试2：主页 VOTD 原生广告

```bash
# 1. 进入主页
# 2. 向下滚动到 Verse of the Day 卡片
```

**预期日志**：
```
NATIVE_AD_TRACK: 🎯 Native Ad Request
NATIVE_AD_TRACK:    Caller: HomeFragment.loadVOTDNativeAd
NATIVE_AD_TRACK: ✅ Native ad displayed successfully
```

### 测试3：Quiz 答题结果页原生广告

```bash
# 1. 进入 Quiz
# 2. 完成答题
# 3. 查看结果页
```

**预期日志**：
```
NATIVE_AD_TRACK: 🎯 Native Ad Request
NATIVE_AD_TRACK:    Caller: AdNativeSmallWrapperView.loadNativeAd
NATIVE_AD_TRACK: ✅ Native ad displayed successfully
```

---

## 🛠️ 快速调试命令

### 查看所有原生广告请求
```bash
adb logcat -d | grep "🎯 Native Ad Request" -A 10
```

### 查看所有广告加载失败
```bash
adb logcat -d | grep "❌ Native ad load failed" -A 5
```

### 查看所有广告展示成功
```bash
adb logcat -d | grep "✅ Native ad displayed successfully"
```

### 查看缓存池状态
```bash
adb logcat -d | grep "Cache pool" | tail -10
```

### 实时监控原生广告全流程
```bash
adb logcat | grep -E "NATIVE_AD_TRACK|NativeAdManager: ✅|NativeAdManager: ❌"
```

---

## ⚠️ 常见问题

### Q1: 日志显示"No ad available"
**原因**：
1. 用户已订阅（`User is subscribed`）
2. 广告加载失败（`Error code: 3 - No fill`）
3. 缓存池为空且正在加载

**解决**：
- 检查订阅状态
- 等待广告加载完成
- 查看加载失败的错误码

### Q2: 日志显示"Container visibility: 8 (GONE)"
**原因**：容器被隐藏

**解决**：
- 检查布局文件中容器的初始可见性
- 检查是否有代码将容器设置为GONE

### Q3: 日志显示广告加载成功，但看不到广告
**原因**：
1. 容器在屏幕外（需要滚动）
2. 容器被其他视图遮挡
3. 容器大小为0

**解决**：
- 检查`Container parent`信息
- 检查布局层级
- 使用Layout Inspector查看视图层级

### Q4: 日志显示"Ad is already loading"
**原因**：重复调用加载方法

**解决**：
- 这是正常的保护机制
- 等待当前加载完成
- 不需要处理

---

## 📝 日志示例（完整流程）

```
NATIVE_AD_TRACK: ═══════════════════════════════════════════════
NATIVE_AD_TRACK: 🎯 Native Ad Request
NATIVE_AD_TRACK:    Caller: FragOnboardLanguage.setupNativeAd:299
NATIVE_AD_TRACK:    Activity: ActivityOnboarding
NATIVE_AD_TRACK:    Container: FrameLayout@123456789
NATIVE_AD_TRACK:    Layout ResId: 2130903456
NATIVE_AD_TRACK:    Container visibility: 0 (0=VISIBLE)
NATIVE_AD_TRACK:    Container parent: LinearLayout
NATIVE_AD_TRACK: ═══════════════════════════════════════════════
NATIVE_AD_TRACK: → loadAdWithCallback() called, waiting for response...
NATIVE_AD_TRACK: 🔄 NativeAdManager.loadNewAd() - Starting ad load (0/3)
NATIVE_AD_TRACK:    Ad Unit ID: ca-app-pub-3940256099942544/2247696110
NATIVE_AD_TRACK: ✅ Native ad loaded from AdMob
NATIVE_AD_TRACK:    Headline: Test Ad
NATIVE_AD_TRACK:    Body: This is a test ad for development
NATIVE_AD_TRACK:    Advertiser: Google Ads
NATIVE_AD_TRACK: ✅ Ad added to cache pool (1/3)
NATIVE_AD_TRACK:    Pending callbacks: 1
NATIVE_AD_TRACK: → Callback received for: FragOnboardLanguage.setupNativeAd:299
NATIVE_AD_TRACK:    Ad object: NOT NULL
NATIVE_AD_TRACK: → Inflating ad view for: FragOnboardLanguage.setupNativeAd:299
NATIVE_AD_TRACK:    Ad headline: Test Ad
NATIVE_AD_TRACK:    Ad body: This is a test ad for development
NATIVE_AD_TRACK:    AdView inflated: NativeAdView
NATIVE_AD_TRACK: → Populating ad view...
NATIVE_AD_TRACK:    ✅ Ad view populated
NATIVE_AD_TRACK: → Adding ad view to container...
NATIVE_AD_TRACK:    Container child count before: 0
NATIVE_AD_TRACK:    Container child count after: 1
NATIVE_AD_TRACK:    Container visibility set to: VISIBLE
NATIVE_AD_TRACK: ✅ Native ad displayed successfully for: FragOnboardLanguage.setupNativeAd:299
```

