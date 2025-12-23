#!/bin/bash

# 🔥 终极清理脚本 - 解决顽固的 Gradle Daemon 缓存问题
# 用于解决 KAPT JDK 模块访问错误

echo "🔥 开始终极清理..."
echo ""

# 1. 强制终止所有 Gradle/Kotlin 进程
echo "⏹️  终止所有 Gradle 进程..."
pkill -9 -f "gradle" 2>/dev/null
pkill -9 -f "kotlin" 2>/dev/null
pkill -9 -f "java.*gradle" 2>/dev/null
sleep 2

# 2. 删除用户级 Gradle 缓存
echo "🗑️  删除 ~/.gradle/caches..."
rm -rf ~/.gradle/caches/

echo "🗑️  删除 ~/.gradle/daemon..."
rm -rf ~/.gradle/daemon/

echo "🗑️  删除 ~/.gradle/wrapper..."
rm -rf ~/.gradle/wrapper/

# 3. 删除项目级缓存
echo "🗑️  删除项目 .gradle 目录..."
rm -rf .gradle/

# 4. 删除所有模块的 build 目录
echo "🗑️  删除所有 build 目录..."
rm -rf app/build/
rm -rf adlib/build/
rm -rf quiz/build/
rm -rf peacedesign/build/
rm -rf shaheendevelopersAds_SDK/build/
rm -rf */build/

# 5. 删除 Kotlin 编译缓存
echo "🗑️  删除 Kotlin 缓存..."
rm -rf ~/.kotlin/

# 6. 删除 Android Studio 缓存（如果存在）
echo "🗑️  删除 Android Studio 缓存..."
rm -rf ~/Library/Caches/AndroidStudio*/gradle/
rm -rf ~/Library/Caches/Google/AndroidStudio*/

echo ""
echo "======================================"
echo "✅ 终极清理完成！"
echo "======================================"
echo ""
echo "📝 下一步操作："
echo ""
echo "1. 确认 Android Studio 已完全关闭"
echo "2. 重新打开 Android Studio"
echo "3. 等待索引完成"
echo "4. Sync Project with Gradle Files"
echo "5. Build → Rebuild Project"
echo ""
echo "⚠️  重要："
echo "   第一次 Sync 会重新下载所有依赖"
echo "   可能需要 5-10 分钟，请耐心等待"
echo ""

