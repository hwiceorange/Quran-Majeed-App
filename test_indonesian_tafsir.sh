#!/bin/bash

# 🧪 印尼语 Tafsir 完整测试脚本

set -e

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║           🧪 印尼语 Tafsir 测试脚本                             ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# 检查手机连接
echo "📱 检查手机连接..."
if ! adb devices | grep -q "device$"; then
    echo "❌ 未检测到手机连接"
    echo "   请确保："
    echo "   1. 手机已通过 USB 连接"
    echo "   2. 已开启 USB 调试"
    echo "   3. 已授权此电脑"
    exit 1
fi
echo "✅ 手机已连接"
echo ""

# 卸载旧版本
echo "🗑️  卸载旧版本..."
adb uninstall com.quran.quranaudio.online 2>/dev/null || echo "   (没有旧版本)"
echo ""

# 安装新版本
echo "📦 安装新版本..."
if [ ! -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "❌ APK 文件不存在"
    echo "   请先在 Android Studio 中编译应用"
    exit 1
fi

adb install app/build/outputs/apk/debug/app-debug.apk
echo "✅ 安装完成"
echo ""

# 清除应用数据（确保全新状态）
echo "🧹 清除应用数据..."
adb shell pm clear com.quran.quranaudio.online
echo "✅ 数据已清除"
echo ""

# 启动日志监控
echo "📊 启动日志监控..."
echo "================================"
echo ""
echo "⚠️  现在请在手机上操作："
echo ""
echo "1. 打开 Quran0 应用"
echo "2. 在语言选择页选择 **印尼语 (Indonesian)**"
echo "3. 完成引导流程"
echo "4. 打开任意经文（例如 Surah Al-Fatihah）"
echo "5. 点击 **Tafsir（注释）** 按钮"
echo ""
echo "================================"
echo "📋 日志输出（自动监控中）："
echo "================================"
echo ""

# 清除旧日志
adb logcat -c

# 监控关键日志
adb logcat | grep -E 'TafsirManager|MainActivity.*Tafsir|ActivityTafsir' --line-buffered | while read line; do
    # 高亮重要信息
    if [[ "$line" =~ "✅" ]]; then
        echo -e "\033[0;32m$line\033[0m"  # 绿色
    elif [[ "$line" =~ "❌" ]]; then
        echo -e "\033[0;31m$line\033[0m"  # 红色
    elif [[ "$line" =~ "⚠️" ]]; then
        echo -e "\033[0;33m$line\033[0m"  # 黄色
    elif [[ "$line" =~ "id-tafsir-kemenag" ]]; then
        echo -e "\033[1;32m$line\033[0m"  # 粗体绿色（重要！）
    elif [[ "$line" =~ "Parsed.*language" ]]; then
        echo -e "\033[1;36m$line\033[0m"  # 粗体青色
    else
        echo "$line"
    fi
done

