# 📋 多语言古兰经和 Tafsir 数据同步实施计划

##目录
1. [数据获取结果](#数据获取结果)
2. [详细实施方案](#详细实施方案)
3. [脚本开发清单](#脚本开发清单)
4. [测试计划](#测试计划)
5. [部署策略](#部署策略)

---

## 数据获取结果

### 📊 Quran.com API 资源统计

| 资源类型 | 总数 | 优先级 1 | 优先级 2 | 优先级 3 |
|---------|------|---------|---------|---------|
| 翻译 (Translations) | **126** | **18** | **8** | **11** |
| 注释 (Tafsirs) | **20** | **5** | - | - |

### 📖 优先级 1 翻译（18个 - 必须同步）

#### 孟加拉语 (Bengali) - 3个
| ID | 名称 | 作者 | Slug |
|----|------|------|------|
| 161 | Taisirul Quran | Tawheed Publication | `bn-taisirul-quran` |
| 163 | Sheikh Mujibur Rahman | Darussalaam Publication | `bn-sheikh-mujibur-rahman` |
| 164 | Muhiuddin Khan | Mobile Apps Ltd | `bn-muhiuddin-khan` |

#### 印尼语 (Indonesian) - 3个
| ID | 名称 | 作者 | Slug |
|----|------|------|------|
| 134 | King Fahad Quran Complex | King Fahad Quran Complex | `id-king-fahad` |
| 33 | Ministry of Religious Affairs | Kementerian Agama RI | `id-indonesian-ministry-of-religious-affairs` |
| 141 | Tafsir Jalalayn | Lajnah Pentashihan | `id-tafsir-jalalayn` |

#### 马来语 (Malay) - 1个
| ID | 名称 | 作者 | Slug |
|----|------|------|------|
| 39 | Abdullah Muhammad Basmeih | Abdullah Muhammad Basmeih | `ms-basmeih` |

#### 土耳其语 (Turkish) - 5个
| ID | 名称 | 作者 | Slug |
|----|------|------|------|
| 77 | Diyanet İşleri | Diyanet İşleri | `tr-diyanet-isleri` |
| 78 | Süleyman Ateş | Süleyman Ateş | `tr-suleyman-ates` |
| 124 | Elmalılı Hamdi Yazır | Elmalılı Hamdi Yazır | `tr-elmali-hamdi-yazir` |
| 125 | Mehmet Okuyan | Mehmet Okuyan | `tr-mehmet-okuyan` |
| 126 | Ömer Nasuhi Bilmen | Ömer Nasuhi Bilmen | `tr-omer-nasuhi-bilmen` |

#### 乌尔都语 (Urdu) - 6个
| ID | 名称 | 作者 | Slug |
|----|------|------|------|
| 234 | Fatah Muhammad Jalandhari | Fatah Muhammad Jalandhari | `ur-fatah-muhammad-jalandhari` |
| 54 | Maulana Muhammad Junagarhi | Maulana Muhammad Junagarhi | `ur-junagarri` |
| 156 | Fe Zilal al-Qur'an | Sayyid Ibrahim Qutb | `urdu-sayyid-qatab` |
| 151 | Shaykh al-Hind Mahmud al-Hasan | Shaykh al-Hind | `tafsir-e-usmani` |
| 158 | Bayan-ul-Quran | Dr. Israr Ahmad | `bayan-ul-quran` |
| 97 | Tafheem e Qur'an | Syed Abu Ali Maududi | `ur-al-maududi` |

---

### 📝 高优先级 Tafsir（5个）

| ID | 名称 | 语言 | Slug | 作者 |
|----|------|------|------|------|
| 169 | Ibn Kathir (Abridged) | English | `en-tafisr-ibn-kathir` | Hafiz Ibn Kathir |
| 16 | Tafsir Muyassar | Arabic | `ar-tafsir-muyassar` | المیسر |
| 164 | Tafseer ibn Kathir | Bengali | `bn-tafseer-ibn-e-kaseer` | Tawheed Publication |
| 90 | Al-Qurtubi | Arabic | `ar-tafseer-al-qurtubi` | Qurtubi |
| 159 | Bayan ul Quran | Urdu | `tafsir-bayan-ul-quran` | Dr. Israr Ahmad |

**⚠️ 注意**: 印尼语 Tafsir (`id-tafsir-kemenag`) 已在自定义服务器，不在 Quran.com API 中。

---

## 详细实施方案

### 方案 A: 翻译（Translation）同步

#### 目标
将优先级 1 的 18 个翻译同步到本地数据库，支持离线访问。

#### 实施步骤

##### 步骤 1: 下载完整翻译数据

```python
# sync_translations.py

for translation in priority_1_translations:
    translation_id = translation['id']
    slug = translation['slug']
    
    # 从 Quran.com API 获取完整经文
    url = f"https://api.quran.com/api/v4/quran/translations/{translation_id}"
    response = requests.get(url)
    data = response.json()
    
    # 保存原始 JSON
    save_to_file(f"raw_data/{slug}.json", data)
```

##### 步骤 2: 转换数据格式

**Quran.com API 格式**:
```json
{
  "translations": [
    {
      "chapter_number": 1,
      "verse_number": 1,
      "verse_key": "1:1",
      "text": "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
      "resource_id": 131
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
          "text": "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
          "footnotes": []
        }
      ]
    }
  ]
}
```

##### 步骤 3: 生成 Slug

```python
def generate_app_slug(translation):
    lang_code = translation['language_name'][:2]  # 前两个字母
    trans_id = translation['id']
    name_slug = slugify(translation['name'])
    
    return f"{lang_code}_{trans_id}_{name_slug}"
```

**示例**:
- Bengali Taisirul: `bn_161_taisirul-quran`
- Turkish Diyanet: `tr_77_diyanet-isleri`
- Malay Basmeih: `ms_39_basmeih`

##### 步骤 4: 导入到 SQLite 数据库

```python
# 1. 创建翻译元数据
INSERT INTO QuranTranslationBookInfo (
    slug, langCode, langName, bookName, 
    authorName, displayName, downloadPath
) VALUES (
    'bn_161_taisirul-quran', 'bn', 'Bengali',
    'Taisirul Quran', 'Tawheed Publication',
    'Taisirul Quran', 'https://api.quran.com/api/v4/quran/translations/161'
);

# 2. 创建翻译内容表
CREATE TABLE IF NOT EXISTS `bn_161_taisirul-quran` (
    _id TEXT PRIMARY KEY,
    chapterNo INTEGER,
    verseNo INTEGER,
    text TEXT,
    footnotes TEXT
);

# 3. 批量插入 6236 条经文
INSERT INTO `bn_161_taisirul-quran` 
(_id, chapterNo, verseNo, text, footnotes)
VALUES ('1:1', 1, 1, '...', '[]');
```

#### 数据量估算

| 语言 | 翻译数 | 每个大小 | 总大小 |
|------|--------|---------|--------|
| Bengali | 3 | ~3 MB | 9 MB |
| Indonesian | 3 | ~3 MB | 9 MB |
| Malay | 1 | ~3 MB | 3 MB |
| Turkish | 5 | ~3 MB | 15 MB |
| Urdu | 6 | ~4 MB | 24 MB |
| **总计** | **18** | - | **60 MB** |

---

### 方案 B: Tafsir（注释）同步策略

#### 现状分析

当前 Tafsir 存储方式：
- ✅ **文件缓存**: 每个经文的 Tafsir 单独存储为 JSON 文件
- ✅ **按需加载**: 用户查看时才下载
- ✅ **灵活**: 不占用数据库空间

#### 推荐策略: 优化文件缓存

**不建议同步到数据库的原因**:
1. 数据量巨大: 6,236 verses × 20 tafsirs = 124,720 条记录
2. 平均每条 2-5 KB = 约 250-600 MB
3. 很多 Tafsir 用户可能永远不会使用
4. 现有文件缓存机制已经很高效

**优化方案**:

##### 1. 智能预加载（已实现）

```kotlin
// TafsirPreloader.kt

// 预加载常用章节的 Tafsir
val popularSurahs = listOf(1, 2, 18, 36, 55, 67, 112, 113, 114)

fun preloadPopularTafsirs(context: Context) {
    val tafsirKey = SPReader.getSavedTafsirKey(context)
    
    CoroutineScope(Dispatchers.IO).launch {
        for (surahId in popularSurahs) {
            // 后台下载并缓存
            downloadSurahTafsir(tafsirKey, surahId)
        }
    }
}
```

##### 2. 增强元数据管理

创建 Tafsir 元数据数据库（轻量级）:

```sql
CREATE TABLE TafsirMetadata (
    tafsir_key TEXT PRIMARY KEY,
    tafsir_name TEXT NOT NULL,
    author_name TEXT NOT NULL,
    language_code TEXT NOT NULL,
    language_name TEXT NOT NULL,
    slug TEXT NOT NULL,
    api_id INTEGER,
    source TEXT NOT NULL,  -- 'quran_com' or 'custom'
    total_cached_verses INTEGER DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 示例数据
INSERT INTO TafsirMetadata VALUES (
    'bn-tafseer-ibn-e-kaseer',
    'Tafseer ibn Kathir',
    'Tawheed Publication',
    'bn',
    'bengali',
    'bn-tafseer-ibn-e-kaseer',
    164,
    'quran_com',
    0,
    CURRENT_TIMESTAMP
);
```

##### 3. 缓存管理策略

```kotlin
// TafsirCacheManager.kt

fun getCacheStats(): CacheStats {
    val cacheDir = fileUtils.tafsirCacheDir
    val totalFiles = cacheDir.listFiles()?.size ?: 0
    val totalSize = cacheDir.walkTopDown()
        .filter { it.isFile }
        .map { it.length() }
        .sum()
    
    return CacheStats(totalFiles, totalSize)
}

fun clearOldCache(olderThanDays: Int = 30) {
    // 清理 30 天未访问的缓存
}

fun clearAllCache() {
    fileUtils.tafsirCacheDir.deleteRecursively()
}
```

---

## 脚本开发清单

### 脚本 1: `sync_translations.py`

**功能**: 从 Quran.com API 下载翻译并转换为应用格式

**输入**: `quran_api_data/translations_priority_1.json`

**输出**: 
- `translation_data/raw/{slug}.json` - 原始 API 数据
- `translation_data/converted/{slug}.json` - 转换后的数据
- `translation_data/metadata.json` - 元数据汇总

**关键功能**:
```python
def download_translation(translation_id, slug):
    """下载单个翻译的完整经文"""
    
def convert_api_format_to_app_format(api_data):
    """转换数据格式"""
    
def validate_translation_data(data):
    """验证数据完整性（确保6236条经文）"""
    
def generate_metadata(translations):
    """生成元数据文件"""
```

---

### 脚本 2: `import_to_sqlite.py`

**功能**: 将转换后的翻译数据导入 Android SQLite 数据库

**输入**: `translation_data/converted/*.json`

**输出**: `QuranTranslation.db`

**关键功能**:
```python
def create_database():
    """创建数据库和必要的表"""
    
def import_translation(db, slug, data, metadata):
    """导入单个翻译"""
    # 1. INSERT INTO QuranTranslationBookInfo
    # 2. CREATE TABLE `{slug}`
    # 3. INSERT 6236 verses
    
def optimize_database(db):
    """优化数据库（创建索引、VACUUM）"""
```

---

### 脚本 3: `sync_tafsir_metadata.py`

**功能**: 同步 Tafsir 元数据到轻量级数据库

**输入**: `quran_api_data/tafsirs_priority.json`

**输出**: `TafsirMetadata.db`

**关键功能**:
```python
def create_tafsir_metadata_db():
    """创建 Tafsir 元数据数据库"""
    
def import_tafsir_metadata(tafsirs):
    """导入元数据"""
    
def sync_with_custom_tafsirs():
    """同步自定义服务器的 Tafsir（印尼语等）"""
```

---

### 脚本 4: `update_local_translation_data.py`

**功能**: 自动更新 `LocalTranslationData.kt` 文件

**输入**: `translation_data/metadata.json`

**输出**: 更新的 `LocalTranslationData.kt`

**关键功能**:
```python
def generate_bengali_versions(translations):
    """生成孟加拉语版本列表"""
    
def generate_malay_versions(translations):
    """生成马来语版本列表"""
    
def update_kotlin_file(file_path, generated_code):
    """更新 Kotlin 文件"""
```

---

## 测试计划

### 阶段 1: 数据完整性测试

#### 测试 1.1: 翻译数据完整性
```python
def test_translation_completeness(slug):
    """验证翻译包含完整的 6236 条经文"""
    db = sqlite3.connect("QuranTranslation.db")
    cursor = db.cursor()
    
    cursor.execute(f"SELECT COUNT(*) FROM `{slug}`")
    count = cursor.fetchone()[0]
    
    assert count == 6236, f"Expected 6236 verses, found {count}"
```

#### 测试 1.2: 章节和经文号验证
```python
def test_verse_keys(slug):
    """验证所有经文的章节号和经文号正确"""
    # 预期: 1:1 到 114:6
    expected_verses = generate_expected_verse_keys()
    
    actual_verses = fetch_verse_keys_from_db(slug)
    
    assert set(actual_verses) == set(expected_verses)
```

---

### 阶段 2: 功能测试

#### 测试 2.1: 孟加拉语翻译显示

**测试步骤**:
1. 在引导页选择 Bengali
2. 选择 Taisirul Quran 翻译
3. 进入古兰经列表页
4. 打开 Surah 1（Al-Fatihah）
5. 验证显示孟加拉语翻译文本

**预期结果**:
```
✅ UI 语言为孟加拉语
✅ 显示孟加拉语翻译
✅ 翻译文本不为空
✅ 不显示错误或警告
```

#### 测试 2.2: 现有语言不受影响

**测试语言**: English, Indonesian, Urdu

**测试步骤**:
1. 切换到测试语言
2. 打开古兰经阅读器
3. 验证翻译显示正常

**预期结果**:
```
✅ 英语 (Sahih International) 显示正常
✅ 印尼语 (Kompleks Al Quran) 显示正常
✅ 乌尔都语 (Junagarhi) 显示正常
```

#### 测试 2.3: Tafsir 功能测试

**测试步骤**:
1. 选择孟加拉语
2. 打开古兰经 1:1
3. 点击 Tafsir 按钮
4. 验证显示孟加拉语 Tafsir

**预期结果**:
```
✅ Tafsir 自动选择为 Bengali Ibn Kathir
✅ Tafsir 文本正确显示
✅ 可以切换到其他语言的 Tafsir
```

---

### 阶段 3: 性能测试

#### 测试 3.1: 数据库查询性能

```kotlin
fun testTranslationQueryPerformance() {
    val startTime = System.currentTimeMillis()
    
    // 查询整个 Surah
    val translations = quranTranslationFactory.getTranslations(
        translSlugs = setOf("bn_161_taisirul-quran"),
        chapterNo = 2  // Al-Baqarah (286 verses)
    )
    
    val duration = System.currentTimeMillis() - startTime
    
    // 应该在 100ms 内完成
    assert(duration < 100) { "Query took ${duration}ms, expected < 100ms" }
}
```

#### 测试 3.2: 应用启动时间

验证添加新翻译后，应用启动时间增加不超过 200ms。

#### 测试 3.3: APK 体积

```bash
# 对比 APK 体积
du -sh app/build/outputs/apk/release/app-release.apk

# 预期增加: 60-80 MB（如果全部打包）
# 推荐: 不预装，保持原体积
```

---

### 阶段 4: 兼容性测试

#### 测试 4.1: 数据库版本兼容

测试从旧版本数据库升级到新版本。

#### 测试 4.2: 数据迁移

测试用户从旧版本（文件存储）迁移到新版本（数据库存储）。

---

## 部署策略

### 策略 A: 渐进式部署（推荐）

#### 第 1 阶段: 内部测试
- 部署 18 个优先级 1 翻译
- 内部测试 2-3 天
- 修复发现的问题

#### 第 2 阶段: Beta 测试
- 发布 Beta 版本给 100-1000 用户
- 收集反馈和崩溃报告
- 监控性能指标

#### 第 3 阶段: 正式发布
- 逐步推送给所有用户（10% → 50% → 100%）
- 持续监控
- 准备回滚方案

---

### 策略 B: 按需下载（推荐）

**不预装翻译，按需下载**:

优点:
- ✅ APK 体积小
- ✅ 用户只下载需要的翻译
- ✅ 更新翻译更容易

实施:
```kotlin
// DownloadManager.kt

fun downloadTranslationOnDemand(slug: String) {
    // 1. 检查是否已下载
    if (isTranslationDownloaded(slug)) return
    
    // 2. 从 Quran.com API 下载
    val data = fetchTranslationFromApi(slug)
    
    // 3. 保存到数据库
    quranTranslDBHelper.storeTranslation(bookInfo, data)
    
    // 4. 标记为已下载
    markAsDownloaded(slug)
}
```

---

### 策略 C: 混合模式

**预装常用语言 + 按需下载其他**:

预装:
- English (Sahih International) - 3 MB
- Indonesian - 3 MB
- Urdu - 4 MB
- **总计**: 10 MB

按需下载:
- Bengali, Malay, Turkish, 其他

---

## 风险缓解

### 风险 1: 数据损坏

**缓解措施**:
- 下载后验证数据完整性（SHA256）
- 导入前验证经文数量（6236）
- 保留备份数据

### 风险 2: API 变更

**缓解措施**:
- 版本化 API 调用
- 缓存 API 响应
- 准备本地备份数据

### 风险 3: 存储空间不足

**缓解措施**:
- 检查可用空间后再下载
- 提供清理缓存功能
- 压缩数据库

---

## 时间估算

| 任务 | 估算时间 |
|------|---------|
| 开发同步脚本 | 1-2 天 |
| 测试脚本 | 0.5 天 |
| 运行同步（18个翻译） | 2-4 小时 |
| 集成到应用 | 1 天 |
| 功能测试 | 1-2 天 |
| 性能优化 | 0.5-1 天 |
| 文档更新 | 0.5 天 |
| **总计** | **5-7 天** |

---

## 下一步行动

### 立即执行

1. ✅ 数据获取完成
2. ⏭️ 开发 `sync_translations.py`
3. ⏭️ 开发 `import_to_sqlite.py`
4. ⏭️ 测试数据完整性
5. ⏭️ 集成到应用

### 可选优化

- 开发 Tafsir 元数据管理
- 实现智能预加载
- 添加缓存管理界面

---

**文档创建日期**: 2024-11-28  
**最后更新**: 2024-11-28  
**状态**: ✅ 方案确定，准备实施

