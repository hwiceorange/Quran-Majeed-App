#!/usr/bin/env python3
"""
跨语言字符串资源完整性验证工具
验证 ui_strings.xml 在所有语言版本中的完整性
"""

import xml.etree.ElementTree as ET
import sys
from pathlib import Path

def parse_strings_file(filepath):
    """解析strings XML文件，返回key集合"""
    try:
        tree = ET.parse(filepath)
        root = tree.getroot()
        keys = set()
        for string_elem in root.findall('string'):
            name = string_elem.get('name')
            if name:
                keys.add(name)
        return keys
    except Exception as e:
        print(f"❌ 解析文件失败 {filepath}: {e}")
        return set()

def main():
    # 定义文件路径
    base_path = Path("app/src/main/res")
    
    files = {
        'en': base_path / "values" / "strings.xml",
        'id': base_path / "values-in" / "strings.xml",
        'ar': base_path / "values-ar" / "strings.xml"
    }
    
    # 解析所有文件
    all_keys = {}
    for lang, filepath in files.items():
        if not filepath.exists():
            print(f"❌ 文件不存在: {filepath}")
            sys.exit(1)
        all_keys[lang] = parse_strings_file(filepath)
        print(f"✅ {lang.upper()}: 找到 {len(all_keys[lang])} 个字符串")
    
    # 以英语为基准
    base_keys = all_keys['en']
    
    print("\n" + "="*60)
    print("📊 跨语言完整性检查报告")
    print("="*60)
    
    # 检查印尼语
    print("\n🇮🇩 印尼语 (values-in/strings.xml)")
    missing_in_id = base_keys - all_keys['id']
    extra_in_id = all_keys['id'] - base_keys
    
    if not missing_in_id and not extra_in_id:
        print("✅ 完美！所有字符串都已翻译，没有多余的keys")
    else:
        if missing_in_id:
            print(f"⚠️  缺失 {len(missing_in_id)} 个翻译:")
            for key in sorted(missing_in_id):
                print(f"   - {key}")
        if extra_in_id:
            print(f"⚠️  多余 {len(extra_in_id)} 个keys（英语版本中不存在）:")
            for key in sorted(extra_in_id):
                print(f"   - {key}")
    
    # 检查阿拉伯语
    print("\n🇸🇦 阿拉伯语 (values-ar/strings.xml)")
    missing_in_ar = base_keys - all_keys['ar']
    extra_in_ar = all_keys['ar'] - base_keys
    
    if not missing_in_ar and not extra_in_ar:
        print("✅ 完美！所有字符串都已翻译，没有多余的keys")
    else:
        if missing_in_ar:
            print(f"⚠️  缺失 {len(missing_in_ar)} 个翻译:")
            for key in sorted(missing_in_ar):
                print(f"   - {key}")
        if extra_in_ar:
            print(f"⚠️  多余 {len(extra_in_ar)} 个keys（英语版本中不存在）:")
            for key in sorted(extra_in_ar):
                print(f"   - {key}")
    
    
    # 总结
    print("\n" + "="*60)
    print("📋 总结")
    print("="*60)
    
    total_issues = len(missing_in_id) + len(extra_in_id) + len(missing_in_ar) + len(extra_in_ar)
    
    if total_issues == 0:
        print("✅ 所有语言版本完全一致，无需修复")
        return 0
    else:
        print(f"⚠️  发现 {total_issues} 个问题需要修复")
        print("\n💡 建议使用 Android Studio 的 Translations Editor:")
        print("   Tools -> Translations Editor -> 选择 strings.xml")
        print("   检查并填补所有空白单元格")
        
        # 保存缺失的keys到文件以便补充
        if missing_in_id or missing_in_ar:
            with open('missing_translations.txt', 'w', encoding='utf-8') as f:
                if missing_in_id:
                    f.write("=== 印尼语缺失的Keys ===\n")
                    for key in sorted(missing_in_id):
                        f.write(f"{key}\n")
                    f.write("\n")
                if missing_in_ar:
                    f.write("=== 阿拉伯语缺失的Keys ===\n")
                    for key in sorted(missing_in_ar):
                        f.write(f"{key}\n")
            print("\n📄 缺失的keys已保存到 missing_translations.txt")
        return 1

if __name__ == "__main__":
    sys.exit(main())

