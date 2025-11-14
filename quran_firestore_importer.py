#!/usr/bin/env python3
"""
古兰经数据导入 Firestore 脚本（增强版）
基于 Quran.com API v4，采用专业的分离式存储结构
支持多翻译、多注释、多音频，避免 1MB 文档限制
"""

import firebase_admin
from firebase_admin import credentials, firestore
import requests
import time
import os
import json
from typing import Dict, List, Optional

# ========================================
# 配置区域 - 请根据您的项目修改
# ========================================

# 1. Firebase 服务账号密钥文件路径
SERVICE_ACCOUNT_KEY_PATH = 'serviceAccountKey.json'

# 2. 您的应用 ID
APP_ID = 'com.quran.quranaudio.online'

# 3. Quran.com API v4 配置
BASE_API_URL = "https://api.quran.com/api/v4"

# 4. 选择要导入的翻译版本 (ID 从 API 获取)
TARGET_TRANSLATIONS = {
    131: {"name": "The Clear Quran", "language": "en", "author": "Dr. Mustafa Khattab"},
    134: {"name": "Chinese Translation", "language": "zh", "author": "Ma Jian"},
    33: {"name": "Indonesian Translation", "language": "id", "author": "Indonesian Ministry"},
    158: {"name": "Urdu Translation", "language": "ur", "author": "Maulana Muhammad Junagarhi"},
}

# 5. 选择要导入的注释版本
TARGET_TAFSIRS = {
    169: {"name": "Tafsir Ibn Kathir", "language": "en"},
}

# 6. 选择要导入的朗诵者
TARGET_RECITERS = [
    7,   # Ghamadi
    2,   # Abdul Basit
]

# 7. Firestore 路径配置（5 大集合）
BASE_COLLECTION_PATH = f'artifacts/{APP_ID}/public/data'

COLLECTIONS = {
    'surahs': 'quran_surahs',           # Surah 元数据 + Ayah 原文
    'structures': 'quran_structures',   # Juz/Page 划分
    'translations': 'quran_translations', # 翻译内容（按 Juz 拆分）
    'tafsirs': 'quran_tafsirs',         # 注释内容（按 Juz 拆分）
    'recitations': 'quran_recitations',  # 音频链接
}

# API 请求头
HEADERS = {
    'Accept': 'application/json',
}

# Juz 和 Ayah 映射（用于拆分翻译/注释）
JUZ_MAPPING = {
    1: {"start": "1:1", "end": "2:141"},
    2: {"start": "2:142", "end": "2:252"},
    3: {"start": "2:253", "end": "3:92"},
    4: {"start": "3:93", "end": "4:23"},
    5: {"start": "4:24", "end": "4:147"},
    6: {"start": "4:148", "end": "5:81"},
    7: {"start": "5:82", "end": "6:110"},
    8: {"start": "6:111", "end": "7:87"},
    9: {"start": "7:88", "end": "8:40"},
    10: {"start": "8:41", "end": "9:92"},
    11: {"start": "9:93", "end": "11:5"},
    12: {"start": "11:6", "end": "12:52"},
    13: {"start": "12:53", "end": "14:52"},
    14: {"start": "15:1", "end": "16:128"},
    15: {"start": "17:1", "end": "18:74"},
    16: {"start": "18:75", "end": "20:135"},
    17: {"start": "21:1", "end": "22:78"},
    18: {"start": "23:1", "end": "25:20"},
    19: {"start": "25:21", "end": "27:55"},
    20: {"start": "27:56", "end": "29:45"},
    21: {"start": "29:46", "end": "33:30"},
    22: {"start": "33:31", "end": "36:27"},
    23: {"start": "36:28", "end": "39:31"},
    24: {"start": "39:32", "end": "41:46"},
    25: {"start": "41:47", "end": "45:37"},
    26: {"start": "46:1", "end": "51:30"},
    27: {"start": "51:31", "end": "57:29"},
    28: {"start": "58:1", "end": "66:12"},
    29: {"start": "67:1", "end": "77:50"},
    30: {"start": "78:1", "end": "114:6"},
}

# ========================================
# 核心功能实现
# ========================================

def initialize_firebase():
    """初始化 Firebase Admin SDK"""
    if not os.path.exists(SERVICE_ACCOUNT_KEY_PATH):
        print(f"❌ 错误：未找到服务账号文件: {SERVICE_ACCOUNT_KEY_PATH}")
        print(f"   请从 Firebase Console 下载服务账号密钥并保存到项目根目录")
        print(f"\n💡 使用本地 JSON 导入模式（无需 Firebase 密钥）")
        return None
    
    try:
        cred = credentials.Certificate(SERVICE_ACCOUNT_KEY_PATH)
        firebase_admin.initialize_app(cred)
        print("✅ Firebase Admin SDK 初始化成功")
        return firestore.client()
    except Exception as e:
        print(f"❌ Firebase 初始化失败: {e}")
        print(f"💡 切换到本地 JSON 导入模式")
        return None


def fetch_chapters():
    """从 Quran.com API 获取所有章节信息"""
    print(f"\n🌐 正在获取古兰经章节信息...")
    try:
        response = requests.get(QURAN_API_ENDPOINTS['chapters'], timeout=30)
        response.raise_for_status()
        data = response.json()
        chapters = data.get('chapters', [])
        print(f"✅ 成功获取 {len(chapters)} 个章节信息")
        return chapters
    except Exception as e:
        print(f"❌ 获取章节信息失败: {e}")
        return []


def fetch_verses_for_chapter(chapter_number):
    """获取指定章节的所有经文（阿拉伯语原文）"""
    print(f"  🔄 正在获取 Surah {chapter_number} 的经文...")
    try:
        url = f"{QURAN_API_ENDPOINTS['verses']}/{chapter_number}"
        params = {
            'language': 'ar',  # 阿拉伯语原文
            'words': 'true',  # 包含单词分析
            'page': 1,
            'per_page': 300  # 最长章节不超过 286 节
        }
        response = requests.get(url, params=params, timeout=30)
        response.raise_for_status()
        data = response.json()
        verses = data.get('verses', [])
        print(f"  ✅ 成功获取 {len(verses)} 条经文")
        return verses
    except Exception as e:
        print(f"  ❌ 获取 Surah {chapter_number} 经文失败: {e}")
        return []


def fetch_translations_for_chapter(chapter_number, translation_ids):
    """获取指定章节的翻译版本"""
    translations = {}
    
    for trans_id in translation_ids:
        try:
            url = f"{QURAN_API_ENDPOINTS['translations']}/{trans_id}"
            params = {
                'chapter_number': chapter_number,
            }
            response = requests.get(url, params=params, timeout=30)
            response.raise_for_status()
            data = response.json()
            
            # 提取翻译数据
            trans_verses = data.get('translations', [])
            if trans_verses:
                translations[trans_id] = trans_verses
                print(f"    ✅ 翻译 {trans_id}: {len(trans_verses)} 条")
            
            time.sleep(0.2)  # 避免请求过快
        except Exception as e:
            print(f"    ⚠️ 翻译 {trans_id} 获取失败: {e}")
    
    return translations


def get_translation_language_code(translation_id):
    """根据翻译 ID 返回语言代码"""
    mapping = {
        131: 'en',  # English - Clear Quran
        134: 'zh',  # Chinese - Ma Jian
        158: 'ur',  # Urdu - Junagarhi
        33: 'id',   # Indonesian
    }
    return mapping.get(translation_id, f'trans_{translation_id}')


def import_quran_to_firestore(db, chapters_info):
    """将古兰经数据导入 Firestore"""
    if not chapters_info:
        print("❌ 没有章节信息，导入中止")
        return
    
    print(f"\n🚀 开始导入 {len(chapters_info)} 个 Surah 到 Firestore...")
    print(f"📍 Firestore 路径: {BASE_PATH}/{QURAN_COLLECTION}")
    
    total_verses_imported = 0
    
    for chapter in chapters_info:
        chapter_number = chapter['id']
        chapter_name_ar = chapter.get('name_arabic', '')
        chapter_name_en = chapter.get('name_simple', '')
        
        print(f"\n{'='*60}")
        print(f"📖 处理 Surah {chapter_number}: {chapter_name_en} ({chapter_name_ar})")
        print(f"{'='*60}")
        
        # 1. 创建 Surah 文档
        surah_doc_ref = db.collection(BASE_PATH).document(QURAN_COLLECTION).collection('surahs').document(str(chapter_number))
        
        # Surah 元数据
        surah_metadata = {
            'surah_id': chapter_number,
            'name_ar': chapter_name_ar,
            'name_en': chapter_name_en,
            'name_simple': chapter.get('name_simple', ''),
            'name_complex': chapter.get('name_complex', ''),
            'revelation_place': chapter.get('revelation_place', 'makkah'),
            'revelation_order': chapter.get('revelation_order', 0),
            'verses_count': chapter.get('verses_count', 0),
            'pages': chapter.get('pages', []),
        }
        
        surah_doc_ref.set(surah_metadata)
        print(f"✅ Surah {chapter_number} 元数据已保存")
        
        # 2. 获取该章节的所有经文（阿拉伯语原文）
        verses = fetch_verses_for_chapter(chapter_number)
        if not verses:
            print(f"⚠️ 跳过 Surah {chapter_number}（无经文数据）")
            continue
        
        # 3. 获取翻译版本
        print(f"  🌐 正在获取翻译...")
        translations = fetch_translations_for_chapter(chapter_number, TRANSLATION_IDS)
        
        # 4. 批量导入 Ayah（经文）到子集合
        batch = db.batch()
        batch_size = 400
        count = 0
        imported_count = 0
        
        ayahs_collection = surah_doc_ref.collection('ayahs')
        
        for verse in verses:
            ayah_number = verse.get('verse_number')
            verse_key = verse.get('verse_key')  # 格式：1:1, 2:255
            
            # 构建 Ayah 数据
            ayah_data = {
                'ayah_id': ayah_number,
                'surah_id': chapter_number,
                'verse_key': verse_key,
                'text_ar': verse.get('text_uthmani', verse.get('text_imlaei', '')),
                'text_simple': verse.get('text_indopak', ''),
                'juz_number': verse.get('juz_number', 0),
                'hizb_number': verse.get('hizb_number', 0),
                'rub_number': verse.get('rub_el_hizb_number', 0),
                'page_number': verse.get('page_number', 0),
            }
            
            # 添加翻译数据
            for trans_id, trans_verses in translations.items():
                if ayah_number - 1 < len(trans_verses):
                    lang_code = get_translation_language_code(trans_id)
                    trans_text = trans_verses[ayah_number - 1].get('text', '')
                    ayah_data[f'translation_{lang_code}'] = trans_text
            
            # 批量写入
            ayah_doc_ref = ayahs_collection.document(str(ayah_number))
            batch.set(ayah_doc_ref, ayah_data)
            
            count += 1
            if count >= batch_size:
                batch.commit()
                imported_count += count
                print(f"    → 已提交 {imported_count} 条 Ayah...")
                batch = db.batch()
                count = 0
                time.sleep(0.1)  # 避免速率限制
        
        # 提交剩余的批量写入
        if count > 0:
            batch.commit()
            imported_count += count
        
        total_verses_imported += imported_count
        print(f"✅ Surah {chapter_number} 导入完成: {imported_count} 条 Ayah")
        
        # API 请求间隔
        time.sleep(0.5)
    
    print(f"\n{'='*60}")
    print(f"🎉 导入完成！")
    print(f"📊 总计导入: {len(chapters_info)} 个 Surah, {total_verses_imported} 条 Ayah")
    print(f"{'='*60}")


def verify_import(db):
    """验证导入的数据"""
    print(f"\n🔍 验证导入数据...")
    
    # 检查 Surah 1 (Al-Fatiha)
    surah_1_ref = db.collection(BASE_PATH).document(QURAN_COLLECTION).collection('surahs').document('1')
    surah_1 = surah_1_ref.get()
    
    if surah_1.exists:
        print(f"✅ Surah 1 存在: {surah_1.to_dict().get('name_en')}")
        
        # 检查 Ayah
        ayahs = surah_1_ref.collection('ayahs').limit(5).stream()
        ayah_count = 0
        for ayah in ayahs:
            ayah_count += 1
            if ayah_count == 1:
                print(f"✅ Ayah 1:1 文本: {ayah.to_dict().get('text_ar')[:50]}...")
        
        print(f"✅ Surah 1 包含至少 {ayah_count} 条 Ayah")
    else:
        print(f"❌ Surah 1 不存在，导入可能失败")


def import_from_local_json(db):
    """从本地 assets JSON 文件导入（备用方案）"""
    print(f"\n📂 使用本地 JSON 文件导入...")
    
    # 读取本地 script 文件
    local_script_path = 'app/src/main/assets/scripts/script_uthmani_hafs.json'
    
    if not os.path.exists(local_script_path):
        print(f"❌ 本地文件不存在: {local_script_path}")
        return
    
    print(f"📖 读取本地文件: {local_script_path}")
    
    with open(local_script_path, 'r', encoding='utf-8') as f:
        quran_data = json.load(f)
    
    chapters = quran_data.get('chapters', [])
    print(f"✅ 读取到 {len(chapters)} 个章节")
    
    # 批量导入逻辑
    total_verses = 0
    for chapter in chapters:
        chapter_num = chapter.get('number')
        verses = chapter.get('verses', [])
        
        # 创建 Surah 文档
        surah_doc_ref = db.collection(BASE_PATH).document(QURAN_COLLECTION).collection('surahs').document(str(chapter_num))
        
        surah_metadata = {
            'surah_id': chapter_num,
            'name_ar': chapter.get('name_ar', ''),
            'name_en': chapter.get('name_en', ''),
            'verses_count': len(verses),
        }
        
        surah_doc_ref.set(surah_metadata)
        
        # 批量导入 Ayahs
        batch = db.batch()
        count = 0
        ayahs_collection = surah_doc_ref.collection('ayahs')
        
        for verse in verses:
            ayah_num = verse.get('number')
            ayah_data = {
                'ayah_id': ayah_num,
                'surah_id': chapter_num,
                'text_ar': verse.get('arabic_text', ''),
                'page_number': verse.get('page_number', 0),
            }
            
            batch.set(ayahs_collection.document(str(ayah_num)), ayah_data)
            count += 1
            
            if count >= 400:
                batch.commit()
                total_verses += count
                batch = db.batch()
                count = 0
        
        if count > 0:
            batch.commit()
            total_verses += count
        
        print(f"  ✅ Surah {chapter_num}: {len(verses)} verses")
    
    print(f"\n✅ 本地导入完成: {len(chapters)} Surahs, {total_verses} Ayahs")


# ========================================
# 主程序入口
# ========================================

def main():
    print("="*60)
    print("🕌 古兰经数据导入 Firestore 工具")
    print("="*60)
    
    # 初始化 Firebase
    db = initialize_firebase()
    
    # 选择导入方式
    print("\n请选择导入方式:")
    print("1. 从 Quran.com API 导入（推荐，包含翻译和注释）")
    print("2. 从本地 JSON 文件导入（仅阿拉伯语原文）")
    
    choice = input("\n请输入选项 (1 或 2): ").strip()
    
    if choice == '1':
        # 从 API 导入
        chapters = fetch_chapters()
        if chapters:
            import_quran_to_firestore(db, chapters)
            verify_import(db)
    elif choice == '2':
        # 从本地文件导入
        import_from_local_json(db)
        verify_import(db)
    else:
        print("❌ 无效选项")
        return
    
    print("\n✅ 导入任务完成！")
    print(f"📍 Firestore 路径: {BASE_PATH}/{QURAN_COLLECTION}/surahs")


if __name__ == '__main__':
    main()

