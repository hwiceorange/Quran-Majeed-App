# 🧪 重新测试孟加拉语翻译

## ✅ 已添加详细调试日志

我已经在以下位置添加了详细的调试日志：

1. **下载和保存**（`FragOnboardQuranVersion.kt`）
   - QuranTranslBookInfo 创建信息
   - 数据库保存结果
   - 数据库中所有翻译列表

2. **读取翻译**（`QuranTranslationFactory.kt`）
   - 请求的 slug 列表
   - SQL 查询语句
   - 查询结果数量
   - 是否找到翻译内容

## 🔄 测试步骤

```bash
# 1. 编译
cd /Users/huwei/AndroidStudioProjects/quran0
./gradlew assembleDebug

# 2. 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. 清除数据（重新开始）
adb shell pm clear com.quran.quranaudio.online

# 4. 启动并监控日志
adb logcat -c
adb logcat | grep -E "FragOnboardQuranVersion|QuranTranslationFactory|TranslUtils"
```

## 📱 操作流程

1. 启动应用 → 引导页
2. 选择 "Bengali (বাংলা)"
3. 点击 Continue
4. 选择 "তাইসীরুল কুরআন"（第一个）
5. 点击 Continue（等待下载 2-3秒）
6. 进入主页 → 打开古兰经
7. 打开 Surah 1 (Al-Fatihah)
8. **观察经文页面**：是否显示孟加拉语翻译

## 🔍 关键日志查找

### 下载时应该看到：

```
📊 QuranTranslBookInfo created:
   slug: 'bn_161_taisirul-quran'
   
✅ Translation stored in database with slug: 'bn_161_taisirul-quran'

📋 All translations in database (X):
   - slug: 'bn_161_taisirul-quran', displayName: 'তাইসীরুল কুরআন'
```

### 打开经文页面时应该看到：

```
📖 Getting translations for verse 1:1
   Requested slugs: [bn_161_taisirul-quran]
   
   🔍 Querying table 'bn_161_taisirul-quran'...
      SQL: SELECT * FROM `bn_161_taisirul-quran` WHERE ...
      Result count: 1
      ✅ Found translation: [孟加拉语文本...]
      
   📊 Result: 1 translations found
```

## ⚠️ 如果没有显示翻译

请复制完整的 logcat 输出，特别注意：

1. `QuranTranslBookInfo created` 的 slug 值
2. `All translations in database` 列表中是否包含孟加拉语
3. `Getting translations for verse` 的 Requested slugs
4. `Query failed` 或 `No translation found` 的错误信息

这些日志将帮助我们准确定位问题所在！

---

**准备好后，请重新运行测试并提供日志！** 📊

