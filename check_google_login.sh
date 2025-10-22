#!/bin/bash

# Google 登录诊断脚本
# 使用方法：./check_google_login.sh

echo "========================================="
echo "🔍 Google 登录诊断工具"
echo "========================================="
echo ""

# 检查 ADB 连接
echo "1. 检查设备连接..."
if ! command -v adb &> /dev/null; then
    echo "❌ 错误：ADB 未安装或不在 PATH 中"
    exit 1
fi

DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device" | wc -l)
if [ "$DEVICE_COUNT" -eq 0 ]; then
    echo "❌ 错误：未检测到设备，请连接设备后重试"
    exit 1
fi

DEVICE_NAME=$(adb shell getprop ro.product.model)
ANDROID_VERSION=$(adb shell getprop ro.build.version.release)
echo "✅ 设备已连接: $DEVICE_NAME (Android $ANDROID_VERSION)"
echo ""

# 检查应用是否安装
echo "2. 检查应用状态..."
if adb shell pm list packages | grep -q "com.quran.quranaudio.online"; then
    echo "✅ 应用已安装"
else
    echo "❌ 应用未安装"
    exit 1
fi

# 检查应用是否正在运行
if adb shell pidof com.quran.quranaudio.online > /dev/null; then
    echo "✅ 应用正在运行"
    APP_PID=$(adb shell pidof com.quran.quranaudio.online)
    echo "   进程 ID: $APP_PID"
else
    echo "⚠️  应用未运行，请先启动应用"
fi
echo ""

# 清除旧日志
echo "3. 准备日志监控..."
adb logcat -c
echo "✅ 已清除旧日志"
echo ""

echo "========================================="
echo "📱 请在设备上执行以下操作："
echo "   1. 点击主页右上角的头像图标"
echo "   2. 选择一个 Google 账户登录"
echo "   3. 观察下方日志输出"
echo "========================================="
echo ""
echo "🔍 实时日志（按 Ctrl+C 停止）："
echo "========================================="

# 实时显示 Google 登录相关日志
adb logcat | grep --line-buffered -E "FragMain|GoogleAuth" | while read line; do
    # 使用颜色标记不同级别的日志
    if echo "$line" | grep -q "ERROR\|E/"; then
        echo -e "\033[0;31m$line\033[0m"  # 红色 - 错误
    elif echo "$line" | grep -q "SUCCESS\|updated successfully"; then
        echo -e "\033[0;32m$line\033[0m"  # 绿色 - 成功
    elif echo "$line" | grep -q "WARNING\|W/"; then
        echo -e "\033[0;33m$line\033[0m"  # 黄色 - 警告
    else
        echo "$line"  # 正常
    fi
done

