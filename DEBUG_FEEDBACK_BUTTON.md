# 🐛 反馈按钮调试指南

## 问题诊断流程

### 问题1: 悬浮按钮遮挡底部导航 ✅ 已修复
**原因**: Y 坐标设置为 72dp，不足以避开底部导航栏（56dp）+ 按钮自身（56dp）

**修复**: 增加到 128dp（56 + 56 + 16 安全边距）

```kotlin
val bottomMarginDp = 128  // 原来是 72
```

---

### 问题2: 点击反馈按钮无响应 ✅ 已修复
**根本原因**: 
1. `FloatingActionButton` 设置了 `clickable=true`，拦截了触摸事件
2. `OnTouchListener` 可能被子视图消费事件
3. `FLAG_NOT_FOCUSABLE` 与子视图的 `focusable=true` 冲突

**修复方案**:

#### 1. 布局修改（`feedback_floating_button.xml`）
```xml
<!-- 修改前 -->
<FrameLayout ...>  <!-- 无 clickable -->
    <FloatingActionButton clickable="true" focusable="true" />  <!-- ❌ 拦截事件 -->
</FrameLayout>

<!-- 修改后 -->
<FrameLayout 
    android:id="@+id/feedback_button_container"
    clickable="true"      <!-- ✅ 容器处理点击 -->
    focusable="true">
    <FloatingActionButton clickable="false" focusable="false" />  <!-- ✅ 不拦截 -->
</FrameLayout>
```

#### 2. 双重点击检测机制
```kotlin
// 方案1：OnTouchListener（支持拖动检测）
setupTouchListener()

// 方案2：OnClickListener（备用方案）
floatingView?.setOnClickListener {
    onFeedbackButtonClicked()
}
```

#### 3. 增强日志
现在每次点击都会输出详细日志：
```
FeedbackFloatingButton: ═══════════════════════════════════════════════
FeedbackFloatingButton: 📱 onFeedbackButtonClicked() START
FeedbackFloatingButton: ═══════════════════════════════════════════════
FeedbackFloatingButton: → Activity: MainActivity
FeedbackFloatingButton: → Is FragmentActivity: true
FeedbackFloatingButton: → Creating FeedbackBottomSheetDialog...
FeedbackFloatingButton: → Getting supportFragmentManager...
FeedbackFloatingButton: → FragmentManager: androidx.fragment.app.FragmentManagerImpl@...
FeedbackFloatingButton: → Showing dialog...
FeedbackFloatingButton: ✅ Dialog.show() called successfully
FeedbackFloatingButton: ═══════════════════════════════════════════════
```

---

## 如何测试

### 1. 查看悬浮按钮是否显示

**预期**:
- 应用启动 3 秒后，右下角出现半透明黑色圆形按钮
- 按钮位置：距离底部 128dp，距离右边 24dp
- 按钮不遮挡底部导航栏

**检查日志**:
```bash
adb logcat | grep "FeedbackFloatingButton.*Floating button shown"
```

**预期输出**:
```
FeedbackFloatingButton: ✅ Floating button shown at y=384 (128dp)
```

---

### 2. 测试点击响应

**操作**: 点击悬浮按钮

**预期行为**:
- 弹出底部弹窗，显示反馈界面（Emoji 选择）
- 如果失败，显示 Toast 提示错误信息

**检查日志**:
```bash
adb logcat | grep -E "FeedbackFloatingButton|FeedbackBottomSheet"
```

**成功日志**:
```
FeedbackFloatingButton: 🎯 Detected as CLICK, opening dialog...
FeedbackFloatingButton: 📱 onFeedbackButtonClicked() START
FeedbackFloatingButton: → Activity: MainActivity
FeedbackFloatingButton: → Is FragmentActivity: true
FeedbackFloatingButton: ✅ Dialog.show() called successfully
FeedbackBottomSheet: onCreateDialog() called
FeedbackBottomSheet: onCreateView() called
```

**失败日志** (如果出现):
```
FeedbackFloatingButton: ❌ Exception in onFeedbackButtonClicked
FeedbackFloatingButton: ❌ Exception type: java.lang.XXXException
FeedbackFloatingButton: ❌ Exception message: ...
```

---

### 3. 测试触摸事件

**操作**: 按住并拖动悬浮按钮

**预期行为**:
- 按钮可以拖动到新位置
- 拖动后松手，按钮停留在新位置（不触发点击）

**检查日志**:
```bash
adb logcat | grep "FeedbackFloatingButton.*Touch"
```

**预期输出**:
```
FeedbackFloatingButton: Touch DOWN at (...)
FeedbackFloatingButton: Touch MOVE - dragging
FeedbackFloatingButton: Touch UP - isMoved: true
FeedbackFloatingButton: Detected as DRAG, position saved
```

---

### 4. 测试返回键

**操作**: 按返回键

**预期行为**:
- **如果停留时间 < 1 分钟**:
  - 第一次：显示 Toast "再按一次退出应用"
  - 第二次（2秒内）：弹出退出挽留对话框
- **如果停留时间 > 1 分钟**:
  - 直接退出应用

**检查日志**:
```bash
adb logcat | grep "ExitInterceptor"
```

**预期输出**:
```
ExitInterceptor: ⬅️ onBackPressed() - stayDuration: 30s
ExitInterceptor: ⚠️ First back press - waiting for second press
ExitInterceptor: 🚨 Double back press detected - showing exit dialog
ExitInterceptor: 📱 Exit dialog shown (stay duration < 1 min)
```

---

## 常见问题排查

### Q1: 悬浮按钮不显示

**检查清单**:
1. ✅ 是否在 `MainActivity.onCreate()` 调用了 `initFeedbackSystem()`？
2. ✅ 是否延迟了 3 秒显示？
3. ✅ 是否有异常日志？

**排查命令**:
```bash
adb logcat | grep "FeedbackFloatingButton"
```

**预期日志**:
```
MainActivity: 💬 Initializing feedback system...
FeedbackFloatingButton: ✅ Floating button shown at y=384 (128dp)
```

---

### Q2: 点击无响应

**检查清单**:
1. ✅ 是否看到 "Touch DOWN" 日志？（证明触摸被捕获）
2. ✅ 是否看到 "onFeedbackButtonClicked" 日志？（证明点击被识别）
3. ✅ Activity 是否是 FragmentActivity？
4. ✅ 是否有异常抛出？

**排查命令**:
```bash
adb logcat | grep -E "FeedbackFloatingButton.*(Touch|Click|Exception)"
```

**如果看到 "Touch UP - isMoved: true"**:
- 说明系统误判为拖动，而非点击
- 可能手指抖动导致移动超过 10px 阈值
- **解决方案**: 降低阈值（目前是 10px）

**如果看到 "Activity is not FragmentActivity"**:
- MainActivity 不继承 FragmentActivity
- **解决方案**: 检查 MainActivity 是否继承自 `androidx.fragment.app.FragmentActivity` 或 `AppCompatActivity`

---

### Q3: 弹窗不显示

**检查清单**:
1. ✅ 是否成功调用了 `dialog.show()`？
2. ✅ FragmentManager 是否为 null？
3. ✅ BottomSheetDialog 的布局是否存在？

**排查命令**:
```bash
adb logcat | grep "FeedbackBottomSheet"
```

**预期日志**:
```
FeedbackBottomSheet: onCreateDialog() called
FeedbackBottomSheet: onCreateView() called
FeedbackBottomSheet: onViewCreated() called
```

**如果没有这些日志**:
- `dialog.show()` 调用失败或被忽略
- 可能 FragmentManager 状态异常（Activity 正在销毁等）

---

### Q4: 返回键被拦截，无法导航

**症状**: 按返回键时，总是显示 "再按一次退出应用"

**原因**: `ExitInterceptor.onBackPressed()` 在停留时间 < 1 分钟时总是返回 `true`

**检查**:
```bash
adb logcat | grep "ExitInterceptor"
```

**预期日志**:
```
ExitInterceptor: ⬅️ onBackPressed() - stayDuration: XXs
ExitInterceptor: ⚠️ First back press - waiting for second press  // ✅ 正常
```

**如果每次都返回 true**:
- 这是预期行为（在 MainActivity 中拦截退出）
- MainActivity 是根 Activity，按返回键会退出应用
- 如果有多个 Activity，应该只在最后一个 Activity 拦截

---

## 完整测试脚本

将以下脚本保存为 `test_feedback.sh`，赋予执行权限后运行：

```bash
#!/bin/bash
# 反馈系统测试脚本

echo "清空日志..."
adb logcat -c

echo "✅ 日志已清空"
echo ""
echo "请进行以下测试："
echo "1. 等待应用启动 3 秒，观察悬浮按钮是否显示"
echo "2. 点击悬浮按钮，查看是否弹出反馈弹窗"
echo "3. 拖动悬浮按钮，测试位置是否改变"
echo "4. 按返回键，测试退出拦截逻辑"
echo ""
echo "测试完成后，按 Ctrl+C 停止日志捕获"
echo ""
echo "开始捕获日志..."
echo "═══════════════════════════════════════════════"

adb logcat | grep -E "FeedbackFloatingButton|FeedbackBottomSheet|ExitInterceptor|MainActivity.*Feedback"
```

**使用方法**:
```bash
chmod +x test_feedback.sh
./test_feedback.sh
```

---

## Git 提交记录

```
4d2df55 - 🐛 Fix feedback button click not working
ff0bc54 - 🐛 Fix feedback button position and back press issue
c008fe9 - 🔧 Fix FeedbackBottomSheetDialog getInstance() call
de4e911 - 🔧 Fix FeedbackManager getInstance() call from Java
d2af982 - ✨ Implement minimal feedback system v1.9.21
```

---

## 技术细节

### WindowManager.LayoutParams

```kotlin
WindowManager.LayoutParams(
    WRAP_CONTENT,  // width
    WRAP_CONTENT,  // height
    TYPE_APPLICATION_PANEL,  // type（应用内悬浮窗）
    FLAG_NOT_FOCUSABLE,  // flags（不获取焦点，不阻止其他窗口交互）
    PixelFormat.TRANSLUCENT  // format（支持半透明）
)
```

**关键点**:
- `FLAG_NOT_FOCUSABLE`: 允许触摸事件穿透到下层窗口
- 但这与子视图的 `focusable=true` 冲突，导致触摸事件混乱
- **解决方案**: 子视图全部设置 `focusable=false`

### Gravity 坐标系

```kotlin
gravity = Gravity.BOTTOM or Gravity.END
x = 24dp  // 从右边往左偏移
y = 128dp  // 从底部往上偏移
```

**注意**:
- `Gravity.BOTTOM or Gravity.END` 表示参考点是右下角
- `x` 的正值是往左移，负值是往右移
- `y` 的正值是往上移，负值是往下移

---

**END OF DEBUG GUIDE**

