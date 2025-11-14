# 📊 应用数据来源完整分析报告

## 📋 概述

本报告详细分析了 Quran Majeed 应用中所有核心数据的来源、存储方式和更新机制。

---

## 🕌 古兰经相关数据

### 1. 古兰经文本（阿拉伯语原文）

**数据来源**: ✅ **本地 JSON 文件**

**文件位置**:
```
/app/src/main/assets/scripts/
├── script_uthmani_hafs.json    (~25 MB, Uthmani 字体版本)
└── script_indopak.json         (~25 MB, Indo-Pak 字体版本)
```

**数据结构**:
```json
{
  "chapters": [
    {
      "number": 1,
      "verses": [
        {
          "id": 1,
          "number": 1,
          "arabic_text": "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ",
          "page_number": 1
        }
      ]
    }
  ]
}
```

**加载方式**:
- 通过 `QuranParser.kt` 解析 JSON
- 缓存在内存中的 `Quran` 对象
- 路径: `QuranParser.parse() → ctx.assets.open()`

**特点**:
- ✅ 完全离线
- ✅ 加载速度快
- ✅ 包含完整 114 章、6236 节
- ✅ 包含页码、Juz、Hizb 等元数据
- ❌ 文件体积大（~50 MB 总计）
- ❌ 更新需要重新发布 APK

---

### 2. 古兰经翻译

**数据来源**: 🌐 **混合模式（预装 + 在线下载）**

#### A. 预装翻译（本地）

**文件位置**:
```
/app/src/main/assets/prebuilt_translations/
├── en_saheeh_v1_1_1/
│   ├── en_saheeh_v1_1_1.json
│   └── manifest.json
├── en_the_clear_quran/
│   ├── en_the_clear_quran.json
│   └── manifest.json
├── ur_junagarhi/
│   ├── ur_junagarhi.json
│   └── manifest.json
├── ur_israr_ahmad/
│   └── ur_israr-ahmad.json
└── in/
    ├── in_quran-complex.json
    └── manifest.json
```

**预装语言**:
- ✅ English (2 个版本)
- ✅ Urdu (2 个版本)
- ✅ Indonesian (1 个版本)

#### B. 在线下载翻译

**API 端点**: 
```
https://apis.dochubai.com/quran/api/translations/available_translations_info.json
https://apis.dochubai.com/quran/{path}
```

**实现文件**: `GithubApi.kt`

**支持的翻译** (可下载):
- 100+ 种语言
- 200+ 个翻译版本

**下载流程**:
```
1. 用户在设置中选择翻译
   ↓
2. 检查本地是否已下载
   ↓
3. 如未下载，从 API 获取
   ↓
4. 保存到应用数据目录
   ↓
5. 加载并显示
```

**缓存位置**: `/data/data/com.quran.quranaudio.online/files/translations/`

**特点**:
- ✅ 支持多语言
- ✅ 动态下载（按需）
- ✅ 本地缓存
- ⚠️ 首次使用需要网络

---

### 3. 古兰经注释（Tafsir）

**数据来源**: 🌐 **在线 API**

**API 端点**:
```
https://api.quran.com/api/qdc/tafsirs/{slug}/by_ayah/{verseKey}
https://api.quran.com/api/v4/resources/tafsirs
```

**实现文件**: `QuranApi.kt`, `TafsirManager.kt`

**支持的注释版本**:
- Tafsir Ibn Kathir (English)
- Tafsir al-Jalalayn (Arabic)
- Tafsir Muyassar (Arabic)
- Al-Tafsir al-Wasit (Arabic)
- 其他 20+ 种注释

**加载方式**:
```kotlin
quranApi.getTafsir(
    slug = "en-tafisr-ibn-kathir",
    verseKey = "1:1"
)
```

**特点**:
- ✅ 实时获取
- ✅ 内容权威
- ✅ 多语言支持
- ❌ 需要网络连接
- ⚠️ 无本地缓存（每次重新请求）

---

### 4. 古兰经元数据

**数据来源**: ✅ **本地 JSON 文件**

**文件位置**: `/app/src/main/assets/quran_meta.json`

**包含信息**:
- 章节名称（阿拉伯语、英语、音译）
- 降示地点（麦加/麦地那）
- 降示顺序
- 经文数量
- 页码范围
- Juz 和 Hizb 划分

**加载方式**: `QuranMeta.prepareInstance()`

**特点**:
- ✅ 完全离线
- ✅ 结构化数据
- ✅ 快速查询

---

## 📿 圣训（Hadith）数据

### 数据来源

**数据来源**: ✅ **本地 JSON 文件**

**文件位置**:
```
/app/src/main/assets/
├── ara-bukhari.min.json    (阿拉伯语)
├── ara-muslim.min.json
├── ara-nasai.min.json
├── ara-abudawud.min.json
├── ara-tirmidhi.min.json
├── ara-ibnmajah.min.json
├── urd-bukhari.min.json    (乌尔都语)
├── urd-muslim.min.json
├── urd-nasai.min.json
├── urd-abudawud.min.json
├── urd-tirmidhi.min.json
├── urd-ibnmajah.min.json
├── ind-bukhari.min.json    (印尼语)
├── ind-muslim.min.json
├── ind-nasai.min.json
├── ind-abudawud.min.json
├── ind-tirmidhi.min.json
└── ind-ibnmajah.min.json
```

**包含的圣训集**:
1. ✅ Sahih al-Bukhari (布哈里圣训集)
2. ✅ Sahih Muslim (穆斯林圣训集)
3. ✅ Sunan an-Nasa'i (奈萨仪圣训集)
4. ✅ Sunan Abu Dawud (艾布·达乌德圣训集)
5. ✅ Jami' at-Tirmidhi (提尔米兹圣训集)
6. ✅ Sunan Ibn Majah (伊本·马杰圣训集)

**支持语言**:
- ✅ 阿拉伯语（原文）
- ✅ 乌尔都语（翻译）
- ✅ 印尼语（翻译）

**数据格式**:
```json
{
  "metadata": {
    "name": "Sahih al Bukhari",
    "sections": {
      "1": "Revelation",
      "2": "Belief",
      ...
    }
  },
  "hadiths": [
    {
      "hadithnumber": "1",
      "text": "阿拉伯语圣训文本...",
      "reference": {
        "book": 1,
        "hadith": "1"
      }
    }
  ]
}
```

**加载方式**: `IOUtils.toString(getAssets().open("ara-bukhari.min.json"))`

**特点**:
- ✅ 完全离线
- ✅ 六大圣训集齐全
- ✅ 多语言支持
- ❌ 文件体积大（总计 ~100+ MB）
- ❌ 更新需要重新发布 APK

**可能的原始数据源**:
- sunnah.com
- hadith-api (fawazahmed0/hadith-api)
- The9Books API

---

## 🙏 祷告时间数据

### 数据来源

**数据来源**: 🧮 **本地计算 + 在线天气 API**

**计算库**: `PrayerTimesCalculator` (基于天文算法)

**输入参数**:
- 📍 地理位置（经纬度）
- 📅 日期
- ⚙️ 计算方法（Muslim World League, Umm al-Qura, 等）
- 🌍 时区

**地理位置来源**:
1. **GPS 定位** (LocationHelper)
2. **用户手动输入** (AutoCompleteTextPreference)
3. **默认位置**: 麦加 (21.4225, 39.8262)

**地址解析 API**:
- **Nominatim** (OpenStreetMap): `https://nominatim.openstreetmap.org/`
- **Photon**: `https://photon.komoot.io/api/`

**特点**:
- ✅ 精确计算
- ✅ 支持 15+ 种计算方法
- ✅ 支持时区自动调整
- ⚠️ 首次需要网络获取地址

---

## 🎵 古兰经朗诵音频

### 数据来源

**数据来源**: 🌐 **在线音频流**

**API 端点**: 通过 `GithubApi` 获取朗诵者信息
```
https://apis.dochubai.com/quran/apis/recitations/available_recitations_info.json
```

**音频 URL 格式** (示例):
```
https://everyayah.com/data/{reciter_id}/{surah:003d}{ayah:003d}.mp3
```

**支持的朗诵者**: 50+ 位
- Abdul Basit
- Mishary Rashid Alafasy
- Saad Al-Ghamdi
- 等等

**播放方式**: 
- `RecitationService.kt`
- 使用 `MediaPlayer` 流式播放

**特点**:
- ✅ 高质量音频
- ✅ 多位朗诵者可选
- ✅ 流式播放（无需完整下载）
- ❌ 需要稳定网络连接

---

## 🌍 直播流媒体

### 麦加和麦地那直播

**数据来源**: 🌐 **在线视频流**

**麦加直播 URL**:
```java
String[] meccaLiveUrls = {
    "http://m.live.net.sa:1935/live/quran/playlist.m3u8",       // HLS主流
    "https://ythls.armelin.one/channel/UCos52-JmjOoBnBOnxJCWAQA.m3u8",  // YouTube转HLS
    "https://www.youtube.com/watch?v=e85tJVzKwDU",              // YouTube备用1
    "https://www.youtube.com/watch?v=yd19lGSibQ4"               // YouTube备用2
};
```

**麦地那直播 URL**:
```java
String[] medinaLiveUrls = {
    "http://m.live.net.sa:1935/live/sunnah/playlist.m3u8",
    "https://ythls.armelin.one/channel/UCJr4gikBowJ8I-iUXs7CkMg.m3u8",
    "https://www.youtube.com/watch?v=4s4XX-qaNgg",
    "https://www.youtube.com/watch?v=0lg0XeJ2gAU",
    "https://www.youtube.com/watch?v=4Ar8JHRCdSE"
};
```

**实现文件**: `HomeFragment.java`

**特点**:
- ✅ 24/7 实时直播
- ✅ 多个备用流
- ✅ 自动切换
- ❌ 需要稳定网络

---

## 📱 应用配置和更新

### 应用更新检测

**数据来源**: 🌐 **GitHub API**

**API 端点**:
```
https://apis.dochubai.com/quran/apis/versions/app_updates.json
https://apis.dochubai.com/quran/apis/versions/resources_versions.json
```

**实现文件**: `GithubApi.kt`

**功能**:
- 检查新版本
- 获取更新日志
- 资源版本管理

---

## 🎯 数据总结表

| 数据类型 | 来源 | 存储方式 | 是否需要网络 | 更新方式 |
|---------|------|---------|------------|---------|
| **古兰经文本** | 本地 JSON | Assets | ❌ 否 | 重新发布 APK |
| **预装翻译** | 本地 JSON | Assets | ❌ 否 | 重新发布 APK |
| **在线翻译** | dochubai.com API | 应用数据目录 | ✅ 首次需要 | 动态下载 |
| **Tafsir 注释** | quran.com API | 无缓存 | ✅ 是 | 实时获取 |
| **Hadith 圣训** | 本地 JSON | Assets | ❌ 否 | 重新发布 APK |
| **祷告时间** | 本地计算 | 内存缓存 | ⚠️ 地址需要 | 实时计算 |
| **朗诵音频** | everyayah.com | 流式播放 | ✅ 是 | 实时流式传输 |
| **直播流** | YouTube/HLS | 流式播放 | ✅ 是 | 实时流式传输 |
| **应用更新** | GitHub API | SharedPreferences | ✅ 是 | 启动时检查 |

---

## 🆕 Firestore 导入方案

### 为什么需要 Firestore？

当前数据存储的**局限性**:
1. ❌ 无法跨设备同步用户数据
2. ❌ 更新内容需要重新发布 APK
3. ❌ 无法支持 LLM 智能学习计划
4. ❌ 无法实现动态内容推送
5. ❌ 查询功能受限

### Firestore 导入后的优势

1. **支持 LLM 学习计划** ✅
   - AI 可以访问完整古兰经数据
   - 基于用户偏好生成个性化学习路径
   - 智能推荐相关经文和主题

2. **跨设备同步** ✅
   - 学习进度云端存储
   - 笔记和书签同步
   - 多设备无缝切换

3. **动态更新** ✅
   - 无需重新发布 APK
   - 翻译和注释实时更新
   - 支持 A/B 测试

4. **高效查询** ✅
   - 按主题查询
   - 按关键词搜索
   - 按 Juz/Page 快速定位

---

## 📦 导入工具

### 已创建的文件

1. **`quran_firestore_importer.py`** - Python 导入脚本
2. **`requirements_importer.txt`** - Python 依赖
3. **`QURAN_FIRESTORE_IMPORT_GUIDE.md`** - 详细使用指南

### 使用步骤

```bash
# 1. 安装依赖
pip3 install -r requirements_importer.txt

# 2. 配置 Firebase 服务账号密钥
# 从 Firebase Console 下载并保存为 serviceAccountKey.json

# 3. 运行导入脚本
python3 quran_firestore_importer.py

# 4. 选择导入方式
# 选项 1: 从 quran.com API 导入（包含翻译）
# 选项 2: 从本地 JSON 导入（仅原文）
```

---

## 🔐 安全性考虑

### API 密钥管理

1. **Firebase 服务账号密钥**:
   - ✅ 已添加到 `.gitignore`
   - ⚠️ 切勿提交到 Git
   - ⚠️ 定期轮换密钥

2. **API 端点**:
   - ✅ quran.com API: 公开免费，无需密钥
   - ✅ dochubai.com API: 公开免费，无需密钥

### 数据隐私

1. **用户数据**:
   - ✅ 存储在用户私有路径 (`/users/{userId}/`)
   - ✅ 安全规则保护（仅用户本人可访问）

2. **公共数据**:
   - ✅ 古兰经文本（公共领域）
   - ✅ 翻译（已获授权）
   - ✅ 只读权限（无法篡改）

---

## 🎯 建议的数据架构演进

### 当前架构（v1.6.9）

```
应用层
  ↓
本地 Assets (古兰经文本、Hadith)
  +
在线 API (翻译、注释、音频)
```

**优点**: 离线可用  
**缺点**: 无法同步，更新困难

### 目标架构（建议）

```
应用层
  ↓
Firestore (古兰经、翻译、注释) + 本地缓存
  +
用户数据同步 (学习进度、笔记、书签)
  +
LLM 学习计划生成
```

**优点**:
- ✅ 跨设备同步
- ✅ 动态更新
- ✅ 支持 AI 功能
- ✅ 离线缓存可用

---

## 📊 数据量估算

### 古兰经数据

| 项目 | 数量 | 大小估算 |
|------|------|---------|
| Surah 文档 | 114 | ~50 KB |
| Ayah 文档 | 6,236 | ~10 MB |
| 翻译 (每种) | 6,236 | ~2-3 MB |
| 总计 (含 4 种翻译) | ~6,350 文档 | ~25 MB |

### Firestore 成本估算（免费配额）

| 项目 | 免费配额 | 预计使用 | 状态 |
|------|---------|---------|------|
| **存储** | 1 GB | ~25 MB | ✅ 足够 |
| **读取** | 50,000/天 | ~1,000/天 | ✅ 足够 |
| **写入** | 20,000/天 | ~100/天 | ✅ 足够 |

**结论**: 完全在免费配额内，无需付费 ✅

---

## 🔄 迁移计划

### 阶段 1: 数据导入（当前）
- [x] 创建导入脚本
- [ ] 执行数据导入
- [ ] 验证数据完整性

### 阶段 2: Android 端适配
- [ ] 创建 `QuranFirestoreRepository`
- [ ] 实现离线缓存机制
- [ ] 集成到现有阅读器

### 阶段 3: LLM 集成
- [ ] 集成 LLM API (OpenAI/Gemini)
- [ ] 实现学习计划生成
- [ ] 创建学习计划 UI

---

## 📚 相关文档

1. **QURAN_FIRESTORE_IMPORT_GUIDE.md** - 导入工具使用指南
2. **DATA_SOURCES_ANALYSIS.md** (本文档) - 数据来源分析
3. **quran_firestore_importer.py** - 导入脚本
4. **firestore.rules** - Firestore 安全规则

---

**分析日期**: 2025-11-02  
**版本**: v1.6.9  
**状态**: ✅ 分析完成，工具已就绪

