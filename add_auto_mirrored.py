#!/usr/bin/env python3
"""
为方向性图标添加 autoMirrored="true" 属性
"""

import re
from pathlib import Path

def add_auto_mirrored(file_path):
    """为vector drawable添加autoMirrored属性"""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # 检查是否已经有autoMirrored属性
    if 'android:autoMirrored' in content:
        return False, "已存在"
    
    # 检查是否是vector drawable
    if '<vector' not in content:
        return False, "非vector"
    
    # 在<vector标签中添加autoMirrored属性
    # 查找<vector标签的结束位置（可能跨多行）
    pattern = r'(<vector[^>]*)(>)'
    match = re.search(pattern, content, re.DOTALL)
    
    if match:
        vector_tag = match.group(1)
        closing = match.group(2)
        
        # 添加autoMirrored属性
        new_vector_tag = vector_tag + '\n    android:autoMirrored="true"'
        new_content = content[:match.start()] + new_vector_tag + closing + content[match.end():]
        
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        return True, "已添加"
    
    return False, "未找到vector标签"

def main():
    # 需要处理的方向性图标文件名模式
    directional_patterns = [
        '*arrow*',
        '*back*',
        '*next*',
        '*forward*',
        '*previous*',
        '*chevron*',
    ]
    
    drawable_dirs = [
        Path('app/src/main/res/drawable'),
        Path('app/src/main/res/drawable-v24'),
    ]
    
    print("="*70)
    print("为方向性图标添加 autoMirrored 属性")
    print("="*70)
    
    all_files = []
    for drawable_dir in drawable_dirs:
        if drawable_dir.exists():
            for pattern in directional_patterns:
                all_files.extend(drawable_dir.glob(f'{pattern}.xml'))
    
    # 去重
    all_files = list(set(all_files))
    
    print(f"\n📁 找到 {len(all_files)} 个方向性图标文件\n")
    
    added = []
    skipped = []
    
    for file_path in sorted(all_files):
        modified, reason = add_auto_mirrored(file_path)
        if modified:
            added.append(file_path.name)
            print(f"✅ {file_path.name} - {reason}")
        else:
            skipped.append((file_path.name, reason))
            print(f"⏭️  {file_path.name} - {reason}")
    
    print("\n" + "="*70)
    print("📊 总结")
    print("="*70)
    print(f"✅ 添加 autoMirrored: {len(added)} 个文件")
    print(f"⏭️  跳过: {len(skipped)} 个文件")
    
    if len(added) > 0:
        print("\n🎉 autoMirrored 属性添加完成！")
        print("\n💡 这些图标现在会在RTL语言（乌尔都语/阿拉伯语）中自动镜像显示")

if __name__ == '__main__':
    main()

