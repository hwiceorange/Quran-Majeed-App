# Quiz Review & Learn 测试指南

## APK 信息
- **路径:** `/Users/huwei/AndroidStudioProjects/quran0/app/build/outputs/apk/debug/app-debug.apk`
- **大小:** 104MB
- **编译时间:** 2025-11-18 00:19
- **版本:** 1.8.1 (73)

---

## 安装说明

### 通过ADB安装
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 查看实时日志
```bash
# 查看Quiz模块相关日志
adb logcat | grep -E "QuizReviewLearn|VerseLoaderHelper|QuranQuestionFragment"

# 或使用标签过滤
adb logcat -s QuizReviewLearn VerseLoaderHelper
```

---

## 测试步骤

### 1. 崩溃测试 🔴 高优先级

**目标:** 确认进入Quiz模块不再崩溃

**步骤:**
1. 启动应用
2. 导航到Quiz模块
3. 开始答题（任意难度）
4. 故意答错一道题
5. 检查是否成功进入 "Review & Learn" 页面

**预期结果:**
✅ 应用不崩溃
✅ 成功显示 "Review & Learn" 页面
✅ 日志中显示：
```
📋 Setting up views for question: 1-1-1
   📍 Surah: 1, Ayah: 1
   ✅ Correct Answer: [答案内容]
```

**如果崩溃:**
请运行以下命令并提供完整的错误日志：
```bash
adb logcat -d > crash_log.txt
```

---

### 2. 经文显示测试 📖 高优先级

**目标:** 确认经文数据正确显示

**步骤:**
1. 在Quiz中答错一道题
2. 进入 "Review & Learn" 页面
3. 检查 "Related Verse" 卡片

**检查点:**
- [ ] **章节引用显示正确**
  - 格式："Surah X, Ayah Y"
  - 不应该显示 "Surah 0, Ayah 0"
  
- [ ] **阿拉伯语经文显示**
  - 应该显示实际的阿拉伯语文本
  - 不应该只显示占位符 "قُلْ هُوَ ٱللَّهُ أَحَدٌ"
  
- [ ] **英语翻译显示**
  - 应该显示对应的英语翻译
  - 不应该只显示阿拉伯语

**预期日志:**
```
🔍 Loading verse: Surah=1, Ayah=1
📚 Found 1 saved translation(s)
✅ Translation loaded successfully
✅ Successfully loaded verse: Surah:1, Ayah:1
   📖 Arabic: بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ...
   🌍 Translation: In the name of Allah, the Entirely Merciful...
```

**如果显示问题:**
请检查日志中的错误信息：
```bash
adb logcat | grep -E "❌|⚠️"
```

---

### 3. UI样式测试 🎨 中优先级

**目标:** 确认UI样式与截图一致

**检查点:**

#### 正确答案卡片
- [ ] 背景颜色：浅绿色 (#E8F5E9)
- [ ] 圆角：明显的圆角（16dp）
- [ ] 无阴影：扁平设计
- [ ] 左侧勾号图标：大小适中（32x32dp）
- [ ] "Correct Answer:" 标签：绿色粗体
- [ ] 答案内容：深灰色，易读

#### 经文卡片  
- [ ] 背景颜色：浅灰色 (#F8F9FA)
- [ ] 圆角：明显的圆角（16dp）
- [ ] "Related Verse" 标签：绿色粗体
- [ ] 章节引用：灰色小字（如果显示）
- [ ] 阿拉伯语：较大字号（22sp），右对齐
- [ ] 英语翻译：中等字号（15sp），行间距充足

#### 简化Tafsir卡片
- [ ] 背景颜色：浅灰色
- [ ] "Simplified Tafsir" 标签：绿色粗体
- [ ] 说明文本：易读，行间距充足
- [ ] "Full Tafsir (Premium)" 链接：带锁图标

#### 操作按钮
- [ ] "Try Again" 按钮：主绿色，带视频图标
- [ ] "Skip" 按钮：次级绿色，带视频图标
- [ ] "Quit Level" 按钮：灰色文字，无背景

**对比方法:**
1. 打开您之前提供的截图
2. 与应用中的实际显示对比
3. 重点关注颜色、圆角、字体大小

---

### 4. 功能测试 ⚙️ 中优先级

**目标:** 确认所有功能正常工作

#### 4.1 Try Again 功能
1. 点击 "Try Again" 按钮
2. 应该显示激励广告
3. 观看完广告后
4. 应该返回到**当前错误的题目**（不是第一题）
5. 可以重新作答

#### 4.2 Skip 功能
1. 点击 "Skip" 按钮
2. 应该显示激励广告
3. 观看完广告后
4. 应该跳到下一道题
5. 如果是第3题，应该进入下一关

#### 4.3 Quit Level 功能
1. 点击 "Quit Level"
2. 应该返回到当前关卡的第一题
3. 不需要观看广告

#### 4.4 Full Tafsir (Premium) 功能
1. 如果未订阅：点击后跳转到订阅页面
2. 如果已订阅：点击后显示详细Tafsir

#### 4.5 返回按钮
1. 点击左上角返回按钮
2. 应该等同于 "Quit Level"

---

### 5. 边缘情况测试 🔍 低优先级

#### 5.1 无Quran数据
如果用户未下载Quran数据：
- [ ] 应该显示友好的错误提示
- [ ] 不应该崩溃
- [ ] 章节引用应该正常显示

#### 5.2 无翻译数据
如果用户未下载翻译：
- [ ] 阿拉伯语应该正常显示
- [ ] 翻译区域显示 "Translation not available"
- [ ] 不应该崩溃

#### 5.3 网络问题
- [ ] 离线情况下，经文数据应该从本地加载
- [ ] 不应该有网络请求延迟

---

## 日志示例

### 正常流程日志
```
QuizReviewLearn: 📋 Setting up views for question: 1-1-1
QuizReviewLearn:    📍 Surah: 1, Ayah: 1
QuizReviewLearn:    ✅ Correct Answer: In the name of Allah
QuizReviewLearn:    📝 Explanation: Bismillah translates as 'In the name of Allah'....
QuizReviewLearn: 📖 Loading verse data for Surah:1, Ayah:1
VerseLoaderHelper: 🔍 Loading verse: Surah=1, Ayah=1
VerseLoaderHelper: 📚 Found 1 saved translation(s)
VerseLoaderHelper: ✅ Translation loaded successfully
VerseLoaderHelper: ✅ Successfully loaded verse: Surah:1, Ayah:1
VerseLoaderHelper:    📖 Arabic: بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ...
VerseLoaderHelper:    🌍 Translation: In the name of Allah, the Entirely Merciful, the Especially Merciful....
QuizReviewLearn: ✅ Verse data loaded successfully
```

### 错误日志示例
如果出现问题，日志可能显示：
```
VerseLoaderHelper: ❌ Quran module class not found
VerseLoaderHelper: ❌ Error loading verse: [错误详情]
VerseLoaderHelper: ⚠️ No saved translations found
QuizReviewLearn: ⚠️ Failed to load verse data, using placeholder
```

---

## 问题报告

如果发现问题，请提供以下信息：

### 1. 基本信息
- Android版本：
- 设备型号：
- 应用版本：1.8.1 (73)
- 是否首次安装：

### 2. 问题描述
- 问题类型：[崩溃/显示错误/功能异常]
- 复现步骤：
- 预期行为：
- 实际行为：

### 3. 日志信息
```bash
# 导出完整日志
adb logcat -d > full_log.txt

# 或导出过滤后的日志
adb logcat -d | grep -E "QuizReviewLearn|VerseLoaderHelper" > quiz_log.txt
```

### 4. 截图
请提供：
- 问题发生时的截图
- 如果是UI问题，提供对比截图

---

## 测试通过标准

✅ **所有测试通过** 如果：
1. 进入Quiz模块不崩溃
2. 章节和Ayah号正确显示（不是0）
3. 阿拉伯语经文正确显示
4. 英语翻译正确显示
5. UI样式与截图基本一致
6. 所有功能按钮正常工作

⚠️ **部分测试通过** 如果：
1. 核心功能正常但有小UI差异
2. 某些边缘情况有问题
3. 日志中有警告但不影响使用

❌ **测试失败** 如果：
1. 应用崩溃
2. 经文数据完全无法加载
3. 核心功能无法使用

---

## 下一步行动

根据测试结果：

### 如果测试通过 ✅
1. 可以开始使用新功能
2. 考虑在真机上进行更全面的测试
3. 收集用户反馈

### 如果有小问题 ⚠️
1. 记录具体问题
2. 提供详细的复现步骤
3. 提供日志和截图
4. 讨论优先级

### 如果测试失败 ❌
1. 立即停止测试
2. 提供完整的崩溃日志
3. 详细描述复现步骤
4. 回滚到之前的稳定版本

---

## 联系支持

如有任何问题，请提供：
1. 完整的logcat日志
2. 问题截图
3. 详细的复现步骤
4. 设备和系统信息

---

**祝测试顺利！** 🎉

