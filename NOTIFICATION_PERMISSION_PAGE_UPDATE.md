# 🔔 通知权限页面布局调整

## 📋 调整内容

### 1. 中间引导授权模块

**调整项目**:
- ✅ 卡片背景颜色：`#2C2C2E` → `#8F9395`
- ✅ Allow 按钮背景：`#1C1C1E` → `#B4C8E8`
- ✅ Don't Allow 按钮背景：`#1C1C1E` → `#B4C8E8`
- ✅ 按钮文字颜色：统一改为 `#304465`
- ✅ 按钮文字改为大写：`ALLOW` / `DON'T ALLOW`

**修改位置**: `fragment_onboard_notification.xml`

```xml
<!-- 卡片背景 -->
<com.google.android.material.card.MaterialCardView
    app:cardBackgroundColor="#8F9395"
    ...>
    
    <!-- Allow 按钮 -->
    <MaterialButton
        android:text="ALLOW"
        android:textColor="#304465"
        app:backgroundTint="#B4C8E8"
        ... />
    
    <!-- Don't Allow 按钮 -->
    <MaterialButton
        android:text="DON'T ALLOW"
        android:textColor="#304465"
        app:backgroundTint="#B4C8E8"
        ... />
```

---

### 2. Allow 按钮手指图标

**调整项目**:
- ✅ 移除原有的 `ic_hand_pointer` 图标
- ✅ 替换为 `ic_hand.png`
- ✅ 移除白色着色（显示原始图片颜色）

**修改位置**: `fragment_onboard_notification.xml`

```xml
<ImageView
    android:id="@+id/icon_hand_pointer"
    android:src="@drawable/ic_hand"
    ... />
```

**图片文件**: `/Users/huwei/AndroidStudioProjects/quran0/app/src/main/res/drawable/ic_hand.png` ✅ 已存在

---

### 3. 好评用户头像

**调整项目**:
- ✅ 移除原有的 `placeholder_user_avatar` 头像
- ✅ 替换为 `ic_head.png`
- ✅ 保持圆形显示区域不变

**修改位置**: `fragment_onboard_notification.xml`

```xml
<ImageView
    android:id="@+id/img_user_avatar"
    android:layout_width="48dp"
    android:layout_height="48dp"
    android:src="@drawable/ic_head"
    android:scaleType="centerCrop"
    android:background="@drawable/circle_background"
    ... />
```

**⚠️ 注意**: 需要确保 `/Users/huwei/AndroidStudioProjects/quran0/app/src/main/res/drawable/ic_head.png` 文件存在，否则会编译错误。

---

## 🎨 视觉效果对比

### 调整前
- 卡片背景：深灰色 `#2C2C2E`
- 按钮背景：更深灰 `#1C1C1E`
- Allow 按钮文字：青绿色 `#48C9A9`
- Don't Allow 按钮文字：蓝色 `#5B9AE8`
- 手指图标：白色着色的下载图标

### 调整后
- 卡片背景：中灰色 `#8F9395` ✨
- 按钮背景：浅蓝色 `#B4C8E8` ✨
- 按钮文字：深蓝色 `#304465` ✨
- 手指图标：原始彩色手指图标 ✨
- 用户头像：自定义头像图片 ✨

---

## 📱 页面结构

```
┌─────────────────────────────────────┐
│                                     │
│    Stay Consistent with Your Salah  │
│                                     │
│    Never miss a prayer...           │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  卡片 (#8F9395)              │   │
│  │                              │   │
│  │  QuranApp Would Like to Send │   │
│  │  You Prayer & Learning       │   │
│  │  Notifications               │   │
│  │                              │   │
│  │  Notifications include...    │   │
│  │                              │   │
│  │  ┌────────────────────┐      │   │
│  │  │ ALLOW (#B4C8E8)    │🖐️   │
│  │  └────────────────────┘      │   │
│  │                              │   │
│  │  ┌────────────────────┐      │   │
│  │  │ DON'T ALLOW        │      │   │
│  │  └────────────────────┘      │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ "Alhamdulillah! ..."    👤  │   │
│  │                     Aisha K. │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## ✅ 完成状态

- ✅ 卡片背景颜色已调整
- ✅ 按钮背景颜色已调整
- ✅ 按钮文字颜色已调整
- ✅ 手指图标已替换（ic_hand.png 存在）
- ⚠️ 用户头像已替换（需确保 ic_head.png 存在）

---

## 🧪 测试步骤

1. 确保 `ic_head.png` 文件存在于 `app/src/main/res/drawable/` 目录
2. 编译应用
3. 清除应用数据：`adb shell pm clear com.quran.quranaudio.online`
4. 启动应用，进入引导流程
5. 检查通知权限页面的视觉效果

**期望效果**:
- 中间卡片为灰蓝色调
- 按钮为浅蓝色背景，深蓝色文字
- 右侧显示彩色手指图标
- 底部显示自定义用户头像

---

**更新日期**: 2025-11-13  
**更新类型**: UI 调整  
**影响页面**: 新用户引导 - 通知权限页面

