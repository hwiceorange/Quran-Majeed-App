#!/usr/bin/env python3
"""
RTL布局自动修复工具
将 paddingLeft/Right 和 marginLeft/Right 替换为 paddingStart/End 和 marginStart/End
"""

import re
from pathlib import Path

def fix_rtl_attributes(file_path):
    """修复单个XML文件的RTL属性（避免重复）"""
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    modified = False
    changes = []
    new_lines = []
    
    for line in lines:
        modified_line = line
        
        # 只有在同一行中不存在Start/End属性时才替换Left/Right
        # 替换 paddingLeft -> paddingStart (如果该行没有paddingStart)
        if 'android:paddingLeft' in modified_line:
            # 检查整个文件中该元素是否已有paddingStart
            if 'android:paddingStart' not in modified_line:
                modified_line = modified_line.replace('android:paddingLeft', 'android:paddingStart')
                if modified_line != line:
                    changes.append('paddingLeft -> paddingStart')
                    modified = True
        
        # 替换 paddingRight -> paddingEnd (如果该行没有paddingEnd)
        if 'android:paddingRight' in modified_line:
            if 'android:paddingEnd' not in modified_line:
                modified_line = modified_line.replace('android:paddingRight', 'android:paddingEnd')
                if modified_line != line:
                    changes.append('paddingRight -> paddingEnd')
                    modified = True
        
        # 替换 marginLeft -> marginStart (如果该行没有marginStart)
        if 'android:layout_marginLeft' in modified_line:
            if 'android:layout_marginStart' not in modified_line:
                modified_line = modified_line.replace('android:layout_marginLeft', 'android:layout_marginStart')
                if modified_line != line:
                    changes.append('marginLeft -> marginStart')
                    modified = True
        
        # 替换 marginRight -> marginEnd (如果该行没有marginEnd)
        if 'android:layout_marginRight' in modified_line:
            if 'android:layout_marginEnd' not in modified_line:
                modified_line = modified_line.replace('android:layout_marginRight', 'android:layout_marginEnd')
                if modified_line != line:
                    changes.append('marginRight -> marginEnd')
                    modified = True
        
        new_lines.append(modified_line)
    
    # 如果有修改，写入文件
    if modified:
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(new_lines)
        return True, list(set(changes))
    
    return False, []

def main():
    # 查找所有layout XML文件
    layout_dirs = [
        Path('app/src/main/res/layout'),
        Path('app/src/main/res/layout-land'),
        Path('app/src/main/res/layout-sw600dp'),
    ]
    
    all_xml_files = []
    for layout_dir in layout_dirs:
        if layout_dir.exists():
            all_xml_files.extend(layout_dir.glob('*.xml'))
    
    print("="*70)
    print("RTL布局自动修复工具")
    print("="*70)
    print(f"\n📁 找到 {len(all_xml_files)} 个 XML 文件\n")
    
    fixed_files = []
    total_changes = 0
    
    for xml_file in all_xml_files:
        modified, changes = fix_rtl_attributes(xml_file)
        if modified:
            fixed_files.append(xml_file.name)
            total_changes += len(changes)
            print(f"✅ {xml_file.name}")
            for change in changes:
                print(f"   - {change}")
    
    print("\n" + "="*70)
    print("📊 修复总结")
    print("="*70)
    print(f"✅ 修改了 {len(fixed_files)} 个文件")
    print(f"✅ 共进行 {total_changes} 处RTL修正")
    
    if len(fixed_files) == 0:
        print("\n🎉 所有布局文件已经符合RTL规范！")
    else:
        print("\n🎉 RTL布局修复完成！")
        print("\n💡 建议：")
        print("   1. 运行 ./gradlew assembleDebug 验证编译")
        print("   2. 在乌尔都语/阿拉伯语环境下测试RTL显示")

if __name__ == '__main__':
    main()

