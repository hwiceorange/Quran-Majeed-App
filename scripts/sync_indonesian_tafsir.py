#!/usr/bin/env python3
"""
印尼语 Tafsir (古兰经注释) 数据同步脚本

数据源: EQuran.id API v2.0
目标: 同步所有 114 个 Surah 的印尼语 Tafsir 注释到本地
"""

import requests
import json
import time
import os
from datetime import datetime
from typing import Dict, List, Optional

# ═══════════════════════════════════════════════════════════════
# 配置
# ═══════════════════════════════════════════════════════════════

# API 配置
API_BASE_URL = "https://equran.id/api/v2"
API_TIMEOUT = 30  # 秒

# 输出配置
OUTPUT_DIR = "tafsir_data/indonesian"
OUTPUT_FORMAT = "json"  # json 或 sql

# 同步配置
TOTAL_SURAHS = 114
RETRY_ATTEMPTS = 3
RETRY_DELAY = 2  # 秒
REQUEST_DELAY = 0.5  # 请求间隔，避免频繁请求

# ═══════════════════════════════════════════════════════════════
# 工具函数
# ═══════════════════════════════════════════════════════════════

def log(message: str, level: str = "INFO"):
    """打印带时间戳的日志"""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    print(f"[{timestamp}] [{level}] {message}")


def ensure_output_dir():
    """确保输出目录存在"""
    if not os.path.exists(OUTPUT_DIR):
        os.makedirs(OUTPUT_DIR)
        log(f"创建输出目录: {OUTPUT_DIR}")


def save_json(data: Dict, filename: str):
    """保存 JSON 数据到文件"""
    filepath = os.path.join(OUTPUT_DIR, filename)
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    log(f"保存文件: {filepath}")


# ═══════════════════════════════════════════════════════════════
# API 调用函数
# ═══════════════════════════════════════════════════════════════

def fetch_tafsir_list() -> Optional[Dict]:
    """
    获取所有 Surah 的 Tafsir 列表
    
    端点: GET /api/v2/tafsir
    返回: 所有 Surah 的概览列表
    """
    url = f"{API_BASE_URL}/tafsir"
    
    log(f"正在获取 Tafsir 列表...")
    
    for attempt in range(RETRY_ATTEMPTS):
        try:
            response = requests.get(url, timeout=API_TIMEOUT)
            response.raise_for_status()
            
            data = response.json()
            
            if data.get("code") == 200:
                log(f"✅ 成功获取 Tafsir 列表")
                return data
            else:
                log(f"⚠️ API 返回异常状态码: {data.get('code')}", "WARN")
                
        except requests.exceptions.RequestException as e:
            log(f"❌ 请求失败 (尝试 {attempt + 1}/{RETRY_ATTEMPTS}): {e}", "ERROR")
            if attempt < RETRY_ATTEMPTS - 1:
                time.sleep(RETRY_DELAY)
    
    return None


def fetch_surah_tafsir(surah_number: int) -> Optional[Dict]:
    """
    获取特定 Surah 的完整 Tafsir 内容
    
    参数:
        surah_number: Surah 编号 (1-114)
    
    端点: GET /api/v2/tafsir/{surah_number}
    返回: 该 Surah 的所有 Ayat 的 Tafsir 注释
    """
    url = f"{API_BASE_URL}/tafsir/{surah_number}"
    
    log(f"正在获取 Surah {surah_number} 的 Tafsir...")
    
    for attempt in range(RETRY_ATTEMPTS):
        try:
            response = requests.get(url, timeout=API_TIMEOUT)
            response.raise_for_status()
            
            data = response.json()
            
            if data.get("code") == 200:
                tafsir_count = len(data.get("data", {}).get("tafsir", []))
                log(f"✅ 成功获取 Surah {surah_number} ({tafsir_count} 条注释)")
                return data
            else:
                log(f"⚠️ API 返回异常状态码: {data.get('code')}", "WARN")
                
        except requests.exceptions.RequestException as e:
            log(f"❌ 请求失败 (尝试 {attempt + 1}/{RETRY_ATTEMPTS}): {e}", "ERROR")
            if attempt < RETRY_ATTEMPTS - 1:
                time.sleep(RETRY_DELAY)
    
    return None


# ═══════════════════════════════════════════════════════════════
# 数据处理函数
# ═══════════════════════════════════════════════════════════════

def process_tafsir_data(raw_data: Dict) -> Dict:
    """
    处理原始 Tafsir 数据，提取关键字段
    
    返回标准化的数据结构，便于存储和使用
    """
    if not raw_data or "data" not in raw_data:
        return {}
    
    surah_data = raw_data["data"]
    tafsir_list = surah_data.get("tafsir", [])
    
    processed = {
        "surah_id": surah_data.get("nomor"),
        "surah_name": surah_data.get("nama"),
        "surah_name_latin": surah_data.get("namaLatin"),
        "surah_name_translation": surah_data.get("arti"),
        "total_ayat": surah_data.get("jumlahAyat"),
        "language": "id",  # 印尼语
        "source": "Kemenag",  # 印尼宗教事务部
        "tafsir": []
    }
    
    for tafsir_item in tafsir_list:
        processed["tafsir"].append({
            "ayat_id": tafsir_item.get("ayat"),
            "text": tafsir_item.get("teks", "").strip()
        })
    
    return processed


# ═══════════════════════════════════════════════════════════════
# 主同步函数
# ═══════════════════════════════════════════════════════════════

def sync_all_tafsir():
    """
    同步所有 114 个 Surah 的 Tafsir 数据
    """
    log("=" * 60)
    log("开始同步印尼语 Tafsir 数据")
    log("=" * 60)
    
    # 确保输出目录存在
    ensure_output_dir()
    
    # 步骤 1: 获取 Tafsir 列表（可选，用于验证）
    log("\n📋 步骤 1: 获取 Tafsir 列表")
    tafsir_list = fetch_tafsir_list()
    
    if tafsir_list:
        save_json(tafsir_list, "tafsir_list.json")
    else:
        log("⚠️ 无法获取 Tafsir 列表，将直接同步各 Surah", "WARN")
    
    # 步骤 2: 遍历所有 Surah，下载 Tafsir
    log(f"\n📥 步骤 2: 开始下载所有 Surah 的 Tafsir (共 {TOTAL_SURAHS} 个)")
    
    success_count = 0
    failed_surahs = []
    all_tafsir_data = {}
    
    for surah_number in range(1, TOTAL_SURAHS + 1):
        log(f"\n[{surah_number}/{TOTAL_SURAHS}] 处理 Surah {surah_number}...")
        
        # 获取原始数据
        raw_data = fetch_surah_tafsir(surah_number)
        
        if raw_data:
            # 处理数据
            processed_data = process_tafsir_data(raw_data)
            
            if processed_data:
                # 保存单个 Surah 的 Tafsir
                filename = f"surah_{surah_number:03d}_tafsir.json"
                save_json(processed_data, filename)
                
                # 添加到汇总数据
                all_tafsir_data[str(surah_number)] = processed_data
                
                success_count += 1
            else:
                log(f"❌ Surah {surah_number} 数据处理失败", "ERROR")
                failed_surahs.append(surah_number)
        else:
            log(f"❌ Surah {surah_number} 下载失败", "ERROR")
            failed_surahs.append(surah_number)
        
        # 请求间隔，避免频繁请求
        if surah_number < TOTAL_SURAHS:
            time.sleep(REQUEST_DELAY)
    
    # 步骤 3: 保存汇总数据
    log(f"\n💾 步骤 3: 保存汇总数据")
    save_json(all_tafsir_data, "all_tafsir_data.json")
    
    # 步骤 4: 生成统计报告
    log("\n" + "=" * 60)
    log("同步完成！统计报告:")
    log("=" * 60)
    log(f"✅ 成功: {success_count}/{TOTAL_SURAHS}")
    log(f"❌ 失败: {len(failed_surahs)}/{TOTAL_SURAHS}")
    
    if failed_surahs:
        log(f"失败的 Surah 列表: {failed_surahs}", "WARN")
    
    log(f"\n📁 输出目录: {os.path.abspath(OUTPUT_DIR)}")
    log("=" * 60)
    
    return {
        "success_count": success_count,
        "failed_count": len(failed_surahs),
        "failed_surahs": failed_surahs,
        "output_dir": os.path.abspath(OUTPUT_DIR)
    }


# ═══════════════════════════════════════════════════════════════
# 验证函数
# ═══════════════════════════════════════════════════════════════

def verify_api_access():
    """
    验证 API 访问是否正常
    
    测试访问 Surah 1 (Al-Fatiha) 的 Tafsir
    """
    log("=" * 60)
    log("验证 API 访问")
    log("=" * 60)
    
    log("\n测试 API 端点: /api/v2/tafsir/1")
    
    data = fetch_surah_tafsir(1)
    
    if data:
        log("\n✅ API 访问正常")
        log(f"响应代码: {data.get('code')}")
        log(f"响应状态: {data.get('status')}")
        
        if "data" in data:
            surah_data = data["data"]
            log(f"\nSurah 信息:")
            log(f"  - 编号: {surah_data.get('nomor')}")
            log(f"  - 名称: {surah_data.get('nama')}")
            log(f"  - 拉丁名: {surah_data.get('namaLatin')}")
            log(f"  - 含义: {surah_data.get('arti')}")
            log(f"  - 总计 Ayat: {surah_data.get('jumlahAyat')}")
            
            tafsir_list = surah_data.get("tafsir", [])
            log(f"  - Tafsir 注释数量: {len(tafsir_list)}")
            
            if tafsir_list:
                first_tafsir = tafsir_list[0]
                log(f"\n第一条 Tafsir 预览:")
                log(f"  - Ayat: {first_tafsir.get('ayat')}")
                log(f"  - 内容: {first_tafsir.get('teks', '')[:100]}...")
        
        log("\n" + "=" * 60)
        return True
    else:
        log("\n❌ API 访问失败", "ERROR")
        log("=" * 60)
        return False


# ═══════════════════════════════════════════════════════════════
# 主程序入口
# ═══════════════════════════════════════════════════════════════

def main():
    """主程序入口"""
    global OUTPUT_DIR  # 🔧 必须在使用前声明
    
    import argparse
    
    parser = argparse.ArgumentParser(
        description="印尼语 Tafsir 数据同步脚本"
    )
    parser.add_argument(
        "--verify",
        action="store_true",
        help="仅验证 API 访问，不执行同步"
    )
    parser.add_argument(
        "--auto",
        action="store_true",
        help="自动执行模式，无需用户确认"
    )
    parser.add_argument(
        "--output",
        type=str,
        default=OUTPUT_DIR,
        help=f"输出目录 (默认: {OUTPUT_DIR})"
    )
    
    args = parser.parse_args()
    
    # 更新输出目录
    OUTPUT_DIR = args.output
    
    if args.verify:
        # 仅验证 API
        verify_api_access()
    else:
        # 先验证 API
        if verify_api_access():
            # 执行完整同步
            if not args.auto:
                input("\n按 Enter 键开始完整同步...")
            else:
                log("\n🤖 自动模式：开始完整同步...")
            sync_all_tafsir()
        else:
            log("\n❌ API 验证失败，取消同步", "ERROR")
            return 1
    
    return 0


if __name__ == "__main__":
    exit(main())

