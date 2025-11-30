#!/usr/bin/env python3
"""
获取 Quran.com API 的所有可用翻译和 Tafsir 资源

API 文档: https://api-docs.quran.com/docs/category/quran

Usage:
    python fetch_quran_api_resources.py
"""

import requests
import json
import time
from pathlib import Path
from datetime import datetime

# ═══════════════════════════════════════════════════════════════
# 配置
# ═══════════════════════════════════════════════════════════════

API_BASE_URL = "https://api.quran.com"
OUTPUT_DIR = Path(__file__).parent / "quran_api_data"
RETRY_DELAY = 2  # 秒
MAX_RETRIES = 3

# ═══════════════════════════════════════════════════════════════
# 日志函数
# ═══════════════════════════════════════════════════════════════

def log(message: str, level: str = "INFO"):
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] [{level}] {message}")

# ═══════════════════════════════════════════════════════════════
# API 请求函数
# ═══════════════════════════════════════════════════════════════

def fetch_with_retry(url: str, max_retries: int = MAX_RETRIES):
    """带重试机制的 API 请求"""
    for attempt in range(max_retries):
        try:
            log(f"Fetching: {url}")
            response = requests.get(url, timeout=30)
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

# ═══════════════════════════════════════════════════════════════
# 1. 获取所有翻译资源
# ═══════════════════════════════════════════════════════════════

def fetch_all_translations():
    """获取所有可用的古兰经翻译"""
    log("=" * 60)
    log("📖 获取所有翻译资源...")
    log("=" * 60)
    
    url = f"{API_BASE_URL}/api/v4/resources/translations"
    data = fetch_with_retry(url)
    
    if not data:
        log("Failed to fetch translations", "ERROR")
        return None
    
    translations = data.get("translations", [])
    log(f"✅ 获取到 {len(translations)} 个翻译版本")
    
    # 按语言分组统计
    lang_stats = {}
    for trans in translations:
        lang = trans.get("language_name", "unknown")
        lang_stats[lang] = lang_stats.get(lang, 0) + 1
    
    log("\n📊 语言分布:")
    for lang, count in sorted(lang_stats.items(), key=lambda x: x[1], reverse=True)[:20]:
        log(f"   {lang}: {count} 个版本")
    
    # 保存完整数据
    output_file = OUTPUT_DIR / "translations_all.json"
    output_file.parent.mkdir(parents=True, exist_ok=True)
    
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    log(f"💾 已保存到: {output_file}")
    
    return translations

# ═══════════════════════════════════════════════════════════════
# 2. 获取所有 Tafsir 资源
# ═══════════════════════════════════════════════════════════════

def fetch_all_tafsirs():
    """获取所有可用的古兰经注释"""
    log("=" * 60)
    log("📝 获取所有 Tafsir 资源...")
    log("=" * 60)
    
    url = f"{API_BASE_URL}/api/v4/resources/tafsirs"
    data = fetch_with_retry(url)
    
    if not data:
        log("Failed to fetch tafsirs", "ERROR")
        return None
    
    tafsirs = data.get("tafsirs", [])
    log(f"✅ 获取到 {len(tafsirs)} 个 Tafsir 版本")
    
    # 按语言分组统计
    lang_stats = {}
    for tafsir in tafsirs:
        lang = tafsir.get("language_name", "unknown")
        lang_stats[lang] = lang_stats.get(lang, 0) + 1
    
    log("\n📊 语言分布:")
    for lang, count in sorted(lang_stats.items(), key=lambda x: x[1], reverse=True):
        log(f"   {lang}: {count} 个版本")
    
    # 保存完整数据
    output_file = OUTPUT_DIR / "tafsirs_all.json"
    output_file.parent.mkdir(parents=True, exist_ok=True)
    
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    
    log(f"💾 已保存到: {output_file}")
    
    return tafsirs

# ═══════════════════════════════════════════════════════════════
# 3. 筛选高优先级翻译
# ═══════════════════════════════════════════════════════════════

def filter_priority_translations(translations):
    """筛选高优先级翻译"""
    log("=" * 60)
    log("🎯 筛选高优先级翻译...")
    log("=" * 60)
    
    # 高优先级语言
    priority_languages = {
        "bengali": {"priority": 1, "min_count": 3, "preferred_ids": [161, 163, 164]},
        "malay": {"priority": 1, "min_count": 2, "preferred_ids": [39, 134]},
        "turkish": {"priority": 1, "min_count": 5, "preferred_ids": [77, 78, 124, 125, 126]},
        "indonesian": {"priority": 1, "min_count": 3, "preferred_ids": [33, 134, 141]},
        "urdu": {"priority": 1, "min_count": 5, "preferred_ids": [97, 151, 158, 234]},
        "english": {"priority": 2, "min_count": 5, "preferred_ids": [131, 84, 85, 19, 20]},
        "arabic": {"priority": 2, "min_count": 3, "preferred_ids": [203, 206, 207]},
        "persian": {"priority": 2, "min_count": 3, "preferred_ids": [135, 136, 229]},
        "french": {"priority": 3, "min_count": 2, "preferred_ids": [31, 136]},
        "german": {"priority": 3, "min_count": 2, "preferred_ids": [27, 208]},
        "spanish": {"priority": 3, "min_count": 2, "preferred_ids": [83, 143]},
        "russian": {"priority": 3, "min_count": 2, "preferred_ids": [45, 79]},
        "chinese": {"priority": 3, "min_count": 2, "preferred_ids": [56, 109]},
    }
    
    priority_translations = {
        "priority_1": [],  # 高优先级（必须同步）
        "priority_2": [],  # 中优先级（推荐同步）
        "priority_3": [],  # 低优先级（可选同步）
    }
    
    for trans in translations:
        lang = trans.get("language_name", "").lower()
        trans_id = trans.get("id")
        
        if lang in priority_languages:
            config = priority_languages[lang]
            priority_level = f"priority_{config['priority']}"
            
            # 优先选择推荐的 ID
            if trans_id in config.get("preferred_ids", []):
                priority_translations[priority_level].append(trans)
            # 或者选择前 N 个
            elif len([t for t in priority_translations[priority_level] if t.get("language_name", "").lower() == lang]) < config.get("min_count", 1):
                priority_translations[priority_level].append(trans)
    
    # 统计
    for level, trans_list in priority_translations.items():
        log(f"\n{level.upper()}:")
        lang_counts = {}
        for trans in trans_list:
            lang = trans.get("language_name", "unknown")
            lang_counts[lang] = lang_counts.get(lang, 0) + 1
        
        for lang, count in sorted(lang_counts.items()):
            log(f"   {lang}: {count} 个版本")
        
        log(f"   总计: {len(trans_list)} 个翻译")
    
    # 保存
    for level, trans_list in priority_translations.items():
        output_file = OUTPUT_DIR / f"translations_{level}.json"
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump({"translations": trans_list}, f, ensure_ascii=False, indent=2)
        log(f"💾 {level} 已保存到: {output_file}")
    
    return priority_translations

# ═══════════════════════════════════════════════════════════════
# 4. 筛选高优先级 Tafsir
# ═══════════════════════════════════════════════════════════════

def filter_priority_tafsirs(tafsirs):
    """筛选高优先级 Tafsir"""
    log("=" * 60)
    log("🎯 筛选高优先级 Tafsir...")
    log("=" * 60)
    
    # 高优先级语言和 Tafsir
    priority_tafsirs_config = {
        "english": ["en-tafisr-ibn-kathir", "en-tafsir-al-jalalayn"],
        "arabic": ["ar-tafsir-muyassar", "ar-tafseer-al-tabari", "ar-tafseer-al-qurtubi"],
        "indonesian": ["id-tafsir-kemenag"],
        "urdu": ["tafsir-bayan-ul-quran", "ur-tafseer-tafheem-ul-quran"],
        "bengali": ["bn-tafseer-ibn-e-kaseer"],
        "turkish": ["tr-ates", "tr-diyanet"],
        "persian": ["fa-tafsir-saadi"],
        "malay": [],  # 马来语可能没有 Tafsir
    }
    
    priority_tafsirs = []
    
    for tafsir in tafsirs:
        lang = tafsir.get("language_name", "").lower()
        slug = tafsir.get("slug", "")
        
        if lang in priority_tafsirs_config:
            if slug in priority_tafsirs_config[lang]:
                priority_tafsirs.append(tafsir)
            elif not priority_tafsirs_config[lang]:  # 空列表表示选择所有
                priority_tafsirs.append(tafsir)
    
    log(f"✅ 筛选出 {len(priority_tafsirs)} 个高优先级 Tafsir")
    
    # 保存
    output_file = OUTPUT_DIR / "tafsirs_priority.json"
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump({"tafsirs": priority_tafsirs}, f, ensure_ascii=False, indent=2)
    
    log(f"💾 已保存到: {output_file}")
    
    # 详细列表
    log("\n📋 高优先级 Tafsir 列表:")
    for tafsir in priority_tafsirs:
        log(f"   [{tafsir.get('id')}] {tafsir.get('name')} ({tafsir.get('language_name')})")
        log(f"       Slug: {tafsir.get('slug')}")
        log(f"       Author: {tafsir.get('author_name')}")
        log("")
    
    return priority_tafsirs

# ═══════════════════════════════════════════════════════════════
# 5. 生成摘要报告
# ═══════════════════════════════════════════════════════════════

def generate_summary_report(translations, tafsirs, priority_translations, priority_tafsirs):
    """生成数据摘要报告"""
    log("=" * 60)
    log("📊 生成摘要报告...")
    log("=" * 60)
    
    report = {
        "generated_at": datetime.now().isoformat(),
        "api_base_url": API_BASE_URL,
        "summary": {
            "total_translations": len(translations) if translations else 0,
            "total_tafsirs": len(tafsirs) if tafsirs else 0,
            "priority_1_translations": len(priority_translations.get("priority_1", [])),
            "priority_2_translations": len(priority_translations.get("priority_2", [])),
            "priority_3_translations": len(priority_translations.get("priority_3", [])),
            "priority_tafsirs": len(priority_tafsirs) if priority_tafsirs else 0,
        },
        "notes": [
            "优先级 1: 孟加拉语、马来语、土耳其语、印尼语、乌尔都语 - 必须同步",
            "优先级 2: 英语、阿拉伯语、波斯语 - 推荐同步",
            "优先级 3: 法语、德语、西班牙语、俄语、中文 - 可选同步",
            "Tafsir 推荐按需加载，不建议全部同步到数据库"
        ]
    }
    
    # 保存报告 (确保目录存在)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    output_file = OUTPUT_DIR / "summary_report.json"
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    
    log(f"💾 报告已保存到: {output_file}")
    
    # 打印摘要
    log("\n" + "=" * 60)
    log("📈 数据摘要")
    log("=" * 60)
    log(f"总翻译数: {report['summary']['total_translations']}")
    log(f"总 Tafsir 数: {report['summary']['total_tafsirs']}")
    log(f"\n优先级 1 翻译: {report['summary']['priority_1_translations']} 个")
    log(f"优先级 2 翻译: {report['summary']['priority_2_translations']} 个")
    log(f"优先级 3 翻译: {report['summary']['priority_3_translations']} 个")
    log(f"\n高优先级 Tafsir: {report['summary']['priority_tafsirs']} 个")
    log("=" * 60)

# ═══════════════════════════════════════════════════════════════
# 主函数
# ═══════════════════════════════════════════════════════════════

def main():
    log("🚀 开始获取 Quran.com API 资源...")
    log("")
    
    # 1. 获取所有翻译
    translations = fetch_all_translations()
    log("")
    
    # 2. 获取所有 Tafsir
    tafsirs = fetch_all_tafsirs()
    log("")
    
    # 3. 筛选高优先级翻译
    priority_translations = {}
    if translations:
        priority_translations = filter_priority_translations(translations)
    log("")
    
    # 4. 筛选高优先级 Tafsir
    priority_tafsirs = []
    if tafsirs:
        priority_tafsirs = filter_priority_tafsirs(tafsirs)
    log("")
    
    # 5. 生成摘要报告
    generate_summary_report(translations, tafsirs, priority_translations, priority_tafsirs)
    log("")
    
    log("✅ 所有数据获取完成！")
    log(f"📂 数据目录: {OUTPUT_DIR.absolute()}")

if __name__ == "__main__":
    main()

