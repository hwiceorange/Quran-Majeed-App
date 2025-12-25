# 崩溃日志获取命令

## 方法1: 实时监控崩溃（推荐）

在Android Studio编译运行前，先在终端执行：

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App

# 清空旧日志
adb logcat -c

# 实时查看诊断日志和崩溃信息
adb logcat | grep -E "DIAGNOSE|DIAGNOSE_ERROR|FATAL EXCEPTION|AndroidRuntime|Caused by"
```

**说明**: 这会实时显示所有诊断日志和崩溃信息，崩溃时立即可见

---

## 方法2: 后台记录完整日志

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App

# 清空旧日志
adb logcat -c

# 后台记录所有日志
adb logcat > full_log.txt &

# 记下进程ID（用于后续停止）
echo "日志记录中... 崩溃后按 Ctrl+C 停止，或执行: killall adb"
```

**在Android Studio运行应用，崩溃后执行：**

```bash
# 停止日志记录
killall adb

# 查看诊断日志
grep "DIAGNOSE" full_log.txt

# 查看崩溃堆栈
grep -A 50 "FATAL EXCEPTION" full_log.txt

# 查看根本原因
grep -A 10 "Caused by:" full_log.txt
```

---

## 方法3: 使用自动化脚本

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App

# 运行脚本
./check_crash.sh
```

**说明**: 这个脚本会自动：
1. 清空日志
2. 卸载旧版本
3. 安装新APK
4. 启动应用
5. 记录日志15秒
6. 自动分析崩溃

**生成文件**:
- `crash_log_full.txt` - 完整日志
- `debug_report.txt` - 诊断摘要

---

## 方法4: 快速查看最近崩溃（Android Studio运行后）

应用崩溃后立即执行：

```bash
# 查看最近的崩溃
adb logcat -d | grep -A 50 "FATAL EXCEPTION" | head -60

# 查看诊断日志（找到崩溃前最后执行的步骤）
adb logcat -d | grep "DIAGNOSE"

# 查看根本原因
adb logcat -d | grep -A 10 "Caused by:"
```

---

## 关键日志说明

### ✅ 正常启动应该看到：

```
DIAGNOSE: ========================================
DIAGNOSE: App.onCreate() START
DIAGNOSE: ========================================
DIAGNOSE: ✅ super.onCreate() completed
DIAGNOSE: → Starting WebView isolation check...
DIAGNOSE: ✅ MAIN process - using default WebView
DIAGNOSE: → Starting AdFactory initialization...
DIAGNOSE: ✅ AdFactory.init() completed
DIAGNOSE: → Starting InterstitialAdManager initialization...
DIAGNOSE: ✅ InterstitialAdManager.initialize() completed
DIAGNOSE: ✅ InterstitialAdManager.preloadAd() completed
DIAGNOSE: → Starting NativeAdManager initialization...
DIAGNOSE: ✅ NativeAdManager.initialize() completed
DIAGNOSE: ✅ NativeAdManager.preloadAd() completed
DIAGNOSE: → Starting QuranDataProvider injection...
DIAGNOSE: ✅ QuranDataProvider injection completed
DIAGNOSE: → Registering ActivityLifecycleCallbacks...
DIAGNOSE: ✅ ActivityLifecycleCallbacks registered
DIAGNOSE: → Loading Typefaces...
DIAGNOSE: ✅ All Typefaces loaded
DIAGNOSE: → Configuring WorkManager...
DIAGNOSE: ✅ WorkManager configured
DIAGNOSE: → Creating NotificationChannels...
DIAGNOSE: ✅ NotificationChannels created
DIAGNOSE: ========================================
DIAGNOSE: ✅ App.onCreate() COMPLETED SUCCESSFULLY
DIAGNOSE: ========================================
DIAGNOSE: ========================================
DIAGNOSE: SplashScreenActivity.onCreate() START
DIAGNOSE: ========================================
DIAGNOSE: ✅ super.onCreate() completed
DIAGNOSE: → Setting content view...
DIAGNOSE: ✅ setContentView() completed
DIAGNOSE: → Initializing SharedPref...
DIAGNOSE: ✅ SharedPref initialized
DIAGNOSE: → Finding ProgressBar view...
DIAGNOSE: ✅ ProgressBar found: true
DIAGNOSE: → Getting AdConfig ad ID...
DIAGNOSE: ✅ Ad ID retrieved: ca-app-pub-xxxxx
DIAGNOSE: → Calling AdFactory.loadAppOpenAd()...
DIAGNOSE: ✅ AdFactory.loadAppOpenAd() called successfully
DIAGNOSE: → Calling requestConfig()...
DIAGNOSE: ✅ requestConfig() called successfully
DIAGNOSE: ========================================
DIAGNOSE: ✅ SplashScreenActivity.onCreate() COMPLETED
DIAGNOSE: ========================================
```

### ❌ 崩溃时会看到：

```
DIAGNOSE: → Starting AdFactory initialization...
DIAGNOSE_ERROR: ❌ AdFactory.init() FAILED
java.lang.NoSuchFieldError: INSTANCE
    at com.quran.quranaudio.online.App.onCreate(App.java:162)
    ...
FATAL EXCEPTION: main
Process: com.quran.quranaudio.online, PID: 12345
java.lang.RuntimeException: Unable to create application com.quran.quranaudio.online.App
    ...
Caused by: java.lang.NoSuchFieldError: No static field INSTANCE of type Lcom/quranaudio/common/ad/AdFactory;
    ...
```

**关键信息**:
1. **最后成功的步骤**: 崩溃前最后一个显示 `✅` 的 DIAGNOSE 日志
2. **失败的步骤**: 显示 `❌` 的 DIAGNOSE_ERROR 日志
3. **异常类型**: FATAL EXCEPTION 后的异常类名
4. **根本原因**: Caused by 后的详细错误

---

## 快速诊断流程

1. **Android Studio点击运行**
2. **立即打开终端执行**:
   ```bash
   adb logcat -c && adb logcat | grep -E "DIAGNOSE|FATAL"
   ```
3. **观察日志输出**
4. **崩溃时Ctrl+C停止**
5. **把日志发给我**

---

## 一键命令（复制粘贴即可）

```bash
cd /Users/huwei_kt126.com/Documents/Quran-Majeed-App && adb logcat -c && echo "✅ 日志已清空，请在Android Studio运行应用..." && adb logcat | grep -E "DIAGNOSE|DIAGNOSE_ERROR|FATAL EXCEPTION|AndroidRuntime|Caused by" | tee crash_diagnosis.txt
```

**说明**: 
- 自动清空旧日志
- 实时显示诊断信息
- 同时保存到 `crash_diagnosis.txt`
- 崩溃后Ctrl+C停止，查看文件即可

---

## 发给我的日志内容

请把以下内容发给我：

1. **诊断日志** (DIAGNOSE开头的行)
2. **错误日志** (DIAGNOSE_ERROR开头的行)  
3. **崩溃堆栈** (FATAL EXCEPTION部分，约30-50行)
4. **根本原因** (Caused by 部分，约5-10行)

可以直接把 `crash_diagnosis.txt` 文件内容发给我。

