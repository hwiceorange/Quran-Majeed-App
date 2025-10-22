# Wudu Guide 页面实现文档

**创建日期**: 2025-10-20  
**状态**: ✅ 已完成并编译成功  

---

## 📋 功能概述

实现了一个完整的 **Wudu Guide（小净指南）** 页面，用于为用户提供详细的伊斯兰教洗礼（Wudu）步骤指导。

### 主要特性
- ✅ 12个详细步骤，包含英文和阿拉伯文说明
- ✅ 每个步骤配有配图（支持图片展示）
- ✅ 支持 Dua（祈祷词）展示（阿拉伯文、音译、英文）
- ✅ 底部免责声明（Disclaimer）
- ✅ 与应用主题风格完全统一
- ✅ 支持竖屏显示
- ✅ RecyclerView 列表实现，性能优秀

---

## 📁 创建的文件

### 1. Java 类文件

#### `WuduStep.java`
**路径**: `/app/src/main/java/com/quran/quranaudio/online/wudu/WuduStep.java`

**用途**: Wudu 步骤的数据模型类

**主要字段**:
- `stepNumber`: 步骤编号
- `title / titleArabic`: 英文/阿拉伯文标题
- `description / descriptionArabic`: 英文/阿拉伯文描述
- `imageName`: 图片文件名
- `dua / duaArabic / duaTransliteration`: 祈祷词（可选）

#### `WuduGuideActivity.java`
**路径**: `/app/src/main/java/com/quran/quranaudio/online/wudu/WuduGuideActivity.java`

**用途**: Wudu Guide 主活动页面

**主要功能**:
- 加载 12 个 Wudu 步骤数据
- 设置 RecyclerView 和 Adapter
- 处理返回按钮点击

#### `WuduStepsAdapter.java`
**路径**: `/app/src/main/java/com/quran/quranaudio/online/wudu/WuduStepsAdapter.java`

**用途**: RecyclerView 适配器

**支持两种 ViewType**:
- `VIEW_TYPE_STEP`: 普通步骤卡片
- `VIEW_TYPE_DISCLAIMER`: 底部免责声明卡片

---

### 2. 布局文件

#### `activity_wudu_guide.xml`
**路径**: `/app/src/main/res/layout/activity_wudu_guide.xml`

**用途**: Activity 主布局

**组件**:
- 绿色工具栏（带返回按钮和标题）
- RecyclerView 列表容器

#### `item_wudu_step.xml`
**路径**: `/app/src/main/res/layout/item_wudu_step.xml`

**用途**: 单个 Wudu 步骤的卡片布局

**组件**:
- 步骤标题（英文 + 阿拉伯文）
- MaterialCardView 卡片
  - 步骤配图
  - 阿拉伯文描述
  - 英文描述
  - Dua 容器（可选显示）

#### `item_wudu_disclaimer.xml`
**路径**: `/app/src/main/res/layout/item_wudu_disclaimer.xml`

**用途**: 底部免责声明布局

**内容**:
> **Disclaimer**: This guide has been compiled based on authoritative resources such as IslamQA and WikiHow, and adheres to the prevailing Madhab school of jurisprudence. If you have any questions, please consult your local imam.

---

### 3. AndroidManifest.xml 更新

在 `AndroidManifest.xml` 中注册了 `WuduGuideActivity`:

```xml
<activity
    android:name="com.quran.quranaudio.online.wudu.WuduGuideActivity"
    android:screenOrientation="portrait"
    android:theme="@style/AppTheme"
    android:exported="false" />
```

---

## 📸 图片资源要求

### 存储位置
```
/app/src/main/res/drawable-xxhdpi/wudu/
```

### 图片文件名
- `wudu_step_01.jpg` - Intention (Niyyah)
- `wudu_step_02.jpg` - Wash Hands
- `wudu_step_03.jpg` - Rinse Mouth
- `wudu_step_04.jpg` - Sniff Water into Nose
- `wudu_step_05.jpg` - Wash Face
- `wudu_step_06.jpg` - Wash Arms
- `wudu_step_07.jpg` - Wipe Head (Maseh)
- `wudu_step_08.jpg` - Wipe Ears
- `wudu_step_09.jpg` - Wash Feet
- `wudu_step_10.jpg` - Follow the Order (Tarbiṭ)
- `wudu_step_11.jpg` - Recite Dua After Wudu
- `wudu_step_12.jpg` - When Wudu is Nullified

### 图片规格
- **宽度**: 800-1000px
- **格式**: JPG
- **分辨率**: @xxhdpi (2x 或 3x)

---

## 📝 12 个 Wudu 步骤详细内容

### 步骤 1: Intention (Niyyah) - النية
- **描述**: Make the intention in your heart to perform wudu for the sake of Allah. Focus on the phrase 'Bismillah' to help center yourself.
- **Dua**: بسم الله (Bismillah)

### 步骤 2: Wash Hands - غسل اليدين
- **描述**: Wash your right hand with your left hand three times, then your left hand with your right hand three times, up to the wrists and between the fingers.

### 步骤 3: Rinse Mouth - المضمضة
- **描述**: Take water into your mouth three times using your right hand and swish it around thoroughly.

### 步骤 4: Sniff Water into Nose - الاستنشاق
- **描述**: Inhale water into your nose three times using your right hand, then blow it out gently. Be careful not to choke.

### 步骤 5: Wash Face - غسل الوجه
- **描述**: Wash your face three times from the hairline to the chin and from one ear to the other.

### 步骤 6: Wash Arms - غسل الذراعين
- **描述**: Wash your arms from the fingertips to the elbows three times, starting with the right arm, then the left.

### 步骤 7: Wipe Head (Maseh) - مسح الرأس
- **描述**: Wipe your head once with wet hands from the front to the back and then back to the front.

### 步骤 8: Wipe Ears - مسح الأذنين
- **描述**: Wipe the inside and outside of your ears once with your thumbs.

### 步骤 9: Wash Feet - غسل القدمين
- **描述**: Wash both feet up to the ankles, including between the toes, starting with the right foot.

### 步骤 10: Follow the Order (Tarbiṭ) - الترتيب
- **描述**: Perform all steps in the correct sequence. If the order is broken, restart wudu.

### 步骤 11: Recite Dua After Wudu - دعاء بعد الوضوء
- **描述**: After completing wudu, recite: 'Ash-hadu an laa ilaaha illallaah…' to seal your purification.
- **Dua**: أشهد أن لا إله إلا الله وحده لا شريك له... (Ash-hadu an laa ilaaha illallaah wahdahu laa shareeka lahu...)

### 步骤 12: When Wudu is Nullified - بطلان الوضوء
- **描述**: Wudu is nullified by urination, defecation, passing gas, deep sleep, or sexual intercourse. Ghusl is required after intercourse.

---

## 🎨 UI/UX 设计特点

### 颜色方案
- **主色调**: `@color/colorPrimary` (#4e8545 绿色)
- **卡片背景**: `@color/hadith_card_lite` (#F8EACE 浅黄色)
- **内容卡片**: `@color/colorBGQuranPageHeader` 
- **文本颜色**: 
  - 标题：绿色 (`@color/colorPrimary`)
  - 阿拉伯文：深色 (`@color/onPrimary1`)
  - 英文描述：灰色 (`@color/dark_grey`)

### 布局风格
- **卡片式设计**: MaterialCardView with rounded corners (10dp radius)
- **层次分明**: 使用分隔线区分不同内容区域
- **阿拉伯文优先**: 阿拉伯文字体使用 `@font/arab`，显示在描述顶部
- **响应式图片**: 图片自适应宽度，最大高度 400dp

### 字体大小
- **步骤标题**: 18sp (英文) / 20sp (阿拉伯文)
- **步骤描述**: 15sp (英文) / 18sp (阿拉伯文)
- **Dua 文本**: 20sp (阿拉伯文) / 14sp (英文/音译)
- **免责声明**: 14sp (标题) / 12sp (内容)

---

## 🚀 如何使用

### 在代码中启动 Wudu Guide 页面

```java
// 从任何 Activity 或 Fragment 启动
Intent intent = new Intent(context, WuduGuideActivity.class);
startActivity(intent);
```

### 推荐的集成位置

根据您的需求，建议在以下位置添加入口：

1. **Salat Times 页面** (Prayer Fragment)
   - 在祈祷时间页面添加 "Wudu Guide" 按钮
   - 帮助用户在礼拜前准备

2. **主页** (Home Fragment)
   - 作为快捷功能卡片

3. **设置页面** (Settings Fragment)
   - 在"Learn"或"Resources"分类下添加

### 示例：在 Salat 页面添加按钮

```java
// 在 PrayersFragment 或类似页面中
Button wuduGuideBtn = findViewById(R.id.btn_wudu_guide);
wuduGuideBtn.setOnClickListener(v -> {
    Intent intent = new Intent(getActivity(), WuduGuideActivity.class);
    startActivity(intent);
});
```

---

## ✅ 编译状态

**编译结果**: ✅ **BUILD SUCCESSFUL**

```
BUILD SUCCESSFUL in 3m 19s
168 actionable tasks: 18 executed, 150 up-to-date
```

**警告**: 仅有已存在的过时API警告，无新增错误

---

## 📌 后续优化建议

### 1. 添加音频播放功能
- 为每个 Dua 添加音频朗读功能
- 使用 MediaPlayer 播放阿拉伯文 Dua 的发音

### 2. 添加视频支持
- 为关键步骤添加视频演示
- 使用 VideoView 或 ExoPlayer 播放

### 3. 添加分享功能
- 允许用户分享单个步骤
- 生成漂亮的图片分享到社交媒体

### 4. 添加多语言支持
- 添加乌尔都语（Urdu）翻译
- 添加其他常见语言（如印尼语、马来语等）

### 5. 添加离线图片
- 确保图片已添加到 `drawable-xxhdpi` 文件夹
- 如果图片未找到，显示占位图

### 6. 添加收藏/书签功能
- 允许用户标记难记的步骤
- 快速访问常用步骤

---

## 🔧 技术栈

- **语言**: Java
- **UI Framework**: Android XML Layouts
- **设计组件**: Material Design Components
- **RecyclerView**: 列表展示
- **ConstraintLayout**: 响应式布局

---

## 📞 联系与支持

如有问题或需要进一步优化，请随时联系开发团队。

---

**实现完成时间**: 2025-10-20  
**编译版本**: debug  
**目标 Android 版本**: API 21+

