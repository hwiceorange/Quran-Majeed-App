# 📍 印尼语 Tafsir 数据位置和恢复指南

## 🔍 在服务器上查找数据文件

### 方法 1: 在 Hostinger 文件管理器中搜索

1. **登录 Hostinger hPanel**
2. **点击 "文件管理器" (File Manager)**
3. **使用搜索功能**，搜索以下文件名：
   - `tafsir_indonesian.sql`
   - `tafsir_kemenag.sql`
   - `tafsir*.sql`
   - `indonesian*.sql`

### 方法 2: 检查常见目录

```
可能的位置：
├── /public_html/quran/database/
├── /public_html/quran/data/
├── /public_html/quran/sql/
├── /public_html/quran/backups/
├── /public_html/database/
├── /public_html/backups/
├── /public_html/tafsir/
└── /public_html/scripts/
```

### 方法 3: 在 phpMyAdmin 中检查

**可能数据已经在其他表中：**

1. **在 phpMyAdmin 中，查看所有表**
2. **查找类似名称的表：**
   - `tafsir`
   - `quran_tafsir`
   - `tafsir_id`
   - `indonesian_tafsir`
   - `kemenag_tafsir`

**检查 SQL：**
```sql
-- 显示所有表
SHOW TABLES;

-- 搜索包含 tafsir 的表
SHOW TABLES LIKE '%tafsir%';
```

---

## 📊 数据来源信息

根据之前的记录，印尼语 Tafsir 数据来自：

### 原始数据源
- **API:** https://equran.id/api/v2/tafsir/
- **版本:** Kemenag (印尼宗教事务部)
- **总章节:** 114 章
- **总记录:** 约 6,236 条

### API 端点
```
# 获取所有 Tafsir 列表
GET https://equran.id/api/v2/tafsir

# 获取特定章节的 Tafsir
GET https://equran.id/api/v2/tafsir/{surah_number}

示例：
GET https://equran.id/api/v2/tafsir/1  # 第 1 章
GET https://equran.id/api/v2/tafsir/2  # 第 2 章
...
GET https://equran.id/api/v2/tafsir/114  # 第 114 章
```

---

## 🔄 重新同步数据的方法

### 方法 1: 使用现成的完整 SQL 文件（推荐）

**如果您有备份或之前下载的文件：**

1. **在本地查找文件：**
   ```
   /Users/huwei/AndroidStudioProjects/quran0/
   /Users/huwei/Downloads/
   /Users/huwei/Documents/quran/
   ```

2. **可能的文件名：**
   - `tafsir_indonesian.sql`
   - `tafsir_kemenag_full.sql`
   - `indonesian_tafsir_complete.sql`

3. **导入到数据库：**
   - 在 phpMyAdmin 点击 "导入" (Import)
   - 选择文件
   - 执行导入

---

### 方法 2: 使用 Python 脚本重新下载

**创建同步脚本：**

文件位置：`/Users/huwei/AndroidStudioProjects/quran0/scripts/resync_indonesian_tafsir.py`

**使用步骤：**

1. **运行脚本下载数据**
2. **生成 SQL 文件**
3. **上传到服务器**
4. **导入到数据库**

---

### 方法 3: 手动从 API 获取并导入

**如果需要，我可以为您生成完整的下载和导入脚本。**

---

## 📂 本地项目中的相关文件

### 检查这些位置

```bash
# 在项目根目录搜索
cd /Users/huwei/AndroidStudioProjects/quran0

# 查找 SQL 文件
find . -name "*tafsir*.sql" -type f

# 查找数据文件
find . -name "*indonesian*.sql" -type f
find . -name "*kemenag*.sql" -type f

# 检查 scripts 目录
ls -la scripts/

# 检查 database 目录（如果有）
ls -la database/

# 检查 server_deploy 目录
ls -la server_deploy/
```

---

## 🎯 临时解决方案：使用测试数据

**当前已有测试数据（13 条记录）：**

位置：`/Users/huwei/AndroidStudioProjects/quran0/test_tafsir_data.sql`

**包含：**
- 第 1 章（Al-Fatihah）：7 节
- 第 2 章（Al-Baqarah）：3 节
- 第 3 章（Ali Imran）：3 节

**导入步骤：**

1. **在 phpMyAdmin 点击 "SQL" 标签**
2. **复制 `test_tafsir_data.sql` 的内容**
3. **粘贴并执行**
4. **验证：**
   ```sql
   SELECT COUNT(*) FROM tafsir_indonesian;
   SELECT * FROM tafsir_indonesian LIMIT 5;
   ```

**这样您可以先测试 API 是否工作，然后再导入完整数据。**

---

## 🆘 如果找不到数据文件

### 选项 A: 我帮您重新生成（推荐）

**我可以为您创建：**

1. ✅ **完整的 Python 下载脚本**
   - 从 eQuran API 下载所有 114 章
   - 自动生成 SQL INSERT 语句
   - 保存为 `tafsir_indonesian_complete.sql`

2. ✅ **分章节的 SQL 文件**
   - 如果完整文件太大
   - 可以分成多个文件导入

3. ✅ **直接导入脚本**
   - 连接数据库
   - 直接插入数据

**需要我创建吗？**

---

### 选项 B: 从备份恢复

**检查可能的备份位置：**

1. **Hostinger 自动备份**
   - 在控制面板查看 "备份" (Backups)
   - 可能有数据库自动备份

2. **本地备份**
   - Time Machine（Mac）
   - 本地备份文件夹

3. **Git 仓库**（如果有提交过）
   ```bash
   cd /Users/huwei/AndroidStudioProjects/quran0
   git log --all --full-history -- "*tafsir*.sql"
   ```

---

### 选项 C: 使用第三方数据源

**可用的印尼语 Tafsir 数据源：**

1. **eQuran.id API** (推荐)
   - https://equran.id/api/v2/tafsir/
   - 完整且免费

2. **Quran.com API**
   - https://api.quran.com/api/v4/tafsirs/
   - 可能有印尼语选项

3. **GitHub 开源数据**
   - 搜索 "Indonesian Quran Tafsir dataset"

---

## 📝 下一步行动

### 立即可做（使用测试数据）

1. ✅ **导入测试数据** (`test_tafsir_data.sql`)
2. ✅ **测试 API 是否工作**
3. ✅ **验证应用端集成**

**测试 URL：**
```
https://apis.dochubai.com/quran/apis/tafsirs/id-tafsir-kemenag/by_ayah/1:1
```

---

### 稍后完成（完整数据）

1. ⏳ **查找或重新下载完整数据**
2. ⏳ **导入 6,236 条完整记录**
3. ⏳ **全面测试所有章节**

---

## 🚀 快速开始命令

### 搜索本地文件

```bash
cd /Users/huwei/AndroidStudioProjects/quran0

# 查找所有 SQL 文件
find . -name "*.sql" -type f

# 查找包含 tafsir 的文件
find . -name "*tafsir*" -type f

# 查找包含 indonesian 的文件
find . -name "*indonesian*" -type f
```

### 导入测试数据（在 phpMyAdmin）

```sql
-- 复制 test_tafsir_data.sql 的内容并执行

-- 验证导入
SELECT COUNT(*) as total FROM tafsir_indonesian;
SELECT * FROM tafsir_indonesian ORDER BY surah_id, ayat_id LIMIT 10;
```

---

## 📞 需要帮助

**请告诉我：**

1. ✅ 是否在本地找到 SQL 文件？
   - 运行搜索命令的结果

2. ✅ 是否需要我创建下载脚本？
   - 我可以生成完整的 Python 脚本

3. ✅ 先用测试数据测试？
   - 13 条记录足够验证功能

**我随时准备帮您！** 🚀

