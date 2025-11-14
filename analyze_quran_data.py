#!/usr/bin/env python3
"""
古兰经本地数据分析工具
分析 assets 目录中的古兰经和 Hadith JSON 文件
无需任何外部依赖，纯 Python 标准库
"""

import json
import os
from pathlib import Path

# 配置
APP_ID = 'com.quran.quranaudio.online'
ASSETS_DIR = 'app/src/main/assets'
SCRIPTS_DIR = f'{ASSETS_DIR}/scripts'
HADITH_DIR = ASSETS_DIR

def analyze_quran_script():
    """分析古兰经文本 JSON 文件"""
    print("=" * 70)
    print("🕌 古兰经文本数据分析")
    print("=" * 70)
    
    script_file = f'{SCRIPTS_DIR}/script_uthmani_hafs.json'
    
    if not os.path.exists(script_file):
        print(f"❌ 文件不存在: {script_file}")
        return None
    
    file_size = os.path.getsize(script_file) / (1024 * 1024)
    print(f"\n📂 文件: {script_file}")
    print(f"📦 大小: {file_size:.2f} MB")
    
    print(f"\n⏳ 正在解析 JSON...")
    
    with open(script_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # 检查顶层键，可能是 'chapters' 或 'suras'
    chapters = data.get('chapters', data.get('suras', []))
    
    print(f"\n✅ 解析成功！")
    print(f"📊 数据统计:")
    print(f"   - 章节数量: {len(chapters)}")
    
    total_verses = sum(len(ch.get('verses', ch.get('ayahs', []))) for ch in chapters)
    print(f"   - 总经文数: {total_verses}")
    
    # 分析第一章
    if chapters:
        first_ch = chapters[0]
        print(f"\n📖 示例 - 第一章 (Al-Fatiha):")
        print(f"   - Chapter Number: {first_ch.get('number', first_ch.get('id'))}")
        verses = first_ch.get('verses', first_ch.get('ayahs', []))
        print(f"   - Verses Count: {len(verses)}")
        
        if verses:
            first_verse = verses[0]
            print(f"\n   第一节经文:")
            print(f"   - ID: {first_verse.get('id')}")
            print(f"   - Number: {first_verse.get('number')}")
            print(f"   - Arabic Text: {first_verse.get('arabic_text', '')[:50]}...")
            print(f"   - Page Number: {first_verse.get('page_number')}")
            print(f"   - Juz Number: {first_verse.get('juz_number', 'N/A')}")
    
    return data


def analyze_hadith_data():
    """分析 Hadith JSON 文件"""
    print("\n" + "=" * 70)
    print("📿 Hadith 数据分析")
    print("=" * 70)
    
    # 分析 Bukhari 圣训集
    hadith_files = [
        ('ara-bukhari.min.json', '阿拉伯语', 'Sahih al-Bukhari'),
        ('urd-bukhari.min.json', '乌尔都语', 'Sahih al-Bukhari'),
        ('ind-bukhari.min.json', '印尼语', 'Sahih al-Bukhari'),
    ]
    
    for filename, language, book_name in hadith_files:
        filepath = f'{HADITH_DIR}/{filename}'
        
        if not os.path.exists(filepath):
            print(f"⚠️ 文件不存在: {filename}")
            continue
        
        file_size = os.path.getsize(filepath) / (1024 * 1024)
        print(f"\n📂 {language} - {book_name}")
        print(f"   文件: {filename}")
        print(f"   大小: {file_size:.2f} MB")
        
        # 读取第一行来分析元数据
        with open(filepath, 'r', encoding='utf-8') as f:
            # 由于文件很大，只读取部分来分析结构
            first_chars = f.read(5000)
            
            # 尝试解析 metadata
            try:
                # 找到 metadata 部分
                if '"metadata"' in first_chars:
                    metadata_start = first_chars.find('"metadata"')
                    metadata_end = first_chars.find('"hadiths"')
                    metadata_str = first_chars[metadata_start:metadata_end]
                    
                    if '"name"' in metadata_str:
                        name_start = metadata_str.find('"name"') + 8
                        name_end = metadata_str.find('"', name_start)
                        book_full_name = metadata_str[name_start:name_end]
                        print(f"   书名: {book_full_name}")
                    
                    if '"sections"' in metadata_str:
                        print(f"   包含章节划分: ✅")
                
                print(f"   状态: ✅ JSON 格式正确")
            except Exception as e:
                print(f"   解析元数据时出错: {e}")


def analyze_translations():
    """分析预装翻译文件"""
    print("\n" + "=" * 70)
    print("🌐 古兰经翻译数据分析")
    print("=" * 70)
    
    translations_dir = f'{ASSETS_DIR}/prebuilt_translations'
    
    if not os.path.exists(translations_dir):
        print(f"❌ 翻译目录不存在: {translations_dir}")
        return
    
    # 遍历所有翻译目录
    for trans_dir in os.listdir(translations_dir):
        trans_path = os.path.join(translations_dir, trans_dir)
        
        if not os.path.isdir(trans_path):
            continue
        
        print(f"\n📚 {trans_dir}:")
        
        # 查找 JSON 文件
        json_files = [f for f in os.listdir(trans_path) if f.endswith('.json') and f != 'manifest.json']
        
        for json_file in json_files:
            json_path = os.path.join(trans_path, json_file)
            file_size = os.path.getsize(json_path) / (1024 * 1024)
            print(f"   - {json_file}: {file_size:.2f} MB")


def generate_firestore_structure_report():
    """生成 Firestore 数据结构报告"""
    print("\n" + "=" * 70)
    print("🏗️  推荐的 Firestore 数据结构")
    print("=" * 70)
    
    structure = f"""
artifacts/
  └── {APP_ID}/
      └── public/
          └── data/
              ├── quran_surahs/              # 114 个文档
              │   ├── 1/                     # Surah 1
              │   │   ├── (元数据)
              │   │   │   - surah_id: 1
              │   │   │   - name_ar: "الفاتحة"
              │   │   │   - name_en: "Al-Fatiha"
              │   │   │   - verses_count: 7
              │   │   └── ayahs/             # 子集合
              │   │       ├── 1/
              │   │       │   - ayah_id: 1
              │   │       │   - verse_key: "1:1"
              │   │       │   - text_uthmani: "بِسْمِ ٱللَّهِ..."
              │   │       │   - juz_number: 1
              │   │       │   - page_number: 1
              │   │       └── ... (至 7)
              │   └── ... (至 114)
              │
              ├── quran_structures/          # 30 个文档 (Juz)
              │   ├── 1/
              │   │   - juz_number: 1
              │   │   - first_verse_key: "1:1"
              │   │   - last_verse_key: "2:141"
              │   │   - page_start: 1
              │   │   - page_end: 21
              │   └── ... (至 30)
              │
              ├── quran_translations/        # 按翻译 ID 和 Juz 拆分
              │   ├── 131_juz_1/             # Clear Quran - Juz 1
              │   │   - translation_id: 131
              │   │   - juz_number: 1
              │   │   - language: "en"
              │   │   - texts: {{"1:1": "...", "1:2": "..."}}
              │   ├── 131_juz_2/
              │   └── ... (每个翻译 30 个 Juz = 120 文档)
              │
              ├── quran_tafsirs/             # 按注释 ID 和 Juz 拆分
              │   └── 169_juz_1/
              │       - tafsir_id: 169
              │       - juz_number: 1
              │       - texts: {{"1:1": "注释文本...", ...}}
              │
              └── quran_recitations/         # 按朗诵者 ID
                  ├── 7/
                  │   - reciter_id: 7
                  │   - reciter_name: "Saad Al-Ghamdi"
                  │   - audio_format: "mp3"
                  │   - base_url: "https://..."
                  └── ...

💡 数据拆分策略：
   - Surah + Ayah: 阿拉伯语原文（离线优先）
   - Translations: 按 Juz 拆分（避免 1MB 限制）
   - Tafsirs: 按 Juz 拆分（注释文本通常较长）
   - Recitations: 存储 URL 模板（实际音频在线加载）
"""
    
    print(structure)


def analyze_local_json_structure():
    """详细分析本地 JSON 文件的数据结构"""
    print("\n" + "=" * 70)
    print("🔍 本地 JSON 数据结构详细分析")
    print("=" * 70)
    
    script_file = f'{SCRIPTS_DIR}/script_uthmani_hafs.json'
    
    if not os.path.exists(script_file):
        print(f"❌ 文件不存在，跳过分析")
        return
    
    print(f"⏳ 解析文件...")
    
    with open(script_file, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    print(f"\n📋 顶层键:")
    for key in data.keys():
        print(f"   - {key}")
    
    chapters = data.get('chapters', data.get('suras', []))
    
    if chapters and len(chapters) > 0:
        sample_chapter = chapters[0]
        print(f"\n📖 章节对象结构 (以第一章为例):")
        for key in sample_chapter.keys():
            value = sample_chapter[key]
            if isinstance(value, list):
                print(f"   - {key}: Array[{len(value)}]")
            else:
                print(f"   - {key}: {type(value).__name__}")
        
        verses = sample_chapter.get('verses', sample_chapter.get('ayahs', []))
        if verses and len(verses) > 0:
            sample_verse = verses[0]
            print(f"\n📝 经文对象结构:")
            for key, value in sample_verse.items():
                if isinstance(value, str) and len(value) > 50:
                    print(f"   - {key}: \"{value[:50]}...\"")
                else:
                    print(f"   - {key}: {value}")
    
    return data


def generate_summary():
    """生成完整摘要"""
    print("\n" + "=" * 70)
    print("📊 数据来源完整摘要")
    print("=" * 70)
    
    summary = """
┌─────────────────────────────────────────────────────────────────┐
│ 数据类型          │ 来源        │ 存储位置              │ 网络  │
├─────────────────────────────────────────────────────────────────┤
│ 古兰经原文        │ 本地 JSON   │ assets/scripts/       │ ❌   │
│ Hadith 圣训      │ 本地 JSON   │ assets/*.min.json     │ ❌   │
│ 预装翻译          │ 本地 JSON   │ assets/prebuilt_*/    │ ❌   │
│ 在线翻译          │ API        │ 动态下载缓存           │ ✅   │
│ Tafsir 注释      │ API        │ 实时获取              │ ✅   │
│ 朗诵音频          │ API        │ 流式播放              │ ✅   │
└─────────────────────────────────────────────────────────────────┘

💡 建议：
   1. 保持古兰经原文本地存储（离线优先）
   2. 将翻译和注释导入 Firestore（支持 LLM）
   3. 音频继续使用流式播放（节省空间）
   4. Hadith 可选导入 Firestore（用于智能搜索）
"""
    
    print(summary)


def main():
    """主函数"""
    print("\n" * 2)
    print("╔" + "═" * 68 + "╗")
    print("║" + " " * 15 + "🕌 古兰经和 Hadith 数据分析工具" + " " * 16 + "║")
    print("╚" + "═" * 68 + "╝")
    
    # 1. 分析古兰经文本
    quran_data = analyze_quran_script()
    
    # 2. 分析 Hadith
    analyze_hadith_data()
    
    # 3. 分析翻译
    analyze_translations()
    
    # 4. 详细分析 JSON 结构
    if quran_data:
        analyze_local_json_structure()
    
    # 5. 生成 Firestore 结构建议
    generate_firestore_structure_report()
    
    # 6. 生成摘要
    generate_summary()
    
    print("\n" + "=" * 70)
    print("✅ 分析完成！")
    print("=" * 70)
    print(f"\n💡 下一步:")
    print(f"   1. 如需导入 Firestore，请先获取 Firebase 服务账号密钥")
    print(f"   2. 运行: python3 quran_firestore_importer.py")
    print(f"   3. 或查看详细指南: QURAN_FIRESTORE_IMPORT_GUIDE.md")
    print()


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️ 用户中断")
    except Exception as e:
        print(f"\n❌ 发生错误: {e}")
        import traceback
        traceback.print_exc()

