#!/bin/bash

# 🔄 重新编译和测试脚本（修复 JSON 格式后）

set -e

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║           🔄 Tafsir JSON 修复后重新编译测试                     ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# 1. Clean 构建
echo "🧹 清理旧构建..."
./gradlew clean

# 2. 重新编译
echo ""
echo "🔨 重新编译应用..."
./gradlew :app:assembleDebug

# 3. 检查 APK
echo ""
echo "📦 检查 APK 中的 assets 文件..."
if unzip -p app/build/outputs/apk/debug/app-debug.apk assets/tafsir/available_tafsirs_info.json | grep -q '"key"'; then
    echo "✅ APK 包含正确格式的 JSON（包含 'key' 字段）"
else
    echo "❌ 警告: APK 中的 JSON 格式可能不正确"
    echo ""
    echo "JSON 内容预览："
    unzip -p app/build/outputs/apk/debug/app-debug.apk assets/tafsir/available_tafsirs_info.json | head -20
    exit 1
fi

# 4. 检查手机连接
echo ""
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

# 5. 卸载旧版本
echo ""
echo "🗑️  卸载旧版本..."
adb uninstall com.quran.quranaudio.online 2>/dev/null || echo "   (没有旧版本)"

# 6. 安装新版本
echo ""
echo "📦 安装新版本..."
adb install app/build/outputs/apk/debug/app-debug.apk

# 7. 清除应用数据
echo ""
echo "🧹 清除应用数据..."
adb shell pm clear com.quran.quranaudio.online

# 8. 启动日志监控
echo ""
echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║                      📊 日志监控中...                            ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""
echo "⚠️  现在请在手机上操作："
echo ""
echo "1. 打开 Quran0 应用"
echo "2. 在语言选择页选择 **印尼语 (Indonesian)**"
echo "3. 完成引导流程"
echo "4. 打开任意经文（例如 Surah Al-Fatihah）"
echo "5. 点击 **Tafsir（注释）** 按钮"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🔍 关键日志（期待看到）："
echo ""
echo "  ✅ Loaded from assets, length=XXX"
echo "  ✅ Parsed 3 language groups"
echo "     - id: 1 tafsirs"
echo "  ✅ Auto-selected Tafsir: id-tafsir-kemenag"
echo "  ✅ TafsirManager prepared, models available: true"
echo ""
echo "❌ 如果看到 'JSON parse error'，说明还有问题"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "📋 日志输出："
echo ""

# 清除旧日志
adb logcat -c

# 监控关键日志（带颜色高亮）
adb logcat | grep -E 'TafsirManager|MainActivity.*Tafsir|ActivityTafsir' --line-buffered | while read line; do
    # 高亮关键信息
    if [[ "$line" =~ "✅" ]]; then
        echo -e "\033[0;32m$line\033[0m"  # 绿色
    elif [[ "$line" =~ "❌" ]] || [[ "$line" =~ "parse error" ]]; then
        echo -e "\033[0;31m$line\033[0m"  # 红色
    elif [[ "$line" =~ "⚠️" ]]; then
        echo -e "\033[0;33m$line\033[0m"  # 黄色
    elif [[ "$line" =~ "id-tafsir-kemenag" ]]; then
        echo -e "\033[1;32m$line\033[0m"  # 粗体绿色（重要！）
    elif [[ "$line" =~ "Parsed.*language" ]]; then
        echo -e "\033[1;36m$line\033[0m"  # 粗体青色
    elif [[ "$line" =~ "Loaded from assets" ]]; then
        echo -e "\033[1;35m$line\033[0m"  # 粗体紫色
    else
        echo "$line"
    fi
done

