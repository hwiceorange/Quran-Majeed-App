# Verse of The Day Card - Font Quick Reference

## 🎯 字体配置速查表

### 阿拉伯语经文
```xml
<TextView
    android:textSize="@dimen/dmnReaderTextSizeArUthmaniMedium"  <!-- 28sp -->
    android:fontFamily="@font/uthmanic_hafs"                     <!-- Uthmanic Hafs -->
    android:textDirection="rtl"                                  <!-- RTL -->
    android:lineSpacingMultiplier="1.6"                          <!-- 1.6x 行距 -->
    android:letterSpacing="0.02"                                 <!-- 字间距 -->
    android:gravity="center" />                                  <!-- 居中 -->
```

### 翻译文本
```xml
<TextView
    android:textSize="@dimen/dmnCommonSize"                      <!-- 16sp -->
    android:fontFamily="sans-serif"                              <!-- Roboto -->
    android:lineSpacingMultiplier="1.5"                          <!-- 1.5x 行距 -->
    android:textStyle="normal" />
```

### 经文出处
```xml
<TextView
    android:textSize="@dimen/dmnCommonSize3"                     <!-- 12sp -->
    android:fontFamily="sans-serif-light"                        <!-- Roboto Light -->
    android:alpha="0.9" />                                       <!-- 90% 透明度 -->
```

## 📊 字号对照表

| 资源名称 | 实际值 | 用途 |
|---------|--------|------|
| `dmnReaderTextSizeArUthmaniMedium` | 28sp | ✓ 阿拉伯语经文 |
| `dmnReaderTextSizeArUthmaniSmall` | 23sp | 小号经文 |
| `dmnCommonSize` | 16sp | ✓ 翻译文本 |
| `dmnCommonSize3` | 12sp | ✓ 经文出处 |

## 🎨 可用字体库

### 古兰经字体
| 字体文件 | 资源名称 | 推荐用途 |
|---------|---------|---------|
| `uthmanic_hafs.ttf` | `@font/uthmanic_hafs` | ✓ VOTD 阿拉伯语 |
| `indopak.ttf` | `@font/indopak` | IndoPak 风格 |
| `uthman.otf` | `@font/uthman` | 标准 Uthmanic |

### 系统字体
- `sans-serif` → Roboto (翻译)
- `sans-serif-light` → Roboto Light (出处)
- `sans-serif-medium` → Roboto Medium

## 🔄 实际显示效果

```
╔═══════════════════════════════════════════╗
║  Verse of The Day                         ║
║                                           ║
║  إِذَا أُلْقُوا۟ إِلَيْهَا سَمِعُوا۟ لَهَا شَهِيقًۭا وَهِىَ تَفُورُ  ║
║  (28sp, Uthmanic Hafs, RTL, 居中)       ║
║                                           ║
║  When they are thrown into it, they      ║
║  hear from it a [dreadful] inhaling      ║
║  while it boils up.                      ║
║  (16sp, Sans-serif, 1.5x行距)            ║
║                                           ║
║  ─────────────────────────────────────   ║
║                                           ║
║  Surah Al-Mulk 67: Verse 7      [图标]   ║
║  (12sp, Light, 90%透明度)                ║
╚═══════════════════════════════════════════╝
```

## 📱 不同屏幕密度的实际像素值

| 密度 | 阿拉伯语 (28sp) | 翻译 (16sp) | 出处 (12sp) |
|-----|----------------|------------|------------|
| mdpi | ~19px | ~11px | ~8px |
| hdpi | ~28px | ~16px | ~12px |
| xhdpi | ~37px | ~21px | ~16px |
| **xxhdpi** | **~56px** | **~32px** | **~24px** |
| xxxhdpi | ~75px | ~43px | ~32px |

## ✅ 检查清单

测试时验证以下项目：

**视觉效果**
- [ ] 阿拉伯语使用 Uthmanic Hafs 字体
- [ ] 文字从右到左显示（RTL）
- [ ] 字号层次清晰（28sp > 16sp > 12sp）
- [ ] 行距舒适，无重叠

**功能验证**
- [ ] 内容自动加载（500ms 延迟）
- [ ] 阿拉伯语和翻译正确分离
- [ ] 作者信息已移除
- [ ] 经文出处格式正确

**日志验证**
```bash
adb logcat -s PrayerAlarmScheduler:D | grep VOTD
```
应看到：
- "Verse of The Day card initialized"
- "VOTD content extracted and displayed"

---

**快速测试命令**
```bash
# 重启应用
adb shell am force-stop com.quran.quranaudio.online
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity

# 查看日志
adb logcat -s PrayerAlarmScheduler:D
```










