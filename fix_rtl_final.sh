#!/bin/bash
# RTL布局修复最终版本
# 仅替换Left/Right为Start/End，不产生重复

echo "🔧 开始RTL布局修复..."

# 使用sed进行精确替换，确保不产生重复
find app/src/main/res/layout* -name "*.xml" -type f | while read file; do
    # 只替换没有对应Start/End属性的Left/Right
    # 使用临时文件避免并发问题
    temp_file=$(mktemp)
    
    # 读取文件并逐行处理
    awk '
    {
        line = $0
        # 仅当该行不包含marginStart时，才替换marginLeft为marginStart
        if (index(line, "android:layout_marginLeft") > 0 && index(line, "android:layout_marginStart") == 0) {
            gsub(/android:layout_marginLeft/, "android:layout_marginStart", line)
        }
        # 仅当该行不包含marginEnd时，才替换marginRight为marginEnd
        if (index(line, "android:layout_marginRight") > 0 && index(line, "android:layout_marginEnd") == 0) {
            gsub(/android:layout_marginRight/, "android:layout_marginEnd", line)
        }
        # 仅当该行不包含paddingStart时，才替换paddingLeft为paddingStart
        if (index(line, "android:paddingLeft") > 0 && index(line, "android:paddingStart") == 0) {
            gsub(/android:paddingLeft/, "android:paddingStart", line)
        }
        # 仅当该行不包含paddingEnd时，才替换paddingRight为paddingEnd
        if (index(line, "android:paddingRight") > 0 && index(line, "android:paddingEnd") == 0) {
            gsub(/android:paddingRight/, "android:paddingEnd", line)
        }
        print line
    }
    ' "$file" > "$temp_file"
    
    # 只有文件有变化时才替换
    if ! diff -q "$file" "$temp_file" > /dev/null 2>&1; then
        mv "$temp_file" "$file"
        echo "✅ $(basename $file)"
    else
        rm "$temp_file"
    fi
done

echo "✅ RTL布局修复完成！"

