#!/usr/bin/env python3
"""
古兰经专业数据导入 Firestore 脚本
基于 Quran.com API v4，采用分离式存储结构
支持多翻译、多注释、按 Juz 拆分，避免 1MB 文档限制
"""

import firebase_admin
from firebase_admin import credentials, firestore
import requests
import time
import os
import json
from typing import Dict, List, Optional
from collections import defaultdict

# ========================================
# 配置区域
# ========================================

SERVICE_ACCOUNT_KEY_PATH = 'serviceAccountKey.json'
APP_ID = 'com.quran.quranaudio.online'
BASE_API_URL = "https://api.quran.com/api/v4"

# 选择要导入的翻译（ID 可从 API 查询）
TARGET_TRANSLATIONS = {
    131: {"name": "The Clear Quran", "language": "en", "author": "Dr. Mustafa Khattab"},
    134: {"name": "Chinese Translation", "language": "zh", "author": "Ma Jian"},
    33: {"name": "Indonesian Translation", "language": "id", "author": "Indonesian Ministry"},
    158: {"name": "Urdu Translation", "language": "ur", "author": "Maulana Muhammad Junagarhi"},
}

# Firestore 路径
BASE_COLLECTION_PATH = f'artifacts/{APP_ID}/public/data'

# Juz 边界映射（用于拆分翻译和注释）
JUZ_BOUNDARIES = {
    1: (1, 1, 2, 141), 2: (2, 142, 2, 252), 3: (2, 253, 3, 92),
    4: (3, 93, 4, 23), 5: (4, 24, 4, 147), 6: (4, 148, 5, 81),
    7: (5, 82, 6, 110), 8: (6, 111, 7, 87), 9: (7, 88, 8, 40),
    10: (8, 41, 9, 92), 11: (9, 93, 11, 5), 12: (11, 6, 12, 52),
    13: (12, 53, 14, 52), 14: (15, 1, 16, 128), 15: (17, 1, 18, 74),
    16: (18, 75, 20, 135), 17: (21, 1, 22, 78), 18: (23, 1, 25, 20),
    19: (25, 21, 27, 55), 20: (27, 56, 29, 45), 21: (29, 46, 33, 30),
    22: (33, 31, 36, 27), 23: (36, 28, 39, 31), 24: (39, 32, 41, 46),
    25: (41, 47, 45, 37), 26: (46, 1, 51, 30), 27: (51, 31, 57, 29),
    28: (58, 1, 66, 12), 29: (67, 1, 77, 50), 30: (78, 1, 114, 6),
}

# ========================================
# 工具函数
# ========================================

def initialize_firebase():
    """初始化 Firebase"""
    if not os.path.exists(SERVICE_ACCOUNT_KEY_PATH):
        print(f"❌ 未找到 Firebase 密钥: {SERVICE_ACCOUNT_KEY_PATH}")
        print(f"   请从 Firebase Console 下载服务账号密钥")
        print(f"   或使用本地模式（选项 2）")
        return None
    
    try:
        cred = credentials.Certificate(SERVICE_ACCOUNT_KEY_PATH)
        firebase_admin.initialize_app(cred)
        print("✅ Firebase 初始化成功")
        return firestore.client()
    except Exception as e:
        print(f"❌ Firebase 初始化失败: {e}")
        return None


def fetch_api(url, params=None, max_retries=3):
    """通用 API 请求函数（带重试）"""
    for attempt in range(max_retries):
        try:
            response = requests.get(url, params=params, timeout=30)
            response.raise_for_status()
            return response.json()
        except Exception as e:
            print(f"  ⚠️ API 请求失败 (尝试 {attempt + 1}/{max_retries}): {e}")
            if attempt < max_retries - 1:
                time.sleep(2 ** attempt)
    return None


def get_juz_for_verse(surah_id, ayah_id):
    """根据 Surah 和 Ayah 编号返回 Juz 编号"""
    for juz_num, (start_surah, start_ayah, end_surah, end_ayah) in JUZ_BOUNDARIES.items():
        if (surah_id, ayah_id) >= (start_surah, start_ayah) and (surah_id, ayah_id) <= (end_surah, end_ayah):
            return juz_num
    return 30  # 默认返回最后一个 Juz


# ========================================
# 导入函数 1: Surah 元数据和 Ayah 原文
# ========================================

def import_surahs_and_ayahs(db):
    """导入 Surah 元数据和 Ayah 阿拉伯语原文"""
    print("\n" + "="*70)
    print("📖 步骤 1: 导入 Surah 元数据和 Ayah 原文")
    print("="*70)
    
    # 1. 获取所有章节信息
    chapters_data = fetch_api(f'{BASE_API_URL}/chapters')
    if not chapters_data or 'chapters' not in chapters_data:
        print("❌ 无法获取章节列表")
        return False
    
    chapters = chapters_data['chapters']
    print(f"✅ 获取到 {len(chapters)} 个章节信息")
    
    surahs_collection = db.collection(f'{BASE_COLLECTION_PATH}/quran_surahs')
    total_ayahs = 0
    
    for chapter in chapters:
        surah_id = str(chapter['id'])
        print(f"\n📖 Surah {surah_id}: {chapter['name_simple']}")
        
        # 2. 保存 Surah 元数据
        surah_doc = surahs_collection.document(surah_id)
        surah_doc.set({
            'surah_id': chapter['id'],
            'name_ar': chapter['name_arabic'],
            'name_en': chapter['name_simple'],
            'name_complex': chapter['name_complex'],
            'revelation_place': chapter['revelation_place'],
            'revelation_order': chapter['revelation_order'],
            'verses_count': chapter['verses_count'],
            'pages': chapter.get('pages', []),
        })
        
        # 3. 获取该章节的所有经文
        verses_data = fetch_api(
            f'{BASE_API_URL}/verses/by_chapter/{surah_id}',
            params={'words': 'false', 'per_page': 300}
        )
        
        if not verses_data or 'verses' not in verses_data:
            print(f"  ⚠️ 未获取到 Surah {surah_id} 的经文")
            continue
        
        verses = verses_data['verses']
        
        # 4. 批量保存 Ayah
        batch = db.batch()
        count = 0
        ayahs_collection = surah_doc.collection('ayahs')
        
        for verse in verses:
            ayah_id = str(verse['verse_number'])
            
            ayah_data = {
                'ayah_id': verse['verse_number'],
                'surah_id': verse['chapter_id'],
                'verse_key': verse['verse_key'],
                'text_uthmani': verse.get('text_uthmani', ''),
                'text_imlaei': verse.get('text_imlaei', ''),
                'juz_number': verse['juz_number'],
                'hizb_number': verse['hizb_number'],
                'rub_el_hizb': verse['rub_el_hizb_number'],
                'page_number': verse['page_number'],
            }
            
            batch.set(ayahs_collection.document(ayah_id), ayah_data)
            count += 1
            
            if count >= 400:
                batch.commit()
                batch = db.batch()
                count = 0
        
        if count > 0:
            batch.commit()
        
        total_ayahs += len(verses)
        print(f"  ✅ 导入 {len(verses)} 条 Ayah")
        
        time.sleep(0.3)  # API 限流
    
    print(f"\n✅ 步骤 1 完成: {len(chapters)} 个 Surah, {total_ayahs} 条 Ayah")
    return True


# ========================================
# 导入函数 2: Juz 结构
# ========================================

def import_juz_structure(db):
    """导入 Juz 结构信息"""
    print("\n" + "="*70)
    print("📚 步骤 2: 导入 Juz 结构")
    print("="*70)
    
    juzs_data = fetch_api(f'{BASE_API_URL}/juzs')
    if not juzs_data or 'juzs' not in juzs_data:
        print("❌ 无法获取 Juz 列表")
        return False
    
    structures_collection = db.collection(f'{BASE_COLLECTION_PATH}/quran_structures')
    
    for juz in juzs_data['juzs']:
        juz_id = str(juz['id'])
        
        juz_doc = {
            'juz_number': juz['id'],
            'first_verse_key': juz['verse_mapping'].get('first_verse_key', ''),
            'last_verse_key': juz['verse_mapping'].get('last_verse_key', ''),
        }
        
        structures_collection.document(juz_id).set(juz_doc)
        print(f"  ✅ Juz {juz_id}: {juz_doc['first_verse_key']} - {juz_doc['last_verse_key']}")
    
    print(f"\n✅ 步骤 2 完成: {len(juzs_data['juzs'])} 个 Juz")
    return True


# ========================================
# 导入函数 3: 翻译（按 Juz 拆分）
# ========================================

def import_translations(db):
    """导入翻译（按 Juz 拆分避免 1MB 限制）"""
    print("\n" + "="*70)
    print("🌐 步骤 3: 导入翻译（按 Juz 拆分）")
    print("="*70)
    
    translations_collection = db.collection(f'{BASE_COLLECTION_PATH}/quran_translations')
    
    for trans_id, meta in TARGET_TRANSLATIONS.items():
        print(f"\n📗 翻译 {trans_id}: {meta['name']} ({meta['language']})")
        
        # 获取完整翻译
        print(f"  🌐 获取翻译数据...")
        trans_data = fetch_api(f'{BASE_API_URL}/quran/translations/{trans_id}')
        
        if not trans_data or 'translations' not in trans_data:
            print(f"  ❌ 获取失败")
            continue
        
        translations = trans_data['translations']
        print(f"  ✅ 获取 {len(translations)} 条翻译")
        
        # 按 Juz 分组
        juz_texts = defaultdict(dict)
        
        for trans in translations:
            verse_key = trans['verse_key']
            surah_id, ayah_id = map(int, verse_key.split(':'))
            juz_num = get_juz_for_verse(surah_id, ayah_id)
            juz_texts[juz_num][verse_key] = trans['text']
        
        # 保存每个 Juz 的翻译
        for juz_num, texts in juz_texts.items():
            doc_id = f"{trans_id}_juz_{juz_num}"
            doc_data = {
                'translation_id': trans_id,
                'name': meta['name'],
                'language': meta['language'],
                'author': meta['author'],
                'juz_number': juz_num,
                'texts': texts,
            }
            
            translations_collection.document(doc_id).set(doc_data)
            print(f"  ✅ Juz {juz_num}: {len(texts)} 条翻译")
    
    print(f"\n✅ 步骤 3 完成: {len(TARGET_TRANSLATIONS)} 种翻译 × 30 Juz")
    return True


# ========================================
# 导入函数 4: 从本地 JSON 导入
# ========================================

def import_from_local_json(db):
    """从本地 assets JSON 文件导入（快速模式）"""
    print("\n" + "="*70)
    print("📂 本地模式: 从 assets JSON 导入")
    print("="*70)
    
    script_path = 'app/src/main/assets/scripts/script_uthmani_hafs.json'
    
    if not os.path.exists(script_path):
        print(f"❌ 文件不存在: {script_path}")
        return False
    
    print(f"📖 读取: {script_path}")
    
    with open(script_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    suras = data.get('suras', [])
    print(f"✅ 解析到 {len(suras)} 个章节")
    
    # 导入数据
    surahs_collection = db.collection(f'{BASE_COLLECTION_PATH}/quran_surahs')
    total_ayahs = 0
    
    for sura in suras:
        surah_id = str(sura['index'])
        ayahs = sura.get('ayas', [])
        
        print(f"\n📖 Surah {surah_id}: {len(ayahs)} ayahs")
        
        # Surah 元数据
        surah_doc = surahs_collection.document(surah_id)
        surah_doc.set({
            'surah_id': sura['index'],
            'verses_count': len(ayahs),
        })
        
        # Ayahs
        batch = db.batch()
        count = 0
        ayahs_collection = surah_doc.collection('ayahs')
        
        for aya in ayahs:
            ayah_id = str(aya['index'])
            verse_key = f"{sura['index']}:{aya['index']}"
            
            ayah_data = {
                'ayah_id': aya['index'],
                'surah_id': sura['index'],
                'verse_key': verse_key,
                'text': aya['text'],
                'end': aya.get('end', ''),
            }
            
            batch.set(ayahs_collection.document(ayah_id), ayah_data)
            count += 1
            total_ayahs += 1
            
            if count >= 400:
                batch.commit()
                batch = db.batch()
                count = 0
        
        if count > 0:
            batch.commit()
        
        print(f"  ✅ 导入完成")
    
    print(f"\n✅ 本地导入完成: {len(suras)} Surahs, {total_ayahs} Ayahs")
    return True


# ========================================
# 主程序
# ========================================

def main():
    print("\n" + "="*70)
    print("🕌 古兰经专业数据导入工具 (Firestore)")
    print("="*70)
    
    # 初始化 Firebase
    db = initialize_firebase()
    
    if not db:
        print("\n⚠️ Firebase 未初始化")
        print("💡 请确保 serviceAccountKey.json 存在于项目根目录")
        print("💡 或从 Firebase Console 下载: https://console.firebase.google.com/")
        return
    
    print("\n请选择导入模式:")
    print("1. 从 Quran.com API 导入（完整数据 + 翻译，需网络，~30-60 分钟）")
    print("2. 从本地 JSON 导入（仅原文，快速，~5 分钟）")
    
    choice = input("\n请输入选项 (1 或 2，直接回车默认 2): ").strip() or '2'
    
    if choice == '1':
        print("\n🌐 API 模式启动...")
        
        # 1. 导入 Surahs 和 Ayahs
        if not import_surahs_and_ayahs(db):
            print("\n❌ Surah/Ayah 导入失败，终止")
            return
        
        # 2. 导入 Juz 结构
        if not import_juz_structure(db):
            print("\n⚠️ Juz 结构导入失败，跳过")
        
        # 3. 导入翻译
        if not import_translations(db):
            print("\n⚠️ 翻译导入失败，跳过")
        
        print("\n✅ API 模式导入完成！")
        
    elif choice == '2':
        print("\n📂 本地模式启动...")
        
        if not import_from_local_json(db):
            print("\n❌ 本地导入失败")
            return
        
        print("\n✅ 本地模式导入完成！")
    
    else:
        print("❌ 无效选项")
        return
    
    # 验证导入
    print("\n" + "="*70)
    print("🔍 验证导入结果")
    print("="*70)
    
    verify_data(db)
    
    print("\n" + "="*70)
    print("🎉 导入任务完成！")
    print("="*70)
    print(f"\n📍 Firestore 路径: {BASE_COLLECTION_PATH}/quran_surahs")
    print(f"💡 可在 Firebase Console 查看: https://console.firebase.google.com/")


def verify_data(db):
    """验证导入的数据"""
    try:
        # 检查 Surah 1
        surah_1 = db.collection(f'{BASE_COLLECTION_PATH}/quran_surahs').document('1').get()
        
        if surah_1.exists:
            data = surah_1.to_dict()
            print(f"✅ Surah 1 存在: {data.get('name_en', 'N/A')}")
            print(f"   verses_count: {data.get('verses_count', 0)}")
            
            # 检查 Ayah
            ayahs = db.collection(f'{BASE_COLLECTION_PATH}/quran_surahs/1/ayahs').limit(3).stream()
            ayah_count = 0
            for ayah in ayahs:
                ayah_count += 1
                if ayah_count == 1:
                    ayah_data = ayah.to_dict()
                    text = ayah_data.get('text_uthmani', ayah_data.get('text', ''))
                    print(f"   Ayah 1: {text[:50]}...")
            
            print(f"   包含至少 {ayah_count} 条 Ayah")
        else:
            print("❌ Surah 1 不存在")
            
    except Exception as e:
        print(f"⚠️ 验证失败: {e}")


if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️ 用户中断")
    except Exception as e:
        print(f"\n❌ 发生错误: {e}")
        import traceback
        traceback.print_exc()

