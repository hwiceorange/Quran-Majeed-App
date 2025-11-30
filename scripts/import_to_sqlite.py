#!/usr/bin/env python3
"""
将转换后的翻译数据导入 Android SQLite 数据库

功能：
1. 创建 QuranTranslation.db 数据库
2. 为每个翻译创建独立的表
3. 导入 6236 条经文
4. 创建索引优化查询性能
5. ⚠️ 不影响现有预装的 4 个翻译

Usage:
    python import_to_sqlite.py
"""

import sqlite3
import json
from pathlib import Path
from datetime import datetime

# ═══════════════════════════════════════════════════════════════
# 配置
# ═══════════════════════════════════════════════════════════════

INPUT_DIR = Path(__file__).parent / "translation_data" / "converted"
METADATA_FILE = Path(__file__).parent / "translation_data" / "metadata.json"
OUTPUT_DB = Path(__file__).parent / "QuranTranslation_New.db"

# 现有预装翻译（不要覆盖）
EXISTING_PREBUILT_SLUGS = [
    "en_101_sahih-international",
    "en_102_the-clear-quran",
    "in_junagarhi",
    "in_quran-complex"
]

# ═══════════════════════════════════════════════════════════════
# 日志函数
# ═══════════════════════════════════════════════════════════════

def log(message: str, level: str = "INFO"):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] [{level}] {message}")

# ═══════════════════════════════════════════════════════════════
# 数据库操作
# ═══════════════════════════════════════════════════════════════

def create_database():
    """创建数据库和元数据表"""
    log("创建数据库...")
    
    conn = sqlite3.connect(OUTPUT_DB)
    cursor = conn.cursor()
    
    # 创建翻译元数据表
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS QuranTranslationBookInfo (
            slug TEXT PRIMARY KEY,
            langCode TEXT NOT NULL,
            langName TEXT NOT NULL,
            bookName TEXT NOT NULL,
            authorName TEXT,
            displayName TEXT NOT NULL,
            isPremium INTEGER DEFAULT 0,
            lastUpdated TEXT,
            downloadPath TEXT
        )
    ''')
    
    conn.commit()
    conn.close()
    
    log("✅ 数据库创建完成")

def escape_table_name(table_name: str) -> str:
    """转义表名（处理特殊字符）"""
    return f"`{table_name}`"

def create_translation_table(conn: sqlite3.Connection, slug: str):
    """为单个翻译创建内容表"""
    cursor = conn.cursor()
    
    escaped_name = escape_table_name(slug)
    
    cursor.execute(f'''
        CREATE TABLE IF NOT EXISTS {escaped_name} (
            _id TEXT PRIMARY KEY,
            chapterNo INTEGER NOT NULL,
            verseNo INTEGER NOT NULL,
            text TEXT NOT NULL,
            footnotes TEXT
        )
    ''')
    
    # 创建索引
    cursor.execute(f'''
        CREATE INDEX IF NOT EXISTS idx_{slug.replace("-", "_").replace("_", "")}_chapter_verse 
        ON {escaped_name}(chapterNo, verseNo)
    ''')
    
    conn.commit()

def insert_translation_metadata(conn: sqlite3.Connection, metadata: dict):
    """插入翻译元数据"""
    cursor = conn.cursor()
    
    # 映射语言代码
    lang_code_map = {
        'be': 'bn',  # bengali
        'in': 'id',  # indonesian
        'ma': 'ms',  # malay
        'tu': 'tr',  # turkish
        'ur': 'ur',  # urdu
    }
    
    lang_code = metadata.get('language_code', '')
    lang_code = lang_code_map.get(lang_code, lang_code)
    
    cursor.execute('''
        INSERT OR REPLACE INTO QuranTranslationBookInfo 
        (slug, langCode, langName, bookName, authorName, displayName, isPremium, lastUpdated, downloadPath)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
    ''', (
        metadata.get('slug'),
        lang_code,
        metadata.get('language_name'),
        metadata.get('book_name'),
        metadata.get('author_name'),
        metadata.get('display_name'),
        0,  # isPremium
        metadata.get('synced_at'),
        metadata.get('download_path')
    ))
    
    conn.commit()

def insert_translation_verses(conn: sqlite3.Connection, slug: str, data: dict):
    """插入翻译的经文内容"""
    cursor = conn.cursor()
    escaped_name = escape_table_name(slug)
    
    chapters = data.get('chapters', [])
    total_inserted = 0
    
    for chapter in chapters:
        chapter_no = chapter.get('number')
        verses = chapter.get('verses', [])
        
        for verse in verses:
            verse_no = verse.get('number')
            text = verse.get('text', '')
            footnotes = json.dumps(verse.get('footnotes', []))
            
            verse_key = f"{chapter_no}:{verse_no}"
            
            cursor.execute(f'''
                INSERT OR REPLACE INTO {escaped_name} 
                (_id, chapterNo, verseNo, text, footnotes)
                VALUES (?, ?, ?, ?, ?)
            ''', (verse_key, chapter_no, verse_no, text, footnotes))
            
            total_inserted += 1
    
    conn.commit()
    return total_inserted

# ═══════════════════════════════════════════════════════════════
# 主处理流程
# ═══════════════════════════════════════════════════════════════

def import_translation(conn: sqlite3.Connection, slug: str, metadata: dict) -> bool:
    """导入单个翻译"""
    
    # 检查是否是现有预装翻译
    if slug in EXISTING_PREBUILT_SLUGS:
        log(f"⏭️ 跳过预装翻译: {slug} (保持原有数据)", "WARN")
        return False
    
    translation_name = metadata.get('book_name')
    
    log("=" * 60)
    log(f"导入翻译: {translation_name}")
    log(f"Slug: {slug}")
    log("=" * 60)
    
    # 读取转换后的数据
    data_file = INPUT_DIR / f"{slug}.json"
    
    if not data_file.exists():
        log(f"❌ 数据文件不存在: {data_file}", "ERROR")
        return False
    
    with open(data_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    try:
        # 1. 创建翻译表
        log("📋 创建翻译表...")
        create_translation_table(conn, slug)
        
        # 2. 插入元数据
        log("💾 插入元数据...")
        insert_translation_metadata(conn, metadata)
        
        # 3. 插入经文内容
        log("📝 插入经文内容...")
        total_verses = insert_translation_verses(conn, slug, data)
        log(f"✅ 已插入 {total_verses} 条经文")
        
        if total_verses != 6236:
            log(f"⚠️ 警告: 预期 6236 条经文，实际插入 {total_verses} 条", "WARN")
        
        log(f"✅ 导入完成: {translation_name}")
        log("")
        
        return True
        
    except Exception as e:
        log(f"❌ 导入失败: {translation_name}", "ERROR")
        log(f"   错误: {e}", "ERROR")
        return False

# ═══════════════════════════════════════════════════════════════
# 数据库优化
# ═══════════════════════════════════════════════════════════════

def optimize_database(conn: sqlite3.Connection):
    """优化数据库"""
    log("🔧 优化数据库...")
    
    cursor = conn.cursor()
    
    # VACUUM - 压缩数据库
    log("   执行 VACUUM...")
    cursor.execute("VACUUM")
    
    # ANALYZE - 更新统计信息
    log("   执行 ANALYZE...")
    cursor.execute("ANALYZE")
    
    conn.commit()
    log("✅ 数据库优化完成")

def get_database_info(conn: sqlite3.Connection):
    """获取数据库信息"""
    cursor = conn.cursor()
    
    # 获取所有表
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")
    tables = cursor.fetchall()
    
    log("=" * 60)
    log("📊 数据库信息")
    log("=" * 60)
    
    log(f"总表数: {len(tables)}")
    
    # 元数据表
    cursor.execute("SELECT COUNT(*) FROM QuranTranslationBookInfo")
    translation_count = cursor.fetchone()[0]
    log(f"翻译元数据: {translation_count} 条")
    
    # 各个翻译的经文数
    cursor.execute("SELECT slug, bookName, langName FROM QuranTranslationBookInfo")
    translations = cursor.fetchall()
    
    log("\n翻译列表:")
    for slug, book_name, lang_name in translations:
        try:
            cursor.execute(f"SELECT COUNT(*) FROM {escape_table_name(slug)}")
            verse_count = cursor.fetchone()[0]
            log(f"  - {book_name} ({lang_name}): {verse_count} 条经文")
        except:
            log(f"  - {book_name} ({lang_name}): 表不存在", "WARN")
    
    # 数据库文件大小
    db_size = OUTPUT_DB.stat().st_size / (1024 * 1024)  # MB
    log(f"\n数据库文件大小: {db_size:.2f} MB")
    
    log("=" * 60)

# ═══════════════════════════════════════════════════════════════
# 主函数
# ═══════════════════════════════════════════════════════════════

def main():
    log("🚀 开始导入翻译数据到数据库...")
    log("")
    
    # 检查输入文件
    if not METADATA_FILE.exists():
        log(f"❌ 元数据文件不存在: {METADATA_FILE}", "ERROR")
        log("   请先运行 sync_translations.py", "ERROR")
        return
    
    if not INPUT_DIR.exists():
        log(f"❌ 数据目录不存在: {INPUT_DIR}", "ERROR")
        log("   请先运行 sync_translations.py", "ERROR")
        return
    
    # 读取元数据
    with open(METADATA_FILE, 'r', encoding='utf-8') as f:
        metadata_json = json.load(f)
    
    translations = metadata_json.get('translations', [])
    log(f"📋 找到 {len(translations)} 个翻译待导入")
    log("")
    
    # 警告：跳过预装翻译
    log("⚠️ 注意: 以下预装翻译将被跳过（保持原有数据）:")
    for slug in EXISTING_PREBUILT_SLUGS:
        log(f"   - {slug}")
    log("")
    
    # 创建数据库
    create_database()
    
    # 连接数据库
    conn = sqlite3.connect(OUTPUT_DB)
    
    try:
        # 导入所有翻译
        success_count = 0
        skip_count = 0
        fail_count = 0
        
        for i, metadata in enumerate(translations, 1):
            slug = metadata.get('slug')
            log(f"进度: {i}/{len(translations)}")
            
            if slug in EXISTING_PREBUILT_SLUGS:
                skip_count += 1
                log(f"⏭️ 跳过: {metadata.get('book_name')}")
                log("")
                continue
            
            result = import_translation(conn, slug, metadata)
            
            if result:
                success_count += 1
            else:
                fail_count += 1
        
        # 优化数据库
        optimize_database(conn)
        
        # 显示数据库信息
        get_database_info(conn)
        
        # 统计
        log("")
        log("=" * 60)
        log("📊 导入统计")
        log("=" * 60)
        log(f"✅ 成功导入: {success_count}")
        log(f"⏭️ 跳过预装: {skip_count}")
        log(f"❌ 导入失败: {fail_count}")
        log(f"📁 数据库文件: {OUTPUT_DB}")
        log("=" * 60)
        
        if fail_count == 0:
            log("🎉 所有翻译导入成功！")
            log("")
            log("下一步:")
            log("1. 将数据库文件集成到 Android 应用")
            log("2. 更新 LocalTranslationData.kt")
            log("3. 测试按需下载功能")
        else:
            log(f"⚠️ 有 {fail_count} 个翻译导入失败，请检查日志", "WARN")
        
    finally:
        conn.close()

if __name__ == "__main__":
    main()

