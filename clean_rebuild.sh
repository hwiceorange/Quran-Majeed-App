#!/bin/bash

# 🧹 清理并重新构建脚本
# 用于解决 KAPT/Gradle 缓存问题

echo "🧹 开始清理项目..."

# 进入项目目录
cd "$(dirname "$0")"

# 1. 停止 Gradle Daemon
echo "⏹️  停止 Gradle Daemon..."
./gradlew --stop

# 2. 清理项目构建缓存
echo "🗑️  清理项目构建缓存..."
./gradlew clean

# 3. 删除 .gradle 缓存目录
echo "🗑️  删除 .gradle 缓存..."
rm -rf .gradle/

# 4. 删除所有模块的 build 目录
echo "🗑️  删除 build 目录..."
rm -rf app/build/
rm -rf adlib/build/
rm -rf quiz/build/
rm -rf */build/

# 5. 删除 Kotlin 编译缓存
echo "🗑️  删除 Kotlin 编译缓存..."
rm -rf ~/.gradle/caches/
rm -rf ~/.kotlin/

echo ""
echo "✅ 清理完成！"
echo ""
echo "🔨 开始重新构建..."
echo ""

# 6. 重新构建项目
./gradlew assembleRelease --no-daemon --stacktrace

# 检查构建结果
if [ $? -eq 0 ]; then
    echo ""
    echo "🎉 构建成功！"
    echo "📦 APK 位置: app/build/outputs/apk/release/"
else
    echo ""
    echo "❌ 构建失败，请查看上面的错误信息"
    exit 1
fi

