#!/bin/bash

# 🛑 强制停止所有 Gradle 进程
# 用于解决 Gradle Daemon 使用旧 JVM 参数的问题

echo "🛑 强制停止所有 Gradle 相关进程..."
echo ""

# 查找并杀死所有 Gradle Daemon 进程
echo "正在查找 Gradle Daemon 进程..."
GRADLE_PIDS=$(ps aux | grep -i "gradle.*daemon" | grep -v grep | awk '{print $2}')

if [ -z "$GRADLE_PIDS" ]; then
    echo "✅ 没有运行中的 Gradle Daemon"
else
    echo "找到以下 Gradle Daemon 进程:"
    ps aux | grep -i "gradle.*daemon" | grep -v grep
    echo ""
    echo "正在终止..."
    echo "$GRADLE_PIDS" | xargs kill -9 2>/dev/null
    echo "✅ Gradle Daemon 已停止"
fi

echo ""

# 查找并杀死所有 Kotlin 编译进程
echo "正在查找 Kotlin 编译进程..."
KOTLIN_PIDS=$(ps aux | grep -i "kotlin.*compile" | grep -v grep | awk '{print $2}')

if [ -z "$KOTLIN_PIDS" ]; then
    echo "✅ 没有运行中的 Kotlin 编译进程"
else
    echo "找到以下 Kotlin 编译进程:"
    ps aux | grep -i "kotlin.*compile" | grep -v grep
    echo ""
    echo "正在终止..."
    echo "$KOTLIN_PIDS" | xargs kill -9 2>/dev/null
    echo "✅ Kotlin 编译进程已停止"
fi

echo ""

# 查找并杀死所有 Java 编译进程（与 KAPT 相关）
echo "正在查找 Java 编译进程..."
JAVA_COMPILE_PIDS=$(ps aux | grep -i "java.*kapt\|javac.*kapt" | grep -v grep | awk '{print $2}')

if [ -z "$JAVA_COMPILE_PIDS" ]; then
    echo "✅ 没有运行中的 KAPT 相关进程"
else
    echo "找到以下 KAPT 相关进程:"
    ps aux | grep -i "java.*kapt\|javac.*kapt" | grep -v grep
    echo ""
    echo "正在终止..."
    echo "$JAVA_COMPILE_PIDS" | xargs kill -9 2>/dev/null
    echo "✅ KAPT 相关进程已停止"
fi

echo ""
echo "======================================"
echo "✅ 所有 Gradle/Kotlin/KAPT 进程已停止"
echo "======================================"
echo ""
echo "📝 下一步操作："
echo "1. 在 Android Studio 中："
echo "   File → Invalidate Caches → Invalidate and Restart"
echo ""
echo "2. 或在终端执行："
echo "   ./clean_rebuild.sh"
echo ""

