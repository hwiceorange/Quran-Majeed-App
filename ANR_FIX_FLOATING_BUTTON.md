# 🔥 ANR 修复：反馈浮动按钮

## 🐛 问题描述

### 现象
- 应用启动后出现 "Process system isn't responding" ANR 对话框
- 设备系统无响应，需要强制等待或关闭应用
- 日志显示：`ANR in com.quran.quranaudio.online`

### 根本原因
反馈系统的浮动按钮在使用 `WindowManager.addView()` 时触发了 ANR，原因如下：

1. **Window Token 无效**
   - `TYPE_APPLICATION_PANEL` 窗口类型必须关联到有效的 Activity window token
   - 如果 Activity 不在前台或正在销毁，token 无效会导致 `BadTokenException` 或系统 ANR

2. **Activity 生命周期未检查**
   - 延迟 3 秒后执行 `feedbackFloatingButton.show()`
   - 但没有检查 Activity 是否仍然有效（可能已 finishing/destroyed）

3. **缺少 Window Token 显式绑定**
   - `WindowManager.LayoutParams` 没有显式设置 `token`
   - 导致系统无法正确关联窗口到 Activity

---

## ✅ 修复方案

### 1. `FeedbackFloatingButton.kt` - 添加 Activity 状态检查

#### 修复点 1: 检查 Activity 状态
```kotlin
// 检查 Activity 是否正在销毁
if (activity.isFinishing || activity.isDestroyed) {
    android.util.Log.w("FeedbackFloatingButton", "⚠️ Activity is finishing/destroyed, abort showing floating button")
    return
}
```

#### 修复点 2: 检查 Window Token 可用性
```kotlin
// 获取 decorView 并检查 token
val decorView = activity.window?.decorView
if (decorView == null || decorView.windowToken == null) {
    android.util.Log.w("FeedbackFloatingButton", "⚠️ Activity window token not ready, abort showing floating button")
    return
}
```

#### 修复点 3: 显式设置 Window Token
```kotlin
params = WindowManager.LayoutParams(
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.WRAP_CONTENT,
    WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
    PixelFormat.TRANSLUCENT
).apply {
    gravity = Gravity.BOTTOM or Gravity.END
    x = (24 * density).toInt()
    y = bottomMarginPx
    // 🆕 显式绑定 token
    token = decorView.windowToken
}
```

#### 修复点 4: 细化异常处理
```kotlin
} catch (e: android.view.WindowManager.BadTokenException) {
    android.util.Log.e("FeedbackFloatingButton", "❌ BadTokenException: Activity window token invalid", e)
    // 不显示 Toast，因为 Activity 可能已不可用
} catch (e: IllegalStateException) {
    android.util.Log.e("FeedbackFloatingButton", "❌ IllegalStateException: Activity state invalid", e)
} catch (e: Exception) {
    android.util.Log.e("FeedbackFloatingButton", "❌ Failed to show floating button", e)
}
```

---

### 2. `MainActivity.java` - 增强初始化检查

#### 修复点 1: 增加延迟时间
```java
// 从 3 秒增加到 5 秒，确保 Activity 完全就绪
new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
    // ...
}, 5000); // 原来是 3000
```

#### 修复点 2: 延迟回调中检查 Activity 状态
```java
// 在显示浮动按钮前检查 Activity 状态
if (isFinishing() || isDestroyed()) {
    android.util.Log.w("MainActivity", "⚠️ Activity is finishing/destroyed, skip showing floating button");
    return;
}

// 检查窗口可用性
if (getWindow() == null || getWindow().getDecorView().getWindowToken() == null) {
    android.util.Log.w("MainActivity", "⚠️ Activity window not ready, skip showing floating button");
    return;
}
```

#### 修复点 3: 详细日志输出
```java
android.util.Log.d("MainActivity", "→ Activity state validated, creating floating button...");
android.util.Log.d("MainActivity", "   → isFinishing=" + isFinishing() + ", isDestroyed=" + isDestroyed());
android.util.Log.d("MainActivity", "   → hasWindowToken=" + (getWindow().getDecorView().getWindowToken() != null));
```

---

## 🧪 测试验证

### 测试步骤
1. 清除应用缓存和数据
2. 重新安装 APK
3. 启动应用并观察日志
4. 等待 5 秒后检查浮动按钮是否正常显示
5. 快速切换应用（测试 Activity 状态变化）
6. 旋转屏幕（测试配置变化）

### 预期日志输出
```
MainActivity: 💬 Initializing feedback system...
MainActivity:    → Current Activity state: isFinishing=false, isDestroyed=false
MainActivity: ✅ Feedback system initialized (button will show after 5s)

... 5 秒后 ...

MainActivity: → Activity state validated, creating floating button...
MainActivity:    → isFinishing=false, isDestroyed=false
MainActivity:    → hasWindowToken=true
FeedbackFloatingButton: ✅ Activity state check passed
FeedbackFloatingButton:    → isFinishing: false
FeedbackFloatingButton:    → isDestroyed: false
FeedbackFloatingButton:    → hasWindowToken: true
FeedbackFloatingButton: → Attempting to add FloatingView to WindowManager...
FeedbackFloatingButton:    → Window Token: android.view.ViewRootImpl$W@xxxxxxx
FeedbackFloatingButton: ✅ Floating button shown successfully at y=xxx (128dp)
MainActivity: ✅ Feedback floating button shown
```

### 如果仍有问题
```
FeedbackFloatingButton: ⚠️ Activity is finishing/destroyed, abort showing floating button
或
FeedbackFloatingButton: ⚠️ Activity window token not ready, abort showing floating button
或
FeedbackFloatingButton: ❌ BadTokenException: Activity window token invalid
```

这些日志会帮助诊断问题，但**不会导致 ANR**，只是悄悄跳过显示浮动按钮。

---

## 🔍 验证命令

### 实时查看日志
```bash
adb logcat | grep -E "MainActivity|FeedbackFloatingButton|ANR|BadToken"
```

### 查看 ANR 日志
```bash
adb logcat -d | grep -A 20 "ANR in com.quran.quranaudio.online"
```

### 检查 WindowManager 错误
```bash
adb logcat -d | grep -E "WindowManager.*BadToken|WindowManager.*unable to add window"
```

---

## 📊 技术细节

### WindowManager.LayoutParams.TYPE_APPLICATION_PANEL

**特性**:
- 必须关联到父窗口的 token
- 只能在 Activity 前台显示时使用
- 如果父窗口被销毁，子窗口自动移除

**替代方案** (未采用，因为需要权限):
- `TYPE_APPLICATION_OVERLAY` - 需要 `SYSTEM_ALERT_WINDOW` 权限
- `TYPE_TOAST` - Android 8.0+ 已废弃

**为什么选择 TYPE_APPLICATION_PANEL**:
- 无需额外权限
- 生命周期自动关联到 Activity
- 适合应用内浮动按钮

### Activity 生命周期关键时机

```
onCreate() 
  ↓
onStart()
  ↓
onResume() ✅ Window token 可用
  ↓
[用户交互]
  ↓
onPause()
  ↓
onStop()
  ↓
onDestroy() ❌ Window token 无效
```

**最佳实践**:
- 在 `onResume()` 之后添加窗口
- 在 `onPause()` 之前移除窗口
- 延迟添加需检查 `isFinishing()` 和 `isDestroyed()`

---

## 🚀 预期效果

### ✅ 修复后
1. **无 ANR**: 应用启动流畅，无系统无响应对话框
2. **安全降级**: 如果 Activity 状态不满足，悄悄跳过显示浮动按钮
3. **详细日志**: 出现问题时有清晰的日志定位原因
4. **用户体验**: 浮动按钮在 Activity 完全就绪后（5秒）平滑显示

### ⚠️ 已知限制
- 如果用户在启动后 5 秒内退出应用，浮动按钮不会显示（这是预期行为）
- 快速旋转屏幕可能导致浮动按钮重建（会在新 Activity 的 5 秒后重新显示）

---

## 📝 版本历史

### v1.9.22 (2025-12-26)
- 🔥 修复反馈浮动按钮导致的 ANR
- ✅ 添加 Activity 状态检查（isFinishing, isDestroyed）
- ✅ 添加 Window Token 可用性检查
- ✅ 显式绑定 WindowManager.LayoutParams.token
- ✅ 增加初始化延迟时间（3秒 → 5秒）
- ✅ 细化异常处理（BadTokenException, IllegalStateException）
- 📝 添加详细的诊断日志

---

**修复后请重新测试并提供日志！** 🚀

