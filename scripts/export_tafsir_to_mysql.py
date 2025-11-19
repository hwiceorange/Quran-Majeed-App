#!/usr/bin/env python3
"""
导出 SQLite 中的印尼语 Tafsir 数据到 MySQL 格式的 SQL 文件
"""

import sqlite3
import sys
from pathlib import Path

def export_to_mysql_sql():
    """导出数据到 MySQL SQL 文件"""
    
    # 数据库文件路径
    db_path = Path(__file__).parent / "tafsir_database.db"
    output_path = Path(__file__).parent.parent / "tafsir_indonesian_complete.sql"
    
    print(f"📂 Reading from: {db_path}")
    print(f"📝 Writing to: {output_path}")
    
    try:
        # 连接 SQLite 数据库
        conn = sqlite3.connect(db_path)
        cursor = conn.cursor()
        
        # 获取总记录数
        cursor.execute("SELECT COUNT(*) FROM tafsir_indonesian")
        total = cursor.fetchone()[0]
        print(f"\n📊 Total records: {total}")
        
        # 打开输出文件
        with open(output_path, 'w', encoding='utf-8') as f:
            # 写入文件头
            f.write("-- ========================================\n")
            f.write("-- 印尼语古兰经注释完整数据\n")
            f.write("-- ========================================\n")
            f.write(f"-- 总记录数: {total}\n")
            f.write("-- 来源: Kemenag (印尼宗教事务部)\n")
            f.write("-- API: https://equran.id/api/v2/tafsir/\n")
            f.write("-- ========================================\n\n")
            
            # 写入表结构（如果表不存在则创建）
            f.write("-- 创建表结构\n")
            f.write("CREATE TABLE IF NOT EXISTS `tafsir_indonesian` (\n")
            f.write("  `id` int(11) NOT NULL AUTO_INCREMENT,\n")
            f.write("  `surah_id` int(11) NOT NULL COMMENT '章节编号 (1-114)',\n")
            f.write("  `ayat_id` int(11) NOT NULL COMMENT '经文编号',\n")
            f.write("  `text` text NOT NULL COMMENT '印尼语注释内容',\n")
            f.write("  `language` varchar(10) NOT NULL DEFAULT 'id' COMMENT '语言代码',\n")
            f.write("  PRIMARY KEY (`id`),\n")
            f.write("  KEY `idx_verse` (`surah_id`, `ayat_id`, `language`)\n")
            f.write(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;\n\n")
            
            # 写入数据
            f.write("-- 插入数据\n")
            
            # 查询所有数据
            cursor.execute("""
                SELECT surah_id, ayat_id, language, source, text
                FROM tafsir_indonesian
                ORDER BY surah_id, ayat_id
            """)
            
            # 批量写入 INSERT 语句
            batch_size = 100
            records = cursor.fetchall()
            
            for i in range(0, len(records), batch_size):
                batch = records[i:i + batch_size]
                
                f.write("INSERT INTO `tafsir_indonesian` (`surah_id`, `ayat_id`, `text`, `language`) VALUES\n")
                
                for idx, (surah_id, ayat_id, language, source, text) in enumerate(batch):
                    # 转义单引号和反斜杠
                    text_escaped = text.replace('\\', '\\\\').replace("'", "\\'")
                    
                    # 写入 VALUES
                    comma = ',' if idx < len(batch) - 1 else ';'
                    f.write(f"({surah_id}, {ayat_id}, '{text_escaped}', '{language}'){comma}\n")
                
                f.write("\n")
                
                # 显示进度
                progress = min(i + batch_size, total)
                percentage = (progress / total) * 100
                print(f"✅ Progress: {progress}/{total} ({percentage:.1f}%)", end='\r')
            
            print()  # 新行
            
        conn.close()
        
        # 显示文件大小
        file_size = output_path.stat().st_size / (1024 * 1024)  # MB
        print(f"\n✅ Export complete!")
        print(f"📄 Output file: {output_path}")
        print(f"📊 File size: {file_size:.2f} MB")
        print(f"📝 Total records: {total}")
        
        print("\n🎯 Next steps:")
        print("1. Upload the SQL file to your server")
        print("2. Import it in phpMyAdmin")
        print("3. Test the API")
        
        return output_path
        
    except Exception as e:
        print(f"\n❌ Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    export_to_mysql_sql()

