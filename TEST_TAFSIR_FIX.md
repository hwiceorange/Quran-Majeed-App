# 🧪 Tafsir 修复测试指南

## 🎯 测试目标

1. ✅ 确保 Tafsir 清单从 GitHub API 成功加载
2. ✅ 确保印尼语 Tafsir 被自动选择
3. ✅ 确保注释内容能够正常显示

---

## 📋 测试步骤

### 1. 编译应用

```bash
cd /Users/huwei/AndroidStudioProjects/quran0
./gradlew :app:assembleDebug
```

### 2. 卸载并重新安装

```bash
adb uninstall com.quran.quranaudio.online
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. 监控日志

```bash
adb logcat -c
adb logcat | grep -E 'TafsirManager|MainActivity.*Tafsir|ActivityTafsir'
```

### 4. 在手机上测试

1. 打开应用（语言设置为印尼语）
2. 打开古兰经任意经文
3. 点击 Tafsir（注释）按钮
4. 观察日志和应用行为

---

## 📊 预期日志

```
TafsirManager: 🔧 prepare called: force=false, hasModel=false
TafsirManager: 📥 loadTafsirs called: force=false
TafsirManager: 📂 Tafsir manifest file: .../available_tafsirs.json, exists=false
TafsirManager: ⚠️ Manifest file not found, forcing network load
TafsirManager: 📥 loadTafsirs called: force=true
TafsirManager: 🌐 Force loading from network...
TafsirManager: 📥 Received response, length=XXXX  ← 成功！
TafsirManager: ✅ Network load successful
TafsirManager: 📊 postTafsirsLoad called
TafsirManager: ✅ Parsed X language groups
TafsirManager:    - id: X tafsirs  ← 印尼语
MainActivity: ✅ Auto-selected and saved Tafsir: id-tafsir-kemenag
```

---

## ❌ 如果仍然失败

如果看到：
```
TafsirManager: ❌ Network load failed: ...
```

请提供完整的错误日志。

---

## 🔍 印尼语 Tafsir 数据位置

根据您提供的信息：

- **本地数据库**: `/Users/huwei/AndroidStudioProjects/quran0/scripts/tafsir_database.db`
- **表名**: `tafsir_indonesian`
- **记录数**: 6,236 条
- **语言**: `id` (Indonesian)

**下一步（清单加载成功后）:**
1. 确认印尼语 Tafsir 在清单中的 key
2. 修改应用逻辑，从服务器或本地数据库加载印尼语 Tafsir 内容
3. 集成到 `ActivityTafsir` 和答题模块

---

**请先运行上述测试，并提供日志结果！** 🚀
