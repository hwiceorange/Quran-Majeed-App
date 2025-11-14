# 🎨 7天免费试用页面插图更新

## 📋 更新内容

### 插图替换

**位置**: 新用户引导页 - 7天免费试用页面

**修改项目**:
- ✅ 图片来源：`@drawable/illustration_trial` → `@drawable/ic_junaid`
- ✅ 图片宽度：与页面宽度保持一致（移除左右边距）
- ✅ 图片高度：自适应（`wrap_content` + `adjustViewBounds`）
- ✅ 图片居中：保持居中对齐
- ✅ 顶部边距：移除顶部边距（`marginTop="0dp"`）

---

## 🔧 技术实现

**修改文件**: `fragment_onboard_trial.xml`

### 修改前

```xml
<ImageView
    android:id="@+id/img_illustration"
    android:layout_width="0dp"
    android:layout_height="300dp"
    android:layout_marginStart="32dp"
    android:layout_marginTop="60dp"
    android:layout_marginEnd="32dp"
    android:src="@drawable/illustration_trial"
    android:scaleType="fitCenter"
    ... />
```

**特点**:
- 固定高度 300dp
- 左右各有 32dp 边距
- 顶部有 60dp 边距

---

### 修改后

```xml
<ImageView
    android:id="@+id/img_illustration"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_marginStart="0dp"
    android:layout_marginTop="0dp"
    android:layout_marginEnd="0dp"
    android:src="@drawable/ic_junaid"
    android:scaleType="fitCenter"
    android:adjustViewBounds="true"
    ... />
```

**特点**:
- ✅ 高度自适应（`wrap_content`）
- ✅ 左右无边距（宽度与页面一致）
- ✅ 顶部无边距（从页面顶部开始）
- ✅ 保持图片宽高比（`adjustViewBounds="true"`）

---

## 📱 页面布局结构

```
┌─────────────────────────────────────┐
│ ┌─────────────────────────────────┐ │
│ │                                 │ │
│ │      ic_junaid.png (插图)      │ │
│ │       (全宽度，居中显示)        │ │
│ │                                 │ │
│ └─────────────────────────────────┘ │
│                                     │
│          You got                    │
│         7 days free                 │
│      to be inspired by              │
│        God's Word!                  │
│                                     │
│                                     │
│  ┌─────────────────────────────┐   │
│  │    Try for Free ➡️          │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

---

## 🎨 视觉效果

### 布局特点

1. **全宽度显示**: 图片从屏幕左边缘延伸到右边缘
2. **居中对齐**: 使用 ConstraintLayout 约束实现水平居中
3. **自适应高度**: 根据图片原始宽高比自动调整高度
4. **无边距**: 顶部、左侧、右侧都没有边距，实现全屏效果

### 图片属性

- **scaleType**: `fitCenter` - 保持图片比例，居中显示
- **adjustViewBounds**: `true` - 根据图片比例调整 ImageView 的边界
- **layout_width**: `0dp` - 在 ConstraintLayout 中表示 match_constraint（匹配约束）
- **layout_height**: `wrap_content` - 根据内容自适应高度

---

## 📊 对比

| 属性 | 修改前 | 修改后 |
|------|--------|--------|
| 图片文件 | illustration_trial | ic_junaid ✨ |
| 宽度 | 页面宽度 - 64dp | 页面全宽 ✨ |
| 高度 | 固定 300dp | 自适应 ✨ |
| 顶部边距 | 60dp | 0dp ✨ |
| 左右边距 | 各 32dp | 0dp ✨ |

---

## ✅ 验证

**图片文件状态**: 
- ✅ `/Users/huwei/AndroidStudioProjects/quran0/app/src/main/res/drawable/ic_junaid.png` 
- ✅ 文件大小: 262 KB
- ✅ 文件已存在

---

## 🧪 测试步骤

1. 编译应用
2. 清除应用数据：`adb shell pm clear com.quran.quranaudio.online`
3. 启动应用，进入新用户引导流程
4. 浏览到"7天免费试用"页面

**期望效果**:
- ✅ 顶部显示 Junaid 插图
- ✅ 图片宽度与屏幕宽度一致（无左右边距）
- ✅ 图片从页面顶部开始显示
- ✅ 图片高度根据原始比例自动调整
- ✅ 图片下方显示文字："You got 7 days free..."
- ✅ 底部显示"Try for Free"按钮

---

**更新日期**: 2025-11-13  
**更新类型**: UI 插图替换  
**影响页面**: 新用户引导 - 7天免费试用页面

