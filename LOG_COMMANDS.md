# 日志获取命令

## 🚀 快速开始

### 方法1: 使用自动化脚本（推荐）

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App
./GET_DETAILED_LOG.sh
```

**然后**:
1. 在另一个终端卸载应用: `adb uninstall com.quran.quranaudio.online`
2. 在Android Studio重新运行
3. 等待进入多语言页面
4. 观察日志输出
5. Ctrl+C停止，查看 `detailed_ad_log.txt`

---

### 方法2: 手动命令（更灵活）

```bash
# 清空日志
adb logcat -c

# 实时查看详细日志
adb logcat | grep -E "DIAGNOSE|DIAGNOSE_ERROR|FATAL|FragOnboardLanguage|NativeAdHelper|NativeAdManager"
```

---

### 方法3: 只看崩溃日志

```bash
adb logcat | grep -E "FATAL EXCEPTION|AndroidRuntime|Caused by" -A 30
```

---

### 方法4: 分步骤查看

**步骤1**: 清空并卸载
```bash
adb logcat -c
adb uninstall com.quran.quranaudio.online
```

**步骤2**: 在Android Studio运行应用

**步骤3**: 实时查看日志
```bash
adb logcat | grep -E "DIAGNOSE|FragOnboard|NativeAd"
```

---

## 📊 预期日志内容

### ✅ 正常流程应该看到：

```
DIAGNOSE: App.onCreate() START
DIAGNOSE: ✅ App.onCreate() COMPLETED SUCCESSFULLY
DIAGNOSE: SplashScreenActivity.onCreate() START
DIAGNOSE: ✅ SplashScreenActivity.onCreate() COMPLETED
ActivitySplash: 🎯 First launch detected - Showing Onboarding
DIAGNOSE: ========================================
DIAGNOSE: FragOnboardLanguage.onViewCreated() START
DIAGNOSE: ========================================
DIAGNOSE: ✅ super.onViewCreated() completed
DIAGNOSE: → Setting up language list...
DIAGNOSE: ✅ Language list setup completed
DIAGNOSE: → Setting up native ad...
DIAGNOSE: →→ setupNativeAd() called
DIAGNOSE: →→ Native ad container found: true
DIAGNOSE: →→ User subscribed: false
DIAGNOSE: →→ Calling NativeAdHelper.displayNativeAdWithAutoLoad()
DIAGNOSE: →→→ NativeAdHelper.displayNativeAdWithAutoLoad() CALLED
DIAGNOSE: →→→ Activity: FragOnboardLanguage
DIAGNOSE: →→→ Checking subscription: false
DIAGNOSE: →→→ Getting cached ad from NativeAdManager...
DIAGNOSE: →→→ Cached ad result: FOUND
DIAGNOSE: →→→ Displaying cached ad...
DIAGNOSE: ✅ Cached ad displayed successfully
DIAGNOSE: ✅ Native ad setup initiated
DIAGNOSE: ========================================
DIAGNOSE: ✅ FragOnboardLanguage.onViewCreated() COMPLETED
DIAGNOSE: ========================================
```

### ❌ 如果有问题会看到：

**广告缓存为空**:
```
DIAGNOSE: →→→ Cached ad result: NULL
DIAGNOSE: →→→ No cached ad, loading dynamically...
DIAGNOSE: →→→ Dynamic load callback received
DIAGNOSE: →→→ Loaded ad: SUCCESS/NULL
```

**订阅检查错误**:
```
DIAGNOSE: →→ User subscribed: true
DIAGNOSE: →→ User is subscribed, hiding ad container
```

**崩溃**:
```
DIAGNOSE_ERROR: ❌ setupNativeAd() FAILED
DIAGNOSE_ERROR: ❌ Exception type: ...
DIAGNOSE_ERROR: ❌ Exception message: ...
FATAL EXCEPTION: main
...
```

---

## 🔍 日志分析要点

1. **检查是否进入多语言页面**
   - 看有没有 `FragOnboardLanguage.onViewCreated`

2. **检查广告缓存状态**
   - 看 `Cached ad result: FOUND` 还是 `NULL`

3. **检查订阅状态**
   - 看 `User subscribed: true/false`

4. **检查是否有异常**
   - 看有没有 `DIAGNOSE_ERROR` 或 `FATAL EXCEPTION`

---

## 📋 完整测试流程

```bash
# 1. 打开终端1 - 日志监控
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App
adb logcat -c
adb logcat | grep -E "DIAGNOSE|FATAL|FragOnboard|NativeAd" | tee full_test_log.txt

# 2. 打开终端2 - 清理并运行
adb uninstall com.quran.quranaudio.online
# 然后在Android Studio点击运行

# 3. 观察终端1的日志输出

# 4. 测试完成后，Ctrl+C停止日志，查看文件
cat full_test_log.txt
```

---

## 💡 快速命令（复制粘贴）

### 清理重测
```bash
adb logcat -c && adb uninstall com.quran.quranaudio.online && echo "✅ 已清理，请在Android Studio运行" && adb logcat | grep -E "DIAGNOSE|FATAL|FragOnboard|NativeAd"
```

### 查看崩溃
```bash
adb logcat -d | grep -A 50 "FATAL EXCEPTION"
```

### 查看最近日志
```bash
adb logcat -d | grep "DIAGNOSE" | tail -100
```

