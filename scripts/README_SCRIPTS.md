# 🌍 多语言翻译同步脚本使用指南

## 📋 概述

本目录包含用于从 Quran.com API 同步多语言翻译并导入到 Android SQLite 数据库的脚本。

### 目标
- 同步 18 个优先级 1 翻译（孟加拉语、马来语、土耳其语、印尼语、乌尔都语）
- 采用按需下载策略（不预装到 APK）
- **不影响现有 4 个预装翻译**

---

## 🔧 脚本列表

### 1. `fetch_quran_api_resources.py` ✅
**状态**: 已完成并执行

**功能**: 从 Quran.com API 获取所有可用翻译和 Tafsir 资源

**输出**:
```
quran_api_data/
├── translations_all.json (126个翻译)
├── translations_priority_1.json (18个 ⭐)
├── translations_priority_2.json (8个)
├── translations_priority_3.json (11个)
├── tafsirs_all.json (20个Tafsir)
├── tafsirs_priority.json (5个)
└── summary_report.json
```

---

### 2. `sync_translations.py` ✅
**状态**: 已开发，待执行

**功能**: 
- 下载优先级 1 的 18 个翻译完整经文
- 转换为应用数据格式
- 验证数据完整性（6236条经文）

**输入**: `quran_api_data/translations_priority_1.json`

**输出**:
```
translation_data/
├── raw/
│   ├── bn_161_taisirul-quran.json
│   ├── ms_39_basmeih.json
│   ├── tr_77_diyanet-isleri.json
│   └── ... (18个文件)
├── converted/
│   ├── bn_161_taisirul-quran.json
│   ├── ms_39_basmeih.json
│   └── ... (18个文件)
└── metadata.json
```

**使用方法**:
```bash
cd /Users/huwei/AndroidStudioProjects/quran0/scripts
python3 sync_translations.py
```

**预计时间**: 10-20 分钟（取决于网络速度）

---

### 3. `import_to_sqlite.py` ✅
**状态**: 已开发，待执行

**功能**:
- 创建 SQLite 数据库
- 为每个翻译创建独立表
- 导入 6236 条经文
- 优化数据库性能

**输入**: `translation_data/converted/*.json`

**输出**: `QuranTranslation_New.db`

**使用方法**:
```bash
cd /Users/huwei/AndroidStudioProjects/quran0/scripts
python3 import_to_sqlite.py
```

**预计时间**: 5-10 分钟

**重要**: 
- ⚠️ 自动跳过现有预装翻译（`en_101_sahih-international`, `en_102_the-clear-quran`, `in_junagarhi`, `in_quran-complex`）
- ✅ 只导入新的 18 个翻译

---

## 🚀 完整执行流程

### 步骤 1: 环境准备

确保已安装 Python 3 和必要的库：
```bash
pip3 install requests
```

### 步骤 2: 获取 API 资源（已完成✅）

```bash
python3 fetch_quran_api_resources.py
```

**结果**: 
- ✅ 获取了 126 个翻译
- ✅ 获取了 20 个 Tafsir
- ✅ 筛选出 18 个优先级 1 翻译

### 步骤 3: 同步翻译数据

```bash
python3 sync_translations.py
```

**预期输出**:
```
[2024-11-28 12:00:00] [INFO] 🚀 开始同步翻译数据...
[2024-11-28 12:00:01] [INFO] 📋 找到 18 个优先级 1 翻译
[2024-11-28 12:00:02] [INFO] ============================================================
[2024-11-28 12:00:02] [INFO] 处理翻译: Taisirul Quran
[2024-11-28 12:00:02] [INFO] ID: 161
[2024-11-28 12:00:02] [INFO] 生成的 Slug: bn_161_taisirul-quran
[2024-11-28 12:00:02] [INFO] ============================================================
[2024-11-28 12:00:02] [INFO] 📥 下载原始数据...
[2024-11-28 12:00:05] [INFO] 💾 原始数据已保存
[2024-11-28 12:00:05] [INFO] 🔄 转换数据格式...
[2024-11-28 12:00:06] [INFO] ✓ 验证数据完整性...
[2024-11-28 12:00:06] [INFO] ✅ Taisirul Quran: Validation passed (6236 verses)
[2024-11-28 12:00:06] [INFO] ✅ 处理完成: Taisirul Quran
...
[2024-11-28 12:15:00] [INFO] ============================================================
[2024-11-28 12:15:00] [INFO] 📊 同步完成统计
[2024-11-28 12:15:00] [INFO] ============================================================
[2024-11-28 12:15:00] [INFO] ✅ 成功: 18
[2024-11-28 12:15:00] [INFO] ❌ 失败: 0
[2024-11-28 12:15:00] [INFO] 🎉 所有翻译同步成功！
```

### 步骤 4: 导入到数据库

```bash
python3 import_to_sqlite.py
```

**预期输出**:
```
[2024-11-28 12:20:00] [INFO] 🚀 开始导入翻译数据到数据库...
[2024-11-28 12:20:01] [INFO] 📋 找到 18 个翻译待导入
[2024-11-28 12:20:02] [INFO] ⚠️ 注意: 以下预装翻译将被跳过（保持原有数据）:
[2024-11-28 12:20:02] [INFO]    - en_101_sahih-international
[2024-11-28 12:20:02] [INFO]    - en_102_the-clear-quran
[2024-11-28 12:20:02] [INFO]    - in_junagarhi
[2024-11-28 12:20:02] [INFO]    - in_quran-complex
[2024-11-28 12:20:03] [INFO] ============================================================
[2024-11-28 12:20:03] [INFO] 导入翻译: Taisirul Quran
[2024-11-28 12:20:03] [INFO] Slug: bn_161_taisirul-quran
[2024-11-28 12:20:03] [INFO] ============================================================
[2024-11-28 12:20:03] [INFO] 📋 创建翻译表...
[2024-11-28 12:20:04] [INFO] 💾 插入元数据...
[2024-11-28 12:20:04] [INFO] 📝 插入经文内容...
[2024-11-28 12:20:30] [INFO] ✅ 已插入 6236 条经文
[2024-11-28 12:20:30] [INFO] ✅ 导入完成: Taisirul Quran
...
[2024-11-28 12:28:00] [INFO] 🔧 优化数据库...
[2024-11-28 12:28:10] [INFO] ✅ 数据库优化完成
[2024-11-28 12:28:10] [INFO] ============================================================
[2024-11-28 12:28:10] [INFO] 📊 数据库信息
[2024-11-28 12:28:10] [INFO] ============================================================
[2024-11-28 12:28:10] [INFO] 总表数: 19 (1个元数据表 + 18个翻译表)
[2024-11-28 12:28:10] [INFO] 翻译元数据: 18 条
[2024-11-28 12:28:10] [INFO] 数据库文件大小: 55.32 MB
[2024-11-28 12:28:10] [INFO] ============================================================
[2024-11-28 12:28:10] [INFO] 🎉 所有翻译导入成功！
```

---

## 📊 生成的数据

### 翻译数据（18个）

#### 孟加拉语 (3个)
- `bn_161_taisirul-quran` - Taisirul Quran
- `bn_163_sheikh-mujibur-rahman` - Sheikh Mujibur Rahman
- `bn_164_muhiuddin-khan` - Muhiuddin Khan

#### 印尼语 (3个)
- `id_134_king-fahad` - King Fahad Quran Complex
- `id_33_indonesian-ministry` - Ministry of Religious Affairs
- `id_141_tafsir-jalalayn` - Tafsir Jalalayn

#### 马来语 (1个)
- `ms_39_basmeih` - Abdullah Muhammad Basmeih

#### 土耳其语 (5个)
- `tr_77_diyanet-isleri` - Diyanet İşleri
- `tr_78_suleyman-ates` - Süleyman Ateş
- `tr_124_elmali-hamdi-yazir` - Elmalılı Hamdi Yazır
- `tr_125_mehmet-okuyan` - Mehmet Okuyan
- `tr_126_omer-nasuhi-bilmen` - Ömer Nasuhi Bilmen

#### 乌尔都语 (6个)
- `ur_234_fatah-muhammad` - Fatah Muhammad Jalandhari
- `ur_54_junagarri` - Maulana Muhammad Junagarhi
- `ur_156_sayyid-qatab` - Fe Zilal al-Qur'an
- `ur_151_tafsir-e-usmani` - Shaykh al-Hind
- `ur_158_bayan-ul-quran` - Bayan-ul-Quran
- `ur_97_al-maududi` - Tafheem e Qur'an

### 数据库结构

```sql
-- 元数据表
QuranTranslationBookInfo (
    slug TEXT PRIMARY KEY,
    langCode TEXT,
    langName TEXT,
    bookName TEXT,
    authorName TEXT,
    displayName TEXT,
    isPremium INTEGER,
    lastUpdated TEXT,
    downloadPath TEXT
)

-- 翻译内容表 (每个翻译一个表)
`bn_161_taisirul-quran` (
    _id TEXT PRIMARY KEY,        -- 格式: "1:1"
    chapterNo INTEGER,           -- 1-114
    verseNo INTEGER,             -- 1-286
    text TEXT,                   -- 翻译文本
    footnotes TEXT               -- JSON array
)
```

---

## ⚠️ 重要注意事项

### 1. 不影响现有预装翻译 ✅

脚本会**自动跳过**以下预装翻译：
- `en_101_sahih-international` (English - Sahih International)
- `en_102_the-clear-quran` (English - The Clear Quran)
- `in_junagarhi` (Urdu - Junagarhi)
- `in_quran-complex` (Indonesian - Kompleks Al Quran)

这些翻译将保持原有数据不变。

### 2. 按需下载策略

- ✅ 新翻译**不预装**到 APK 中
- ✅ 用户在引导页选择后**自动下载**
- ✅ 下载后保存到本地数据库
- ✅ 支持离线访问

### 3. 数据库文件

生成的数据库文件 `QuranTranslation_New.db` 约 **55-60 MB**，包含：
- 18 个新翻译
- 每个翻译 6,236 条经文
- 完整的元数据信息

---

## 🧪 验证和测试

### 验证数据完整性

脚本会自动验证：
- ✅ 每个翻译包含 114 章
- ✅ 每章包含正确数量的经文
- ✅ 总计 6,236 条经文
- ✅ 无空文本

### 手动验证

```sql
-- 连接数据库
sqlite3 QuranTranslation_New.db

-- 查看所有翻译
SELECT slug, bookName, langName FROM QuranTranslationBookInfo;

-- 查看特定翻译的经文数
SELECT COUNT(*) FROM `bn_161_taisirul-quran`;
-- 应该返回: 6236

-- 查看示例经文
SELECT * FROM `bn_161_taisirul-quran` WHERE chapterNo=1 LIMIT 5;
```

---

## 📱 集成到 Android 应用

### 下一步骤

1. **复制数据库文件**
   ```bash
   cp QuranTranslation_New.db ../app/src/main/assets/databases/
   ```

2. **更新 `LocalTranslationData.kt`**
   - 添加新翻译到对应语言列表
   - 设置 `isPrebuilt = false`
   - 设置 `isDownloaded = false`

3. **测试按需下载**
   - 在引导页选择孟加拉语
   - 选择 Taisirul Quran
   - 验证自动下载并保存到数据库

4. **测试现有功能**
   - 验证英语翻译正常显示
   - 验证印尼语翻译正常显示
   - 验证乌尔都语翻译正常显示

---

## 🐛 故障排除

### 问题 1: API 请求失败

**症状**: `HTTPSConnectionPool: Max retries exceeded`

**解决**:
- 检查网络连接
- 使用VPN或代理
- 增加 `RETRY_DELAY` 和 `MAX_RETRIES`

### 问题 2: 数据验证失败

**症状**: `Expected 6236 verses, found XXXX`

**解决**:
- 检查 API 数据是否完整
- 重新下载该翻译
- 查看详细日志

### 问题 3: 数据库导入失败

**症状**: `sqlite3.OperationalError`

**解决**:
- 检查文件权限
- 删除旧数据库文件重试
- 检查磁盘空间

---

## 📞 支持

如有问题，请查看：
1. 详细日志输出
2. `MULTI_LANGUAGE_SYNC_ANALYSIS.md` - 技术分析
3. `MULTI_LANGUAGE_SYNC_IMPLEMENTATION_PLAN.md` - 实施计划
4. `MULTI_LANGUAGE_SYNC_SUMMARY.md` - 完整总结

---

## ✅ 检查清单

执行前检查：
- [ ] Python 3 已安装
- [ ] requests 库已安装
- [ ] 网络连接正常
- [ ] 磁盘空间充足（至少 200MB）

执行后检查：
- [ ] `translation_data/raw/` 包含 18 个文件
- [ ] `translation_data/converted/` 包含 18 个文件
- [ ] `translation_data/metadata.json` 存在
- [ ] `QuranTranslation_New.db` 生成且大小约 55-60 MB
- [ ] 数据库包含 18 个翻译表
- [ ] 每个表包含 6,236 条经文

---

**文档创建日期**: 2024-11-28  
**最后更新**: 2024-11-28  
**状态**: ✅ 脚本已开发，待执行

