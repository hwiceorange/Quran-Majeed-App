#!/bin/bash

echo "========================================"
echo "替换 google-services.json 脚本"
echo "========================================"
echo ""

# 检查下载文件是否存在
if [ ! -f ~/Downloads/google-services.json ]; then
    echo "❌ 错误：未找到 ~/Downloads/google-services.json"
    echo ""
    echo "请先从 Firebase Console 下载文件："
    echo "1. 访问 https://console.firebase.google.com/project/quran-majeed-aa3d2/settings/general"
    echo "2. 点击下载 google-services.json"
    echo "3. 保存到 ~/Downloads/"
    echo ""
    exit 1
fi

# 备份旧文件
echo "📦 备份旧文件..."
cp app/google-services.json app/google-services.json.backup.$(date +%Y%m%d_%H%M%S)

# 替换文件
echo "📥 替换 google-services.json..."
cp ~/Downloads/google-services.json app/google-services.json

# 验证新文件
echo ""
echo "🔍 验证新文件内容..."
echo ""

# 检查是否包含 Debug SHA-1 的 OAuth 客户端
DEBUG_SHA1="8ae5e2c39e284c7c3277ed2e8957bf08ab4f9e45"
if grep -qi "$DEBUG_SHA1" app/google-services.json; then
    echo "✅ 发现 Debug SHA-1 配置"
else
    echo "⚠️  警告：未发现 Debug SHA-1 配置"
    echo "请确认是否在 Firebase Console 添加了 Debug SHA-1"
fi

# 检查 OAuth 客户端数量
OAUTH_COUNT=$(grep -c "\"client_type\":" app/google-services.json)
echo "✅ OAuth 客户端数量：$OAUTH_COUNT"

# 检查 Web Client ID
WEB_CLIENT_ID=$(grep -o '"517834286063-52gsp24nqkb7sht7e7jn31397nhanumb.apps.googleusercontent.com"' app/google-services.json | head -1)
if [ ! -z "$WEB_CLIENT_ID" ]; then
    echo "✅ Web Client ID 存在"
else
    echo "❌ 未找到 Web Client ID"
fi

echo ""
echo "========================================"
echo "✅ 替换完成！"
echo "========================================"
echo ""
echo "下一步："
echo "1. 运行 ./gradlew clean"
echo "2. 运行 ./gradlew installDebug"
echo "3. 测试 Google 登录"
echo ""

