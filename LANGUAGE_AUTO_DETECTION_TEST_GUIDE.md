# 语言自动检测功能测试指南

## ✅ 修复已完成

**修复日期**: 2025-10-22  
**修复内容**: 实现首次启动时自动检测设备语言  
**编译状态**: ✅ BUILD SUCCESSFUL

---

## 🔧 已修改的文件

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| `SPAppConfigs.kt` | 添加设备语言自动检测逻辑 | ✅ 完成 |
| `quran_module/activities/base/BaseActivity.java` | 删除跳过默认语言的特殊处理 | ✅ 完成 |
| `prayertimes/ui/BaseActivity.java` | 删除跳过默认语言的特殊处理 | ✅ 完成 |

---

## 🧪 测试场景

### 场景 1: 支持语言的自动检测（最重要）

#### 测试步骤

**前置条件**: 应用从未安装过（或已完全卸载清除数据）

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 设置设备语言为**印尼语** (Bahasa Indonesia) | - |
| 2 | 安装并启动应用 | ✅ 应用界面显示**印尼语** |
| 3 | 卸载应用，设置设备语言为**阿拉伯语** | - |
| 4 | 重新安装并启动应用 | ✅ 应用界面显示**阿拉伯语**（RTL布局） |
| 5 | 卸载应用，设置设备语言为**乌尔都语** | - |
| 6 | 重新安装并启动应用 | ✅ 应用界面显示**乌尔都语**（RTL布局） |
| 7 | 卸载应用，设置设备语言为**马来语** | - |
| 8 | 重新安装并启动应用 | ✅ 应用界面显示**马来语** |
| 9 | 卸载应用，设置设备语言为**土耳其语** | - |
| 10 | 重新安装并启动应用 | ✅ 应用界面显示**土耳其语** |
| 11 | 卸载应用，设置设备语言为**孟加拉语** | - |
| 12 | 重新安装并启动应用 | ✅ 应用界面显示**孟加拉语** |

---

### 场景 2: 不支持语言的回退到英语

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 设置设备语言为**中文** | - |
| 2 | 安装并启动应用 | ✅ 应用界面显示**英语**（默认） |
| 3 | 卸载应用，设置设备语言为**法语** | - |
| 4 | 重新安装并启动应用 | ✅ 应用界面显示**英语**（默认） |
| 5 | 卸载应用，设置设备语言为**日语** | - |
| 6 | 重新安装并启动应用 | ✅ 应用界面显示**英语**（默认） |

---

### 场景 3: 手动切换语言后保持偏好

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 设置设备语言为**印尼语**，启动应用 | ✅ 应用显示印尼语 |
| 2 | 进入 Settings → Language → 选择**阿拉伯语** | ✅ 应用重启后显示阿拉伯语 |
| 3 | 关闭应用，将设备语言改为**英语** | - |
| 4 | 重新启动应用 | ✅ 应用仍显示**阿拉伯语**（保持用户偏好） |
| 5 | 卸载重装应用（设备语言仍为英语） | ✅ 应用显示**英语** |

---

### 场景 4: RTL语言的布局验证

**测试语言**: 阿拉伯语、乌尔都语

| 检查项 | 预期效果 |
|--------|----------|
| 整体布局方向 | ✅ 从右到左 |
| 文本对齐 | ✅ 右对齐 |
| 导航抽屉 | ✅ 从右侧滑出 |
| 返回按钮 | ✅ 在右上角，箭头指向右 |
| 祷告时间卡片 | ✅ 从右到左排列 |
| 图标镜像 | ✅ 方向性图标自动翻转 |

---

## 🖥️ 测试命令（ADB）

### 快速测试脚本（推荐）

```bash
#!/bin/bash
# 语言自动检测测试脚本

# 定义测试语言
declare -A LANGUAGES=(
    ["印尼语"]="in-ID"
    ["阿拉伯语"]="ar-SA"
    ["乌尔都语"]="ur-PK"
    ["马来语"]="ms-MY"
    ["土耳其语"]="tr-TR"
    ["孟加拉语"]="bn-BD"
    ["中文"]="zh-CN"
)

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.quran.quranaudio.online"

echo "=========================================="
echo "语言自动检测测试"
echo "=========================================="

for lang_name in "${!LANGUAGES[@]}"; do
    locale="${LANGUAGES[$lang_name]}"
    
    echo ""
    echo "📱 测试 $lang_name ($locale)"
    echo "------------------------------------------"
    
    # 1. 卸载应用
    echo "1️⃣ 卸载应用..."
    adb uninstall $PACKAGE_NAME 2>/dev/null
    
    # 2. 设置设备语言
    echo "2️⃣ 设置设备语言为 $lang_name..."
    adb shell "setprop persist.sys.locale $locale"
    adb shell "stop"
    sleep 2
    adb shell "start"
    sleep 5
    
    # 3. 安装应用
    echo "3️⃣ 安装应用..."
    adb install -r $APK_PATH
    
    # 4. 启动应用
    echo "4️⃣ 启动应用..."
    adb shell am start -n $PACKAGE_NAME/.SplashScreenActivity
    
    echo "✅ 请在设备上验证应用显示的语言"
    echo ""
    read -p "按 Enter 继续下一个测试..."
done

echo ""
echo "=========================================="
echo "✅ 所有测试完成！"
echo "=========================================="
```

### 手动测试命令

#### 测试印尼语

```bash
# 卸载应用
adb uninstall com.quran.quranaudio.online

# 设置为印尼语
adb shell "setprop persist.sys.locale in-ID; stop; start"
sleep 5

# 安装并启动
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
```

#### 测试阿拉伯语（RTL）

```bash
# 卸载应用
adb uninstall com.quran.quranaudio.online

# 设置为阿拉伯语
adb shell "setprop persist.sys.locale ar-SA; stop; start"
sleep 5

# 安装并启动
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
```

#### 测试乌尔都语（RTL）

```bash
# 卸载应用
adb uninstall com.quran.quranaudio.online

# 设置为乌尔都语
adb shell "setprop persist.sys.locale ur-PK; stop; start"
sleep 5

# 安装并启动
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
```

#### 测试马来语

```bash
# 卸载应用
adb uninstall com.quran.quranaudio.online

# 设置为马来语
adb shell "setprop persist.sys.locale ms-MY; stop; start"
sleep 5

# 安装并启动
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
```

#### 测试土耳其语

```bash
# 卸载应用
adb uninstall com.quran.quranaudio.online

# 设置为土耳其语
adb shell "setprop persist.sys.locale tr-TR; stop; start"
sleep 5

# 安装并启动
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
```

#### 测试孟加拉语

```bash
# 卸载应用
adb uninstall com.quran.quranaudio.online

# 设置为孟加拉语
adb shell "setprop persist.sys.locale bn-BD; stop; start"
sleep 5

# 安装并启动
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
```

#### 测试不支持的语言（中文）

```bash
# 卸载应用
adb uninstall com.quran.quranaudio.online

# 设置为中文
adb shell "setprop persist.sys.locale zh-CN; stop; start"
sleep 5

# 安装并启动（应显示英语）
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.quran.quranaudio.online/.SplashScreenActivity
```

#### 恢复英语

```bash
adb shell "setprop persist.sys.locale en-US; stop; start"
```

---

## 📊 验证检查清单

### UI文本检查

打开以下页面，确认文本正确显示为对应语言：

- [ ] **主页** (Home)
  - [ ] "Daily Quests" / "Misi Harian" / "المهام اليومية" / etc.
  - [ ] "Verse of the Day" / "Ayat Hari Ini" / "آية اليوم" / etc.
  
- [ ] **Salat页面**
  - [ ] 祷告名称 (Fajr, Dhohr, Asr, Maghrib, Icha)
  - [ ] "Next Prayer" / "Solat Seterusnya" / etc.
  
- [ ] **Settings页面**
  - [ ] "Language" / "Bahasa" / "اللغة" / etc.
  - [ ] "Theme" / "Tema" / "الثيم" / etc.

- [ ] **Daily Quests页面**
  - [ ] 任务描述文本
  - [ ] "Track" 按钮

### RTL布局检查（阿拉伯语、乌尔都语）

- [ ] 整体布局从右到左
- [ ] 文本右对齐
- [ ] 导航抽屉从右侧滑出
- [ ] 返回按钮在右上角
- [ ] 时间显示正确（RTL数字格式）
- [ ] 列表项从右到左排列

---

## 🐛 常见问题排查

### 问题 1: 设备语言改变后应用仍显示旧语言

**可能原因**: SharedPreferences 中已保存语言偏好

**解决方法**:
```bash
# 清除应用数据
adb shell pm clear com.quran.quranaudio.online

# 或完全卸载重装
adb uninstall com.quran.quranaudio.online
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 问题 2: RTL语言布局显示异常

**检查项**:
1. `AndroidManifest.xml` 中 `android:supportsRtl="true"` 是否存在
2. 方向性图标是否添加了 `android:autoMirrored="true"`
3. 布局文件是否使用了 `Start/End` 而不是 `Left/Right`

**验证命令**:
```bash
# 检查 RTL 支持
grep "supportsRtl" app/src/main/AndroidManifest.xml

# 检查图标镜像
grep -r "autoMirrored" app/src/main/res/drawable* | wc -l
```

### 问题 3: 首次启动仍显示英语

**调试步骤**:
```bash
# 查看日志，确认检测到的设备语言
adb logcat | grep -i "locale\|language"

# 检查 SharedPreferences
adb shell run-as com.quran.quranaudio.online cat shared_prefs/sp_app_configs.xml
```

---

## 📝 测试结果记录表

| 测试语言 | 设备Locale | 预期语言 | 实际显示 | RTL布局 | 状态 |
|---------|-----------|---------|---------|---------|------|
| 🇮🇩 印尼语 | in-ID | 印尼语 | ___ | N/A | ⬜ |
| 🇸🇦 阿拉伯语 | ar-SA | 阿拉伯语 | ___ | ✅ / ❌ | ⬜ |
| 🇵🇰 乌尔都语 | ur-PK | 乌尔都语 | ___ | ✅ / ❌ | ⬜ |
| 🇲🇾 马来语 | ms-MY | 马来语 | ___ | N/A | ⬜ |
| 🇹🇷 土耳其语 | tr-TR | 土耳其语 | ___ | N/A | ⬜ |
| 🇧🇩 孟加拉语 | bn-BD | 孟加拉语 | ___ | N/A | ⬜ |
| 🇨🇳 中文 | zh-CN | 英语 | ___ | N/A | ⬜ |
| 🇫🇷 法语 | fr-FR | 英语 | ___ | N/A | ⬜ |

---

## 🎯 验收标准

### ✅ 必须通过

1. **首次安装时自动检测设备语言**
   - 设备语言在支持列表中 → 显示对应语言
   - 设备语言不在支持列表中 → 显示英语

2. **RTL语言布局正确**
   - 阿拉伯语和乌尔都语显示从右到左布局
   - 文本右对齐
   - 导航元素镜像翻转

3. **手动切换后保持偏好**
   - 用户手动选择语言后，不受设备语言影响
   - 重启应用保持用户选择

4. **编译无错误**
   - `./gradlew assembleDebug` 成功
   - 无新增警告或错误

### ✅ 可选验证

1. **性能测试**
   - 应用启动速度无明显降低
   - 语言切换流畅

2. **边界情况**
   - 设备语言设置为 `null` 或无效值
   - 同时更改设备语言和区域设置

---

## 📞 测试支持

如有问题，请检查：

1. **分析报告**: `LANGUAGE_AUTO_DETECTION_ANALYSIS.md`
2. **多语言扩展报告**: `MULTILANG_EXPANSION_REPORT.md`
3. **代码修改位置**:
   - `app/src/main/java/com/quran/quranaudio/online/quran_module/utils/sharedPrefs/SPAppConfigs.kt:48-72`
   - `app/src/main/java/com/quran/quranaudio/online/quran_module/activities/base/BaseActivity.java:70-84`
   - `app/src/main/java/com/quran/quranaudio/online/prayertimes/ui/BaseActivity.java:40-54`

---

**测试指南版本**: 1.0  
**最后更新**: 2025-10-22  
**修复验证**: ✅ BUILD SUCCESSFUL

