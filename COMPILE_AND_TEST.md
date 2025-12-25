# 🔧 编译并测试

## ✅ 已修复的编译错误

1. ✅ `InterstitialAdManager.INSTANCE` → `InterstitialAdManager.Companion.getInstance()`
2. ✅ `AdConfig.AD_INTERSTITIAL` → 使用`showAdIfAvailable(activity)`单参数版本
3. ✅ `onBackPressed()` 过时警告 → 使用`getOnBackPressedDispatcher().onBackPressed()`

---

## 🚀 编译命令

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App

# 在Android Studio中点击 Build → Make Project
# 或者在终端运行：
./gradlew assembleDebug
```

---

## 📝 测试日志命令

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App

# 清空日志并实时监控所有关键事件
adb logcat -c && echo "✅ 日志已清空，请在Android Studio运行应用" && adb logcat | grep -E "DIAGNOSE|HomeFragment|TasbihFragment|NativeAd|InterstitialAd"
```

---

## 📋 完整测试流程

### 1️⃣ 测试多语言页原生广告

```bash
# 卸载并重新安装
adb uninstall com.quran.quranaudio.online
# 然后在Android Studio运行

# 进入多语言页面，向下滚动查看底部
```

**预期**：看到原生广告（Test Ad）

---

### 2️⃣ 测试Tasbih插屏广告 ⭐

```bash
# 1. 进入主页
# 2. 点击 Dhikr (Tasbih)
# 3. 点击Tasbih至少50次（完成每日任务）
# 4. 点击返回按钮（左上角）
# 5. 应该看到插屏广告弹出
```

**预期日志**：
```
DIAGNOSE: →→ TasbihFragment: Back button clicked
DIAGNOSE: →→ Checking if Dhikr quest completed: true
DIAGNOSE: →→ Dhikr completed! Showing interstitial ad before navigating back
DIAGNOSE: ✅ Interstitial ad shown, delaying navigation
InterstitialAdManager: 📺 Showing interstitial ad
DIAGNOSE: ✅ Ad display complete, navigating back
```

**如果任务未完成**：
```
DIAGNOSE: →→ Checking if Dhikr quest completed: false
DIAGNOSE: →→ Dhikr not completed, navigating back directly
```

---

### 3️⃣ 测试Verse of the Day原生广告 ⭐

```bash
# 1. 在主页向下滚动
# 2. 找到 "Verse of the Day" 卡片
# 3. 卡片底部应该有原生广告
```

**预期日志**：
```
DIAGNOSE: →→ HomeFragment.initializeVerseOfDayCard() called
DIAGNOSE: →→ Calling loadVOTDNativeAd()...
DIAGNOSE: →→ HomeFragment.loadVOTDNativeAd() called
DIAGNOSE: →→ votdNativeAdContainer: NOT NULL
DIAGNOSE: →→ User subscribed: false
DIAGNOSE: ✅ NativeAdHelper.displayNativeAdWithAutoLoad returned for VOTD
NativeAdHelper: ✅ Native ad displayed successfully
```

---

## 🔍 关键修改说明

### TasbihFragment.java

**修改前（错误）**：
```java
com.quranaudio.common.ad.InterstitialAdManager.INSTANCE.showAdIfAvailable(
    getActivity(),
    com.quranaudio.common.ad.AdConfig.AD_INTERSTITIAL,
    new Runnable() { ... }
);
```

**修改后（正确）**：
```java
boolean adShown = com.quranaudio.common.ad.InterstitialAdManager.Companion.getInstance().showAdIfAvailable(getActivity());

if (adShown) {
    // 延迟500ms后返回
    new android.os.Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
            navigateBack(v);
        }
    }, 500);
} else {
    // 直接返回
    navigateBack(v);
}
```

**原因**：
1. Kotlin单例从Java调用需要使用`.Companion.getInstance()`
2. `showAdIfAvailable`方法只接收`Activity`参数，返回`boolean`
3. 使用延迟返回模式，让广告有时间渲染

---

### HomeFragment.java

**修改**：添加了详细的诊断日志

```java
private void loadVOTDNativeAd() {
    android.util.Log.d("DIAGNOSE", "→→ HomeFragment.loadVOTDNativeAd() called");
    // ... 详细的null检查和订阅状态检查
    
    com.quranaudio.common.ad.NativeAdHelper.Companion.getInstance().displayNativeAdWithAutoLoad(
        getActivity(),
        votdNativeAdContainer,
        com.quran.quranaudio.quiz.R.layout.layout_ad_native_small_wrapper
    );
}
```

---

## ⚠️ 测试注意事项

1. **Tasbih插屏**：必须完成至少50次点击才会触发
2. **VOTD原生**：如果用户已订阅，广告会被隐藏
3. **多语言原生**：可能需要向下滚动才能看到（在底部）
4. **首次安装**：需要等待几秒让广告池预加载

---

## 🛠️ 调试技巧

### 查看插屏广告状态
```bash
adb logcat -d | grep "InterstitialAdManager" | tail -30
```

### 查看原生广告状态
```bash
adb logcat -d | grep "NativeAdManager" | tail -30
```

### 查看Tasbih任务状态
```bash
adb logcat -d | grep -E "TasbihFragment|dailyQuestCompleted" | tail -20
```

### 实时监控所有广告事件
```bash
adb logcat | grep -E "ad loaded|ad displayed|ad shown|ad dismissed|DIAGNOSE"
```

