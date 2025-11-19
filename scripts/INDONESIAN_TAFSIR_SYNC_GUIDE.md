# 印尼语 Tafsir 数据同步指南

## 📋 概述

本指南说明如何从 EQuran.id API 获取印尼语 Tafsir（古兰经注释）数据，并同步到服务端供客户端使用。

---

## 🎯 目标

- ✅ 获取印尼官方宗教事务部 (Kemenag) 版本的 Tafsir 数据
- ✅ 同步所有 114 个 Surah 的完整注释
- ✅ 标准化数据格式，便于存储和使用
- ✅ 供客户端根据 Surah 和 Ayat 查询使用

---

## 📊 数据源信息

### API 基础信息

| 项目 | 信息 |
|------|------|
| API 名称 | EQuran.id API v2.0 |
| 根 URL | `https://equran.id/api/v2` |
| 数据版本 | Kemenag (印尼宗教事务部) |
| 语言 | 印尼语 (Bahasa Indonesia) |
| 章节数量 | 114 Surahs |
| 访问限制 | 公共 API，无需 Token |

### API 端点

#### 1. 获取所有 Tafsir 列表

```
GET https://equran.id/api/v2/tafsir
```

**响应示例：**
```json
{
  "code": 200,
  "status": "ok",
  "data": [
    {
      "nomor": 1,
      "nama": "الفاتحة",
      "namaLatin": "Al Fatihah",
      "jumlahAyat": 7,
      ...
    },
    ...
  ]
}
```

#### 2. 获取特定 Surah 的 Tafsir

```
GET https://equran.id/api/v2/tafsir/{surah_number}
```

**参数：**
- `surah_number`: 1-114

**响应示例：**
```json
{
  "code": 200,
  "status": "ok",
  "data": {
    "nomor": 1,
    "nama": "الفاتحة",
    "namaLatin": "Al Fatihah",
    "jumlahAyat": 7,
    "tafsir": [
      {
        "ayat": 1,
        "teks": "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ\n\n(Dengan nama Allah) ..."
      },
      ...
    ]
  }
}
```

---

## 🛠️ 使用同步脚本

### 前置条件

**Python 环境：**
```bash
python --version  # 需要 Python 3.6+
```

**安装依赖：**
```bash
pip install requests
```

### 脚本位置

```
scripts/sync_indonesian_tafsir.py
```

### 使用方法

#### 方法 1：验证 API 访问（推荐先执行）

```bash
cd /Users/huwei/AndroidStudioProjects/quran0/scripts
python sync_indonesian_tafsir.py --verify
```

**输出示例：**
```
[2025-11-18 18:30:00] [INFO] ════════════════════════════════════════════════════════════
[2025-11-18 18:30:00] [INFO] 验证 API 访问
[2025-11-18 18:30:00] [INFO] ════════════════════════════════════════════════════════════
[2025-11-18 18:30:00] [INFO] 测试 API 端点: /api/v2/tafsir/1
[2025-11-18 18:30:01] [INFO] 正在获取 Surah 1 的 Tafsir...
[2025-11-18 18:30:02] [INFO] ✅ 成功获取 Surah 1 (7 条注释)
[2025-11-18 18:30:02] [INFO] ✅ API 访问正常
[2025-11-18 18:30:02] [INFO] 响应代码: 200
[2025-11-18 18:30:02] [INFO] 响应状态: ok
[2025-11-18 18:30:02] [INFO] 
[2025-11-18 18:30:02] [INFO] Surah 信息:
[2025-11-18 18:30:02] [INFO]   - 编号: 1
[2025-11-18 18:30:02] [INFO]   - 名称: الفاتحة
[2025-11-18 18:30:02] [INFO]   - 拉丁名: Al Fatihah
[2025-11-18 18:30:02] [INFO]   - 含义: Pembukaan
[2025-11-18 18:30:02] [INFO]   - 总计 Ayat: 7
[2025-11-18 18:30:02] [INFO]   - Tafsir 注释数量: 7
```

---

#### 方法 2：执行完整同步

```bash
cd /Users/huwei/AndroidStudioProjects/quran0/scripts
python sync_indonesian_tafsir.py
```

**或指定输出目录：**
```bash
python sync_indonesian_tafsir.py --output /path/to/output
```

**同步过程：**
```
[2025-11-18 18:35:00] [INFO] ════════════════════════════════════════════════════════════
[2025-11-18 18:35:00] [INFO] 开始同步印尼语 Tafsir 数据
[2025-11-18 18:35:00] [INFO] ════════════════════════════════════════════════════════════

[2025-11-18 18:35:00] [INFO] 📋 步骤 1: 获取 Tafsir 列表
[2025-11-18 18:35:01] [INFO] ✅ 成功获取 Tafsir 列表

[2025-11-18 18:35:01] [INFO] 📥 步骤 2: 开始下载所有 Surah 的 Tafsir (共 114 个)

[2025-11-18 18:35:01] [INFO] [1/114] 处理 Surah 1...
[2025-11-18 18:35:02] [INFO] ✅ 成功获取 Surah 1 (7 条注释)
[2025-11-18 18:35:02] [INFO] 保存文件: tafsir_data/indonesian/surah_001_tafsir.json

[2025-11-18 18:35:03] [INFO] [2/114] 处理 Surah 2...
...

[2025-11-18 18:50:00] [INFO] 💾 步骤 3: 保存汇总数据
[2025-11-18 18:50:01] [INFO] 保存文件: tafsir_data/indonesian/all_tafsir_data.json

[2025-11-18 18:50:01] [INFO] ════════════════════════════════════════════════════════════
[2025-11-18 18:50:01] [INFO] 同步完成！统计报告:
[2025-11-18 18:50:01] [INFO] ════════════════════════════════════════════════════════════
[2025-11-18 18:50:01] [INFO] ✅ 成功: 114/114
[2025-11-18 18:50:01] [INFO] ❌ 失败: 0/114
[2025-11-18 18:50:01] [INFO] 📁 输出目录: /Users/huwei/AndroidStudioProjects/quran0/scripts/tafsir_data/indonesian
```

---

### 输出文件结构

```
tafsir_data/indonesian/
├── tafsir_list.json                    # 所有 Surah 的概览列表
├── all_tafsir_data.json                # 所有 Tafsir 的汇总数据
├── surah_001_tafsir.json               # Surah 1 的 Tafsir
├── surah_002_tafsir.json               # Surah 2 的 Tafsir
├── ...
└── surah_114_tafsir.json               # Surah 114 的 Tafsir
```

---

### 单个 Surah 文件格式

**文件名：** `surah_001_tafsir.json`

**内容结构：**
```json
{
  "surah_id": 1,
  "surah_name": "الفاتحة",
  "surah_name_latin": "Al Fatihah",
  "surah_name_translation": "Pembukaan",
  "total_ayat": 7,
  "language": "id",
  "source": "Kemenag",
  "tafsir": [
    {
      "ayat_id": 1,
      "text": "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ\n\n(Dengan nama Allah) Aku membaca dengan menyebut nama Allah..."
    },
    {
      "ayat_id": 2,
      "text": "اَلْحَمْدُ لِلّٰهِ رَبِّ الْعٰلَمِيْنَۙ\n\n(Segala puji) lafal الحمد..."
    },
    ...
  ]
}
```

---

## 📊 数据库 Schema 设计

### 方案 1：单表设计（推荐）

**表名：** `tafsir_indonesian`

| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| `id` | INTEGER | 主键，自增 | PRIMARY |
| `surah_id` | INTEGER | Surah 编号 (1-114) | INDEX |
| `ayat_id` | INTEGER | Ayat 编号 | INDEX |
| `language` | VARCHAR(10) | 语言代码 ("id") | - |
| `source` | VARCHAR(50) | 数据来源 ("Kemenag") | - |
| `text` | TEXT | Tafsir 注释内容 | - |
| `created_at` | TIMESTAMP | 创建时间 | - |
| `updated_at` | TIMESTAMP | 更新时间 | - |

**复合索引：**
```sql
CREATE INDEX idx_surah_ayat ON tafsir_indonesian(surah_id, ayat_id);
```

**SQL 创建语句：**
```sql
CREATE TABLE tafsir_indonesian (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_id INTEGER NOT NULL,
    ayat_id INTEGER NOT NULL,
    language VARCHAR(10) NOT NULL DEFAULT 'id',
    source VARCHAR(50) NOT NULL DEFAULT 'Kemenag',
    text TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(surah_id, ayat_id, language)
);

CREATE INDEX idx_surah_ayat ON tafsir_indonesian(surah_id, ayat_id);
```

---

### 方案 2：多表设计（可扩展）

#### 表 1：Surah 元数据

**表名：** `tafsir_surah_metadata`

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `surah_id` | INTEGER | Surah 编号 (PRIMARY) |
| `surah_name` | VARCHAR(100) | 阿拉伯语名称 |
| `surah_name_latin` | VARCHAR(100) | 拉丁名称 |
| `surah_name_translation` | VARCHAR(200) | 翻译名称 |
| `total_ayat` | INTEGER | 总 Ayat 数量 |

#### 表 2：Tafsir 内容

**表名：** `tafsir_content`

| 字段名 | 类型 | 说明 | 索引 |
|--------|------|------|------|
| `id` | INTEGER | 主键 | PRIMARY |
| `surah_id` | INTEGER | Surah 编号 | FOREIGN KEY, INDEX |
| `ayat_id` | INTEGER | Ayat 编号 | INDEX |
| `language` | VARCHAR(10) | 语言代码 | INDEX |
| `source` | VARCHAR(50) | 数据来源 | - |
| `text` | TEXT | Tafsir 内容 | - |

---

## 🔄 数据导入到数据库

### Python 脚本示例（SQLite）

创建文件：`import_tafsir_to_db.py`

```python
#!/usr/bin/env python3
import sqlite3
import json
import os
from pathlib import Path

# 配置
DB_PATH = "tafsir_database.db"
JSON_DIR = "tafsir_data/indonesian"

def create_database():
    """创建数据库和表"""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # 创建表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tafsir_indonesian (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            surah_id INTEGER NOT NULL,
            ayat_id INTEGER NOT NULL,
            language VARCHAR(10) NOT NULL DEFAULT 'id',
            source VARCHAR(50) NOT NULL DEFAULT 'Kemenag',
            text TEXT NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            
            UNIQUE(surah_id, ayat_id, language)
        )
    ''')
    
    # 创建索引
    cursor.execute('''
        CREATE INDEX IF NOT EXISTS idx_surah_ayat 
        ON tafsir_indonesian(surah_id, ayat_id)
    ''')
    
    conn.commit()
    conn.close()
    print("✅ 数据库和表创建完成")

def import_tafsir_data():
    """导入 Tafsir 数据到数据库"""
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # 遍历所有 JSON 文件
    json_files = sorted(Path(JSON_DIR).glob("surah_*_tafsir.json"))
    
    total_records = 0
    
    for json_file in json_files:
        with open(json_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        surah_id = data["surah_id"]
        language = data["language"]
        source = data["source"]
        
        for tafsir_item in data["tafsir"]:
            ayat_id = tafsir_item["ayat_id"]
            text = tafsir_item["text"]
            
            try:
                cursor.execute('''
                    INSERT OR REPLACE INTO tafsir_indonesian 
                    (surah_id, ayat_id, language, source, text)
                    VALUES (?, ?, ?, ?, ?)
                ''', (surah_id, ayat_id, language, source, text))
                
                total_records += 1
            except sqlite3.Error as e:
                print(f"❌ 导入失败 Surah {surah_id}, Ayat {ayat_id}: {e}")
        
        print(f"✅ 导入 Surah {surah_id} ({len(data['tafsir'])} 条注释)")
    
    conn.commit()
    conn.close()
    
    print(f"\n🎉 导入完成！共导入 {total_records} 条记录")

if __name__ == "__main__":
    create_database()
    import_tafsir_data()
```

**运行：**
```bash
python import_tafsir_to_db.py
```

---

## 🌐 服务端 API 设计建议

### REST API 端点

#### 1. 获取特定 Ayat 的 Tafsir

```
GET /api/tafsir/{surah_id}/{ayat_id}?lang=id
```

**参数：**
- `surah_id`: Surah 编号 (1-114)
- `ayat_id`: Ayat 编号
- `lang`: 语言代码（默认：`id`）

**响应：**
```json
{
  "success": true,
  "data": {
    "surah_id": 1,
    "ayat_id": 1,
    "language": "id",
    "source": "Kemenag",
    "text": "بِسْمِ اللّٰهِ الرَّحْمٰنِ الرَّحِيْمِ\n\n(Dengan nama Allah) ..."
  }
}
```

---

#### 2. 获取整个 Surah 的 Tafsir

```
GET /api/tafsir/{surah_id}?lang=id
```

**响应：**
```json
{
  "success": true,
  "data": {
    "surah_id": 1,
    "surah_name": "Al Fatihah",
    "total_ayat": 7,
    "language": "id",
    "source": "Kemenag",
    "tafsir": [
      {
        "ayat_id": 1,
        "text": "..."
      },
      ...
    ]
  }
}
```

---

#### 3. 批量下载（供客户端离线使用）

```
GET /api/tafsir/download?lang=id&format=json
```

**响应：** ZIP 文件，包含所有 Surah 的 Tafsir JSON 文件

---

## 📱 Android 客户端集成建议

### 1. 下载和缓存策略

**首次安装：**
- 不预装 Tafsir 数据（减小 APK 体积）
- 用户订阅后，从服务器下载印尼语 Tafsir 包

**下载方式：**
```kotlin
// 下载整个印尼语 Tafsir 包
fun downloadIndonesianTafsir(onProgress: (Int) -> Unit, onComplete: () -> Unit) {
    val url = "$API_BASE_URL/api/tafsir/download?lang=id&format=json"
    
    // 下载 ZIP 文件
    downloadFile(url, localPath) { progress ->
        onProgress(progress)
    }
    
    // 解压到本地数据库
    extractAndImportToDb(localPath)
    
    onComplete()
}
```

---

### 2. 本地数据库结构

**使用 Room Database：**

```kotlin
@Entity(
    tableName = "tafsir_indonesian",
    indices = [Index(value = ["surah_id", "ayat_id"], unique = true)]
)
data class TafsirEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    @ColumnInfo(name = "surah_id")
    val surahId: Int,
    
    @ColumnInfo(name = "ayat_id")
    val ayatId: Int,
    
    @ColumnInfo(name = "language")
    val language: String = "id",
    
    @ColumnInfo(name = "source")
    val source: String = "Kemenag",
    
    @ColumnInfo(name = "text")
    val text: String,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
```

---

### 3. 查询 Tafsir

```kotlin
@Dao
interface TafsirDao {
    @Query("SELECT * FROM tafsir_indonesian WHERE surah_id = :surahId AND ayat_id = :ayatId AND language = :language")
    suspend fun getTafsir(surahId: Int, ayatId: Int, language: String = "id"): TafsirEntity?
    
    @Query("SELECT * FROM tafsir_indonesian WHERE surah_id = :surahId AND language = :language")
    suspend fun getSurahTafsir(surahId: Int, language: String = "id"): List<TafsirEntity>
}
```

**使用示例：**
```kotlin
// 获取特定 Ayat 的 Tafsir
val tafsir = tafsirDao.getTafsir(surahId = 1, ayatId = 1)
textView.text = tafsir?.text ?: "无注释"

// 获取整个 Surah 的 Tafsir
val surahTafsir = tafsirDao.getSurahTafsir(surahId = 1)
```

---

## 🔄 更新策略

### 数据更新频率

| 类型 | 频率 | 原因 |
|------|------|------|
| **Tafsir 内容** | 每年或按需 | 注释内容极少变化 |
| **数据结构** | 版本升级时 | API 或格式变化时更新 |

### 版本管理

**在数据库中添加版本表：**

```sql
CREATE TABLE tafsir_metadata (
    language VARCHAR(10) PRIMARY KEY,
    source VARCHAR(50),
    version VARCHAR(20),
    last_updated TIMESTAMP
);

INSERT INTO tafsir_metadata VALUES ('id', 'Kemenag', '1.0.0', CURRENT_TIMESTAMP);
```

**客户端检查更新：**
```kotlin
suspend fun checkTafsirUpdate(): Boolean {
    val serverVersion = api.getTafsirVersion("id")
    val localVersion = database.getTafsirVersion("id")
    return serverVersion > localVersion
}
```

---

## 🎯 总结

### 实施步骤

1. ✅ **验证 API 访问**
   ```bash
   python sync_indonesian_tafsir.py --verify
   ```

2. ✅ **执行完整同步**
   ```bash
   python sync_indonesian_tafsir.py
   ```

3. ✅ **导入到数据库**
   ```bash
   python import_tafsir_to_db.py
   ```

4. ✅ **部署服务端 API**
   - 提供查询接口
   - 提供批量下载接口

5. ✅ **客户端集成**
   - 实现下载和缓存
   - 集成到 Tafsir 页面

---

### 关键优势

| 优势 | 说明 |
|------|------|
| ✅ **官方权威数据** | Kemenag 官方版本 |
| ✅ **无 Token 限制** | 公共 API，免费使用 |
| ✅ **完整覆盖** | 所有 114 Surahs |
| ✅ **标准化格式** | 统一的 JSON 结构 |
| ✅ **离线支持** | 本地缓存，无需网络 |

---

### 相关文件

- **`sync_indonesian_tafsir.py`** - 数据同步脚本
- **`import_tafsir_to_db.py`** - 数据库导入脚本（需创建）
- **`tafsir_data/indonesian/`** - 同步后的 JSON 数据

---

**创建时间：** 2025-11-18  
**脚本版本：** 1.0.0  
**数据源：** EQuran.id API v2.0  
**数据版本：** Kemenag (印尼宗教事务部)

---

**准备就绪！请按照本指南执行数据同步。** 🚀✨

