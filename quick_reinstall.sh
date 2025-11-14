#!/bin/bash

# ====================================================
# 快速重装脚本 - 防止 Google 登录/订阅缓存冲突
# ====================================================

set -e

echo ""
echo "🔄 开始快速重装流程..."
echo "=================================="
echo ""

# 1. 检查设备连接
echo "📱 检查设备连接..."
if ! adb devices | grep -q "device$"; then
    echo "❌ 错误: 没有连接的设备！"
    echo "   请连接设备后重试。"
    exit 1
fi
echo "✅ 设备已连接"
echo ""

# 2. 卸载应用
echo "🗑️  卸载旧版本应用..."
if adb uninstall com.quran.quranaudio.online 2>/dev/null; then
    echo "✅ 应用已卸载"
else
    echo "⚠️  应用未安装（跳过）"
fi
echo ""

# 3. 清除 Google Play Services 缓存
echo "🧹 清除 Google Play Services 缓存..."
if adb shell pm clear com.google.android.gms 2>/dev/null; then
    echo "✅ Google Play Services 缓存已清除"
else
    echo "⚠️  无法清除 Google Play Services 缓存"
fi
echo ""

# 4. 清理项目构建
echo "🧹 清理项目构建..."
./gradlew clean > /dev/null 2>&1
echo "✅ 项目已清理"
echo ""

# 5. 编译安装
echo "🔨 编译并安装应用..."
echo "   这可能需要几分钟时间..."
echo ""
./gradlew installDebug

echo ""
echo "=================================="
echo "✅ 快速重装完成！"
echo ""
echo "📝 重要提示:"
echo "   1. 如果 Google 登录仍然失败，请重启设备"
echo "   2. 重启命令: adb reboot"
echo "   3. 重启后等待设备完全启动再测试"
echo ""
echo "🎯 下一步:"
echo "   - 打开应用测试 Google 登录"
echo "   - 测试订阅功能"
echo "   - 如有问题，查看日志: adb logcat | grep -E \"GoogleAuth|Billing\""
echo ""


