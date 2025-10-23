#!/usr/bin/env python3
"""
清理重复的margin和padding属性
"""

import re
from pathlib import Path

def remove_duplicate_attributes(file_path):
    """删除XML文件中重复的margin和padding属性"""
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    cleaned_lines = []
    seen_attributes = {}
    current_element_start = -1
    changes = []
    
    for i, line in enumerate(lines):
        # 检测元素开始
        if re.search(r'<\w+|<[\w.]+:', line) and not line.strip().startswith('<!--'):
            if '>' in line and not '/>' in line and not '</' in line:
                # 单行元素
                current_element_start = -1
                seen_attributes = {}
            else:
                # 多行元素开始
                current_element_start = i
                seen_attributes = {}
        
        # 检测元素结束
        if (current_element_start >= 0 and ('>' in line or '/>' in line)):
            current_element_start = -1
            seen_attributes = {}
        
        # 检查是否有重复的属性
        margin_match = re.search(r'android:(layout_margin(?:Start|End|Top|Bottom)|padding(?:Start|End|Top|Bottom))="([^"]*)"', line)
        
        if margin_match and current_element_start >= 0:
            attr_name = margin_match.group(1)
            
            if attr_name in seen_attributes:
                # 发现重复，跳过这一行
                changes.append(f"删除重复的 {attr_name}")
                continue  # 不添加这一行
            else:
                seen_attributes[attr_name] = True
        
        cleaned_lines.append(line)
    
    # 如果有修改，写入文件
    if len(lines) != len(cleaned_lines):
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(cleaned_lines)
        return True, len(lines) - len(cleaned_lines), changes
    
    return False, 0, []

def main():
    layout_dirs = [
        Path('app/src/main/res/layout'),
        Path('app/src/main/res/layout-land'),
    ]
    
    all_xml_files = []
    for layout_dir in layout_dirs:
        if layout_dir.exists():
            all_xml_files.extend(layout_dir.glob('*.xml'))
    
    print("="*70)
    print("清理重复的margin和padding属性")
    print("="*70)
    print(f"\n📁 扫描 {len(all_xml_files)} 个 XML 文件\n")
    
    fixed_files = []
    total_removed = 0
    
    for xml_file in all_xml_files:
        modified, removed, changes = remove_duplicate_attributes(xml_file)
        if modified:
            fixed_files.append(xml_file.name)
            total_removed += removed
            print(f"✅ {xml_file.name} - 删除了 {removed} 个重复属性")
    
    print("\n" + "="*70)
    print("📊 清理总结")
    print("="*70)
    print(f"✅ 修改了 {len(fixed_files)} 个文件")
    print(f"✅ 共删除 {total_removed} 个重复属性")
    
    if len(fixed_files) == 0:
        print("\n🎉 没有发现重复属性！")
    else:
        print("\n🎉 重复属性清理完成！")

if __name__ == '__main__':
    main()

