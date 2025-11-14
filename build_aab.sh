#!/bin/bash

# Google Play AAB 打包脚本
# 用于生成上传到 Google Play Console 的 AAB (Android App Bundle) 文件

echo "🚀 开始构建 AAB 包..."
echo ""

# 设置颜色输出
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Gradle Wrapper 是否存在
if [ ! -f "./gradlew" ]; then
    echo "❌ 错误: gradlew 文件不存在"
    exit 1
fi

# 给 gradlew 执行权限
chmod +x ./gradlew

echo "📦 正在构建 Release AAB..."
echo ""

# 清理并构建 AAB
./gradlew clean bundleRelease

# 检查构建是否成功
if [ $? -eq 0 ]; then
    echo ""
    echo "${GREEN}✅ AAB 构建成功！${NC}"
    echo ""
    
    # AAB 文件位置
    AAB_PATH="app/build/outputs/bundle/release/app-release.aab"
    
    if [ -f "$AAB_PATH" ]; then
        # 获取文件大小
        FILE_SIZE=$(du -h "$AAB_PATH" | cut -f1)
        
        echo "📂 AAB 文件位置:"
        echo "   ${GREEN}$(pwd)/$AAB_PATH${NC}"
        echo ""
        echo "📊 文件大小: ${YELLOW}$FILE_SIZE${NC}"
        echo ""
        echo "📋 下一步操作:"
        echo "   1. 在 Google Play Console 中创建新版本"
        echo "   2. 上传此 AAB 文件: $AAB_PATH"
        echo "   3. 填写版本说明和其他必要信息"
        echo "   4. 提交审核"
        echo ""
        
        # 尝试打开文件所在目录（macOS）
        if [[ "$OSTYPE" == "darwin"* ]]; then
            echo "💡 正在打开文件所在目录..."
            open -R "$AAB_PATH"
        fi
    else
        echo "⚠️  警告: 找不到 AAB 文件，但构建可能已成功"
        echo "   请检查: app/build/outputs/bundle/release/"
    fi
else
    echo ""
    echo "❌ 构建失败！请检查错误信息"
    exit 1
fi

