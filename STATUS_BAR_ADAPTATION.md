# Android 35 状态栏适配说明

## 问题描述
升级到 Android 35 后，应用所有界面出现状态栏与应用功能界面重叠的问题。这是由于 Android 系统在新版本中强制要求应用支持 Edge-to-Edge 显示模式。

## 解决方案

### 1. 全局透明状态栏设置
- 在 `app/src/main/res/values/themes.xml` 中配置：
  ```xml
  <item name="android:windowDrawsSystemBarBackgrounds">true</item>
  <item name="android:statusBarColor">@android:color/transparent</item>
  <item name="android:navigationBarColor">@android:color/transparent</item>
  ```

### 2. 状态栏高度计算工具
- 创建了 `StatusBarUtils.kt` 工具类
- 提供状态栏高度获取、WindowInsets 处理等功能
- 兼容 Android 11+ 的新 API

### 3. 基类布局处理
- 修改 `BaseActivity.kt` 全局处理状态栏适配
- 自动为所有继承的 Activity 应用 Edge-to-Edge 模式
- 提供可重写的方法供子类自定义行为

### 4. XML布局标记要求
- 移除布局文件中的 `android:fitsSystemWindows="true"`
- 改用代码动态处理系统栏内边距

### 5. 特殊页面排除机制
- 为全屏页面（如 `LiveActivity`）提供排除机制
- 重写 `isExcludedFromSystemBarInsets()` 返回 `true`

## 使用方法

### 普通Activity
继承 `BaseActivity` 即可自动适配：
```kotlin
class YourActivity : BaseActivity() {
    // 自动应用状态栏适配
}
```

### 自定义状态栏样式
```kotlin
override fun isLightStatusBar(): Boolean {
    return false // 深色状态栏
}
```

### 排除系统栏处理
```kotlin
override fun isExcludedFromSystemBarInsets(): Boolean {
    return true // 全屏模式
}
```

### 手动应用内边距
```kotlin
// 为特定View应用状态栏内边距
applySystemBarInsetsToView(myView, applyTop = true, applyBottom = false)
```

## 兼容性
- 支持 Android 6.0 (API 23) 及以上版本
- 优化支持 Android 11+ (API 30) 的新 WindowInsets API
- 特别针对 Android 35 的 Edge-to-Edge 要求进行适配

## 注意事项
1. 确保所有自定义 Activity 继承正确的 BaseActivity
2. 对于需要特殊处理的页面，重写相应方法
3. 测试在不同 Android 版本上的显示效果
4. 注意刘海屏和打孔屏设备的适配

## 测试验证
构建成功，无编译错误。建议在以下设备上测试：
- Android 35 设备
- 不同屏幕尺寸的设备
- 刘海屏/打孔屏设备
- 折叠屏设备
