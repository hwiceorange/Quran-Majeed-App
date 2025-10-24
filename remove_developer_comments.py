#!/usr/bin/env python3
"""
批量删除源代码文件中的第三方开发者注释
"""
import os
import re
from pathlib import Path

# 要删除的注释模式
PATTERNS_TO_REMOVE = [
    # Java/Kotlin 风格的多行注释
    r'/\*\*?\s*\n\s*\*\s*Author:\s*Rai Adnan\s*\n\s*\*\s*Whatsapp:\s*\+923002375907\s*\n\s*\*\s*Email:\s*officialshaheendevelopers@gmail\.com\s*\n\s*\*\s*Portfolio:\s*https://codecanyon\.net/user/shaheendevelopers/portfolio\s*\n\s*\*/\s*\n',
    
    # XML 风格的注释
    r'<!--\s*\n?\s*\*\s*Author:\s*Rai Adnan\s*\n\s*\*\s*Whatsapp:\s*\+923002375907\s*\n\s*\*\s*Email:\s*officialshaheendevelopers@gmail\.com\s*\n\s*\*\s*Portfolio:\s*https://codecanyon\.net/user/shaheendevelopers/portfolio\s*\n?\s*-->\s*\n',
]

def remove_developer_comments(file_path):
    """删除文件中的开发者注释"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        original_content = content
        
        # 尝试所有模式
        for pattern in PATTERNS_TO_REMOVE:
            content = re.sub(pattern, '', content, flags=re.MULTILINE)
        
        # 如果内容有变化，写回文件
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        return False
    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False

def process_directory(directory):
    """处理目录中的所有源代码文件"""
    extensions = ['.java', '.kt', '.xml']
    modified_files = []
    
    # 遍历目录
    for root, dirs, files in os.walk(directory):
        # 跳过 build 目录
        if 'build' in dirs:
            dirs.remove('build')
        
        for file in files:
            if any(file.endswith(ext) for ext in extensions):
                file_path = os.path.join(root, file)
                if remove_developer_comments(file_path):
                    modified_files.append(file_path)
                    print(f"✓ Modified: {file_path}")
    
    return modified_files

if __name__ == '__main__':
    # 处理 app/src/main 目录
    app_dir = '/Users/huwei/AndroidStudioProjects/quran0/app/src/main'
    
    print("🔍 开始扫描并删除开发者注释...")
    print(f"目标目录: {app_dir}\n")
    
    modified_files = process_directory(app_dir)
    
    print(f"\n✅ 完成！共修改了 {len(modified_files)} 个文件")
    
    if modified_files:
        print("\n修改的文件列表：")
        for f in modified_files[:20]:  # 只显示前20个
            print(f"  - {f}")
        if len(modified_files) > 20:
            print(f"  ... 以及其他 {len(modified_files) - 20} 个文件")

