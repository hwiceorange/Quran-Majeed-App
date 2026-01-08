#!/bin/bash

# 🚀 一键安装并调试应用
# 自动安装APK、启动应用、监控崩溃日志

echo "╔════════════════════════════════════════════════════════════╗"
echo "║         🚀 Quran Majeed - Install & Debug Script          ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.quran.quranaudio.online"
MAIN_ACTIVITY=".SplashScreenActivity"

# 检查设备连接
echo "📱 检查设备连接..."
if ! adb devices | grep -q "device$"; then
    echo "❌ 没有检测到Android设备"
    echo "请确保："
    echo "  1. USB调试已开启"
    echo "  2. 设备已通过USB连接"
    echo "  3. 已授权USB调试"
    exit 1
fi

DEVICE_MODEL=$(adb shell getprop ro.product.model)
ANDROID_VERSION=$(adb shell getprop ro.build.version.release)
SDK_VERSION=$(adb shell getprop ro.build.version.sdk)

echo "✅ 设备已连接"
echo "   机型: $DEVICE_MODEL"
echo "   Android: $ANDROID_VERSION (API $SDK_VERSION)"
echo ""

# 检查APK文件
if [ ! -f "$APK_PATH" ]; then
    echo "❌ APK文件不存在: $APK_PATH"
    echo "请先编译: ./gradlew assembleDebug"
    exit 1
fi

echo "📦 APK文件: $APK_PATH"
APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
echo "   大小: $APK_SIZE"
echo ""

# 卸载旧版本
echo "🗑️  卸载旧版本..."
adb uninstall $PACKAGE_NAME 2>/dev/null
sleep 1

# 安装新版本
echo "📲 安装新版本..."
if adb install -r "$APK_PATH"; then
    echo "✅ 安装成功"
else
    echo "❌ 安装失败"
    exit 1
fi
echo ""

# 清除旧日志
echo "🧹 清除旧日志..."
adb logcat -c
sleep 1

# 启动应用
echo "🚀 启动应用..."
adb shell am start -n "${PACKAGE_NAME}/${PACKAGE_NAME}${MAIN_ACTIVITY}"
sleep 1

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo "║              📊 监控应用日志（实时）                       ║"
echo "╠════════════════════════════════════════════════════════════╣"
echo "║  🔍 监控标签：                                             ║"
echo "║     • CRASH_DEBUG    - 应用初始化日志                      ║"
echo "║     • CRASH_HANDLER  - 全局异常捕获                        ║"
echo "║     • AndroidRuntime - 系统崩溃日志                        ║"
echo "║                                                            ║"
echo "║  ⌨️  操作提示：                                             ║"
echo "║     • 按 Ctrl+C 停止监控                                   ║"
echo "║     • 应用崩溃时会自动显示完整堆栈                         ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# 监控崩溃日志（带颜色输出）
adb logcat -v time | grep --line-buffered -E "CRASH_DEBUG|CRASH_HANDLER|AndroidRuntime.*FATAL|libc.*Fatal|DEBUG.*Native.*crashed" | while read line; do
    # 高亮显示关键信息
    if echo "$line" | grep -q "CRASH_HANDLER"; then
        echo -e "\033[1;31m$line\033[0m"  # 红色
    elif echo "$line" | grep -q "CRASH_DEBUG.*✅"; then
        echo -e "\033[1;32m$line\033[0m"  # 绿色
    elif echo "$line" | grep -q "CRASH_DEBUG.*❌"; then
        echo -e "\033[1;31m$line\033[0m"  # 红色
    elif echo "$line" | grep -q "FATAL"; then
        echo -e "\033[1;31m$line\033[0m"  # 红色
    else
        echo "$line"
    fi
done

