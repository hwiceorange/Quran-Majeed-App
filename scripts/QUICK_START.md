# 印尼语 Tafsir 数据同步 - 快速开始

## 🚀 5 分钟快速开始

### 前置条件

```bash
# 检查 Python 版本（需要 3.6+）
python3 --version

# 安装依赖
pip3 install requests
```

---

## 📦 步骤 1：验证 API 访问（1 分钟）

```bash
cd /Users/huwei/AndroidStudioProjects/quran0/scripts

# 验证 API 是否可访问
python3 sync_indonesian_tafsir.py --verify
```

**预期输出：**
```
[INFO] ✅ API 访问正常
[INFO] Surah 信息:
[INFO]   - 编号: 1
[INFO]   - 名称: الفاتحة
[INFO]   - 拉丁名: Al Fatihah
[INFO]   - Tafsir 注释数量: 7
```

**如果失败：** 检查网络连接

---

## 📥 步骤 2：同步所有 Tafsir 数据（15 分钟）

```bash
# 执行完整同步（约 15 分钟，114 个 Surahs）
python3 sync_indonesian_tafsir.py
```

**过程：**
- 下载所有 114 个 Surah 的 Tafsir
- 保存为 JSON 文件
- 总数据量约 5-10 MB

**输出目录：**
```
tafsir_data/indonesian/
├── surah_001_tafsir.json  (Surah 1)
├── surah_002_tafsir.json  (Surah 2)
...
└── surah_114_tafsir.json  (Surah 114)
```

---

## 💾 步骤 3：导入到数据库（5 分钟）

```bash
# 创建数据库并导入数据
python3 import_tafsir_to_db.py
```

**输出：**
```
[INFO] ✅ 数据库和表创建完成
[INFO] ✅ Surah 1: 导入 7 条注释
[INFO] ✅ Surah 2: 导入 286 条注释
...
[INFO] ✅ 导入记录总数: 6236
[INFO] 📁 数据库文件: tafsir_database.db
```

**生成文件：**
- `tafsir_database.db` - SQLite 数据库文件

---

## ✅ 步骤 4：验证数据（1 分钟）

```bash
# 验证数据库内容
python3 import_tafsir_to_db.py --verify-only
```

**输出：**
```
[INFO] 📊 Tafsir 记录总数: 6236
[INFO] 📊 Surah 数量: 114/114
[INFO] ✅ 所有 114 个 Surahs 的数据完整
```

---

## 🧪 步骤 5：测试查询（1 分钟）

### 方法 1：使用 SQLite 命令行

```bash
sqlite3 tafsir_database.db

# 查询 Surah 1, Ayat 1 的 Tafsir
SELECT text FROM tafsir_indonesian WHERE surah_id = 1 AND ayat_id = 1;

# 退出
.quit
```

### 方法 2：使用 Python

```python
import sqlite3

conn = sqlite3.connect('tafsir_database.db')
cursor = conn.cursor()

cursor.execute('''
    SELECT text FROM tafsir_indonesian 
    WHERE surah_id = 1 AND ayat_id = 1
''')

text = cursor.fetchone()[0]
print(text)

conn.close()
```

---

## 📊 数据统计

### 完整数据集

| 项目 | 数量 |
|------|------|
| **Surahs** | 114 |
| **Ayat (总计)** | 6,236 |
| **Tafsir 记录** | 6,236 |
| **语言** | 印尼语 (id) |
| **来源** | Kemenag |
| **数据大小** | ~10 MB (JSON) |
| **数据库大小** | ~15 MB (SQLite) |

---

## 🔍 常见问题

### Q1: API 访问失败怎么办？

**A:** 
1. 检查网络连接
2. 尝试使用代理
3. 等待几分钟后重试

---

### Q2: 同步中断了怎么办？

**A:** 重新运行同步脚本，已下载的 Surah 会被跳过

```bash
# 脚本会自动检查已存在的文件
python3 sync_indonesian_tafsir.py
```

---

### Q3: 如何只同步特定的 Surah？

**A:** 修改脚本或手动调用 API：

```bash
curl "https://equran.id/api/v2/tafsir/1" > surah_001_tafsir.json
```

---

### Q4: 数据库在哪里？

**A:** 
```bash
# 查看数据库位置
ls -lh tafsir_database.db

# 查看数据库大小
du -h tafsir_database.db
```

---

### Q5: 如何更新数据？

**A:** 重新运行同步和导入脚本：

```bash
# 删除旧数据
rm -rf tafsir_data/indonesian/
rm tafsir_database.db

# 重新同步
python3 sync_indonesian_tafsir.py
python3 import_tafsir_to_db.py
```

---

## 🌐 下一步：部署到服务端

### 选项 1：直接使用 SQLite 数据库

```bash
# 将数据库文件上传到服务器
scp tafsir_database.db user@server:/path/to/db/
```

### 选项 2：导入到 MySQL/PostgreSQL

参考 `INDONESIAN_TAFSIR_SYNC_GUIDE.md` 中的数据库迁移章节

### 选项 3：提供 REST API

创建 API 服务，提供查询接口：

```python
# Flask 示例
from flask import Flask, jsonify
import sqlite3

app = Flask(__name__)

@app.route('/api/tafsir/<int:surah_id>/<int:ayat_id>')
def get_tafsir(surah_id, ayat_id):
    conn = sqlite3.connect('tafsir_database.db')
    cursor = conn.cursor()
    
    cursor.execute('''
        SELECT * FROM tafsir_indonesian 
        WHERE surah_id = ? AND ayat_id = ?
    ''', (surah_id, ayat_id))
    
    result = cursor.fetchone()
    conn.close()
    
    if result:
        return jsonify({
            'success': True,
            'data': {
                'surah_id': result[1],
                'ayat_id': result[2],
                'language': result[3],
                'source': result[4],
                'text': result[5]
            }
        })
    else:
        return jsonify({'success': False, 'error': 'Not found'}), 404

if __name__ == '__main__':
    app.run(debug=True)
```

---

## 📄 相关文档

- **`INDONESIAN_TAFSIR_SYNC_GUIDE.md`** - 完整同步指南
- **`sync_indonesian_tafsir.py`** - 数据同步脚本
- **`import_tafsir_to_db.py`** - 数据库导入脚本

---

## ✅ 完成清单

- [ ] 验证 API 访问
- [ ] 同步所有 Tafsir 数据
- [ ] 导入到数据库
- [ ] 验证数据完整性
- [ ] 测试查询
- [ ] 部署到服务端
- [ ] 集成到客户端应用

---

## 🎯 成功标准

同步成功后，您应该有：

✅ **114 个 JSON 文件** - 每个 Surah 一个文件  
✅ **SQLite 数据库** - 包含 6,236 条 Tafsir 记录  
✅ **完整的元数据** - Surah 名称、语言、来源信息  
✅ **可查询的数据** - 通过 SQL 或 API 查询

---

**准备就绪！开始同步印尼语 Tafsir 数据。** 🚀✨

**估计总时间：** 约 25-30 分钟（包括同步、导入和验证）

---

**创建时间：** 2025-11-18  
**版本：** 1.0.0  
**状态：** 已准备好使用

