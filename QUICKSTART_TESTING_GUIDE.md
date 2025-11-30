# 🚀 快速开始指南

## 📱 测试按需下载功能

按需下载功能已经完全实现！现在您可以立即测试。

---

## 方式 1: 自动化测试脚本（推荐）

### 运行测试脚本
```bash
cd /Users/huwei/AndroidStudioProjects/quran0
./test_multilang.sh
```

**脚本会自动**:
1. ✅ 检查设备连接
2. ✅ 编译应用
3. ✅ 安装到设备
4. ✅ 启动应用
5. ✅ 监控日志

**然后您只需要**:
- 在设备上选择 Bengali
- 选择翻译
- 点击 Continue
- 验证下载和显示

---

## 方式 2: 手动测试

### 步骤 1: 编译和安装
```bash
cd /Users/huwei/AndroidStudioProjects/quran0

# 编译
./gradlew assembleDebug

# 安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 清除数据（模拟新用户）
adb shell pm clear com.quran.quranaudio.online

# 启动
adb shell am start -n com.quran.quranaudio.online/.quran_module.activities.ActivitySplash
```

### 步骤 2: 测试流程

#### 测试孟加拉语 🇧🇩
```
1. 启动应用 → 引导页
2. 选择语言 → "Bengali (বাংলা)"
3. 点击 Continue
4. 选择翻译 → "তাইসীরুল কুরআন" (第一个，推荐)
5. 点击 Continue
6. 等待下载（2-3秒）
7. 进入主页 → 打开古兰经
8. 验证显示孟加拉语翻译

✅ 预期结果：
- UI 为孟加拉语
- 章节列表为孟加拉语
- 经文翻译为孟加拉语
- 总共6,236条经文都有翻译
```

#### 测试马来语 🇲🇾
```
1. 设置 → 语言 → "Bahasa Melayu"
2. 重启应用
3. 设置 → 翻译管理
4. 选择 "Abdullah Muhammad Basmeih"
5. 下载并验证

✅ 预期结果：
- UI 为马来语
- 翻译为马来语
```

#### 测试土耳其语 🇹🇷
```
1. 设置 → 语言 → "Türkçe"
2. 重启应用
3. 设置 → 翻译管理
4. 选择 "Diyanet İşleri"
5. 下载并验证

✅ 预期结果：
- UI 为土耳其语
- 翻译为土耳其语
```

### 步骤 3: 验证现有功能（重要！）

```bash
# 测试英语（预装）
设置 → 语言 → English
古兰经 → Surah 1
验证: Sahih International 显示正常 ✅

# 测试印尼语（预装）
设置 → 语言 → Bahasa Indonesia
古兰经 → Surah 1
验证: Kompleks Al Quran 显示正常 ✅

# 测试乌尔都语（预装）
设置 → 语言 → اردو
古兰经 → Surah 1
验证: Junagarhi 显示正常 ✅
```

---

## 方式 3: 查看日志

### 实时监控下载过程
```bash
# 清除旧日志
adb logcat -c

# 监控关键日志
adb logcat | grep -E "FragOnboardQuranVersion|QuranTranslDBHelper|TranslUtils"
```

### 关键日志标识

**成功下载**:
```
FragOnboardQuranVersion: 📥 STEP 4: 开始下载古兰经翻译版本
FragOnboardQuranVersion: 📡 下载源: Quran Foundation API
FragOnboardQuranVersion: Translation ID: 161
FragOnboardQuranVersion: ✅ Translation downloaded: তাইসীরুল কুরআন
```

**保存到数据库**:
```
QuranTranslDBHelper: Storing translation: bn_161_taisirul-quran
QuranTranslDBHelper: Created table: bn_161_taisirul_quran
QuranTranslDBHelper: Inserted 6236 verses
```

**Tafsir配置**:
```
FragOnboardQuranVersion: 📖 配置默认 Tafsir...
FragOnboardQuranVersion: ✅ 选择的 Tafsir: bn-tafseer-ibn-e-kaseer
FragOnboardQuranVersion: ✅ Tafsir 已保存
```

---

## 🔍 验证数据库

### 检查已下载翻译
```bash
# 进入设备 shell
adb shell

# 查看数据库文件
ls -lh /data/data/com.quran.quranaudio.online/databases/

# 查看数据库表（需要root或调试模式）
sqlite3 /data/data/com.quran.quranaudio.online/databases/QuranTranslation.db ".tables"
```

**预期看到**:
```
QuranTranslationBookInfo    # 翻译元数据表
bn_161_taisirul_quran      # 孟加拉语翻译表 🆕
en_101_sahih_international # 英语翻译表（预装）
...
```

### 检查 SharedPreferences
```bash
adb shell cat /data/data/com.quran.quranaudio.online/shared_prefs/key.translations.xml
```

**预期看到**:
```xml
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <set name="key.translations">
        <string>bn_161_taisirul-quran</string>
    </set>
</map>
```

---

## 🐛 故障排除

### 问题 1: 下载失败

**症状**: 点击 Continue 后无反应

**检查**:
```bash
# 1. 检查网络
ping api.quran.com

# 2. 查看错误日志
adb logcat | grep -E "Exception|Error|Failed"
```

**解决**:
- 确保设备连接到互联网
- 检查是否有防火墙限制
- 重试下载

### 问题 2: 翻译不显示

**症状**: 下载完成但经文页面没有翻译

**检查**:
```bash
# 1. 检查 SharedPreferences
adb shell cat /data/data/com.quran.quranaudio.online/shared_prefs/key.translations.xml

# 2. 检查数据库表
adb shell sqlite3 /data/data/com.quran.quranaudio.online/databases/QuranTranslation.db ".tables"
```

**解决**:
- 清除应用数据重试
- 检查翻译 slug 是否正确
- 查看完整日志

### 问题 3: 应用崩溃

**症状**: 选择翻译后应用崩溃

**检查**:
```bash
# 查看崩溃日志
adb logcat | grep -E "AndroidRuntime|FATAL"
```

**解决**:
- 查看堆栈跟踪
- 检查 Null Pointer Exception
- 提供完整日志

---

## ✅ 成功标志

### 您应该看到

1. **引导页**:
   - ✅ 孟加拉语 UI
   - ✅ 3个孟加拉语翻译选项
   - ✅ "তাইসীরুল কুরআন" 为推荐（第一个）

2. **下载过程**:
   - ✅ 下载时间 2-3 秒
   - ✅ Toast 提示"下载完成"
   - ✅ 日志显示"Translation downloaded"

3. **古兰经页面**:
   - ✅ 章节列表显示孟加拉语
   - ✅ 经文显示孟加拉语翻译
   - ✅ 所有6,236条经文都有翻译
   - ✅ 文本清晰可读

4. **现有功能**:
   - ✅ 英语翻译正常
   - ✅ 印尼语翻译正常
   - ✅ 乌尔都语翻译正常
   - ✅ 无任何功能退化

---

## 📚 相关文档

- **`PROJECT_DELIVERY_REPORT.md`** ⭐ 项目交付报告（完整概述）
- **`ON_DEMAND_DOWNLOAD_COMPLETE.md`** - 实现详情和使用指南
- **`FINAL_SUMMARY_AND_RECOMMENDATIONS.md`** - 方案对比和推荐

---

## 🎯 测试清单

### 基本测试
- [ ] 编译成功
- [ ] 安装成功
- [ ] 应用启动正常

### 孟加拉语测试
- [ ] 引导页显示孟加拉语选项
- [ ] 选择 Taisirul Quran
- [ ] 下载成功（2-3秒）
- [ ] 经文显示孟加拉语
- [ ] 全部6,236条经文有翻译

### 其他语言测试
- [ ] 马来语下载和显示
- [ ] 土耳其语下载和显示

### 回归测试
- [ ] 英语翻译正常（Sahih International）
- [ ] 英语翻译正常（The Clear Quran）
- [ ] 印尼语翻译正常（Kompleks Al Quran）
- [ ] 乌尔都语翻译正常（Junagarhi）

### 性能测试
- [ ] 下载速度 < 5秒
- [ ] APK 体积无显著增加
- [ ] 内存使用正常
- [ ] UI 响应流畅

---

## 🚀 准备发布

测试通过后：

1. ✅ 更新版本号
2. ✅ 准备发布说明
3. ✅ 生成签名 APK
4. ✅ 上传到 Google Play

---

**祝测试顺利！如有问题请查看日志或参考文档。** 🎉

