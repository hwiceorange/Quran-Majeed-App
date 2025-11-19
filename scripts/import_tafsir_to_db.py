#!/usr/bin/env python3
"""
Tafsir 数据导入到数据库脚本

将同步后的 JSON 数据导入到 SQLite/MySQL/PostgreSQL 数据库
"""

import sqlite3
import json
import os
from pathlib import Path
from datetime import datetime
from typing import Dict, List

# ═══════════════════════════════════════════════════════════════
# 配置
# ═══════════════════════════════════════════════════════════════

# 数据库配置
DB_TYPE = "sqlite"  # sqlite, mysql, postgresql
DB_PATH = "tafsir_database.db"  # SQLite 路径

# JSON 数据目录
JSON_DIR = "tafsir_data/indonesian"

# ═══════════════════════════════════════════════════════════════
# 工具函数
# ═══════════════════════════════════════════════════════════════

def log(message: str, level: str = "INFO"):
    """打印带时间戳的日志"""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] [{level}] {message}")


# ═══════════════════════════════════════════════════════════════
# 数据库操作
# ═══════════════════════════════════════════════════════════════

def create_database():
    """创建数据库和表"""
    log("创建数据库和表...")
    
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # 创建主表
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
    
    cursor.execute('''
        CREATE INDEX IF NOT EXISTS idx_language 
        ON tafsir_indonesian(language)
    ''')
    
    # 创建元数据表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS tafsir_metadata (
            language VARCHAR(10) PRIMARY KEY,
            source VARCHAR(50) NOT NULL,
            version VARCHAR(20) NOT NULL,
            total_records INTEGER DEFAULT 0,
            last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    ''')
    
    # 创建 Surah 元数据表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS surah_metadata (
            surah_id INTEGER PRIMARY KEY,
            surah_name VARCHAR(100) NOT NULL,
            surah_name_latin VARCHAR(100) NOT NULL,
            surah_name_translation VARCHAR(200),
            total_ayat INTEGER NOT NULL
        )
    ''')
    
    conn.commit()
    conn.close()
    
    log("✅ 数据库和表创建完成")


def import_tafsir_data():
    """导入 Tafsir 数据到数据库"""
    log("=" * 60)
    log("开始导入 Tafsir 数据到数据库")
    log("=" * 60)
    
    if not os.path.exists(JSON_DIR):
        log(f"❌ 数据目录不存在: {JSON_DIR}", "ERROR")
        return
    
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # 遍历所有 JSON 文件
    json_files = sorted(Path(JSON_DIR).glob("surah_*_tafsir.json"))
    
    if not json_files:
        log(f"❌ 在 {JSON_DIR} 中没有找到 Tafsir JSON 文件", "ERROR")
        conn.close()
        return
    
    total_records = 0
    total_surahs = 0
    
    for json_file in json_files:
        log(f"\n处理文件: {json_file.name}")
        
        try:
            with open(json_file, 'r', encoding='utf-8') as f:
                data = json.load(f)
            
            surah_id = data["surah_id"]
            language = data.get("language", "id")
            source = data.get("source", "Kemenag")
            
            # 插入或更新 Surah 元数据
            cursor.execute('''
                INSERT OR REPLACE INTO surah_metadata 
                (surah_id, surah_name, surah_name_latin, surah_name_translation, total_ayat)
                VALUES (?, ?, ?, ?, ?)
            ''', (
                surah_id,
                data.get("surah_name", ""),
                data.get("surah_name_latin", ""),
                data.get("surah_name_translation", ""),
                data.get("total_ayat", 0)
            ))
            
            # 插入 Tafsir 数据
            surah_records = 0
            for tafsir_item in data.get("tafsir", []):
                ayat_id = tafsir_item["ayat_id"]
                text = tafsir_item["text"]
                
                try:
                    cursor.execute('''
                        INSERT OR REPLACE INTO tafsir_indonesian 
                        (surah_id, ayat_id, language, source, text, updated_at)
                        VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    ''', (surah_id, ayat_id, language, source, text))
                    
                    surah_records += 1
                except sqlite3.Error as e:
                    log(f"  ❌ 导入失败 Ayat {ayat_id}: {e}", "ERROR")
            
            total_records += surah_records
            total_surahs += 1
            
            log(f"  ✅ Surah {surah_id}: 导入 {surah_records} 条注释")
            
        except Exception as e:
            log(f"  ❌ 处理文件失败: {e}", "ERROR")
    
    # 更新元数据
    cursor.execute('''
        INSERT OR REPLACE INTO tafsir_metadata 
        (language, source, version, total_records, last_updated)
        VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
    ''', ('id', 'Kemenag', '1.0.0', total_records))
    
    conn.commit()
    conn.close()
    
    log("\n" + "=" * 60)
    log("导入完成！")
    log("=" * 60)
    log(f"✅ 导入 Surahs: {total_surahs}/114")
    log(f"✅ 导入记录总数: {total_records}")
    log(f"📁 数据库文件: {os.path.abspath(DB_PATH)}")
    log("=" * 60)


def verify_database():
    """验证数据库内容"""
    log("\n" + "=" * 60)
    log("验证数据库内容")
    log("=" * 60)
    
    if not os.path.exists(DB_PATH):
        log(f"❌ 数据库文件不存在: {DB_PATH}", "ERROR")
        return
    
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    # 检查 Tafsir 记录数
    cursor.execute("SELECT COUNT(*) FROM tafsir_indonesian")
    total_records = cursor.fetchone()[0]
    log(f"📊 Tafsir 记录总数: {total_records}")
    
    # 检查 Surah 数量
    cursor.execute("SELECT COUNT(DISTINCT surah_id) FROM tafsir_indonesian")
    total_surahs = cursor.fetchone()[0]
    log(f"📊 Surah 数量: {total_surahs}/114")
    
    # 检查元数据
    cursor.execute("SELECT * FROM tafsir_metadata WHERE language = 'id'")
    metadata = cursor.fetchone()
    if metadata:
        log(f"📊 元数据信息:")
        log(f"   - 语言: {metadata[0]}")
        log(f"   - 来源: {metadata[1]}")
        log(f"   - 版本: {metadata[2]}")
        log(f"   - 记录数: {metadata[3]}")
        log(f"   - 更新时间: {metadata[4]}")
    
    # 检查示例数据
    log(f"\n📝 示例数据 (Surah 1, Ayat 1):")
    cursor.execute('''
        SELECT surah_id, ayat_id, substr(text, 1, 100) as text_preview
        FROM tafsir_indonesian 
        WHERE surah_id = 1 AND ayat_id = 1
    ''')
    sample = cursor.fetchone()
    if sample:
        log(f"   - Surah ID: {sample[0]}")
        log(f"   - Ayat ID: {sample[1]}")
        log(f"   - 内容预览: {sample[2]}...")
    
    # 检查缺失的 Surah
    cursor.execute('''
        SELECT surah_id FROM surah_metadata 
        WHERE surah_id NOT IN (SELECT DISTINCT surah_id FROM tafsir_indonesian)
    ''')
    missing_surahs = cursor.fetchall()
    if missing_surahs:
        log(f"\n⚠️ 缺失的 Surahs: {[s[0] for s in missing_surahs]}", "WARN")
    else:
        log(f"\n✅ 所有 114 个 Surahs 的数据完整")
    
    conn.close()
    log("=" * 60)


def export_sample_queries():
    """导出示例查询语句"""
    log("\n" + "=" * 60)
    log("示例查询语句")
    log("=" * 60)
    
    queries = [
        ("获取特定 Ayat 的 Tafsir", 
         "SELECT text FROM tafsir_indonesian WHERE surah_id = 1 AND ayat_id = 1;"),
        
        ("获取整个 Surah 的 Tafsir", 
         "SELECT ayat_id, text FROM tafsir_indonesian WHERE surah_id = 1 ORDER BY ayat_id;"),
        
        ("统计每个 Surah 的注释数量", 
         "SELECT surah_id, COUNT(*) as count FROM tafsir_indonesian GROUP BY surah_id ORDER BY surah_id;"),
        
        ("查询包含特定关键词的注释", 
         "SELECT surah_id, ayat_id, substr(text, 1, 100) FROM tafsir_indonesian WHERE text LIKE '%Allah%' LIMIT 5;"),
        
        ("获取 Surah 元数据", 
         "SELECT * FROM surah_metadata WHERE surah_id = 1;"),
    ]
    
    for title, query in queries:
        log(f"\n{title}:")
        log(f"  {query}")
    
    log("\n" + "=" * 60)


# ═══════════════════════════════════════════════════════════════
# 主程序入口
# ═══════════════════════════════════════════════════════════════

def main():
    """主程序入口"""
    global DB_PATH, JSON_DIR  # 🔧 必须在使用前声明
    
    import argparse
    
    parser = argparse.ArgumentParser(
        description="Tafsir 数据导入到数据库"
    )
    parser.add_argument(
        "--db",
        type=str,
        default=DB_PATH,
        help=f"数据库文件路径 (默认: {DB_PATH})"
    )
    parser.add_argument(
        "--json-dir",
        type=str,
        default=JSON_DIR,
        help=f"JSON 数据目录 (默认: {JSON_DIR})"
    )
    parser.add_argument(
        "--verify-only",
        action="store_true",
        help="仅验证数据库内容，不导入数据"
    )
    
    args = parser.parse_args()
    
    # 更新全局配置
    DB_PATH = args.db
    JSON_DIR = args.json_dir
    
    if args.verify_only:
        # 仅验证
        verify_database()
        export_sample_queries()
    else:
        # 创建数据库
        create_database()
        
        # 导入数据
        import_tafsir_data()
        
        # 验证导入结果
        verify_database()
        
        # 显示示例查询
        export_sample_queries()
    
    return 0


if __name__ == "__main__":
    exit(main())

