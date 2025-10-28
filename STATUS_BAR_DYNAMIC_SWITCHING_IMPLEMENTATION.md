# 动态状态栏切换 - 实施总结

## 📋 实施日期
2025-10-23

## 🎯 目标
解决所有5个底部导航页面共用透明状态栏导致的对比度不足问题，实现差异化的状态栏配置。

---

## 🔧 核心改动

### 1. MainActivity.java - 动态状态栏管理

#### 改动位置
- **文件**: `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/MainActivity.java`
- **行号**: 81-249

#### 主要变更

##### A. 移除全局透明状态栏设置
**修改前**:
```java
// 设置全局透明沉浸式状态栏
setupTransparentStatusBar();
```

**修改后**:
```java
NavController navController = Navigation.findNavController(this, R.id.home_host_fragment);
NavigationUI.setupWithNavController(navView, navController);

// 添加导航监听器，动态调整状态栏
setupDynamicStatusBar(navController);
```

##### B. 实现动态状态栏切换方法
**新增方法**: `setupDynamicStatusBar(NavController navController)`

```java
/**
 * 设置动态状态栏切换
 * 根据不同页面的需求，动态调整状态栏颜色和图标颜色
 * 
 * 页面配置：
 * - Home: 绿色状态栏 (#41966F) + 白色图标
 * - Salat: 透明状态栏 (沉浸式) + 白色图标
 * - 其他: 白色状态栏 + 深色图标
 */
private void setupDynamicStatusBar(NavController navController) {
    navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
        int destinationId = destination.getId();
        String pageName = getResources().getResourceEntryName(destinationId);
        
        android.util.Log.d("MainActivity", "🔄 导航到页面: " + pageName + " (ID: " + destinationId + ")");
        
        if (destinationId == R.id.nav_home) {
            // Home页面：绿色状态栏，白色图标
            setStatusBarColor(0xFF41966F, false);
            android.util.Log.d("MainActivity", "✅ Home页面: 绿色状态栏 + 白色图标");
        } else if (destinationId == R.id.nav_namaz) {
            // Salat页面：透明状态栏，白色图标，沉浸式
            setStatusBarTransparent(false);
            android.util.Log.d("MainActivity", "✅ Salat页面: 透明状态栏 + 白色图标 (沉浸式)");
        } else {
            // 其他页面：白色状态栏，深色图标
            setStatusBarColor(0xFFFFFFFF, true);
            android.util.Log.d("MainActivity", "✅ 其他页面: 白色状态栏 + 深色图标");
        }
    });
}
```

##### C. 实现实色状态栏设置方法
**新增方法**: `setStatusBarColor(int color, boolean lightIcons)`

```java
/**
 * 设置实色状态栏
 * @param color 状态栏颜色（ARGB格式，如 0xFF41966F）
 * @param lightIcons true=深色图标（用于浅色背景），false=白色图标（用于深色背景）
 */
private void setStatusBarColor(int color, boolean lightIcons) {
    try {
        Window window = getWindow();
        View decorView = window.getDecorView();
        
        // 内容不延伸到状态栏下方（实色背景时）
        WindowCompat.setDecorFitsSystemWindows(window, true);
        
        // 设置状态栏颜色
        window.setStatusBarColor(color);
        
        // 设置图标颜色
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
        wic.setAppearanceLightStatusBars(lightIcons);
        
        android.util.Log.d("MainActivity", "📱 状态栏设置: 颜色=" + String.format("#%08X", color) + ", 图标=" + (lightIcons ? "深色" : "白色"));
    } catch (Exception e) {
        android.util.Log.e("MainActivity", "❌ 设置实色状态栏失败", e);
    }
}
```

##### D. 实现透明状态栏设置方法
**新增方法**: `setStatusBarTransparent(boolean lightIcons)`

```java
/**
 * 设置透明状态栏（沉浸式）
 * @param lightIcons true=深色图标，false=白色图标
 */
private void setStatusBarTransparent(boolean lightIcons) {
    try {
        Window window = getWindow();
        View decorView = window.getDecorView();
        
        // 内容延伸到状态栏下方（沉浸式）
        WindowCompat.setDecorFitsSystemWindows(window, false);
        
        // 设置状态栏为完全透明
        window.setStatusBarColor(Color.TRANSPARENT);
        
        // 设置图标颜色
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(window, decorView);
        wic.setAppearanceLightStatusBars(lightIcons);
        
        android.util.Log.d("MainActivity", "📱 状态栏设置: 透明沉浸式, 图标=" + (lightIcons ? "深色" : "白色"));
    } catch (Exception e) {
        android.util.Log.e("MainActivity", "❌ 设置透明状态栏失败", e);
    }
}
```

---

### 2. PrayersFragment.java - Salat页面Header适配

#### 改动位置
- **文件**: `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/home/PrayersFragment.java`
- **行号**: 248-281

#### 主要变更

##### A. 优化Header Padding设置
**修改前**:
```java
// 为header添加状态栏高度的顶部padding
View header = rootView.findViewById(R.id.main_header);
```

**修改后**:
```java
// 为header内容添加状态栏高度的顶部padding，避免内容被状态栏遮挡
// Salat页面使用透明沉浸式状态栏，内容会延伸到状态栏下方
// 此方法确保header内的UI元素（日期、位置等）有足够的顶部间距
View headerContent = rootView.findViewById(R.id.header_content);
```

##### B. 新增 setupHeaderPadding 方法
```java
/**
 * 为header内容添加状态栏高度的顶部padding，避免内容被状态栏遮挡
 * Salat页面使用透明沉浸式状态栏，内容会延伸到状态栏下方
 * 此方法确保header内的UI元素（日期、位置等）有足够的顶部间距
 */
private void setupHeaderPadding(View rootView) {
    View headerContent = rootView.findViewById(R.id.header_content);
    if (headerContent != null) {
        ViewCompat.setOnApplyWindowInsetsListener(headerContent, (v, insets) -> {
            int statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            
            // 为header内容添加顶部padding，确保不被状态栏遮挡
            v.setPadding(
                v.getPaddingLeft(),
                statusBarHeight + dpToPx(10), // 状态栏高度 + 额外10dp间距
                v.getPaddingRight(),
                v.getPaddingBottom()
            );
            
            Log.d("PrayersFragment", "✅ Salat Header顶部padding: 状态栏=" + statusBarHeight + "px + 10dp额外间距");
            return WindowInsetsCompat.CONSUMED;
        });
    } else {
        Log.w("PrayersFragment", "⚠️ 未找到header_content视图");
    }
}

/**
 * 将dp转换为px
 */
private int dpToPx(int dp) {
    float density = getResources().getDisplayMetrics().density;
    return Math.round(dp * density);
}
```

---

## 📊 页面状态栏配置表

| 页面 | Navigation ID | 状态栏颜色 | 图标颜色 | 内容延伸 | 说明 |
|------|--------------|-----------|---------|---------|------|
| **Home** | `R.id.nav_home` | 绿色 (#41966F) | 白色 | 否 | 与绿色Header一致，高对比度 |
| **Salat** | `R.id.nav_namaz` | 透明 | 白色 | 是 | 沉浸式，Header延伸到状态栏 |
| **99 Names** | `R.id.nav_name_99` | 白色 (#FFFFFF) | 深色 | 否 | 浅色背景，深色图标清晰可见 |
| **Tasbih** | `R.id.nav_tasbih` | 白色 (#FFFFFF) | 深色 | 否 | 浅色背景，深色图标清晰可见 |
| **Settings** | `R.id.navigation_settings` | 白色 (#FFFFFF) | 深色 | 否 | 浅色背景，深色图标清晰可见 |

---

## 🔍 技术要点

### 1. Android 14兼容性
- **`enforceStatusBarContrast=false`** 已在 `values-v35/themes.xml` 中设置
- 允许完全自定义状态栏颜色和图标颜色
- 实色背景（绿色/白色）天然高对比度，系统不会干预

### 2. WindowCompat.setDecorFitsSystemWindows()
- **`true`**: 内容从状态栏下方开始，状态栏区域为系统保留（用于实色背景）
- **`false`**: 内容延伸到状态栏下方，实现沉浸式效果（用于透明背景）

### 3. WindowInsetsControllerCompat.setAppearanceLightStatusBars()
- **`true`**: 深色图标（用于浅色背景，如白色状态栏）
- **`false`**: 白色图标（用于深色背景，如绿色状态栏或透明状态栏+深色Header）

### 4. Window Insets Listener
- 动态监听系统窗口插入（status bar高度）
- 为沉浸式页面的Header内容添加顶部Padding
- 确保UI元素不被状态栏遮挡

---

## ✅ 测试验证

### 测试步骤
1. 打开应用，观察Home页面状态栏
2. 点击底部导航切换到Salat页面
3. 依次切换到99 Names、Tasbih、Settings页面
4. 观察每个页面的状态栏颜色和图标颜色是否符合预期

### 日志监控
监控以下日志标签：
- `MainActivity.*🔄` - 页面导航事件
- `MainActivity.*✅` - 状态栏设置成功
- `MainActivity.*📱` - 状态栏配置详情
- `PrayersFragment.*✅` - Salat Header padding设置
- `PrayersFragment.*⚠️` - 警告信息

### 预期结果
- ✅ Home页面：绿色状态栏，白色图标，内容不延伸
- ✅ Salat页面：透明状态栏，白色图标，Header延伸到状态栏，日期/位置有足够顶部间距
- ✅ 99 Names页面：白色状态栏，深色图标，内容不延伸
- ✅ Tasbih页面：白色状态栏，深色图标，内容不延伸
- ✅ Settings页面：白色状态栏，深色图标，内容不延伸

---

## 📝 相关文件

### 修改的文件
1. `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/MainActivity.java`
2. `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/home/PrayersFragment.java`

### 配置文件（未修改，保持现状）
1. `app/src/main/res/values-v35/themes.xml` - Android 14主题配置
2. `app/src/main/res/layout/layout_home_header.xml` - Home页面Header（已有 `fitsSystemWindows="true"`）
3. `app/src/main/res/layout/fragment_prayer_next_prayer_layout.xml` - Salat页面Header

### 文档文件
1. `STATUS_BAR_FINAL_ANALYSIS.md` - 问题分析报告
2. `STATUS_BAR_DYNAMIC_SWITCHING_IMPLEMENTATION.md` - 实施总结（本文档）

---

## 🚀 下一步行动

1. **用户测试**：等待用户在实际设备上测试并提供反馈
2. **日志分析**：根据日志输出确认状态栏切换是否正常
3. **视觉验证**：确认所有页面的状态栏颜色和图标颜色符合预期
4. **边缘情况测试**：
   - 快速切换多个页面
   - 旋转屏幕
   - 从后台恢复应用
5. **性能优化**：如有必要，优化状态栏切换的流畅度

---

## 📚 技术参考

### Android官方文档
- [Edge-to-Edge Display](https://developer.android.com/develop/ui/views/layout/edge-to-edge)
- [WindowInsetsCompat](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat)
- [WindowCompat](https://developer.android.com/reference/androidx/core/view/WindowCompat)

### Navigation Component
- [Navigation Listeners](https://developer.android.com/guide/navigation/navigation-programmatic#listen)

---

**实施完成时间**: 2025-10-23  
**版本**: v1.0  
**状态**: 待用户测试验证







