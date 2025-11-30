# 📊 多语言古兰经和 Tafsir 数据同步方案

## 目录
- [当前数据库结构分析](#当前数据库结构分析)
- [Quran.com API 资源](#qurancom-api-资源)
- [数据同步策略](#数据同步策略)
- [实施计划](#实施计划)

---

## 当前数据库结构分析

### 1. 古兰经翻译（Translation）数据库

#### 数据库信息
- **数据库名**: `QuranTranslation.db`
- **版本**: 1
- **位置**: `/data/data/com.quran.quranaudio.online/databases/`

#### 表结构

##### 表 1: `QuranTranslationBookInfo` (翻译元数据表)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `slug` | TEXT PRIMARY KEY | 翻译唯一标识符，如 `en_101_sahih-international` |
| `langCode` | TEXT | 语言代码，如 `en`, `bn`, `ms`, `tr` |
| `langName` | TEXT | 语言名称，如 `English`, `Bengali` |
| `bookName` | TEXT | 翻译书名 |
| `authorName` | TEXT | 作者名称 |
| `displayName` | TEXT | 显示名称 |
| `isPremium` | INTEGER | 是否高级版本 (0/1) |
| `lastUpdated` | TEXT | 最后更新时间 |
| `downloadPath` | TEXT | 下载路径（API 地址） |

**合约类**: `QuranTranslInfoContract.java`

##### 表 2: 动态翻译内容表 (每个翻译一个表)

**表名格式**: `` `{slug}` `` (需要转义，如 `` `en_101_sahih-international` ``)

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `_id` | TEXT PRIMARY KEY | 格式 `{chapterNo}:{verseNo}`，如 `1:1` |
| `chapterNo` | INTEGER | 章节号 (1-114) |
| `verseNo` | INTEGER | 经文号 (1-286) |
| `text` | TEXT | 翻译文本 |
| `footnotes` | TEXT | 脚注 (JSON 数组字符串) |

**索引**: 按 `chapterNo ASC, verseNo ASC` 排序

**合约类**: `QuranTranslContract.java`

#### 数据存储逻辑

```kotlin
// QuranTranslDBHelper.kt

// 1. 创建翻译元数据记录
storeTranslationInfo(bookInfo, DB)

// 2. 创建该翻译的专属表
CREATE TABLE IF NOT EXISTS `{slug}` (
    _id TEXT PRIMARY KEY,
    chapterNo INTEGER,
    verseNo INTEGER,
    text TEXT,
    footnotes TEXT
)

// 3. 插入 6236 条经文翻译
insertTranslationQuery(DB, slug, chapterNo, verseNo, text, footnotes)
```

#### 当前预装翻译

| 语言 | Slug | 显示名称 | 位置 |
|------|------|---------|------|
| English | `en_101_sahih-international` | Sahih International | `assets/prebuilt_translations/en_saheeh_v1_1_1/` |
| English | `en_102_the-clear-quran` | The Clear Quran | `assets/prebuilt_translations/en_the_clear_quran/` |
| Urdu | `in_junagarhi` | Junagarhi | `assets/prebuilt_translations/ur_junagarhi/` |
| Indonesian | `in_quran-complex` | Kompleks Al Quran | `assets/prebuilt_translations/in/` |

**总计**: 4 个预装翻译（2个英语 + 1个乌尔都语 + 1个印尼语）

---

### 2. 古兰经注释（Tafsir）存储

#### 当前存储方式

**⚠️ 重要发现：Tafsir 目前不存储在数据库中！**

##### 存储位置
- **类型**: 文件缓存（非数据库）
- **位置**: `/data/data/com.quran.quranaudio.online/files/tafsirs/`
- **格式**: JSON 文件
- **命名**: `{tafsirKey}_{chapterNo}_{verseNo}.json`

##### 工作流程

```kotlin
// ActivityTafsir.kt - loadContent()

1. 检查本地文件缓存
   val tafsirFile = fileUtils.getTafsirFileSingleVerse(tafsirKey, chapterNo, verseNo)
   if (tafsirFile.length() > 0) {
       // 从文件读取
       val tafsir = JsonHelper.json.decodeFromString<TafsirModel>(tafsirFile.readText())
   }

2. 如果缓存不存在，从网络加载
   val tafsir = when {
       slug.startsWith("id-") -> {
           // 印尼语从自定义服务器加载
           RetrofitInstance.customTafsir.getTafsir(slug, "$chapterNo:$verseNo")["tafsir"]!!
       }
       else -> {
           // 其他从 Quran.com 加载
           RetrofitInstance.quran.getTafsir(slug, "$chapterNo:$verseNo")["tafsir"]!!
       }
   }

3. 保存到文件缓存
   tafsirFile.writeText(JsonHelper.json.encodeToString(tafsir))
```

##### Tafsir 元数据

**存储位置**: SharedPreferences 和内存
- **可用 Tafsir 列表**: 从 API 加载后缓存到 `TafsirManager`
- **当前选中的 Tafsir**: SharedPreferences (`key.tafsir`)

##### 当前支持的 Tafsir 语言

根据 `TafsirLanguageMapper.kt`:

| 语言代码 | 推荐 Tafsir Slug | 说明 |
|---------|-----------------|------|
| `en` | `en-tafisr-ibn-kathir` | Tafsir Ibn Kathir (English) |
| `ar` | `ar-tafsir-muyassar` | التفسير الميسر |
| `ur` | `tafsir-bayan-ul-quran` | Bayan ul Quran |
| `bn` | `bn-tafseer-ibn-e-kaseer` | Tafseer Ibn Katheer (Bengali) |
| `ru` | `ru-tafseer-al-saddi` | Tafseer Al-Saddi (Russian) |
| `ku` | `kurd-tafsir-rebar` | Kurdish Tafsir |
| `id` | `id-tafsir-kemenag` | Tafsir Kemenag (Indonesian) ⭐ |

⭐ 印尼语 Tafsir 从自定义服务器加载（已同步 6,236 条到 MySQL）

---

## Quran.com API 资源

### 1. 翻译资源 (Translations)

#### API 端点

```
GET https://api.quran.com/api/v4/resources/translations
GET https://api.quran.com/api/v4/resources/translations?language={lang_code}
```

#### 获取特定翻译的完整经文

```
GET https://api.quran.com/api/v4/quran/translations/{translation_id}
```

**示例**: 
- Bengali (Taisirul Quran): `https://api.quran.com/api/v4/quran/translations/161`
- Malay (Abdullah Basmeih): `https://api.quran.com/api/v4/quran/translations/39`
- Turkish (Diyanet): `https://api.quran.com/api/v4/quran/translations/77`

#### 响应格式

```json
{
  "translations": [
    {
      "id": 161,
      "name": "Taisirul Quran",
      "author_name": "Tawheed Publication",
      "slug": "bn-taisirul-quran",
      "language_name": "bengali",
      "translated_name": {
        "name": "তাইসীরুল কুরআন",
        "language_name": "bengali"
      }
    }
  ]
}
```

#### 可用语言

Quran.com 支持 **100+ 语言**，**200+ 翻译版本**，包括：

| 语言 | 代码 | 可用翻译数 | 示例翻译 ID |
|------|-----|-----------|-----------|
| English | `en` | 20+ | 131, 84, 85 |
| Bengali | `bn` | 5+ | 161, 163, 164 |
| Malay | `ms` | 3+ | 39, 134 |
| Turkish | `tr` | 15+ | 77, 78, 124 |
| Indonesian | `id` | 10+ | 33, 134, 141 |
| Urdu | `ur` | 15+ | 97, 151, 158 |
| Arabic | `ar` | 5+ | 203, 206 |
| Persian | `fa` | 10+ | 135, 136 |
| French | `fr` | 5+ | 31, 136 |
| German | `de` | 8+ | 27, 208 |
| Spanish | `es` | 3+ | 83, 143 |
| Russian | `ru` | 5+ | 45, 79 |
| Chinese | `zh` | 2+ | 56, 109 |

---

### 2. Tafsir 资源 (Commentaries)

#### API 端点

```
GET https://api.quran.com/api/v4/resources/tafsirs
```

#### 获取特定经文的 Tafsir

```
GET https://api.quran.com/api/qdc/tafsirs/{slug}/by_ayah/{verse_key}
```

**示例**: 
```
https://api.quran.com/api/qdc/tafsirs/en-tafisr-ibn-kathir/by_ayah/1:1
```

#### 响应格式

```json
{
  "tafsirs": [
    {
      "id": 169,
      "name": "Tafsir Ibn Kathir",
      "author_name": "Ibn Kathir",
      "slug": "en-tafisr-ibn-kathir",
      "language_name": "english"
    },
    {
      "id": 93,
      "name": "Tafsir Al-Jalalayn",
      "author_name": "Jalal ad-Din al-Mahalli & Jalal ad-Din as-Suyuti",
      "slug": "ar-tafseer-al-jalalayn",
      "language_name": "arabic"
    }
  ]
}
```

#### 可用 Tafsir 数量

Quran.com 提供 **40+ 种 Tafsir**，涵盖：

| 语言 | 可用 Tafsir 数 | 示例 |
|------|--------------|------|
| English | 5+ | Ibn Kathir, Al-Jalalayn (English) |
| Arabic | 10+ | Muyassar, Tabari, Qurtubi |
| Indonesian | 3+ | Tafsir Kementerian Agama |
| Urdu | 5+ | Bayan ul Quran, Tafheem ul Quran |
| Bengali | 2+ | Ibn Kathir (Bengali) |
| Turkish | 3+ | Diyanet İşleri Tafsiri |
| Russian | 2+ | Tafseer Saadi (Russian) |

---

## 数据同步策略

### 策略 1: 翻译（Translation）同步到数据库

#### 目标
将 Quran.com API 的所有翻译版本同步到本地数据库，支持离线访问。

#### 同步范围

**优先级 1（高优先级）**:
- ✅ Bengali (孟加拉语): 5个版本
- ✅ Malay (马来语): 3个版本
- ✅ Turkish (土耳其语): 15个版本
- ✅ Indonesian (印尼语): 补充新版本
- ✅ Urdu (乌尔都语): 补充新版本

**优先级 2（中优先级）**:
- Arabic (阿拉伯语): 5个版本
- Persian (波斯语): 10个版本
- French (法语): 5个版本
- German (德语): 8个版本
- Spanish (西班牙语): 3个版本

**优先级 3（低优先级）**:
- Russian, Chinese, 其他小语种

#### 实施步骤

```python
# sync_translations.py

1. 获取所有可用翻译列表
   GET https://api.quran.com/api/v4/resources/translations

2. 过滤需要同步的翻译（根据优先级）

3. 对于每个翻译:
   a. 从 API 获取完整经文
      GET https://api.quran.com/api/v4/quran/translations/{id}
   
   b. 转换为应用数据格式
      {
        "chapters": [
          {
            "number": 1,
            "verses": [
              {
                "number": 1,
                "text": "...",
                "footnotes": []
              }
            ]
          }
        ]
      }
   
   c. 生成 slug: "{lang_code}_{id}_{name}"
   
   d. 导入到 Android 数据库
      - INSERT INTO QuranTranslationBookInfo
      - CREATE TABLE `{slug}`
      - INSERT 6236 verses

4. 生成元数据文件
   available_translations_info.json
```

#### 数据格式转换

**Quran.com API 格式**:
```json
{
  "translations": [
    {
      "chapter_number": 1,
      "verse_number": 1,
      "text": "...",
      "resource_id": 161
    }
  ]
}
```

**应用数据库格式**:
```json
{
  "chapters": [
    {
      "number": 1,
      "verses": [
        {
          "number": 1,
          "text": "...",
          "footnotes": []
        }
      ]
    }
  ]
}
```

---

### 策略 2: Tafsir（注释）同步到数据库

#### 当前问题
- ❌ Tafsir 存储在文件缓存中，不是数据库
- ❌ 按需加载，首次需要网络
- ❌ 无法批量预加载

#### 改进方案

##### 方案 A: 保持文件缓存（推荐）

**优点**:
- ✅ 不影响现有逻辑
- ✅ 减小数据库体积
- ✅ 灵活性高

**改进**:
1. 预下载常用语言的 Tafsir
2. 后台批量缓存热门经文
3. 智能预加载（根据阅读历史）

**实施**:
```kotlin
// TafsirPreloader.kt (已存在)

fun preloadPopularVerses(context: Context, languageCode: String) {
    // 预加载热门经文（如 Surah 1, 2, 18, 36, 67, 112-114）
    val popularSurahs = listOf(1, 2, 18, 36, 67, 112, 113, 114)
    // 后台下载
}
```

##### 方案 B: 存储到数据库（可选）

**数据库表设计**:

```sql
-- Tafsir 元数据表
CREATE TABLE TafsirMetadata (
    tafsir_key TEXT PRIMARY KEY,
    tafsir_name TEXT NOT NULL,
    author_name TEXT NOT NULL,
    language_code TEXT NOT NULL,
    language_name TEXT NOT NULL,
    slug TEXT NOT NULL,
    source TEXT NOT NULL
);

-- Tafsir 内容表（多语言共享）
CREATE TABLE TafsirContent (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    tafsir_key TEXT NOT NULL,
    chapter_no INTEGER NOT NULL,
    verse_no INTEGER NOT NULL,
    text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(tafsir_key, chapter_no, verse_no),
    FOREIGN KEY (tafsir_key) REFERENCES TafsirMetadata(tafsir_key)
);

CREATE INDEX idx_tafsir_verse 
ON TafsirContent(tafsir_key, chapter_no, verse_no);
```

**数据同步**:
```python
# sync_tafsirs.py

1. 获取所有可用 Tafsir 列表
   GET https://api.quran.com/api/v4/resources/tafsirs

2. 对于每个 Tafsir:
   a. 插入元数据
      INSERT INTO TafsirMetadata
   
   b. 对于 114 章的每节经文:
      GET https://api.quran.com/api/qdc/tafsirs/{slug}/by_ayah/{chapter}:{verse}
      INSERT INTO TafsirContent

3. 估算数据量:
   - 6,236 verses × 40 tafsirs = 249,440 条记录
   - 每条平均 2KB = 约 500MB
   - **不推荐全部同步！**
```

**推荐**:
- 只同步 5-10 个常用 Tafsir
- 优先级: English (Ibn Kathir), Arabic (Muyassar), Indonesian, Urdu, Bengali

---

## 实施计划

### 阶段 1: 数据获取与分析（1-2天）

#### 任务 1.1: 获取 Quran.com API 完整翻译列表
```bash
curl "https://api.quran.com/api/v4/resources/translations" > translations_list.json
```

#### 任务 1.2: 获取 Quran.com API 完整 Tafsir 列表
```bash
curl "https://api.quran.com/api/v4/resources/tafsirs" > tafsirs_list.json
```

#### 任务 1.3: 分析和筛选
- 确定高优先级翻译（15-20个）
- 确定高优先级 Tafsir（5-10个）
- 估算数据量

---

### 阶段 2: 开发同步脚本（2-3天）

#### 任务 2.1: 翻译同步脚本
**文件**: `scripts/sync_translations_from_quran_com.py`

**功能**:
1. 从 Quran.com API 获取翻译
2. 转换为应用数据格式
3. 生成 JSON 文件
4. 批量导入到 SQLite 数据库

#### 任务 2.2: Tafsir 同步脚本
**文件**: `scripts/sync_tafsirs_from_quran_com.py`

**功能**:
1. 从 Quran.com API 获取 Tafsir
2. 选择性下载（文件缓存 或 数据库）
3. 生成元数据文件

#### 任务 2.3: 数据库迁移脚本
**文件**: `scripts/migrate_to_android_db.py`

**功能**:
1. 读取生成的 JSON 文件
2. 创建 Android SQLite 数据库
3. 批量插入数据
4. 生成可部署的 `.db` 文件

---

### 阶段 3: 集成到应用（2-3天）

#### 任务 3.1: 更新 `LocalTranslationData.kt`
添加新同步的翻译版本到本地列表

#### 任务 3.2: 更新 `TranslUtils.java`
添加新翻译的 slug 常量

#### 任务 3.3: 测试现有功能
- ✅ 英语翻译显示正常
- ✅ 印尼语翻译显示正常
- ✅ 乌尔都语翻译显示正常
- ✅ 新增语言翻译显示正常

#### 任务 3.4: 测试 Tafsir 功能
- ✅ 英语 Tafsir 显示正常
- ✅ 印尼语 Tafsir 显示正常
- ✅ 新增语言 Tafsir 显示正常

---

### 阶段 4: 优化与部署（1-2天）

#### 任务 4.1: 数据库优化
- 创建索引
- 优化查询性能
- 压缩数据库体积

#### 任务 4.2: 缓存策略优化
- 智能预加载
- 清理过期缓存
- 限制缓存大小

#### 任务 4.3: 文档更新
- 更新 README
- 更新数据源说明
- 更新 API 文档

---

## 技术约束

### 数据量限制

| 数据类型 | 单个版本大小 | 推荐同步数量 | 总大小估算 |
|---------|------------|------------|-----------|
| Translation | ~2-5 MB | 20-30 个 | 60-150 MB |
| Tafsir (文件缓存) | 按需加载 | 不限 | 按需 |
| Tafsir (数据库) | ~10-20 MB | 5-10 个 | 50-200 MB |

### APK 体积控制

**当前预装**:
- 4个翻译 × 3 MB = 12 MB
- 0个 Tafsir (按需加载)
- **总计**: ~12 MB

**建议策略**:
- 预装翻译: 保持 4-6 个（主流语言）
- 其他翻译: 按需下载
- Tafsir: 全部按需加载（不预装）

---

## 风险评估

### 高风险
- ❌ 数据库体积过大导致 APK 过大
- ❌ 数据格式不兼容导致显示错误
- ❌ 影响现有功能的稳定性

### 中风险
- ⚠️ API 请求限制导致同步失败
- ⚠️ 数据更新频率不明确
- ⚠️ 部分翻译质量参差不齐

### 低风险
- ✅ 用户网络环境差导致下载失败（已有重试机制）
- ✅ 存储空间不足（可清理缓存）

---

## 下一步行动

1. **立即执行**: 获取 Quran.com API 完整数据列表
2. **优先级 1**: 同步孟加拉语、马来语、土耳其语翻译
3. **优先级 2**: 优化 Tafsir 预加载机制
4. **优先级 3**: 添加更多语言支持

---

**文档创建日期**: 2024-11-28  
**最后更新**: 2024-11-28  
**状态**: ✅ 分析完成，待执行

