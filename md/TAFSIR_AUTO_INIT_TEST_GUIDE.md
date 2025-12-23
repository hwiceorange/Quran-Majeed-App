# 🧪 Tafsir 注释自动初始化 - 测试指南

**问题：** 首次使用时点击注释弹窗 "Tafsir Not Available"  
**修复：** MainActivity 自动初始化默认 Tafsir  
**测试目标：** 验证各语言的 Tafsir 自动选择功能

---

## 🚀 快速测试（5分钟）

### 测试 1：印尼语 Tafsir 自动初始化

```bash
# 步骤 1: 清空应用数据（模拟首次使用）
adb shell pm clear com.quran.quranaudio.online

# 步骤 2: 安装最新版本
cd /Users/huwei/AndroidStudioProjects/quran0
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 步骤 3: 启动日志监控
adb logcat | grep -E "MainActivity|Tafsir|🔧|✅|🌍"
```

#### 在设备上操作：

1. **打开应用，完成引导流程**
   - 选择语言：Bahasa Indonesia
   - 选择翻译版本：Kompleks Al Quran
   - 完成其他引导步骤

2. **进入主页后，打开任意古兰经章节**
   - 例如：Surah 1 (Al-Fatihah)

3. **点击任意经文查看 Tafsir**
   - 点击经文 → 底部出现 Tafsir 按钮 → 点击

#### ✅ 预期结果：

- ✅ **直接显示印尼语 Tafsir 内容**
- ✅ **无弹窗**
- ✅ **无需手动选择**

#### ✅ 预期日志：

```
MainActivity: 🔧 No Tafsir selected, initializing default Tafsir...
MainActivity: 🌍 Target language for Tafsir: id
MainActivity: ✅ Auto-selected and saved Tafsir: id-tafsir-jalalayn for language: id
```

#### ❌ 如果失败：

如果仍然弹出 "Tafsir Not Available"，提供以下信息：
1. 完整日志输出（从 MainActivity 启动到打开 Tafsir）
2. 选择的语言
3. 网络连接状态

---

## 📋 完整测试清单

### 场景 1：首次使用各语言

| 语言 | 操作步骤 | 预期结果 | 状态 |
|------|----------|----------|------|
| **印尼语 (id)** | 清空数据 → 选择印尼语 → 打开 Tafsir | 直接显示，无弹窗 | ⬜ |
| **乌尔都语 (ur)** | 清空数据 → 选择乌尔都语 → 打开 Tafsir | 直接显示，无弹窗 | ⬜ |
| **英语 (en)** | 清空数据 → 选择英语 → 打开 Tafsir | 直接显示（预装） | ⬜ |
| **阿拉伯语 (ar)** | 清空数据 → 选择阿拉伯语 → 打开 Tafsir | 直接显示（预装） | ⬜ |

### 场景 2：已有 Tafsir 设置

| 步骤 | 操作 | 预期结果 | 状态 |
|------|------|----------|------|
| 2.1 | 首次使用英语，已初始化 Tafsir | - | ⬜ |
| 2.2 | 切换语言为印尼语 | 仍使用英语 Tafsir（已保存） | ⬜ |
| 2.3 | 可在设置中手动切换 | 切换成功 | ⬜ |

### 场景 3：网络异常

| 步骤 | 操作 | 预期结果 | 状态 |
|------|------|----------|------|
| 3.1 | 断开网络 → 首次使用印尼语 | 应用不崩溃 | ⬜ |
| 3.2 | 点击 Tafsir | 提示网络错误或自动回退 | ⬜ |

---

## 🔍 详细测试步骤

### 测试 1：印尼语（详细版）

```bash
# 1. 清空数据
adb shell pm clear com.quran.quranaudio.online

# 2. 启动日志监控（新终端窗口）
adb logcat -c  # 清空旧日志
adb logcat | grep -E "MainActivity|Tafsir|TranslUtils|🔧|✅|🌍" > test_indonesian.log

# 3. 安装应用
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### 在设备上：
1. 打开应用
2. **语言选择页：** 选择 "Bahasa Indonesia"
3. **古兰经版本页：** 选择 "Kompleks Al Quran Raja Fahd"
4. **完成引导流程：** 跳过通知、试用等步骤
5. **主页：** 点击 "Quran" 或 "القرآن"
6. **章节列表：** 选择任意章节（例如：Surah 1）
7. **经文页面：** 点击任意经文
8. **Tafsir 按钮：** 点击底部的 "Tafsir" 按钮

#### 预期：
- ✅ 直接显示印尼语 Tafsir 内容
- ✅ 无弹窗

#### 查看日志：
```bash
cat test_indonesian.log | grep "Tafsir"
```

---

### 测试 2：乌尔都语

重复测试 1 的步骤，但选择语言：اردو (Urdu)

**预期：**
- ✅ 直接显示乌尔都语 Tafsir
- ✅ 无弹窗

---

### 测试 3：英语（已有预装）

重复测试 1 的步骤，但选择语言：English

**预期：**
- ✅ 直接显示英语 Tafsir (预装)
- ✅ 无弹窗
- ✅ 日志显示使用预装 Tafsir

---

## 📊 测试结果记录

### 测试环境

- **测试日期：** _____________
- **测试人员：** _____________
- **设备型号：** _____________
- **Android 版本：** _____________
- **应用版本：** _____________

### 测试结果

| 测试场景 | 通过 | 失败 | 备注 |
|----------|------|------|------|
| 印尼语首次使用 | ⬜ | ⬜ | |
| 乌尔都语首次使用 | ⬜ | ⬜ | |
| 英语首次使用 | ⬜ | ⬜ | |
| 阿拉伯语首次使用 | ⬜ | ⬜ | |
| 已有设置不重复 | ⬜ | ⬜ | |
| 网络异常处理 | ⬜ | ⬜ | |

### 总体评价

- ⬜ 全部通过
- ⬜ 部分通过（需要修复）
- ⬜ 未通过（需要重新修复）

---

## 🐛 问题报告模板

如果测试未通过，请按以下格式报告：

```
### 问题描述
[描述具体问题]

### 重现步骤
1. [步骤 1]
2. [步骤 2]
3. [步骤 3]

### 预期结果
[应该看到什么]

### 实际结果
[实际看到什么]

### 截图
[如果可能，提供截图]

### 日志输出
```
[粘贴相关日志]
```

### 环境信息
- 应用语言设置：[例如：印尼语]
- 系统语言：[例如：英语]
- 网络连接：[WiFi/移动数据/断开]
- Android 版本：[例如：Android 14]
```

---

## ✅ 签收

当所有测试通过时，请在此确认：

- [ ] 印尼语 Tafsir 自动初始化正常
- [ ] 乌尔都语 Tafsir 自动初始化正常
- [ ] 英语 Tafsir 显示正常（预装）
- [ ] 阿拉伯语 Tafsir 显示正常（预装）
- [ ] 已有设置不会被覆盖
- [ ] 网络异常时不崩溃
- [ ] 无其他 Tafsir 相关问题

**签名：** _______________  
**日期：** _______________

---

**End of Test Guide** ✅

