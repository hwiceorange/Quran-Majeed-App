# 🧪 测试其他语言指南

## 问题发现

从英语测试日志可以看到，英语是**正常工作的**。

但我发现 API 返回的语言键包括：
```
en, bn, de, fa, fr, gu, hi, in, ku, ml, ru, tr, ur
                            ^^
```

**API 使用 `in` 而不是 `id` 表示印尼语！**

---

## 🔧 已修复

我已经添加了语言代码映射：
- 应用内部：`id`（印尼语）
- 主 API：`in`（印尼语）
- 映射逻辑会自动转换

---

## 📋 测试清单

请按以下顺序测试每种语言：

### 1️⃣ 测试土耳其语

**步骤**:
```bash
# 清除应用数据
adb shell pm clear com.quran.quranaudio.online

# 启动日志
adb logcat -c
adb logcat | grep "FragOnboardQuranVersion"
```

**操作**:
1. 启动应用
2. 选择 **Türkçe**（土耳其语）
3. 点击 Continue
4. 观察日志

**期望日志**:
```
STEP 1: selectedLanguageCode = 'tr'
STEP 2: API返回的语言键: ..., tr, ...
        ✅ 找到语言键 'tr'
STEP 3: 所有版本的语言代码: languageCode='tr'
```

**如果看到**:
- ❌ `语言键 'tr' 在API中不存在` → API 问题
- ❌ 其他语言的版本（如 `languageCode='en'`）→ 过滤问题

---

### 2️⃣ 测试印尼语

**步骤**:
```bash
# 清除应用数据
adb shell pm clear com.quran.quranaudio.online

# 启动日志
adb logcat -c
adb logcat | grep "FragOnboardQuranVersion"
```

**操作**:
1. 启动应用
2. 选择 **Bahasa Indonesia**（印尼语）
3. 点击 Continue
4. 观察日志

**期望日志**:
```
STEP 1: selectedLanguageCode = 'id'
STEP 2: 语言代码: app='id' → API='in'
        API返回的语言键: ..., in, ...
        ✅ 找到语言键 'in'
STEP 3: 所有版本的语言代码: languageCode='id'
```

---

### 3️⃣ 测试阿拉伯语

**步骤**:
```bash
adb shell pm clear com.quran.quranaudio.online
adb logcat -c
adb logcat | grep "FragOnboardQuranVersion"
```

**操作**:
1. 启动应用
2. 选择 **العربية**（阿拉伯语）
3. 点击 Continue
4. 观察日志

**期望日志**:
```
STEP 1: selectedLanguageCode = 'ar'
STEP 2: ✅ 找到语言键 'ar'
STEP 3: 所有版本的语言代码: languageCode='ar'
```

---

### 4️⃣ 测试马来语

**步骤**:
```bash
adb shell pm clear com.quran.quranaudio.online
adb logcat -c
adb logcat | grep "FragOnboardQuranVersion"
```

**操作**:
1. 启动应用
2. 选择 **Bahasa Melayu**（马来语）
3. 点击 Continue
4. 观察日志

**期望日志**:
```
STEP 1: selectedLanguageCode = 'ms'
STEP 2: ✅ 找到语言键 'ms' 或者回退到备用API
STEP 3: 所有版本的语言代码: languageCode='ms'
```

---

## 📤 问题报告格式

如果某个语言有问题，请提供：

```
### 测试的语言
[例如: 土耳其语]

### 完整日志
[从 STEP 1 到 STEP 3 的所有日志]

### 问题描述
[例如: 显示了英语版本，而不是土耳其语版本]

### STEP 3 的关键信息
所有版本的语言代码:
  - [列出所有显示的版本及其 languageCode]
```

---

## 🎯 关键检查点

对于每种语言，检查：

1. **STEP 1**: `selectedLanguageCode` 是否正确？
2. **STEP 2**: API是否返回了该语言的键？
3. **STEP 2**: 是否成功找到该语言键？
4. **STEP 3**: `availableVersions` 中的所有版本是否都是该语言？
5. **STEP 3**: 是否有被过滤掉的其他语言版本？

---

## 💡 预期结果

### ✅ 成功的情况
```
所有版本的语言代码:
  - Translation 1: languageCode='tr'
  - Translation 2: languageCode='tr'
  - Translation 3: languageCode='tr'
```

### ❌ 失败的情况
```
所有版本的语言代码:
  - English Translation: languageCode='en'
  - Turkish Translation: languageCode='tr'
  - Arabic Translation: languageCode='ar'
```

如果看到失败情况，会有日志：
```
⚠️ 过滤掉: English Translation (其语言='en', 期望='tr')
⚠️ 过滤掉: Arabic Translation (其语言='ar', 期望='tr')
```

---

## 📞 下一步

测试完所有语言后，请将日志发给我：
1. 哪些语言正常工作
2. 哪些语言有问题
3. 问题语言的完整日志

这样我就能精确定位问题！

