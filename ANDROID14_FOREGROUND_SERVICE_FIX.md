# Android 14 前台服务类型修复

## 🔍 问题诊断

### 崩溃日志
```
Caused by: android.app.MissingForegroundServiceTypeException: Starting FGS without a type
callerApp=ProcessRecord{...} targetSDK=35

at TranslationDownloadService.showNotification(TranslationDownloadService.kt:114)
at TranslationDownloadService.onStartCommand(TranslationDownloadService.kt:101)
```

### 根本原因
- **Android 14 (API 35)** 引入了新的前台服务限制
- 启动前台服务必须在 `AndroidManifest.xml` 中声明 `foregroundServiceType`
- `startForeground()` 调用时需要传递服务类型参数
- 应用 `targetSDK=35`，触发了 Android 14 的强制检查

### 影响范围
当用户选择非英语/阿语的语言后，点击"选择古兰经翻译版本"时：
1. 引导页面成功选择翻译版本（如印尼语）
2. 后台启动 `TranslationDownloadService` 下载翻译
3. 服务调用 `startForeground()` 时崩溃
4. 应用闪退

---

## ✅ 修复方案

### 修复1：AndroidManifest.xml - 声明服务类型

**文件：** `app/src/main/AndroidManifest.xml`

**修改前：**
```xml
<service android:name=".quran_module.utils.services.TranslationDownloadService" />
<service android:name=".quran_module.utils.services.RecitationChapterDownloadService" />
<service android:name=".quran_module.utils.services.KFQPCScriptFontsDownloadService" />
```

**修改后：**
```xml
<service android:name=".quran_module.utils.services.TranslationDownloadService" 
         android:foregroundServiceType="dataSync" />
<service android:name=".quran_module.utils.services.RecitationChapterDownloadService" 
         android:foregroundServiceType="dataSync" />
<service android:name=".quran_module.utils.services.KFQPCScriptFontsDownloadService" 
         android:foregroundServiceType="dataSync" />
```

**说明：**
- `dataSync` - 用于下载和同步数据的前台服务类型
- 符合这些服务的实际用途（下载古兰经翻译、章节音频、字体等）

---

### 修复2：TranslationDownloadService.kt - 更新 startForeground() 调用

**文件：** `app/.../services/TranslationDownloadService.kt`

**修改前：**
```kotlin
private fun showNotification(...) {
    notifManager.cancel(NOTIF_ID)
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    startForeground(notifId, notification)
}
```

**修改后：**
```kotlin
private fun showNotification(...) {
    notifManager.cancel(NOTIF_ID)
    ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
    
    // 🔧 Android 14+ requires foreground service type
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
            notifId,
            notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    } else {
        startForeground(notifId, notification)
    }
}
```

**说明：**
- `Build.VERSION_CODES.UPSIDE_DOWN_CAKE` = API 34 (Android 14)
- Android 14+ 使用三参数版本的 `startForeground()`
- Android 13 及以下使用原来的两参数版本
- `FOREGROUND_SERVICE_TYPE_DATA_SYNC` 对应 `dataSync` 类型

---

### 修复3：RecitationChapterDownloadService.kt - 批量更新

**文件：** `app/.../services/RecitationChapterDownloadService.kt`

**修改的 startForeground() 调用位置：**
1. `onCreate()` - 第72行
2. `onStartCommand()` - 第95行
3. `showNotification()` - 第209行

**添加的辅助方法：**
```kotlin
/**
 * 🔧 Android 14+ 兼容的 startForeground 方法
 */
private fun startForegroundCompat(notifId: Int, notification: Notification) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        startForeground(
            notifId,
            notification,
            android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
    } else {
        startForeground(notifId, notification)
    }
}
```

**说明：**
- 创建辅助方法避免代码重复
- 三处调用全部替换为 `startForegroundCompat()`

---

### 修复4：KFQPCScriptFontsDownloadService.kt - 单处更新

**文件：** `app/.../services/KFQPCScriptFontsDownloadService.kt`

**修改位置：** 第315行

**修改方式：** 同 `TranslationDownloadService`，添加 Android 14 版本检查

---

## 📊 修改文件清单

### AndroidManifest.xml
- ✅ 添加 `TranslationDownloadService` 的 `foregroundServiceType="dataSync"`
- ✅ 添加 `RecitationChapterDownloadService` 的 `foregroundServiceType="dataSync"`
- ✅ 添加 `KFQPCScriptFontsDownloadService` 的 `foregroundServiceType="dataSync"`

### 服务文件 (3个)
1. ✅ **TranslationDownloadService.kt** - 1处修改
2. ✅ **RecitationChapterDownloadService.kt** - 3处修改 + 辅助方法
3. ✅ **KFQPCScriptFontsDownloadService.kt** - 1处修改

**总计：** 4个文件，8处修改

---

## 🧪 测试流程

### 准备工作
```bash
cd /Users/huwei/AndroidStudioProjects/quran0
adb uninstall com.quran.quranaudio.online
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 测试1：印尼语翻译下载 ✅

**步骤：**
1. 打开应用
2. 在多语言页选择 **印尼语 (Indonesian)**
3. 点击 **Continue**
4. 等待引导页加载
5. 选择一个翻译版本（如 "Tafsir Al-Qur'an Kemenag"）
6. 点击 **Continue**

**预期结果：**
- ✅ 后台下载服务启动成功
- ✅ 显示下载通知
- ✅ 应用不崩溃
- ✅ 引导流程继续

### 测试2：其他语言（乌尔都语、马来语等）

**步骤：** 同上，选择其他非英语/阿语的语言

**预期结果：** 同测试1

### 测试3：章节音频下载

**步骤：**
1. 进入古兰经阅读页面
2. 选择一个章节
3. 点击下载音频按钮
4. 观察下载过程

**预期结果：**
- ✅ 下载服务启动成功
- ✅ 显示下载进度通知
- ✅ 应用不崩溃

---

## 📋 Android 14 前台服务类型参考

### 常用服务类型

| 类型 | 用途 | 本项目使用 |
|------|------|-----------|
| `dataSync` | 数据同步、下载 | ✅ 翻译/音频/字体下载 |
| `mediaPlayback` | 媒体播放 | ✅ 古兰经朗诵播放 |
| `location` | 位置服务 | ❌ 未使用 |
| `phoneCall` | 电话通话 | ❌ 未使用 |
| `camera` | 相机使用 | ❌ 未使用 |
| `microphone` | 麦克风使用 | ❌ 未使用 |

### 权限要求

**Android 14+ 前台服务强制要求：**
1. 在 `AndroidManifest.xml` 中声明 `foregroundServiceType`
2. 在 `startForeground()` 调用时传递对应的类型常量
3. 如果需要，在运行时请求相关权限（如 `location` 需要位置权限）

**本项目：**
- `dataSync` 不需要额外的运行时权限
- `mediaPlayback` 不需要额外的运行时权限

---

## ✅ 验证清单

- [x] **编译成功** - BUILD SUCCESSFUL
- [x] **AndroidManifest 更新** - 3个服务声明了类型
- [x] **TranslationDownloadService** - startForeground() 更新
- [x] **RecitationChapterDownloadService** - 3处 startForeground() 更新
- [x] **KFQPCScriptFontsDownloadService** - startForeground() 更新
- [ ] **测试印尼语翻译下载** - 待用户测试
- [ ] **测试其他语言翻译下载** - 待用户测试
- [ ] **测试音频下载** - 待用户测试

---

## 📚 相关文档

- [Android 14 Foreground Service Types](https://developer.android.com/about/versions/14/changes/fgs-types-required)
- [ServiceInfo.ForegroundServiceType](https://developer.android.com/reference/android/content/pm/ServiceInfo#foreground-service-types)
- [Android 14 Behavior Changes](https://developer.android.com/about/versions/14/behavior-changes-14)

---

**修复完成时间：** 2025-11-18
**修复人员：** AI Assistant  
**状态：** ✅ 编译成功，等待测试验证
**APK位置：** `app/build/outputs/apk/debug/app-debug.apk`

