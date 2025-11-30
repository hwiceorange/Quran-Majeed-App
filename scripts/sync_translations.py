#!/usr/bin/env python3
"""
下载和转换 Quran.com API 翻译数据

功能：
1. 从 Quran.com API 下载优先级 1 的 18 个翻译
2. 转换为应用数据库格式
3. 验证数据完整性（6236条经文）
4. 生成元数据文件

Usage:
    python sync_translations.py
"""

import requests
import json
import time
from pathlib import Path
from datetime import datetime
import re

# ═══════════════════════════════════════════════════════════════
# 配置
# ═══════════════════════════════════════════════════════════════

API_BASE_URL = "https://api.quran.com"
INPUT_FILE = Path(__file__).parent / "quran_api_data" / "translations_priority_1.json"
OUTPUT_DIR = Path(__file__).parent / "translation_data"
RAW_DATA_DIR = OUTPUT_DIR / "raw"
CONVERTED_DATA_DIR = OUTPUT_DIR / "converted"
RETRY_DELAY = 2
MAX_RETRIES = 3

# 预期的经文数量（按章节）
EXPECTED_VERSE_COUNTS = {
    1: 7, 2: 286, 3: 200, 4: 176, 5: 120, 6: 165, 7: 206, 8: 75, 9: 129, 10: 109,
    11: 123, 12: 111, 13: 43, 14: 52, 15: 99, 16: 128, 17: 111, 18: 110, 19: 98, 20: 135,
    21: 112, 22: 78, 23: 118, 24: 64, 25: 77, 26: 227, 27: 93, 28: 88, 29: 69, 30: 60,
    31: 34, 32: 30, 33: 73, 34: 54, 35: 45, 36: 83, 37: 182, 38: 88, 39: 75, 40: 85,
    41: 54, 42: 53, 43: 89, 44: 59, 45: 37, 46: 35, 47: 38, 48: 29, 49: 18, 50: 45,
    51: 60, 52: 49, 53: 62, 54: 55, 55: 78, 56: 96, 57: 29, 58: 22, 59: 24, 60: 13,
    61: 14, 62: 11, 63: 11, 64: 18, 65: 12, 66: 12, 67: 30, 68: 52, 69: 52, 70: 44,
    71: 28, 72: 28, 73: 20, 74: 56, 75: 40, 76: 31, 77: 50, 78: 40, 79: 46, 80: 42,
    81: 29, 82: 19, 83: 36, 84: 25, 85: 22, 86: 17, 87: 19, 88: 26, 89: 30, 90: 20,
    91: 15, 92: 21, 93: 11, 94: 8, 95: 8, 96: 19, 97: 5, 98: 8, 99: 8, 100: 11,
    101: 11, 102: 8, 103: 3, 104: 9, 105: 5, 106: 4, 107: 7, 108: 3, 109: 6, 110: 3,
    111: 5, 112: 4, 113: 5, 114: 6
}

TOTAL_VERSES = sum(EXPECTED_VERSE_COUNTS.values())  # 6236

# ═══════════════════════════════════════════════════════════════
# 日志函数
# ═══════════════════════════════════════════════════════════════

def log(message: str, level: str = "INFO"):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] [{level}] {message}")

# ═══════════════════════════════════════════════════════════════
# Slug 生成
# ═══════════════════════════════════════════════════════════════

def slugify(text: str) -> str:
    """转换为 slug 格式"""
    text = text.lower()
    text = re.sub(r'[^\w\s-]', '', text)
    text = re.sub(r'[-\s]+', '-', text)
    return text.strip('-')

def generate_app_slug(translation: dict) -> str:
    """
    生成应用使用的 slug 格式
    格式: {lang_code}_{id}_{name_slug}
    """
    lang_name = translation.get('language_name', 'unknown')
    
    # 语言代码映射
    lang_code_map = {
        'bengali': 'bn',
        'indonesian': 'id',
        'malay': 'ms',
        'turkish': 'tr',
        'urdu': 'ur',
        'english': 'en',
        'arabic': 'ar',
        'persian': 'fa',
        'french': 'fr',
        'german': 'de',
        'spanish': 'es',
        'russian': 'ru',
        'chinese': 'zh'
    }
    
    lang_code = lang_code_map.get(lang_name.lower(), lang_name[:2].lower())
    trans_id = translation.get('id')
    
    # 使用原始 slug（如果存在），否则从名称生成
    original_slug = translation.get('slug')
    if original_slug:
        name_slug = original_slug.split('-', 1)[-1] if '-' in original_slug else slugify(translation.get('name', ''))
    else:
        name_slug = slugify(translation.get('name', ''))
    
    return f"{lang_code}_{trans_id}_{name_slug}"

# ═══════════════════════════════════════════════════════════════
# API 请求
# ═══════════════════════════════════════════════════════════════

def fetch_with_retry(url: str, max_retries: int = MAX_RETRIES):
    """带重试机制的 API 请求"""
    for attempt in range(max_retries):
        try:
            log(f"Fetching: {url}")
            response = requests.get(url, timeout=60)
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException as e:
            log(f"Attempt {attempt + 1}/{max_retries} failed: {e}", "ERROR")
            if attempt < max_retries - 1:
                log(f"Retrying in {RETRY_DELAY} seconds...", "WARN")
                time.sleep(RETRY_DELAY)
            else:
                log(f"Max retries reached. Skipping.", "ERROR")
                return None

def download_translation(translation_id: int) -> dict:
    """
    下载单个翻译的完整经文
    使用 verses endpoint 可以获得包含章节和经文号的数据
    """
    # 使用 verses endpoint，它返回包含 chapter_number 和 verse_number 的数据
    url = f"{API_BASE_URL}/api/v4/verses/by_key/1:1?translations={translation_id}&per_page=6236"
    
    # 或者使用章节循环下载
    all_verses = []
    for chapter in range(1, 115):  # 1-114
        chapter_url = f"{API_BASE_URL}/api/v4/verses/by_chapter/{chapter}?translations={translation_id}"
        log(f"   下载第 {chapter}/114 章...")
        chapter_data = fetch_with_retry(chapter_url)
        
        if not chapter_data:
            return None
            
        verses = chapter_data.get('verses', [])
        all_verses.extend(verses)
        
        # 避免 API 限流
        if chapter < 114:
            time.sleep(0.5)
    
    return {'verses': all_verses}

# ═══════════════════════════════════════════════════════════════
# 数据转换
# ═══════════════════════════════════════════════════════════════

def convert_api_format_to_app_format(api_data: dict, translation_id: int) -> dict:
    """
    转换 Quran.com API 格式为应用数据库格式
    
    API 格式 (verses endpoint):
    {
      "verses": [
        {
          "id": 1,
          "verse_key": "1:1",
          "verse_number": 1,
          "translations": [
            {
              "id": 161,
              "resource_id": 161,
              "text": "..."
            }
          ]
        }
      ]
    }
    
    应用格式:
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
    """
    verses_list = api_data.get('verses', [])
    
    # 按章节组织数据
    chapters_dict = {}
    
    for verse_data in verses_list:
        verse_key = verse_data.get('verse_key', '')
        if not verse_key or ':' not in verse_key:
            continue
            
        chapter_no, verse_no = map(int, verse_key.split(':'))
        
        # 获取翻译文本
        translations = verse_data.get('translations', [])
        text = ''
        for trans in translations:
            if trans.get('resource_id') == translation_id:
                text = trans.get('text', '')
                break
        
        if chapter_no not in chapters_dict:
            chapters_dict[chapter_no] = {
                'number': chapter_no,
                'verses': []
            }
        
        chapters_dict[chapter_no]['verses'].append({
            'number': verse_no,
            'text': text,
            'footnotes': []
        })
    
    # 转换为列表并排序
    chapters = [chapters_dict[i] for i in sorted(chapters_dict.keys())]
    
    return {
        'chapters': chapters
    }

# ═══════════════════════════════════════════════════════════════
# 数据验证
# ═══════════════════════════════════════════════════════════════

def validate_translation_data(data: dict, translation_name: str) -> bool:
    """
    验证翻译数据完整性
    确保包含所有 6236 条经文
    """
    chapters = data.get('chapters', [])
    
    if len(chapters) != 114:
        log(f"❌ {translation_name}: Expected 114 chapters, found {len(chapters)}", "ERROR")
        return False
    
    total_verses = 0
    errors = []
    
    for chapter in chapters:
        chapter_no = chapter.get('number')
        verses = chapter.get('verses', [])
        verse_count = len(verses)
        expected_count = EXPECTED_VERSE_COUNTS.get(chapter_no, 0)
        
        if verse_count != expected_count:
            errors.append(f"Chapter {chapter_no}: expected {expected_count} verses, found {verse_count}")
        
        total_verses += verse_count
    
    if total_verses != TOTAL_VERSES:
        log(f"❌ {translation_name}: Expected {TOTAL_VERSES} verses, found {total_verses}", "ERROR")
        for error in errors[:5]:  # 只显示前5个错误
            log(f"   {error}", "ERROR")
        return False
    
    if errors:
        log(f"⚠️ {translation_name}: Found {len(errors)} chapter mismatches", "WARN")
        for error in errors[:3]:
            log(f"   {error}", "WARN")
    
    log(f"✅ {translation_name}: Validation passed ({total_verses} verses)")
    return True

# ═══════════════════════════════════════════════════════════════
# 主处理流程
# ═══════════════════════════════════════════════════════════════

def process_translation(translation: dict) -> dict:
    """处理单个翻译"""
    translation_id = translation.get('id')
    translation_name = translation.get('name')
    slug = generate_app_slug(translation)
    
    log("=" * 60)
    log(f"处理翻译: {translation_name}")
    log(f"ID: {translation_id}")
    log(f"生成的 Slug: {slug}")
    log(f"语言: {translation.get('language_name')}")
    log("=" * 60)
    
    # 检查是否已经下载和转换
    converted_file = CONVERTED_DATA_DIR / f"{slug}.json"
    if converted_file.exists():
        log(f"⏭️ 已下载并转换，跳过: {translation_name}")
        # 读取已有的元数据
        with open(converted_file, 'r', encoding='utf-8') as f:
            data = json.load(f)
        
        metadata = {
            'slug': slug,
            'translation_id': translation_id,
            'language_code': translation.get('language_name', 'unknown')[:2].lower(),
            'language_name': translation.get('language_name'),
            'book_name': translation.get('name'),
            'author_name': translation.get('author_name'),
            'display_name': translation.get('name'),
            'download_path': f"https://api.quran.com/api/v4/quran/translations/{translation_id}",
            'original_slug': translation.get('slug'),
            'is_prebuilt': False,
            'total_verses': TOTAL_VERSES,
            'synced_at': datetime.now().isoformat()
        }
        return metadata
    
    # 1. 下载原始数据
    log("📥 下载原始数据...")
    raw_data = download_translation(translation_id)
    
    if not raw_data:
        log(f"❌ 下载失败: {translation_name}", "ERROR")
        return None
    
    # 保存原始数据
    raw_file = RAW_DATA_DIR / f"{slug}.json"
    raw_file.parent.mkdir(parents=True, exist_ok=True)
    with open(raw_file, 'w', encoding='utf-8') as f:
        json.dump(raw_data, f, ensure_ascii=False, indent=2)
    log(f"💾 原始数据已保存: {raw_file}")
    
    # 2. 转换格式
    log("🔄 转换数据格式...")
    converted_data = convert_api_format_to_app_format(raw_data, translation_id)
    
    # 3. 验证数据
    log("✓ 验证数据完整性...")
    if not validate_translation_data(converted_data, translation_name):
        log(f"❌ 验证失败: {translation_name}", "ERROR")
        return None
    
    # 保存转换后的数据
    converted_file = CONVERTED_DATA_DIR / f"{slug}.json"
    converted_file.parent.mkdir(parents=True, exist_ok=True)
    with open(converted_file, 'w', encoding='utf-8') as f:
        json.dump(converted_data, f, ensure_ascii=False, indent=2)
    log(f"💾 转换后数据已保存: {converted_file}")
    
    # 4. 生成元数据
    metadata = {
        'slug': slug,
        'translation_id': translation_id,
        'language_code': translation.get('language_name', 'unknown')[:2].lower(),
        'language_name': translation.get('language_name'),
        'book_name': translation.get('name'),
        'author_name': translation.get('author_name'),
        'display_name': translation.get('name'),
        'download_path': f"https://api.quran.com/api/v4/quran/translations/{translation_id}",
        'original_slug': translation.get('slug'),
        'is_prebuilt': False,  # 按需下载，不预装
        'total_verses': TOTAL_VERSES,
        'synced_at': datetime.now().isoformat()
    }
    
    log(f"✅ 处理完成: {translation_name}")
    log("")
    
    return metadata

# ═══════════════════════════════════════════════════════════════
# 主函数
# ═══════════════════════════════════════════════════════════════

def main():
    log("🚀 开始同步翻译数据...")
    log("")
    
    # 创建输出目录
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    RAW_DATA_DIR.mkdir(parents=True, exist_ok=True)
    CONVERTED_DATA_DIR.mkdir(parents=True, exist_ok=True)
    
    # 读取优先级 1 翻译列表
    if not INPUT_FILE.exists():
        log(f"❌ 输入文件不存在: {INPUT_FILE}", "ERROR")
        return
    
    with open(INPUT_FILE, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    translations = data.get('translations', [])
    log(f"📋 找到 {len(translations)} 个优先级 1 翻译")
    log("")
    
    # 处理每个翻译
    all_metadata = []
    success_count = 0
    fail_count = 0
    
    for i, translation in enumerate(translations, 1):
        log(f"进度: {i}/{len(translations)}")
        
        metadata = process_translation(translation)
        
        if metadata:
            all_metadata.append(metadata)
            success_count += 1
        else:
            fail_count += 1
        
        # 避免 API 限流
        if i < len(translations):
            time.sleep(1)
    
    # 保存汇总元数据
    metadata_file = OUTPUT_DIR / "metadata.json"
    with open(metadata_file, 'w', encoding='utf-8') as f:
        json.dump({
            'synced_at': datetime.now().isoformat(),
            'total_translations': len(all_metadata),
            'success_count': success_count,
            'fail_count': fail_count,
            'translations': all_metadata
        }, f, ensure_ascii=False, indent=2)
    
    log("=" * 60)
    log("📊 同步完成统计")
    log("=" * 60)
    log(f"✅ 成功: {success_count}")
    log(f"❌ 失败: {fail_count}")
    log(f"📁 原始数据: {RAW_DATA_DIR}")
    log(f"📁 转换数据: {CONVERTED_DATA_DIR}")
    log(f"📄 元数据: {metadata_file}")
    log("=" * 60)
    
    if success_count == len(translations):
        log("🎉 所有翻译同步成功！")
    else:
        log(f"⚠️ 有 {fail_count} 个翻译同步失败，请检查日志", "WARN")

if __name__ == "__main__":
    main()

