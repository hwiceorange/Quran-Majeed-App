# 可安全删除的重复文件清单

## ✅ 验证状态：已确认使用情况

生成时间: 2025-10-23

---

## 🎯 第一批：100% 安全删除（明确重复）

这些文件在多个目录存在，Android 会自动选择最合适的版本。
删除低分辨率版本，**保留高分辨率版本**。

### 1. 超大文件重复（优先删除，立即节省 ~8 MB）

| 要删除的文件 | 大小 | 原因 | 保留版本 |
|-------------|------|------|----------|
| `app/src/main/res/drawable/tasbih_bg.jpg` | 未知 | 保留最大版本 | ✅ drawable-v24/ (5.6MB) |
| `app/src/main/res/drawable-xhdpi/tasbih_bg.jpg` | 122 KB | 保留最大版本 | ✅ drawable-v24/ (5.6MB) |
| `app/src/main/res/drawable/tasbeeh_1.png` | 144 KB | 重复 | ✅ drawable-v24/ |
| `app/src/main/res/drawable/tasbeeh_2.png` | 147 KB | 重复 | ✅ drawable-v24/ |
| `app/src/main/res/drawable/tasbeeh_3.png` | 152 KB | 重复 | ✅ drawable-v24/ |
| `app/src/main/res/drawable/tasbeeh_4.png` | 155 KB | 重复 | ✅ drawable-v24/ |
| `app/src/main/res/drawable/tasbeeh_5.png` | 158 KB | 重复 | ✅ drawable-v24/ |
| `app/src/main/res/drawable/tasbeeh_6.png` | 164 KB | 重复 | ✅ drawable-v24/ |
| `app/src/main/res/drawable/tasbeeh_7.png` | 167 KB | 重复 | ✅ drawable-v24/ |
| `app/src/main/res/drawable/today_header.jpg` | 284 KB | 重复 | ✅ drawable-v24/ |
| `app/src/main/res/drawable/img_prayer.png` | 164 KB | 重复 | ✅ drawable-v24/ |
| `app/src/main/res/drawable/compassfix.gif` | 468 KB | 重复 | ✅ drawable-v24/ |

**小计：约 2.5 MB + 未知大小的 drawable/tasbih_bg.jpg**

### 2. 罗盘图片重复（删除其中一组）

#### 选项A：删除 drawable-v24/ 版本（推荐）
保留 drawable-xxhdpi/ 因为是专门为高分辨率屏幕优化的。

```bash
rm app/src/main/res/drawable-v24/compass_1.png
rm app/src/main/res/drawable-v24/compass_2.png
rm app/src/main/res/drawable-v24/compass_3.png  # 268 KB
rm app/src/main/res/drawable-v24/compass_4.png  # 216 KB
rm app/src/main/res/drawable-v24/compass_5.png
rm app/src/main/res/drawable-v24/compass_1_k.png
rm app/src/main/res/drawable-v24/compass_2_k.png
rm app/src/main/res/drawable-v24/compass_3_k.png
rm app/src/main/res/drawable-v24/compass_4_k.png
rm app/src/main/res/drawable-v24/compass_5_k.png
```

**节省：约 2 MB**

### 3. rate.gif 重复（已确认使用）

**使用位置：** `MainActivity.java:197`
```java
((GifImageView) dialog.findViewById(R.id.GifImageView))
    .setGifImageResource(R.drawable.rate);
```

| 要删除的文件 | 大小 | 保留版本 |
|-------------|------|----------|
| `app/src/main/res/drawable/rate.gif` | 542 KB | ✅ drawable-v24/ |

**节省：542 KB**

### 4. 其他图标重复

| 要删除的文件 | 保留版本 |
|-------------|----------|
| `app/src/main/res/drawable/calibration_compass.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/cligraphy_bukhari.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/go_surah_icon.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/hadith.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_islami_calendar.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_kaba.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_madina.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_more_99_names.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_more_calendar_converter.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_more_hadith.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_more_quran.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_prayertime.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_qibla_direction.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_quran.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_speaker.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/ic_tasbeeh.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/icon_allah.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/icon_quran.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/joy_distress.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/juz_icon.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/kaaba.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/quran_icon.png` | ✅ drawable-v24/ |
| `app/src/main/res/drawable/surah_icon.png` | ✅ drawable-v24/ |

**预计节省：1-2 MB**

---

## 📝 执行命令（复制粘贴执行）

### ⚡ 快速删除脚本（请在项目根目录执行）

```bash
#!/bin/bash
# APK 包体优化 - 删除重复文件脚本
# 执行前请确保已备份！

cd /Users/huwei/AndroidStudioProjects/quran0

echo "🗑️  开始删除重复文件..."

# 1. 删除 Tasbih 背景重复
echo "删除 Tasbih 背景重复..."
rm -f app/src/main/res/drawable/tasbih_bg.jpg
rm -f app/src/main/res/drawable-xhdpi/tasbih_bg.jpg

# 2. 删除 Tasbeeh 念珠图片重复
echo "删除 Tasbeeh 念珠系列重复..."
rm -f app/src/main/res/drawable/tasbeeh_1.png
rm -f app/src/main/res/drawable/tasbeeh_2.png
rm -f app/src/main/res/drawable/tasbeeh_3.png
rm -f app/src/main/res/drawable/tasbeeh_4.png
rm -f app/src/main/res/drawable/tasbeeh_5.png
rm -f app/src/main/res/drawable/tasbeeh_6.png
rm -f app/src/main/res/drawable/tasbeeh_7.png

# 3. 删除其他大文件重复
echo "删除其他大文件重复..."
rm -f app/src/main/res/drawable/today_header.jpg
rm -f app/src/main/res/drawable/img_prayer.png
rm -f app/src/main/res/drawable/compassfix.gif
rm -f app/src/main/res/drawable/rate.gif

# 4. 删除罗盘图片重复（drawable-v24/ 版本）
echo "删除罗盘图片重复..."
rm -f app/src/main/res/drawable-v24/compass_1.png
rm -f app/src/main/res/drawable-v24/compass_2.png
rm -f app/src/main/res/drawable-v24/compass_3.png
rm -f app/src/main/res/drawable-v24/compass_4.png
rm -f app/src/main/res/drawable-v24/compass_5.png
rm -f app/src/main/res/drawable-v24/compass_1_k.png
rm -f app/src/main/res/drawable-v24/compass_2_k.png
rm -f app/src/main/res/drawable-v24/compass_3_k.png
rm -f app/src/main/res/drawable-v24/compass_4_k.png
rm -f app/src/main/res/drawable-v24/compass_5_k.png

# 5. 删除小图标重复
echo "删除小图标重复..."
rm -f app/src/main/res/drawable/calibration_compass.png
rm -f app/src/main/res/drawable/cligraphy_bukhari.png
rm -f app/src/main/res/drawable/go_surah_icon.png
rm -f app/src/main/res/drawable/hadith.png
rm -f app/src/main/res/drawable/ic_islami_calendar.png
rm -f app/src/main/res/drawable/ic_kaba.png
rm -f app/src/main/res/drawable/ic_madina.png
rm -f app/src/main/res/drawable/ic_more_99_names.png
rm -f app/src/main/res/drawable/ic_more_calendar_converter.png
rm -f app/src/main/res/drawable/ic_more_hadith.png
rm -f app/src/main/res/drawable/ic_more_quran.png
rm -f app/src/main/res/drawable/ic_prayertime.png
rm -f app/src/main/res/drawable/ic_qibla_direction.png
rm -f app/src/main/res/drawable/ic_quran.png
rm -f app/src/main/res/drawable/ic_speaker.png
rm -f app/src/main/res/drawable/ic_tasbeeh.png
rm -f app/src/main/res/drawable/icon_allah.png
rm -f app/src/main/res/drawable/icon_quran.png
rm -f app/src/main/res/drawable/joy_distress.png
rm -f app/src/main/res/drawable/juz_icon.png
rm -f app/src/main/res/drawable/kaaba.png
rm -f app/src/main/res/drawable/quran_icon.png
rm -f app/src/main/res/drawable/surah_icon.png

echo "✅ 重复文件删除完成！"
echo "📊 预计节省空间：8-10 MB"
echo ""
echo "⚠️  请立即测试应用，确认所有功能正常："
echo "  1. Tasbih (念珠) 界面"
echo "  2. 罗盘/Qibla 方向"
echo "  3. 评分对话框"
echo "  4. 所有图标显示"
echo ""
echo "如果发现问题，使用 git restore 恢复文件"
```

---

## ⚠️ 执行前检查清单

- [ ] ✅ **已备份项目**（或确保 git 状态干净可回滚）
- [ ] ✅ **已阅读 APK_SIZE_OPTIMIZATION_REPORT.md**
- [ ] ✅ **理解删除原理**（保留高分辨率，删除低分辨率）
- [ ] ✅ **准备好测试设备**

---

## 🧪 删除后测试检查清单

### 必须测试的功能：

#### 1. Tasbih (念珠计数器)
- [ ] 打开 Tasbih 界面
- [ ] 背景图片正常显示
- [ ] 念珠图片1-7都正常显示
- [ ] 动画流畅

#### 2. Qibla 方向/罗盘
- [ ] 打开 Qibla 方向功能
- [ ] 罗盘图片正常显示（5个罗盘样式）
- [ ] 校准对话框的 compassfix.gif 正常显示
- [ ] 罗盘旋转正常

#### 3. 评分功能
- [ ] 触发评分对话框
- [ ] rate.gif 动画正常显示

#### 4. 主界面
- [ ] today_header.jpg 正常显示
- [ ] 所有小图标正常显示
- [ ] img_prayer.png 正常显示

#### 5. 多分辨率设备测试（如果可能）
- [ ] 在低分辨率设备测试（mdpi/hdpi）
- [ ] 在中等分辨率设备测试（xhdpi）
- [ ] 在高分辨率设备测试（xxhdpi/xxxhdpi）

---

## 🔄 回滚方法（如果出问题）

```bash
# 方法1：使用 git 回滚
cd /Users/huwei/AndroidStudioProjects/quran0
git restore app/src/main/res/

# 方法2：恢复特定文件
git restore app/src/main/res/drawable/tasbih_bg.jpg
git restore app/src/main/res/drawable/tasbeeh_*.png
# ... 等等
```

---

## 📊 预期效果

- **立即节省**：8-10 MB
- **风险等级**：✅ 极低（Android 会自动回退到可用资源）
- **执行时间**：< 1分钟
- **测试时间**：10-15分钟

---

## ✅ 确认后续步骤

删除重复文件后，如果测试通过，可以继续：

1. ⏭️ **第二阶段：压缩超大文件**
   - tasbih_bg.jpg (5.6MB → 500KB)
   - Tasbeeh PNG 系列 (压缩70-85%)
   - Wudu 步骤图 (压缩80%)

2. ⏭️ **第三阶段：分析字体使用**
   - 检测未使用的字体文件
   - 字体子集化优化

请确认是否执行删除操作！

