# Qada' Tracker UI 重新设计完成

## 日期
2025-11-06

## 概述
完全重新设计了 `QadaTrackerActivity` 的UI和布局，严格按照用户提供的截图进行实现。

---

## 🎨 设计规范（基于截图）

### 颜色方案
- **背景色**: `#F8F9FA` (浅灰)
- **卡片背景**: `#FFFFFF` (白色)
- **主文字**: `#1A1A1A` (深黑)
- **次要文字**: `#999999` (浅灰)
- **Ada' (准时)**: `#5A9B7F` (绿色)
- **Qada' (弥补)**: `#F5AC1C` (橙色)
- **Missed (错过)**: `#E57373` (红色)
- **成就卡片背景**: `#E8F5E9` (浅绿)
- **Tab背景**: `#E8E8E8` (灰色)

### 字体规范
- **标题**: 20sp, Bold
- **日期范围**: 16sp, Bold
- **Tab**: 15sp, Bold
- **祷告名称**: 14sp
- **表头**: 11sp, Bold
- **图例**: 13sp (Weekly), 12sp (Monthly)

### 圆角和间距
- **Tab容器**: 25dp圆角
- **Tab按钮**: 22dp圆角
- **卡片**: 16dp圆角
- **成就卡片**: 12dp圆角
- **卡片间距**: 16dp
- **状态圆点**: 12dp直径

---

## 新增/修改文件

### 布局文件

#### 1. `activity_qada_tracker.xml` (主布局)
**结构**:
```
LinearLayout (垂直)
├── Top App Bar
│   ├── 返回按钮
│   └── 标题 "Your Activity"
└── ScrollView
    └── LinearLayout
        ├── Tab Layout (胶囊样式)
        │   ├── Weekly按钮
        │   └── Monthly按钮
        ├── 日期导航
        │   ├── 左箭头
        │   ├── 日期文本
        │   └── 右箭头
        └── FrameLayout (内容容器)
            ├── Weekly View (include)
            └── Monthly View (include)
```

#### 2. `view_qada_weekly.xml` (周视图)
**组件**:
- 圆形进度卡片 (85% 完成度)
  - 大号百分比 (48sp)
  - "Completed" 副标题
  - "This Week ↑ +5%" 增长提示
- Prayer Breakdown 卡片
  - 表格布局 (Mon-Sun横向, Fajr-Isha纵向)
  - 颜色图例 (Ada', Qada', Missed)
- 成就卡片 (Great Consistency!)
  - 星形图标
  - 鼓励文字

#### 3. `view_qada_monthly.xml` (月视图)
**组件**:
- 圆形进度卡片 (更大号: 52sp)
- Prayer Breakdown 表格
  - 列头: DATE | FAJR | DHUHR | ASR | MAGHRIB | ISHA
  - 每行显示一天的5个祷告状态
  - 颜色图例 (Ada' (On Time), Qada' (Made Up), Missed)

### Drawable资源

#### 1. `bg_tab_container.xml`
胶囊形Tab容器背景 (#E8E8E8, 25dp圆角)

#### 2. `selector_tab_button.xml`
Tab按钮选择器
- 选中: 白色背景 (#FFFFFF, 22dp圆角)
- 未选中: 透明背景

#### 3. `selector_tab_text.xml`
Tab文字颜色选择器
- 选中: #1A1A1A
- 未选中: #999999

#### 4. `circular_progress_drawable.xml`
圆形进度条
- 背景环: #E0E0E0 (灰色)
- 进度环: #5A9B7F (绿色)

#### 5. 状态圆点
- `bg_circle_green.xml` - Ada' 状态 (#5A9B7F)
- `bg_circle_orange.xml` - Qada' 状态 (#F5AC1C)
- `bg_circle_red.xml` - Missed 状态 (#E57373)

#### 6. 图标
- `ic_star.xml` - 成就星形图标
- `ic_chevron_left.xml` - 左箭头
- `ic_chevron_right.xml` - 右箭头

---

## 功能实现

### Java代码 - `QadaTrackerActivity.java`

#### 核心功能

1. **Tab切换**
   - Weekly/Monthly视图切换
   - 动态显示/隐藏视图
   - 更新日期显示

2. **日期导航**
   - `navigatePrevious()` - 上一周/月
   - `navigateNext()` - 下一周/月
   - 自动更新日期范围显示

3. **Weekly视图**
   ```java
   buildWeeklyPrayerGrid()
   ├── createWeeklyHeaderRow() - 星期标题 (Mon-Sun)
   └── createWeeklyPrayerRow() - 祷告行 (Fajr-Isha)
       └── 为每天生成状态圆点
   ```

4. **Monthly视图**
   ```java
   buildMonthlyPrayerTable()
   ├── createMonthlyHeaderRow() - 表头 (DATE, FAJR, etc.)
   └── createMonthlyDataRow() - 数据行
       └── 为每个祷告生成状态圆点
   ```

#### 日期格式化

**Weekly格式**: 
- `Oct 23 - Oct 29` (本周一到周日)
- 使用 `LocalDate` + `TemporalAdjusters`

**Monthly格式**: 
- `July 2024` (月份 年份)
- 使用 `DateTimeFormatter`

#### 模拟数据

当前使用模拟数据展示UI：
- `generateMockWeeklyData()` - 生成7天的状态数据
- `generateMockDailyData()` - 生成5个祷告的状态数据

**状态码**:
- `0` = Ada' (绿色)
- `1` = Qada' (橙色)
- `2` = Missed (红色)

---

## UI效果对比

### Weekly View
✅ 胶囊形Tab (选中白色背景)
✅ 左右箭头导航
✅ 日期范围显示 (`Oct 23 - Oct 29`)
✅ 圆形进度 (85%)
✅ `This Week ↑ +5%` 增长提示
✅ Prayer Breakdown 表格
  - 横向: Mon-Sun
  - 纵向: Fajr, Dhuhr, Asr, Maghrib, Isha
  - 颜色圆点: 绿/橙/红
✅ 图例说明
✅ 成就卡片 (浅绿背景 + 星形图标)

### Monthly View
✅ 日期显示 (`July 2024`)
✅ 更大的圆形进度 (180dp)
✅ 表格布局
  - 列头: DATE | FAJR | DHUHR | ASR | MAGHRIB | ISHA
  - 数据行: 01, 02, 03... + 状态圆点
✅ 图例说明 (On Time, Made Up, Missed)

---

## 测试步骤

### 1. 测试Tab切换
1. ✅ 打开 QadaTrackerActivity
2. ✅ 默认显示 Weekly视图
3. ✅ 点击 "Monthly"
   - Weekly视图隐藏
   - Monthly视图显示
   - 日期格式从 `Oct 23 - Oct 29` 变为 `July 2024`
4. ✅ 点击 "Weekly"
   - 切换回Weekly视图

### 2. 测试日期导航
1. ✅ Weekly视图下点击右箭头
   - 日期范围向后移动一周
   - Prayer Breakdown更新
2. ✅ 点击左箭头
   - 日期范围向前移动一周
3. ✅ 切换到Monthly视图
4. ✅ 点击右/左箭头
   - 月份切换

### 3. 测试UI样式
1. ✅ 检查Tab样式
   - 圆角胶囊形
   - 选中白色背景
   - 未选中灰色文字
2. ✅ 检查圆形进度
   - Weekly: 160dp
   - Monthly: 180dp
   - 进度环颜色: 绿色 (#5A9B7F)
3. ✅ 检查Prayer Breakdown
   - Weekly: 表格布局 (横向星期 + 纵向祷告)
   - Monthly: 表格布局 (DATE列 + 5个祷告列)
4. ✅ 检查颜色圆点
   - 绿色: Ada'
   - 橙色: Qada'
   - 红色: Missed

### 4. 测试返回功能
1. ✅ 点击返回按钮
2. ✅ 返回到Salat页面

---

## 下一步开发计划

### 优先级1: 数据集成
1. **连接真实数据**
   - 替换 `generateMockWeeklyData()` 为实际Firestore查询
   - 从 `PrayerLogRepository` 获取指定日期范围的祷告记录
   - 根据真实数据显示状态圆点

2. **日期范围查询**
   ```java
   // Weekly: 获取本周 (Monday-Sunday) 的所有祷告记录
   getWeeklyPrayerLogs(weekStart, weekEnd)
   
   // Monthly: 获取本月 (1号-最后一天) 的所有祷告记录
   getMonthlyPrayerLogs(monthStart, monthEnd)
   ```

3. **动态计算完成率**
   - 统计 Ada' + Qada' 的总数
   - 计算百分比
   - 更新圆形进度条

### 优先级2: 增长提示
1. **"This Week ↑ +5%"**
   - 对比上周和本周的完成率
   - 计算增长/下降百分比
   - 动态显示增长指标
   - 上升显示绿色 + ↑
   - 下降显示红色 + ↓

### 优先级3: 成就系统
1. **"Great Consistency!" 卡片**
   - 分析本周/月数据
   - 识别最稳定的祷告 (最少Missed)
   - 动态生成鼓励文字
   - 条件触发 (例如: 全Ada'时显示特殊成就)

### 优先级4: 交互优化
1. **点击查看详情**
   - 点击状态圆点 → 打开Log Prayer Modal
   - 允许编辑/查看该祷告的详细信息

2. **长按操作**
   - 长按状态圆点 → 显示更多选项
   - 快速标记为Ada'/Qada'/Missed

3. **加载状态**
   - 数据加载时显示ProgressBar
   - 网络错误时显示重试按钮

### 优先级5: 性能优化
1. **缓存机制**
   - 缓存已查询的日期范围数据
   - 减少Firestore查询次数

2. **懒加载**
   - 仅加载当前显示的月份/周
   - 滑动时动态加载相邻数据

---

## 技术细节

### 布局层级优化
- 使用 `<include>` 标签分离Weekly和Monthly视图
- 避免过深的View层级
- 使用 `FrameLayout` 实现视图切换

### 动态View生成
```java
// Weekly: 5行 × 8列 (祷告名 + 7天) = 40个View
// Monthly: 6行 × 6列 (日期 + 5祷告) × N天 = 36N个View
```

**性能考虑**:
- 使用 `ViewHolder` 模式（未来优化）
- RecyclerView替代ScrollView（未来优化）

### 日期计算
```java
// 获取本周一
LocalDate weekStart = currentDate.with(
    TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
);

// 获取本周日
LocalDate weekEnd = currentDate.with(
    TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)
);
```

### 兼容性处理
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    // 使用 LocalDate (API 26+)
} else {
    // 回退到 Calendar (API < 26)
}
```

---

## 版本信息
- **应用版本**: 1.7.3
- **编译日期**: 2025-11-06
- **编译状态**: ✅ 成功
- **安装状态**: ✅ 已安装到物理设备

---

## 问题排查

### 如果Weekly表格显示不全
- 使用 `HorizontalScrollView` 包裹表格
- 检查每列宽度设置 (dpToPx)

### 如果颜色不正确
- 确认drawable资源中的颜色值
- 检查 `bg_circle_*.xml` 文件

### 如果Tab不能切换
- 确认 `setSelected(true/false)` 调用
- 检查视图的 `setVisibility()` 状态

### 如果日期不更新
- 确认 `currentDate` 变量更新
- 检查 `updateDateDisplay()` 调用时机

---

## 相关文档
- `QADA_TRACKER_ACTIVITY_INTEGRATION.md` - 初始集成文档
- `QADA_UI_PERMISSION_FIX.md` - Qada' UI优化和权限修复
- `PRAYER_STATUS_UI_IMPLEMENTATION.md` - 祷告状态UI实现

---

**状态**: ✅ UI重新设计完成，使用模拟数据
**下一步**: 集成真实Firestore数据





