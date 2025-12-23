# 🕌 古兰经数据导入 Firestore 完整指南

## 📋 概述

本指南帮助您将权威的古兰经文本、翻译和注释数据从公开 API 导入到 Firebase Firestore，用于支持基于 LLM 的个性化学习计划功能。

---

## 🎯 数据来源分析

### 当前应用的数据源

#### 1️⃣ **古兰经文本（阿拉伯语原文）**
- **来源**: 本地 JSON 文件
- **位置**: `/app/src/main/assets/scripts/`
  - `script_uthmani_hafs.json` - Uthmani 字体版本
  - `script_indopak.json` - Indo-Pak 字体版本
- **特点**: 
  - ✅ 完全离线
  - ✅ 包含完整 114 章、6236 节经文
  - ✅ 包含页码、Juz、Hizb 等元数据
  - ❌ 文件非常大（~25MB）

#### 2️⃣ **古兰经翻译**
- **来源**: 混合模式
  - **预装翻译**: `/app/src/main/assets/prebuilt_translations/`
    - English: Saheeh International, The Clear Quran
    - Urdu: Israr Ahmad, Junagarhi
    - Indonesian: Quran Complex
  - **在线下载**: 通过 `GithubApi` 从 `https://apis.dochubai.com/quran/` 下载
- **特点**:
  - ✅ 支持 10+ 种语言
  - ✅ 可动态下载新翻译
  - ✅ 本地缓存

#### 3️⃣ **古兰经注释（Tafsir）**
- **来源**: 在线 API
- **API**: `https://api.quran.com/api/qdc/tafsirs/{slug}/by_ayah/{verseKey}`
- **支持版本**:
  - Tafsir Ibn Kathir (English)
  - Tafsir al-Jalalayn
  - Tafsir Muyassar
  - 其他多种注释
- **特点**:
  - ✅ 实时获取
  - ✅ 多语言支持
  - ❌ 需要网络连接

#### 4️⃣ **Hadith（圣训）**
- **来源**: 本地 JSON 文件（前面已分析）
- **位置**: `/app/src/main/assets/*.min.json`
- **特点**: 
  - ✅ 完全离线
  - ✅ 6 大圣训集
  - ✅ 多语言支持（阿拉伯语、乌尔都语、印尼语）

---

## 🚀 导入 Firestore 的优势

### 为什么要导入 Firestore？

1. **支持 LLM 学习计划**:
   - AI 可以根据用户的学习进度和偏好生成个性化学习路径
   - 支持基于主题的学习（如：信仰、礼拜、慈善等）

2. **跨设备同步**:
   - 用户在多设备间同步学习进度
   - 云端存储用户的笔记和收藏

3. **动态内容更新**:
   - 无需重新发布 APK 即可更新翻译和注释
   - 支持 A/B 测试不同的学习策略

4. **高效查询**:
   - 按主题、按 Juz、按页码快速查询
   - 支持全文搜索（结合 Algolia 或 Elasticsearch）

---

## 🛠️ 导入工具使用指南

### 准备工作

#### 1. 安装 Python 依赖

```bash
cd /Users/huwei/AndroidStudioProjects/quran0
pip3 install -r requirements_importer.txt
```

#### 2. 获取 Firebase 服务账号密钥

1. 登录 [Firebase Console](https://console.firebase.google.com/)
2. 选择您的项目
3. 进入 **项目设置** → **服务账号**
4. 点击 **生成新的私钥**
5. 下载 JSON 文件，重命名为 `serviceAccountKey.json`
6. 将文件放在项目根目录

⚠️ **重要**: 请将 `serviceAccountKey.json` 添加到 `.gitignore`，避免泄露密钥！

#### 3. 配置脚本参数

编辑 `quran_firestore_importer.py`，修改以下配置：

```python
# 服务账号密钥路径
SERVICE_ACCOUNT_KEY_PATH = 'serviceAccountKey.json'

# 您的应用 ID
APP_ID = 'com.quran.quranaudio.online'

# 选择要导入的翻译版本
TRANSLATION_IDS = [
    131,  # 英语 - The Clear Quran
    134,  # 中文 - Ma Jian
    158,  # 乌尔都语 - Junagarhi
    33,   # 印尼语 - Ministry of Religious Affairs
]
```

---

### 运行导入

#### 方式 1: 从 Quran.com API 导入（推荐）

```bash
python3 quran_firestore_importer.py
# 选择选项 1
```

**优势**:
- ✅ 包含权威翻译
- ✅ 包含注释（Tafsir）
- ✅ 数据最新

**时间**: 预计 30-60 分钟（取决于网络速度和 API 速率限制）

#### 方式 2: 从本地 JSON 文件导入（快速）

```bash
python3 quran_firestore_importer.py
# 选择选项 2
```

**优势**:
- ✅ 速度快（5-10 分钟）
- ✅ 不依赖网络

**劣势**:
- ❌ 仅包含阿拉伯语原文
- ❌ 需要后续手动添加翻译

---

## 📊 Firestore 数据结构

### 路径设计

```
artifacts/
  └── com.quran.quranaudio.online/
      └── public/
          └── data/
              └── quran_texts/
                  └── surahs/
                      ├── 1/                    # Surah 1 (Al-Fatiha)
                      │   ├── (元数据)
                      │   │   - surah_id: 1
                      │   │   - name_ar: "الفاتحة"
                      │   │   - name_en: "Al-Fatiha"
                      │   │   - verses_count: 7
                      │   │   - revelation_place: "makkah"
                      │   └── ayahs/           # Ayah 子集合
                      │       ├── 1/
                      │       │   - ayah_id: 1
                      │       │   - text_ar: "بِسْمِ ٱللَّهِ..."
                      │       │   - translation_en: "In the name of Allah..."
                      │       │   - translation_zh: "奉至仁至慈的真主之名"
                      │       │   - page_number: 1
                      │       │   - juz_number: 1
                      │       ├── 2/
                      │       └── ... (至 7)
                      ├── 2/                    # Surah 2 (Al-Baqarah)
                      │   └── ayahs/
                      │       ├── 1/
                      │       └── ... (至 286)
                      └── ... (至 114)
```

### 文档字段说明

#### Surah 文档字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `surah_id` | Number | 章节编号 (1-114) |
| `name_ar` | String | 阿拉伯语名称 |
| `name_en` | String | 英语名称 |
| `verses_count` | Number | 经文数量 |
| `revelation_place` | String | 降示地点 (makkah/madinah) |
| `revelation_order` | Number | 降示顺序 |
| `pages` | Array | 页码范围 |

#### Ayah 文档字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `ayah_id` | Number | 节编号 (在章节内) |
| `surah_id` | Number | 所属章节编号 |
| `verse_key` | String | 经文键 (如 "1:1", "2:255") |
| `text_ar` | String | 阿拉伯语原文 |
| `translation_en` | String | 英语翻译 |
| `translation_zh` | String | 中文翻译 |
| `translation_ur` | String | 乌尔都语翻译 |
| `translation_id` | String | 印尼语翻译 |
| `page_number` | Number | 页码 |
| `juz_number` | Number | Juz 编号 (1-30) |
| `hizb_number` | Number | Hizb 编号 |

---

## 🔒 Firestore 安全规则

### 规则配置

编辑 `firestore.rules`:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // 🕌 古兰经公共数据（所有用户只读）
    match /artifacts/{appId}/public/data/quran_texts/surahs/{surahId} {
      allow read: if true;  // 公开数据，所有人可读
      allow write: if false;  // 禁止写入
      
      // Ayah 子集合
      match /ayahs/{ayahId} {
        allow read: if true;  // 公开数据
        allow write: if false;  // 禁止写入
      }
    }
    
    // 👤 用户私有数据
    match /artifacts/{appId}/users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### 部署规则

```bash
firebase deploy --only firestore:rules
```

---

## 🌐 可用的古兰经 API

### 1. Quran.com API (推荐) ✅

**官网**: https://quran.com  
**API 文档**: https://api-docs.quran.com/  
**Base URL**: `https://api.quran.com/api/v4`

**主要端点**:
- `GET /chapters` - 获取所有章节信息
- `GET /verses/by_chapter/{chapter_number}` - 获取章节经文
- `GET /quran/translations/{translation_id}` - 获取翻译
- `GET /quran/tafsirs/{tafsir_id}` - 获取注释

**特点**:
- ✅ 完全免费
- ✅ 权威数据源
- ✅ 多语言翻译（100+ 种）
- ✅ 多种注释版本
- ✅ 包含音频朗诵 URL
- ✅ RESTful API，易于使用

### 2. Al-Quran Cloud API

**Base URL**: `https://api.alquran.cloud/v1`

**主要端点**:
- `GET /surah/{number}` - 获取章节
- `GET /ayah/{reference}` - 获取经文

### 3. Islamic Network API

**Base URL**: `https://api.aladhan.com/v1`

**主要端点**:
- `GET /quran/{reference}` - 获取经文

---

## 📝 使用步骤

### 步骤 1: 准备环境

```bash
# 1. 安装 Python 依赖
pip3 install -r requirements_importer.txt

# 2. 下载 Firebase 服务账号密钥
# （请参考上方"准备工作"部分）

# 3. 确认密钥文件位置
ls -l serviceAccountKey.json
```

### 步骤 2: 配置脚本

编辑 `quran_firestore_importer.py`，修改配置：

```python
SERVICE_ACCOUNT_KEY_PATH = 'serviceAccountKey.json'
APP_ID = 'com.quran.quranaudio.online'

# 选择要导入的翻译（可添加更多）
TRANSLATION_IDS = [
    131,  # English - Clear Quran
    134,  # Chinese
    158,  # Urdu
    33,   # Indonesian
]
```

### 步骤 3: 运行导入

```bash
# 从 API 导入（推荐）
python3 quran_firestore_importer.py
# 选择选项 1

# 预计时间: 30-60 分钟
# 导入数据: 114 Surahs, 6236 Ayahs, 4 种翻译
```

### 步骤 4: 验证导入

```bash
# 查看 Firestore Console
# 导航到: artifacts/com.quran.quranaudio.online/public/data/quran_texts/surahs
# 确认有 114 个文档

# 或使用脚本内置的验证功能
# 脚本会自动验证 Surah 1 的数据
```

---

## 🔍 数据验证

### 验证清单

- [ ] Surah 1 (Al-Fatiha) 存在
- [ ] Surah 1 包含 7 条 Ayah
- [ ] Ayah 1:1 包含阿拉伯语原文
- [ ] Ayah 1:1 包含至少 1 种翻译
- [ ] Surah 2 (Al-Baqarah) 存在
- [ ] Surah 2 包含 286 条 Ayah
- [ ] Surah 114 (An-Nas) 存在
- [ ] 总计 114 个 Surah
- [ ] 总计 6236 条 Ayah

### 验证工具

```bash
# 查看 Firestore 数据（使用 Firebase CLI）
firebase firestore:get artifacts/com.quran.quranaudio.online/public/data/quran_texts/surahs/1

# 查看 Ayah 数据
firebase firestore:get artifacts/com.quran.quranaudio.online/public/data/quran_texts/surahs/1/ayahs/1
```

---

## 📱 Android 应用端读取示例

### Kotlin 代码示例

```kotlin
// 读取 Surah 1 的所有 Ayah
val db = FirebaseFirestore.getInstance()
val appId = "com.quran.quranaudio.online"

db.collection("artifacts/$appId/public/data/quran_texts/surahs/1/ayahs")
    .orderBy("ayah_id")
    .get()
    .addOnSuccessListener { documents ->
        for (document in documents) {
            val arabicText = document.getString("text_ar")
            val englishTranslation = document.getString("translation_en")
            val chineseTranslation = document.getString("translation_zh")
            
            Log.d("Quran", "Ayah ${document.id}: $arabicText")
            Log.d("Quran", "Translation (EN): $englishTranslation")
            Log.d("Quran", "Translation (ZH): $chineseTranslation")
        }
    }
    .addOnFailureListener { e ->
        Log.e("Quran", "Error loading Quran data", e)
    }
```

### Java 代码示例

```java
FirebaseFirestore db = FirebaseFirestore.getInstance();
String appId = "com.quran.quranaudio.online";

db.collection("artifacts/" + appId + "/public/data/quran_texts/surahs/1/ayahs")
    .orderBy("ayah_id")
    .get()
    .addOnSuccessListener(queryDocumentSnapshots -> {
        for (DocumentSnapshot document : queryDocumentSnapshots) {
            String arabicText = document.getString("text_ar");
            String translation = document.getString("translation_en");
            
            android.util.Log.d("Quran", "Ayah: " + arabicText);
            android.util.Log.d("Quran", "Translation: " + translation);
        }
    });
```

---

## 💡 LLM 学习计划生成示例

### 应用场景

利用导入的 Firestore 数据，结合 LLM（如 OpenAI GPT），可以实现：

1. **个性化学习路径**:
   - 根据用户的学习目标（如：学习祷告相关经文）
   - AI 推荐相关的 Surah 和 Ayah
   - 生成每日学习计划

2. **主题式学习**:
   - 用户选择主题（如：慈善、耐心、感恩）
   - AI 从 6236 节经文中筛选相关内容
   - 按难度和长度排序

3. **智能复习**:
   - 基于艾宾浩斯遗忘曲线
   - AI 决定复习时机和内容
   - 自适应调整难度

### LLM 提示词示例

```
System Prompt:
You are an Islamic scholar assistant. You have access to the complete Quran text 
with translations in multiple languages stored in Firestore.

User Request:
"I want to learn about patience (Sabr) in Islam. Please create a 7-day learning 
plan for me, selecting relevant verses from the Quran."

LLM Response:
Based on your interest in Sabr (patience), I've created a 7-day learning plan:

Day 1: Introduction to Sabr
- Surah 2, Ayah 153: "O you who believe, seek help through patience and prayer..."
- Surah 2, Ayah 155-157: "And We will surely test you with something of fear..."

Day 2: Patience in Adversity
- Surah 39, Ayah 10: "Indeed, the patient will be given their reward without account."
- Surah 16, Ayah 96: "What is with you will end, but what is with Allah will remain..."

[... continues for 7 days]
```

---

## ⚠️ 注意事项

### 1. API 速率限制

- Quran.com API 可能有速率限制
- 建议在导入过程中添加延迟（已在脚本中实现）
- 如遇 429 错误，增加 `time.sleep()` 时间

### 2. Firestore 写入限制

- 每个批处理最多 500 个操作
- 脚本已设置为 400 个/批次（留出余地）
- 每秒最多 1 个批处理（已添加延迟）

### 3. 成本估算

- **Firestore 存储**: 
  - 114 Surahs + 6236 Ayahs ≈ 6350 个文档
  - 每个文档 ~1-2 KB
  - 总存储 ~10-15 MB
  - 免费配额: 1 GB/月 ✅

- **读取操作**:
  - 用户每次打开学习计划页面 ≈ 1-10 次读取
  - 免费配额: 50,000 次/天 ✅

### 4. 数据版权

- ✅ 古兰经原文: 公共领域
- ✅ quran.com 翻译: 已获授权用于非商业应用
- ⚠️ 商业使用请联系 quran.com 获取许可

---

## 🎯 后续开发建议

### 阶段 1: 数据导入（当前）

- [x] 创建导入脚本
- [x] 配置 Firestore 安全规则
- [ ] 执行数据导入
- [ ] 验证数据完整性

### 阶段 2: Android 端集成

- [ ] 创建 `QuranFirestoreRepository` 读取 Firestore 数据
- [ ] 实现缓存机制（减少网络请求）
- [ ] 集成到现有的 `ActivityReader`

### 阶段 3: LLM 学习计划

- [ ] 集成 LLM API（OpenAI / Gemini / Claude）
- [ ] 实现学习计划生成逻辑
- [ ] 创建学习计划 UI
- [ ] 实现学习进度跟踪

---

## 📂 相关文件

- `quran_firestore_importer.py` - 导入脚本
- `requirements_importer.txt` - Python 依赖
- `firestore.rules` - Firestore 安全规则
- `QURAN_FIRESTORE_IMPORT_GUIDE.md` (本文档) - 使用指南

---

## 🆘 故障排查

### 问题 1: "服务账号密钥不存在"

**解决方案**:
```bash
# 确认文件存在
ls -l serviceAccountKey.json

# 如果不存在，从 Firebase Console 重新下载
```

### 问题 2: "API 请求失败"

**解决方案**:
```bash
# 检查网络连接
ping api.quran.com

# 检查 API 端点是否可访问
curl https://api.quran.com/api/v4/chapters
```

### 问题 3: "Firestore 写入权限被拒绝"

**解决方案**:
- 确认服务账号密钥有正确的权限
- 检查 Firebase 项目是否正确
- 在 Firebase Console 中检查 IAM 角色

---

**生成时间**: 2025-11-02  
**版本**: v1.6.9  
**作者**: AI Assistant

